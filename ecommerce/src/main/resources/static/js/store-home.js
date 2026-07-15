(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', async function () {
        const wishlisted = await window.getWishlistedIds();

        loadCategories();
        loadProducts('newArrivalsGrid', '/api/products?size=8&sortBy=createdAt&direction=desc', wishlisted);
        loadProducts('trendingGrid', '/api/products?size=8&sortBy=averageRating&direction=desc', wishlisted);
        loadBrands();

        document.getElementById('newsletterForm').addEventListener('submit', (e) => {
            e.preventDefault();
            e.target.reset();
            window.toastSuccess('Subscribed! Watch your inbox for deals.');
        });
    });

    async function loadCategories() {
        const grid = document.getElementById('categoryGrid');
        try {
            const res = await fetch('/api/categories');
            if (!res.ok) throw new Error('Failed to load categories');
            const categories = (await res.json()).slice(0, 8);
            if (categories.length === 0) {
                grid.innerHTML = '<p class="text-muted small">No categories yet.</p>';
                return;
            }
            grid.innerHTML = categories.map(c => `
                <div class="col-6 col-md-3">
                    <a href="/products?categoryId=${c.id}" class="card-surface d-block p-3 text-center text-decoration-none text-reset h-100">
                        ${c.imageUrl ? `<img src="${c.imageUrl}" alt="" style="width:56px;height:56px;object-fit:cover;border-radius:12px" class="mb-2">` : '<i class="fa-solid fa-shapes fa-2x text-brand mb-2"></i>'}
                        <div class="small fw-semibold">${escapeHtml(c.name)}</div>
                    </a>
                </div>`).join('');
        } catch (e) {
            grid.innerHTML = '<p class="text-muted small">Could not load categories.</p>';
        }
    }

    async function loadProducts(gridId, url, wishlisted) {
        const grid = document.getElementById(gridId);
        try {
            const res = await fetch(url);
            if (!res.ok) throw new Error('Failed to load products');
            const data = await res.json();
            const items = data.content || [];
            if (items.length === 0) {
                grid.innerHTML = '<p class="text-muted small">Nothing here yet.</p>';
                return;
            }
            grid.innerHTML = items.map(p => window.renderProductCard(p, wishlisted.has(p.id))).join('');
            window.wireProductGridEvents(grid);
        } catch (e) {
            grid.innerHTML = '<p class="text-muted small">Could not load products.</p>';
        }
    }

    async function loadBrands() {
        const row = document.getElementById('brandsRow');
        try {
            const res = await fetch('/api/brands');
            if (!res.ok) throw new Error('Failed to load brands');
            const brands = await res.json();
            if (brands.length === 0) {
                row.innerHTML = '<p class="text-muted small">No brands yet.</p>';
                return;
            }
            row.innerHTML = brands.map(b => `
                <a href="/products?brandId=${b.id}" class="card-surface d-flex align-items-center justify-content-center px-4 text-decoration-none text-reset" style="height:56px">
                    ${b.logoUrl ? `<img src="${b.logoUrl}" alt="${escapeHtml(b.name)}" style="max-height:32px;max-width:100px;object-fit:contain">` : `<span class="fw-semibold small">${escapeHtml(b.name)}</span>`}
                </a>`).join('');
        } catch (e) {
            row.innerHTML = '<p class="text-muted small">Could not load brands.</p>';
        }
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }
})();
