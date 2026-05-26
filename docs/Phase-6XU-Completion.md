# Phase 6XU Completion - UOW-1133 NPC Dialog Adapter Service-Facts Composition

Date: May 26, 2026

## Unit Of Work

UOW-1133: `[Phase 6][UOW-1133] Compose NPC dialog service fallback facts`

## Summary

UOW-1133 composes optional `NpcDialogServiceSelectFacts` into the top-level NPC dialog target input adapter. A single non-live `QuestDialogNpcTargetBranchRuntimeSnapshot` can now produce staged branch, optional interaction, optional controller, and optional `DialogService` fallback metadata.

This remains staged only. It does not call production `GameServerConnection`, live known-list lookup, live talk-range geometry, NPC AI, `DialogService`, QuestEngine, packet sends, trade/goods static-data lookup, or service side effects.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XU-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests\|NpcDialogControllerDispatchPlanServiceTests\|NpcDialogServiceSelectPlanServiceTests\|QuestDialogNpcTargetBranchPlanServiceTests\|NpcDialogInteractionAllowedPlanServiceTests" --nologo` | Passed: 71 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,298 tests. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1133

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `QuestDialogNpcTargetBranchInputAssemblyPlanService`; `QuestDialogNpcTargetBranchPlanService` | Packet / Non-Live Input Adapter | Partial | Unit Tested | Partial Parity | One snapshot can now compose branch, interaction, controller, and service fallback metadata in Java order. Production packet routing remains disabled. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` | `NpcDialogControllerDispatchPlanService` composed by top-level adapter | Controller Dispatch Dependency | Partial | Unit Tested | Partial Parity | Controller facts now carry optional service facts through the adapter. Live controller/AI execution remains disabled. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `NpcDialogServiceSelectPlan` reachable from top-level adapter | Service Fallback Dependency | Partial | Unit Tested | Partial Parity | Service fallback metadata can be produced from the top-level NPC-target snapshot. Planner remains descriptor-only with explicit facts. |
| `com.aionemu.gameserver.services.DialogService.isInteractionAllowed` | `NpcDialogInteractionAllowedPlanService` consumed before controller/service composition | Service Guard Dependency | Partial | Existing Unit Coverage | Partial Parity | Interaction plan still gates branch dispatch before controller/service composition. Live dependency resolution remains absent. |
| `com.aionemu.gameserver.dataholders.TradeListData` / `TradeListTemplate` | `NpcDialogServiceSelectFacts` passed through `QuestDialogNpcControllerDispatchFacts` | Static Data Dependency | Not Started | Unit Tested as explicit input | Needs Verification | BUY trade-list facts can flow through the full staged chain. No live trade/goods XML lookup, legion filtering, or static-data comparison. |
| `com.aionemu.gameserver.ai.NpcAI.onDialogSelect` | `QuestDialogNpcControllerDispatchFacts.NpcAiHandledDialogSelect` | AI Dispatch Dependency | Not Started | Unit Tested as explicit input | Needs Verification | AI handled state still gates service fallback composition. No live AI handler execution. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_ComposesDialogServicePlanThroughControllerDispatchFacts` | Full staged chain can produce a BUY `SM_TRADELIST` descriptor and Java integer modifier from top-level snapshot facts. | Source-reviewed Java branch order and BUY formula. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_DoesNotComposeDialogServicePlanWhenControllerShortCircuits` | AI-handled controller branch suppresses `DialogService` sub-plan even when service facts are supplied. | Source-reviewed Java AI boolean guard. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not consume the staged branch/controller/service composition.
- Known-list lookup, target typing, live interaction dependency resolution, talk-range geometry, NPC AI, QuestEngine, packet sends/serialization, trade/goods static-data lookup, legion filtering, service side effects, threading/player-ordering, and Java runtime comparison remain disabled.
- `DialogService` branch behavior remains grouped/descriptive for many actions; no Java runtime comparison has verified the composed planner chain.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live top-level adapter/service composition
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 6 blocked/partial categories: production routing, live known-list/type resolution, live controller/AI/range execution, live QuestEngine/packets, live static-data/service side effects, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit connects the staged `DialogService` fallback planner to the top-level NPC-target dialog adapter.

## Next Recommended Unit Of Work

Add a read-only trade-list/static-data input adapter for `DialogService` BUY and TRADE_IN facts: derive `HasTradeList`, `HasSellableTradeGoods`, `TradeSellPriceRate`, and `HasTradeInList` from C# static data while keeping legion level and production routing explicit/disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Trade-list fact adapter | New service/tests around trade-list and goods-list data | Medium | Recommended next; inspect Java `TradeListData`, `TradeListTemplate`, `GoodsListData`. |
| B | Dialog action registry warning audit | Static-data validation audit/tests | Medium | Compare Java `DialogAction.nameOf` behavior before loader warnings. |
| C | Talk-range geometry audit | New non-live range planner/tests | Medium | Requires careful Java `PositionUtil` review and C# world object geometry mapping. |

## Do Not Parallelize

- `GameServerConnection.cs`: production dialog routing remains high-risk.
- Existing NPC dialog adapter/controller/service planner files: assign exclusive ownership for composition work.
- Phase 6 progress/handoff docs: orchestrator-owned.
