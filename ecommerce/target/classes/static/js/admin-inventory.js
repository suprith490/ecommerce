(function () {
    'use strict';

    let activeTab = 'low-stock';
    let currentPage = 0;
    let totalPages = 1;
    const pageSize = 10;

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('#inventoryTabs [data-tab]').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('#inventoryTabs .nav-link').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                activeTab = btn.getAttribute('data-tab');
                currentPage = 0;
                loadInventory();
            });
        });

        document.getElementById('prevPageBtn').addEventListener('click', () => changePage(-1));
        document.getElementById('nextPageBtn').addEventListener('click', () => changePage(1));

        loadInventory();
    });

    function changePage(delta) {
        const next = currentPage + delta;
        if (next < 0 || next >= totalPages) return;
        currentPage = next;
        loadInventory();
    }

    async function loadInventory() {
        const body = document.getElementById('inventoryBody');
        body.innerHTML = '<tr><td colspan="5"><div class="skeleton" style="height:1.2rem"></div></td></tr>';

        try {
            const res = await fetch(`/api/admin/products/inventory/${activeTab}?page=${currentPage}&size=${pageSize}`);
            if (!res.ok) throw new Error('Failed to load inventory');
            const pageData = await res.json();
            totalPages = pageData.totalPages || 1;

            document.getElementById('pageLabel').textContent = `Page ${currentPage + 1} of ${Math.max(totalPages, 1)} · ${pageData.totalElements} product(s)`;
            document.getElementById('prevPageBtn').disabled = currentPage === 0;
            document.getElementById('nextPageBtn').disabled = currentPage >= totalPages - 1;

            renderRows(pageData.content || []);
        } catch (e) {
            console.error(e);
            body.innerHTML = '<tr><td colspan="5" class="text-danger text-center py-3">Could not load inventory</td></tr>';
        }
    }

    function renderRows(products) {
        const body = document.getElementById('inventoryBody');
        if (products.length === 0) {
            body.innerHTML = `<tr><td colspan="5" class="text-muted text-center py-4">
                ${activeTab === 'low-stock' ? 'Nothing is running low right now.' : 'Nothing is out of stock right now.'}
            </td></tr>`;
            return;
        }

        body.innerHTML = products.map(p => `
            <tr>
                <td class="fw-semibold">${escapeHtml(p.name)}</td>
                <td class="font-mono small text-muted">${escapeHtml(p.sku)}</td>
                <td>${escapeHtml(p.categoryName || '—')}</td>
                <td>
                    <span class="status-pill ${p.stock === 0 ? 'status-pill--cancelled' : 'status-pill--shipped'}">${p.stock} unit(s)</span>
                </td>
                <td>
                    <form class="d-flex gap-2 stock-form" data-id="${p.id}">
                        <input type="number" min="0" class="form-control form-control-sm" style="width:100px" value="${p.stock}">
                        <button type="submit" class="btn btn-sm btn-gradient">Update</button>
                    </form>
                </td>
            </tr>`).join('');

        body.querySelectorAll('.stock-form').forEach(form => {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                const id = form.getAttribute('data-id');
                const newStock = form.querySelector('input').value;
                try {
                    const res = await fetch(`/api/admin/products/${id}/stock?stock=${encodeURIComponent(newStock)}`, { method: 'PATCH' });
                    if (!res.ok) {
                        const err = await res.json().catch(() => ({}));
                        throw new Error(err.message || 'Could not update stock');
                    }
                    window.toastSuccess('Stock updated');
                    await loadInventory();
                } catch (err) {
                    window.toastError(err.message);
                }
            });
        });
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }
})();
