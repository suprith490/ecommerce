(function () {
    'use strict';

    let addressModal;
    let addresses = [];

    document.addEventListener('DOMContentLoaded', async function () {
        addressModal = new bootstrap.Modal(document.getElementById('addressModal'));

        await loadProfile();
        await loadAddresses();

        document.getElementById('profileForm').addEventListener('submit', handleProfileUpdate);
        document.getElementById('passwordForm').addEventListener('submit', handlePasswordChange);
        document.getElementById('addAddressBtn').addEventListener('click', () => openAddressModal(null));
        document.getElementById('addressForm').addEventListener('submit', handleAddressSubmit);
    });

    async function loadProfile() {
        try {
            const res = await fetch('/api/auth/me');
            if (res.status === 401) { window.requireLoginThen('/profile'); return; }
            if (!res.ok) throw new Error('Failed to load profile');
            const user = await res.json();

            document.getElementById('profileNameLabel').textContent = user.name;
            document.getElementById('profileEmailLabel').textContent = user.email;
            document.getElementById('pfName').value = user.name;
            document.getElementById('pfPhoto').value = user.profileImageUrl || '';

            const avatar = document.getElementById('profileAvatar');
            if (user.profileImageUrl) {
                avatar.innerHTML = `<img src="${user.profileImageUrl}" style="width:100%;height:100%;object-fit:cover;border-radius:50%">`;
            } else {
                avatar.textContent = user.name.trim().charAt(0).toUpperCase();
            }
        } catch (e) {
            window.toastError('Could not load your profile');
        }
    }

    async function handleProfileUpdate(e) {
        e.preventDefault();
        try {
            const res = await fetch('/api/profile', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: document.getElementById('pfName').value,
                    profileImageUrl: document.getElementById('pfPhoto').value
                })
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not update profile');
            }
            window.toastSuccess('Profile updated');
            await loadProfile();
        } catch (err) {
            window.toastError(err.message);
        }
    }

    async function handlePasswordChange(e) {
        e.preventDefault();
        try {
            const res = await fetch('/api/profile/password', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    currentPassword: document.getElementById('currentPassword').value,
                    newPassword: document.getElementById('newPassword').value
                })
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not update password');
            }
            window.toastSuccess('Password updated');
            e.target.reset();
        } catch (err) {
            window.toastError(err.message);
        }
    }

    async function loadAddresses() {
        const container = document.getElementById('addressList');
        try {
            const res = await fetch('/api/addresses');
            if (!res.ok) throw new Error('Failed to load addresses');
            addresses = await res.json();
            renderAddresses();
        } catch (e) {
            container.innerHTML = '<p class="text-danger small">Could not load addresses.</p>';
        }
    }

    function renderAddresses() {
        const container = document.getElementById('addressList');
        if (addresses.length === 0) {
            container.innerHTML = '<p class="text-muted small mb-0">No saved addresses yet.</p>';
            return;
        }

        container.innerHTML = addresses.map(a => `
            <div class="d-flex justify-content-between align-items-start border rounded p-2" data-id="${a.id}">
                <div class="small">
                    <div class="fw-semibold">${escapeHtml(a.fullName)} ${a.isDefault ? '<span class="status-pill status-pill--delivered ms-1">Default</span>' : ''}</div>
                    <div>${escapeHtml(a.addressLine1)}${a.addressLine2 ? ', ' + escapeHtml(a.addressLine2) : ''}</div>
                    <div>${escapeHtml(a.city)}, ${escapeHtml(a.state)} ${escapeHtml(a.postalCode)}</div>
                    <div class="text-muted">${escapeHtml(a.phone)}</div>
                </div>
                <div class="d-flex gap-1">
                    ${!a.isDefault ? `<button class="btn btn-sm btn-light" data-set-default="${a.id}" title="Set as default"><i class="fa-regular fa-star"></i></button>` : ''}
                    <button class="btn btn-sm btn-light" data-edit-address="${a.id}"><i class="fa-solid fa-pen"></i></button>
                    <button class="btn btn-sm btn-light text-danger" data-delete-address="${a.id}"><i class="fa-solid fa-trash"></i></button>
                </div>
            </div>`).join('');

        container.querySelectorAll('[data-set-default]').forEach(btn =>
            btn.addEventListener('click', () => setDefaultAddress(btn.getAttribute('data-set-default'))));
        container.querySelectorAll('[data-edit-address]').forEach(btn =>
            btn.addEventListener('click', () => openAddressModal(btn.getAttribute('data-edit-address'))));
        container.querySelectorAll('[data-delete-address]').forEach(btn =>
            btn.addEventListener('click', () => deleteAddress(btn.getAttribute('data-delete-address'))));
    }

    function openAddressModal(id) {
        document.getElementById('addressForm').reset();
        document.getElementById('addrId').value = id || '';
        document.getElementById('addressModalTitle').textContent = id ? 'Edit address' : 'Add address';
        document.getElementById('addrCountry').value = 'India';

        if (id) {
            const a = addresses.find(x => String(x.id) === String(id));
            if (a) {
                document.getElementById('addrName').value = a.fullName;
                document.getElementById('addrPhone').value = a.phone;
                document.getElementById('addrLine1').value = a.addressLine1;
                document.getElementById('addrLine2').value = a.addressLine2 || '';
                document.getElementById('addrCity').value = a.city;
                document.getElementById('addrState').value = a.state;
                document.getElementById('addrPostal').value = a.postalCode;
                document.getElementById('addrCountry').value = a.country;
                document.getElementById('addrDefault').checked = a.isDefault;
            }
        }
        addressModal.show();
    }

    async function handleAddressSubmit(e) {
        e.preventDefault();
        const id = document.getElementById('addrId').value;
        const payload = {
            fullName: document.getElementById('addrName').value,
            phone: document.getElementById('addrPhone').value,
            addressLine1: document.getElementById('addrLine1').value,
            addressLine2: document.getElementById('addrLine2').value,
            city: document.getElementById('addrCity').value,
            state: document.getElementById('addrState').value,
            postalCode: document.getElementById('addrPostal').value,
            country: document.getElementById('addrCountry').value || 'India',
            isDefault: document.getElementById('addrDefault').checked
        };

        try {
            const res = await fetch(id ? `/api/addresses/${id}` : '/api/addresses', {
                method: id ? 'PUT' : 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not save address');
            }
            addressModal.hide();
            window.toastSuccess(id ? 'Address updated' : 'Address added');
            await loadAddresses();
        } catch (err) {
            window.toastError(err.message);
        }
    }

    async function setDefaultAddress(id) {
        try {
            const res = await fetch(`/api/addresses/${id}/default`, { method: 'PATCH' });
            if (!res.ok) throw new Error('Could not set default address');
            await loadAddresses();
        } catch (e) {
            window.toastError(e.message);
        }
    }

    async function deleteAddress(id) {
        const result = await Swal.fire({
            icon: 'warning', title: 'Delete this address?', showCancelButton: true,
            confirmButtonText: 'Delete', confirmButtonColor: '#e53935'
        });
        if (!result.isConfirmed) return;

        try {
            const res = await fetch(`/api/addresses/${id}`, { method: 'DELETE' });
            if (!res.ok) throw new Error('Could not delete address');
            await loadAddresses();
        } catch (e) {
            window.toastError(e.message);
        }
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }
})();
