(function () {
    'use strict';

    let cart = null;
    let addresses = [];
    let selectedAddressId = null;
    let addressModal;

    document.addEventListener('DOMContentLoaded', async function () {
        addressModal = new bootstrap.Modal(document.getElementById('addressModal'));

        await Promise.all([loadCart(), loadAddresses()]);

        document.getElementById('addAddressBtn').addEventListener('click', () => addressModal.show());
        document.getElementById('addressForm').addEventListener('submit', handleAddAddress);
        document.getElementById('placeOrderBtn').addEventListener('click', placeOrder);
    });

    async function loadCart() {
        try {
            const res = await fetch('/api/cart');
            if (res.status === 401) { window.requireLoginThen('/checkout'); return; }
            if (!res.ok) throw new Error('Failed to load cart');
            cart = await res.json();

            if (!cart.items || cart.items.length === 0) {
                window.location.href = '/cart';
                return;
            }
            renderSummary();
        } catch (e) {
            console.error(e);
            window.toastError('Could not load your cart');
        }
    }

    function renderSummary() {
        const discount = Number(sessionStorage.getItem('appliedCouponDiscount') || 0);
        const code = sessionStorage.getItem('appliedCouponCode');
        const el = document.getElementById('orderSummary');

        el.innerHTML = `
            <div class="d-flex justify-content-between mb-2"><span class="text-muted">Subtotal</span><span>${window.formatCurrency(cart.subtotal)}</span></div>
            <div class="d-flex justify-content-between mb-2"><span class="text-muted">Shipping</span><span>${Number(cart.shippingCharge) === 0 ? 'FREE' : window.formatCurrency(cart.shippingCharge)}</span></div>
            <div class="d-flex justify-content-between mb-2"><span class="text-muted">GST</span><span>${window.formatCurrency(cart.gst)}</span></div>
            ${discount > 0 ? `<div class="d-flex justify-content-between mb-2 text-success"><span>Discount (${escapeHtml(code)})</span><span>-${window.formatCurrency(discount)}</span></div>` : ''}
            <hr>
            <div class="d-flex justify-content-between fw-bold h6"><span>Total</span><span>${window.formatCurrency(Number(cart.total) - discount)}</span></div>`;
    }

    async function loadAddresses() {
        const container = document.getElementById('addressList');
        try {
            const res = await fetch('/api/addresses');
            if (!res.ok) throw new Error('Failed to load addresses');
            addresses = await res.json();
            renderAddresses();
        } catch (e) {
            container.innerHTML = '<p class="text-danger small">Could not load addresses.</p>';
        }
    }

    function renderAddresses() {
        const container = document.getElementById('addressList');
        if (addresses.length === 0) {
            container.innerHTML = '<p class="text-muted small mb-0">No saved addresses — add one to continue.</p>';
            document.getElementById('placeOrderBtn').disabled = true;
            return;
        }

        if (!selectedAddressId || !addresses.some(a => a.id === selectedAddressId)) {
            const defaultAddr = addresses.find(a => a.isDefault) || addresses[0];
            selectedAddressId = defaultAddr.id;
        }

        container.innerHTML = addresses.map(a => `
            <label class="card-surface p-3 d-flex gap-2 align-items-start" style="cursor:pointer">
                <input type="radio" name="addressSelect" value="${a.id}" ${a.id === selectedAddressId ? 'checked' : ''} class="mt-1">
                <div class="small">
                    <div class="fw-semibold">${escapeHtml(a.fullName)} ${a.isDefault ? '<span class="status-pill status-pill--delivered ms-1">Default</span>' : ''}</div>
                    <div>${escapeHtml(a.addressLine1)}${a.addressLine2 ? ', ' + escapeHtml(a.addressLine2) : ''}</div>
                    <div>${escapeHtml(a.city)}, ${escapeHtml(a.state)} ${escapeHtml(a.postalCode)}</div>
                    <div class="text-muted">${escapeHtml(a.phone)}</div>
                </div>
            </label>`).join('');

        container.querySelectorAll('input[name="addressSelect"]').forEach(input => {
            input.addEventListener('change', () => { selectedAddressId = Number(input.value); });
        });

        document.getElementById('placeOrderBtn').disabled = false;
    }

    async function handleAddAddress(e) {
        e.preventDefault();
        const payload = {
            fullName: document.getElementById('addrName').value,
            phone: document.getElementById('addrPhone').value,
            addressLine1: document.getElementById('addrLine1').value,
            addressLine2: document.getElementById('addrLine2').value,
            city: document.getElementById('addrCity').value,
            state: document.getElementById('addrState').value,
            postalCode: document.getElementById('addrPostal').value,
            country: document.getElementById('addrCountry').value || 'India'
        };

        try {
            const res = await fetch('/api/addresses', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not save address');
            }
            const newAddress = await res.json();
            addressModal.hide();
            e.target.reset();
            selectedAddressId = newAddress.id;
            await loadAddresses();
            window.toastSuccess('Address saved');
        } catch (err) {
            window.toastError(err.message);
        }
    }

    async function placeOrder() {
        if (!selectedAddressId) { window.toastError('Please select a delivery address'); return; }
        const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked').value;
        const couponCode = sessionStorage.getItem('appliedCouponCode') || null;

        const btn = document.getElementById('placeOrderBtn');
        btn.disabled = true;
        btn.textContent = 'Placing order…';

        try {
            const res = await fetch('/api/orders/checkout', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ addressId: selectedAddressId, paymentMethod, couponCode })
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not place order');
            }
            const order = await res.json();
            sessionStorage.removeItem('appliedCouponCode');
            sessionStorage.removeItem('appliedCouponDiscount');
            window.location.href = `/orders/${order.id}?justPlaced=1`;
        } catch (err) {
            window.toastError(err.message);
            btn.disabled = false;
            btn.textContent = 'Place order';
        }
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }
})();
