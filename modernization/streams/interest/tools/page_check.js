// Drives stream-INTCALC.html in the machine's Chrome (CDP) with real mouse and keyboard events and
// compares what the page shows with a recount of manifest.json read from disk.
// Usage: NODE_PATH=<dir holding playwright-core> node page_check.js <baseline sha> <screenshot.png> <result.json>
const { chromium } = require('playwright-core'); const fs = require('fs'); const path = require('path');
const dir = path.resolve(__dirname, '..'); const pageFile = path.join(dir, 'stream-INTCALC.html');
const [sha, shotPath, resultPath] = process.argv.slice(2);
const manifest = JSON.parse(fs.readFileSync(path.join(dir, 'manifest.json'), 'utf8'));
const short = (dsn) => dsn.replace('AWS.M2.CARDDEMO.', '');
const uniq = (list) => [...new Set(list)];
const outside = manifest.outside, edges = manifest.edges, scope = manifest.scope;
const expectedCards = {
  jobs: scope.jobs.length, steps: scope.steps.length, programsInScope: scope.programs.filter(p => p.inScope).length,
  copybooks: scope.copybooks.length, datasets: uniq(scope.datasets.map(d => d.dsn)).length, edges: edges.length,
  edgesProven: edges.filter(e => e.status === 'PROVEN').length, edgesCandidate: edges.filter(e => e.status === 'CANDIDATE').length,
  setupJobs: manifest.setup.jobs.length, outside: outside.length,
  outsideJobs: uniq(outside.filter(o => o.actorKind === 'batch job step').map(o => o.actor.split('.')[0])).length,
  outsidePrograms: uniq(outside.filter(o => o.actorKind === 'CICS program').map(o => o.actor)).length,
  outsideTransactions: uniq(outside.filter(o => o.actorKind === 'CICS transaction').map(o => o.actor)).length,
  predecessors: manifest.scheduler.predecessors.length, successors: manifest.scheduler.successors.length,
  unprovenOrderings: manifest.scheduler.unproven.length, disagreements: manifest.views.disagreements.length,
  openAsks: manifest.openAsks.length };
const expectedRows = { jobs: scope.jobs.length, steps: scope.steps.length, programs: scope.programs.length,
  datasets: scope.datasets.length, edges: edges.length, setup: manifest.setup.jobs.length, outside: outside.length,
  'outside-matrix': uniq(outside.map(o => o.dataset)).length, exports: manifest.scheduler.exports.length,
  'folder-order': manifest.scheduler.folderOrder.length, predecessors: manifest.scheduler.predecessors.length,
  successors: manifest.scheduler.successors.length, unproven: manifest.scheduler.unproven.length, asks: manifest.openAsks.length };
scope.copybooks.forEach(c => { expectedRows['copybook-' + c.name] = c.fields.length; });
const filterData = {
  edges: edges.map(e => ({ status: e.status, kind: e.kind })),
  outside: outside.map(o => ({ status: o.status, dataset: short(o.dataset), actorkind: o.actorKind, direction: o.direction, relation: o.relation || '' })),
  asks: manifest.openAsks.map(a => ({ severity: a.severity })) };
const searchTerms = { edges: ['XREFFIL1', 'CEE3ABD', 'rewrite'], outside: ['POSTTRAN', 'COACTUPC', 'CLOSEFIL'], asks: ['rerun', 'calendar'] };
const checks = []; const detail = {};
const record = (name, pass, note) => { checks.push({ check: name, result: pass ? 'PASS' : 'FAIL', note }); };

