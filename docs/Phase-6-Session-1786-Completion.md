# Phase 6 Session 1786 Completion - Item Persistent-State Transitions

Date: 2026-05-30
Unit of Work: UOW-1786
Status: Complete

## Scope

Port the narrow Java `Item.setPersistentState` transition rules and apply them to the adjacent live item mutation paths that were silently resetting or overstating dirty state, especially the current identify/reidentify planners and `EquipmentService` item copies.

## Completed Work

- Updated `dotnetConversion/src/Aion.GameServer/Model/GameObjects/InventoryItem.cs`:
  - added `TransitionPersistentState`
- Updated dirty-state planner copies:
  - `dotnetConversion/src/Aion.GameServer/Services/IdentifyItemExecutionPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/TuneResultApplicationPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/TuningActionExecutionPlanService.cs`
  - these now request `UpdateRequired` through the Java-shaped transition helper instead of assigning the enum directly
- Updated `dotnetConversion/src/Aion.GameServer/Services/EquipmentService.cs`:
  - preserved `PendingTuneResult`
  - preserved unchanged `PersistentState`
  - applied Java-shaped `UpdateRequired` transitions when count, slot, equip state, or soul-bind state changed
- Added or updated focused tests:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerInventoryPersistentStateTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/EquipmentServiceTests.cs`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerInventoryPersistentStateTests|FullyQualifiedName~EquipmentServiceTests|FullyQualifiedName~IdentifyItemExecutionPlanServiceTests|FullyQualifiedName~TuneResultApplicationPlanServiceTests|FullyQualifiedName~TuningActionExecutionPlanServiceTests"`
- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate"`
- `dotnet test dotnetConversion\AionServer.slnx`

Result:

- Focused transition, equipment, and retuning validation passed with 51 tests.
- The first full-suite run failed in `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate`.
- That failing test then passed immediately in isolation with 1 test.
- The second full-suite rerun passed cleanly with 4758 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4551` game

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.gameobjects.Item`
- `com.aionemu.gameserver.model.gameobjects.player.Player.getDirtyItemsToUpdate`
- `com.aionemu.gameserver.model.items.storage.Storage`
- `com.aionemu.gameserver.model.gameobjects.player.Equipment`
- `com.aionemu.gameserver.services.item.ItemActionService`
- `com.aionemu.gameserver.model.templates.item.actions.TuningAction`

## Migration Parity Table - UOW-1786

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.Item.setPersistentState` | `Aion.GameServer.Model.GameObjects.InventoryItem.TransitionPersistentState` | Item Dirty-State Transition Helper | Complete | Unit Tested | Verified Parity | C# now mirrors the Java item-level transition rules with direct unit coverage. |
| `com.aionemu.gameserver.services.item.ItemActionService.identifyItem` + `com.aionemu.gameserver.services.item.ItemActionService.applyTuneResult` + `com.aionemu.gameserver.model.templates.item.actions.TuningAction.act` dirty-item transition behavior | `Aion.GameServer.Services.IdentifyItemExecutionPlanService` + `TuneResultApplicationPlanService` + `TuningActionExecutionPlanService` | Runtime Mutation Dirty-State Bridge | Partial | Regression Tested | Partial Parity | C# planner copies now use the Java-shaped transition helper, so newly created items remain `New` instead of being over-promoted. |
| `com.aionemu.gameserver.model.gameobjects.player.Equipment.equipItem` + `unEquipItem` + `switchHands` + `usePowerShard` item mutation boundary | `Aion.GameServer.Services.EquipmentService` | Equipment Item Mutation Service | Partial | Regression Tested | Partial Parity | C# equipment copies now preserve preview state and dirty-state semantics for the touched equip-related mutations, but Java equipment-level persistent state is still not modeled directly. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getDirtyItemsToUpdate` equipment participation assumption | `Aion.GameServer.Model.GameObjects.Player.GetDirtyItemsToUpdate` + `Aion.GameServer.Services.EquipmentService` | Dirty-State Harvest Participation | Partial | Regression Tested | Partial Parity | Equipped items represented inside `InventoryItems` now keep correct item-level dirty state through current equipment mutations, but storage/equipment aggregation is still incomplete. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `TransitionPersistentState_MirrorsJavaItemStateRules` | The C# item-state helper follows Java transition semantics for `NEW`, `UPDATE_REQUIRED`, `DELETED`, and `UPDATED`. | Java `Item.setPersistentState` source | Unit | No storage-level side effects |
| `ChangeEquipment_EquipsOneHandWeaponInMainHandWithoutDualWieldSkill` | Equipping a `NEW` item keeps it `NEW`. | Java `Equipment.equipItem` + `Item.setPersistentState` source | Regression | No storage-level aggregation proof |
| `ChangeEquipment_UnequipsUpdatedItemAndMarksItUpdateRequired` | Unequipping an already persisted item marks it `UpdateRequired`. | Java `Equipment.unEquipItem` + `Item.setPersistentState` source | Regression | No delete-side-list proof |

## Risks / Gaps

- Java `Storage.deletedItems`, storage-level `PersistentState`, and `equipment.getPersistentState()` are still absent in C#.
- Only the touched retuning/identify/equipment paths were updated to use or preserve the new transition semantics in this unit.
- Dirty-row filtered persistence is still missing; logout remains snapshot-based.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows in this unit.
- Total artifacts ported: 1 item transition helper, 3 planner transition updates, 1 equipment copy-path preservation update, and 3 focused test updates/additions.
- Total artifacts with verified parity: 1 grouped row.
- Total artifacts needing verification: 3 grouped rows.
- Total blocked artifacts: storage deleted-item queues, storage/equipment-level dirty-state participation, and Java-shaped filtered persistence writes.
- Estimated overall migration completion: Phase 6 remains about 72%.

## Next Recommended Unit of Work

- Port the minimum modeled `Storage.deletedItems` / filtered dirty-row behavior needed for `Player.getDirtyItemsToUpdate` to represent Java deletions and removals more honestly for cube, warehouse, and account warehouse items.
- Safe alternatives if a different isolated slice is preferred:
  - execute the opt-in MySQL logout persistence test path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`
  - `CraftService.finishCrafting` product selection
  - `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/InventoryItem.cs`
- `dotnetConversion/src/Aion.GameServer/Services/IdentifyItemExecutionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TuneResultApplicationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TuningActionExecutionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/EquipmentService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerInventoryPersistentStateTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/EquipmentServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1786-Completion.md`
- `docs/Phase-6-Session-1786-Handoff.md`
