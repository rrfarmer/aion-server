# Phase 6QQ Completion Handoff - ItemPurification Random Bonus Selection Seam

Date: May 25, 2026
Unit of Work: UOW-947
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-947] Wire item purification random bonuses`)

## Status

Phase 6 is still in progress. This unit removes the ItemPurification random-bonus planning blocker for target items whose inventory stat-bonus sets differ from the source item.

The handler remains non-persistent and mostly non-mutating. It still does not mutate inventory/AP, persist changes, call the send bridge, synthesize AP rank packets, or execute full Java `ItemFactory` / `ItemSocketService` side effects.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/ItemRandomBonusTable.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationInheritanceService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationWorkflowService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemRandomBonusTableTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationInheritanceServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionItemPurificationTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QQ-Completion.md`

## What Changed

- Added `ItemRandomBonusTable.AreBonusSetsEqual`, matching Java's same-id/null-equivalence/group-count comparison.
- Threaded optional `ItemRandomBonusTable` and deterministic `Func<double>` roll through:
  - `ItemPurificationInheritanceService`
  - `ItemPurificationWorkflowService`
  - `GameServerConnection.HandleItemPurificationAsync`
- Target item inheritance now:
  - preserves the source random bonus when Java says inventory bonus sets are equal
  - selects a target-set random bonus using `SelectRandomBonusNumber` when sets differ
  - preserves `rerolledRandomBonusId` as an explicit deterministic override
- Added tests for random-bonus equality, weighted selection, inheritance behavior, and handler composition through allocation.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Random-bonus seam | `TuningAction`, `ItemRandomBonusData`, `ItemPurificationService.upgradeItem` | random bonus table, ItemPurification planner/handler/test files | Service / Utility / Tests | No | Medium | Shared ItemPurification planner and handler signatures require one writer. |
| B | Non-persistent mutation snapshot preview | `ItemPurificationService.decreaseMaterials`, storage packet fanout | likely new service/tests plus ItemPurification fixtures | Service / Tests | No with A | Medium | Depends on ready application plans and overlaps ItemPurification tests. |
| C | Kinah charge-all partial drift | `ItemChargeService` charge-all Kinah path | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Test Creation | Yes | Medium | Separate from ItemPurification files; deferred. |
| D | Java ItemPurification runtime observer design | ItemPurification runtime packet path | docs only | Documentation / Analysis | Yes | Low | Useful when Java tooling exists; no runtime parity claim possible now. |

## File Ownership Map Used

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | UOW-947 implementation, tests, docs, commit | random-bonus table, ItemPurification planner/handler/test files, progress/handoff docs | unrelated files | Code, tests, parity docs, commit |
| Explorer | Java random-bonus behavior audit | read-only repo inspection | all writes | Behavior report and parity risks |

## Sub-Agent Outputs Integrated

