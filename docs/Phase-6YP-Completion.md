# Phase 6YP Completion - UOW-1154 Trade-List Limited-Item Composition

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by composing staged limited-item facts into `SM_TRADELIST` packet plans and the production non-sending `BUY` boundary. The C# server still does not send trade-list or no-sell packets from `BUY`.

The Java implementation is the source of truth:
- `CM_DIALOG_SELECT` reaches `NpcController.onDialogSelect`.
- `DialogService.onDialogSelect` creates `SM_TRADELIST` for sellable goods.
- `SM_TRADELIST` adds limited items from `LimitedItemTradeService.getLimitedTradeNpc(tlist.getNpcId())`.
- `LimitedItem.getBuyCount(playerObjId)` defaults to `0` until purchase mutation changes it.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YP-Completion.md`

## Implementation Notes

- Added optional `LimitedItemFactInput` to `QuestDialogNpcTargetBranchRuntimeSnapshot`.
- `QuestDialogNpcTargetBranchInputAssemblyPlanService` now composes a `NpcDialogLimitedItemFactAdapterPlan` only for the staged Java-style `BUY` fallback path.
- Ready `SmTradeListPacketPlan` instances now receive limited-item packet rows from the adapter.
- `GameServerConnection.CreateNonLiveBuyDialogSelectPlan` now supplies staged limited-item input from the player and NPC ids.
- The socket-boundary fixture now proves a limited item flows from XML to the observed non-live packet plan with default buy count `0`.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests|GameServerConnectionStorageExpansionDialogTests|NpcDialogLimitedItemFactAdapterServiceTests|SmTradeListPacketPlanServiceTests" --nologo` passed 36 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,163 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,370 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` / `CreateNonLiveBuyDialogSelectPlan` | Packet / Handler | Partial | Unit Tested | Partial Parity | Production `BUY` remains non-sending but now supplies limited-item facts into staged plans. Live Java controller routing and packet sends remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `Aion.GameServer.Services.SmTradeListPacketPlan` / `SmTradeList` | Packet Plan / Packet | Partial | Unit Tested | Partial Parity | Packet plans now carry limited-item rows sourced from static goods-list limits. Serializer exists, but live sends and Java runtime golden vectors remain missing. |
| `com.aionemu.gameserver.services.LimitedItemTradeService` | `NpcDialogLimitedItemFactAdapterService` composed by `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Service Adapter | Partial | Unit Tested | Partial Parity | Adapter is now part of staged `BUY` plan composition. Java singleton map lifecycle, cron reset scheduling, mutable sell counts, and purchase mutation remain missing. |
| `com.aionemu.gameserver.model.limiteditems.LimitedItem` | `NpcDialogLimitedItemFact` / `SmTradeListLimitedItemSummary` | Model / DTO | Partial | Unit Tested | Needs Verification | Production boundary can emit default buy count `0` and static sell limit into packet plan rows. Runtime buy counts and sell-limit mutation are not live. |
| `com.aionemu.gameserver.model.templates.goods.GoodsList` | `GoodsListSummary` through production `BUY` fixture | Static Data DTO | Partial | Unit Tested | Partial Parity | Production boundary fixture proves limited item rows flow from XML into packet plans. Full Java corpus comparison remains unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_ComposesDialogServiceFactsFromStaticTradeData` | Unit / Planner Composition | `SM_TRADELIST` constructor; `LimitedItemTradeService.start` | Static limited-item facts are composed into a ready packet plan alongside trade-list facts. | Source-reviewed Java paths plus deterministic C# planner test. | No live mutation or Java runtime capture. |
| `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_BuyTradeListRemainsDisabledAtSocketBoundaryUntilRoutingReady` | Unit / Socket Boundary Regression | `CM_DIALOG_SELECT`; `DialogService.onDialogSelect BUY`; `SM_TRADELIST` | Production `BUY` observes a non-live ready packet plan that includes a limited-item row and still sends no packets. | Source-reviewed Java route plus production C# handler test. | No live send, no runtime buy count mutation, no Java packet bytes. |

## Remaining Risks

- Live limited-item service lifecycle, cron reset scheduling, sell-limit decrement, and per-player buy-count mutation remain missing.
- `SmTradeList` still is not sent by `GameServerConnection`.
- Java runtime golden-vector comparison is still absent.
- Player legion level and vendor buy modifier are still staged defaults at the production boundary.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 staged limited-item composition path into production `BUY` packet plans
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: live limited-item mutation, cron reset scheduling, Java runtime packet vectors, price/legion runtime facts, and live send wiring
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add non-live runtime fact inputs for vendor buy modifier and player legion level, or begin Java runtime golden-vector design/capture notes before any live `BUY` send enablement.

Recommended starting points:
- `game-server/src/com/aionemu/gameserver/services/trade/PricesService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`

Keep live sends disabled until runtime price/legion facts, limited-item mutation, Java runtime golden vectors, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: replace staged vendor modifier and legion defaults with explicit non-live runtime fact seams.
- Why: packet plans now carry trade tabs and limited rows, but price and legion facts are still hard-coded at the production boundary.
- Files: `GameServerConnection.cs`, focused tests, possibly small adapter services.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java runtime golden-vector design notes | docs only | Low | Useful before claiming packet verified parity. |
| B | Read-only `SM_TRADE_IN_LIST` Java audit | none | Low | Safe analysis for a later trade-in slice. |
| C | Static-data limited-item corpus count comparison design | docs/test planning | Low | Do not claim parity until Java-generated counts exist. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Price/legion runtime fact seam | focused service/tests/docs | live packet sends |
| Agent A | Java runtime golden-vector design notes | docs only | code files |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
