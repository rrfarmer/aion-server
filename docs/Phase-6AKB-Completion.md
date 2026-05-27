# Phase 6AKB Completion - Extraction Planner Missing Reward Guard

Date: 2026-05-27
Unit of Work: UOW-1452
Status: Complete after validation.

## Scope

Pin the pure service-level C# extraction planner behavior for missing reward templates. This unit is test/documentation-only.

## Completed Work

- Added `CreateBreakItemPlan_MissingRewardTemplateFailsBeforeConsumption`, a service-level test that pins C# `MissingRewardTemplate` preflight behavior.
- The test uses a minimal item-template table containing the extraction tool and target armor but not the derived Alpha extraction reward template.
- Asserted the C# planner returns `BreakItemFailure.MissingRewardTemplate`, records the derived Alpha reward id, emits no mutation plan, and leaves the player's target/tool inventory untouched.
- No production code changed in this unit.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~EnchantServiceTests.CreateBreakItemPlan"`.
- Result: passed 9 tests.

## Migration Parity Table - UOW-1452

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.EnchantService.breakItem` | `Aion.GameServer.Services.EnchantService.CreateBreakItemPlan` | Service / Extraction Planner | Partial | Unit Tested | Intentional Difference | C# now has service-level coverage that missing reward templates fail before target/tool consumption. Java computes the same stone id but consumes before `ItemService.addItem` throws if the template is absent. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` | `InventoryAddService.CreateAddItemPlan` / reward template preflight | Reward Add Service | Partial | Unit Tested | Intentional Difference | Java `Objects.requireNonNull(itemTemplate)` is post-consumption for break-item extraction. C# keeps reward-template resolution before mutation planning for transaction safety. |
| `com.aionemu.gameserver.model.enchants.EnchantmentStone` | `EnchantService.GetExtractionStoneItemId` / Alpha reward id assertion | Enum / Threshold Mapping | Partial | Unit Tested | Partial Parity | Missing-template test asserts the derived reward id is Alpha (`166000191`) with deterministic zero RNG. Full threshold coverage remains in UOW-1450. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateBreakItemPlan_MissingRewardTemplateFailsBeforeConsumption` | Unit / intentional difference | `EnchantService.breakItem`, `ItemService.addItem` | Missing reward template returns `MissingRewardTemplate`, keeps derived reward id, emits no mutation inventory, and leaves target/tool untouched. | Deterministic C# assertion against reviewed Java consume-then-throw branch, documented as intentional difference. | Does not mirror Java's unsafe post-consumption exception; no Java runtime artifact. |
| Existing `CreateBreakItemPlan*` tests | Existing Unit | `EnchantService.breakItem` | Weapon/armor reward, threshold, and guard paths remained stable. | 9-test focused service slice passed. | Full Java runtime comparison remains blocked. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Missing reward-template behavior remains an intentional C# difference for transaction safety.
- Java post-guard storage races remain hard to represent in the pure C# planner.
- Full packet bytes remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts changed in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 2 grouped rows
- Total blocked artifacts: Java runtime artifact generation, post-guard storage race reproduction, Java packet byte comparisons
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: switch back to toy-pet/kisk only if a fixture-level scheduled-task hook is acceptable.
- Otherwise continue low-churn extraction planner coverage by documenting the post-guard storage-race gap and moving on to the next Phase 6 backlog item.

## Suggested Acceptance Criteria

- Avoid production changes unless a small, clearly Java-breadcrumbed seam already exists.
- If touching toy-pet/kisk lifetime scheduling, observe scheduled delay/order without relying on long real-time waits.
- If staying on extraction, keep the C# transaction-safety difference explicit and do not mark it verified parity.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Toy-pet lifetime schedule observability design | `ToyPetSpawnAction`, `Kisk`, connection test fixture | Medium | Read-only first; current fixture lacks schedule metadata. |
| B | Extraction post-guard race documentation | `EnchantService.breakItem`, `Storage.delete/decreaseByObjectId` | Low | Could be docs-only if no pure seam exists. |
| C | Next Phase 6 backlog slice | progress `Next Steps` | Medium | Pick a narrow pre-existing planner/test surface. |

## Do Not Parallelize

- Shared `GameServerConnection.cs` item-use changes.
- Shared extraction test fixtures.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1452] Cover extraction planner reward guard`.
- C# files changed in UOW-1452:
  - `dotnetConversion/tests/Aion.GameServer.Tests/EnchantServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKB-Completion.md`
