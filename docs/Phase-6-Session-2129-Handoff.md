# Phase 6 Session 2129 Handoff - FindGroup Action 12 Declined Message Payload Evidence

Date: 2026-06-02
Unit of Work: UOW-2129
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

For ordinary Phase 6 test-only, planner, disabled-boundary, packet-evidence, and documentation units, use filtered `dotnet test` commands that include only the edited test class and directly adjacent service/parser tests. Filtered `dotnet test` already builds the affected project and dependencies; do not add a full solution build merely as a compile check.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` can compose non-live `CmFindGroup` boundary plans from injected composition/adapter services, but it is not invoked by live packet processing.
- `FindGroupSideEffectDispatchExecutorService` records opt-in direct-packet and world-broadcast execution order, including direct-before-broadcast order, disabled-boundary action `2`/`6` posted-message-before-refresh order, disabled-boundary action `10` action-26-before-action-10 order, and disabled-boundary action `13` no-action-26 update evidence.
- Action `9` disabled boundary evidence surfaces instance-group mutation status and covers missing removal still sending refreshed action `10` show-list behavior.
- Action `12` disabled boundary evidence covers accepted group invite, accepted alliance invite, declined whisper, missing applicant, missing responder instance-group branches, and the inner branch status surfaced on the boundary intent plan.
- Action `12` declined whisper now has focused payload evidence for the planned `SM_MESSAGE` shape: `ChatType.WHISPER` id `4`, non-staff Elyos sender race filter, responder id/name, and Java `ChatUtil.l10n(1400217)` encoded string.
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

## Current UOW Commit Message

- `[Phase 6][UOW-2129] Add find group declined message payload evidence`

## Validation In UOW-2129

- Focused C# passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests" --no-restore`
  - Final result: 77 tests passed.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - No Java source changed. This UOW reviewed Java `FindGroupService.sendInstanceApplicationResult`, `SM_MESSAGE.writeImpl`, and `ChatType.WHISPER`; no focused Java packet-generation fixture was identified for this existing C# packet evidence slice.
- Broad .NET validation was skipped:
  - Broad-validation trigger: none.
  - The UOW did not enable live `CmFindGroup`, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` declined branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; `Aion.GameServer.Network.Aion.ServerPackets.SmMessage` | Service Method / Packet | Partial | Unit Tested | Partial Parity | Focused evidence now asserts the planned declined action `12` `SmMessage` unencrypted payload for Java `SM_MESSAGE.writeImpl` field order and `ChatType.WHISPER` id `4`. Live `CM_FIND_GROUP` dispatch and Java runtime/socket trace remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE.writeImpl` | `Aion.GameServer.Network.Aion.ServerPackets.SmMessage.WritePayload` | Packet | Partial | Unit Tested | Partial Parity | The action `12` declined whisper packet covers chat type, sender race filter, sender object id, sender name, and localized message string. Other `SM_MESSAGE` branches such as staff race suppression, manual system messages, truncation warning behavior, and active-player null early return remain outside this UOW. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `12` declined whisper now has focused payload evidence, but live socket behavior remains unverified.
- Java runtime/socket trace was not produced in UOW-2129.
- Broader `SM_MESSAGE` behavior remains only partially covered.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: add live-readiness failure-result tests for missing world recipients or skipped invite recipients before any `ProcessPacketAsync` wiring.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Review multi-step mutation ordering under concurrent singleton callers before live dispatch.
- Add a narrow adapter-result failure evidence slice for missing direct recipients or skipped invite recipients.

## Files Changed In UOW-2129

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRecruitmentPlanServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2129-Completion.md`
- `docs/Phase-6-Session-2129-Handoff.md`
