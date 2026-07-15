(function () {
    'use strict';

    let currentPage = 0;
    const pageSize = 10;
    let totalPages = 1;
    let categories = [];
    let brands = [];
    let productModal;

    document.addEventListener('DOMContentLoaded', async function () {
        productModal = new bootstrap.Modal(document.getElementById('productModal'));

        await Promise.all([loadCategories(), loadBrands()]);
        await loadProducts();

        document.getElementById('addProductBtn').addEventListener('click', () => openModal(null));
        document.getElementById('addImageRowBtn').addEventListener('click', () => addImageRow());
        document.getElementById('productForm').addEventListener('submit', handleSubmit);
        document.getElementById('prevPageBtn').addEventListener('click', () => changePage(-1));
        document.getElementById('nextPageBtn').addEventListener('click', () => changePage(1));
    });

    async function loadCategories() {
        const res = await fetch('/api/admin/categories');
        categories = res.ok ? await res.json() : [];
        const select = document.getElementById('pCategory');
        select.innerHTML = categories.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
    }

    async function loadBrands() {
        const res = await fetch('/api/admin/brands');
        brands = res.ok ? await res.json() : [];
        const select = document.getElementById('pBrand');
        select.innerHTML = '<option value="">— No brand —</option>' +
            brands.map(b => `<option value="${b.id}">${escapeHtml(b.name)}</option>`).join('');
    }

    async function loadProducts() {
        const body = document.getElementById('productsBody');
        body.innerHTML = '<tr><td colspan="8"><div class="skeleton" style="height:1.2rem"></div></td></tr>';

        try {
            const res = await fetch(`/api/admin/products?page=${currentPage}&size=${pageSize}`);
            if (!res.ok) throw new Error('Failed to load products');
            const pageData = await res.json();
            totalPages = pageData.totalPages || 1;

            document.getElementById('resultCount').textContent = `${pageData.totalElements} product(s)`;
            document.getElementById('pageLabel').textContent = `Page ${currentPage + 1} of ${Math.max(totalPages, 1)}`;
            document.getElementById('prevPageBtn').disabled = currentPage === 0;
            document.getElementById('nextPageBtn').disabled = currentPage >= totalPages - 1;

            renderRows(pageData.content || []);
        } catch (e) {
            console.error(e);
            body.innerHTML = '<tr><td colspan="8" class="text-danger text-center py-3">Could not load products</td></tr>';
        }
    }

    function renderRows(products) {
        const body = document.getElementById('productsBody');
        if (products.length === 0) {
            body.innerHTML = '<tr><td colspan="8" class="text-muted text-center py-4">No products yet — add your first one above.</td></tr>';
            return;
        }

        body.innerHTML = products.map(p => {
            const thumb = (p.images && p.images[0]) ? p.images[0].imageUrl : null;
            const priceHtml = p.offerPrice
                ? `<span class="text-decoration-line-through text-muted small">${window.formatCurrency(p.price)}</span> <span class="fw-semibold">${window.formatCurrency(p.effectivePrice)}</span>`
                : `<span class="fw-semibold">${window.formatCurrency(p.price)}</span>`;
            const stockBadge = p.stock === 0
                ? '<span class="status-pill status-pill--cancelled">Out of stock</span>'
                : (p.lowStock ? '<span class="status-pill status-pill--shipped">Low: ' + p.stock + '</span>' : `<span class="status-pill status-pill--delivered">${p.stock} in stock</span>`);
            const statusBadge = p.active
                ? '<span class="status-pill status-pill--delivered">Visible</span>'
                : '<span class="status-pill status-pill--return_requested">Hidden</span>';

            return `
            <tr>
                <td>${thumb ? `<img src="${thumb}" alt="" style="width:44px;height:44px;object-fit:cover;border-radius:8px">` : '<div style="width:44px;height:44px;border-radius:8px;background:#e2e8f0"></div>'}</td>
                <td class="fw-semibold">${escapeHtml(p.name)}</td>
                <td class="font-mono small">${escapeHtml(p.sku)}</td>
                <td>${escapeHtml(p.categoryName || '—')}</td>
                <td>${priceHtml}</td>
                <td>${stockBadge}</td>
                <td>${statusBadge}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-light" data-edit="${p.id}"><i class="fa-solid fa-pen"></i></button>
                    <button class="btn btn-sm btn-light text-danger" data-delete="${p.id}" data-name="${escapeHtml(p.name)}"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>`;
        }).join('');

        body.querySelectorAll('[data-edit]').forEach(btn =>
            btn.addEventListener('click', () => openModal(btn.getAttribute('data-edit'))));
        body.querySelectorAll('[data-delete]').forEach(btn =>
            btn.addEventListener('click', () => confirmDelete(btn.getAttribute('data-delete'), btn.getAttribute('data-name'))));
    }

    function changePage(delta) {
        const next = currentPage + delta;
        if (next < 0 || next >= totalPages) return;
        currentPage = next;
        loadProducts();
    }

    async function openModal(id) {
        document.getElementById('productForm').reset();
        document.getElementById('imageRows').innerHTML = '';
        document.getElementById('productId').value = id || '';
        document.getElementById('productModalTitle').textContent = id ? 'Edit product' : 'Add product';

        if (id) {
            const res = await fetch(`/api/admin/products/${id}`);
            if (!res.ok) { window.toastError('Could not load this product'); return; }
            const p = await res.json();

            document.getElementById('pName').value = p.name;
            document.getElementById('pSku').value = p.sku;
            document.getElementById('pCategory').value = p.categoryId;
            document.getElementById('pBrand').value = p.brandId || '';
            document.getElementById('pPrice').value = p.price;
            document.getElementById('pOfferPrice').value = p.offerPrice || '';
            document.getElementById('pStock').value = p.stock;
            document.getElementById('pDescription').value = p.description || '';
            document.getElementById('pSpecifications').value = p.specifications || '';
            document.getElementById('pActive').checked = p.active;

            (p.images || []).forEach(img => addImageRow(img.imageUrl, img.altText, img.primary));
        } else {
            addImageRow();
        }

        productModal.show();
    }

    function addImageRow(url = '', alt = '', primary = false) {
        const container = document.getElementById('imageRows');
        const row = document.createElement('div');
        row.className = 'd-flex gap-2 align-items-center image-row';
        row.innerHTML = `
            <input type="url" class="form-control form-control-sm img-url" placeholder="https://…" value="${escapeAttr(url)}" required>
            <input type="text" class="form-control form-control-sm img-alt" placeholder="Alt text" value="${escapeAttr(alt)}" style="max-width:160px">
            <div class="form-check form-check-inline m-0" title="Primary image">
                <input class="form-check-input img-primary" type="radio" name="primaryImage" ${primary ? 'checked' : ''}>
            </div>
            <button type="button" class="btn btn-sm btn-light text-danger remove-row"><i class="fa-solid fa-xmark"></i></button>
        `;
        row.querySelector('.remove-row').addEventListener('click', () => row.remove());
        container.appendChild(row);
    }

    async function handleSubmit(e) {
        e.preventDefault();
        const id = document.getElementById('productId').value;

        const images = Array.from(document.querySelectorAll('#imageRows .image-row')).map((row, index) => ({
            imageUrl: row.querySelector('.img-url').value,
            altText: row.querySelector('.img-alt').value,
            primary: row.querySelector('.img-primary').checked,
            sortOrder: index
        })).filter(img => img.imageUrl);

        const payload = {
            name: document.getElementById('pName').value,
            sku: document.getElementById('pSku').value,
            categoryId: Number(document.getElementById('pCategory').value),
            brandId: document.getElementById('pBrand').value ? Number(document.getElementById('pBrand').value) : null,
            price: Number(document.getElementById('pPrice').value),
            offerPrice: document.getElementById('pOfferPrice').value ? Number(document.getElementById('pOfferPrice').value) : null,
            stock: Number(document.getElementById('pStock').value),
            description: document.getElementById('pDescription').value,
            specifications: document.getElementById('pSpecifications').value,
            active: document.getElementById('pActive').checked,
            images
        };

        try {
            const res = await fetch(id ? `/api/admin/products/${id}` : '/api/admin/products', {
                method: id ? 'PUT' : 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not save product');
            }
            productModal.hide();
            window.toastSuccess(id ? 'Product updated' : 'Product created');
            await loadProducts();
        } catch (err) {
            window.toastError(err.message);
        }
    }

    async function confirmDelete(id, name) {
        const result = await Swal.fire({
            icon: 'warning',
            title: `Delete "${name}"?`,
            text: 'This cannot be undone.',
            showCancelButton: true,
            confirmButtonText: 'Delete',
            confirmButtonColor: '#f43f5e'
        });
        if (!result.isConfirmed) return;

        try {
            const res = await fetch(`/api/admin/products/${id}`, { method: 'DELETE' });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not delete product');
            }
            window.toastSuccess('Product deleted');
            await loadProducts();
        } catch (err) {
            window.toastError(err.message);
        }
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }

    function escapeAttr(str) {
        return escapeHtml(str).replace(/"/g, '&quot;');
    }
})();
