"use strict";

const moduleIds = ["CARDDEMO", "AUTH", "TRANTYPE", "VSAMMQ"];
const steps = [
  { id: 1, title: "Sweep the estate" },
  { id: 2, title: "Name the modules" },
  { id: 3, title: "Check the map" },
  { id: 4, title: "Scan the modules" },
  { id: 5, title: "Roll the estate up" },
  { id: 6, title: "Deeper module analysis" },
  { id: 7, title: "Open asks and next steps" },
  { id: 8, title: "Strangler-fig migration" },
];
const placeableExcluded = new Set(["document", "data", "tooling", "binary", "empty", "unclassified"]);
const state = { catalog: null, modules: null, estate: null, perModule: {}, migration: null };

const esc = (value) => String(value ?? "").replace(/[&<>"']/g, (ch) =>
  ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[ch]));
const pill = (text, cls = "") => `<span class="pill ${esc(cls)}">${esc(text)}</span>`;
const cites = (list, max = 4) => {
  if (!list || !list.length) return "";
  const shown = list.slice(0, max).map((c) => `<code>${esc(c)}</code>`).join(", ");
  const more = list.length > max ? ` +${list.length - max}` : "";
  return `<div class="cite">${shown}${more}</div>`;
};
const stat = (value, label) => `<div class="stat"><b>${esc(value)}</b><span>${esc(label)}</span></div>`;
const reportLink = (path, label) => `<a href="#/report/${encodeURIComponent(path)}">${esc(label)}</a>`;

async function getJson(path) {
  const response = await fetch(path);
  if (!response.ok) return null;
  return response.json();
}

async function loadStore() {
  const [catalog, modules, estate, migration] = await Promise.all([
    getJson("store/catalog.json"), getJson("store/modules.json"), getJson("store/estate.json"), getJson("migration.json"),
  ]);
  Object.assign(state, { catalog, modules, estate, migration });
  const files = ["descriptions", "findings", "online", "chains", "annotations", "modulenotes", "profile", "dyncalls"];
  await Promise.all(moduleIds.map(async (moduleId) => {
    const docs = await Promise.all(files.map((f) => getJson(`store/${moduleId}/${f}.json`)));
    state.perModule[moduleId] = Object.fromEntries(files.map((f, i) => [f, docs[i]]));
  }));
}

function renderNav(route) {
  const stepLinks = steps.map((s) =>
    `<a href="#/step/${s.id}" class="${route === "step/" + s.id ? "active" : ""}"><span class="num">${s.id}</span>${esc(s.title)}</a>`).join("");
  const moduleLinks = moduleIds.map((m) =>
    `<a href="#/module/${m}" class="${route === "module/" + m ? "active" : ""}">${esc(m)}</a>`).join("");
  document.getElementById("nav").innerHTML =
    `<h4>Walkthrough</h4>${stepLinks}<h4>Modules</h4>${moduleLinks}` +
    `<h4>mftb reports</h4><a href="#/report/${encodeURIComponent("reports/estate.html")}" class="${route.startsWith("report/") ? "active" : ""}">Estate report</a>`;
}

function stepNav(stepId) {
  const prev = stepId > 1 ? `<button class="ghost" onclick="location.hash='#/step/${stepId - 1}'">← ${esc(steps[stepId - 2].title)}</button>` : "<span></span>";
  const next = stepId < steps.length ? `<button onclick="location.hash='#/step/${stepId + 1}'">${esc(steps[stepId].title)} →</button>` : "<span></span>";
  return `<div class="stepnav">${prev}${next}</div>`;
}

function stepHeader(stepId, lede) {
  return `<div class="cite">Step ${stepId} of ${steps.length}</div><h2>${esc(steps[stepId - 1].title)}</h2><p class="lede">${lede}</p>`;
}

