# Phase 6 Session 2096 Completion - Find Group Instance Mutation Evidence

Date: 2026-06-01
Unit of Work: UOW-2096
Status: Completed

## Scope

- Inspected Java `CM_FIND_GROUP` actions 8, 9, and 17 plus the corresponding `FindGroupService` instance-group mutation methods.
- Added controlled C# evidence that parsed action 8/9/17 `CmFindGroup` payloads can flow through disabled boundary composition and produce the expected direct packet intents.
- Preserved live `CM_FIND_GROUP` deferral; `GameServerConnection` was not wired to the executor.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Action 8 reads `instanceMaskId`, an unused byte, `message`, and `minMembers`, then dispatches to `registerInstanceGroup(player, instanceMaskId, message, minMembers)`.
  - Action 9 reads `playerOrTeamId` and `instanceMaskId`, but `runImpl` dispatches to `removeInstanceGroup(player)` and does not pass those parsed ids.
  - Action 17 reads `playerOrTeamId`, `instanceMaskId`, and `message`, but `runImpl` dispatches to `updateInstanceGroup(player, message)` and does not pass the parsed ids.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `registerInstanceGroup` stores by active player object id and sends `new SM_FIND_GROUP(14, List.of(instanceGroup))` directly to the player.
  - `removeInstanceGroup` removes by active player object id and then calls `showInstanceGroups(player, true)`.
  - `updateInstanceGroup` updates the active player's existing instance group and then calls `showInstanceGroups(player, true)`.

## What Changed

- Added parsed-packet tests:
  - Action 8 register instance group composes and executes the Java action 14 direct packet intent.
  - Action 9 remove instance group composes and executes the updated action 10 show-list direct packet intent using active-player state, matching Java dispatch.
  - Action 17 update instance group composes and executes the updated action 10 show-list direct packet intent using active-player state, matching Java dispatch.
- Updated readiness/aggregate evidence text to include action 8/9/17 instance-group mutation evidence.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~SmFindGroupTests" --no-restore`
  - Result: passed, 89 tests.
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
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`; `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Client Packet / Boundary Evidence | Partial | Unit Tested | Partial Parity | Real parsed C# action 0, action 1, action 4, action 5, action 8, action 9, action 10, action 13, action 15, and action 17 `CmFindGroup` payloads can feed disabled planner composition and explicit opt-in executor evidence. Live `GameServerConnection` dispatch remains intentionally deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.registerInstanceGroup` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RegisterInstanceGroup`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupSideEffectDispatchExecutorService` | Planner / Opt-In Direct Packet Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 8 can produce the Java-shaped action 14 direct packet intent and execute it through the registry when explicitly invoked. It does not prove live socket order or real-client parity. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeInstanceGroup` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveInstanceGroup`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupSideEffectDispatchExecutorService` | Planner / Opt-In Direct Packet Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 9 can remove the active player's instance group and produce the Java-shaped action 10 update packet. Java-parsed ids are intentionally not used by runImpl. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.updateInstanceGroup` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.UpdateInstanceGroup`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupSideEffectDispatchExecutorService` | Planner / Opt-In Direct Packet Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 17 can update the active player's instance group message and produce the Java-shaped action 10 update packet. Java-parsed ids are intentionally not used by runImpl. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionEightRegisterInstanceGroupAsDirectPacket` | Unit | Java `CM_FIND_GROUP` action 8 and `FindGroupService.registerInstanceGroup` source review | Parsed action 8 payload composes action 14 direct packet intent through the opt-in executor | Focused C# unit test using real packet parsing and reviewed Java source | Does not wire `GameServerConnection`; no encrypted socket/runtime comparison |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionNineRemoveInstanceGroupAsUpdatedShowList` | Unit | Java `CM_FIND_GROUP` action 9 and `FindGroupService.removeInstanceGroup` source review | Parsed action 9 payload removes the active player's instance group and composes the action 10 updated show-list packet | Focused C# unit test using real packet parsing and reviewed Java source | Does not wire `GameServerConnection`; no encrypted socket/runtime comparison |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionSeventeenUpdateInstanceGroupAsUpdatedShowList` | Unit | Java `CM_FIND_GROUP` action 17 and `FindGroupService.updateInstanceGroup` source review | Parsed action 17 payload updates the active player's instance group message and composes the action 10 updated show-list packet | Focused C# unit test using real packet parsing and reviewed Java source | Does not wire `GameServerConnection`; no encrypted socket/runtime comparison |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 5 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 4 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor is opt-in only and is not invoked by the packet boundary.
- Parsed action 11/12 instance application boundary evidence remains connection-adjacent and should be reviewed in a future unit.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and concurrency remain unverified.
- Broad .NET validation was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-Session-2096-Completion.md`
- `docs/Phase-6-Session-2096-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add controlled parsed-boundary evidence for actions 11 and 12 using the existing direct and invite executor services.

Safe alternative candidates:

- Add narrow Java-side fixture/golden evidence for a `FindGroupService` packet branch if a matching Java test target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Review whether parsed actions 2/3/6/7 add/update recruitment/application branches need boundary evidence beyond existing planner tests.
