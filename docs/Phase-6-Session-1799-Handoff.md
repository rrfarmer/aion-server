# Phase 6 Session 1799 Handoff - Add CM_CRAFT Runtime Plan Foundation

Date: 2026-05-31
Unit of Work: UOW-1799
Status: Completed, pending commit

## What Changed

- Added Java-shaped `CmCraft` client packet parsing for:
  - `unk`
  - `targetTemplateId`
  - `recipeId`
  - `targetObjId`
  - `materialsCount`
  - `craftType`
  - repeated material `(itemId, count)` pairs
- Registered `CmCraft` in `GameClientPacketFactory`:
  - opcode `141`
  - `InGame` only
- Added non-live `CmCraftRuntimePlanService` for:
  - silent missing-player / unspawned-player return
  - silent shutdown-soon return
  - Java non-morph static-target validation
  - Java morph-substance bypass when `unk == 129`
  - start intent carrying recipe id, target object id, craft type, and materials
- Added focused tests:
  - `CmCraftTests`
  - `CmCraftRuntimePlanServiceTests`

## Files Changed

- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1799-Completion.md`
- `docs/Phase-6-Session-1799-Handoff.md`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmCraft.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmCraftRuntimePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftRuntimePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftTests.cs`

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_CRAFT`
- `com.aionemu.gameserver.services.craft.CraftService`
- `com.aionemu.gameserver.skillengine.task.CraftingTask`
- `com.aionemu.gameserver.skillengine.task.AbstractCraftTask`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmCraft`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Services.CmCraftRuntimePlanService`
- `Aion.GameServer.Tests.CmCraftTests`
- `Aion.GameServer.Tests.CmCraftRuntimePlanServiceTests`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraftTests|FullyQualifiedName~CmCraftRuntimePlanServiceTests|FullyQualifiedName~GamePacketTests"`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`

Results:

- Focused `CM_CRAFT` packet/planner validation passed with 250 tests.
- The first full-suite attempt hit the command timeout boundary.
- The second full-suite rerun passed cleanly with 4807 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4600` game

## Parity Table Summary

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_CRAFT.readImpl` | `CmCraft` | Packet / Parsing | Complete | Regression Tested | Verified Parity | Java field order and repeated material-pair parsing are directly represented and tested. |
| `AionClientPacketFactory` `CM_CRAFT` registration | `GameClientPacketFactory` opcode `141` registration | Packet Factory / Registration | Complete | Regression Tested | Verified Parity | `CmCraft` is now registered at opcode `141` as `InGame` only. |
| `CM_CRAFT.runImpl` pre-`CraftService.startCrafting` guard flow | `CmCraftRuntimePlanService.CreatePlan` | Deterministic Runtime Planner | Partial | Unit Tested | Partial Parity | Silent guard order and morph bypass are represented, but there is still no live dispatch or `CraftService.startCrafting` call. |

## Known Gaps

- No live `CmCraft` dispatch in `GameServerConnection`.
- No `CraftService.startCrafting` parity surface yet for the larger guard, material, DP, cooldown, and scheduler path.
- No live packet or side-effect evidence for Java `CM_CRAFT` silent-return behavior.

## Risks

- The next live craft unit can widen quickly if it mixes packet dispatch, world target lookup, material consumption, and scheduler timing.
- The reviewed Java `CraftService.startCrafting` path has many guard branches, so the safest next step is to keep `GameServerConnection` dispatch narrow and consume the existing planner first.

## Next Recommended Unit of Work

- Next sequential task: port the narrow live `CmCraft` dispatch shell in `GameServerConnection`, consuming `CmCraftRuntimePlanService` and preserving the silent Java guard behavior before widening into `CraftService.startCrafting`.

Safe alternative candidates:

- Execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.
- Return to the deferred `TemperingEffect.apply/endEffect` ownership surface only if a narrower deterministic slice becomes obvious.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmCraft.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmCraftRuntimePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftRuntimePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before selecting the next UOW.
- Re-inspect Java `CM_CRAFT`, `CraftService.startCrafting`, and `CraftingTask` before touching live runtime code.
- Use `CmCraftRuntimePlanService` as the current Java guard-order oracle and keep the next live dispatch slice narrow.
