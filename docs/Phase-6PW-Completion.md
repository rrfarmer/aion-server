# Phase 6PW Completion Handoff - ItemPurification Application Operation Plan

Date: May 25, 2026
Unit of Work: UOW-927
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-927] Add ItemPurification application operation plan`)

## Status

Phase 6 is still in progress. This unit adds a pure, non-mutating ItemPurification application-operation plan that enumerates the intended Java-order runtime, persistence, packet, quest, and AP-rank side-effect categories for the composed workflow.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationApplicationPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationApplicationPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PW-Completion.md`

## What Changed

- Added `ItemPurificationApplicationPlanService`.
- Added `ItemPurificationApplicationPlan`, operation, status, operation type, and effect records/enums.
- Preserved Java `CM_ITEM_PURIFICATION` application order after the composed workflow:
  - material item updates/deletes
  - AP spend
  - documented Kinah no-op
  - base item update/delete
  - target item add
- Flagged target object id allocation when the inherited target item still has object id `0`.
- Flagged random-bonus selection when Java would call `TuningAction.getRandomStatBonusIdFor` and no rerolled id has been injected.
- Kept live inventory mutation, repository writes, AP rank mutation, packet emission, system messages, quest callbacks, and object-id allocation out of scope.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter ItemPurificationApplicationPlanServiceTests
```

Result: passed, 5 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1588 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION.runImpl` | `Aion.GameServer.Services.ItemPurificationApplicationPlanService.CreateApplicationPlan` | Workflow Application Planner | Partial | Regression Tested in C# | Partial Parity | Enumerates post-workflow operation order after prior parser/guard planning. It still does not send the Java success system message, execute live mutations, persist dirty inventory, or emit packets. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `ItemPurificationApplicationPlanService` + `ItemPurificationMaterialMutationPlan` | Service Mutation Application Projection | Partial | Regression Tested in C# | Partial Parity | Preserves Java order of material item updates/deletes, AP spend, Kinah no-op, then base-item update/delete. Live `Storage` mutation, `AbyssPointsService.addAp`, AP rank side effects, packets, audit logging, and persistence are not executed. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `ItemPurificationApplicationPlanService` + `ItemPurificationInheritancePlan` | Service Target Add Projection | Partial | Regression Tested in C# | Partial Parity | Target add is listed after material/base operations and carries runtime/persistence/packet/quest effects. Live `ItemFactory.newItem`, `IDFactory.nextId`, `Storage.add`, `SM_INVENTORY_ADD_ITEM`, cube-size packet, and `QuestEngine.onItemGet` remain unimplemented. |
| `com.aionemu.gameserver.services.item.ItemFactory.newItem` | `ItemPurificationApplicationPlan.RequiresTargetObjectIdAllocation` | Factory / Allocation Boundary | Not Started for live allocation | Regression Tested in C# planner | Needs Verification | C# flags object id `0` as requiring allocation instead of allocating. Java creates a new `Item` with a fresh `IDFactory` id and `PersistentState.NEW`; C# live factory/default state still needs a dedicated port. |
| `com.aionemu.gameserver.model.items.storage.Storage.add` | `ItemPurificationApplicationOperationType.AddTargetItem` | Storage Add Boundary Projection | Not Started for live add | Regression Tested in C# planner | Needs Verification | C# records the target add and effect categories only. Java directly calls `Storage.add`, bypassing `ItemService.addItem`, inventory-full checks, and expirable registration. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseKinah` | `ItemPurificationApplicationOperationType.PreserveKinahNoOp` | Currency Boundary Projection | Partial | Regression Tested in C# | Needs Verification | C# documents that Java passes a negative amount and `Storage.decreaseKinah` only mutates positive amounts. Needs runtime/project-owner decision before live behavior is finalized. |
| `com.aionemu.gameserver.dao.InventoryDAO.store` | Not ported in this unit | Repository / Dirty-State Persistence | Not Started | Manual Analysis | Needs Verification | Java persists dirty items later, batching deletes, inserts, then updates; `ItemStoneListDAO.save` runs after item rows. C# only records that persistence is required and does not write repositories. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `ItemPurificationApplicationOperationType.SpendAbyssPoints` | AP Mutation Boundary Projection | Not Started for live mutation | Regression Tested in C# planner | Needs Verification | Operation plan records AP spend amount and AP rank side-effect category. It does not mutate AP, ranking, legion contribution, siege hooks, or packets. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateApplicationPlan_OrdersMaterialApKinahBaseAndTargetOperationsLikeJava` | Regression | Java `CM_ITEM_PURIFICATION.runImpl`, `ItemPurificationService.decreaseMaterials`, `upgradeItem`, `Storage.add` source review | Operation order: material delete, AP spend, Kinah no-op, base delete, target add; target add effect categories. | Deterministic C# regression grounded in Java source ordering. | Does not execute Java runtime or emit packets. |
| `CreateApplicationPlan_PreservesMaterialUpdateBeforeApAndBaseDelete` | Regression | Java `Storage.decreaseByItemId` and `decreaseMaterials` source review | Partial material stack update is ordered before AP spend and base delete. | Deterministic C# regression. | Java item-storage iteration order still not runtime-compared. |
| `CreateApplicationPlan_FlagsPlaceholderTargetObjectId` | Regression | Java `ItemFactory.newItem` / `IDFactory.nextId` source review | Target object id `0` is treated as an allocation blocker while preserving the target-add operation. | Deterministic C# planner regression. | No live object-id allocation or factory default comparison. |
| `CreateApplicationPlan_FlagsRandomBonusSelectionWhenRerollWasNotInjected` | Regression | Java `TuningAction.getRandomStatBonusIdFor` branch in `upgradeItem` source review | Random-bonus reroll dependency is explicit when source/target stat bonus sets differ and no rerolled id was injected. | Deterministic C# planner regression. | No data-backed random bonus selection. |
| `CreateApplicationPlan_RejectsMissingOrUnplannedWorkflow` | Regression | C# workflow boundary plus Java `runImpl` early-return sequence source review | Missing/null and failed workflow plans produce no application operations. | Deterministic C# guard regression. | Java null base item may throw earlier; live difference remains documented. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The application plan is descriptive and does not mutate runtime state or persist rows.
- Java success system message is sent during validation before material/AP/base/target operations; this plan starts after the composed workflow and does not model that message yet.
- Live target object-id allocation, `ItemFactory` defaults, `PersistentState.NEW`, `Storage.add`, cube-size packet, and `QuestEngine.onItemGet` are not wired.
- Java dirty-state persistence batches deleted, new, and updated item rows later; C# repository save shape is not implemented for this flow.
- `ItemStoneListDAO.save` ordering after inventory rows is only documented, not ported.
- AP rank side effects, legion/ranking/siege hooks, Kinah parity decision, system messages, packet byte order, and rollback/error behavior remain unimplemented.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 1 pure ItemPurification application-operation planner slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 7 blocked/not-started categories, including Java runtime artifact generation, live object-id allocation/factory defaults, live storage add/delete/update, repository dirty-state persistence, AP/rank side effects, packet/system-message fanout, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 68% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Pure application-operation plan | `CM_ITEM_PURIFICATION.runImpl`, `ItemPurificationService`, `ItemFactory`, `Storage`, `InventoryDAO` | new service/test files | Planner | Yes, with read-only explorers | Medium | Completed in UOW-927. |
| B | Packet-order plan | `SM_SYSTEM_MESSAGE`, `ItemPacketService`, `Storage` packet methods | new service/test files | Packet Planner | Maybe | Medium | Recommended next; still non-mutating and can document message/fanout order. |
| C | Live mutation/fanout | full Java purification flow | connection, repositories, packets | Integration | No | High | Too broad until allocation, repository, AP, Kinah, system-message, and packet order decisions are made. |
| D | ItemCharge AP hardening | item charge/AP spend callers | service/test files | Separate AP Caller | Yes later | Medium | Independent from ItemPurification but not selected this unit. |

## Next Recommended Unit of Work

Recommended sequential task:
- Add a dry-run `ItemPurificationPacketPlanService` or equivalent packet-order plan that includes the validation success system message before material/AP/base/target operations and enumerates expected item update/delete/add/cube-size packet categories without sending them.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Analyze Java `ItemPacketService` packet order for material update/delete, base delete, target add, and cube-size packet fanout | read-only Java packet/storage files | edits, docs | Packet sequence report with packet classes and update/delete/add types. |
| Agent B | Analyze C# server packet DTOs and existing item-use packet test helpers | read-only C# network/tests files | edits, docs | Candidate packet-operation enum/result shape and test assertions. |
| Orchestrator | Implement pure packet-order plan if safe | new service/test files plus docs | live repository writes/fanout unless scoped | Code, tests, docs, commit. |

## Do Not Parallelize

- `GameServerConnection.cs` live handler edits with packet-plan work.
- Repository persistence with object-id allocation/factory work.
- Kinah behavior changes without explicit parity decision.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue with the packet-order plan or another narrow AP/item caller boundary.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
