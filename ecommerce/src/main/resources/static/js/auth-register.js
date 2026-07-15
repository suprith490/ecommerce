(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        document.getElementById('registerForm').addEventListener('submit', handleRegister);
    });

    async function handleRegister(e) {
        e.preventDefault();

        const password = document.getElementById('regPassword').value;
        const confirmPassword = document.getElementById('regConfirmPassword').value;
        if (password !== confirmPassword) {
            window.toastError('Passwords do not match');
            return;
        }

        const registerPayload = {
            name: document.getElementById('regName').value,
            email: document.getElementById('regEmail').value,
            password
        };

        try {
            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(registerPayload)
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Could not create account');
            }

            // Seamless UX: log the new user straight in rather than sending them to a separate login step.
            const loginRes = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: registerPayload.email, password: registerPayload.password, rememberMe: false })
            });
            if (!loginRes.ok) {
                window.toastSuccess('Account created — please log in');
                window.location.href = '/login';
                return;
            }

            window.location.href = '/';
        } catch (err) {
            window.toastError(err.message);
        }
    }
})();
