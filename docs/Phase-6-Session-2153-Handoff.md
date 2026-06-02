# Phase 6 Session 2153 Handoff - FindGroup Action 1 Missing Recruitment Fanout Boundary

Date: 2026-06-02
Unit of Work: UOW-2153
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Focused validation is the default. Do not run the broad .NET suite or full solution build unless a documented broad-validation trigger applies. Each completion/handoff should record the exact focused command, the broad-validation skip/run rationale, and the Java/Maven decision.

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` can compose non-live boundary plans but is not invoked live.
- Actions `0`, `2`, `4`, `6`, `8`, `9`, `10`, `11`, `13`, `15`, and `17` have disabled-boundary-plus-opt-in direct-packet execution trace evidence.
- Action `1` removed-recruitment and missing-recruitment branches now have disabled-boundary evidence; the missing branch records `Missing` and no packet side effects.
- Action `5` has disabled-boundary-plus-opt-in world-broadcast fanout trace evidence for the removed-application branch.
- Action `11` resolved-recipient and missing-recipient branches have disabled-boundary evidence; the missing-recipient branch records `MissingRecipient` and no packet side effects.
- Action `12` accepted group and alliance replies have disabled-boundary-plus-opt-in invite-request ordering trace evidence.
- Action `12` declined whisper, missing applicant, missing instance group, and missing invite player cases have focused disabled evidence.
- `FindGroupSideEffectDispatchExecutorService` records opt-in direct/broadcast execution order, but not live boundary execution.
- `FindGroupDirectPacketTriggerOrderingReadinessService` blocks live direct-packet ordering claims.
- `FindGroupWorldBroadcastFanoutReadinessService` blocks live world-broadcast fanout claims for actions `1` and `5`.
- `FindGroupConcurrentMutationOrderingReadinessService` records deterministic shared-singleton interleaving fixtures but still blocks live singleton interleaving claims.
- `PHASE-6-PROGRESS.md` remained untouched.

## UOW-2153 Summary

This UOW added focused readiness evidence for Java `CM_FIND_GROUP` action `1`:

- Java action `1` reads `playerOrTeamId`, `serverId`, `unk1`, `unk2`, and `unk3`.
- Java ignores the parsed `playerOrTeamId` in `runImpl`; it removes by current team id or active player object id.
- Java broadcasts `SM_FIND_GROUP` action `1` only when a recruitment existed.
- Java filters that broadcast to players with the removed recruitment's race.
- Java missing recruitment removal returns null and sends no packet.
- C# disabled boundary now surfaces `FindGroupRecruitmentPlanStatus` in the intent plan.
- Focused tests prove action `1` removed status plus same-race fanout/opposite-race exclusion, and action `1` missing status plus no side effects.
- Readiness-report and design documentation now include action `1` missing-branch no-send evidence while keeping live world-broadcast fanout blocked.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment`
- `com.aionemu.gameserver.utils.PacketSendUtility.broadcastToWorld`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.FindGroupRecruitmentPlanService`
- `Aion.GameServer.Services.FindGroupConnectionBoundarySideEffectCompositionEvidenceService`
- `Aion.GameServer.Services.FindGroupWorldBroadcastFanoutReadinessService`
- `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService`
- `Aion.GameServer.Tests.GameServerConnectionFindGroupBoundaryTests`
- `Aion.GameServer.Tests.FindGroupWorldBroadcastFanoutReadinessServiceTests`

## Validation In UOW-2153

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupWorldBroadcastFanoutReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~SmFindGroupTests" --no-restore
```

Result:

- Passed: 115
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

Full .NET validation was skipped intentionally because this was a focused boundary-status/test/readiness-report unit with no live dispatch, shared packet primitive, common runtime base, persistence, solution-wide contract, shared infrastructure, or broad behavior change.

Java/Maven validation was not run because no Java source changed and no narrow executable Java test target was identified for this non-live C# boundary-trace fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet | Partial | Unit Tested | Partial Parity | Action `1` disabled boundary can parse/compose removed and missing recruitment branches. Live `ProcessPacketAsync` dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveRecruitment` | Service Method | Partial | Unit Tested | Partial Parity | Removed recruitment composes race-filtered world broadcast; missing recruitment records missing status and no side effects. Live socket ordering remains unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastToWorld` | `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService.ExecuteAsync` | Dispatch Utility | Partial | Unit Tested | Partial Parity | Opt-in executor records same-race action `1` broadcast and opposite-race exclusion; missing branch has no broadcast intent. This does not prove live `GameServerConnection` behavior. |
| `com.aionemu.gameserver.network.aion.AionClientPacket.run` | `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync` | Connection Boundary | Blocked | Unit Tested | Partial Parity | Java synchronously invokes `runImpl`; C# still defers `CmFindGroup` in `ProcessPacketAsync`, so live world-broadcast fanout remains blocked. |

## Known Gaps

- Live `CM_FIND_GROUP` execution is still disabled.
- No live `ProcessPacketAsync` trace proves actions `1` or `5` world-broadcast fanout relative to a triggering live `CM_FIND_GROUP` client packet.
- Action `5` removed-application branch has disabled fanout evidence, but the missing-application branch still needs surfaced status/no-send boundary evidence.
- Direct packet and world-broadcast action evidence remains non-live until `ProcessPacketAsync` is wired.
- No encrypted socket or real-client comparison covers FindGroup.

## Next Recommended Unit of Work

Next sequential task:

- Add action `5` missing-application world-broadcast boundary evidence, proving Java's `applications.remove(player.getObjectId())` no-broadcast branch and surfacing application mutation status at the disabled connection-boundary intent plan without enabling live dispatch.

Safe candidates:

- Add action `12` live invite dispatch failure/result handling evidence.
- Add focused runtime trace scaffolding for FindGroup caller interleavings if a narrow non-invasive hook exists.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2153

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundarySideEffectCompositionEvidenceService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupWorldBroadcastFanoutReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupWorldBroadcastFanoutReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2153-Completion.md`
- `docs/Phase-6-Session-2153-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2153] Add find group action one missing fanout trace
```