function viewSweep() {
  const { summary, meta, asks } = state.catalog;
  const kinds = Object.entries(summary.by_kind).sort((a, b) => b[1] - a[1]);
  const maxCount = kinds[0][1];
  const kindRows = kinds.map(([kind, count]) => `<tr><td><code>${esc(kind)}</code></td>
    <td class="num">${count}</td><td style="width:45%"><div class="bar" style="width:${(count / maxCount) * 100}%;${placeableExcluded.has(kind) ? "background:var(--border)" : ""}"></div></td>
    <td>${placeableExcluded.has(kind) ? pill("not placeable") : pill("artifact", "ok")}</td></tr>`).join("");
  const askRows = asks.map((a) => `<tr><td>${pill(a.severity, a.severity)}</td><td><code>${esc(a.kind)}</code></td>
    <td>${esc(a.message)}<div class="cite">${esc(a.request)}</div></td></tr>`).join("");
  return stepHeader(1, `<code>mftb scan-estate ${esc(meta.estate.join(" "))}</code> read every file under the estate root and typed each one by its own content (not its extension). Nothing was excluded, so this is the whole universe the rest of the analysis is counted against.`) +
    `<div class="stats">${stat(summary.files, "files swept")}${stat(summary.entries, "catalogued")}${stat(summary.lines.toLocaleString(), "lines")}${stat(summary.names, "names declared")}${stat(summary.unclassified, "unclassified")}${stat(summary.asks, "sweep asks")}</div>
    <h3>What the estate is made of</h3><table><thead><tr><th>kind</th><th class="num">files</th><th></th><th></th></tr></thead><tbody>${kindRows}</tbody></table>
    <h3>What the sweep could not type or read (${asks.length})</h3>
    <p class="lede">Each is an open question, not a silent gap: HLASM and PL/I are catalogued by name only, EBCDIC data is binary to the sweep, and a few copybooks/DDL match no signature.</p>
    <table><thead><tr><th>severity</th><th>kind</th><th>what and the ask</th></tr></thead><tbody>${askRows}</tbody></table>` + stepNav(1);
}

function moduleCard(mod) {
  const estateMod = state.estate.modules.find((m) => m.id === mod.id) || {};
  return `<div class="card"><div style="display:flex;justify-content:space-between;gap:8px"><b><a href="#/module/${mod.id}">${esc(mod.id)}</a></b>${pill(mod.naming, mod.naming === "confirmed" ? "ok" : "medium")}</div>
    <div style="font-weight:600;margin-top:4px">${esc(mod.name)}</div>
    <p style="margin:6px 0">${esc(mod.what_it_does)}</p>
    <div class="cite">${mod.members.length} members · ${estateMod.files ?? "?"} files · ${esc(mod.reasoning || "")}</div>
    ${cites(mod.evidence, 3)}</div>`;
}

function viewModules() {
  const { modules, shared, unplaced } = state.modules;
  return stepHeader(2, "The module-classification skill grouped the catalogue into applications using the repository's own structure first (sub-application directories, one CICS CSD group each, the scheduler exports), never a name prefix alone. Names are <b>proposed</b> until you confirm or rename them.") +
    `<div class="grid">${modules.map(moduleCard).join("")}</div>
    <div class="callout warn"><b>Decision for you:</b> confirm or rename CARDDEMO, AUTH, TRANTYPE and VSAMMQ. The customer's own names always win.</div>
    <h3>Shared code (${shared.length})</h3>
    <table><thead><tr><th>name</th><th>artifact</th><th>used by</th><th>why</th></tr></thead><tbody>${shared.map((s) =>
      `<tr><td><code>${esc(s.name)}</code><div class="cite">${esc(s.path)}</div></td><td>${esc(s.artifact)}</td><td>${s.used_by.map((u) => pill(u)).join(" ")}</td><td>${esc(s.why)}${cites(s.evidence, 2)}</td></tr>`).join("")}</tbody></table>
    <h3>Unplaced (${unplaced.length})</h3><p class="lede">${unplaced.length ? unplaced.map((u) => `${esc(u.name)}: ${esc(u.request)}`).join("<br>") : "None — every catalogued artifact is owned by a module or listed as shared."}</p>` + stepNav(2);
}

