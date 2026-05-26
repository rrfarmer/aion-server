# Phase 6YZ Completion - UOW-1164 Trade/Goods Field-Level Corpus Samples

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by extending the source-data corpus regression from counts to selected field-level samples for Java trade-list and goods-list XML parsing.

No production code changed, and live sends remain disabled.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YZ-Completion.md`

## Implementation Notes

- Extended `AssertTradeListCorpusMatchesJavaStaticData` with sample comparisons derived directly from Java XML.
- Covered one ordinary trade template, one trade-in template, one purchase template, ordinary goods lists, a limited goods list, a trade-in goods list, and a purchase goods list.
- The helper assertions compare C# parsed summaries against source XML fields:
  - NPC id
  - NPC type defaulting
  - sell/AP/buy price rates
  - purchase save count
  - tab id order
  - legion-level defaulting
  - sales time
  - item id order
  - `sell_limit`
  - `buy_limit`
  - Java limited-item predicate

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "StaticDataLoadingTests" --nologo` passed 18 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,180 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,387 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.TradeListData` | `Aion.GameServer.Dataholders.TradeListTable` / `StaticDataLoadingTests.AssertTradeListCorpusMatchesJavaStaticData` | Static Data Repository | Partial | Regression Tested | Partial Parity | Source XML sample rows now compare field-level trade, trade-in, and purchase template values plus tab ordering. Java runtime dataholder execution remains absent. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate` | `Aion.GameServer.Dataholders.TradeListTemplateSummary` | Static Data DTO | Partial | Regression Tested | Partial Parity | Tests cover selected corpus values for NPC id, NPC type defaults, sell/AP/buy rates, save count, and tab ids/order. Exhaustive all-row field comparison remains open. |
| `com.aionemu.gameserver.dataholders.GoodsListData` | `Aion.GameServer.Dataholders.GoodsListTable` / `StaticDataLoadingTests.AssertTradeListCorpusMatchesJavaStaticData` | Static Data Repository | Partial | Regression Tested | Partial Parity | Source XML sample rows now compare ordinary, trade-in, and purchase goods-list lookups by id. Duplicate-id runtime overwrite behavior remains inferred, not Java-runtime captured. |
| `com.aionemu.gameserver.model.templates.goods.GoodsList` | `Aion.GameServer.Dataholders.GoodsListSummary` / `GoodsListItemSummary` | Static Data DTO | Partial | Regression Tested | Partial Parity | Tests cover selected corpus values for legion level defaults, sales time, item ordering, item ids, `sell_limit`, `buy_limit`, and the Java limited-item predicate. Full sales-time scheduling semantics remain non-live. |
| `com.aionemu.gameserver.services.LimitedItemTradeService.start` | `NpcDialogLimitedItemFactAdapterService` prerequisite data via `GoodsListTable` | Service Dependency / Static Data Projection | Partial | Regression Tested | Needs Verification | Limited-item source fields are now sample-checked from Java XML, but per-NPC aggregation, live service startup, buy-count mutation, and scheduling behavior remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts` via `AssertTradeListCorpusMatchesJavaStaticData` | Regression / Source Corpus | `TradeListData.afterUnmarshal`; `GoodsListData.afterUnmarshal`; `TradeListTemplate`; `GoodsList.getLimitedItems` | Selected source XML rows match parsed C# DTO fields for template rates/defaults, tab ordering, goods-list sales time, item ordering, and item limits. | Objective comparison against checked-in Java source XML files. | Does not run Java dataholder code; only selected rows are field-level compared, not the entire corpus. |

## Remaining Risks

- Java runtime dataholder snapshots are still absent; this is source XML verification, not Java process output.
- Exhaustive field-level corpus comparison for every trade/goods template remains open.
- Duplicate-id last-write behavior is still inferred through C# breadcrumbs/counts rather than replayed against Java runtime output.
- Live `LimitedItemTradeService.start` grouping by NPC, sales-time scheduling, and buy-count persistence remain non-live.
- Live trade-list, trade-in, and no-sell packet sends remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 field-level source-corpus regression extension
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: Java runtime dataholder snapshots, exhaustive all-row field comparison, duplicate-id Java runtime overwrite comparison, live limited-item service grouping, and live packet sends
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Draft Java runtime vector tooling for `SM_TRADELIST`/`SM_TRADE_IN_LIST`, or add a concrete no-sell `SM_SYSTEM_MESSAGE` packet prerequisite audit before enabling live trade-list sends.

Recommended starting points:
- `docs/TradeList-Java-Golden-Vector-Design.md`
- `docs/TradeInList-Java-Audit.md`
- Java `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADELIST.java`
- Java `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADE_IN_LIST.java`
- Java `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmTradeList.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmTradeInList.cs`

Keep live `SM_TRADELIST`, `SM_TRADE_IN_LIST`, and no-sell system-message sends disabled until runtime facts, Java runtime golden vectors, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: Java runtime vector tooling design for `SM_TRADELIST` and `SM_TRADE_IN_LIST`.
- Why: source-derived serializers and source-data corpus checks exist, but Java-generated packet vectors are still missing before live sends can be considered.
- Files: docs only unless implementing a dedicated tool in a later unit.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | No-sell `SM_SYSTEM_MESSAGE` packet prerequisite audit | docs/tests | Low | Keep production sends disabled. |
| B | Duplicate-id source fixture regression | `StaticDataLoadingTests.cs` | Low | Focus on Java last-write-wins behavior with a small XML fixture. |
| C | Live legion-level lookup discovery | read-only code search/docs | Low | Avoid production wiring until a C# legion aggregate exists. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Runtime vector tooling design | docs only | code files unless explicitly scoped |
| Agent A | No-sell packet audit | separate docs file only | progress/handoff docs, code files |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
