# Phase 6RZ Completion Handoff - Nearby Quest World-Instance Registry

Date: May 25, 2026
Unit of Work: UOW-982
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-982] Add nearby quest instance registry`)

## Status

Phase 6 is still in progress. This unit implements only the minimal world-map instance quest-id storage prerequisite for the nearby-quest refresh path.

Production `CM_ITEM_PURIFICATION` dispatch remains plan-only. No real player-controller nearby refresh, dynamic quest handler registration, NPC-spawn population, delayed refresh scheduling, candidate filtering, or live quest callback execution was implemented.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6RZ-Completion.md`

## What Changed

- Added `WorldMapInstanceRuntimeState.QuestIds` as a snapshot of registered nearby-start quest ids.
- Added `WorldMapInstanceRuntimeState.RegisterQuestStartIds(IEnumerable<int>)`.
- Matched Java `WorldMapInstance.addObject(Npc)` set-add semantics for `QuestNpc.getOnQuestStart()` ids:
  - duplicate quest ids are stored once
  - duplicate-only registration returns `false`
  - at least one newly registered quest id returns `true`
- Added a focused world-map runtime test for those set semantics.
- Updated readiness docs to mark only the storage prerequisite complete.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | World-instance quest-id registry | `WorldMapInstance`, `QuestNpc` | `WorldMapInstanceRuntimeState.cs`, `WorldMapRuntimeStateTests.cs` | Implementation / Tests | No write parallelism | Low | Completed by orchestrator; shared world runtime state should have one writer. |
| B | Side-effect persistence docs | AP rank equipment/abyss skill persistence paths | docs/read-only first | Analysis | Yes if read-only | Medium | Safe future sidecar candidate. |
| C | Equipment side-effect persistence implementation | Java AP rank equipment side effects | persistence services/repository/tests | Implementation | No | Medium | Sequential until repository contract is narrowed. |
| D | Java observer artifact generation | `CM_ITEM_PURIFICATION`, packet/DB capture | Java/tooling/docs | Parity Verification | No | Medium | Still blocked locally by Java 8/Maven gap. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~WorldMapRuntimeStateTests
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Results:

- Focused world-map suite: passed, 16 tests.
- Full game-server suite: passed, 1666 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.WorldMapInstance` | `Aion.GameServer.World.WorldMapInstanceRuntimeState` | World / Quest Registry | Partial | Unit Tested | Partial Parity | C# now stores quest ids and reports whether registration added any new id, matching Java set-add semantics. Java `addObject(Npc)`, `QuestEngine.getQuestNpc`, delayed refresh scheduling, map-region lookup, and player sends remain unported for this path. |
| `com.aionemu.gameserver.model.templates.quest.QuestNpc` | Not started for dynamic quest start registration | Quest Handler Registration | Not Started | No Tests | Unknown | Java `QuestNpc.getOnQuestStart()` feeds `WorldMapInstance.questIds`; C# has no verified dynamic quest handler registration table yet. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | Future C# nearby quest refresh service/adapter consuming `WorldMapInstanceRuntimeState.QuestIds` and `SmNearbyQuests` | Controller / Quest UI | Not Started | Manual Only | Needs Verification | Packet and registry storage prerequisites exist, but no candidate filtering, start-condition evaluation, level-diff calculation, controller method, or send path exists. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | Not started for nearby quest UI | Service / Quest Predicate | Not Started | No Tests | Unknown | Still needed with `allowedDiffToMinLevel = 2`, `warn = false`, and no skip flags before real marker calculation can exist. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `WorldMapRuntimeStateTests.WorldMapInstanceRuntimeState_TracksQuestStartIdsLikeJavaWorldMapInstance` | Unit | Java `WorldMapInstance.addObject(Npc)` and `QuestNpc.getOnQuestStart` | Quest ids are stored once, duplicate-only registration reports no new addition, and later new ids are exposed by snapshot. | Deterministic C# test from source-reviewed Java `ConcurrentHashMap.newKeySet().add` behavior. | Does not invoke NPC spawn, `QuestEngine.getQuestNpc`, delayed 1500 ms refresh scheduling, player iteration, or packet send. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- World-instance quest-id storage exists, but no C# path populates it from NPC spawn or dynamic quest handlers yet.
- No C# nearby-UI `QuestService.checkStartConditions` equivalent exists.
- No C# dynamic `QuestNpc.onQuestStart` handler registration table exists.
- `SmNearbyQuests` is only a packet prerequisite; no production code sends it yet.
- The current ItemPurification dispatcher seam must remain no-op until candidate calculation, start-condition evaluation, and a controlled send boundary exist.
- ItemPurification persistent execution still does not persist secondary rank-limit equipment unequips or abyss skill deletion intents from AP-rank side effects.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.

## Summary Metrics

- Total Java artifacts discovered: 4
- Total artifacts ported: 1 partial world-map quest registry storage prerequisite in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4
- Total blocked artifacts: 3 blocked/not-started categories, including dynamic quest-id population, quest start-condition evaluation, and dynamic quest handler registration
- Estimated overall migration completion: Phase 6 remains about 70% complete

## Next Recommended Unit of Work

Recommended safe task:
- Audit or implement a staged `QuestNpc.onQuestStart` registration source that can populate `WorldMapInstanceRuntimeState.RegisterQuestStartIds` without invoking real dynamic handlers; keep `QuestService.checkStartConditions`, player-controller sends, dynamic handlers, and production ItemPurification dispatch disabled.

Alternative safe task:
- Use the sidecar persistence-gap analysis to document or implement the next ItemPurification side-effect persistence prerequisite.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Dynamic quest-start registration analysis | Java/C# quest handler files read-only | Medium | Safe as a read-only explorer while orchestrator owns docs. |
| B | Side-effect persistence docs | Java/C# persistence files read-only | Medium | Safe if no repository payload edits. |
| C | Equipment side-effect persistence implementation | persistence service/repository/tests | Medium | Sequential until contract is defined; do not parallelize with skill persistence contract edits. |
| D | Java observer artifact generation feasibility | Java/tooling files or docs | Medium | Only if Java 25/Maven tooling is available. |

## Do Not Parallelize

- Multiple agents editing world runtime files, packet files, or Phase 6 docs.
- Production `CM_ITEM_PURIFICATION` automatic dispatch with quest callback work.
- Real dynamic quest handler invocation with nearby-refresh work.
- ItemPurification persistence contract changes across repository/service/test files until ownership is narrowed.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Keep production `CM_ITEM_PURIFICATION` automatic dispatch disabled.
5. Run focused and full tests for any C# code changes.
6. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
7. Create the next handoff and commit the completed unit.
