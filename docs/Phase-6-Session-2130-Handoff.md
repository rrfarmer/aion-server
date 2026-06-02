# Phase 6 Session 2130 Handoff - FindGroup Action 12 Failure Result Evidence

Date: 2026-06-02
Unit of Work: UOW-2130
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

Focused validation is the default. Do not run the broad .NET suite or full solution build without a documented broad-validation trigger.

Before expensive commands, record:

- Changed surface.
- Exact focused C#, Java/Maven, or hygiene command.
- Java/Maven command or the reason it is unavailable/not relevant.
- Broad-validation trigger, or `none`.
- Broad .NET suite/build decision.

For ordinary Phase 6 test-only, planner, disabled-boundary, packet-evidence, failure-result, and documentation units, use filtered `dotnet test` commands that include only the edited test class and directly adjacent service/parser tests. Filtered `dotnet test` already builds the affected project and dependencies; do not add a full solution build merely as a compile check.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` can compose non-live `CmFindGroup` boundary plans from injected composition/adapter services, but it is not invoked by live packet processing.
- `FindGroupSideEffectDispatchExecutorService` records opt-in direct-packet and world-broadcast execution order, including direct-before-broadcast order, disabled-boundary action `2`/`6` posted-message-before-refresh order, disabled-boundary action `10` action-26-before-action-10 order, and disabled-boundary action `13` no-action-26 update evidence.
- Action `9` disabled boundary evidence surfaces instance-group mutation status and covers missing removal still sending refreshed action `10` show-list behavior.
- Action `12` disabled boundary evidence covers accepted group invite, accepted alliance invite, declined whisper, missing applicant, missing responder instance-group branches, and the inner branch status surfaced on the boundary intent plan.
- Action `12` declined whisper has focused payload evidence for the planned `SM_MESSAGE` shape: `ChatType.WHISPER` id `4`, non-staff Elyos sender race filter, responder id/name, and Java `ChatUtil.l10n(1400217)` encoded string.
- Action `12` failure-result evidence now covers declined-whisper missing direct recipient and accepted-invite missing inviter/responder without live dispatch or request mutation.
- Action `15` disabled boundary evidence surfaces member-info status and covers missing target no-side-effect behavior.
- Action `17` disabled boundary evidence surfaces instance-group mutation status and covers missing instance-group no-side-effect behavior.
- Actions `20` and `25` disabled boundary evidence covers Java's parsed-only no-run behavior: no direct packets, no world broadcasts, no executor order entries, and no registry side effects.
- `PHASE-6-PROGRESS.md` should remain untouched in normal sessions.

## Latest Completed Work

- UOW-2100: non-live `CM_FIND_GROUP` dispatch adapter/result surface added.
- UOW-2101: `FindGroupRecruitmentPlanService` state stores aligned with Java concurrent map shape.
- UOW-2102: Java `FindGroupService.getInstance` lifecycle call-site readiness inventory added.
- UOW-2103: non-live group/alliance disband recruitment cleanup evidence added.
- UOW-2104: injected connection wiring evidence added for group/alliance joined-team cleanup.
- UOW-2105: production DI singleton graph evidence added for FindGroup joined-team/disband callers.
- UOW-2106: logout cleanup now uses the injected shared FindGroup service without requiring observer activation.
- UOW-2107: active docs now require focused validation decisions before expensive broad .NET commands.
- UOW-2108: `GameServerConnection` can consume injected non-live FindGroup composition/adapter services to create disabled boundary plans.
- UOW-2109: focused `onJoinedTeam` mutation-priority evidence added for leader solo-recruitment re-add over full-team removal.
- UOW-2110: opt-in FindGroup side-effect executor now records direct/broadcast execution order.
- UOW-2111: disabled action `12` accepted group invite connection-helper evidence added.
- UOW-2112: disabled action `12` accepted alliance invite connection-helper evidence added.
- UOW-2113: disabled action `12` declined whisper connection-helper evidence added.
- UOW-2114: disabled action `12` missing applicant and missing instance-group connection-helper evidence added.
- UOW-2115: show-list plans now have materialized snapshot evidence.
- UOW-2116: group joined-team recorder ordering evidence added.
- UOW-2117: alliance joined-team recorder ordering evidence added.
- UOW-2118: disabled client-action-to-logout singleton cleanup evidence added.
- UOW-2119: disabled client-action-to-group-disband singleton cleanup evidence added.
- UOW-2120: disabled client-action-to-alliance-disband singleton cleanup evidence added.
- UOW-2121: action `12` disabled boundary intent plan now exposes branch status for accepted group, accepted alliance, declined, missing applicant, and missing instance group.
- UOW-2122: disabled boundary execution order now records action `2`/`6` posted message before refreshed show-list direct packet.
- UOW-2123: disabled boundary execution order now records action `10` form-anywhere action `26` mask list before action `10` instance-group show list.
- UOW-2124: disabled boundary action `13` update evidence now records no action `26` mask-list packet.
- UOW-2125: disabled boundary action `17` now exposes instance-group mutation status and records missing-instance-group no-side-effect behavior.
- UOW-2126: disabled boundary action `9` now exposes instance-group mutation status and records missing-removal refreshed-list behavior.
- UOW-2127: disabled boundary action `15` now exposes member-info status and records missing-target no-side-effect behavior.
- UOW-2128: disabled boundary actions `20` and `25` now record parsed-only no-run behavior without side effects.
- UOW-2129: action `12` declined `SM_MESSAGE` whisper now has focused Java-shaped payload evidence.
- UOW-2130: action `12` disabled direct/invite dispatch failure-result evidence now covers missing recipient/player branches.

## Current UOW Commit Message

- `[Phase 6][UOW-2130] Add find group action 12 failure result evidence`

## Validation In UOW-2130

- Focused C# passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupInstanceApplicationDirectDispatchPlanServiceTests|FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - Final result: 79 tests passed.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - No Java source changed. This UOW reviewed Java `CM_FIND_GROUP.runImpl` action `12` and `FindGroupService.sendInstanceApplicationResult`; no focused Java test target was identified for this disabled C# failure-result evidence.
- Broad .NET validation was skipped:
  - Broad-validation trigger: none.
  - The UOW did not enable live `CmFindGroup`, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` action `12` | `Aion.GameServer.Services.FindGroupInstanceApplicationDirectDispatchPlanService`; `Aion.GameServer.Services.FindGroupInstanceApplicationInviteDispatchPlanService` | Client Action Boundary | Partial | Unit Tested | Partial Parity | Focused disabled evidence records failure-result surfaces for declined-whisper missing direct recipient and accepted-invite missing inviter/responder. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; disabled direct/invite dispatch plans | Service Method | Partial | Unit Tested | Partial Parity | Java source reviewed for applicant lookup and invite/decline branching. C# disabled tests surface missing recipient/player status and prove no direct packet audit or invite question mutation is produced in these failure paths. Java runtime/socket trace remains unverified. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `12` failure-result behavior is covered only through disabled direct/invite dispatch plans.
- Java runtime/socket trace was not produced in UOW-2130.
- Broader invite request behavior and live connection behavior remain unverified.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: review multi-step mutation ordering under concurrent singleton callers before live dispatch, or add one more adapter-result evidence slice for direct packet/world broadcast execution failures if a concrete missing status is not yet surfaced.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Add live-readiness tests around connection-registry direct packet ordering relative to the triggering client packet.
- Inventory remaining `CM_FIND_GROUP` blockers into a concise go/no-go checklist before any live `ProcessPacketAsync` wiring.

## Files Changed In UOW-2130

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupInstanceApplicationDirectDispatchPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupInstanceApplicationInviteDispatchPlanServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2130-Completion.md`
- `docs/Phase-6-Session-2130-Handoff.md`
