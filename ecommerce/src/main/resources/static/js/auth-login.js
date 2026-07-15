(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        document.getElementById('loginForm').addEventListener('submit', handleLogin);
        document.getElementById('forgotPasswordForm').addEventListener('submit', handleForgotPassword);
    });

    function getRedirectTarget() {
        const params = new URLSearchParams(window.location.search);
        return params.get('redirect') || '/';
    }

    async function handleLogin(e) {
        e.preventDefault();
        const payload = {
            email: document.getElementById('loginEmail').value,
            password: document.getElementById('loginPassword').value,
            rememberMe: document.getElementById('rememberMe').checked
        };

        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not log in');
            }
            window.location.href = getRedirectTarget();
        } catch (err) {
            window.toastError(err.message);
        }
    }

    async function handleForgotPassword(e) {
        e.preventDefault();
        const email = document.getElementById('forgotEmail').value;

        try {
            const res = await fetch('/api/auth/forgot-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email })
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok) throw new Error(data.message || 'Could not process request');

            bootstrap.Modal.getInstance(document.getElementById('forgotPasswordModal')).hide();
            window.toastSuccess(data.message || 'Reset link sent if the account exists');
        } catch (err) {
            window.toastError(err.message);
        }
    }
})();