function viewCheck() {
  const placeable = state.catalog.entries.filter((e) => !placeableExcluded.has(e.kind));
  const placed = new Map();
  state.modules.modules.forEach((m) => m.members.forEach((mem) => placed.set(mem.path, (placed.get(mem.path) || []).concat(m.id))));
  state.modules.shared.forEach((s) => placed.set(s.path, (placed.get(s.path) || []).concat("shared")));
  state.modules.unplaced.forEach((u) => placed.set(u.path, (placed.get(u.path) || []).concat("unplaced")));
  const missing = placeable.filter((e) => !placed.has(e.path));
  const doubled = [...placed.entries()].filter(([, owners]) => owners.length > 1);
  const ok = !missing.length && !doubled.length;
  const byModule = state.modules.modules.map((m) => {
    const kinds = {};
    m.members.forEach((mem) => { kinds[mem.artifact] = (kinds[mem.artifact] || 0) + 1; });
    return `<tr><td><b>${esc(m.id)}</b></td><td class="num">${m.members.length}</td><td>${Object.entries(kinds).map(([k, v]) => `${esc(k)} ${v}`).join(" · ")}</td></tr>`;
  }).join("");
  return stepHeader(3, "<code>mftb modules --check</code> is the one machine-verifiable part of the map: every placeable catalogue entry must sit exactly once in a module, in <code>shared[]</code> or in <code>unplaced[]</code>. This page re-runs that check in your browser against the same two files.") +
    `<div class="callout ${ok ? "" : "warn"}">${ok ? pill("pass", "ok") : pill("fail", "high")} <b>${placed.size} of ${placeable.length}</b> placeable artifacts placed · ${missing.length} missing · ${doubled.length} placed twice</div>
    <table><thead><tr><th>owner</th><th class="num">members</th><th>by artifact</th></tr></thead><tbody>${byModule}
    <tr><td><b>shared</b></td><td class="num">${state.modules.shared.length}</td><td>${state.modules.shared.map((s) => esc(s.name)).join(", ")}</td></tr></tbody></table>
    ${missing.length ? `<h3>Not placed</h3><p>${missing.map((e) => `<code>${esc(e.path)}</code>`).join(", ")}</p>` : ""}` + stepNav(3);
}

function viewScans() {
  const rows = state.estate.modules.map((m) => {
    const desc = state.perModule[m.id].descriptions?.summary || {};
    const online = state.perModule[m.id].online?.summary || {};
    const chains = state.perModule[m.id].chains?.chains || [];
    return `<tr><td><a href="#/module/${m.id}"><b>${esc(m.id)}</b></a></td><td class="num">${m.programs}</td><td class="num">${m.jobs}</td>
      <td class="num">${online.transactions ?? 0}</td><td class="num">${chains.length}</td><td class="num">${m.datasets}</td><td class="num">${m.cics_commands}</td>
      <td class="num">${m.findings.open}</td><td class="num">${m.findings.high ? pill(m.findings.high, "high") : 0}</td>
      <td>${desc.subjects ? pill(`${desc.written}/${desc.subjects}`, desc.written === desc.subjects ? "ok" : "medium") : "–"}</td></tr>`;
  }).join("");
  return stepHeader(4, "Each module was scanned with <code>mftb scan --module &lt;M&gt; --from-catalog</code> (programs, jobs, schedule, DB2, connections, dynamic calls, traces, environment profile), then the batch-chain and online passes. Every program, job and PROC carries a one-line business description written from the source and cited.") +
    `<table><thead><tr><th>module</th><th class="num">programs</th><th class="num">jobs</th><th class="num">transactions</th><th class="num">chains</th><th class="num">datasets</th><th class="num">CICS cmds</th><th class="num">open findings</th><th class="num">high</th><th>descriptions</th></tr></thead><tbody>${rows}</tbody></table>
    <p class="lede" style="margin-top:10px">“programs” counts every program name a scan saw referenced, including CICS/MQ/IMS runtime and siblings it calls, not only the source members the module owns.</p>` + stepNav(4);
}

