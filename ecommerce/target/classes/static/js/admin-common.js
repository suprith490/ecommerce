// Shared behavior across every admin page: sidebar toggle, logout, current-admin avatar.
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        const toggle = document.getElementById('sidebarToggle');
        const sidebar = document.getElementById('adminSidebar');
        if (toggle && sidebar) {
            toggle.addEventListener('click', function () {
                sidebar.classList.toggle('show');
            });
        }

        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', async function () {
                try {
                    await fetch('/api/auth/logout', { method: 'POST' });
                } finally {
                    window.location.href = '/';
                }
            });
        }

        loadCurrentAdmin();
    });

    async function loadCurrentAdmin() {
        try {
            const res = await fetch('/api/auth/me');
            if (!res.ok) return;
            const user = await res.json();

            const initialEl = document.getElementById('adminAvatarInitial');
            const nameEl = document.getElementById('adminNameLabel');
            if (initialEl && user.name) {
                initialEl.textContent = user.name.trim().charAt(0).toUpperCase();
            }
            if (nameEl) {
                nameEl.textContent = user.name + ' · ' + user.email;
            }
        } catch (e) {
            // Non-fatal — the page still works without the avatar populated.
            console.warn('Could not load current admin user', e);
        }
    }

    // Small utility other admin scripts reuse for currency formatting.
    window.formatCurrency = function (value) {
        const num = Number(value || 0);
        return '\u20B9' + num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    };

    window.formatDate = function (isoString) {
        if (!isoString) return '—';
        const d = new Date(isoString);
        return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
    };

    window.toastError = function (message) {
        if (window.Swal) {
            Swal.fire({ icon: 'error', title: 'Something went wrong', text: message, confirmButtonColor: '#7c3aed' });
        } else {
            alert(message);
        }
    };

    window.toastSuccess = function (message) {
        if (window.Swal) {
            Swal.fire({ icon: 'success', title: message, timer: 1600, showConfirmButton: false });
        } else {
            alert(message);
        }
    };
})();
