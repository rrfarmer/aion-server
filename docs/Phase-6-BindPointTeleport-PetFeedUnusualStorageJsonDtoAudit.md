# Phase 6 - Pet Feed Unusual Storage JSON DTO Audit

Date: May 27, 2026
Unit of Work: UOW-1371

## Scope

This is a read-only audit for future unusual-storage JSON artifact DTO and field-order design. It does not implement DTO classes, serialize JSON, write files, retain raw packet bytes, mutate storage, dispatch packets, or change capture runtime behavior.

Java remains the source of truth. Source breadcrumbs reviewed in this unit:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`
- `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob`
- `com.aionemu.commons.logging.DiscordChannelAppender`

Reference schema reviewed:

- `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageRuntimeArtifactSchema.md`

## Findings

The current nested snapshot classes are capture-internal state, not yet stable JSON DTOs. They contain useful metadata but do not map 1:1 to the schema-v1 artifact shape:

- `ArtifactSnapshot` has scenario, output directory, player id, storage id/ordinal, item id, timestamps, construction-time blob metadata, and two packet snapshots.
- `PacketSnapshot` has packet index, Java class name, clear-frame length, encoded opcode, remaining bytes, and observed blob metadata.
- `ItemBlobSnapshot` and payload snapshots carry decoded metadata for audited blob entries.

The schema-v1 document requires stable top-level sections:

- `schemaVersion`
- `scenario`
- `javaSources`
- `storage`
- `timing`
- `constructionSnapshot`
- `encodeSnapshot`
- `packets`
- `notes`

Direct serialization of the private nested snapshot classes would expose implementation structure, omit schema-required sections, and make field order dependent on serializer reflection behavior. Future JSON output should use dedicated DTO/build methods rather than serializing `ArtifactSnapshot` directly.

`DiscordChannelAppender` shows existing fastjson2 usage through `JSON.toJSONBytes(...)`, but it serializes a small `Map` for webhook payloads and does not establish deterministic artifact field-order policy. Future artifact output should either use explicit DTOs with field-based order known from source order or construct ordered maps in schema order before calling fastjson2.

## Future DTO Requirements

Future schema DTO construction should:

- build `schemaVersion` as an explicit integer constant;
- emit fields in the schema-v1 order above;
- keep Java source breadcrumbs in `javaSources`;
- split route facts into `storage` and `constructionSnapshot`;
- split mutable encode-time item/blob facts into `encodeSnapshot`;
- represent packet bytes as explicit `bodyHex` and `canonicalPayloadHex` fields before claiming packet byte parity;
- preserve decoded packet metadata for `SM_WAREHOUSE_ADD_ITEM` and `SM_CUBE_UPDATE`;
- emit missing byte fields as empty strings or null only with explicit notes;
- preserve time values as captured millis plus normalized remaining-second fields where Java behavior is time-sensitive;
- avoid exposing `outputDirectory` in the JSON artifact payload unless it is intentionally part of diagnostics.

## Migration Parity Table - UOW-1371

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Read-only audit concludes current nested snapshots should feed dedicated schema DTOs rather than be serialized directly. Raw/canonical byte fields are still absent. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Packet | Partial | Manual Only | Needs Verification | Future DTO must include warehouse route fields, decoded item count/add mask, item blob bytes, and canonical payload hex before C# warehouse-add parity can be checked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Packet | Partial | Manual Only | Needs Verification | Future DTO must preserve action/actionValue/count/expand decoded fields plus body/canonical payload hex. Existing C# helper covers metadata but not Java runtime artifacts. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | future C# item blob serializer/comparator | Packet Blob Utility | Partial | Manual Only | Needs Verification | Current capture stores decoded metadata snapshots but not raw blob hex or full decoded schema-v1 dynamic/template sections. |
| `com.aionemu.commons.logging.DiscordChannelAppender` | No C# artifact equivalent in current slice | Utility | Not Started | Manual Only | Needs Verification | Reviewed as existing fastjson2 usage. It serializes a small webhook map and does not define deterministic artifact field-order policy. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Read-only audit | Java capture source, packet source, blob source, and schema document review | Documents DTO and field-order requirements before JSON output. | Source inspection only. | No DTO implementation, no JSON serialization, no field-order test, no raw byte retention, and no C# reader validation. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Dedicated schema DTOs are not implemented.
- Raw `bodyHex`, `canonicalPayloadHex`, and blob hex are still missing.
- Fastjson2 deterministic field-order behavior has not been validated in this repo.
- C# artifact reader/schema validation and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 read-only JSON DTO audit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java compile validation, schema DTOs, field-order validation, raw byte retention, JSON writer, Java runtime artifacts, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled schema DTO shell for `ArtifactSnapshot` serialization: create private DTO/build methods in schema-v1 order, but do not call fastjson2, write files, or add byte hex fields until raw/canonical byte retention is implemented.
