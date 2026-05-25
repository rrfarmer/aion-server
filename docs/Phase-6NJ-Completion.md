# Phase 6NJ Completion Handoff - Selectable Decompose Source Guards

Date: May 24, 2026
Unit of Work: UOW-862
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-862] Test selectable decompose source guards`)

## Status

Phase 6 is still in progress. This unit added runtime regression coverage for C# selectable decompose source lookup guards: equipped source rows and non-cube source rows do not proceed.

The tests verify C# no-op behavior before persistence, packet emission, or runtime mutation. Java `getInventory().getItemByObjId` equipped/location behavior was not runtime-compared, so parity remains partial.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NJ-Completion.md`

## What Changed

- Added `HandleSelectDecomposableAsync_NonCubeOrEquippedSourceDoesNotCallPersistenceOrSendPackets`.
- Updated `CreatePlayer` fixture helper to accept optional `IsEquipped` and `Location` overrides.
- The new theory verifies:
  - equipped source rows return before persistence, packets, or mutation
  - non-cube-location source rows return before persistence, packets, or mutation
  - source item metadata remains unchanged

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests"
```

Result: passed, 21 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1437 tests.

New test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleSelectDecomposableAsync_NonCubeOrEquippedSourceDoesNotCallPersistenceOrSendPackets` | C# equipped and non-cube selectable source rows return before persistence, packets, or mutation. | Java `CM_SELECT_DECOMPOSABLE` source reviewed; Java inventory lookup equipped/location behavior not runtime-compared. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `GameServerConnection.HandleSelectDecomposableAsync` / `GameServerConnectionInventoryExpansionUseItemTests` | Client Packet Handler / Runtime Test | Partial | Regression Tested | Partial Parity | Equipped and non-cube source guards are covered. Java inventory lookup semantics, inventory-full behavior, encrypted socket loop, Java runtime comparison, and live-client behavior remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.getItemByObjId` | C# source lookup filter `item.Location == CubeStorageId && !item.IsEquipped` | Inventory Lookup Dependency | Partial | Regression Tested for guarded no-op paths | Needs Verification | C# explicitly rejects equipped and non-cube rows. Java equipped/non-cube lookup assumptions require deeper verification. |
| `com.aionemu.gameserver.services.item.ItemService.ItemUpdatePredicate` | `IPlayerEnterWorldRepository.SaveDecomposeActionMutationAsync` / repository call counter | Repository Boundary / Test Support | Partial | Regression Tested for no repository call | Partial Parity | Persistence is not reached when source lookup rejects the item. Live SQL behavior remains unverified. |
| `com.aionemu.gameserver.services.item.ItemService` / `ItemPacketService.ItemAddType.DECOMPOSABLE` | `InventoryAddService.CreateAddItemPlan` guarded by source lookup success | Inventory Mutation / Reward Add Dependency | Partial | Regression Tested for no-op guard | Partial Parity | Reward planning/add is not reached for equipped/non-cube source rows. Inventory-full behavior remains unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseByObjectId` | `ApplySourceItemMutationAsync` guarded by source lookup success | Inventory Mutation / Source Consume Dependency | Partial | Regression Tested for no-op guard | Partial Parity | Source consume is not reached for equipped/non-cube source rows. Java runtime behavior remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` / `SM_SYSTEM_MESSAGE` / `SM_SECONDARY_SHOW_DECOMPOSABLE` / inventory packets | C# packet emission guarded by source lookup success | Packet / Early-return Dependency | Partial | Regression Tested for absence on guarded paths | Partial Parity | Guarded paths emit no packets. Full packet byte parity and live-client output remain unverified. |

## Remaining Risks

- Java runtime semantics for equipped/non-cube `getInventory().getItemByObjId` remain unverified.
- Inventory-full behavior for selectable selection remains untested.
- Dispatch coverage still bypasses the encrypted socket read loop and active-player setup lifecycle.
- Java runtime behavior under persistence/DAO failure remains unverified and may differ from the C# deferred-mutation transaction-safety boundary.
- Full packet byte parity, opcode/frame/crypto, broadcast fanout, socket visibility, threading/date-time precision, serialization side effects, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 C# runtime regression slice for selectable decompose equipped/non-cube source guards
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 15 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Move to the next decompose completion gap: add a normal decompose successful completion regression if the existing fixture can wait through the scheduler and assert source consume, reward add, success animation `end=1`, and reward packets. If that is too slow/flaky, add selectable/normal inventory-full behavior through `DecomposeService` first.

Suggested scope:

- Reuse the normal decompose fixture item `100` and reward `200`.
- Let the scheduled 3000 ms completion run, then assert source decrement/delete, reward add, final success animation `end=1`, and reward packet ordering.
- Keep Java runtime comparison, full packet bytes, and live-client validation documented as gaps.
- If scheduler completion is too slow/flaky, switch to a service-level inventory-full regression.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Normal decompose completion test | same item-use fixture | Yes, if exclusive | Best next runtime gap if timing is stable. |
| Inventory-full service test | separate service test if one exists | Maybe | Safer if it avoids the shared fixture. |
| Encrypted socket-loop dispatch audit | connection/socket tests read-only first | Yes as analysis | Could become larger than a narrow unit. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Scheduler completion edits alongside service inventory tests if they share fixtures.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Try normal decompose completion regression first; fall back to service-level inventory-full if scheduler timing is unsuitable.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
