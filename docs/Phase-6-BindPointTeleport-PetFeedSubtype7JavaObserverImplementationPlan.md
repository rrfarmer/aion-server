# Phase 6 - Pet Feed Subtype 7 Java Observer Implementation Plan

Date: May 27, 2026
Unit of Work: UOW-1336
Status: Design complete; no Java observer hook or runtime artifacts were generated in this unit.

## Purpose

This document turns the subtype `7` runtime-vector design and send-timing feasibility audit into a concrete Java-side observer implementation plan. It does not modify Java production behavior and does not claim Java runtime parity.

Java source of truth:

- `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_EMOTION.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- `game-server/src/com/aionemu/gameserver/network/aion/AionConnection.java`
- `game-server/src/com/aionemu/gameserver/network/aion/AionServerPacket.java`

## Required Capture Point

Do not capture only at `PacketSendUtility.sendPacket`.

Reason:

- `PacketSendUtility.sendPacket(Player, AionServerPacket)` checks `player.isOnline()` and delegates to `player.getClientConnection().sendPacket(packet)`.
- The connection queues the packet object.
- `SM_PET` subtype `7` reads mutable `PetCommonData` during `writeImpl`, not in its constructor.
- Capturing at queue time cannot prove whether subtype `7` serialized pre-reset or post-reset feed-progress/refeed-delay state.

Preferred capture point:

- `AionServerPacket.write(AionConnection con, ByteBuffer buffer)` after `writeImpl(con)`, `flip()`, and length stamping, but before `con.encrypt(...)`.

Alternative capture point:

- `AionConnection.writeData(ByteBuffer data)` immediately after `packet.write(this, data)`, if the plan also needs encrypted wire bytes. This is less useful for stable parity because encryption mutates the slice and depends on connection crypt state.

## Proposed Java Hook Shape

Add a no-op observer with test-only install/reset methods. Production default must be null/no-op and must not allocate or copy buffers.

Suggested files:

- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketSerializationObserver.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketSerializationObservation.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketSerializationPhase.java`
- guarded changes in `game-server/src/com/aionemu/gameserver/network/aion/AionServerPacket.java`

Suggested API:

```java
public interface ServerPacketSerializationObserver {
	void onSerialized(ServerPacketSerializationObservation observation);
}
```

Observation fields:

| Field | Required | Notes |
|---|---|---|
| `packetClassName` | Yes | Fully qualified Java packet class name. |
| `packetSimpleName` | Yes | Example: `SM_PET`. |
| `opcode` | Yes | `packet.getOpCode()` before obfuscation. |
| `phase` | Yes | Start with `PLAIN_AFTER_LENGTH_BEFORE_ENCRYPT`. |
| `payloadHex` | Yes | Stable canonical bytes chosen by the hook. |
| `bodyHex` | Yes | Packet body without opcode/framing where practical. |
| `bufferLimit` | Yes | Helps detect length/header mistakes. |
| `connectionState` | Optional | Useful but must not force full player/session setup. |
| `activePlayerObjectId` | Optional | Helpful for feed scenarios when available. |

Hook placement sketch:

1. `AionServerPacket.write` writes placeholder length, opcode/static/checksum, and body.
2. It flips and stamps the packet length.
3. If an observer is installed, copy stable clear bytes from a duplicate buffer before encryption.
4. Then call `con.encrypt(...)` exactly as Java does now.

Important implementation rule:

- Never pass the live mutable `ByteBuffer` to the observer. Copy into a fresh byte array from a duplicated buffer so observer code cannot disturb `position`, `limit`, encryption, or socket writes.

## Byte Forms

The C# verifier already supports:

- `bodyHex`
- `canonicalPayloadHex`
- optional `wireFrameHex`

Recommended Java observer output for this subtype:

| Byte Form | Required | Recommended Source |
|---|---|---|
| `bodyHex` | Yes | Decoded packet body after the server-packet opcode/static/checksum header is removed. |
| `canonicalPayloadHex` | Yes | Plain opcode convention used by C# packet tests plus `bodyHex`. |
| `wireFrameHex` | Optional | Full encrypted frame only if crypt state is deterministic and documented. |

