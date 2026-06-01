# Phase 6 Session 2081 Handoff - Find Group Instance-Group Join Removal

Date: 2026-06-01
Unit of Work: UOW-2081
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
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, auto-group static-data facts, form-anywhere config facts, auto-group corpus evidence, parsed-but-no-runImpl adapter evidence, live-dispatch readiness reporting, logout cleanup observer evidence, joined-team invite observer evidence, and instance-group join-threshold removal evidence.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupConnectionClientActionCompositionPlanService` can source active-player, world-player, team/member, `AutoGroupTable`, and `GameServerOptions.Instance.FormInstanceGroupAnywhere` facts for disabled plans.
- `PlayerEnterWorldService.LeaveWorldAsync` can record an observer-only disabled `FindGroupLogoutCleanupPlan` before pending question denial when supplied with a find-group service and observer.
- `PlayerGroupInviteRequestService` and `PlayerAllianceInviteRequestService` can expose observer-only disabled `FindGroupJoinedTeamPlan` evidence when supplied with `FindGroupJoinedTeamLifecycleRecorder`.
- `FindGroupRecruitmentPlanService.OnJoinedTeam` now removes stored instance-group registration when the effective current-team size reaches `MinMembers`, matching Java `ServerWideGroup.getMembers()` threshold behavior for this planner slice.

## Latest Completed Work

- UOW-2077: connection-adjacent evidence for `CM_FIND_GROUP` actions `20` and `25` as parsed-but-no-runImpl.
- UOW-2078: find-group live-dispatch readiness report and tests.
- UOW-2079: observer-only find-group logout cleanup plan in leave-world composition.
- UOW-2080: observer-only find-group joined-team plan in group/alliance invite accept composition.
- UOW-2081: find-group joined-team instance-group threshold removal in disabled planner.

## Recent Commits

- `17b52546b [Phase 6][UOW-2080] Record find group joined-team invites`
- `cd37ed13e [Phase 6][UOW-2079] Record find group logout cleanup`
- `bff53d353 [Phase 6][UOW-2078] Add find group dispatch readiness report`
- `9a05fb2ad [Phase 6][UOW-2077] Add find group no-runImpl adapter evidence`

## Validation In UOW-2081

- Focused C# find-group planner/invite tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~PlayerGroupInviteRequestServiceTests|FullyQualifiedName~PlayerAllianceInviteRequestServiceTests" --no-restore`
  - Result: 39 tests passed.
  - Existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - No narrow Java test class exists for `FindGroupService.onJoinedTeam` or `ServerWideGroup` under `game-server/test`.
  - Java evidence is source review of the exact methods listed in the completion doc.
- Broad .NET validation was skipped:
  - Localized disabled planner mutation plus focused tests; no live dispatch, packet primitive, persistence, crypto, scheduling, connection dispatch, or broad runtime side-effect surface changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onJoinedTeam` instance-group branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam`; `FindGroupInstanceGroupRemovalPlan` | Find Group Service / Plan | Partial | Unit Tested | Partial Parity | C# now removes a stored instance-group registration when the effective current-team member count reaches `MinMembers`, matching Java's `ServerWideGroup.getMembers()` proxy for the represented planner slice. |
| `com.aionemu.gameserver.model.gameobjects.findGroup.ServerWideGroup.getMembers` threshold behavior | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam` current-team-size proxy | Model Behavior / Planner Input | Partial | Unit Tested | Partial Parity | C# uses `currentTeam.Size` when no explicit join-state override is supplied. Full live auto-group membership/runtime behavior remains outside this disabled planner slice. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- Joined-team handling remains disabled planner/observer evidence, not a live singleton service path.
- Full live auto-group registration, registration windows, periodic instance scheduling, and `SM_AUTO_GROUP` workflows remain outside this UOW.
- Encrypted socket behavior, real-client behavior, service concurrency, and live group/alliance invite dispatch from `CM_FIND_GROUP` remain unverified.

## Next Recommended Unit of Work

- Next sequential task: update `FindGroupLiveDispatchReadinessReportService` to recognize the joined-team observer and instance-group threshold evidence while keeping live dispatch blocked for direct-send/world-broadcast/group-alliance invite runtime gaps.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for `AutoGroupData` if a lightweight Java test can be introduced safely.
- Review one remaining `FindGroupService` action branch for runtime-fact gaps before live dispatch is considered.
- Audit `GameServerConnection` `CmFindGroup` deferred branch against the readiness report to define the final pre-live checklist.

## Files Changed In UOW-2081

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRecruitmentPlanServiceTests.cs`
- `docs/Phase-6-Session-2081-Completion.md`
- `docs/Phase-6-Session-2081-Handoff.md`
