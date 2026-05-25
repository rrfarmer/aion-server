# Phase 6OE Completion Handoff - Decompose JSON Observation Projection

Date: May 25, 2026
Unit of Work: UOW-883
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-883] Project decompose observation json`)

## Status

Phase 6 is still in progress. This unit added a C# in-memory JSON observation projection for the two first-capture selectable decompose scenarios from `docs/Phase-6-Decompose-Java-Capture-Contract.md`.

The projection is comparison-readiness work only. It does not create Java runtime evidence, write blessed artifact files, or prove parity. Its value is giving future Java captures a contract-shaped C# output to compare against mechanically.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OE-Completion.md`

## What Changed

- Added `CaptureSelectableDecomposeObservationJson_ProjectsContractComparablePackets`.
- Added deterministic in-memory projection JSON for:
  - `JD-SEL-DEC-001`
  - `JD-SEL-DEL-001`
- Projected fixture, client packet, packet order, decoded packet fields, final inventory, unsupported gaps, and risks.
- Decoded packet observations for:
  - `SM_ITEM_USAGE_ANIMATION`
  - `SM_SYSTEM_MESSAGE`
  - `SM_INVENTORY_UPDATE_ITEM`
  - `SM_DELETE_ITEM`
  - `SM_CUBE_UPDATE`
  - `SM_SECONDARY_SHOW_DECOMPOSABLE`
  - `SM_INVENTORY_ADD_ITEM`
- Asserted contract packet order and key decoded fields for decrement and delete selectable scenarios.

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 30 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1451 tests.

Added test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CaptureSelectableDecomposeObservationJson_ProjectsContractComparablePackets` | Builds C# JSON observations for `JD-SEL-DEC-001` and `JD-SEL-DEL-001`, then asserts contract packet order and key decoded Level 2 fields. | Aligned to Java capture contract and Java source-reviewed packet order only; no Java runtime artifact yet. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / `GameServerConnection.HandleSelectDecomposableAsync` | Client Packet Handler | Partial | Regression Tested | Partial Parity | C# now projects both first-capture selectable scenarios into contract-shaped JSON. Java runtime comparison remains missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `Aion.GameServer.Network.Aion.ServerPackets.SmItemUsageAnimation` | Server Packet | Partial | Regression Tested | Partial Parity | Projection decodes player/target/source/item ids, time, end, and unknown fields. Java runtime constructor/default behavior remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested | Needs Verification | Projection decodes message id and parameters. It does not yet assert Java factory-name mapping beyond observed C# fields. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Server Packet | Partial | Regression Tested | Partial Parity | Projection decodes source object id, item name, general-info count, and update type mask `0x16` for selectable decrement. Broader blob fields and Java runtime bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` | Server Packet | Partial | Regression Tested | Partial Parity | Projection decodes source object id and delete type `0x17` for selectable delete. Java runtime packet bytes remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server Packet | Partial | Regression Tested | Partial Parity | Projection includes decoded delete follow-up cube update with `items_count = 0`. Other cube callers and Java runtime bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SECONDARY_SHOW_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSecondaryShowDecomposable` | Server Packet | Partial | Regression Tested | Partial Parity | Projection decodes source object id, unknown dword, and reward count `0` for secondary clear. Full Java byte comparison remains missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Server Packet | Partial | Regression Tested | Partial Parity | Projection includes add type `DECOMPOSABLE`, item id, generated object id, count, slot, and cloth flag for selectable rewards. Java runtime object-id and byte parity remain unverified. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Utility | Partial | Regression Tested | Partial Parity | Projection reuses general-info blob count decoding for update/add packets only. Additional blob entries, equipment fields, temporary data, and serialization differences remain unverified. |

## Remaining Risks

- Java runtime capture remains blocked locally because Java 25 JDK and Maven are unavailable; `LoopbackCaptureProof` still has not been compiled or run.
- The JSON projection is in-memory test evidence only. It does not write blessed artifacts, compare against Java files, or include encrypted/unencrypted byte payloads.
- `SM_SYSTEM_MESSAGE` projection decodes numeric message data but does not yet map IDs back to Java factory names in JSON.
- `ItemInfoBlob` projection remains shallow and only extracts the first general-info entry count.
- Object-id parity remains fixture-local and must be compared against a Java runtime capture before verification.
- Threading/scheduler and live-client behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 9
- Total artifacts ported: 0 production code artifacts; 1 C# JSON projection test plus packet decoders for 7 server-packet shapes
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked artifacts: 8 blocked/not-started categories, including Java loopback proof validation, Java runtime JSON artifacts, C# artifact-file comparison tests, byte capture, Java message factory-name projection, full item-info blob comparison, object-id parity, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, run and harden `LoopbackCaptureProof`.

If tooling remains blocked, good next units are:

- Add a guarded file-backed comparison test that loads future `docs/parity-artifacts/java/decompose/selectable/*.json` files when present and skips/records absence without claiming parity.
- Add a docs-only live-server capture runbook that explains how to produce Java JSON artifacts matching the capture contract.
- Extend the C# projection with Java factory-name mapping for `SM_SYSTEM_MESSAGE`, keeping it source-reviewed and explicitly unverified.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java proof validation/fix | `game-server/test/com/aionemu/gameserver/network/aion/LoopbackCaptureProof.java` | No | Requires Java 25/Maven and sequential compile/run feedback. |
| Guarded Java artifact comparison test | Test helper file plus future artifact path | Maybe | Safe only if no one else edits the decompose test fixture. |
| Live-server capture runbook | docs only | Yes | Useful fallback while Java proof remains tooling-blocked. |
| System-message factory mapping | C# test helper only | No | Same shared projection helper; do sequentially. |

## Do Not Parallelize

- `GameServerConnection.cs` with any other item-use handler edit.
- `GameServerConnectionInventoryExpansionUseItemTests.cs` with other decompose/projection edits.
- `LoopbackCaptureProof.java` with another Java network harness edit.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-Decompose-Java-Capture-Contract.md`, `docs/Phase-6-Java-Loopback-Capture-Design.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Prefer Java proof validation if Java 25/Maven tooling is available; otherwise choose isolated comparison-readiness work.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
