# Phase 6OA Completion Handoff - Java Loopback Capture Design

Date: May 25, 2026
Unit of Work: UOW-879
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-879] Design Java loopback decompose capture`)

## Status

Phase 6 is still in progress. This unit converted the Java selectable-decompose runtime comparison path from a broad idea into a concrete loopback capture design.

The recommended Java runtime path is now: start a real Java `NioServer`, accept a real `AionConnection`, drive it from a loopback client socket, decode outbound server frames from the client side, and write future JSON artifacts for `JD-SEL-DEC-001` / `JD-SEL-DEL-001`.

No parity is verified by this unit; it is design and source audit only.

## Files Changed

- `docs/Phase-6-Java-Loopback-Capture-Design.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OA-Completion.md`

## What Changed

- Added `docs/Phase-6-Java-Loopback-Capture-Design.md`.
- Confirmed the Java socket path:
  - `NioServer.connect` starts dispatcher threads and binds configured `ServerCfg` channels.
  - `Acceptor.accept` registers the accepted socket before `AionConnection.initialized()`.
  - `AionConnection.initialized()` sends `SM_KEY`.
  - `Crypt.encrypt` enables crypt while leaving `SM_KEY` unencrypted.
  - `AionClientPacketFactory` registers `CM_SELECT_DECOMPOSABLE` as opcode `236`, valid in `IN_GAME`.
- Chose client-side frame capture instead of reflection into Java private packet queues.
- Documented client packet encoding, server frame decoding, relevant server opcodes, fixture risks, stop conditions, and the next proof-harness target.
- Updated `docs/PHASE-6-PROGRESS.md` with Session 879 parity table, tests, risks, metrics, and next recommended unit.

## Tests

No .NET tests were run because this was a documentation/source-design-only unit with no production code or test code changes.

Previous full validation remains from Session 878:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1450 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.commons.network.NioServer` | `Aion.GameServer.Network.Aion.GameServerConnection` / test socket harnesses | Network Bootstrap | Partial | Manual Only | Needs Verification | Loopback design uses real Java server socket and dispatcher with `readWriteThreads = 0`. No Java harness has been implemented; dispatcher shutdown/thread cleanup remains a risk. |
| `com.aionemu.commons.network.Acceptor` | `Aion.GameServer.Network.Aion.GameServerConnection` accept path | Network Bootstrap | Partial | Manual Only | Needs Verification | Source review confirms accepted sockets are registered before `initialized()` sends `SM_KEY`, giving a valid `SelectionKey` for `sendPacket`. Runtime capture remains missing. |
| `com.aionemu.commons.network.AConnection` | `Aion.Commons.Network` connection primitives / `GameServerConnection` | Connection Base | Partial | Manual Only | Needs Verification | Design intentionally avoids fake connections because Java `sendPacket` is final and depends on a private registered key. Reflection differences remain unsupported for first proof. |
| `com.aionemu.gameserver.network.aion.AionConnection` | `Aion.GameServer.Network.Aion.GameServerConnection` | Game Connection | Partial | Manual Only | Needs Verification | Loopback design exercises real `SM_KEY`, crypt enablement, packet queue, and `processData`; heartbeat scheduler cleanup and player fixture attachment remain risks. |
| `com.aionemu.gameserver.network.Crypt` | `Aion.GameServer.Network.Aion.GameCrypt` | Crypto Utility | Partial | Unit Tested in C#; Manual Only for Java design | Needs Verification | Design documents Java key recovery, client opcode encoding, and server frame decoding. No Java-generated crypt vectors or runtime artifacts produced in this unit. |
| `com.aionemu.gameserver.network.EncryptionKeyPair` | `Aion.GameServer.Network.Aion.GameCrypt` | Crypto Utility | Partial | Unit Tested in C#; Manual Only for Java design | Needs Verification | Client/server rolling-key algorithms are source-reviewed for harness implementation. Java runtime byte parity remains unverified. |
| `com.aionemu.gameserver.network.aion.AionClientPacketFactory` | `Aion.GameServer.Network.Aion.ClientPacketFactory` / socket parser | Packet Factory | Partial | Regression Tested in C#; Manual Only for Java design | Needs Verification | Design identifies opcode `236` and valid `IN_GAME` state for `CM_SELECT_DECOMPOSABLE`. Future proof must show encrypted frame dispatch reaches the Java handler. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` | Client Packet Handler | Partial | Regression Tested in C#; Manual Only for Java design | Partial Parity | Source-reviewed packet order remains the expected runtime target. No Java runtime artifact generated yet. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | `Aion.GameServer.Network.Aion.GameServerPacket` | Packet Serialization | Partial | Regression Tested in C#; Manual Only for Java design | Needs Verification | Design captures outbound bytes from the loopback client side and decodes opcodes/fields, avoiding private queue reflection. Java packet bytes remain uncaptured. |
| `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes` | `Aion.GameServer.Network.Aion.ServerPackets` opcode map | Packet Metadata | Partial | Manual Only | Needs Verification | Relevant decompose response opcodes are documented for future decoder implementation. Broader opcode parity remains unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `Aion.GameServer.Services.Items` packet writers | Service / Packet Side Effects | Partial | Regression Tested in C#; Manual Only for Java design | Partial Parity | Design preserves Java delete order `SM_DELETE_ITEM` then `SM_CUBE_UPDATE` as a required runtime check. Java runtime output still missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.GameServerConnection` send/broadcast helpers | Utility | Partial | Manual Only | Needs Verification | Design requires online player and empty known-list fanout to make self-send order deterministic. Broadcast/threading differences remain unverified. |

