# Phase 6AQP Completion - ItemCharge Kinah Charge-All Save Failure Ordering

Date: 2026-05-28
Unit of Work: UOW-1622
Status: Complete after focused validation

## Scope

This unit paired the AP charge-all save-failure regression from UOW-1621 with explicit Kinah coverage. Java `ItemChargeService.startChargingEquippedItems` accepts the question response, processes the quoted payment through `processKinahPayment`, then calls `chargeItems(... requirePayment=false)`.

C# stages the Kinah update and charged items, persists through `SaveItemChargeAllMutationAsync`, and only after a successful save mutates live inventory or sends packets. This unit locks down the failed-save branch for Kinah.

## Completed Work

- Added `EmptyPlayerEnterWorldRepository.ChargeAllPaymentKinahItem`.
- Added `HandleQuestionResponseAsync_ChargeAllKinahPaymentSaveFailureStopsBeforeInMemoryMutationAndPackets`.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, Migration Parity Table, risks, metrics, and next-unit guidance.

## Validation

Focused ItemCharge charge-all tests passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ItemChargeServiceTests|FullyQualifiedName~HandleQuestionResponseAsync_ChargeAllApPaymentSaveFailureStopsBeforeInMemoryMutationAndPackets|FullyQualifiedName~HandleQuestionResponseAsync_ChargeAllKinahPaymentSaveFailureStopsBeforeInMemoryMutationAndPackets|FullyQualifiedName~HandleQuestionResponseAsync_ChargeAllKinahPaymentRejectsInsufficientKinahWithoutSideEffects|FullyQualifiedName~HandleQuestionResponseAsync_ChargeAllKinahPaymentChargesOnlyCurrentChargeableItemWhenOnePendingItemIsStale|FullyQualifiedName~HandleQuestionResponseAsync_ChargeAllKinahPaymentChargesOnlyCurrentChargeableItemWhenOnePendingItemIsMissing|FullyQualifiedName~GameServerConnectionChargeAllQuestionResponseTests"
```

Result: 22 passed, 0 failed.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Kinah charge-all persistence-failure ordering | fake repository + charge-all response tests | Low | Yes | Mirrors AP save-failure coverage with a branch-specific staged Kinah assertion. |
| Charge-all multi-item packet/order audit | existing charge-all tests | Medium | No | Broader than the missing branch. |
| Nearby controller-position/map-region adapter | nearby adapter/report files | Medium | No | Best next non-live nearby slice. |
| Live nearby refresh dispatch | world/connection services | High | No | Deferred; requires production timing and socket behavior. |

No sub-agent was spawned because the change is a small branch-specific assertion in the same shared test surface.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `HandleQuestionResponseAsync_ChargeAllKinahPaymentSaveFailureStopsBeforeInMemoryMutationAndPackets` | Added | Accepted Kinah charge-all response consumes request, calls repository with staged Kinah and charged item payloads, then failed save prevents live Kinah/item mutation and sends no packets. | Documents C# transaction-oriented behavior against Java accepted-response/live-mutation ordering. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.item.ItemChargeService.startChargingEquippedItems` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeAllQuestionResponseAsync` | Service / Request Flow | Partial | Regression Tested | Partial Parity | Kinah accept path now has explicit failed-save coverage matching AP. C# consumes the response and stages persistence before live mutation; Java mutates live payment/charges after acceptance. |
| `com.aionemu.gameserver.services.item.ItemChargeService.processKinahPayment` | `ItemChargeService.CreateKinahPaymentPlan`; `SaveItemChargeAllMutationAsync` Kinah payload | Payment Service / Inventory Mutation | Partial | Regression Tested | Needs Verification | New fake capture verifies staged Kinah count after quoted payment, but production inventory SQL rollback and Java `tryDecreaseKinah` dirty persistence timing remain unverified. |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItems` | `HandleChargeAllQuestionResponseAsync` plus `ItemChargeService.CreateChargePlan` | Service / Charge-All Mutation | Partial | Unit Tested + Regression Tested | Partial Parity | Staged charged item is captured by the repository fake and live item charge remains unchanged on save failure. Java live object identity and runtime packet ordering remain unverified. |
| `com.aionemu.gameserver.dao.InventoryDAO` / Kinah inventory persistence | `IPlayerEnterWorldRepository.SaveItemChargeAllMutationAsync`; `EmptyPlayerEnterWorldRepository.ChargeAllPaymentKinahItem` | Repository Boundary / Test Fake | Partial | Regression Tested fake boundary | Needs Verification | Fake now captures staged Kinah item for charge-all. Real SQL transaction rollback/autocommit behavior and Java dirty-state persistence are not integration-compared. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.respond` | `QuestionResponseRegistry.Respond`; `PendingChargeAllRequest` | Request Registry / Dispatch | Partial | Regression Tested | Needs Verification | Kinah save-failure regression confirms accepted response is consumed before failed persistence returns. Java anonymous callback invocation remains unverified. |

## Remaining Risks

- C# charge-all persistence-first staging remains intentionally different from Java's live Kinah spend and charge mutation.
- Real SQL transaction rollback/autocommit behavior was not integration-tested.
- Java `Storage.tryDecreaseKinah` dirty-state behavior, live captured `Item` object identity, multi-item iteration order, stat observer fanout, encrypted packet bytes, date/time behavior, and threading remain unverified.
- The regression covers one chargeable Kinah item; mixed charge-way and multi-item exact packet ordering remain open.
- `docs/commit-conventions.md` was requested by startup flow but is absent in this repository.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 fake-repository Kinah capture plus 1 charge-all Kinah save-failure regression.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification.
- Total blocked artifacts: Java runtime charge-all comparison, real SQL rollback validation, dirty-state timing comparison, live object identity, encrypted packet/frame comparison, multi-item/order comparison.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Nearby controller-position/map-region adapter | nearby adapter/report files if narrow | Keep metadata-only; no production timers or sends. |
| Charge-all multi-item packet/order audit | existing ItemCharge charge-all tests | Use only if staying in ItemCharge. |
| Java protection serializer implementation | Java serializer/generated artifacts | Use only if Java tooling/runtime artifact strategy is ready. |

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1622] Guard kinah charge-all save failure ordering
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQP-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
