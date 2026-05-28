# Phase 6AQW Completion - ItemCharge Charge-All Packet Cadence

Date: 2026-05-28
Unit of Work: UOW-1629
Status: Complete after focused regression tests

## Scope

This unit audited Java ItemCharge charge-all multi-item behavior and fixed one C# packet-cadence gap. Java `ItemChargeService.chargeItems` loops over charged items and each successful `chargeItem` sends item update, item success system message, and `PlayerGameStats.updateStatsVisually()` before `chargeItems` sends the final all-complete system message.

The C# charge-all accept path already quoted one payment and revalidated each item at accept time, but it was sending a single stats packet after all charged items. This unit moved stats packet emission into the per-item loop for accepted charge-all responses.

## Completed Work

- Reviewed Java `ItemChargeService.startChargingEquippedItems`.
- Reviewed Java `ItemChargeService.chargeItems`.
- Reviewed Java `ItemChargeService.chargeItem`.
- Reviewed Java `PlayerGameStats.updateStatsVisually`.
- Updated `GameServerConnection.HandleChargeAllQuestionResponseAsync` to send `SmStatsInfo` after each charged item success.
- Added `HandleQuestionResponseAsync_ChargeAllApPaymentSendsPerItemUpdatesStatsThenAllComplete`.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `startChargingEquippedItems` calculates one charge-all payment, stores it in the request handler, and on accept calls `processPayment(player, chargeWay, payAmount)`.
- Java accept then calls `chargeItems(player, filteredItems, 2, false, false)`, so each item recalculates chargeability without additional payment.
- Java `chargeItem` sends `SM_INVENTORY_UPDATE_ITEM` when charge changes, sends a per-item success `SM_SYSTEM_MESSAGE`, then calls `player.getGameStats().updateStatsVisually()`.
- Java `chargeItems` sends one charge-way-specific all-complete message after the item loop if at least one item was updated.
- C# still persists charge-all mutations through a batched repository call before packet emission. That differs from Java's in-memory mutation loop and needs transaction parity review before any deeper change.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~GameServerConnectionChargeAllQuestionResponseTests|FullyQualifiedName~ItemChargeServiceTests|FullyQualifiedName~GamePacketTests"
```

Result: passed 342 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| ItemCharge charge-all multi-item packet order | `GameServerConnection.cs`, charge-all connection tests | Medium | Yes | Found a concrete stats-packet cadence gap against Java. |
| Nearby region-id calculation audit | read-only Java/C# world region files | Low | No | Independent safe follow-up. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later; unrelated to charge-all. |
| Live nearby refresh dispatch | world/connection services | High | No | Still blocked by live region storage. |

No sub-agent was spawned because the selected work touched shared connection behavior and its paired tests.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `HandleQuestionResponseAsync_ChargeAllApPaymentSendsPerItemUpdatesStatsThenAllComplete` | Added | Accepted AP charge-all with two current items spends quoted AP, persists charged items in request order, sends AP packets first, then item update/success/stats for each charged item, then charge2-all complete. | Static source review of Java `ItemChargeService.chargeItems`, `chargeItem`, and `PlayerGameStats.updateStatsVisually`; no Java runtime packet capture. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.item.ItemChargeService.startChargingEquippedItems` | `Aion.GameServer.Network.Aion.GameServerConnection.StartChargingEquippedItemsAsync`; `PendingChargeAllRequest` | Service / Question Flow | Partial | Existing Unit Tested + Regression Tested | Partial Parity | Existing flow quotes one payment and registers a pending question before sending `SM_QUESTION_WINDOW`. This unit did not retest start prompt creation directly; Java runtime comparison and concurrent request behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItems` | `GameServerConnection.HandleChargeAllQuestionResponseAsync` | Service / Multi-item Mutation | Partial | Regression Tested | Partial Parity | C# now emits item update, item success, and stats packet per charged item, then one all-complete message. Persistence is transactional through repository save before packets, unlike Java's in-memory mutation/persistence model; documented as a C# safety difference needing verification. |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItem` | `ItemChargeService.CreateChargePlan`; charge-all per-item loop in `GameServerConnection` | Item Service / Packet Sequence | Partial | Unit Tested + Regression Tested | Partial Parity | Per-item chargeability is recalculated at accept time with `requirePayment=false`, matching Java's quoted-payment shape. Exact `ChargeInfo.updateChargePoints` observer effects and Java item packet bytes are not runtime-compared. |
| `com.aionemu.gameserver.model.stats.container.PlayerGameStats.updateStatsVisually` | `GameServerConnection.CreateStatsInfoPacket` emission inside accepted charge-all loop | Stats Packet Dependency | Partial | Regression Tested | Partial Parity | Fixed gap: multi-item charge-all now sends `SmStatsInfo` after each charged item instead of one batched stats packet. Exact Java stat calculations and packet bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet | Partial | Regression Tested | Needs Verification | Test asserts packet order and charged object ids for two items. It does not compare full Java serialized packet bytes or encrypted frames. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet | Partial | Regression Tested | Needs Verification | Test asserts AP spend, two item-charge success messages, and final charge2-all complete message ids/parameters. Java localization lookup and runtime packet capture remain unverified. |

## Remaining Risks

- C# persists charge-all mutations before packet emission through `SaveItemChargeAllMutationAsync`; Java mutates each item during `chargeItems`.
- Java `filteredItems` iteration comes from equipped item stream order; C# uses pending request item order from quoted plans. Current tests verify C# order but not Java runtime ordering.
- Exact `SM_INVENTORY_UPDATE_ITEM`, `SM_SYSTEM_MESSAGE`, and `SM_STATS_INFO` bytes are not compared against Java captures.
- AP side-effect packets and rank-change side effects are covered locally but not runtime-compared to Java.
- A symmetric all-current kinah multi-item packet-order regression is still recommended.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 1 charge-all packet cadence fix plus 1 focused regression test.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped packet rows explicitly marked Needs Verification.
- Total blocked artifacts: Java runtime packet capture, encrypted frame comparison, concurrent inventory mutation parity, exact stats calculation comparison, transaction model review.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Symmetric kinah charge-all packet-order regression | charge-all connection tests | Cover two current kinah charge-all items with per-item update/success/stats and final charge-all complete. |
| Charge-all transaction/ordering audit | Java ItemCharge and C# repository batching docs/tests | Document whether batched persistence remains an intentional C# safety difference. |
| Nearby region-id calculation audit | read-only Java/C# world region files | Useful before live nearby region storage. |

## Next Work Options

## Recommended Sequential Task

- Task: add the symmetric charge-all kinah all-current two-item packet-order regression.
- Why: AP multi-item packet cadence is now covered; kinah charge-all uses different payment packets and should lock the same per-item cadence.
- Files: likely `GameServerConnectionInventoryExpansionUseItemTests.cs` plus progress/handoff docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all transaction audit | read-only Java/C# ItemCharge/repository files | Low | Good docs-only follow-up before persistence changes. |
| B | Nearby region-id calculation research | read-only Java/C# world region files | Low | Independent from ItemCharge. |
| C | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Kinah charge-all packet-order regression | charge-all connection tests, docs | Java writes, nearby dispatch |
| Read-only Agent | Charge-all transaction audit | read-only Java/C# ItemCharge and repository files | all writes |

## Do Not Parallelize

- Shared charge-all connection test edits: one owner only.
- Java source files: read-only only.
- Live nearby dispatch: still high risk and intentionally disabled.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1629] Align charge-all per-item stats packets
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQW-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
