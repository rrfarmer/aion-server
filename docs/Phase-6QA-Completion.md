# Phase 6QA Completion Handoff - ItemPurification Inventory Update Packet Bridge

Date: May 25, 2026
Unit of Work: UOW-931
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-931] Bridge ItemPurification inventory update packet`)

## Status

Phase 6 is still in progress. This unit adds a narrow concrete `SmInventoryUpdateItem` bridge for ItemPurification packet-plan update operations while keeping delete/add/cube/AP packet fanout as metadata-only intents.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationPacketPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QA-Completion.md`

## What Changed

- Added `ItemPurificationInventoryPacketInput` as a caller-provided post-mutation `InventoryItem` plus `ItemTemplateSummary` snapshot for inventory update packet construction.
- Extended `ItemPurificationPacketPlanService.CreatePacketPlan` with an optional object-id keyed inventory packet input map.
- `InventoryUpdateItem` operations now attach a concrete `SmInventoryUpdateItem` only when the supplied snapshot matches the planned object id, item id, post-mutation count, and template id.
- Success system-message packets remain concrete.
- Delete, cube-size, AP, Kinah no-packet, and target-add operations remain metadata-only.
- Kept live sends, inventory/AP mutation, object-id allocation, storage mutation, repository persistence, and connection wiring out of scope.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Update-only inventory packet bridge | `ItemPacketService.sendItemUpdatePacket`, `SM_INVENTORY_UPDATE_ITEM` | `ItemPurificationPacketPlanService.cs`, focused tests | Packet Planner Bridge | Write sequential | Medium | Shared service/test files; completed by orchestrator. |
| B | Java update-vs-delete behavior check | `Storage.decreaseItemCount`, `Storage.decreaseByItemId`, `ItemPacketService`, `SM_INVENTORY_UPDATE_ITEM` | read-only | Java Analysis | Yes | Low | Completed by read-only explorer; confirmed update uses `DEC_ITEM_USE` mask `0x16` for remaining stacks and delete/cube for exhausted stacks. |
| C | C# packet-input API check | `SmInventoryUpdateItem`, `InventoryItem`, `ItemTemplateSummary` | read-only | C# Analysis | Yes | Low | Completed by read-only explorer; confirmed caller-provided post-mutation snapshots are the safe input boundary. |
| D | Delete/add/cube concrete fanout | `SM_DELETE_ITEM`, `SM_INVENTORY_ADD_ITEM`, `SM_CUBE_UPDATE` | packet plan/service/tests | Packet Planner Bridge | Later | Medium-High | Cube-size counts and target-add runtime state expand scope. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter ItemPurificationPacketPlanServiceTests
```

Result: passed, 5 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1593 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `Aion.GameServer.Services.ItemPurificationPacketPlanService.CreatePacketPlan` | Packet Planner / Inventory Update Bridge | Partial | Regression Tested in C# | Partial Parity | Update operations can now carry a concrete `SmInventoryUpdateItem` when caller supplies a post-mutation item/template snapshot keyed by object id. Java source reviewed: cube updates use `ItemUpdateType.DEC_ITEM_USE` mask `0x16`. No live send, Java runtime byte comparison, or payload golden file exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` via `ItemPurificationPacketOperation.ConcretePacket` | Server Packet DTO / Inventory Update | Partial | Regression Tested in C# planner | Needs Verification | C# packet DTO was reused without changing payload serialization. Existing `SmInventoryInfo` blob behavior is partial and not Java byte-compared; date/time-sensitive expiry fields, equipment/socket/polish/conditioning branches, and template l10n output remain risks. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `ItemPurificationApplicationOperationType.UpdateMaterialItemCount` plus inventory packet input validation | Storage Mutation Boundary Projection | Partial | Regression Tested in C# planner | Partial Parity | Java sends update packets only after the item count remains above zero; C# requires `InventoryItem.Count == operation.NewCount` before creating the concrete update packet. Live mutation, persistent-state changes, concurrency behavior, and rollback/error behavior remain unimplemented. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByItemId` | `ItemPurificationApplicationPlanService` + `ItemPurificationPacketPlanService` | Material Stack Consumption Projection | Partial | Regression Tested in C# planner | Partial Parity | Java may emit multiple delete/update packets across material stacks. C# planner can represent multiple operations and now concrete update packets for matching snapshots, but delete/cube packets remain metadata-only and Java iteration ordering is not runtime-compared. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `ItemPurificationPacketPlanService` | ItemPurification Packet Fanout Projection | Partial | Regression Tested in C# planner | Partial Parity | Material/base update packet construction is partially bridged for caller-provided snapshots. AP/rank packets, Kinah sign behavior, delete/add/cube packets, repository dirty-state saves, and quest/logging side effects remain missing. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePacketPlan_AttachesConcreteInventoryUpdatePacketWhenRuntimeItemInputProvided` | Regression | Java `Storage.decreaseItemCount`, `ItemPacketService.sendItemUpdatePacket`, and `SM_INVENTORY_UPDATE_ITEM` source review | Validates a partial material-stack update can carry a concrete `SmInventoryUpdateItem` with opcode `29` and mask `0x16` when supplied with a matching post-mutation item/template snapshot; validates other inventory/AP/cube operations remain metadata-only. | Deterministic C# regression grounded in Java update-vs-delete path and packet type. | Does not serialize payload bytes, send packets, compare Java runtime output, or cover equipment blob branches. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Concrete inventory update packets rely on caller-provided post-mutation snapshots; live mutation code does not yet produce or send those snapshots.
- `SmInventoryUpdateItem` payload parity depends on the broader `SmInventoryInfo` blob implementation, including template l10n, equipment/socket/polish/conditioning branches, and date/time-sensitive expiry fields.
- Delete, add, cube-size, AP, and Kinah packet operations remain metadata-only.
- Java material consumption can span multiple stacks and mix delete/update packets; C# planner represents this but does not runtime-compare Java storage iteration order.
- AP rank packet/fanout, target object-id allocation, `Storage.add`, dirty-state persistence, `ItemStoneListDAO.save`, Kinah parity decision, packet byte order, and rollback/error behavior remain unimplemented.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 1 narrow ItemPurification inventory-update packet bridge slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, live ItemPurification packet emission, delete/add/cube concrete fanout, live object-id allocation/storage mutation, repository dirty-state persistence, and AP/rank side effects
- Estimated overall migration completion: Phase 6 remains about 68% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Add a narrow concrete `SmDeleteItem` bridge for `ItemPurificationPacketPlanService` delete operations only, gated on the existing operation object id and Java `ItemDeleteType.USE` mask `0x17`, while keeping cube-size packets metadata-only because exact cube counts/expand fields still need live storage state.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java delete packet behavior analysis | read-only Java `ItemPacketService`, `SM_DELETE_ITEM`, `Storage.delete` | Low | Confirm constructor args, mask, and cube update ordering. |
| B | C# delete packet API analysis | read-only C# `SmDeleteItem`, existing packet tests | Low | Confirm minimum inputs and whether payload construction is isolated. |
| C | ItemCharge AP hardening fallback | ItemCharge service/test files | Medium | Independent fallback if delete bridge expands into cube/live storage concerns. |

Suggested parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Reconfirm Java `SM_DELETE_ITEM` behavior for purification delete paths | read-only Java files | edits, docs | Delete packet and cube-order report. |
| Agent B | Inspect C# `SmDeleteItem` constructor/test coverage | read-only C# packet/tests | edits, docs | Safe input-shape report. |
| Orchestrator | Add delete-packet bridge if safe | `ItemPurificationPacketPlanService.cs`, focused tests, docs | live inventory/AP packet emission | Code, tests, docs, commit. |

## Do Not Parallelize

- `GameServerConnection.cs` live handler edits with packet-plan DTO work.
- Concrete delete/add/cube packet emission with object-id allocation/factory work.
- Kinah behavior changes without explicit parity decision.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, add the narrow delete-packet bridge or choose another AP/item caller boundary.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
