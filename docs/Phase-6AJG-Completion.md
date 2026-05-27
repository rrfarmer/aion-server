# Phase 6AJG Completion - Composition Source Cleanup Seal Metadata

Date: 2026-05-27
Unit of Work: UOW-1431
Status: Complete after validation.

## Scope

Continue Phase 6 cleanup/seal full item-blob propagation by wiring Java-confirmed remaining-stack consumed item full update packets for composition tools and stones. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Performed parallel work discovery around `CM_COMPOSITE_STONES`, composition consumed input packets, extraction target delete parity, and toy-pet source cleanup/seal.
- Spawned a read-only C# explorer for the safest composition connection test seam and integrated its fixture/timing guidance.
- Passed cleanup/seal static-data context into `GameServerConnection.SendConsumedItemPacketsAsync` from `CompleteCompositeStonesAsync`.
- Preserved default-zero cleanup/seal behavior for other callers of the shared consumed-item sender.
- Added fixture static-data templates for a composition tool plus two enchantment stones, with cleanup rows for each consumed input.
- Added a focused opcode `208` connection test proving remaining-stack tool/stone `SM_INVENTORY_UPDATE_ITEM` packets carry cleanup/seal flag `3`.
- Updated `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`, and `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs"`.
- Result: passed 1 test.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_AssemblyAddsRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractAddsRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~CompositionServiceTests|FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesCompositeStonesPacket"`.
- Result: passed 12 tests.

## Migration Parity Table - UOW-1431

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` | `Aion.GameServer.Network.Aion.ClientPackets.CmCompositeStones` / `GameServerConnection.HandleCompositeStonesAsync` | Client Packet / Connection Handler | Partial | Regression Tested | Partial Parity | Opcode `208` parsing and connection dispatch are covered for the scheduled happy path. Packet-byte Java runtime comparison, invalid-state handling, and encrypted real-client ordering remain unverified. |
| `com.aionemu.gameserver.model.templates.item.actions.CompositionAction` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteCompositeStonesAsync` / `Aion.GameServer.Services.CompositionService` | Item Action / Service | Partial | Regression Tested | Partial Parity | Java consumes tool, first stone, and second stone by item id after a 5s self-only animation. C# remaining-stack consumed updates now carry cleanup/seal flag `3`. Reward randomness/template fanout and exhausted delete/cube branches remain unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByItemId` | `Aion.GameServer.Services.CompositionService.CreateMutationPlan` plus `GameServerConnection.SendConsumedItemPacketsAsync` | Storage / Packet Caller | Partial | Unit + Regression Tested | Partial Parity | C# preserves Java-style sequential item-id consumption in the plan and now passes cleanup/seal context into remaining-stack full update packets. Java's no-rollback behavior on later consume failure is only service-tested for one missing-stone edge and not packet-tested. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.DEC_ITEM_USE` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem.DecreaseItemUse` | Packet Update Type | Partial | Regression Tested | Partial Parity | Focused connection test asserts composition consumed full updates use `DEC_ITEM_USE` and item mask `0`. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` consumed by composition connection path | Dataholder Context | Partial | Regression Tested | Partial Parity | Existing cleanup table now flows into composition consumed tool/stone full update packets. Loader behavior was not changed; absent rows still write flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` via composition consumed full updates | Serialization Entry | Partial | Regression Tested | Partial Parity | Covered composition consumed full update packets can now feed the Java-shaped cleanup/seal field. Temporary-exchange remaining seconds, runtime conditioning presence, and Java runtime byte comparison remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs` | Regression / connection packet serialization | `CM_COMPOSITE_STONES`, `CompositionAction`, `Storage.decreaseByItemId`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Opcode `208` schedules composition, then remaining-stack tool/stone consumed updates carry cleanup/seal flag `3` and `DEC_ITEM_USE`; start/end animations remain ordered around completion. | C# packet parsing against reviewed Java full-blob consumed-input shape and static-data predicate. | No Java runtime bytes; reward packet fanout intentionally avoided by omitting reward template; exhausted consumed delete/cube branches not covered. |
| `CompositionServiceTests.CreateMutationPlan_ConsumesInputsAndAddsCalculatedReward` | Existing Unit | `CompositionAction.run` | Regression slice verifies service-level tool/stone consume plus reward plan still works. | Deterministic C# service assertions from Java-reviewed formula. | Not a packet test; no cleanup/seal metadata. |
| `CompositionServiceTests.CreateMutationPlan_ConsumesWhatJavaDecreaseByItemIdCanConsumeWhenSecondStoneIsMissing` | Existing Unit | Java sequential `decreaseByItemId` no-rollback behavior | Regression slice verifies one partial-consume missing-stone edge remains covered. | Deterministic C# service assertions. | Not packet-tested; no Java runtime comparison. |
| `GamePacketTests.ClientPacketFactory_ParsesCompositeStonesPacket` | Existing Packet Parser | `CM_COMPOSITE_STONES.readImpl` | Regression slice verifies opcode `208` parsing remains intact. | Deterministic C# parser assertion. | No runtime dispatch or packet send. |

