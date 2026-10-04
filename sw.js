// Кэш нужен, чтобы приложение открывалось без интернета.
var CACHE='fin-v3',FONTS='fin-fonts';
var FILES=['./','index.html','manifest.webmanifest','icon-192-v2.png','icon-512-v2.png'];
self.addEventListener('install',function(e){
  e.waitUntil(caches.open(CACHE).then(function(c){return c.addAll(FILES);}));
  self.skipWaiting();
});
self.addEventListener('activate',function(e){
  e.waitUntil(caches.keys().then(function(keys){
    return Promise.all(keys.filter(function(k){return k!==CACHE&&k!==FONTS;}).map(function(k){return caches.delete(k);}));
  }));
  self.clients.claim();
});
self.addEventListener('fetch',function(e){
  var req=e.request;if(req.method!=='GET')return;
  var url=new URL(req.url);
  // шрифты Google: сначала из кэша, чтобы без интернета был тот же шрифт
  if(url.hostname==='fonts.googleapis.com'||url.hostname==='fonts.gstatic.com'){
    e.respondWith(caches.open(FONTS).then(function(c){
      return c.match(req).then(function(hit){
        return hit||fetch(req).then(function(res){if(res.ok||res.type==='opaque')c.put(req,res.clone());return res;});
      });
    }));
    return;
  }
  if(url.origin!==location.origin)return; // курсы валют идут напрямую
  if(/version\.json$/.test(url.pathname))return; // проверка обновлений — всегда из сети, в кэш не кладём
  // ключ без ?параметров, чтобы не копились копии одной страницы
  var key=url.origin+url.pathname;
  // сначала сеть (чтобы обновления доходили), если сети нет — из кэша
  e.respondWith(fetch(req).then(function(res){
    if(res.ok){var copy=res.clone();caches.open(CACHE).then(function(c){c.put(key,copy);});}
    return res;
  }).catch(function(){return caches.match(key).then(function(r){return r||caches.match('./');});}));
});
