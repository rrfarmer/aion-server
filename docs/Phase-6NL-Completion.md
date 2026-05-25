# Phase 6NL Completion Handoff - Normal Decompose Inventory Full Guard

Date: May 24, 2026
Unit of Work: UOW-864
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-864] Test normal decompose inventory full guard`)

## Status

Phase 6 is still in progress. This unit added connection-path regression coverage for the normal decompose inventory-full guard before scheduling.

The test verifies that a full normal cube sends the Java inventory-full system message id and does not schedule delayed use, call persistence, consume the source, add rewards, or emit usage animation. Java runtime output was not executed, so parity remains partial.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NL-Completion.md`

## What Changed

- Added `HandleUseItemAsync_DecomposeInventoryFullDoesNotScheduleOrMutate`.
- Added `AssertSystemMessagePayload` helper to assert serialized system-message ids in the item-use fixture.
- The new test verifies:
  - full base normal cube rejects decompose before scheduling
  - `UsingItemObjectId` remains `0`
  - `SaveDecomposeActionMutationAsync` is not called
  - source and filler inventory rows remain unchanged
  - exactly one packet is sent: `SmSystemMessage.DecomposeItemInventoryFull` with id `1300447`

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests"
```

Result: passed, 23 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1439 tests.

New test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_DecomposeInventoryFullDoesNotScheduleOrMutate` | C# connection behavior for normal decompose with a full normal cube: message id `1300447`, no schedule, no persistence, no source/reward mutation. | Java `DecomposeAction.canAct` source reviewed; Java runtime, packet bytes, and live-client behavior not compared. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction.canAct` | `DecomposeService.CanAct` via `GameServerConnection.HandleDecomposeUseItemAsync` | Item Action Guard / Runtime Handler | Partial | Regression Tested | Partial Parity | Normal cube full guard is covered before scheduling and mutation. Selectable inventory-full, special-cube connection branch, Java runtime comparison, and live-client behavior remain unverified. |
| `com.aionemu.gameserver.model.items.storage.ItemStorage.isFull` / `Player.getInventory().isFull` | `InventoryCapacity.HasFreeCubeSlot` | Inventory Capacity Dependency | Partial | Regression Tested through connection path | Partial Parity | Test fills the base C# cube limit of 27 normal-cube slots. Java expansion, kinah/equipped exclusions, and special-cube edge cases remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_DECOMPOSE_ITEM_INVENTORY_IS_FULL` | `SmSystemMessage.DecomposeItemInventoryFull` | Packet / System Message | Partial | Regression Tested for message id | Partial Parity | Serialized system-message id `1300447` is asserted. Full Java packet byte parity, localization, opcode/frame/crypto, and live-client output remain unverified. |
| `com.aionemu.gameserver.controllers.CreatureController.addTask(TaskId.ITEM_USE)` / `ThreadPoolManager.schedule` | `SchedulePendingItemUseAsync` | Scheduler / Threading Dependency | Partial | Regression Tested for non-use on guard failure | Needs Verification | Guard rejection leaves pending item-use unset. Java task lifecycle and threading remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseByObjectId` | `ApplySourceItemMutationAsync` guarded by `CanAct` success | Inventory Mutation / Source Consume Dependency | Partial | Regression Tested for no-op guard | Partial Parity | Source consume is not reached when normal cube is full. Source-delete success path and Java runtime behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService` / `ItemPacketService.ItemAddType.DECOMPOSABLE` | `InventoryAddService.CreateAddItemPlan` guarded by `CanAct` success | Inventory Reward Dependency | Partial | Regression Tested for no-op guard | Partial Parity | Reward planning/add is not reached on normal inventory-full rejection. Selectable inventory-full and special-cube connection behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService.ItemUpdatePredicate` | `IPlayerEnterWorldRepository.SaveDecomposeActionMutationAsync` / repository call counter | Repository Boundary / Test Support | Partial | Regression Tested for no repository call | Partial Parity | Persistence is not called on inventory-full rejection. Live SQL/autocommit behavior remains unverified. |

## Remaining Risks

- Selectable decompose inventory-full behavior remains untested at the connection level.
- Special-cube inventory-full behavior has service coverage but not connection-path packet/no-mutation coverage.
- Normal decompose source-delete scheduled completion needs explicit coverage.
- Java runtime behavior under persistence/DAO failure remains unverified and may differ from the C# deferred-mutation transaction-safety boundary.
- Full packet byte parity, opcode/frame/crypto, broadcast fanout, socket visibility, threading/date-time precision, serialization side effects, random reward selection, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 1 C# runtime regression slice for normal decompose inventory-full connection guard
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 14 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Add normal decompose source-delete scheduled completion using a source count of `1`: wait through the scheduler and assert source removal, `SmDeleteItem` use-delete packet, reward add, success message, final animation `end=1`, and pending cleanup.

Follow-ups after that:

- Selectable inventory-full connection behavior.
- Special-cube inventory-full connection behavior.
- Encrypted socket-loop dispatch and live-client validation.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Normal source-delete scheduled completion | same item-use fixture | Yes, if exclusive | Best compact next gap. |
| Selectable inventory-full connection behavior | same item-use fixture | No with source-delete | Useful but shares fixture ownership. |
| Special-cube connection inventory-full | fixture static data plus item-use tests | No with other fixture edits | May require static-data fixture expansion. |
| Encrypted socket-loop dispatch audit | connection/socket tests read-only first | Yes as analysis | Could become larger than a narrow unit. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Special-cube fixture static-data edits alongside normal source-delete fixture assertions.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Add normal source-delete scheduled completion coverage first unless a newer handoff supersedes this.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
