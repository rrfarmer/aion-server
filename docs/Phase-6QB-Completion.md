# Phase 6QB Completion Handoff - ItemPurification Delete Packet Bridge

Date: May 25, 2026
Unit of Work: UOW-932
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-932] Bridge ItemPurification delete packet`)

## Status

Phase 6 is still in progress. This unit adds a narrow concrete `SmDeleteItem` bridge for ItemPurification packet-plan delete operations while keeping cube-size/add/AP packet fanout as metadata-only intents.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationPacketPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QB-Completion.md`

## What Changed

- `ItemPurificationPacketPlanService` now attaches `new SmDeleteItem(operation.ObjectId, UseDeleteType)` to material/base delete operations.
- Java delete ordering is preserved: each concrete delete packet operation is immediately followed by a cube-size metadata operation.
- Added focused regression coverage that decodes the concrete delete packet payload and validates object id plus Java `ItemDeleteType.USE` mask `0x17`.
- Success system-message, inventory-update, and delete packets can now be concrete in the dry-run packet plan.
- Cube-size, target-add, AP, and Kinah no-packet operations remain metadata-only.
- Kept live sends, inventory/AP mutation, object-id allocation, storage mutation, repository persistence, and connection wiring out of scope.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Delete-only packet bridge | `ItemPacketService.sendItemDeletePacket`, `SM_DELETE_ITEM` | `ItemPurificationPacketPlanService.cs`, focused tests | Packet Planner Bridge | Write sequential | Low-Medium | Shared service/test files; completed by orchestrator. |
| B | Java delete/cube ordering check | `Storage.decreaseItemCount`, `Storage.delete`, `ItemPacketService`, `SM_DELETE_ITEM`, `SM_CUBE_UPDATE` | read-only | Java Analysis | Yes | Low | Completed by read-only explorer; confirmed delete first, cube update second, delete type `USE` mask `0x17`. |
| C | C# delete DTO API check | `SmDeleteItem`, existing packet tests | read-only | C# Analysis | Yes | Low | Completed by read-only explorer; confirmed object id plus delete type is enough for concrete packet construction. |
| D | Concrete cube-size bridge | `SM_CUBE_UPDATE.cubeSize` | packet plan/service/tests | Later | Medium | Cube-size counts/expansion fields require live player storage state. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter ItemPurificationPacketPlanServiceTests
```

Result: passed, 6 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1594 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket` | `Aion.GameServer.Services.ItemPurificationPacketPlanService.CreatePacketPlan` | Packet Planner / Inventory Delete Bridge | Partial | Regression Tested in C# | Partial Parity | Delete operations now carry concrete `SmDeleteItem` packets with object id and `USE` delete type `0x17`, followed by cube-size metadata operations. Java source reviewed; no live send or Java runtime packet capture exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` via `ItemPurificationPacketOperation.ConcretePacket` | Server Packet DTO / Inventory Delete | Complete for this packet DTO path | Regression Tested in C# planner | Partial Parity | Focused test decodes C# payload as object id then delete type. Java byte-golden/runtime comparison is still unavailable, so parity remains partial rather than verified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` | `ItemPurificationPacketOperationType.CubeSizeUpdate` | Packet Intent / Cube Size | Partial | Regression Tested in C# planner | Needs Verification | C# preserves delete-then-cube ordering but keeps cube payload metadata-only because Java computes cube counts/expansion fields from live storage/player state at send time. |
| `com.aionemu.gameserver.model.items.storage.Storage.delete` | `ItemPurificationApplicationOperationType.DeleteMaterialItem` / `DeleteBaseItem` plus delete packet plan | Storage Delete Boundary Projection | Partial | Regression Tested in C# planner | Partial Parity | Packet planner creates the delete packet only; it does not remove items, mark persistent state, queue deleted rows, log deletion, or fire `QuestEngine.onItemRemoved`. Threading/concurrency behavior remains unported. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `ItemPurificationApplicationPlanService` + `ItemPurificationPacketPlanService` | Stack Consumption Projection | Partial | Regression Tested in C# planner | Partial Parity | Java maps exhausted non-Kinah stacks from `DEC_ITEM_USE` to delete type `USE`. C# planner preserves delete/update split from application operations, but live item mutation and Java runtime ordering across storage collections are not compared. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePacketPlan_PutsUpgradeSuccessMessageBeforeInventoryFanout` updated | Regression | Java `ItemPurificationService.isPurificationAllowed`, `ItemPacketService.sendItemDeletePacket` source review | Validates the success message still leads the fanout and only non-delete/non-update operations remain metadata-only. | Deterministic C# regression. | Does not send packets or compare Java runtime bytes. |
| `CreatePacketPlan_AttachesConcreteInventoryUpdatePacketWhenRuntimeItemInputProvided` updated | Regression | Java update/delete packet split source review | Validates update operations stay concrete when caller supplies snapshots while delete operations may also be concrete. | Deterministic C# regression. | No live send or Java byte comparison. |
| `CreatePacketPlan_AttachesConcreteDeletePacketsBeforeCubeSizeMetadata` | Regression | Java `ItemPacketService.sendItemDeletePacket`, `SM_DELETE_ITEM.writeImpl`, and `Storage.decreaseItemCount` source review | Validates material/base delete operations carry concrete `SmDeleteItem` packets, serialize object id plus `0x17`, and remain immediately followed by metadata-only cube-size operations. | Deterministic C# payload regression grounded in Java packet fields and ordering. | No Java runtime golden file; cube-size payload remains unmodeled. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Concrete delete packets are now in the packet plan but still not emitted by live ItemPurification handling.
- Cube-size packets remain metadata-only because Java computes payload fields from live player storage/cube expansion state.
- Target add, AP, and Kinah packet operations remain metadata-only.
- Live inventory mutation, delete queue/persistent-state changes, item deletion logging, quest item-removal hooks, AP/rank side effects, target object-id allocation, `Storage.add`, dirty-state persistence, `ItemStoneListDAO.save`, Kinah parity decision, packet byte order, and rollback/error behavior remain unimplemented.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 1 narrow ItemPurification delete-packet bridge slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, live ItemPurification packet emission, cube/add/AP concrete fanout, live object-id allocation/storage mutation, repository dirty-state persistence, and AP/rank side effects
- Estimated overall migration completion: Phase 6 remains about 68% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Add a narrow concrete `SmInventoryAddItem` bridge for `ItemPurificationPacketPlanService` target-add operations only if caller-provided target item/template snapshots can keep object-id allocation and `Storage.add` out of scope; otherwise pivot to ItemCharge AP spend hardening.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java target add packet behavior analysis | read-only Java `ItemPacketService.sendStorageUpdatePacket`, `SM_INVENTORY_ADD_ITEM`, `Storage.add`, `ItemPurificationService.upgradeItem` | Low-Medium | Confirm add mask, constructor inputs, and cube update ordering. |
| B | C# add packet API analysis | read-only C# `SmInventoryAddItem`, `InventoryItem`, `ItemTemplateSummary`, existing packet tests | Low | Confirm whether caller-provided target snapshot/template is enough. |
| C | ItemCharge AP hardening fallback | ItemCharge service/test files | Medium | Independent fallback if add bridge expands into object-id allocation/storage add concerns. |

Suggested parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Reconfirm Java `SM_INVENTORY_ADD_ITEM` behavior for purification target-add path | read-only Java files | edits, docs | Add packet and cube-order report. |
| Agent B | Inspect C# `SmInventoryAddItem` constructor/test coverage | read-only C# packet/tests | edits, docs | Safe input-shape report. |
| Orchestrator | Add target-add packet bridge if safe | `ItemPurificationPacketPlanService.cs`, focused tests, docs | live object-id allocation/storage add/AP packet emission | Code, tests, docs, commit. |

## Do Not Parallelize

- `GameServerConnection.cs` live handler edits with packet-plan DTO work.
- Concrete add/cube packet emission with object-id allocation/factory work.
- Kinah behavior changes without explicit parity decision.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, add the narrow target-add packet bridge or choose another AP/item caller boundary.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