## Remaining Risks

- Composition reward add/update packet fanout was intentionally avoided in the new test by omitting a reward template, so random reward template handling remains covered only by lower-level service tests and existing reward wiring.
- Exhausted composition tool/stone delete plus cube update paths remain unverified at the connection level.
- Java composition always sends a success end animation even if a consume fails and no reward is added; C# no-rollback/failure packet ordering still needs dedicated edge coverage.
- `SendConsumedItemPacketsAsync` is shared by other connection paths; default-zero cleanup context preserves current behavior, but additional callers should be audited before assuming coverage.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 C# composition consumed full-update caller path changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime artifact generation, composition reward runtime byte comparison, exhausted consumed delete/cube branches, composition no-rollback packet edge, extraction target default-delete/cube parity
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: correct extraction target direct-delete/default-mask plus cube update parity as a focused packet semantics unit.
- Why: UOW-1430 Java analysis confirmed `EnchantService.breakItem` direct-deletes the target before consuming the extraction tool/source, while the current C# tested path still treats the target delete as a use-delete branch.
- Candidate files:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`

## Alternative Sequential Task

- Task: add composition exhausted tool/stone delete/cube and no-rollback edge coverage.
- Why: UOW-1431 covered only remaining-stack full update blobs; Java's delete-only and partial-consume behavior is still packet-unverified.
- Candidate files:
  - `dotnetConversion/src/Aion.GameServer/Services/CompositionService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/CompositionServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Extraction target delete/cube parity analysis | Java/C# extraction/storage sources, read-only | Medium | Confirm target default delete plus cube packet order and exact C# branch to change. |
| B | Composition no-rollback packet edge analysis | Java/C# composition sources, read-only | Medium | Map consume-failure packet order before changing connection behavior. |
| C | Toy-pet source consume readiness | Java/C# toy-pet/kisk sources, read-only | Medium | Scheduling/world-spawn packet order still needs careful mapping. |
| D | Admin/house dye source cleanup audit | Java/C# admin/housing dye sources, read-only | Medium | Candidate source full-update callers, but may cross admin/housing seams. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Analyze extraction target direct-delete/default-mask/cube parity | Java/C# extraction/storage sources, read-only | all writes, docs, commits |
| Explorer B | Analyze composition exhausted/no-rollback packet ordering | Java/C# composition sources, read-only | all writes, docs, commits |
| Orchestrator | Implement one selected packet semantics slice after analysis | selected production/test files only | shared docs until validation; unrelated files |

## Do Not Parallelize

- `GameServerConnection.cs` implementation changes.
- Shared item-use test fixture edits.
- Shared progress/handoff/audit docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1431] Wire composition source cleanup seal metadata`.
- Java source of truth for this unit:
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_COMPOSITE_STONES.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/CompositionAction.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- C# files changed in this unit:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AJG-Completion.md`
