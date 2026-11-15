const { chromium } = require('playwright-core'); const fs = require('fs');
(async () => {
  const b = await chromium.connectOverCDP('http://localhost:29229'); const p = await b.contexts()[0].newPage(); await p.setViewportSize({ width: 1400, height: 900 }).catch(() => {});
  const errs = []; p.on('console', m => { if (m.type() === 'error') errs.push(m.text()); }); p.on('pageerror', e => errs.push(String(e)));
  await p.goto('file:///home/ubuntu/repos/ts-cobol-carddemo/modernization/legacy/call-graph.html'); await p.waitForTimeout(800);
  const D = JSON.parse(fs.readFileSync('/home/ubuntu/repos/ts-cobol-carddemo/modernization/legacy/call-graph.json')); const E = D.edges; const res = [];
  const mc = async l => { await l.scrollIntoViewIfNeeded(); const bb = await l.boundingBox(); await p.mouse.click(bb.x + bb.width / 2, bb.y + bb.height / 2); await p.waitForTimeout(80); };
  for (const t of ['call', 'job', 'data', 'copybook', 'tran', 'sched']) {
    await mc(p.locator('#tab-' + t)); const s = p.locator('#p-' + t); const shown = async () => parseInt(await s.locator('[data-shown]').innerText());
    const r = { tab: t, all: await shown() === E.filter(e => e.type === t).length };
    const st = s.locator('select[data-f=status]'); await st.focus(); await p.keyboard.press('ArrowDown'); await p.waitForTimeout(80);   // PROVEN
    r.statusProven = await shown() === E.filter(e => e.type === t && e.status === 'PROVEN').length && await s.locator('details[data-status=CANDIDATE]').isHidden();
    await st.focus(); await p.keyboard.press('ArrowDown'); await p.waitForTimeout(80);   // CANDIDATE
    r.statusCandidate = await shown() === E.filter(e => e.type === t && e.status === 'CANDIDATE').length;
    await st.focus(); await p.keyboard.press('ArrowUp'); await p.keyboard.press('ArrowUp'); await p.waitForTimeout(80);
    const mo = s.locator('select[data-f=mod]'); await mo.focus(); await p.keyboard.press('ArrowDown'); await p.waitForTimeout(80); const mv = await mo.inputValue();
    r.module = await shown() === E.filter(e => e.type === t && e.mod === mv).length; await mo.focus(); await p.keyboard.press('ArrowUp'); await p.waitForTimeout(80);
    await mc(s.locator('input[data-f=xmod]')); r.crossModule = await shown() === E.filter(e => e.type === t && e.xmod).length; await mc(s.locator('input[data-f=xmod]'));
    await mc(s.locator('input[data-f=q]')); await p.keyboard.type('COSGN00C', { delay: 5 }); await p.waitForTimeout(120);
    r.search = await shown() === E.filter(e => e.type === t && (e.frm + ' ' + e.to + ' ' + e.rel + ' ' + e.note + ' ' + e.cite.join(' ')).toLowerCase().includes('cosgn00c')).length;
    await p.keyboard.press('Control+a'); await p.keyboard.press('Backspace'); await p.waitForTimeout(80); r.restored = await shown() === E.filter(e => e.type === t).length;
    let ok = 0; for (const stt of ['PROVEN', 'CANDIDATE', 'OPEN']) { const d = s.locator('details[data-status=' + stt + ']'); await mc(d.locator('summary')); const a = await d.evaluate(e => e.open); await mc(d.locator('summary')); const z = await d.evaluate(e => e.open); if (!a && z) ok++; }
    r.collapsibles = ok === 3; res.push(r);
  }
  await mc(p.locator('#tab-lists')); let ok = 0; const n = await p.locator('#p-lists summary').count();
  for (let i = 0; i < n; i++) { const d = p.locator('#p-lists details').nth(i); const a = await d.evaluate(e => e.open); await mc(d.locator('summary')); if (await d.evaluate(e => e.open) !== a) ok++; }
  const lists = await p.evaluate(() => [...document.querySelectorAll('#p-lists details')].map(d => d.querySelectorAll('tbody tr').length));
  await mc(p.locator('#tab-dis')); const dis = await p.locator('#p-dis tbody tr').count();
  await mc(p.locator('#tab-overview')); const node = p.locator('#callsvg .node[data-id=COMEN01C]'); await mc(node); const hl = await p.locator('#dn').innerText(); await p.keyboard.press('Escape'); const cleared = (await p.locator('#dn').innerText()) === '';
  console.log(JSON.stringify({ tabs: res, listCollapsibles: ok + '/' + n, listRows: lists, expectLists: [D.uncalled.length, D.unreferenced.length, D.unscheduled.length, D.copybooks.filter(c => c.shared).length], disRows: dis, expectDis: D.disagreements.length, highlight: hl, escClears: cleared, consoleErrors: errs }));
  await p.close(); await b.close();
})();
