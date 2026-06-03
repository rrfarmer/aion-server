# Phase 6 Session 2441 Completion

## UOW

[Phase 6] UOW-2441: Add alliance offline-timeout scan executor/config binding

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/configs/main/GroupConfig.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutDispatchService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceOfflineTimeoutDispatchServiceTests.cs`

## Implementation Notes

- Added `GameServerOptions.Group` with Java parity bindings for:
  - `gameserver.playergroup.removetime`
  - `gameserver.playeralliance.removetime`
- Added `PlayerAllianceOfflineTimeoutDispatchService.DispatchExpiredScanAsync`, which mirrors the Java checker shape by repeatedly dispatching expired alliance members until the scan observes no more eligible offline members.
- Kept this as an executor/config parity slice. No hosted fixed-rate scheduler or startup registration was added.
- The offence-invader VortexService side effect remains represented as a dispatch result flag only.

## Tests

- Added `DispatchExpiredScanAsync_DrainsExpiredMembersUsingConfiguredAllianceRemoveTimeLikeJavaChecker`.
  - Verifies the scan drains multiple expired offline alliance members using the configured alliance removal time.
  - Verifies members still within the timeout remain in the alliance.
  - Verifies fanout packet counts across successive leave-timeout dispatches.
- Updated `GameServerOptionsTests` to cover group/alliance timeout defaults and `mygs.properties` overrides.

## Validation

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~GameServerOptionsTests" --no-restore
```

- Passed: 7 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed.
- Reported only expected LF-to-CRLF working-tree warnings for touched C# files.

Java/Maven validation was skipped because no Java code or Java fixtures changed. Broad .NET validation was skipped because this UOW only added a narrow executor/config binding and did not wire hosted startup composition.

## Parity Table

| Java Surface | C# Surface | Status | Coverage |
| --- | --- | --- | --- |
| `GroupConfig.GROUP_REMOVE_TIME` / `ALLIANCE_REMOVE_TIME` | `GameServerGroupOptions` | Partial parity | Unit tested |
| `PlayerAllianceService.OfflinePlayerAllianceChecker.run` | `PlayerAllianceOfflineTimeoutDispatchService.DispatchExpiredScanAsync` | Partial parity | Regression tested |
| `PlayerAllianceLeavedEvent` timeout path | Existing alliance leave planner/dispatcher | Partial parity | Regression tested through scan loop |

## Metrics

- Conservative Phase 6 parity estimate: 41%.

## Remaining Risks

- No hosted fixed-rate scheduler has been registered for alliance offline timeout scans.
- `VortexService` offence-invader removal is still not executed by the timeout dispatcher.
- Group offline timeout may need an analogous executor/config bridge.
- Startup composition remains unwired for this checker.
