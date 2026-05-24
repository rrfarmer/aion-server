# Phase 6HB Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6HA and covers Session 698.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "LeaveWorld_PersistsAcceptedStorageExpansionFields|StorageExpansionNpcServiceTests"`
  - Result: Passed, 10 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1264 tests.

## Recent Work Completed

### Session 698 - Storage Expansion Logout Persistence Boundary

- Added `PlayerEnterWorldServiceTests.LeaveWorld_PersistsAcceptedStorageExpansionFields`.
- The test accepts one cube NPC expansion and one warehouse NPC expansion through `StorageExpansionNpcService`, then leaves the world through `PlayerEnterWorldService.LeaveWorldAsync`.
- The capturing logout repository now proves the mutated represented fields are handed to persistence:
  - `NpcExpands = 1`,
  - `WarehouseNpcExpands = 1`,
  - Kinah decreased by both expansion prices.
- Existing `MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync` already updates Java-compatible `players.npc_expands` and `players.wh_npc_expands` columns.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.CubeExpandService.expandCube` / `npcExpand` | `Aion.GameServer.Services.StorageExpansionNpcService.RequestCubeExpansion` / `HandleResponse` | Service / Mutation Source | Partial | Regression Tested | Needs Verification | Accepted cube NPC expansion now has test coverage through logout persistence boundary after represented Kinah decrement and `NpcExpands` mutation. Java `player.setCubeLimit()`, deeper storage limit state, DAO transaction timing, Java runtime comparison, and live socket-order validation remain unverified. |
| `com.aionemu.gameserver.services.WarehouseService.expandWarehouse` / `expand` | `Aion.GameServer.Services.StorageExpansionNpcService.RequestWarehouseExpansion` / `HandleResponse` | Service / Mutation Source | Partial | Regression Tested | Needs Verification | Accepted warehouse NPC expansion now has test coverage through logout persistence boundary after represented Kinah decrement and `WarehouseNpcExpands` mutation. Java `player.setWarehouseLimit()`, deeper storage limit state, DAO transaction timing, Java runtime comparison, and live socket-order validation remain unverified. |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` / `PlayerService.storePlayer` | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` | Service / Logout Persistence Boundary | Partial | Regression Tested | Needs Verification | Leave-world path passes the mutated player to `SavePlayerLogoutAsync`, preserving accepted NPC expansion fields. Java logout side effects beyond represented pending-request cleanup, online flags, and save call ordering remain broader than this test. Threading and shutdown-race behavior were not compared. |
| `com.aionemu.gameserver.dao.PlayerDAO.storePlayer` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync` | Repository / DAO | Partial | Regression Tested indirectly | Needs Verification | Existing SQL update includes `npc_expands` and `wh_npc_expands` in the same logout save statement as Java. This unit uses a capturing repository seam, not a live MySQL write/readback, so database autocommit behavior, parameter ordering against a real server, schema drift, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.setNpcExpands/setWhNpcExpands` | `Aion.GameServer.Model.GameObjects.Player.NpcExpands` / `WarehouseNpcExpands` | Runtime Model | Partial | Regression Tested | Needs Verification | In-memory mutations survive through leave-world save handoff. Java common-data observer side effects, serialization differences beyond represented packet fields, and thread-safety semantics remain unverified. |
| `com.aionemu.gameserver.model.items.storage.StorageType.CUBE/REGULAR_WAREHOUSE` | `SmCubeUpdate.CubeSize` / `SmWarehouseInfo.CreateRegularWarehouseUpdatePackets` dependencies | Storage / Packet Dependency | Partial | Existing Regression Tested | Needs Verification | Expansion response still sends represented cube/warehouse refresh packets before logout. This unit did not add new storage-limit recalculation support, Java golden bytes, encrypted frames, or live-client inventory/warehouse UI validation. |

## Tests Added Or Updated

- `PlayerEnterWorldServiceTests.LeaveWorld_PersistsAcceptedStorageExpansionFields`
  - Validates accepted cube and warehouse NPC expansion mutations are retained through the leave-world logout repository boundary.
  - Validates represented Kinah is decreased by both expansion prices.
  - Source-derived from Java `CubeExpandService`, `WarehouseService`, and `PlayerDAO.storePlayer`.
  - Does not compare against Java runtime execution or a live MySQL write/readback.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 6
- Total artifacts ported or partially modeled in this handoff window: 1 storage-expansion logout persistence-boundary coverage slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: live MySQL persistence comparison, storage limit recalculation, full real-data comparison, end-to-end dialog tests, golden/encrypted packet comparison, and live-client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- The persistence boundary is covered with a capturing repository seam only; live MySQL write/readback remains unverified.
- Java `player.setCubeLimit()` and `player.setWarehouseLimit()` deeper storage-object recalculation are still represented only by fields and outgoing packets.
- Full real `storage_expander` XML count parity and duplicate NPC id overwrite behavior remain unverified.
- End-to-end production dialog tests for action ids `47` and `48` remain missing.
- Java golden packet bytes, encrypted frames, localized client rendering, and live client validation remain unperformed.
- Logout ordering and shutdown-race behavior are not compared against Java.

## Next Recommended Unit of Work

Add an end-to-end production dialog test for `CM_DIALOG_SELECT` actions `47` and `48` using loaded storage-expander templates and targeted NPC validation, or add a real-XML count comparison for cube/warehouse expansion template loading. Keep live MySQL write/readback and storage-limit recalculation as follow-up work.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6HA-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
