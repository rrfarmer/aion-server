# Phase 6YG Completion - UOW-1145 SM_TRADELIST Packet Plan

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by adding a non-live `SM_TRADELIST` packet-plan prerequisite. This unit does not enable production `BUY` dialog sends. It pins down the Java packet constructor filtering and `writeImpl` field order so the future live packet serializer has a narrow, reviewed target.

The Java implementation is the source of truth:
- `SM_TRADELIST(Player, Npc, TradeListTemplate, int)` filters trade tabs through `DataManager.GOODSLIST_DATA.getGoodsListById(tab.getId())` and `GoodsList.getLegionLevel() <= playerLegionLevel`.
- `SM_TRADELIST.writeImpl` writes target object id, `TradeNpcType.index()`, buy-price modifier, fixed 4.5 modifier `100`, buy/sell flags, visible trade-tab ids, and limited item rows.
- `TradeNpcType.index()` maps `NORMAL=1`, `ABYSS=2`, `LEGION_COIN=3`, `REWARD=4`, and `ABYSS_KINAH=5`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/SmTradeListPacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmTradeListPacketPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YG-Completion.md`

## Implementation Notes

- Added `SmTradeListPacketPlanService` as a pure non-live planner.
- Added explicit `SmTradeListLimitedItemSummary` for the packet rows emitted by Java `LimitedItem`.
- The planner:
  - maps Java trade NPC type names to packet indexes;
  - filters trade tabs by goods-list existence and legion level;
  - reports missing and restricted goods-list ids;
  - carries explicit buy/sell tab flags;
  - models limited-item fields;
  - exposes `JavaWriteOrder` as a descriptor list for future byte serialization.
- Unknown trade NPC type values produce `UnknownTradeNpcType` and keep `IsLive = false`.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmTradeListPacketPlanServiceTests|NpcDialogServiceSelectPlanServiceTests|NpcDialogTradeListFactAdapterServiceTests" --nologo` passed 28 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,155 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,362 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `Aion.GameServer.Services.SmTradeListPacketPlanService` | Packet Plan | Partial | Unit Tested | Partial Parity | Non-live planner models Java constructor filtering and `writeImpl` field order. No live `GameServerPacket`, opcode `253`, binary serialization, connection send, or Java byte comparison yet. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeNpcType` | `SmTradeListPacketPlanService.TryGetTradeNpcTypeIndex` | Enum Mapping | Partial | Unit Tested | Needs Verification | All known Java indexes are covered by unit tests. C# has not introduced a full enum type and unknown XML enum load behavior remains different from Java JAXB failure behavior. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate` | `Aion.GameServer.Dataholders.TradeListTemplateSummary` consumed by `SmTradeListPacketPlanService` | Static Data DTO | Partial | Unit Tested through planner | Partial Parity | Planner consumes NPC id, NPC type, and tab ids. Other template fields such as sell rates and purchase/save-count behavior remain outside this packet-plan unit. |
| `com.aionemu.gameserver.dataholders.GoodsListData` / `com.aionemu.gameserver.model.templates.goods.GoodsList` | `Aion.GameServer.Dataholders.GoodsListTable` / `GoodsListSummary` consumed by planner | Static Data | Partial | Unit Tested through planner | Partial Parity | Planner mirrors Java `GoodsListData.getGoodsListById` filtering for missing goods lists and legion-level restrictions. Full item payload contents and XML/JAXB edge cases remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.Npc.canSell` / `Npc.canBuy` | `SmTradeListPacketPlanInput.NpcCanSell` / `NpcCanBuy` | Runtime Fact Dependency | Not Started | Unit Tested as explicit input | Needs Verification | Java computes flags from trade-list availability and supported actions. C# planner accepts explicit flags only; production calculation is not wired. |
| `com.aionemu.gameserver.services.LimitedItemTradeService` / `LimitedTradeNpc` / `LimitedItem` | `SmTradeListLimitedItemSummary` explicit input | Service Dependency / DTO | Partial | Unit Tested as explicit input | Needs Verification | Limited-item packet rows are modeled, but live limited-item service lookup and per-player `getBuyCount(playerObjId)` are not ported. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmTradeListPacketPlanServiceTests.CreatePlan_FiltersTradeTabsAndModelsJavaWriteOrder` | Unit / Packet Plan | `SM_TRADELIST` constructor and `writeImpl`; `GoodsListData.getGoodsListById` | Java field order descriptor, buy/sell flags, fixed modifier `100`, visible trade-tab filtering, missing/restricted goods ids, and limited item fields. | Source-reviewed Java logic plus deterministic C# unit test. | Does not serialize bytes or compare to Java runtime output. |
| `SmTradeListPacketPlanServiceTests.CreatePlan_MapsJavaTradeNpcTypeIndexes` | Unit | `TradeNpcType.index()` | All known Java trade NPC type names map to their packet indexes. | Source-reviewed Java enum values plus unit test coverage. | C# still uses string-to-index mapping rather than a dedicated enum/static-data validation path. |
| `SmTradeListPacketPlanServiceTests.CreatePlan_ReportsUnknownTradeNpcTypeWithoutClaimingReady` | Unit | Java JAXB enum loading / `TradeNpcType` | Unknown C# trade type input is reported as not ready and stays non-live. | Conservative readiness test. | Java JAXB failure behavior is not reproduced; this is a staged guard only. |

