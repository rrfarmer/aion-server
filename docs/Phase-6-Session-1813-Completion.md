# Phase 6 Session 1813 Completion - Wire CM_CRAFT Composition Observer Seam

Date: 2026-05-31
Unit of Work: UOW-1813
Status: Complete

## Scope

Wire a non-live `GameServerConnection` observer seam for `CmCraftStartCompositionPlan` so real CM_CRAFT packet processing can be observed through the UOW-1812 adapter. This unit intentionally does not execute live `CraftService.startCrafting`, spend DP, mutate inventory, send packets, create `CraftingTask`, or start scheduler work.

## Completed Work

- Added optional `cmCraftStartCompositionPlanObserver` constructor hook to `GameServerConnection`.
- Added `ObserveCraftStartCompositionPlan(...)` in the CM_CRAFT handler path.
- Preserved the existing `CmCraftRuntimePlan` observer and deferred start-crafting behavior.
- Composed `CmCraftStartCompositionPlan` from real parsed CM_CRAFT packets, runtime guard result, target facts, and available static data.
- Used loaded static data when available for recipe/product lookup; when static data is absent, the composition plan conservatively records missing recipe/product evidence instead of dispatching live side effects.
- Added integration tests proving:
  - a real CM_CRAFT start intent records a non-live composition plan
  - runtime-blocked CM_CRAFT packets record a composition plan without validation/consumption/task planners
  - no packets are sent by the observer seam

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 304 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4556 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_CRAFT.runImpl`
- `com.aionemu.gameserver.services.craft.CraftService.startCrafting`
- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`

## Migration Parity Table - UOW-1813

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_CRAFT.runImpl` guard success before `CraftService.startCrafting(...)` | `GameServerConnection.HandleCraftAsync` + `ObserveCraftStartCompositionPlan` | Handler Adapter | Partial | Integration Tested | Partial Parity | Real packet processing now records the non-live composition plan after runtime guard success; live `startCrafting` is still deferred. |
| `CM_CRAFT.runImpl` silent return guards | `CmCraftStartCompositionPlanStatus.RuntimeBlocked` observer path | Handler Adapter | Partial | Integration Tested | Partial Parity | Runtime-blocked packets produce an observable non-live composition plan with no validation/consumption/task side effects. |
| `CraftService.startCrafting` static recipe/product lookups | `ObserveCraftStartCompositionPlan` static-data lookup inputs | Handler Adapter | Partial | Build/Unit Tested | Partial Parity | C# uses loaded static data when available; fixture without static data verifies conservative missing-recipe behavior only. |

## Risks / Gaps

- No live `CraftService.startCrafting` execution.
- No DP spend, live inventory mutation, persistence, packet sending, live `CraftingTask`, or scheduler startup is wired.
- Integration tests do not yet load full static data for a ready successful composition path.
- C# target facts still depend on current world/NPC abstractions and do not yet model Java `StaticObject` directly.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Add static-data-backed CM_CRAFT handler composition coverage for a real recipe/product so the observer seam can prove successful ready-path composition from packet processing.
- Safe alternatives:
  - begin non-live inventory mutation plan for material/bonus consumption
  - add a live-safe craft finish cooldown application mutation plan
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionCraftTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1813-Completion.md`
- `docs/Phase-6-Session-1813-Handoff.md`
