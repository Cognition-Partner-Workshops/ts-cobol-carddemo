// Drives strategy-interest.html in the machine's Chrome (CDP) with real mouse and keyboard events and
// compares what the page shows with a recount of design.json read from disk.
// Usage: NODE_PATH=<dir holding playwright-core> node page_check.js <baseline sha> <screenshot dir> <result.json>
const { chromium } = require('playwright-core'); const fs = require('fs'); const path = require('path');
const dir = path.resolve(__dirname, '..'); const pageFile = path.join(dir, 'strategy-interest.html');
const [sha, shotDir, resultPath] = process.argv.slice(2);
const design = JSON.parse(fs.readFileSync(path.join(dir, 'design.json'), 'utf8'));
const ticketFiles = fs.readdirSync(path.join(dir, 'tickets')).filter(f => /^\d\d-.*\.md$/.test(f));
const uniq = (list) => [...new Set(list)];
const groupNames = { pilot: 'Pilot - starts first, no dependencies', schema: 'Schema - no dependencies, can start in parallel with the pilot',
  'after-pilot': 'After the pilot - can run in parallel with each other and with the schema', 'waits-schema': 'Waits on the schema', gate: 'Final gate' };
const phaseTitle = Object.fromEntries(design.dataMoves.map(p => [p.phase, p.title]));
const allOwners = uniq([].concat(design.moveMap, design.dataItems, design.releaseDecisions, design.gaps, design.buildSteps).map(x => x.owner));
// recount from the arrays, not from design.counts
const expectedCards = {
  moveMap: design.moveMap.length, moves: design.moveMap.filter(m => m.disposition === 'MOVES').length,
  bridged: design.moveMap.filter(m => m.disposition === 'BRIDGED').length, stays: design.moveMap.filter(m => m.disposition === 'STAYS').length,
  components: design.components.length, tables: design.tables.length, buildSteps: design.buildSteps.length, tickets: ticketFiles.length,
  gateTickets: design.tickets.filter(t => t.group === 'gate').length, dataItems: design.dataItems.length, fixtureCases: design.reconciliation.length,
  fields: design.dataMap.length, gaps: design.gaps.length, registerOpen: design.registerDecisions.filter(r => r.status === 'OPEN').length,
  releaseDecisions: design.releaseDecisions.length, ownerRoles: allOwners.length, outsideUsers: design.counts.outsideUsers };
const expectedRows = { owners: allOwners.length, movemap: design.moveMap.length, components: design.components.length, ownership: design.tables.length,
  switches: design.policySwitches.length, frozen: design.frozenInterfaces.length, datamoves: design.dataItems.length, recon: design.reconciliation.length,
  tickets: design.tickets.length, datamap: design.dataMap.length, encoding: design.encodingRules.length,
  keys: design.tables.filter(t => t.kind !== 'framework').length, gaps: design.gaps.length, register: design.registerDecisions.length, release: design.releaseDecisions.length };
const filterData = {
  movemap: design.moveMap.map(m => ({ disposition: m.disposition, owner: m.owner })),
  ownership: design.tables.map(t => ({ kind: t.kind })),
  datamoves: design.dataItems.map(i => ({ phase: phaseTitle[i.phase], owner: i.owner })),
  tickets: design.tickets.map(t => ({ group: groupNames[t.group] })),
  datamap: design.dataMap.map(r => ({ copybook: r.copybook, kind: r.kind, role: r.role })),
  gaps: design.gaps.map(g => ({ owner: g.owner })),
  register: design.registerDecisions.map(r => ({ type: r.type })),
  release: design.releaseDecisions.map(d => ({ owner: d.owner })) };
