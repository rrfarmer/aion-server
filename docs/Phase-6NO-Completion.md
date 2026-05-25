# Phase 6NO Completion Handoff - Normal Decompose Special Cube Full Guard

Date: May 25, 2026
Unit of Work: UOW-867
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-867] Test normal decompose special cube full guard`)

## Status

Phase 6 is still in progress. This unit added connection-path regression coverage for the normal decompose special-cube inventory-full guard.

The test verifies that when a normal decompose reward would go to the special cube and the special cube is full, C# sends the Java inventory-full system message and does not schedule delayed use, call persistence, consume the source, add rewards, or emit usage animation. Java runtime output was not executed, so parity remains partial.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NO-Completion.md`

## What Changed

- Extended the item-use fixture static data with:
  - source item `102` with normal `<decompose/>`
  - special-cube reward item `204` with `<inventory id="2"/>`
  - special-cube filler item `205` with `<inventory id="2"/>`
- Added `HandleUseItemAsync_DecomposeSpecialCubeFullDoesNotScheduleOrMutate`.
- The new test verifies:
  - the special cube is full with 102 special-cube filler rows
  - normal decompose sends `SmSystemMessage.DecomposeItemInventoryFull` with id `1300447`
  - `UsingItemObjectId` remains `0`
  - `SaveDecomposeActionMutationAsync` is not called
  - source and filler inventory rows remain unchanged
  - no usage animation or reward packet is emitted

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests"
```

Result: passed, 26 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1442 tests.

New test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_DecomposeSpecialCubeFullDoesNotScheduleOrMutate` | C# connection behavior for normal decompose with a full special cube and a special-cube reward: message id `1300447`, no schedule, no persistence, no source/reward mutation. | Java `DecomposeAction.canAct` and `containsSpecialCubeItems` source reviewed; Java runtime, packet bytes, and live-client behavior not compared. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction.canAct` | `DecomposeService.CanAct` via `GameServerConnection.HandleDecomposeUseItemAsync` | Item Action Guard / Runtime Handler | Partial | Regression Tested | Partial Parity | Special-cube full guard is covered through the connection path. Java runtime comparison and live-client behavior remain unverified. |
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction.containsSpecialCubeItems` | `DecomposeService.ContainsSpecialCubeItems` private helper | Item Action Guard / Static Data Dependency | Partial | Regression Tested through connection path | Partial Parity | Fixture reward `204` has `ExtraInventoryId=2`, causing the full special-cube rejection. Mixed random rewards and broader level/race filtering remain unverified. |
| `com.aionemu.gameserver.model.items.storage.ItemStorage.isFullSpecialCube` / `Player.getInventory().isFullSpecialCube` | `InventoryCapacity.HasFreeSpecialCubeSlot` / `GetUsedSpecialCubeSlots` | Inventory Capacity Dependency | Partial | Regression Tested through connection path | Partial Parity | Test fills 102 special-cube rows. Java expansion/limit constants and live storage behavior are source-reviewed but not runtime-compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_DECOMPOSE_ITEM_INVENTORY_IS_FULL` | `SmSystemMessage.DecomposeItemInventoryFull` | Packet / System Message | Partial | Regression Tested for message id | Partial Parity | Serialized system-message id `1300447` is asserted. Full Java packet byte parity, localization, opcode/frame/crypto, and live-client output remain unverified. |
| `com.aionemu.gameserver.controllers.CreatureController.addTask(TaskId.ITEM_USE)` / `ThreadPoolManager.schedule` | `SchedulePendingItemUseAsync` | Scheduler / Threading Dependency | Partial | Regression Tested for non-use on guard failure | Needs Verification | Guard rejection leaves pending item-use unset. Java task lifecycle and threading remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseByObjectId` | `ApplySourceItemMutationAsync` guarded by `CanAct` success | Inventory Mutation / Source Consume Dependency | Partial | Regression Tested for no-op guard | Partial Parity | Source consume is not reached when special cube is full. Java runtime behavior remains unverified. |
| `com.aionemu.gameserver.services.item.ItemService` / `ItemPacketService.ItemAddType.DECOMPOSABLE` | `InventoryAddService.CreateAddItemPlan` guarded by `CanAct` success | Inventory Reward Dependency | Partial | Regression Tested for no-op guard | Partial Parity | Reward planning/add is not reached on special-cube full normal decompose rejection. Java DB/runtime comparison remains unverified. |
| `com.aionemu.gameserver.services.item.ItemService.ItemUpdatePredicate` | `IPlayerEnterWorldRepository.SaveDecomposeActionMutationAsync` / repository call counter | Repository Boundary / Test Support | Partial | Regression Tested for no repository call | Partial Parity | Persistence is not called on special-cube inventory-full rejection. Live SQL/autocommit behavior remains unverified. |

## Remaining Risks

- Java runtime behavior under persistence/DAO failure remains unverified and may differ from the C# deferred-mutation transaction-safety boundary.
- Decompose coverage is still mostly connection/test-fixture based rather than live Java-vs-C# runtime comparison.
- Encrypted socket-loop dispatch and active-player lifecycle setup remain only partially covered.
- Full packet byte parity, opcode/frame/crypto, broadcast fanout, socket visibility, threading/date-time precision, serialization side effects, random reward selection, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 1 C# runtime regression slice for normal decompose special-cube inventory-full connection guard
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 11 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Move from decompose branch coverage to dispatch/read-loop validation.

Suggested scope:

- First do a read-only audit of the current encrypted/socket-loop tests and `GameServerConnection` packet read path.
- If a compact fixture exists, add encrypted or socket-loop coverage for `CM_SELECT_DECOMPOSABLE` or normal `CM_USE_ITEM` decompose routing.
- If socket-loop setup expands too much, create a focused Java-runtime comparison plan for decompose packet/order behavior before implementing more code.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Socket-loop dispatch audit | read-only across connection/socket tests and `GameServerConnection` | Yes as analysis | Best next step before writing socket-loop tests. |
| Encrypted selectable dispatch test | likely connection/socket test file | Maybe | Only safe after audit identifies isolated target files. |
| Java runtime comparison plan | docs/read-only Java harness analysis | Yes as analysis | Useful if live socket test is too broad. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Fixture static-data XML edits alongside socket-loop test edits unless one agent owns the whole fixture.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Audit socket-loop/dispatch coverage before choosing the next implementation unit.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
