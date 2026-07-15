(function () {
    'use strict';

    let couponModal;

    document.addEventListener('DOMContentLoaded', async function () {
        couponModal = new bootstrap.Modal(document.getElementById('couponModal'));
        await loadCoupons();

        document.getElementById('addCouponBtn').addEventListener('click', () => openModal(null));
        document.getElementById('couponForm').addEventListener('submit', handleSubmit);
    });

    async function loadCoupons() {
        const body = document.getElementById('couponsBody');
        try {
            const res = await fetch('/api/admin/coupons');
            if (!res.ok) throw new Error('Failed to load coupons');
            const coupons = await res.json();
            document.getElementById('resultCount').textContent = `${coupons.length} coupon(s)`;

            if (coupons.length === 0) {
                body.innerHTML = '<tr><td colspan="7" class="text-muted text-center py-4">No coupons yet — add your first one above.</td></tr>';
                return;
            }

            const now = new Date();
            body.innerHTML = coupons.map(c => {
                const expired = new Date(c.expiryDate) < now;
                const discountLabel = c.discountType === 'PERCENTAGE' ? `${c.discountValue}%` : window.formatCurrency(c.discountValue);
                const usageLabel = c.usageLimit ? `${c.usedCount} / ${c.usageLimit}` : `${c.usedCount} / ∞`;
                const statusBadge = !c.active
                    ? '<span class="status-pill status-pill--return_requested">Inactive</span>'
                    : (expired ? '<span class="status-pill status-pill--cancelled">Expired</span>' : '<span class="status-pill status-pill--delivered">Active</span>');

                return `
                <tr>
                    <td class="font-mono fw-semibold">${escapeHtml(c.code)}</td>
                    <td>${discountLabel}${c.maxDiscountAmount ? ` <span class="text-muted small">(cap ${window.formatCurrency(c.maxDiscountAmount)})</span>` : ''}</td>
                    <td>${window.formatCurrency(c.minOrderAmount)}</td>
                    <td>${window.formatDate(c.expiryDate)}</td>
                    <td>${usageLabel}</td>
                    <td>${statusBadge}</td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-light" data-edit="${c.id}"><i class="fa-solid fa-pen"></i></button>
                        <button class="btn btn-sm btn-light text-danger" data-delete="${c.id}" data-code="${escapeHtml(c.code)}"><i class="fa-solid fa-trash"></i></button>
                    </td>
                </tr>`;
            }).join('');

            body.querySelectorAll('[data-edit]').forEach(btn =>
                btn.addEventListener('click', () => openModal(btn.getAttribute('data-edit'))));
            body.querySelectorAll('[data-delete]').forEach(btn =>
                btn.addEventListener('click', () => confirmDelete(btn.getAttribute('data-delete'), btn.getAttribute('data-code'))));
        } catch (e) {
            console.error(e);
            body.innerHTML = '<tr><td colspan="7" class="text-danger text-center py-3">Could not load coupons</td></tr>';
        }
    }

    async function openModal(id) {
        document.getElementById('couponForm').reset();
        document.getElementById('couponId').value = id || '';
        document.getElementById('couponModalTitle').textContent = id ? 'Edit coupon' : 'Add coupon';
        document.getElementById('coMinOrder').value = 0;

        if (id) {
            const res = await fetch(`/api/admin/coupons/${id}`);
            if (!res.ok) { window.toastError('Could not load this coupon'); return; }
            const c = await res.json();
            document.getElementById('coCode').value = c.code;
            document.getElementById('coType').value = c.discountType;
            document.getElementById('coValue').value = c.discountValue;
            document.getElementById('coMinOrder').value = c.minOrderAmount;
            document.getElementById('coMaxDiscount').value = c.maxDiscountAmount || '';
            document.getElementById('coExpiry').value = (c.expiryDate || '').substring(0, 16);
            document.getElementById('coUsageLimit').value = c.usageLimit || '';
            document.getElementById('coActive').checked = c.active;
        }

        couponModal.show();
    }

    async function handleSubmit(e) {
        e.preventDefault();
        const id = document.getElementById('couponId').value;

        const payload = {
            code: document.getElementById('coCode').value.trim().toUpperCase(),
            discountType: document.getElementById('coType').value,
            discountValue: Number(document.getElementById('coValue').value),
            minOrderAmount: Number(document.getElementById('coMinOrder').value || 0),
            maxDiscountAmount: document.getElementById('coMaxDiscount').value ? Number(document.getElementById('coMaxDiscount').value) : null,
            expiryDate: document.getElementById('coExpiry').value,
            usageLimit: document.getElementById('coUsageLimit').value ? Number(document.getElementById('coUsageLimit').value) : null,
            active: document.getElementById('coActive').checked
        };

        try {
            const res = await fetch(id ? `/api/admin/coupons/${id}` : '/api/admin/coupons', {
                method: id ? 'PUT' : 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not save coupon');
            }
            couponModal.hide();
            window.toastSuccess(id ? 'Coupon updated' : 'Coupon created');
            await loadCoupons();
        } catch (err) {
            window.toastError(err.message);
        }
    }

    async function confirmDelete(id, code) {
        const result = await Swal.fire({
            icon: 'warning',
            title: `Delete "${code}"?`,
            showCancelButton: true,
            confirmButtonText: 'Delete',
            confirmButtonColor: '#f43f5e'
        });
        if (!result.isConfirmed) return;

        try {
            const res = await fetch(`/api/admin/coupons/${id}`, { method: 'DELETE' });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not delete coupon');
            }
            window.toastSuccess('Coupon deleted');
            await loadCoupons();
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
