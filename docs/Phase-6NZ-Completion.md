# Phase 6NZ Completion Handoff - Decompose Delete Cube Update

Date: May 25, 2026
Unit of Work: UOW-878
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-878] Send cube update after decompose delete`)

## Status

Phase 6 is still in progress. This unit fixed the focused C# packet-order gap found while defining the Java selectable-decompose capture contract.

Java `ItemPacketService.sendItemDeletePacket` sends `SM_DELETE_ITEM` followed by `SM_CUBE_UPDATE.cubeSize(storageType, player)` for cube deletes. The C# shared decompose source-delete helper now sends `SmCubeUpdate.CubeSize(player)` immediately after `SmDeleteItem`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NZ-Completion.md`

## What Changed

- Threaded `Player` into `ApplySourceItemMutationAsync`.
- Sent `SmCubeUpdate.CubeSize(player)` immediately after `SmDeleteItem` when a source item is deleted.
- Updated regression expectations for:
  - direct normal decompose source delete
  - direct selectable decompose source delete
  - encrypted socket normal decompose source delete
- Kept decrement paths unchanged.

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 29 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1450 tests.

Updated tests:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `HandleUseItemAsync_DecomposeDeletesLastSourceAndAddsReward` | Direct scheduled decompose source delete emits `SmDeleteItem`, then `SmCubeUpdate`, then final animation and reward add. | Java `DecomposeAction.postValidate` plus `ItemPacketService.sendItemDeletePacket` source reviewed; no Java runtime artifact yet. |
| `HandleSelectDecomposableAsync_SelectableRewardDeletesSingleCountSource` | Selectable source delete emits `SmDeleteItem`, then `SmCubeUpdate`, before secondary clear and reward add. | Java `CM_SELECT_DECOMPOSABLE.runImpl` plus `ItemPacketService.sendItemDeletePacket` source reviewed; no Java runtime artifact yet. |
| `RunAsync_EncryptedUseItemFrameDeletesLastSourceAndAddsReward` | Encrypted socket path observes `SmCubeUpdate` after `SmDeleteItem` during scheduled decompose completion. | Source-derived C# socket evidence; no Java runtime artifact or byte comparison yet. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket` | `Aion.GameServer.Network.Aion.GameServerConnection.ApplySourceItemMutationAsync` | Service / Packet Side Effect | Partial | Regression Tested | Partial Parity | C# now sends `SmDeleteItem` followed by `SmCubeUpdate.CubeSize(player)` for source item deletes through normal/selectable decompose paths, matching Java source order for cube deletes. Java runtime artifact comparison and byte-level packet parity remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server Packet | Partial | Regression Tested through handler paths | Partial Parity | Existing C# packet support is reused after delete. Tests assert packet presence/order, not decoded cube-size fields in this unit. Java runtime bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / `GameServerConnection.HandleSelectDecomposableAsync` | Client Packet Handler | Partial | Regression Tested | Partial Parity | Selectable source-delete path now includes cube update after delete before secondary decomposable clear and reward add. Java runtime capture remains missing. |
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDecomposeUseItemAsync` / scheduled completion | Scheduled Item Action | Partial | Regression Tested | Partial Parity | Normal decompose source-delete path now includes cube update after delete before final usage animation and reward add. Java scheduler timing and runtime comparison remain unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage.delete` | `Aion.GameServer.Network.Aion.GameServerConnection.ApplySourceItemMutationAsync` | Storage Mutation | Partial | Regression Tested through decompose paths | Partial Parity | C# delete branch now models Java packet side effect for the shared decompose source mutation helper. Persistence, quest callback, and broader storage delete callers remain only partially covered. |

## Remaining Risks

- Java runtime capture remains unimplemented.
- `SmCubeUpdate` decoded field parity after delete was not asserted in this unit; tests cover type/order.
- Broader cube-update side effects for other cube delete callers may still be missing if they do not use `ApplySourceItemMutationAsync`.
- Java `Storage.delete` also marks persistent state, tracks deleted items, logs, and invokes quest removal; those side effects remain only partially modeled.
- Level 3/4 Java byte capture remains deferred until Java connection/crypt state is controlled.
- Threading and scheduler ordering remain unverified against Java runtime output.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 1 C# packet-side-effect fix plus 3 focused regression assertions
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, C# artifact comparison tests, decoded cube-update field comparison, deterministic Java static-data fixture, unencrypted byte capture, and encrypted frame capture
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Return to Java runtime comparison implementation planning by adding Java loopback socket capture design notes for the `JD-SEL-DEC-001` / `JD-SEL-DEL-001` contract.

If staying in C# cleanup first, add decoded `SmCubeUpdate` field assertions for the decompose delete tests to strengthen packet-field evidence.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java loopback socket capture design notes | docs only / read-only Java | Yes as analysis | Best next step toward actual Java runtime artifacts. |
| Decoded `SmCubeUpdate` assertions | `GameServerConnectionInventoryExpansionUseItemTests.cs` | No | Same shared test fixture file; do sequentially. |
| Live-server capture runbook | docs only | Yes as analysis | Useful fallback after loopback design. |
| Java artifact output folder skeleton | docs/parity-artifacts | Maybe | Wait until capture method is chosen. |

## Do Not Parallelize

- `GameServerConnection.cs` with other item-use handler edits.
- `GameServerConnectionInventoryExpansionUseItemTests.cs` with any other decompose test edits.
- Progress and handoff docs.
- Java runtime harness implementation until loopback/live-server capture path is chosen.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-Java-Decompose-Harness-Feasibility.md`, `docs/Phase-6-Decompose-Java-Capture-Contract.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Prefer Java loopback socket capture design notes next.
6. Run focused and full tests for any code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
