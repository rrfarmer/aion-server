# Phase 6PX Completion Handoff - ItemPurification Packet Order Plan

Date: May 25, 2026
Unit of Work: UOW-928
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-928] Add ItemPurification packet order plan`)

## Status

Phase 6 is still in progress. This unit adds a pure ItemPurification packet-order planner that enumerates Java packet intents without creating or sending live packets.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationPacketPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PX-Completion.md`

## What Changed

- Added `ItemPurificationPacketPlanService`.
- Added dry-run packet operation records for system message, inventory update, delete, cube-size, AP placeholder, Kinah no-packet, and inventory add.
- Preserved Java packet intent order:
  - upgrade success system message id `1402579`
  - material update/delete fanout
  - AP placeholder
  - Kinah no-packet marker
  - base delete/update fanout
  - target add fanout
- Recorded Java masks:
  - `DEC_ITEM_USE` update mask `0x16`
  - `USE` delete mask `0x17`
  - `ITEM_COLLECT` add mask `0x19`
- Kept concrete packet creation, payload serialization, live sends, AP packets, inventory mutation, object-id allocation, and persistence out of scope.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter ItemPurificationPacketPlanServiceTests
```

Result: passed, 4 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1592 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPurificationService.isPurificationAllowed` | `Aion.GameServer.Services.ItemPurificationPacketPlanService.CreatePacketPlan` | System Message Packet Planner | Partial | Regression Tested in C# | Partial Parity | Records Java success message id `1402579` before inventory/AP/base/target fanout. Does not yet create `SmSystemMessage`, look up l10n names from live templates, or send the packet. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `ItemPurificationPacketPlanService` + `ItemPurificationApplicationPlan` | Packet Order Projection | Partial | Regression Tested in C# | Partial Parity | Maps material update/delete, AP placeholder, Kinah no-packet marker, and base delete in Java order. Live `Storage` mutation, AP packet side effects, and packet emission are absent. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `ItemPurificationPacketOperationType.InventoryUpdateItem` | Packet Intent / Inventory Update | Partial | Regression Tested in C# | Partial Parity | Material/base count updates are planned as opcode `29` with `DEC_ITEM_USE` mask `0x16`. Payload serialization and template-backed `SmInventoryUpdateItem` construction are not executed. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket` | `ItemPurificationPacketOperationType.DeleteItem` + `CubeSizeUpdate` | Packet Intent / Inventory Delete | Partial | Regression Tested in C# | Partial Parity | Material/base deletes are planned as opcode `28` with delete mask `0x17`, immediately followed by opcode `130` cube-size update. Warehouse variants and payload bytes are not modeled. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `ItemPurificationPacketOperationType.InventoryAddItem` + `CubeSizeUpdate` | Packet Intent / Inventory Add | Partial | Regression Tested in C# | Partial Parity | Target add is planned as opcode `27` with add mask `0x19`, followed by opcode `130` cube-size update. Java's possible `PARTIAL_WITH_SLOT` remap remains a live packet concern; purification targets are expected to use first-available slot. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` | `ItemPurificationPacketOperationType.CubeSizeUpdate` | Packet Intent / Cube Size | Partial | Regression Tested in C# | Needs Verification | Cube-size packet positions are represented after deletes/adds. Exact item counts, expand fields, and serialized payload are not computed. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `ItemPurificationPacketOperationType.AbyssPointsUpdate` | AP Packet Placeholder | Not Started for live AP packets | Regression Tested in C# planner | Needs Verification | Packet plan preserves AP position after material packets but before Kinah/base/target work. Actual AP/rank packet fanout remains unimplemented. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseKinah` | `ItemPurificationPacketOperationType.KinahNoPacket` | Currency Packet Boundary Projection | Partial | Regression Tested in C# | Needs Verification | C# records no Kinah packet because Java calls `decreaseKinah` with a negative amount and `Storage.decreaseKinah` only acts for positive amounts. Runtime/project-owner parity decision still needed. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePacketPlan_PutsUpgradeSuccessMessageBeforeInventoryFanout` | Regression | Java `ItemPurificationService.isPurificationAllowed`, `decreaseMaterials`, `upgradeItem`, and `ItemPacketService` source review | Success system message id `1402579` appears before material delete/cube, AP placeholder, Kinah no-packet, base delete/cube, and target add/cube. | Deterministic C# regression grounded in Java packet order. | Does not serialize or send packets. |
| `CreatePacketPlan_UsesJavaInventoryMasksForUpdateDeleteAndAdd` | Regression | Java `ItemPacketService.ItemUpdateType`, `ItemDeleteType`, and `ItemAddType` source review | Update opcode/mask `29/0x16`, delete opcode/mask `28/0x17`, and add opcode/mask `27/0x19`. | Deterministic C# regression. | No payload bytes or `PARTIAL_WITH_SLOT` remap test. |
| `CreatePacketPlan_FlagsRuntimeInputBlockersButStillListsDryRunPackets` | Regression | Java `ItemFactory.newItem` allocation dependency and C# application-plan status source review | Packet plan remains dry-run and marks runtime inputs needed when target object id allocation is missing. | Deterministic C# planner regression. | No live object-id allocation. |
| `CreatePacketPlan_RejectsMissingOrEmptyApplicationPlan` | Regression | C# application-plan boundary plus Java early-return sequence source review | Missing/null application plans produce no packet intents. | Deterministic C# guard regression. | Java null base item may throw earlier; live difference remains documented. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Packet plan is descriptive and does not create `GameServerPacket` instances or write payloads.
- Success system message names are caller-provided strings; live l10n lookup and `SmSystemMessage` factory method remain unported for ItemPurification.
- AP packet/rank fanout is a placeholder only.
- Exact `SM_CUBE_UPDATE` counts and expand fields are not computed in this dry-run plan.
- Live target object-id allocation, `Storage.add`, dirty-state persistence, `ItemStoneListDAO.save`, system-message failure branches, Kinah parity decision, packet byte order, and rollback/error behavior remain unimplemented.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 1 pure ItemPurification packet-order planner slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 7 blocked/not-started categories, including Java runtime artifact generation, concrete packet serialization, live object-id allocation/factory defaults, live storage mutation, repository dirty-state persistence, AP/rank side effects, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 68% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Packet-order plan | `ItemPurificationService`, `ItemPacketService`, `Storage`, `SM_SYSTEM_MESSAGE` | new service/test files | Packet Planner | Yes, with read-only explorers | Medium | Completed in UOW-928. |
| B | Upgrade success system-message packet | `SM_SYSTEM_MESSAGE.STR_ITEM_UPGRADE_MSG_UPGRADE_SUCCESS` | `SmSystemMessage.cs`, `GamePacketTests.cs`, maybe packet-plan service/test | Packet DTO | Maybe | Low-Medium | Recommended next; concrete message packet is small and keeps inventory/AP fanout as intents. |
| C | Concrete inventory packet emission | `ItemPacketService`, `SM_INVENTORY_*`, `SM_CUBE_UPDATE` | connection, packet services, templates | Packet Fanout | No | High | Too broad until live inventory mutation/object-id allocation exists. |
| D | ItemCharge AP hardening | item charge/AP spend callers | service/test files | Separate AP Caller | Yes later | Medium | Independent fallback if ItemPurification live path remains blocked. |

