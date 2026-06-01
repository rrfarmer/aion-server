# Phase 6 Session 2078 Handoff - Find Group Live Dispatch Readiness Report

Date: 2026-06-01
Unit of Work: UOW-2078
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
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, auto-group static-data facts, form-anywhere config facts, auto-group corpus evidence, parsed-but-no-runImpl adapter evidence, and now a live-dispatch readiness report.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupConnectionClientActionCompositionPlanService` can source active-player, world-player, team/member, `AutoGroupTable`, and `GameServerOptions.Instance.FormInstanceGroupAnywhere` facts for disabled plans.
- `FindGroupLiveDispatchReadinessReportService` says live dispatch is blocked pending direct send, world broadcast, group/alliance invite, lifecycle hook, encrypted socket, real-client, and concurrency evidence.
- `StaticData.AutoGroups` has focused unit coverage and real Java static-data corpus assertions.

## Latest Completed Work

- UOW-2074: `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` config fact for action `10` disabled composition.
- UOW-2075: real Java auto-group static-data corpus assertions for `StaticData.AutoGroups`.
- UOW-2076: focused-test policy tightened in orchestration, parity, and startup docs.
- UOW-2077: connection-adjacent evidence for `CM_FIND_GROUP` actions `20` and `25` as parsed-but-no-runImpl.
- UOW-2078: find-group live-dispatch readiness report and tests.

## Recent Commits

- `9a05fb2ad [Phase 6][UOW-2077] Add find group no-runImpl adapter evidence`
- `534290305 [Phase 6][UOW-2076] Tighten focused test policy`
- `5b7e242dd [Phase 6][UOW-2075] Add autogroup static data corpus evidence`

## Validation In UOW-2078

- Focused C# find-group readiness tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupClientActionDispatchPrerequisitesTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests" --no-restore`
  - Result: 32 tests passed.
  - Existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven parser golden passed:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_FIND_GROUP_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: 12 tests passed.
- Broad .NET validation was skipped:
  - Readiness/report UOW; no live dispatch, shared infrastructure, packet primitive, serialization helper, crypto, scheduling, world state, persistence, common model/state mutation, or broad production side effect changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReportService` | Client Packet Dispatch Readiness / Report | Partial | Unit Tested / Golden File Tested | Partial Parity | Report enumerates Java runImpl action branches and confirms all live-dispatchable actions remain deferred pending runtime side-effect gates. This is readiness evidence only; live dispatch remains disabled in `GameServerConnection`. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReportService`; `Aion.GameServer.Services.FindGroupClientActionDispatchPrerequisites` | Service / Runtime Gate Map | Partial | Unit Tested | Partial Parity | Report names unresolved live gates: direct sends, world broadcasts, group/alliance invites, lifecycle hooks, and runtime comparison. It does not execute Java-equivalent side effects. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` actions `20` and `25` | `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReportService` | Client Packet Readiness / Report | Partial | Unit Tested / Golden File Tested | Partial Parity | Report preserves these actions as parsed-but-no-runImpl and non-dispatching, matching Java source review and existing parser golden evidence. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- Direct packet sends, world broadcasts, group/alliance invite execution, lifecycle hook wiring, encrypted socket behavior, real-client behavior, and service concurrency remain unverified for find-group.
- `AutoGroupType`, `AutoGroupService`, registration windows, periodic instance scheduling, and `SM_AUTO_GROUP` live workflows remain outside recent units.

## Next Recommended Unit of Work

- Next sequential task: inspect the `FindGroupService.onLogout` live lifecycle call chain and decide whether the C# `FindGroupRecruitmentPlanService.OnLogout` disabled cleanup can be wired into existing player logout composition without enabling packet side effects.

Safe alternative candidates:

- Inspect `FindGroupService.onJoinedTeam` live call sites and compare them to existing C# team/group lifecycle surfaces.
- Add Java-side fixture/golden evidence for `AutoGroupData` if a lightweight Java test can be introduced safely.
- Review one remaining `FindGroupService` action branch for runtime-fact gaps before live dispatch is considered.

## Files Changed In UOW-2078

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-Session-2078-Completion.md`
- `docs/Phase-6-Session-2078-Handoff.md`
