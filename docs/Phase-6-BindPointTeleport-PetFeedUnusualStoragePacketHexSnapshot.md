# Phase 6 - Pet Feed Unusual Storage Packet Hex Snapshot

Date: May 27, 2026
Unit of Work: UOW-1376

## Scope

This unit adds disabled packet hex snapshot fields for future unusual-storage JSON artifacts. It does not call fastjson2, serialize JSON, create directories, write files, mutate storage, dispatch packets, or enable capture by default.

Java remains the source of truth. Source breadcrumbs touched in this unit:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.network.aion.AionServerPacket`

## Implementation

`PacketSnapshot` now stores compact uppercase hex strings copied from the read-only clear-frame duplicate passed by `AionServerPacket.write(...)`:

- `clearFrameHex`: bytes from offset `0` through `clearFrame.limit()`;
- `bodyHex`: bytes from offset `7` through `clearFrame.limit()`;
- `canonicalPayloadHex`: currently equal to `bodyHex`.

The copy uses absolute indexed `ByteBuffer.get(i)` reads, so it does not change the observer duplicate position and does not mutate the packet buffer before encryption.

The schema DTO packet section now emits `bodyHex` and `canonicalPayloadHex` from the retained packet snapshot. Full `clearFrameHex` is retained internally for diagnostics but is not part of the current schema-v1 packet DTO.

No JSON/file output was added.

## Migration Parity Table - UOW-1376

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Adds compact clear-frame/body/canonical hex fields to `PacketSnapshot` and feeds schema DTO packet byte fields. No JSON serialization, file output, runtime artifact, or C# reader validation exists. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | future C# packet writer/capture equivalent | Packet Serialization Base | Partial | Manual Only | Needs Verification | Existing observer boundary provides read-only clear bytes after length stamping and before encryption. This unit relies on that boundary but does not alter packet serialization. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java packet serialization and capture source review | Adds passive compact hex snapshot fields without JSON/file output. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime artifact, no C# reader validation, and no byte comparison. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Hex copy behavior is source-reviewed only and not runtime-tested.
- `canonicalPayloadHex` is currently identical to `bodyHex`; future C# reader expectations must confirm whether additional canonicalization is needed.
- Full `clearFrameHex` is retained internally but not serialized by schema-v1 yet.
- JSON serialization, file output, C# artifact reader/schema validation, and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 2 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled packet hex snapshot slice
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 2 grouped rows
- Total blocked artifacts: Java compile validation, runtime artifact validation, JSON writer, Java runtime artifacts, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a read-only audit for item blob hex retention: inspect `ItemInfoBlob` and `ItemBlobEntry` write paths to determine where compact blob hex can be copied without reserializing inconsistently or mutating buffers.
