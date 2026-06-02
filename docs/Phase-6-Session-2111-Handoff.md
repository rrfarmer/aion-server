# Phase 6 Session 2111 Handoff - FindGroup Action 12 Connection Helper Evidence

Date: 2026-06-02
Unit of Work: UOW-2111
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

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` can compose non-live `CmFindGroup` boundary plans from injected composition/adapter services, but it is not invoked by live packet processing.
- `FindGroupSideEffectDispatchExecutorService` records opt-in direct-packet and world-broadcast execution order, including direct-before-broadcast order, for future live-boundary audits.
- UOW-2111 added disabled action `12` accept evidence: the connection helper can resolve an online applicant through `IGameClientConnectionRegistry.ForEachOnlinePlayer` and compose a group invite request through injected runtimes without live socket sends.
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

## Current UOW Commit Message

- `[Phase 6][UOW-2111] Add find group action 12 connection helper evidence`

## Validation In UOW-2111

- Focused C# passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests" --no-restore`
  - First run failed at compile time because the new test asserted a non-existent `PendingGroupInviteRequest.InvitedObjectId` property.
  - Final result after correcting the assertion: 14 tests passed.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - No Java source changed. This UOW reviewed Java `CM_FIND_GROUP` and `FindGroupService.sendInstanceApplicationResult`; no focused Java test target was identified for this disabled C# connection-helper evidence.
- Broad .NET validation was skipped:
  - Broad-validation trigger: none.
  - The UOW did not enable live `CmFindGroup`, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `12` | `Aion.GameServer.Network.Aion.GameServerConnection.CreateDisabledFindGroupBoundaryPlan`; `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet / Connection Helper | Partial | Unit Tested | Partial Parity | Disabled connection helper parses action `12`, resolves the applicant, and composes non-live side effects. Live `ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` accept branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; `FindGroupConnectionBoundaryDispatchAdapterService`; `FindGroupInstanceApplicationInviteDispatchPlanService` | Service Method / Boundary Adapter | Partial | Unit Tested | Partial Parity | Focused evidence covers accepted group invite composition with `minMembers <= 6` through the disabled connection helper. Alliance accept, declined whisper connection-helper evidence, live invite packet side effects, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.world.World.getPlayer` applicant resolution | `IGameClientConnectionRegistry.ForEachOnlinePlayer`; `GameServerConnection.ResolveOnlinePlayerByObjectId` | Runtime Resolver | Partial | Unit Tested | Partial Parity | The disabled connection helper can resolve an online applicant from the connection registry. World/runtime identity, socket order, and real-client behavior remain unverified. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `12` accepted group invite has disabled connection-helper evidence, but accepted alliance invite and declined whisper still need connection-helper evidence.
- Action `12` live invite packet sends, question response execution, group/alliance membership mutation, Java runtime traces, real-client behavior, and socket-level order remain unverified.
- Direct packet sends, race-filtered world broadcasts, visibility filtering, singleton mutation ordering, enumeration snapshots, and concurrency remain unverified for live dispatch.
- Broad .NET suite/build was not run in UOW-2111 because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: add disabled action `12` connection-helper evidence for the accepted alliance invite branch (`minMembers > 6`) using the connection resolver and injected group/alliance runtimes.

Safe alternative candidates:

- Add disabled action `12` connection-helper evidence for declined whisper direct packet intent.
- Continue reviewing `FindGroupRecruitmentPlanService` enumeration snapshot behavior against Java stream snapshots under concurrent map state.
- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.

## Files Changed In UOW-2111

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2111-Completion.md`
- `docs/Phase-6-Session-2111-Handoff.md`
