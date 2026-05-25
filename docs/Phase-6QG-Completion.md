# Phase 6QG Completion Handoff - ItemCharge AP Payment Guard

Date: May 25, 2026
Unit of Work: UOW-937
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-937] Add ItemCharge AP payment guard`)

## Status

Phase 6 is still in progress. This unit adds a pure AP affordability guard for ItemCharge AP payments. It does not mutate AP, send packets, persist changes, or rewire live selected-item/charge-all handlers.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemChargeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemChargeServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QG-Completion.md`

## What Changed

- Added `ItemChargeService.CreateAbyssPointPaymentPlan`.
- Added `ItemChargeAbyssPointPaymentPlan`.
- Added `ItemChargeAbyssPointPaymentStatus`.
- The guard returns:
  - `Ready` with an `AbyssPointsAddPlan` when AP payment is affordable
  - `NoPaymentRequired`
  - `NoPlayer`
  - `InsufficientAbyssPoints`
  - `PaymentTooLarge`
- The guard creates a negative AP plan only after checking current AP, matching Java `ItemChargeService.processAPPayment`.
- Live selected-item and charge-all handlers remain unchanged in this unit.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Pure ItemCharge AP payment guard | `ItemChargeService.processAPPayment`, `AbyssPointsService.addAp` | `ItemChargeService.cs`, `ItemChargeServiceTests.cs` | Service/Test | Sequential writer | Low | Completed; compact Java behavior and isolated files. |
| B | Live charge selected-item AP audit | `CM_CHARGE_ITEM.runImpl` | read-only `GameServerConnection.cs` | Analysis | Yes | Medium | Existing live AP checks appear present; consolidation deferred. |
| C | Charge-all AP confirm audit | `startChargingEquippedItems` accept handler | read-only charge-all tests/handler | Analysis | Yes | Medium | Existing confirm AP checks appear present; consolidation deferred. |
| D | Broad AP side effects | `AbyssPointsService` rank/legion/siege | many AP files | Later | High | Too broad for this unit. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "ItemChargeServiceTests|AbyssPointsServiceTests"
```

Result: passed, 20 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1611 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemChargeService.processAPPayment` | `Aion.GameServer.Services.ItemChargeService.CreateAbyssPointPaymentPlan` | Service / AP Payment Guard | Partial | Regression Tested in C# | Partial Parity | C# now has a pure AP affordability guard before creating an AP spend plan. It does not mutate AP directly and is not yet wired into live charge handlers. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.AbyssPointsService.CreateAddApPlan` via `ItemChargeAbyssPointPaymentPlan` | AP Spend Planner | Partial | Regression Tested in C# | Partial Parity | Affordable AP payments produce a negative AP plan without mutating the player. Rank/legion/siege side effects remain at the existing AP planner boundary. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CHARGE_ITEM` | existing `GameServerConnection.HandleChargeItemAsync` plus pure guard | Client Handler / Caller Boundary | Partial | Existing Regression Tested in C# + New Service Tests | Needs Verification | Live selected-item handler already has AP checks, but this unit does not rewire it to the new guard. Java runtime packet comparison remains unavailable. |
| `com.aionemu.gameserver.services.item.ItemChargeService.startChargingEquippedItems` | existing charge-all request/response flow plus pure guard | Service / Request Flow | Partial | Existing Regression Tested in C# + New Service Tests | Needs Verification | Charge-all confirm path already checks AP before applying mutation, but this unit leaves live flow unchanged. Consolidation remains future work. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateAbyssPointPaymentPlan_RejectsInsufficientApBeforeAbyssPointsClamp` | Regression | Java `ItemChargeService.processAPPayment` source review | Validates insufficient AP returns a failure without creating an `AbyssPointsAddPlan` or mutating player AP. | Deterministic C# regression for Java affordability guard. | Not wired to live handler in this unit. |
| `CreateAbyssPointPaymentPlan_CreatesNegativeAbyssPointsPlanWhenAffordable` | Regression | Java `processAPPayment` delegating to `AbyssPointsService.addAp` | Validates affordable AP payment returns a negative AP plan and leaves the player unchanged until caller applies it. | Deterministic C# regression grounded in Java call order. | AP side-effect fanout remains at `AbyssPointsService` boundary. |
| `CreateAbyssPointPaymentPlan_RejectsPaymentsThatCannotMatchJavaIntSpend` | Regression | Java `processAPPayment` casts required AP to `int` through `addAp(player, (int) -requiredAP)` | Validates C# rejects payments larger than `int.MaxValue` rather than overflowing. | Deterministic C# guard regression; documented as a safe C# boundary for Java int behavior. | Exact Java overflow behavior is not replicated intentionally; no runtime comparison. |
| `CreateAbyssPointPaymentPlan_SkipsZeroPayment` | Regression | Java `processAPPayment` is only meaningful for positive AP costs | Validates zero/negative AP costs do not create AP spend plans. | Deterministic C# guard regression. | Does not alter charge planning. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The new AP guard is not yet wired into `GameServerConnection` selected-item or charge-all live paths.
- Existing live handlers still duplicate AP affordability checks.
- Kinah payment parity for `ItemChargeService.processKinahPayment` remains outside this unit.
- Broader AP rank/legion/siege side effects remain owned by `AbyssPointsService` and are not completed here.

## Summary Metrics

- Total Java artifacts discovered: 4
- Total artifacts ported: 1 narrow ItemCharge AP payment guard slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked artifacts: 4 blocked/not-started categories, including Java runtime artifact generation, live handler guard consolidation, kinah payment guard parity, and broader AP side-effect completion
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Consolidate selected-item and charge-all live ItemCharge AP checks onto `ItemChargeService.CreateAbyssPointPaymentPlan` with focused regressions proving insufficient AP sends no AP packets, no charge packets, no success/all-complete messages, and no persistence mutation.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Selected-item charge AP guard consolidation | `GameServerConnection.cs`, charge live tests | Medium | Touches live handler; keep focused to selected item if charge-all grows too broad. |
| B | Charge-all AP guard consolidation | `GameServerConnection.cs`, charge-all tests | Medium | Same handler file as selected-item, so do sequentially if both edit. |
| C | Kinah payment guard analysis | read-only Java/C# ItemCharge kinah paths | Low | Could be separate if AP live consolidation is too broad. |

Suggested parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Inspect selected-item insufficient AP live packet/persistence behavior | read-only `GameServerConnection.cs`, live charge tests | edits, docs | Exact assertions and risk report. |
| Agent B | Inspect charge-all insufficient AP live packet/persistence behavior | read-only `GameServerConnection.cs`, charge-all tests | edits, docs | Exact assertions and risk report. |
| Orchestrator | Wire one live AP guard path if bounded | handler/test files, docs | broad AP side effects, unrelated handlers | Code, tests, docs, commit. |

## Do Not Parallelize

- Multiple agents editing `GameServerConnection.cs`.
- ItemCharge live AP guard consolidation with broad `AbyssPointsService` rank/legion/siege changes.
- Kinah and AP live consolidation in the same unit unless tests show they are inseparable.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, consolidate one ItemCharge live AP guard path onto the pure payment guard.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
