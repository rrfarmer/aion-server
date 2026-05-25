# Phase 6NG Completion Handoff - Selectable Decompose Missing Source/Data

Date: May 24, 2026
Unit of Work: UOW-859
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-859] Test selectable decompose missing data`)

## Status

Phase 6 is still in progress. This unit added compact runtime regressions for selectable decompose selection no-op paths when the source object is missing or the source item has no selectable decompose data.

The tests verify C# early-return behavior before persistence, packet emission, or runtime mutation. They still use the private handler seam; packet-factory dispatch remains the main next risk.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NG-Completion.md`

## What Changed

- Added `HandleSelectDecomposableAsync_MissingSourceDoesNotCallPersistenceOrSendPackets`.
- Added `HandleSelectDecomposableAsync_NonSelectableSourceDoesNotCallPersistenceOrSendPackets`.
- The new tests verify:
  - missing source object id returns without repository calls
  - non-selectable normal decompose source returns without repository calls
  - source inventory remains unchanged
  - no packets are emitted

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests"
```

Result: passed, 17 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1433 tests.

New tests:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleSelectDecomposableAsync_MissingSourceDoesNotCallPersistenceOrSendPackets` | C# missing source object id returns before persistence, packets, or mutation. | Source-derived Java review of `CM_SELECT_DECOMPOSABLE.runImpl`; no Java runtime comparison. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleSelectDecomposableAsync_NonSelectableSourceDoesNotCallPersistenceOrSendPackets` | C# non-selectable decompose source returns before persistence, packets, or mutation. | Source-derived Java review of `CM_SELECT_DECOMPOSABLE.runImpl`; no Java runtime comparison. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `GameServerConnection.HandleSelectDecomposableAsync` / `GameServerConnectionInventoryExpansionUseItemTests` | Client Packet Handler / Runtime Test | Partial | Regression Tested | Partial Parity | Missing source and non-selectable source early returns are covered. Missing reward template, equipped/location filtering, packet-factory dispatch, Java runtime comparison, and live-client behavior remain unverified. |
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | fixture `DecomposableItemTable` / `DecomposeService.CreateSelectableRewardPlan` | Static Data / Reward Selection Dependency | Partial | Regression Tested for non-selectable no-op | Partial Parity | Normal decompose data without selectable list produces no-op selection. Missing reward template and broader data edge cases remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.getItemByObjId` | C# inventory lookup in `HandleSelectDecomposableAsync` | Inventory Lookup Dependency | Partial | Regression Tested for missing object id | Partial Parity | Missing object id returns before repository calls, packets, or mutation. Location/equipped filtering edge cases remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService` / `ItemPacketService.ItemAddType.DECOMPOSABLE` | `InventoryAddService.CreateAddItemPlan` guarded by selection plan success | Inventory Mutation / Reward Add Dependency | Partial | Regression Tested for no-op guards | Partial Parity | Missing source/data tests prove reward add is not reached. Missing reward template and inventory-full behavior remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseByObjectId` | `ApplySourceItemMutationAsync` guarded by source and selection plan success | Inventory Mutation / Source Consume Dependency | Partial | Regression Tested for no-op guards | Partial Parity | Missing source/data tests prove source consume is not reached. Equipped/location filtering and persistence rollback remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` / `SM_SYSTEM_MESSAGE` / `SM_SECONDARY_SHOW_DECOMPOSABLE` / inventory packets | C# packet emission guarded by source and selection plan success | Packet / Early-return Dependency | Partial | Regression Tested for absence on no-op paths | Partial Parity | Missing source/data tests emit no packets. Full packet byte parity and live dispatch remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService.ItemUpdatePredicate` | `IPlayerEnterWorldRepository.SaveDecomposeActionMutationAsync` / repository call counter | Repository Boundary / Test Support | Partial | Regression Tested for no-op guards | Partial Parity | Tests assert persistence is not called for no-op paths. Live SQL behavior remains unverified. |

## Remaining Risks

- Missing reward template, equipped/location source filtering, and inventory-full behavior still need coverage.
- The selection tests invoke a private handler reflectively; packet-factory dispatch for real client frames remains unverified and is now the main seam risk for this packet family.
- Java runtime behavior under persistence/DAO failure remains unverified and may differ from the C# deferred-mutation transaction-safety boundary.
- Full packet byte parity, opcode/frame/crypto, broadcast fanout, socket visibility, threading/date-time precision, serialization side effects, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 1 C# runtime regression slice for selectable decompose missing-source/data early returns
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 18 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Add packet-factory dispatch coverage for `CM_SELECT_DECOMPOSABLE` if feasible with the existing connection fixture: feed an opcode `236` frame through the normal packet processing path and prove it reaches the same selection behavior without reflective handler invocation. If dispatch setup is too broad, add missing reward-template no-op coverage first.

Suggested scope:

- First inspect existing packet processor/connection tests for `ProcessPacketAsync` or equivalent helper seams.
- Prefer reusing `GamePacketTests.ClientPacketFactory_ParsesSelectDecomposablePacket` if dispatch cannot be cheaply done at connection level.
- Keep the test to one valid selection branch; do not combine dispatch with new mutation edge cases.
- If dispatch is too broad, use the fallback missing reward-template no-op test and document dispatch as still open.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Packet-factory dispatch audit | packet factory/tests read-only or separate packet test | Yes if not editing fixture | Best next analysis step. |
| Missing reward-template no-op test | same item-use fixture | Yes, if exclusive | Fallback next unit if dispatch is too broad. |
| Java DB failure behavior research | Java source/read-only | Yes | Helps classify C# deferred-mutation difference. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Packet dispatch infrastructure edits alongside fixture edits.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Try packet-factory dispatch coverage first; fall back to missing reward-template no-op if dispatch is too broad.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
