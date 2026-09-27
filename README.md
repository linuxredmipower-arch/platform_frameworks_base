# Frameworks Base — Miku UI TDA 适配（A15 线）

`frameworks/base` 的 Miku UI 适配补丁，**仅 A15 线**。

## 项目

**Miku UI TDA** —— 基于 AOSP 的 Android 定制 ROM。

- **当前线**：Android 16（`Blooming_v2` = `android-16.0.0_r4` + LineageOS 23.2），设备 **marble / POCO F5**
- **本仓库属于**：A15 线（`Vampire_v3`），**已冻结**

## 本仓库

| 项 | 值 |
|---|---|
| 树内路径 | `frameworks/base` |
| 上游 | [`Miku-UI/platform_frameworks_base`](https://github.com/Miku-UI/platform_frameworks_base) |
| 我们的分支 | `miku-a15`（A15 线，已冻结） |

**`miku-a15` 上的两个补丁**：

| # | 补丁 |
|---|---|
| 35 | QS 透明化 |
| 36 | QS 主题遮罩 |

## ⚠️ 这两项**不移植到 A16**（已决策，非疏漏）

A16 的 `ScrimController.setClipsQsScrim()` 首行即

```java
if (Flags.notificationShadeBlur() || Flags.bouncerUiRevamp()) { mClipsQsScrim = false; return; }
```

而本构建 release（`trunk_staging`）下该 flag 为 **ENABLED** → A15 补丁依赖的 `mClipQsScrim` 路线在 A16 **恒为 false**，整套改动（含 Miku 残留的 `MIKU_ALPHA`）都是死代码。**A16 走 blur 面板路径，保持 QS/QQS 默认观感。**

所以 A16 线的 `frameworks/base` 用的是 **Miku 官方 `Blooming_v2` 原样**，本 fork **没有 `miku-a16` 分支**。将来若要移植，需另开分支。

## 相关文档

- 全部 fork 的关系图与清单：`linuxredmipower-arch/miku_docs` → `repo_map.md`（「关键四」）
- 决策全文：`miku_docs/debug/2026-09-27-a16-qs-scope-decision.md`
