# Phase 6 Session 2441 Handoff

## Latest Completed UOW

[Phase 6] UOW-2441: Add alliance offline-timeout scan executor/config binding

## Summary

UOW-2441 exposed the Java group/alliance offline timeout keys in `GameServerOptions.Group` and added a narrow alliance timeout scan executor. The scan loops over the existing single-member timeout dispatcher until no expired offline alliance member remains, matching the Java `OfflinePlayerAllianceChecker.run` shape without adding hosted startup registration.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutDispatchService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceOfflineTimeoutDispatchServiceTests.cs`
- `docs/Phase-6-Session-2441-Completion.md`
- `docs/Phase-6-Session-2441-Handoff.md`

## Validation Completed

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~GameServerOptionsTests" --no-restore
```

- Passed: 7 tests.

```powershell
git diff --check
```

- Passed with only LF-to-CRLF working-tree warnings.

## Suggested Next UOW

[Phase 6] UOW-2442: Add a narrow alliance offline-timeout scheduler wrapper or offence-timeout side-effect executor

Two safe next slices are available:

1. Add a testable scheduler wrapper around `DispatchExpiredScanAsync` that uses `GameServerOptions.Group.AllianceRemoveTimeSeconds` and Java-like timing constants, without registering it in startup yet.
2. Add a narrow offence-invader side-effect executor for the existing `WouldRemoveOffenceInvader` result flag, after locating the closest C# Vortex/Rift service equivalent.

The scheduler wrapper is the more direct continuation of the Java checker, but startup registration should be treated as a separate composition UOW because it broadens the validation surface.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/configs/main/GroupConfig.java`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutDispatchService.cs`
- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- Any existing C# hosted/timer services under `dotnetConversion/src/Aion.GameServer`
- Any C# Vortex/Rift service equivalents if choosing the offence side-effect path

## Suggested Validation

For a scheduler wrapper UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutSchedulerTests" --no-restore
```

If no scheduler test file exists, create one alongside the dispatcher tests.

For an offence side-effect UOW, keep validation focused on the timeout dispatcher plus the located Vortex/Rift service tests.

## Broad Validation Triggers

- Startup/DI registration of a hosted scheduler should trigger composition-focused validation.
- Java/Maven validation is not expected unless Java source or Java fixtures change.
- Broad .NET validation is not required for an unregistered scheduler class or a narrow executor with focused coverage.
