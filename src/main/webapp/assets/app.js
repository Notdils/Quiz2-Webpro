// Harvesta UI behaviour: mobile drawer, toasts, delete confirmation, busy buttons, Chart.js charts.
(function () {
    'use strict';

    document.addEventListener('click', function (event) {
        var toggle = event.target.closest('[data-nav-toggle]');
        if (toggle) {
            document.body.classList.toggle('nav-open');
            return;
        }
        if (event.target.closest('.scrim')) {
            document.body.classList.remove('nav-open');
        }
        var close = event.target.closest('[data-dismiss]');
        if (close) {
            close.closest('.toast').remove();
        }
    });

    // Confirm before destructive actions; show a spinner on submit.
    document.addEventListener('submit', function (event) {
        var form = event.target;
        var message = form.getAttribute('data-confirm');
        if (message && !window.confirm(message)) {
            event.preventDefault();
            return;
        }
        var button = form.querySelector('button[type=submit]:not([formnovalidate])');
        if (button && !form.hasAttribute('data-confirm')) {
            button.setAttribute('aria-busy', 'true');
        }
    });

    // Filters submit as soon as a select changes.
    document.querySelectorAll('form.filters select').forEach(function (select) {
        select.addEventListener('change', function () { select.form.submit(); });
    });

    // Toasts disappear after a few seconds.
    document.querySelectorAll('.toast').forEach(function (toast) {
        setTimeout(function () {
            toast.classList.add('hide');
            setTimeout(function () { toast.remove(); }, 300);
        }, 5000);
    });

    // Live preview of a chosen photo.
    document.querySelectorAll('input[type=file][data-preview]').forEach(function (input) {
        input.addEventListener('change', function () {
            var img = document.getElementById(input.getAttribute('data-preview'));
            if (img && input.files && input.files[0]) {
                img.src = URL.createObjectURL(input.files[0]);
                img.hidden = false;
            }
        });
    });

    if (!window.Chart) {
        return;
    }

    var green = '#2F7D4F', soil = '#8B5E3C', gold = '#D9A406', red = '#C0392B', blue = '#2B6CB0';
    var palette = [green, gold, blue, soil, '#6FCF97', red, '#7C3AED', '#0E7490'];
    var idr = function (v) { return 'IDR ' + Number(v).toLocaleString('en-US', { maximumFractionDigits: 0 }); };
    var num = function (v) { return Number(v).toLocaleString('en-US', { maximumFractionDigits: 2 }); };

    Chart.defaults.font.family = "'Inter', system-ui, sans-serif";
    Chart.defaults.color = '#6B7280';
    Chart.defaults.borderColor = '#EEE9DC';
    Chart.defaults.plugins.legend.labels.usePointStyle = true;
    Chart.defaults.plugins.legend.labels.boxWidth = 8;
    Chart.defaults.maintainAspectRatio = false;

    var builders = {
        harvest: function (d) {
            return {
                type: 'bar',
                data: {
                    labels: d.labels,
                    datasets: [
                        { label: 'kg', data: d.kg, backgroundColor: green, borderRadius: 6, maxBarThickness: 28 },
                        { label: 'pcs', data: d.pcs, backgroundColor: gold, borderRadius: 6, maxBarThickness: 28 }
                    ]
                },
                options: { plugins: { tooltip: { callbacks: { label: function (c) { return c.dataset.label + ': ' + num(c.raw); } } } },
                    scales: { x: { grid: { display: false } }, y: { beginAtZero: true } } }
            };
        },
        finance: function (d) {
            return {
                type: 'line',
                data: {
                    labels: d.labels,
                    datasets: [
                        { label: 'Sales', data: d.sales, borderColor: green, backgroundColor: 'rgba(47,125,79,.12)', fill: true, tension: .35, pointRadius: 3 },
                        { label: 'Expenses', data: d.expenses, borderColor: soil, backgroundColor: 'rgba(139,94,60,.08)', fill: true, tension: .35, pointRadius: 3 }
                    ]
                },
                options: { interaction: { mode: 'index', intersect: false },
                    plugins: { tooltip: { callbacks: { label: function (c) { return c.dataset.label + ': ' + idr(c.raw); } } } },
                    scales: { x: { grid: { display: false } }, y: { beginAtZero: true, ticks: { callback: function (v) { return Number(v) >= 1e6 ? (v / 1e6) + 'M' : num(v); } } } } }
            };
        },
        doughnut: function (d) {
            return {
                type: 'doughnut',
                data: { labels: d.labels, datasets: [{ data: d.values, backgroundColor: palette, borderWidth: 2, borderColor: '#fff' }] },
                options: { cutout: '62%', plugins: { legend: { position: 'right' } } }
            };
        }
    };

    document.querySelectorAll('canvas[data-chart]').forEach(function (canvas) {
        var build = builders[canvas.getAttribute('data-kind')];
        if (build) {
            new Chart(canvas, build(JSON.parse(canvas.getAttribute('data-chart'))));
        }
    });
})();