For subtype `7`, `bodyHex` must include the exact serialized FOOD payload:

1. action id `PetAction.FOOD`
2. function header bytes
3. subtype byte `7`
4. feed-progress data
5. refeed-delay seconds
6. item object id
7. trailing zero

For end-feeding `SM_EMOTION`, `bodyHex` must include:

1. player object id
2. `EmotionType.END_FEEDING`
3. player state
4. movement speed

## Artifact Output Layout

Recommended generated artifacts:

```text
parity-artifacts/
  pet-feed-subtype7/
    java/
      schema-v1.json
      rewarded-feed-single-count.json
      rewarded-feed-repeat-count.json
      rewarded-feed-cooldown-boundary.json
```

Do not check in generated artifacts until they are reproducible from a documented command and reviewed against Java source.

## Scenario Runner Shape

Suggested future Java runner classes:

- `game-server/test/com/aionemu/gameserver/parity/petfeed/PetFeedSubtype7VectorGenerator.java`
- `game-server/test/com/aionemu/gameserver/parity/petfeed/PetFeedSubtype7Scenario.java`
- `game-server/test/com/aionemu/gameserver/parity/petfeed/PetFeedSubtype7PacketCapture.java`
- `game-server/test/com/aionemu/gameserver/parity/petfeed/PetFeedSubtype7ArtifactWriter.java`

If Java test-source wiring is too invasive, use a clearly named standalone parity/debug class that is never invoked by production startup.

Recommended CLI:

```text
PetFeedSubtype7VectorGenerator \
  --scenario rewarded-feed-single-count \
  --output parity-artifacts/pet-feed-subtype7/java \
  --player-object-id 7001 \
  --pet-object-id 7101 \
  --food-item-object-id 5001 \
  --food-item-id 188000001 \
  --reward-item-id 186000001 \
  --cooldown-minutes 1 \
  --requested-count 1
```

Recommended options:

| Option | Required | Notes |
|---|---|---|
| `--scenario` | Yes | One of the artifact names above. |
| `--output` | Yes | Output directory. |
| `--player-object-id` | Yes | Stable player id for `SM_EMOTION`. |
| `--pet-object-id` | Yes | Stable pet id for state snapshots. |
| `--food-item-object-id` | Yes | Should appear in subtype `2`, not subtype `7`. |
| `--food-item-id` | Yes | Must resolve to a deterministic accepted food. |
| `--reward-item-id` | Yes | Must match deterministic reward selection. |
| `--cooldown-minutes` | Yes | Positive value required to expose delay semantics. |
| `--requested-count` | Yes | Capture `1` and `2` scenarios. |
| `--java-commit` | Optional | Use `git rev-parse HEAD` when omitted. |

## Scenario Execution Plan

The runner should execute real `PetService.checkFeeding` when fixture setup can safely satisfy dependencies.

Minimum fixture requirements:

1. Player object with fixed object id, online connection double, state, movement speed, and attack-speed stats.
2. Pet object with `PetCommonData`, feed progress one feed away from `FULL`, template/flavour, and master reference.
3. Food item with fixed object id, item id, count, template level, and storage context.
4. Pet flavour and reward group that deterministically returns the requested reward item and cooldown.
5. Packet observer installed before invoking `checkFeeding`.
6. DAO/reward/item-service side effects either stubbed in a documented fixture or captured as blocked if full execution is impossible.

Capture state snapshots:

- before `checkFeeding`
- at serialization time for each observed packet if possible
- after `checkFeeding` returns

The artifact must include whether the scenario is:

- `captureLevel = "pet-service"`
- `captureLevel = "packet-constructor"`
- `captureLevel = "manual-serialization"`

Only `pet-service` plus send-time serialization can resolve subtype `7` timing parity.

## C# Verifier Follow-Up

The current C# verifier is:

- `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedSubtype7JavaVectorArtifactReaderTests.cs`

After Java artifacts exist, it should:

