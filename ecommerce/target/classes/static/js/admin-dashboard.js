(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', loadDashboard);

    async function loadDashboard() {
        try {
            const res = await fetch('/api/admin/analytics/dashboard');
            if (!res.ok) {
                throw new Error('Failed to load dashboard (status ' + res.status + ')');
            }
            const data = await res.json();

            animateNumber('statRevenue', data.totalRevenue, true);
            animateNumber('statOrders', data.totalOrders);
            animateNumber('statUsers', data.totalUsers);
            animateNumber('statProducts', data.totalProducts);

            document.getElementById('statPending').textContent = data.pendingOrders;
            document.getElementById('statLowStock').textContent = data.lowStockCount;
            document.getElementById('statOutOfStock').textContent = data.outOfStockCount;

            renderRevenueChart(data.revenueByMonth || []);
            renderStatusChart(data.orderStatusBreakdown || {});
            renderTopProductsChart(data.topSellingProducts || []);

            renderRecentOrders(data.recentOrders || []);
            renderRecentUsers(data.recentUsers || []);
            renderRecentProducts(data.recentProducts || []);
        } catch (e) {
            console.error(e);
            window.toastError('Could not load dashboard data. Please refresh the page.');
        }
    }

    function animateNumber(elementId, value, asCurrency) {
        const el = document.getElementById(elementId);
        if (!el) return;
        const target = Number(value || 0);
        const duration = 900;
        const start = performance.now();

        function tick(now) {
            const progress = Math.min((now - start) / duration, 1);
            const current = target * progress;
            el.textContent = asCurrency ? window.formatCurrency(current) : Math.round(current).toLocaleString('en-IN');
            if (progress < 1) requestAnimationFrame(tick);
        }
        requestAnimationFrame(tick);
    }

    function renderRevenueChart(points) {
        const ctx = document.getElementById('revenueChart');
        if (!ctx) return;
        const gradient = ctx.getContext('2d').createLinearGradient(0, 0, 0, 260);
        gradient.addColorStop(0, 'rgba(124, 58, 237, 0.35)');
        gradient.addColorStop(1, 'rgba(124, 58, 237, 0)');

        new Chart(ctx, {
            type: 'line',
            data: {
                labels: points.map(p => p.month),
                datasets: [{
                    label: 'Revenue',
                    data: points.map(p => p.revenue),
                    borderColor: '#7c3aed',
                    backgroundColor: gradient,
                    fill: true,
                    tension: 0.35,
                    pointBackgroundColor: '#7c3aed',
                    pointRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, ticks: { callback: v => '\u20B9' + v } },
                    x: { grid: { display: false } }
                }
            }
        });
    }

    function renderStatusChart(breakdown) {
        const ctx = document.getElementById('statusChart');
        if (!ctx) return;
        const labels = Object.keys(breakdown);
        const values = Object.values(breakdown);
        const palette = ['#7c3aed', '#22d3ee', '#0e7490', '#f59e0b', '#10b981', '#f43f5e', '#94a3b8', '#64748b', '#475569'];

        new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels.map(formatStatusLabel),
                datasets: [{ data: values, backgroundColor: palette, borderWidth: 0 }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 11 } } } }
            }
        });
    }

    function renderTopProductsChart(topProducts) {
        const ctx = document.getElementById('topProductsChart');
        if (!ctx) return;

        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: topProducts.map(p => p.productName),
                datasets: [{
                    label: 'Units sold',
                    data: topProducts.map(p => p.totalSold),
                    backgroundColor: '#22d3ee',
                    borderRadius: 6,
                    maxBarThickness: 28
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: { x: { beginAtZero: true } }
            }
        });
    }

    function renderRecentOrders(orders) {
        const body = document.getElementById('recentOrdersBody');
        if (!body) return;
        if (orders.length === 0) {
            body.innerHTML = '<tr><td colspan="5" class="text-muted text-center py-3">No orders yet</td></tr>';
            return;
        }
        body.innerHTML = orders.map(o => `
            <tr>
                <td class="font-mono">${o.orderNumber}</td>
                <td>${statusPill(o.status)}</td>
                <td>${o.itemCount}</td>
                <td>${window.formatCurrency(o.totalAmount)}</td>
                <td>${window.formatDate(o.placedAt)}</td>
            </tr>`).join('');
    }

    function renderRecentUsers(users) {
        const body = document.getElementById('recentUsersBody');
        if (!body) return;
        if (users.length === 0) {
            body.innerHTML = '<tr><td colspan="4" class="text-muted text-center py-3">No users yet</td></tr>';
            return;
        }
        body.innerHTML = users.map(u => `
            <tr>
                <td>${escapeHtml(u.name)}</td>
                <td class="text-muted">${escapeHtml(u.email)}</td>
                <td>${u.role}</td>
                <td>${window.formatDate(u.createdAt)}</td>
            </tr>`).join('');
    }

    function renderRecentProducts(products) {
        const body = document.getElementById('recentProductsBody');
        if (!body) return;
        if (products.length === 0) {
            body.innerHTML = '<tr><td colspan="4" class="text-muted text-center py-3">No products yet</td></tr>';
            return;
        }
        body.innerHTML = products.map(p => `
            <tr>
                <td>${escapeHtml(p.name)}</td>
                <td>${window.formatCurrency(p.effectivePrice)}</td>
                <td>${p.inStock ? '<span class="text-success">In stock</span>' : '<span class="text-danger">Out of stock</span>'}</td>
                <td>${p.averageRating ? p.averageRating.toFixed(1) + ' ★' : '—'}</td>
            </tr>`).join('');
    }

    function statusPill(status) {
        const cls = 'status-pill--' + String(status).toLowerCase();
        return `<span class="status-pill ${cls}">${formatStatusLabel(status)}</span>`;
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
