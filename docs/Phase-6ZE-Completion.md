# Phase 6ZE Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1169
Status: Phase 6 continues; trade-list/trade-in live sends remain disabled.

## Session Summary

UOW-1169 audited the Java live legion-level lookup path used by trade-list `BUY` filtering and compared it to the current C# staged runtime-fact boundary.

Files changed:

- `docs/TradeList-LegionLevel-LiveLookup-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ZE-Completion.md`

No production Java, C# runtime code, packet send wiring, or socket behavior changed.

## What Changed

- Added `TradeList-LegionLevel-LiveLookup-Audit.md`.
- Documented Java's branch-level and packet-constructor-level goods-list filtering by player legion level.
- Traced Java hydration through `Player.getLegion`, `LegionMember`, `LegionService`, `LegionDAO.loadLegion`, and `legions.level`.
- Identified the C# gap:
  - `Player` has `LegionId`, name, and emblem fields, but no `LegionLevel`.
  - `PlayerEnterWorldRepository` joins `legions`, but does not select `l.level`.
  - `GameServerConnection` passes only `player.LegionId` into `NpcDialogTradeRuntimeFactAdapterService`, so the adapter falls back to Java's no-legion level `0`.
- Defined the narrow next implementation path:
  - add `Player.LegionLevel`;
  - select and hydrate `l.level AS legion_level`;
  - pass `PlayerLegionLevel: player.LegionLevel` into the trade runtime adapter;
  - keep `IsLive = false` and all trade sends disabled until vector/verifier gates are complete.

## Validation

- `git diff --check` passed.

No `dotnet test` run was required because this was documentation-only and changed no executable code.

## Migration Parity Table - UOW-1169

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.DialogService` | `Aion.GameServer.Services.NpcDialogTradeRuntimeFactAdapterService` / `NpcDialogTradeListFactAdapterService` | Service / Runtime Fact Consumer | Partial | Manual Only | Needs Verification | Java branch-level goods-list filtering by legion level is audited. C# still supplies staged `0` unless tests inject a level. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `Aion.GameServer.Network.Aion.ServerPackets.SmTradeList` / `SmTradeListPacketPlanService` | Packet / Runtime Fact Consumer | Partial | Manual Only | Needs Verification | Java packet constructor repeats legion-level filtering. Future C# live send path must feed the same level into branch and packet-plan filtering. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model | Partial | Manual Only | Partial Parity | C# has `LegionId`, name, and emblem fields, but no `LegionLevel`; audit identifies this as the narrow hydration gap. |
| `com.aionemu.gameserver.model.team.legion.LegionMember` | future C# legion member/session model or projected player fields | Model | Not Started | Manual Only | Needs Verification | Java `Player.getLegion()` depends on attached `LegionMember`. C# currently uses projected player legion fields and lacks a live member object. |
| `com.aionemu.gameserver.model.team.legion.Legion` | future C# legion model or `Player.LegionLevel` projection | Model | Partial | Manual Only | Needs Verification | Java `Legion.getLegionLevel()` reads cached `legionLevel`, default `1`, hydrated from DB. C# can project `legions.level` but does not yet. |
| `com.aionemu.gameserver.services.LegionService` | future C# live legion service/cache | Service | Not Started | Manual Only | Needs Verification | Java owns cached legion lookup, disband checks, and member cache. C# has no equivalent live service for session-level level changes. |
| `com.aionemu.gameserver.dao.LegionDAO` | `Aion.GameServer.Data.PlayerEnterWorldRepository` future join projection | Repository | Partial | Manual Only | Needs Verification | Java loads `legions.level`; C# enter-world query joins `legions` but currently selects only name/emblem fields. |
| `com.aionemu.gameserver.dao.LegionMemberDAO` | `Aion.GameServer.Data.PlayerEnterWorldRepository` `legion_members` join | Repository | Partial | Manual Only | Partial Parity | C# already joins `legion_members` for `LegionId`; audit identifies adding `l.level` as the next narrow data projection. |

Tests added or updated:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Manual / Docs Only | `DialogService`; `SM_TRADELIST`; `Player`; `LegionMember`; `Legion`; `LegionService`; `LegionDAO`; `LegionMemberDAO` | No executable tests were added; this unit documents the source lookup path and current C# hydration gap. | Java and C# source reviewed; `git diff --check` passed. | No C# `Player.LegionLevel` hydration yet; no Java runtime vectors for legion-restricted vendors. |

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 legion-level live lookup audit document added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: C# legion-level hydration, live legion service/cache, Java legion vendor vectors, C# artifact verifier, and live packet sends
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- C# still defaults `PlayerLegionLevel` to `0` at the trade boundary in production planning.
- `PlayerEnterWorldRepository` does not yet select or hydrate `legions.level`.
- C# has no live `LegionService`/cache owner for level changes during a session.
- Java runtime vectors for `buy-legion-restricted` and `buy-mixed-legion-tabs` are still missing.
- Live `SM_TRADELIST`, `SM_TRADE_IN_LIST`, and no-sell sends remain disabled.

## Next Recommended Unit of Work

Primary next unit:

- Implement narrow C# `Player.LegionLevel` hydration and adapter handoff while keeping `NpcDialogTradeRuntimeFactAdapterPlan.IsLive = false` and all trade packet sends disabled.

Suggested scope:

- Add `LegionLevel` to `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`.
- Select `l.level AS legion_level` in `PlayerEnterWorldRepository`.
- Hydrate `Player.LegionLevel`.
- Pass `PlayerLegionLevel: player.LegionLevel` in `GameServerConnection.CreateNonLiveTradeDialogSelectPlan`.
- Add focused tests for no-legion default, hydrated legion level, and adapter handoff.
- Do not mark runtime facts live and do not enable live packet sends.

Safe parallel candidates:

- Vendor-buy modifier live ownership audit.
- Java generator skeleton feasibility audit focused on Maven/test layout.
- C# vector verifier artifact reader design, without comparing artifacts until Java output exists.

Do not start live packet send wiring until Java vector artifacts and C# verifier tests exist.
