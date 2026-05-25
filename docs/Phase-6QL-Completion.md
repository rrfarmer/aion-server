# Phase 6QL Completion Handoff - Charge-All Missing Item Drift Regression

Date: May 25, 2026
Unit of Work: UOW-942
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-942] Cover missing charge-all drift`)

## Status

Phase 6 is still in progress. This unit adds focused regression coverage for the C# charge-all approximation when a pending item from question time is missing from current inventory at accept time and another pending item remains chargeable.

No production code changed in this unit. The test documents the current C# boundary: Java captures live mutable `Item` references in `ItemChargeService.startChargingEquippedItems`, while C# pending requests can only safely rebuild from the player's current inventory by object id and item id.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QL-Completion.md`

## What Changed

- Added `HandleQuestionResponseAsync_ChargeAllApPaymentChargesOnlyCurrentChargeableItemWhenOnePendingItemIsMissing`.
- The test seeds a two-item AP charge-all request, removes one pending item from current inventory before accept, and leaves the second item chargeable.
- Verified C# spends the original quoted AP amount, persists only the surviving current item update, clears the pending request, sends AP spend packets, sends one charge update/success pair for the surviving item, then sends stats and charge-all complete.
- Documented that this is a C# approximation of Java's captured-object semantics, not Verified Parity for deleted/no-longer-owned Java item lifecycle behavior.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Missing/deleted charge-all drift regression | `ItemChargeService.startChargingEquippedItems`, `chargeItems`, `chargeItem`, `Item` | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Test Creation | No | Medium | Completed sequentially because the shared charge handler test file and docs are the only edit surfaces. |
| B | Missing/deleted captured-object Java analysis | same Java charge-all artifacts | read-only Java/C# files | Java Analysis | Yes | Low | Recent source review already bounded behavior enough for this regression; deeper runtime lifecycle proof remains blocked. |
| C | ItemPurification live adapter prerequisite | `CM_ITEM_PURIFICATION`, `ItemPurificationService` | ItemPurification service/tests | Integration Fix/Test | Maybe | Medium | Deferred; recommended next if moving away from ItemCharge drift. |
| D | Kinah partial-drift variant | `ItemChargeService.processKinahPayment`, charge-all accept path | charge handler tests | Test Creation | No with A | Medium | Deferred to keep this unit one AP scenario. |

## Selected Parallel Batch

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Add missing-current-item charge-all AP regression and docs | Test Creation / Documentation Update | `GameServerConnectionInventoryExpansionUseItemTests.cs`, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6QL-Completion.md` | Production code, other docs unless needed | UOW-940 handler hardening and UOW-941 two-item fixture helper | Passing regression, updated parity table, handoff |

No sub-agents were spawned because safe implementation parallelism was not available for the shared test/doc files.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests|ItemChargeServiceTests"
```