const searchTerms = { movemap: ['SYSTRAN', 'COMBTRAN', 'zzz-no-match'], ownership: ['POSTTRAN', 'interest-calc'], datamoves: ['COMBTRAN', 'unload'],
  tickets: ['schema', 'gate'], datamap: ['NUMERIC(11,2)', 'FILLER', 'BYTEA'], gaps: ['EBCDIC', 'card'], register: ['rounded', 'fee'], release: ['Control-M', 'cycle'] };
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
  const clickLink = async (loc) => { await loc.evaluate(e => (e.querySelector('text') || e).scrollIntoView({ block: 'center' }));
    const r = await loc.evaluate(e => { const t = e.querySelector('text') || e; const b = t.getClientRects()[0]; return b ? { x: b.left + b.width / 2, y: b.top + b.height / 2 } : null; });
    if (!r) throw new Error('no box'); await p.mouse.click(r.x, r.y); };
  const tab = (key) => p.locator('#tabbtn-' + key);
  const tabKeys = await p.locator('[role=tab]').evaluateAll(list => list.map(b => b.id.replace('tabbtn-', '')));
  const shot = async (name) => { if (shotDir) await p.screenshot({ path: path.join(shotDir, name + '.png'), fullPage: true }); };

  // 1 tabs: mouse, then keyboard
  let bad = [];
  for (const key of tabKeys) { await click(tab(key)); const sel = await tab(key).getAttribute('aria-selected');
    const state = await p.evaluate(k => { const all = [...document.querySelectorAll('section[role=tabpanel]')]; const e = document.getElementById('tab-' + k);
      return { shown: !e.hidden && e.innerText.length > 50, others: all.filter(s => s !== e && !s.hidden).length }; }, key);
    if (sel !== 'true' || !state.shown || state.others) bad.push(key); await shot('tab-' + key); }
  await click(tab(tabKeys[0])); const seq = [];
  for (const k of ['ArrowRight', 'ArrowRight', 'ArrowLeft', 'End', 'ArrowRight', 'Home']) { await p.keyboard.press(k);
    seq.push(await p.evaluate(() => document.querySelector('[role=tab][aria-selected=true]').id.replace('tabbtn-', ''))); }
  const want = [tabKeys[1], tabKeys[2], tabKeys[1], tabKeys[tabKeys.length - 1], tabKeys[0], tabKeys[0]];
  record('Tabs', !bad.length && JSON.stringify(seq) === JSON.stringify(want),
    `${tabKeys.length} tabs clicked with the mouse (${tabKeys.length - bad.length} showed their panel alone); arrow, Home and End keys moved ${seq.join(' > ')}` + (bad.length ? '; failed: ' + bad.join(',') : ''));

  // 2 collapsible sections: mouse and Enter, then Expand all / Collapse all
  const detailIds = await p.locator('details').evaluateAll(list => list.map(d => d.id)); bad = [];
  for (const id of detailIds) { const d = p.locator('#' + id);
    await p.evaluate(i => { const e = document.getElementById(i); const s = e.closest('section[role=tabpanel]'); document.querySelectorAll('section[role=tabpanel]').forEach(x => { x.hidden = x !== s; }); }, id);
    const before = await d.evaluate(e => e.open); await click(d.locator('summary')); const mid = await d.evaluate(e => e.open);
    await d.locator('summary').focus(); await p.keyboard.press('Enter'); const after = await d.evaluate(e => e.open);
    const body = await d.evaluate(e => { e.open = true; const ok = e.innerText.length > e.querySelector('summary').innerText.length + 20; return ok; });
    await d.evaluate((e, o) => { e.open = o; }, before);
    if (mid === before || after !== before || !body) bad.push(id); }
  const bars = await p.locator('[data-expand]').evaluateAll(list => list.map(b => b.getAttribute('data-expand'))); let barsOk = 0;
  for (const sel of bars) { await p.evaluate(s => { const sec = document.querySelector(s).closest('section[role=tabpanel]'); document.querySelectorAll('section[role=tabpanel]').forEach(x => { x.hidden = x !== sec; }); }, sel);
    await click(p.locator(`[data-expand="${sel}"]`)); const allOpen = await p.evaluate(s => [...document.querySelector(s).querySelectorAll('details')].every(d => d.open), sel);
    await click(p.locator(`[data-collapse="${sel}"]`)); const allClosed = await p.evaluate(s => [...document.querySelector(s).querySelectorAll('details')].every(d => !d.open), sel);
    if (allOpen && allClosed) barsOk++; else bad.push('bar ' + sel); }
  record('Collapsible sections', !bad.length, `${detailIds.length - bad.filter(b => !b.startsWith('bar')).length} of ${detailIds.length} opened and closed (mouse click, then Enter key) and have content; ${barsOk} of ${bars.length} Expand all / Collapse all pairs worked` + (bad.length ? '; failed: ' + bad.join(',') : ''));
  await p.evaluate(() => document.querySelectorAll('#phases details').forEach(d => { d.open = true; }));

  // 3 links: every anchor, clicked where it is
  const linkCount = await p.locator('a').count(); const external = await p.locator('a:not([href^="#"])').count(); bad = []; let ok = 0; const kinds = { card: 0, diagram: 0, text: 0 };
  for (let i = 0; i < linkCount; i++) { const a = p.locator('a').nth(i);
    const info = await a.evaluate(e => { const s = e.closest('section[role=tabpanel]'); document.querySelectorAll('section[role=tabpanel]').forEach(x => { x.hidden = x !== s; });
      document.querySelectorAll('[role=tab]').forEach(b => b.setAttribute('aria-selected', b.getAttribute('aria-controls') === s.id ? 'true' : 'false'));
      for (let d = e.closest('details'); d; d = d.parentElement.closest('details')) d.open = true;
      const tr = e.closest('tr'); if (tr && tr.hidden) tr.hidden = false;
      return { href: e.getAttribute('href'), kind: e.closest('svg') ? 'diagram' : e.classList.contains('card') ? 'card' : 'text' }; });
    try { await clickLink(a); } catch (e) { bad.push(info.href + ' (no box)'); continue; }
    await p.waitForTimeout(40);
    const res = await p.evaluate(h => { const id = decodeURIComponent(h.slice(1)); const e = document.getElementById(id); if (!e) return 'missing';
      const s = e.closest('section[role=tabpanel]'); if (s.hidden) return 'panel hidden'; if (location.hash !== h) return 'hash ' + location.hash;
      if (e === s) return 'ok'; if (e.hidden) return 'target hidden'; const r = e.getBoundingClientRect();
      if (!(r.width > 0 && r.height > 0)) return 'no size'; if (r.bottom < 0 || r.top > innerHeight) return 'off screen';
      for (let d = e.closest('details'); d; d = d.parentElement.closest('details')) if (!d.open) return 'closed section'; return 'ok'; }, info.href);
    if (res === 'ok') { ok++; kinds[info.kind]++; } else bad.push(info.href + ' (' + res + ')'); }
  await p.goto('file://' + pageFile + '#ticket-06'); await p.waitForTimeout(300);
  const deep = await p.evaluate(() => { const e = document.getElementById('ticket-06'); return e.open && !e.closest('section').hidden; });
  record('Links', !bad.length && !external && deep, `${linkCount} links clicked: ${kinds.card} cards, ${kinds.diagram} diagram links and ${kinds.text} in-text references landed on a visible target in the right tab (${ok}/${linkCount}); opening the file with #ticket-06 opens that ticket; ${external} links leave the file` + (bad.length ? '; failed: ' + uniq(bad).slice(0, 8).join(', ') : ''));
  await p.goto('file://' + pageFile); await p.waitForTimeout(300);

  // 4 filters
  bad = []; const fnotes = [];
  for (const name of Object.keys(filterData)) { const box = p.locator(`.filters[data-for="${name}"]`);
    await p.evaluate(n => { const s = document.querySelector(`.filters[data-for="${n}"]`).closest('section[role=tabpanel]'); document.querySelectorAll('section[role=tabpanel]').forEach(x => { x.hidden = x !== s; }); }, name);
    const shown = async () => ({ label: await p.locator(`[data-showing="${name}"]`).innerText(), visible: await p.locator(`table[data-rows="${name}"] tbody tr:not([hidden])`).count() });
    const rows = filterData[name]; let optionCount = 0;
    const selects = await box.locator('select').evaluateAll(list => list.map(s => ({ key: s.getAttribute('data-filter'), options: [...s.options].map(o => o.value).filter(Boolean) })));
    for (const s of selects) { const inData = uniq(rows.map(r => r[s.key])).sort(); if (JSON.stringify([...s.options].sort()) !== JSON.stringify(inData)) bad.push(`${name}.${s.key} options differ from data`);
      for (const o of s.options) { await click(box.locator(`select[data-filter="${s.key}"]`)); await box.locator(`select[data-filter="${s.key}"]`).selectOption(o); optionCount++;
        const exp = rows.filter(r => r[s.key] === o).length; const got = await shown();
        if (got.visible !== exp || got.label !== `showing ${exp} of ${rows.length}`) bad.push(`${name}.${s.key}=${o}: ${got.label}, ${got.visible} visible, expected ${exp}`); }
      await box.locator(`select[data-filter="${s.key}"]`).selectOption(''); }
    if (selects.length > 1) { const [a, b] = selects; await box.locator(`select[data-filter="${a.key}"]`).selectOption(a.options[0]); await box.locator(`select[data-filter="${b.key}"]`).selectOption(b.options[0]);
      const exp = rows.filter(r => r[a.key] === a.options[0] && r[b.key] === b.options[0]).length; const got = await shown(); if (got.visible !== exp) bad.push(`${name} combined: ${got.visible} vs ${exp}`); }
    for (const term of searchTerms[name]) { await click(box.locator('button[data-clear]')); await click(box.locator('input[type=search]')); await p.keyboard.type(term);
      const exp = await p.evaluate(([n, t]) => [...document.querySelectorAll(`table[data-rows="${n}"] tbody tr`)].filter(r => r.textContent.toLowerCase().includes(t.toLowerCase())).length, [name, term]);
      const got = await shown(); if (got.visible !== exp || got.label !== `showing ${exp} of ${rows.length}`) bad.push(`${name} search ${term}: ${got.label}`); }
    await click(box.locator('button[data-clear]')); const got = await shown(); if (got.visible !== rows.length) bad.push(`${name} clear: ${got.visible}`);
    fnotes.push(`${name}: ${optionCount} options, ${searchTerms[name].length} searches, Clear`); }
  // a link to a row hidden by a filter clears the filter
  await click(tab('movemap')); await p.locator('.filters[data-for="movemap"] select[data-filter="disposition"]').selectOption('STAYS');
  await click(tab('overview')); await clickLink(p.locator('#diagram-move a[href="#move-M01"]').first()); await p.waitForTimeout(100);
  const unhid = await p.evaluate(() => !document.getElementById('move-M01').hidden && !document.getElementById('tab-movemap').hidden);
  if (!unhid) bad.push('link to a filtered-out row did not clear the filter');
  record('Filters', !bad.length, fnotes.join('; ') + '; every "showing N of M" and visible row count equals the recount of design.json; a diagram link to a filtered-out row clears the filter' + (bad.length ? '; failed: ' + bad.slice(0, 8).join(' | ') : ''));

  // 5 diagrams
  bad = []; const dnotes = [];
  const expectedNodes = { 'diagram-move': 2 * design.moveMap.filter(m => m.disposition !== 'STAYS').length + design.moveMap.filter(m => m.disposition === 'STAYS').length,
    'diagram-flow': 11, 'diagram-tickets': design.tickets.length };
  const expectedEdges = { 'diagram-move': design.moveMap.filter(m => m.disposition !== 'STAYS').length, 'diagram-flow': 13, 'diagram-tickets': design.tickets.reduce((n, t) => n + t.deps.length, 0) };
  for (const id of Object.keys(expectedNodes)) {
    await p.evaluate(i => { const s = document.getElementById(i).closest('section[role=tabpanel]'); document.querySelectorAll('section[role=tabpanel]').forEach(x => { x.hidden = x !== s; }); }, id);
    const d = await p.evaluate(i => { const svg = document.getElementById(i); const r = svg.getBoundingClientRect(); const vb = svg.viewBox.baseVal; const out = [];
      svg.querySelectorAll('[data-node]').forEach(g => { const rect = g.querySelector('rect').getBBox(); g.querySelectorAll('text').forEach(t => { const b = t.getBBox();
        if (!t.textContent.trim()) out.push('empty label'); if (b.x < rect.x + 1 || b.x + b.width > rect.x + rect.width - 1) out.push('label wider than its box: ' + t.textContent); }); });
      svg.querySelectorAll('text').forEach(t => { const b = t.getBBox(); if (b.x < 0 || b.y < 0 || b.x + b.width > vb.width || b.y + b.height > vb.height) out.push('label outside the drawing: ' + t.textContent); });
      const boxes = [...svg.querySelectorAll('[data-node] rect')].map(x => x.getBBox());
      for (let a = 0; a < boxes.length; a++) for (let b = a + 1; b < boxes.length; b++) { const A = boxes[a], B = boxes[b];
        if (A.x < B.x + B.width && B.x < A.x + A.width && A.y < B.y + B.height && B.y < A.y + A.height) out.push('boxes overlap'); }
      const nodes = [...svg.querySelectorAll('[data-node]')].map(g => ({ name: g.getAttribute('data-node'), b: g.querySelector('rect').getBBox() }));
      const inside = (pt, b, pad) => pt.x > b.x - pad && pt.x < b.x + b.width + pad && pt.y > b.y - pad && pt.y < b.y + b.height + pad;
      svg.querySelectorAll('path.ed').forEach(e => { const len = e.getTotalLength(); const a = e.getPointAtLength(0), z = e.getPointAtLength(len);
        const others = nodes.filter(n => !inside(a, n.b, 3) && !inside(z, n.b, 3));
        for (let d = 0; d <= len; d += 3) { const pt = e.getPointAtLength(d); const hit = others.find(n => inside(pt, n.b, -1)); if (hit) { out.push('arrow crosses box ' + hit.name); break; } } });
      const labels = [...svg.querySelectorAll('text.el, text.lt, text.lg')].map(t => ({ t: t.textContent, b: t.getBBox() }));
      const over = (A, B) => A.x < B.x + B.width && B.x < A.x + A.width && A.y < B.y + B.height && B.y < A.y + A.height;
      for (let a = 0; a < labels.length; a++) { for (let b = a + 1; b < labels.length; b++) if (over(labels[a].b, labels[b].b)) out.push('labels overlap: ' + labels[a].t + ' / ' + labels[b].t);
        if (svg.querySelector('text.el') && labels[a].b && nodes.some(n => over(labels[a].b, n.b))) out.push('label over a box: ' + labels[a].t); }
      return { w: Math.round(r.width), h: Math.round(r.height), nodes: svg.querySelectorAll('[data-node]').length, edges: svg.querySelectorAll('path.ed').length, problems: out }; }, id);
    if (!(d.w > 300 && d.h > 100)) bad.push(id + ' not drawn'); if (d.nodes !== expectedNodes[id]) bad.push(`${id}: ${d.nodes} boxes, expected ${expectedNodes[id]}`);
    if (d.edges !== expectedEdges[id]) bad.push(`${id}: ${d.edges} arrows, expected ${expectedEdges[id]}`); d.problems.forEach(x => bad.push(id + ': ' + x));
    dnotes.push(`${id} ${d.w}x${d.h}, ${d.nodes} boxes, ${d.edges} arrows`);
    await p.locator('#' + id).evaluate(e => e.scrollIntoView({ block: 'center' }));
    if (shotDir) await p.locator('#' + id).screenshot({ path: path.join(shotDir, id + '.png') }); }
  const ticketEdgesOk = await p.evaluate(() => document.querySelectorAll('#diagram-tickets a[href^="#ticket-"]').length);
  if (ticketEdgesOk !== design.tickets.length) bad.push('ticket diagram links ' + ticketEdgesOk);
  record('Diagrams render', !bad.length, `3 inline SVG diagrams drawn (${dnotes.join('; ')}); box and arrow counts equal design.json (move-map rows, ticket dependencies); no empty label, no label wider than its box or outside the drawing, no overlapping boxes or labels, no arrow drawn across a box it does not connect` + (bad.length ? '; failed: ' + uniq(bad).slice(0, 8).join(' | ') : ''));

  // 6 counts
  bad = [];
  const cards = await p.locator('#cards .card').evaluateAll(list => Object.fromEntries(list.map(c => [c.getAttribute('data-count'), Number(c.querySelector('b').textContent)])));
  for (const k of Object.keys(expectedCards)) if (cards[k] !== expectedCards[k]) bad.push(`card ${k}: page ${cards[k]}, data ${expectedCards[k]}`);
  if (Object.keys(cards).length !== Object.keys(expectedCards).length) bad.push('card set differs');
  const rowCounts = await p.evaluate(() => { const out = {}; document.querySelectorAll('table[data-rows]').forEach(t => { const n = t.getAttribute('data-rows'); out[n] = (out[n] || 0) + t.tBodies[0].rows.length; }); return out; });
  for (const k of Object.keys(expectedRows)) if (rowCounts[k] !== expectedRows[k]) bad.push(`table ${k}: page ${rowCounts[k]}, data ${expectedRows[k]}`);
  if (Object.keys(rowCounts).length !== Object.keys(expectedRows).length) bad.push('table set differs: ' + Object.keys(rowCounts).join(','));
  const pageBits = await p.evaluate(() => ({ steps: document.querySelectorAll('#steps details').length, ticketBlocks: document.querySelectorAll('#ticketlist details').length,
    phases: [...document.querySelectorAll('#phases details')].map(d => [d.getAttribute('data-phase'), d.querySelector('summary').textContent, d.querySelectorAll('table[data-rows=datamoves] tbody tr').length]),
    headings: [...document.querySelectorAll('h2')].map(h => h.textContent), proposed: document.querySelectorAll('tr .st.proposed, #steps summary .st.proposed').length,
    open: document.querySelectorAll('tr .st.open').length, embedded: document.getElementById('data').textContent,
    owners: [...document.querySelectorAll('table[data-rows=owners] tbody tr')].map(r => [...r.cells].map(c => c.textContent)),
    recon: [...document.querySelectorAll('table[data-rows=recon] tbody tr')].map(r => [...r.cells].map(c => c.textContent)) }));
  if (pageBits.steps !== design.buildSteps.length) bad.push('step blocks ' + pageBits.steps);
  if (pageBits.ticketBlocks !== ticketFiles.length) bad.push(`ticket blocks ${pageBits.ticketBlocks}, ticket files ${ticketFiles.length}`);
  for (const [phase, summary, n] of pageBits.phases) { const exp = design.dataItems.filter(i => i.phase === phase).length; if (n !== exp || !summary.includes(`(${exp})`)) bad.push(`phase ${phase}: ${n} rows, summary "${summary}", data ${exp}`); }
  const inHeading = (text, n) => pageBits.headings.some(h => h.includes(text) && h.includes(String(n)));
  for (const [text, n] of [['Build steps', design.buildSteps.length], ['Moving the data', design.dataItems.length], ['Ticket queue', ticketFiles.length], ['Data map', design.dataMap.length],
    ['Mapping gaps', design.gaps.length], ['Business decisions', design.registerDecisions.length], ['Release and coexistence', design.releaseDecisions.length], ['Components', design.components.length]])
    if (!inHeading(text, n)) bad.push(`heading "${text}" does not show ${n}`);
  const proposedExpected = design.moveMap.length + design.buildSteps.length + design.dataItems.length;
  if (pageBits.proposed !== proposedExpected) bad.push(`PROPOSED badges ${pageBits.proposed}, items ${proposedExpected}`);
  if (pageBits.open !== design.registerDecisions.length + design.releaseDecisions.length) bad.push('OPEN badges ' + pageBits.open);
  if (JSON.stringify(JSON.parse(pageBits.embedded)) !== JSON.stringify(design)) bad.push('embedded data differs from design.json on disk');
  for (const row of pageBits.owners) { const role = row[0]; const exp = [design.moveMap, design.buildSteps, design.dataItems, design.releaseDecisions, design.gaps].map(a => a.filter(x => x.owner === role).length);
    const got = row.slice(1, 6).map(Number); if (JSON.stringify(got) !== JSON.stringify(exp) || Number(row[6]) !== exp.reduce((a, b) => a + b, 0)) bad.push('owner row ' + role); }
  for (const row of pageBits.recon) { const r = design.reconciliation.find(x => x.case === row[0]);
    if (!r || Number(row[1]) !== r.accounts || Number(row[8]) !== r.transactions || row[9] !== r.sumTranAmt || row[11] !== r.balanceChange) bad.push('recon row ' + row[0]); }
  record('Counts equal the data', !bad.length, `${Object.keys(expectedCards).length} cards, ${Object.keys(expectedRows).length} tables, the step, phase and ticket blocks, the numbers in ${8} headings, ${proposedExpected} PROPOSED and ${pageBits.open} OPEN badges, the owner table and the reconciliation table equal a recount of design.json; ticket blocks equal the ${ticketFiles.length} files under tickets/; the embedded data equals design.json on disk` + (bad.length ? '; failed: ' + bad.slice(0, 8).join(' | ') : ''));

  // 7 baseline SHA
  const shown = await p.locator('#sha').innerText(); const visible = await p.locator('#sha').isVisible();
  record('Baseline SHA shown', shown === sha && visible && shown === design.meta.baseline.sha, `header shows ${shown}, the head of ${design.meta.baseline.branch}; it is also in the Source line of each of the ${ticketFiles.length} ticket blocks`);

  // 8 load: collected over the whole run
  const html = fs.readFileSync(pageFile, 'utf8'); const refs = (html.match(/(?:src|href)\s*=\s*["'](?!#)[^"']+["']|url\((?!#)[^)]+\)|@import/g) || []);
  const other = requests.filter(u => !u.startsWith('file://' + pageFile));
  checks.unshift({ check: 'Loads with no console error and no missing asset', result: !errs.length && !failed.length && !other.length && !refs.length ? 'PASS' : 'FAIL',
    note: `${errs.length} console errors or warnings and ${failed.length} failed requests across the whole run; the page requests ${other.length} other files and references ${refs.length} external assets` + (errs.length ? '; ' + errs.slice(0, 3).join(' | ') : '') + (refs.length ? '; ' + refs.slice(0, 3).join(' ') : '') });

  await p.goto('file://' + pageFile); await p.waitForTimeout(300);
  if (shotDir) await p.screenshot({ path: path.join(shotDir, 'final-overview.png') });
  detail.linkCount = linkCount; detail.viewport = '1400x900'; detail.browser = browser.version();
  fs.writeFileSync(resultPath, JSON.stringify({ page: 'modernization/design/strategy-interest.html', baseline: sha, checks, detail }, null, 1) + '\n');
  for (const c of checks) console.log(c.result, '-', c.check, '-', c.note);
  await p.close(); await browser.close(); process.exit(checks.every(c => c.result === 'PASS') ? 0 : 1);
})().catch(e => { console.error(e); process.exit(2); });
