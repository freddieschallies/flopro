// FloPro Job Cards service worker: works offline, always fetches the newest version when online.
const BUILD = "__BUILD__";
const CACHE = "flopro-" + BUILD;
const CORE = ["./", "index.html", "manifest.webmanifest", "icons/icon-192.png", "icons/icon-512.png"];

self.addEventListener("install", e => {
  self.skipWaiting();
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(CORE)).catch(() => {}));
});

self.addEventListener("activate", e => {
  e.waitUntil(
    caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

// Network first (so updates arrive straight away), cache as fallback when offline.
self.addEventListener("fetch", e => {
  const url = new URL(e.request.url);
  if (e.request.method !== "GET" || url.origin !== location.origin) return;
  if (url.pathname.endsWith("version.json")) return; // always live
  e.respondWith(
    fetch(e.request, { cache: "no-store" })
      .then(res => {
        if (res.ok) { const copy = res.clone(); caches.open(CACHE).then(c => c.put(e.request, copy)); }
        return res;
      })
      .catch(() => caches.match(e.request).then(r => r || caches.match("index.html")))
  );
});
