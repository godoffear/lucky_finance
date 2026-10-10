// Быстрая проверка: приложение открывается без ошибок. Запуск: python3 -m http.server 8765 & node tools/smoke.js
const {chromium}=require('playwright');
(async()=>{
  const b=await chromium.launch(),p=await b.newPage({viewport:{width:390,height:900}}),errs=[];
  p.on('pageerror',e=>errs.push(e.message));
  await p.route(/api\.github\.com|currency-api|cbr-xml/,r=>r.abort());
  await p.goto('http://localhost:8765/index.html');await p.waitForTimeout(800);
  console.log(errs.length?'ОШИБКИ: '+errs.join('; '):'OK, ошибок нет');
  await b.close();process.exit(errs.length?1:0);
})();
