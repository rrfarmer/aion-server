# Phase 6NI Completion Handoff - Selectable Decompose Missing Reward Template

Date: May 24, 2026
Unit of Work: UOW-861
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-861] Test selectable decompose missing reward template`)

## Status

Phase 6 is still in progress. This unit added runtime regression coverage for the C# selectable decompose guard when the selected reward has no item template.

The test verifies the C# no-op behavior before persistence, packet emission, or runtime mutation. Java packet source was reviewed, but Java `ItemService.addItem` behavior for missing reward templates was not runtime-compared.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NI-Completion.md`

## What Changed

- Added `HandleSelectDecomposableAsync_MissingRewardTemplateDoesNotCallPersistenceOrSendPackets`.
- Updated `CreatePlayer` fixture helper to accept optional race/class overrides.
- The new test verifies:
  - Asmodian/Gladiator reward filtering exposes missing-template reward `203`
  - selecting index `1` chooses that missing-template reward after filtering
  - no decompose persistence call occurs
  - source inventory remains unchanged
  - no packets are emitted

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests"
```

Result: passed, 19 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1435 tests.

New test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleSelectDecomposableAsync_MissingRewardTemplateDoesNotCallPersistenceOrSendPackets` | C# missing selected reward template returns before persistence, packets, or mutation. | Java `CM_SELECT_DECOMPOSABLE` source reviewed; Java runtime missing-template behavior not compared. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `GameServerConnection.HandleSelectDecomposableAsync` / `GameServerConnectionInventoryExpansionUseItemTests` | Client Packet Handler / Runtime Test | Partial | Regression Tested | Partial Parity | C# missing reward-template guard is covered after obtainable filtering and reward selection. Java runtime behavior for missing templates remains unverified. |
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | fixture `DecomposableItemTable` / `DecomposeService.CreateSelectableRewardPlan` | Static Data / Reward Selection Dependency | Partial | Regression Tested for race-filtered missing-template selection | Partial Parity | Asmodian/Gladiator filtering makes reward `203` selectable at index `1`. Broader race/class combinations remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService` / `ItemPacketService.ItemAddType.DECOMPOSABLE` | `InventoryAddService.CreateAddItemPlan` / `CreateDecomposeRewardInventoryPlan` | Inventory Mutation / Reward Add Dependency | Partial | Regression Tested for missing-template no-op | Partial Parity | C# returns before persistence/mutation when selected reward has no item template. Java `ItemService.addItem` missing-template behavior remains unverified. |
| `com.aionemu.gameserver.services.item.ItemService.ItemUpdatePredicate` | `IPlayerEnterWorldRepository.SaveDecomposeActionMutationAsync` / repository call counter | Repository Boundary / Test Support | Partial | Regression Tested for no repository call | Partial Parity | Persistence is not called when reward template is missing. Live SQL behavior remains unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseByObjectId` | `ApplySourceItemMutationAsync` guarded by reward inventory plan success | Inventory Mutation / Source Consume Dependency | Partial | Regression Tested for no-op guard | Partial Parity | Source consume is not reached for missing reward template. Equipped/location source filtering remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` / `SM_SYSTEM_MESSAGE` / `SM_SECONDARY_SHOW_DECOMPOSABLE` / inventory packets | C# packet emission guarded by reward inventory plan success | Packet / Early-return Dependency | Partial | Regression Tested for absence on missing-template path | Partial Parity | Missing reward-template path emits no packets. Full packet byte parity and live-client output remain unverified. |

## Remaining Risks

- Java runtime behavior for missing reward templates is unverified; this C# no-op may be a safety guard rather than proven Java-equivalent behavior.
- Equipped/location source filtering and inventory-full behavior still need coverage.
- Dispatch coverage still bypasses the encrypted socket read loop and active-player setup lifecycle.
- Java runtime behavior under persistence/DAO failure remains unverified and may differ from the C# deferred-mutation transaction-safety boundary.
- Full packet byte parity, opcode/frame/crypto, broadcast fanout, socket visibility, threading/date-time precision, serialization side effects, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 C# runtime regression slice for selectable decompose missing reward-template guard
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 16 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Add compact equipped/location source filtering coverage for selectable decompose selection: equipped source item and/or non-cube location source should return without repository calls, packets, or runtime mutation, matching C#'s current guard and documenting the Java inventory lookup assumption.

Suggested scope:

- Reuse the selectable fixture.
- Add helper parameters to create a source item with `IsEquipped=true` and/or non-zero non-cube location.
- Assert no repository call, unchanged inventory, and no packets.
- Document that Java `getInventory().getItemByObjId` assumptions around equipped/non-cube items still need runtime comparison if ambiguous.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Equipped/location no-op tests | same item-use fixture | Yes, if exclusive | Best next small unit. |
| Encrypted socket-loop dispatch audit | connection/socket tests read-only first | Yes as analysis | Could become larger than a narrow unit. |
| Java missing-template behavior research | Java source/read-only | Yes | Helps classify the C# missing-template guard. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Socket/dispatch infrastructure edits alongside fixture edits.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Keep the next unit narrow: equipped/location source no-op.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
