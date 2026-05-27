# Phase 6 - Pet Feed Java Packet Capture Observer Shell

Date: May 27, 2026
Unit of Work: UOW-1347

## Scope

This unit implements the generic disabled-by-default Java packet serialization observer shell identified in UOW-1346.

Java source touched:

- `com.aionemu.gameserver.network.aion.AionServerPacket`
- `com.aionemu.gameserver.network.aion.capture.ServerPacketCaptureObserver`
- `com.aionemu.gameserver.network.aion.capture.NoOpServerPacketCaptureObserver`

## What Changed

- Added `ServerPacketCaptureObserver`.
- Added `NoOpServerPacketCaptureObserver`.
- Added a volatile capture observer field to `AionServerPacket`.
- Added `AionServerPacket.setCaptureObserver(...)`, with `null` resetting to no-op.
- Added the guarded serialization callback after packet length stamping and before encryption:

```text
buf.flip()
buf.putShort((short) buf.limit())
observer.onPacketSerialized(con, this, buf.asReadOnlyBuffer())
con.encrypt(...)
```

The default path remains disabled. The read-only buffer duplicate is only created when `observer.isEnabled()` returns `true`.

## Boundaries Preserved

- No artifact writing was added.
- No unusual-storage scenario context hook was added.
- No Java config flag was added yet.
- No live dispatch behavior changed intentionally.
- No packet-specific serializer was modified.
- No C# reader behavior changed.

## Validation

- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Validation was blocked because `mvn` is not available on PATH and no Maven wrapper exists in the repository (`mvnw`/`mvnw.cmd` absent).
- Existing docs in this repository also note Java 25/Maven tooling is unavailable locally.

## Migration Parity Table - UOW-1347

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.AionServerPacket.write` | future Java runtime artifact producer; C# guarded artifact readers | Serialization Hook | Partial | Manual Only | Needs Verification | Added disabled-by-default observer callback after length stamping and before encryption. Maven compile could not run locally. No artifacts generated. |
| `com.aionemu.gameserver.network.aion.capture.ServerPacketCaptureObserver` | future C# artifact comparison contract | Interface | Partial | No Tests | Needs Verification | New Java observer interface with enabled guard and serialized clear-frame callback. No concrete capture implementation yet. |
| `com.aionemu.gameserver.network.aion.capture.NoOpServerPacketCaptureObserver` | N/A | Utility / No-op Observer | Complete | No Tests | Needs Verification | Default observer returns disabled and does nothing. Compile validation is blocked locally. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` default branch | future `PetFeedUnusualStorageArtifactCapture`; C# reader | Service Flow / Observer Context | Not Started | No Tests | Needs Verification | Construction context hook remains future work. Generic bytes alone do not populate unusual-storage schema-v1. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests` | Packet / Runtime Artifact Target | Partial | Unit Tested reader only | Needs Verification | Generic hook can observe clear bytes later, but no enabled observer, scenario context, or artifact writer exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` | `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests` | Packet / Runtime Artifact Target | Partial | Unit Tested reader only | Needs Verification | Generic hook can observe clear bytes later, but no Java artifact generation exists. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | `AionServerPacket.write` source and UOW-1346 placement audit | Adds disabled observer shell at the documented byte boundary. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime artifacts. |

## Remaining Risks

- Java compile validation is blocked locally by missing Maven/Java 25 tooling.
- Enabled observer implementation is not present.
- Scenario context registration is not present.
- Artifact writer/JSON serializer is not present.
- Observer implementations must copy bytes immediately and avoid mutating buffer position/limit.
- Observer failures must be isolated before any non-no-op implementation is enabled.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 generic observer shell plus 2 capture package types
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java compile validation, enabled observer implementation, scenario context writer, artifact writer, item blob decoder, live unusual-storage adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add the unusual-storage construction context registration seam in `ItemPacketService.sendStorageUpdatePacket` and a no-op `PetFeedUnusualStorageArtifactCapture` shell, still disabled by default and without artifact writing.
