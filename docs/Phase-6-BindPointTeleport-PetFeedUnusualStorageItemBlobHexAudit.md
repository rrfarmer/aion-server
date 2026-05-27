# Phase 6 - Pet Feed Unusual Storage Item Blob Hex Retention Audit

Date: May 27, 2026
Unit of Work: UOW-1377

## Scope

This is a read-only audit of where future unusual-storage artifacts can retain compact item-blob hex. It does not add blob hex fields, serialize JSON, write files, mutate packet buffers, mutate storage, dispatch packets, or enable capture by default.

Java remains the source of truth. Source breadcrumbs reviewed in this unit:

- `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob`
- `com.aionemu.gameserver.network.aion.iteminfo.ItemBlobEntry`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- representative `com.aionemu.gameserver.network.aion.iteminfo.*BlobEntry` implementations

## Findings

`ItemInfoBlob.writeMe(ByteBuffer)` writes a two-byte payload-size prefix, then each `ItemBlobEntry` writes one byte of entry id followed by that entry's payload bytes. `ItemInfoBlob.size()` returns the sum of every entry payload size plus one id byte per entry, so the total serialized blob byte count is `2 + blob.size()`.

The packet-body path is byte-perfect because `AionServerPacket.write(...)` exposes the already-written clear frame. However, extracting only the item blob from `SM_WAREHOUSE_ADD_ITEM.bodyHex` requires parsing through:

- warehouse route fields;
- item object id and template id;
- one unknown item-info byte;
- Java UTF-16 `writeS(itemTemplate.getL10n())` with a null terminator;
- the item-blob length prefix and payload;
- the trailing equipment-slot short.

This is feasible, but it would duplicate packet parsing logic inside the capture seam and create another place where string-length or offset mistakes could corrupt artifact interpretation.

The dedicated-blob-buffer path is simpler: during observer-side `SM_WAREHOUSE_ADD_ITEM` capture, build the same first-item `ItemInfoBlob`, allocate a heap `ByteBuffer` with `2 + blob.size()` bytes, call `blob.writeMe(buffer)`, flip it, and copy compact hex from offset `0` through `limit`. This avoids mutating the packet clear-frame duplicate and produces exactly the blob wire format including the two-byte size prefix.

The dedicated-buffer path is still not full proof of packet bytes because it reserializes from live `Item` state after `writeImpl(...)` has already run. Several entry writers read mutable or time-sensitive fields:

- `GeneralInfoBlobEntry` reads count, creator, expiration remaining seconds, temporary exchange remaining time, and cleanup/seal static-data flags.
- `EnchantInfoBlobEntry` reads identification-dependent socket/bonus values, manastones, godstone, dye color/time, idian stone, tempering/plume stats, amplification, and buff skill.
- `ConditioningInfoBlobEntry`, `PremiumOptionInfoBlobEntry`, `PolishInfoBlobEntry`, and `WrapInfoBlobEntry` read live item state.
- Slot-oriented entries read template slot metadata and sometimes current color/equipment state.
- `BonusInfoBlobEntry` reads stat-function modifier values when template modifiers add bonus entries.

Because of those mutable/time-sensitive reads, future artifacts should label dedicated-buffer blob hex as an observer-time reserialization unless it is objectively compared with the blob slice inside `PacketSnapshot.bodyHex`.

## Recommendation

Use a two-step implementation:

1. Add disabled `itemBlob.hex` retention using a dedicated observer-time blob buffer sized as `2 + blob.size()`. Keep it no-output until JSON/file writing is added later.
2. Add a later verifier that locates the serialized blob inside `SM_WAREHOUSE_ADD_ITEM.bodyHex` and records whether the dedicated-buffer hex matches the actual packet slice.

Do not claim verified blob parity from the dedicated buffer alone.

## Migration Parity Table - UOW-1377

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Helper | Partial | Manual Only | Needs Verification | Read-only audit confirms `writeMe` emits a two-byte size prefix plus ordered entry id/payload bytes. Future dedicated-buffer hex should allocate `2 + blob.size()`. No byte retention implemented in this unit. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemBlobEntry` | C# item-blob entry helpers inside inventory/warehouse packet serializers | Serialization Entry Base | Partial | Manual Only | Needs Verification | Entry base writes one-byte Java entry id before each payload. Reflection is not used; future C# parity must preserve entry ids and entry ordering. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Packet | Partial | Manual Only | Needs Verification | Packet-body extraction is byte-perfect but requires parsing through variable UTF-16 item name and trailing equipment-slot fields. Dedicated-buffer hex is simpler but reserializes observer-time state. |
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Current `itemBlob.hex` is still a placeholder. Future implementation should retain dedicated-buffer hex first and later compare it against a body slice before parity can be verified. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` | C# general item-blob writer | Serialization Entry | Partial | Manual Only | Needs Verification | Reads mutable/time-sensitive count, creator, expiration, temporary exchange, and cleanup/seal static-data inputs. Reserialized hex can drift if item state changes between packet write and capture-side reserialization. |
| `com.aionemu.gameserver.network.aion.iteminfo.EnchantInfoBlobEntry` | C# enchant item-blob writer | Serialization Entry | Partial | Manual Only | Needs Verification | Reads identification state, item skin, sockets, stones, dye timing/color, idian, tempering/plume stats, amplification, and buff skill. Precision/rounding is integer-based; timing drift remains possible for dye seconds. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Read-only audit | Java item-blob and warehouse-add source review | Documents safe blob-hex retention options and reserialization risks. | Source inspection only. | No byte retention implementation, no Java compile, no runtime artifact, no C# reader validation, and no packet-slice comparison. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Dedicated-buffer blob hex would re-read mutable live `Item` fields after packet serialization and must not be treated as verified packet bytes by itself.
- Packet-body blob slicing is more authoritative but requires careful parsing of Java UTF-16 `writeS` output and the item-blob length prefix.
- Date/time handling remains sensitive for expiration and dye remaining seconds.
- Serialization differences around `STAT_BONUSES`, plume tempering stats, cleanup/seal static data, idian values, and identification-dependent premium/enchant fields still need C# comparison.
- JSON serialization, file output, C# artifact reader/schema validation, and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 read-only item-blob hex retention audit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java compile validation, item blob hex implementation, packet-slice verifier, Java runtime artifacts, JSON writer, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add disabled `itemBlob.hex` snapshot retention using an observer-time dedicated `ByteBuffer` sized as `2 + ItemInfoBlob.size()`. Copy compact uppercase hex from the flipped blob buffer, feed it into the existing schema DTO placeholder, and keep JSON/file output disabled.
