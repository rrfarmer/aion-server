# Phase 6 Session 2443 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2443: Add alliance offline-timeout creation trigger guard

## Commits Made

- `[Phase 6][UOW-2443] Add alliance timeout trigger guard`

## Summary

UOW-2443 added a Java-like one-shot creation trigger hook to `PlayerAllianceRuntime.CreateAlliance`. The runtime accepts an optional `startOfflineTimeoutCheck` callback and invokes it only once after the first alliance is created, mirroring Java `offlineCheckStarted.compareAndSet(false, true)`. Production DI/startup wiring remains intentionally unregistered.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceRuntimeTests.cs`
- `docs/Phase-6-Session-2443-Completion.md`
- `docs/Phase-6-Session-2443-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService`
- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.createAlliance`
- `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.initializeOfflineCheck`

## C# Artifacts Touched

- `Aion.GameServer.Services.PlayerAllianceRuntime`
- `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler` (adjacent validation only)

## Validation Completed

Validation target: Java `PlayerAllianceService.createAlliance` starts the offline checker exactly once through `offlineCheckStarted.compareAndSet(false, true)`, while later creations and failed duplicate creation do not request another scheduler start.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutSchedulerTests" --no-restore
```

- Passed: 36 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings.

Java/Maven validation was skipped because no Java source or Java fixtures changed, and this UOW relied on direct source review of the Java creation trigger. Broad .NET validation was skipped because no hosted startup registration, shared scheduler primitive, packet primitive, persistence, or live side-effect wiring changed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.createAlliance` | `Aion.GameServer.Services.PlayerAllianceRuntime.CreateAlliance` | Service/runtime | Partial | Unit Tested | Partial Parity | One-shot offline checker trigger is tested. C# still separates leader creation from later member add and does not wire production scheduler callback. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.offlineCheckStarted` | `Aion.GameServer.Services.PlayerAllianceRuntime._offlineTimeoutCheckStarted` | Concurrency guard | Partial | Unit Tested | Partial Parity | C# one-shot callback guard mirrors Java `AtomicBoolean.compareAndSet(false, true)` for runtime creation. Full lifecycle/threading parity remains unverified. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.initializeOfflineCheck` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler.Start` | Service scheduler | Partial | Adjacent Unit Tested | Partial Parity | Scheduler wrapper exists and was adjacent-validated. Runtime can now accept a callback, but production composition has not connected them. |

## Known Gaps

- `PlayerAllianceRuntime` is not yet constructed in production with `PlayerAllianceOfflineTimeoutScheduler.Start`.
- No production scheduler lifecycle owner exists to cancel the scheduled task on shutdown.
- Java `createAlliance(Player leader, Player invited, TeamType type)` creates and adds two players; C# runtime still models create plus separate add-member calls.
- `VortexService.removeInvaderPlayer` is still not invoked for offence timeout removals.
- Group offline timeout checker remains separate and not yet ported to matching trigger/scheduler wiring.

## Remaining Risks

- Wiring the scheduler callback in DI may require careful lifecycle/cancellation handling and may create a composition validation trigger.
- A live scheduler callback will cross runtime state, scheduler, and connection dispatch surfaces; validate focused runtime/scheduler behavior first before any broader composition check.
- Offence/defence Vortex side effects still block fuller alliance timeout parity.

## Next Recommended UOW

[Phase 6] UOW-2444: Add production composition readiness for alliance offline timeout scheduler or port the offence-timeout Vortex side-effect executor

The next sequential task is to wire `PlayerAllianceRuntime` to `PlayerAllianceOfflineTimeoutScheduler.Start` in a narrow composition/readiness surface. Prefer a composition plan/readiness service or DI extension test before live hosted registration so the lifecycle/cancellation shape can be validated without broad startup churn.

Alternative safe candidate: inspect Java `VortexService` and C# Vortex/Rift equivalents, then execute or plan the `WouldRemoveOffenceInvader` side effect in `PlayerAllianceOfflineTimeoutDispatchService`.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupServiceCollectionExtensions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceOfflineTimeoutScheduler.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs` only if choosing live startup registration
- Any C# Vortex/Rift service equivalents under `dotnetConversion/src/Aion.GameServer`

## Suggested Validation

For a composition/readiness UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupServiceCollectionExtensionsTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutSchedulerTests|FullyQualifiedName~PlayerAllianceRuntimeTests" --no-restore
```

Behavior to prove: the shared `PlayerAllianceRuntime` can be constructed with a scheduler-start callback and creating the first alliance schedules exactly one offline timeout check through the Java-like scheduler cadence.

For an offence side-effect UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests|FullyQualifiedName~Vortex" --no-restore
```

Narrow the `Vortex` portion to the actual located C# test class before running.

Java/Maven is not expected unless Java source or fixtures change. Broad-validation trigger is `none` for a non-live composition/readiness plan. Live hosted registration or live side-effect execution may create a broad-validation trigger and should be documented before any wider .NET validation.
