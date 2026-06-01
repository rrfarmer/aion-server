# Phase 6 Session 2080 Handoff - Find Group Joined-Team Invite Observer

Date: 2026-06-01
Unit of Work: UOW-2080
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
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, auto-group static-data facts, form-anywhere config facts, auto-group corpus evidence, parsed-but-no-runImpl adapter evidence, live-dispatch readiness reporting, logout cleanup observer evidence, and joined-team invite observer evidence.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupConnectionClientActionCompositionPlanService` can source active-player, world-player, team/member, `AutoGroupTable`, and `GameServerOptions.Instance.FormInstanceGroupAnywhere` facts for disabled plans.
- `FindGroupLiveDispatchReadinessReportService` still blocks live dispatch pending direct send, world broadcast, group/alliance invite dispatch, lifecycle hook completion, encrypted socket, real-client, and concurrency evidence.
- `PlayerEnterWorldService.LeaveWorldAsync` can record an observer-only disabled `FindGroupLogoutCleanupPlan` before pending question denial when supplied with a find-group service and observer.
- `PlayerGroupInviteRequestService` and `PlayerAllianceInviteRequestService` can now expose observer-only disabled `FindGroupJoinedTeamPlan` evidence when supplied with `FindGroupJoinedTeamLifecycleRecorder`.

## Latest Completed Work

- UOW-2076: focused-test policy tightened in orchestration, parity, and startup docs.
- UOW-2077: connection-adjacent evidence for `CM_FIND_GROUP` actions `20` and `25` as parsed-but-no-runImpl.
- UOW-2078: find-group live-dispatch readiness report and tests.
- UOW-2079: observer-only find-group logout cleanup plan in leave-world composition.
- UOW-2080: observer-only find-group joined-team plan in group/alliance invite accept composition.

## Recent Commits

- `cd37ed13e [Phase 6][UOW-2079] Record find group logout cleanup`
- `bff53d353 [Phase 6][UOW-2078] Add find group dispatch readiness report`
- `9a05fb2ad [Phase 6][UOW-2077] Add find group no-runImpl adapter evidence`
- `534290305 [Phase 6][UOW-2076] Tighten focused test policy`

## Validation In UOW-2080

- Focused C# invite/find-group tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerGroupInviteRequestServiceTests|FullyQualifiedName~PlayerAllianceInviteRequestServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - Result: 38 tests passed.
  - Existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - Targeted search found no narrow Java test class for `PlayerGroupInvite`, `PlayerAllianceInvite`, entered events, or `onJoinedTeam` under `game-server/test`.
  - Java evidence is source review of the exact invite/event/service call chain.
- Broad .NET validation was skipped:
  - Optional observer-only invite lifecycle evidence plus focused tests; no live find-group dispatch, packet primitive, persistence, crypto, scheduling, shared serialization, or broad side-effect surface changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupEnteredEvent` / `PlayerGroupService.addPlayerToGroup` find-group slice | `Aion.GameServer.Services.PlayerGroupInviteRequestService`; `FindGroupJoinedTeamLifecycleRecorder.RecordGroupJoin` | Group Lifecycle / Service | Partial | Unit Tested | Partial Parity | C# now records disabled `FindGroupService.onJoinedTeam` plans after accepted group invite membership mutation. Live singleton dispatch remains disabled. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceEnteredEvent` / `PlayerAllianceService.addPlayerToAlliance` find-group slice | `Aion.GameServer.Services.PlayerAllianceInviteRequestService`; `FindGroupJoinedTeamLifecycleRecorder.RecordAllianceJoin` | Alliance Lifecycle / Service | Partial | Unit Tested | Partial Parity | C# now records disabled `FindGroupService.onJoinedTeam` plans after accepted alliance invite membership mutation, including group merge members. Live singleton dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onJoinedTeam` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam`; `FindGroupJoinedTeamPlan` | Find Group Service / Plan | Partial | Unit Tested | Partial Parity | Existing planner behavior is now connected to group/alliance invite accept paths as observer-only evidence. Instance-group min-member removal state is still not sourced from live auto-group runtime. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- The joined-team hook is observer-only; it does not send direct packets or world broadcasts.
- Instance-group min-member removal input is not yet sourced from a live C# auto-group runtime.
- Encrypted socket behavior, real-client behavior, service concurrency, and live group/alliance invite dispatch from `CM_FIND_GROUP` remain unverified.
- `AutoGroupType`, `AutoGroupService`, registration windows, periodic instance scheduling, and `SM_AUTO_GROUP` live workflows remain outside recent units.

## Next Recommended Unit of Work

- Next sequential task: inspect whether the joined-team recorder can safely source instance-group min-member state from existing auto-group/instance-group C# surfaces, or whether a readiness report should keep it blocked.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for `AutoGroupData` if a lightweight Java test can be introduced safely.
- Review one remaining `FindGroupService` action branch for runtime-fact gaps before live dispatch is considered.
- Audit `GameServerConnection` `CmFindGroup` deferred branch against the readiness report to define the final pre-live checklist.

## Files Changed In UOW-2080

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupJoinedTeamLifecycleRecorder.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupInviteRequestService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceInviteRequestService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerGroupInviteRequestServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceInviteRequestServiceTests.cs`
- `docs/Phase-6-Session-2080-Completion.md`
- `docs/Phase-6-Session-2080-Handoff.md`
