# Phase 6NN Completion Handoff - Selectable Decompose Full Cube Overflow

Date: May 25, 2026
Unit of Work: UOW-866
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-866] Test selectable decompose full cube overflow`)

## Status

Phase 6 is still in progress. This unit added connection-path regression coverage for selectable decompose when the normal cube is already full.

The important Java parity distinction is that selectable decompose uses `CM_SELECT_DECOMPOSABLE.runImpl`, not the normal `DecomposeAction.canAct` inventory-full guard. Java then calls `ItemService.addItem(..., allowInventoryOverflow=true, ...)`, so the selected reward can still be added beyond the normal cube limit. The C# test now locks this behavior down.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NN-Completion.md`

## What Changed

- Added `HandleSelectDecomposableAsync_FullCubeStillAddsSelectableRewardLikeJavaOverflow`.
- The new test verifies:
  - the base cube starts full at 27 used cube slots
  - selectable decompose still calls persistence
  - source item `101 x2` decrements to `101 x1`
  - reward item `202 x3` is added as an overflow row
  - used cube slots become `28`
  - normal selectable success packets are emitted in order

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests"
```

Result: passed, 25 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1441 tests.

New test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleSelectDecomposableAsync_FullCubeStillAddsSelectableRewardLikeJavaOverflow` | C# selectable decompose proceeds with a full normal cube and adds reward as an overflow row, matching Java `allowInventoryOverflow=true` behavior. | Java `CM_SELECT_DECOMPOSABLE.runImpl` and `ItemService.addItem` source reviewed; Java runtime, packet bytes, and live-client behavior not compared. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `GameServerConnection.HandleSelectDecomposableAsync` / `GameServerConnectionInventoryExpansionUseItemTests` | Client Packet Handler / Runtime Test | Partial | Regression Tested | Partial Parity | Full normal-cube selectable selection is covered for Java's no-`canAct` path: source decrement, reward add, persistence call, and packet emission still occur at 27 used cube slots. Java runtime comparison, encrypted socket loop, and live-client behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` | `InventoryAddService.CreateAddItemPlan` via `CreateDecomposeRewardInventoryPlan` | Service / Inventory Reward Dependency | Partial | Regression Tested through connection path | Partial Parity | Test covers selectable reward add with `allowInventoryOverflow=true`, producing a 28th C# cube row. Special-cube overflow, stack merge ordering beyond this fixture, Java DB/runtime comparison, and packet bytes remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService.ItemUpdatePredicate` / `ItemPacketService.ItemAddType.DECOMPOSABLE` / `ItemUpdateType.INC_ITEM_COLLECT` | `IPlayerEnterWorldRepository.SaveDecomposeActionMutationAsync` / `SmInventoryAddItem.CreateDecomposable` | Repository Boundary / Packet Predicate Dependency | Partial | Regression Tested | Partial Parity | Test asserts one persistence call and normal selectable add packet sequence when inventory is full. SQL/autocommit behavior and Java runtime side effects remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseByObjectId` | `ApplySourceItemMutationAsync` / `SmInventoryUpdateItem.DecreaseItemUse` | Inventory Mutation / Source Consume Dependency | Partial | Regression Tested | Partial Parity | Source stack `101 x2` decrements to `101 x1` before reward add, matching reviewed Java ordering. Java runtime output remains unverified. |
| `com.aionemu.gameserver.model.items.storage.ItemStorage.isFull` | `InventoryCapacity.GetUsedCubeSlots` | Inventory Capacity Dependency | Partial | Regression Tested for selectable overflow distinction | Partial Parity | Test intentionally verifies used cube slots become `28`, documenting selectable behavior differs from normal decompose's pre-use full-inventory guard because Java passes `allowInventoryOverflow=true`. Expansion/kinah/equipped/special-cube cases remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` / `SM_SYSTEM_MESSAGE` / `SM_SECONDARY_SHOW_DECOMPOSABLE` / inventory packets | `SmItemUsageAnimation` / `SmSystemMessage.UncompressCompressedItemSucceeded` / `SmSecondaryShowDecomposable` / inventory packets | Packet / Selection Completion | Partial | Regression Tested for type/order and selected fields | Partial Parity | Packet ordering remains the normal selectable success sequence despite full cube. Full packet byte parity, opcode/frame/crypto, broadcast fanout, and live-client output remain unverified. |

## Remaining Risks

- Special-cube inventory-full behavior has service coverage but not connection-path packet/no-mutation coverage.
- Java runtime behavior under persistence/DAO failure remains unverified and may differ from the C# deferred-mutation transaction-safety boundary.
- Selectable full-cube behavior is based on Java source review rather than live Java runtime comparison.
- Full packet byte parity, opcode/frame/crypto, broadcast fanout, socket visibility, threading/date-time precision, serialization side effects, random reward selection, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 C# runtime regression slice for selectable decompose full-cube overflow behavior
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 12 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Add special-cube inventory-full connection coverage for normal decompose.

Suggested scope:

- Extend the fixture static data with a normal decompose source item whose reward has `<inventory id="2"/>`.
- Add a matching special-cube filler template and fill the player's special cube to 102 used special slots.
- Invoke normal decompose and assert:
  - `SmSystemMessage.DecomposeItemInventoryFull` with id `1300447`
  - no scheduled pending item-use
  - no persistence call
  - no source mutation
  - no reward mutation

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Special-cube connection inventory-full | same item-use fixture and fixture XML | Yes, if exclusive | Best next decompose edge; requires fixture static-data edits. |
| Encrypted socket-loop dispatch audit | connection/socket tests read-only first | Yes as analysis | Can run in parallel only as read-only analysis. |
| Java runtime comparison plan for decompose | docs or read-only Java harness analysis | Yes as analysis | Useful later, but do not mix with fixture edits unless read-only. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Fixture static-data XML edits alongside any other test using the same fixture.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Add special-cube inventory-full connection coverage first unless a newer handoff supersedes this.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
