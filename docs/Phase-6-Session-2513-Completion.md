# Phase 6 Session 2513 Completion

## UOW

[Phase 6] UOW-2513: Add guarded Vortex defender acceptance participant runtime report

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `com.aionemu.gameserver.services.vortex.Invasion.addPlayer(player, false)`
- `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders.RequestResponseHandler.acceptRequest`
- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.addPlayer`
- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.createAlliance`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationAcceptancePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexDefenderAcceptanceParticipantRuntimeReportService`, an opt-in report that consumes `VortexDefenderInvitationAcceptanceTransitionRuntimeReport`.
- Added `VortexDefenderAcceptanceParticipantRuntimeReport` and status metadata for no-op, would-record, already-participant, and warning/no-participant-put outcomes.
- When the accepted transition reaches an add-player plan with `WouldPutParticipant`, the report derives defender ids before from the supplied current ids or the transition plan, then appends the responder id as Java `defenders.put(player.getObjectId(), player)` would.
- The Java warning path, full-alliance path, denied response, and missing-request path preserve no-op participant metadata.
- Scope remains guarded. The report does not call `VortexInvasionRuntime.AddDefender`, does not mutate live alliances or teams, and does not send packets.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DefenderAcceptanceParticipantRuntimeReport_RecordsAcceptedParticipantWithoutMutatingRuntime` | Unit | `Invasion.addPlayer(player, false)` source review | Accepted defender transition that would create the defender alliance reports participant ids before/after | Focused C# test validates `[1001] -> [1001, 1004]`, create-defender-alliance metadata, unchanged runtime defender ids, unchanged player membership, and disabled live side effects | Does not mutate live participant map or alliance |
| `DefenderAcceptanceParticipantRuntimeReport_PreservesNoMutationForDeniedMissingFullAllianceAndWarn` | Unit | `RequestResponseHandler.handle`, `acceptRequest`, and `Invasion.addPlayer(player, false)` source review | Denied, missing, full-alliance, and too-many-participants warning outcomes do not report a participant put | Focused C# test validates no-op before/after ids, warning status for the Java early-return branch, no live mutation, and no live packet send | Non-Vortex/payload-missing branches remain covered by existing response-consumption tests |

## Validation Decision

- Changed surface: one C# opt-in participant runtime report plus focused tests.
- Specific behavior/contract: Java `Invasion.addPlayer(player, false)` writes the defender participant map only after add-to-existing-alliance, create-defender-alliance, or record-first logic succeeds. If the defender participant count is greater than one and no live alliance exists, Java logs a warning and returns before `defenders.put`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Result: Passed, 132 tests. Existing nullable/analyzer warnings were emitted from unrelated files and prior Vortex test lines.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and no narrow Java Vortex add-player fixture was added.
- Broad-validation trigger: none. This report is opt-in and does not mutate live defender maps, groups, alliances, packets, schedulers, spawns, despawns, or portals.
- Broad .NET decision: full project/solution validation skipped because the focused command covered the edited Vortex tests and adjacent response-registry contract.
- Why this scope is sufficient: focused tests cover accepted participant would-record ids, create-alliance participant metadata, denied/missing/full-alliance no-op outcomes, Java warning early-return behavior, and disabled live mutation.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported expected CRLF conversion warnings for edited C# source.
- `git diff --cached --check` passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.addPlayer(player, false)` | `Aion.GameServer.Services.VortexDefenderAcceptanceParticipantRuntimeReportService` | Defender participant runtime report | Partial | Unit Tested | Partial Parity | C# reports the participant-map effect of record-first/add-existing/create-alliance branches without live mutation. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.addPlayer` | `Aion.GameServer.Services.VortexDefenderAddPlayerTransitionPlanService` / `VortexDefenderAcceptanceParticipantRuntimeReport` | Defender alliance add metadata | Partial | Unit Tested | Partial Parity | C# carries add-existing-alliance metadata and would-record participant ids; live alliance mutation remains disabled. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.createAlliance` | `Aion.GameServer.Services.VortexDefenderAddPlayerTransitionPlanService` / `VortexDefenderAcceptanceParticipantRuntimeReport` | Defender alliance creation metadata | Partial | Unit Tested | Partial Parity | C# carries `AllianceDefence` creation metadata and would-record participant ids; live alliance creation remains disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.defenders.put` | `Aion.GameServer.Services.VortexDefenderAcceptanceParticipantRuntimeReport` | Defender participant map metadata | Partial | Unit Tested | Partial Parity | C# exposes before/after defender ids and preserves warning/no-op outcomes; live map mutation remains disabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 45%

## Remaining Risks

- Live Vortex response-handler callbacks still do not mutate group, alliance, or defender participant maps.
- Existing defender ids for participant reports are supplied by the caller or derived from the transition plan; production location/alliance containers remain unwired.
- Java warning behavior is source-reviewed and C# unit-tested through planner metadata, but no Java fixture/golden validates it directly.
- Enabling live participant-map mutation will be a broad-validation trigger.
