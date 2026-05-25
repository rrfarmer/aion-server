# Phase 6PZ Completion Handoff - ItemPurification Packet Plan Concrete Message Bridge

Date: May 25, 2026
Unit of Work: UOW-930
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-930] Bridge ItemPurification packet plan success message`)

## Status

Phase 6 is still in progress. This unit bridges the concrete `SmSystemMessage.ItemUpgradeSuccess` packet into the dry-run ItemPurification packet plan while leaving inventory/AP packet fanout as metadata-only intents.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationPacketPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PZ-Completion.md`

## What Changed

- Added an optional concrete `GameServerPacket` field to `ItemPurificationPacketOperation`.
- `ItemPurificationPacketPlanService.CreatePacketPlan` now attaches `SmSystemMessage.ItemUpgradeSuccess(sourceItemName, targetItemName)` to the first `UpgradeSuccessSystemMessage` operation.
- Later inventory update/delete/add/cube-size operations, AP placeholder, and Kinah no-packet marker remain metadata-only.
- Kept live sends, inventory/AP mutation, object-id allocation, storage mutation, repository persistence, and connection wiring out of scope.

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
| `com.aionemu.gameserver.services.item.ItemPurificationService.isPurificationAllowed` | `Aion.GameServer.Services.ItemPurificationPacketPlanService.CreatePacketPlan` | Packet Planner / Service Message Boundary | Partial | Regression Tested in C# | Partial Parity | The first packet-plan operation now carries a concrete `SmSystemMessage.ItemUpgradeSuccess` matching Java success-message id and parameters. Live l10n lookup, connection send ordering, validation failure packets, and Java runtime byte comparison remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_ITEM_UPGRADE_MSG_UPGRADE_SUCCESS` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.ItemUpgradeSuccess` via packet plan | Server Packet DTO / System Message | Complete for factory, partial for ItemPurification wiring | Regression Tested in C# | Partial Parity | Concrete packet is now reachable from the dry-run ItemPurification packet plan. It is still not sent by `GameServerConnection` and has no Java runtime capture. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `ItemPurificationPacketOperationType.InventoryUpdateItem` | Packet Intent / Inventory Update | Partial | Regression Tested in C# planner | Needs Verification | Inventory update operations remain metadata-only with opcode/mask; no `SmInventoryUpdateItem` construction or payload serialization is wired for purification. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket` | `ItemPurificationPacketOperationType.DeleteItem` + `CubeSizeUpdate` | Packet Intent / Inventory Delete | Partial | Regression Tested in C# planner | Needs Verification | Delete and cube-size operation order remains dry-run. Live inventory deletion, cube counts, and payload bytes are not modeled. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `ItemPurificationPacketOperationType.AbyssPointsUpdate` | AP Packet Placeholder | Not Started for live AP packets | Regression Tested in C# planner | Needs Verification | AP spend still has only a placeholder packet operation. Ranking/legion/siege/AP packet side effects remain missing. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePacketPlan_PutsUpgradeSuccessMessageBeforeInventoryFanout` updated with concrete packet assertion | Regression | Java `ItemPurificationService.isPurificationAllowed` and `SM_SYSTEM_MESSAGE.STR_ITEM_UPGRADE_MSG_UPGRADE_SUCCESS` source review | First operation still precedes inventory fanout and now carries a concrete `SmSystemMessage`, while all later operations remain metadata-only. | Deterministic C# regression grounded in Java message ordering and existing C# packet factory. | Does not send packets or compare Java runtime bytes. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Concrete success message is now in the plan but still not emitted by live ItemPurification handling.
- Live l10n lookup for base/result item names is not wired into a purification send path.
- Inventory update/delete/add/cube-size packets remain dry-run metadata for purification.
- AP rank packet/fanout, target object-id allocation, `Storage.add`, dirty-state persistence, `ItemStoneListDAO.save`, Kinah parity decision, packet byte order, and rollback/error behavior remain unimplemented.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 1 pure ItemPurification packet-plan concrete-message bridge slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, live ItemPurification packet emission, concrete inventory fanout, live object-id allocation/storage mutation, repository dirty-state persistence, and AP/rank side effects
- Estimated overall migration completion: Phase 6 remains about 68% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Packet-plan concrete message bridge | `ItemPurificationService.isPurificationAllowed`, `SM_SYSTEM_MESSAGE` | `ItemPurificationPacketPlanService.cs`, focused tests | Planner Bridge | No need | Low | Completed in UOW-930. |
| B | Inventory update packet bridge | `ItemPacketService.sendItemUpdatePacket`, `SM_INVENTORY_UPDATE_ITEM` | `ItemPurificationPacketPlanService.cs`, tests | Packet Planner Bridge | Maybe | Medium | Recommended next if it can stay caller-provided template/item only. |
| C | Concrete inventory delete/add/cube fanout | `ItemPacketService`, `SM_DELETE_ITEM`, `SM_INVENTORY_ADD_ITEM`, `SM_CUBE_UPDATE` | packet plan/service tests, maybe packet DTOs | Packet Planner Bridge | Maybe | Medium-High | Broader than update-only because cube-size counts and target add template/object id are unresolved. |
| D | ItemCharge AP hardening | item charge/AP spend callers | service/test files | Separate AP Caller | Yes later | Medium | Independent fallback if ItemPurification packet bridges grow too wide. |

## Next Recommended Unit of Work

Recommended sequential task:
- Add a narrow concrete `SmInventoryUpdateItem` bridge for `ItemPurificationPacketPlanService` update operations only. Require caller-provided item/template data for the operation and keep delete/add/cube/AP packets metadata-only if live template/cube plumbing expands the scope.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Analyze whether `ItemPurificationPacketPlanService` can accept an operation-object-id to `InventoryItem`/template map without live mutation coupling | read-only C# services/tests | edits, docs | Recommendation for update-packet bridge input shape. |
| Agent B | Reconfirm Java `SM_INVENTORY_UPDATE_ITEM` behavior for partial material stacks only | read-only Java storage/packet files | edits, docs | Edge-case report for update vs delete packet selection. |
| Orchestrator | Add update-packet bridge if safe | `ItemPurificationPacketPlanService.cs`, focused tests, docs | live inventory/AP packet emission | Code, tests, docs, commit. |

## Do Not Parallelize

- `GameServerConnection.cs` live handler edits with packet-plan DTO work.
- Concrete inventory packet emission with object-id allocation/factory work.
- Kinah behavior changes without explicit parity decision.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, add the narrow inventory-update packet bridge or choose another AP/item caller boundary.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