## Next Recommended Unit of Work

Recommended sequential task:
- Add `SmSystemMessage.ItemUpgradeSuccess(string baseItemName, string resultItemName)` for Java `STR_ITEM_UPGRADE_MSG_UPGRADE_SUCCESS` id `1402579`, with packet serialization regression. Then optionally let `ItemPurificationPacketPlanService` expose a concrete message factory or marker while keeping inventory/AP packets as dry-run intents.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Confirm Java `SM_SYSTEM_MESSAGE` payload for `STR_ITEM_UPGRADE_MSG_UPGRADE_SUCCESS` and nearby generated-method conventions | read-only Java packet files | edits, docs | Message id/parameter report. |
| Agent B | Inspect existing C# `SmSystemMessage` tests and helpers for best insertion point | read-only C# packet tests | edits, docs | Exact test shape for message serialization. |
| Orchestrator | Add concrete `SmSystemMessage` factory and focused tests | `SmSystemMessage.cs`, `GamePacketTests.cs`, optional packet-plan tests/docs | live inventory/AP packet emission | Code, tests, docs, commit. |

## Do Not Parallelize

- `GameServerConnection.cs` live handler edits with packet DTO work.
- Concrete inventory packet emission with object-id allocation/factory work.
- Kinah behavior changes without explicit parity decision.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, add the upgrade-success system-message packet or choose another narrow AP/item caller boundary.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
