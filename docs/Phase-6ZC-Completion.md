# Phase 6ZC Completion - UOW-1167 Trade/Goods Duplicate Lookup Regression

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by adding a duplicate-id static-data fixture regression for Java `TradeListData` and `GoodsListData` last-write-wins lookup behavior.

No production code changed, and live sends remain disabled.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ZC-Completion.md`

## Implementation Notes

- Added `StaticData_TradeAndGoodsDuplicateIdsUseJavaLastWriteLookup`.
- The fixture duplicates:
  - ordinary `tradelist_template`
  - `trade_in_list_template`
  - `purchase_template`
  - ordinary `goodslists/list`
  - `goodslists/in_list`
  - `goodslists/purchase_list`
- The regression verifies raw parsed lists preserve both rows while Java-style lookup maps return the later duplicate row.
- This directly models Java `afterUnmarshal` loops using `HashMap.put(key, value)`.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "StaticDataLoadingTests" --nologo` passed 19 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,182 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,389 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.TradeListData` | `Aion.GameServer.Dataholders.TradeListTable` | Static Data Repository | Partial | Regression Tested | Partial Parity | Duplicate ordinary, trade-in, and purchase template ids now exercise Java `HashMap.put` last-write-wins lookup behavior. Java runtime dataholder artifact remains absent. |
| `com.aionemu.gameserver.dataholders.GoodsListData` | `Aion.GameServer.Dataholders.GoodsListTable` | Static Data Repository | Partial | Regression Tested | Partial Parity | Duplicate ordinary, trade-in, and purchase goods-list ids now exercise Java `HashMap.put` last-write-wins lookup behavior. Java runtime dataholder artifact remains absent. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate` | `Aion.GameServer.Dataholders.TradeListTemplateSummary` | Static Data DTO | Partial | Regression Tested | Partial Parity | Regression verifies later duplicate template values for rates, type, save count, and tab ids are returned from lookups. Exhaustive corpus field comparison remains open. |
| `com.aionemu.gameserver.model.templates.goods.GoodsList` | `Aion.GameServer.Dataholders.GoodsListSummary` / `GoodsListItemSummary` | Static Data DTO | Partial | Regression Tested | Partial Parity | Regression verifies later duplicate goods-list values for legion level, sales time, item id, and item limits are returned from lookups. Live limited-item grouping remains non-live. |
| `com.aionemu.gameserver.services.LimitedItemTradeService.start` | `NpcDialogLimitedItemFactAdapterService` prerequisite lookup data | Service Dependency / Static Data Projection | Partial | Regression Tested | Needs Verification | Duplicate goods-list lookup behavior is now covered, but live service startup grouping and buy-count mutation remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `StaticDataLoadingTests.StaticData_TradeAndGoodsDuplicateIdsUseJavaLastWriteLookup` | Regression / Fixture | `TradeListData.afterUnmarshal`; `GoodsListData.afterUnmarshal` | Duplicate source rows are all parsed, while lookup maps return the last duplicate row for ordinary/trade-in/purchase trade templates and goods lists. | Deterministic fixture modeled on Java `HashMap.put` loop behavior. | Does not execute Java runtime dataholders; duplicate behavior is source-reviewed and C# fixture-verified. |

## Remaining Risks

- Java runtime dataholder snapshots are still absent.
- Exhaustive all-row field comparison remains open.
- Live `LimitedItemTradeService.start` grouping by NPC, sales-time scheduling, and buy-count persistence remain non-live.
- Live trade-list, trade-in, and no-sell packet sends remain disabled.
- Java runtime packet vectors are still missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 duplicate-id lookup regression
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: Java runtime dataholder snapshots, exhaustive all-row comparison, live limited-item service grouping, live packet sends, and Java runtime packet vectors
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Continue toward Java vector generator implementation planning, or audit live legion-level lookup prerequisites for moving beyond staged `BUY` runtime facts.

Recommended starting points:
- `docs/TradeList-Java-Golden-Vector-Design.md`
- Java `game-server/src/com/aionemu/gameserver/network/aion/AionServerPacket.java`
- Java `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- Java `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeRuntimeFactAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`

Keep live `SM_TRADELIST`, `SM_TRADE_IN_LIST`, and no-sell system-message sends disabled until Java runtime vectors, live runtime facts, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: Java vector generator implementation plan or skeleton design.
- Why: source-derived packet tests and static-data lookup tests are now fairly deep, but live send readiness still needs Java-generated artifacts.
- Files: docs first; avoid code unless a narrow tool location is selected.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Live legion-level lookup discovery | read-only code search/docs | Low | Identify C# homes for legion aggregates; do not wire production yet. |
| B | Trade-list live-send readiness audit | docs only | Low | Summarize remaining gates before sends. |
| C | Java vector generator implementation sketch | docs only | Low | Keep separate from progress/handoff docs if using an agent. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Java vector generator plan | `docs/TradeList-Java-Golden-Vector-Design.md`, progress/handoff docs | live send wiring |
| Agent A | Legion-level lookup discovery | separate docs file only | code files, progress/handoff docs |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