## Remaining Risks

- No live `SM_TRADELIST` packet class, opcode `253` registration, binary serialization, or send path exists.
- Java runtime byte comparison has not been generated.
- Live `LimitedItemTradeService`, player legion lookup, `Npc.canSell/canBuy`, no-sell message routing, static-data enum validation, and production `BUY` controller routing remain disabled or unverified.
- Unknown trade NPC type handling is conservative in C# and may differ from Java JAXB failure timing.
- Threading, serialization byte layout, collection mutation behavior, and live `DataManager` composition remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live packet planner plus 1 explicit limited-item row DTO
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 6 blocked/partial categories: live packet serialization, opcode/send path, limited-item service lookup, NPC buy/sell flag calculation, player legion/runtime data, and Java byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Continue trade-list parity by composing `SmTradeListPacketPlanService` output into the existing non-live `NpcDialogServiceSelectPlan` / `QuestDialogNpcTargetBranchInputAssemblyPlanService` flow, still without live sends.

Recommended starting points:
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogServiceSelectPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SmTradeListPacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogServiceSelectPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmTradeListPacketPlanServiceTests.cs`

Do not enable live `SM_TRADELIST` sends until binary packet serialization, opcode `253`, Java byte comparison, price modifier sourcing, player legion lookup, `Npc.canSell/canBuy`, limited-item service lookup, and no-sell fallback behavior are pinned down.

# Next Work Options

## Recommended Sequential Task

- Task: compose non-live `SmTradeListPacketPlan` descriptors into the existing dialog service/select plan flow.
- Why: it connects the packet prerequisite to the staged `BUY` planner without touching production socket sends.
- Files: `NpcDialogServiceSelectPlanService.cs`, `QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`, their focused test files.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Read-only Java `SM_TRADE_IN_LIST` audit | none | Low | Safe analysis for later trade-in parity. |
| B | Static-data trade NPC type validation tests | `StaticDataLoadingTests.cs` or a new focused test file | Medium | Avoid changing `StaticData.cs` unless the validation gap is understood. |
| C | Non-live limited-item service shape audit | none or new notes-only doc section | Low | Live service lookup is blocked, but dependencies can be mapped. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Compose packet plan into dialog planners | `NpcDialogServiceSelectPlanService.cs`, `QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`, focused tests, docs | Production `GameServerConnection.cs` live sends until composition is proven |
| Agent A | Read-only `SM_TRADE_IN_LIST` Java audit | read-only Java files | all writes |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler; only touch once non-live dialog and packet plans are fully connected.
- `StaticData.cs`: shared loader; avoid concurrent edits with any trade-list model work.
- Progress/handoff docs: orchestrator-owned.
