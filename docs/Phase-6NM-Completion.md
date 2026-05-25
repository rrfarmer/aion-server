# Phase 6NM Completion Handoff - Normal Decompose Source Delete Success

Date: May 24, 2026
Unit of Work: UOW-865
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-865] Test normal decompose source delete success`)

## Status

Phase 6 is still in progress. This unit added scheduled normal decompose coverage for the count-1 source path where source consumption deletes the inventory row.

The test verifies C# delayed completion removes the source, emits `SmDeleteItem` with the Java use-delete type, adds the deterministic reward, sends the success sequence, and clears pending item-use state. Java runtime output was not executed, so parity remains partial.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NM-Completion.md`

## What Changed

- Added `HandleUseItemAsync_DecomposeDeletesLastSourceAndAddsReward`.
- Added `AssertDeleteItemPayload` helper to assert serialized source delete object id and delete type.
- The new test verifies:
  - start usage animation for item `100` with `time=3000` and `end=0`
  - pending `UsingItemObjectId` is set during the delay and cleared after completion
  - source item object id `5001` is removed from runtime inventory
  - reward item `200 x1` is added
  - success message, `SmDeleteItem` with delete type `0x17`, final success animation `end=1`, and reward add packet are emitted in order

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests"
```

Result: passed, 24 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1440 tests.

New test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_DecomposeDeletesLastSourceAndAddsReward` | C# scheduled normal decompose count-1 success deletes source, adds reward, emits ordered packets, and clears pending item-use state. | Java `DecomposeAction.act` source reviewed; Java runtime, packet bytes, and live-client behavior not compared. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `GameServerConnection.HandleDecomposeUseItemAsync` / `CompleteDecomposeUseItemAsync` | Item Action / Scheduled Runtime Handler | Partial | Regression Tested | Partial Parity | Count-1 normal scheduled success is covered for source deletion, reward add, success message, final animation `end=1`, and pending cleanup. Selectable inventory-full, special-cube branch, Java runtime comparison, and live-client behavior remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseByObjectId` | `ApplySourceItemMutationAsync` with deleted source object id | Inventory Mutation / Source Consume Dependency | Partial | Regression Tested | Partial Parity | Source deletion is covered for count `1`. Java SQL side effects and runtime output remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` | `SmDeleteItem` | Packet | Partial | Regression Tested for object id and delete type | Partial Parity | Test asserts source object id `5001` and delete type `0x17`. Full Java packet byte parity, opcode/frame/crypto, and socket visibility remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `SmItemUsageAnimation` | Packet | Partial | Regression Tested for selected fields and order | Partial Parity | Start and final success animations are asserted around source delete. Broadcast fanout and Java byte comparison remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage.DecomposeItemSucceed` | Packet | Partial | Regression Tested for packet type/order | Needs Verification | Success message is emitted before source delete. Localized payload bytes and Java output are not compared. |
| `com.aionemu.gameserver.services.item.ItemService` / `ItemPacketService.ItemAddType.DECOMPOSABLE` / `ItemUpdateType.INC_ITEM_COLLECT` | `InventoryAddService.CreateAddItemPlan` / `SmInventoryAddItem.CreateDecomposable` | Service / Inventory Reward Dependency | Partial | Regression Tested | Partial Parity | Reward add is covered after source delete. Inventory-full, random reward ranges, Java DB/runtime comparison, and live-client behavior remain unverified. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `ThreadPoolManager` / `SchedulePendingItemUseAsync` | Scheduler / Threading Dependency | Partial | Regression Tested through delayed completion | Needs Verification | C# delayed source-delete completion runs when active-player state matches. Java executor/cancellation semantics, threading races, and date/time precision remain unverified. |

## Remaining Risks

- Selectable decompose inventory-full behavior remains untested at the connection level.
- Special-cube inventory-full behavior has service coverage but not connection-path packet/no-mutation coverage.
- Java runtime behavior under persistence/DAO failure remains unverified and may differ from the C# deferred-mutation transaction-safety boundary.
- Scheduled completion depends on C# active-player reference checks; Java controller/task lifecycle was source-reviewed but not runtime-compared.
- Full packet byte parity, opcode/frame/crypto, broadcast fanout, socket visibility, threading/date-time precision, serialization side effects, random reward selection, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 1 C# runtime regression slice for normal decompose source-delete scheduled success
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 13 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Add selectable decompose inventory-full connection coverage or special-cube inventory-full connection coverage.

Suggested approach:

- Prefer selectable inventory-full first if the existing fixture can fill the base cube while preserving selectable source behavior.
- If selectable setup is awkward, extend fixture static data with a special-cube reward/filler pair and assert special-cube full sends message id `1300447`, does not call persistence, and does not mutate runtime inventory.
- Keep Java runtime comparison, encrypted socket-loop dispatch, and live-client validation documented as gaps.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Selectable inventory-full connection behavior | same item-use fixture | Yes, if exclusive | Best next decompose edge if fixture setup is compact. |
| Special-cube connection inventory-full | fixture static data plus item-use tests | No with selectable fixture edits | May require fixture static-data expansion. |
| Encrypted socket-loop dispatch audit | connection/socket tests read-only first | Yes as analysis | Could become larger than a narrow unit. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Special-cube fixture static-data edits alongside selectable inventory-full fixture assertions.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Add selectable inventory-full or special-cube connection coverage first unless a newer handoff supersedes this.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
