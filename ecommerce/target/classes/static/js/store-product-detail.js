(function () {
    'use strict';

    let product = null;
    let currentUser = null;
    let selectedImageIndex = 0;
    let reviewPage = 0;

    document.addEventListener('DOMContentLoaded', async function () {
        const slug = window.location.pathname.split('/').filter(Boolean).pop();

        try {
            currentUser = await fetchCurrentUser();
        } catch (e) { currentUser = null; }

        try {
            const res = await fetch(`/api/products/${slug}`);
            if (!res.ok) throw new Error('Product not found');
            product = await res.json();
            document.getElementById('pageTitle').textContent = `${product.name} · Suprith Store`;
            renderProduct();
            loadReviews();
            loadRelated();
        } catch (e) {
            document.getElementById('productRoot').innerHTML =
                '<div class="text-center py-5"><h2 class="h5">Product not found</h2><a href="/products" class="text-brand">Browse all products →</a></div>';
        }
    });

    async function fetchCurrentUser() {
        const res = await fetch('/api/auth/me');
        if (!res.ok) return null;
        return res.json();
    }

    function renderProduct() {
        const wishlistedPromise = window.getWishlistedIds();
        const images = product.images && product.images.length > 0 ? product.images : [{ imageUrl: 'https://placehold.co/600x600?text=No+Image', altText: product.name }];

        document.getElementById('productRoot').innerHTML = `
        <div class="row g-4">
            <div class="col-12 col-lg-6">
                <div class="gallery-main mb-3" id="galleryMain">
                    <img src="${images[0].imageUrl}" alt="${escapeHtml(images[0].altText || product.name)}" id="galleryMainImg">
                </div>
                <div class="d-flex gap-2 flex-wrap" id="galleryThumbs">
                    ${images.map((img, i) => `
                        <div class="gallery-thumb ${i === 0 ? 'active' : ''}" data-index="${i}">
                            <img src="${img.imageUrl}" alt="">
                        </div>`).join('')}
                </div>
            </div>

            <div class="col-12 col-lg-6">
                <p class="text-muted small mb-1">${escapeHtml(product.categoryName || '')}${product.brandName ? ' · ' + escapeHtml(product.brandName) : ''}</p>
                <h1 class="h3 fw-bold mb-2">${escapeHtml(product.name)}</h1>
                <div class="rating-stars mb-2">
                    ${product.ratingCount > 0 ? '★'.repeat(Math.round(product.averageRating)) + '☆'.repeat(5 - Math.round(product.averageRating)) + ` <span class="text-muted small">(${product.ratingCount} review${product.ratingCount === 1 ? '' : 's'})</span>` : '<span class="text-muted small">No ratings yet</span>'}
                </div>

                <div class="d-flex align-items-center gap-2 mb-2">
                    ${product.offerPrice ? `<span class="text-decoration-line-through text-muted">${window.formatCurrency(product.price)}</span>` : ''}
                    <span class="h4 fw-bold mb-0">${window.formatCurrency(product.effectivePrice)}</span>
                    ${product.discountPercentage > 0 ? `<span class="status-pill status-pill--delivered">${product.discountPercentage}% OFF</span>` : ''}
                </div>
                <p class="small text-muted mb-3">Inclusive of all taxes</p>

                <p class="small ${product.inStock ? 'text-success' : 'text-danger'} fw-semibold mb-3">
                    <i class="fa-solid ${product.inStock ? 'fa-circle-check' : 'fa-circle-xmark'} me-1"></i>
                    ${product.inStock ? (product.lowStock ? `Only ${product.stock} left in stock` : 'In stock') : 'Out of stock'}
                </p>

                <div class="d-flex align-items-center gap-3 mb-3">
                    <div class="input-group" style="width:130px">
                        <button class="btn btn-light" id="qtyMinus" type="button">−</button>
                        <input type="text" class="form-control text-center" id="qtyInput" value="1" readonly>
                        <button class="btn btn-light" id="qtyPlus" type="button">+</button>
                    </div>
                    <button class="wishlist-toggle border" id="wishlistBtn" style="position:static" aria-label="Toggle wishlist">
                        <i class="fa-regular fa-heart"></i>
                    </button>
                </div>

                <div class="d-flex gap-2 mb-4">
                    <button class="btn btn-outline-brand flex-fill" id="addToCartBtn" ${!product.inStock ? 'disabled' : ''}>
                        <i class="fa-solid fa-cart-plus me-2"></i>Add to cart
                    </button>
                    <button class="btn btn-brand flex-fill" id="buyNowBtn" ${!product.inStock ? 'disabled' : ''}>
                        Buy now
                    </button>
                </div>

                <div class="card-surface p-3 mb-3">
                    <h2 class="h6 fw-bold mb-2">Description</h2>
                    <p class="small mb-0">${escapeHtml(product.description || 'No description provided.')}</p>
                </div>

                ${product.specifications ? `
                <div class="card-surface p-3">
                    <h2 class="h6 fw-bold mb-2">Specifications</h2>
                    <p class="small mb-0" style="white-space:pre-line">${escapeHtml(product.specifications)}</p>
                </div>` : ''}
            </div>

            <div class="col-12">
                <hr class="my-4">
                <h2 class="h5 fw-bold mb-3">Customer reviews</h2>
                <div id="reviewFormContainer" class="card-surface p-3 mb-4"></div>
                <div id="reviewsList" class="d-flex flex-column gap-3">
                    <div class="skeleton" style="height:4rem"></div>
                </div>
                <div class="text-center mt-3">
                    <button class="btn btn-outline-brand btn-sm d-none" id="loadMoreReviewsBtn">Load more reviews</button>
                </div>
            </div>

            <div class="col-12">
                <hr class="my-4">
                <h2 class="h5 fw-bold mb-3">You may also like</h2>
                <div class="row g-3" id="relatedGrid">
                    <div class="col-6 col-md-3"><div class="skeleton" style="height:260px"></div></div>
                    <div class="col-6 col-md-3"><div class="skeleton" style="height:260px"></div></div>
                    <div class="col-6 col-md-3"><div class="skeleton" style="height:260px"></div></div>
                    <div class="col-6 col-md-3"><div class="skeleton" style="height:260px"></div></div>
                </div>
            </div>
        </div>`;

        wireGallery(images);
        wireBuyButtons();
        renderReviewForm();

        wishlistedPromise.then(ids => {
            const btn = document.getElementById('wishlistBtn');
            const active = ids.has(product.id);
            btn.classList.toggle('active', active);
            btn.querySelector('i').className = active ? 'fa-solid fa-heart' : 'fa-regular fa-heart';
            btn.addEventListener('click', async () => {
                const isActive = btn.classList.contains('active');
                const result = await window.toggleWishlist(product.id, isActive);
                if (result !== null) {
                    btn.classList.toggle('active', result);
                    btn.querySelector('i').className = result ? 'fa-solid fa-heart' : 'fa-regular fa-heart';
                }
            });
        });
    }

    function wireGallery(images) {
        const main = document.getElementById('galleryMain');
        const mainImg = document.getElementById('galleryMainImg');
        main.addEventListener('click', () => main.classList.toggle('zoomed'));

        document.querySelectorAll('.gallery-thumb').forEach(thumb => {
            thumb.addEventListener('click', () => {
                const i = Number(thumb.getAttribute('data-index'));
                selectedImageIndex = i;
                mainImg.src = images[i].imageUrl;
                main.classList.remove('zoomed');
                document.querySelectorAll('.gallery-thumb').forEach(t => t.classList.remove('active'));
                thumb.classList.add('active');
            });
        });
    }

    function wireBuyButtons() {
        const qtyInput = document.getElementById('qtyInput');
        document.getElementById('qtyMinus').addEventListener('click', () => {
            qtyInput.value = Math.max(1, Number(qtyInput.value) - 1);
        });
        document.getElementById('qtyPlus').addEventListener('click', () => {
            qtyInput.value = Math.min(product.stock, Number(qtyInput.value) + 1);
        });

        document.getElementById('addToCartBtn').addEventListener('click', () => {
            window.addToCart(product.id, Number(qtyInput.value));
        });

        document.getElementById('buyNowBtn').addEventListener('click', async () => {
            const ok = await window.addToCart(product.id, Number(qtyInput.value));
            if (ok) window.location.href = '/checkout';
        });
    }

    function renderReviewForm() {
        const container = document.getElementById('reviewFormContainer');
        if (!currentUser) {
            container.innerHTML = `<p class="small text-muted mb-0"><a href="/login?redirect=${encodeURIComponent(window.location.pathname)}" class="text-brand fw-semibold">Log in</a> to write a review.</p>`;
            return;
        }

        container.innerHTML = `
            <h3 class="h6 fw-bold mb-2">Write a review</h3>
            <form id="reviewForm">
                <div class="mb-2">
                    <div class="rating-input" id="ratingInput">
                        ${[1,2,3,4,5].map(i => `<i class="fa-regular fa-star" data-star="${i}" style="cursor:pointer;color:#ffb800"></i>`).join(' ')}
                    </div>
                    <input type="hidden" id="ratingValue" value="0">
                </div>
                <input type="text" class="form-control form-control-sm mb-2" id="reviewTitle" placeholder="Title (optional)">
                <textarea class="form-control form-control-sm mb-2" id="reviewComment" rows="2" placeholder="Share your experience…" required></textarea>
                <button type="submit" class="btn btn-brand btn-sm">Submit review</button>
            </form>`;

        const stars = container.querySelectorAll('[data-star]');
        stars.forEach(star => star.addEventListener('click', () => {
            const val = Number(star.getAttribute('data-star'));
            document.getElementById('ratingValue').value = val;
            stars.forEach(s => s.className = Number(s.getAttribute('data-star')) <= val ? 'fa-solid fa-star' : 'fa-regular fa-star');
        }));

        document.getElementById('reviewForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const rating = Number(document.getElementById('ratingValue').value);
            if (rating < 1) { window.toastError('Please select a star rating'); return; }

            try {
                const res = await fetch(`/api/products/${product.id}/reviews`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        rating,
                        title: document.getElementById('reviewTitle').value,
                        comment: document.getElementById('reviewComment').value
                    })
                });
                if (!res.ok) {
                    const err = await res.json().catch(() => ({}));
                    throw new Error(err.message || 'Could not submit review');
                }
                window.toastSuccess('Review submitted');
                e.target.reset();
                reviewPage = 0;
                loadReviews();
            } catch (err) {
                window.toastError(err.message);
            }
        });
    }

    async function loadReviews() {
        const list = document.getElementById('reviewsList');
        try {
            const res = await fetch(`/api/products/${product.id}/reviews?page=${reviewPage}&size=5`);
            if (!res.ok) throw new Error('Failed to load reviews');
            const data = await res.json();
            const items = data.content || [];

            if (reviewPage === 0 && items.length === 0) {
                list.innerHTML = '<p class="text-muted small">No reviews yet — be the first to share your thoughts.</p>';
                document.getElementById('loadMoreReviewsBtn').classList.add('d-none');
                return;
            }

            const html = items.map(r => renderReviewCard(r)).join('');
            if (reviewPage === 0) list.innerHTML = html; else list.insertAdjacentHTML('beforeend', html);

            wireReviewActions(list);

            const moreBtn = document.getElementById('loadMoreReviewsBtn');
            if (data.number < data.totalPages - 1) {
                moreBtn.classList.remove('d-none');
                moreBtn.onclick = () => { reviewPage++; loadReviews(); };
            } else {
                moreBtn.classList.add('d-none');
            }
        } catch (e) {
            list.innerHTML = '<p class="text-danger small">Could not load reviews.</p>';
        }
    }

    function renderReviewCard(r) {
        const isOwn = currentUser && currentUser.id === r.userId;
        return `
        <div class="card-surface p-3" data-review-id="${r.id}">
            <div class="d-flex justify-content-between">
                <div>
                    <div class="fw-semibold small">${escapeHtml(r.userName)}</div>
                    <div class="rating-stars small">${'★'.repeat(r.rating)}${'☆'.repeat(5 - r.rating)}</div>
                </div>
                <div class="text-muted small">${window.formatDate(r.createdAt)}${r.edited ? ' · edited' : ''}</div>
            </div>
            ${r.title ? `<div class="fw-semibold small mt-2">${escapeHtml(r.title)}</div>` : ''}
            <p class="small mb-2 mt-1">${escapeHtml(r.comment)}</p>
            <div class="d-flex align-items-center gap-3">
                <button class="btn btn-sm btn-light like-btn ${r.likedByCurrentUser ? 'text-brand' : ''}" data-like="${r.id}">
                    <i class="fa-${r.likedByCurrentUser ? 'solid' : 'regular'} fa-thumbs-up me-1"></i>${r.likeCount}
                </button>
                ${isOwn ? `<button class="btn btn-sm btn-light text-danger" data-delete-review="${r.id}">Delete</button>` : ''}
            </div>
        </div>`;
    }

    function wireReviewActions(container) {
        container.querySelectorAll('[data-like]').forEach(btn => {
            btn.addEventListener('click', async () => {
                if (!currentUser) { window.requireLoginThen(window.location.pathname); return; }
                const id = btn.getAttribute('data-like');
                try {
                    const res = await fetch(`/api/reviews/${id}/like`, { method: 'POST' });
                    if (!res.ok) throw new Error('Could not update like');
                    const updated = await res.json();
                    btn.innerHTML = `<i class="fa-${updated.likedByCurrentUser ? 'solid' : 'regular'} fa-thumbs-up me-1"></i>${updated.likeCount}`;
                    btn.classList.toggle('text-brand', updated.likedByCurrentUser);
                } catch (e) {
                    window.toastError(e.message);
                }
            });
        });

        container.querySelectorAll('[data-delete-review]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const result = await Swal.fire({
                    icon: 'warning', title: 'Delete your review?', showCancelButton: true,
                    confirmButtonText: 'Delete', confirmButtonColor: '#e53935'
                });
                if (!result.isConfirmed) return;
                const id = btn.getAttribute('data-delete-review');
                try {
                    const res = await fetch(`/api/reviews/${id}`, { method: 'DELETE' });
                    if (!res.ok) throw new Error('Could not delete review');
                    window.toastSuccess('Review deleted');
                    reviewPage = 0;
                    loadReviews();
                } catch (e) {
                    window.toastError(e.message);
                }
            });
        });
    }

    async function loadRelated() {
        const grid = document.getElementById('relatedGrid');
        try {
            const res = await fetch(`/api/products?categoryId=${product.categoryId}&size=5`);
            if (!res.ok) throw new Error('Failed to load related products');
            const data = await res.json();
            const wishlisted = await window.getWishlistedIds();
            const items = (data.content || []).filter(p => p.id !== product.id).slice(0, 4);

            if (items.length === 0) {
                grid.innerHTML = '<p class="text-muted small">No related products found.</p>';
                return;
            }
            grid.innerHTML = items.map(p => window.renderProductCard(p, wishlisted.has(p.id))).join('');
            window.wireProductGridEvents(grid);
        } catch (e) {
            grid.innerHTML = '<p class="text-muted small">Could not load related products.</p>';
        }
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }
})();
