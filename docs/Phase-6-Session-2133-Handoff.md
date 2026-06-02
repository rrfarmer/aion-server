# Phase 6 Session 2133 Handoff - FindGroup Direct Packet Trigger Ordering Readiness

Date: 2026-06-02
Unit of Work: UOW-2133
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

For ordinary Phase 6 test-only, planner, disabled-boundary, packet-evidence, failure-result, readiness-report, and documentation units, use filtered `dotnet test` commands that include only the edited test class and directly adjacent service/parser tests. Filtered `dotnet test` already builds the affected project and dependencies; do not add a full solution build merely as a compile check.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` can compose non-live `CmFindGroup` boundary plans from injected composition/adapter services, but it is not invoked by live packet processing.
- `FindGroupSideEffectDispatchExecutorService` records opt-in direct-packet and world-broadcast execution order, including direct-before-broadcast order, disabled-boundary action `2`/`6` posted-message-before-refresh order, disabled-boundary action `10` action-26-before-action-10 order, and disabled-boundary action `13` no-action-26 update evidence.
- `FindGroupDirectPacketTriggerOrderingReadinessService` now records that Java `AionClientPacket.run` invokes `CM_FIND_GROUP.runImpl` synchronously and that C# opt-in executor order does not prove live direct-packet ordering relative to the triggering client packet.
- Action `9` disabled boundary evidence surfaces instance-group mutation status and covers missing removal still sending refreshed action `10` show-list behavior.
- Action `12` disabled boundary evidence covers accepted group invite, accepted alliance invite, declined whisper, missing applicant, missing responder instance-group branches, declined `SM_MESSAGE` payload, and missing-recipient/player failure results.
- Action `15` disabled boundary evidence surfaces member-info status and covers missing target no-side-effect behavior.
- Action `17` disabled boundary evidence surfaces instance-group mutation status and covers missing instance-group no-side-effect behavior.
- Actions `20` and `25` disabled boundary evidence covers Java's parsed-only no-run behavior: no direct packets, no world broadcasts, no executor order entries, and no registry side effects.
- `FindGroupLiveDispatchGoNoGoChecklistService` records a concise blocked checklist for connection boundary wiring, shared singleton lifecycle, direct packet dispatch, world broadcast dispatch, action `12` invite dispatch, parsed-only no-op actions, and runtime/socket comparison.
- `FindGroupLiveDispatchActionGateMatrixService` maps each parsed Java `CM_FIND_GROUP` action to missing live evidence gates:
  - actions `1` and `5`: world-broadcast gate;
  - actions `3` and `7`: shared-singleton lifecycle gate;
  - actions `0`/`2`/`4`/`6`/`8`/`9`/`10`/`11`/`12`/`13`/`15`/`17`: direct-packet gate;
  - action `12`: action-12 invite gate plus direct-packet gate;
  - actions `20` and `25`: parsed-only ready no-op gate.
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
- UOW-2131: live dispatch readiness now has a blocked go/no-go checklist.
- UOW-2132: live dispatch readiness now has an action-by-action gate matrix.
- UOW-2133: direct packet trigger-ordering readiness now records the missing live boundary ordered trace.

## Current UOW Commit Message

- `[Phase 6][UOW-2133] Add find group direct trigger ordering readiness`

## Validation In UOW-2133

- Focused C# passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketTriggerOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests" --no-restore`
  - Final result: 15 tests passed.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - No Java source changed. This UOW reviewed Java `AionClientPacket.run`, `CM_FIND_GROUP.runImpl`, and `FindGroupService` direct send call paths; no focused Java test target was identified for this C# readiness-report surface.
- Broad .NET validation was skipped:
  - Broad-validation trigger: none.
  - The UOW did not enable live `CmFindGroup`, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.AionClientPacket.run` | `Aion.GameServer.Services.FindGroupDirectPacketTriggerOrderingReadinessService` | Readiness Report | Partial | Unit Tested | Partial Parity | Report records reviewed Java synchronous packet-run shape but does not prove C# live boundary ordering. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupDirectPacketTriggerOrderingReadinessService`; `FindGroupLiveDispatchReadinessReportService` | Readiness Report | Partial | Unit Tested | Partial Parity | Report keeps live direct-packet ordering blocked until C# proves sends are ordered relative to the triggering client packet. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupDirectPacketTriggerOrderingReadinessService` | Readiness Report | Partial | Unit Tested | Partial Parity | Java direct `PacketSendUtility.sendPacket` branch order was reviewed; C# opt-in executor order exists but live `ProcessPacketAsync` dispatch remains deferred. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The trigger-ordering report is readiness evidence only; it does not execute live sends or prove client-visible behavior.
- Direct-packet ordering relative to the triggering client packet remains unverified.
- Shared singleton lifecycle still needs live `CM_FIND_GROUP` execution proof against the same C# state store.
- Race fanout, invite request mutation, and runtime/socket comparison remain unverified.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: add a small readiness report for world-broadcast race filtering and ordering relative to direct packet sends, still without enabling live `ProcessPacketAsync` dispatch.

Safe alternative candidates:

- Review multi-step mutation ordering under concurrent singleton callers before live dispatch.
- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Add a disabled boundary ordered-trace test shape that models the eventual live trigger/send observer without invoking `ProcessPacketAsync`.

## Files Changed In UOW-2133

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketTriggerOrderingReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketTriggerOrderingReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2133-Completion.md`
- `docs/Phase-6-Session-2133-Handoff.md`
