# Phase 6 Session 1834 Completion - Add Finish Craft Exception-Risk Diagnostic

Date: 2026-05-31
Unit of Work: UOW-1834
Status: Complete

## Scope

Inspect Java finish-craft exception/null behavior around missing combo products and missing item templates, then add a disabled C# diagnostic with tests before any live execution wiring.

## Completed Work

- Inspected Java `RecipeTemplate.getComboProduct(int)`.
- Inspected Java `CraftService.finishCrafting` product selection and work-order fail-craft combo lookup.
- Inspected Java `ItemService.addItem` missing item-template guard.
- Added `CraftService.CreateFinishExceptionRiskPlan(...)`.
- Added `CraftFinishExceptionRiskPlan`.
- Added `CraftFinishExceptionRiskStatus`.
- Recorded Java exception-shaped edges without throwing:
  - combo-product list present but missing index can throw `IndexOutOfBoundsException`
  - critical product selection with no combo list can throw `NullPointerException` by unboxing null
  - missing item template can throw `NullPointerException` through `Objects.requireNonNull`
- Added focused tests for each exception-risk branch and for no-known-risk behavior.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftingXpFormulaServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused craft tests passed with 80 tests.
- Standard CM_CRAFT/craft/packet/craft-XP/static-data tests passed with 372 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4596 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.templates.recipe.RecipeTemplate.getComboProduct`
- `com.aionemu.gameserver.services.craft.CraftService.finishCrafting`
- `com.aionemu.gameserver.services.item.ItemService.addItem`

## Migration Parity Table - UOW-1834

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `RecipeTemplate.getComboProduct` exception behavior | `CraftService.CreateFinishExceptionRiskPlan` | Exception Diagnostic | Partial | Unit Tested | Partial Parity | C# records Java index/null exception edges without throwing. |
| `CraftService.finishCrafting` product selection | `CraftFinishExceptionRiskPlan` | Exception Diagnostic | Partial | Unit Tested | Partial Parity | C# records product-selection null/index risks; existing disabled planners remain conservative. |
| `ItemService.addItem` missing-template guard | `CraftFinishExceptionRiskStatus.JavaWouldThrowMissingItemTemplateAtAddItem` | Exception Diagnostic | Partial | Unit Tested | Partial Parity | C# records Java `Objects.requireNonNull` missing-template behavior; live exception propagation remains unwired. |

## Risks / Gaps

- The exception-risk diagnostic does not throw.
- Existing disabled finish-craft planners still return conservative statuses for exception-shaped edges.
- Live finish-craft exception propagation remains unwired.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Begin live logout craft cooldown save design only after explicit connection/error behavior scoping, using the existing disabled cooldown persistence plans as evidence.
- Safe alternatives:
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`
  - add a disabled finish-craft live-execution readiness checklist
  - inspect Java `ItemService.addItem` storage insertion/packet behavior in more detail before reward live wiring

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1834-Completion.md`
- `docs/Phase-6-Session-1834-Handoff.md`
