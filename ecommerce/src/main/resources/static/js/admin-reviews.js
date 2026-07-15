(function () {
    'use strict';

    let currentPage = 0;
    let totalPages = 1;
    const pageSize = 15;

    document.addEventListener('DOMContentLoaded', function () {
        document.getElementById('prevPageBtn').addEventListener('click', () => changePage(-1));
        document.getElementById('nextPageBtn').addEventListener('click', () => changePage(1));
        loadReviews();
    });

    function changePage(delta) {
        const next = currentPage + delta;
        if (next < 0 || next >= totalPages) return;
        currentPage = next;
        loadReviews();
    }

    async function loadReviews() {
        const body = document.getElementById('reviewsBody');
        body.innerHTML = '<tr><td colspan="7"><div class="skeleton" style="height:1.2rem"></div></td></tr>';

        try {
            const res = await fetch(`/api/admin/reviews?page=${currentPage}&size=${pageSize}`);
            if (!res.ok) throw new Error('Failed to load reviews');
            const pageData = await res.json();
            totalPages = pageData.totalPages || 1;

            document.getElementById('resultCount').textContent = `${pageData.totalElements} review(s)`;
            document.getElementById('pageLabel').textContent = `Page ${currentPage + 1} of ${Math.max(totalPages, 1)}`;
            document.getElementById('prevPageBtn').disabled = currentPage === 0;
            document.getElementById('nextPageBtn').disabled = currentPage >= totalPages - 1;

            renderRows(pageData.content || []);
        } catch (e) {
            console.error(e);
            body.innerHTML = '<tr><td colspan="7" class="text-danger text-center py-3">Could not load reviews</td></tr>';
        }
    }

    function renderRows(reviews) {
        const body = document.getElementById('reviewsBody');
        if (reviews.length === 0) {
            body.innerHTML = '<tr><td colspan="7" class="text-muted text-center py-4">No reviews yet.</td></tr>';
            return;
        }

        body.innerHTML = reviews.map(r => `
            <tr>
                <td class="fw-semibold">${escapeHtml(r.productName)}</td>
                <td>${escapeHtml(r.userName)}</td>
                <td>${'★'.repeat(r.rating)}${'☆'.repeat(5 - r.rating)}</td>
                <td class="small" style="max-width:280px">
                    ${r.title ? `<div class="fw-semibold">${escapeHtml(r.title)}</div>` : ''}
                    <div class="text-muted text-truncate" style="max-width:280px">${escapeHtml(r.comment)}</div>
                    ${r.edited ? '<span class="text-muted small fst-italic">(edited)</span>' : ''}
                </td>
                <td>${r.likeCount}</td>
                <td>${window.formatDate(r.createdAt)}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-light text-danger" data-delete="${r.id}"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>`).join('');

        body.querySelectorAll('[data-delete]').forEach(btn =>
            btn.addEventListener('click', () => confirmDelete(btn.getAttribute('data-delete'))));
    }

    async function confirmDelete(id) {
        const result = await Swal.fire({
            icon: 'warning',
            title: 'Remove this review?',
            text: 'This cannot be undone.',
            showCancelButton: true,
            confirmButtonText: 'Remove',
            confirmButtonColor: '#f43f5e'
        });
        if (!result.isConfirmed) return;

        try {
            const res = await fetch(`/api/reviews/${id}`, { method: 'DELETE' });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not delete review');
            }
            window.toastSuccess('Review removed');
            await loadReviews();
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
