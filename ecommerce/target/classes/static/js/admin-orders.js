(function () {
    'use strict';

    let currentPage = 0;
    let totalPages = 1;
    const pageSize = 10;
    let orderModal;
    let currentOrderId = null;

    document.addEventListener('DOMContentLoaded', function () {
        orderModal = new bootstrap.Modal(document.getElementById('orderModal'));
        document.getElementById('prevPageBtn').addEventListener('click', () => changePage(-1));
        document.getElementById('nextPageBtn').addEventListener('click', () => changePage(1));
        document.getElementById('updateStatusBtn').addEventListener('click', updateStatus);
        loadOrders();
    });

    function changePage(delta) {
        const next = currentPage + delta;
        if (next < 0 || next >= totalPages) return;
        currentPage = next;
        loadOrders();
    }

    async function loadOrders() {
        const body = document.getElementById('ordersBody');
        body.innerHTML = '<tr><td colspan="7"><div class="skeleton" style="height:1.2rem"></div></td></tr>';

        try {
            const res = await fetch(`/api/admin/orders?page=${currentPage}&size=${pageSize}`);
            if (!res.ok) throw new Error('Failed to load orders');
            const pageData = await res.json();
            totalPages = pageData.totalPages || 1;

            document.getElementById('resultCount').textContent = `${pageData.totalElements} order(s)`;
            document.getElementById('pageLabel').textContent = `Page ${currentPage + 1} of ${Math.max(totalPages, 1)}`;
            document.getElementById('prevPageBtn').disabled = currentPage === 0;
            document.getElementById('nextPageBtn').disabled = currentPage >= totalPages - 1;

            renderRows(pageData.content || []);
        } catch (e) {
            console.error(e);
            body.innerHTML = '<tr><td colspan="7" class="text-danger text-center py-3">Could not load orders</td></tr>';
        }
    }

    function renderRows(orders) {
        const body = document.getElementById('ordersBody');
        if (orders.length === 0) {
            body.innerHTML = '<tr><td colspan="7" class="text-muted text-center py-4">No orders yet.</td></tr>';
            return;
        }

        body.innerHTML = orders.map(o => `
            <tr>
                <td class="font-mono small">${o.orderNumber}</td>
                <td>
                    <div class="fw-semibold">${escapeHtml(o.customerName)}</div>
                    <div class="text-muted small">${escapeHtml(o.customerEmail)}</div>
                </td>
                <td>${statusPill(o.status)}</td>
                <td><span class="small">${o.paymentMethod} · ${o.paymentStatus}</span></td>
                <td class="fw-semibold">${window.formatCurrency(o.totalAmount)}</td>
                <td>${window.formatDate(o.placedAt)}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-light" data-view="${o.id}"><i class="fa-solid fa-eye"></i> View</button>
                </td>
            </tr>`).join('');

        body.querySelectorAll('[data-view]').forEach(btn =>
            btn.addEventListener('click', () => openOrder(btn.getAttribute('data-view'))));
    }

    async function openOrder(id) {
        currentOrderId = id;
        document.getElementById('orderModalTitle').textContent = 'Loading…';
        document.getElementById('orderModalBody').innerHTML = '<div class="skeleton" style="height:8rem"></div>';
        orderModal.show();

        try {
            const res = await fetch(`/api/admin/orders/${id}`);
            if (!res.ok) throw new Error('Failed to load order');
            const o = await res.json();

            document.getElementById('orderModalTitle').textContent = o.orderNumber;
            document.getElementById('statusSelect').value = o.status;

            const itemsHtml = o.items.map(i => `
                <tr>
                    <td>${escapeHtml(i.productName)}</td>
                    <td>${i.quantity}</td>
                    <td>${window.formatCurrency(i.unitPrice)}</td>
                    <td>${window.formatCurrency(i.lineTotal)}</td>
                </tr>`).join('');

            const timelineHtml = o.statusHistory.map(h => `
                <li class="mb-2">
                    <span class="fw-semibold">${formatStatusLabel(h.status)}</span>
                    <span class="text-muted small"> — ${window.formatDate(h.changedAt)}</span>
                    ${h.note ? `<div class="small text-muted">${escapeHtml(h.note)}</div>` : ''}
                </li>`).join('');

            const addr = o.shippingAddress;

            document.getElementById('orderModalBody').innerHTML = `
                <div class="row g-4">
                    <div class="col-md-6">
                        <h6 class="small text-uppercase text-muted mb-2">Delivery address</h6>
                        <p class="mb-0 small">
                            ${escapeHtml(addr.fullName)}<br>
                            ${escapeHtml(addr.phone)}<br>
                            ${escapeHtml(addr.addressLine1)}${addr.addressLine2 ? ', ' + escapeHtml(addr.addressLine2) : ''}<br>
                            ${escapeHtml(addr.city)}, ${escapeHtml(addr.state)} ${escapeHtml(addr.postalCode)}<br>
                            ${escapeHtml(addr.country)}
                        </p>
                    </div>
                    <div class="col-md-6">
                        <h6 class="small text-uppercase text-muted mb-2">Order timeline</h6>
                        <ul class="list-unstyled small mb-0">${timelineHtml}</ul>
                    </div>
                    <div class="col-12">
                        <h6 class="small text-uppercase text-muted mb-2">Items</h6>
                        <table class="table admin-table mb-0">
                            <thead><tr><th>Product</th><th>Qty</th><th>Unit price</th><th>Line total</th></tr></thead>
                            <tbody>${itemsHtml}</tbody>
                        </table>
                    </div>
                    <div class="col-12">
                        <div class="d-flex justify-content-end">
                            <table class="small">
                                <tr><td class="text-muted pe-3">Subtotal</td><td class="text-end">${window.formatCurrency(o.subtotal)}</td></tr>
                                <tr><td class="text-muted pe-3">Shipping</td><td class="text-end">${window.formatCurrency(o.shippingCharge)}</td></tr>
                                <tr><td class="text-muted pe-3">GST</td><td class="text-end">${window.formatCurrency(o.gst)}</td></tr>
                                ${o.discountAmount > 0 ? `<tr><td class="text-muted pe-3">Discount (${o.couponCode || ''})</td><td class="text-end">-${window.formatCurrency(o.discountAmount)}</td></tr>` : ''}
                                <tr><td class="fw-bold pe-3">Total</td><td class="text-end fw-bold">${window.formatCurrency(o.totalAmount)}</td></tr>
                            </table>
                        </div>
                    </div>
                </div>`;
        } catch (e) {
            console.error(e);
            document.getElementById('orderModalBody').innerHTML = '<p class="text-danger">Could not load this order.</p>';
        }
    }

    async function updateStatus() {
        if (!currentOrderId) return;
        const status = document.getElementById('statusSelect').value;

        try {
            const res = await fetch(`/api/admin/orders/${currentOrderId}/status`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ status, note: 'Updated by admin' })
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not update status');
            }
            window.toastSuccess('Order status updated');
            orderModal.hide();
            await loadOrders();
        } catch (err) {
            window.toastError(err.message);
        }
    }

    function statusPill(status) {
        return `<span class="status-pill status-pill--${String(status).toLowerCase()}">${formatStatusLabel(status)}</span>`;
    }

    function formatStatusLabel(status) {
        return String(status).toLowerCase().split('_').join(' ');
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }
})();
