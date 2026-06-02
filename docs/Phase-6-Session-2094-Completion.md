# Phase 6 Session 2094 Completion - Find Group Show List Direct Evidence

Date: 2026-06-01
Unit of Work: UOW-2094
Status: Completed

## Scope

- Inspected Java `FindGroupService.showRecruitments` and `FindGroupService.showApplications`.
- Added controlled C# evidence extraction for show-list packets so parsed action 0 and action 4 `CmFindGroup` payloads can compose direct packet intents for the active player.
- Preserved live `CM_FIND_GROUP` deferral; `GameServerConnection` was not wired to the executor.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Action 0 dispatches to `FindGroupService.getInstance().showRecruitments(player)`.
  - Action 4 dispatches to `FindGroupService.getInstance().showApplications(player)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `showRecruitments(Player)` filters stored recruitments by player race and sends `new SM_FIND_GROUP(0, recruitments)` directly to the player.
  - `showApplications(Player)` filters stored applications by player race and sends `new SM_FIND_GROUP(4, applications)` directly to the player.

## What Changed

- Updated `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`.
  - Converts `FindGroupRecruitmentShowPlan` into a direct packet intent for the active player.
  - Converts `FindGroupApplicationShowPlan` into a direct packet intent for the active player.
  - Also extracts nested show-list plans from add recruitment/application mutation plans.
  - Continues to report `IsCmFindGroupBoundaryWired = false`.
- Added parsed-packet tests:
  - Action 0 recruitment show list composes a direct `SmFindGroup` packet intent and executes through the opt-in registry executor.
  - Action 4 application show list composes a direct `SmFindGroup` packet intent and executes through the opt-in registry executor.
- Updated readiness/aggregate evidence text to include action 0/4 show-list direct packet evidence and action 1/5 world-broadcast evidence.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~SmFindGroupTests" --no-restore`
  - Result: passed, 84 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: this UOW added C# boundary evidence extraction around reviewed Java source behavior. It did not change Java source, Java packet parsing, or a Java-executable test target.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: controlled evidence extraction and tests only; no live handler wiring, packet primitive, serialization helper, crypto, scheduling, world-state infrastructure, persistence, or shared connection dispatch branch changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`; `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Client Packet / Boundary Evidence | Partial | Unit Tested | Partial Parity | Real parsed C# action 0, action 1, action 4, action 5, and action 15 `CmFindGroup` payloads can feed disabled planner composition and explicit opt-in executor evidence. Live `GameServerConnection` dispatch remains intentionally deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showRecruitments` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowRecruitments`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupSideEffectDispatchExecutorService` | Planner / Opt-In Direct Packet Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 0 can produce the Java-shaped direct recruitment show-list packet intent and execute it through the registry when explicitly invoked. It does not prove live socket order or real-client parity. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showApplications` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowApplications`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupSideEffectDispatchExecutorService` | Planner / Opt-In Direct Packet Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 4 can produce the Java-shaped direct application show-list packet intent and execute it through the registry when explicitly invoked. It does not prove live socket order or real-client parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionZeroRecruitmentShowAsDirectPacket` | Unit | Java `CM_FIND_GROUP` action 0 and `FindGroupService.showRecruitments` source review | Parsed action 0 payload composes disabled planner output, extracts a direct show-list packet intent, and sends it through the opt-in executor | Focused C# unit test using real packet parsing and reviewed Java source | Does not wire `GameServerConnection`; no encrypted socket/runtime comparison |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionFourApplicationShowAsDirectPacket` | Unit | Java `CM_FIND_GROUP` action 4 and `FindGroupService.showApplications` source review | Parsed action 4 payload composes disabled planner output, extracts a direct show-list packet intent, and sends it through the opt-in executor | Focused C# unit test using real packet parsing and reviewed Java source | Does not wire `GameServerConnection`; no encrypted socket/runtime comparison |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 5 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor is opt-in only and is not invoked by the packet boundary.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and concurrency remain unverified.
- Broad .NET validation was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundarySideEffectCompositionEvidenceService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-Session-2094-Completion.md`
- `docs/Phase-6-Session-2094-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: review parsed action 10/13 instance-group show-list direct packet evidence, including action 10's optional form-anywhere enable-register packet.

Safe alternative candidates:

- Add narrow Java-side fixture/golden evidence for a `FindGroupService` packet branch if a matching Java test target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Add controlled evidence for another direct-packet branch not yet represented by parsed boundary tests.
