# Phase 6 Session 2493 Completion

## UOW

[Phase 6] UOW-2493: Compose Vortex stopInvasion side-effect metadata with kick removal

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Extended the non-live stop side-effect planner to attach `VortexKickPlayerRemovalPlan` metadata to each Java-shaped online invader kick.
- Preserved Java `Invasion.stopInvasion` ordering:
  - clear active Vortex;
  - kill invader Kisks;
  - kick online invaders;
  - despawn invasion NPCs;
  - spawn `PEACE` NPCs.
- Simulates per-online-invader passed-player removal counts in the supplied invader order for `syncPassed(true)` metadata.
- Offline invaders are skipped for kick/removal metadata, matching Java `if (invader.isOnline())`.
- Scope remains metadata-only. It does not send packets, teleport players, mutate alliances, mutate participant maps, mutate passed-player maps, sync passed-player state, kill Kisks, despawn NPCs, or spawn NPCs.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StopSideEffectPlan_ComposesJavaStopInvasionOrderWithoutLiveExecution` | Unit | `Invasion.stopInvasion` and `Invasion.kickPlayer` source review | Java stop ordering plus attached kick/removal plans only for online invaders | Focused C# test validates active Vortex clearing, Kisk death intent, online invader kick order, per-invader kick/removal metadata, passed-player sync counts, despawn intent, PEACE spawn intent, and disabled live side effects | Metadata only; relies on supplied snapshots and supplied invader order |

## Validation Decision

- Changed surface: non-live Vortex stop side-effect composition metadata and focused tests.
- Specific behavior/contract: C# metadata mirrors Java `Invasion.stopInvasion` ordering and composes Java `kickPlayer(player, true)` removal intent for each online invader without enabling live mutation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 90 tests. Existing nullable/analyzer warnings were emitted from unrelated files.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex stop/removal fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only composes non-live metadata and tests, without enabling live packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, Kisk death, despawn, spawn, scheduler dispatch, or zone-player/Kisk map mutation.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited Vortex stop/kick metadata paths.
- Why this scope is sufficient: the new code is an inert planner over supplied stop result, invader, alliance, passed-player, Kisk, despawn, and peace-spawn snapshots.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported expected CRLF conversion warnings for the edited source and test files.
- `git diff --cached --check` passed after staging. Git reported expected CRLF conversion warnings for edited/new files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionSideEffectPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# composes Java stop order and online-invader kick metadata. Live side effects remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer(Player, boolean=true)` | `Aion.GameServer.Services.VortexKickPlayerRemovalPlan` via stop plan | Runtime planner | Partial | Unit Tested | Partial Parity | C# attaches per-online-invader kick/removal metadata. Live packet, teleport, alliance, participant, passed-player, and sync mutation remain disabled. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `Aion.GameServer.Services.VortexStopInvaderKiskSnapshot` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records Kisk death intent only. Live Kisk controller death remains disabled. |
| `com.aionemu.gameserver.model.vortex.VortexStateType.PEACE` | `Aion.GameServer.Services.VortexStopPeaceSpawnSnapshot` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records PEACE spawn metadata only. Live spawn remains disabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 1
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live side effects remain disabled across packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, Kisk death, despawn, and spawn.
- Passed-player count simulation depends on supplied invader ordering and snapshots.
- Production adapters still need real Vortex location, player, alliance, passed-player, Kisk, despawn, and spawn inputs before live use.