(async () => {
  const browser = await chromium.connectOverCDP('http://localhost:29229'); const ctx = browser.contexts()[0];
  const p = await ctx.newPage(); await p.setViewportSize({ width: 1400, height: 900 }).catch(() => {});
  const errs = [], failed = [], requests = [];
  p.on('console', m => { if (m.type() === 'error' || m.type() === 'warning') errs.push(m.type() + ': ' + m.text().slice(0, 200)); });
  p.on('pageerror', e => errs.push('pageerror: ' + String(e).slice(0, 200)));
  p.on('request', r => requests.push(r.url()));
  p.on('requestfailed', r => failed.push(r.url().slice(-100)));
  p.on('response', r => { if (r.status() >= 400) failed.push(r.status() + ' ' + r.url().slice(-100)); });
  await p.goto('file://' + pageFile); await p.waitForTimeout(600);
  const click = async (loc) => { await loc.evaluate(e => e.scrollIntoView({ block: 'center' })); const bb = await loc.boundingBox(); if (!bb) throw new Error('no box');
    await p.mouse.click(bb.x + Math.min(bb.width / 2, 30), bb.y + bb.height / 2); };
  // a wrapped link has several boxes and a diagram link is a line or a label: click the middle of its first box,
  // scrolled clear of the sticky tab bar
  const clickLink = async (loc) => { await loc.evaluate(e => (e.querySelector('text') || e).scrollIntoView({ block: 'center' }));
    const r = await loc.evaluate(e => { const t = e.querySelector('text') || e; const b = t.getClientRects()[0]; return b ? { x: b.left + b.width / 2, y: b.top + b.height / 2 } : null; });
    if (!r) throw new Error('no box'); await p.mouse.click(r.x, r.y); };
  const tab = (key) => p.locator('#tabbtn-' + key);
  const panelOf = (id) => p.evaluate(i => { const e = document.getElementById(i); const s = e && e.closest('section[role=tabpanel]'); return s ? { panel: s.id, hidden: s.hidden } : null; }, id);
  const tabKeys = await p.locator('[role=tab]').evaluateAll(list => list.map(b => b.id.replace('tabbtn-', '')));

  // 1 tabs: mouse, then keyboard
  let bad = [];
  for (const key of tabKeys) { await click(tab(key)); const sel = await tab(key).getAttribute('aria-selected');
    const shown = await p.evaluate(k => { const e = document.getElementById('tab-' + k); return !e.hidden && e.innerText.length > 50; }, key);
    const others = await p.evaluate(k => [...document.querySelectorAll('section[role=tabpanel]')].filter(s => !s.hidden && s.id !== 'tab-' + k).length, key);
    if (sel !== 'true' || !shown || others) bad.push(key); }
  await click(tab(tabKeys[0])); const keyTrail = [];
  for (const key of ['ArrowRight', 'ArrowRight', 'ArrowLeft', 'End', 'ArrowRight', 'Home']) { await p.keyboard.press(key);
    keyTrail.push(await p.evaluate(() => document.querySelector('[role=tab][aria-selected=true]').id.replace('tabbtn-', ''))); }
  const wantTrail = [tabKeys[1], tabKeys[2], tabKeys[1], tabKeys[tabKeys.length - 1], tabKeys[0], tabKeys[0]];
  record('Tabs', !bad.length && JSON.stringify(keyTrail) === JSON.stringify(wantTrail),
    `${tabKeys.length} tabs clicked with the mouse (${tabKeys.length - bad.length} showed their panel alone); arrow, Home and End keys moved ${keyTrail.join(' > ')}`);

  // 2 collapsibles and expand/collapse buttons
  let toggled = 0, total = 0; bad = []; let buttons = 0;
  for (const key of tabKeys) { await click(tab(key)); const panel = p.locator('#tab-' + key);
    const n = await panel.locator('summary').count(); total += n;
    for (let i = 0; i < n; i++) { const s = panel.locator('summary').nth(i); const state = () => s.evaluate(e => e.parentElement.open);
      const a = await state(); await click(s); const b = await state(); await s.focus(); await p.keyboard.press('Enter'); const c = await state();
      if (a !== b && c === a) toggled++; else bad.push(key + ':' + i); }
    const exp = panel.locator('[data-expand]'), col = panel.locator('[data-collapse]');
    if (await exp.count()) { buttons += 2; await click(col); const closed = await panel.locator('details[open]').count();
      await click(exp); const open = await panel.locator('details:not([open])').count(); if (closed || open) bad.push(key + ':expand/collapse'); } }
  record('Collapsible sections', !bad.length && toggled === total,
    `${toggled} of ${total} opened and closed (mouse click, then Enter key); ${buttons} Expand all / Collapse all buttons worked` + (bad.length ? '; failed ' + bad.join(',') : ''));
  detail.collapsibles = { total, toggled };

  // 3 links: every anchor, tab by tab, with all details open
  const linkStats = { cite: 0, citeOk: 0, inPage: 0, inPageOk: 0, diagram: 0, diagramOk: 0, external: 0 }; bad = [];
  for (const key of tabKeys) { await click(tab(key)); const panel = p.locator('#tab-' + key);
    if (await panel.locator('[data-expand]').count()) await click(panel.locator('[data-expand]'));
    await panel.evaluate(e => e.querySelectorAll('details').forEach(d => { d.open = true; }));
    const n = await panel.locator('a').count();
    for (let i = 0; i < n; i++) { const a = panel.locator('a').nth(i);
      const info = await a.evaluate(e => ({ href: e.getAttribute('href') || '', cite: e.getAttribute('data-cite'), svg: !!e.closest('svg') }));
      if (!info.href.startsWith('#')) { linkStats.external++; bad.push('external ' + info.href); continue; }
      try { await clickLink(a); await p.waitForTimeout(15);
        if (info.cite) { linkStats.cite++;
          const got = await p.evaluate(() => { const d = document.getElementById('src'); return { open: d.open, title: document.getElementById('src-title').textContent,
            lines: document.querySelectorAll('#src-body .ln').length, text: document.getElementById('src-body').textContent.length }; });
          const want = manifest.excerpts[info.cite]; await p.keyboard.press('Escape');
          const closed = await p.evaluate(() => !document.getElementById('src').open);
          if (got.open && got.title === info.cite && want && got.lines === want.lines.length && got.text > 0 && closed) linkStats.citeOk++; else bad.push('cite ' + info.cite);
        } else { const id = info.href.slice(1); const where = await panelOf(id); const hash = await p.evaluate(() => location.hash);
          const ok = where && !where.hidden && hash === info.href;
          if (info.svg) { linkStats.diagram++; if (ok) linkStats.diagramOk++; else bad.push('diagram ' + info.href); }
          else { linkStats.inPage++; if (ok) linkStats.inPageOk++; else bad.push('link ' + info.href); }
          await click(tab(key)); await panel.evaluate(e => e.querySelectorAll('details').forEach(d => { d.open = true; })); }
      } catch (e) { bad.push(info.href + ' ' + (info.cite || '') + ' ' + e.message.slice(0, 40)); } } }
  await p.goto('file://' + pageFile + '#A4'); await p.waitForTimeout(300);
  const deep = await panelOf('A4'); const deepOk = deep && deep.panel === 'tab-asks' && !deep.hidden;
  const allLinks = linkStats.cite + linkStats.inPage + linkStats.diagram;
  record('Links', !bad.length && deepOk && linkStats.external === 0,
    `${allLinks} links clicked: ${linkStats.citeOk}/${linkStats.cite} source references opened the cited lines and closed with Esc, ` +
    `${linkStats.inPageOk}/${linkStats.inPage} in-page links and ${linkStats.diagramOk}/${linkStats.diagram} diagram links landed on a visible target; ` +
    `opening the file with #A4 lands on the ask; 0 links leave the file` + (bad.length ? '; failed: ' + bad.slice(0, 8).join(' | ') : ''));
  detail.links = linkStats;

  // 4 filters: every option of every drop-down by keyboard, search by typing, Clear by mouse
  const filterNotes = []; bad = [];
  for (const name of Object.keys(filterData)) { await click(tab(name)); const box = p.locator(`.filters[data-for="${name}"]`);
    const state = () => p.evaluate(nm => { const rows = [...document.querySelectorAll(`table[data-rows="${nm}"] tbody tr`)];
      const out = document.querySelector(`[data-showing="${nm}"]`); return { visible: rows.filter(r => r.getClientRects().length).length, total: rows.length, text: out.textContent }; }, name);
    let optionsTried = 0; const selects = await box.locator('select').count();
    for (let s = 0; s < selects; s++) { const sel = box.locator('select').nth(s); const field = await sel.getAttribute('data-filter');
      const values = await sel.evaluate(e => [...e.options].map(o => o.value)); await sel.focus();
      for (let v = 1; v < values.length; v++) { await p.keyboard.press('ArrowDown'); await p.waitForTimeout(15); optionsTried++;
        const want = filterData[name].filter(r => r[field] === values[v]).length; const got = await state(); const cur = await sel.inputValue();
        if (cur !== values[v] || got.visible !== want || got.text !== `showing ${want} of ${filterData[name].length}`) bad.push(`${name}.${field}=${values[v]} want ${want} got ${got.visible} "${got.text}"`); }
      await click(box.locator('[data-clear]')); const after = await state(); if (after.visible !== after.total) bad.push(name + ' clear'); }
    if (name === 'outside') { let combos = 0;
      for (const status of ['PROVEN', 'CANDIDATE']) for (const kind of ['batch job step', 'CICS program', 'CICS transaction']) {
        await box.locator('select[data-filter=status]').focus(); await p.keyboard.type(status.slice(0, 2));
        await box.locator('select[data-filter=actorkind]').focus(); await p.keyboard.type(kind === 'batch job step' ? 'b' : kind === 'CICS program' ? 'CICS p' : 'CICS t');
        await p.waitForTimeout(700); combos++;
        const want = filterData.outside.filter(r => r.status === status && r.actorkind === kind).length; const got = await state();
        if (got.visible !== want) bad.push(`outside ${status} x ${kind} want ${want} got ${got.visible}`); await click(box.locator('[data-clear]')); }
      filterNotes.push(`${combos} status x actor combinations`); }
    for (const term of searchTerms[name]) { await click(box.locator('input[type=search]')); await p.keyboard.type(term); await p.waitForTimeout(30);
      const res = await p.evaluate(([nm, t]) => { const rows = [...document.querySelectorAll(`table[data-rows="${nm}"] tbody tr`)]; const has = r => r.textContent.toLowerCase().includes(t.toLowerCase());
        return { visible: rows.filter(r => !r.hidden).length, wrongShown: rows.filter(r => !r.hidden && !has(r)).length, wrongHidden: rows.filter(r => r.hidden && has(r)).length }; }, [name, term]);
      if (!res.visible || res.wrongShown || res.wrongHidden) bad.push(`${name} search ${term} ${JSON.stringify(res)}`);
      await click(box.locator('[data-clear]')); const after = await state(); if (after.visible !== after.total) bad.push(name + ' clear after search'); }
    filterNotes.push(`${name}: ${optionsTried} drop-down options, ${searchTerms[name].length} searches, Clear`); }
  record('Filters', !bad.length, filterNotes.join('; ') + '; every "showing N of M" equals the recount of manifest.json' + (bad.length ? '; failed: ' + bad.slice(0, 6).join(' | ') : ''));

  // 5 diagrams
  await click(tab('overview'));
  const diagrams = await p.evaluate(() => [...document.querySelectorAll('svg.diagram')].map(s => { const b = s.getBoundingClientRect();
    return { id: s.id, w: Math.round(b.width), h: Math.round(b.height), boxes: s.querySelectorAll('rect.node').length, lines: s.querySelectorAll('line,path.es').length,
      emptyText: [...s.querySelectorAll('text')].filter(t => !t.textContent.trim() || !t.getBBox().width).length,
      outOfView: [...s.querySelectorAll('rect.node,text')].filter(e => { const r = e.getBoundingClientRect(); return r.left < b.left - 1 || r.right > b.right + 1 || r.top < b.top - 1 || r.bottom > b.bottom + 1; }).length,
      edgeIds: [...new Set([...s.querySelectorAll('a')].map(a => (a.getAttribute('href') || '').slice(1)).filter(h => /^[CDI]\d+$/.test(h)))].sort() }; }));
  const stream = diagrams.find(d => d.id === 'diagram-stream') || {}; const sched = diagrams.find(d => d.id === 'diagram-scheduler') || {};
  const edgeIds = edges.map(e => e.id).sort();
  const diagramOk = diagrams.length === 2 && diagrams.every(d => d.w > 700 && d.h > 150 && d.boxes > 5 && d.lines > 3 && !d.emptyText && !d.outOfView) &&
    JSON.stringify(stream.edgeIds) === JSON.stringify(edgeIds) && sched.boxes === manifest.setup.jobs.length + manifest.scheduler.folderOrder.length;
  record('Diagrams render', diagramOk, `2 inline SVG diagrams drawn (${stream.w}x${stream.h} with ${stream.boxes} boxes, ${sched.w}x${sched.h} with ${sched.boxes} boxes), no empty or clipped label; ` +
    `the stream diagram carries all ${edgeIds.length} edge ids of the manifest; the scheduler diagram has ${manifest.setup.jobs.length} setup and ${manifest.scheduler.folderOrder.length} folder jobs`);
  detail.diagrams = diagrams;

  // 6 counts
  bad = []; const cards = await p.evaluate(() => [...document.querySelectorAll('[data-count]')].map(c => [c.getAttribute('data-count'), Number(c.querySelector('b').textContent)]));
  for (const [key, shown] of cards) if (expectedCards[key] !== shown) bad.push(`card ${key} shows ${shown}, data ${expectedCards[key]}`);
  for (const key of Object.keys(expectedCards)) if (manifest.counts[key] !== expectedCards[key]) bad.push(`manifest.counts.${key}`);
  const rows = await p.evaluate(() => Object.fromEntries([...document.querySelectorAll('table[data-rows]')].map(t => [t.getAttribute('data-rows'), t.querySelectorAll('tbody tr').length])));
  for (const key of Object.keys(expectedRows)) if (rows[key] !== expectedRows[key]) bad.push(`table ${key} has ${rows[key]} rows, data ${expectedRows[key]}`);
  const matrix = await p.evaluate(() => [...document.querySelectorAll('table[data-rows="outside-matrix"] tbody tr')].map(r => [...r.cells].map(c => c.textContent.trim())));
  for (const m of matrix) { const sub = outside.filter(o => short(o.dataset) === m[0]);
    const want = [sub.filter(o => o.actorKind === 'batch job step').length, sub.filter(o => o.actorKind === 'CICS program').length, sub.filter(o => o.actorKind === 'CICS transaction').length,
      sub.filter(o => ['write', 'read-write', 'define', 'delete'].includes(o.direction)).length, sub.length];
    if (JSON.stringify(m.slice(1).map(Number)) !== JSON.stringify(want)) bad.push('matrix ' + m[0]); }
  const blocks = await p.evaluate(() => ({ views: document.querySelectorAll('#tab-views details.block').length, copybooks: document.querySelectorAll('#tab-scope details.block').length }));
  if (blocks.views !== manifest.views.disagreements.length || blocks.copybooks !== scope.copybooks.length) bad.push('detail blocks');
  const prose = await p.evaluate(() => document.querySelector('#tab-outside p').textContent.replace(/\s+/g, ' '));
  const wantProse = `${outside.length} rows: ${outside.filter(o => o.actorKind === 'batch job step').length} batch job steps`;
  if (!prose.includes(wantProse) || !prose.includes(`${outside.filter(o => o.status === 'PROVEN').length} PROVEN, ${outside.filter(o => o.status === 'CANDIDATE').length} CANDIDATE`)) bad.push('outside prose');
  const edgeProse = await p.evaluate(() => document.querySelector('#tab-edges p').textContent.replace(/\s+/g, ' '));
  if (!edgeProse.includes(`${expectedCards.edgesProven} PROVEN, ${expectedCards.edgesCandidate} CANDIDATE`)) bad.push('edges prose');
  record('Counts equal the data', !bad.length, `${cards.length} cards, ${Object.keys(expectedRows).length} tables, the dataset matrix and the PROVEN/CANDIDATE totals in the text equal a recount of manifest.json ` +
    `(e.g. ${edges.length} edges = ${expectedCards.edgesProven} PROVEN + ${expectedCards.edgesCandidate} CANDIDATE; ${outside.length} outside rows)` + (bad.length ? '; failed: ' + bad.join(' | ') : ''));

  // 7 baseline sha
  const shaShown = await p.evaluate(() => { const e = document.getElementById('sha'); return { text: e.textContent, visible: e.getClientRects().length > 0 }; });
  await p.evaluate(() => window.scrollTo(0, 0));
  record('Baseline SHA shown', shaShown.text === sha && shaShown.visible && manifest.meta.baseline.sha === sha, `header shows ${shaShown.text}, the head of demo/01-intake`);

  // 8 load: console and assets (after all the driving above)
  const assets = requests.filter(u => !u.startsWith('file://' + pageFile));
  const html = fs.readFileSync(pageFile, 'utf8'); const externalRefs = (html.match(/(src|href)="(https?:|\/\/|[^#"][^"]*\.(js|css|png|svg|woff2?))/g) || []).length;
  checks.unshift({ check: 'Loads with no console error and no missing asset', result: !errs.length && !failed.length && !assets.length && !externalRefs ? 'PASS' : 'FAIL',
    note: `${errs.length} console errors or warnings and ${failed.length} failed requests across the whole run; the page requests ${assets.length} other files and references ${externalRefs} external assets` +
      (errs.length ? '; ' + errs.slice(0, 3).join(' | ') : '') });
  await p.goto('file://' + pageFile); await p.waitForTimeout(400); await p.screenshot({ path: shotPath, fullPage: true });
  fs.writeFileSync(resultPath, JSON.stringify({ page: 'modernization/streams/interest/stream-INTCALC.html', sha, checks, detail }, null, 1) + '\n');
  for (const c of checks) console.log(c.result.padEnd(5), c.check, '-', c.note);
  await p.close(); await browser.close(); process.exit(checks.every(c => c.result === 'PASS') ? 0 : 1);
})().catch(e => { console.error(e); process.exit(2); });
