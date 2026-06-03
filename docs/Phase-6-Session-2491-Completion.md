# Phase 6 Session 2491 Completion

## UOW

[Phase 6] UOW-2491: Compose Vortex invader updateInvaders/addPlayer metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexInvaderUpdateInvadersPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added non-live `VortexInvaderUpdateInvadersPlanService`.
- The new planner composes Java `Invasion.updateInvaders` with existing `Invasion.addPlayer(player, true)` metadata:
  - already-invader guard returns before add-player intent;
  - first invader records participant-put intent;
  - second invader records `ALLIANCE_OFFENCE` creation and team-removal metadata;
  - existing non-disbanded alliance records alliance-add metadata through the existing add-player planner;
  - missing/disbanded alliance with multiple participants records warning/no-participant-put metadata.
- Scope remains metadata-only. It does not mutate live invader participants, remove players from groups or alliances, create/add to alliances, or emit live logger warnings.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `InvaderUpdateInvadersPlan_SkipsAlreadyInvaderBeforeAddPlayerLikeJava` | Unit | `Invasion.updateInvaders` source review | Existing invader returns before add-player work | Focused C# test validates already-invader status, no add-player intent, no participant put, and disabled live mutation | Does not inspect live concurrent map |
| `InvaderUpdateInvadersPlan_ComposesRecordFirstInvaderAddPlayer` | Unit | `Invasion.updateInvaders` and `Invasion.addPlayer(player, true)` source review | First invader routes to addPlayer and participant-put metadata | Focused C# test validates record-first status, add-player intent, participant-put intent, and Java breadcrumb | Does not mutate live invader map |
| `InvaderUpdateInvadersPlan_ComposesOffenceAllianceCreationForSecondInvader` | Unit | `Invasion.addPlayer(player, true)` source review | Second invader records group/alliance removal and offence-alliance creation metadata | Focused C# test validates `ALLIANCE_OFFENCE`, removal plan ordering, participant-put intent, and disabled live mutation | Does not create live alliance |
| `InvaderUpdateInvadersPlan_ComposesAddPlayerWarningWhenAllianceMissingWithManyInvaders` | Unit | `Invasion.addPlayer(player, true)` source review | Missing/disbanded alliance with multiple invaders warns and skips participant put | Focused C# test validates warning status, no participant-put intent, existing ids, and disabled live mutation | Does not emit live logger warning |

## Validation Decision

- Changed surface: non-live Vortex invader update composition metadata and focused tests.
- Specific behavior/contract: C# metadata mirrors Java `Invasion.updateInvaders` and `Invasion.addPlayer(player, true)` routing for already-invader guard, add-player invocation, offence-alliance transition, warning branch, participant-put intent, and no live mutation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 86 tests. Existing nullable/analyzer warnings were emitted from unrelated files.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex invader-update fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds non-live composition metadata and tests, without enabling live group/alliance mutation, participant mutation, packet dispatch, scheduler dispatch, or zone-player/Kisk map mutation.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited Vortex invader update composition paths.
- Why this scope is sufficient: the new code is an inert composition planner over supplied invader, existing invader, and invader-alliance snapshots.

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
| `com.aionemu.gameserver.services.vortex.Invasion.updateInvaders` | `Aion.GameServer.Services.VortexInvaderUpdateInvadersPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# composes already-invader guard and addPlayer invocation as metadata. Live invader participant mutation remains disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.addPlayer(Player, boolean=true)` | `Aion.GameServer.Services.VortexInvaderUpdateInvadersPlanService` / `VortexInvaderUpdateAddPlayerPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# composes invader add-player transition metadata, including offence-alliance creation and missing-alliance warning branches. Live team/alliance mutation remains disabled. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Services.VortexInvaderAllianceSnapshot` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records only existence/disbanded snapshots needed by invader update branches. Full alliance runtime behavior remains elsewhere. |
| `com.aionemu.gameserver.model.team.TeamType.ALLIANCE_OFFENCE` | `Aion.GameServer.Model.GameObjects.PlayerAllianceTeamType.AllianceOffence` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records offence-alliance creation intent only. Live alliance creation remains disabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 1
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live invader participant map mutation remains disabled.
- Live group/alliance removal and invader alliance create/add behavior remains disabled.
- Java concurrent map behavior is represented only by supplied snapshots.
- Live logger warning emission remains disabled.
- Production Vortex lifecycle adapters still need real player, alliance, and location inputs before live use.
