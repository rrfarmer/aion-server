# Phase 6 Session 2490 Completion

## UOW

[Phase 6] UOW-2490: Compose Vortex defender updateDefenders acceptance/addPlayer metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderUpdateDefendersPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added non-live `VortexDefenderUpdateDefendersPlanService`.
- The new planner composes the existing defender-side metadata services around Java `Invasion.updateDefenders`:
  - invitation/request and `SM_QUESTION_WINDOW(904306, 0, 0)` intent through `VortexDefenderInvitationPlanService`;
  - `RequestResponseHandler.acceptRequest` team-removal intent through `VortexDefenderInvitationAcceptancePlanService`;
  - accepted defender `addPlayer(responder, false)` transition intent through `VortexDefenderAddPlayerTransitionPlanService`.
- Preserved Java's second full-alliance gate in `acceptRequest`: accepted responders still record group/alliance removal intent, but do not route to `addPlayer(false)` when the defender alliance is full.
- Preserved Java's `addPlayer(false)` warning branch when more than one defender exists and no active alliance can receive the responder.
- Scope remains metadata-only. It does not mutate live requests, send packets, remove players from groups or alliances, create/add to alliances, or put defenders into the live participant map.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DefenderUpdateDefendersPlan_ComposesInvitationRequestAndQuestionWindowIntent` | Unit | `Invasion.updateDefenders` source review | Invitation stage records request storage and question-window intent | Focused C# test validates request id, question-window intent, existing defender ids, and disabled live request/packet mutation | Does not install live request handler or send packet |
| `DefenderUpdateDefendersPlan_ComposesAcceptanceRemovalAndAddPlayerTransition` | Unit | `Invasion.updateDefenders.acceptRequest` and `Invasion.addPlayer(player, false)` source review | Acceptance stage composes group-removal intent and defender alliance creation/add-player transition | Focused C# test validates removal choice, addPlayer routing, alliance-defence creation metadata, participant put intent, and disabled live mutation | Does not mutate live group/alliance/defender maps |
| `DefenderUpdateDefendersPlan_FullAllianceBlocksAcceptanceAddPlayerLikeJavaSecondGate` | Unit | `Invasion.updateDefenders.acceptRequest` source review | Full defender alliance blocks addPlayer after responder team-removal checks | Focused C# test validates acceptance full-alliance status, removal intent, no add-player subplan, and disabled live mutation | Does not exercise live alliance fullness changes during request lifetime |
| `DefenderUpdateDefendersPlan_ComposesAddPlayerWarningWhenAllianceMissingWithManyDefenders` | Unit | `Invasion.addPlayer(player, false)` source review | Accepted responder still routes to addPlayer, but addPlayer warns and skips participant put when no alliance exists with multiple defenders | Focused C# test validates warning status, no participant put intent, and disabled live mutation | Does not emit live logger warning |

## Validation Decision

- Changed surface: non-live Vortex defender updateDefenders composition metadata and focused tests.
- Specific behavior/contract: C# metadata mirrors Java `Invasion.updateDefenders`, its `RequestResponseHandler.acceptRequest`, and `Invasion.addPlayer(player, false)` routing for invitation request/question-window intent, responder team-removal intent, second full-alliance gate, accepted defender add-player transition, and no live mutation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 82 tests. Existing nullable/analyzer warnings were emitted from unrelated files.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex defender-update fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds non-live composition metadata and tests, without enabling live request storage, packet dispatch, group/alliance mutation, or participant mutation.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited Vortex defender update composition paths.
- Why this scope is sufficient: the new code is an inert composition planner over supplied defender, responder, existing defender, alliance, and request-slot snapshots.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported an expected CRLF conversion warning for the edited test file.
- `git diff --cached --check` passed after staging. Git reported expected CRLF conversion warnings for edited/new files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderUpdateDefendersPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# composes invitation/request and question-window intent as metadata. Live request storage and packet dispatch remain disabled. |
| `com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler.acceptRequest` | `Aion.GameServer.Services.VortexDefenderUpdateDefendersPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# composes responder group/alliance removal and second full-alliance gate as metadata. Live team mutation remains disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.addPlayer(Player, boolean=false)` | `Aion.GameServer.Services.VortexDefenderUpdateDefendersPlanService` / `VortexDefenderAddPlayerTransitionPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# composes defender add-player transition metadata, including alliance-defence creation and missing-alliance warning branches. Live defender/alliance mutation remains disabled. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Services.VortexDefenderAllianceSnapshot` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records only existence/full/disbanded snapshots needed by defender update branches. Full alliance runtime behavior remains elsewhere. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 1
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Defender request storage and `SM_QUESTION_WINDOW` packet dispatch remain disabled.
- `RequestResponseHandler` runtime wiring remains absent.
- Live group/alliance removal and defender alliance create/add behavior remains disabled.
- Live defender participant map mutation remains disabled.
- Java concurrent map and runtime request timing are represented only by supplied snapshots.
- Production Vortex lifecycle adapters still need real player, alliance, request, and location inputs before live use.
