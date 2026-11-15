// Drives every generated HTML page in the machine's Chrome (CDP) with real mouse and keyboard events.
const { chromium } = require('playwright-core'); const fs = require('fs'); const path = require('path');
const SHA = 'a5da4ee6bb4261392ac0a44a0756b82914b97fe8'; const ROOT = '/home/ubuntu/repos/ts-cobol-carddemo';
const pages = process.argv.slice(2); const out = [];
(async () => {
  const b = await chromium.connectOverCDP('http://localhost:29229'); const ctx = b.contexts()[0];
  for (const rel of pages) {
    const f = path.join(ROOT, rel); const p = await ctx.newPage(); await p.setViewportSize({ width: 1400, height: 900 }).catch(() => {});
    const errs = [], failed = [];
    p.on('console', m => { if (m.type() === 'error') errs.push(m.text().slice(0, 200)); });
    p.on('pageerror', e => errs.push('pageerror: ' + String(e).slice(0, 200)));
    p.on('requestfailed', r => failed.push(r.url().slice(-80)));
    p.on('response', r => { if (r.status() >= 400) failed.push(r.status() + ' ' + r.url().slice(-80)); });
    const r = { page: rel }; const t0 = Date.now();
    await p.goto('file://' + f); await p.waitForTimeout(800);
    const vis = async (sel) => p.evaluate(s => [...document.querySelectorAll(s)].map((e, i) => e.offsetParent !== null || e.getClientRects().length ? i : -1).filter(i => i >= 0), sel);
    const click = async (loc) => { await loc.scrollIntoViewIfNeeded({ timeout: 2000 }); const bb = await loc.boundingBox(); if (!bb) throw new Error('nobox');
      await p.mouse.click(bb.x + Math.min(bb.width / 2, 40), bb.y + bb.height / 2); };
    // tabs (role=tab) first
    let n = await p.locator('[role=tab]').count(), ok = 0, bad = [];
    for (let i = 0; i < n; i++) { try { const l = p.locator('[role=tab]').nth(i); await click(l); await p.waitForTimeout(60);
      const sel = await l.getAttribute('aria-selected'); const pan = await l.getAttribute('aria-controls'); const shown = pan ? await p.evaluate(id => { const e = document.getElementById(id); return !!e && !e.hidden && e.innerText.length > 20; }, pan) : true;
      if (sel === 'true' && shown) ok++; else bad.push(i); } catch (e) { bad.push(i + ':' + e.message.slice(0, 40)); } }
    r.tabs = { total: n, ok, bad }; if (n) { await click(p.locator('[role=tab]').first()); 
      await p.locator('[role=tab]').first().focus(); await p.keyboard.press('ArrowRight'); r.tabKeyboard = await p.locator('[role=tab]').nth(1).getAttribute('aria-selected') === 'true'; await p.keyboard.press('ArrowLeft'); }
    // collapsibles: toggle every summary reachable (opening one may reveal more), twice
    ok = 0; bad = []; let seen = 0;
    for (let pass = 0; pass < 6; pass++) { const idx = await vis('summary'); let did = 0;
      for (const i of idx) { const l = p.locator('summary').nth(i); const done = await l.evaluate(e => e.dataset.chk === '1'); if (done) continue; did++; seen++;
        try { const before = await l.evaluate(e => e.parentElement.open); await click(l); await p.waitForTimeout(25); const mid = await l.evaluate(e => e.parentElement.open);
          await click(l); await p.waitForTimeout(25); const after = await l.evaluate(e => e.parentElement.open);
          if (mid !== before && after === before) ok++; else bad.push(i); if (!after) { await click(l); }
          await l.evaluate(e => e.dataset.chk = '1'); } catch (e) { bad.push(i + ':' + e.message.slice(0, 30)); await l.evaluate(e => e.dataset.chk = '1').catch(() => {}); } }
      if (!did) break; }
    r.collapsibles = { total: await p.locator('summary').count(), toggled: seen, ok, bad: bad.slice(0, 8), badCount: bad.length };
    // text filters: type with the keyboard, expect the page text to change, clear, expect it back
    ok = 0; bad = []; let idx = await vis('input[type=text],input[type=search],input:not([type])');
    for (const i of idx) { const l = p.locator('input[type=text],input[type=search],input:not([type])').nth(i);
      try { const scope = l.locator('xpath=ancestor::*[self::section or self::details or self::main][1]'); const t0 = (await scope.innerText()).length; await click(l); await p.keyboard.type('zqzq', { delay: 5 }); await p.waitForTimeout(60);
        const t1 = (await scope.innerText()).length; await p.keyboard.press('Control+a'); await p.keyboard.press('Backspace'); await p.waitForTimeout(60); const t2 = (await scope.innerText()).length;
        if (t1 !== t0 && t2 === t0) ok++; else bad.push(i + ':' + t0 + '/' + t1 + '/' + t2); } catch (e) { bad.push(i + ':' + e.message.slice(0, 30)); } }
    r.textFilters = { visible: idx.length, ok, bad: bad.slice(0, 8), badCount: bad.length };
    // checkboxes
    ok = 0; bad = []; idx = await vis('input[type=checkbox]');
    for (const i of idx) { const l = p.locator('input[type=checkbox]').nth(i); try { const a = await l.isChecked(); await click(l); const m = await l.isChecked(); await click(l); const z = await l.isChecked(); if (m !== a && z === a) ok++; else bad.push(i); } catch (e) { bad.push(i); } }
    r.checkboxes = { visible: idx.length, ok, bad };
    // selects: keyboard (focus by mouse press on the label side is flaky for native popups, so focus + arrow keys)
    ok = 0; bad = []; let single = 0; idx = await vis('select');
    for (const i of idx) { const l = p.locator('select').nth(i);
      try { const no = await l.evaluate(e => e.options.length); if (no < 2) { single++; continue; } await l.scrollIntoViewIfNeeded({ timeout: 2000 }); await l.focus(); const a = await l.inputValue();
        await p.keyboard.press('ArrowDown'); await p.waitForTimeout(30); const m = await p.locator('select').nth(i).inputValue().catch(() => '?');
        await p.locator('select').nth(i).focus(); await p.keyboard.press('ArrowUp'); await p.waitForTimeout(30); const z = await p.locator('select').nth(i).inputValue().catch(() => '?');
        if (m !== a && z === a) ok++; else bad.push(i + ':' + a + '>' + m + '>' + z); } catch (e) { bad.push(i + ':' + e.message.slice(0, 30)); } }
    r.selects = { visible: idx.length, oneOption: single, ok, bad: bad.slice(0, 8), badCount: bad.length };
    // buttons that are not tabs: mouse click each, no error allowed
    ok = 0; bad = []; const e0 = errs.length; idx = await vis('button:not([role=tab])');
    for (const i of idx) { const l = p.locator('button:not([role=tab])').nth(i); try { if (await l.isDisabled()) { ok++; continue; } await click(l); await p.waitForTimeout(15); ok++; } catch (e) { bad.push(i + ':' + e.message.slice(0, 30)); } }
    r.buttons = { visible: idx.length, clicked: ok, bad: bad.slice(0, 8), badCount: bad.length, errorsRaised: errs.length - e0 };
    // links
    await p.goto('file://' + f); await p.waitForTimeout(600);
    const links = await p.evaluate(() => [...document.querySelectorAll('a[href]')].map(a => a.getAttribute('href')));
    const ids = new Set(await p.evaluate(() => [...document.querySelectorAll('[id]')].map(e => e.id))); const broken = [];
    for (const h of new Set(links)) { if (h.startsWith('#')) { if (h.length > 1 && !ids.has(decodeURIComponent(h.slice(1)))) broken.push(h); }
      else if (/^https?:/.test(h)) broken.push('external ' + h); else { const t = path.resolve(path.dirname(f), decodeURIComponent(h.split('#')[0])); if (!fs.existsSync(t)) broken.push(h); } }
    const fileLinks = [...new Set(links.filter(h => !h.startsWith('#')))]; let clicked = 0, clickBad = [];
    for (const h of fileLinks.slice(0, 8)) { try { const l = p.locator('a[href="' + h + '"]').first(); if (!(await l.isVisible())) { const s = p.locator('summary'); for (let k = 0; k < await s.count() && !(await l.isVisible()); k++) { if (!(await s.nth(k).evaluate(e => e.parentElement.open)) && await s.nth(k).isVisible()) await click(s.nth(k)); } }
        await click(l); await p.waitForTimeout(500); const u = decodeURIComponent(p.url()); if (u.includes(path.basename(h.split('#')[0]))) clicked++; else clickBad.push(h + ' -> ' + u.slice(-40));
        await p.goto('file://' + f); await p.waitForTimeout(400); } catch (e) { clickBad.push(h + ':' + e.message.slice(0, 40)); await p.goto('file://' + f).catch(() => {}); } }
    const anchors = [...new Set(links.filter(h => h.startsWith('#') && h.length > 1))]; let aok = 0;
    for (const h of anchors.slice(0, 10)) { try { const l = p.locator('a[href="' + h + '"]').first(); if (await l.isVisible()) { await click(l); await p.waitForTimeout(80); if (p.url().endsWith(h)) aok++; } else aok++; } catch (e) {} }
    r.links = { total: links.length, distinct: new Set(links).size, broken, fileLinksClicked: clicked + '/' + Math.min(fileLinks.length, 8), clickBad, anchorsClicked: aok + '/' + Math.min(anchors.length, 10) };
    await p.goto('file://' + f); await p.waitForTimeout(800);
    r.diagrams = await p.evaluate(() => [...document.querySelectorAll('svg')].map(s => { const b = s.getBoundingClientRect(); return { w: Math.round(b.width), h: Math.round(b.height), shapes: s.querySelectorAll('rect,circle,path,line,text').length }; }).filter(d => d.shapes > 3));
    r.sha = await p.evaluate(s => document.body.innerText.includes(s), SHA);
    r.selfcheck = await p.evaluate(() => { const e = document.getElementById('selfcheck'); return e ? e.innerText : null; });
    r.title = await p.title(); r.console = errs.slice(0, 5); r.consoleCount = errs.length; r.failedRequests = failed.slice(0, 5);
    const shot = '/home/ubuntu/logs/shots/' + rel.replace(/^store\/reports\//, '').replace(/^modernization\/legacy\//, '').replace(/\//g, '__').replace('.html', '.png');
    await p.screenshot({ path: shot }); r.shot = shot; r.secs = Math.round((Date.now() - t0) / 1000);
    out.push(r); console.log(JSON.stringify(r)); await p.close();
  }
  await b.close();
})();
