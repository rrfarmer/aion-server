# Phase 6 Session 2390 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2390`: Modeled Java autogroup penalty refresh scheduling as C# result intents.

## Files Changed
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2390-Completion.md`
- `docs/Phase-6-Session-2390-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.AutoGroupService`
- `com.aionemu.gameserver.services.instance.PeriodicInstanceManager`
- `com.aionemu.gameserver.services.autogroup.AutoGroupUtility`

## C# Artifacts Touched
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
- `Aion.GameServer.Services.PeriodicInstanceRegistrationService` as the future live refresh counterpart
- `Aion.GameServer.Tests.AutoGroupLookingPartyRegistrationServiceTests`

## What Changed
- Added `AutoGroupPenaltyRefreshIntent` with the Java 10000 ms delay and source breadcrumb.
- `CancelRegistrationAsync(...)`, `ApplyReadyMatchPlanAsync(...)`, and open quick-entry attachment now expose which players Java would penalize and refresh after delay.
- Confirmed by Java source review that the Java `penalties` set is not a registration guard; it deduplicates delayed refresh work.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore`
- Result: 60 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped; no broad-validation trigger for this non-live result-contract slice.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Penalty refresh scheduling is represented as result intents for cancellation and additional-registration cleanup. Live delayed scheduling/dedupe remains unwired. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService` | Service | Partial | Unit Tested | Partial Parity | C# can create open-registration packets, but this UOW does not schedule delayed refresh delivery. |

## Known Gaps
- Live penalty refresh scheduling is not wired.
- C# does not yet keep a Java-style `penalties` set for deduping repeated delayed refresh schedules.
- Java `AutoGroupService.onLogout(...)` still needs planner/runtime coverage.
- Cancel-enter active-instance penalty currently remains documented through runtime breadcrumbs and result intents elsewhere; live delayed refresh delivery is still future work.

## Next Recommended UOW
- `UOW-2391`: Add a focused live penalty-refresh scheduler/dedupe adapter, or a scheduler planner if the live delivery boundary is not safe enough.

## Suggested Discovery For UOW-2391
- Java:
  - `AutoGroupService.penalisePlayerAndScheduleRemoval(int objectId)`
  - `PeriodicInstanceManager.checkAndSendOpenRegistrations(int objectId)`
  - `PeriodicInstanceManager.checkAndSendOpenRegistrations(Player player)`
- C#:
  - `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateOpenRegistrationPackets(...)`
  - `Aion.GameServer.Network.Aion.GameServerConnection` cancel-registration and cancel-enter paths
  - `Aion.GameServer.Services.ExpirableTaskService` or `ThreadPoolManager` if live delayed scheduling is chosen
  - `AutoGroupPenaltyRefreshIntent` call sites from `AutoGroupLookingPartyRegistrationService`

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: Java-style penalty refresh scheduling dedupes per player for 10000 ms and eventually sends open registration packets through the C# periodic-registration packet creator.
- Focused C# command if live scheduling is wired:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests" --no-restore`
- Focused C# command if only a scheduler planner is added:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: live scheduler/connection dispatch if live delayed delivery is enabled; none for a planner-only slice.

## Safe Candidate UOWs
- Port `AutoGroupService.onLogout(...)` search-entry cleanup as a planner.
- Add tests for stop-registration close behavior confirming it sends cancel windows without penalty refresh intents.
- Improve cancel-enter destroy-if-possible modeling if online-inside-player facts become available.
