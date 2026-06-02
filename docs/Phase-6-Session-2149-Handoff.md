# Phase 6 Session 2149 Handoff - FindGroup Action 17 Direct Packet Trace

Date: 2026-06-02
Unit of Work: UOW-2149
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
- Actions `0`, `4`, `8`, `9`, `10`, `11`, `13`, `15`, and `17` have disabled-boundary-plus-opt-in direct-packet execution trace evidence.
- Action `17` existing update records action `10` updated show-list direct packet execution after disabled boundary acceptance.
- Action `17` missing update records missing status with no packet, executor, registry, or socket side effects.
- Action `10` form-anywhere evidence records optional action `26` mask-list direct send before the action `10` show-list direct send.
- Action `13` update evidence records action `10` show-list direct send and no action `26` mask-list send even when form-anywhere is enabled.
- Actions `1` and `5` have disabled-boundary-plus-opt-in world-broadcast fanout trace evidence.
- Action `12` accepted group and alliance replies have disabled-boundary-plus-opt-in invite-request ordering trace evidence.
- Action `12` declined whisper, missing applicant, missing instance group, and missing invite player cases have focused disabled evidence.
- `FindGroupSideEffectDispatchExecutorService` records opt-in direct/broadcast execution order, but not live boundary execution.
- `FindGroupDirectPacketTriggerOrderingReadinessService` blocks live direct-packet ordering claims.
- `FindGroupWorldBroadcastFanoutReadinessService` blocks live world-broadcast fanout claims for actions `1` and `5`.
- `FindGroupConcurrentMutationOrderingReadinessService` records deterministic shared-singleton interleaving fixtures but still blocks live singleton interleaving claims.
- `PHASE-6-PROGRESS.md` remained untouched.

## UOW-2149 Summary

This UOW added focused readiness evidence for Java `CM_FIND_GROUP` action `17`:

- Java action `17` reads `playerOrTeamId`, `instanceMaskId`, and `message`, then calls `FindGroupService.updateInstanceGroup(player, message)`.
- Java ignores the parsed ids in `runImpl`; the active player's object id selects the instance-group entry.
- Java existing update changes message/last-update and calls `showInstanceGroups(player, true)`.
- Java missing update performs no side effects.
- C# disabled boundary composes action `17` existing update as a direct `SmFindGroup` action `10` show-list intent.
- C# disabled boundary records action `17` missing update as missing/no-side-effect.
- Focused tests record disabled action `17` acceptance before opt-in registry direct-packet execution for the existing update.
- Readiness reports now distinguish action `0`/`4`/`8`/`9`/`10`/`11`/`13`/`15`/`17` disabled direct-packet trace evidence from still-missing live `ProcessPacketAsync` direct-packet proof.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `com.aionemu.gameserver.services.findgroup.FindGroupService.updateInstanceGroup`
- `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.FindGroupRecruitmentPlanService`
- `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`
- `Aion.GameServer.Services.FindGroupDirectPacketTriggerOrderingReadinessService`
- `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReportService`
- `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService`
- `Aion.GameServer.Tests.GameServerConnectionFindGroupBoundaryTests`

## Validation In UOW-2149

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupDirectPacketTriggerOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore
```

Result:

- Passed: 100
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

Full .NET validation was skipped intentionally because this was a focused test/readiness-report unit with no live dispatch, shared packet primitive, common runtime base, persistence, or solution-wide contract change.

Java/Maven validation was not run because no Java source changed and no narrow executable Java test target was identified for this non-live C# boundary-trace fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet | Partial | Unit Tested | Partial Parity | Action `17` disabled boundary can be parsed/composed and opt-in executed in ordered direct-packet traces. Live `ProcessPacketAsync` dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.updateInstanceGroup` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.UpdateInstanceGroup` | Service Method | Partial | Unit Tested | Partial Parity | Existing update changes message/last-update and composes an updated action `10` show-list direct packet; missing update records missing status and no side effects. Live socket ordering remains unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService.ExecuteAsync` | Dispatch Utility | Partial | Unit Tested | Partial Parity | Opt-in action `17` registry direct send records the updated `SmFindGroup` show-list packet after disabled boundary acceptance. This does not prove live `GameServerConnection` behavior. |
| `com.aionemu.gameserver.network.aion.AionClientPacket.run` | `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync` | Connection Boundary | Blocked | Unit Tested | Partial Parity | Java synchronously invokes `runImpl`; C# still defers `CmFindGroup` in `ProcessPacketAsync`, so live trigger-order parity remains blocked. |

## Known Gaps

- Live `CM_FIND_GROUP` execution is still disabled.
- No live `ProcessPacketAsync` trace proves actions `0`, `4`, `8`, `9`, `10`, `11`, `13`, `15`, or `17` direct-packet ordering relative to a triggering live `CM_FIND_GROUP` client packet.
- Direct packet and world-broadcast action evidence remains non-live until `ProcessPacketAsync` is wired.
- No encrypted socket or real-client comparison covers FindGroup.

## Next Recommended Unit of Work

Next sequential task:

- Add focused direct-action disabled-boundary-plus-opt-in ordered trace evidence for action `2` add recruitment, proving the posted system message is recorded before the refreshed action `0` show-list packet after boundary acceptance.

Safe candidates:

- Add the matching action `6` add application posted-message-before-refresh boundary trace.
- Add focused live-boundary readiness for missing-recipient direct-packet failures.
- Add focused runtime trace scaffolding for FindGroup caller interleavings if a narrow non-invasive hook exists.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2149

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketTriggerOrderingReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketTriggerOrderingReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2149-Completion.md`
- `docs/Phase-6-Session-2149-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2149] Add find group action seventeen direct trace
```
