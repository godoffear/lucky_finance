// Скачивает шрифты Onest и Unbounded в папку fonts/ (один раз; результат лежит в репозитории).
// Оставляем только нужные наборы знаков: кириллица, латиница, расширенная латиница и вьетнамский (для донгов).
const fs = require('fs'), path = require('path');
const URL_ = 'https://fonts.googleapis.com/css2?family=Onest:wght@400;500;600;700&family=Unbounded:wght@500;600&display=swap';
const UA = 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36';
const KEEP = [/^U\+0301, U\+0400-045F/, /^U\+0000-00FF/, /^U\+0100-02BA/, /^U\+0102-0103/];
(async () => {
  const dir = path.join(__dirname, '..', 'fonts');
  fs.rmSync(dir, { recursive: true, force: true }); fs.mkdirSync(dir, { recursive: true });
  const css = await (await fetch(URL_, { headers: { 'User-Agent': UA } })).text();
  const blocks = css.match(/@font-face\s*\{[^}]*\}/g) || [];
  let out = '', n = 0;
  for (let b of blocks) {
    const range = (b.match(/unicode-range:\s*([^;]+);/) || [])[1] || '';
    if (!KEEP.some(r => r.test(range))) continue;
    const u = (b.match(/url\((https:[^)]+\.woff2)\)/) || [])[1];
    const name = path.basename(u);
    if (!fs.existsSync(path.join(dir, name))) { fs.writeFileSync(path.join(dir, name), Buffer.from(await (await fetch(u)).arrayBuffer())); n++; }
    out += b.replace(u, name) + '\n';
  }
  fs.writeFileSync(path.join(dir, 'fonts.css'), out);
  console.log('шрифтовых файлов:', n);
})();
