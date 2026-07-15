(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', loadWishlist);

    async function loadWishlist() {
        const grid = document.getElementById('wishlistGrid');
        try {
            const res = await fetch('/api/wishlist');
            if (res.status === 401) { window.requireLoginThen('/wishlist'); return; }
            if (!res.ok) throw new Error('Failed to load wishlist');
            const data = await res.json();
            const items = data.items || [];

            if (items.length === 0) {
                grid.innerHTML = `
                    <div class="col-12 text-center py-5">
                        <i class="fa-regular fa-heart fa-2x text-muted mb-3"></i>
                        <p class="mb-3">Your wishlist is empty.</p>
                        <a href="/products" class="btn btn-brand">Discover products</a>
                    </div>`;
                return;
            }

            grid.innerHTML = items.map(item => {
                const p = item.product;
                const img = p.primaryImageUrl || 'https://placehold.co/400x400?text=No+Image';
                return `
                <div class="col-6 col-md-3">
                    <div class="product-card">
                        <div class="product-image">
                            <a href="/products/${p.slug}"><img src="${img}" alt="${escapeHtml(p.name)}" loading="lazy"></a>
                            ${p.discountPercentage > 0 ? `<span class="discount-badge">${p.discountPercentage}% OFF</span>` : ''}
                        </div>
                        <div class="card-body-custom">
                            <a href="/products/${p.slug}" class="text-decoration-none text-reset">
                                <div class="small fw-semibold text-truncate">${escapeHtml(p.name)}</div>
                            </a>
                            <div class="price-row">
                                <span class="fw-bold">${window.formatCurrency(p.effectivePrice)}</span>
                                ${!p.inStock ? '<div class="small text-danger">Out of stock</div>' : ''}
                                <div class="d-flex gap-2 mt-2">
                                    <button class="btn btn-brand btn-sm flex-fill" data-move="${p.id}" ${!p.inStock ? 'disabled' : ''}>Move to cart</button>
                                    <button class="btn btn-light btn-sm text-danger" data-remove="${p.id}"><i class="fa-solid fa-trash"></i></button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>`;
            }).join('');

            grid.querySelectorAll('[data-move]').forEach(btn =>
                btn.addEventListener('click', () => moveToCart(btn.getAttribute('data-move'))));
            grid.querySelectorAll('[data-remove]').forEach(btn =>
                btn.addEventListener('click', () => removeItem(btn.getAttribute('data-remove'))));
        } catch (e) {
            console.error(e);
            grid.innerHTML = '<div class="col-12 text-danger text-center py-5">Could not load your wishlist.</div>';
        }
    }

    async function moveToCart(productId) {
        try {
            const res = await fetch(`/api/wishlist/items/${productId}/move-to-cart?quantity=1`, { method: 'POST' });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not move item to cart');
            }
            window.toastSuccess('Moved to cart');
            window.refreshNavCounts();
            loadWishlist();
        } catch (e) {
            window.toastError(e.message);
        }
    }

    async function removeItem(productId) {
        try {
            const res = await fetch(`/api/wishlist/items/${productId}`, { method: 'DELETE' });
            if (!res.ok) throw new Error('Could not remove item');
            window.refreshNavCounts();
            loadWishlist();
        } catch (e) {
            window.toastError(e.message);
        }
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }
})();
