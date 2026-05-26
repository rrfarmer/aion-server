# Phase 6YX Completion - UOW-1162 Trade/Goods Source Corpus Regression

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by adding a source-data corpus regression for Java trade-list and goods-list XML parsing.

No production code or live socket sends changed.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YX-Completion.md`

## Implementation Notes

- Extended `DataManager_LoadsRealJavaStaticDataManifestCounts` with `AssertTradeListCorpusMatchesJavaStaticData`.
- The new regression compares C# parsed table counts against Java source XML:
  - `game-server/data/static_data/npc_trade_list.xml`
  - `game-server/data/static_data/goodslists/goodslists.xml`
- It checks raw template/list counts and distinct lookup-index counts for Java-style after-unmarshal maps.
- It also checks the Java limited-item predicate from `GoodsList.getLimitedItems`: an item is limited only when both `sell_limit` and `buy_limit` are present.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "StaticDataLoadingTests" --nologo` passed 18 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,179 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,386 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.TradeListData` | `Aion.GameServer.Dataholders.TradeListTable` / `StaticDataLoadingTests.AssertTradeListCorpusMatchesJavaStaticData` | Static Data Repository | Partial | Regression Tested | Partial Parity | Full source XML counts for trade, trade-in, and purchase templates now match parsed C# table counts and distinct Java-style lookup indexes. Runtime Java dataholder execution is still not captured. |
| `com.aionemu.gameserver.dataholders.GoodsListData` | `Aion.GameServer.Dataholders.GoodsListTable` / `StaticDataLoadingTests.AssertTradeListCorpusMatchesJavaStaticData` | Static Data Repository | Partial | Regression Tested | Partial Parity | Full source XML counts for ordinary, trade-in, and purchase goods lists now match parsed C# table counts and distinct Java-style lookup indexes. Duplicate-id last-write behavior is count-checked, not behaviorally replayed with Java runtime. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate` | `Aion.GameServer.Dataholders.TradeListTemplateSummary` | Static Data DTO | Partial | Regression Tested | Partial Parity | Corpus test verifies all source templates are represented by parsed summaries. Field-level values beyond counts remain covered by focused fixture tests, not exhaustive corpus comparisons. |
| `com.aionemu.gameserver.model.templates.goods.GoodsList` | `Aion.GameServer.Dataholders.GoodsListSummary` / `GoodsListItemSummary` | Static Data DTO | Partial | Regression Tested | Partial Parity | Corpus test verifies list counts and Java limited-item predicate requiring both `sell_limit` and `buy_limit`. Sales-time and item-id corpus values are not exhaustively compared in this unit. |
| `com.aionemu.gameserver.services.LimitedItemTradeService.start` | `NpcDialogLimitedItemFactAdapterService` prerequisite data via `GoodsListTable` | Service Dependency / Static Data Projection | Partial | Regression Tested | Needs Verification | Source-data limited-item counts now match the Java XML predicate, but live service startup grouping, per-NPC aggregation, buy-count mutation, and sales-time handling remain non-live and need further verification. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts` via `AssertTradeListCorpusMatchesJavaStaticData` | Regression / Source Corpus | `TradeListData.afterUnmarshal`; `GoodsListData.afterUnmarshal`; `GoodsList.getLimitedItems` | Parsed C# trade/goods tables match Java source XML corpus counts, distinct lookup indexes, and limited-item predicate. | Objective comparison against checked-in Java source XML files. | Does not run Java dataholder code; does not exhaustively compare every field value or live limited-item service grouping. |

## Remaining Risks

- Java runtime dataholder snapshots are still absent; this is source XML verification, not Java process output.
- Duplicate-id last-write behavior is inferred through distinct index counts and existing code breadcrumbs, not replayed against a Java runtime.
- Exhaustive field-level corpus comparison for every trade/goods template remains open.
- Live `LimitedItemTradeService.start` grouping by NPC, sales-time propagation, and buy-count persistence remain non-live.
- Live trade-list and trade-in packet sends remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 source-data corpus regression for trade/goods list tables
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 4 blocked/partial categories: Java runtime dataholder snapshots, exhaustive field-level corpus comparison, live limited-item service grouping, and live packet sends
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a non-sending `TRADE_IN` no-list socket-boundary fallback regression, or deepen static-data corpus comparison to field-level sample rows for trade/goods lists before enabling any live sends.

Recommended starting points:
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogServiceSelectPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeListFactAdapterService.cs`
- Java `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- Java `game-server/src/com/aionemu/gameserver/model/templates/goods/GoodsList.java`

Keep live `SM_TRADELIST` and `SM_TRADE_IN_LIST` sends disabled until runtime facts, Java runtime golden vectors, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: add `TRADE_IN` no-list socket-boundary fallback regression.
- Why: the happy-path socket boundary is covered, but the Java no-sell branch for missing trade-in lists is only covered at planner level.
- Files: `GameServerConnectionStorageExpansionDialogTests.cs`, progress/handoff docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Field-level static-data corpus samples | `StaticDataLoadingTests.cs` | Low | Compare selected NPC/template/list ids against source XML values. |
| B | Java runtime vector tooling sketch | docs only | Low | Extend vector-design docs without claiming runtime verification. |
| C | Live legion-level lookup discovery | read-only code search/docs | Low | Avoid production wiring until a C# legion aggregate exists. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | `TRADE_IN` no-list boundary fallback | `GameServerConnectionStorageExpansionDialogTests.cs`, docs | live socket sends |
| Agent A | Field-level corpus sample design | docs or `StaticDataLoadingTests.cs` only | `GameServerConnection.cs` |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
