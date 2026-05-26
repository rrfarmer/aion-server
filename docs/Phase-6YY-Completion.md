# Phase 6YY Completion - UOW-1163 Trade-In No-List Boundary Fallback

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by adding a production socket-boundary regression for the `TRADE_IN` missing-list fallback.

No production code changed, and live sends remain disabled.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YY-Completion.md`

## Implementation Notes

- Added `HandleDialogSelectAsync_TradeInNoTradeListPlansNoSellMessageWithoutSending`.
- The test drives `CM_DIALOG_SELECT` action `78` through `GameServerConnection.HandleDialogSelectAsync`.
- The fixture NPC supports `TRADE_IN`, but no `trade_in_list_template` exists for its template id.
- The observed plan reaches `DialogServiceFallback`, produces `TradeInUnavailable`, records `SystemMessageDoesNotSellItem`, creates no trade-list packet plans, and sends no packets.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "GameServerConnectionStorageExpansionDialogTests|NpcDialogServiceSelectPlanServiceTests|NpcDialogTradeListFactAdapterServiceTests" --nologo` passed 31 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,180 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,387 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `GameServerConnection.HandleDialogSelectAsync` observed through `GameServerConnectionStorageExpansionDialogTests` | Client Packet / Socket Boundary | Partial | Unit Tested | Partial Parity | Boundary now covers `TRADE_IN` happy path and missing-list fallback without sending. Broader dialog actions and live controller dispatch remain incomplete. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` `TRADE_IN` | `NpcDialogServiceSelectPlanService` via production boundary test | Service Boundary / Planner | Partial | Unit Tested | Partial Parity | Missing trade-in list now produces staged no-sell descriptor at the socket boundary. Live `SM_SYSTEM_MESSAGE` send remains disabled. |
| `com.aionemu.gameserver.dataholders.TradeListData.getTradeInListTemplate` | `TradeListTable.GetTradeInListTemplate` via boundary fixture | Static Data Repository | Partial | Unit Tested | Partial Parity | Boundary verifies a missing trade-in template remains unavailable and does not create a packet plan. Full Java runtime dataholder comparison remains absent. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADE_IN_LIST` | `SmTradeInListPacketPlan` absence in missing-list boundary | Packet Plan / Packet Dependency | Partial | Unit Tested | Partial Parity | Regression confirms no trade-in packet plan is created for missing source template. Live packet sends and Java runtime payload vectors remain absent. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `NpcDialogServiceDescriptorKind.SystemMessageDoesNotSellItem` | Packet Intent / Descriptor | Partial | Unit Tested | Needs Verification | Descriptor records Java no-sell intent, but concrete socket packet bytes and exact message dispatch are not live or Java-runtime verified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_TradeInNoTradeListPlansNoSellMessageWithoutSending` | Unit / Socket Boundary | `CM_DIALOG_SELECT.runImpl`; `DialogService.onDialogSelect` `TRADE_IN`; `TradeListData.getTradeInListTemplate`; `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM` | Missing trade-in list reaches staged dialog-service fallback, creates a no-sell descriptor, creates no trade-list packet plans, and sends no packets. | Source-reviewed Java route plus deterministic C# boundary regression. | No live `SM_SYSTEM_MESSAGE` packet bytes or Java runtime vector. |

## Remaining Risks

- Live `SM_TRADE_IN_LIST` and no-sell system-message sends remain disabled.
- Java runtime golden-vector comparison is absent for both trade-in list payloads and no-sell fallback bytes.
- NPC AI/controller live routing remains incomplete.
- Trade-in purchase execution/AP formulas remain separate from list-opening and fallback behavior.
- Static-data field-level corpus comparisons and Java runtime dataholder snapshots remain open.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-sending socket-boundary fallback regression
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: live trade-in sends, live no-sell message sends, Java runtime vectors, NPC AI/controller routing, and trade-in purchase execution
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Deepen static-data corpus comparison to field-level sample rows for trade/goods lists, or draft Java runtime vector tooling for `SM_TRADELIST`/`SM_TRADE_IN_LIST` before enabling live sends.

Recommended starting points:
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/TradeList-Java-Golden-Vector-Design.md`
- `docs/TradeInList-Java-Audit.md`
- Java `game-server/data/static_data/npc_trade_list.xml`
- Java `game-server/data/static_data/goodslists/goodslists.xml`
- Java `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADELIST.java`
- Java `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADE_IN_LIST.java`

Keep live `SM_TRADELIST`, `SM_TRADE_IN_LIST`, and no-sell system-message sends disabled until runtime facts, Java runtime golden vectors, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: field-level static-data corpus samples for trade/goods list rows.
- Why: corpus counts now match, but selected field-level source rows would catch parser regressions in rates, NPC type, tab ids, legion level, sales time, and limited-item limits.
- Files: `StaticDataLoadingTests.cs`, progress/handoff docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java runtime vector tooling sketch | docs only | Low | Extend or create docs for packet vector generation; do not claim runtime verification. |
| B | Trade-in system-message concrete packet prerequisite audit | docs/tests | Low | Keep live socket sends disabled. |
| C | Live legion-level lookup discovery | read-only code search/docs | Low | Avoid production wiring until a C# legion aggregate exists. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Field-level static-data corpus samples | `StaticDataLoadingTests.cs`, docs | live socket sends |
| Agent A | Java vector tooling sketch | docs only | code files |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
