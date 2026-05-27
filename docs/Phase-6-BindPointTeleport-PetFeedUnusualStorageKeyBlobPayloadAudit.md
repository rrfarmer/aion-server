# Phase 6 - Pet Feed Unusual Storage Key Blob Payload Audit

Date: May 27, 2026
Unit of Work: UOW-1358

## Scope

This unit performs a read-only payload-field audit for the highest-risk item-blob entries needed by future unusual-storage warehouse-add artifacts.

Java source audited:

- `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.CompositeItemBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.EnchantInfoBlobEntry`

C# source audited:

- `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo`

No Java or C# source behavior was changed.

## Java Payload Fields

### `GeneralInfoBlobEntry` (`0x00`)

Java writes:

| Order | Width | Java Source Field |
|---|---|---|
| 1 | `H` | `ownerItem.getItemMask()` |
| 2 | `Q` | `ownerItem.getItemCount()` |
| 3 | `S` | `ownerItem.getItemCreator()` |
| 4 | `C` | constant `0` |
| 5 | `D` | `ownerItem.secondsUntilExpiration()` |
| 6 | `D` | constant `0` |
| 7 | `D` | `ownerItem.getTemporaryExchangeTimeRemaining()` |
| 8 | `H` | `DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled(ownerItem.getItemId()) ? 3 : 0` |
| 9 | `D` | constant `0` remaining unsealing time |
| 10 | `H` | constant `18` |

C# currently writes mask/count/creator/expiration and constants, but writes zeroes for temporary exchange remaining and cleanup/seal restriction. Expiration remains time-dependent.

### `CompositeItemBlobEntry` (`0x0E`)

Java writes:

| Order | Width | Java Source Field |
|---|---|---|
| 1 | `D` | `ownerItem.getFusionedItemId()` |
| 2 | `D * 6` | fusion stones by slot `0..Item.MAX_BASIC_STONES - 1`, zero-filled when missing |
| 3 | `C` | `ownerItem.getFusionedItemOptionalSockets()` |
| 4 | `C` | `ownerItem.getFusionedItemBonusStatsId()` |

C# currently matches fusioned item id, fusion stone slot order, and optional fusion socket, but writes zero for fusion bonus stats id.

### `EnchantInfoBlobEntry` (`0x0B`)

Java writes:

| Field Group | Java Source Field |
|---|---|
| Soulbound/enchant/skin | `item.isSoulBound()`, `item.getEnchantLevel()`, `item.getItemSkinTemplate().getTemplateId()` |
| Optional sockets/enchant bonus | `-1` when unidentified, otherwise `item.getOptionalSockets()` and `item.getEnchantBonus()` |
| Mana stones | item mana stones by slot `0..Item.MAX_BASIC_STONES - 1` |
| Godstone | `item.getGodStoneId()` |
| Dye | `item.getColorTimeLeft()`, `item.getItemColor()`, and `Math.max(0, dyeExpiration)` |
| Idian | `item.getIdianStone().getItemId()` and polish number when present and polish number is positive |
| Tempering | `item.getTempering()` |
| Plume tempering stats | for tempered plumes: HP stat id/value plus physical or magical stat id/value including `item.getRndPlumeBonusValue()` |
| Tail fields | remaining stat placeholders, amplified flag, buff skill, two zero skill ids |

C# currently matches the broad fixed layout and many item fields, but writes zeroes for the plume tempering stat block and derives dye expiration from local wall-clock state.

## Artifact Field Requirements

Future Java artifacts should include these payload fields explicitly before warehouse-add byte comparison is enabled:

- General: item mask, count, creator, expiration remaining seconds, temporary exchange remaining seconds, cleanup/seal restriction flag, unseal remaining seconds, and fixed tail constant.
- Composite: fusioned item id, six fusion stone item ids by slot, optional fusion sockets, and fusion bonus stats id.
- Enchant: soulbound, enchant level, skin template id, identified-derived optional sockets/enchant bonus, six mana stone ids by slot, godstone id, dye color, dye remaining seconds, idian item id, idian polish number, tempering, plume tempering stat ids/values, amplified flag, and buff skill.
- Time normalization: capture timestamp and all remaining-second values used for expiration, dye, and temporary exchange.

## Boundaries Preserved

- No source behavior was changed.
- No Java artifact writer was added.
- No observer install, file output, raw byte retention, or JSON serialization was added.
- No C# serializer gap was closed in this unit.
- No parity was promoted to verified.

## Validation

- Ran `git diff --check`.
- Java compile validation was not required for this docs-only audit and remains blocked locally by missing Maven tooling.
- No Java runtime artifacts were generated.
- No .NET tests were required because no C# behavior changed.

## Migration Parity Table - UOW-1358

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Manual Only | Partial Parity | C# currently omits Java temporary exchange remaining seconds and cleanup/seal restriction flag. Expiration is time-dependent and needs normalization. |
| `com.aionemu.gameserver.network.aion.iteminfo.CompositeItemBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteCompositeItemBlob` | Serialization Entry | Partial | Manual Only | Partial Parity | C# currently writes zero for Java `getFusionedItemBonusStatsId()`. Fusion stone slot order appears structurally aligned but needs runtime verification. |
| `com.aionemu.gameserver.network.aion.iteminfo.EnchantInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteEnchantInfo` | Serialization Entry | Partial | Manual Only | Partial Parity | C# currently omits Java plume tempering stat ids/values and relies on time-derived dye expiration. Dynamic item payload fields need Java artifact capture before verification. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_CLEAN_UP` | future C# item cleanup/seal static-data projection | Static Data Dependency | Not Started | No Tests | Needs Verification | Java `GeneralInfoBlobEntry` depends on cleanup/seal static data to write `3` for account/legion warehouse storage restrictions. C# currently writes zero. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Read-only Audit | Java item-info blob source review | Documents payload fields and C# gaps for key blob entries. | Source audit only. | No runtime Java artifact, decoded payload writer, C# serializer fix, or byte comparison validation. |

## Remaining Risks

- Java compile/runtime validation remains blocked locally by missing Maven/Java 25 tooling.
- Date/time fields need deterministic normalization before Java/C# byte comparison can be stable.
- Static cleanup/seal data does not yet have a confirmed C# projection.
- Plume tempering stat ids/values depend on Java `PlumStatEnum`, template tempering name, tempering level, and random plume bonus value.
- Fusion bonus stats id is still missing from C# serialization.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, decoded payload writer, cleanup/seal static-data projection, C# serializer gap closure, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled Java artifact payload DTO shape for the audited key blob entries. Start with metadata-only nested DTOs for general/composite/enchant dynamic fields inside the no-output item-blob snapshot, without JSON writing, byte retention, or C# serializer changes.
