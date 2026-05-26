# Phase 6XW Completion - UOW-1135 NPC Dialog Static Trade-Fact Composition

Date: May 26, 2026

## Unit Of Work

UOW-1135: `[Phase 6][UOW-1135] Compose NPC dialog static trade facts`

## Summary

UOW-1135 composes the UOW-1134 trade-list fact adapter into the top-level NPC dialog target input assembly path. A staged `QuestDialogNpcTargetBranchRuntimeSnapshot` can now carry optional trade-list fact input, optional static-data tables, and produce audit-visible `NpcDialogTradeListFactAdapterPlan` metadata plus a composed `NpcDialogServiceSelectPlan`.

This remains staged only. It does not call production `GameServerConnection`, live known-list lookup, live player legion lookup, live `PricesService`, live `DialogService`, packet sends, packet serialization, Java validation warnings, or Java runtime comparison.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XW-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests\|NpcDialogTradeListFactAdapterServiceTests\|NpcDialogControllerDispatchPlanServiceTests\|NpcDialogServiceSelectPlanServiceTests" --nologo` | Passed: 44 tests. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 2,099 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,306 tests. |

## Migration Parity Table - UOW-1135

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Services.QuestDialogNpcTargetBranchInputAssemblyPlanService` | Packet / Non-Live Input Adapter | Partial | Unit Tested | Partial Parity | Top-level staged snapshot can derive static-data-backed BUY facts before controller/service fallback composition. Production packet routing remains disabled. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` | `QuestDialogNpcTargetBranchInputAssemblyPlanService` -> `NpcDialogControllerDispatchPlanService` | Controller Dispatch Dependency | Partial | Unit Tested | Partial Parity | Static trade facts are derived only when controller facts indicate Java would fall through talk range and AI to `DialogService`. Live controller/AI execution remains disabled. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `NpcDialogTradeListFactAdapterPlan` and composed `NpcDialogServiceSelectPlan` | Service Fallback Dependency | Partial | Unit Tested | Partial Parity | BUY facts can be derived from static tables and then consumed by the staged service planner. Still descriptor-only; no live packet sends. |
| `com.aionemu.gameserver.dataholders.TradeListData` | `TradeListTable` supplied to `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Static Data Dependency | Partial | Unit Tested | Needs Verification | Static table is optional top-level adapter input. No Java `DataManager` runtime comparison or validation warning parity. |
| `com.aionemu.gameserver.dataholders.GoodsListData` | `GoodsListTable` supplied to `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Static Data Dependency | Partial | Unit Tested | Needs Verification | Ordinary goods-list legion filtering can feed top-level BUY facts. Item payload semantics remain absent. |
| `com.aionemu.gameserver.services.PricesService.getVendorBuyModifier` | `NpcDialogTradeListFactAdapterInput.VendorBuyModifier` in runtime snapshot | Service Dependency | Partial | Unit Tested as explicit input | Needs Verification | Runtime price lookup remains explicit input; no config/service call parity yet. |
| `com.aionemu.gameserver.ai.NpcAI.onDialogSelect` | `QuestDialogNpcControllerDispatchFacts.NpcAiHandledDialogSelect` | AI Dispatch Dependency | Not Started | Unit Tested as explicit input | Needs Verification | AI-handled branch suppresses static trade-fact derivation and service composition. No live AI dispatch. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_ComposesDialogServiceFactsFromStaticTradeData` | Top-level snapshot plus static tables can produce BUY `SM_TRADELIST` descriptor and Java integer price modifier. | Source-reviewed Java branch/controller/service order and BUY loop. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_PrefersExplicitDialogServiceFactsOverStaticTradeData` | Explicit facts keep precedence over optional static-data derivation. | Deterministic C# regression; this precedence is a staging convention. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_DoesNotDeriveStaticTradeFactsWhenControllerShortCircuits` | AI-handled controller branch suppresses static fact derivation and `DialogService` composition. | Source-reviewed Java AI boolean guard. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not consume this staged top-level adapter.
- Live known-list lookup, target typing, player legion level, vendor buy modifier lookup, NPC AI, QuestEngine, packet sends/serialization, service side effects, threading/player-ordering, Java validation warnings, and Java runtime comparison remain disabled.
- Static-data-derived facts are optional top-level inputs; no live `DataManager` equivalent has been wired into production runtime.
- Explicit-facts precedence is a staged C# compatibility behavior; Java has no equivalent because it resolves from live/static services directly.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live top-level adapter composition
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 7 blocked/partial categories: production routing, live known-list/type resolution, live player/NPC fact sourcing, live price service lookup, packet/service side effects, Java validation warnings, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit composes the new static trade-list adapter into the staged NPC dialog snapshot pipeline.

## Next Recommended Unit Of Work

Start the dialog action registry warning audit: compare Java `NpcData.isFunctionDialog`, `NpcTemplate.supportsAction`, and `DialogAction.nameOf` warning/audit behavior against the C# staged branch plan so unsupported/unknown action metadata is explicit before production routing.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Dialog action registry warning audit | Static-data validation/audit planner/tests | Medium | Recommended next; focus on unsupported function actions and unknown `DialogAction.nameOf` metadata. |
| B | Talk-range geometry audit | New non-live range planner/tests | Medium | Requires Java `PositionUtil` review and C# world object geometry mapping. |
| C | Trade-list adapter runtime-readiness audit | Docs/tests only unless routing is clearly ready | Medium | Identify exact live dependencies before any `GameServerConnection` wiring. |

## Do Not Parallelize

- `GameServerConnection.cs`: production dialog routing remains high-risk.
- Existing NPC dialog adapter/controller/service planner files: assign exclusive ownership for composition work.
- `StaticData.cs`: shared parser surface; avoid concurrent edits.
- Phase 6 progress/handoff docs: orchestrator-owned.
