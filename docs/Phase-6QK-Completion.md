# Phase 6QK Completion Handoff - Charge-All Partial Drift Regression

Date: May 25, 2026
Unit of Work: UOW-941
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-941] Cover partial charge-all drift`)

## Status

Phase 6 is still in progress. This unit adds focused regression coverage for Java's charge-all accept-time drift behavior when the original pending set contains one stale/already-charged item and one item that remains chargeable.

No production code changed in this unit; UOW-940 already moved the handler to payment-before-current-item-revalidation semantics. This unit locks the mixed stale/current behavior with packet and persistence assertions.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QK-Completion.md`

## What Changed

- Added `HandleQuestionResponseAsync_ChargeAllApPaymentChargesOnlyCurrentChargeableItemWhenOnePendingItemIsStale`.
- Built a two-item AP charge-all fixture helper so one pending item can be stale by accept time while another remains chargeable.
- Added a compact charge-packet assertion helper for `SmInventoryUpdateItem` charge packets. Charge packets serialize the object id, item string, and conditioning-info blob, but do not include the trailing update-type mask used by full item update packets.
- Verified the handler spends the original quoted AP amount, persists only the current chargeable item, clears the pending request, leaves both final items at level-1 charge, and emits AP spend packets followed by one charge update/success pair, stats, and charge-all complete.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Partial charge-all drift regression | `ItemChargeService.startChargingEquippedItems`, `chargeItems`, `chargeItem` | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Regression Test | Sequential writer | Medium | Completed by orchestrator because the shared fixture and packet helpers are in one large test file. |
| B | Missing/deleted captured-object analysis | same Java charge-all artifacts | read-only Java/C# charge-all files | Analysis | Yes | Low | Deferred; useful next if documenting the C# limitation around Java live captured object refs. |
| C | ItemPurification live adapter prerequisite | ItemPurification packet/mutation flow | service/test files | Later | Maybe | Medium | Deferred; charge-all drift was the handoff recommendation. |
| D | Kinah partial-drift variant | ItemCharge Kinah payment path | charge handler tests | Regression Test | Maybe | Medium | Deferred to avoid expanding this unit beyond one AP parity slice. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests|ItemChargeServiceTests"
```

Result: passed, 65 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1620 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemChargeService.startChargingEquippedItems` | `Aion.GameServer.Network.Aion.GameServerConnection.StartChargingEquippedItemsAsync` and `HandleChargeAllQuestionResponseAsync` | Service / Request Flow | Partial | Regression Tested in C# | Partial Parity | Mixed stale/current charge-all AP regression now covers original quoted payment with one skipped stale item and one charged current item. C# still cannot mutate Java-style detached captured item references after deletion/no-longer-owned drift. |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItems` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeAllQuestionResponseAsync` plus `Aion.GameServer.Services.ItemChargeService.CreateChargePlan` | Service / Charge-All Mutation | Partial | Regression Tested in C# | Partial Parity | C# charges only currently chargeable pending items and emits all-complete only when at least one item updates. Java live object reference and no-refund/reprice semantics remain only partially represented. |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItem` | `Aion.GameServer.Services.ItemChargeService.CreateChargePlan` consumed at accept time | Service / Item Charge Planning | Partial | Regression Tested in C# | Partial Parity | Current charge/rank/improvement checks are exercised for mixed stale/current accept-time behavior. Java `ChargeInfo.updateChargePoints`, null-conditioning exceptions, and full stats internals remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet DTO | Partial | Regression Tested in C# | Needs Verification | Test validates charge packet object id and compact blob framing. Byte-level Java packet comparison remains blocked by runtime tooling. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.AbyssPointsService.CreateAddApPlan` via charge-all AP payment | AP Spend Planner | Partial | Regression Tested in C# | Needs Verification | Mixed stale/current charge-all AP spend is covered at handler level. Broader rank/legion/siege side effects remain at the existing planner boundary. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleQuestionResponseAsync_ChargeAllApPaymentChargesOnlyCurrentChargeableItemWhenOnePendingItemIsStale` | Regression | Java `ItemChargeService.startChargingEquippedItems.acceptRequest` -> `processPayment` -> `chargeItems(... requirePayment=false)` source review | Validates original quoted AP is spent, only the still-chargeable item is persisted/packeted, the stale already-charged item is skipped, and one charge-all-complete packet is sent. | Deterministic C# live-handler regression for Java payment-before-current-item-revalidation behavior. | No Java runtime packet trace; missing/deleted captured-object behavior remains approximated. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- C# cannot preserve Java's exact live captured `Item` reference semantics for deleted/traded/no-longer-owned items; it requires the current inventory object id and matching item id to rebuild a safe current plan.
- Charge-all payment remains quoted from question time and is not recalculated or refunded, matching inspected Java but still allowing overpay/underpay drift.
- Full Java `ChargeInfo.updateChargePoints`, stats update internals, storage dirty-state, and repository transaction behavior remain incomplete.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 1 mixed stale/current charge-all regression slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including Java runtime artifact generation, exact captured-object deletion semantics, full `ChargeInfo` mutation/stat internals, and broader repository transaction parity
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Add a compact missing/deleted pending item drift regression documenting the structural C# limitation around Java's captured live `Item` references, or pivot back to ItemPurification live adapter prerequisites using the existing packet-input snapshot assembler.

If continuing ItemCharge, keep the next unit to one scenario:
- Pending charge-all item is absent from current inventory while another pending item remains current and chargeable.
- Expected C# approximation: spend the quoted payment, charge only the current item, skip the missing item, document that Java may still hold a live captured `Item` object depending on inventory lifecycle semantics.
- Do not claim Verified Parity without Java runtime packet/mutation capture.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Missing/deleted captured-object analysis | read-only Java/C# charge-all files | Low | Good read-only companion if a writer works on a test. |
| B | Missing-item drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Should be a single writer because the test file and helpers are shared. |
| C | ItemPurification live adapter prerequisite | ItemPurification service/test files | Medium | Safe fallback if ItemCharge drift grows too interpretive. |
| D | Kinah partial-drift variant | charge handler tests | Medium | Useful after AP missing/deleted behavior is documented. |

## Do Not Parallelize

- Multiple agents editing `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Multiple agents editing `GameServerConnection.cs`.
- Charge-all drift hardening with broad repository transaction rewrites.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, either add the compact missing/deleted charge-all drift regression or pivot to ItemPurification live adapter prerequisites.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
