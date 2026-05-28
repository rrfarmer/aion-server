# Phase 6AQN Completion - ItemCharge Selected Save Failure Ordering

Date: 2026-05-28
Unit of Work: UOW-1620
Status: Complete after focused validation

## Scope

This unit audited selected ItemCharge success/failure ordering and added a focused persistence-failure regression. Java `ItemChargeService.chargeItem` processes payment before charge mutation, sends charge update only after `ChargeInfo.updateChargePoints`, then sends success/stat/all-complete messages after at least one item updated.

C# intentionally stages payment and charge mutation, attempts repository persistence first, and only then mutates player state and sends packets. This transaction-oriented difference is safer for the port, but it needed a regression proving failed persistence stops before in-memory mutation and packets.

## Completed Work

- Reviewed Java selected charge ordering in `ItemChargeService.chargeItems`, `chargeItem`, and `processPayment`.
- Reviewed C# selected charge ordering in `GameServerConnection.HandleChargeItemAsync`.
- Added `EmptyPlayerEnterWorldRepository.SaveItemChargeMutationResult`.
- Added `HandleChargeItemAsync_SaveFailureStopsBeforeInMemoryMutationAndPackets`.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, Migration Parity Table, risks, metrics, and next-unit guidance.

## Validation

Focused ItemCharge tests passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ItemChargeServiceTests|FullyQualifiedName~HandleChargeItemAsync_ApPaymentSendsAbyssPointsPlannerPackets|FullyQualifiedName~HandleChargeItemAsync_SelectedEquippedItemCanBeChargedLikeJavaInventoryLookup|FullyQualifiedName~HandleChargeItemAsync_ApPaymentRejectsInsufficientAbyssPointsWithoutSideEffects|FullyQualifiedName~HandleChargeItemAsync_SaveFailureStopsBeforeInMemoryMutationAndPackets|FullyQualifiedName~HandleChargeItemAsync_KinahPaymentRejectsInsufficientKinahWithoutSideEffects"
```

Result: 20 passed, 0 failed.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Selected ItemCharge persistence/packet ordering | fake repository + selected charge tests | Low | Yes | Adds missing fail-closed persistence failure regression. |
| Charge-all persistence/packet ordering audit | charge-all tests/repository fake | Medium | No | Best next ItemCharge follow-up; separate pending-request path. |
| Nearby controller-position/map-region adapter | nearby adapter/report files | Medium | No | Safe only if kept metadata-only. |
| Live nearby refresh dispatch | world/connection services | High | No | Deferred; requires production timing and socket behavior. |

No sub-agent was spawned because the test required a shared fake-repository knob and one connection test file.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `HandleChargeItemAsync_SaveFailureStopsBeforeInMemoryMutationAndPackets` | Added | Ready selected AP charge calls repository with staged rank payload; failed save prevents player AP/charge mutation and sends no packets. | Documents C# transaction-oriented behavior against Java live mutation ordering. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItems` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeItemAsync` | Service / Handler Composition | Partial | Regression Tested | Partial Parity | C# success, failed payment, and failed persistence branches are covered. C# persists before mutating live state; Java mutates live objects directly. |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItem` | `Aion.GameServer.Services.ItemChargeService.CreateChargePlan`; handler application path | Service / Charge Mutation | Partial | Unit Tested + Regression Tested | Partial Parity | Charge math and staged mutation are tested. Java `ChargeInfo.updateChargePoints` observer behavior and exact packet ordering remain partially modeled. |
| `com.aionemu.gameserver.services.item.ItemChargeService.processAPPayment` | `ItemChargeAbyssPointPaymentPlan`; `SaveItemChargeMutationAsync` rank payload | Service / AP Payment Dependency | Partial | Regression Tested | Needs Verification | New test verifies staged AP rank payload is not applied to player when persistence fails. Java runtime AP side effects remain unverified. |
| `com.aionemu.gameserver.dao.InventoryDAO` / dirty persistence side effects | `IPlayerEnterWorldRepository.SaveItemChargeMutationAsync`; `EmptyPlayerEnterWorldRepository.SaveItemChargeMutationResult` | Repository Boundary / Test Fake | Partial | Regression Tested fake boundary | Needs Verification | Fake can simulate failed persistence. Real SQL transaction behavior, rollback, and Java dirty-state/autocommit timing remain unverified. |

## Remaining Risks

- C# selected charge uses a transaction-oriented staging boundary while Java mutates live objects directly.
- Real SQL transaction failure/rollback behavior was not integration-tested.
- Java packet ordering after AP side effects may include additional configured side effects not fully represented by C# tests.
- Charge-all path uses a separate pending-request/repository flow and should be audited separately.
- Threading, object identity, encrypted packet bytes, date/time handling, and Java runtime comparison remain unverified.
- `docs/commit-conventions.md` was requested by startup flow but is absent in this repository.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows.
- Total artifacts ported: 1 fake-repository failure knob plus 1 selected ItemCharge save-failure regression.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification.
- Total blocked artifacts: Java runtime selected-charge comparison, real SQL rollback validation, dirty-state timing comparison, AP side-effect fanout, encrypted packet/frame comparison, charge-all ordering audit.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Charge-all persistence/packet ordering audit | Java `startChargingEquippedItems`/`chargeItems`; C# question-response charge-all path | Add focused `SaveItemChargeAllMutationAsync` failure regression if missing. |
| Nearby controller-position/map-region adapter | nearby adapter/report files if narrow | Keep metadata-only; no production sends. |
| Java protection serializer implementation | Java serializer/generated artifacts | Use only if Java tooling/runtime artifact strategy is ready. |

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1620] Guard item charge save failure ordering
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQN-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
