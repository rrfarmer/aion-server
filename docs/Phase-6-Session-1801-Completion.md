# Phase 6 Session 1801 Completion - Add Craft Start Early Validation Planner

Date: 2026-05-31
Unit of Work: UOW-1801
Status: Complete

## Scope

Port the smallest deterministic Java `CraftService.startCrafting` / early `checkCraft` validation slice below live mutation: recipe/product readiness, in-progress crafting guard, morph target bypass, and non-morph static-tool target validation. This unit intentionally stops before DP spend, player stance, inventory fullness, recipe ownership, cooldowns, skills, materials, bonus item consumption, and scheduler startup.

## Completed Work

- Added `CraftService.CreateStartCraftingValidationPlan(...)`.
- Added `CraftStartValidationPlan` and `CraftStartValidationStatus`.
- Modeled Java early guard ordering for:
  - missing player
  - missing recipe
  - missing product item template
  - existing in-progress crafting task
  - non-morph missing/non-static target
  - non-morph tool range failure
  - morph recipe bypass for skill `40009`
  - ready-for-next-validation continuation
- Added focused `CraftServiceTests` for each modeled branch.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests" --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag" --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused `CraftServiceTests` passed with 18 tests.
- Full-suite attempts built successfully and passed commons/chat/login, but failed two unrelated tests in `GameServerConnectionInventoryExpansionUseItemTests` during each full game test run.
- The initially failed inventory tests passed when rerun directly with 2 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4524 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.startCrafting`
- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.model.gameobjects.StaticObject`
- `com.aionemu.gameserver.utils.PositionUtil`

## Migration Parity Table - UOW-1801

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.startCrafting` recipe/product lookup and early `checkCraft` guards | `CraftService.CreateStartCraftingValidationPlan` | Deterministic Validation Planner | Partial | Unit Tested | Partial Parity | Java source reviewed; C# planner covers missing recipe/product, in-progress crafting, morph target bypass, non-morph static-target and range guard branches. It does not execute Java `sendCancelCraft`, DP, stance, inventory, recipe-list, cooldown, skill, material, bonus-item, task interval, or scheduler behavior. Java `recipeTemplate` null behavior is conservative in C# because Java dereferences the recipe before `checkCraft`. |
| `CraftService.checkCraft` morph target bypass for skill `40009` | `CraftStartValidationPlan.MorphSubstancesSkillId` branch | Validation Guard | Complete for this branch | Unit Tested | Verified Parity | Java source reviewed; morph recipes skip static-object target validation and continue to later guards. |
| `CraftService.checkCraft` non-morph static tool guard | `CraftService.CreateStartCraftingValidationPlan` target facts | Validation Guard | Partial | Unit Tested | Partial Parity | C# represents target existence/static/range facts supplied by callers. First-class Java `StaticObject` identity and bound-radius overload details remain pending. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateStartCraftingValidationPlan_ReportsMissingRecipeOrProductTemplate` | Unit Added | Java `startCrafting` / `checkCraft` null guards | Missing recipe/product template resolves to cancel-ready failure plans. | Source-derived planner regression. | C# handles missing recipe conservatively because Java dereferences earlier. |
| `CreateStartCraftingValidationPlan_RejectsInProgressBeforeTargetValidation` | Unit Added | Java `checkCraft` crafting task guard | Existing in-progress crafting fails before target validation. | Source-derived planner regression. | No live `CraftingTask` object yet. |
| `CreateStartCraftingValidationPlan_AllowsMorphRecipeWithoutStaticTarget` | Unit Added | Java `checkCraft` morph branch | Skill `40009` bypasses static-object target checks. | Source-derived planner regression. | Later material/DP/skill guards not included. |
| `CreateStartCraftingValidationPlan_RejectsNonMorphMissingOrNonStaticTarget` | Unit Added | Java `checkCraft` non-morph target guard | Non-morph crafting requires a static target fact. | Source-derived planner regression. | StaticObject identity is represented by an input fact. |
| `CreateStartCraftingValidationPlan_RejectsNonMorphTargetTooFar` | Unit Added | Java `PositionUtil.isInRange(player, target, 5, false)` branch | Non-morph crafting fails when the static tool is too far. | Source-derived planner regression. | Range calculation itself is supplied by caller. |
| `CreateStartCraftingValidationPlan_NonMorphStaticTargetContinuesToLaterGuards` | Unit Added | Java `checkCraft` continuation after static target guard | Valid non-morph target proceeds to later unported guards. | Source-derived planner regression. | Later validation branches remain pending. |

## Risks / Gaps

- No live `CraftService.startCrafting` execution.
- No `sendCancelCraft` packet fanout from this planner.
- No DP, stance, inventory, recipe-list, cooldown, skill, material, bonus-item, task interval, or scheduler behavior in this unit.
- Full-suite validation currently exposes unrelated order-sensitive failures in `GameServerConnectionInventoryExpansionUseItemTests`; the failed tests passed in isolation, and the broad game-server suite excluding that class passed.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped rows in this unit.
- Total artifacts ported: 1 deterministic craft start validation planner, 1 validation status model, and 6 focused unit tests.
- Total artifacts with verified parity: 1 grouped row for the isolated morph target bypass branch.
- Total artifacts needing verification: 2 grouped rows.
- Total blocked artifacts: live start-craft execution, first-class static craft targets, cancel packet fanout, materials/DP/cooldown/skill validation, and scheduler startup.
- Estimated overall migration completion: Phase 6 remains about 73%.

## Next Recommended Unit of Work

- Port the next smallest `CraftService.startCrafting` validation slice after the early target planner: likely DP requirement planning and cancel-craft packet plan composition, reusing existing `SpendRecipeDpForCraftStartAsync` only after validation succeeds.
- Safe alternatives:
  - investigate and stabilize the order-sensitive `GameServerConnectionInventoryExpansionUseItemTests` failures seen during full-suite runs
  - execute the opt-in MySQL logout delete/retuning persistence path with `AION_GAMESERVER_DB_INTEGRATION=1`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1801-Completion.md`
- `docs/Phase-6-Session-1801-Handoff.md`
