# Phase 6 - Pet Feed Unusual Storage Schema DTO Shell

Date: May 27, 2026
Unit of Work: UOW-1372

## Scope

This unit adds a disabled schema-v1 DTO shell for future unusual-storage JSON artifacts. It does not call fastjson2, serialize JSON, create directories, write files, retain raw packet bytes, mutate storage, dispatch packets, or enable capture by default.

Java remains the source of truth. Source breadcrumbs touched in this unit:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType`

Reference schema:

- `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageRuntimeArtifactSchema.md`

## Implementation

`PetFeedUnusualStorageArtifactCapture.writeArtifact(...)` now builds the existing path helper and a schema-v1 artifact map inside the guarded no-op writer boundary. The DTO shell uses `LinkedHashMap` builders to preserve schema section insertion order for a future serializer.

The shell currently builds these schema-v1 sections:

- `schemaVersion`
- `scenario`
- `javaSources`
- `storage`
- `timing`
- `constructionSnapshot`
- `encodeSnapshot`
- `packets`
- `notes`

Known byte fields remain explicit placeholders:

- packet `bodyHex` is empty;
- packet `canonicalPayloadHex` is empty;
- item blob `hex` is empty;
- notes state that raw/canonical byte retention is not implemented.

Known partial fields remain explicit null/zero placeholders where the current capture snapshot does not yet carry enough data, including item id, template id, localized name, equipment slot, absolute expire time, storage type name, and captured epoch seconds.

## Migration Parity Table - UOW-1372

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Adds private schema-v1 ordered-map DTO builders and invokes them inside the no-op writer boundary. No JSON serialization, file output, raw byte retention, or runtime validation exists. Several schema fields remain explicit placeholders. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType` | future C# artifact/schema reader metadata | Enum | Partial | Manual Only | Needs Verification | DTO shell uses Java `ItemAddType.ALL_SLOT.name()` and `getMask()` to populate construction/packet decoded metadata. Behavior is source-reviewed only. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java capture source and schema document review | Adds schema-v1 ordered DTO builders without JSON/file output. | Source implementation only. | Maven/Java 25 compile unavailable locally; no field-order test, no JSON artifact, no raw byte retention, and no C# reader validation. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- DTO builder behavior is not runtime-tested.
- Fastjson2 field-order behavior is still unvalidated because serialization is not enabled.
- Raw `bodyHex`, `canonicalPayloadHex`, and item blob hex remain missing.
- Several encode-time item/template fields are still placeholders.
- C# artifact reader/schema validation and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 2 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled schema DTO shell
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 2 grouped rows
- Total blocked artifacts: Java compile validation, DTO runtime validation, field-order validation, raw byte retention, JSON writer, Java runtime artifacts, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a read-only encode-time item field audit for the schema placeholders: identify which item/template/localization/equipment fields can be populated from `SM_WAREHOUSE_ADD_ITEM` or its live `Item` reference at observer time without adding byte retention or JSON output.
