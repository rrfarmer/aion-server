# Phase 6 AIN Completion - ItemPurification Target-Add Cleanup-Seal Metadata

Date: 2026-05-27
Unit of Work: UOW-1412
Status: ItemPurification target-add packet planning now consumes caller-provided cleanup/seal metadata.

## Completed

- Updated `ItemPurificationInventoryPacketInput` to carry optional `GeneralInfoWarehouseRestrictionFlag` context for the already-created target item snapshot.
- Updated `ItemPurificationPacketPlanService.CreateInventoryAddPacket` so target-item `SmInventoryAddItem.CreateItemCollect` packets pass the supplied cleanup/seal flag into the full item blob.
- Extended the focused concrete target-add packet-plan test to assert nested `GENERAL_INFO` cleanup/seal flag `3`.
- Updated progress, cleanup/seal static-data audit, and C# blob gap audit.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~ItemPurificationPacketPlanServiceTests.CreatePacketPlan_AttachesConcreteTargetAddPacketWhenRuntimeItemInputProvided|FullyQualifiedName~ItemPurificationPacketPlanServiceTests.SendConcretePacketsAsync_SendsConcretePacketsInPlanOrderAndSkipsMetadata|FullyQualifiedName~GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava"`.
- Result: passed 3 tests.

## Migration Parity Table - UOW-1412

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Services.ItemPurificationPacketPlanService.CreatePacketPlan` / `CreateInventoryAddPacket` | Service / Packet Plan | Partial | Regression Tested | Partial Parity | Target-item add packet planning now accepts caller-provided cleanup/seal context and feeds it to `SmInventoryAddItem.CreateItemCollect`. The broader purification workflow remains partial: live static-data flag sourcing, persistence, AP/quest side effects, and runtime Java comparison are not complete. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` cube item-collect branch | `Aion.GameServer.Services.ItemPurificationInventoryPacketInput` and `ItemPurificationPacketPlanService.CreateInventoryAddPacket` | Packet Context / DTO | Partial | Regression Tested | Partial Parity | C# packet input now carries `GeneralInfoWarehouseRestrictionFlag` for the already-created target item snapshot. This is precomputed context, not a static-data lookup inside the packet planner. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` item-collect branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateItemCollect` via ItemPurification target add | Packet | Partial | Regression Tested | Partial Parity | Focused target-add test asserts restricted target add packet carries nested `GENERAL_INFO` flag `3`. Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | future ItemPurification runtime input assembler / `ItemPurificationInventoryPacketInput.GeneralInfoWarehouseRestrictionFlag` | Dataholder Context / DTO | Partial | No Direct Static-Data Test In This Unit | Needs Verification | This unit only consumes a supplied flag. Live ItemPurification runtime input assembly still needs deterministic lookup from `StaticData.ItemRestrictionCleanups`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | ItemPurification target-add packets can now feed the Java-shaped field through inventory add packets. Temporary-exchange remaining seconds and time-dependent fields remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ItemPurificationPacketPlanServiceTests.CreatePacketPlan_AttachesConcreteTargetAddPacketWhenRuntimeItemInputProvided` | Regression / packet-plan serialization | `ItemPurificationService.upgradeItem`, `ItemPacketService.sendStorageUpdatePacket`, `SM_INVENTORY_ADD_ITEM`, `GeneralInfoBlobEntry` | Supplied target-item packet input with cleanup/seal flag `3` produces an item-collect add packet whose full item blob carries flag `3`. | C# packet parsing against deterministic Java source field order and supplied flag value. | Does not compute the flag from static data or compare Java runtime bytes. |
| `ItemPurificationPacketPlanServiceTests.SendConcretePacketsAsync_SendsConcretePacketsInPlanOrderAndSkipsMetadata` | Regression / send adapter order | ItemPurification packet fanout order | Existing coverage ensures concrete target add packets still send in plan order after input record expansion. | Source-derived C# assertions. | Does not inspect cleanup/seal flag. |
| `GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Existing packet guard confirms supplied cleanup/seal flag `3` reaches the full item blob. | C# packet-byte assertion against deterministic Java source order/field value. | Does not prove live ItemPurification computes the flag. |

## Remaining Risks

- Live ItemPurification runtime input assembly still needs to compute the cleanup/seal flag from `StaticData.ItemRestrictionCleanups`.
- ItemPurification persistence, AP/quest side effects, target item inheritance, random bonus selection, and live dispatch remain partial and separately tracked.
- Remaining default inventory add/update call sites still need classification and narrow wiring where applicable.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds, runtime conditioning presence, and time-normalized expiration/dye comparison remain unresolved item-blob risks.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 C# packet-plan context/caller surface changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: live ItemPurification static-data flag sourcing, remaining inventory add/update caller flag sources, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: live ItemPurification target-add cleanup/seal flag sourcing, if deterministic runtime context is present.
- Scope:
  - inspect the handler-level ItemPurification runtime input bridge that creates `ItemPurificationInventoryPacketInput`;
  - compute `GeneralInfoWarehouseRestrictionFlag` from `StaticData.ItemRestrictionCleanups` for the purified target item template id;
  - add a focused handler/bridge test that verifies the flag survives into the packet-plan input without enabling automatic live dispatch.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | ItemPurification runtime input flag sourcing | ItemPurification handler/runtime input bridge and paired tests | Medium | Use only if the existing runtime bridge already has template/static-data context. |
| B | Remaining inventory add/update caller sweep | read-only `GameServerConnection.cs`, service packet planners, tests | Low | Good fallback if ItemPurification runtime context is too broad. |
| C | Quest item reward executor design audit | read-only quest reward projection/planning services and Java `QuestService` | Medium | Still broad; keep separate from narrow cleanup-seal caller fixes. |

## Do Not Parallelize

- Do not edit the same packet planner and its tests from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not claim verified parity until there is Java runtime packet evidence or deterministic generated artifact comparison.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live or staged inventory-producing callers.
- Last completed UOW: UOW-1412.
- Last commit planned: `[Phase 6][UOW-1412] Wire purification target cleanup seal metadata`.
- Key changed files in this handoff:
  - `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPacketPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationPacketPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AIN-Completion.md`
