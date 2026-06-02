# Phase 6 Session 2085 Handoff - Focused Test Policy Tightening

Date: 2026-06-01
Unit of Work: UOW-2085
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

Do not run the broad .NET suite or full solution build as a routine heartbeat, end-of-unit habit, or substitute for choosing the right parity evidence. Full validation is opt-in by evidence, not habit: name the broad-validation trigger before running an unfiltered project test, solution test, or solution build.

Use the narrowest command that proves the scoped change:

- Documentation-only units: run `git diff --check`; runtime tests are not applicable unless generated artifacts, scripts, or executable docs changed.
- Test-only units: run the edited test class or smallest directly affected filter; do not broaden unless product-code risk is revealed.
- Production-code units: run edited service/packet/parser tests plus directly adjacent adapter/composition tests.
- Shared-surface units: start focused, then escalate only for shared infrastructure, packet primitives, serialization helpers, crypto, scheduling, world state, persistence, connection dispatch, live side effects, common model/state changes, suspicious focused failures, explicit user request, or release/readiness checkpoint.

Preferred command shapes:

- C# targeted tests:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SpecificTestClass|FullyQualifiedName~AdjacentTestClass" --no-restore`
- Java targeted tests:
  - `mvn -pl game-server -am test "-Dtest=SpecificJavaTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- Documentation hygiene:
  - `git diff --check`

Avoid these unless a broad trigger is documented:

- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` without a filter
- `dotnet build dotnetConversion\AionServer.slnx`

When broad validation is skipped, document the focused commands and why they were sufficient. When Java/Maven is skipped, document why a narrower Java parity command was unavailable or irrelevant.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, auto-group static-data facts, form-anywhere config facts, auto-group corpus evidence, parsed-but-no-runImpl adapter evidence, live-dispatch readiness reporting, logout cleanup observer evidence, joined-team invite observer evidence, instance-group join-threshold removal evidence, readiness evidence alignment, action 12 disabled invite executor evidence, and side-effect dispatch audit evidence.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupLiveDispatchReadinessReportService` separates observer/executor/audit evidence from global live-dispatch blockers.
- `FindGroupConnectionClientActionCompositionPlanService` can source active-player, world-player, team/member, `AutoGroupTable`, and `GameServerOptions.Instance.FormInstanceGroupAnywhere` facts for disabled plans.
- `FindGroupInstanceApplicationInviteDispatchPlanService` can consume action 12 invite intents and compose group/alliance invite request-service results without sending packets.
- `FindGroupSideEffectDispatchAuditService` can audit direct packet and world-broadcast intents without calling the live connection registry.
- `PlayerEnterWorldService.LeaveWorldAsync` can record observer-only disabled `FindGroupLogoutCleanupPlan` before pending question denial.
- `PlayerGroupInviteRequestService` and `PlayerAllianceInviteRequestService` can expose observer-only disabled `FindGroupJoinedTeamPlan` evidence when supplied with `FindGroupJoinedTeamLifecycleRecorder`.
- `FindGroupRecruitmentPlanService.OnJoinedTeam` removes stored instance-group registration when effective current-team size reaches `MinMembers`, matching Java `ServerWideGroup.getMembers()` threshold behavior for this planner slice.

## Latest Completed Work

- UOW-2081: find-group joined-team instance-group threshold removal in disabled planner.
- UOW-2082: find-group readiness report records lifecycle observer evidence while staying blocked for live dispatch.
- UOW-2083: action 12 disabled group/alliance invite executor evidence.
- UOW-2084: side-effect dispatch audit contract for direct packet and world-broadcast intents.
- UOW-2085: focused testing policy tightened so full .NET suite/build is exception-only.

## Recent Commits

- `54a4dfa40 [Phase 6][UOW-2084] Add find group side-effect dispatch audit`
- `188e80fb8 [Phase 6][UOW-2083] Add find group action 12 invite executor evidence`
- `26a5f4579 [Phase 6][UOW-2082] Align find group readiness evidence`
- `dcbd85521 [Phase 6][UOW-2081] Remove joined instance group registrations`

## Validation In UOW-2085

- Documentation hygiene passed:
  - `git diff --check`
- Runtime C# tests were not run:
  - Documentation-only policy update; no generated artifacts, scripts, executable docs, C# code, Java code, packet primitives, parsers, or runtime behavior changed.
- Focused Java/Maven was not run:
  - Documentation-only policy update; no Java source-of-truth behavior or Java-executable parity target changed.
- Broad .NET validation was skipped:
  - No broad-validation trigger applied.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| N/A | N/A | Documentation / Process | N/A | Manual Only | N/A | Process-only update. No Java or C# runtime artifact was changed. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Direct packet sends, world race-filter fanout, encrypted socket behavior, real-client behavior, visibility filtering, and concurrency remain unverified.
- A future opt-in executor must still prove connection-registry send/broadcast behavior before live dispatch can be considered.

## Next Recommended Unit of Work

- Next sequential task: inspect `GameServerConnection`'s deferred `CmFindGroup` branch and create a final disabled boundary aggregation report that lists planner, invite executor, side-effect audit, lifecycle observer, and remaining live blockers in one place.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for another `FindGroupService` packet branch where a narrow Java target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Design opt-in connection-registry direct-send/world-broadcast executor tests without wiring them into `CM_FIND_GROUP`.

## Files Changed In UOW-2085

- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2085-Completion.md`
- `docs/Phase-6-Session-2085-Handoff.md`
