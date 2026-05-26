# Phase 6YT Completion - UOW-1158 Trade-In List Java Audit

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity with a read-only Java audit of `DialogService` `TRADE_IN` and `SM_TRADE_IN_LIST`.

No production code changed in this unit.

## Files Changed

- `docs/TradeInList-Java-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YT-Completion.md`

## Implementation Notes

- Audited Java `DialogService.onDialogSelect` `TRADE_IN`.
- Audited Java `SM_TRADE_IN_LIST.writeImpl`.
- Audited `TradeListData.getTradeInListTemplate` and the separate trade-in-list map.
- Pinned Java opcode `151` for `SM_TRADE_IN_LIST` from `ServerPacketsOpcodes`.
- Documented that `TRADE_IN` uses fixed `buyPriceModifier = 100`, does not call `PricesService`, does not apply `sell_price_rate`, does not filter by goods or legion level, and does not include limited-item rows.
- Documented the recommended C# port shape for a future non-live packet plan and serializer.

## Validation

- No test run was needed for this docs-only audit unit.
- `git diff --check` passed with only existing line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADE_IN_LIST` | `docs/TradeInList-Java-Audit.md` / future `SmTradeInList` | Packet / Verification Design | Not Started | Manual Only | Needs Verification | Java packet opcode `151`, guards, and write order are audited. C# has no concrete packet plan or serializer yet. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` `TRADE_IN` | `NpcDialogServiceSelectPlanService` trade-in descriptor | Service Boundary | Partial | Unit Tested | Partial Parity | C# has a non-live descriptor path, but production socket routing and concrete packet planning are missing. Audit confirms Java fixed modifier `100` and missing-list no-sell branch. |
| `com.aionemu.gameserver.dataholders.TradeListData.getTradeInListTemplate` | `Aion.GameServer.Dataholders.TradeListTable.GetTradeInListTemplate` | Static Data Repository | Partial | Unit Tested | Partial Parity | Existing C# lookup mirrors Java naming and data split. Full corpus/runtime comparison remains unverified. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate` | `Aion.GameServer.Dataholders.TradeListTemplateSummary` | Static Data DTO | Partial | Unit Tested | Needs Verification | Audit documents trade-in use of NPC id, trade NPC type, count, and tab ids. Empty-template/empty-tab behavior still needs packet-plan tests. |
| `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes` | future `SmTradeInList.PacketOpCode` | Opcode Registry | Not Started | Manual Only | Needs Verification | Source review pins `SM_TRADE_IN_LIST` opcode `151`; C# constant does not exist yet. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Manual / Docs Only | `SM_TRADE_IN_LIST`; `DialogService`; `TradeListData`; `TradeListTemplate`; `ServerPacketsOpcodes` | No executable tests were added; the unit documents the Java behavior for a future C# packet-plan/serializer slice. | Java source reviewed and documented. | C# packet plan, serializer, payload tests, and Java runtime vectors remain missing. |

## Remaining Risks

- Trade-in packet plan and serializer are not implemented.
- Production `TRADE_IN` socket routing remains disabled.
- Java runtime golden vectors are absent.
- Trade-in purchase execution/AP formulas are separate and not covered by this packet-list audit.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 Java audit document added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 4 blocked/partial categories: trade-in packet plan/serializer, production routing, Java runtime vectors, and trade-in purchase execution
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a non-live `SM_TRADE_IN_LIST` packet plan and serializer with source-derived payload tests, keeping production sends disabled.

Recommended starting points:
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADE_IN_LIST.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`
- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogServiceSelectPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeListFactAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogServiceSelectPlanServiceTests.cs`

Keep live sends disabled until runtime price/legion facts, limited-item mutation, Java runtime golden vectors, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: add `SmTradeInListPacketPlanService` and `SmTradeInList` serializer with source-derived tests.
- Why: the Java audit is now complete and C# already has static trade-in list loading plus non-live service descriptors.
- Files: new service/packet/test files, `NpcDialogServiceSelectPlanService` only if attaching a packet-plan descriptor is needed, progress/handoff docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Static-data limited-item corpus count comparison design | docs/test planning | Low | Do not claim parity until Java-generated counts exist. |
| B | Java runtime artifact tooling sketch for trade-list vectors | docs only | Low | Can extend `TradeList-Java-Golden-Vector-Design.md`. |
| C | Live legion-level lookup discovery | read-only code search/docs | Low | Avoid touching `GameServerConnection` until a C# legion aggregate exists. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | `SM_TRADE_IN_LIST` packet plan/serializer | focused service/packet/tests/docs | production live sends |
| Agent A | Static-data limited-item corpus count comparison design | docs only | code files |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
