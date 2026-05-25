# Phase 6NQ Completion Handoff - Encrypted Normal Decompose Socket Loop

Date: May 25, 2026
Unit of Work: UOW-869
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-869] Test encrypted normal decompose socket loop`)

## Status

Phase 6 is still in progress. This unit added encrypted socket/read-loop regression coverage for normal `CM_USE_ITEM` decompose scheduling and completion.

The new test starts `GameServerConnection.RunAsync`, observes the initial `SmKey`, writes an encrypted client frame for Java opcode `CM_USE_ITEM`, waits through the real 3000ms C# scheduler, and verifies normal decompose completion from encrypted dispatch through source decrement, reward add, success message, final usage animation, and pending-use cleanup. Java runtime output was not executed, so parity remains partial.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NQ-Completion.md`

## What Changed

- Added `RunAsync_EncryptedUseItemFrameSchedulesAndCompletesDecompose`.
- Reused the UOW-868 socket fixture support for uninitialized crypt startup, server-frame reads, encrypted client-frame writes, and deterministic client payload encryption.
- Verified normal decompose over the socket loop:
  - `SmKey` is sent first
  - encrypted opcode `37` is decrypted and dispatched as `CmUseItem`
  - normal decompose schedules and completes through the real 3000ms C# scheduler
  - source item `100 x2` decrements to `100 x1`
  - reward `200 x1` is added
  - pending item-use is cleared
  - packet observer sequence is `SmKey`, start animation, success message, source update, final animation, reward add

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 28 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1444 tests.

New test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.RunAsync_EncryptedUseItemFrameSchedulesAndCompletesDecompose` | C# game connection loop sends `SmKey`, reads/decrypts encrypted opcode `37`, dispatches normal decompose, waits through scheduler completion, decrements source, adds reward, and emits scheduled success packets. | Java `AionConnection.initialized`, `AionPacketHandler`, `GameCrypt`, `AionClientPacketFactory`, `CM_USE_ITEM`, and `DecomposeAction.act` source reviewed; Java runtime bytes and live-client behavior not compared. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.AionConnection.initialized` | `GameServerConnection.RunAsync` / `SmKey` | Connection Lifecycle / Handshake | Partial | Regression Tested | Partial Parity | Test observes `SmKey` before encrypted client packet processing. Java runtime bytes and live key negotiation remain unverified. |
| `com.aionemu.gameserver.network.aion.AionPacketHandler` / `GameCrypt.decrypt` | `GameServerConnection.ReadPacketAsync` / `GameCrypt.DecryptClientPayload` | Frame Reader / Crypto | Partial | Regression Tested | Partial Parity | Test sends encrypted opcode `37` through a real TCP stream. Java crypt runtime, multi-packet key evolution, and corrupt-packet behavior remain unverified. |
| `com.aionemu.gameserver.network.aion.AionClientPacketFactory` | `GameClientPacketFactory.TryCreatePacket` | Packet Factory / Dispatch | Partial | Regression Tested through socket loop | Partial Parity | Encoded opcode `37` dispatches as `CmUseItem` in `InGame` state. Broader opcode-table and state-gating parity remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | `GameServerConnection.HandleUseItemAsync` via `RunAsync` | Client Packet Handler | Partial | Regression Tested through socket loop | Partial Parity | Normal decompose item-use now runs through `RunAsync -> ReadPacketAsync -> ProcessPacketAsync`. Java runtime comparison and full packet bytes remain unverified. |
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `GameServerConnection.HandleDecomposeUseItemAsync` / `CompleteDecomposeUseItemAsync` | Item Action / Scheduled Runtime Handler | Partial | Regression Tested through socket loop | Partial Parity | Encrypted normal `CM_USE_ITEM` schedules and completes through the real C# scheduler, decrementing source and adding reward. Java executor timing, observer lifecycle, and runtime output remain unverified. |
| `com.aionemu.gameserver.controllers.CreatureController.addTask(TaskId.ITEM_USE)` / `ThreadPoolManager.schedule` | `ThreadPoolManager` / `SchedulePendingItemUseAsync` | Scheduler / Threading Dependency | Partial | Regression Tested through socket loop | Needs Verification | Test waits for scheduled completion and pending cleanup. Java task cancellation/threading/date-time precision remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseByObjectId` | `ApplySourceItemMutationAsync` / `SmInventoryUpdateItem.DecreaseItemUse` | Inventory Mutation / Source Consume Dependency | Partial | Regression Tested through socket loop | Partial Parity | Source stack `100 x2` decrements to `100 x1`. Count-1 delete variant has direct-handler coverage but not socket-loop coverage. Java DB/runtime output remains unverified. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` / `ItemUpdatePredicate` | `InventoryAddService.CreateAddItemPlan` / `SmInventoryAddItem.CreateDecomposable` | Service / Reward Dependency | Partial | Regression Tested through socket loop | Partial Parity | Reward `200 x1` is added after scheduled completion. Random ranges, SQL/autocommit behavior, inventory-full retry states, and Java runtime side effects remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` / `SM_SYSTEM_MESSAGE` / inventory packets | `SmItemUsageAnimation` / `SmSystemMessage.DecomposeItemSucceed` / inventory packets | Packet / Scheduled Completion | Partial | Regression Tested for observer type/order and selected fields | Partial Parity | Observer sequence after `SmKey` is asserted. Encrypted server bytes, broadcast visibility, and Java packet byte parity remain unverified. |

## Remaining Risks

- Count-1 source-delete normal decompose has direct-handler coverage but not socket-loop coverage.
- Java runtime behavior under persistence/DAO failure remains unverified and may differ from the C# deferred-mutation transaction-safety boundary.
- Client encryption helper mirrors the C# decrypt implementation; Java crypt was source-reviewed but not runtime-compared.
- Scheduler coverage is deterministic in C# tests but Java executor timing, observer cancellation, and threading edge cases remain unverified.
- Full packet byte parity, opcode/frame/crypto breadth, broadcast fanout, socket visibility, serialization side effects, random reward selection, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 9
- Total artifacts ported: 1 C# socket-loop regression slice for encrypted normal decompose scheduled completion
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked artifacts: 9 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Add the count-1 source-delete variant through the encrypted normal `CM_USE_ITEM` socket loop if it remains compact.

Suggested scope:

- Send encrypted opcode `37` through `RunAsync` with source item count `1`.
- Use `includeThreadPoolManager: true`.
- Wait through scheduler completion.
- Assert `SmDeleteItem` use-delete type `0x17`, reward add, success message, final usage animation `end=1`, and pending cleanup.
- If this feels redundant, pivot to a focused Java-runtime comparison plan for decompose packet/order and crypt bytes.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Count-1 normal `CM_USE_ITEM` socket-loop test | `GameServerConnectionInventoryExpansionUseItemTests.cs` | No | Shared fixture ownership; keep single-writer. |
| Java runtime comparison plan | docs plus Java source audit | Yes as analysis | Good pivot after socket-loop variants. |
| Corrupt encrypted packet audit | `GameServerConnection.cs` and socket smoke tests | Yes as read-only analysis | Separate from decompose behavior unless a write is selected. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Fixture crypt/key helpers alongside source-delete socket-loop edits unless one agent owns the whole fixture.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Start with count-1 normal decompose socket-loop feasibility.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
