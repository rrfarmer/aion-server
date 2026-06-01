# Phase 6 Session 2083 Handoff - Find Group Action 12 Invite Executor Evidence

Date: 2026-06-01
Unit of Work: UOW-2083
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
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, auto-group static-data facts, form-anywhere config facts, auto-group corpus evidence, parsed-but-no-runImpl adapter evidence, live-dispatch readiness reporting, logout cleanup observer evidence, joined-team invite observer evidence, instance-group join-threshold removal evidence, readiness evidence alignment, and action 12 disabled invite executor evidence.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupLiveDispatchReadinessReportService` separates observer/executor evidence from global live-dispatch blockers.
- `FindGroupConnectionClientActionCompositionPlanService` can source active-player, world-player, team/member, `AutoGroupTable`, and `GameServerOptions.Instance.FormInstanceGroupAnywhere` facts for disabled plans.
- `FindGroupInstanceApplicationInviteDispatchPlanService` can consume action 12 invite intents and compose group/alliance invite request-service results without sending packets.
- `PlayerEnterWorldService.LeaveWorldAsync` can record observer-only disabled `FindGroupLogoutCleanupPlan` before pending question denial.
- `PlayerGroupInviteRequestService` and `PlayerAllianceInviteRequestService` can expose observer-only disabled `FindGroupJoinedTeamPlan` evidence when supplied with `FindGroupJoinedTeamLifecycleRecorder`.
- `FindGroupRecruitmentPlanService.OnJoinedTeam` removes stored instance-group registration when effective current-team size reaches `MinMembers`, matching Java `ServerWideGroup.getMembers()` threshold behavior for this planner slice.

## Latest Completed Work

- UOW-2079: observer-only find-group logout cleanup plan in leave-world composition.
- UOW-2080: observer-only find-group joined-team plan in group/alliance invite accept composition.
- UOW-2081: find-group joined-team instance-group threshold removal in disabled planner.
- UOW-2082: find-group readiness report records lifecycle observer evidence while staying blocked for live dispatch.
- UOW-2083: action 12 disabled group/alliance invite executor evidence.

## Recent Commits

- `26a5f4579 [Phase 6][UOW-2082] Align find group readiness evidence`
- `dcbd85521 [Phase 6][UOW-2081] Remove joined instance group registrations`
- `17b52546b [Phase 6][UOW-2080] Record find group joined-team invites`
- `cd37ed13e [Phase 6][UOW-2079] Record find group logout cleanup`

## Validation In UOW-2083

- Focused C# action-12/find-group tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~PlayerGroupInviteRequestServiceTests|FullyQualifiedName~PlayerAllianceInviteRequestServiceTests" --no-restore`
  - Result: 64 tests passed.
  - Existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven parser golden passed:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_FIND_GROUP_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: 12 tests passed.
- Broad .NET validation was skipped:
  - Disabled executor/readiness update only; no live connection dispatch, packet primitive, persistence, crypto, scheduling, world-state, or live send path was enabled.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action 12 | `Aion.GameServer.Services.FindGroupClientActionPlanService`; `FindGroupInstanceApplicationInviteDispatchPlanService` | Client Action / Service Plan | Partial | Unit Tested; Java Golden Tested | Partial Parity | C# parses/plans action 12 and can compose disabled group/alliance invite request-service results. Live `GameServerConnection` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` accepted invite branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; `FindGroupInstanceApplicationInviteDispatchPlanService` | Find Group Service / Invite Dispatch | Partial | Unit Tested | Partial Parity | C# preserves group-vs-alliance invite selection by `minMembers` and now composes the matching invite request service result. Direct packet sends remain disabled. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.inviteToGroup` find-group action 12 path | `Aion.GameServer.Services.PlayerGroupInviteRequestService.SendInvite` via disabled executor | Group Invite / Request Service | Partial | Unit Tested | Partial Parity | Disabled executor registers the party question request and exposes packet plans; it does not send packets or enable live action 12 dispatch. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.inviteToAlliance` find-group action 12 path | `Aion.GameServer.Services.PlayerAllianceInviteRequestService.SendInvite` via disabled executor | Alliance Invite / Request Service | Partial | Unit Tested | Partial Parity | Disabled executor registers the alliance question request and preserves existing redirection behavior through the request service; it does not send packets or enable live action 12 dispatch. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The action 12 executor is not invoked from the live connection boundary.
- Direct packet sends from group/alliance invite request results are still not executed by this path.
- World broadcasts, encrypted socket behavior, real-client behavior, visibility filtering, and concurrency remain unverified.

## Next Recommended Unit of Work

- Next sequential task: define a no-live-send executor contract for direct `FindGroupDirectPacketIntent` and `FindGroupWorldBroadcastIntent` dispatch so direct sends/broadcasts can be audited separately before any live `CM_FIND_GROUP` boundary wiring.

Safe alternative candidates:

- Inspect `GameServerConnection` `CmFindGroup` deferred branch and design the final disabled boundary aggregation report.
- Add Java-side fixture/golden evidence for another `FindGroupService` packet branch where a narrow Java target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.

## Files Changed In UOW-2083

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupInstanceApplicationInviteDispatchPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupInstanceApplicationInviteDispatchPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-Session-2083-Completion.md`
- `docs/Phase-6-Session-2083-Handoff.md`
