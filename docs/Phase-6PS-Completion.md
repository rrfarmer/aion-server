# Phase 6PS Completion Handoff - ItemPurification Material Mutation Planner

Date: May 25, 2026
Unit of Work: UOW-923
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-923] Add ItemPurification material mutation planner`)

## Status

Phase 6 is still in progress. This unit adds a pure material/base/kinah mutation planner for Java `ItemPurificationService.decreaseMaterials`, without live packet or repository mutation wiring.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationMaterialMutationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationMaterialMutationServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PS-Completion.md`

## What Changed

- Added `ItemPurificationMaterialMutationService`.
- Modeled Java's material consumption order by item id, including partial consumption on later failure.
- Projected AP spend only after all material requirements succeed.
- Documented Java's `decreaseKinah(-necessaryKinah)` call as a no-op at this boundary.
- Modeled base-item deletion by object id after material/AP/kinah steps and carried the deletion result without failing the plan.
- Kept live persistence, AP rank mutation, audit logging, inventory packets, target creation, and fanout out of scope.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter ItemPurificationMaterialMutationServiceTests
```

Result: passed, 4 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1576 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `Aion.GameServer.Services.ItemPurificationMaterialMutationService.CreateDecreaseMaterialsPlan` | Service / Item Mutation Planner | Partial | Regression Tested in C# | Partial Parity | Models material consumption order, AP spend projection, kinah no-op documentation, and base-delete attempt as a pure plan. Live mutation, audit logging, persistence, packet fanout, and target creation remain unported. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByItemId` | `ItemPurificationMaterialMutationService.PlanDecreaseByItemId` | Inventory Utility Projection | Partial | Regression Tested in C# | Partial Parity | Preserves partial material consumption on late failure. Java item-storage ordering has not been runtime-compared. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByObjectId` | `ItemPurificationMaterialMutationService.PlanDecreaseByObjectId` | Inventory Utility Projection | Partial | Regression Tested in C# | Partial Parity | Records base-delete success/failure while matching Java caller's ignored return value. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseKinah` | `ItemPurificationMaterialMutationPlan.KinahSpendApplied` | Inventory Utility / Currency Boundary | Partial | Regression Tested in C# | Needs Verification | C# documents the observed Java negative-spend no-op. Live behavior needs a parity decision before production wiring. |
| `com.aionemu.gameserver.model.gameobjects.Item.decreaseItemCount` | `ItemPurificationMaterialMutationService.PlanDecreaseItemCount` | Item Utility Projection | Partial | Regression Tested in C# | Partial Parity | Mirrors positive-count min-consumption and zero-count deletion projection for non-kinah items. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `ItemPurificationMaterialMutationPlan.AbyssPointsToSpend` | Service Boundary Projection | Not Started for live mutation | Regression Tested in C# planner | Needs Verification | Planner projects spend amount only. It does not mutate AP/rank, persist, or fan out side effects. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | Not ported in this unit | Client Packet Handler | Not Started | Manual Analysis | Needs Verification | Live packet remains unported. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateDecreaseMaterialsPlan_ConsumesMaterialsPlansApAndDeletesBaseItem` | Regression | Java `ItemPurificationService.decreaseMaterials`, `Storage.decreaseByItemId`, `Storage.decreaseByObjectId` source review | Multi-stack material consumption, AP spend projection, Kinah no-op, base deletion, mutation step ordering, and unchanged source player inventory. | Deterministic C# regression grounded in Java source. | No live persistence or packet fanout. |
| `CreateDecreaseMaterialsPlan_PreservesJavaPartialMaterialConsumptionOnLateFailure` | Regression | Java `Storage.decreaseByItemId` source review | Earlier material deletion remains and later short stack is consumed/deleted before failure; AP/base work does not proceed. | Deterministic C# regression for Java no-rollback behavior. | Java item-storage ordering not runtime-compared. |
| `CreateDecreaseMaterialsPlan_DocumentsJavaKinahNegativeSpendAsNoMutation` | Regression | Java `ItemPurificationService.decreaseMaterials` plus `Storage.decreaseKinah` source review | Necessary Kinah is carried but no Kinah stack mutation is planned. | Deterministic C# regression documenting observed Java call/sign mismatch. | Needs live parity decision. |
| `CreateDecreaseMaterialsPlan_IgnoresBaseDeleteFailureLikeJavaCaller` | Regression | Java `decreaseMaterials` return-value source review | Base-delete failure is recorded but does not fail the plan. | Deterministic C# regression. | Live audit/error behavior remains absent. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Planner is a pure projection; it does not persist inventory rows, delete `item_stones`, call `AbyssPointsService.addAp`, update ranks, send inventory/AP packets, or log audit events.
- Java item-storage iteration order is approximated by current C# `Player.InventoryItems` order and has not been runtime-compared.
- Kinah behavior is intentionally conservative: C# documents Java's negative `decreaseKinah` no-op instead of silently changing it.
- Live `CM_ITEM_PURIFICATION`, final workflow assembly, target item creation/add, persistence, packet fanout, and rollback/error-message behavior remain missing.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 1 material/base/kinah mutation planner slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, live packet handler, AP rank mutation side effects, kinah live behavior decision, repository persistence, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 68% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Material/base/kinah mutation planner | `ItemPurificationService.decreaseMaterials`, `Storage.decreaseByItemId`, `Storage.decreaseByObjectId`, `Storage.decreaseKinah` | new planner/test files | Service Planner | No within same files | Medium | Completed in UOW-923. |
| B | Generic inventory decrement helper | `Storage`, `Item` | shared inventory helpers/tests | Utility | Maybe later | Medium | Deferred to avoid broad shared-helper churn before live caller requirements are known. |
| C | Live purification packet/action | `CM_ITEM_PURIFICATION`, `ItemPurificationService` | packet, services, repositories | Integration | No | High | Still crosses validation, mutation, persistence, fanout, and kinah parity decision boundaries. |

## Next Recommended Unit of Work

Recommended sequential task:
- Add a pure non-persistent ItemPurification workflow planner that composes the lookup adapter, AP validation, material mutation planner, and target inheritance planner in Java order.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Audit Java `ItemPurificationService.isPurificationAllowed`, `decreaseMaterials`, and `upgradeItem` call order | read-only Java/C# planner files | edits, docs | Exact workflow ordering report. |
| Agent B | Inspect existing C# planner result shapes for composition patterns | read-only C# service/test files | edits, docs | Proposed composed plan status/result shape. |
| Orchestrator | Implement composed planner if safe | new planner/test files plus docs | live packet/repository persistence | Code, tests, docs, commit. |

## Do Not Parallelize

- Live `CM_ITEM_PURIFICATION` wiring with composed planner work.
- Repository persistence with pure planner work.
- Kinah live behavior changes without an explicit parity decision.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue with the composed ItemPurification workflow planner or another narrow AP/item caller boundary.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
