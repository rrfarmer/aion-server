# Phase 6 Session 1803 Handoff - Craft Stance and Inventory Guards

Date: 2026-05-31
Unit of Work: UOW-1803
Status: Completed

## What Changed

- Extended `CraftService.CreateStartCraftingValidationPlan(...)` with Java current-stance and inventory-full guards.
- Added `CraftStartValidationStatus.InvalidCurrentStance` and `CraftStartValidationStatus.InventoryFull`.
- Added `FailurePacket` to `CraftStartValidationPlan` for planned Java system-message packets.
- Added `SmSystemMessage.CraftCannotCombineWhileInCurrentStance()` and `SmSystemMessage.CombineInventoryFull()`.
- Added focused tests for guard ordering and message IDs.

## Files Changed

- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1803-Completion.md`
- `docs/Phase-6-Session-1803-Handoff.md`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.model.actions.PlayerMode.RIDE`
- `com.aionemu.gameserver.model.gameobjects.Creature.isInAnyHide`
- `com.aionemu.gameserver.model.items.storage.Storage.isFull`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`

## C# Artifacts Touched

- `Aion.GameServer.Services.CraftService`
- `Aion.GameServer.Services.CraftStartValidationPlan`
- `Aion.GameServer.Services.CraftStartValidationStatus`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Tests.CraftServiceTests`
- `Aion.GameServer.Tests.GamePacketTests`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused craft/packet tests passed with 265 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4530 tests.

## Parity Table Summary

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.checkCraft` current-stance guard | `CraftService.CreateStartCraftingValidationPlan` `InvalidCurrentStance` branch | Validation Guard | Partial | Unit Tested | Partial Parity | Checks ride mode and any hide after DP guard and attaches message id `1300122`; live fanout remains pending. |
| `CraftService.checkCraft` inventory-full guard | `CraftService.CreateStartCraftingValidationPlan` `InventoryFull` branch | Validation Guard | Partial | Unit Tested | Partial Parity | Checks current C# cube capacity after stance guard and attaches message id `1330037`; Java storage internals remain broader than this planner. |
| `SM_SYSTEM_MESSAGE.STR_SKILL_CAN_NOT_COMBINE_WHILE_IN_CURRENT_STANCE` | `SmSystemMessage.CraftCannotCombineWhileInCurrentStance` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id `1300122` verified. |
| `SM_SYSTEM_MESSAGE.STR_COMBINE_INVENTORY_IS_FULL` | `SmSystemMessage.CombineInventoryFull` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id `1330037` verified. |

## Known Gaps

- No live `CraftService.startCrafting` execution.
- No live failure packet or cancel packet sending.
- Recipe ownership, cooldown, skill presence/level, material validation/consumption, bonus item consumption, DP spend, task interval, scheduler startup, and craft completion remain pending.
- Inventory fullness uses current C# capacity modeling rather than a complete Java `Storage` port.

## Risks

- Wiring live failure fanout must preserve Java ordering: system message from the failing guard, then `sendCancelCraft` from `startCrafting`.
- Recipe ownership and cooldown checks may require adding recipe-list and cooldown state to `Player` or planner facts without pretending the full Java containers are ported.
- Do not consume material mutation or bonus item decrement until all earlier guards are covered.

## Next Recommended Unit of Work

- Next sequential task: port recipe ownership and cooldown guard planning from `CraftService.checkCraft`, including system messages `STR_COMBINE_CAN_NOT_FIND_RECIPE` and `STR_ITEM_CANT_USE_UNTIL_DELAY_TIME`.

Safe alternative candidates:

- Wire non-live validation failure orchestration that combines `FailurePacket` and cancel packet plans without live sending.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Execute the opt-in MySQL logout delete/retuning persistence path with `AION_GAMESERVER_DB_INTEGRATION=1`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before choosing the next UOW.
- Re-inspect Java `CraftService.checkCraft`, `RecipeList.isRecipePresent`, `CraftCooldowns.hasCooldown`, and `SM_SYSTEM_MESSAGE`.
- Keep the remaining start-craft work planner-level until live failure fanout and mutation boundaries are explicit.
