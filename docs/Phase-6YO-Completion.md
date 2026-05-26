# Phase 6YO Completion - UOW-1153 Trade-List Limited-Item Facts

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by staging Java limited-item trade facts for future `SM_TRADELIST` packet plans. This unit extends goods-list static-data loading and adds a non-live adapter; it does not enable live `BUY` sends.

The Java implementation is the source of truth:
- `LimitedItemTradeService.start` scans `TradeListData.getTradeListTemplate()` and each template's trade tabs.
- For each tab, Java calls `GoodsListData.getGoodsListById(tab.getId())`.
- `GoodsList.getLimitedItems()` returns items only when both `buy_limit` and `sell_limit` are present.
- `LimitedItem.getBuyCount(playerObjectId)` defaults to `0`.
- Java schedules `LimitedItem.setToDefault` using `salestime`, but live scheduling is out of scope here.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/TradeListTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogLimitedItemFactAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogLimitedItemFactAdapterServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YO-Completion.md`

## Implementation Notes

- `GoodsListSummary` now carries `SalesTime` and child `GoodsListItemSummary` rows.
- `GoodsListItemSummary` models item id plus optional `SellLimit` and `BuyLimit`.
- `StaticData` now builds goods-list summaries after reading nested `<salestime>` and `<item>` children.
- `NpcDialogLimitedItemFactAdapterService` scans one NPC's ordinary trade-list tabs, skips missing goods lists while recording ids, selects only items with both limits, and projects packet rows.
- Staged per-item buy counts can be supplied; missing counts default to `0`.
- Live cron reset scheduling, mutable sell-limit decrement, purchase mutation, and service singleton lifecycle remain unimplemented.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "NpcDialogLimitedItemFactAdapterServiceTests|StaticData_LoadsTradeListsAndGoodsListsLikeJavaDataholders" --nologo` passed 3 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,163 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,370 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.goods.GoodsList` | `Aion.GameServer.Dataholders.GoodsListSummary` | Static Data DTO | Partial | Unit Tested | Partial Parity | C# now preserves list id, legion level, salestime, item ids, sell limits, and buy limits. JAXB edge cases and full corpus comparison remain unverified. |
| `com.aionemu.gameserver.model.templates.goods.GoodsList.Item` | `Aion.GameServer.Dataholders.GoodsListItemSummary` | Static Data DTO | Partial | Unit Tested | Partial Parity | C# models item id and optional sell/buy limits. Java only treats items with both limits as limited items. |
| `com.aionemu.gameserver.services.LimitedItemTradeService` | `Aion.GameServer.Services.NpcDialogLimitedItemFactAdapterService` | Service Adapter | Partial | Unit Tested | Partial Parity | Non-live adapter scans one NPC's trade-list tabs and projects limited items for packet planning. Java startup-wide map, cron reset scheduling, mutable sell counts, and singleton lifecycle are not ported. |
| `com.aionemu.gameserver.model.limiteditems.LimitedItem` | `Aion.GameServer.Services.NpcDialogLimitedItemFact` / `SmTradeListLimitedItemSummary` | Model / Packet DTO | Partial | Unit Tested | Needs Verification | C# carries item id, sell limit, buy limit, salestime, and staged player buy count. Live mutation, default reset, and concurrency semantics remain missing. |
| `com.aionemu.gameserver.model.limiteditems.LimitedTradeNpc` | `NpcDialogLimitedItemFactAdapterPlan.LimitedItems` | Model Collection | Partial | Unit Tested | Needs Verification | C# returns a non-live list for one NPC. Java mutable `LimitedTradeNpc` aggregation and service map lifecycle are not ported. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `StaticDataLoadingTests.StaticData_LoadsTradeListsAndGoodsListsLikeJavaDataholders` | Unit / XML Loader | `GoodsList`; `GoodsList.Item`; goodslists XSD | Loader preserves `salestime`, ordinary item ids, and limited item `sell_limit`/`buy_limit` while keeping existing trade-list counts. | Source-reviewed Java JAXB fields plus XML fixture. | No full corpus comparison or JAXB runtime capture. |
| `NpcDialogLimitedItemFactAdapterServiceTests.CreatePlan_CollectsLimitedItemsFromNpcTradeTabsLikeJavaStartup` | Unit / Adapter | `LimitedItemTradeService.start`; `GoodsList.getLimitedItems`; `LimitedItem.getBuyCount` | Adapter collects only items with both limits, carries salestime, records missing goods-list ids, defaults missing buy counts to `0`, and produces packet rows. | Source-reviewed Java startup logic plus deterministic C# test. | No cron scheduling, mutable sell-limit decrement, purchase mutation, or singleton map lifecycle. |
| `NpcDialogLimitedItemFactAdapterServiceTests.CreatePlan_ReturnsEmptyWhenNpcHasNoTradeList` | Unit / Adapter | `LimitedItemTradeService.getLimitedTradeNpc` absent key behavior | Missing NPC trade list yields empty staged facts. | Source-reviewed Java map lookup behavior. | Does not model service startup map population. |

## Remaining Risks

- Live `LimitedItemTradeService` lifecycle, cron reset scheduling, sell-limit mutation, and per-player purchase count mutation remain missing.
- Static-data corpus parity for limited items is not yet compared against Java-generated output.
- The production `BUY` boundary still does not pass limited items into `SmTradeListPacketPlan`.
- Live packet sends remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 static-data surface extension plus 1 non-live limited-item adapter
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: cron reset scheduling, mutable purchase/sell counts, full corpus comparison, production packet-plan integration, and live send wiring
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Compose `NpcDialogLimitedItemFactAdapterService` into the staged `QuestDialogNpcTargetBranchInputAssemblyPlanService`/production `BUY` boundary so ready `SmTradeListPacketPlan` instances can carry limited-item rows without sending packets.

Recommended starting points:
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`

Keep live sends disabled until price/legion facts, runtime limited-item mutation, Java runtime golden vectors, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: compose limited-item facts into staged trade-list packet plans.
- Why: the static-data and adapter surface now exists; packet plans can carry limited rows before live sends are enabled.
- Files: `QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`, focused tests, possibly `GameServerConnection.cs` fixture wiring.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Read-only `SM_TRADE_IN_LIST` Java audit | none | Low | Safe analysis for a later trade-in slice. |
| B | Java runtime golden-vector design notes | docs only | Low | Useful before claiming packet verified parity. |
| C | Static-data limited-item corpus count comparison design | docs/test planning | Low | Do not claim parity until Java-generated counts exist. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Compose limited items into staged packet plans | focused service/tests/docs | live packet sends |
| Agent A | Read-only `SM_TRADE_IN_LIST` audit | read-only Java files | all writes |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- `StaticData.cs`: shared XML loader.
- Progress/handoff docs: orchestrator-owned.