function viewRollup() {
  const { connections, site, inventory } = state.estate;
  const crossRows = state.modules.cross_module.map((c) =>
    `<tr><td>${pill(c.from)} → ${pill(c.to)}</td><td>${esc(c.how)}<div class="cite">${esc(c.why)}</div>${cites(c.evidence, 2)}</td></tr>`).join("");
  const pairCounts = {};
  connections.forEach((c) => { const key = c.between.join(" ↔ "); pairCounts[key] = pairCounts[key] || {}; pairCounts[key][c.kind] = (pairCounts[key][c.kind] || 0) + 1; });
  const pairRows = Object.entries(pairCounts).map(([pair, kinds]) =>
    `<tr><td><b>${esc(pair)}</b></td><td>${Object.entries(kinds).map(([k, v]) => `${esc(k)} ${v}`).join(" · ")}</td></tr>`).join("");
  const siteRows = site.map((s) => {
    const answers = Object.entries(s.answers).map(([mod, a]) => `${esc(mod)}: ${esc(a.value)} ${a.confidence === "assumed" ? pill("assumed", "medium") : ""}`).join("<br>");
    return `<tr><td><code>${esc(s.field)}</code></td><td>${s.agreed ? esc(s.value) : pill("modules disagree", "high")}</td><td>${answers}</td></tr>`;
  }).join("");
  const invRows = inventory.map((i) => `<tr><td><code>${esc(i.kind)}</code></td><td class="num">${i.files}</td>${moduleIds.map((m) => `<td class="num">${i.by_module[m] || ""}</td>`).join("")}<td class="num">${i.by_module.shared || ""}</td></tr>`).join("");
  return stepHeader(5, "<code>mftb report-estate</code> adds up what the module stores already say: where one application crosses into another at run time, what code and data they share, and what each says about the platform. This is the migration-wave question.") +
    `<h3>Where a module boundary is crossed at run time</h3><table><thead><tr><th>edge</th><th>how, and why it matters</th></tr></thead><tbody>${crossRows}</tbody></table>
    <h3>Connections between modules (${connections.length})</h3><table><thead><tr><th>pair</th><th>by kind</th></tr></thead><tbody>${pairRows}</tbody></table>
    <h3>Site and environment</h3><p class="lede">A field two modules answer differently is shown as disagreeing, never merged. “Assumed” is an mftb default nobody has confirmed.</p>
    <table><thead><tr><th>field</th><th>estate value</th><th>per module</th></tr></thead><tbody>${siteRows}</tbody></table>
    <h3>Inventory by owner</h3><table><thead><tr><th>kind</th><th class="num">files</th>${moduleIds.map((m) => `<th class="num">${m}</th>`).join("")}<th class="num">shared</th></tr></thead><tbody>${invRows}</tbody></table>` + stepNav(5);
}


