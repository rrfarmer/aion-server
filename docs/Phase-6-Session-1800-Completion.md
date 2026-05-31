# Phase 6 Session 1800 Completion - Add Live CM_CRAFT Dispatch Shell

Date: 2026-05-31
Unit of Work: UOW-1800
Status: Complete

## Scope

Port the narrow live `CM_CRAFT` dispatch shell in `GameServerConnection`, consuming the existing `CmCraftRuntimePlanService` guard planner without widening into Java `CraftService.startCrafting`, material consumption, DP mutation, or `CraftingTask` scheduler execution.

## Completed Work

- Added `CmCraft` routing in `GameServerConnection`:
  - dispatches opcode `141` packets from the existing packet factory path
  - handles missing active-player packets through the planner rather than dropping them before guard evaluation
  - preserves Java shutdown-soon guard ordering by skipping target lookup while shutdown-soon is true
  - applies non-morph target existence, 10m range, same world/instance, and template-id facts before invoking the planner
  - preserves morph-substance bypass behavior when `unk == 129`
- Added a narrow plan observer hook for socket-level regression tests.
- Kept `StartCrafting` as a logged deferred boundary because Java `CraftService.startCrafting` is not fully ported.
- Added `GameServerConnectionCraftTests` for live packet-dispatch evidence.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionCraftTests|FullyQualifiedName~CmCraftTests|FullyQualifiedName~CmCraftRuntimePlanServiceTests"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionCraftTests|FullyQualifiedName~CmCraftTests|FullyQualifiedName~CmCraftRuntimePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`

Result:

- Focused craft dispatch/packet/planner validation passed with 13 tests.
- Full .NET suite passed with 4811 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4604` game

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_CRAFT`
- `com.aionemu.gameserver.services.craft.CraftService.startCrafting`
- `com.aionemu.gameserver.GameServer.isShuttingDownSoon`
- `com.aionemu.gameserver.utils.PositionUtil`

## Migration Parity Table - UOW-1800

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_CRAFT.runImpl` live dispatch guard shell | `GameServerConnection.HandleCraftAsync` + `CmCraftRuntimePlanService.CreatePlan` | Client Packet Dispatch / Runtime Guard | Partial | Regression Tested | Partial Parity | Java source reviewed; live C# dispatch now reaches the planner and preserves missing-player, shutdown-soon, non-morph target validation, and morph bypass branches. C# still does not call `CraftService.startCrafting`, and static object identity is approximated through current world-visible object metadata until a first-class `StaticObject` model exists. |
| `GameServer.isShuttingDownSoon` guard precedence in `CM_CRAFT.runImpl` | `GameServerConnection` injected shutdown-soon gate | Runtime Guard | Partial | Regression Tested | Partial Parity | Socket-level regression proves shutdown-soon produces a silent plan before target validation. The injected hook is not yet wired to the production `ShutdownHook` countdown. |
| `PositionUtil.isInRange(player, staticObject, 10)` pre-start craft guard | `GameServerConnection.IsInCraftTargetRange` | Range Check | Partial | Regression Tested | Partial Parity | C# uses existing PositionUtil-style strict squared-distance checks and same world/instance validation. Java bound-radius/static-object overload details remain future verification when static objects are modeled directly. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmCraftWithoutActivePlayerRecordsJavaSilentNoPlayerPlan` | Regression Added | Java `CM_CRAFT.runImpl` opening guard | Socket dispatch records the Java silent no-player/not-spawned plan and sends no packets. | Source-derived live dispatch regression. | Uses C# active-player absence as the missing-player case. |
| `ProcessPacketAsync_CmCraftShutdownSoonReturnsBeforeTargetValidation` | Regression Added | Java `CM_CRAFT.runImpl` shutdown guard | Shutdown-soon short-circuits before target validation and sends no packets. | Source-derived live dispatch regression with injected shutdown hook. | Production shutdown hook is not wired yet. |
| `ProcessPacketAsync_CmCraftNonMorphInvalidTargetRecordsSilentInvalidPlan` | Regression Added | Java `CM_CRAFT.runImpl` non-morph target validation | Non-morph craft with an out-of-range target resolves to the silent invalid-target plan. | Source-derived live dispatch regression. | Uses current C# world-visible object metadata, not a Java-equivalent static-object model. |
| `ProcessPacketAsync_CmCraftMorphBypassesMissingTargetAndRecordsStartIntent` | Regression Added | Java `CM_CRAFT.runImpl` `unk == 129` branch | Morph requests bypass target validation and preserve recipe, craft type, and material payload. | Source-derived live dispatch regression. | Does not execute downstream morph crafting. |

## Risks / Gaps

- C# still does not execute Java `CraftService.startCrafting`.
- `CraftingTask`, `SM_CRAFT_UPDATE` / `SM_CRAFT_ANIMATION` runtime scheduling, material consumption, DP spend, cooldown persistence, XP/reward handling, recipe deletion, craft logging, and quest/event callbacks remain outside this unit.
- C# lacks a first-class Java `StaticObject` world model for craft stations; target validation currently uses available `IWorldNpcObject` metadata as a conservative live-dispatch boundary.
- The shutdown-soon dependency is test-injected and still needs production `ShutdownHook` countdown integration.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped rows in this unit.
- Total artifacts ported: 1 live `CmCraft` dispatch shell, 1 shutdown-soon test seam, 1 target-range helper, and 4 socket-level regressions.
- Total artifacts with verified parity: 0 new grouped rows in this unit.
- Total artifacts needing verification: 3 grouped rows.
- Total blocked artifacts: full `CraftService.startCrafting` runtime, first-class static craft targets, and live `CraftingTask` scheduling.
- Estimated overall migration completion: Phase 6 remains about 73%.

## Next Recommended Unit of Work

- Port the smallest Java `CraftService.startCrafting` pre-task validation slice that can honestly run after the live dispatch shell, likely recipe/template lookup plus the early `checkCraft` null/in-progress/morph-target guard boundary before material mutation or scheduler start.
- Safe alternatives:
  - execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`
  - port Java `DropRegistrationService.calculateBoostDropRate`
  - return to the deferred `TemperingEffect.apply/endEffect` ownership surface only if a narrower deterministic slice becomes obvious

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionCraftTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1800-Completion.md`
- `docs/Phase-6-Session-1800-Handoff.md`