Result: passed, 66 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1621 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemChargeService.startChargingEquippedItems` | `Aion.GameServer.Network.Aion.GameServerConnection.StartChargingEquippedItemsAsync` and `HandleChargeAllQuestionResponseAsync` | Service / Request Flow | Partial | Regression Tested in C# | Partial Parity | Missing-current-item drift now has explicit C# regression coverage: quoted payment is spent, but only current matching inventory items are charged. Java live captured `Item` references may behave differently if a removed item remains reachable; runtime lifecycle evidence is unavailable. |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItems` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeAllQuestionResponseAsync` plus `Aion.GameServer.Services.ItemChargeService.CreateChargePlan` | Service / Charge-All Mutation | Partial | Regression Tested in C# | Partial Parity | C# skips missing pending items and charges the remaining current item. Java `chargeItems` iterates captured objects and does not re-query inventory by object id, so exact deleted/no-longer-owned semantics remain a documented gap. |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItem` | `Aion.GameServer.Services.ItemChargeService.CreateChargePlan` consumed at accept time | Service / Item Charge Planning | Partial | Regression Tested in C# | Partial Parity | Current item charge/rank/improvement checks are used for the surviving item. Missing item has no C# current object to mutate. Java `ChargeInfo.updateChargePoints`, null-conditioning exceptions, and stats internals remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.Item` | `Aion.GameServer.Model.GameObjects.InventoryItem` | Model / Inventory Item | Partial | Regression Tested in C# | Needs Verification | C# uses immutable-style inventory row copies and current list replacement. Java holds mutable `Item` references captured at question time; object lifecycle/removal semantics are not runtime-verified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet DTO | Partial | Regression Tested in C# | Needs Verification | Test validates only the surviving item's charge packet shape and object id. Byte-level Java packet comparison remains blocked by Java runtime tooling. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.AbyssPointsService.CreateAddApPlan` via charge-all AP payment | AP Spend Planner | Partial | Regression Tested in C# | Needs Verification | Missing/current charge-all AP spend is covered at handler level. Broader rank/legion/siege side effects remain at the existing planner boundary. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleQuestionResponseAsync_ChargeAllApPaymentChargesOnlyCurrentChargeableItemWhenOnePendingItemIsMissing` | Regression | Java `ItemChargeService.startChargingEquippedItems.acceptRequest` -> `processPayment` -> `chargeItems(... requirePayment=false)` source review plus documented C# current-inventory approximation | Validates original quoted AP is spent, the missing pending item is skipped, only the surviving current item is persisted/packeted, and one charge-all-complete packet is sent. | Deterministic C# live-handler regression for the documented C# approximation of Java payment-before-revalidation behavior. | Does not prove Java runtime deleted-object behavior; no Java packet trace; exact mutable `Item` lifecycle semantics remain unknown. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- C# cannot preserve Java's exact live captured `Item` reference semantics for deleted/traded/no-longer-owned items; this unit freezes the current-inventory approximation rather than claiming Verified Parity.
- Charge-all payment remains quoted from question time and is not recalculated or refunded, matching inspected Java but still allowing overpay/underpay drift.
- Full Java `ChargeInfo.updateChargePoints`, stats update internals, storage dirty-state, repository transaction behavior, and mutable item lifecycle semantics remain incomplete.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 missing/current charge-all regression slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 4 blocked/not-started categories, including Java runtime artifact generation, exact captured-object deletion semantics, full `ChargeInfo` mutation/stat internals, and broader repository transaction parity
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Pivot back to ItemPurification live adapter prerequisites using the existing packet-input snapshot assembler, or add a narrow Kinah partial-drift variant if keeping the next unit inside ItemCharge.

If choosing ItemPurification:
- Inspect Java `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_ITEM_PURIFICATION.java`.
- Inspect Java `game-server/src/com/aionemu/gameserver/services/item/ItemPurificationService.java`.
- Compare with C# `GameServerConnection.HandleItemPurificationAsync`, `ItemPurificationWorkflowService`, and `GameServerConnectionItemPurificationTests`.
- Keep the unit focused on one live adapter or packet-order gap.

If choosing ItemCharge:
- Add one Kinah partial-drift regression analogous to the AP missing/stale coverage.
- Avoid broad storage transaction rewrites.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | ItemPurification Java behavior analysis | read-only Java/C# ItemPurification files | Low | Safe read-only companion to choose the next live adapter slice. |
| B | ItemPurification live adapter test | `GameServerConnectionItemPurificationTests.cs` and maybe one service file | Medium | Keep isolated from ItemCharge tests. |
| C | Kinah partial-drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Single writer only; useful if staying in ItemCharge. |
| D | Charge packet byte-comparison design notes | docs or parity notes only | Low | Do not claim runtime parity until Java tooling exists. |

## Do Not Parallelize

- Multiple agents editing `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Multiple agents editing `GameServerConnection.cs`.
- Multiple agents editing ItemPurification workflow service/tests if a live adapter writer is active.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, choose either the ItemPurification live adapter prerequisite or the narrow Kinah partial-drift regression.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
