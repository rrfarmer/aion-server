# Phase 6 Session 2121 Completion - FindGroup Action 12 Boundary Status Evidence

Date: 2026-06-02
Unit of Work: UOW-2121
Status: Completed

## Scope

- Exposed action `12` instance-application branch status on the disabled `CM_FIND_GROUP` boundary intent plan.
- Added focused connection-helper and adapter assertions for accepted group invite, accepted alliance invite, declined whisper, missing applicant, and missing responder instance-group outcomes.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Java `runImpl` action `12` calls `FindGroupService.sendInstanceApplicationResult(player, playerOrTeamId, instanceApplicationReply)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - Missing applicant: no side effects.
  - Accepted reply with missing responder instance group: no side effects.
  - Accepted reply with `minMembers <= 6`: group invite.
  - Accepted reply with `minMembers > 6`: alliance invite.
  - Declined reply: sends a whisper `SM_MESSAGE` to the applicant.

## What Changed

- Added `InstanceApplicationStatus` to `FindGroupConnectionBoundarySideEffectIntentPlan`.
- Preserved the status from `FindGroupClientActionPlanService` through the disabled composition and adapter result surface.
- Added focused tests that assert the status for action `12` accepted group invite, accepted alliance invite, declined, missing applicant, and missing instance-group branches.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record the new boundary status evidence.

## Validation

- Changed surface:
  - Production evidence/result surface plus focused tests and documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - Final result: passed, 71 tests.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `CM_FIND_GROUP.runImpl` action `12` and `FindGroupService.sendInstanceApplicationResult`; no focused Java test target was identified for this disabled C# boundary status surface.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped boundary evidence.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` action `12` | `Aion.GameServer.Services.FindGroupConnectionBoundarySideEffectIntentPlan.InstanceApplicationStatus`; `FindGroupConnectionBoundaryDispatchAdapterService` | Client Action Boundary | Partial | Unit Tested | Partial Parity | Focused evidence preserves action `12` branch status for accepted group invite, accepted alliance invite, declined, missing applicant, and missing instance group. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; boundary intent-plan status surface | Service Method | Partial | Unit Tested | Partial Parity | Existing planner behavior is now visible at the connection boundary. Live socket behavior, Java runtime trace, and packet-byte comparison remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFindGroupBoundaryTests` action `12` status assertions | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.sendInstanceApplicationResult` | Disabled connection helper preserves accepted group, accepted alliance, declined, missing applicant, and missing instance-group statuses | Focused C# unit tests plus reviewed Java source | Does not prove live dispatch, real socket behavior, Java runtime trace, or packet-byte parity |
| `FindGroupConnectionBoundaryDispatchAdapterServiceTests` accepted group status assertions | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.sendInstanceApplicationResult` | Adapter result keeps accepted group status when invite runtime is present or absent | Focused C# unit tests plus reviewed Java source | Does not prove declined packet bytes, alliance runtime execution, or live boundary behavior |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 3 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `12` branch status is visible through the disabled boundary plan, but live boundary execution, real socket behavior, Java runtime traces, and packet-byte parity remain unverified.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundarySideEffectCompositionEvidenceService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryDispatchAdapterServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2121-Completion.md`
- `docs/Phase-6-Session-2121-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add packet-byte evidence for action `12` declined `SM_MESSAGE`, or add focused connection-registry ordering evidence for `FindGroupSideEffectDispatchExecutorService` under a disabled boundary plan.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Add live-readiness failure-result tests for missing world recipients or skipped invite recipients before any `ProcessPacketAsync` wiring.
- Review multi-step mutation ordering under concurrent singleton callers before live dispatch.
