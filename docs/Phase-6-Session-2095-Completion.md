# Phase 6 Session 2095 Completion - Find Group Instance Show Direct Evidence

Date: 2026-06-01
Unit of Work: UOW-2095
Status: Completed

## Scope

- Inspected Java `CM_FIND_GROUP` actions 10 and 13 plus `FindGroupService.showInstanceGroups`.
- Added controlled C# evidence extraction for instance-group show-list packets so parsed action 10 and action 13 `CmFindGroup` payloads can compose direct packet intents for the active player.
- Preserved live `CM_FIND_GROUP` deferral; `GameServerConnection` was not wired to the executor.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Action 10 dispatches to `FindGroupService.getInstance().showInstanceGroups(player, false)`.
  - Action 13 dispatches to `FindGroupService.getInstance().showInstanceGroups(player, true)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `showInstanceGroups(Player, boolean)` filters stored instance groups by player race.
  - When `isUpdate == false` and `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` is enabled, Java sends `new SM_FIND_GROUP(instanceMaskIds)` before the action 10 list packet.
  - Java always sends `new SM_FIND_GROUP(10, instanceGroups)` after the optional enable-register packet.

## What Changed

- Updated `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`.
  - Converts instance-group show-list plans into direct packet intents for the active player.
  - Keeps the existing optional action 26 enable-register intent order before the action 10 show-list intent.
  - Continues to report `IsCmFindGroupBoundaryWired = false`.
- Added parsed-packet tests:
  - Action 10 composes the optional action 26 enable-register packet followed by the action 10 instance-group show-list packet when form-anywhere is enabled.
  - Action 13 composes only the action 10 instance-group update packet, even when form-anywhere facts are supplied.
- Updated readiness/aggregate evidence text to include action 10/13 instance-group show direct packet evidence.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~SmFindGroupTests" --no-restore`
  - Result: passed, 86 tests.
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
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`; `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Client Packet / Boundary Evidence | Partial | Unit Tested | Partial Parity | Real parsed C# action 0, action 1, action 4, action 5, action 10, action 13, and action 15 `CmFindGroup` payloads can feed disabled planner composition and explicit opt-in executor evidence. Live `GameServerConnection` dispatch remains intentionally deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroups` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowInstanceGroupsForClient`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupSideEffectDispatchExecutorService` | Planner / Opt-In Direct Packet Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves parsed action 10 can produce Java's optional action 26 enable-register direct packet followed by the action 10 show-list packet, and parsed action 13 can produce only the action 10 update packet. It does not prove live socket order or real-client parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionTenInstanceGroupShowWithEnableRegisterPacket` | Unit | Java `CM_FIND_GROUP` action 10 and `FindGroupService.showInstanceGroups(player, false)` source review | Parsed action 10 payload composes optional action 26 direct packet followed by action 10 show-list direct packet through the opt-in executor | Focused C# unit test using real packet parsing and reviewed Java source | Does not wire `GameServerConnection`; no encrypted socket/runtime comparison |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionThirteenInstanceGroupUpdateWithoutEnableRegisterPacket` | Unit | Java `CM_FIND_GROUP` action 13 and `FindGroupService.showInstanceGroups(player, true)` source review | Parsed action 13 payload composes only the action 10 update direct packet through the opt-in executor | Focused C# unit test using real packet parsing and reviewed Java source | Does not wire `GameServerConnection`; no encrypted socket/runtime comparison |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 5 C# surfaces.
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

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundarySideEffectCompositionEvidenceService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-Session-2095-Completion.md`
- `docs/Phase-6-Session-2095-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: review parsed action 8/9/17 instance-group mutation branches for direct packet/show-list boundary evidence.

Safe alternative candidates:

- Add narrow Java-side fixture/golden evidence for a `FindGroupService` packet branch if a matching Java test target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Add controlled evidence for action 11/12 parsed boundary paths now that direct/invite executors already exist.
