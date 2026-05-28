# Phase 6AQX Completion - ItemCharge Kinah Charge-All Packet Order

Date: 2026-05-28
Unit of Work: UOW-1630
Status: Complete after focused regression tests

## Scope

This unit added the symmetric Kinah charge-all two-item packet-order regression recommended by UOW-1629. It covers the chargeWay 1 path after the AP charge-all path was tightened to Java's per-item stats packet cadence.

No production code changed in this unit.

## Completed Work

- Added `HandleQuestionResponseAsync_ChargeAllKinahPaymentSendsPerItemUpdatesStatsThenAllComplete`.
- Verified one quoted Kinah payment mutation for two accepted charge-all items.
- Verified charged item repository order.
- Verified final player inventory charge and Kinah count.
- Verified packet order: Kinah decrease, item update/success/stats for first item, item update/success/stats for second item, then charge-all complete.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, test evidence, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `startChargingEquippedItems` calculates one quoted payment and, on accept, runs `processPayment(player, chargeWay, payAmount)` once.
- Java `chargeItems(player, filteredItems, 2, false, false)` then loops charged items without additional payment.
- Java `chargeItem` sends item update, item success system message, then `updateStatsVisually()` for each successful item.
- Java `chargeItems` sends one chargeWay-specific all-complete message after the loop.
- This C# test validates the same observable packet cadence for the local Kinah charge-all path, but not Java runtime packet bytes.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~GameServerConnectionChargeAllQuestionResponseTests|FullyQualifiedName~ItemChargeServiceTests|FullyQualifiedName~GamePacketTests"
```

Result: passed 343 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Kinah charge-all multi-item packet order | charge-all connection tests | Low | Yes | Symmetric coverage for chargeWay 1 after AP cadence fix. |
| Charge-all transaction/ordering audit | read-only Java/C# ItemCharge/repository files | Low | No | Recommended next before persistence semantics changes. |
| Nearby region-id calculation analysis | read-only Java/C# world region files | Low | No | Independent safe nearby strand. |
| Live nearby refresh dispatch | world/connection services | High | No | Still blocked by live region storage. |

No sub-agent was spawned because the selected work is a focused test-only change plus orchestrator-owned docs.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `HandleQuestionResponseAsync_ChargeAllKinahPaymentSendsPerItemUpdatesStatsThenAllComplete` | Added | Accepted Kinah charge-all with two current items spends one quoted Kinah payment, persists charged items in order, sends Kinah decrease first, sends item update/success/stats for each item, then sends charge-all complete. | Static source review of Java `ItemChargeService.startChargingEquippedItems`, `chargeItems`, `chargeItem`, `processKinahPayment`, and `PlayerGameStats.updateStatsVisually`; no Java runtime packet capture. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItems` | `GameServerConnection.HandleChargeAllQuestionResponseAsync` | Service / Multi-item Mutation | Partial | Regression Tested | Partial Parity | Kinah two-item path now has symmetric packet-order coverage with per-item update/success/stats and one all-complete message. Java runtime iteration and persistence timing remain unverified. |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItem` | accepted charge-all per-item loop in `GameServerConnection` plus `ItemChargeService.CreateChargePlan` | Item Service / Packet Sequence | Partial | Unit Tested + Regression Tested | Partial Parity | Test verifies two current Kinah chargeWay 1 items are charged in pending request order with Java-like per-item packet cadence. Exact `ChargeInfo` side effects and Java item packet bytes are not compared. |
| `com.aionemu.gameserver.services.item.ItemChargeService.processKinahPayment` | `ItemChargeService.CreateKinahPaymentPlan`; Kinah update path in `GameServerConnection` | Payment Service | Partial | Regression Tested | Partial Parity | Test verifies one quoted Kinah payment is applied before item packets and only one Kinah inventory update is sent. Java `Storage.tryDecreaseKinah` runtime behavior and concurrent inventory changes remain unverified. |
| `com.aionemu.gameserver.model.stats.container.PlayerGameStats.updateStatsVisually` | `GameServerConnection.CreateStatsInfoPacket` per charged Kinah item | Stats Packet Dependency | Partial | Regression Tested | Partial Parity | Symmetric Kinah test confirms `SmStatsInfo` after each charged item. Exact Java stat values and packet bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `SmInventoryUpdateItem` | Packet | Partial | Regression Tested | Needs Verification | Test asserts Kinah decrease packet first, then charged item object ids 7101 and 7102 in order. Full Java serialized bytes/encrypted frames are not compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Packet | Partial | Regression Tested | Needs Verification | Test asserts two chargeWay 1 success messages and final charge-all complete message ids/parameters. Java localization/runtime packet capture remains unverified. |

## Remaining Risks

- C# batched charge-all persistence before packets remains different from Java's item-loop mutation timing and still needs transaction parity review.
- Java equipped-item stream ordering is not runtime-compared to C# pending request order.
- Exact packet bytes for Kinah decrease, item updates, system messages, and stats info are not compared against Java captures.
- Concurrent inventory or AP/Kinah mutations between prompt and accept remain covered only by local stale/missing/insufficient tests, not Java runtime comparison.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 1 focused Kinah packet-order regression test; no production artifacts.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped packet rows explicitly marked Needs Verification.
- Total blocked artifacts: Java runtime packet capture, encrypted frame comparison, transaction timing parity, concurrent inventory mutation parity, exact stat calculation comparison.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Charge-all transaction/ordering audit | read-only Java ItemCharge and C# repository/connection files | Decide whether C# batched persistence remains an intentional safety difference or needs further parity work. |
| Nearby region-id calculation analysis | read-only Java/C# world region files | Useful before live nearby region storage. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: run a read-only charge-all transaction/ordering audit.
- Why: AP and Kinah packet cadence are now covered; the largest remaining charge-all question is mutation/persistence timing.
- Files: Java `ItemChargeService`, Java `Item`/`ChargeInfo` persistence breadcrumbs if needed, C# `GameServerConnection`, `PlayerEnterWorldService`, and `PlayerEnterWorldRepository`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Nearby region-id calculation research | read-only Java/C# world region files | Low | Independent from ItemCharge. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Charge-all transaction audit | read-only ItemCharge/repository files | Low | One owner for docs if updating. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Charge-all transaction/ordering audit | read-only Java/C# ItemCharge and repository files; docs | Java writes, production persistence changes unless a separate implementation unit is chosen |
| Read-only Agent | Nearby region-id calculation research | read-only world region files | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Production repository persistence changes: one owner only and only after audit.
- Live nearby dispatch: still high risk and intentionally disabled.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1630] Cover kinah charge-all packet order
```

Files changed in this unit:

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQX-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
