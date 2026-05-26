# Phase 6ZF Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1170
Status: Phase 6 continues; trade-list/trade-in live sends remain disabled.

## Session Summary

UOW-1170 implemented the narrow legion-level hydration path identified by UOW-1169. C# can now carry a hydrated player legion level into the staged trade-list runtime facts, while preserving Java's no-legion fallback and keeping live sends disabled.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/TradeList-LegionLevel-LiveLookup-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ZF-Completion.md`

## What Changed

- Added `Player.LegionLevel` with a Java breadcrumb for `Player.getLegion().getLegionLevel()`.
- Extended `PlayerEnterWorldRepository.LoadPlayerAsync` to select and hydrate `l.level AS legion_level`.
- Updated `GameServerConnection.CreateNonLiveTradeDialogSelectPlan` to pass `PlayerLegionLevel` only when `player.LegionId != 0`.
- Added `HandleDialogSelectAsync_BuyRestrictedGoodsUsesHydratedLegionLevelWithoutSending`.
- Updated `TradeList-LegionLevel-LiveLookup-Audit.md` with the UOW-1170 implementation note.

Live `SM_TRADELIST`, `SM_TRADE_IN_LIST`, and no-sell packet sends remain disabled.

## Validation

- `git diff --check` passed with existing line-ending warnings only.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "GameServerConnectionStorageExpansionDialogTests|NpcDialogTradeRuntimeFactAdapterServiceTests|NpcDialogTradeListFactAdapterServiceTests" --nologo` passed 17 tests.
- First full `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` timed out at 184 seconds before returning results.
- Rerunning with a longer timeout passed 2,183 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,390 tests.

## Migration Parity Table - UOW-1170

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model | Partial | Unit Tested | Partial Parity | C# now projects `LegionLevel` for trade planning. It remains a projected field, not a full Java `LegionMember`/`Legion` object graph. |
| `com.aionemu.gameserver.dao.LegionDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.LoadPlayerAsync` | Repository | Partial | Regression Tested | Partial Parity | C# now selects and hydrates `legions.level` during enter-world player load. Java runtime DAO execution is source-reviewed, not runtime-compared. |
| `com.aionemu.gameserver.dao.LegionMemberDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.LoadPlayerAsync` | Repository | Partial | Regression Tested | Partial Parity | C# already joined `legion_members`; this unit uses that membership to decide whether to pass a projected level. No live member cache exists. |
| `com.aionemu.gameserver.services.DialogService` | `Aion.GameServer.Network.Aion.GameServerConnection.CreateNonLiveTradeDialogSelectPlan` / `NpcDialogTradeRuntimeFactAdapterService` | Service Boundary | Partial | Unit Tested | Partial Parity | Non-live boundary now passes hydrated legion level for legion players and preserves Java no-legion fallback for non-members. Live sends remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `Aion.GameServer.Services.SmTradeListPacketPlanService` / `SmTradeList` | Packet / Plan Consumer | Partial | Unit Tested | Partial Parity | Staged plan now receives the hydrated level and can include restricted tabs when allowed. Java runtime vectors and live packet sends are still missing. |
| `com.aionemu.gameserver.model.team.legion.Legion` | `Aion.GameServer.Model.GameObjects.Player.LegionLevel` projection | Model / Runtime Fact | Partial | Unit Tested | Needs Verification | The level value is projected from DB; C# still lacks Java's live cached `Legion` owner, level-up mutation, and session update behavior. |
| `com.aionemu.gameserver.services.LegionService` | future C# live legion service/cache | Service | Not Started | No Tests | Needs Verification | No live C# service/cache was added. Session-time level changes remain unimplemented. |

Tests added or updated:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_BuyRestrictedGoodsUsesHydratedLegionLevelWithoutSending` | Unit / Socket Boundary | `DialogService.onDialogSelect BUY`; `SM_TRADELIST` constructor legion filter; `Player.getLegion().getLegionLevel` | A player with `LegionId = 77` and `LegionLevel = 5` carries the level into staged runtime facts, passes the `legion_lvl="5"` goods-list filter, creates a non-live trade-list packet plan, and sends no packets. | Deterministic C# boundary regression from source-reviewed Java branch and packet constructor behavior. | Does not execute Java runtime, does not run DB integration, and does not validate live session level changes. |

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 narrow runtime fact projection/handoff path
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: Java legion trade vectors, live legion service/cache, DB integration proof for the new projection, C# artifact verifier, and live packet sends
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Java runtime vectors for legion-restricted and mixed-legion trade-list scenarios are still missing.
- C# still lacks a live `LegionService`/cache owner for level changes during an active session.
- The repository SQL hydration was source-reviewed and compile/test validated, but not DB-integration executed in this unit.
- Live `SM_TRADELIST`, `SM_TRADE_IN_LIST`, and no-sell sends remain disabled.
- Vendor-buy modifier ownership remains staged through configuration and still needs a live ownership audit before live sends.

## Next Recommended Unit of Work

Primary next unit:

- Audit vendor-buy modifier live ownership for trade-list `BUY` planning.

Suggested scope:

- Read Java `PricesService.getVendorBuyModifier()` and `PricesConfig.VENDOR_BUY_MODIFIER`.
- Compare C# `GameServerOptions.Prices.VendorBuyModifier` and any configuration binding/loading path.
- Document whether the staged config path is sufficient or whether a live config owner is missing.
- Keep `NpcDialogTradeRuntimeFactAdapterPlan.IsLive = false` and all trade packet sends disabled.

Safe parallel candidates:

- Java generator skeleton feasibility audit focused on Maven/test layout.
- C# vector verifier artifact reader design, without generated Java artifacts.
- DB integration proof for `Player.LegionLevel` hydration if an integration database is available.

Do not start live packet send wiring until Java vector artifacts and C# verifier tests exist.
