# Phase 6AQY Completion - Charge-All Transaction Audit

Date: 2026-05-28
Unit of Work: UOW-1631
Status: Complete after read-only source review

## Scope

This unit performed the read-only charge-all transaction and ordering audit recommended by UOW-1630. The goal was to decide whether C# charge-all repository batching is an intentional safety difference or an immediate parity bug after packet cadence was fixed.

No production code changed.

## Completed Work

- Reviewed Java `ItemChargeService.startChargingEquippedItems`.
- Reviewed Java `ItemChargeService.chargeItems`.
- Reviewed Java `ItemChargeService.chargeItem`.
- Reviewed Java `ChargeInfo.updateChargePoints`.
- Reviewed Java `InventoryDAO.UPDATE_QUERY`.
- Reviewed C# `GameServerConnection.HandleChargeAllQuestionResponseAsync`.
- Reviewed C# `PlayerEnterWorldService.SaveItemChargeAllMutationAsync`.
- Reviewed C# `PlayerEnterWorldRepository.SaveItemChargeAllMutationAsync`.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, audit findings, parity table, risks, metrics, and next-unit guidance.

## Audit Findings

- Java charge-all quotes one payment before accept, then accept calls `processPayment(player, chargeWay, payAmount)` once.
- Java `chargeItems(player, filteredItems, 2, false, false)` mutates each item in memory during the loop.
- Java `ChargeInfo.updateChargePoints` is synchronized, clamps charge, marks the item `UPDATE_REQUIRED`, marks equipment `UPDATE_REQUIRED` when equipped, and returns whether the visible charge-bar step changed.
- Java `ItemChargeService` itself does not perform an immediate DB transaction for charge-all. Persistence is deferred to the normal dirty-state inventory/equipment persistence path.
- C# revalidates current items at accept, stages all charged items, then persists charged items plus one Kinah/AP payment in a single repository transaction before mutating in-memory inventory or emitting packets.
- C# repository batching is intentionally safer on local save failure: existing tests assert save failure causes no in-memory mutation and no packets.

## Decision

Keep C# charge-all repository batching as an intentional difference for now.

Do not change persistence semantics without broader persistence model work and Java runtime failure-mode evidence. The difference should remain documented as unverified parity, not claimed as verified parity.

## Validation

No tests were run because this unit is read-only analysis and documentation only.

`git diff --check` should be run before commit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Charge-all transaction/ordering audit | read-only Java/C# ItemCharge and repository files | Low | Yes | Documents whether C# batching is intentional after packet cadence fixes. |
| Nearby region-id calculation research | read-only Java/C# world region files | Low | No | Recommended next. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Independent. |
| Production persistence change | charge-all repository/connection files | High | No | Deferred; audit does not justify changing transaction semantics yet. |

No sub-agent was spawned because this was a narrow docs-only audit.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| None | N/A | Documentation-only transaction audit. | Static source review of Java and C# charge-all persistence paths. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.item.ItemChargeService.startChargingEquippedItems` | `GameServerConnection.StartChargingEquippedItemsAsync`; `PendingChargeAllRequest` | Service / Question Flow | Partial | Existing Regression Tested + Manual Analysis | Partial Parity | Both quote one payment before accept. Java stores a live `RequestResponseHandler` over the filtered `Item` collection; C# stores DTO snapshots and revalidates current items at accept. Concurrent prompt-to-accept mutation remains Needs Verification. |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItems` | `GameServerConnection.HandleChargeAllQuestionResponseAsync` | Service / Multi-item Mutation | Refactored | Regression Tested + Manual Analysis | Intentional Difference | Java mutates each item in memory and marks persistence state during the loop. C# persists all charged items and payment in one repository transaction before in-memory mutation/packets. Kept intentionally for save-failure safety, but no Java failure-mode comparison exists. |
| `com.aionemu.gameserver.model.items.ChargeInfo.updateChargePoints` | `ItemChargeService.CreateChargePlan`; repository charge updates | Item Charge State | Partial | Unit Tested + Manual Analysis | Needs Verification | Java synchronized method clamps charge, marks item persistent, marks equipment persistent when equipped, and returns charge-bar-step change. C# planning is immutable and persistence is explicit through repository updates; threading and persistent-state side effects differ. |
| `com.aionemu.gameserver.dao.InventoryDAO.UPDATE_QUERY` | `PlayerEnterWorldRepository.SaveItemChargeAllMutationAsync` | Repository / Persistence | Refactored | Existing Regression Tested + Manual Analysis | Intentional Difference | Java full inventory update query persists many columns when normal persistence runs. C# charge-all updates only `charge`, payment item count, and AP rank in a transaction. This is narrower and safer locally but not Java-runtime verified. |
| `com.aionemu.gameserver.model.gameobjects.Persistable.PersistentState` | explicit C# repository save calls | Persistence State Model | Partial | Manual Only | Needs Verification | Java persistence dirty flags are not ported 1:1 for charge-all. C# has explicit save calls and failure gates. Broader persistence model still needs audit. |

## Remaining Risks

- C# batched transaction remains an intentional difference, not verified Java parity.
- Java dirty-flag persistence and C# explicit repository persistence are structurally different across more than ItemCharge.
- Prompt-to-accept concurrent item changes are locally covered for stale/missing/insufficient cases, but not compared to Java runtime behavior with the live filtered item collection.
- C# only updates the `charge` column for charged items; Java normal `InventoryDAO.UPDATE_QUERY` can persist many item fields when dirty state is flushed.
- Threading/synchronization differences remain: Java `ChargeInfo.updateChargePoints` is synchronized; C# charge planning uses immutable copies and connection-level async flow.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 0 code artifacts; 1 read-only charge-all transaction audit.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification; 2 grouped rows marked Intentional Difference pending broader verification.
- Total blocked artifacts: Java runtime failure-mode comparison, dirty-flag persistence model parity, DB integration comparison, concurrent prompt-to-accept mutation comparison.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Nearby region-id calculation analysis | read-only Java/C# world region files | Study Java 2D/3D region id derivation before live C# region storage. |
| C# charge-all transaction rollback regression | DB integration or repository-level tests | Only if local rollback/no-mutation coverage needs strengthening. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: return to nearby with read-only Java/C# region-id calculation analysis.
- Why: nearby live region storage remains blocked on region identity and membership semantics.
- Files: Java `WorldMap2DInstance`, `WorldMap3DInstance`, `MapRegion`, and C# world/nearby region-key models.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all rollback regression research | repository tests/read-only DB helper review | Low | Only implement if clear existing coverage gap is found. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Region-id calculation research | read-only Java/C# world region files | Low | One owner for docs updates. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Nearby region-id calculation analysis | read-only world/nearby files; docs | Java writes, live nearby dispatch |
| Read-only Agent | Charge-all rollback coverage audit | read-only repository/test files | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch: still high risk and intentionally disabled.
- Shared region abstraction changes: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1631] Document charge-all transaction timing
```

Files changed in this unit:

- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQY-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
