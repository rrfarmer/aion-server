# Phase 6 Session 1796 Completion - Port Crafting Product Selection

Date: 2026-05-30
Unit of Work: UOW-1796
Status: Complete

## Scope

Port the smallest coherent Java `CraftService.finishCrafting` slice by carrying recipe `comboproduct` metadata into C# and adding a deterministic planner for crafted product selection, quantity, and creator-name intent.

## Completed Work

- Updated `dotnetConversion/src/Aion.GameServer/Dataholders/RecipeTemplateTable.cs`:
  - added `ComboProducts` to `RecipeTemplateSummary`
  - added Java-shaped 1-based `GetComboProduct(int)` lookup
- Updated `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`:
  - switched recipe loading to a builder so nested `<comboproduct itemid="..."/>` values are preserved
  - added explicit handling for self-closing `<recipe_template .../>` nodes after a craft-learn regression exposed that shape
- Updated `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`:
  - added non-live `CreateFinishProductPlan(...)`
  - modeled Java base-vs-combo product selection
  - preserved recipe quantity
  - modeled Java crafted-equipment creator-name intent when item-template metadata identifies weapon/armor outputs
  - kept malformed combo data conservative through `MissingComboProduct`
- Updated `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`:
  - added focused planner regressions for base product, combo product, Java combo ordering, creator intent, and conservative missing-combo handling
- Updated `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`:
  - added a real-Java-data assertion for recipe `155000078` and combo product `100200209`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~StaticDataLoadingTests|FullyQualifiedName~CraftingXpFormulaServiceTests"`
- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleUseItemAsync_CraftLearnTicketWritesCleanupSealFlagForRemainingSource|FullyQualifiedName~ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate|FullyQualifiedName~HandleUseItemAsync_DecomposeAddsRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~StaticDataLoadingTests"`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`

Result:

- Initial focused validation passed with 36 tests.
- The first two full-suite attempts hit the command timeout boundary before completion.
- A later full-suite attempt completed and exposed three game-server failures:
  - `HandleUseItemAsync_CraftLearnTicketWritesCleanupSealFlagForRemainingSource`
  - `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate`
  - `HandleUseItemAsync_DecomposeAddsRestrictedRewardWithCleanupSealFlag`
- Investigation showed the craft-learn failure was introduced by the new recipe builder not flushing self-closing `<recipe_template .../>` nodes; the parser was patched to finalize empty recipe elements immediately.
- After that fix, the focused rerun covering the craft/static-data slice plus the three previously failing game-server tests passed with 31 tests.
- The final full-suite rerun passed cleanly with 4789 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4582` game

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService`
- `com.aionemu.gameserver.model.templates.recipe.RecipeTemplate`
- `com.aionemu.gameserver.model.templates.recipe.ComboProduct`

## Migration Parity Table - UOW-1796

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `RecipeTemplate.comboproduct` + `getComboProduct(int)` | `RecipeTemplateSummary.ComboProducts` + `GetComboProduct(int)` + `StaticData` recipe parsing | Static Data / Lookup Surface | Complete | Regression Tested | Verified Parity | Combo products are now stored in Java order, looked up with Java’s 1-based indexing, and proven against both fixture data and real Java static data. |
| `CraftService.finishCrafting` product-selection branch | `CraftService.CreateFinishProductPlan` | Deterministic Result Planner | Partial | Unit Tested | Partial Parity | Base vs combo product selection and quantity are source-shaped, but live crafted-item persistence and XP/cooldown branches remain unported. |
| Java crafted equipment `changeItem(...)` creator branch | `CraftService.CreateFinishProductPlan` creator-name output | Deterministic Mutation Intent | Partial | Unit Tested | Partial Parity | Creator-name intent is now modeled for weapon/armor outputs when item-template metadata is present. This is planner-only evidence. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreateFinishProductPlan_UsesBaseProductWhenCraftDoesNotCrit` | Non-critical crafting uses the base recipe product, preserves quantity, and avoids creator intent for non-equipment outputs. | Java `CraftService.finishCrafting` | Unit | No live item creation. |
| `CreateFinishProductPlan_UsesComboProductAndMarksCreatorForWeapons` | Critical crafting uses combo product `1` and carries creator-name intent for weapon outputs. | Java `CraftService.finishCrafting` + `changeItem(...)` | Unit | No live persistence path. |
| `CreateFinishProductPlan_UsesComboIndexInJavaOrder` | Combo lookup remains Java-ordered and 1-based. | Java `RecipeTemplate.getComboProduct(int)` | Unit | No malformed-data Java runtime comparison. |
| `CreateFinishProductPlan_ReportsMissingComboProductConservatively` | Missing combo-product metadata is surfaced conservatively. | Java precondition review for `finishCrafting` | Unit | Intentionally not a Java malformed-data runtime clone. |
| `DataManager_LoadsRealJavaStaticDataManifestCounts` | Real Java recipe `155000078` retains both base and combo product data after load. | Java `recipe_templates.xml` | Regression | Spot-check only. |

## Risks / Gaps

- The live `finishCrafting` runtime is still missing: recipe deletion, fail-craft quest hook, skill XP, player XP, crafted-item persistence, log output, and craft cooldown.
- Missing combo-product metadata currently produces a conservative planner status rather than any malformed-data Java failure mode.
- Only one representative real combo recipe is explicitly asserted in the static-data bridge tests.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped rows in this unit.
- Total artifacts ported: 1 combo-product static-data surface, 1 deterministic craft-result planner, 1 creator-name intent branch, and 5 focused tests/regressions.
- Total artifacts with verified parity: 1 grouped row.
- Total artifacts needing verification: 2 grouped rows.
- Total blocked artifacts: live `finishCrafting` persistence/xp/cooldown integration.
- Estimated overall migration completion: Phase 6 remains about 73%.

## Next Recommended Unit of Work

- Port the next smallest live Java `CraftService.finishCrafting` boundary, most likely the crafted-item add/creator mutation path before widening into XP grants or cooldown persistence.
- Safe alternatives if a different isolated slice is preferred:
  - execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`
  - `DropRegistrationService.calculateBoostDropRate`
  - return to the deferred `TemperingEffect.apply/endEffect` ownership surface only if a narrower deterministic slice becomes obvious

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/RecipeTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1796-Completion.md`
- `docs/Phase-6-Session-1796-Handoff.md`
