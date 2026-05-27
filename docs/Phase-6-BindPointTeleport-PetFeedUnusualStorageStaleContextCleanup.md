# Phase 6 - Pet Feed Unusual Storage Stale Context Cleanup

Date: May 27, 2026
Unit of Work: UOW-1351

## Scope

This unit adds bounded stale-context cleanup and timestamp metadata to the disabled unusual-storage artifact capture registry.

Java source touched:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`

## What Changed

- Added a 30-second maximum pending-context age.
- Prunes expired pending contexts before enqueueing a new context.
- Prunes expired pending contexts before observing serialized packet metadata.
- Adds registration and completion timestamps to the in-memory `ArtifactSnapshot`.
- Keeps the pending queue bounded to four contexts per player.

## Boundaries Preserved

- No capture enable path was added.
- No observer installation was added.
- No artifact file writing was added.
- No packet bytes are copied or retained.
- No JSON serializer was added.
- No item/blob decoded-entry extraction was added.
- No live C# unusual-storage dispatch behavior was enabled.

## Validation

- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Validation was blocked because `mvn` is not available on PATH and no Maven wrapper exists in the repository (`mvnw`/`mvnw.cmd` absent).
- No Java runtime artifacts were generated.
- No .NET tests were required for this Java-only disabled cleanup slice.

## Migration Parity Table - UOW-1351

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Added stale-context pruning and registration/completion timestamp metadata. Missing enable/config path, installed observer, file writer, byte copying, decoded item/blob output, and runtime validation. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests` | Packet Target | Partial | Unit Tested reader only | Needs Verification | Stale pruning reduces risk of pairing old construction context to a later warehouse-add packet, but packet route fields still are not inspected. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`; future artifacts | Packet Target | Partial | Unit Tested reader only | Needs Verification | Stale pruning reduces risk of pairing old construction context to a later cube-update packet, but body fields still are not decoded from Java bytes. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java packet order and capture source review | Adds disabled stale-context pruning and timestamp metadata for the correlated packet pair. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime artifacts; no enabled observer; no byte/schema comparison. |

## Remaining Risks

- Java compile validation is blocked locally by missing Maven/Java 25 tooling.
- The 30-second age bound is a capture safety choice, not verified Java gameplay behavior.
- Matching still uses packet class and active player only; packet route fields remain uninspected.
- Snapshot fields are not externally observable because capture remains disabled and no writer exists.
- No raw bytes or item/blob decoded fields are captured, so serialization parity remains unverified.
- Threading behavior remains unproven under real connection queue serialization.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled stale-context cleanup/timestamp slice
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: Java compile validation, enable/config path, observer installation, artifact writer, byte copying, item blob decoder, live unusual-storage adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Perform a read-only Java artifact writer/config audit before adding output: identify existing JSON dependencies, server config conventions, safe output directories, and whether a bounded async writer already exists. Keep capture disabled until the audit and Java compile validation are available.
