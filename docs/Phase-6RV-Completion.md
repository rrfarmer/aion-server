# Phase 6RV Completion Handoff - Quest Nearby Refresh Planning

Date: May 25, 2026
Unit of Work: UOW-978
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-978] Plan quest nearby refresh candidates`)

## Status

Phase 6 is still in progress. This unit adds an opt-in no-op planner that maps projected ItemPurification get/remove notifications to Java-style nearby-quest refresh candidates using `StaticData.QuestUpdateItems`.

Production `CM_ITEM_PURIFICATION` dispatch remains plan-only. No player-controller nearby refresh, dynamic quest handler dispatch, or real quest callback execution was implemented.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationQuestMutationNotifier.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationQuestMutationNotifierTests.cs`
- `docs/ItemPurification-QuestUpdateItems-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6RV-Completion.md`

## What Changed

- Added `PlanningItemPurificationQuestMutationNotifier`.
- Added `ItemPurificationNearbyQuestRefreshPlan`, `ItemPurificationNearbyQuestRefreshCandidate`, and `ItemPurificationNearbyQuestRefreshPlanStatus`.
- Extended `ItemPurificationQuestNotificationDispatchResult` with optional `NearbyQuestRefreshPlan` metadata.
- The planner preserves projected notification order and includes only candidates whose item id is present in `QuestUpdateItemTable`.
- Existing no-op notifier behavior remains unchanged unless callers explicitly choose the planning notifier.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Nearby-refresh planning seam | `QuestEngine.onItemGet`, `QuestEngine.onItemRemoved` | `ItemPurificationQuestMutationNotifier.cs`, `ItemPurificationQuestMutationNotifierTests.cs` | Implementation / Test Creation | No | Medium | Shared notifier result shape requires one owner. |
| B | Side-effect persistence analysis | ItemPurification equipment/skill persistence paths | docs/read-only first | Java Analysis | Yes | Medium | Safe as analysis, deferred behind quest planning seam. |
| C | Java observer artifact generation | `CM_ITEM_PURIFICATION`, packet send paths | Java/tooling docs | Parity Verification | No | Medium | Still blocked locally by Java 8 and missing Maven. |

No sub-agents were spawned because the selected unit changes a shared notifier contract.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "ItemPurificationQuestMutationNotifierTests|ItemPurificationLiveExecutionServiceTests|ItemPurificationApplicationPlanServiceTests"
```

Result: passed, 13 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1664 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.QuestEngine.onItemGet` | `Aion.GameServer.Services.PlanningItemPurificationQuestMutationNotifier`; `ItemPurificationNearbyQuestRefreshPlan` | Quest Callback / Planning Service | Partial | Unit Tested | Partial Parity | C# can project get-item intent and plan nearby-refresh candidates through static update-item membership. Real get-item handler dispatch, player-controller refresh invocation, threading behavior, and Java runtime comparison remain missing. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onItemRemoved` | `Aion.GameServer.Services.PlanningItemPurificationQuestMutationNotifier`; `ItemPurificationNearbyQuestRefreshPlan` | Quest Callback / Planning Service | Partial | Unit Tested | Partial Parity | C# can project remove intent and plan nearby-refresh candidates through static update-item membership. Java remove behavior has no symmetric handler map, but the actual controller refresh invocation is still not wired. |
| `com.aionemu.gameserver.questEngine.QuestEngine.init` | `Aion.GameServer.Dataholders.QuestUpdateItemTable` consumed by `PlanningItemPurificationQuestMutationNotifier` | Quest Engine / Static Data Dependency | Partial | Unit Tested | Partial Parity | Static update-item membership from UOW-977 is now consumed by a no-op planner. Java runtime comparison and live quest-engine integration remain missing. |
| `com.aionemu.gameserver.model.templates.quest.InventoryItem` | `Aion.GameServer.Dataholders.QuestUpdateItemTable` membership lookups | XML DTO / Projection Dependency | Partial | Unit Tested | Partial Parity | The planner relies on item ids projected from quest inventory items. Missing/nullable `item_id` Java edge behavior and broader XML serialization/reflection remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `ItemPurificationLiveExecutionService` optional `IItemPurificationQuestMutationNotifier` path | Client Handler / Explicit Helper | Partial | Regression Tested in C# | Needs Verification | Explicit callers can supply the planning notifier, but production `HandleInfrastructurePacketAsync` still uses plan-only dispatch and no real nearby refresh or quest handlers execute. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlanningNotifier_FiltersNearbyRefreshCandidatesThroughQuestUpdateItems` | Unit | Java `QuestEngine.onItemGet`, `QuestEngine.onItemRemoved`, and `questUpdateItems.contains(itemId)` source review | Validates that only projected get/remove candidates whose item id is in `questUpdateItems` become nearby-refresh candidates, preserving notification order. | Deterministic unit test from source-reviewed Java membership behavior. | Does not invoke real `updateNearbyQuests`, dynamic handlers, or Java runtime. |
| `PlanningNotifier_ReportsNoRefreshCandidatesWhenItemsAreNotQuestUpdateItems` | Unit | Java `questUpdateItems.contains(itemId)` source review | Validates non-member get/remove candidates do not request nearby refresh. | Deterministic unit test from source-reviewed Java behavior. | No live quest engine execution. |
| `PlanningNotifier_ReportsNoNotificationsWithoutRefreshing` | Unit | Java callback no-op effect when no storage notification occurs | Validates empty notification lists produce no refresh candidates. | Deterministic unit test over C# planner guard. | No Java runtime comparison. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The new planner is opt-in and no-op; no C# path invokes the player controller's nearby quest refresh.
- Real dynamic get-item quest handler registration and dispatch remain unported for this path.
- The planner depends on `QuestUpdateItemTable`; broader quest XML edge cases and Java-generated membership counts remain unverified.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 1 no-op nearby-refresh planning seam
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 3 blocked/not-started categories, including live nearby-quest refresh dispatch, real get-item handler dispatch, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 70% complete

## Next Recommended Unit of Work

Recommended safe task:
- Add an opt-in nearby-refresh dispatcher interface that can consume `ItemPurificationNearbyQuestRefreshPlan`, with a default no-op implementation and tests proving no refresh is invoked unless explicitly supplied. Keep automatic production dispatch disabled.

Alternative safe task:
- Analyze ItemPurification side-effect persistence gaps for rank-limited equipment and abyss skill changes before extending the repository payload.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Nearby-refresh dispatcher interface | new service/test files plus `ItemPurificationQuestMutationNotifier.cs` if needed | Medium | Do sequentially if changing notifier contracts again. |
| B | Side-effect persistence analysis | docs/read-only repository and live execution code | Medium | Safe as analysis if no repository payload edits. |
| C | Java observer artifact generation feasibility | Java/tooling files or docs | Medium | Only if Java 25/Maven tooling is available. |

## Do Not Parallelize

- Multiple agents editing `ItemPurificationQuestMutationNotifier.cs`, quest callback contracts, or progress/handoff docs.
- Production `CM_ITEM_PURIFICATION` automatic dispatch with quest callback work.
- Real dynamic quest handler invocation with nearby-refresh work unless file ownership is isolated and reviewed.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Keep production `CM_ITEM_PURIFICATION` automatic dispatch disabled.
5. Run focused and full tests for any C# code changes.
6. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
7. Create the next handoff and commit the completed unit.
