# Phase 6NF Completion Handoff - Selectable Decompose Persistence Failure

Date: May 24, 2026
Unit of Work: UOW-858
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-858] Test selectable decompose persistence failure`)

## Status

Phase 6 is still in progress. This unit added a runtime regression for the selectable decompose selection persistence-failure boundary.

The test verifies the C# deferred-mutation behavior when `SaveDecomposeActionMutationAsync` returns `false`. It does not prove Java runtime parity for DAO/autocommit failures, and it does not cover missing source/data/template paths, packet-factory dispatch, or live-client validation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NF-Completion.md`

## What Changed

- Extended `EmptyPlayerEnterWorldRepository` with:
  - `SaveDecomposeActionMutationResult`
  - `SaveDecomposeActionMutationCalls`
- Added `HandleSelectDecomposableAsync_PersistenceFailureDoesNotMutateRuntimeInventory`.
- The new test verifies:
  - `SaveDecomposeActionMutationAsync` is called once
  - source item `101` remains count `2`
  - existing reward stack `201` remains count `4`
  - no packets are emitted when persistence returns `false`

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests"
```

Result: passed, 15 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1431 tests.

New test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleSelectDecomposableAsync_PersistenceFailureDoesNotMutateRuntimeInventory` | C# selectable selection persistence failure returns before runtime source/reward mutation and before packet emission. | Java source ordering reviewed, but Java runtime DAO/autocommit failure behavior was not compared. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `GameServerConnection.HandleSelectDecomposableAsync` / `GameServerConnectionInventoryExpansionUseItemTests` | Client Packet Handler / Runtime Test | Partial | Regression Tested | Partial Parity | C# persistence-failure boundary is covered. Java runtime DAO/autocommit failure behavior, missing source/data/template paths, packet-factory dispatch, and live-client behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService` / `ItemPacketService.ItemAddType.DECOMPOSABLE` | `InventoryAddService.CreateAddItemPlan` / deferred `ApplyRewardInventoryMutation` | Inventory Mutation / Reward Add Dependency | Partial | Regression Tested for persistence no-op | Partial Parity | Planned reward merge is not applied when repository persistence fails. Java DB/runtime rollback behavior and inventory-full behavior remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseByObjectId` | `ApplySourceItemMutationAsync` guarded by persistence success | Inventory Mutation / Source Consume Dependency | Partial | Regression Tested for persistence no-op | Partial Parity | Source decrement is not applied when persistence fails. C# intentionally defers runtime mutation until persistence success; Java runtime failure behavior remains unverified. |
| `com.aionemu.gameserver.services.item.ItemService.ItemUpdatePredicate` | `IPlayerEnterWorldRepository.SaveDecomposeActionMutationAsync` / `EmptyPlayerEnterWorldRepository.SaveDecomposeActionMutationResult` | Repository Boundary / Test Support | Partial | Regression Tested through connection fixture | Needs Verification | Test-helper control added for decompose persistence failure. Production SQL behavior is unchanged; live SQL transaction/autocommit parity remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `SmItemUsageAnimation` | Packet / Persistence Failure Dependency | Partial | Regression Tested for absence on persistence failure | Partial Parity | C# persistence failure emits no animation. Java failure behavior remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage.UncompressCompressedItemSucceeded(...)` | System Message / Persistence Failure Dependency | Partial | Regression Tested for absence on persistence failure | Partial Parity | C# persistence failure emits no success message. Success payload bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SECONDARY_SHOW_DECOMPOSABLE` | `SmSecondaryShowDecomposable` | Packet / Persistence Failure Dependency | Partial | Regression Tested for absence on persistence failure | Partial Parity | C# persistence failure emits no secondary-show packet. Java failure behavior remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` / inventory add packets | `SmInventoryUpdateItem` / `SmInventoryAddItem` | Packet / Persistence Failure Dependency | Partial | Regression Tested for absence on persistence failure | Partial Parity | C# persistence failure emits no source or reward inventory packets. Full byte parity remains unverified. |

## Remaining Risks

- Java runtime behavior under persistence/DAO failure remains unverified and may differ from the C# deferred-mutation transaction-safety boundary.
- Missing source item, missing selectable data, missing reward template, and inventory-full behavior still need coverage.
- The selection tests invoke a private handler reflectively; packet-factory dispatch for real client frames remains unverified.
- Full packet byte parity, opcode/frame/crypto, broadcast fanout, socket visibility, threading/date-time precision, serialization side effects, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 1 C# runtime regression slice for selectable decompose persistence failure
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 19 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Add compact missing-source/data negative coverage for selectable decompose selection: missing source object id and/or non-selectable source item should return without repository calls, packets, or runtime mutation. Keep packet-factory dispatch as a separate follow-up unless tiny.

Suggested scope:

- Reuse the selectable fixture and repository call counter.
- Invoke selection with a missing object id and assert no repository call, no packets, and unchanged inventory.
- If still tiny, invoke selection against a non-selectable source item and assert the same no-op behavior.
- Leave packet-factory dispatch and Java runtime DB failure comparison as separate units.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Missing-source/data negative tests | same test file | Yes, if exclusive | Best next unit. |
| Packet-factory dispatch audit | packet factory/tests read-only or separate packet test | Yes if not editing fixture | Useful to reduce reflection seam risk. |
| Java DB failure behavior research | Java source/read-only | Yes | Helps classify C# deferred-mutation difference. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Broad handler edits unless missing-data tests expose a mismatch.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Keep the next unit narrow: selectable missing-source/data no-op paths.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
