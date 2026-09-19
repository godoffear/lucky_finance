// Кэш нужен, чтобы приложение открывалось без интернета.
var CACHE='fin-v1';
var FILES=['./','index.html','manifest.webmanifest','icon-192.png','icon-512.png'];
self.addEventListener('install',function(e){
  e.waitUntil(caches.open(CACHE).then(function(c){return c.addAll(FILES);}));
  self.skipWaiting();
});
self.addEventListener('activate',function(e){
  e.waitUntil(caches.keys().then(function(keys){
    return Promise.all(keys.filter(function(k){return k!==CACHE;}).map(function(k){return caches.delete(k);}));
  }));
  self.clients.claim();
});
self.addEventListener('fetch',function(e){
  var req=e.request;
  if(req.method!=='GET'||new URL(req.url).origin!==location.origin)return; // курсы ЦБ и шрифты идут напрямую
  // сначала сеть (чтобы обновления доходили), если сети нет — из кэша
  e.respondWith(fetch(req).then(function(res){
    var copy=res.clone();caches.open(CACHE).then(function(c){c.put(req,copy);});
    return res;
  }).catch(function(){return caches.match(req).then(function(r){return r||caches.match('./');});}));
});