## Remaining Risks

- Java runtime capture remains unimplemented.
- Player/account/static-data setup may still require broad Java infrastructure or test-safe DB configuration.
- `IDFactory` and DAO initialization may block deterministic reward item creation.
- Java dispatcher and `ThreadPoolManager` scheduler cleanup must be proven before adding repeatable tests.
- Packet byte decoding logic must handle Java little-endian frame/body layout and signed int key overflow exactly.
- Level 3/4 byte capture remains unverified until the loopback decoder emits stable unencrypted/encrypted bytes.
- Threading differences remain unresolved between Java packet processor execution and C# async handler execution.

## Summary Metrics

- Total Java artifacts discovered: 12
- Total artifacts ported: 0 code artifacts; 1 loopback capture design document added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 12
- Total blocked artifacts: 7 blocked/not-started categories, including Java loopback proof harness, deterministic player/static-data fixture, Java ID allocation fixture, Java runtime artifacts for both selectable scenarios, C# artifact comparison tests, unencrypted body byte capture, and encrypted frame byte capture
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Create a Java loopback proof harness that reaches the handshake and encrypted client-packet delivery boundary.

Suggested scope:

- Add a focused Java test/proof utility under an appropriate test/support location.
- Start `NioServer` with `readWriteThreads = 0` on a configured loopback port.
- Use a test `ConnectionFactory` that stores the accepted `AionConnection`.
- Connect a loopback client socket.
- Read and decode `SM_KEY`.
- Recover the base key and initialize client/server frame helpers.
- Send one encrypted client frame with the correct static client code and checksum shape.
- Attempt to dispatch `CM_SELECT_DECOMPOSABLE` with a minimal fixture or document the exact fixture blocker.
- Do not claim parity unless a runtime artifact is generated and compared.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java loopback proof harness | Java test/support files | No | First implementation should be sequential because build, fixture, and cleanup decisions are still unknown. |
| Live-server capture runbook | docs only | Yes after proof stalls | Useful fallback if loopback fixture setup blocks. |
| Decoded `SmCubeUpdate` assertions | `GameServerConnectionInventoryExpansionUseItemTests.cs` | No with C# item-use work | Good cleanup, but separate from Java harness. |
| Java artifact directory/schema starter | `docs/parity-artifacts` | Maybe | Prefer waiting until real artifact generator writes output. |

## Do Not Parallelize

- Java harness files with simultaneous Java network/fixture edits.
- Progress and handoff docs.
- `GameServerConnection.cs` or decompose test files with unrelated item-use edits.
- Java runtime artifact comparison tests until the first artifact exists.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-Java-Decompose-Harness-Feasibility.md`, `docs/Phase-6-Decompose-Java-Capture-Contract.md`, `docs/Phase-6-Java-Loopback-Capture-Design.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Start with the Java loopback proof harness unless the user redirects.
6. Keep the proof boundary narrow: handshake and encrypted client-frame delivery first.
7. Run focused Java/.NET validation appropriate to any code changes.
8. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
9. Create the next handoff and commit the completed unit.
