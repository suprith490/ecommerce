(function () {
    'use strict';

    const CANCELLABLE = new Set(['PLACED', 'CONFIRMED', 'PACKED']);

    document.addEventListener('DOMContentLoaded', async function () {
        const id = window.location.pathname.split('/').filter(Boolean).pop();
        const justPlaced = new URLSearchParams(window.location.search).get('justPlaced') === '1';

        try {
            const res = await fetch(`/api/orders/${id}`);
            if (res.status === 401) { window.requireLoginThen(window.location.pathname); return; }
            if (!res.ok) throw new Error('Order not found');
            const order = await res.json();
            render(order, justPlaced);
        } catch (e) {
            document.getElementById('orderRoot').innerHTML =
                '<div class="text-center py-5"><h2 class="h5">Order not found</h2><a href="/orders" class="text-brand">Back to my orders →</a></div>';
        }
    });

    function render(order, justPlaced) {
        const addr = order.shippingAddress;

        const itemsHtml = order.items.map(i => `
            <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
                <div class="d-flex align-items-center gap-3">
                    <img src="${i.productImageUrl || 'https://placehold.co/64x64?text=Item'}" style="width:56px;height:56px;object-fit:cover;border-radius:10px" alt="">
                    <div>
                        <div class="small fw-semibold">${escapeHtml(i.productName)}</div>
                        <div class="small text-muted">Qty ${i.quantity} × ${window.formatCurrency(i.unitPrice)}</div>
                    </div>
                </div>
                <div class="fw-semibold small">${window.formatCurrency(i.lineTotal)}</div>
            </div>`).join('');

        const timelineHtml = order.statusHistory.map((h, idx) => `
            <li class="mb-3 position-relative ps-3" style="border-left:2px solid ${idx === order.statusHistory.length - 1 ? '#ff5722' : '#e5e7eb'}">
                <div class="fw-semibold small">${formatStatusLabel(h.status)}</div>
                <div class="text-muted small">${window.formatDate(h.changedAt)}</div>
                ${h.note ? `<div class="small text-muted">${escapeHtml(h.note)}</div>` : ''}
            </li>`).join('');

        let actionsHtml = '';
        if (CANCELLABLE.has(order.status)) {
            actionsHtml = `<button class="btn btn-outline-brand btn-sm" id="cancelOrderBtn">Cancel order</button>`;
        } else if (order.status === 'DELIVERED') {
            actionsHtml = `<button class="btn btn-outline-brand btn-sm" id="returnOrderBtn">Request return</button>`;
        }

        document.getElementById('orderRoot').innerHTML = `
            ${justPlaced ? `
            <div class="card-surface p-4 mb-4 text-center" style="border:1px solid rgba(0,200,83,.3)">
                <i class="fa-solid fa-circle-check fa-2x text-success mb-2"></i>
                <h1 class="h5 fw-bold mb-1">Order placed successfully!</h1>
                <p class="text-muted small mb-0">We'll send updates as your order makes its way to you.</p>
            </div>` : ''}

            <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-4">
                <div>
                    <h1 class="h5 fw-bold mb-1 font-mono">${order.orderNumber}</h1>
                    <div class="small text-muted">Placed on ${window.formatDate(order.placedAt)}</div>
                </div>
                <div class="text-end">
                    <div class="status-pill status-pill--${String(order.status).toLowerCase()} mb-2">${formatStatusLabel(order.status)}</div>
                    <div>${actionsHtml}</div>
                </div>
            </div>

            <div class="row g-4">
                <div class="col-12 col-lg-8">
                    <div class="card-surface p-3 mb-3">
                        <h2 class="h6 fw-bold mb-2">Items</h2>
                        ${itemsHtml}
                    </div>
                    <div class="card-surface p-3">
                        <h2 class="h6 fw-bold mb-2">Order timeline</h2>
                        <ul class="list-unstyled mb-0">${timelineHtml}</ul>
                    </div>
                </div>
                <div class="col-12 col-lg-4">
                    <div class="card-surface p-3 mb-3">
                        <h2 class="h6 fw-bold mb-2">Delivery address</h2>
                        <p class="small mb-0">
                            ${escapeHtml(addr.fullName)}<br>${escapeHtml(addr.phone)}<br>
                            ${escapeHtml(addr.addressLine1)}${addr.addressLine2 ? ', ' + escapeHtml(addr.addressLine2) : ''}<br>
                            ${escapeHtml(addr.city)}, ${escapeHtml(addr.state)} ${escapeHtml(addr.postalCode)}<br>
                            ${escapeHtml(addr.country)}
                        </p>
                    </div>
                    <div class="card-surface p-3">
                        <h2 class="h6 fw-bold mb-2">Payment</h2>
                        <p class="small mb-3">${order.paymentMethod} · <span class="text-muted">${order.paymentStatus}</span></p>
                        <div class="d-flex justify-content-between small mb-1"><span class="text-muted">Subtotal</span><span>${window.formatCurrency(order.subtotal)}</span></div>
                        <div class="d-flex justify-content-between small mb-1"><span class="text-muted">Shipping</span><span>${window.formatCurrency(order.shippingCharge)}</span></div>
                        <div class="d-flex justify-content-between small mb-1"><span class="text-muted">GST</span><span>${window.formatCurrency(order.gst)}</span></div>
                        ${order.discountAmount > 0 ? `<div class="d-flex justify-content-between small mb-1 text-success"><span>Discount (${order.couponCode || ''})</span><span>-${window.formatCurrency(order.discountAmount)}</span></div>` : ''}
                        <hr>
                        <div class="d-flex justify-content-between fw-bold"><span>Total</span><span>${window.formatCurrency(order.totalAmount)}</span></div>
                    </div>
                </div>
            </div>`;

        const cancelBtn = document.getElementById('cancelOrderBtn');
        if (cancelBtn) cancelBtn.addEventListener('click', () => performAction(order.id, 'cancel'));

        const returnBtn = document.getElementById('returnOrderBtn');
        if (returnBtn) returnBtn.addEventListener('click', () => performAction(order.id, 'return'));
    }

    async function performAction(orderId, action) {
        const { value: reason } = await Swal.fire({
            title: action === 'cancel' ? 'Cancel this order?' : 'Request a return?',
            input: 'text',
            inputLabel: 'Reason',
            inputPlaceholder: action === 'cancel' ? 'e.g. Ordered by mistake' : 'e.g. Item not as described',
            showCancelButton: true,
            confirmButtonText: action === 'cancel' ? 'Cancel order' : 'Request return',
            confirmButtonColor: '#e53935',
            inputValidator: (value) => !value ? 'Please provide a reason' : undefined
        });
        if (!reason) return;

        try {
            const res = await fetch(`/api/orders/${orderId}/${action}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ reason })
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not complete this action');
            }
            window.toastSuccess(action === 'cancel' ? 'Order cancelled' : 'Return requested');
            window.location.reload();
        } catch (e) {
            window.toastError(e.message);
        }
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
