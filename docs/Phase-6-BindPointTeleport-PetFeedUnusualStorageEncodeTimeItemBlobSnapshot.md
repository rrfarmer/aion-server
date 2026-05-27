# Phase 6 - Pet Feed Unusual Storage Encode-Time Item Blob Snapshot

Date: May 27, 2026
Unit of Work: UOW-1357

## Scope

This unit moves the disabled unusual-storage item-blob snapshot closer to Java encode-time truth. It adds a passive metadata projection on `SM_WAREHOUSE_ADD_ITEM` and lets the packet observer snapshot recompute item-blob entry metadata when the serialized packet is observed.

Java source touched:

- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`

Java dependency used:

- `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob`

## What Changed

- `SM_WAREHOUSE_ADD_ITEM` now exposes `getFirstItemInfoBlob()`, which recomputes `ItemInfoBlob.getFullBlob(player, item)` for the first item carried by the packet.
- `PetFeedUnusualStorageArtifactCapture.PacketSnapshot` now has an optional `observedItemBlob` field.
- `PacketSnapshot.from(...)` now fills `observedItemBlob` only when the observed packet is `SM_WAREHOUSE_ADD_ITEM`.
- The existing construction-time item-blob snapshot remains in the capture context, giving future artifacts a way to detect construction-time versus observer-time drift.

## Important Parity Boundary

This still does not decode individual blob payload fields or retain serialized bytes. The observer-side snapshot recomputes entry metadata after `writeImpl` has already built the packet, but it is still a metadata projection rather than a byte-level decode of the already-written buffer.

Warehouse-add byte comparison must remain guarded until Java runtime artifacts include raw/canonical packet bytes and enough decoded dynamic fields to resolve the known C# serializer gaps.

## Boundaries Preserved

- Capture remains disabled by default.
- No global observer installation was added.
- `onSnapshotReady` remains a no-op.
- No JSON writer, output directory creation, bounded writer queue, or raw byte retention was added.
- No C# reader, serializer, or comparison behavior changed.
- No parity was promoted to verified.

## Validation

- Ran `git diff --check`.
- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Java validation remains blocked locally because `mvn` is not available on PATH and no Maven wrapper exists.
- No Java runtime artifacts were generated.

## Migration Parity Table - UOW-1357

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Packet | Partial | Manual Only | Needs Verification | Adds a passive first-item blob metadata projection for observer capture. Packet serialization order and writes are unchanged. No runtime byte comparison. |
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Observer-side packet snapshots now carry optional warehouse-add item-blob metadata while capture remains disabled and no-op. No writer, raw bytes, or C# consumption. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Helper / Dependency | Partial | Manual Only | Needs Verification | Reused to recompute observer-time entry names/ids/payload sizes. Payload fields still are not decoded and known serializer gaps remain. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java packet/item-info source review | Adds disabled observer-side metadata projection without changing packet writes. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime observer validation, artifact output, raw byte retention, or Java/C# byte comparison. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- `observedItemBlob` is recomputed from packet-held item state and is not a parser over the already-written packet buffer.
- Metadata still includes only entry names, ids, and payload sizes; dynamic payload fields remain missing.
- Raw packet bytes, deterministic JSON output, and bounded writer queue are still unimplemented.
- C# warehouse-add byte comparison remains guarded by known item-blob serializer gaps.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled observer-side metadata projection
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: Java compile validation, raw/canonical artifact bytes, item-blob payload decoder, artifact writer, C# serializer gap closure, warehouse-add byte comparison, live unusual-storage adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a read-only payload-field audit for the remaining item-blob entries needed by unusual-storage artifacts, starting with `GeneralInfoBlobEntry`, `CompositeItemBlobEntry`, and `EnchantInfoBlobEntry`, so the future Java artifact writer knows exactly which dynamic payload fields must be serialized before C# warehouse-add byte comparison can be enabled.
