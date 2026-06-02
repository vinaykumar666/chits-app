/**
 * YGC Internal - Service Worker
 * Handles: caching, offline support, background sync, push notifications
 */

const CACHE_NAME = 'ygc-v2.1';
const STATIC_CACHE = 'ygc-static-v2.1';
const RUNTIME_CACHE = 'ygc-runtime-v2.1';

// Assets to pre-cache at install time
const PRECACHE_ASSETS = [
  '/',
  '/login',
  '/css/app.css',
  '/css/responsive.css',
  '/js/app.js',
  '/offline.html',
  'https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css',
  'https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css',
  'https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js'
];

// Install: pre-cache critical assets
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(STATIC_CACHE)
      .then(cache => cache.addAll(PRECACHE_ASSETS).catch(() => {}))
      .then(() => self.skipWaiting())
  );
});

// Activate: clean up old caches
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(
        keys.filter(key => key !== STATIC_CACHE && key !== RUNTIME_CACHE)
            .map(key => caches.delete(key))
      )
    ).then(() => self.clients.claim())
  );
});

// Fetch: network-first for API, cache-first for static
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);

  // Skip SSE (EventSource) — never cache
  if (url.pathname.includes('/api/notifications/subscribe')) return;
  // Skip non-GET
  if (event.request.method !== 'GET') return;
  // Skip chrome-extension etc
  if (!url.protocol.startsWith('http')) return;

  // API calls: network-first, no cache
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(
      fetch(event.request).catch(() =>
        new Response(JSON.stringify({ error: 'Offline' }), {
          headers: { 'Content-Type': 'application/json' }
        })
      )
    );
    return;
  }

  // HTML pages: network-first with offline fallback
  if (event.request.headers.get('Accept')?.includes('text/html')) {
    event.respondWith(
      fetch(event.request)
        .then(response => {
          const clone = response.clone();
          caches.open(RUNTIME_CACHE).then(c => c.put(event.request, clone));
          return response;
        })
        .catch(async () => {
          const cached = await caches.match(event.request);
          return cached || caches.match('/offline.html');
        })
    );
    return;
  }

  // Static assets: cache-first
  event.respondWith(
    caches.match(event.request).then(cached => {
      if (cached) return cached;
      return fetch(event.request).then(response => {
        if (response.ok) {
          const clone = response.clone();
          caches.open(RUNTIME_CACHE).then(c => c.put(event.request, clone));
        }
        return response;
      });
    })
  );
});

// ─── Push Notification Handler ────────────────────────────────────────────────
self.addEventListener('push', event => {
  let data = {};
  try {
    data = event.data?.json() || {};
  } catch (e) {
    data = { title: 'YGC Alert', message: event.data?.text() || 'New notification' };
  }

  const typeIconMap = {
    CHIT_REGISTRATION_APPROVED: '✅',
    CHIT_REGISTRATION_REJECTED: '❌',
    BID_WINDOW_OPEN:            '🔔',
    BID_SUBMITTED:              '📋',
    BID_WINNER_ANNOUNCED:       '🏆',
    PAYMENT_REMINDER:           '💰',
    PAYMENT_DUE_ALERT:          '⚠️',
    CHIT_MATURITY:              '🎉',
    AGREEMENT_APPROVED:         '📄'
  };

  const icon = typeIconMap[data.type] || '🔔';
  const options = {
    body: data.message || 'You have a new notification',
    icon: '/icons/icon-192.png',
    badge: '/icons/icon-72.png',
    tag: data.type || 'ygc-notification',
    renotify: true,
    requireInteraction: ['PAYMENT_DUE_ALERT', 'BID_WINNER_ANNOUNCED'].includes(data.type),
    vibrate: [200, 100, 200],
    data: {
      url: data.url || '/',
      notificationId: data.id,
      type: data.type
    },
    actions: [
      { action: 'view', title: 'View Details' },
      { action: 'dismiss', title: 'Dismiss' }
    ]
  };

  event.waitUntil(
    self.registration.showNotification(`${icon} ${data.title || 'YGC Internal'}`, options)
  );
});

// Handle notification click
self.addEventListener('notificationclick', event => {
  event.notification.close();

  if (event.action === 'dismiss') return;

  const url = event.notification.data?.url || '/';
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clientList => {
      // Focus existing tab if open
      for (const client of clientList) {
        if (client.url.includes(self.location.origin) && 'focus' in client) {
          client.navigate(url);
          return client.focus();
        }
      }
      // Open new window
      if (clients.openWindow) return clients.openWindow(url);
    })
  );
});

// Background sync for offline form submissions
self.addEventListener('sync', event => {
  if (event.tag === 'sync-payments') {
    event.waitUntil(syncOfflineData('pending-payments', '/api/payments'));
  }
  if (event.tag === 'sync-bids') {
    event.waitUntil(syncOfflineData('pending-bids', '/api/bids'));
  }
});

async function syncOfflineData(storeName, endpoint) {
  // Placeholder — actual IndexedDB sync would go here in production
  console.log(`[SW] Background sync triggered: ${storeName} → ${endpoint}`);
}
