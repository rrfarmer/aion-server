# Phase 6 Session 2084 Completion - Find Group Side-Effect Dispatch Audit Contract

Date: 2026-06-01
Unit of Work: UOW-2084
Status: Completed

## Scope

- Added audit-only coverage for find-group direct packet and world-broadcast side-effect intents.
- Preserved live `CM_FIND_GROUP` dispatch deferral and avoided any connection-registry send/broadcast calls.
- Updated readiness reporting to record the new audit evidence.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - Direct packet boundaries use `PacketSendUtility.sendPacket(...)`.
  - World broadcast boundaries use `PacketSendUtility.broadcastToWorld(..., p -> p.getRace() == ...)`.
  - Reviewed send/broadcast call sites for recruitment/application lists, add/remove notifications, instance-group windows, member info, instance application whisper, and localized decline whisper.

## What Changed

- Added `FindGroupSideEffectDispatchAuditService`.
  - Audits `FindGroupDirectPacketIntent` records by recipient object id, packet type, and Java source.
  - Audits `FindGroupWorldBroadcastIntent` records by race, packet type, Java source, and the Java race-filter shape.
  - Always returns `DispatchLiveSideEffects = false`.
- Added focused tests proving:
  - direct packet intents are recorded without live dispatch;
  - world broadcast intents preserve the race-filter evidence without live dispatch;
  - null broadcast intents are ignored safely.
- Updated `FindGroupLiveDispatchReadinessReportService` observer evidence to include the side-effect dispatch audit contract.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupSideEffectDispatchAuditServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests" --no-restore`
  - Result: passed, 43 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: this UOW added C# audit-only reporting around reviewed Java send/broadcast call sites and did not alter Java packet parsing or introduce a Java-executable parity target.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: audit-only service and readiness evidence update; no live dispatch, connection registry send, packet primitive, persistence, crypto, scheduling, world-state, or connection-dispatch behavior changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService` direct `PacketSendUtility.sendPacket` boundaries | `Aion.GameServer.Services.FindGroupSideEffectDispatchAuditService`; `FindGroupDirectPacketDispatchAudit` | Side-Effect Boundary / Audit Service | Partial | Unit Tested | Partial Parity | C# can audit planned direct packet intents by recipient and packet type without sending. Live send execution remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` `PacketSendUtility.broadcastToWorld` race-filter boundaries | `Aion.GameServer.Services.FindGroupSideEffectDispatchAuditService`; `FindGroupWorldBroadcastDispatchAudit` | Side-Effect Boundary / Audit Service | Partial | Unit Tested | Partial Parity | C# can audit planned world-broadcast intents by race and packet type without broadcasting. Live race-filter fanout remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` side-effect readiness | `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReportService` | Readiness Report / Service | Partial | Unit Tested | Partial Parity | Readiness report records side-effect audit evidence while preserving live-dispatch blockers. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupSideEffectDispatchAuditServiceTests.CreateAuditPlan_RecordsDirectPacketIntentWithoutLiveDispatch` | Unit | Java `FindGroupService` `PacketSendUtility.sendPacket` source review | Direct packet intent metadata is auditable without live send | Focused C# unit test | Does not execute socket send or Java runtime |
| `FindGroupSideEffectDispatchAuditServiceTests.CreateAuditPlan_RecordsWorldBroadcastRaceFilterWithoutLiveDispatch` | Unit | Java `FindGroupService` `PacketSendUtility.broadcastToWorld` source review | World broadcast intent preserves race-filter evidence without live broadcast | Focused C# unit test | Does not execute world fanout or Java runtime |
| `FindGroupSideEffectDispatchAuditServiceTests.CreateAuditPlan_IgnoresNullBroadcastIntentAndStaysNonLive` | Unit | C# defensive audit behavior | Null optional broadcast intent is skipped safely | Focused C# unit test | Java has no corresponding null audit helper |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 1.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect audit service does not send packets or broadcast to the world.
- Direct packet sends, world race-filter fanout, encrypted socket behavior, real-client behavior, visibility filtering, and concurrency remain unverified.
- A future opt-in executor must still prove connection-registry send/broadcast behavior before live dispatch can be considered.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupSideEffectDispatchAuditService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupSideEffectDispatchAuditServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-Session-2084-Completion.md`
- `docs/Phase-6-Session-2084-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect `GameServerConnection`'s deferred `CmFindGroup` branch and create a final disabled boundary aggregation report that lists planner, invite executor, side-effect audit, lifecycle observer, and remaining live blockers in one place.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for another `FindGroupService` packet branch where a narrow Java target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Design opt-in connection-registry direct-send/world-broadcast executor tests without wiring them into `CM_FIND_GROUP`.
