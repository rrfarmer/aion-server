# Phase 6 Session 2098 Handoff - Find Group Recruitment/Application Mutation Boundary Evidence

Date: 2026-06-01
Unit of Work: UOW-2098
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
- Parsed action 0/4 show-list direct packet evidence exists.
- Parsed action 1/5 world-broadcast evidence exists.
- Parsed action 2/6 add recruitment/application direct packet evidence now exists, including posted system messages plus refreshed show-list packets.
- Parsed action 3/7 update recruitment/application no-packet evidence now exists.
- Parsed action 8 register-instance-group direct packet evidence exists.
- Parsed action 9 remove-instance-group updated show-list evidence exists. Java ignores parsed ids at dispatch and uses active-player state.
- Parsed action 10/13 instance-group show-list evidence exists, including action 10's optional action 26 enable-register packet.
- Parsed action 11 instance-application direct applicant packet evidence exists.
- Parsed action 12 declined instance-application whisper evidence exists.
- Parsed action 12 accepted group/alliance invite intent evidence exists, including the `minMembers <= 6` vs `> 6` branch.
- Parsed action 15 member-info direct packet evidence exists.
- Parsed action 17 update-instance-group updated show-list evidence exists. Java ignores parsed ids at dispatch and uses active-player state plus parsed message.
- Parsed actions 20 and 25 remain parsed-only because Java `runImpl` has no branch.
- `FindGroupSideEffectDispatchExecutorService` can explicitly execute planned direct packet intents through `IGameClientConnectionRegistry.SendPacketToPlayerAsync` and planned race-filtered world broadcasts through `IGameClientConnectionRegistry.BroadcastToWorldAsync`.
- `FindGroupInstanceApplicationInviteDispatchPlanService` can compose disabled group/alliance invite request-service results from action 12 invite intents.
- The opt-in executor and invite dispatcher are not wired into `CM_FIND_GROUP`.

## Latest Completed Work

- UOW-2095: parsed action 10/13 instance-group show-list direct packet boundary evidence added.
- UOW-2096: parsed action 8/9/17 instance-group mutation boundary evidence added.
- UOW-2097: parsed action 11/12 instance-application direct/invite boundary evidence added.
- UOW-2098: parsed action 2/3/6/7 recruitment/application mutation boundary evidence added.

## Recent Commits

- Current UOW commit message: `[Phase 6][UOW-2098] Add find group recruitment mutation evidence`
- `17eb85462 [Phase 6][UOW-2097] Add find group instance application evidence`
- `b9f5696b5 [Phase 6][UOW-2096] Add find group instance mutation evidence`
- `2ca0d1328 [Phase 6][UOW-2095] Add find group instance show direct evidence`
- `1295034a8 [Phase 6][UOW-2094] Add find group show list direct evidence`

## Validation In UOW-2098

- Focused C# boundary composition tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~SmFindGroupTests" --no-restore`
  - Result: 97 tests passed.
  - Existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - C# boundary evidence was added around reviewed Java source behavior. No Java source, Java parser behavior, or Java-executable parity target changed.
- Broad .NET validation was skipped:
  - Controlled evidence tests/readiness text only; no live handler wiring, packet primitive, serialization helper, crypto, scheduling, world-state infrastructure, persistence, or shared connection dispatch branch changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`; `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Client Packet / Boundary Evidence | Partial | Unit Tested | Partial Parity | Real parsed C# action 0, action 1, action 2, action 3, action 4, action 5, action 6, action 7, action 8, action 9, action 10, action 11, action 12, action 13, action 15, and action 17 `CmFindGroup` payloads can feed disabled planner composition and explicit opt-in executor or invite-dispatch evidence. Live `GameServerConnection` dispatch remains intentionally deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.AddRecruitment`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupSideEffectDispatchExecutorService` | Planner / Opt-In Direct Packet Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 2 can add an active-player recruitment and produce the Java-shaped posted system message plus action 0 show-list direct packet. Team subject/runtime details remain planner-covered but not live-dispatch verified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.updateRecruitment` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.UpdateRecruitment`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Planner / Boundary Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 3 can update the active player's recruitment message/type and produce no direct or world packet intents, matching reviewed Java behavior. Live singleton state and concurrency remain unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.AddApplication`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupSideEffectDispatchExecutorService` | Planner / Opt-In Direct Packet Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 6 can add an active-player application and produce the Java-shaped posted system message plus action 4 show-list direct packet. Live socket order remains unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.updateApplication` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.UpdateApplication`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Planner / Boundary Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 7 can update the active player's application message/type/class/level and produce no direct or world packet intents, matching reviewed Java behavior. Live singleton state and concurrency remain unverified. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor and invite dispatcher are opt-in only and are not invoked by the packet boundary.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and concurrency remain unverified.
- Broad .NET suite/build was not run in UOW-2098 because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: review whether all parsed `CM_FIND_GROUP.runImpl` branches now have enough controlled boundary evidence to support a narrow live-dispatch design document, without enabling live dispatch yet.

Safe alternative candidates:

- Add narrow Java-side fixture/golden evidence for a `FindGroupService` packet branch if a matching Java test target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Review action 20/25 parsed-only behavior for documentation clarity before any live dispatch work.

## Files Changed In UOW-2098

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-Session-2098-Completion.md`
- `docs/Phase-6-Session-2098-Handoff.md`
