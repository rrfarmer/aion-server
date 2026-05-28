# Phase 6AQO Completion - ItemCharge Charge-All Save Failure Ordering

Date: 2026-05-28
Unit of Work: UOW-1621
Status: Complete after focused validation

## Scope

This unit audited the ItemCharge charge-all accept path after UOW-1620 hardened selected-charge save failure behavior. Java `ItemChargeService.startChargingEquippedItems` accepts a response, processes one quoted payment, then calls `chargeItems(... requirePayment=false)` and sends charge/success/stat/all-complete packets only for items that update.

C# keeps an intentional transaction-oriented boundary: it consumes the accepted question response, stages AP/Kinah and current item charge changes, saves through `SaveItemChargeAllMutationAsync`, and only then mutates player state or sends packets. This unit added regression coverage for the failed-save branch.

## Completed Work

- Reviewed Java `ItemChargeService.startChargingEquippedItems`, `chargeItems`, and `chargeItem`.
- Reviewed C# `HandleChargeAllQuestionResponseAsync`.
- Added `EmptyPlayerEnterWorldRepository.SaveItemChargeAllMutationResult`.
- Added `HandleQuestionResponseAsync_ChargeAllApPaymentSaveFailureStopsBeforeInMemoryMutationAndPackets`.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, Migration Parity Table, risks, metrics, and next-unit guidance.

## Validation

Focused ItemCharge charge-all tests passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ItemChargeServiceTests|FullyQualifiedName~HandleQuestionResponseAsync_ChargeAllApPaymentSendsAbyssPointsPlannerPackets|FullyQualifiedName~HandleQuestionResponseAsync_ChargeAllApPaymentHonorsConfiguredAbyssPointCapClamp|FullyQualifiedName~HandleQuestionResponseAsync_ChargeAllApPaymentSaveFailureStopsBeforeInMemoryMutationAndPackets|FullyQualifiedName~HandleQuestionResponseAsync_ChargeAllApPaymentRejectsInsufficientAbyssPointsWithoutSideEffects|FullyQualifiedName~HandleQuestionResponseAsync_ChargeAllKinahPaymentRejectsInsufficientKinahWithoutSideEffects|FullyQualifiedName~GameServerConnectionChargeAllQuestionResponseTests"
```

Result: 22 passed, 0 failed.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Charge-all persistence-failure ordering | fake repository + charge-all response tests | Low | Yes | Adds missing fail-closed persistence failure regression. |
| Charge-all multi-item packet/order audit | existing charge-all tests | Medium | No | Broader than the missing save-failure branch. |
| Nearby controller-position/map-region adapter | nearby adapter/report files | Medium | No | Safe only if kept metadata-only. |
| Live nearby refresh dispatch | world/connection services | High | No | Deferred; requires production timing and socket behavior. |

No sub-agent was spawned because the implementation touches the same fake repository and shared connection test file.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `HandleQuestionResponseAsync_ChargeAllApPaymentSaveFailureStopsBeforeInMemoryMutationAndPackets` | Added | Accepted AP charge-all response consumes request, calls repository with staged rank/item payloads, then failed save prevents player AP/item mutation and sends no packets. | Documents C# transaction-oriented behavior against Java accepted-response/live-mutation ordering. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.item.ItemChargeService.startChargingEquippedItems` | `Aion.GameServer.Network.Aion.GameServerConnection.StartChargingEquippedItemsAsync`; `HandleChargeAllQuestionResponseAsync` | Service / Request Flow | Partial | Regression Tested | Partial Parity | Charge-all accept consumes the response and stages quoted payment before charge mutation. New regression covers failed C# persistence before live state/packet effects. Java mutates live objects after payment and has no equivalent staged repository boundary. |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItems` | `HandleChargeAllQuestionResponseAsync` plus `ItemChargeService.CreateChargePlan` | Service / Charge-All Mutation | Partial | Unit Tested + Regression Tested | Partial Parity | C# recalculates current chargeable items and sends charge/success/stat/all-complete only after successful repository save. Java live-object identity and exact multi-item iteration ordering remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.respond` | `QuestionResponseRegistry.Respond`; `PendingChargeAllRequest` | Request Registry / Dispatch | Partial | Regression Tested | Needs Verification | New save-failure regression confirms accepted charge-all response is consumed even when persistence fails. Java anonymous handler invocation remains unverified. |
| `com.aionemu.gameserver.dao.InventoryDAO` / `AbyssRankDAO` persistence side effects | `IPlayerEnterWorldRepository.SaveItemChargeAllMutationAsync`; `EmptyPlayerEnterWorldRepository.SaveItemChargeAllMutationResult` | Repository Boundary / Test Fake | Partial | Regression Tested fake boundary | Needs Verification | Fake can simulate failed charge-all persistence. Production SQL transaction rollback/autocommit timing and Java dirty-state persistence are not integration-compared. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `AbyssPointsService.CreateAddApPlan` via charge-all AP payment plan | Service Dependency | Partial | Regression Tested through charge-all AP accept | Needs Verification | Staged AP rank payload is captured but not applied when persistence fails. Java AP side effects, rank threshold fanout, and packet ordering are not runtime-compared. |

## Remaining Risks

- C# charge-all uses a transaction-oriented persistence-first boundary, while Java spends/mutates live state directly after response acceptance.
- Real SQL transaction rollback/autocommit behavior was not integration-tested for `SaveItemChargeAllMutationAsync`.
- Kinah charge-all save failure is not directly covered by a new regression; AP branch covers the shared save-failure gate.
- Java live captured `Item` object identity, deleted-item behavior, multi-item iteration order, AP rank side effects, stat observer fanout, encrypted packet bytes, date/time behavior, and threading remain unverified.
- `docs/commit-conventions.md` was requested by startup flow but is absent in this repository.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 fake-repository failure knob plus 1 charge-all AP save-failure regression.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification.
- Total blocked artifacts: Java runtime charge-all comparison, real SQL rollback validation, dirty-state timing comparison, AP/stat side-effect fanout, encrypted packet/frame comparison, multi-item/order comparison.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Kinah charge-all save-failure regression | charge-all response test plus existing fake result knob | Mirrors AP save-failure behavior through the shared repository boundary. |
| Nearby controller-position/map-region adapter | nearby adapter/report files if narrow | Keep metadata-only; no production sends. |
| Java protection serializer implementation | Java serializer/generated artifacts | Use only if Java tooling/runtime artifact strategy is ready. |

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1621] Guard charge-all save failure ordering
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQO-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
