# Phase 6 Session 2499 Completion

## UOW

[Phase 6] UOW-2499: Compose Vortex start defender invitation batch metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAllianceUpdatePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionRuntimeSnapshotCollectorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexDefenderInvitationBatchPlanService` to compose one non-live `VortexDefenderUpdateDefendersPlan` per prepared defender update candidate.
- The batch plan preserves Java `Invasion.updateDefenders` invitation gates: existing defender skip, full-alliance skip, request-slot failure, and question-window intent only after request storage succeeds.
- `VortexStartInvasionSnapshotRequest`, `VortexStartInvasionRuntimeSnapshotCollectorService`, and `VortexStartInvasionSideEffectPlan` now carry defender invitation batch metadata.
- Scope remains metadata-only. It does not store live response requests, send `SM_QUESTION_WINDOW`, execute acceptance callbacks, mutate groups, mutate alliances, mutate defender maps, despawn NPCs, spawn NPCs, start portals, or schedule stops.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DefenderInvitationBatchPlan_ComposesOneInvitationPlanPerDefenderUpdateCandidate` | Unit | `Invasion.updateAlliance` and `Invasion.updateDefenders` source review | Each prepared defender update candidate gets one invitation plan and preserves already-defender, request-slot, and question-window branches | Focused C# test validates Java branch metadata and disabled live request/packet/alliance/group/defender mutation flags | Does not install Java `RequestResponseHandler` callbacks |
| `DefenderInvitationBatchPlan_FullAllianceSkipsAllInvitationRequestsLikeJavaFirstGate` | Unit | `Invasion.updateDefenders` source review | Full defender alliance blocks invitation request storage for all defender candidates | Focused C# test validates full-alliance branch counts and disabled live request/packet flags | Does not compare against Java runtime output |
| `StartSnapshotCollector_PreparesRuntimeStaticRequestWithDefenderAllianceMetadata` | Unit | `Invasion.startInvasion`, `Invasion.updateAlliance`, and `Invasion.updateDefenders` source review | Runtime/static start request now includes defender invitation batch metadata | Focused C# test validates batch counts for planned, request-not-stored, and already-defender branches | Supplied runtime candidates still come from test fixtures |
| `StartCoordinator_PreparedRuntimeStaticRequestCarriesDefenderAllianceUpdateMetadata` | Unit | `Invasion.startInvasion` source review | Prepared start request batch metadata reaches the coordinator side-effect plan | Focused C# test validates preserved batch reference and invitation/question-window counts | Live start side effects remain disabled |

## Validation Decision

- Changed surface: non-live Vortex defender invitation batch planner, start request metadata, side-effect report metadata, and focused tests.
- Specific behavior/contract: C# Vortex start defender-alliance metadata composes Java `updateDefenders` invitation intent for each exact defender-race zone player while keeping live request, packet, acceptance, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 100 tests. Existing nullable/analyzer warnings were emitted from unrelated files.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex defender-invitation fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds non-live metadata/adapters and tests, without enabling live request storage, packet dispatch, acceptance callbacks, alliance mutation, group mutation, defender mutation, portal spawn, NPC despawn, NPC spawn, or scheduler dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited batch/request/coordinator path.
- Why this scope is sufficient: the new code only composes and carries inert Java-shaped invitation metadata for the existing start defender-update plan.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported expected CRLF conversion warnings for edited C# source and test files.
- `git diff --cached --check` passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance` | `Aion.GameServer.Services.VortexDefenderInvitationBatchPlanService` | Runtime metadata service | Partial | Unit Tested | Partial Parity | C# composes one invitation metadata plan per defender-race candidate from the prepared update-alliance scan. Live request and packet side effects remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderUpdateDefendersPlan` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# batch composition preserves existing-defender skip, full-alliance skip, request-slot failure, and question-window intent branches. Acceptance callback remains metadata-only. |
| `com.aionemu.gameserver.services.vortex.Invasion.startInvasion` | `Aion.GameServer.Services.VortexStartInvasionSideEffectPlan` | Runtime coordinator metadata | Partial | Unit Tested | Partial Parity | C# start side-effect plan carries defender invitation batch counts in Java start ordering. Live despawn, spawn, portal, request, alliance, group, defender-map, and scheduler side effects remain disabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live defender request storage and `SM_QUESTION_WINDOW` dispatch remain disabled.
- Java `RequestResponseHandler.acceptRequest` acceptance flow remains represented only by existing metadata plans.
- Prepared start requests still depend on supplied runtime candidates rather than production world/location/alliance/requester containers.
- Full production XML coverage for Vortex static INVASION spawns has not been compared against Java output.
