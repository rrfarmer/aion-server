# Phase 6 - Pet Feed Unusual Storage Packet Body Blob Verifier

Date: May 27, 2026
Unit of Work: UOW-1380

## Scope

This unit adds a disabled diagnostic verifier that compares observer-time `itemBlob.hex` against the authoritative `SM_WAREHOUSE_ADD_ITEM.bodyHex` blob slice. It does not call fastjson2, serialize JSON, create directories, write files, mutate packet buffers, mutate storage, dispatch packets, or enable capture by default.

Java remains the source of truth. Source breadcrumbs touched in this unit:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.PacketWriteHelper`
- `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob`

## Implementation

`PetFeedUnusualStorageArtifactCapture` now records `itemBlob.packetBodyVerification` in the schema DTO as one of:

- `matched`
- `mismatched`
- `unavailable`

For `SM_WAREHOUSE_ADD_ITEM`, `PacketSnapshot.from(...)` now derives the packet-body blob slice from compact `bodyHex` and the observer-time localized item name:

```text
nameBytes = ((localizedName == null ? 0 : localizedName.length()) + 1) * 2
blobStart = 14 + nameBytes
payloadSize = unsignedShort(bodyHex, blobStart)
blobEnd = blobStart + 2 + payloadSize
packetBlobHex = bodyHex.substring(blobStart * 2, blobEnd * 2)
```

The verifier fails closed to `unavailable` when hex is malformed, item count is not `1`, offsets do not fit, required capture inputs are missing, or any runtime exception occurs. A length or byte mismatch returns `mismatched`.

## Boundaries Preserved

- Capture remains disabled unless config explicitly enables it.
- No global observer behavior changed.
- No JSON serialization or file output was added.
- No C# reader or serializer behavior changed.
- No warehouse-add byte comparison was enabled.
- `matched` is only a Java self-check between two Java capture views; it is not C# parity verification.

## Migration Parity Table - UOW-1380

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Adds fail-closed `itemBlob.packetBodyVerification` diagnostic comparing packet-body slice to observer-time `itemBlob.hex`. No JSON/file output, runtime artifact, C# reader validation, or Java/C# byte comparison exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Packet | Partial | Manual Only | Needs Verification | Verifier uses audited singleton-item body layout. It fails closed if body shape does not match item count `1` or offset bounds. |
| `com.aionemu.gameserver.network.PacketWriteHelper` | C# packet write helpers | Packet Utility | Partial | Manual Only | Needs Verification | Verifier uses Java UTF-16 name-length math from `writeS`; no C# string writer validation is performed. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Helper | Partial | Manual Only | Needs Verification | Verifier reads the Java two-byte blob payload-size prefix from packet body and compares the resulting slice to observer-time `ItemInfoBlob.writeMe` hex. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java capture, warehouse-add, packet helper, and item-blob source review | Adds passive fail-closed Java self-check metadata without JSON/file output. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime artifact, no C# reader validation, and no Java/C# byte comparison. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Verifier behavior is source-reviewed only and not runtime-tested.
- `matched` confirms only that Java observer-time blob reserialization matches the Java packet-body slice for that capture. It does not verify C# parity.
- The verifier assumes the current singleton-item unusual-storage `SM_WAREHOUSE_ADD_ITEM` path.
- Date/time and mutable item fields can still produce `mismatched` if observer-time reserialization drifts from packet bytes.
- C# endianness, UTF-16 string writing, `STAT_BONUSES`, plume tempering stats, cleanup/seal static data, idian values, and identification-dependent premium/enchant fields still need runtime comparison.
- JSON serialization, file output, C# artifact reader/schema validation, and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled packet-body blob-slice verifier
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java compile validation, runtime artifact validation, Java runtime artifacts, JSON writer, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a read-only audit for fastjson2 writer activation now that packet body/canonical hex, item-blob hex, and packet-body self-check metadata exist in the DTO shell. Confirm field ordering, file naming, atomic write, and queue drain behavior before adding actual JSON/file output.
