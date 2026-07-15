(function () {
    'use strict';

    let currentPage = 0;
    let totalPages = 1;
    const pageSize = 8;

    document.addEventListener('DOMContentLoaded', function () {
        document.getElementById('prevPageBtn').addEventListener('click', () => changePage(-1));
        document.getElementById('nextPageBtn').addEventListener('click', () => changePage(1));
        loadOrders();
    });

    function changePage(delta) {
        const next = currentPage + delta;
        if (next < 0 || next >= totalPages) return;
        currentPage = next;
        loadOrders();
    }

    async function loadOrders() {
        const list = document.getElementById('ordersList');
        try {
            const res = await fetch(`/api/orders?page=${currentPage}&size=${pageSize}`);
            if (res.status === 401) { window.requireLoginThen('/orders'); return; }
            if (!res.ok) throw new Error('Failed to load orders');
            const data = await res.json();
            totalPages = data.totalPages || 1;

            document.getElementById('pageLabel').textContent = `Page ${currentPage + 1} of ${Math.max(totalPages, 1)}`;
            document.getElementById('prevPageBtn').disabled = currentPage === 0;
            document.getElementById('nextPageBtn').disabled = currentPage >= totalPages - 1;

            const items = data.content || [];
            if (items.length === 0) {
                list.innerHTML = `
                    <div class="card-surface p-5 text-center">
                        <i class="fa-solid fa-box-open fa-2x text-muted mb-3"></i>
                        <p class="mb-3">You haven't placed any orders yet.</p>
                        <a href="/products" class="btn btn-brand">Start shopping</a>
                    </div>`;
                return;
            }

            list.innerHTML = items.map(o => `
                <a href="/orders/${o.id}" class="card-surface p-3 d-flex flex-row justify-content-between align-items-center text-decoration-none text-reset">
                    <div class="d-flex align-items-center gap-3">
                        <img src="${o.firstProductImageUrl || 'https://placehold.co/64x64?text=Order'}" style="width:56px;height:56px;object-fit:cover;border-radius:10px" alt="">
                        <div>
                            <div class="fw-semibold font-mono small">${o.orderNumber}</div>
                            <div class="small text-muted">${o.itemCount} item(s) · ${window.formatDate(o.placedAt)}</div>
                        </div>
                    </div>
                    <div class="text-end">
                        <div class="status-pill status-pill--${String(o.status).toLowerCase()} mb-1">${formatStatusLabel(o.status)}</div>
                        <div class="fw-bold small">${window.formatCurrency(o.totalAmount)}</div>
                    </div>
                </a>`).join('');
        } catch (e) {
            console.error(e);
            list.innerHTML = '<p class="text-danger">Could not load your orders.</p>';
        }
    }

    function formatStatusLabel(status) {
        return String(status).toLowerCase().split('_').join(' ');
    }
})();
