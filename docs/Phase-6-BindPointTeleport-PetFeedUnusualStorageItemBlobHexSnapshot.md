# Phase 6 - Pet Feed Unusual Storage Item Blob Hex Snapshot

Date: May 27, 2026
Unit of Work: UOW-1378

## Scope

This unit adds disabled item-blob hex retention inside the Java unusual-storage artifact snapshot. It does not call fastjson2, serialize JSON, create directories, write files, mutate packet buffers, mutate storage, dispatch packets, or enable capture by default.

Java remains the source of truth. Source breadcrumbs touched in this unit:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob`
- `com.aionemu.gameserver.network.aion.iteminfo.ItemBlobEntry`

## Implementation

`ItemBlobSnapshot` now retains compact uppercase hex for the serialized `ItemInfoBlob` shape. The helper allocates a dedicated heap `ByteBuffer` sized as `2 + ItemInfoBlob.size()`, calls `ItemInfoBlob.writeMe(buffer)`, flips the buffer, and copies compact hex through the existing absolute-read `compactHex(...)` helper.

The schema DTO `itemBlob.hex` field now comes from the retained snapshot instead of an empty placeholder. The artifact notes explicitly state that this hex is observer-time reserialization until a future packet-body slice verifier compares it with the actual `SM_WAREHOUSE_ADD_ITEM` body slice.

The retained `size` field remains `ItemInfoBlob.size()`, matching Java's payload size excluding the two-byte size prefix. The `hex` field includes the two-byte size prefix because that is what Java writes on the wire before the entry id/payload sequence.

## Boundaries Preserved

- Capture remains disabled unless config explicitly enables it.
- No global observer behavior changed.
- No JSON serialization or file output was added.
- No C# reader or serializer behavior changed.
- No packet-slice comparison was added.
- No parity was promoted to verified.

## Migration Parity Table - UOW-1378

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Adds `ItemBlobSnapshot.hex` and feeds schema `itemBlob.hex` from a dedicated observer-time blob buffer. No JSON/file output, runtime artifact, C# reader validation, or packet-body slice verifier exists. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Helper | Partial | Manual Only | Needs Verification | Existing `writeMe` is reused to serialize the dedicated blob buffer with the Java two-byte size prefix. This reserializes live item state and must later be compared against packet body bytes. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemBlobEntry` | C# item-blob entry helpers inside inventory/warehouse packet serializers | Serialization Entry Base | Partial | Manual Only | Needs Verification | Existing Java entry id/payload write path is reused through `ItemInfoBlob.writeMe`. Entry ordering and ids still need C# runtime comparison. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java `ItemInfoBlob` and capture source review | Adds passive dedicated-buffer item-blob hex retention without JSON/file output. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime artifact, no C# reader validation, no packet-body slice comparison, and no Java/C# byte comparison. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- `itemBlob.hex` is observer-time reserialization from live `Item` state, not yet an extracted slice from the already-written packet body.
- Date/time handling can still drift for expiration and dye remaining seconds.
- Serialization differences around `STAT_BONUSES`, plume tempering stats, cleanup/seal static data, idian values, and identification-dependent premium/enchant fields still need C# comparison.
- `ByteBuffer.allocate(2 + blob.size())` depends on Java `getSize()` implementations matching their writes; source audit supports this, but runtime validation is still missing.
- JSON serialization, file output, C# artifact reader/schema validation, and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled item-blob hex snapshot slice
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: Java compile validation, runtime artifact validation, packet-slice verifier, Java runtime artifacts, JSON writer, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a read-only audit for locating the `ItemInfoBlob` slice inside `SM_WAREHOUSE_ADD_ITEM.bodyHex`, including exact offsets for warehouse route fields, object/template ids, the unknown item-info byte, Java UTF-16 `writeS` name bytes, the two-byte blob size prefix, and trailing equipment-slot bytes.
