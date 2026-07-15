(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        loadCategoryMenu();
        loadAuthState();
        refreshCounts();
        wireSearch();
    });

    async function loadCategoryMenu() {
        const menu = document.getElementById('categoryMegaMenu');
        if (!menu) return;
        try {
            const res = await fetch('/api/categories');
            if (!res.ok) throw new Error('Failed to load categories');
            const categories = await res.json();
            if (categories.length === 0) {
                menu.innerHTML = '<div class="text-muted small px-2 py-2">No categories yet</div>';
                return;
            }
            menu.innerHTML = categories.map(c => `<a href="/products?categoryId=${c.id}">${escapeHtml(c.name)}</a>`).join('');
        } catch (e) {
            menu.innerHTML = '<div class="text-muted small px-2 py-2">Could not load categories</div>';
        }
    }

    async function loadAuthState() {
        const menu = document.getElementById('accountMenu');
        if (!menu) return;
        try {
            const res = await fetch('/api/auth/me');
            if (!res.ok) return; // stays as the logged-out menu
            const user = await res.json();
            menu.innerHTML = `
                <li><span class="dropdown-item-text small text-muted">${escapeHtml(user.name)}</span></li>
                <li><hr class="dropdown-divider"></li>
                <li><a class="dropdown-item" href="/profile"><i class="fa-regular fa-user me-2"></i>My profile</a></li>
                <li><a class="dropdown-item" href="/orders"><i class="fa-solid fa-box me-2"></i>My orders</a></li>
                ${user.role === 'ADMIN' ? '<li><a class="dropdown-item" href="/admin/dashboard"><i class="fa-solid fa-gauge me-2"></i>Admin dashboard</a></li>' : ''}
                <li><hr class="dropdown-divider"></li>
                <li><button class="dropdown-item text-danger" id="navLogoutBtn" type="button"><i class="fa-solid fa-right-from-bracket me-2"></i>Logout</button></li>`;

            document.getElementById('navLogoutBtn').addEventListener('click', async () => {
                await fetch('/api/auth/logout', { method: 'POST' });
                window.location.href = '/';
            });
        } catch (e) {
            // stay logged-out
        }
    }

    async function refreshCounts() {
        try {
            const cartRes = await fetch('/api/cart');
            if (cartRes.ok) {
                const cart = await cartRes.json();
                setBadge('cartCount', cart.itemCount);
            }
        } catch (e) { /* not logged in — leave badge hidden */ }

        try {
            const wishRes = await fetch('/api/wishlist');
            if (wishRes.ok) {
                const wishlist = await wishRes.json();
                setBadge('wishlistCount', wishlist.itemCount);
            }
        } catch (e) { /* not logged in — leave badge hidden */ }
    }

    function setBadge(id, count) {
        const el = document.getElementById(id);
        if (!el) return;
        if (count > 0) {
            el.textContent = count > 99 ? '99+' : count;
            el.classList.remove('d-none');
        } else {
            el.classList.add('d-none');
        }
    }

    function wireSearch() {
        const input = document.getElementById('navSearchInput');
        const box = document.getElementById('searchSuggestions');
        if (!input || !box) return;

        let debounce;
        input.addEventListener('input', () => {
            clearTimeout(debounce);
            const term = input.value.trim();
            if (term.length < 2) { box.classList.remove('show'); return; }
            debounce = setTimeout(() => runSearch(term, box), 300);
        });

        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && input.value.trim()) {
                window.location.href = '/products?search=' + encodeURIComponent(input.value.trim());
            }
        });

        document.addEventListener('click', (e) => {
            if (!box.contains(e.target) && e.target !== input) box.classList.remove('show');
        });
    }

    async function runSearch(term, box) {
        try {
            const res = await fetch(`/api/products?search=${encodeURIComponent(term)}&size=6`);
            if (!res.ok) return;
            const data = await res.json();
            const items = data.content || [];
            if (items.length === 0) {
                box.innerHTML = '<div class="text-muted small px-3 py-2">No matches found</div>';
            } else {
                box.innerHTML = items.map(p => `
                    <a href="/products/${p.slug}" class="suggestion-item">
                        ${p.primaryImageUrl ? `<img src="${p.primaryImageUrl}" style="width:36px;height:36px;object-fit:cover;border-radius:6px">` : ''}
                        <div>
                            <div class="small fw-semibold">${escapeHtml(p.name)}</div>
                            <div class="small text-muted">${window.formatCurrency(p.effectivePrice)}</div>
                        </div>
                    </a>`).join('');
            }
            box.classList.add('show');
        } catch (e) {
            box.classList.remove('show');
        }
    }

    // ---- Shared helpers reused across storefront pages ----

    window.formatCurrency = function (value) {
        const num = Number(value || 0);
        return '\u20B9' + num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    };

    window.formatDate = function (isoString) {
        if (!isoString) return '—';
        return new Date(isoString).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
    };

    window.toastError = function (message) {
        if (window.Swal) {
            Swal.fire({ icon: 'error', title: 'Something went wrong', text: message, confirmButtonColor: '#ff5722' });
        } else {
            alert(message);
        }
    };

    window.toastSuccess = function (message) {
        if (window.Swal) {
            Swal.fire({ icon: 'success', title: message, timer: 1400, showConfirmButton: false, toast: true, position: 'top-end' });
        } else {
            alert(message);
        }
    };

    window.requireLoginThen = function (redirectPath) {
        window.location.href = '/login?redirect=' + encodeURIComponent(redirectPath);
    };

    /** Adds a product to the cart; redirects to login if the visitor isn't authenticated. */
    window.addToCart = async function (productId, quantity) {
        quantity = quantity || 1;
        try {
            const res = await fetch('/api/cart/items', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ productId, quantity })
            });
            if (res.status === 401) { window.requireLoginThen(window.location.pathname); return false; }
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not add to cart');
            }
            window.toastSuccess('Added to cart');
            refreshCounts();
            return true;
        } catch (e) {
            window.toastError(e.message);
            return false;
        }
    };

    window.toggleWishlist = async function (productId, isCurrentlyWishlisted) {
        try {
            const res = await fetch(`/api/wishlist/items/${productId}`, {
                method: isCurrentlyWishlisted ? 'DELETE' : 'POST'
            });
            if (res.status === 401) { window.requireLoginThen(window.location.pathname); return null; }
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not update wishlist');
            }
            refreshCounts();
            return !isCurrentlyWishlisted;
        } catch (e) {
            window.toastError(e.message);
            return null;
        }
    };

    window.refreshNavCounts = refreshCounts;

    let wishlistedIds = null;
    async function getWishlistedIds() {
        if (wishlistedIds !== null) return wishlistedIds;
        try {
            const res = await fetch('/api/wishlist');
            if (!res.ok) { wishlistedIds = new Set(); return wishlistedIds; }
            const data = await res.json();
            wishlistedIds = new Set((data.items || []).map(i => i.product.id));
        } catch (e) {
            wishlistedIds = new Set();
        }
        return wishlistedIds;
    }

    /** Renders a standard product-grid card. Used by Home, PLP, and PDP "related products". */
    window.renderProductCard = function (p, isWishlisted) {
        const img = p.primaryImageUrl || 'https://placehold.co/400x400?text=No+Image';
        return `
        <div class="col-6 col-md-3">
            <div class="product-card">
                <div class="product-image">
                    <a href="/products/${p.slug}"><img src="${img}" alt="${escapeHtml(p.name)}" loading="lazy"></a>
                    ${p.discountPercentage > 0 ? `<span class="discount-badge">${p.discountPercentage}% OFF</span>` : ''}
                    <button class="wishlist-toggle ${isWishlisted ? 'active' : ''}" data-wishlist-id="${p.id}" aria-label="Toggle wishlist">
                        <i class="fa-${isWishlisted ? 'solid' : 'regular'} fa-heart"></i>
                    </button>
                </div>
                <div class="card-body-custom">
                    <a href="/products/${p.slug}" class="text-decoration-none text-reset">
                        <div class="small fw-semibold text-truncate">${escapeHtml(p.name)}</div>
                    </a>
                    <div class="rating-stars small mb-1">
                        ${p.ratingCount > 0 ? '★'.repeat(Math.round(p.averageRating)) + '☆'.repeat(5 - Math.round(p.averageRating)) + ` <span class="text-muted">(${p.ratingCount})</span>` : '<span class="text-muted">No ratings yet</span>'}
                    </div>
                    <div class="price-row">
                        ${p.offerPrice ? `<span class="text-decoration-line-through text-muted small">${window.formatCurrency(p.price)}</span> ` : ''}
                        <span class="fw-bold">${window.formatCurrency(p.effectivePrice)}</span>
                        ${!p.inStock ? '<div class="small text-danger">Out of stock</div>' : ''}
                        <button class="btn btn-brand btn-sm w-100 mt-2" data-add-to-cart="${p.id}" ${!p.inStock ? 'disabled' : ''}>
                            <i class="fa-solid fa-cart-plus me-1"></i>Add to cart
                        </button>
                    </div>
                </div>
            </div>
        </div>`;
    };

    /** Wires up "Add to cart" and wishlist-toggle buttons inside a freshly-rendered grid container. */
    window.wireProductGridEvents = function (container) {
        container.querySelectorAll('[data-add-to-cart]').forEach(btn => {
            btn.addEventListener('click', () => window.addToCart(Number(btn.getAttribute('data-add-to-cart')), 1));
        });
        container.querySelectorAll('[data-wishlist-id]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = Number(btn.getAttribute('data-wishlist-id'));
                const isActive = btn.classList.contains('active');
                const result = await window.toggleWishlist(id, isActive);
                if (result !== null) {
                    btn.classList.toggle('active', result);
                    btn.querySelector('i').className = result ? 'fa-solid fa-heart' : 'fa-regular fa-heart';
                }
            });
        });
    };

    window.getWishlistedIds = getWishlistedIds;

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }
})();
