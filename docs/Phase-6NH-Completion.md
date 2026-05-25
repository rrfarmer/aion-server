# Phase 6NH Completion Handoff - Selectable Decompose Dispatch

Date: May 24, 2026
Unit of Work: UOW-860
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-860] Test selectable decompose dispatch`)

## Status

Phase 6 is still in progress. This unit added connection dispatch-path coverage for `CM_SELECT_DECOMPOSABLE` / opcode `236`.

The test verifies encoded client payload parsing through `GameClientPacketFactory` and `GameServerConnection.ProcessPacketAsync`, then observes the valid selectable selection behavior. It still bypasses encrypted socket read-loop and active-player lifecycle setup by using reflection for the protected processing seam.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NH-Completion.md`

## What Changed

- Added `ProcessPacketAsync_SelectDecomposableDispatchesSelection`.
- Added test helpers for:
  - encoded client payload construction
  - protected `ProcessPacketAsync` invocation
  - setting active player and `InGame` state for dispatch-path testing
- The new test verifies:
  - encoded opcode `236` dispatches as `CmSelectDecomposable`
  - source item `101` decrements from count `2` to `1`
  - selected reward `202` is added with count `3`
  - packet order remains default `SmItemUsageAnimation`, success `SmSystemMessage`, source update, empty `SmSecondaryShowDecomposable`, and reward add

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests"
```

Result: passed, 18 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1434 tests.

New test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_SelectDecomposableDispatchesSelection` | C# encoded opcode `236` parsing and connection dispatch reach valid selectable selection behavior. | Source-derived Java review of `CM_SELECT_DECOMPOSABLE.readImpl/runImpl`; no Java runtime comparison. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `GameServerConnection.ProcessPacketAsync` / `GameClientPacketFactory` / `HandleSelectDecomposableAsync` | Client Packet Handler / Dispatch Runtime Test | Partial | Regression Tested | Partial Parity | Encoded opcode `236` parsing and connection dispatch are covered for a valid selection. Encrypted socket loop, active-player lifecycle, Java runtime comparison, and live-client behavior remain unverified. |
| `com.aionemu.gameserver.network.aion.AionClientPacketFactory` | `GameClientPacketFactory.TryCreatePacket` | Packet Factory / Dispatch Dependency | Partial | Regression Tested through connection path | Partial Parity | Opcode registration and valid-state behavior are covered through connection processing. Malformed frame and encrypted read-loop behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService` / `ItemPacketService.ItemAddType.DECOMPOSABLE` | `InventoryAddService.CreateAddItemPlan` / `SmInventoryAddItem.CreateDecomposable` | Inventory Mutation / Reward Add Dependency | Partial | Regression Tested through dispatch path | Partial Parity | Valid dispatch selection adds reward `202 x3` as a new row. Java DB/runtime comparison remains unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseByObjectId` | `ApplySourceItemMutationAsync` / `SmInventoryUpdateItem.DecreaseItemUse` | Inventory Mutation / Source Consume Dependency | Partial | Regression Tested through dispatch path | Partial Parity | Valid dispatch selection decrements source `101`. Live Java/runtime output remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `SmItemUsageAnimation` | Packet / Selection Completion | Partial | Regression Tested through dispatch path | Partial Parity | Default selection animation payload is asserted. Broadcast fanout, write-time `usingItem` side effect, Java bytes, and encrypted socket framing remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage.UncompressCompressedItemSucceeded(...)` | System Message / Selection Completion | Partial | Regression Tested for packet type/order through dispatch path | Needs Verification | Packet type/order is covered through dispatch; payload bytes and localization remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SECONDARY_SHOW_DECOMPOSABLE` | `SmSecondaryShowDecomposable` | Packet / Selection Completion | Partial | Regression Tested through dispatch path | Partial Parity | Empty secondary-show ordering is covered through dispatch. Java-generated bytes remain unverified. |
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | fixture `DecomposableItemTable` / `DecomposeService.CreateSelectableRewardPlan` | Static Data / Reward Selection Dependency | Partial | Regression Tested through dispatch path | Partial Parity | Dispatch test selects reward `202 x3` after obtainable filtering. Broader data edge cases remain unverified. |

## Remaining Risks

- Missing reward template, equipped/location source filtering, and inventory-full behavior still need coverage.
- Dispatch coverage still bypasses the encrypted socket read loop and active-player setup lifecycle.
- Java runtime behavior under persistence/DAO failure remains unverified and may differ from the C# deferred-mutation transaction-safety boundary.
- Full packet byte parity, opcode/frame/crypto, broadcast fanout, socket visibility, threading/date-time precision, serialization side effects, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 1 C# runtime regression slice for selectable decompose packet dispatch
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 17 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Add missing reward-template no-op coverage for selectable decompose selection, or shift to the next decompose completion gap if reward-template setup becomes artificial. A compact test can use an Asmodian player selecting the existing reward candidate `203`, which has no fixture item template, and assert no repository call, packets, or inventory mutation.

Suggested scope:

- Reuse the selectable fixture and set the player race to `ASMODIANS`.
- Select the filtered list entry that maps to item `203`, which intentionally has no item template in the fixture.
- Assert repository call count remains `0`, inventory is unchanged, and no packets are emitted.
- Keep inventory-full behavior and encrypted socket-loop validation as separate units.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Missing reward-template no-op test | same item-use fixture | Yes, if exclusive | Best next small unit. |
| Encrypted socket-loop dispatch audit | connection/socket tests read-only first | Yes as analysis | Could become larger than a narrow unit. |
| Java DB failure behavior research | Java source/read-only | Yes | Helps classify C# deferred-mutation difference. |
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
5. Keep the next unit narrow: missing reward-template no-op, unless moving to a new decompose completion gap.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
