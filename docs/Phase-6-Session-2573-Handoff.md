# Phase 6 Session 2573 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2573: Wire CM_QUEST_SHARE live (quest-share decision planner + dispatch handler). Completes the
quest-share arc. See [Phase-6-Session-2573-Completion.md](Phase-6-Session-2573-Completion.md).

## Session Summary

| UOW | Summary |
|-----|---------|
| 2569 | Live group kinah distribution (partyType 1, non-alliance). |
| 2570 | Live alliance kinah distribution (partyType 2 + sub-group). |
| 2571 | Quest-template share metadata (`CannotShare`, `Target`) → `NearbyQuestTemplateSummary`. |
| 2572 | `SM_QUEST_ACTION` SHARE variant (`SmQuestAction.Share`, ActionType 5) + golden test. |
| 2573 | CM_QUEST_SHARE wired live: `QuestSharePlanService` + `HandleQuestShareAsync`. 12 planner tests. |

## MILESTONE: CM_QUEST_SHARE is live

Three-UOW arc complete: template metadata (2571) → SHARE packet (2572) → live decision + dispatch (2573).
Parser `CmQuestShare` → `HandleQuestShareAsync` → `QuestSharePlanService` → SM_QUEST_ACTION.SHARE + system
messages (1100000/1100001/1100002/1100003/1100005).

## What changed (UOW-2573)

Java source of truth: `CM_QUEST_SHARE.runImpl` (full decision); `GroupConfig.GROUP_MAX_DISTANCE`=100;
`Player.isInAlliance()`; `PositionUtil.isInRange(VO,VO,float)` (same world+instance + squared distance < range²).

C# changes:
- `QuestSharePlanService` (new pure planner): full runImpl decision → ordered `QuestShareInstruction` list.
- `GameServerConnection`: `CmQuestShare` case now dispatches; new `HandleQuestShareAsync` resolves
  template/state/members and fans out the plan.
- `QuestSharePlanServiceTests` (new, 12 cases).

### Documented gaps (carried + new)

- `HandleQuestShareAsync` glue not directly unit-tested (test seam lacks runtime template injection); decision is
  planner-covered, glue mirrors the unit-tested distribution handler.
- `QuestTemplate.cannotGiveup` not yet ported (likely needed by the next CM_DELETE_QUEST abandon UOW).
- Carried: League partyType-3 distribution; canTrade gate (distribution); in-memory kinah persistence;
  legion-history live response; exchange + legion-rank DB reads unit-only; XP/level-up not ported.

## Validation Decision (UOW-2573)

- Changed surface: production-code — new pure planner + thin live dispatch handler (emits packets only; no shared
  state mutation).
- Specific behavior/contract: CM_QUEST_SHARE.runImpl decision parity (1100001 cannot-share/null template; silent
  return on null/COMPLETE state; group/alliance ∩ online ∩ in-range, self excluded; empty ⇒ 1100005/1100000 by
  Target; per member 1100003 fail or SM_QUEST_ACTION.SHARE + 1100002).
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter
  "FullyQualifiedName~QuestSharePlanServiceTests"` → 12/12. (SHARE bytes golden-tested in UOW-2572.)
- Focused Java/Maven command: none (parity from reviewed runImpl + GROUP_MAX_DISTANCE=100 + isInRange semantics).
- Broad-validation trigger: none.
- Broad .NET decision: skipped (filtered test built `Aion.GameServer` incl. handler + test project).
- Why sufficient: planner tests cover every runImpl branch; thin handler is a mechanical mirror of the
  unit-tested distribution handler.

## Files Changed (UOW-2573)

- `dotnetConversion/src/Aion.GameServer/Services/QuestSharePlanService.cs` — new planner.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs` — dispatch case + `HandleQuestShareAsync`.
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestSharePlanServiceTests.cs` — new (12 cases).

## Migration Parity Table (UOW-2573)

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_QUEST_SHARE.runImpl` (decision) | `QuestSharePlanService.Plan` | Service | Complete | Unit Tested | Verified Parity | all branches covered |
| `CM_QUEST_SHARE.runImpl` (dispatch) | `HandleQuestShareAsync` | Handler | Complete | Manual Only | Partial Parity | thin glue; not directly unit-tested |
| `PositionUtil.isInRange(VO,VO,float)` | `QuestSharePlanService.IsInGroupRange` | Utility | Complete | Unit Tested | Verified Parity | world+instance + distance² < range² |
| `Player.isInAlliance()` | `TeamMembership == Alliance` | Predicate | Complete | Unit Tested | Verified Parity | SHARE alliance flag |

