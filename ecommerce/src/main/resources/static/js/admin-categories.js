(function () {
    'use strict';

    let categoryModal;
    let allCategories = [];

    document.addEventListener('DOMContentLoaded', async function () {
        categoryModal = new bootstrap.Modal(document.getElementById('categoryModal'));
        await loadCategories();

        document.getElementById('addCategoryBtn').addEventListener('click', () => openModal(null));
        document.getElementById('categoryForm').addEventListener('submit', handleSubmit);
    });

    async function loadCategories() {
        const body = document.getElementById('categoriesBody');
        try {
            const res = await fetch('/api/admin/categories');
            if (!res.ok) throw new Error('Failed to load categories');
            allCategories = await res.json();
            document.getElementById('resultCount').textContent = `${allCategories.length} categor${allCategories.length === 1 ? 'y' : 'ies'}`;
            renderTree();
            populateParentSelect();
        } catch (e) {
            console.error(e);
            body.innerHTML = '<tr><td colspan="6" class="text-danger text-center py-3">Could not load categories</td></tr>';
        }
    }

    function renderTree() {
        const body = document.getElementById('categoriesBody');
        if (allCategories.length === 0) {
            body.innerHTML = '<tr><td colspan="6" class="text-muted text-center py-4">No categories yet — add your first one above.</td></tr>';
            return;
        }

        const byParent = {};
        allCategories.forEach(c => {
            const key = c.parentId || 'root';
            (byParent[key] = byParent[key] || []).push(c);
        });

        const rows = [];
        function walk(parentKey, depth) {
            (byParent[parentKey] || []).forEach(c => {
                rows.push(renderRow(c, depth));
                walk(c.id, depth + 1);
            });
        }
        walk('root', 0);
        body.innerHTML = rows.join('');

        body.querySelectorAll('[data-edit]').forEach(btn =>
            btn.addEventListener('click', () => openModal(btn.getAttribute('data-edit'))));
        body.querySelectorAll('[data-delete]').forEach(btn =>
            btn.addEventListener('click', () => confirmDelete(btn.getAttribute('data-delete'), btn.getAttribute('data-name'))));
    }

    function renderRow(c, depth) {
        const indent = '&nbsp;&nbsp;&nbsp;&nbsp;'.repeat(depth) + (depth > 0 ? '↳ ' : '');
        const statusBadge = c.active
            ? '<span class="status-pill status-pill--delivered">Visible</span>'
            : '<span class="status-pill status-pill--return_requested">Hidden</span>';

        return `
        <tr>
            <td>${c.imageUrl ? `<img src="${c.imageUrl}" alt="" style="width:36px;height:36px;object-fit:cover;border-radius:8px">` : ''}</td>
            <td>${indent}${escapeHtml(c.name)}</td>
            <td class="font-mono small text-muted">${escapeHtml(c.slug)}</td>
            <td>${c.parentName ? escapeHtml(c.parentName) : '<span class="text-muted">—</span>'}</td>
            <td>${statusBadge}</td>
            <td class="text-end">
                <button class="btn btn-sm btn-light" data-edit="${c.id}"><i class="fa-solid fa-pen"></i></button>
                <button class="btn btn-sm btn-light text-danger" data-delete="${c.id}" data-name="${escapeHtml(c.name)}"><i class="fa-solid fa-trash"></i></button>
            </td>
        </tr>`;
    }

    function populateParentSelect(excludeId) {
        const select = document.getElementById('cParent');
        const options = allCategories.filter(c => String(c.id) !== String(excludeId));
        select.innerHTML = '<option value="">— Top-level category —</option>' +
            options.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
    }

    async function openModal(id) {
        document.getElementById('categoryForm').reset();
        document.getElementById('categoryId').value = id || '';
        document.getElementById('categoryModalTitle').textContent = id ? 'Edit category' : 'Add category';
        populateParentSelect(id);

        if (id) {
            const res = await fetch(`/api/admin/categories/${id}`);
            if (!res.ok) { window.toastError('Could not load this category'); return; }
            const c = await res.json();
            document.getElementById('cName').value = c.name;
            document.getElementById('cParent').value = c.parentId || '';
            document.getElementById('cImage').value = c.imageUrl || '';
            document.getElementById('cDescription').value = c.description || '';
            document.getElementById('cActive').checked = c.active;
        }

        categoryModal.show();
    }

    async function handleSubmit(e) {
        e.preventDefault();
        const id = document.getElementById('categoryId').value;

        const payload = {
            name: document.getElementById('cName').value,
            parentId: document.getElementById('cParent').value ? Number(document.getElementById('cParent').value) : null,
            imageUrl: document.getElementById('cImage').value,
            description: document.getElementById('cDescription').value,
            active: document.getElementById('cActive').checked
        };

        try {
            const res = await fetch(id ? `/api/admin/categories/${id}` : '/api/admin/categories', {
                method: id ? 'PUT' : 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not save category');
            }
            categoryModal.hide();
            window.toastSuccess(id ? 'Category updated' : 'Category created');
            await loadCategories();
        } catch (err) {
            window.toastError(err.message);
        }
    }

    async function confirmDelete(id, name) {
        const result = await Swal.fire({
            icon: 'warning',
            title: `Delete "${name}"?`,
            text: 'Categories with sub-categories or assigned products cannot be deleted.',
            showCancelButton: true,
            confirmButtonText: 'Delete',
            confirmButtonColor: '#f43f5e'
        });
        if (!result.isConfirmed) return;

        try {
            const res = await fetch(`/api/admin/categories/${id}`, { method: 'DELETE' });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not delete category');
            }
            window.toastSuccess('Category deleted');
            await loadCategories();
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
