# Phase 6AJI Completion - Extraction Source Exhausted Delete Parity

Date: 2026-05-27
Unit of Work: UOW-1433
Status: Complete after validation.

## Scope

Cover the packet-visible extraction source/tool exhausted-stack branch after UOW-1432 aligned target direct-delete packets. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Reviewed Java `Storage.decreaseByObjectId` and `ItemPacketService.sendItemDeletePacket`.
- Updated `GameServerConnection.SendExtractConsumedItemPacketsAsync` so an exhausted extraction source/tool sends `SM_DELETE_ITEM` with use-delete mask `0x17`, followed by `SM_CUBE_UPDATE`.
- Preserved UOW-1432 target default-delete/cube behavior and UOW-1430 remaining-stack source/tool cleanup/seal full update behavior.
- Added a focused extraction connection test with source/tool count `1`.
- Spawned a read-only composition explorer for the next unit and integrated its findings into the next-work guidance.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractAddsRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractMergesRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractDeletesLastSourceWithUseDeleteAndCubeUpdate"`.
- Result: passed 3 tests.

## Migration Parity Table - UOW-1433

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.EnchantService.breakItem` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteExtractUseItemAsync` / `SendExtractConsumedItemPacketsAsync` | Service / Connection Packet Caller | Partial | Regression Tested | Partial Parity | C# now covers both target direct-delete/cube and source/tool exhausted use-delete/cube packet branches for extraction. Java's odd success/no-reward edge when tool consume fails still is not modeled at packet level. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByObjectId` | `Aion.GameServer.Services.EnchantService.CreateBreakItemPlan` plus `GameServerConnection.SendExtractConsumedItemPacketsAsync` | Storage / Packet Caller | Partial | Regression Tested | Partial Parity | Exhausted extraction source/tool stacks now send `SM_DELETE_ITEM` use-delete mask and a cube update, matching Java `DEC_ITEM_USE` delete behavior. Remaining-stack full update cleanup/seal behavior remains covered from UOW-1430. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` plus `SmCubeUpdate.CubeSizeSnapshot` | Packet Fanout Helper | Partial | Regression Tested | Partial Parity | Focused test asserts target delete/cube then source delete/cube counts in packet order. Warehouse delete variants and non-extraction callers are outside this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` use-delete type | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem.UseDeleteType` | Packet | Complete | Regression Tested | Partial Parity | Source/tool exhausted delete uses mask `0x17`. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Packet | Partial | Regression Tested | Partial Parity | Tests assert projected cube counts after target deletion and after exhausted source/tool deletion. Expand fields are represented-player snapshots; Java runtime byte comparison remains blocked. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractDeletesLastSourceWithUseDeleteAndCubeUpdate` | Regression / connection packet serialization | `EnchantService.breakItem`, `Storage.decreaseByObjectId`, `ItemPacketService.sendItemDeletePacket` | Source/tool count `1` deletes with use-delete mask `0x17`, sends cube update after target delete and after source delete, adds extraction reward, and ends animation. | C# packet parsing against reviewed Java storage packet fanout. | No Java runtime bytes; Java tool-consume failure after target delete remains unmodeled. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractAddsRestrictedRewardWithCleanupSealFlag` | Existing Regression | Same Java action, remaining-stack source branch | Regression slice verifies UOW-1432 target direct-delete/cube and UOW-1430 source full-update cleanup/seal still pass. | Deterministic C# packet assertions from Java-reviewed branches. | No Java runtime bytes. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractMergesRestrictedRewardWithCleanupSealFlag` | Existing Regression | Same Java action plus reward merge path | Regression slice verifies merge reward path still preserves target direct-delete/cube and source full update. | Deterministic C# packet assertions. | No Java runtime bytes. |

## Remaining Risks

- Java deletes the target before attempting source/tool consume and still returns success after a target delete even if tool consume fails; C# composed persistence does not yet model that failure edge.
- AP extraction delete/cube semantics were not changed.
- Composition exhausted tool/stone delete/cube and no-rollback edge coverage remains pending.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 extraction source/tool exhausted delete packet branch changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, break-item tool-consume failure edge, AP extraction delete/cube audit, composition exhausted/no-rollback edge coverage
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: add composition exhausted tool/stone delete/cube and no-rollback edge coverage.
- Why: the read-only explorer confirmed Java emits consumed-item packet side effects inline in tool, first-stone, second-stone order, with cube updates after deletes. C# currently batches all updates before all deletes in `SendConsumedItemPacketsAsync`.
- Candidate files:
  - `dotnetConversion/src/Aion.GameServer/Services/CompositionService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/CompositionServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | AP extraction delete/cube audit | Java/C# AP extraction sources, read-only | Medium | Keep separate from Java `ExtractAction`; do not assume the same target/source semantics. |
| B | Toy-pet source consume readiness | Java/C# toy-pet/kisk sources, read-only | Medium | Scheduling/world-spawn packet order still needs careful mapping. |
| C | Composition future test planning | read-only test fixture/source review | Low | Existing opcode `208` seam supports exhausted and no-rollback tests without new fixture XML. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Audit AP extraction delete/cube behavior | Java/C# AP extraction sources, read-only | all writes, docs, commits |
| Orchestrator | Implement composition consumed-operation ordering and tests | selected composition production/test files | shared docs until validation; unrelated files |

## Do Not Parallelize

- `GameServerConnection.cs` composition implementation changes.
- Shared item-use test fixture edits.
- Shared progress/handoff/audit docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1433] Align extraction source delete cube packets`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/EnchantService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_DELETE_ITEM.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`
- Composition explorer findings for next work:
  - Java `CompositionAction` consumes tool, first stone, second stone by item id in exact order.
  - Remaining stacks send full `SM_INVENTORY_UPDATE_ITEM` with `DEC_ITEM_USE` mask `0x16`.
  - Exhausted stacks send `SM_DELETE_ITEM` use-delete mask `0x17`, then `SM_CUBE_UPDATE`.
  - Java sends success end animation even if a later consume fails and no reward is added.
  - C# currently batches all updated consumed items before all deleted consumed items, so mixed update/delete packet order can differ.
- C# files changed in UOW-1433:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJI-Completion.md`
