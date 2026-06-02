# Phase 6 Session 2137 Handoff - FindGroup Action 0 Direct Boundary Trace Readiness

Date: 2026-06-02
Unit of Work: UOW-2137
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Focused validation is the default. Do not run the broad .NET suite or full solution build unless a documented broad-validation trigger applies. Each completion/handoff should record the exact focused command and the reason broad validation was skipped or run.

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` can compose non-live boundary plans but is not invoked live.
- Action `0` now has disabled-boundary-plus-opt-in direct-packet execution trace evidence.
- `FindGroupSideEffectDispatchExecutorService` records opt-in direct/broadcast execution order, but not live boundary execution.
- `FindGroupDirectPacketTriggerOrderingReadinessService` blocks live direct-packet ordering claims.
- `FindGroupWorldBroadcastFanoutReadinessService` blocks live world-broadcast fanout claims for actions `1` and `5`.
- `FindGroupConcurrentMutationOrderingReadinessService` records deterministic shared-singleton interleaving fixtures but still blocks live singleton interleaving claims.
- `PHASE-6-PROGRESS.md` remained untouched.

## UOW-2137 Summary

This UOW added focused readiness evidence for Java `CM_FIND_GROUP` action `0`:

- Java action `0` calls `FindGroupService.showRecruitments(player)`.
- C# disabled boundary composes action `0` as a direct `SmFindGroup` intent for the active player.
- A focused test records disabled action `0` acceptance before opt-in registry execution of the direct `SmFindGroup` packet.
- Readiness reports now distinguish that evidence from still-missing live `ProcessPacketAsync` trigger-order proof.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `com.aionemu.gameserver.services.findgroup.FindGroupService.showRecruitments`
- `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`
- `Aion.GameServer.Services.FindGroupDirectPacketTriggerOrderingReadinessService`
- `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReportService`
- `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService`
- `Aion.GameServer.Tests.GameServerConnectionFindGroupBoundaryTests`

## Validation In UOW-2137

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupDirectPacketTriggerOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests" --no-restore
```

Result:

- Passed: 26
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

Full .NET validation was skipped intentionally because this was a focused readiness-report and connection-boundary test unit with no live dispatch, shared packet primitive, common runtime base, persistence, or solution-wide contract change.

Java/Maven validation was not run because no Java source changed and no narrow executable Java test target was identified for this non-live C# readiness-report and boundary-trace fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet | Partial | Unit Tested | Partial Parity | Action `0` disabled boundary can be parsed/composed and opt-in executed in an ordered trace. Live `ProcessPacketAsync` dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showRecruitments` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowRecruitments` | Service Method | Partial | Unit Tested | Partial Parity | Same-race direct `SmFindGroup` action `0` intent evidence exists and can be opt-in executed through `IGameClientConnectionRegistry`. Live socket ordering remains unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService.ExecuteAsync` | Dispatch Utility | Partial | Unit Tested | Partial Parity | Opt-in direct registry send order is recorded for action `0`; this does not prove live `GameServerConnection.SendPacketAsync` behavior. |
| `com.aionemu.gameserver.network.aion.AionClientPacket.run` | `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync` | Connection Boundary | Blocked | Unit Tested | Partial Parity | Java synchronously invokes `runImpl`; C# still defers `CmFindGroup` in `ProcessPacketAsync`, so live trigger-order parity remains blocked. |

## Known Gaps

- Live `CM_FIND_GROUP` execution is still disabled.
- No live `ProcessPacketAsync` trace proves outgoing direct packets are ordered relative to a triggering live `CM_FIND_GROUP` client packet.
- No test proves live action `1`/`5` world-broadcast same-race fanout and opposite-race exclusion.
- No test proves live action `12` invite dispatch behavior.
- No encrypted socket or real-client comparison covers FindGroup.

## Next Recommended Unit of Work

Next sequential task:

- Add focused live-boundary readiness evidence for action `1` or `5` world-broadcast same-race/opposite-race fanout without enabling broad live `CM_FIND_GROUP` dispatch.

Safe candidates:

- Add focused action `12` live invite-dispatch failure/result readiness.
- Add another simple direct-action disabled-boundary-plus-opt-in ordered trace, such as action `4` show applications or action `8` register instance group.
- Add runtime trace scaffolding for FindGroup caller interleavings if a narrow non-invasive hook exists.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2137

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketTriggerOrderingReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketTriggerOrderingReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2137-Completion.md`
- `docs/Phase-6-Session-2137-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2137] Add find group action zero direct boundary trace
```
