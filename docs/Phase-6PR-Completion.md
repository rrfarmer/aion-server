# Phase 6PR Completion Handoff - ItemPurification Inheritance Planner

Date: May 25, 2026
Unit of Work: UOW-922
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-922] Add ItemPurification inheritance planner`)

## Status

Phase 6 is still in progress. This unit adds a pure target-item inheritance planner for Java `ItemPurificationService.upgradeItem`.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationInheritanceService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationInheritanceServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PR-Completion.md`

## What Changed

- Added `ItemPurificationInheritanceService`.
- Modeled Java target item state projection for purification upgrade.
- Added tests for copied fields, tune clamp, enchant minus 5, amplified/buff skill guards, random bonus preservation/reroll branch, and missing input guards.
- Kept live `ItemFactory`, inventory add, random-bonus runtime selection, socket mutation service, persistence, and packet fanout out of scope.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter ItemPurificationInheritanceServiceTests
```

Result: passed, 4 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1572 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Services.ItemPurificationInheritanceService.CreateTargetItemPlan` | Service / Item Mutation Planner | Partial | Regression Tested in C# | Partial Parity | Models target item inheritance as a pure projection. Live factory, inventory add, persistence, packet fanout, and failure/no-rollback behavior remain unported. |
| `com.aionemu.gameserver.model.gameobjects.Item` | `Aion.GameServer.Model.GameObjects.InventoryItem` | Model / Input Projection | Partial | Regression Tested in C# | Needs Verification | Covers currently modeled fields. Java fusion template object and item color expiration semantics are not fully modeled. |
| `com.aionemu.gameserver.model.templates.item.ItemTemplate` | `Aion.GameServer.Dataholders.ItemTemplateSummary` | Static Data DTO / Input Projection | Partial | Regression Tested in C# | Needs Verification | Consumes target max tune, max enchant, and stat-bonus set ids. Full factory/template behavior remains outside this unit. |
| `com.aionemu.gameserver.dataholders.ItemRandomBonusData.areBonusSetsEqual` | `ItemPurificationInheritanceService` stat-bonus set comparison | Static Data Helper Projection | Partial | Regression Tested in C# | Needs Verification | Uses source/target `StatBonusSetId` equality for inventory bonus sets. Full helper behavior is not ported here. |
| `com.aionemu.gameserver.model.templates.item.actions.TuningAction.getRandomStatBonusIdFor` | `ItemPurificationInheritanceService` injected reroll id | Random Selection Projection | Partial | Regression Tested in C# | Needs Verification | Runtime random selection is not implemented; selected id is caller-projected. |
| `com.aionemu.gameserver.services.item.ItemSocketService.addManaStone` | `InventoryItem.ManaStones` / `FusionStones` copied projection | Service / Socket Mutation Projection | Partial | Regression Tested in C# | Needs Verification | Copies projected socket lists rather than invoking live mutation/persistence. |
| `com.aionemu.gameserver.services.item.ItemFactory.newItem` | Not ported in this unit | Factory | Not Started for purification live path | No Tests in this unit | Unknown | Target object id is projected by caller. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | Not ported in this unit | Client Packet Handler | Not Started | Manual Analysis | Needs Verification | Live packet remains unported. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateTargetItemPlan_CopiesJavaUpgradeStateAndClampsTargetFields` | Regression | Java `ItemPurificationService.upgradeItem` source review | Validates field copy, enchant minus 5, tune clamp, amplified/buff preservation, sockets/stones/godstone, random bonus preservation, creator, color, soulbound, and tempering. | Deterministic C# regression grounded in Java source. | No live factory/inventory/persistence. |
| `CreateTargetItemPlan_DropsAmplifiedAndBuffSkillWhenTargetEnchantFallsBelowLimits` | Regression | Java amplified/buff guard source review | Validates amplified and buff skill are dropped when post-purification enchant is below target max/20 threshold. | Deterministic C# regression. | No Java runtime comparison. |
| `CreateTargetItemPlan_RerollsRandomBonusWhenInventoryBonusSetsDiffer` | Regression | Java random-bonus branch source review | Validates differing stat-bonus sets use injected reroll id. | Deterministic C# regression for branch behavior. | Random selection itself is not implemented. |
| `CreateTargetItemPlan_ReportsMissingInputs` | Guard Regression | C# planner boundary | Validates missing projected inputs fail explicitly. | Deterministic C# guard regression. | Java live null exception behavior remains unported. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Planner is a pure projection and does not allocate, add, persist, fan out, or roll back.
- Random bonus reroll selection is injected and not Java-runtime compared.
- Full `ItemRandomBonusData.areBonusSetsEqual` and `TuningAction.getRandomStatBonusIdFor` parity is not claimed.
- Java fusioned item template object and optional socket semantics are represented by current C# scalar/list projections only.
- Live `CM_ITEM_PURIFICATION`, material/base/target mutation, kinah mutation parity decision, persistence, and packet fanout remain missing.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 1 target-item inheritance planner slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, live item factory/inventory add, random-bonus selection, socket mutation persistence/fanout, live packet handler, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 68% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Target item inheritance planner | `ItemPurificationService.upgradeItem` | new planner/test files | Service Planner | No within same files | Medium | Completed in UOW-922. |
| B | Material/base/kinah mutation planner | `ItemPurificationService.decreaseMaterials` | new planner/test files | Service Planner | Maybe | Medium | Can remain pure, but must document Java partial material and kinah quirks. |
| C | Random bonus selection helper | `ItemRandomBonusData`, `TuningAction` | existing static data/services/tests | Utility/Test | Maybe | Medium | Separate if scoped to random-bonus tables only. |
| D | Live purification packet/action | `CM_ITEM_PURIFICATION` | packet/inventory/repository files | Integration | No | High | Crosses mutation, persistence, fanout, and Java bug decisions. |

## Next Recommended Unit of Work

Recommended sequential task:
- Add a pure material/base/kinah mutation planner for `ItemPurificationService.decreaseMaterials`, explicitly documenting Java's partial material-consumption risk and likely kinah no-op from `decreaseKinah(-necessaryKinah)`.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Analyze Java `Storage.decreaseByItemId`, `decreaseByObjectId`, and `decreaseKinah` edge cases | read-only Java/C# inventory model | edits, docs | Exact mutation-order report. |
| Agent B | Inspect C# inventory mutation helper patterns | read-only C# services/tests | edits, docs | Proposed pure mutation plan shape. |
| Orchestrator | Implement material/base/kinah planner if safe | new planner/test files plus docs | live packet/repository persistence | Code, tests, docs, commit. |

## Do Not Parallelize

- Live `CM_ITEM_PURIFICATION` wiring with mutation planning.
- Repository persistence with pure planner work.
- Random bonus helper changes with target inheritance planner edits.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue material/base/kinah mutation planning or another narrow AP/item caller boundary.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
