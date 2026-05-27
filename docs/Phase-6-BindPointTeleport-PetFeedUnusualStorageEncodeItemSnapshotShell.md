# Phase 6 - Pet Feed Unusual Storage Encode-Time Item Snapshot Shell

Date: May 27, 2026
Unit of Work: UOW-1374

## Scope

This unit adds a disabled encode-time item snapshot shell for future unusual-storage JSON artifacts. It does not call fastjson2, serialize JSON, create directories, write files, retain raw packet bytes, mutate storage, dispatch packets, or enable capture by default.

Java remains the source of truth. Source breadcrumbs touched in this unit:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.model.gameobjects.Item`
- `com.aionemu.gameserver.model.templates.item.ItemTemplate`

## Implementation

`PacketSnapshot.from(...)` now uses `SM_WAREHOUSE_ADD_ITEM.getFirstItem()` at observer time to build a passive `EncodeTimeItemSnapshot`.

The snapshot captures:

- item id;
- item count;
- item location at serialization time;
- equipment slot;
- template id;
- localized name from `ItemTemplate.getL10n()`;
- pack count;
- absolute expire time;
- absolute temporary exchange time;
- temporary exchange remaining time;
- charge points;
- enchant level;
- item mask;
- nullable item color.

The schema DTO item section now prefers this observer-time item snapshot and falls back to earlier item-blob payload metadata only when the encode-time item snapshot is unavailable.

No byte retention or JSON/file output was added.

## Migration Parity Table - UOW-1374

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Adds passive encode-time item snapshot metadata and feeds schema DTO item fields from it. No JSON serialization, file output, raw byte retention, or runtime validation exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Packet | Partial | Manual Only | Needs Verification | Uses existing `getFirstItem()` observer accessor to capture item/template fields at encode time. Packet byte body/canonical fields remain missing. |
| `com.aionemu.gameserver.model.gameobjects.Item` | future C# item model / artifact schema DTO | Model | Partial | Manual Only | Needs Verification | Getter values are source-reviewed only. Time fields and nullable color behavior still need runtime artifact validation before parity can be claimed. |
| `com.aionemu.gameserver.model.templates.item.ItemTemplate` | future C# item template model / artifact schema DTO | Model / Static Data | Partial | Manual Only | Needs Verification | Uses `getTemplateId()` and `getL10n()` for schema item fields. Localization/string parity remains unverified. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java packet, item, template, and capture source review | Adds observer-time item snapshot metadata without JSON/file output. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime artifact, no raw byte retention, no C# reader validation, and no byte comparison. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Encode-time item snapshot behavior is source-reviewed only and not runtime-tested.
- Temporary exchange fields now capture both absolute and remaining values, but the schema DTO currently emits the remaining value under `temporaryExchangeTime`; naming may need refinement before JSON output.
- Equipment slot emits the full Java `long`, while packet encoding writes the low 16 bits; this must be documented or split before byte comparison.
- Raw item blob hex and packet body/canonical payload hex remain missing.
- C# artifact reader/schema validation and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled encode-time item snapshot shell
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java compile validation, runtime artifact validation, raw byte retention, JSON writer, Java runtime artifacts, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a read-only audit for raw/canonical byte retention boundaries in `PacketSnapshot.from(...)`: determine whether to store full clear-frame hex, body hex after length/opcode, and canonical payload hex without mutating the buffer or affecting encryption.
