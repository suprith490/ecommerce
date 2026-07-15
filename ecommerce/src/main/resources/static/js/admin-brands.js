(function () {
    'use strict';

    let brandModal;

    document.addEventListener('DOMContentLoaded', async function () {
        brandModal = new bootstrap.Modal(document.getElementById('brandModal'));
        await loadBrands();

        document.getElementById('addBrandBtn').addEventListener('click', () => openModal(null));
        document.getElementById('brandForm').addEventListener('submit', handleSubmit);
    });

    async function loadBrands() {
        const body = document.getElementById('brandsBody');
        try {
            const res = await fetch('/api/admin/brands');
            if (!res.ok) throw new Error('Failed to load brands');
            const brands = await res.json();
            document.getElementById('resultCount').textContent = `${brands.length} brand(s)`;

            if (brands.length === 0) {
                body.innerHTML = '<tr><td colspan="5" class="text-muted text-center py-4">No brands yet — add your first one above.</td></tr>';
                return;
            }

            body.innerHTML = brands.map(b => {
                const statusBadge = b.active
                    ? '<span class="status-pill status-pill--delivered">Visible</span>'
                    : '<span class="status-pill status-pill--return_requested">Hidden</span>';
                return `
                <tr>
                    <td>${b.logoUrl ? `<img src="${b.logoUrl}" alt="" style="width:36px;height:36px;object-fit:cover;border-radius:8px">` : ''}</td>
                    <td class="fw-semibold">${escapeHtml(b.name)}</td>
                    <td class="font-mono small text-muted">${escapeHtml(b.slug)}</td>
                    <td>${statusBadge}</td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-light" data-edit="${b.id}"><i class="fa-solid fa-pen"></i></button>
                        <button class="btn btn-sm btn-light text-danger" data-delete="${b.id}" data-name="${escapeHtml(b.name)}"><i class="fa-solid fa-trash"></i></button>
                    </td>
                </tr>`;
            }).join('');

            body.querySelectorAll('[data-edit]').forEach(btn =>
                btn.addEventListener('click', () => openModal(btn.getAttribute('data-edit'))));
            body.querySelectorAll('[data-delete]').forEach(btn =>
                btn.addEventListener('click', () => confirmDelete(btn.getAttribute('data-delete'), btn.getAttribute('data-name'))));
        } catch (e) {
            console.error(e);
            body.innerHTML = '<tr><td colspan="5" class="text-danger text-center py-3">Could not load brands</td></tr>';
        }
    }

    async function openModal(id) {
        document.getElementById('brandForm').reset();
        document.getElementById('brandId').value = id || '';
        document.getElementById('brandModalTitle').textContent = id ? 'Edit brand' : 'Add brand';

        if (id) {
            const res = await fetch(`/api/admin/brands/${id}`);
            if (!res.ok) { window.toastError('Could not load this brand'); return; }
            const b = await res.json();
            document.getElementById('bName').value = b.name;
            document.getElementById('bLogo').value = b.logoUrl || '';
            document.getElementById('bDescription').value = b.description || '';
            document.getElementById('bActive').checked = b.active;
        }

        brandModal.show();
    }

    async function handleSubmit(e) {
        e.preventDefault();
        const id = document.getElementById('brandId').value;

        const payload = {
            name: document.getElementById('bName').value,
            logoUrl: document.getElementById('bLogo').value,
            description: document.getElementById('bDescription').value,
            active: document.getElementById('bActive').checked
        };

        try {
            const res = await fetch(id ? `/api/admin/brands/${id}` : '/api/admin/brands', {
                method: id ? 'PUT' : 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not save brand');
            }
            brandModal.hide();
            window.toastSuccess(id ? 'Brand updated' : 'Brand created');
            await loadBrands();
        } catch (err) {
            window.toastError(err.message);
        }
    }

    async function confirmDelete(id, name) {
        const result = await Swal.fire({
            icon: 'warning',
            title: `Delete "${name}"?`,
            text: 'Brands with assigned products cannot be deleted.',
            showCancelButton: true,
            confirmButtonText: 'Delete',
            confirmButtonColor: '#f43f5e'
        });
        if (!result.isConfirmed) return;

        try {
            const res = await fetch(`/api/admin/brands/${id}`, { method: 'DELETE' });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not delete brand');
            }
            window.toastSuccess('Brand deleted');
            await loadBrands();
        } catch (err) {
            window.toastError(err.message);
        }
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }
})();
