(function () {
    'use strict';

    let currentPage = 0;
    let totalPages = 1;
    const pageSize = 12;
    let categoryId = null;
    let brandId = null;
    let searchTerm = null;
    let sortBy = 'createdAt';
    let direction = 'desc';
    let wishlisted = new Set();

    document.addEventListener('DOMContentLoaded', async function () {
        const params = new URLSearchParams(window.location.search);
        categoryId = params.get('categoryId');
        brandId = params.get('brandId');
        searchTerm = params.get('search');
        if (params.get('sortBy')) sortBy = params.get('sortBy');
        if (params.get('direction')) direction = params.get('direction');

        document.getElementById('sortSelect').value = `${sortBy}:${direction}`;

        wishlisted = await window.getWishlistedIds();

        await Promise.all([loadCategoryFilters(), loadBrandFilters()]);
        await loadProducts();

        document.getElementById('sortSelect').addEventListener('change', (e) => {
            const [sb, dir] = e.target.value.split(':');
            sortBy = sb; direction = dir; currentPage = 0;
            loadProducts();
        });

        document.getElementById('prevPageBtn').addEventListener('click', () => changePage(-1));
        document.getElementById('nextPageBtn').addEventListener('click', () => changePage(1));
        document.getElementById('clearFiltersBtn').addEventListener('click', () => {
            categoryId = null; brandId = null; searchTerm = null; currentPage = 0;
            window.history.replaceState({}, '', '/products');
            loadCategoryFilters(); loadBrandFilters(); loadProducts();
        });
    });

    function changePage(delta) {
        const next = currentPage + delta;
        if (next < 0 || next >= totalPages) return;
        currentPage = next;
        loadProducts();
    }

    async function loadCategoryFilters() {
        const box = document.getElementById('categoryFilters');
        try {
            const res = await fetch('/api/categories');
            if (!res.ok) throw new Error('Failed to load categories');
            const categories = await res.json();
            box.innerHTML = categories.map(c => `
                <div class="form-check">
                    <input class="form-check-input" type="radio" name="categoryFilter" id="cat-${c.id}" value="${c.id}" ${String(categoryId) === String(c.id) ? 'checked' : ''}>
                    <label class="form-check-label" for="cat-${c.id}">${escapeHtml(c.name)}</label>
                </div>`).join('');
            box.querySelectorAll('input').forEach(input => input.addEventListener('change', () => {
                categoryId = input.value; currentPage = 0; loadProducts();
            }));
        } catch (e) {
            box.innerHTML = '<span class="text-muted">Could not load categories</span>';
        }
    }

    async function loadBrandFilters() {
        const box = document.getElementById('brandFilters');
        try {
            const res = await fetch('/api/brands');
            if (!res.ok) throw new Error('Failed to load brands');
            const brands = await res.json();
            box.innerHTML = brands.map(b => `
                <div class="form-check">
                    <input class="form-check-input" type="radio" name="brandFilter" id="brand-${b.id}" value="${b.id}" ${String(brandId) === String(b.id) ? 'checked' : ''}>
                    <label class="form-check-label" for="brand-${b.id}">${escapeHtml(b.name)}</label>
                </div>`).join('');
            box.querySelectorAll('input').forEach(input => input.addEventListener('change', () => {
                brandId = input.value; currentPage = 0; loadProducts();
            }));
        } catch (e) {
            box.innerHTML = '<span class="text-muted">Could not load brands</span>';
        }
    }

    async function loadProducts() {
        const grid = document.getElementById('productsGrid');
        grid.innerHTML = '<div class="col-6 col-md-4"><div class="skeleton" style="height:280px"></div></div>'.repeat(3);

        try {
            const params = new URLSearchParams({ page: currentPage, size: pageSize, sortBy, direction });
            if (categoryId) params.set('categoryId', categoryId);
            if (brandId) params.set('brandId', brandId);
            if (searchTerm) params.set('search', searchTerm);

            const res = await fetch(`/api/products?${params.toString()}`);
            if (!res.ok) throw new Error('Failed to load products');
            const data = await res.json();
            totalPages = data.totalPages || 1;

            document.getElementById('resultCount').textContent =
                searchTerm ? `${data.totalElements} result(s) for "${searchTerm}"` : `${data.totalElements} product(s)`;
            document.getElementById('pageLabel').textContent = `Page ${currentPage + 1} of ${Math.max(totalPages, 1)}`;
            document.getElementById('prevPageBtn').disabled = currentPage === 0;
            document.getElementById('nextPageBtn').disabled = currentPage >= totalPages - 1;

            const items = data.content || [];
            if (items.length === 0) {
                grid.innerHTML = '<div class="col-12 text-center text-muted py-5"><i class="fa-solid fa-box-open fa-2x mb-3 d-block"></i>No products match your filters.</div>';
                return;
            }
            grid.innerHTML = items.map(p => window.renderProductCard(p, wishlisted.has(p.id)).replace('col-6 col-md-3', 'col-6 col-md-4')).join('');
            window.wireProductGridEvents(grid);
        } catch (e) {
            console.error(e);
            grid.innerHTML = '<div class="col-12 text-center text-danger py-5">Could not load products.</div>';
        }
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }
})();
