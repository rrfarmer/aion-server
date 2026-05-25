# Phase 6NP Completion Handoff - Encrypted Selectable Decompose Socket Loop

Date: May 25, 2026
Unit of Work: UOW-868
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-868] Test encrypted selectable decompose socket loop`)

## Status

Phase 6 is still in progress. This unit added encrypted socket/read-loop regression coverage for selectable decompose dispatch.

The new test starts `GameServerConnection.RunAsync`, observes the initial `SmKey`, writes an encrypted client frame for Java opcode `CM_SELECT_DECOMPOSABLE`, and verifies that C# decrypts, parses, dispatches, mutates inventory, and emits the expected selectable success packet sequence. Java runtime output was not executed, so parity remains partial.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NP-Completion.md`

## What Changed

- Added `RunAsync_EncryptedSelectDecomposableFrameDispatchesSelection`.
- Added fixture support for:
  - starting `RunAsync` with an uninitialized `GameCrypt`
  - reading the initial server frame from the paired test client
  - writing encrypted client frames to the real `NetworkStream`
- Added deterministic client-frame encryption in the test, mirroring the C# `GameEncryptionKeyPair.DecryptClient` algorithm for key `0x01020304`.
- Verified socket-loop dispatch for selectable decompose:
  - `SmKey` is sent first
  - encrypted opcode `236` is decrypted and dispatched
  - source item `101 x2` decrements to `101 x1`
  - reward `202 x3` is added
  - packet observer sequence is `SmKey`, usage animation, success message, source update, secondary decomposable clear, reward add

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 27 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1443 tests.

New test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.RunAsync_EncryptedSelectDecomposableFrameDispatchesSelection` | C# game connection loop sends `SmKey`, reads/decrypts an encrypted selectable-decompose client frame, dispatches opcode `236`, decrements source, adds reward, and emits selectable success packets. | Java `AionConnection.initialized`, `AionPacketHandler`, `GameCrypt`, `AionClientPacketFactory`, and `CM_SELECT_DECOMPOSABLE` source reviewed; Java runtime bytes and live-client behavior not compared. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.AionConnection.initialized` | `GameServerConnection.RunAsync` / `SmKey` | Connection Lifecycle / Handshake | Partial | Regression Tested | Partial Parity | Test observes `SmKey` before client packet processing. Java runtime bytes and live key negotiation remain unverified. |
| `com.aionemu.gameserver.network.aion.AionPacketHandler` / `GameCrypt.decrypt` | `GameServerConnection.ReadPacketAsync` / `GameCrypt.DecryptClientPayload` | Frame Reader / Crypto | Partial | Regression Tested | Partial Parity | Test sends a deterministic encrypted client frame through a real TCP stream. Java crypt runtime and corrupt-packet behavior remain unverified. |
| `com.aionemu.gameserver.network.aion.AionClientPacketFactory` | `GameClientPacketFactory.TryCreatePacket` | Packet Factory / Dispatch | Partial | Regression Tested through socket loop | Partial Parity | Encoded opcode `236` dispatches as `CmSelectDecomposable` in `InGame` state. Broader state gating and opcode-table parity remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `GameServerConnection.HandleSelectDecomposableAsync` via `RunAsync` | Client Packet Handler | Partial | Regression Tested through socket loop | Partial Parity | Selectable decompose now runs through `RunAsync -> ReadPacketAsync -> ProcessPacketAsync`. Java runtime comparison and full packet bytes remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseByObjectId` | `ApplySourceItemMutationAsync` / `SmInventoryUpdateItem.DecreaseItemUse` | Inventory Mutation / Source Consume Dependency | Partial | Regression Tested through socket loop | Partial Parity | Source stack decrements from encrypted dispatch. Java DB/runtime output remains unverified. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` / `ItemUpdatePredicate` | `InventoryAddService.CreateAddItemPlan` / `SmInventoryAddItem.CreateDecomposable` | Service / Reward Dependency | Partial | Regression Tested through socket loop | Partial Parity | Reward `202 x3` is added after encrypted dispatch. Random ranges, SQL/autocommit behavior, and Java runtime side effects remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` / `SM_SYSTEM_MESSAGE` / `SM_SECONDARY_SHOW_DECOMPOSABLE` / inventory packets | `SmItemUsageAnimation` / `SmSystemMessage.UncompressCompressedItemSucceeded` / `SmSecondaryShowDecomposable` / inventory packets | Packet / Selection Completion | Partial | Regression Tested for observer type/order and selected fields | Partial Parity | Observer sequence after `SmKey` is asserted. Encrypted server bytes, broadcast visibility, and Java packet byte parity remain unverified. |

## Remaining Risks

- Normal decompose `CM_USE_ITEM` still lacks socket-loop coverage through scheduler completion.
- Java runtime behavior under persistence/DAO failure remains unverified and may differ from the C# deferred-mutation transaction-safety boundary.
- Client encryption helper mirrors the C# decrypt implementation; Java crypt was source-reviewed but not runtime-compared.
- Decompose coverage is still mostly connection/test-fixture based rather than live Java-vs-C# runtime comparison.
- Full packet byte parity, opcode/frame/crypto breadth, broadcast fanout, socket visibility, threading/date-time precision, serialization side effects, random reward selection, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 1 C# socket-loop regression slice for encrypted selectable decompose dispatch
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 10 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Add socket-loop coverage for normal `CM_USE_ITEM` decompose with scheduler completion if the current fixture can keep the timing stable.

Suggested scope:

- Send encrypted opcode `37` through `RunAsync`.
- Use `includeThreadPoolManager: true`.
- Wait for the initial usage animation and scheduled completion.
- Assert source decrement or delete, reward add, success message, final usage animation `end=1`, and pending cleanup.
- If the 3000ms scheduler timing is flaky, reduce the unit to an initial scheduling socket-loop test, or create the Java-runtime comparison plan for decompose packet/order behavior.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Normal `CM_USE_ITEM` socket-loop test | `GameServerConnectionInventoryExpansionUseItemTests.cs` | No | Shared fixture ownership; keep single-writer. |
| Java runtime comparison plan | docs plus Java source audit | Yes as analysis | Useful if socket scheduler timing becomes too broad. |
| Corrupt encrypted packet audit | `GameServerConnection.cs` and socket smoke tests | Yes as read-only analysis | Separate from decompose behavior unless a write is selected. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Fixture crypt/key helpers alongside normal scheduler socket-loop edits unless one agent owns the whole fixture.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Start with normal `CM_USE_ITEM` socket-loop feasibility, especially scheduler timing.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
