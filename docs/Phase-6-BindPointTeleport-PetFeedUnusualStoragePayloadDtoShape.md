# Phase 6 - Pet Feed Unusual Storage Payload DTO Shape

Date: May 27, 2026
Unit of Work: UOW-1360

## Scope

This unit adds a disabled schema-only Java payload DTO shape for audited item-blob fields inside the no-output unusual-storage capture snapshot.

Java source touched:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`

Java source dependencies referenced:

- `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob`
- `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.CompositeItemBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.EnchantInfoBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.ConditioningInfoBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.PremiumOptionInfoBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.PolishInfoBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.WrapInfoBlobEntry`

## What Changed

- `ItemBlobSnapshot` now carries an optional `ItemBlobPayloadSnapshot`.
- `ItemBlobPayloadSnapshot` contains nested no-output payload DTOs for:
  - general
  - composite
  - enchant
  - conditioning
  - premium option
  - polish
  - wrap
- `SM_WAREHOUSE_ADD_ITEM` now exposes `getFirstItem()` so the disabled observer-side capture path can build payload DTO metadata from the packet-held Java item.
- Construction-time and observer-time blob snapshots can both carry payload DTO metadata when the item reference is available.

## Captured Payload Fields

General payload:

- item mask
- item count
- item creator
- seconds until expiration
- temporary exchange remaining seconds
- account/legion warehouse restriction flag from `DataManager.ITEM_CLEAN_UP`

Composite payload:

- fusioned item id
- six fusion stone item ids by slot
- fusioned item optional sockets
- fusioned item bonus stats id

Enchant payload:

- soulbound flag
- enchant level
- skin template id
- optional manastone sockets
- enchant bonus
- six mana stone item ids by slot
- godstone id
- dye color
- dye remaining seconds
- idian stone item id
- idian polish number
- tempering level
- plume tempering stat id/value pairs
- amplified flag
- buff skill

Small payloads:

- conditioning-info presence and charge points
- premium identified flag, bonus stats id, and tune count
- idian polish charge
- pack count

## Boundaries Preserved

- Capture remains disabled by default.
- No observer installation was added.
- `onSnapshotReady` remains a no-op.
- No JSON writer, output directory creation, bounded writer queue, or raw byte retention was added.
- No C# reader, serializer, or comparison behavior changed.
- No parity was promoted to verified.

## Validation

- Ran `git diff --check`.
- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Java validation remains blocked locally because `mvn` is not available on PATH and no Maven wrapper exists.
- No Java runtime artifacts were generated.

## Migration Parity Table - UOW-1360

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Adds schema-only no-output payload DTOs to construction-time and observer-time item-blob snapshots. Capture remains disabled and no writer exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Packet | Partial | Manual Only | Needs Verification | Exposes the first packet item for disabled capture metadata. Packet serialization writes are unchanged. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry / DTO Source | Partial | Manual Only | Partial Parity | Payload DTO records Java fields including temporary exchange remaining seconds and cleanup/seal restriction flag; C# serializer still writes zeroes for those gaps. |
| `com.aionemu.gameserver.network.aion.iteminfo.CompositeItemBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteCompositeItemBlob` | Serialization Entry / DTO Source | Partial | Manual Only | Partial Parity | Payload DTO records fusion bonus stats id; C# serializer still writes zero for that field. |
| `com.aionemu.gameserver.network.aion.iteminfo.EnchantInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteEnchantInfo` | Serialization Entry / DTO Source | Partial | Manual Only | Partial Parity | Payload DTO records dye time and plume tempering stats; C# serializer still has known gaps and time-dependent behavior. |
| `com.aionemu.gameserver.network.aion.iteminfo.ConditioningInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteConditioningInfoBlob` | Serialization Entry / DTO Source | Partial | Manual Only | Needs Verification | Payload DTO records runtime conditioning-info presence and charge points. C# still uses template/charge heuristics for inclusion. |
| `com.aionemu.gameserver.network.aion.iteminfo.PremiumOptionInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WritePremiumOptionBlob` | Serialization Entry / DTO Source | Partial | Manual Only | Needs Verification | Payload DTO records identified state, bonus stats id, and tune count. Runtime mapping still needs Java artifacts. |
| `com.aionemu.gameserver.network.aion.iteminfo.PolishInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WritePolishInfoBlob` | Serialization Entry / DTO Source | Partial | Manual Only | Needs Verification | Payload DTO records idian polish charge. Runtime mapping still needs Java artifacts. |
| `com.aionemu.gameserver.network.aion.iteminfo.WrapInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteWrapInfoBlob` | Serialization Entry / DTO Source | Partial | Manual Only | Needs Verification | Payload DTO records pack count. Runtime artifact verification is still missing. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java item-info blob source review | Adds disabled schema-only DTO fields for audited payload inputs. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime artifact, JSON schema output, C# reader verification, or byte comparison. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- DTO fields are not written to disk yet and have no C# reader/schema validation.
- Time-dependent expiration, dye, and temporary exchange fields still need deterministic normalization in the future artifact writer.
- Raw/canonical Java packet bytes are still not retained.
- C# warehouse-add byte comparison remains blocked by serializer gaps and missing runtime artifacts.

## Summary Metrics

- Total Java artifacts discovered: 9 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled schema-only payload DTO shape
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 9 grouped rows
- Total blocked artifacts: Java compile validation, JSON artifact writer, raw/canonical byte retention, C# artifact reader/schema validation, C# serializer gap closure, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a read-only fastjson2 deterministic writer audit, then add a disabled bounded writer queue only after field ordering, file naming, byte encoding, and failure/drop behavior are documented. Do not enable observer installation or write files until the writer safety design is complete.