function viewDeep() {
  const rows = moduleIds.map((m) => {
    const pm = state.perModule[m];
    const ann = pm.annotations?.dynamic_calls || [];
    const sites = pm.dyncalls?.sites?.length || 0;
    const resolved = ann.filter((a) => a.status === "resolved").length;
    const prof = pm.profile?.summary || {};
    const openDyn = (pm.findings?.findings || []).filter((f) => f.status === "open" && f.kind.startsWith("dynamic-call")).length;
    return `<tr><td><a href="#/module/${m}"><b>${m}</b></a></td><td class="num">${sites}</td><td class="num">${resolved}</td><td class="num">${openDyn}</td>
      <td class="num">${(pm.modulenotes?.members || []).length}</td><td class="num">${pm.online?.summary?.transactions ?? 0}</td><td class="num">${(pm.chains?.chains || []).length}</td>
      <td class="num">${prof.detected ?? 0}</td><td class="num">${prof.defaulted ?? 0}</td><td class="num">${prof.unanswered ?? 0}</td></tr>`;
  }).join("");
  const dynRows = moduleIds.flatMap((m) => (state.perModule[m].annotations?.dynamic_calls || []).map((a) =>
    `<tr><td>${pill(m)}</td><td><code>${esc(a.program)}</code><div class="cite">${esc(a.site)}</div></td><td>${pill(a.status, a.status === "resolved" ? "ok" : "medium")}</td>
     <td>${a.targets.map((t) => `<code>${esc(t.name)}</code> <span class="cite">${esc(t.basis)}</span>`).join(", ") || "–"}${(a.open_ends || []).map((o) => `<div class="cite">open: ${esc(o.request || o.kind)}</div>`).join("")}</td></tr>`)).join("");
  const bindRows = moduleIds.flatMap((m) => (state.perModule[m].modulenotes?.members || []).map((b) =>
    `<tr><td>${pill(m)}</td><td><code>${esc(b.name)}</code></td><td>${esc(b.binding.kind)}${b.module ? " → " + pill(b.module) : ""} ${pill(b.binding.confirmed ? "confirmed" : b.binding.confidence, b.binding.confirmed ? "ok" : "medium")}</td><td>${esc(b.binding.reasoning)}${b.request ? `<div class="cite">ask: ${esc(b.request)}</div>` : ""}</td></tr>`)).join("");
  const profAsks = {};
  moduleIds.forEach((m) => (state.perModule[m].profile?.asks || []).forEach((a) => { const k = a.field || a.kind; (profAsks[k] = profAsks[k] || { a, mods: [] }).mods.push(m); }));
  const profRows = Object.entries(profAsks).map(([k, { a, mods }]) => `<tr><td><code>${esc(k)}</code></td><td>${pill(a.severity || "", a.severity)}</td><td>${mods.map((m) => pill(m)).join(" ")}</td><td>${esc(a.request || a.message || "")}</td></tr>`).join("");
  return stepHeader(6, "The per-module passes of <code>module-inventory-analysis</code>: dynamic-call resolution (cited, never guessed), name bindings for what a scan cannot read, every CICS transaction and batch chain worked end to end, and the environment profile. What reasoning could not close stays an ask.") +
    `<table><thead><tr><th>module</th><th class="num">dyn. sites</th><th class="num">annotated resolved</th><th class="num">dyn. findings open</th><th class="num">bindings</th><th class="num">transactions</th><th class="num">chains</th><th class="num">profile detected</th><th class="num">assumed</th><th class="num">unanswered</th></tr></thead><tbody>${rows}</tbody></table>
    <h3>Dynamic-call resolution</h3><table><thead><tr><th>module</th><th>site</th><th>status</th><th>targets (basis)</th></tr></thead><tbody>${dynRows}</tbody></table>
    <h3>Name bindings</h3><table><thead><tr><th>module</th><th>name</th><th>binding</th><th>why / ask</th></tr></thead><tbody>${bindRows}</tbody></table>
    <h3>Environment questions for the customer (${Object.keys(profAsks).length})</h3><p class="lede">Asked once per field; assumed values are mftb defaults, not facts.</p>
    <table><thead><tr><th>field</th><th>severity</th><th>modules</th><th>the ask</th></tr></thead><tbody>${profRows}</tbody></table>` + stepNav(6);
}

const nextStep = {
  CARDDEMO: "posting and interest are migrated behind the switch (step 8); the CICS screens are analysis-only off-host.",
  AUTH: "Answer the IMS, Db2 and MQ environment asks before slicing its 5 batch chains.",
  TRANTYPE: "trantype-update is the suggested next slice; trantype-dynamic waits on the CBTRNDYN runtime parameter.",
  VSAMMQ: "No batch work; its 2 MQ transactions wait on the online target design.",
};

