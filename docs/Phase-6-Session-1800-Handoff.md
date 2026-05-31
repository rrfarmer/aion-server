# Phase 6 Session 1800 Handoff - Live CM_CRAFT Dispatch Shell

Date: 2026-05-31
Unit of Work: UOW-1800
Status: Completed

## What Changed

- Added live `CmCraft` dispatch in `GameServerConnection`.
- The dispatch now consumes `CmCraftRuntimePlanService` for:
  - silent missing-player / not-spawned return
  - silent shutdown-soon return before target validation
  - non-morph target existence/range/template validation
  - morph-substance target bypass when `unk == 129`
  - deferred `StartCrafting` intent observation
- Added socket-level `GameServerConnectionCraftTests`.
- Did not port Java `CraftService.startCrafting` execution yet.

## Files Changed

- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1800-Completion.md`
- `docs/Phase-6-Session-1800-Handoff.md`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionCraftTests.cs`

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_CRAFT`
- `com.aionemu.gameserver.services.craft.CraftService.startCrafting`
- `com.aionemu.gameserver.GameServer.isShuttingDownSoon`
- `com.aionemu.gameserver.utils.PositionUtil`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.CmCraftRuntimePlanService`
- `Aion.GameServer.Tests.GameServerConnectionCraftTests`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionCraftTests|FullyQualifiedName~CmCraftTests|FullyQualifiedName~CmCraftRuntimePlanServiceTests"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionCraftTests|FullyQualifiedName~CmCraftTests|FullyQualifiedName~CmCraftRuntimePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`

Results:

- Focused craft dispatch/packet/planner validation passed with 13 tests.
- Full .NET suite passed with 4811 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4604` game

## Parity Table Summary

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_CRAFT.runImpl` live dispatch guard shell | `GameServerConnection.HandleCraftAsync` + `CmCraftRuntimePlanService.CreatePlan` | Client Packet Dispatch / Runtime Guard | Partial | Regression Tested | Partial Parity | Live C# dispatch reaches the planner and preserves key silent guard branches, but does not call `CraftService.startCrafting`. |
| `GameServer.isShuttingDownSoon` guard precedence | `GameServerConnection` injected shutdown-soon gate | Runtime Guard | Partial | Regression Tested | Partial Parity | Socket-level regression proves guard ordering; production `ShutdownHook` countdown is not wired yet. |
| `PositionUtil.isInRange(player, staticObject, 10)` pre-start guard | `GameServerConnection.IsInCraftTargetRange` | Range Check | Partial | Regression Tested | Partial Parity | Uses strict squared-distance and same world/instance checks; first-class Java `StaticObject` parity remains pending. |

## Known Gaps

- No live `CraftService.startCrafting` execution.
- No first-class C# `StaticObject` craft-station model.
- No material consumption, DP spend, cooldown persistence, `CraftingTask` scheduling, XP/reward, recipe deletion, craft logging, or quest/event callbacks.
- Shutdown-soon is test-injected but not wired to production `ShutdownHook`.

## Risks

- The next start-craft unit can expand quickly because Java `checkCraft` combines data lookup, target semantics, inventory/material checks, DP, player modes, recipe ownership, cooldowns, and skill levels.
- Static craft station parity should remain conservatively reported until the C# world model can distinguish Java `StaticObject` from NPC-like metadata.

## Next Recommended Unit of Work

- Next sequential task: port the smallest Java `CraftService.startCrafting` pre-task validation slice, likely recipe/product-template lookup plus early `checkCraft` null/in-progress/morph-target guard planning before material mutation or scheduler start.

Safe alternative candidates:

- Execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.
- Return to the deferred `TemperingEffect.apply/endEffect` ownership surface only if a narrower deterministic slice becomes obvious.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmCraftRuntimePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionCraftTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftRuntimePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before selecting the next UOW.
- Re-inspect Java `CM_CRAFT`, `CraftService.startCrafting`, `checkCraft`, `CraftingTask`, and `AbstractCraftTask`.
- Keep the next slice below scheduler/material mutation unless the required C# dependencies are already clearly present.
