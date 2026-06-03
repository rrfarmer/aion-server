# Phase 6 Session 2447 Completion

## UOW

[Phase 6] UOW-2447: Port alliance timeout offence Vortex side-effect execution

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/model/vortex/VortexLocation.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceInfoPlan.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutDispatchService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceOfflineTimeoutDispatchServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceRuntimeTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexInvasionRuntime` as a narrow C# runtime for Java `VortexService.removeInvaderPlayer` active-invasion participant cleanup.
- The runtime tracks active Vortex invasion locations, invader object ids, and passed-player portal state, then removes both invader and passed-player state during offence timeout cleanup.
- `PlayerAllianceOfflineTimeoutPlan` now carries the timed-out `Player` reference so dispatch can execute the Java-equivalent side effect against the same object.
- `PlayerAllianceOfflineTimeoutDispatchService` now calls `VortexInvasionRuntime.RemoveInvaderPlayer` when the timed-out member came from an offence alliance.
- The dispatch result preserves the existing `WouldRemoveOffenceInvader` flag and adds executed `VortexInvaderRemoval` evidence plus `RemovedOffenceInvader`.
- `Program.cs` registers `VortexInvasionRuntime` in the production graph beside Vortex/Rift services.
- Scope is intentionally limited to the offline timeout path. Java online kick messages and home teleport from `Invasion.kickPlayer` remain future work for live Vortex kick parity, because `OfflinePlayerAllianceChecker` only selects offline members.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `RemoveInvaderPlayer_RemovesActiveInvaderAndPassedPortalStateLikeJava` | Unit | `VortexService.removeInvaderPlayer`, `Invasion.kickPlayer` | Active invader cleanup removes the invader and passed-player state | Snapshot is empty after removal and result reports Java source path | Does not model defender alliance, online packets, or teleport |
| `DispatchNextExpiredAsync_ExecutesOffenceInvaderVortexRemovalLikeJavaTimeout` | Unit/dispatch | `OfflinePlayerAllianceChecker.run`, `VortexService.removeInvaderPlayer` | Offence alliance timeout dispatch executes Vortex removal, not just the planning flag | Dispatch result has `RemovedOffenceInvader`, Vortex state is cleared, and alliance membership is removed | Uses in-memory runtime, not a live spawned Vortex controller |
| `RemoveNextExpiredOfflineMemberWithLeaveWorkflow_DispatchesTimeoutWithoutLeagueBroadcastLikeJava` | Unit | `OfflinePlayerAllianceChecker.run` | Timeout plan keeps the exact timed-out player object needed by side-effect dispatch | `TimedOutPlayer` is asserted as the same instance | Vortex execution is covered in dispatch test |

## Validation Decision

- Changed surface: production dispatch now mutates Vortex invasion runtime state for offence alliance timeouts.
- Specific behavior/contract: Java `OfflinePlayerAllianceChecker.run` calls `VortexService.removeInvaderPlayer` for offence alliances before firing leave timeout; C# timeout dispatch now executes active-invader and passed-player cleanup for that path.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~Vortex|FullyQualifiedName~Rift" --no-restore
```

- Result: Passed, 109 tests. Existing nullable/analyzer warnings were emitted.
- Additional DI command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupServiceCollectionExtensionsTests" --no-restore
```

- Result: Passed, 5 tests.
- Focused Java/Maven command: skipped; no Java source or Java fixtures changed, and Java behavior was verified by direct source review.
- Broad-validation trigger: present because production dispatch can now mutate Vortex invasion runtime state.
- Broad .NET decision: full project/solution validation was skipped after focused validation passed because the changed live mutation is isolated to alliance timeout dispatch and the new in-memory Vortex participant runtime; focused tests built the project, covered dispatch, alliance runtime, Vortex runtime, adjacent Vortex/Rift tests, and DI constructor composition.
- Why this scope is sufficient: no packet serialization primitives, persistence mappings, database schema, crypto, socket protocol framing, spawned world state, or general teleport APIs changed. The live mutation is currently reachable only through the timeout dispatch path and the new singleton runtime.

## Additional Hygiene

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings for touched C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.removeInvaderPlayer` | `Aion.GameServer.Services.VortexInvasionRuntime.RemoveInvaderPlayer` | Service/runtime side effect | Partial | Unit Tested | Partial Parity | Active invader and passed-player cleanup are ported for timeout dispatch; online packet/teleport behavior is deferred. |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexInvasionRuntime.RemoveInvaderPlayer` | Vortex participant mutation | Partial | Unit Tested | Partial Parity | Removes invader and passed-player state; does not yet own Vortex alliance disband, defender side, zone sync, or teleport. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService` | Scheduled timeout dispatcher | Partial | Unit Tested | Partial Parity | Offence timeout now executes Vortex cleanup before dispatching leave packets. |
| `com.aionemu.gameserver.model.vortex.VortexLocation` | `Aion.GameServer.Dataholders.VortexLocationSummary` plus `VortexInvasionRuntime` | Data/runtime bridge | Partial | Unit Tested | Partial Parity | Runtime uses location id and invasion world/home/start positions from existing Vortex data. |

## Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live Vortex online kick parity is still incomplete: Java sends `1401452` or `1401476`, may send `1401474`, and teleports online invaders home.
- Full Vortex controller sync/pass-list parity is represented as in-memory passed-player cleanup only; spawned controller integration remains future work.
- Defender Vortex removal and group offline timeout scheduling remain separate gaps.
- No real-host Vortex lifecycle smoke exists yet because active Vortex spawning is still not fully ported.
