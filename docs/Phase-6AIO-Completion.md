# Phase 6 AIO Completion - ItemPurification Snapshot Cleanup-Seal Static Data

Date: 2026-05-27
Unit of Work: UOW-1413
Status: ItemPurification packet-input snapshot and handler/live bridges now compute/pass cleanup-seal metadata from static-data context when supplied.

## Completed

- Updated `ItemPurificationPacketInputSnapshotService.CreateInputs` to accept optional `ItemRestrictionCleanupTable` context.
- Added Java-predicate cleanup/seal flag computation at snapshot input assembly for post-mutation inventory items.
- Threaded optional cleanup context through `ItemPurificationHandlerPacketBridgeService`, explicit live execution, persistent live execution, and `GameServerConnection` opt-in ItemPurification live execution paths.
- Added focused snapshot and handler bridge tests proving restricted target-item packet inputs carry flag `3`.
- Updated progress, cleanup/seal audit, and C# blob gap audit.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~ItemPurificationPacketInputSnapshotServiceTests.CreateInputs_ComputesCleanupSealFlagFromRestrictionTable|FullyQualifiedName~GameServerConnectionItemPurificationTests.ItemPurificationHandlerPacketBridge_ComposesConcretePacketsFromPostMutationSnapshots|FullyQualifiedName~GameServerConnectionItemPurificationTests.ItemPurificationHandlerMutationBridge_ComposesConcretePacketsFromCurrentInventoryPreview|FullyQualifiedName~ItemPurificationPacketPlanServiceTests.CreatePacketPlan_AttachesConcreteTargetAddPacketWhenRuntimeItemInputProvided"`.
- Result: passed 4 tests.

## Migration Parity Table - UOW-1413

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Services.ItemPurificationHandlerPacketBridgeService.CreateConcretePacketPlan` and `CreateConcretePacketPlanFromCurrentInventory` | Service / Handler Bridge | Partial | Regression Tested | Partial Parity | Handler packet bridges can now pass cleanup/seal static-data context into post-mutation packet inputs before target-add packet planning. Automatic live handler dispatch remains disabled. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` cube item-collect branch | `Aion.GameServer.Services.ItemPurificationPacketInputSnapshotService.CreateInputs` | Packet Input Snapshot Service | Partial | Unit Tested | Partial Parity | Snapshot assembly now computes `GeneralInfoWarehouseRestrictionFlag` from supplied static-data context using Java's cleanup predicate. Missing/null static-data context intentionally keeps the default zero flag. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` item-collect branch | `Aion.GameServer.Services.ItemPurificationPacketInputSnapshotService` to `SmInventoryAddItem.CreateItemCollect` path | Packet / Packet Context | Partial | Regression Tested | Partial Parity | Tests prove restricted target item packet inputs carry flag `3`; UOW-1412 packet-plan coverage proves the supplied flag reaches the full blob. Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` consumed by ItemPurification snapshot/live seams | Dataholder Context | Partial | Unit Tested | Partial Parity | This unit consumes the previously ported cleanup table. It does not add new XML parsing behavior. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` via ItemPurification snapshot/packet-plan bridge | Serialization Entry | Partial | Regression Tested | Partial Parity | ItemPurification target-add bridge now has a static-data-backed route to the Java-shaped cleanup/seal field. Temporary-exchange and time-dependent fields remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ItemPurificationPacketInputSnapshotServiceTests.CreateInputs_ComputesCleanupSealFlagFromRestrictionTable` | Unit / snapshot input assembly | `ItemRestrictionCleanupData.hasAccountOrLegionWhStorabilityDisabled`, `GeneralInfoBlobEntry` | Restricted target item gets flag `3`; unrestricted consumed material gets flag `0`. | C# assertion against deterministic Java cleanup predicate. | Does not compare Java runtime bytes. |
| `GameServerConnectionItemPurificationTests.ItemPurificationHandlerPacketBridge_ComposesConcretePacketsFromPostMutationSnapshots` | Regression / handler bridge | `ItemPurificationService.upgradeItem`, `ItemPacketService.sendStorageUpdatePacket` | Handler bridge passes cleanup static-data context into packet inputs for target add. | Source-derived C# bridge assertion. | Explicit bridge only; automatic handler dispatch remains disabled. |
| `GameServerConnectionItemPurificationTests.ItemPurificationHandlerMutationBridge_ComposesConcretePacketsFromCurrentInventoryPreview` | Regression / mutation preview bridge | `ItemPurificationService.decreaseMaterials`, `upgradeItem` | Current-inventory preview bridge carries cleanup/seal flag `3` after mutation snapshot composition. | Source-derived C# bridge assertion. | Does not persist or compare Java runtime packets. |
| `ItemPurificationPacketPlanServiceTests.CreatePacketPlan_AttachesConcreteTargetAddPacketWhenRuntimeItemInputProvided` | Regression / packet-plan serialization | `SM_INVENTORY_ADD_ITEM`, `GeneralInfoBlobEntry` | Existing UOW-1412 guard still proves supplied flag reaches target add blob. | Deterministic C# packet parsing. | Supplied by C# bridge, not Java runtime bytes. |

## Remaining Risks

- ItemPurification automatic CM handler dispatch remains plan-only unless explicit live/persistent opt-in seams are invoked.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Remaining inventory add/update default callers still need classification and narrow wiring where applicable.
- Temporary-exchange remaining seconds, runtime conditioning presence, and time-normalized expiration/dye comparison remain unresolved item-blob risks.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 C# ItemPurification snapshot/handler/live bridge route changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: automatic ItemPurification handler dispatch, remaining inventory add/update caller flag sources, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: remaining inventory add/update cleanup-seal caller sweep and smallest implementation.
- Scope:
  - search remaining `SmInventoryAddItem.CreateItemCollect`, `SmInventoryAddItem.CreateAllSlot`, and full-blob `SmInventoryUpdateItem` callers still using default cleanup/seal flag;
  - classify each as not applicable, already wired, needs static-data context, or needs precomputed context;
  - implement the smallest remaining caller with focused packet or bridge tests.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Remaining inventory add/update caller sweep | read-only `GameServerConnection.cs`, service packet planners, tests | Low | Good next first step after this unit. |
| B | Quest item reward executor design audit | read-only quest reward projection/planning services and Java `QuestService` | Medium | Still broad; keep separate from tiny cleanup-seal caller fixes. |
| C | Java packet observer artifact design | docs/test harness only | Medium | Blocked for runtime execution locally, but design notes can reduce future comparison risk. |

## Do Not Parallelize

- Do not edit the same packet planner and its tests from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not claim verified parity until there is Java runtime packet evidence or deterministic generated artifact comparison.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live or staged inventory-producing callers.
- Last completed UOW: UOW-1413.
- Last commit planned: `[Phase 6][UOW-1413] Source purification snapshot cleanup seal flags`.
- Key changed files in this handoff:
  - `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPacketInputSnapshotService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationHandlerPacketBridgeService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationLiveExecutionService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPersistentLiveExecutionService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationPacketInputSnapshotServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionItemPurificationTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AIO-Completion.md`
