(function () {
    'use strict';

    let currentPage = 0;
    let totalPages = 1;
    const pageSize = 10;
    let searchTerm = '';
    let searchDebounce;

    document.addEventListener('DOMContentLoaded', function () {
        document.getElementById('prevPageBtn').addEventListener('click', () => changePage(-1));
        document.getElementById('nextPageBtn').addEventListener('click', () => changePage(1));
        document.getElementById('searchInput').addEventListener('input', (e) => {
            clearTimeout(searchDebounce);
            searchDebounce = setTimeout(() => {
                searchTerm = e.target.value.trim();
                currentPage = 0;
                loadUsers();
            }, 350);
        });
        loadUsers();
    });

    function changePage(delta) {
        const next = currentPage + delta;
        if (next < 0 || next >= totalPages) return;
        currentPage = next;
        loadUsers();
    }

    async function loadUsers() {
        const body = document.getElementById('usersBody');
        body.innerHTML = '<tr><td colspan="6"><div class="skeleton" style="height:1.2rem"></div></td></tr>';

        try {
            const params = new URLSearchParams({ page: currentPage, size: pageSize });
            if (searchTerm) params.set('search', searchTerm);

            const res = await fetch(`/api/admin/users?${params.toString()}`);
            if (!res.ok) throw new Error('Failed to load users');
            const pageData = await res.json();
            totalPages = pageData.totalPages || 1;

            document.getElementById('resultCount').textContent = `${pageData.totalElements} user(s)`;
            document.getElementById('pageLabel').textContent = `Page ${currentPage + 1} of ${Math.max(totalPages, 1)}`;
            document.getElementById('prevPageBtn').disabled = currentPage === 0;
            document.getElementById('nextPageBtn').disabled = currentPage >= totalPages - 1;

            renderRows(pageData.content || []);
        } catch (e) {
            console.error(e);
            body.innerHTML = '<tr><td colspan="6" class="text-danger text-center py-3">Could not load users</td></tr>';
        }
    }

    function renderRows(users) {
        const body = document.getElementById('usersBody');
        if (users.length === 0) {
            body.innerHTML = '<tr><td colspan="6" class="text-muted text-center py-4">No users found.</td></tr>';
            return;
        }

        body.innerHTML = users.map(u => {
            const statusBadge = u.enabled
                ? '<span class="status-pill status-pill--delivered">Active</span>'
                : '<span class="status-pill status-pill--cancelled">Disabled</span>';
            const toggleBtn = u.enabled
                ? `<button class="btn btn-sm btn-light" data-disable="${u.id}" data-name="${escapeHtml(u.name)}"><i class="fa-solid fa-user-slash"></i></button>`
                : `<button class="btn btn-sm btn-light" data-enable="${u.id}"><i class="fa-solid fa-user-check"></i></button>`;

            return `
            <tr>
                <td class="fw-semibold">${escapeHtml(u.name)}</td>
                <td class="text-muted">${escapeHtml(u.email)}</td>
                <td><span class="status-pill status-pill--confirmed">${u.role}</span></td>
                <td>${statusBadge}</td>
                <td>${window.formatDate(u.createdAt)}</td>
                <td class="text-end">
                    ${toggleBtn}
                    <button class="btn btn-sm btn-light text-danger" data-delete="${u.id}" data-name="${escapeHtml(u.name)}"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>`;
        }).join('');

        body.querySelectorAll('[data-disable]').forEach(btn =>
            btn.addEventListener('click', () => disableUser(btn.getAttribute('data-disable'), btn.getAttribute('data-name'))));
        body.querySelectorAll('[data-enable]').forEach(btn =>
            btn.addEventListener('click', () => enableUser(btn.getAttribute('data-enable'))));
        body.querySelectorAll('[data-delete]').forEach(btn =>
            btn.addEventListener('click', () => confirmDelete(btn.getAttribute('data-delete'), btn.getAttribute('data-name'))));
    }

    async function disableUser(id, name) {
        const result = await Swal.fire({
            icon: 'warning',
            title: `Disable ${name}?`,
            text: 'They will be logged out immediately and unable to sign back in until re-enabled.',
            showCancelButton: true,
            confirmButtonText: 'Disable',
            confirmButtonColor: '#f59e0b'
        });
        if (!result.isConfirmed) return;

        try {
            const res = await fetch(`/api/admin/users/${id}/disable`, { method: 'PATCH' });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not disable user');
            }
            window.toastSuccess('User disabled');
            await loadUsers();
        } catch (err) {
            window.toastError(err.message);
        }
    }

    async function enableUser(id) {
        try {
            const res = await fetch(`/api/admin/users/${id}/enable`, { method: 'PATCH' });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not enable user');
            }
            window.toastSuccess('User enabled');
            await loadUsers();
        } catch (err) {
            window.toastError(err.message);
        }
    }

    async function confirmDelete(id, name) {
        const result = await Swal.fire({
            icon: 'warning',
            title: `Delete ${name}?`,
            text: 'This cannot be undone. Users with existing orders cannot be deleted — disable them instead.',
            showCancelButton: true,
            confirmButtonText: 'Delete',
            confirmButtonColor: '#f43f5e'
        });
        if (!result.isConfirmed) return;

        try {
            const res = await fetch(`/api/admin/users/${id}`, { method: 'DELETE' });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not delete user');
            }
            window.toastSuccess('User deleted');
            await loadUsers();
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
