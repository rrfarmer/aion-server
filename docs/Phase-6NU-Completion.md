# Phase 6NU Completion Handoff - Corrupt Game Client Packet Threshold

Date: May 25, 2026
Unit of Work: UOW-873
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-873] Test corrupt game client packet threshold`)

## Status

Phase 6 is still in progress. This unit added connection-level socket coverage for the Java corrupt encrypted packet threshold.

The new smoke test connects to the hosted C# game socket, reads the initial `SmKey`, sends three invalid client frames after crypt is enabled, verifies the first two keep the connection active, and verifies the third closes the client connection. This is source-derived C# runtime evidence aligned to Java `AionConnection.processData`; it is not a Java runtime comparison or byte-vector test.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameClientSocketServerSmokeTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NU-Completion.md`

## What Changed

- Added `GameClientSocketServer_ClosesAfterThreeBadEncryptedClientFrames`.
- Reused the existing hosted socket smoke test server and frame helpers.
- Stabilized the existing decompose socket-loop tests by waiting for full packet count, closing the connection loop, and snapshotting packet observations before assertions.
- Verified:
  - `SmKey` is sent before client frames are processed
  - first bad encrypted client frame is ignored
  - second bad encrypted client frame is ignored
  - third bad encrypted client frame closes the socket
  - active connection count returns to zero after shutdown
- Kept the unit narrow:
  - no production code changes
  - no Java-generated crypt vectors
  - no server-encryption byte comparison
  - no Java runtime harness

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameClientSocketServerSmokeTests --no-restore
```

Result: passed, 3 tests.

Focused regression:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 29 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1448 tests.

New test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameClientSocketServerSmokeTests.GameClientSocketServer_ClosesAfterThreeBadEncryptedClientFrames` | After `SmKey`, two invalid client frames leave the connection active and the third invalid frame closes it. | Java `AionConnection.MAX_CORRUPT_PACKETS_BEFORE_DISCONNECT = 3` and `processData` decrypt-failure branch source reviewed; no Java runtime output captured. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.AionConnection.processData` | `GameServerConnection.ReadPacketAsync` / `GameClientSocketServerSmokeTests` | Connection Read / Corrupt Packet Handling | Partial | Regression Tested | Partial Parity | Socket test covers Java's corrupt packet threshold intent: first two decrypt failures are ignored, third closes the connection. Java runtime comparison, packet logs, and exact dispatcher return behavior remain unverified. |
| `com.aionemu.gameserver.network.Crypt.decrypt` | `GameCrypt.DecryptClientPayload` | Crypto Utility / Connection Dependency | Partial | Regression Tested through socket path | Partial Parity | Test exercises decrypt-failure propagation through a live C# socket connection after `SmKey`. Java-generated crypt vectors and deterministic encrypted bytes remain missing. |
| `com.aionemu.gameserver.network.EncryptionKeyPair.validateClientPacket` | `GameEncryptionKeyPair.ValidateClientPacket` via `GameCrypt` | Crypto Validation Dependency | Partial | Regression Tested through socket path | Partial Parity | Invalid packet body fails validation repeatedly and increments the connection-level corrupt counter. Validation details remain private and source-derived, not Java-runtime compared. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `RunAsync_EncryptedSelectDecomposableFrameDispatchesSelection` | Client Packet Handler / Test Stabilization | Partial | Regression Tested | Partial Parity | Existing socket-loop test was stabilized with connection close and packet snapshot assertions. No new Java behavior was verified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` / `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `RunAsync_EncryptedUseItemFrameSchedulesAndCompletesDecompose` / `RunAsync_EncryptedUseItemFrameDeletesLastSourceAndAddsReward` | Client Packet Handler / Scheduled Item Action / Test Stabilization | Partial | Regression Tested | Partial Parity | Existing normal decompose socket-loop tests were stabilized with connection close and packet snapshot assertions. No new Java behavior was verified. |

## Remaining Risks

- Java runtime comparison remains unimplemented; no artifact currently proves Java-vs-C# packet order or byte parity for decompose.
- Game crypt and corrupt-threshold tests are source-derived and deterministic but not Java-generated golden vectors.
- Server encryption key evolution and encrypted server-packet byte parity remain untested.
- Pre-key garbage skip behavior and valid-packet recovery after corrupt frames are only partially covered.
- Full packet byte parity, opcode/frame/crypto breadth, broadcast fanout, socket visibility, serialization side effects, random reward selection, and live-client validation remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 1 C# socket-loop corrupt-packet threshold regression slice plus stabilization of 3 existing decompose socket-loop tests
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 7 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Either add a deterministic server-encryption/key-evolution test for `GameCrypt.EncryptServerPayload` / `GameServerPacket.SerializeFrame`, or return to the Java harness spike from `docs/Phase-6-Decompose-Java-Runtime-Comparison-Plan.md`.

Suggested scope:

- Prefer the server-encryption test only if expected bytes can be derived cleanly without copying too much private crypto logic into the test.
- Otherwise begin the Java harness feasibility spike for selectable decompose packet-order capture.
- Keep progress/handoff docs orchestrator-owned.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Server encryption key evolution test | `GameCryptTests.cs` or a dedicated test file | Maybe | Safe if expected bytes are simple/deterministic; avoid production crypto edits. |
| Java harness feasibility spike | read-only Java/test-harness audit, possibly `dotnetConversion/tools` later | Maybe | Keep separate from C# crypto test writes. |
| Decompose Java comparison artifact design | docs only | Yes as analysis | Could refine the plan with exact fixture data and capture schema. |
| Progress/handoff docs | docs | No | Orchestrator-owned after validation. |

## Do Not Parallelize

- Multiple writers in crypto/socket test files.
- Java harness/vector generator edits alongside C# crypto tests unless exact file ownership is isolated.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Choose between server-encryption key-evolution testing and Java harness feasibility.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
