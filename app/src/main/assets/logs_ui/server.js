// 网页服务相关逻辑：请求日志列表、渲染卡片、触发下载/生成
//
// 约定后端接口：
// - GET /api/logs/list -> JSON Array [{ name, size, lastModified, type }]
// - GET /api/logs/{filename} -> 下载（保持原文件名）
// - GET /api/logs/create-manual-and-download -> 生成并下载

(function () {
  "use strict";

  function $(id) {
    return document.getElementById(id);
  }

  function inferTypeFromName(name) {
    if (typeof name !== "string") return "unknown";
    if (name.startsWith("logs_manual_")) return "manual";
    if (name.startsWith("logs_crash_")) return "crash";
    if (name.startsWith("logs_interaction_")) return "interaction";
    return "unknown";
  }

  // 显示名称：去掉前缀，格式化日期时间
  function formatDisplayName(name) {
    if (typeof name !== "string") return "";

    var s = name;
    if (s.startsWith("logs_manual_")) s = s.substring("logs_manual_".length);
    if (s.startsWith("logs_crash_")) s = s.substring("logs_crash_".length);
    if (s.startsWith("logs_interaction_")) s = s.substring("logs_interaction_".length);

    // 日期格式化：2026-02-20 -> 2026/02/20
    s = s.replace(/^(\d{4})-(\d{2})-(\d{2})/, "$1/$2/$3");
    // 日期与时间之间的下划线替换为空格
    s = s.replace(/_/g, " ");
    return s;
  }

  async function fetchLogList() {
    var resp = await fetch("/api/logs/list", { cache: "no-store" });
    if (!resp.ok) {
      var text = "";
      try {
        text = await resp.text();
      } catch (e) {
        text = "";
      }
      throw new Error("list failed: " + resp.status + (text ? " " + text : ""));
    }
    var data = await resp.json();
    return Array.isArray(data) ? data : [];
  }

  function clearNode(node) {
    while (node.firstChild) node.removeChild(node.firstChild);
  }

  function makeLogCard(item) {
    var originalName = item && item.name ? String(item.name) : "";
    var displayName = formatDisplayName(originalName);

    var card = document.createElement("mdui-card");
    card.className = "log-card";
    card.setAttribute("clickable", "");

    var downloadBtn = document.createElement("mdui-button-icon");
    downloadBtn.className = "download-btn";
    downloadBtn.setAttribute("icon", "download");
    downloadBtn.setAttribute("selectable", "");
    downloadBtn.setAttribute("href", "/api/logs/" + encodeURIComponent(originalName));
    downloadBtn.setAttribute("download", originalName);

    var bar = document.createElement("div");
    bar.className = "bar";

    var left = document.createElement("div");
    left.className = "side side--left";
    left.appendChild(downloadBtn);

    var title = document.createElement("div");
    title.className = "log-name";
    title.textContent = displayName || originalName;

    var right = document.createElement("div");
    right.className = "side side--right";
    right.setAttribute("aria-hidden", "true");

    bar.appendChild(left);
    bar.appendChild(title);
    bar.appendChild(right);
    card.appendChild(bar);

    return card;
  }

  function renderList(container, items) {
    clearNode(container);
    for (var i = 0; i < items.length; i++) {
      container.appendChild(makeLogCard(items[i]));
    }
  }

  function splitItems(items) {
    var manual = [];
    var crash = [];
    var interaction = [];

    for (var i = 0; i < items.length; i++) {
      var it = items[i] || {};
      var type = it.type || inferTypeFromName(it.name);
      if (type === "manual") manual.push(it);
      else if (type === "crash") crash.push(it);
      else if (type === "interaction") interaction.push(it);
    }

    manual.sort(function (a, b) {
      return (b.lastModified || 0) - (a.lastModified || 0);
    });
    crash.sort(function (a, b) {
      return (b.lastModified || 0) - (a.lastModified || 0);
    });
    interaction.sort(function (a, b) {
      return (b.lastModified || 0) - (a.lastModified || 0);
    });

    return { manual: manual, crash: crash, interaction: interaction };
  }

  var state = {
    manualListId: null,
    crashListId: null,
    interactionListId: null,
  };

  async function refresh() {
    var manualEl = $(state.manualListId);
    var crashEl = $(state.crashListId);
    var interactionEl = $(state.interactionListId);
    if (!manualEl || !crashEl) return;

    var items = await fetchLogList();
    var split = splitItems(items);
    renderList(manualEl, split.manual);
    renderList(crashEl, split.crash);
    if (interactionEl) renderList(interactionEl, split.interaction);
  }

  async function createManualAndDownload() {
    window.open("/api/logs/create-manual-and-download", "_blank");
    setTimeout(function () {
      refresh().catch(function () { });
    }, 1500);
  }

  function init(opts) {
    state.manualListId = opts && opts.manualListId ? opts.manualListId : "manualLogList";
    state.crashListId = opts && opts.crashListId ? opts.crashListId : "crashLogList";
    state.interactionListId = opts && opts.interactionListId ? opts.interactionListId : "interactionLogList";

    refresh().catch(function () { });
  }

  window.LogService = {
    init: init,
    refresh: refresh,
    createManualAndDownload: createManualAndDownload
  };
})();
