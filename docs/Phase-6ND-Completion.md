# Phase 6ND Completion Handoff - Selectable Decompose Source Delete

Date: May 24, 2026
Unit of Work: UOW-856
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-856] Test selectable decompose source delete`)

## Status

Phase 6 is still in progress. This unit added a runtime regression for the selectable decompose valid selection path when the source item has count `1` and must be deleted.

The test verifies the C# source-delete branch and packet order. It does not cover existing-stack reward merge, persistence failure, packet-factory dispatch, Java runtime comparison, or live-client validation.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ND-Completion.md`

## What Changed

- Added `HandleSelectDecomposableAsync_SelectableRewardDeletesSingleCountSource`.
- Updated the fixture helper `CreatePlayer` to accept an optional source item count.
- The new test verifies:
  - valid selection index `0` maps to reward item `201`
  - a single-count source item `101` is removed from `player.InventoryItems`
  - reward item `201` is added with count `2`
  - packet order is default `SmItemUsageAnimation`, success `SmSystemMessage`, source `SmDeleteItem`, empty `SmSecondaryShowDecomposable`, and reward `SmInventoryAddItem`

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests"
```

Result: passed, 13 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1429 tests.

New test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleSelectDecomposableAsync_SelectableRewardDeletesSingleCountSource` | C# selectable selection source-delete runtime behavior, delete packet ordering, empty secondary-show ordering, and reward add mutation. | Source-derived Java review of `CM_SELECT_DECOMPOSABLE.runImpl`; no Java runtime comparison. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `GameServerConnection.HandleSelectDecomposableAsync` / `GameServerConnectionInventoryExpansionUseItemTests` | Client Packet Handler / Runtime Test | Partial | Regression Tested | Partial Parity | Single-count source delete is covered. Existing-stack merge, persistence failure, missing source/data/template, packet-factory dispatch, Java runtime comparison, and live-client behavior remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseByObjectId` | `ApplySourceItemMutationAsync` / `SmDeleteItem.UseDeleteType` | Inventory Mutation / Source Consume Dependency | Partial | Regression Tested for delete path | Partial Parity | Source decrement and delete branches now have selectable selection coverage. Delete packet bytes and rollback behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService` / `ItemPacketService.ItemAddType.DECOMPOSABLE` | `InventoryAddService.CreateAddItemPlan` / `SmInventoryAddItem.CreateDecomposable` | Inventory Mutation / Reward Add Dependency | Partial | Regression Tested for new-row reward add with delete source | Partial Parity | New-row reward add is covered for both decrement and delete source paths. Existing-stack merge and inventory-full handling remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `SmItemUsageAnimation` | Packet / Selection Completion | Partial | Regression Tested for this call site | Partial Parity | Default selection animation is asserted. Java bytes, opcode/frame/crypto, broadcast fanout, and write-time `usingItem` side effects remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage.UncompressCompressedItemSucceeded(...)` | System Message / Selection Completion | Partial | Regression Tested for packet type/order | Needs Verification | Packet type/order is covered; payload bytes and localization remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SECONDARY_SHOW_DECOMPOSABLE` | `SmSecondaryShowDecomposable` | Packet / Selection Completion | Partial | Regression Tested for empty-list payload | Partial Parity | Empty secondary-show ordering after delete is covered. Java bytes remain unverified. |
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | fixture `DecomposableItemTable` / `DecomposeService.CreateSelectableRewardPlan` | Static Data / Reward Selection Dependency | Partial | Regression Tested through valid index | Partial Parity | Valid reward `201` with fixed count `2` is covered. Broader race/class and random min/max behavior remain unverified. |

## Remaining Risks

- Existing-stack reward merge, inventory-full behavior, missing source/data/template paths, and persistence failure paths still need coverage.
- Delete packet byte payload and repository rollback behavior remain unverified.
- The selection tests invoke a private handler reflectively; packet-factory dispatch for real client frames remains unverified.
- Parity is based on C# runtime tests and Java source review, not Java-generated packet bytes or live Java runtime comparison.
- Packet opcode/frame/crypto, broadcast fanout, socket visibility, threading/date-time precision, serialization side effects, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 1 C# runtime regression slice for selectable decompose source-delete selection
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 21 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Add a narrow selectable reward existing-stack merge regression: give the player an existing stack for reward `201` or `202`, select that reward, verify the existing stack count increases and `SmInventoryUpdateItem.IncreaseItemCollect` is emitted instead of `SmInventoryAddItem`.

Suggested scope:

- Reuse the selectable fixture and add an existing reward stack to `player.InventoryItems`.
- Select the matching reward and keep source count `2` unless the delete branch is intentionally part of the test.
- Assert source decrement, reward stack increment, and packet order.
- Leave persistence failure and missing-template behavior as separate units if the test starts growing.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Existing-stack merge selection test | same test file | Yes, if exclusive | Best next unit. |
| Persistence failure selection audit | repository/service/test read-only | Yes after merge test | Separate follow-up candidate. |
| Missing-template/data negative audit | handler/service/test read-only | Yes after merge test | Separate follow-up candidate. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Broad handler edits unless the merge test exposes a mismatch.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Keep the next unit narrow: selectable reward existing-stack merge.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
