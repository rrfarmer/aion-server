# Phase 6 - Pet Feed Unusual Storage Byte Retention Audit

Date: May 27, 2026
Unit of Work: UOW-1375

## Scope

This is a read-only audit of raw/canonical byte retention boundaries for future unusual-storage artifacts. It does not retain bytes, serialize JSON, write files, mutate packet buffers, mutate storage, dispatch packets, or enable capture by default.

Java remains the source of truth. Source breadcrumbs reviewed in this unit:

- `com.aionemu.gameserver.network.aion.AionServerPacket`
- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.commons.network.packet.BaseServerPacket`
- `com.aionemu.commons.utils.NetworkUtils`

Reference schema:

- `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageRuntimeArtifactSchema.md`

## Findings

`AionServerPacket.write(...)` currently gives the capture observer a read-only duplicate after:

1. reserving the length field;
2. writing the encoded opcode/static bytes/check bytes;
3. running `writeImpl(...)`;
4. flipping the buffer;
5. stamping packet length at bytes `0..1`;
6. before slicing/encrypting the buffer.

This is the correct boundary for future clear-byte retention. The duplicate passed to `onPacketSerialized(...)` is read-only and its position/limit are independent of the mutable packet buffer. Future capture code can safely read bytes using absolute indexed `ByteBuffer.get(index)` calls without changing position or affecting encryption.

## Future Byte Fields

Recommended future retention fields:

- `clearFrameHex`: full clear frame from index `0` inclusive to `clearFrame.limit()` exclusive, including length and opcode/static/check bytes.
- `bodyHex`: packet body after the two-byte length and five-byte opcode/static/check header, starting at index `7`.
- `canonicalPayloadHex`: same as `bodyHex` unless a packet-specific canonicalization rule is introduced later.

For schema-v1 packets, `bodyHex` and `canonicalPayloadHex` are the fields needed by the C# reader. Keeping full `clearFrameHex` as internal/debug metadata may help diagnose length/opcode mistakes, but it should be documented separately from canonical payload parity.

## Hex Formatting

`NetworkUtils.toHex(ByteBuffer, int, int)` already exists, but it returns a formatted dump with offsets, spaces, line breaks, and ASCII text. That is useful for logs but not ideal for JSON artifact parity fields. Future artifact hex should use compact uppercase hex with no spaces or offsets, generated from absolute reads over the read-only duplicate.

## Safety Rules

Future byte-copy code should:

- avoid `clearFrame.get(byte[])` unless it duplicates first, because relative reads would alter the duplicate position;
- prefer absolute `clearFrame.get(i)` over `duplicate().get(...)`;
- clamp body start to `min(7, clearFrame.limit())`;
- treat malformed/truncated frames as empty body/canonical payload and record a note;
- avoid keeping a reference to the mutable `ByteBuffer`;
- store copied strings or byte arrays in `PacketSnapshot`, not the buffer itself.

## Migration Parity Table - UOW-1375

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.AionServerPacket` | future C# packet writer/capture equivalent | Packet Serialization Base | Partial | Manual Only | Needs Verification | Read-only audit confirms observer sees clear frame after length stamping and before encryption. Future byte retention must use absolute reads and must not mutate buffers. |
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Current `PacketSnapshot` stores lengths/opcode only. Future fields should retain compact full-frame/body/canonical hex strings copied from the observer duplicate. |
| `com.aionemu.commons.network.packet.BaseServerPacket` | future C# packet base | Packet Utility | Partial | Manual Only | Needs Verification | Reviewed write helpers and buffer ownership. Packet subclasses write into shared `buf`; capture must not retain or mutate it. |
| `com.aionemu.commons.utils.NetworkUtils` | future C# hex utility / artifact helper | Utility | Not Started | Manual Only | Needs Verification | Existing `toHex` is formatted for logs, not compact JSON artifact fields. Future artifact helper should use a compact deterministic hex format. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Read-only audit | Java packet serialization and utility source review | Documents safe clear-frame/body/canonical byte retention boundaries. | Source inspection only. | No byte retention implementation, no Java compile, no runtime artifact, no C# reader validation, and no byte comparison. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- No byte retention fields are implemented yet.
- Canonical payload definition is currently identical to body hex; future C# reader expectations must confirm this.
- Truncated/malformed frame note behavior is not implemented.
- JSON serialization, file output, C# artifact reader/schema validation, and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 read-only byte retention audit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java compile validation, byte retention implementation, compact hex helper, JSON writer, Java runtime artifacts, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add disabled packet hex snapshot fields to `PacketSnapshot`: compact full clear-frame hex, body hex from offset `7`, and canonical payload hex matching body hex for now. Use absolute indexed reads over the read-only duplicate, and do not serialize JSON or write files yet.
