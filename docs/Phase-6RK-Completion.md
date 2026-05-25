# Phase 6RK Completion Handoff - ItemPurification Quest Notification Projection

Date: May 25, 2026
Unit of Work: UOW-967
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-967] Add item purification quest notification projection`)

## Status

Phase 6 is still in progress. This unit adds a pure ItemPurification quest-notification projection over application-plan operations.

Production `CM_ITEM_PURIFICATION` dispatch remains plan-only. The new projection emits metadata candidates only; it does not invoke quest handlers, refresh nearby quests, send packets, persist data, or wire live dispatch.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationApplicationPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationApplicationPlanServiceTests.cs`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6RK-Completion.md`

## What Changed

- Added `ItemPurificationApplicationPlanService.ProjectQuestNotifications`.
- Added `ItemPurificationQuestNotificationCandidate`.
- Added `ItemPurificationQuestNotificationType`.
- The projection maps:
  - `DeleteMaterialItem` to `ItemRemoved`.
  - `DeleteBaseItem` to `ItemRemoved`.
  - `AddTargetItem` to `ItemGet`.
- The projection ignores:
  - partial material count updates
  - AP spend
  - Java purification Kinah no-op
  - any operation without `QuestNotification` metadata
- Added `ProjectQuestNotifications_EmitsOnlyJavaDeleteAndCubeAddCandidatesInOperationOrder`.
- Updated readiness/audit/progress docs with parity table, risks, metrics, and next recommended unit.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Quest notification projection | `Storage`, `QuestEngine`, `ItemPurificationService` | `ItemPurificationApplicationPlanService.cs`, focused tests | Implementation/Test | No for writes | Low | Completed sequentially because service/test/docs are shared ownership surfaces. |
| B | AP spend rank-drop/packet-order tests | `AbyssPointsService`, `AbyssRank` | live-execution/AP tests | Test | Yes if separate files, otherwise sequential | Medium | Deferred as next recommended unit. |
| C | Java observer artifact generation | `CM_ITEM_PURIFICATION`, `PacketSendUtility` | Java/tooling files | Implementation | Yes if tooling exists | Medium | Blocked locally by Java 8 and missing Maven. |
| D | Static-data quest update item projection audit | `QuestEngine`, quest registration/static data | read-only static-data files | Analysis | Yes | Low | Deferred until before real quest callback dispatch. |

## File Ownership Map Used

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | UOW-967 projection, tests, docs, commit | Application-plan service/test file, ItemPurification audit/readiness docs, progress/handoff docs | `GameServerConnection.cs`, live execution service, persistence service, repository files | Pure projection, focused tests, parity docs |

No sub-agents were spawned for this write unit.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter ItemPurificationApplicationPlanServiceTests
```

