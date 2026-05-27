# Phase 6 - Pet Feed Unusual Storage Item Blob Snapshot Shape

Date: May 27, 2026
Unit of Work: UOW-1356

## Scope

This unit adds a disabled no-output item-blob metadata shape to the Java unusual-storage capture registry. It does not enable capture, install the packet observer, write files, copy raw bytes, or promote warehouse-add byte parity.

Java source touched:

- `com.aionemu.gameserver.network.aion.iteminfo.ItemBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob`
- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`

## What Changed

- `ItemBlobEntry` now exposes its `ItemBlobType` through `getType()`.
- `ItemInfoBlob` now exposes read-only metadata projection helpers:
  - `getBlobEntryMetadata()`
  - `getFullBlobEntryMetadata(Player, Item)`
  - `ItemBlobEntryMetadata`
- `PetFeedUnusualStorageArtifactCapture` now stores a construction-time `ItemBlobSnapshot` inside the no-output capture snapshot.
- The snapshot records:
  - total blob payload size
  - ordered entry names
  - ordered entry ids
  - each entry payload size

## Important Parity Boundary

This is construction-time metadata only. Java `SM_WAREHOUSE_ADD_ITEM.writeItemInfo` still builds and writes `ItemInfoBlob.getFullBlob(player, item)` at encode time, so future runtime artifacts must still record encode-time decoded blob metadata before warehouse-add byte comparison can be considered.

The construction-time shape is intentionally useful as an early schema placeholder and stale-state detector, not as proof of packet parity.

## Boundaries Preserved

- Capture remains disabled unless config explicitly enables it.
- No observer is installed globally by this unit.
- `onSnapshotReady` remains a no-op.
- No raw packet bytes are retained.
- No JSON writer or output directory creation was added.
- No C# reader or serializer behavior changed.
- No warehouse-add byte comparison was enabled.

## Validation

- Ran `git diff --check`.
- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Java validation remains blocked locally because `mvn` is not available on PATH and no Maven wrapper exists.
- No Java runtime artifacts were generated.

## Migration Parity Table - UOW-1356

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.iteminfo.ItemBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo` blob writer helpers | Serialization Entry Base | Partial | Manual Only | Needs Verification | Adds `getType()` so metadata can expose Java entry ids/names without reflection. No payload serialization changed. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Helper | Partial | Manual Only | Needs Verification | Adds metadata projection for ordered entry names/ids/payload sizes. Does not change `writeMe` or full-blob construction order. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobEntryMetadata` | future C# artifact decoded blob metadata DTO | DTO | Partial | Manual Only | Needs Verification | New Java metadata DTO records entry name, entry id, and payload size only. Template-derived and dynamic payload fields are still not decoded. |
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Stores construction-time blob metadata in the no-output snapshot while capture remains disabled and `onSnapshotReady` remains no-op. Encode-time blob metadata is still missing. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java `ItemInfoBlob` source review | Adds a no-output metadata projection without changing serializer writes. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime artifact, JSON schema, encode-time metadata capture, or byte comparison validation. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Construction-time metadata can differ from encode-time metadata if the item/template state mutates before packet serialization.
- Payload fields are not decoded yet; only entry ids, names, and payload sizes are captured.
- Future writer work must still add deterministic JSON, bounded queueing, byte copying policy, and output safety.
- C# warehouse-add byte comparison must remain guarded until Java encode-time artifacts exist and known item-blob serializer gaps are closed.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled no-output metadata projection/snapshot shape
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java compile validation, encode-time item-blob metadata capture, artifact writer, byte copying, warehouse-add comparison, C# serializer gap closure, live unusual-storage adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Move the item-blob metadata snapshot from construction-time placeholder toward encode-time truth: add a disabled observer-side metadata builder for `SM_WAREHOUSE_ADD_ITEM` that can recompute ordered `ItemInfoBlob` metadata from the live packet/item context without writing files or retaining raw bytes. If private packet fields block that safely, perform a read-only access-boundary audit first.
