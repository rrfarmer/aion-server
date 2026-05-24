# Phase 6NC Completion Handoff - Selectable Decompose Invalid Selection

Date: May 24, 2026
Unit of Work: UOW-855
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-855] Test selectable decompose invalid selection`)

## Status

Phase 6 is still in progress. This unit added a compact runtime regression for invalid selectable decompose reward-selection indexes.

The test verifies C# early-return behavior for invalid and post-filter invalid selection indexes. It does not cover missing source/data/template paths, source delete, stack merge, persistence failure, Java runtime comparison, or live-client validation.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NC-Completion.md`

## What Changed

- Added `HandleSelectDecomposableAsync_InvalidSelectableRewardIndexDoesNotMutateInventory`.
- The new theory covers:
  - index `2`, which is invalid after the Asmodian-only reward candidate is filtered out for the Elyos ranger fixture
  - index `99`, a clearly out-of-range index
- The test verifies both cases:
  - preserve the source item `101`
  - preserve source count `2`
  - add no rewards
  - emit no packets

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests"
```

Result: passed, 12 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1428 tests.

New test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleSelectDecomposableAsync_InvalidSelectableRewardIndexDoesNotMutateInventory` | C# invalid selectable reward indexes return before packet emission or inventory mutation. | Source-derived Java review of `CM_SELECT_DECOMPOSABLE.runImpl`; no Java runtime comparison. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `GameServerConnection.HandleSelectDecomposableAsync` / `GameServerConnectionInventoryExpansionUseItemTests` | Client Packet Handler / Runtime Test | Partial | Regression Tested | Partial Parity | Invalid and post-filter invalid indexes are covered. Missing source/data/template, source-delete, stack-merge, persistence failure, packet-factory dispatch, Java runtime comparison, and live-client behavior remain unverified. |
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | fixture `DecomposableItemTable` / `DecomposeService.CreateSelectableRewardPlan` | Static Data / Reward Selection Dependency | Partial | Regression Tested through invalid indexes | Partial Parity | Test proves filtering happens before index validation for this fixture. Broader race/class combinations and Java runtime output remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseByObjectId` | `ApplySourceItemMutationAsync` guarded by selection plan success | Inventory Mutation / Source Consume Dependency | Partial | Regression Tested for no-op invalid paths | Partial Parity | Invalid selections do not decrement source. Single-count delete success path and delete-packet ordering remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `SmItemUsageAnimation` | Packet / Early-return Dependency | Partial | Regression Tested for absence on invalid paths | Partial Parity | Invalid selections emit no animation. Broadcast fanout and Java bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage.UncompressCompressedItemSucceeded(...)` | System Message / Early-return Dependency | Partial | Regression Tested for absence on invalid paths | Partial Parity | Invalid selections emit no success message. Success payload bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SECONDARY_SHOW_DECOMPOSABLE` | `SmSecondaryShowDecomposable` | Packet / Early-return Dependency | Partial | Regression Tested for absence on invalid paths | Partial Parity | Invalid selections emit no secondary-show packet. Java bytes remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService` / `ItemPacketService.ItemAddType.DECOMPOSABLE` | `InventoryAddService.CreateAddItemPlan` / reward packet emission guarded by selection plan success | Inventory Mutation / Reward Add Dependency | Partial | Regression Tested for no-op invalid paths | Partial Parity | Invalid selections add no reward. Existing-stack merge, inventory-full behavior, and persistence rollback remain unverified. |

## Remaining Risks

- Missing source item, missing selectable data, missing reward template, and persistence failure paths still need coverage.
- Source consume still covers decrement only; single-count delete and delete-packet ordering remain unverified.
- Reward add still covers new-row add only; existing-stack merge and inventory-full handling remain unverified.
- The selection tests invoke a private handler reflectively; packet-factory dispatch for real client frames remains unverified.
- Parity is based on C# runtime tests and Java source review, not Java-generated packet bytes or live Java runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 1 C# runtime regression slice for selectable decompose invalid-index early returns
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 23 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Add a narrow selectable selection source-delete regression: configure or mutate the source count to `1`, select a valid reward, verify source removal and `SmDeleteItem` ordering alongside reward add, and keep existing-stack merge/persistence failure for later unless very small.

Suggested scope:

- Reuse the selectable fixture and set the source inventory item count to `1` before invoking selection.
- Select index `0` or `1`; fixed reward counts make either deterministic.
- Assert the source item is removed from `player.InventoryItems`.
- Assert packet order includes `SmDeleteItem` where the decrement test currently sees `SmInventoryUpdateItem`.
- Leave existing-stack merge and persistence failure as separate units if they expand the fixture.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Source-delete selection test | same test file | Yes, if exclusive | Best next unit. |
| Existing-stack merge selection audit | test file and inventory add planner read-only | Yes after source-delete | Separate follow-up candidate. |
| Persistence failure selection audit | repository/service/test read-only | Yes after source-delete | Separate follow-up candidate. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Broad handler edits unless the source-delete test exposes a mismatch.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Keep the next unit narrow: selectable source-delete success path.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
