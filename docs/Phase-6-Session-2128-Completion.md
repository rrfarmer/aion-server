# Phase 6 Session 2128 Completion - FindGroup Parsed-Only No-Run Boundary Evidence

Date: 2026-06-02
Unit of Work: UOW-2128
Status: Completed

## Scope

- Added focused disabled-boundary side-effect evidence for parsed-only `CM_FIND_GROUP` actions `20` and `25`.
- Preserved Java's behavior where `readImpl` parses actions `20` and `25`, but `runImpl` has no branch for either action.
- Confirmed action `25` payload parsing can flow through the disabled boundary without direct packets, world broadcasts, executor order entries, or registry side effects.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Java `readImpl` action `20` reads only the action byte.
  - Java `readImpl` action `25` reads `playerOrTeamId`, `instanceMaskId`, and `bannedPlayerId`.
  - Java `runImpl` has no branch for action `20` or action `25`.

## What Changed

- Converted the disabled-boundary parsed-only no-run evidence test to cover both action `20` and action `25`.
- Added action `25` payload bytes to the boundary evidence path while asserting no side effects are produced.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record action `20`/`25` parsed-only no-run disabled-boundary evidence.

## Validation

- Changed surface:
  - Test-only disabled-boundary evidence plus documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests" --no-restore`
  - Final result: passed, 45 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `CM_FIND_GROUP.readImpl` and `runImpl` for actions `20`/`25`; no focused Java test target was identified for this disabled C# boundary no-side-effect evidence.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped boundary evidence.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.readImpl` actions `20`/`25`; `runImpl` omitted branches | `Aion.GameServer.Services.FindGroupClientActionPlanService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService.ExecuteOptInAsync` | Client Action Boundary | Partial | Unit Tested | Partial Parity | Focused evidence preserves Java's parsed-only no-run behavior for actions `20` and `25` at the disabled boundary. Action `25` payload is parsed and still produces no direct packets, world broadcasts, executor order entries, or registry side effects. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_LeavesParsedButNoRunImplActionWithoutSideEffects` | Unit | Java `CM_FIND_GROUP.readImpl`; Java `CM_FIND_GROUP.runImpl` branch absence | Disabled boundary action `20` and action `25` compose as `ParsedButNoRunImpl` and emit no packet, broadcast, executor-order, or registry side effects | Focused C# unit test plus reviewed Java source | Does not prove live `ProcessPacketAsync` execution, real socket behavior, or Java runtime trace |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 1 class, 2 parsed action branches, 2 omitted run branches.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 1 table row.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Actions `20` and `25` boundary no-run behavior is now covered through disabled evidence, but live socket behavior remains unverified.
- Java runtime/socket trace was not produced in this UOW.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2128-Completion.md`
- `docs/Phase-6-Session-2128-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add packet-byte evidence for action `12` declined `SM_MESSAGE`, or add live-readiness failure-result tests for missing world recipients or skipped invite recipients before any `ProcessPacketAsync` wiring.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Review multi-step mutation ordering under concurrent singleton callers before live dispatch.
- Add a narrow adapter-result failure evidence slice for missing direct recipients or skipped invite recipients.
