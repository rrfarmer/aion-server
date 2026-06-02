# Phase 6 Session 2087 Handoff - Find Group Action 11 Direct Dispatch Evidence

Date: 2026-06-01
Unit of Work: UOW-2087
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
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, auto-group static-data facts, form-anywhere config facts, auto-group corpus evidence, parsed-but-no-runImpl adapter evidence, live-dispatch readiness reporting, logout cleanup observer evidence, joined-team invite observer evidence, instance-group join-threshold removal evidence, readiness evidence alignment, action 11 disabled direct packet executor evidence, action 12 disabled invite executor evidence, side-effect dispatch audit evidence, and boundary aggregate evidence.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupInstanceApplicationDirectDispatchPlanService` can consume action 11 direct packet intents, verify recipient resolution, and return audit evidence without sending packets.
- `FindGroupConnectionBoundaryReadinessAggregateService` provides a single report-only view of the disabled connection boundary, planner, action 11 direct executor, action 12 invite executor, side-effect audit, lifecycle observer evidence, and next live-dispatch requirements.
- `FindGroupLiveDispatchReadinessReportService` separates observer/executor/audit evidence from global live-dispatch blockers.
- `FindGroupConnectionClientActionCompositionPlanService` can source active-player, world-player, team/member, `AutoGroupTable`, and `GameServerOptions.Instance.FormInstanceGroupAnywhere` facts for disabled plans.
- `FindGroupInstanceApplicationInviteDispatchPlanService` can consume action 12 invite intents and compose group/alliance invite request-service results without sending packets.
- `FindGroupSideEffectDispatchAuditService` can audit direct packet and world-broadcast intents without calling the live connection registry.
- `PlayerEnterWorldService.LeaveWorldAsync` can record observer-only disabled `FindGroupLogoutCleanupPlan` before pending question denial.
- `PlayerGroupInviteRequestService` and `PlayerAllianceInviteRequestService` can expose observer-only disabled `FindGroupJoinedTeamPlan` evidence when supplied with `FindGroupJoinedTeamLifecycleRecorder`.
- `FindGroupRecruitmentPlanService.OnJoinedTeam` removes stored instance-group registration when effective current-team size reaches `MinMembers`, matching Java `ServerWideGroup.getMembers()` threshold behavior for this planner slice.

## Latest Completed Work

- UOW-2083: action 12 disabled group/alliance invite executor evidence.
- UOW-2084: side-effect dispatch audit contract for direct packet and world-broadcast intents.
- UOW-2085: focused testing policy tightened so full .NET suite/build is exception-only.
- UOW-2086: disabled `CM_FIND_GROUP` boundary aggregate report added.
- UOW-2087: action 11 disabled direct packet executor evidence added.

## Recent Commits

- `1d96b82ca [Phase 6][UOW-2086] Add find group boundary readiness aggregate`
- `89d823e95 [Phase 6][UOW-2085] Tighten focused test policy`
- `54a4dfa40 [Phase 6][UOW-2084] Add find group side-effect dispatch audit`
- `188e80fb8 [Phase 6][UOW-2083] Add find group action 12 invite executor evidence`

## Validation In UOW-2087

- Focused C# action 11/readiness tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupInstanceApplicationDirectDispatchPlanServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchAuditServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests" --no-restore`
  - Result: 66 tests passed.
  - Existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - C# disabled executor evidence around reviewed Java action 11 source behavior; no Java packet parser, Java runtime behavior, or Java-executable parity target changed.
- Broad .NET validation was skipped:
  - Disabled executor/reporting only; no live dispatch, connection registry send, packet primitive, persistence, crypto, scheduling, world-state mutation, or connection-dispatch behavior changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplication` | `Aion.GameServer.Services.FindGroupInstanceApplicationDirectDispatchPlanService` | Disabled Executor / Side-Effect Boundary | Partial | Unit Tested | Partial Parity | C# can consume the action 11 direct packet intent, resolve intended recipients, and audit the planned `SmFindGroup` packet without live dispatch. It does not send through the connection registry. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action 11 readiness | `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReportService`; `FindGroupConnectionBoundaryReadinessAggregateService` | Readiness Report | Partial | Unit Tested | Partial Parity | Readiness reports now separate action 11 direct packet executor evidence from action 12 invite executor evidence while preserving live-dispatch blockers. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The action 11 direct dispatch service is disabled and does not call the connection registry.
- Direct packet sends, world race-filter fanout, encrypted socket behavior, real-client behavior, visibility filtering, and concurrency remain unverified.
- Future live dispatch still needs an opt-in executor with connection-registry tests before wiring to the packet boundary.

## Next Recommended Unit of Work

- Next sequential task: design opt-in connection-registry direct-send/world-broadcast executor tests for Find Group side effects without wiring them into `CM_FIND_GROUP`.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for another `FindGroupService` packet branch where a narrow Java target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Inspect declined action 12 `SM_MESSAGE` direct packet dispatch evidence and decide whether it should share the action 11 direct-dispatch executor or stay under the generic side-effect audit.

## Files Changed In UOW-2087

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupInstanceApplicationDirectDispatchPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupInstanceApplicationDirectDispatchPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryReadinessAggregateServiceTests.cs`
- `docs/Phase-6-Session-2087-Completion.md`
- `docs/Phase-6-Session-2087-Handoff.md`
