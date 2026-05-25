# Phase 6 Java Loopback Capture Design

Date: May 25, 2026
Unit of Work: UOW-879
Status: Design complete; Java capture harness not implemented

## Purpose

Define the first implementation design for generating Java runtime capture artifacts for the selectable-decompose contract in `docs/Phase-6-Decompose-Java-Capture-Contract.md`.

Java remains the source of truth. This document does not prove parity; it narrows the harness implementation path so the next unit can build a small proof without changing production Java.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Java loopback socket capture design | `NioServer`, `Acceptor`, `AConnection`, `AionConnection`, `Crypt`, packet opcodes | docs only / read-only Java | Java Analysis / Documentation | Yes, orchestrator-owned | Medium | Best next step toward runtime artifacts; no production code ownership conflict. |
| B | Live-server capture runbook | Java launch/config/DB fixture | docs only | Documentation Update | Yes | Medium | Useful fallback, but should inherit the loopback decision first. |
| C | Decoded `SmCubeUpdate` assertions | C# decompose tests | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Test Update | No | Low-Medium | Valuable cleanup, but touches shared item-use test fixture and is less important than Java runtime artifact path. |
| D | Artifact output folder skeleton | `docs/parity-artifacts/java/decompose/selectable` | docs/artifact folders | Documentation / Fixture Scaffold | Maybe later | Low | Wait until the first generator writes real data so empty scaffolding does not imply evidence. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Java loopback socket capture design | Java Analysis / Documentation | Java source reads; `docs/Phase-6-Java-Loopback-Capture-Design.md`; progress and handoff docs | Production Java/C# code changes | UOW-876 feasibility audit and UOW-877 capture contract | Conservative loopback harness design with stop conditions and next implementation unit |

No sub-agents were used because write targets are shared documentation/progress files and the Java source work was read-only.

## Source Anchors

Network/bootstrap:

- `com.aionemu.commons.network.NioServer`
- `com.aionemu.commons.network.Acceptor`
- `com.aionemu.commons.network.Dispatcher`
- `com.aionemu.commons.network.AcceptReadWriteDispatcherImpl`
- `com.aionemu.commons.network.AConnection`
- `com.aionemu.commons.network.ConnectionFactory`
- `com.aionemu.commons.network.ServerCfg`
- `com.aionemu.gameserver.network.aion.GameConnectionFactoryImpl`
- `com.aionemu.gameserver.network.aion.AionConnection`

Packet/crypt:

- `com.aionemu.gameserver.network.Crypt`
- `com.aionemu.gameserver.network.EncryptionKeyPair`
- `com.aionemu.gameserver.network.aion.AionServerPacket`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE`
- `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes`

Selectable-decompose side effects:

- `com.aionemu.gameserver.utils.PacketSendUtility`
- `com.aionemu.gameserver.services.item.ItemPacketService`
- `com.aionemu.gameserver.services.item.ItemService`
- `com.aionemu.gameserver.model.items.storage.Storage`

## Key Java Findings

`NioServer.connect` accepts one or more `ServerCfg` values, initializes dispatchers, opens non-blocking `ServerSocketChannel`s, binds them, registers each with an `Acceptor`, and starts the dispatcher threads.

`Acceptor.accept` creates a real connection through `ConnectionFactory.create(socket, dispatcher)`, registers it for `OP_READ`, then calls `con.initialized()`. For `AionConnection`, `initialized()` sends `SM_KEY`.

`AConnection.sendPacket` is final and requires a valid registered `SelectionKey`. This confirms that loopback is the cleaner path: the harness should not fake `sendPacket`.

`AionConnection.processData` ignores client packets until crypt is enabled. Crypt becomes enabled when `SM_KEY` is written: `AionServerPacket.write` calls `con.encrypt(slice)`, and `Crypt.encrypt` enables crypt without encrypting the first server packet.

`AionClientPacketFactory` registers `CM_SELECT_DECOMPOSABLE` at opcode `236` and valid state `IN_GAME`.

`CM_SELECT_DECOMPOSABLE.runImpl` emits the source-reviewed order:

1. `SM_ITEM_USAGE_ANIMATION`
2. `SM_SYSTEM_MESSAGE`
3. source mutation through `Storage.decreaseByObjectId` / `ItemPacketService`
4. `SM_SECONDARY_SHOW_DECOMPOSABLE`
5. reward add through `ItemService.addItem`

For source delete, `ItemPacketService.sendItemDeletePacket` emits `SM_DELETE_ITEM` followed by `SM_CUBE_UPDATE.cubeSize(storageType, player)`.

## Recommended Capture Shape

Use a real Java loopback socket, but capture from the client side instead of trying to override Java packet send behavior.

Target architecture:

```text
JUnit/proof harness
  |
  | starts NioServer(readWriteThreads = 0)
  v
