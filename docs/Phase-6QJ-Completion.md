# Phase 6QJ Completion Handoff - Charge-All Pending Item Drift Hardening

Date: May 25, 2026
Unit of Work: UOW-940
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-940] Harden charge-all pending item drift`)

## Status

Phase 6 is still in progress. This unit narrows the ItemCharge charge-all pending snapshot drift gap by recalculating current item chargeability at accept time instead of blindly applying stale target charge snapshots.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QJ-Completion.md`

## What Changed

- `HandleChargeAllQuestionResponseAsync` now spends the quoted charge-all payment first, then rebuilds current charge plans with `ItemChargeService.CreateChargePlan(... requirePayment: false)`.
- Stale pending items no longer force their old `TargetCharge` onto the current inventory row.
- If payment succeeds but no pending items are currently chargeable, C# persists the payment-only mutation and sends only payment packets.
- Charge update, item success, stats, and all-complete packets are now sent only when at least one item actually charges.
- `EmptyPlayerEnterWorldRepository` now captures `ChargeAllChargedItems` for focused regression assertions.
- Added a regression for the already-charged-at-accept case.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|
| A | Java charge-all drift audit | `ItemChargeService.startChargingEquippedItems`, `chargeItems`, `chargeItem` | read-only Java files | Java Analysis | Yes | Low | Completed by read-only explorer. |
| B | C# pending snapshot audit | `GameServerConnection`, `PendingChargeAllRequest` | read-only C# files/tests | C# Analysis | Yes | Low | Completed by read-only explorer. |
| C | Focused stale-item hardening | same Java charge-all artifacts | `GameServerConnection.cs`, charge tests, test repository | Integration Fix/Test | Sequential writer | Medium | Completed by orchestrator because shared handler/test fixture ownership overlaps. |
| D | ItemPurification live adapter prerequisites | ItemPurification packet/mutation flow | services/tests | Later | Maybe | Medium | Deferred; ItemCharge drift was the handoff recommendation. |

## Sub-Agent Outputs Integrated

- C# explorer reported that C# applied stale `PendingChargeAllItem.TargetCharge` to any current equipped cube item with the same object id and skipped payment entirely if all pending items disappeared.
- Java explorer reported that Java spends the quoted payment first, then calls `chargeItems(... requirePayment=false)`, where each captured item recomputes current improvement/rank/charge points and may skip without refund.
- Both explorers were closed after their reports were integrated.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests|ItemChargeServiceTests"
```

Result: passed, 64 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1619 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemChargeService.startChargingEquippedItems` | `Aion.GameServer.Network.Aion.GameServerConnection.StartChargingEquippedItemsAsync` and `HandleChargeAllQuestionResponseAsync` | Service / Request Flow | Partial | Regression Tested in C# | Partial Parity | C# now preserves quoted payment and recalculates current item chargeability at accept before charge mutation. C# cannot keep Java live `Item` object references for deleted/no-longer-owned items. |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItems` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeAllQuestionResponseAsync` plus `Aion.GameServer.Services.ItemChargeService.CreateChargePlan` | Service / Charge-All Mutation | Partial | Regression Tested in C# | Partial Parity | C# skips stale/non-chargeable pending items instead of forcing snapshot target charge. Java still has broader live-object behavior and no refund/reprice path. |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItem` | `Aion.GameServer.Services.ItemChargeService.CreateChargePlan` consumed at accept time | Service / Item Charge Planning | Partial | Regression Tested in C# | Partial Parity | Accept-time planning reuses current charge/rank/improvement checks. Java `ChargeInfo.updateChargePoints`, stats update internals, and null-conditioning exception behavior remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler` | `Aion.GameServer.Model.GameObjects.ResponseRequester` / `PendingChargeAllRequest` | Request / Response Boundary | Partial | Existing Regression Tested in C# + New Regression | Needs Verification | C# consumes accepted request before payment/charge work. Runtime Java response ordering and concurrent inventory mutation behavior remain unverified. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.AbyssPointsService.CreateAddApPlan` via charge-all AP payment | AP Spend Planner | Partial | Regression Tested in C# | Needs Verification | Payment-only stale charge-all AP spend is covered. Broader AP rank/legion/siege side effects remain at existing planner boundary. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleQuestionResponseAsync_ChargeAllApPaymentStillSpendsWhenItemAlreadyChargedAtAccept` | Regression | Java `ItemChargeService.startChargingEquippedItems.acceptRequest` -> `processPayment` -> `chargeItems(... requirePayment=false)` source review | Quoted AP is spent even when current item charge means no charge mutation occurs; no charge update, success, stats, or all-complete packets are sent. | Deterministic C# live-handler regression for Java payment-before-revalidation order. | Does not compare Java runtime packets; deleted/no-longer-owned captured-object behavior remains approximated. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- C# cannot preserve Java's exact live captured `Item` reference semantics for deleted/traded/no-longer-owned items; it requires the current inventory object id and matching item id to rebuild a safe current plan.
- Charge-all payment remains quoted from question time and is not recalculated or refunded, matching inspected Java but still allowing overpay/underpay drift.
- Full Java `ChargeInfo.updateChargePoints`, stats update internals, storage dirty-state, and repository transaction behavior remain incomplete.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 1 charge-all accept-time stale item hardening slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including Java runtime artifact generation, exact captured-object deletion semantics, full `ChargeInfo` mutation/stat internals, and broader repository transaction parity
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Continue charge-all drift coverage with a compact partial-drift regression: one pending item already charged or missing and one still chargeable should spend the original quoted payment, charge only the current chargeable item, send one item success/update plus one all-complete, and document C# limitations around missing/deleted Java captured object references.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Partial charge-all drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs`, maybe `GameServerConnection.cs` | Medium | Same shared fixture; should be sequential if editing. |
| B | Missing/deleted captured-object analysis | read-only Java/C# charge-all files | Low | Clarifies how far C# should approximate Java detached object refs. |
| C | ItemPurification live adapter prerequisite | ItemPurification service/test files | Medium | Safe fallback if charge-all drift grows too broad. |

## Do Not Parallelize

- Multiple agents editing `GameServerConnection.cs`.
- Multiple agents editing `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Charge-all drift hardening with broad repository transaction rewrites.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, add the compact partial charge-all drift regression/fix.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
