# Phase 6 - Pet Feed Unusual Storage Artifact Snapshot Shape

Date: May 27, 2026
Unit of Work: UOW-1350

## Scope

This unit adds an in-memory snapshot shape behind the disabled unusual-storage correlation registry. It does not write files, copy packet bytes, or enable capture.

Java source touched:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.network.aion.AionServerPacket.write`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`

## What Changed

- Added an internal `ArtifactSnapshot` shape containing:
  - Java storage type id;
  - Java storage type ordinal;
  - item object id;
  - warehouse-add packet snapshot;
  - cube-update packet snapshot.
- Added an internal `PacketSnapshot` shape containing:
  - packet index in the expected pair;
  - Java packet class name;
  - clear-frame length read from offset `0`;
  - encoded opcode read from offset `2`;
  - remaining bytes at observer callback time.
- Updated the correlation registry to build the snapshot after observing `SM_WAREHOUSE_ADD_ITEM` then `SM_CUBE_UPDATE`.
- Added an explicit no-op `onSnapshotReady(...)` boundary for the future artifact writer.

## Boundaries Preserved

- No artifact writing was added.
- No raw packet bytes are copied or retained.
- No JSON serializer was added.
- No config flag or runtime enable path was added.
- No observer installation was added.
- No item/blob decoded-entry extraction was added.
- No live C# unusual-storage dispatch behavior was enabled.

## Validation

- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Validation was blocked because `mvn` is not available on PATH and no Maven wrapper exists in the repository (`mvnw`/`mvnw.cmd` absent).
- No Java runtime artifacts were generated.
- No .NET tests were required for this Java-only disabled snapshot shape.

## Migration Parity Table - UOW-1350

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Added disabled in-memory snapshot shape and no-op writer boundary. Missing enable/config path, installed observer, file writer, byte copying, decoded item/blob output, and runtime validation. |
| `com.aionemu.gameserver.network.aion.AionServerPacket.write` | future Java runtime artifact producer; C# guarded artifact readers | Serialization Hook | Partial | Manual Only | Needs Verification | Snapshot reads length/opcode metadata from the clear frame supplied by the existing observer hook. No bytes are retained. Maven compile unavailable locally. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests` | Packet Target | Partial | Unit Tested reader only | Needs Verification | Snapshot records packet class name, pair index, clear-frame length, encoded opcode, and remaining bytes only. Warehouse route fields and item blob bytes are still not captured. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`; future artifacts | Packet Target | Partial | Unit Tested reader only | Needs Verification | Snapshot records packet class name, pair index, clear-frame length, encoded opcode, and remaining bytes only. Action/actionValue body fields are still not decoded from Java bytes. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | `AionServerPacket.write` source and Java packet class review | Adds disabled in-memory snapshot metadata for the correlated packet pair. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime artifacts; no enabled observer; no byte/schema comparison. |

## Remaining Risks

- Java compile validation is blocked locally by missing Maven/Java 25 tooling.
- Snapshot fields are not externally observable because capture remains disabled and no writer exists.
- Encoded opcode metadata is source-derived from the clear frame but has not been runtime-verified.
- Matching still uses packet class and active player only; future work must add scenario ids or route checks before writing artifacts.
- No raw bytes or item/blob decoded fields are captured, so serialization parity remains unverified.
- Threading behavior remains unproven under real connection queue serialization.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled in-memory snapshot shape
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java compile validation, enable/config path, observer installation, artifact writer, byte copying, item blob decoder, live unusual-storage adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add stale-context cleanup and stronger route checks before any artifact writer: record registration time and expected storage id/order, prune old pending contexts, and only complete snapshots when the packet pair still belongs to the guarded scenario. Keep output disabled until compile validation is available.
