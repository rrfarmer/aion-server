# Phase 6 Session 2092 Completion - Find Group Parsed Broadcast Evidence

Date: 2026-06-01
Unit of Work: UOW-2092
Status: Completed

## Scope

- Inspected Java `CM_FIND_GROUP` action 1 and `FindGroupService.removeRecruitment`.
- Added controlled C# evidence that a parsed action 1 `CmFindGroup` payload can flow through disabled boundary composition, produce a race-filtered world-broadcast intent, and execute that intent through the opt-in executor.
- Preserved live `CM_FIND_GROUP` deferral; `GameServerConnection` was not wired to the executor.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Action 1 reads `playerOrTeamId`, `serverId`, `unk1`, `unk2`, and `unk3`.
  - `runImpl` dispatches action 1 to `FindGroupService.getInstance().removeRecruitment(player, serverId, unk1, unk2, unk3)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `removeRecruitment(Player, ...)` resolves the current team id, falling back to player object id.
  - Existing recruitment removal broadcasts `new SM_FIND_GROUP(playerOrTeamId, serverId, unk1, unk2, unk3)` with `p -> p.getRace() == recruitment.getRace()`.
  - Missing recruitment removal does not broadcast.

## What Changed

- Added `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionOneWorldBroadcastWithRaceFilter`.
  - Builds a real parsed action 1 `CmFindGroup` payload.
  - Seeds a recruitment entry through `FindGroupRecruitmentPlanService`.
  - Composes the disabled connection boundary plan.
  - Confirms the side-effect evidence extracts a world-broadcast intent, not a direct packet intent.
  - Executes the intent through the opt-in `FindGroupSideEffectDispatchExecutorService` and verifies only same-race players receive the broadcast.
- Updated readiness/aggregate evidence text to mention parsed action 1 world-broadcast composition while keeping live dispatch blocked.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests" --no-restore`
  - Result: passed, 61 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: this UOW added C# boundary evidence around reviewed Java source behavior. It did not change Java source, Java packet parsing, or a Java-executable test target.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: narrow test/readiness evidence change; no live handler wiring, packet primitive, serialization helper, crypto, scheduling, world-state infrastructure, persistence, or shared connection dispatch branch changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`; `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Client Packet / Boundary Evidence | Partial | Unit Tested | Partial Parity | Real parsed C# action 1 and action 15 `CmFindGroup` payloads can feed disabled planner composition and explicit opt-in executor evidence. Live `GameServerConnection` dispatch remains intentionally deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveRecruitment`; `FindGroupSideEffectDispatchExecutorService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Planner / Opt-In World Broadcast Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 1 can produce the Java-shaped race-filtered world broadcast and that the opt-in executor applies the recorded race predicate. It does not prove live socket order or real-client parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionOneWorldBroadcastWithRaceFilter` | Unit | Java `CM_FIND_GROUP` action 1 and `FindGroupService.removeRecruitment` source review | Parsed action 1 payload composes disabled planner output, extracts a world-broadcast intent, and sends only same-race world recipients through the opt-in executor | Focused C# unit test using real packet parsing and reviewed Java source | Does not wire `GameServerConnection`; no encrypted socket/runtime comparison |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor is opt-in only and is not invoked by the packet boundary.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and concurrency remain unverified.
- Broad .NET validation was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-Session-2092-Completion.md`
- `docs/Phase-6-Session-2092-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: review whether parsed action 5 application removal should receive the same controlled world-broadcast boundary evidence, or whether existing generic broadcast coverage is enough.

Safe alternative candidates:

- Add narrow Java-side fixture/golden evidence for a `FindGroupService` packet branch if a matching Java test target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Add controlled evidence for another direct-packet branch not yet represented by parsed boundary tests.