Java NIO dispatcher + real AionConnection
  |
  | SM_KEY, then encrypted server frames
  v
Loopback client socket owned by harness
  |
  | decodes server opcodes and fields
  v
JSON artifact under docs/parity-artifacts/java/decompose/selectable
```

This path uses the real dispatcher, `SelectionKey`, `AionConnection.sendPacket`, packet queue, crypt state, and packet writers. It avoids reflection against private `AConnection.key`, private `AionConnection.sendMsgQueue`, and private final `Crypt`.

## Harness Boot Sequence

1. Pick a test port.
   - Do not rely on port `0` unless the harness bypasses `NioServer`, because `NioServer` does not expose the actual bound port and stores server channel keys privately.
   - Prefer a configurable property, for example `-Daion.capture.port=21077`, and skip/fail early if unavailable.
2. Create a test `ConnectionFactory`.
   - The simplest factory can instantiate `new AionConnection(socket, dispatcher)` and store the accepted connection in an `AtomicReference<AionConnection>`.
   - A subclass may expose test-only helper methods, but it cannot override `sendPacket`, `processData`, or `writeData` because those are final on the relevant classes.
3. Start `NioServer` with `readWriteThreads = 0`.
   - This uses one `AcceptReadWriteDispatcherImpl`, reducing thread interleaving.
4. Connect a loopback client socket.
5. Wait for the accepted `AionConnection`.
6. Read the first server frame, `SM_KEY`.
7. Decode the false key and initialize client-side crypt helpers.
8. Build the Java `Player` fixture, set it online, and attach it:
   - `player.setClientConnection(connection)`
   - `connection.setAccount(account)`
   - `connection.setActivePlayer(player)`
   - ensure state is `IN_GAME`
   - install an empty known-list equivalent if required by the fixture path
9. Install deterministic inventory/static data for one scenario.
10. Send encrypted `CM_SELECT_DECOMPOSABLE` from the loopback client.
11. Drain server frames until the expected sequence is observed or a timeout expires.
12. Decode opcodes/fields into the JSON artifact schema.
13. Close client/server resources and stop scheduler/dispatcher side effects as far as Java APIs allow.

## Client Packet Encoding Notes

The client body after the two-byte frame length must satisfy `EncryptionKeyPair.validateClientPacket`:

- bytes `0..1`: encoded client opcode
- byte `2`: static client packet code `0x65`
- bytes `3..4`: bitwise complement of the encoded opcode as a little-endian short
- remaining bytes: packet payload

`CM_SELECT_DECOMPOSABLE.readImpl` expects:

- `writeD(objectId)`
- `writeD(unknownDword)`
- `writeC(index)`

The encoded opcode is the inverse of `Crypt.decodeClientPacketOpcode`:

```text
encoded = (((opcode + SM_VERSION_CHECK.INTERNAL_VERSION) ^ 0xEF) + 0x0C) ^ 0xEF
```

For the first scenarios, `opcode = 236` and `SM_VERSION_CHECK.INTERNAL_VERSION = 207`.

Client encryption should mirror the Java client-key side of `EncryptionKeyPair`: initialize key bytes from the decoded base key and use the same rolling XOR algorithm as Java server encryption, but with the client key. The encrypted frame length includes the two length bytes; the encrypted body excludes them.

## Server Frame Decoding Notes

The first `SM_KEY` frame is unencrypted. Its body contains:

- encoded server opcode
- static server packet code `0x44`
- bitwise complement of encoded opcode
- false key from `SM_KEY.writeImpl`

Recover the base key with Java int overflow semantics:

```text
baseKey = (falseKey - 0x3FF2CCCF) ^ 0xCD92E4DF
```

Subsequent server frames are encrypted. The harness should decrypt them with the server key, then reverse `Crypt.encodeServerPacketOpcode`:

```text
serverOpcode = (encodedServerOpcode ^ 0xDF) - SM_VERSION_CHECK.INTERNAL_VERSION
```

Relevant server opcode map from `ServerPacketsOpcodes`:

| Opcode | Java Packet |
|---:|---|
| 25 | `SM_SYSTEM_MESSAGE` |
| 27 | `SM_INVENTORY_ADD_ITEM` |
| 28 | `SM_DELETE_ITEM` |
| 29 | `SM_INVENTORY_UPDATE_ITEM` |
| 72 | `SM_KEY` |
| 130 | `SM_CUBE_UPDATE` |
| 183 | `SM_ITEM_USAGE_ANIMATION` |
| 286 | `SM_SECONDARY_SHOW_DECOMPOSABLE` |

For the first proof, the decoder only needs these packet classes and the fields required by `docs/Phase-6-Decompose-Java-Capture-Contract.md`.

## Fixture Risks

The socket/crypt path is straightforward enough for a proof, but player/data setup remains the main risk:

- `Player` construction requires `PlayerAccountData`, `Account`, stats, storage, controller, and move-controller state.
- `PacketSendUtility.sendPacket` drops packets unless `player.isOnline()` is true.
- `broadcastPacketAndReceive` sends to self first, then known players; the fixture must keep known-list fanout empty.
- `ItemService.addItem` uses `DataManager.ITEM_DATA`, `ItemFactory`, `IDFactory`, expirable registration, storage capacity checks, and item packet services.
- `IDFactory` initializes from DAOs, so the proof needs a test-safe DB/config path, a safe singleton setup, or a narrowed fixture technique that still exercises Java item packet side effects.
- `AionConnection` starts `ConnectionAliveChecker` through `ThreadPoolManager`; the proof must close the connection and avoid leaving scheduler threads active.

## Stop Conditions

Stop the loopback implementation and switch to the live-server runbook if any of these occur:

- starting `NioServer` requires production Java changes
- the harness cannot isolate player static data without production DB writes
- `IDFactory` or DAO setup becomes broader than a focused proof
- scheduler/dispatcher threads cannot be cleaned up reliably in a repeatable test
- decoded packet order is polluted by unrelated server traffic
- encrypted client frame submission repeatedly fails before reaching `CM_SELECT_DECOMPOSABLE.runImpl`

Stop and consider a smaller reflected proof only if socket crypt is the blocker. Do not use reflection to bypass item/storage side effects and then call the result runtime parity.

## First Implementation Unit

Recommended next unit:

Create a Java proof harness that reaches the handshake and encrypted client-packet delivery boundary without yet requiring full item/static-data fixture success.

Minimum target:

- start `NioServer` on a configured loopback port
- accept a real `AionConnection`
- read and decode `SM_KEY`
- send one encrypted client frame with a valid body/checksum shape
- observe either `CM_SELECT_DECOMPOSABLE` execution with a controlled no-op fixture or a clear Java-side reason why player/data setup blocks the full scenario
- document the result without marking parity verified

If this proof succeeds, the following unit should add fixture data and write the first `JD-SEL-DEC-001` Level 1/2 artifact.

## Migration Parity Table

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

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / Java Design | Source review of Java NIO bootstrap, Aion connection, crypt, packet factory, and selectable-decompose side effects | Documents a concrete loopback capture path and first proof target. | Static source inspection only. | No Java harness, no runtime artifact, no C# comparison against Java output, no byte vectors. |

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
- Estimated overall migration completion: Phase 6 remains about 66% complete; this unit reduces implementation uncertainty but does not add runtime parity evidence.
