# Phase 6XV Completion - UOW-1134 NPC Dialog Trade-List Fact Adapter

Date: May 26, 2026

## Unit Of Work

UOW-1134: `[Phase 6][UOW-1134] Add NPC dialog trade-list fact adapter`

## Summary

UOW-1134 adds a read-only static-data adapter for staged `DialogService` BUY and TRADE_IN facts. The C# port now parses Java `npc_trade_list` and `goodslists` XML shapes into lightweight tables and derives `HasTradeList`, `HasSellableTradeGoods`, `TradeSellPriceRate`, and `HasTradeInList` for the existing descriptor-only dialog planner.

This remains staged only. It does not call production `GameServerConnection`, live player legion lookup, live `PricesService`, live `DialogService`, packet sends, packet serialization, Java static-data validation warnings, or Java runtime comparison.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/TradeListTable.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeListFactAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogTradeListFactAdapterServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XV-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "NpcDialogTradeListFactAdapterServiceTests\|StaticData_LoadsTradeListsAndGoodsListsLikeJavaDataholders\|NpcDialogServiceSelectPlanServiceTests" --nologo` | Passed: 22 tests. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 2,096 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,303 tests. |

## Migration Parity Table - UOW-1134

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `Aion.GameServer.Services.NpcDialogTradeListFactAdapterService`; `NpcDialogServiceSelectFacts` | Service Fact Adapter | Partial | Unit Tested | Partial Parity | BUY and TRADE_IN fact derivation follows source-reviewed Java checks. No live `DialogService`, packet sends, player object, or Java runtime comparison. |
| `com.aionemu.gameserver.dataholders.TradeListData` | `Aion.GameServer.Dataholders.TradeListTable`; `StaticData.TradeLists` | Static Data Holder | Partial | Unit Tested | Needs Verification | Parses ordinary, trade-in, and purchase templates by NPC id. Java validation warnings and full template behavior are not ported. Duplicate handling is last-write-wins like Java map assignment. |
| `com.aionemu.gameserver.dataholders.GoodsListData` | `Aion.GameServer.Dataholders.GoodsListTable`; `StaticData.GoodsLists` | Static Data Holder | Partial | Unit Tested | Needs Verification | Parses ordinary, trade-in, and purchase list ids plus `legion_lvl`. Item entries, advertise/gossip, sales limits, and Java validation are not ported. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate` | `Aion.GameServer.Dataholders.TradeListTemplateSummary` | DTO / Template Projection | Partial | Unit Tested | Needs Verification | Preserves `npc_id`, referenced goods-list ids, `npc_type`, price rates, buy rate, and save count. Other Java methods/JAXB behavior remain unmodeled. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeTab` | `TradeListTemplateSummary.GoodsListIds` | DTO / Template Projection | Partial | Unit Tested | Needs Verification | Captures tab `id` values used by Java BUY to look up ordinary goods lists. No separate tab identity or behavior. |
| `com.aionemu.gameserver.model.templates.goods.GoodsList` | `Aion.GameServer.Dataholders.GoodsListSummary` | DTO / Template Projection | Partial | Unit Tested | Needs Verification | Captures `id` and `legion_lvl` for BUY availability. Item ids, limits, advertise, gossip, and purchase/trade-in item semantics remain absent. |
| `com.aionemu.gameserver.services.PricesService.getVendorBuyModifier` | `NpcDialogTradeListFactAdapterInput.VendorBuyModifier` | Service Dependency | Partial | Unit Tested as explicit input | Needs Verification | Adapter forwards explicit vendor modifier into existing planner facts. Runtime config/service lookup remains disabled. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `NpcDialogTradeListFactAdapterServiceTests.CreatePlan_ReportsNoTradeGoodsWhenNpcHasNoTradeList` | Missing trade list produces no sellable goods and default sell-rate fact. | Source-reviewed Java null trade-list branch. |
| `NpcDialogTradeListFactAdapterServiceTests.CreatePlan_ReportsMissingGoodsListsWithoutMarkingSellable` | Missing ordinary goods lists and legion-restricted lists do not count as sellable. | Source-reviewed Java goods-list/legion filter. |
| `NpcDialogTradeListFactAdapterServiceTests.CreatePlan_MarksBuySellableWhenAnyGoodsListPassesLegionLevel` | Any ordinary goods list at or below player legion level marks BUY as sellable and preserves rates. | Source-reviewed Java loop behavior and price-rate source. |
| `NpcDialogTradeListFactAdapterServiceTests.CreatePlan_TradeInAvailabilityDoesNotRequireGoodsInList` | Trade-in availability depends on `TradeListData.getTradeInListTemplate`, not goods-list lookup. | Source-reviewed Java TRADE_IN branch. |
| `StaticDataLoadingTests.StaticData_LoadsTradeListsAndGoodsListsLikeJavaDataholders` | Merged-cache parser indexes trade templates and goods lists by Java element/attribute names. | Deterministic XML fixture modeled after Java static-data shapes. |

## Remaining Risks

- Production dialog routing still does not consume the new adapter.
- Player legion level, NPC id sourcing, and `PricesService.getVendorBuyModifier` remain explicit inputs.
- Goods-list item ids, sell/buy limits, advertise/gossip, trade-in item payloads, purchase-list semantics, packet serialization, Java validation warnings, threading, date/time, and Java runtime comparison remain unverified.
- Static-data parsing has deterministic unit coverage but not full cross-runtime XML comparison against Java `DataManager`.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 2 partial read-only static-data holder projections plus 1 partial fact adapter
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 6 blocked/partial categories: production routing, live player/NPC fact sourcing, live price service lookup, full goods/trade payload semantics, Java validation warnings, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit replaces explicit BUY/TRADE_IN static-data assumptions with a tested read-only adapter.

## Next Recommended Unit Of Work

Compose `NpcDialogTradeListFactAdapterService` into the top-level NPC dialog snapshot/fact assembly path as an optional static-data-derived provider, keeping player legion level and vendor buy modifier explicit and production routing disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Trade-list fact composition | `QuestDialogNpcTargetBranchInputAssemblyPlanService` plus tests | Medium | Recommended next; use adapter output to populate existing service facts only when static data is supplied. |
| B | Dialog action registry warning audit | Static-data validation audit/tests | Medium | Compare Java `DialogAction.nameOf` warning behavior before loader warnings. |
| C | Talk-range geometry audit | New non-live range planner/tests | Medium | Requires careful Java `PositionUtil` review and C# world object geometry mapping. |

## Do Not Parallelize

- `GameServerConnection.cs`: production dialog routing remains high-risk.
- `StaticData.cs`: static-data parsing is shared; keep single-owner edits unless coordinating a merge.
- Existing NPC dialog adapter/controller/service planner files: assign exclusive ownership for composition work.
- Phase 6 progress/handoff docs: orchestrator-owned.