- Explorer confirmed Java `ItemPurificationService.upgradeItem` only rerolls source random bonus when `ItemRandomBonusData.areBonusSetsEqual(INVENTORY, sourceSet, targetSet)` is false.
- Explorer confirmed Java equality is same id, null equivalence, then modifier-group count equality, not deep modifier comparison.
- Explorer confirmed `TuningAction.getRandomStatBonusIdFor` delegates to `ItemRandomBonusData.selectRandomBonusNumber(INVENTORY, targetSet)`.
- Explorer noted C# random selection still differs from Java `Rnd` in algorithm/seed and all-zero chance edge behavior.
- Explorer was closed after integration.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionItemPurificationTests|ItemPurificationInheritanceServiceTests|ItemPurificationWorkflowServiceTests|ItemRandomBonusTableTests|StaticDataLoadingTests"
```

Result: passed, 36 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1632 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.TuningAction` | `Aion.GameServer.Services.ItemPurificationInheritanceService` | Runtime Random Selection / Planner | Partial | Regression Tested in C# | Partial Parity | C# now selects a target inventory random-bonus id using the C# random-bonus table when purification requires reroll. Live tuning scroll action timing, item-use observer behavior, pending tune results, and packet fanout remain outside this unit. |
| `com.aionemu.gameserver.dataholders.ItemRandomBonusData` | `Aion.GameServer.Dataholders.ItemRandomBonusTable` | Static Data / Utility | Partial | Unit Tested + Existing Static Data Tested | Partial Parity | Added Java-style `areBonusSetsEqual` group-count comparison and deterministic weighted 1-based selection tests. Pathological all-zero chance behavior intentionally remains conservative in C# and needs Java runtime verification if such data appears. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Services.ItemPurificationInheritanceService` plus `GameServerConnection.HandleItemPurificationAsync` | Service / Target Item Projection | Partial | Regression Tested in C# | Partial Parity | Target projection now preserves random bonus for Java-equal stat bonus sets and rerolls via target set when needed. Full Java item creation, storage add, sockets/godstone/fusion persistence, AP side effects, and quest notifications remain incomplete. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleItemPurificationAsync` | Client Handler / Adapter | Partial | Regression Tested in C# | Partial Parity | Handler can now accept random-bonus table input, choose a deterministic target random bonus in tests, allocate target object id, and produce a ready packet plan without live mutation. It still does not persist, mutate, or send from the live handler. |
| `com.aionemu.gameserver.model.templates.item.bonuses.StatBonusType` | String discriminator `"INVENTORY"` in `ItemRandomBonusTable` calls | Enum / Static Data Discriminator | Partial | Regression Tested in C# | Needs Verification | C# uses existing string-based static-data discriminator instead of a dedicated enum. This is an implementation difference at the planner boundary; broader enum parity remains unverified. |
| `com.aionemu.commons.utils.Rnd` | `Func<double>` test seam and `Random.Shared.NextDouble()` in `ItemRandomBonusTable` | Utility / Randomness | Partial | Regression Tested in C# | Needs Verification | Deterministic tests validate selection buckets. Java RNG algorithm/seed behavior is not matched or runtime-compared; live selection distribution remains approximate. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `AreBonusSetsEqual_MatchesJavaGroupCountComparison` | Unit | Java `ItemRandomBonusData.areBonusSetsEqual` source review | Validates same-id true, equal group counts true, different group counts false, both missing true, and one missing false. | Deterministic C# unit test for Java static-data equality rule. | Does not deep-compare modifier contents because Java does not. |
| `SelectRandomBonusNumber_UsesOneBasedWeightedGroups` | Unit | Java `ItemRandomBonusData.selectRandomBonusNumber` source review | Validates missing set returns `0` and deterministic rolls return 1-based weighted group ids. | Deterministic C# unit test with injected rolls. | Java `Rnd` algorithm/seed and all-zero chance exception behavior are not runtime-compared. |
| `CreateTargetItemPlan_PreservesRandomBonusWhenDifferentInventorySetsHaveEqualGroupCounts` | Regression | Java `ItemPurificationService.upgradeItem` plus `ItemRandomBonusData.areBonusSetsEqual` source review | Validates different stat-bonus set ids with equal group counts do not reroll the source random bonus. | Deterministic C# planner regression for Java's equal-set shortcut. | Planner-only; no live item mutation/persistence. |
| `CreateTargetItemPlan_SelectsRandomBonusWhenInventoryBonusSetsDifferAndTableIsAvailable` | Regression | Java `TuningAction.getRandomStatBonusIdFor` source review | Validates unequal group counts select target-set random bonus id `2` with deterministic roll and mark reroll. | Deterministic C# planner regression for target-set selection. | Does not validate Java RNG distribution or runtime packet output. |
| `HandleItemPurificationAsync_SelectsRandomBonusAndAllocatesWhenBonusTableAvailable` | Regression | Java `CM_ITEM_PURIFICATION` -> `ItemPurificationService.upgradeItem` source review | Validates handler random-bonus selection clears the pending blocker, allocates target id, produces a ready packet plan, and leaves player state unchanged. | Deterministic C# handler regression for planner composition. | Does not persist, mutate inventory/AP, send packets, or compare Java runtime bytes. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `HandleItemPurificationAsync` still does not mutate inventory/AP, persist changes, call the send bridge, synthesize AP rank packets, or execute full Java `ItemFactory`/`ItemSocketService` side effects.
- If `ItemRandomBonusTable` is unavailable, C# falls back to raw stat-bonus set-id equality; Java always has `DataManager.ITEM_RANDOM_BONUSES` in the live path.
- C# random selection uses `Random.Shared.NextDouble()` and deterministic `Func<double>` tests, not Java `Rnd`; distribution parity remains unverified.
- `AreBonusSetsEqual` intentionally compares group counts only because Java does; this may preserve Java's rough equivalence rather than true modifier equality.
- Current target projection copies socket/godstone/fusion references in planner DTOs; this remains safe while non-mutating but must be revisited before live mutation.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 ItemPurification random-bonus selection seam
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/not-started categories, including Java runtime artifact generation, live inventory/AP persistence, live handler send/mutation invocation, full socket/godstone/fusion side-effect parity, and Java RNG/runtime comparison
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Continue ItemPurification live adapter readiness with a non-persistent live mutation snapshot preview from a ready `ItemPurificationApplicationPlan`.

Suggested shape:
- Add a small service that takes `Player.InventoryItems` plus a ready `ItemPurificationApplicationPlan`.
- Produce post-mutation `InventoryItem` snapshots for update/delete/add operations without changing `Player.InventoryItems`.
- Derive cube snapshot candidates from before/after counts where possible, clearly marking unsupported expansion values if live storage state is missing.
- Feed those snapshots into the existing `ItemPurificationHandlerPacketBridgeService.CreateConcretePacketPlan` in a handler-level regression.
- Keep actual repository writes, AP mutation, live send invocation, quest notifications, and Java byte capture separate.

Do not combine with:
- repository transaction persistence
- live inventory/AP mutation
- AP rank side-effect packets
- quest notifications
- Java runtime byte capture

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Mutation snapshot preview implementation | likely new service/test files plus ItemPurification tests | Medium | Single writer if it touches shared ItemPurification fixtures. |
| B | Kinah charge-all partial-drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Separate from ItemPurification files; safe alternative. |
| C | Java ItemPurification runtime observer design | docs only | Low | Do not claim runtime parity until tooling exists. |
| D | Packet byte comparison gap audit | read-only packet tests/golden tooling | Low | Useful if Java artifact format becomes available. |

## Do Not Parallelize

- Multiple agents editing `GameServerConnection.cs`.
- Multiple agents editing `GameServerConnectionItemPurificationTests.cs`.
- Multiple agents changing ItemPurification workflow/application/packet services in the same unit.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, choose mutation snapshot preview or isolated Kinah charge-all partial-drift regression.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