Result: passed, 6 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "ItemPurificationApplicationPlanServiceTests|ItemPurificationLiveMutationServiceTests|ItemPurificationPacketPlanServiceTests|ItemPurificationLiveExecutionServiceTests|ItemPurificationPersistentLiveExecutionServiceTests|GameServerConnectionItemPurificationTests|AbyssPointsServiceTests"
```

Result: passed, 48 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Services.ItemPurificationApplicationPlanService.ProjectQuestNotifications` | Storage / Quest Notification Projection | Partial | Unit Tested | Partial Parity | Projection now preserves Java-relevant notification candidates for delete-only material/base removal and target add, while excluding partial material updates and Kinah no-op. It does not model Java dirty state, deleted queue, synchronization, packet timing, or actual `QuestEngine` invocation. |
| `com.aionemu.gameserver.questEngine.QuestEngine` | `Aion.GameServer.Services.ItemPurificationQuestNotificationCandidate` / `ItemPurificationQuestNotificationType` | Quest Engine / DTO Projection | Partial | Unit Tested | Needs Verification | C# now has ordered metadata candidates for `ItemRemoved` and `ItemGet`, but no handler maps, `questUpdateItems` projection, nearby-quest refresh, threading behavior, or live dispatch. Java runtime comparison is missing. |
| `com.aionemu.gameserver.services.item.ItemPurificationService` | `Aion.GameServer.Services.ItemPurificationApplicationPlanService` | Service / Application Plan | Partial | Unit Tested + Regression Tested | Partial Parity | Application plan now exposes a pure quest-notification projection over the existing Java-like operation order. AP side-effect execution, live quest callbacks, Java runtime packet/DB comparison, and production dispatch remain incomplete. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleInfrastructurePacketAsync` / plan-only path | Client Handler / Dispatch Gate | Partial | Regression Tested in C# | Needs Verification | No handler/dispatch change. Projection reduces one quest-callback readiness gap but does not satisfy automatic dispatch because no live dispatcher or Java runtime artifacts exist. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProjectQuestNotifications_EmitsOnlyJavaDeleteAndCubeAddCandidatesInOperationOrder` | Unit | Java `Storage.decreaseItemCount`, `Storage.delete`, `Storage.add`, `QuestEngine.onItemGet`, and `QuestEngine.onItemRemoved` source review | Verifies partial material update produces no notification, exhausted material delete and base delete produce `ItemRemoved`, AP spend and Kinah no-op produce none, and target add produces `ItemGet`, preserving application-operation order. | Deterministic C# projection test over Java-source-reviewed ordering. | Does not invoke Java, C# quest handlers, nearby-quest refresh, live storage packet timing, or automatic production dispatch. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.
- Quest projection is metadata only; no `IQuestItemMutationNotifier`, handler map, `questUpdateItems` projection, nearby-quest refresh, or live dispatcher exists.
- AP spend packets are modeled in C# but not sent by ItemPurification live execution.
- Rank-change side effects for equipment rank limits and abyss skill refresh are not wired into ItemPurification AP spend.
- Java storage dirty-state, deleted queue, and synchronization behavior remain unmodeled in the C# snapshot path.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 4
- Total artifacts ported: 1 pure quest-notification projection and 1 DTO/enum pair
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked artifacts: 4 blocked/not-started categories, including Java runtime artifact generation, live quest dispatch, AP spend packet/side-effect execution, and automatic production dispatch
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended safe task:
- Add AP spend live-execution tests for rank-drop metadata and AP packet ordering before wiring any side-effect executor.
- Keep production `CM_ITEM_PURIFICATION` dispatch unchanged.

Alternative safe task:
- Add a no-op `IQuestItemMutationNotifier` seam behind explicit opt-in live execution only, preserving projection-only behavior by default.
- Add Java observer artifact generation only if Java 25/Maven tooling is available.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | AP spend rank-drop metadata test | AP service/live mutation tests | Low/Medium | May be separate from packet-order test if file ownership is clean. |
| B | AP spend packet-order test | `ItemPurificationLiveExecutionServiceTests.cs` | Medium | Keep production dispatch disabled. |
| C | No-op quest notifier seam design | new interface/service plus opt-in tests | Medium | Do not invoke real quest handlers yet. |
| D | Java observer artifact generator feasibility | Java observer/test tooling files or read-only docs | Medium | Only if Java 25/Maven is available. |

## Do Not Parallelize

- Multiple agents editing `ItemPurificationLiveExecutionServiceTests.cs`.
- Multiple agents editing `ItemPurificationApplicationPlanService.cs` and its tests.
- Multiple agents editing `GameServerConnection.cs`.
- Multiple agents editing progress and handoff docs.
- Any automatic `CM_ITEM_PURIFICATION` production dispatch work with DB integration or quest/AP side-effect work.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Read `docs/ItemPurification-Automatic-Dispatch-Readiness.md`, `docs/ItemPurification-AP-Quest-Readiness-Audit.md`, and `docs/ItemPurification-Java-Observer-Design.md`.
3. Confirm branch status and latest commit.
4. Run Parallel Work Discovery before selecting the next write unit.
5. Prefer AP spend rank-drop metadata and packet-order tests before wiring live side-effect execution.
6. Keep production `CM_ITEM_PURIFICATION` automatic dispatch disabled.
7. Run focused and full tests for any C# code changes.
8. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
9. Create the next handoff and commit the completed unit.

