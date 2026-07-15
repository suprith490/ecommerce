(function () {
    'use strict';

    let cart = null;

    document.addEventListener('DOMContentLoaded', async function () {
        await loadCart();
        document.getElementById('applyCouponBtn').addEventListener('click', applyCoupon);

        const savedCoupon = sessionStorage.getItem('appliedCouponCode');
        if (savedCoupon) {
            document.getElementById('couponInput').value = savedCoupon;
            await applyCoupon(true);
        }
    });

    async function loadCart() {
        const container = document.getElementById('cartItemsContainer');
        try {
            const res = await fetch('/api/cart');
            if (res.status === 401) { window.requireLoginThen('/cart'); return; }
            if (!res.ok) throw new Error('Failed to load cart');
            cart = await res.json();
            renderItems();
            renderSummary();
            window.refreshNavCounts();
        } catch (e) {
            console.error(e);
            container.innerHTML = '<p class="text-danger">Could not load your cart.</p>';
        }
    }

    function renderItems() {
        const container = document.getElementById('cartItemsContainer');
        if (!cart.items || cart.items.length === 0) {
            container.innerHTML = `
                <div class="card-surface p-5 text-center">
                    <i class="fa-solid fa-cart-shopping fa-2x text-muted mb-3"></i>
                    <p class="mb-3">Your cart is empty.</p>
                    <a href="/products" class="btn btn-brand">Start shopping</a>
                </div>`;
            document.getElementById('checkoutBtn').classList.add('disabled');
            return;
        }

        container.innerHTML = cart.items.map(item => `
            <div class="card-surface p-3 d-flex flex-row gap-3 align-items-center" data-product-id="${item.product.id}">
                <img src="${item.product.primaryImageUrl || 'https://placehold.co/100x100?text=No+Image'}" alt=""
                     style="width:80px;height:80px;object-fit:cover;border-radius:10px">
                <div class="flex-fill">
                    <a href="/products/${item.product.slug}" class="text-decoration-none text-reset fw-semibold small d-block">${escapeHtml(item.product.name)}</a>
                    <div class="small text-muted">${window.formatCurrency(item.unitPrice)} each</div>
                    ${item.quantityExceedsStock ? `<div class="small text-danger">Only ${item.availableStock} left — please reduce quantity</div>` : ''}
                    ${!item.inStock ? '<div class="small text-danger">Out of stock</div>' : ''}
                    <div class="d-flex align-items-center gap-2 mt-2">
                        <div class="input-group input-group-sm" style="width:110px">
                            <button class="btn btn-light qty-minus" type="button">−</button>
                            <input type="text" class="form-control text-center qty-value" value="${item.quantity}" readonly>
                            <button class="btn btn-light qty-plus" type="button">+</button>
                        </div>
                        <button class="btn btn-sm btn-light move-to-wishlist"><i class="fa-regular fa-heart"></i></button>
                        <button class="btn btn-sm btn-light text-danger remove-item"><i class="fa-solid fa-trash"></i></button>
                    </div>
                </div>
                <div class="fw-bold">${window.formatCurrency(item.lineTotal)}</div>
            </div>`).join('');

        container.querySelectorAll('[data-product-id]').forEach(row => {
            const productId = row.getAttribute('data-product-id');
            row.querySelector('.qty-minus').addEventListener('click', () => updateQuantity(productId, -1, row));
            row.querySelector('.qty-plus').addEventListener('click', () => updateQuantity(productId, 1, row));
            row.querySelector('.move-to-wishlist').addEventListener('click', () => moveToWishlist(productId));
            row.querySelector('.remove-item').addEventListener('click', () => removeItem(productId));
        });
    }

    function renderSummary() {
        const el = document.getElementById('orderSummary');
        const discount = Number(sessionStorage.getItem('appliedCouponDiscount') || 0);
        const code = sessionStorage.getItem('appliedCouponCode');

        el.innerHTML = `
            <div class="d-flex justify-content-between mb-2"><span class="text-muted">Subtotal</span><span>${window.formatCurrency(cart.subtotal)}</span></div>
            <div class="d-flex justify-content-between mb-2"><span class="text-muted">Shipping</span><span>${Number(cart.shippingCharge) === 0 ? 'FREE' : window.formatCurrency(cart.shippingCharge)}</span></div>
            <div class="d-flex justify-content-between mb-2"><span class="text-muted">GST</span><span>${window.formatCurrency(cart.gst)}</span></div>
            ${discount > 0 ? `<div class="d-flex justify-content-between mb-2 text-success"><span>Discount (${escapeHtml(code)})</span><span>-${window.formatCurrency(discount)}</span></div>` : ''}
            ${Number(cart.amountToFreeShipping) > 0 ? `<div class="small text-muted mb-2">Add ${window.formatCurrency(cart.amountToFreeShipping)} more for free shipping</div>` : ''}
            <hr>
            <div class="d-flex justify-content-between fw-bold h6"><span>Total</span><span>${window.formatCurrency(Number(cart.total) - discount)}</span></div>`;
    }

    async function updateQuantity(productId, delta, row) {
        const input = row.querySelector('.qty-value');
        const newQty = Math.max(0, Number(input.value) + delta);
        try {
            const res = await fetch(`/api/cart/items/${productId}?quantity=${newQty}`, { method: 'PATCH' });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not update quantity');
            }
            cart = await res.json();
            renderItems();
            renderSummary();
            window.refreshNavCounts();
        } catch (e) {
            window.toastError(e.message);
        }
    }

    async function removeItem(productId) {
        try {
            const res = await fetch(`/api/cart/items/${productId}`, { method: 'DELETE' });
            if (!res.ok) throw new Error('Could not remove item');
            cart = await res.json();
            renderItems();
            renderSummary();
            window.refreshNavCounts();
        } catch (e) {
            window.toastError(e.message);
        }
    }

    async function moveToWishlist(productId) {
        try {
            const res = await fetch(`/api/cart/items/${productId}/move-to-wishlist`, { method: 'POST' });
            if (!res.ok) throw new Error('Could not move item to wishlist');
            cart = await res.json();
            window.toastSuccess('Moved to wishlist');
            renderItems();
            renderSummary();
            window.refreshNavCounts();
        } catch (e) {
            window.toastError(e.message);
        }
    }

    async function applyCoupon(silent) {
        const code = document.getElementById('couponInput').value.trim();
        const messageEl = document.getElementById('couponMessage');
        if (!code) return;

        try {
            const res = await fetch('/api/cart/coupon/preview', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code })
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok) throw new Error(data.message || 'Invalid coupon');

            sessionStorage.setItem('appliedCouponCode', data.code);
            sessionStorage.setItem('appliedCouponDiscount', data.discountAmount);
            messageEl.innerHTML = `<span class="text-success">${escapeHtml(data.message)}</span>`;
            renderSummary();
            if (!silent) window.toastSuccess('Coupon applied');
        } catch (e) {
            sessionStorage.removeItem('appliedCouponCode');
            sessionStorage.removeItem('appliedCouponDiscount');
            messageEl.innerHTML = `<span class="text-danger">${escapeHtml(e.message)}</span>`;
            renderSummary();
        }
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }
})();
