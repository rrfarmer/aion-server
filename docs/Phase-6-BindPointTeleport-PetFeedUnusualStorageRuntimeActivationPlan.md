# Phase 6 - Pet Feed Unusual Storage Runtime Activation Plan

Date: May 27, 2026
Unit of Work: UOW-1383

## Scope

This is a read-only runtime activation plan for generating Java unusual-storage parity artifacts after Java 25/Maven tooling is available. It does not change source code, enable capture by default, write artifacts in this environment, mutate storage, dispatch packets, or start C# reader work.

Java remains the source of truth. Source breadcrumbs reviewed in this unit:

- `com.aionemu.gameserver.services.toypet.PetService.checkFeeding`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket`
- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig`
- `com.aionemu.gameserver.GameServer`
- `com.aionemu.gameserver.ShutdownHook`

## Required Local Config

Artifact generation must be opt-in only. Add these overrides in the local game-server override properties used for the Java run:

```properties
gameserver.petfeed.unusual_storage_artifacts.enabled=true
gameserver.petfeed.unusual_storage_artifacts.output_dir=./parity-artifacts/pet-feed-unusual-storage/java
gameserver.petfeed.unusual_storage_artifacts.max_pending_contexts_per_player=4
gameserver.petfeed.unusual_storage_artifacts.max_queued_artifacts=32
gameserver.petfeed.unusual_storage_artifacts.allowed_scenario=pet_feed_unusual_storage
```

Do not commit an override that enables capture by default.

## Startup and Shutdown Boundaries

`GameServer` calls `PetFeedUnusualStorageArtifactCapture.installIfEnabled()` after data/service startup and before NIO startup. With the flag enabled, this installs the server-packet observer and starts the writer worker. With the flag disabled, it remains a no-op.

`ShutdownHook` calls `PetFeedUnusualStorageArtifactCapture.shutdown()` before shutdown packet fanout, resetting the observer and stopping the worker shell.

## Scenario Trigger

Use the rejected-food path:

1. Start Java game-server with the local artifact config enabled.
2. Log in with a real client or controlled integration harness.
3. Ensure the character owns a pet with a FOOD function.
4. Place a candidate item in an unusual storage location: pet bag, house storage, broker, or mailbox.
5. Trigger pet feeding with an item that resolves to no acceptable food type for that pet.
6. Java `PetService.checkFeeding(...)` should call `ItemPacketService.sendItemUnlockPacket(player, item)`.
7. Java `sendItemUnlockPacket(...)` should resolve the item storage id and call `sendStorageUpdatePacket(player, storageType, item, ItemAddType.ALL_SLOT)`.
8. Java `sendStorageUpdatePacket(...)` should register the capture context, then send `SM_WAREHOUSE_ADD_ITEM` followed by `SM_CUBE_UPDATE`.

The artifact should be written only after the capture observer sees both packets for the same pending context.

## Expected Output

Default output directory:

```text
./parity-artifacts/pet-feed-unusual-storage/java
```

Expected filename shape:

```text
pet_feed_unusual_storage-player-{playerObjectId}-storage-{storageId}-{storageOrdinal}-item-{itemObjectId}-{registeredAtMillis}-{completedAtMillis}.json
```

## Required Artifact Checks

Before starting C# reader work, inspect at least one generated Java artifact and confirm:

- `schemaVersion` is `1`;
- `scenario` is `pet_feed_unusual_storage`;
- `storage.storageId` is one of the unusual Java storage ids under test;
- `storage.storageTypeOrdinal` matches Java `StorageType.ordinal()`;
- `constructionSnapshot.addType` is `ALL_SLOT`;
- `constructionSnapshot.addTypeMask` matches Java `ItemAddType.ALL_SLOT.getMask()`;
- `encodeSnapshot.item.localizedName` is present when Java `ItemTemplate.getL10n()` is non-null;
- `encodeSnapshot.itemBlob.hex` is non-empty;
- `encodeSnapshot.itemBlob.packetBodyVerification` is `matched`;
- `packets[0].javaClass` is `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`;
- `packets[0].bodyHex` and `packets[0].canonicalPayloadHex` are non-empty and equal unless a future canonicalization rule is introduced;
- `packets[1].javaClass` is `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`;
- `packets[1].bodyHex` and `packets[1].canonicalPayloadHex` are non-empty;
- no temp files remain beside the final JSON artifact after shutdown.

If `itemBlob.packetBodyVerification` is `mismatched` or `unavailable`, do not start C# byte comparison. First inspect the item name, body offset assumptions, item count, and mutable/time-sensitive item fields.

## Migration Parity Table - UOW-1383

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService` | future C# pet-feed live adapter | Service | Partial | Manual Only | Needs Verification | Runtime plan uses the Java rejected-food branch in `checkFeeding(...)` as the source scenario. No runtime artifact generated locally. Date/time delay and scheduler behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | future C# item packet/live adapter | Service | Partial | Manual Only | Needs Verification | Runtime plan follows `sendItemUnlockPacket` to `sendStorageUpdatePacket(..., ALL_SLOT)` and the warehouse-add/cube-update packet order. No Java/C# byte comparison yet. |
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Runtime activation plan documents config, scenario, output path, and artifact field checks. Writer exists but local runtime validation is blocked by missing Maven/JDK tooling. |
| `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig` | future C# artifact fixture path/config | Config | Partial | Manual Only | Needs Verification | Capture remains disabled by default. Local overrides must not be committed. |
| `com.aionemu.gameserver.GameServer` | future C# game-server startup capture hook | Bootstrap | Partial | Manual Only | Needs Verification | Startup calls `installIfEnabled()` before NIO startup. Runtime ordering has not been tested locally. |
| `com.aionemu.gameserver.ShutdownHook` | future C# shutdown capture cleanup hook | Shutdown | Partial | Manual Only | Needs Verification | Shutdown resets observer before shutdown packet fanout. Runtime cleanup has not been tested locally. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Read-only activation plan | Java pet-feed/item-packet/startup/shutdown source review | Documents how to generate and manually inspect the first Java unusual-storage artifact once tooling is available. | Source inspection only. | No Java runtime artifact, no C# reader validation, no Java/C# byte comparison, and no compile validation. |

## Remaining Risks

- Java compile/runtime validation remains blocked locally by missing Maven/Java 25 tooling.
- Scenario requires real or controlled client access to a pet-food rejected-item path and an item in unusual storage.
- `itemBlob.packetBodyVerification` can fail if encode-time localized name or mutable item fields drift.
- File output can expose player/item identifiers and must remain local/opt-in.
- C# artifact reader/schema validation, runtime artifact ingestion, and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 read-only runtime activation plan
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java compile/runtime validation, runtime artifact generation, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Prepare the C# artifact reader validation surface for schema-v1 unusual-storage artifacts: add guarded test expectations for `itemBlob.hex`, `itemBlob.packetBodyVerification`, packet body/canonical hex, and schema field presence, but keep warehouse-add byte comparison guarded until a real Java artifact exists.
