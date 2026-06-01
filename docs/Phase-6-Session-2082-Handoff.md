# Phase 6 Session 2082 Handoff - Find Group Readiness Evidence Alignment

Date: 2026-06-01
Unit of Work: UOW-2082
Status: Completed

## Startup Context Rule

Future Phase 6 sessions should not read `PHASE-6-PROGRESS.md` during normal startup.

Read these instead:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- Latest `docs/Phase-6-Session-*-Completion.md`
- Latest `docs/Phase-6-Session-*-Handoff.md`

`docs/PHASE-6-PROGRESS.md` is a historical archive. Open it only for targeted archaeology when the latest completion/handoff docs do not contain enough context.

## Test Selection Rule

Focused validation is the default.

Do not run the broad .NET suite or full solution build as a routine heartbeat, end-of-unit habit, or substitute for choosing the right parity evidence. Full .NET validation is reserved for documented broad-validation triggers.

Choose the narrowest command that still proves the scoped change:

- Documentation-only units: run repository hygiene such as `git diff --check`; runtime tests are not applicable unless generated artifacts, scripts, or executable docs changed.
- Test-only units: run the edited test class or smallest directly affected filter; do not broaden unless product-code risk is revealed.
- Production-code units: run edited service/packet/parser tests plus directly adjacent adapter/composition tests.
- Shared-surface units: start focused, then escalate only for shared infrastructure, packet primitives, serialization helpers, crypto, scheduling, world state, persistence, connection dispatch, live side effects, common model/state changes, suspicious focused failures, explicit user request, or release/readiness checkpoint.

Prefer C# filters such as `--filter "FullyQualifiedName~SpecificTestClass|FullyQualifiedName~RelatedTestClass"` and Java Maven filters such as `-Dtest=SpecificJavaTest`. Avoid unfiltered `dotnet test dotnetConversion/AionServer.slnx`, unfiltered project-wide tests, and full solution builds unless the broad trigger is documented.

When broad validation is skipped, document the focused commands and why they were sufficient. When Java/Maven is skipped, document why a narrower Java parity command was unavailable or irrelevant.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, auto-group static-data facts, form-anywhere config facts, auto-group corpus evidence, parsed-but-no-runImpl adapter evidence, live-dispatch readiness reporting, logout cleanup observer evidence, joined-team invite observer evidence, instance-group join-threshold removal evidence, and readiness evidence alignment.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupLiveDispatchReadinessReportService` now separates observer evidence from global live-dispatch blockers.
- `FindGroupConnectionClientActionCompositionPlanService` can source active-player, world-player, team/member, `AutoGroupTable`, and `GameServerOptions.Instance.FormInstanceGroupAnywhere` facts for disabled plans.
- `PlayerEnterWorldService.LeaveWorldAsync` can record an observer-only disabled `FindGroupLogoutCleanupPlan` before pending question denial when supplied with a find-group service and observer.
- `PlayerGroupInviteRequestService` and `PlayerAllianceInviteRequestService` can expose observer-only disabled `FindGroupJoinedTeamPlan` evidence when supplied with `FindGroupJoinedTeamLifecycleRecorder`.
- `FindGroupRecruitmentPlanService.OnJoinedTeam` removes stored instance-group registration when effective current-team size reaches `MinMembers`, matching Java `ServerWideGroup.getMembers()` threshold behavior for this planner slice.

## Latest Completed Work

- UOW-2078: find-group live-dispatch readiness report and tests.
- UOW-2079: observer-only find-group logout cleanup plan in leave-world composition.
- UOW-2080: observer-only find-group joined-team plan in group/alliance invite accept composition.
- UOW-2081: find-group joined-team instance-group threshold removal in disabled planner.
- UOW-2082: find-group readiness report now records lifecycle observer evidence while staying blocked for live dispatch.

## Recent Commits

- `dcbd85521 [Phase 6][UOW-2081] Remove joined instance group registrations`
- `17b52546b [Phase 6][UOW-2080] Record find group joined-team invites`
- `cd37ed13e [Phase 6][UOW-2079] Record find group logout cleanup`
- `bff53d353 [Phase 6][UOW-2078] Add find group dispatch readiness report`

## Validation In UOW-2082

- Focused C# readiness/find-group tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~PlayerGroupInviteRequestServiceTests|FullyQualifiedName~PlayerAllianceInviteRequestServiceTests" --no-restore`
  - Result: 44 tests passed.
  - Existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - Readiness-report-only update around already-reviewed Java source; no narrow Java executable target was relevant.
- Broad .NET validation was skipped:
  - Readiness-report/test update only; no live dispatch, packet primitive, persistence, crypto, scheduling, world-state, or connection-dispatch behavior changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` readiness action set | `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReportService` | Readiness Report / Service | Partial | Unit Tested | Partial Parity | Report still enumerates Java runImpl actions and parsed-but-no-runImpl actions, now with observer evidence separated from live blockers. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` lifecycle and side-effect readiness | `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReport`; `ObserverEvidence` | Readiness Report / DTO | Partial | Unit Tested | Partial Parity | Observer-only lifecycle evidence is documented without changing live-dispatch blocked status. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- Direct packet sends, world broadcasts, action 12 invite execution, encrypted socket behavior, real-client behavior, visibility filtering, and concurrency remain unverified.
- Observer evidence is not live singleton service wiring.

## Next Recommended Unit of Work

- Next sequential task: inspect the `CM_FIND_GROUP` action 12 group/alliance invite dispatch gap and decide whether a disabled connection-adjacent executor can safely call existing invite request services without enabling live find-group dispatch.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for `AutoGroupData` if a lightweight Java test can be introduced safely.
- Review direct-send/world-broadcast dispatch requirements and define a no-live-send executor contract.
- Audit `GameServerConnection` `CmFindGroup` deferred branch against the readiness report to define the final pre-live checklist.

## Files Changed In UOW-2082

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-Session-2082-Completion.md`
- `docs/Phase-6-Session-2082-Handoff.md`
