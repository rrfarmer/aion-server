# Phase 6 Session 2086 Handoff - Find Group Boundary Readiness Aggregate

Date: 2026-06-01
Unit of Work: UOW-2086
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
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, auto-group static-data facts, form-anywhere config facts, auto-group corpus evidence, parsed-but-no-runImpl adapter evidence, live-dispatch readiness reporting, logout cleanup observer evidence, joined-team invite observer evidence, instance-group join-threshold removal evidence, readiness evidence alignment, action 12 disabled invite executor evidence, side-effect dispatch audit evidence, and boundary aggregate evidence.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupConnectionBoundaryReadinessAggregateService` now provides a single report-only view of the disabled connection boundary, planner, invite executor, side-effect audit, lifecycle observer evidence, and next live-dispatch requirements.
- `FindGroupLiveDispatchReadinessReportService` separates observer/executor/audit evidence from global live-dispatch blockers.
- `FindGroupConnectionClientActionCompositionPlanService` can source active-player, world-player, team/member, `AutoGroupTable`, and `GameServerOptions.Instance.FormInstanceGroupAnywhere` facts for disabled plans.
- `FindGroupInstanceApplicationInviteDispatchPlanService` can consume action 12 invite intents and compose group/alliance invite request-service results without sending packets.
- `FindGroupSideEffectDispatchAuditService` can audit direct packet and world-broadcast intents without calling the live connection registry.
- `PlayerEnterWorldService.LeaveWorldAsync` can record observer-only disabled `FindGroupLogoutCleanupPlan` before pending question denial.
- `PlayerGroupInviteRequestService` and `PlayerAllianceInviteRequestService` can expose observer-only disabled `FindGroupJoinedTeamPlan` evidence when supplied with `FindGroupJoinedTeamLifecycleRecorder`.
- `FindGroupRecruitmentPlanService.OnJoinedTeam` removes stored instance-group registration when effective current-team size reaches `MinMembers`, matching Java `ServerWideGroup.getMembers()` threshold behavior for this planner slice.

## Latest Completed Work

- UOW-2082: find-group readiness report records lifecycle observer evidence while staying blocked for live dispatch.
- UOW-2083: action 12 disabled group/alliance invite executor evidence.
- UOW-2084: side-effect dispatch audit contract for direct packet and world-broadcast intents.
- UOW-2085: focused testing policy tightened so full .NET suite/build is exception-only.
- UOW-2086: disabled `CM_FIND_GROUP` boundary aggregate report added.

## Recent Commits

- `89d823e95 [Phase 6][UOW-2085] Tighten focused test policy`
- `54a4dfa40 [Phase 6][UOW-2084] Add find group side-effect dispatch audit`
- `188e80fb8 [Phase 6][UOW-2083] Add find group action 12 invite executor evidence`
- `26a5f4579 [Phase 6][UOW-2082] Align find group readiness evidence`

## Validation In UOW-2086

- Focused C# boundary/readiness tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupClientActionDispatchPrerequisitesTests|FullyQualifiedName~FindGroupSideEffectDispatchAuditServiceTests|FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests" --no-restore`
  - Result: 21 tests passed.
  - Existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - C# report-only aggregate around reviewed Java source boundaries; no Java packet parser, Java runtime behavior, or Java-executable parity target changed.
- Broad .NET validation was skipped:
  - Report-only service and tests; no live dispatch, connection registry send, packet primitive, persistence, crypto, scheduling, world-state mutation, or connection-dispatch behavior changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` live boundary | `Aion.GameServer.Services.FindGroupConnectionBoundaryReadinessAggregateService` | Boundary Readiness Report | Partial | Unit Tested | Partial Parity | C# now has a single disabled aggregate describing the deferred `CmFindGroup` boundary and remaining live-dispatch blockers. It does not execute Java-equivalent live side effects. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` side-effect and lifecycle readiness | `Aion.GameServer.Services.FindGroupConnectionBoundaryReadinessAggregate`; `FindGroupConnectionBoundaryComponentReadiness` | Readiness DTO / Report | Partial | Unit Tested | Partial Parity | Planner, invite executor, side-effect audit, and lifecycle observer evidence is aggregated. Live send/broadcast/fanout/singleton lifecycle wiring remains deferred. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The boundary aggregate is report-only and does not send packets, broadcast to world, mutate live connection state, or wire singleton lifecycle hooks.
- Direct packet sends, world race-filter fanout, encrypted socket behavior, real-client behavior, visibility filtering, and concurrency remain unverified.
- Actions 20 and 25 remain parsed-only because Java has no `runImpl` branch for them.

## Next Recommended Unit of Work

- Next sequential task: design opt-in connection-registry direct-send/world-broadcast executor tests for Find Group side effects without wiring them into `CM_FIND_GROUP`.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for another `FindGroupService` packet branch where a narrow Java target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Inspect the `CM_FIND_GROUP` action 11 `sendInstanceApplication` direct packet path and add a disabled executor evidence slice, parallel to action 12 invite executor evidence.

## Files Changed In UOW-2086

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryReadinessAggregateServiceTests.cs`
- `docs/Phase-6-Session-2086-Completion.md`
- `docs/Phase-6-Session-2086-Handoff.md`
