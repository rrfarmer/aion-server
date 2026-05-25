# Phase 6OC Completion Handoff - Decompose Cube Update Field Parity

Date: May 25, 2026
Unit of Work: UOW-881
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-881] Assert decompose cube update fields`)

## Status

Phase 6 is still in progress. This unit strengthened the C# decompose source-delete packet evidence by decoding `SmCubeUpdate` payloads, and it fixed the stale cube item count exposed by that assertion.

Java `Storage.delete` removes the item before `ItemPacketService.sendItemDeletePacket` sends `SM_DELETE_ITEM` and `SM_CUBE_UPDATE`. C# now publishes the source mutation to `player.InventoryItems` before sending the source mutation packets, so `SmCubeUpdate.CubeSize(player)` serializes post-delete cube counts.

No Java runtime artifact exists yet; parity remains source-reviewed plus C# regression evidence only.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OC-Completion.md`

## What Changed

- Added `AssertCubeUpdatePayload` to decode `SmCubeUpdate` payload fields in decompose tests.
- Updated delete-source tests to assert:
  - action `0`
  - cube storage ordinal `0`
  - post-delete item count `0`
  - zero NPC/quest/item expansion fields for the fixture player
- Fixed `ApplySourceItemMutationAsync` so source deletion/decrement is published to `player.InventoryItems` before packet emission.
- Preserved existing packet order:
  - delete path: `SmDeleteItem`, then `SmCubeUpdate`
  - decrement path: `SmInventoryUpdateItem`

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
| `HandleUseItemAsync_DecomposeDeletesLastSourceAndAddsReward` | Direct scheduled decompose delete emits decoded post-delete cube update with `itemsCount = 0`. | Java `Storage.delete` removes before `ItemPacketService.sendItemDeletePacket`; source reviewed, no runtime artifact yet. |
| `HandleSelectDecomposableAsync_SelectableRewardDeletesSingleCountSource` | Selectable source delete emits decoded post-delete cube update before secondary clear/reward add. | Java `CM_SELECT_DECOMPOSABLE.runImpl` plus storage/item packet source reviewed; no runtime artifact yet. |
| `RunAsync_EncryptedUseItemFrameDeletesLastSourceAndAddsReward` | Encrypted socket path observes decoded post-delete cube update after source delete. | C# socket evidence aligned to reviewed Java source; no Java encrypted byte comparison yet. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.Storage.delete` | `Aion.GameServer.Network.Aion.GameServerConnection.ApplySourceItemMutationAsync` | Storage Mutation | Partial | Regression Tested | Partial Parity | C# now publishes source deletion/decrement to `player.InventoryItems` before source mutation packet emission, matching Java's storage-first mutation order. Java runtime artifact comparison remains missing. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket` | `Aion.GameServer.Network.Aion.GameServerConnection.ApplySourceItemMutationAsync` | Service / Packet Side Effect | Partial | Regression Tested | Partial Parity | Delete path now sends `SmDeleteItem` then decoded `SmCubeUpdate` with action `0`, cube storage ordinal `0`, and post-delete `itemsCount = 0` in tested decompose scenarios. Runtime Java comparison and byte-level parity remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server Packet | Partial | Regression Tested | Partial Parity | Tests now decode action, action value, item count, and expansion fields after decompose source delete. Java runtime bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / `GameServerConnection.HandleSelectDecomposableAsync` | Client Packet Handler | Partial | Regression Tested | Partial Parity | Selectable delete path now proves decoded cube update fields after `SmDeleteItem` and before secondary clear/reward add. Java runtime capture remains missing. |
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDecomposeUseItemAsync` / scheduled completion | Scheduled Item Action | Partial | Regression Tested | Partial Parity | Normal scheduled delete path now proves decoded cube update fields in both direct handler and encrypted socket paths. Java scheduler/runtime comparison remains unverified. |

## Remaining Risks

- Java runtime capture remains unimplemented and local Java 25/Maven validation remains unavailable.
- `SmCubeUpdate` decoded fields are verified for decompose source-delete paths only; other cube delete callers may still need field-level checks.
- Java `Storage.delete` also tracks deleted items, persistent state, quest removal, and logging; these side effects remain only partially modeled.
- The source mutation publish timing is now closer to Java, but broader decompose reward-add byte parity and generated object-id comparison still need Java artifacts.
- Threading/scheduler ordering remains unverified against Java runtime output.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 1 C# source-mutation timing fix plus 3 decoded `SmCubeUpdate` regression assertions
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 7 blocked/not-started categories, including Java loopback proof validation, Java runtime artifact generation, C# artifact comparison tests, broader cube-update caller field coverage, unencrypted body byte capture, encrypted frame byte capture, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Return to Java runtime capture when Java 25/Maven tooling is available by compiling/running `LoopbackCaptureProof`.

If staying in C# while tooling is blocked, choose another isolated decompose comparison-readiness task. Good candidates:

- Decode/field-assert reward add packets in the existing decompose success tests.
- Add a small C# JSON projection helper for decompose packet observations, matching `docs/Phase-6-Decompose-Java-Capture-Contract.md`, without claiming Java parity.
- Add source-reviewed decoded fields for `SmSecondaryShowDecomposable` in the encrypted socket selectable path if coverage is still type-only there.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java proof validation/fix | `LoopbackCaptureProof.java` | No | Requires Java 25/Maven and sequential compile/run feedback. |
| C# reward packet decoded assertions | decompose test file | No | Same shared test fixture; do sequentially. |
| C# packet observation JSON projection helper | tests/helper file plus docs | Maybe | Safe only if it avoids shared production handler edits. |
| Live-server capture runbook | docs only | Yes | Useful fallback while Java proof remains tooling-blocked. |

## Do Not Parallelize

- `GameServerConnection.cs` with any other item-use handler edit.
- `GameServerConnectionInventoryExpansionUseItemTests.cs` with other decompose test edits.
- `LoopbackCaptureProof.java` with another Java network harness edit.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-Java-Decompose-Harness-Feasibility.md`, `docs/Phase-6-Decompose-Java-Capture-Contract.md`, `docs/Phase-6-Java-Loopback-Capture-Design.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Prefer Java proof validation if Java 25/Maven tooling is available; otherwise choose an isolated C# comparison-readiness task.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
