# Phase 6QH Completion Handoff - Live ItemCharge AP Guard Consolidation

Date: May 25, 2026
Unit of Work: UOW-938
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-938] Consolidate live ItemCharge AP guard`)

## Status

Phase 6 is still in progress. This unit wires the pure ItemCharge AP payment guard from UOW-937 into the live selected-item and charge-all C# handlers.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QH-Completion.md`

## What Changed

- `HandleChargeItemAsync` now uses `ItemChargeService.CreateAbyssPointPaymentPlan` for selected-item AP payments.
- `HandleChargeAllQuestionResponseAsync` now uses the same AP guard for charge-all AP payments.
- Insufficient AP now exits through the same service status surface before AP plan creation, repository persistence, inventory mutation, AP mutation, and packet fanout.
- `EmptyPlayerEnterWorldRepository` now exposes charge mutation call counters for regression assertions.
- Added selected-item and charge-all insufficient-AP live handler tests.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|
| A | Selected-item AP guard consolidation | `CM_CHARGE_ITEM`, `ItemChargeService.processAPPayment` | `GameServerConnection.cs`, live handler tests | Handler/Test | Sequential writer | Medium | Completed; touches shared handler and packet assertions. |
| B | Charge-all AP guard consolidation | `ItemChargeService.startChargingEquippedItems`, `processAPPayment` | `GameServerConnection.cs`, live handler tests | Handler/Test | Sequential writer | Medium | Completed after selected-item because it edits the same handler file. |
| C | Kinah payment guard analysis | `ItemChargeService.processKinahPayment` | read-only charge handler/service files | Analysis | Yes | Low | Deferred to next unit to keep AP unit bounded. |
| D | Broad AP side effects | `AbyssPointsService.addAp` rank/legion/siege behavior | many AP files | Later | No | High | Too broad for this unit. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests|ItemChargeServiceTests|AbyssPointsServiceTests"
```

Result: passed, 66 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1613 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CHARGE_ITEM` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeItemAsync` | Client Handler | Partial | Regression Tested in C# | Partial Parity | Selected-item AP payment now uses the shared C# AP guard before persistence and packet fanout. Java runtime packet comparison remains unavailable. |
| `com.aionemu.gameserver.services.item.ItemChargeService.processAPPayment` | `Aion.GameServer.Services.ItemChargeService.CreateAbyssPointPaymentPlan` consumed by `GameServerConnection` | Service / AP Payment Guard | Partial | Regression Tested in C# | Partial Parity | Live selected-item and charge-all AP callers now share the pure guard. C# safely rejects payments above `int.MaxValue`; Java overflow behavior is not reproduced. |
| `com.aionemu.gameserver.services.item.ItemChargeService.startChargingEquippedItems` | `Aion.GameServer.Network.Aion.GameServerConnection.StartChargingEquippedItemsAsync` and `HandleChargeAllQuestionResponseAsync` | Service / Request Flow | Partial | Regression Tested in C# | Partial Parity | Charge-all accept response now uses the shared AP guard before repository mutation or packet fanout. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.AbyssPointsService.CreateAddApPlan` via `ItemChargeAbyssPointPaymentPlan` | AP Spend Planner | Partial | Regression Tested in C# | Needs Verification | Planner is consumed only after affordability succeeds. Rank/legion/siege side effects remain incomplete at the AP planner boundary. |
| `com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler` | `Aion.GameServer.Model.GameObjects.ResponseRequester` / `PendingChargeAllRequest` | Request / Response Boundary | Partial | Existing Regression Tested in C# | Needs Verification | Charge-all insufficient AP consumes accepted request before returning. Java runtime response ordering is not captured. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleChargeItemAsync_ApPaymentRejectsInsufficientAbyssPointsWithoutSideEffects` | Regression | Java `CM_CHARGE_ITEM.runImpl` -> `ItemChargeService.chargeItems` -> `processAPPayment` source review | Selected-item insufficient AP leaves AP and item charge unchanged, skips repository persistence, and sends no AP, inventory, stats, success, or all-complete packets. | Deterministic C# live-handler regression for the Java affordability failure path. | No Java runtime packet trace. |
| `HandleQuestionResponseAsync_ChargeAllApPaymentRejectsInsufficientAbyssPointsWithoutSideEffects` | Regression | Java `ItemChargeService.startChargingEquippedItems.acceptRequest` source review | Charge-all insufficient AP consumes the response request, leaves AP and item charge unchanged, skips repository persistence, and sends no AP, inventory, stats, success, or all-complete packets. | Deterministic C# live-handler regression for the Java accept-request AP failure path. | No Java runtime response-order artifact. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Kinah payment parity for `ItemChargeService.processKinahPayment` is still separate and still uses inline live handler checks.
- Broader `AbyssPointsService` rank/legion/siege side effects remain outside this unit.
- C# rejects AP payments larger than `int.MaxValue` as a safe boundary; Java's exact overflow behavior is not reproduced or runtime-verified.
- Charge-all request creation snapshots planned charges before accept; live inventory drift between question and response remains only partially guarded.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 2 live ItemCharge AP caller guard integrations
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including Java runtime artifact generation, Kinah payment guard parity, broader AP side-effect completion, and charge-all inventory-drift parity
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Add a narrow `ItemChargeService.CreateKinahPaymentPlan` or equivalent Kinah payment guard for Java `processKinahPayment`, then consolidate selected-item and charge-all Kinah live checks onto it with focused insufficient-Kinah regressions proving no Kinah packet, no charge packet, no success/all-complete message, and no persistence mutation.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Kinah selected-item live guard | `ItemChargeService.cs`, `GameServerConnection.cs`, live charge tests | Medium | Same handler as charge-all; do sequentially if editing. |
| B | Kinah charge-all live guard | `ItemChargeService.cs`, `GameServerConnection.cs`, live charge tests | Medium | Needs response-requester assertions mirroring AP tests. |
| C | Charge-all inventory drift audit | read-only Java/C# charge-all files | Low | Useful if Kinah guard exposes stale pending-item behavior. |

## Do Not Parallelize

- Multiple agents editing `GameServerConnection.cs`.
- Kinah guard consolidation with broad inventory repository transaction rewrites.
- ItemCharge payment work with broader `AbyssPointsService` rank/legion/siege side effects.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, implement the narrow ItemCharge Kinah payment guard/consolidation.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
