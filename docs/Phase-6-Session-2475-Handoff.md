# Phase 6 Session 2475 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2475: Add Vortex start double-start guard metadata

## Commits Made

- `[Phase 6][UOW-2475] Add Vortex start double-start guard metadata`

## Summary

UOW-2475 modeled Java `DimensionalVortex.start`'s double-start guard in C# runtime metadata. `VortexInvasionRuntime.StartInvasionWithResult` now reports `Started` for the first start and `AlreadyStarted` for repeated starts, preserving the active portal and participant metadata. The existing `StartInvasion` method delegates to the guarded result path while preserving its snapshot-returning API.

This remains partial parity. The runtime still models metadata only and does not execute live Vortex start side effects, spawn/despawn behavior, scheduling, teleporting, or dispatch.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2475-Completion.md`
- `docs/Phase-6-Session-2475-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.DimensionalVortex.start`
- `com.aionemu.gameserver.services.VortexService.startInvasion`
- `com.aionemu.gameserver.services.vortex.Invasion.startInvasion`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexInvasionRuntime`
- `Aion.GameServer.Services.VortexStartInvasionResult`
- `Aion.GameServer.Services.VortexStartInvasionStatus`

## Validation Completed

Validation target: C# runtime metadata mirrors Java `DimensionalVortex.start` by treating repeated starts as guarded no-ops that preserve active portal and participant metadata.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Passed: 34 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
```

- Passed. Git reported expected CRLF conversion warnings for edited C# files.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds non-live metadata and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.DimensionalVortex.start` | `Aion.GameServer.Services.VortexInvasionRuntime.StartInvasionWithResult` | Runtime service | Partial | Unit Tested | Partial Parity | C# reports start vs already-started metadata and preserves state on repeated starts. It does not execute live `Invasion.startInvasion`. |
| `com.aionemu.gameserver.services.VortexService.startInvasion` | `Aion.GameServer.Services.VortexInvasionRuntime.StartInvasion` | Runtime service | Partial | Unit Tested | Partial Parity | Existing snapshot-returning start path delegates to the guarded result path. Service-level scheduling and active-invasion ownership are still incomplete. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Service-level `VortexService.startInvasion` active-map ownership and scheduler behavior remain incomplete.
- Production stop snapshot sourcing is still absent for invader kisks, online invaders, and spawned Vortex NPCs.
- Defender alliance lifecycle and live zone handler wiring remain incomplete.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live start work will need Java parity for `VortexService.startInvasion`, `Invasion.startInvasion`, `VortexLocation.setActiveVortex`, `despawn`, INVASION spawn selection, rift generator init, defender alliance update, and scheduled stop timing.
- Java synchronization is represented by the runtime lock, but full Java service concurrency is not ported.
- Static Vortex spawn metadata remains inert until live spawn execution is explicitly scoped.

## Next Recommended UOW

[Phase 6] UOW-2476: Add Vortex start side-effect plan metadata

The next smallest safe task is to model Java `Invasion.startInvasion` as metadata-only ordered plan output. The plan should preserve the Java side-effect order of `setActiveVortex`, `despawn`, `spawn(INVASION)`, `initRiftGenerator`, and `updateAlliance`, while still avoiding live spawn/despawn/rift/alliance execution.

Safe alternative candidates:

- Add static-data fixture coverage for multiple Vortex location ids and PEACE/INVASION row ordering if production XML row ordering becomes a concern.
- Add Java-name mapping metadata for `VortexStateType` serialization if an output contract becomes relevant.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Suggested Validation

For start side-effect plan metadata:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Validation target: C# metadata mirrors Java `Invasion.startInvasion` ordering without executing live spawn/despawn/rift/alliance side effects.

Java/Maven is not expected unless Java fixtures change or a narrow Java Vortex lifecycle fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live metadata and tests; it becomes present if XML shape, production spawn selection, world-map enumeration, scheduler wiring, live spawn/despawn, teleport, or dispatch changes.
