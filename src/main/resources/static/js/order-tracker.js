/**
 * Persistent order tracking. payment-success.html stores the active order id
 * in localStorage. This script (included on other pages, e.g. the menu) picks
 * that up and keeps polling + shows a floating banner, so customers who
 * download their receipt and navigate away still get notified when their
 * order is ready.
 */
(function () {
    const STORAGE_KEY = 'mtugo_tracking_order';
    const POLL_MS = 8000;

    const orderId = localStorage.getItem(STORAGE_KEY);
    if (!orderId) return;

    const STAGES = {
        PAID: { text: 'Preparing your order...', bg: '#fff8e6', border: '#d4af37', color: '#8a6d1a' },
        PREPARING: { text: 'Your order is being cooked...', bg: '#fff8e6', border: '#d4af37', color: '#8a6d1a' },
        READY: { text: 'Order ready for pickup!', bg: '#e9f7ee', border: '#2e7d4f', color: '#1e5c37' },
        COMPLETED: { text: 'Order collected. Thank you!', bg: '#eef3f7', border: '#2a4a5a', color: '#1a2f3f' }
    };

    let banner;

    function buildBanner() {
        banner = document.createElement('div');
        banner.id = 'mtugoOrderTrackerBanner';
        banner.style.cssText = [
            'position:fixed', 'bottom:20px', 'right:20px', 'max-width:280px',
            'background:#fff8e6', 'border-left:4px solid #d4af37', 'border-radius:10px',
            'padding:14px 40px 14px 16px', 'box-shadow:0 4px 20px rgba(0,0,0,0.15)',
            'font-family:Segoe UI, Roboto, sans-serif', 'font-size:0.9em', 'font-weight:600',
            'color:#8a6d1a', 'z-index:9999'
        ].join(';');
        banner.innerHTML =
            '<span id="mtugoTrackerText">Tracking order #' + orderId + '...</span>' +
            '<button id="mtugoTrackerClose" style="position:absolute; top:6px; right:8px; border:none; background:none; cursor:pointer; font-size:1em; color:inherit;">&times;</button>';
        document.body.appendChild(banner);

        document.getElementById('mtugoTrackerClose').addEventListener('click', function () {
            banner.remove();
            localStorage.removeItem(STORAGE_KEY);
            clearInterval(timer);
        });
    }

    function applyStage(status) {
        const stage = STAGES[status];
        if (!stage || !banner) return;
        document.getElementById('mtugoTrackerText').textContent =
            'Order #' + orderId + ': ' + stage.text;
        banner.style.background = stage.bg;
        banner.style.borderLeftColor = stage.border;
        banner.style.color = stage.color;

        if (status === 'READY' && window.Notification && Notification.permission === 'granted') {
            new Notification('Mtugo Hotel', { body: 'Order #' + orderId + ' is ready for pickup!' });
        }
    }

    async function poll() {
        try {
            const res = await fetch('/api/orders/' + orderId + '/status');
            if (!res.ok) return;
            const data = await res.json();
            applyStage(data.status);

            if (data.status === 'COMPLETED') {
                clearInterval(timer);
                localStorage.removeItem(STORAGE_KEY);
                setTimeout(function () { if (banner) banner.remove(); }, 6000);
            }
        } catch (err) {
            console.error('Order tracker poll failed', err);
        }
    }

    buildBanner();
    poll();
    var timer = setInterval(poll, POLL_MS);
})();
