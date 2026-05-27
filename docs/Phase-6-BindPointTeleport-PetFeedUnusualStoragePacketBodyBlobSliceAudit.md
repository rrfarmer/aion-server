# Phase 6 - Pet Feed Unusual Storage Packet Body Blob Slice Audit

Date: May 27, 2026
Unit of Work: UOW-1379

## Scope

This is a read-only audit of how a future verifier can locate the serialized `ItemInfoBlob` inside `SM_WAREHOUSE_ADD_ITEM.bodyHex` and compare it with observer-time `itemBlob.hex`. It does not implement the verifier, serialize JSON, write files, mutate packet buffers, mutate storage, dispatch packets, or enable capture by default.

Java remains the source of truth. Source breadcrumbs reviewed in this unit:

- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.PacketWriteHelper`
- `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob`
- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`

## Body Offset Map

`PacketSnapshot.bodyHex` starts after the two-byte packet length and five-byte opcode/static/check header, so byte offset `0` in this audit is the first `writeImpl(...)` byte.

For the current Java constructor, `SM_WAREHOUSE_ADD_ITEM` carries a singleton item list. The first-item layout is:

| Body Byte Offset | Length | Java Write | Notes |
|---:|---:|---|---|
| `0` | `1` | `writeC(warehouseType)` | Java storage id, not enum ordinal. |
| `1` | `2` | `writeH(addType.getMask())` | Big-endian `ByteBuffer.putShort`. |
| `3` | `2` | `writeH(items.size())` | Expected `1` for the unusual-storage path. |
| `5` | `4` | `writeD(item.getObjectId())` | First item object id. |
| `9` | `4` | `writeD(itemTemplate.getTemplateId())` | Template id. |
| `13` | `1` | `writeC(0)` | Java comment: item info type marker. |
| `14` | variable | `writeS(itemTemplate.getL10n())` | UTF-16 Java chars followed by a UTF-16 null char. |
| `blobStart` | `2` | `ItemInfoBlob.writeMe`: `writeH(size())` | Payload size prefix, excluding this two-byte prefix. |
| `blobStart + 2` | `payloadSize` | `ItemBlobEntry.writeMe(...)` sequence | One-byte entry id plus payload per entry. |
| `blobStart + 2 + payloadSize` | `2` | `writeH((int) (item.getEquipmentSlot() & 0xFFFF))` | Trailing equipment slot. |

The name byte length is:

```text
nameBytes = (localizedName == null ? 1 : localizedName.length() + 1) * 2
blobStart = 14 + nameBytes
payloadSize = unsignedShort(bodyBytes[blobStart], bodyBytes[blobStart + 1])
blobEnd = blobStart + 2 + payloadSize
equipmentSlotStart = blobEnd
```

For compact hex strings, substring positions are byte offsets multiplied by two:

```text
blobHex = bodyHex.substring(blobStart * 2, blobEnd * 2)
expectedMatch = blobHex.equals(itemBlob.hex)
```

## Verifier Requirements

A future verifier should:

- decode compact hex into bytes or use pair-index reads rather than parsing formatted dumps;
- reject odd-length, empty, or non-hex `bodyHex`;
- confirm `itemCount == 1` for this unusual-storage capture path before using the first-item offset recipe;
- derive `nameBytes` from the captured encode-time localized name, accounting for Java `null` as one UTF-16 null char;
- read the blob payload size as unsigned big-endian;
- ensure `blobEnd + 2 <= bodyLength` before slicing, because the equipment-slot short follows the blob;
- compare the packet-body blob slice against `itemBlob.hex`;
- report `Needs Verification` when the slice cannot be derived, rather than assuming the dedicated-buffer reserialization is byte-identical.

## Important Boundary

The packet-body slice is the authoritative source for serialized blob bytes because it comes from the already-written clear frame. The current `itemBlob.hex` remains useful, but it is observer-time reserialization from live `Item` state. Verified blob-byte parity requires a deterministic match between the packet-body slice and `itemBlob.hex`, followed by C# reader/writer comparison.

## Migration Parity Table - UOW-1379

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Packet | Partial | Manual Only | Needs Verification | Read-only audit maps exact body offsets for the singleton-item unusual-storage packet and the future item-blob slice verifier. No verifier or byte comparison implemented. |
| `com.aionemu.gameserver.network.PacketWriteHelper` | C# packet write helpers | Packet Utility | Partial | Manual Only | Needs Verification | Confirms Java `writeS` writes UTF-16 chars plus a UTF-16 null char and primitive writes use `ByteBuffer` big-endian ordering. C# string/endianness parity still needs runtime comparison. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Helper | Partial | Manual Only | Needs Verification | Blob slice length is `2 + unsigned payloadSize`, where payload size comes from the two-byte `ItemInfoBlob.writeMe` prefix. |
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Current `bodyHex` and `itemBlob.hex` fields provide inputs for a future verifier, but this unit does not implement comparison or promote parity. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Read-only audit | Java packet/write-helper/item-blob source review | Documents exact body offset math for a future blob-slice verifier. | Source inspection only. | No verifier implementation, no Java compile, no runtime artifact, no C# reader validation, and no Java/C# byte comparison. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- The offset recipe assumes the current singleton-item constructor path used by unusual-storage capture.
- Captured localized name must match Java `writeS(itemTemplate.getL10n())` at encode time; stale or null mismatches must fail closed.
- Date/time and mutable item fields can still make observer-time `itemBlob.hex` differ from the packet-body slice.
- C# endianness, UTF-16 string writing, `STAT_BONUSES`, plume tempering stats, cleanup/seal static data, idian values, and identification-dependent premium/enchant fields still need runtime comparison.
- JSON serialization, file output, C# artifact reader/schema validation, and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 read-only packet-body blob-slice audit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java compile validation, packet-slice verifier implementation, Java runtime artifacts, JSON writer, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled packet-body blob-slice verifier inside `PetFeedUnusualStorageArtifactCapture`: derive the first-item blob slice from `PacketSnapshot.bodyHex` and the encode-time localized name, compare it to `itemBlob.hex`, and record a tri-state result without throwing, writing files, or enabling capture by default.
