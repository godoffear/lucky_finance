// Собирает папку www для Capacitor: копирует файлы приложения вместе со шрифтами.
const fs = require('fs'), path = require('path');
const root = path.join(__dirname, '..'), www = path.join(root, 'www');
fs.rmSync(www, { recursive: true, force: true });
fs.mkdirSync(www, { recursive: true });
for (const f of ['index.html', 'manifest.webmanifest', 'icon-192-v2.png', 'icon-512-v2.png'])
  fs.copyFileSync(path.join(root, f), path.join(www, f));
fs.cpSync(path.join(root, 'fonts'), path.join(www, 'fonts'), { recursive: true });
console.log('www готова');
