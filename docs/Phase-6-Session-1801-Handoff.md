# Phase 6 Session 1801 Handoff - Craft Start Early Validation Planner

Date: 2026-05-31
Unit of Work: UOW-1801
Status: Completed

## What Changed

- Added `CraftService.CreateStartCraftingValidationPlan(...)`.
- Added `CraftStartValidationPlan` and `CraftStartValidationStatus`.
- Covered the earliest deterministic Java `CraftService.startCrafting` / `checkCraft` guard branches:
  - missing player
  - missing recipe
  - missing product item template
  - in-progress crafting task
  - non-morph missing/non-static target
  - non-morph tool range failure
  - morph target bypass for skill `40009`
  - ready-for-next-validation continuation
- Added focused `CraftServiceTests` coverage.

## Files Changed

- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1801-Completion.md`
- `docs/Phase-6-Session-1801-Handoff.md`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.startCrafting`
- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.model.gameobjects.StaticObject`
- `com.aionemu.gameserver.utils.PositionUtil`

## C# Artifacts Touched

- `Aion.GameServer.Services.CraftService`
- `Aion.GameServer.Services.CraftStartValidationPlan`
- `Aion.GameServer.Services.CraftStartValidationStatus`
- `Aion.GameServer.Tests.CraftServiceTests`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests" --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag" --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused `CraftServiceTests` passed with 18 tests.
- Full-suite attempts built successfully and passed commons/chat/login, but failed two unrelated tests in `GameServerConnectionInventoryExpansionUseItemTests` during each full game test run.
- The initially failed inventory tests passed when rerun directly with 2 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4524 tests.

## Parity Table Summary

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.startCrafting` recipe/product lookup and early `checkCraft` guards | `CraftService.CreateStartCraftingValidationPlan` | Deterministic Validation Planner | Partial | Unit Tested | Partial Parity | Covers missing recipe/product, in-progress crafting, morph target bypass, non-morph static target and range guard branches. Downstream checks and live side effects remain pending. |
| `CraftService.checkCraft` morph target bypass for skill `40009` | `CraftStartValidationPlan.MorphSubstancesSkillId` branch | Validation Guard | Complete for this branch | Unit Tested | Verified Parity | Java source reviewed; morph recipes skip static-object target validation and continue to later guards. |
| `CraftService.checkCraft` non-morph static tool guard | `CraftService.CreateStartCraftingValidationPlan` target facts | Validation Guard | Partial | Unit Tested | Partial Parity | Static target identity is represented by caller-provided facts until first-class C# `StaticObject` exists. |

## Known Gaps

- No live `CraftService.startCrafting` execution.
- No `sendCancelCraft` packet fanout.
- No DP, stance, inventory, recipe-list, cooldown, skill, material, bonus-item, task interval, or scheduler behavior.
- No first-class C# `StaticObject` craft-station model.
- Full-suite validation is currently affected by unrelated order-sensitive inventory/use-item tests.

## Risks

- The next start-craft slice will touch cancellation packet composition and may need careful packet ordering relative to `SM_CRAFT_UPDATE` and `SM_CRAFT_ANIMATION`.
- DP validation already has a focused mutation boundary, but it must only be consumed after the earlier Java guards succeed.
- Do not claim full start-craft parity until the material and scheduler path has runtime evidence.

## Next Recommended Unit of Work

- Next sequential task: port the next smallest `CraftService.startCrafting` validation slice after the early target planner, likely DP requirement planning and cancel-craft packet plan composition, reusing existing `SpendRecipeDpForCraftStartAsync` only after validation succeeds.

Safe alternative candidates:

- Investigate and stabilize the order-sensitive `GameServerConnectionInventoryExpansionUseItemTests` failures seen during full-suite runs.
- Execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftingTaskPacketPlanService.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before choosing the next UOW.
- Re-inspect Java `CraftService.startCrafting`, `checkCraft`, `sendCancelCraft`, `SM_CRAFT_UPDATE`, and `SM_CRAFT_ANIMATION`.
- Preserve the guard ordering: early validation must succeed before DP spend or task scheduling.
