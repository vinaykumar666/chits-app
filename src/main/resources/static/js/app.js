/**
 * YGC Internal - Main App JS
 * Covers: PWA install, Service Worker, Push Notifications (SSE + Web Push),
 *         Mobile UX enhancements, offline detection, bottom nav
 */

(function () {
  'use strict';

  // ─── Service Worker Registration ──────────────────────────────────────────
  function registerSW() {
    if (!('serviceWorker' in navigator)) return;
    navigator.serviceWorker.register('/sw.js', { scope: '/' })
      .then(reg => {
        console.log('[YGC] SW registered, scope:', reg.scope);
        // Check for updates every 60 s
        setInterval(() => reg.update(), 60000);
        reg.addEventListener('updatefound', () => {
          const newWorker = reg.installing;
          newWorker.addEventListener('statechange', () => {
            if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
              showUpdateBanner();
            }
          });
        });
      })
      .catch(err => console.warn('[YGC] SW registration failed:', err));
  }

  function showUpdateBanner() {
    const banner = document.createElement('div');
    banner.id = 'ygc-update-banner';
    banner.innerHTML = `
      <div style="position:fixed;top:0;left:0;right:0;z-index:99999;
           background:#f0a500;color:#000;padding:12px 16px;
           display:flex;align-items:center;justify-content:space-between;
           font-family:system-ui,sans-serif;font-size:14px;font-weight:600;
           box-shadow:0 2px 8px rgba(0,0,0,.2)">
        <span>🔄 New version available!</span>
        <button onclick="window.location.reload()" style="background:#000;color:#f0a500;
          border:none;padding:6px 14px;border-radius:6px;font-weight:700;cursor:pointer">
          Update Now
        </button>
      </div>`;
    document.body.prepend(banner);
  }

  // ─── PWA Install Prompt ───────────────────────────────────────────────────
  let deferredInstallPrompt = null;

  window.addEventListener('beforeinstallprompt', e => {
    e.preventDefault();
    deferredInstallPrompt = e;
    showInstallBanner();
  });

  function showInstallBanner() {
    if (localStorage.getItem('ygc-install-dismissed')) return;
    if (window.matchMedia('(display-mode: standalone)').matches) return;

    const banner = document.createElement('div');
    banner.id = 'ygc-install-banner';
    banner.innerHTML = `
      <div style="position:fixed;bottom:80px;left:16px;right:16px;z-index:9990;
           background:#1a1a2e;color:#fff;border-radius:16px;padding:16px;
           box-shadow:0 8px 32px rgba(0,0,0,.4);display:flex;gap:12px;align-items:center;
           max-width:480px;margin:0 auto;font-family:system-ui,sans-serif">
        <div style="width:48px;height:48px;background:#f0a500;border-radius:12px;
             display:flex;align-items:center;justify-content:center;
             font-size:1.5rem;font-weight:900;color:#000;flex-shrink:0">Y</div>
        <div style="flex:1">
          <div style="font-weight:700;font-size:.95rem">Install YGC App</div>
          <div style="font-size:.8rem;color:#aaa">Get the native app experience</div>
        </div>
        <div style="display:flex;gap:8px">
          <button id="ygc-install-btn" style="background:#f0a500;color:#000;border:none;
            padding:8px 14px;border-radius:8px;font-weight:700;cursor:pointer;font-size:.85rem">
            Install
          </button>
          <button id="ygc-install-dismiss" style="background:rgba(255,255,255,.1);color:#fff;
            border:none;padding:8px 10px;border-radius:8px;cursor:pointer;font-size:.85rem">
            ✕
          </button>
        </div>
      </div>`;
    document.body.appendChild(banner);

    document.getElementById('ygc-install-btn').addEventListener('click', async () => {
      if (!deferredInstallPrompt) return;
      deferredInstallPrompt.prompt();
      const result = await deferredInstallPrompt.userChoice;
      console.log('[YGC] Install prompt result:', result.outcome);
      deferredInstallPrompt = null;
      banner.remove();
    });

    document.getElementById('ygc-install-dismiss').addEventListener('click', () => {
      localStorage.setItem('ygc-install-dismissed', '1');
      banner.remove();
    });
  }

  // iOS-specific install hint
  function showIOSInstallHint() {
    if (localStorage.getItem('ygc-ios-hint-dismissed')) return;
    if (window.matchMedia('(display-mode: standalone)').matches) return;
    const isIOS = /iphone|ipad|ipod/i.test(navigator.userAgent);
    const isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);
    if (!isIOS || !isSafari) return;

    const hint = document.createElement('div');
    hint.innerHTML = `
      <div style="position:fixed;bottom:80px;left:16px;right:16px;z-index:9990;
           background:#1a1a2e;color:#fff;border-radius:16px;padding:16px;
           box-shadow:0 8px 32px rgba(0,0,0,.4);font-family:system-ui,sans-serif;
           max-width:480px;margin:0 auto">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
          <div style="font-weight:700">Install YGC on iPhone</div>
          <button onclick="this.closest('[id]').remove();localStorage.setItem('ygc-ios-hint-dismissed','1')"
            style="background:none;border:none;color:#aaa;font-size:1.2rem;cursor:pointer">✕</button>
        </div>
        <div style="font-size:.85rem;color:#ccc;line-height:1.5">
          Tap <strong style="color:#f0a500">Share</strong> <span style="font-size:1.1em">⬆️</span>
          then <strong style="color:#f0a500">"Add to Home Screen"</strong> for the best experience.
        </div>
      </div>`;
    hint.id = 'ygc-ios-hint';
    document.body.appendChild(hint);
    setTimeout(() => hint.remove(), 15000);
  }

  // ─── SSE Push Notification System ────────────────────────────────────────
  const NOTIFICATION_STYLES = {
    CHIT_REGISTRATION_APPROVED: { cls: 'success', icon: '✅' },
    CHIT_REGISTRATION_REJECTED: { cls: 'error',   icon: '❌' },
    BID_WINDOW_OPEN:            { cls: 'warn',    icon: '🔔' },
    BID_SUBMITTED:              { cls: 'success', icon: '📋' },
    BID_WINNER_ANNOUNCED:       { cls: 'success', icon: '🏆' },
    PAYMENT_REMINDER:           { cls: 'warn',    icon: '💰' },
    PAYMENT_DUE_ALERT:          { cls: 'error',   icon: '⚠️' },
    CHIT_MATURITY:              { cls: 'success', icon: '🎉' },
    AGREEMENT_APPROVED:         { cls: 'success', icon: '📄' }
  };

  let sseConnection = null;
  let reconnectTimeout = null;
  let reconnectDelay = 5000;
  let notificationLog = [];

  function connectSSE() {
    // Only connect on authenticated pages
    if (!document.getElementById('ygc-toast-container')) return;

    if (sseConnection) { sseConnection.close(); sseConnection = null; }

    try {
      sseConnection = new EventSource('/api/notifications/subscribe');

      sseConnection.onopen = () => {
        reconnectDelay = 5000;
        console.log('[YGC] SSE connected');
        updateConnectionIndicator(true);
      };

      sseConnection.onerror = (e) => {
        sseConnection.close();
        sseConnection = null;
        updateConnectionIndicator(false);
        console.warn('[YGC] SSE error, reconnecting in', reconnectDelay, 'ms');
        clearTimeout(reconnectTimeout);
        reconnectTimeout = setTimeout(() => {
          reconnectDelay = Math.min(reconnectDelay * 1.5, 60000);
          connectSSE();
        }, reconnectDelay);
      };

      // Register handlers for all notification types
      Object.keys(NOTIFICATION_STYLES).forEach(type => {
        sseConnection.addEventListener(type, e => {
          try {
            const data = JSON.parse(e.data);
            const style = NOTIFICATION_STYLES[type] || { cls: '', icon: '🔔' };
            showToast(data.title, data.message, style.cls, style.icon);
            logNotification(data, style);
            // Also try Web Push notification if page is hidden
            if (document.hidden) triggerNativeNotification(data, style);
          } catch (err) {
            console.error('[YGC] Notification parse error:', err);
          }
        });
      });
    } catch (err) {
      console.error('[YGC] SSE setup error:', err);
    }
  }

  function showToast(title, message, cls, icon) {
    const container = document.getElementById('ygc-toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `ygc-toast ${cls || ''}`;
    toast.innerHTML = `
      <button class="ygc-toast-close" aria-label="Close">×</button>
      <div class="ygc-toast-title">${icon || ''} ${escapeHtml(title)}</div>
      <div class="ygc-toast-msg">${escapeHtml(message)}</div>
      <div class="ygc-toast-time">${new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}</div>`;

    toast.querySelector('.ygc-toast-close').addEventListener('click', () => toast.remove());
    container.appendChild(toast);

    // Progress bar auto-dismiss
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(120%)';
      setTimeout(() => toast.remove(), 400);
    }, 8000);

    // Also update notification badge
    incrementNotificationBadge();

    // Vibrate on mobile for alerts
    if (cls === 'error' && 'vibrate' in navigator) navigator.vibrate([200, 100, 200]);
  }

  function logNotification(data, style) {
    notificationLog.unshift({ ...data, style, receivedAt: new Date().toISOString() });
    if (notificationLog.length > 50) notificationLog.pop();
    try { sessionStorage.setItem('ygc-notif-log', JSON.stringify(notificationLog.slice(0, 20))); } catch (e) {}
  }

  async function triggerNativeNotification(data, style) {
    if (!('Notification' in window)) return;
    if (Notification.permission !== 'granted') return;
    try {
      const reg = await navigator.serviceWorker.ready;
      reg.showNotification(`${style.icon} ${data.title}`, {
        body: data.message,
        icon: '/icons/icon-192.png',
        badge: '/icons/icon-72.png',
        tag: data.type,
        renotify: true,
        data: { url: data.url || '/' }
      });
    } catch (e) {}
  }

  function updateConnectionIndicator(connected) {
    const dot = document.querySelector('.ygc-conn-dot');
    if (!dot) return;
    dot.style.background = connected ? '#28a745' : '#dc3545';
    dot.title = connected ? 'Connected - Live notifications active' : 'Reconnecting...';
  }

  function incrementNotificationBadge() {
    const badge = document.getElementById('ygc-notif-badge');
    if (!badge) return;
    const current = parseInt(badge.textContent) || 0;
    badge.textContent = current + 1;
    badge.style.display = 'inline-flex';
  }

  // ─── Bell Icon: Notification Panel ───────────────────────────────────────
  function initBellIcon() {
    const btn = document.getElementById('ygc-bell-btn');
    if (!btn) return;

    btn.addEventListener('click', () => {
      let panel = document.getElementById('ygc-notif-panel');
      if (panel) { panel.remove(); return; }

      // Reset badge
      const badge = document.getElementById('ygc-notif-badge');
      if (badge) { badge.textContent = ''; badge.style.display = 'none'; }

      // Build panel
      panel = document.createElement('div');
      panel.id = 'ygc-notif-panel';
      panel.style.cssText = `
        position:fixed;top:60px;right:12px;z-index:9999;
        width:320px;max-width:calc(100vw - 24px);
        background:#fff;border-radius:14px;
        box-shadow:0 8px 40px rgba(0,0,0,.18);
        font-family:system-ui,sans-serif;overflow:hidden;
        border:1px solid #e8e8e8;`;

      const logs = notificationLog.slice(0, 15);
      const items = logs.length
        ? logs.map(n => `
          <div style="padding:10px 14px;border-bottom:1px solid #f0f0f0;display:flex;gap:10px;align-items:flex-start">
            <span style="font-size:1.2rem">${(n.style && n.style.icon) || '🔔'}</span>
            <div style="flex:1;min-width:0">
              <div style="font-weight:600;font-size:.85rem;color:#1a1a2e;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${escapeHtml(n.title || '')}</div>
              <div style="font-size:.78rem;color:#666;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${escapeHtml(n.message || '')}</div>
              <div style="font-size:.7rem;color:#aaa;margin-top:2px">${n.receivedAt ? new Date(n.receivedAt).toLocaleTimeString('en-IN',{hour:'2-digit',minute:'2-digit'}) : ''}</div>
            </div>
          </div>`).join('')
        : `<div style="padding:24px;text-align:center;color:#aaa;font-size:.85rem">
            <div style="font-size:2rem;margin-bottom:8px">🔕</div>No notifications yet
          </div>`;

      panel.innerHTML = `
        <div style="padding:12px 14px;background:#f8f9fa;border-bottom:1px solid #e8e8e8;
             display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:700;font-size:.9rem;color:#1a1a2e">🔔 Notifications</span>
          <button onclick="document.getElementById('ygc-notif-panel').remove()"
            style="background:none;border:none;cursor:pointer;color:#aaa;font-size:1.1rem;line-height:1">✕</button>
        </div>
        <div style="max-height:320px;overflow-y:auto">${items}</div>`;

      document.body.appendChild(panel);

      // Close on outside click
      setTimeout(() => {
        document.addEventListener('click', function handler(e) {
          if (!panel.contains(e.target) && e.target !== btn) {
            panel.remove();
            document.removeEventListener('click', handler);
          }
        });
      }, 50);
    });
  }

  // ─── Web Push Permission Request ──────────────────────────────────────────
  async function requestNotificationPermission() {
    if (!('Notification' in window)) return 'unsupported';
    if (Notification.permission === 'granted') return 'granted';
    if (Notification.permission === 'denied') return 'denied';

    // Show custom prompt before browser prompt for better UX
    return new Promise(resolve => {
      const prompt = document.createElement('div');
      prompt.innerHTML = `
        <div style="position:fixed;top:20px;left:50%;transform:translateX(-50%);z-index:99999;
             background:#fff;border-radius:16px;padding:20px;width:90%;max-width:380px;
             box-shadow:0 8px 40px rgba(0,0,0,.25);font-family:system-ui,sans-serif">
          <div style="font-size:2rem;text-align:center;margin-bottom:8px">🔔</div>
          <div style="font-weight:700;text-align:center;color:#1a1a2e;margin-bottom:4px">Stay Updated</div>
          <div style="text-align:center;color:#666;font-size:.85rem;margin-bottom:16px">
            Allow notifications to receive payment reminders, bid results, and important alerts.
          </div>
          <div style="display:flex;gap:8px">
            <button id="ygc-notif-allow" style="flex:1;background:#f0a500;color:#000;border:none;
              padding:10px;border-radius:8px;font-weight:700;cursor:pointer">Allow</button>
            <button id="ygc-notif-deny" style="flex:1;background:#f0f2f5;color:#666;border:none;
              padding:10px;border-radius:8px;cursor:pointer">Not Now</button>
          </div>
        </div>`;
      document.body.appendChild(prompt);

      document.getElementById('ygc-notif-allow').addEventListener('click', async () => {
        prompt.remove();
        const perm = await Notification.requestPermission();
        resolve(perm);
      });

      document.getElementById('ygc-notif-deny').addEventListener('click', () => {
        prompt.remove();
        resolve('denied');
      });
    });
  }

  // ─── Mobile: Bottom Navigation ────────────────────────────────────────────
  function injectBottomNav() {
    if (!isMobile()) return;
    if (document.getElementById('ygc-bottom-nav')) return;

    // Determine current section from URL
    const path = window.location.pathname;
    const isAdmin = path.startsWith('/admin');
    const isGuest = path === '/login' || path === '/register' || path === '/' ;

    if (isGuest) return;

    const adminLinks = [
      { href: '/admin/dashboard', icon: 'bi-speedometer2', label: 'Dashboard' },
      { href: '/admin/members',   icon: 'bi-people',       label: 'Members' },
      { href: '/admin/payments',  icon: 'bi-cash-coin',    label: 'Payments' },
      { href: '/admin/auctions',  icon: 'bi-hammer',       label: 'Auctions' },
      { href: '/admin/chits',     icon: 'bi-collection',   label: 'Chits' },
    ];

    const memberLinks = [
      { href: '/member/dashboard', icon: 'bi-house',          label: 'Home' },
      { href: '/member/chits',     icon: 'bi-collection',     label: 'Chits' },
      { href: '/member/help',      icon: 'bi-question-circle', label: 'Help' },
      { href: '/terms',            icon: 'bi-file-text',      label: 'Terms' },
      { href: '/logout',           icon: 'bi-box-arrow-left', label: 'Logout' },
    ];

    const links = isAdmin ? adminLinks : memberLinks;

    const nav = document.createElement('nav');
    nav.id = 'ygc-bottom-nav';
    nav.innerHTML = links.map(l => `
      <a href="${l.href}" class="ygc-bnav-item ${path === l.href || path.startsWith(l.href + '/') ? 'active' : ''}">
        <i class="bi ${l.icon}"></i>
        <span>${l.label}</span>
      </a>`).join('');

    document.body.appendChild(nav);

    // Add safe area padding to main content
    const main = document.querySelector('.main-content');
    if (main) main.style.paddingBottom = 'calc(70px + env(safe-area-inset-bottom))';
  }

  function isMobile() {
    return window.innerWidth < 768;
  }

  // ─── Mobile: Sidebar Toggle ───────────────────────────────────────────────
  function injectMobileSidebarToggle() {
    const sidebar = document.querySelector('.sidebar, .member-sidebar');
    if (!sidebar) return;
    if (document.getElementById('ygc-mob-toggle')) return;

    const toggle = document.createElement('button');
    toggle.id = 'ygc-mob-toggle';
    toggle.innerHTML = '<i class="bi bi-list"></i>';
    toggle.setAttribute('aria-label', 'Open menu');

    toggle.addEventListener('click', () => {
      sidebar.classList.toggle('sidebar-open');
      // Overlay
      let overlay = document.getElementById('ygc-sidebar-overlay');
      if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'ygc-sidebar-overlay';
        overlay.addEventListener('click', () => {
          sidebar.classList.remove('sidebar-open');
          overlay.remove();
        });
        document.body.appendChild(overlay);
      } else {
        overlay.remove();
      }
    });

    document.body.appendChild(toggle);
  }

  // ─── Offline Detection Banner ─────────────────────────────────────────────
  function initOfflineDetection() {
    const show = () => {
      if (document.getElementById('ygc-offline-bar')) return;
      const bar = document.createElement('div');
      bar.id = 'ygc-offline-bar';
      bar.textContent = '📡 You are offline — some features may not work';
      document.body.prepend(bar);
    };

    const hide = () => {
      const bar = document.getElementById('ygc-offline-bar');
      if (bar) {
        bar.textContent = '✅ Back online!';
        bar.style.background = '#28a745';
        setTimeout(() => bar.remove(), 2500);
      }
    };

    if (!navigator.onLine) show();
    window.addEventListener('offline', show);
    window.addEventListener('online', hide);
  }

  // ─── Touch Improvements ───────────────────────────────────────────────────
  function initTouchImprovements() {
    // Prevent double-tap zoom on buttons/links
    document.querySelectorAll('button, .btn, a.nav-link').forEach(el => {
      el.addEventListener('touchend', e => { e.preventDefault(); el.click(); }, { passive: false });
    });

    // Fast tap — remove 300ms delay
    if ('ontouchstart' in window) {
      document.documentElement.style.touchAction = 'manipulation';
    }
  }

  // ─── Keyboard handling for iOS ────────────────────────────────────────────
  function initKeyboardHandling() {
    const isIOS = /iphone|ipad|ipod/i.test(navigator.userAgent);
    if (!isIOS) return;

    // Fix scroll position when keyboard opens/closes
    document.querySelectorAll('input, textarea, select').forEach(el => {
      el.addEventListener('focus', () => {
        setTimeout(() => {
          el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 300);
      });
    });
  }

  // ─── Active link highlighting ──────────────────────────────────────────────
  function highlightActiveNav() {
    const path = window.location.pathname;
    document.querySelectorAll('.nav-link, .sidebar-nav a').forEach(link => {
      const href = link.getAttribute('href');
      if (href && path.startsWith(href) && href !== '/') {
        link.classList.add('active');
      }
    });
  }

  // ─── Utility ──────────────────────────────────────────────────────────────
  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  // ─── Init ─────────────────────────────────────────────────────────────────
  function init() {
    registerSW();
    showIOSInstallHint();
    initOfflineDetection();
    connectSSE();
    injectBottomNav();
    injectMobileSidebarToggle();
    initKeyboardHandling();
    highlightActiveNav();
    initBellIcon();

    // Request notification permission after a short delay on authenticated pages
    const isAuthPage = document.querySelector('.sidebar, .member-sidebar');
    if (isAuthPage) {
      setTimeout(async () => {
        if (Notification.permission === 'default') {
          await requestNotificationPermission();
        }
      }, 3000);
    }

    // Re-inject bottom nav on resize
    window.addEventListener('resize', () => {
      const existing = document.getElementById('ygc-bottom-nav');
      if (isMobile() && !existing) injectBottomNav();
      if (!isMobile() && existing) existing.remove();
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})();
