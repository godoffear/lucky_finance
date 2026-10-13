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
// Данные об APK: при --with-apk берём свежую сборку, иначе сохраняем прежние из version.json
let apk;
if (process.argv.includes('--with-apk')) {
  const src = path.join(root, 'android/app/build/outputs/apk/debug/app-debug.apk');
  fs.mkdirSync(path.join(root, 'apk'), { recursive: true });
  fs.copyFileSync(src, path.join(root, 'apk/LuckyFinance.apk'));
  const b = fs.readFileSync(path.join(root, 'apk/LuckyFinance.apk'));
  const gradle = fs.readFileSync(path.join(root, 'android/app/build.gradle'), 'utf8');
  apk = { p: 'apk/LuckyFinance.apk', h: crypto.createHash('sha256').update(b).digest('hex'), s: b.length, build: +(gradle.match(/versionCode (\d+)/) || [])[1] };
} else {
  try { apk = JSON.parse(fs.readFileSync(path.join(root, 'version.json'), 'utf8')).apk; } catch (e) {}
}
const out = { version, minApk: pkg.minApk || 1, notes, files };
if (apk) out.apk = apk;
fs.writeFileSync(path.join(root, 'version.json'), JSON.stringify(out, null, 1) + '\n');
console.log(apk ? 'APK в version.json: сборка ' + apk.build + ', ' + apk.s + ' байт' : 'APK в version.json нет');
console.log('version.json: версия', version, '· файлов', files.length, '· нужен APK не ниже', out.minApk);
