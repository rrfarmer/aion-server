# Phase 6 Session 2443 Completion

## UOW

[Phase 6] UOW-2443: Add alliance offline-timeout creation trigger guard

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAlliance.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceRuntimeTests.cs`

## Implementation Notes

- Added an optional `startOfflineTimeoutCheck` callback to `PlayerAllianceRuntime`.
- `CreateAlliance` now guards that callback with a one-shot flag, mirroring Java `offlineCheckStarted.compareAndSet(false, true)`.
- The callback is invoked only after the alliance state and leader snapshot are created.
- Duplicate alliance creation still throws before the trigger path, so failed creation does not request another offline checker start.
- This UOW intentionally does not register the callback in production DI or startup composition.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `CreateAlliance_StartsOfflineTimeoutCheckOnceLikeJavaAtomicBoolean` | Unit | `PlayerAllianceService.createAlliance` source review | First alliance creation requests the offline checker once; subsequent creations do not | Callback count matches Java `AtomicBoolean.compareAndSet(false, true)` behavior | Does not prove production DI wiring |
| `CreateAlliance_DuplicateDoesNotStartOfflineTimeoutCheckAgainLikeJavaFailedCreate` | Unit | `PlayerAllianceService.createAlliance` source review and existing C# duplicate guard | Failed duplicate create does not request another checker start | Callback count remains one after duplicate failure | Java duplicate id behavior differs in static id generation and is not fully modeled |

## Validation Decision

- Changed surface: one C# runtime service plus focused tests.
- Specific behavior/contract: Java alliance creation starts the scheduled offline checker once through `offlineCheckStarted.compareAndSet(false, true)`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutSchedulerTests" --no-restore
```

- Result: Passed, 36 tests.
- Focused Java/Maven command: skipped; no Java source or Java fixtures changed, and the Java parity source for this slice was direct source review of the creation trigger.
- Broad-validation trigger: none. Production startup/DI registration was intentionally not added.
- Broad .NET decision: skipped; the focused command built the affected project and validated the runtime trigger plus adjacent scheduler contract.
- Why this scope is sufficient: the UOW added a one-shot runtime hook and tests only. It did not change packet primitives, persistence, shared scheduler primitives, live startup composition, or network dispatch.

## Additional Hygiene

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings for touched C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.createAlliance` | `Aion.GameServer.Services.PlayerAllianceRuntime.CreateAlliance` | Service/runtime | Partial | Unit Tested | Partial Parity | Java source reviewed; one-shot offline-check trigger semantics are unit tested. Java create also adds both leader/invited through events and static id generation, which remain broader port surfaces. |
| `java.util.concurrent.atomic.AtomicBoolean` guard in `PlayerAllianceService.offlineCheckStarted` | `Aion.GameServer.Services.PlayerAllianceRuntime._offlineTimeoutCheckStarted` | Concurrency guard | Partial | Unit Tested | Partial Parity | Guard behavior is tested for repeated C# alliance creation. Full threading/lifecycle parity is not claimed because production scheduler wiring is still absent. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.initializeOfflineCheck` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutScheduler.Start` | Service scheduler | Partial | Adjacent Unit Tested | Partial Parity | Scheduler wrapper exists from UOW-2442; this UOW only adds the trigger hook that can call it later. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Production DI does not yet pass `PlayerAllianceOfflineTimeoutScheduler.Start` into `PlayerAllianceRuntime`.
- Java `createAlliance` creates an alliance for leader plus invited player in one service method; the current C# runtime still creates the leader and uses separate `AddMember` calls.
- Offence-invader VortexService removal remains a result flag and does not execute the Java side effect.
- Group offline timeout checker remains a likely parallel gap.
