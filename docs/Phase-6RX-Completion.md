# Phase 6RX Completion Handoff - Nearby Quest Refresh Surface Audit

Date: May 25, 2026
Unit of Work: UOW-980
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-980] Audit nearby quest refresh surface`)

## Status

Phase 6 is still in progress. This docs-only unit audits Java `PlayerController.updateNearbyQuests()` and the C# surfaces required before ItemPurification can invoke real nearby-quest refresh.

Production `CM_ITEM_PURIFICATION` dispatch remains plan-only. No real player-controller nearby refresh, dynamic quest handler dispatch, or live quest callback execution was implemented.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6RX-Completion.md`

## What Changed

- Added `docs/ItemPurification-NearbyQuestRefresh-Audit.md`.
- Documented Java `PlayerController.updateNearbyQuests()` behavior.
- Documented Java `SM_NEARBY_QUESTS` packet layout: `C(0)`, negative count as unsigned `H`, then quest ids with bit `1 << 17` set for positive level-difference values.
- Documented that Java nearby refresh uses `WorldMapInstance.questIds`, populated from dynamic `QuestNpc.onQuestStart` registrations.
- Documented that Java evaluates nearby markers with `QuestService.checkStartConditions(player, questId, false, 2, false, false, false)` and `QuestService.getLevelRequirementDiff`.
- Documented C# gaps: no `SmNearbyQuests`, no world-instance quest id registry, no dynamic `QuestNpc.onQuestStart` table, and no nearby-UI start-condition evaluator.
- Integrated the read-only sidecar persistence scan into the progress/handoff notes.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Player-controller nearby-refresh surface analysis | `PlayerController.updateNearbyQuests`, `QuestService.checkStartConditions`, `SM_NEARBY_QUESTS` | docs/read-only first | Java Analysis / Documentation | Yes | Medium | Completed as docs-only audit by orchestrator. |
| B | Side-effect persistence analysis | ItemPurification rank-limited equipment and abyss skill side effects | docs/read-only first | Java Analysis / Documentation | Yes | Medium | Completed by read-only explorer; no files changed. |
| C | Java observer artifact generation | `CM_ITEM_PURIFICATION`, packet/DB capture paths | tooling/docs | Parity Verification | No | Medium | Still blocked locally by Java 8/Maven gap. |
| D | Real nearby-refresh dispatcher implementation | Player controller/quest UI service, notifier dispatch | production/test files TBD | Implementation | No | High | Needs packet/candidate/start-condition prerequisites first. |

## Sub-Agent Output Integrated

Explorer `019e5f3b-56ed-7673-afa4-577bf4717f12` completed read-only side-effect persistence analysis and was closed.

Key findings:

- Rank-limited equipment unequip is executed in memory during ItemPurification live execution, but persistent execution does not save `EquipmentRankLimitChange.PersistedItems`, so `inventory.is_equipped` and `inventory.slot` can remain stale after rank-drop unequip.
- Abyss skill changes are executed in memory and packeted, but persistent ItemPurification does not save `player_skills` deletion/update intent from `AbyssSkillUpdate`.
- Java temporary abyss skill additions use `PersistentState.NOACTION`, so rank-up additions likely should not be inserted; the meaningful gap is removal/delete persistence.
- Future implementation should be sequential until the repository/persistence contract is defined.

## Tests

Docs-only audit. No build was required.

Previous validation in UOW-979:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1665 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | Future C# nearby quest refresh service/adapter | Controller / Quest UI | Not Started | Manual Only | Needs Verification | Java source reviewed. C# has only no-op ItemPurification planning/dispatch metadata; no real player-controller refresh, quest candidate calculation, or packet send exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | Not started | Server Packet | Not Started | No Tests | Unknown | Java packet writes `C(0)`, negative list size as unsigned `H`, and each quest id with bit `1 << 17` set when level diff is positive. No C# packet exists yet. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | Not started for nearby quest UI | Service / Quest Predicate | Not Started | No Tests | Unknown | Needed with `allowedDiffToMinLevel = 2`, `warn = false`, and no skip flags. Full Java predicate has quest state, repeat-count, race, precondition, and level gates; C# equivalent is missing. |
| `com.aionemu.gameserver.world.WorldMapInstance` | Future C# world-map quest registry | World / Quest Registry | Partial | Manual Only | Needs Verification | C# has world/NPC spawn services, but no discovered equivalent of Java instance-level `questIds` populated from `QuestNpc.onQuestStart`. |
| `com.aionemu.gameserver.model.templates.quest.QuestNpc` | Not started for dynamic quest start registration | Quest Handler Registration | Not Started | No Tests | Unknown | Java dynamic handlers call `registerQuestNpc(...).addOnQuestStart`; C# dynamic quest handler registration is not ported for this path. Reflection/dynamic loading differences are significant. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Manual | Java `PlayerController.updateNearbyQuests`, `SM_NEARBY_QUESTS`, `QuestService.checkStartConditions`, `WorldMapInstance.addObject`, and `QuestNpc` source review | Documents the dependency chain required before real nearby-refresh dispatch. | Static source audit only. | No C# packet, no service implementation, no Java runtime comparison. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- No `SmNearbyQuests` packet exists in C# yet.
- No C# quest start-condition evaluator exists for nearby quest UI.
- No C# dynamic quest handler registration table exists for `QuestNpc.onQuestStart`.
- The current ItemPurification dispatcher seam must remain no-op until these lower-level surfaces exist.
- ItemPurification persistent execution still does not persist secondary rank-limit equipment unequips or abyss skill deletion intents from AP-rank side effects.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 0 in this docs-only audit
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including `SM_NEARBY_QUESTS`, nearby quest candidate calculation, quest start-condition evaluation, and dynamic quest handler registration
- Estimated overall migration completion: Phase 6 remains about 70% complete

## Next Recommended Unit of Work

Recommended safe task:
- Implement the narrow `SmNearbyQuests` packet prerequisite with tests for empty, available, and not-yet-available quest ids; keep quest candidate calculation, real nearby refresh, dynamic quest handlers, and production ItemPurification dispatch disabled.

Alternative safe task:
- Use the sidecar persistence-gap analysis to document or implement the next ItemPurification side-effect persistence prerequisite.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | `SmNearbyQuests` packet | new server packet and packet tests | Low | Can be implemented independently of quest candidate calculation. |
| B | Side-effect persistence docs | docs/read-only repository and service code | Medium | Safe as analysis if no repository payload edits. |
| C | Equipment side-effect persistence implementation | persistence service/repository/tests | Medium | Sequential until contract is defined; do not parallelize with skill persistence contract edits. |
| D | Java observer artifact generation feasibility | Java/tooling files or docs | Medium | Only if Java 25/Maven tooling is available. |

## Suggested Parallel Batch

If continuing immediately:

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Implement `SmNearbyQuests` packet and tests | `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmNearbyQuests.cs`, `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` | ItemPurification services, shared docs until integration |
| Explorer | Read-only side-effect persistence follow-up | Java/C# persistence files read-only | all writes |

Use one worker only for packet implementation unless a separate read-only explorer is helpful; do not let multiple agents edit `GamePacketTests.cs`.

## Do Not Parallelize

- Multiple agents editing `GamePacketTests.cs` or server-packet files.
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
