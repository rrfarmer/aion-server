# Phase 6 Session 2095 Handoff - Find Group Instance Show Direct Evidence

Date: 2026-06-01
Unit of Work: UOW-2095
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

Do not run the broad .NET suite or full solution build as a routine heartbeat, end-of-unit habit, or substitute for choosing the right parity evidence. Filtered `dotnet test` commands already build the affected project and dependencies, so a full solution build needs its own documented broad-validation trigger.

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
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupConnectionClientActionCompositionPlanService` can compose disabled plans from parsed `CmFindGroup` packets plus active-player, world-player, team/member, `AutoGroupTable`, and config facts.
- `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` can extract explicit side-effect intents from disabled boundary composition plans and, only when explicitly invoked, compose them with `FindGroupSideEffectDispatchExecutorService` results.
- Parsed action 0 recruitment show-list direct packet evidence exists.
- Parsed action 4 application show-list direct packet evidence exists.
- Parsed action 10 instance-group show-list evidence now exists, including optional action 26 enable-register packet before action 10 show-list packet.
- Parsed action 13 instance-group update show-list evidence now exists and intentionally omits action 26.
- Parsed action 15 member-info direct packet evidence exists.
- Parsed action 1 recruitment-removal world-broadcast evidence exists.
- Parsed action 5 application-removal world-broadcast evidence exists.
- `FindGroupSideEffectDispatchExecutorService` can explicitly execute planned direct packet intents through `IGameClientConnectionRegistry.SendPacketToPlayerAsync` and planned race-filtered world broadcasts through `IGameClientConnectionRegistry.BroadcastToWorldAsync`.
- The opt-in executor is not wired into `CM_FIND_GROUP`.
- `FindGroupConnectionBoundaryReadinessAggregateService` provides a single report-only view of the disabled connection boundary, planner, boundary composition evidence, instance-application direct executor, action 12 invite executor, side-effect audit/executor, lifecycle observer evidence, and next live-dispatch requirements.
- `FindGroupLiveDispatchReadinessReportService` separates observer/executor/audit/composition evidence from global live-dispatch blockers.
- `PlayerEnterWorldService.LeaveWorldAsync` can record observer-only disabled `FindGroupLogoutCleanupPlan` before pending question denial.
- `PlayerGroupInviteRequestService` and `PlayerAllianceInviteRequestService` can expose observer-only disabled `FindGroupJoinedTeamPlan` evidence when supplied with `FindGroupJoinedTeamLifecycleRecorder`.
- `FindGroupRecruitmentPlanService.OnJoinedTeam` removes stored instance-group registration when effective current-team size reaches `MinMembers`, matching Java `ServerWideGroup.getMembers()` threshold behavior for this planner slice.

## Latest Completed Work

- UOW-2092: parsed action 1 recruitment-removal world-broadcast boundary evidence added.
- UOW-2093: parsed action 5 application-removal world-broadcast boundary evidence added.
- UOW-2094: parsed action 0/4 show-list direct packet boundary evidence added.
- UOW-2095: parsed action 10/13 instance-group show-list direct packet boundary evidence added.

## Recent Commits

- Current UOW commit message: `[Phase 6][UOW-2095] Add find group instance show direct evidence`
- `1295034a8 [Phase 6][UOW-2094] Add find group show list direct evidence`
- `abc76ff1c [Phase 6][UOW-2093] Add find group application broadcast evidence`
- `a29667610 [Phase 6][UOW-2092] Add find group parsed broadcast evidence`
- `89519a3c4 [Phase 6][UOW-2091] Add find group boundary composition evidence`

## Validation In UOW-2095

- Focused C# boundary composition tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~SmFindGroupTests" --no-restore`
  - Result: 86 tests passed.
  - Existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - C# boundary evidence extraction was added around reviewed Java source behavior. No Java source, Java parser behavior, or Java-executable parity target changed.
- Broad .NET validation was skipped:
  - Controlled evidence extraction and tests only; no live handler wiring, packet primitive, serialization helper, crypto, scheduling, world-state infrastructure, persistence, or shared connection dispatch branch changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`; `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Client Packet / Boundary Evidence | Partial | Unit Tested | Partial Parity | Real parsed C# action 0, action 1, action 4, action 5, action 10, action 13, and action 15 `CmFindGroup` payloads can feed disabled planner composition and explicit opt-in executor evidence. Live `GameServerConnection` dispatch remains intentionally deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroups` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowInstanceGroupsForClient`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupSideEffectDispatchExecutorService` | Planner / Opt-In Direct Packet Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 10 can produce Java's optional action 26 enable-register direct packet followed by the action 10 show-list packet, and parsed action 13 can produce only the action 10 update packet. It does not prove live socket order or real-client parity. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor is opt-in only and is not invoked by the packet boundary.
- Parsed action 8/9/17 instance-group mutation boundary evidence is not complete.
- Action 12 invite and direct packet executor evidence remains connection-adjacent and disabled.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and concurrency remain unverified.
- Broad .NET suite/build was not run in UOW-2095 because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: review parsed action 8/9/17 instance-group mutation branches for direct packet/show-list boundary evidence.

Safe alternative candidates:

- Add narrow Java-side fixture/golden evidence for a `FindGroupService` packet branch if a matching Java test target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Add controlled evidence for action 11/12 parsed boundary paths now that direct/invite executors already exist.

## Files Changed In UOW-2095

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundarySideEffectCompositionEvidenceService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-Session-2095-Completion.md`
- `docs/Phase-6-Session-2095-Handoff.md`
