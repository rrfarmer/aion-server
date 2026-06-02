# Phase 6 Session 2098 Completion - Find Group Recruitment/Application Mutation Boundary Evidence

Date: 2026-06-01
Unit of Work: UOW-2098
Status: Completed

## Scope

- Inspected Java `CM_FIND_GROUP` actions 2, 3, 6, and 7 plus the corresponding `FindGroupService` recruitment/application mutation methods.
- Added controlled C# evidence that parsed action 2/3/6/7 `CmFindGroup` payloads can flow through disabled boundary composition.
- Preserved live `CM_FIND_GROUP` deferral; `GameServerConnection` was not wired to the executor.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Action 2 reads `playerOrTeamId`, `message`, and `groupType`, then dispatches to `addRecruitment(player, message, groupType)`.
  - Action 3 reads id/server/unknown fields plus `message` and `groupType`, then dispatches to `updateRecruitment(player, message, groupType)`. Java does not pass the parsed id/server/unknown fields to the update method.
  - Actions 6 and 7 read `playerOrTeamId`, `message`, `groupType`, `classId`, and `level`, then dispatch to add/update application methods.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `addRecruitment` stores a solo/team recruitment, sends `STR_PARTY_MATCH_OFFER_PARTY_POSTED`, then calls `showRecruitments(player)`.
  - `updateRecruitment` mutates message, group type, and last-update timestamp when the active player's solo/current-team entry exists; it sends no packet.
  - `addApplication` stores by active player object id, sends `STR_PARTY_MATCH_SEEK_PARTY_POSTED`, then calls `showApplications(player)`.
  - `updateApplication` mutates message, group type, class id, level, and last-update timestamp when the active player's application exists; it sends no packet.

## What Changed

- Added parsed-packet boundary evidence tests:
  - Action 2 composes and opt-in executes the Java-shaped posted system message plus refreshed recruitment show-list direct packet.
  - Action 3 mutates the active player's recruitment from a parsed packet and produces no packet intents, matching Java's update branch.
  - Action 6 composes and opt-in executes the Java-shaped posted system message plus refreshed application show-list direct packet.
  - Action 7 mutates the active player's application from a parsed packet and produces no packet intents, matching Java's update branch.
- Updated readiness/aggregate evidence text to include action 2/3/6/7 recruitment/application mutation evidence.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~SmFindGroupTests" --no-restore`
  - Result: passed, 97 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: this UOW added C# boundary evidence around reviewed Java source behavior. It did not change Java source, Java packet parsing, or a Java-executable test target.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: controlled evidence tests/readiness text only; no live handler wiring, packet primitive, serialization helper, crypto, scheduling, world-state infrastructure, persistence, or shared connection dispatch branch changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`; `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Client Packet / Boundary Evidence | Partial | Unit Tested | Partial Parity | Real parsed C# action 0, action 1, action 2, action 3, action 4, action 5, action 6, action 7, action 8, action 9, action 10, action 11, action 12, action 13, action 15, and action 17 `CmFindGroup` payloads can feed disabled planner composition and explicit opt-in executor or invite-dispatch evidence. Live `GameServerConnection` dispatch remains intentionally deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.AddRecruitment`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupSideEffectDispatchExecutorService` | Planner / Opt-In Direct Packet Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 2 can add an active-player recruitment and produce the Java-shaped posted system message plus action 0 show-list direct packet. Team subject/runtime details remain planner-covered but not live-dispatch verified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.updateRecruitment` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.UpdateRecruitment`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Planner / Boundary Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 3 can update the active player's recruitment message/type and produce no direct or world packet intents, matching reviewed Java behavior. Live singleton state and concurrency remain unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.AddApplication`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupSideEffectDispatchExecutorService` | Planner / Opt-In Direct Packet Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 6 can add an active-player application and produce the Java-shaped posted system message plus action 4 show-list direct packet. Live socket order remains unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.updateApplication` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.UpdateApplication`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Planner / Boundary Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 7 can update the active player's application message/type/class/level and produce no direct or world packet intents, matching reviewed Java behavior. Live singleton state and concurrency remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionTwoAddRecruitmentAsPostedMessageAndShowList` | Unit | Java `CM_FIND_GROUP` action 2 and `FindGroupService.addRecruitment` source review | Parsed action 2 payload composes posted system message and refreshed recruitment list direct packets | Focused C# unit test using real packet parsing and reviewed Java source | Does not wire `GameServerConnection`; no encrypted socket/runtime comparison |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionThreeUpdateRecruitmentWithoutPackets` | Unit | Java `CM_FIND_GROUP` action 3 and `FindGroupService.updateRecruitment` source review | Parsed action 3 updates active-player recruitment and emits no packet intents | Focused C# unit test using real packet parsing and reviewed Java source | Does not prove live singleton/concurrent map behavior |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionSixAddApplicationAsPostedMessageAndShowList` | Unit | Java `CM_FIND_GROUP` action 6 and `FindGroupService.addApplication` source review | Parsed action 6 payload composes posted system message and refreshed application list direct packets | Focused C# unit test using real packet parsing and reviewed Java source | Does not wire `GameServerConnection`; no encrypted socket/runtime comparison |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionSevenUpdateApplicationWithoutPackets` | Unit | Java `CM_FIND_GROUP` action 7 and `FindGroupService.updateApplication` source review | Parsed action 7 updates active-player application and emits no packet intents | Focused C# unit test using real packet parsing and reviewed Java source | Does not prove live singleton/concurrent map behavior |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 5 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor and invite dispatcher are opt-in only and are not invoked by the packet boundary.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and concurrency remain unverified.
- Broad .NET validation was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-Session-2098-Completion.md`
- `docs/Phase-6-Session-2098-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: review whether all parsed `CM_FIND_GROUP.runImpl` branches now have enough controlled boundary evidence to support a narrow live-dispatch design document, without enabling live dispatch yet.

Safe alternative candidates:

- Add narrow Java-side fixture/golden evidence for a `FindGroupService` packet branch if a matching Java test target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Review action 20/25 parsed-only behavior for documentation clarity before any live dispatch work.