function viewAsks() {
  const asks = state.estate.asks;
  const scopes = ["all", ...new Set(asks.map((a) => a.scope))];
  const html = stepHeader(7, "Everything the analysis could not prove from the repository is an ask with an owner. The high ones are what to put to the customer next. The deeper analysis (step 6) is done for every module; the batch chains it proved now move off the mainframe one slice at a time (step 8).") +
    `<div class="controls"><select id="askScope">${scopes.map((s) => `<option>${esc(s)}</option>`).join("")}</select>
     <select id="askSeverity"><option>all</option><option>high</option><option>medium</option><option>info</option></select>
     <input id="askText" placeholder="filter text" size="24"></div><div id="askTable"></div>
     <h3>Next step per module</h3><div class="grid">${moduleIds.map((m) => {
       const mod = state.estate.modules.find((x) => x.id === m);
       return `<div class="card"><b>${m}</b><p style="margin:6px 0">${esc(nextStep[m] || "")}</p>
         <div class="cite">${mod.findings.open} open findings · ${mod.findings.high} high · boundary must-establish ${mod.boundary["must-establish"] || 0}</div></div>`;
     }).join("")}</div>` + stepNav(7);
  setTimeout(() => {
    const draw = () => {
      const scope = document.getElementById("askScope").value;
      const severity = document.getElementById("askSeverity").value;
      const text = document.getElementById("askText").value.toLowerCase();
      const rows = asks.filter((a) => (scope === "all" || a.scope === scope) && (severity === "all" || a.severity === severity) &&
        (!text || JSON.stringify(a).toLowerCase().includes(text)));
      document.getElementById("askTable").innerHTML = `<div class="cite" style="margin-bottom:6px">${rows.length} of ${asks.length}</div>
        <table><thead><tr><th>scope</th><th>severity</th><th>kind</th><th>what, and the ask</th></tr></thead><tbody>${rows.map((a) =>
          `<tr><td>${pill(a.scope)}</td><td>${pill(a.severity, a.severity)}</td><td><code>${esc(a.kind)}</code></td><td>${esc(a.message)}<div class="cite">${esc(a.request || "")}</div>${cites(a.evidence, 2)}</td></tr>`).join("")}</tbody></table>`;
    };
    ["askScope", "askSeverity", "askText"].forEach((id) => document.getElementById(id).addEventListener("input", draw));
    draw();
  });
  return html;
}

