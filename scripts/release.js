// Готовит выпуск для обновления из приложения: пересчитывает version.json
// (номер версии из index.html, что нового, список файлов с контрольными суммами).
// Запуск: npm run release. После этого файлы можно выкладывать в GitHub.
const fs = require('fs'), path = require('path'), crypto = require('crypto');
const root = path.join(__dirname, '..');
const html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
const version = (html.match(/var VERSION='([^']+)'/) || [])[1];
if (!version) throw new Error('не нашёл VERSION в index.html');
const chlog = html.slice(html.indexOf('var CHANGELOG=['));
const top = (chlog.match(/\{v:'([^']+)',items:\[(.*?)\]\}/) || []);
if (top[1] !== version) throw new Error('верхняя запись CHANGELOG (' + top[1] + ') не совпадает с VERSION (' + version + ')');
const notes = JSON.parse('[' + top[2].replace(/'((?:[^'\\]|\\.)*)'/g, (m, s) => JSON.stringify(s.replace(/\\'/g, "'"))) + ']');
const pkg = JSON.parse(fs.readFileSync(path.join(root, 'package.json'), 'utf8'));
const list = ['index.html', 'manifest.webmanifest', 'icon-192-v2.png', 'icon-512-v2.png'];
for (const f of fs.readdirSync(path.join(root, 'fonts')).sort()) list.push('fonts/' + f);
const files = list.map(p => {
  const b = fs.readFileSync(path.join(root, p));
  return { p, h: crypto.createHash('sha256').update(b).digest('hex'), s: b.length };
});
const out = { version, minApk: pkg.minApk || 1, notes, files };
fs.writeFileSync(path.join(root, 'version.json'), JSON.stringify(out, null, 1) + '\n');
console.log('version.json: версия', version, '· файлов', files.length, '· нужен APK не ниже', out.minApk);
