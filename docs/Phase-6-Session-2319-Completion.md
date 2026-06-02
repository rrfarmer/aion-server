# Phase 6 Session 2319 Completion - Registered Group Portal Transfer

## Scope

Wired the registered group-instance continuation slice of Java `PortalService.port(...)`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`

Java behavior used:

- For group-sized instances (`maxPlayers` 3 or 6), Java resolves an existing registered instance by `player.getPlayerGroup().getTeamId()`.
- When the registered instance has capacity, Java calls `transfer(player, loc, instance, reenter)`.
- `transfer` sets the instance start position if missing, registers the player object id, calls `TeleportService.teleportTo(..., TeleportAnimation.FADE_OUT_BEAM)`, and applies entrance cooldown only when not reentering.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Implemented:

- `QueuePortalContinueTransferAsync` now routes `PortalTeamEntryPlan` through a team continuation path before generic action checks.
- Registered group instances with capacity now:
  - set start position if missing,
  - register the entering player object id,
  - queue `SmTeleportLoc` with `TeleportAnimation.FadeOutBeam`,
  - apply cooldown for non-reentry,
  - skip cooldown for reentry.
- Fresh group-instance allocation remains unsupported and side-effect-free.
- Alliance/league team portal transfer remains unsupported.

## Validation Decision

- Changed surface: shared live portal movement continuation for registered group-instance transfers.
- Specific behavior/contract: Java `PortalService.port(...)` registered group instance branch transfers the entering player into the existing team instance and applies cooldown only when not reentering.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupInstanceTransfersAndAppliesCooldown|FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupReentryTransfersWithoutCooldown|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceRecordsAllocationNeededWithoutPackets" --no-restore
```

First run result: failed 3 because team plans have `Action.None` from the previous unsupported path and the new branch was checked after the generic action guard.

Fix applied: team-plan continuation is checked before the generic action check.

Second run result: passed 3, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain. This filtered command supplied the compile signal for the affected C# project and tests.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this `PortalService.port(...)` group branch; Java source review was the source-of-truth evidence for this narrow movement slice.
- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: shared portal movement continuation was changed.
- Broad .NET decision: skipped full project/solution validation after focused tests passed. The edited branch was isolated to `QueuePortalContinueTransferAsync`, and the focused command covered non-reentry, reentry, and fresh-allocation-still-blocked paths.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.PortalService.port` group registered-instance branch | `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Registered group instance transfer now sets start position, registers player, queues teleport, and applies/skips cooldown according to reentry. Fresh group allocation, member solo-instance scan, and alliance/league branches remain unported. |
| `com.aionemu.gameserver.world.WorldMapInstance.register` / `registerTeam` transfer usage | `Aion.GameServer.World.WorldMapInstanceRuntimeState` | Runtime State | Partial | Focused Boundary Tested | Partial Parity | Existing runtime state is now used by registered group transfer. Broader WorldMapInstance parity was not re-audited. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo` via `PortalService.transfer` | `QueueInstancePortalTransferAsync` / `QueueDelayedTeleportAsync` | Teleport Service | Partial | Focused Boundary Tested | Partial Parity | Transfer uses `TeleportAnimation.FadeOutBeam` and existing delayed teleport packet path. Full TeleportService parity remains partial. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `QueuePortalContinueTransferAsync_RegisteredGroupInstanceTransfersAndAppliesCooldown` | Boundary Runtime | Java source review of `PortalService.port` group branch and `transfer` | Registered group instance transfer sets instance start position, registers player, queues teleport, and adds cooldown when not reentering. | Focused C# boundary execution plus Java source review. | Does not cover fresh group allocation or live dialog routing into this continuation. |
| `QueuePortalContinueTransferAsync_RegisteredGroupReentryTransfersWithoutCooldown` | Boundary Runtime | Java source review of `PortalService.port` reentry handling | Registered group reentry transfers to existing instance and skips cooldown persistence. | Focused C# boundary execution plus Java source review. | Does not cover already-inside same instance no-op cases beyond current C# branch. |
| `QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceRecordsAllocationNeededWithoutPackets` | Boundary Runtime | Java source review of `PortalService.port` fresh group allocation branch | Fresh group allocation remains side-effect-free and documented as unported. | Focused C# boundary execution plus Java source review. | Does not allocate/register a fresh group instance yet. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported/extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: unchanged, conservatively partial.

## Remaining Gaps

- Fresh group-instance allocation and `registerTeam(group)` remain unported.
- Java member solo-instance scan for `!instanceGroupReq` remains unported.
- Alliance/league portal transfer remains unsupported.
- Generic `PortalEntryInteractionService` still returns `UnsupportedTeamPortal` before live continuation.
- Beshmundir follow-entry and difficulty-acceptance movement still need a direct call into this transfer path.
- Real-client or encrypted socket bytes for group portal transfer remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2319] Transfer registered group portals
```

## Next Recommended UOW

Use the registered group transfer primitive for Beshmundir's Walk non-leader follow-entry when a group member is already inside world `300170000`, or first bridge `PortalEntryInteractionService` to allow registered group plans through continuation if that is the narrower dependency.
