# Phase 6XS Completion - UOW-1131 NPC DialogService Fallback Planning

Date: May 26, 2026

## Unit Of Work

UOW-1131: `[Phase 6][UOW-1131] Model NPC DialogService fallback planning`

## Summary

UOW-1131 adds a descriptor-only `NpcDialogServiceSelectPlanService` for the Java `DialogService.onDialogSelect` fallback path. It begins modeling the top-level fallback switch without live side effects: QuestEngine-or-next-page routing, BUY trade-list decisions, grouped dialog-window actions, selected service dispatch branches, trade-in, sell, and default next-page behavior.

This remains staged only. It does not call production `GameServerConnection`, live `QuestEngine`, `PacketSendUtility`, trade/goods static data, teleport, auto-group, pet/housing/craft/faction services, or packet serialization.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogServiceSelectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogServiceSelectPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XS-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "NpcDialogServiceSelectPlanServiceTests\|NpcDialogControllerDispatchPlanServiceTests\|QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests" --nologo` | Passed: 32 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,293 tests. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1131

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `Aion.GameServer.Services.NpcDialogServiceSelectPlanService` | Service Fallback Planner | Partial | Unit Tested | Partial Parity | Descriptor-only planner covers top-level `questId`, `BUY`, grouped dialog-window, selected service-dispatch, trade-in, sell, and default quest/page branches. Large live switch behavior remains unexecuted. |
| `com.aionemu.gameserver.services.DialogService.handleQuestDialogueOrSendNextPage` | `NpcDialogServiceSelectPlanService` quest/page descriptors | Service Helper | Partial | Unit Tested | Partial Parity | Models Java order as QuestEngine first, then conditional `SM_DIALOG_WINDOW`. Does not execute quest handlers, extended reward mutation, packet sends, or next-page serialization. |
| `com.aionemu.gameserver.services.DialogService.sendDialogWindow` | `NpcDialogServiceSelectPlanService` dialog-window descriptors | Service Helper | Partial | Unit Tested | Partial Parity | Models `NpcTemplate.supportsAction` gate and silent unsupported-action skip. Does not resolve `DialogPage.getByActionId` IDs or serialize `SM_DIALOG_WINDOW`. |
| `com.aionemu.gameserver.dataholders.TradeListData` / `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate` | `NpcDialogServiceSelectInput.HasTradeList`, `HasSellableTradeGoods`, `TradeSellPriceRate` | Static Data Dependency | Not Started | Unit Tested as explicit input | Needs Verification | BUY branch uses explicit facts only. No live trade-list lookup, goods-list lookup, legion-level filtering, or Java XML/static-data comparison. |
| `com.aionemu.gameserver.services.PricesService.getVendorBuyModifier` | `NpcDialogServiceSelectInput.VendorBuyModifier` | Config / Price Dependency | Partial | Unit Tested as explicit input | Needs Verification | Java integer formula `vendorBuyModifier * tradeModifier / 100` is represented. Config loading and runtime price service parity are not verified here. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onDialog` | `NpcDialogServiceDescriptorKind.QuestEngineDialog` | Quest Engine Dependency | Not Started | Unit Tested as descriptor | Needs Verification | Descriptor records the call only. Dynamic quest handlers, threading, quest env behavior, and handler return value remain unported here. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW` / `SM_TRADELIST` / `SM_SELL_ITEM` / `SM_TRADE_IN_LIST` / `SM_SYSTEM_MESSAGE` | `NpcDialogServiceDescriptor` packet descriptors | Packet Dependencies | Partial | Unit Tested as descriptors | Needs Verification | Descriptor kinds identify intended packets. Packet byte serialization and ordering are not verified by this planner. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `NpcDialogServiceSelectPlanServiceTests.CreatePlan_RoutesQuestIdThroughQuestEngineOrNextPage` | Non-zero quest IDs plan QuestEngine then conditional dialog-window descriptors with extended reward index. | Source-reviewed Java helper order. |
| `NpcDialogServiceSelectPlanServiceTests.CreatePlan_RoutesUseObjectAndExchangeCoinThroughQuestEngineEvenWithoutQuestId` | `USE_OBJECT` and `EXCHANGE_COIN` route through QuestEngine at `questId == 0`. | Source-reviewed Java condition. |
| `NpcDialogServiceSelectPlanServiceTests.CreatePlan_PlansBuyTradeListWhenTradeListAndSellableGoodsExist` | BUY plans `SM_TRADELIST` and preserves Java integer modifier formula. | Source-reviewed Java formula. |
| `NpcDialogServiceSelectPlanServiceTests.CreatePlan_PlansBuyUnavailableWhenTradeListOrSellableGoodsAreMissing` | Missing trade list or no allowed goods plans the Java does-not-sell system message. | Source-reviewed Java branches. |
| `NpcDialogServiceSelectPlanServiceTests.CreatePlan_PlansDialogWindowOnlyWhenNpcSupportsAction` | Supported actions plan dialog window; unsupported actions silently produce no descriptors. | Source-reviewed Java helper. |
| `NpcDialogServiceSelectPlanServiceTests.CreatePlan_PlansKnownServiceDispatchActions` | Selected Java service branches produce descriptor-only side-effect intents. | Source-reviewed Java switch. |
| `NpcDialogServiceSelectPlanServiceTests.CreatePlan_PlansSellItemWindowForSellActions` | Sell actions plan `SM_SELL_ITEM` descriptor. | Source-reviewed Java switch. |
| `NpcDialogServiceSelectPlanServiceTests.CreatePlan_PlansTradeInFromExplicitTradeInListAvailability` | Trade-in list availability controls `SM_TRADE_IN_LIST` vs does-not-sell system message. | Source-reviewed Java branch. |
| `NpcDialogServiceSelectPlanServiceTests.CreatePlan_DefaultQuestIdZeroActionFallsBackToNextPageDescriptor` | Unknown/default actions use quest/page helper descriptors. | Source-reviewed Java default branch. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` and the staged controller fallback descriptor do not yet consume `NpcDialogServiceSelectPlanService`.
- The planner is descriptor-only: no live `QuestEngine`, `PacketSendUtility`, trade/goods static-data lookup, legion-level filtering, teleport, auto-group, pet, housing, craft, faction, item-charge, warehouse, cube, or recovery execution occurs.
- `DialogPage.getByActionId`, packet byte serialization, service side effects, threading/player-ordering, audit/log behavior, date/time, precision beyond the BUY integer formula, and Java runtime comparison remain unverified.
- Many `DialogService.onDialogSelect` branches are grouped as service descriptors rather than fully modeled branch-specific plans.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 partial descriptor-only `DialogService.onDialogSelect` fallback planner
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 7 blocked/partial categories: production routing, live QuestEngine, live packet sends/serialization, trade/goods static-data lookup, service side-effect execution, Java dialog action warning behavior, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit begins the staged `DialogService.onDialogSelect` fallback planner needed behind NPC AI false returns.

## Next Recommended Unit Of Work

Compose `NpcDialogServiceSelectPlanService` behind `NpcDialogControllerDispatchPlanService` fallback descriptors as an optional non-live `DialogService` sub-plan, preserving AI-handled and out-of-range short-circuit behavior.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Controller fallback composition | `NpcDialogControllerDispatchPlanService.cs` and tests | Medium | Recommended next; keep optional and descriptor-only. |
| B | Dialog action registry warning audit | Static-data validation audit/tests | Medium | Compare Java `DialogAction.nameOf` behavior before loader warnings. |
| C | Trade-list static-data input adapter | New planner/tests around trade/goods facts | Medium | Needs `TradeListData` and `GoodsListData` XML review; avoid live packet routing. |

## Do Not Parallelize

- `GameServerConnection.cs`: production dialog routing remains high-risk.
- Existing dialog adapter/controller planner files: assign exclusive ownership if the next unit composes planner layers.
- Phase 6 progress/handoff docs: orchestrator-owned.
