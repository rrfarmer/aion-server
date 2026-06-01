# Phase 6 Session 2079 Handoff - Find Group Logout Cleanup Observer

Date: 2026-06-01
Unit of Work: UOW-2079
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
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, auto-group static-data facts, form-anywhere config facts, auto-group corpus evidence, parsed-but-no-runImpl adapter evidence, live-dispatch readiness reporting, and logout cleanup observer evidence.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupConnectionClientActionCompositionPlanService` can source active-player, world-player, team/member, `AutoGroupTable`, and `GameServerOptions.Instance.FormInstanceGroupAnywhere` facts for disabled plans.
- `FindGroupLiveDispatchReadinessReportService` says live dispatch is blocked pending direct send, world broadcast, group/alliance invite, lifecycle hook, encrypted socket, real-client, and concurrency evidence.
- `PlayerEnterWorldService.LeaveWorldAsync` can now record an observer-only disabled `FindGroupLogoutCleanupPlan` before pending question denial when supplied with a find-group service and observer.

## Latest Completed Work

- UOW-2075: real Java auto-group static-data corpus assertions for `StaticData.AutoGroups`.
- UOW-2076: focused-test policy tightened in orchestration, parity, and startup docs.
- UOW-2077: connection-adjacent evidence for `CM_FIND_GROUP` actions `20` and `25` as parsed-but-no-runImpl.
- UOW-2078: find-group live-dispatch readiness report and tests.
- UOW-2079: observer-only find-group logout cleanup plan in leave-world composition.

## Recent Commits

- `bff53d353 [Phase 6][UOW-2078] Add find group dispatch readiness report`
- `9a05fb2ad [Phase 6][UOW-2077] Add find group no-runImpl adapter evidence`
- `534290305 [Phase 6][UOW-2076] Tighten focused test policy`

## Validation In UOW-2079

- Focused C# logout/find-group tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests" --no-restore`
  - Result: 69 tests passed.
  - Existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - No narrow `PlayerLeaveWorldService`/logout Java unit test exists under `game-server/src/test` in this workspace.
  - Java evidence is source review of `PlayerLeaveWorldService.leaveWorld` and `FindGroupService.onLogout`.
- Broad .NET validation was skipped:
  - Optional observer-only logout plan hook plus focused tests; no live packet dispatch, packet primitive, persistence, or broad side-effect surface changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` find-group slice | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` | Logout Lifecycle / Service | Partial | Unit Tested | Partial Parity | C# now records disabled `FindGroupService.onLogout` cleanup before question denial, matching Java call order for this slice. Broader leave-world order and live side effects remain partial. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onLogout` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnLogout`; `FindGroupLogoutCleanupPlan` | Service Cleanup / Plan | Partial | Unit Tested | Partial Parity | Removes player-object-id keyed recruitment, application, and instance-group entries without packets. Existing tests also show team-keyed recruitment is not removed by player logout. Live singleton wiring remains observer-only. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- Full Java leave-world ordering is still broader than this slice.
- Direct packet sends, world broadcasts, group/alliance invite execution, encrypted socket behavior, real-client behavior, and service concurrency remain unverified for find-group.
- `FindGroupService.onJoinedTeam` lifecycle wiring remains a separate candidate.
- `AutoGroupType`, `AutoGroupService`, registration windows, periodic instance scheduling, and `SM_AUTO_GROUP` live workflows remain outside recent units.

## Next Recommended Unit of Work

- Next sequential task: inspect `FindGroupService.onJoinedTeam` live call sites and compare them to existing C# team/group lifecycle surfaces before deciding whether an observer-only disabled hook can be safely wired.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for `AutoGroupData` if a lightweight Java test can be introduced safely.
- Review one remaining `FindGroupService` action branch for runtime-fact gaps before live dispatch is considered.
- Audit `GameServerConnection` `CmFindGroup` deferred branch against the new readiness report to define the final pre-live checklist.

## Files Changed In UOW-2079

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2079-Completion.md`
- `docs/Phase-6-Session-2079-Handoff.md`
