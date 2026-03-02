<template>
  <div class="appBg">
    <router-view />
  </div>
</template>

<style>
/* 全局更干净的视觉 */
:root {
  --bg: #f6f7fb;
  --card: rgba(255, 255, 255, 0.92);
  --border: rgba(15, 23, 42, 0.08);
  --text: #0f172a;
  --muted: rgba(15, 23, 42, 0.58);
}

* { box-sizing: border-box; }

html, body, #app {
  height: 100%;
  margin: 0;
  font-family: ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, "PingFang SC",
  "Hiragino Sans GB", "Microsoft YaHei", "Noto Sans CJK SC", Arial, "Apple Color Emoji",
  "Segoe UI Emoji";
  color: var(--text);
}

.appBg {
  min-height: 100%;
  background:
      radial-gradient(1200px 800px at 20% 10%, #e8f1ff 0%, #f7f9fc 45%, #f6f7fb 100%);
}
</style>
