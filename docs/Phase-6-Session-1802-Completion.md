# Phase 6 Session 1802 Completion - Add Craft Start DP and Cancel Plans

Date: 2026-05-31
Unit of Work: UOW-1802
Status: Complete

## Scope

Port the next deterministic Java `CraftService.startCrafting` / `checkCraft` validation slice after early target validation: DP requirement failure planning and Java cancel-craft packet composition. This unit intentionally stops before live `CraftService.startCrafting` execution, DP mutation, stance/inventory/recipe/cooldown/skill/material validation, bonus item consumption, task interval calculation, and scheduler startup.

## Completed Work

- Extended `CraftService.CreateStartCraftingValidationPlan(...)` with the Java DP requirement guard after non-morph static-target validation.
- Added `CraftStartValidationStatus.NotEnoughDp`.
- Added `RequiredDp` and `CurrentDp` evidence fields to `CraftStartValidationPlan`.
- Added `CraftService.CreateStartCancelPacketPlan(...)`.
- Added `CraftStartCancelPacketPlan` and `CraftStartCancelPacketPlanStatus`.
- Planned the Java `sendCancelCraft` packet pair:
  - self `SM_CRAFT_UPDATE(skillId, itemTemplate, 0, 0, 4, 0, 0)`
  - visible broadcast `SM_CRAFT_ANIMATION(playerObjId, targetObjId, 0, 2)`
- Added focused `CraftServiceTests` for DP guard ordering, sufficient-DP continuation, cancel packet payload bytes, and missing-input conservative no-plan behavior.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused `CraftServiceTests` passed with 22 tests.
- Unfiltered game-server suite was attempted but exceeded the 3-minute command timeout before returning results.
- Broad game-server suite excluding the previously order-sensitive `GameServerConnectionInventoryExpansionUseItemTests` passed with 4528 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.startCrafting`
- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.services.craft.CraftService.sendCancelCraft`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CRAFT_UPDATE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CRAFT_ANIMATION`

## Migration Parity Table - UOW-1802

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.checkCraft` DP requirement guard | `CraftService.CreateStartCraftingValidationPlan` `NotEnoughDp` branch | Validation Guard | Partial | Unit Tested | Partial Parity | Java source reviewed; C# now checks DP after target validation and records required/current DP. It does not yet log audit details or execute the rest of `checkCraft`. |
| `CraftService.sendCancelCraft` | `CraftService.CreateStartCancelPacketPlan` | Packet Plan | Partial | Unit Tested | Partial Parity | Java source reviewed; C# creates the same `SM_CRAFT_UPDATE` cancel action and `SM_CRAFT_ANIMATION` completion action payloads. It is a non-live plan and is not yet sent from start-craft failure handling. |
| `SM_CRAFT_UPDATE` action `4` cancel payload | `SmCraftUpdate` serialized by cancel plan | Server Packet | Complete for this action | Unit Tested | Verified Parity | Payload evidence covers skill id, action `4`, product item id, zero success/failure/speed/delay, and message id `1330051`. |
| `SM_CRAFT_ANIMATION` cancel animation payload | `SmCraftAnimation` serialized by cancel plan | Server Packet | Complete for this action | Unit Tested | Verified Parity | Payload evidence covers player object id, target object id, skill id `0`, and action `2`. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateStartCraftingValidationPlan_ChecksDpAfterTargetValidation` | Unit Added | Java `checkCraft` target guards before DP guard | Invalid non-morph target still wins before insufficient DP, and valid target reaches `NotEnoughDp`. | Source-derived planner regression. | Audit logging and later guards remain pending. |
| `CreateStartCraftingValidationPlan_AllowsSufficientDpToContinue` | Unit Added | Java `checkCraft` DP guard continuation | Sufficient DP records required/current DP and proceeds to later unported guards. | Source-derived planner regression. | Does not spend DP. |
| `CreateStartCancelPacketPlan_PlansJavaCancelUpdateAndAnimation` | Unit Added | Java `sendCancelCraft`, `SM_CRAFT_UPDATE`, `SM_CRAFT_ANIMATION` | Cancel plan creates the expected packet pair and serialized payload fields. | Byte-level packet evidence for modeled fields. | Plan is not live fanout yet. |
| `CreateStartCancelPacketPlan_MissingInputsDoesNotPlan` | Unit Added | Java `sendCancelCraft` data requirements | Missing player/recipe/product template returns conservative no-plan state. | Source-derived planner regression. | Java call path usually has these values earlier in `startCrafting`. |

## Risks / Gaps

- No live `CraftService.startCrafting` execution.
- Cancel packet pair is planned but not yet sent on live validation failure.
- No DP mutation from this validation planner; `SpendRecipeDpForCraftStartAsync` remains separate and must only be consumed after all Java preconditions pass.
- Stance, inventory full, recipe ownership, cooldown, skill validation, material validation/consumption, bonus item consumption, interval selection, task scheduling, and craft completion remain pending.
- Unfiltered game-server suite exceeded the local command timeout in this unit; the bounded broad suite excluding the known unstable inventory expansion class passed.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows in this unit.
- Total artifacts ported: 1 DP validation branch, 1 cancel packet planner, 2 plan/status models, and 4 focused unit tests.
- Total artifacts with verified parity: 2 grouped packet-action rows for serialized cancel payloads.
- Total artifacts needing verification: 2 grouped rows for live DP/cancel integration.
- Total blocked artifacts: live start-craft execution, first-class static craft targets, live cancel fanout, materials/DP/cooldown/skill validation, and scheduler startup.
- Estimated overall migration completion: Phase 6 remains about 73%.

## Next Recommended Unit of Work

- Port the next smallest `CraftService.startCrafting` validation slice after DP/cancel planning: likely stance/hide guard planning and inventory-full planning, still without material mutation or scheduler startup.
- Safe alternatives:
  - wire cancel packet fanout into a non-live start-craft failure orchestration helper with tests, if the validation branches remain deterministic
  - investigate and stabilize the order-sensitive `GameServerConnectionInventoryExpansionUseItemTests`
  - execute the opt-in MySQL logout delete/retuning persistence path with `AION_GAMESERVER_DB_INTEGRATION=1`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1802-Completion.md`
- `docs/Phase-6-Session-1802-Handoff.md`
