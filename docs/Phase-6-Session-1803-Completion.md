# Phase 6 Session 1803 Completion - Add Craft Stance and Inventory Guards

Date: 2026-05-31
Unit of Work: UOW-1803
Status: Complete

## Scope

Port the next deterministic Java `CraftService.checkCraft` validation slice after target and DP guards: ride/hide current-stance rejection and inventory-full rejection. This unit remains planner-level and intentionally stops before live `CraftService.startCrafting` execution, cancel packet fanout, recipe ownership, cooldown, skill, material, bonus item, DP spend, task interval, and scheduler startup.

## Completed Work

- Extended `CraftService.CreateStartCraftingValidationPlan(...)` with Java guard ordering for:
  - `player.isInPlayerMode(PlayerMode.RIDE) || player.isInAnyHide()`
  - `player.getInventory().isFull()`
- Added `CraftStartValidationStatus.InvalidCurrentStance` and `CraftStartValidationStatus.InventoryFull`.
- Added `FailurePacket` to `CraftStartValidationPlan` so modeled Java system-message packet evidence can travel with the plan.
- Added `SmSystemMessage.CraftCannotCombineWhileInCurrentStance()` for Java message `1300122`.
- Added `SmSystemMessage.CombineInventoryFull()` for Java message `1330037`.
- Added focused tests proving DP-before-stance ordering, stance-before-inventory ordering, ride/hide handling, full cube handling, and exact system-message IDs.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused craft/packet tests passed with 265 tests.
- Broad game-server suite excluding the previously order-sensitive `GameServerConnectionInventoryExpansionUseItemTests` passed with 4530 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.model.actions.PlayerMode.RIDE`
- `com.aionemu.gameserver.model.gameobjects.Creature.isInAnyHide`
- `com.aionemu.gameserver.model.items.storage.Storage.isFull`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`

## Migration Parity Table - UOW-1803

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.checkCraft` current-stance guard | `CraftService.CreateStartCraftingValidationPlan` `InvalidCurrentStance` branch | Validation Guard | Partial | Unit Tested | Partial Parity | Java source reviewed; C# checks ride mode and any hide after DP guard and attaches `STR_SKILL_CAN_NOT_COMBINE_WHILE_IN_CURRENT_STANCE`. Live packet sending still waits for start-craft orchestration. |
| `CraftService.checkCraft` inventory-full guard | `CraftService.CreateStartCraftingValidationPlan` `InventoryFull` branch | Validation Guard | Partial | Unit Tested | Partial Parity | Java source reviewed; C# checks current cube capacity after stance guard and attaches `STR_COMBINE_INVENTORY_IS_FULL`. Java storage internals are approximated through current `InventoryCapacity`. |
| `SM_SYSTEM_MESSAGE.STR_SKILL_CAN_NOT_COMBINE_WHILE_IN_CURRENT_STANCE` | `SmSystemMessage.CraftCannotCombineWhileInCurrentStance` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id `1300122` verified through packet/system-message tests. |
| `SM_SYSTEM_MESSAGE.STR_COMBINE_INVENTORY_IS_FULL` | `SmSystemMessage.CombineInventoryFull` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id `1330037` verified through packet/system-message tests. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateStartCraftingValidationPlan_RejectsRideOrHideAfterDpValidation` | Unit Added | Java `checkCraft` DP guard before ride/hide guard | Insufficient DP wins before stance; sufficient-DP ride and hide both produce current-stance failure with message id `1300122`. | Source-derived planner regression plus system-message ID evidence. | Live send/cancel orchestration remains pending. |
| `CreateStartCraftingValidationPlan_RejectsInventoryFullAfterStanceValidation` | Unit Added | Java `checkCraft` stance guard before inventory-full guard | Ride status wins before inventory-full; full cube produces inventory-full failure with message id `1330037`. | Source-derived planner regression plus system-message ID evidence. | Inventory full uses current C# cube capacity model. |
| `GamePacketTests` system-message assertions | Unit Updated | Java `SM_SYSTEM_MESSAGE` constants | New C# factories preserve Java message ids `1300122` and `1330037`. | Packet/system-message assertion evidence. | No runtime Java packet capture in this unit. |

## Risks / Gaps

- No live `CraftService.startCrafting` execution.
- `FailurePacket` is planner evidence only; no live packet fanout is wired in this unit.
- Cancel packet pair from UOW-1802 is still not sent on these failures.
- Recipe ownership, cooldown, skill presence/level, material validation/consumption, bonus item consumption, DP spend, task interval, scheduler startup, and craft completion remain pending.
- Inventory fullness uses the current C# `InventoryCapacity` model; first-class Java storage behavior is still incomplete.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows in this unit.
- Total artifacts ported: 2 validation branches, 2 status values, 2 system-message factories, and 3 focused test updates.
- Total artifacts with verified parity: 2 system-message factory rows.
- Total artifacts needing verification: 2 grouped validation rows pending live orchestration.
- Total blocked artifacts: live start-craft execution, live validation failure fanout, recipe/cooldown/skill/material validation, DP spend, and scheduler startup.
- Estimated overall migration completion: Phase 6 remains about 73%.

## Next Recommended Unit of Work

- Port the next smallest `CraftService.checkCraft` validation slice: recipe ownership and cooldown guard planning, including `STR_COMBINE_CAN_NOT_FIND_RECIPE` and `STR_ITEM_CANT_USE_UNTIL_DELAY_TIME`, still without material mutation or scheduler startup.
- Safe alternatives:
  - wire non-live validation failure orchestration that combines `FailurePacket` and cancel packet plans without live sending
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - execute the opt-in MySQL logout delete/retuning persistence path with `AION_GAMESERVER_DB_INTEGRATION=1`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1803-Completion.md`
- `docs/Phase-6-Session-1803-Handoff.md`