function viewMigration() {
  const mig = state.migration;
  if (!mig) return stepHeader(8, "") + `<div class="callout warn">migration.json could not be loaded.</div>`;
  const prUrl = (n) => `https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/${n}`;
  const ok = (v) => pill(v, /^(\d+)\/\1$|green/.test(v) ? "ok" : "medium");
  const sliceRows = mig.slices.map((s) => `<tr><td><b>${esc(s.chain)}</b><div class="cite">${esc(s.module)}</div></td><td>${esc(s.jobs)}<div>${s.programs.map((p) => `<code>${esc(p)}</code>`).join(" ")}</div></td>
    <td>${ok(s.javaParity)}</td><td>${ok(s.legacyParity)}</td><td>${ok(s.ci)}</td><td>${pill(s.default, "info")}<div class="cite">${esc(s.switchFile)}</div></td>
    <td><a href="${prUrl(s.pr)}" target="_blank" rel="noopener">PR ${s.pr}</a><div class="cite">merge #${s.mergeOrder} · base ${esc(s.base)}</div></td></tr>`).join("");
  const sliceCards = mig.slices.map((s) => `<h3>${esc(s.chain)} — ${esc(s.target)}</h3>
    <div class="grid"><div class="card"><b>Legacy quirks reproduced</b><ul class="points">${s.quirks.map((q) => `<li>${esc(q)}</li>`).join("")}</ul></div></div>
    <table style="margin-top:12px"><thead><tr><th>Edge case (no fixture covers it)</th><th>Decision: follow the COBOL as written</th></tr></thead><tbody>${s.decisions.map((d) => `<tr><td>${esc(d.q)}</td><td>${esc(d.a)}</td></tr>`).join("")}</tbody></table>`).join("");
  const switchRows = mig.switch.map((w) => `<tr><td>${esc(w.how)}</td><td><code>${esc(w.command)}</code></td></tr>`).join("");
  const remRows = mig.remaining.map((r) => `<tr><td><b>${esc(r.chain)}</b><div class="cite">${esc(r.module)}</div></td><td>${esc(r.jobs)}<div class="cite">${esc(r.programs)}</div></td>
    <td>${pill(r.status, r.status === "next" ? "info" : r.status === "blocked" ? "high" : "")}</td><td>${esc(r.note)}</td></tr>`).join("");
  const done = mig.slices.length, total = mig.slices.length + mig.remaining.length;
  return stepHeader(8, esc(mig.approach)) +
    `<div class="stats">${stat(`${done}/${total}`, "slices with a Java path")}${stat(mig.slices.reduce((n, s) => n + s.programs.length, 0), "programs migrated")}${stat("24/24", "legacy parity cases")}${stat("0", "slices cut over (default: legacy)")}</div>
    <div class="callout">Nothing is merged and nothing is cut over: COBOL stays the default for every chain. Merge PR 203 first, then PR 204, which is stacked on it. Parity is field-level (<code>tools/parity/compare.py</code>) against the golden fixtures, which were not edited.</div>
    <h3>Slices</h3><table><thead><tr><th>chain</th><th>jobs · programs</th><th>java parity</th><th>legacy parity</th><th>CI</th><th>default</th><th>PR</th></tr></thead><tbody>${sliceRows}</tbody></table>
    <h3>The switch</h3><table><tbody>${switchRows}</tbody></table>
    ${sliceCards}
    <h3>What is still on the mainframe</h3><table><thead><tr><th>chain</th><th>jobs · programs</th><th>status</th><th>note</th></tr></thead><tbody>${remRows}</tbody></table>
    <div class="cite" style="margin-top:8px">Updated ${esc(mig.updated)}</div>` + stepNav(8);
}

