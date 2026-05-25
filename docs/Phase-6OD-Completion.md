# Phase 6OD Completion Handoff - Decompose Reward Add Field Parity

Date: May 25, 2026
Unit of Work: UOW-882
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-882] Assert decompose reward add fields`)

## Status

Phase 6 is still in progress. This unit strengthened C# decompose reward-add evidence by decoding `SmInventoryAddItem` payloads in the existing success paths instead of only asserting packet type.

Java `SM_INVENTORY_ADD_ITEM` writes the add-type mask, item count, object id, template id, localized name, full `ItemInfoBlob`, equipment slot, and cloth flag. The C# tests now assert the decompose add-type mask and deterministic reward fields, including the reward count inside the general-info blob.

No Java runtime artifact exists yet; parity remains source-reviewed plus C# regression evidence only.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OD-Completion.md`

## What Changed

- Added `AssertInventoryAddPayload` to decode `SmInventoryAddItem` payloads.
- Updated decompose reward-add assertions to verify:
  - add type `SmInventoryAddItem.Decomposable` / Java `ItemAddType.DECOMPOSABLE` mask `0x50`
  - one reward item in each packet
  - deterministic reward object id `1`
  - reward item ids `200`, `201`, or `202`
  - reward counts `1`, `2`, or `3` inside the first general-info blob entry
  - equipment slot sentinel `65535`
  - cloth flag `0`
- Applied the assertion to direct handler, process-packet, and encrypted socket decompose paths.

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
| `HandleUseItemAsync_DecomposeCompletesAndAddsReward` | Direct scheduled decompose decrement path emits decoded reward item `200`, count `1`. | Java `DecomposeAction`, `ItemService.addItem`, and `SM_INVENTORY_ADD_ITEM` source reviewed; no runtime artifact yet. |
| `HandleUseItemAsync_DecomposeDeletesLastSourceAndAddsReward` | Source-delete scheduled decompose emits decoded reward item `200`, count `1`. | Source reviewed; no Java runtime artifact yet. |
| `HandleSelectDecomposableAsync_SelectableRewardConsumesSourceAndAddsReward` | Selectable decrement path emits decoded reward item `202`, count `3`. | Source reviewed; no Java runtime artifact yet. |
| `HandleSelectDecomposableAsync_FullCubeStillAddsSelectableRewardLikeJavaOverflow` | Full-cube selectable path still emits decoded reward item `202`, count `3`. | Source reviewed; Java overflow runtime artifact remains missing. |
| `HandleSelectDecomposableAsync_SelectableRewardDeletesSingleCountSource` | Selectable source-delete path emits decoded reward item `201`, count `2`. | Source reviewed; no Java runtime artifact yet. |
| `ProcessPacketAsync_SelectDecomposableDispatchesSelection` | Process-packet dispatch emits decoded selectable reward item `202`, count `3`. | C# dispatch evidence only. |
| `RunAsync_EncryptedSelectDecomposableFrameDispatchesSelection` | Encrypted socket selectable path emits decoded reward item `202`, count `3`. | C# encrypted socket evidence only. |
| `RunAsync_EncryptedUseItemFrameSchedulesAndCompletesDecompose` | Encrypted socket scheduled decompose emits decoded reward item `200`, count `1`. | C# encrypted socket evidence only. |
| `RunAsync_EncryptedUseItemFrameDeletesLastSourceAndAddsReward` | Encrypted socket delete-source decompose emits decoded reward item `200`, count `1`. | C# encrypted socket evidence only. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Server Packet | Partial | Regression Tested | Partial Parity | Tests now decode add type `0x50`, single reward count, object id, item id, blob count, slot sentinel, and cloth flag for decompose reward adds. Java runtime bytes remain uncaptured. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType.DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.Decomposable` | Packet Metadata | Complete | Regression Tested | Partial Parity | C# constant `0x50` matches reviewed Java source and is decoded in tests. Runtime packet comparison remains missing. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Utility | Partial | Regression Tested | Partial Parity | Tests decode the first general-info blob entry and count only. Additional Java blob entries, optional stats, equipment fields, temporary data, and serialization edge cases remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` | `Aion.GameServer.Network.Aion.GameServerConnection.SendDecomposeRewardItemsAsync` | Service / Packet Side Effect | Partial | Regression Tested | Partial Parity | Reward object ids and counts are asserted for deterministic C# fixtures. Java persistence, DAO side effects, full overflow behavior, and runtime object-id parity remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / `GameServerConnection.HandleSelectDecomposableAsync` | Client Packet Handler | Partial | Regression Tested | Partial Parity | Selectable direct, process-packet, and encrypted socket paths now assert decoded reward-add fields. Java runtime capture remains missing. |
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDecomposeUseItemAsync` / scheduled completion | Scheduled Item Action | Partial | Regression Tested | Partial Parity | Normal direct and encrypted socket paths now assert decoded reward-add fields. Java scheduler/threading order and byte-level runtime output remain unverified. |

## Remaining Risks

- Java runtime capture remains blocked locally because Java 25 JDK and Maven are unavailable; `LoopbackCaptureProof` still has not been compiled or run.
- Reward-add parity is source-reviewed plus C# regression evidence only; no Java packet bytes, unencrypted bodies, encrypted frames, or object-id artifact comparison exists yet.
- `ItemInfoBlob` coverage decodes only the first general-info entry count. Additional Java blob entries, equipment fields, temporary data, serialization differences, and unsupported Java behavior remain unverified.
- Deterministic reward object id `1` is fixture-specific and may differ from live Java ID allocation depending on IDFactory state and persistence.
- Threading/scheduler ordering remains unverified against Java runtime output.
- Full inventory overflow, persistence/DAO, and broadcast/known-list side effects remain partial.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 0 production code artifacts; 1 decoded reward-add test helper plus 9 updated decompose reward-add assertions
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 8 blocked/not-started categories, including Java loopback proof validation, Java runtime reward-add artifact generation, C# artifact comparison tests, full item-info blob comparison, object-id parity, encrypted frame byte capture, live-client validation, and broader inventory overflow/persistence side effects
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, run and harden `LoopbackCaptureProof` before attempting full decompose capture artifacts.

If tooling remains blocked, add a C# JSON observation projection for the existing decompose packet sequences so future Java artifacts can be compared mechanically without changing production behavior. Keep it focused on the packet fields already decoded in Sessions 881 and 882.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java proof validation/fix | `game-server/test/com/aionemu/gameserver/network/aion/LoopbackCaptureProof.java` | No | Requires Java 25/Maven and sequential compile/run feedback. |
| C# JSON observation projection helper | Test helper file plus docs | Maybe | Safe only if it avoids production handler edits and the decompose test fixture is not edited concurrently. |
| Secondary show decoded assertions | `GameServerConnectionInventoryExpansionUseItemTests.cs` | No | Same shared test fixture; do sequentially. |
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
5. Prefer Java proof validation if Java 25/Maven tooling is available; otherwise choose isolated C# comparison-readiness work.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
