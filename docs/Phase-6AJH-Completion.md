# Phase 6AJH Completion - Extraction Target Direct Delete Parity

Date: 2026-05-27
Unit of Work: UOW-1432
Status: Complete after validation.

## Scope

Correct the packet-visible extraction target delete branch to match Java `EnchantService.breakItem`. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Reviewed Java `EnchantService.breakItem`, which removes the target with `Storage.delete(targetItem)` before consuming the extraction tool/source.
- Updated `GameServerConnection.SendExtractConsumedItemPacketsAsync` so target deletion sends default `SM_DELETE_ITEM` mask `0x00` plus `SM_CUBE_UPDATE`.
- Preserved the UOW-1430 remaining-stack source/tool cleanup/seal full update behavior.
- Updated existing extraction add/merge tests to assert target default delete, cube update counts, remaining-stack source/tool update, reward packet, and end animation ordering.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractAddsRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractMergesRestrictedRewardWithCleanupSealFlag"`.
- Result: passed 2 tests.

## Migration Parity Table - UOW-1432

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.EnchantService.breakItem` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteExtractUseItemAsync` / `SendExtractConsumedItemPacketsAsync` | Service / Connection Packet Caller | Partial | Regression Tested | Partial Parity | C# now matches Java's packet-visible target direct-delete branch: default delete mask `0x00` plus cube update before source/tool consume packets. Java's tool-consume failure edge and runtime byte comparison remain unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage.delete` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` default constructor plus `SmCubeUpdate.CubeSizeSnapshot` | Storage / Packet Caller | Partial | Regression Tested | Partial Parity | Extraction target delete now uses Java direct-delete semantics. Repository transaction timing and exact Java runtime bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Packet | Partial | Regression Tested | Partial Parity | Tests assert post-target-delete cube item counts for add and merge reward cases. Expand fields are carried from the represented player; Java runtime byte comparison remains blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` default delete type | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` default constructor path | Packet | Complete | Regression Tested | Partial Parity | Focused extraction tests assert delete mask `0` for target deletion. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByObjectId` | `Aion.GameServer.Services.EnchantService.CreateBreakItemPlan` plus source/tool packet fanout | Storage / Packet Caller | Partial | Regression Tested | Partial Parity | Remaining-stack source/tool full update cleanup/seal behavior from UOW-1430 remains covered. Exhausted source/tool delete/cube branch remains unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractAddsRestrictedRewardWithCleanupSealFlag` | Regression / connection packet serialization | `EnchantService.breakItem`, `Storage.delete`, `Storage.decreaseByObjectId` | Target delete uses default mask `0`, cube update follows with post-delete count, remaining source/tool update keeps cleanup/seal flag `3`, reward add still sends. | C# packet parsing against reviewed Java branch order. | No Java runtime bytes; source/tool exhausted delete branch and tool-consume failure edge not covered. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractMergesRestrictedRewardWithCleanupSealFlag` | Regression / connection packet serialization | Same as above plus reward merge path | Target default delete/cube update order holds when reward merges into an existing stack. | Deterministic C# packet assertions from Java-reviewed behavior. | No Java runtime bytes; source/tool exhausted delete branch remains unverified. |

## Remaining Risks

- Java deletes the target before attempting source/tool consume and returns success even if tool consume fails; C# composed persistence still does not model that odd success/no-reward edge at the packet level.
- Exhausted extraction source/tool delete plus cube update branch remains unverified.
- AP extraction target delete semantics were deliberately left unchanged; this unit only covered Java `ExtractAction`/`EnchantService.breakItem`.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 extraction target delete packet branch changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, source/tool exhausted delete/cube branch, break-item tool-consume failure edge, persistence transaction timing
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: add extraction source/tool exhausted delete plus cube update coverage.
- Why: target direct-delete parity is now covered, but Java `Storage.decreaseByObjectId` exhausted source/tool branch should still be explicitly packet-tested.
- Candidate files:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`

## Alternative Sequential Task

- Task: add composition exhausted tool/stone delete/cube and no-rollback edge coverage.
- Why: UOW-1431 covered only remaining-stack full update blobs; Java's delete-only and partial-consume behavior is still packet-unverified.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1432] Align extraction target direct delete packets`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/EnchantService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_DELETE_ITEM.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`
- C# files changed in this unit:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJH-Completion.md`
