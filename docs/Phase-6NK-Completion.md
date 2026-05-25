# Phase 6NK Completion Handoff - Normal Decompose Scheduled Success

Date: May 24, 2026
Unit of Work: UOW-863
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-863] Test normal decompose scheduled success`)

## Status

Phase 6 is still in progress. This unit added runtime regression coverage for the normal non-selectable decompose completion path after the Java-style 3000 ms item-use delay.

The test verifies the C# scheduled path consumes one source item, adds the deterministic reward, emits the expected packet sequence, and clears `UsingItemObjectId`. Java runtime output was not executed, so parity remains partial.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NK-Completion.md`

## What Changed

- Added `HandleUseItemAsync_DecomposeCompletesAndAddsReward`.
- Set the connection active player before scheduled completion so `SchedulePendingItemUseAsync` does not skip the delayed action.
- Updated `WaitUntilAsync` to accept an optional timeout for delayed item-use tests.
- The new test verifies:
  - start usage animation for item `100` with `time=3000` and `end=0`
  - pending `UsingItemObjectId` is set during the delay and cleared after completion
  - source item `100 x2` becomes `100 x1`
  - reward item `200 x1` is added
  - success message, source inventory update, final success animation `end=1`, and reward add packet are emitted in order

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests"
```

Result: passed, 22 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1438 tests.

New test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_DecomposeCompletesAndAddsReward` | C# scheduled normal decompose success consumes source, adds reward, emits ordered packets, and clears pending item-use state. | Java `DecomposeAction.act` source reviewed; Java runtime, packet bytes, and live-client behavior not compared. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `GameServerConnection.HandleDecomposeUseItemAsync` / `CompleteDecomposeUseItemAsync` | Item Action / Scheduled Runtime Handler | Partial | Regression Tested | Partial Parity | Normal scheduled success is covered for source decrement, reward add, success message, final animation `end=1`, and pending cleanup. Inventory-full, source-delete completion, Java runtime comparison, and live-client behavior remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `SmItemUsageAnimation` | Packet | Partial | Regression Tested for type/order and selected fields | Partial Parity | Start and final success animation fields are asserted. Full byte parity, broadcast fanout, opcode/frame/crypto, and socket visibility remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage.DecomposeItemSucceed` | Packet | Partial | Regression Tested for type/order | Needs Verification | Success message packet is emitted in order, but localized payload bytes and Java output are not compared. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseByObjectId` | `ApplySourceItemMutationAsync` / `SmInventoryUpdateItem.DecreaseItemUse` | Inventory Mutation / Source Consume Dependency | Partial | Regression Tested | Partial Parity | Source stack decrement is covered. Source delete, persistence rollback, Java SQL side effects, and Java runtime output remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService` / `ItemPacketService.ItemAddType.DECOMPOSABLE` / `ItemUpdateType.INC_ITEM_COLLECT` | `InventoryAddService.CreateAddItemPlan` / `SmInventoryAddItem.CreateDecomposable` | Service / Inventory Reward Dependency | Partial | Regression Tested | Partial Parity | Reward add is covered for a deterministic new row. Inventory-full behavior, random reward ranges, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | `DecomposableItemTable` / fixture static data | Static Data / DTO Dependency | Partial | Regression Tested with deterministic fixture data | Partial Parity | Fixture covers item `100` -> reward `200 x1`. Broader chance/level/range behavior and Java parser comparison remain unverified. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `Aion.GameServer.Utils.ThreadPoolManager` / `SchedulePendingItemUseAsync` | Scheduler / Threading Dependency | Partial | Regression Tested through delayed completion | Needs Verification | C# delayed completion runs when active-player state matches. Java executor/cancellation semantics, threading races, and date/time precision remain unverified. |

## Remaining Risks

- Inventory-full behavior for normal and selectable decompose remains untested.
- Normal decompose source-delete scheduled completion needs explicit coverage.
- Java runtime behavior under persistence/DAO failure remains unverified and may differ from the C# deferred-mutation transaction-safety boundary.
- Scheduled completion depends on C# active-player reference checks; Java controller/task lifecycle was source-reviewed but not runtime-compared.
- Full packet byte parity, opcode/frame/crypto, broadcast fanout, socket visibility, threading/date-time precision, serialization side effects, random reward selection, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 1 C# runtime regression slice for normal decompose scheduled successful completion
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 15 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Add inventory-full decompose coverage next, preferably at the service or connection-fixture level with a deterministic full cube, and document whether normal and selectable reward planning fail before source consumption, persistence, and packet emission.

Fallback compact unit:

- Add normal decompose source-delete scheduled completion using a source count of `1`.
- Assert final source removal, delete/update packet behavior, reward add, success message, final animation `end=1`, and pending cleanup.
- Keep inventory-full and Java runtime comparison documented as gaps if this fallback is chosen.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Inventory-full decompose coverage | likely service tests or shared item-use fixture | Maybe | Best next parity gap; avoid parallel writers if using the shared fixture. |
| Normal source-delete scheduled completion | same item-use fixture | Yes, if exclusive | Compact fallback if full-cube setup expands too much. |
| Encrypted socket-loop dispatch audit | connection/socket tests read-only first | Yes as analysis | Could become larger than a narrow unit. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Inventory-full setup and source-delete completion if both need the same fixture helper edits.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Try inventory-full decompose coverage first; fall back to normal source-delete scheduled completion if setup is too broad.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