function viewModule(moduleId) {
  const mod = state.modules.modules.find((m) => m.id === moduleId);
  const estateMod = state.estate.modules.find((m) => m.id === moduleId);
  const { descriptions, findings, online, chains } = state.perModule[moduleId];
  const overview = descriptions?.overviews?.find((o) => o.kind === "module");
  const chainOverviews = Object.fromEntries((descriptions?.overviews || []).filter((o) => o.kind === "chain").map((o) => [o.name, o]));
  const txRows = (online?.entry_points || []).map((t) =>
    `<tr><td><code>${esc(t.transid)}</code></td><td>${pill(t.role)}</td><td><code>${esc(t.program)}</code></td><td>${(t.reached_by || []).map((r) => esc(r.from)).join(", ")}</td><td class="cite">${esc(t.source)}</td></tr>`).join("");
  const chainRows = (chains?.chains || []).map((c) => {
    const ov = chainOverviews[c.table];
    return `<tr><td>${reportLink(`reports/${moduleId}/chains/chain-${c.table}.html`, c.table)}</td><td class="num">${c.jobs.length}</td>
      <td>${ov ? `<ul class="points">${ov.key_points.map((p) => `<li>${esc(p)}</li>`).join("")}</ul>` : `<span class="cite">${esc(c.selection_basis)}</span>`}</td></tr>`;
  }).join("");
  const progRows = (descriptions?.descriptions || []).map((d) =>
    `<tr><td><code>${esc(d.name)}</code><div class="cite">${esc(d.kind)}</div></td><td>${esc(d.description)}${cites(d.basis, 2)}</td></tr>`).join("");
  const high = (findings?.findings || []).filter((f) => f.severity === "high" && f.status === "open");
  const pageLinks = (online?.entry_points || [])
    .map((t) => reportLink(`reports/${moduleId}/online/online-${t.transid}.html`, t.transid)).join(" · ");
  return `<div class="cite">Module</div><h2>${esc(mod.id)} — ${esc(mod.name)}</h2><p class="lede">${esc(mod.what_it_does)}</p>
    <div class="stats">${stat(mod.members.length, "members")}${stat(estateMod.jobs, "jobs")}${stat(online?.summary?.transactions ?? 0, "transactions")}${stat((chains?.chains || []).length, "batch chains")}${stat(estateMod.datasets, "datasets")}${stat(estateMod.findings.open, "open findings")}</div>
    <p>${reportLink(`reports/${moduleId}/${moduleId}-report.html`, "Open the full mftb module report")}${pageLinks ? " · transaction pages: " + pageLinks : ""}</p>
    ${overview ? `<h3>What it does, in business terms</h3><div class="card"><ul class="points">${overview.key_points.map((p) => `<li>${esc(p)}</li>`).join("")}</ul>${cites(overview.basis, 6)}</div>` : ""}
    ${txRows ? `<h3>Online transactions (${online.entry_points.length})</h3><table><thead><tr><th>tranid</th><th>role</th><th>initial program</th><th>reached from</th><th>defined at</th></tr></thead><tbody>${txRows}</tbody></table>` : ""}
    ${chainRows ? `<h3>Batch chains</h3><table><thead><tr><th>chain</th><th class="num">jobs</th><th>what it does</th></tr></thead><tbody>${chainRows}</tbody></table>` : ""}
    <h3>High findings (${high.length})</h3>${high.length ? `<table><tbody>${high.map((f) => `<tr><td><code>${esc(f.kind)}</code></td><td>${esc(f.message)}<div class="cite">${esc(f.request || "")}</div></td></tr>`).join("")}</tbody></table>` : "<p class='lede'>None open.</p>"}
    <h3>Programs, jobs and PROCs (${(descriptions?.descriptions || []).length})</h3><table><tbody>${progRows}</tbody></table>`;
}

function viewReport(path) {
  return `<div class="cite">mftb report</div><h2><code>${esc(path)}</code></h2>
    <p class="lede">The page mftb rendered, unchanged. <a href="store/${esc(path)}" target="_blank" rel="noopener">Open in a new tab</a></p>
    <iframe class="report" src="store/${esc(path)}"></iframe>`;
}

function route() {
  const hash = location.hash.replace(/^#\/?/, "") || "step/1";
  renderNav(hash);
  const view = document.getElementById("view");
  const [kind, ...rest] = hash.split("/");
  const arg = decodeURIComponent(rest.join("/"));
  const stepViews = { 1: viewSweep, 2: viewModules, 3: viewCheck, 4: viewScans, 5: viewRollup, 6: viewDeep, 7: viewAsks, 8: viewMigration };
  if (kind === "step" && stepViews[arg]) view.innerHTML = stepViews[arg]();
  else if (kind === "module" && moduleIds.includes(arg)) view.innerHTML = viewModule(arg);
  else if (kind === "report" && /^reports\/[\w./-]+\.html$/.test(arg) && !arg.includes("..")) view.innerHTML = viewReport(arg);
  else view.innerHTML = viewSweep();
  window.scrollTo(0, 0);
}

loadStore().then(() => {
  const { meta } = state.estate;
  document.getElementById("metaLine").textContent =
    `${state.estate.summary.estate} · ${meta.tool} · generated ${meta.generated_at}`;
  window.addEventListener("hashchange", route);
  route();
}).catch((err) => {
  document.getElementById("view").innerHTML = `<div class="callout warn">Could not load the mftb store: ${esc(err.message)}</div>`;
});
