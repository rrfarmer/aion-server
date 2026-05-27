# Phase 6 AHZ Completion - Mail Attachment Cleanup-Seal Flag Input

Date: 2026-05-27
Unit of Work: UOW-1398
Status: mail read attached-item serialization now supports explicit cleanup/seal general-info flag input.

## Completed

- Added optional `generalInfoWarehouseRestrictionFlag` input to `SmMailService.CreateReadPacket`.
- Stored the explicit flag on read-letter packet instances.
- Passed the flag into `SmInventoryInfo.WriteItemInfoBlob` for attached items.
- Preserved default-zero behavior for existing callers.
- Left mailbox/list/status/attachment-state/delete packet shapes unchanged.
- Added focused attached-mail coverage for restricted item id `188053996`, asserting nested `GENERAL_INFO` cleanup/seal flag `3`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.CharacterSelectionServerPackets_WriteJavaShapedPayloads|FullyQualifiedName~GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.SmWarehouseAddItem_WritesCleanupSealFlagInItemBlobLikeJava" --no-restore`.
- Result: passed 4 tests.
- Ran `git diff --check`.

## Migration Parity Table - UOW-1398

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_MAIL_SERVICE` read-letter attached item path | `Aion.GameServer.Network.Aion.ServerPackets.SmMailService` | Packet | Partial | Regression Tested | Partial Parity | C# read-mail attached item serialization can now carry an explicit cleanup/seal flag into the shared item blob and focused tests verify flag `3`. Runtime callers still need cleanup-table or precomputed context plumbing. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Utility | Partial | Regression Tested | Partial Parity | Reuses the UOW-1394 explicit flag input. This unit does not change shared serializer behavior. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | Mail attached-item packet coverage now confirms nested `GENERAL_INFO` flag `3`. Temporary-exchange and time-dependent fields remain unresolved. |
| `com.aionemu.gameserver.services.mail.MailService.readMail` | future runtime caller plumbing into `SmMailService.CreateReadPacket` | Service / Packet Orchestration | Partial | No Tests | Needs Verification | Wrapper packet accepts the flag, but live mail-read callers do not yet compute/pass `ItemRestrictionCleanupTable` context for attached items. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.CharacterSelectionServerPackets_WriteJavaShapedPayloads` attached-mail restricted item assertion | Regression / packet byte layout | `SM_MAIL_SERVICE.writeLetterRead`, `ItemInfoBlob.getFullBlob`, `GeneralInfoBlobEntry` | Read-mail attached item blob writes cleanup/seal field `3` when explicit flag input is supplied. | Deterministic Java packet/source behavior and C# packet-byte assertion. | Does not compare generated Java bytes and does not wire live `MailService.readMail` callers. |
| `GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Guards explicit flag path remains stable for inventory add. | Existing C# packet-byte assertion. | Not a Java runtime comparison. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_UPDATE_ITEM.writeImpl`, `GeneralInfoBlobEntry` | Guards explicit flag path remains stable for inventory update. | Existing C# packet-byte assertion. | Not a Java runtime comparison. |
| `GamePacketTests.SmWarehouseAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_WAREHOUSE_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Guards explicit flag path remains stable for warehouse add. | Existing C# packet-byte assertion. | Not a Java runtime comparison. |

## Remaining Risks

- Runtime wrapper callers still need cleanup-table or precomputed cleanup/seal context for inventory add/update, warehouse add, pet-feed, and mail read flows.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds still serialize as zero.
- Runtime conditioning presence and wall-clock expiration/dye fields can still differ from Java.
- Java template-mask mutation from cleanup rows remains outside these packet-wrapper units.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 2 C# packet/serializer surfaces changed or consumed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: runtime cleanup flag source for wrapper callers, Java runtime artifact generation, temporary-exchange model/template fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: audit and wire the first live runtime caller to compute/pass cleanup/seal flags from `StaticData.ItemRestrictionCleanups`.
- Scope:
  - start with the smallest non-shared caller path that already has static-data access;
  - compute `3` when `ItemRestrictionCleanupTable.HasAccountOrLegionWarehouseStorabilityDisabled(itemId)` is true, otherwise `0`;
  - preserve default-zero behavior where static data is unavailable;
  - add focused caller-level tests without changing `SmInventoryInfo`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Runtime inventory context source audit | read-only `GameServerConnection.cs`, services using inventory add/update | Low | Map where static data is already available before writing. |
| B | Runtime mail context source audit | read-only mail connection/service paths | Low | Find the narrow mail-read caller that can supply cleanup data. |
| C | Broker plume bridge | `SmBrokerService.cs`, broker packet tests | Medium | Separate from cleanup/seal work; avoid `SmInventoryInfo.cs`. |
| D | Temporary exchange model/source audit | read-only Java/C# analysis | Low | Needed before full warehouse-add byte comparison. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Inventory/warehouse caller flag-source audit | read-only `GameServerConnection.cs`, inventory services, Java `ItemPacketService` callers | all writes |
| Agent B | Mail caller flag-source audit | read-only mail connection/service paths and Java `MailService.readMail` | all writes |
| Agent C | Temporary exchange source audit refresh | read-only Java item/template/drop/task paths | all writes |

## Do Not Parallelize

- Do not edit `SmInventoryInfo.cs` concurrently with wrapper packet units; it is the shared item-blob serializer.
- Do not wire broad `GameServerConnection` call sites in parallel unless ownership is exclusive; it is a high-conflict file.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