1. Read all JSON files under `parity-artifacts/pet-feed-subtype7/java`.
2. Validate packet order and decoded fields.
3. Compare `SM_PET` `bodyHex` and `canonicalPayloadHex` through `SmPet.Food(...)`.
4. Compare end-feeding `SM_EMOTION` `bodyHex` and `canonicalPayloadHex` through `SmEmotion`.
5. Report missing artifacts as `Needs Verification`, not failure, until artifact generation is part of the normal parity suite.
6. Keep `IsJavaRuntimeParity` false in feed metadata bridges until generated Java artifacts compare cleanly.

## Readiness Gates

Before enabling live C# subtype `7` feed dispatch:

1. Java observer hook is reviewed and inert by default.
2. At least the single-count reward vector exists at `captureLevel = "pet-service"`.
3. Repeat-count and cooldown-boundary vectors exist or are explicitly blocked with reasons.
4. C# verifier compares `SM_PET` subtype `2`, `6`, `5`, and `7` bodies with no unexplained mismatches.
5. C# verifier compares end-feeding `SM_EMOTION` with no unexplained mismatches.
6. Artifact states prove whether subtype `7` serialized pre-reset or post-reset feed progress.
7. Artifact states prove whether subtype `7` serialized zero delay or scheduled cooldown delay.
8. Reward item add, scheduler, DAO, inventory mutation, and socket ordering decisions are documented before any live C# send is enabled.

## Known Risks

- Java fixture setup for `PetService.checkFeeding` may require static data, item templates, player stats, inventory, DAO, and reward services.
- Adding an observer to Java packet core is sensitive and must remain no-op when unset.
- Copying the wrong buffer range can produce misleading byte artifacts.
- Encryption mutates packet bytes and depends on connection crypt state.
- Manual packet serialization would not prove Java send-time mutable-state behavior.
- Reward selection must be deterministic or artifact comparison will be noisy.
- Wall-clock cooldown seconds can drift unless call start/end timing is recorded.

## Migration Parity Table - UOW-1336

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.AionServerPacket.write` | `docs/Phase-6-BindPointTeleport-PetFeedSubtype7JavaObserverImplementationPlan.md` | Packet Serialization / Design | Not Started | Manual Only | Needs Verification | Plan identifies clear-before-encrypt observer hook. No Java code or artifacts were generated. |
| `com.aionemu.gameserver.network.aion.AionConnection.writeData` | observer implementation plan | Network Connection / Design | Not Started | Manual Only | Needs Verification | Plan keeps this as optional post-write/encrypted capture point. No Java hook was implemented. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | observer implementation plan | Network Utility / Design | Not Started | Manual Only | Needs Verification | Plan explicitly rejects this as the sole capture point because it observes queue-time packets only. |
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` rewarded full branch | `PetFeedSubtype7JavaVectorArtifactReaderTests`; observer implementation plan | Service Flow / Future Runtime Artifact | Partial | Unit Tested schema only | Needs Verification | Existing C# reader can consume future artifacts, but no Java runner/fixture exists yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` and `SM_EMOTION` | `SmPet`; `SmEmotion`; artifact reader | Packet / Future Runtime Comparison | Partial | Unit Tested source-derived | Needs Verification | C# comparison paths exist for future artifacts. Java runtime bytes are missing. |

## Tests Added

No executable tests were added in UOW-1336. This was a documentation/design unit based on Java source review, UOW-1333 vector design, UOW-1334 artifact reader, and UOW-1335 send-timing feasibility findings.

## Remaining Risks

- Java observer hook is not implemented.
- Java runtime subtype `7` artifacts are not generated.
- C# verifier is ready for artifacts but has not compared Java bytes.
- Fixture setup may require static data and service seams that are not yet isolated.
- Live feed dispatch, scheduler execution, reward creation, DAO persistence, inventory mutation, and socket dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 observer implementation plan document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java observer hook, Java subtype `7` artifacts, deterministic pet-feed fixture, live common-data wiring, scheduler, reward creation, DAO persistence, inventory mutation, socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Either implement only the inert Java serialization observer hook behind a test-only install/reset API, or add a pet/house storage unlock behavior audit while Java observer tooling remains too risky for immediate code changes. Do not enable live C# feed dispatch.