## Summary Metrics (conservative)

- New automated coverage: 12 planner unit tests.
- CM_QUEST_SHARE live; quest-share arc complete.
- Overall Phase 6 completion estimate: incremental.

## Remaining Risks

- `HandleQuestShareAsync` glue manual-only (see gaps).
- `cannotGiveup` not ported.
- Carried risks as above.

## Corrective Direction After History Reset

The branch was reset back to `00362a54a` because later preview, metadata, readiness, planner, adapter, and evidence
commits did not materially move runtime parity. A backup of the discarded state exists at
`backup/polluted-phase6-before-runtime-reset`.

Future Phase 6 work must pass the Runtime Progress Gate from `docs/orchestration-rules.md`. Do not recreate the
discarded preview chain.

Invalid next UOWs:

- adding or hardening `QuestStepMutationPreviewService` helpers,
- adding metadata-only or JavaSource-string evidence,
- adding readiness/planner/adapter layers without wiring live behavior in the same UOW,
- test-only assertion passes over dry-run models or constants.

## Next Recommended UOW

1. **UOW-2574: Port `CM_DELETE_QUEST` (quest abandon).** `CM_DELETE_QUEST.runImpl` cancels timed quests +
   `QuestService.abandonQuest`; dispatch deferred in `GameServerConnection.cs` (`CmDeleteQuest` case).
   **Verify first**: `QuestService.abandonQuest` semantics (state removal/reset + SM_QUEST_ACTION.ABANDON +
   `cannot_giveup` gate). `QuestTemplate.cannotGiveup` is the abandon analog of `cannotShare` and is NOT yet on
   `NearbyQuestTemplateSummary` — a small UOW-2571-style extractor add is likely a prerequisite.
2. **Alt**: another deferred handler whose deps are ported — Work Discovery on the `deferred` switch cases.

### Runtime Progress Gate For UOW-2574

```text
Runtime progress gate:
- Deferred/live behavior being advanced: `CM_DELETE_QUEST` client packet dispatch and quest-abandon execution.
- Java source method or runtime path: `CM_DELETE_QUEST.runImpl` and `QuestService.abandonQuest`.
- C# runtime artifact to wire or fix: `GameServerConnection` packet dispatch plus C# quest-abandon service/handler code.
- Client-visible/state/persistence effect expected: active player quest state is removed/reset according to Java, abandon/timer packets are sent as Java sends them, and persistence/nearby refresh behavior is executed or explicitly blocked by a real missing runtime dependency.
- Why this is not preview-only/test-only/documentation-only: success requires live dispatch, packet output, and live player quest-state mutation.
```

Before editing, inspect current C# delete-quest code only to reuse or replace it. Do not add more non-live layers.

### Focused validation recipe for UOW-2574

- Behavior/contract: CM_DELETE_QUEST honors `cannot_giveup`, removes/resets quest state, emits
  SM_QUEST_ACTION.ABANDON (confirm exact Java behavior first).
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter
  "FullyQualifiedName~<CmDeleteQuestTests>|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests"`.
- Java/Maven: use a targeted Java test only if a narrow fixture exists; otherwise document exact Java source review.
- Broad-validation trigger: live dispatch/state/persistence may apply. Name the trigger before broad .NET validation;
  start focused on live handler/service tests.

## Context Needed By Next Session

- CM_QUEST_SHARE is live end-to-end (parser → `HandleQuestShareAsync` → `QuestSharePlanService` → packets).
- `QuestSharePlanService.Plan(...)` is the reusable decision; `Msg*` constants expose the message ids.
- `NearbyQuestTemplateSummary` has `CannotShare`/`Target`; `cannotGiveup` NOT ported.
- `NearbyQuestStartConditionService.CheckNearbyStartConditions` = `QuestService.checkStartConditions`.
- Whole-team online members: `_playerGroupRuntime.GetOnlineMemberPlayers(teamId)` /
  `_playerAllianceRuntime.GetOnlineMemberPlayers(teamId)`.
- SM_QUEST_ACTION variants ported: Add/Update (state), Share (ActionType 5). ABANDON (id 3) is referenced by a
  constant but its `Abandon` factory/wire is NOT yet ported — the next UOW likely adds it.
