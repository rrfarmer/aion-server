# Phase 6 Session 1797 Handoff - Plan Crafted Reward Mutation Boundary

Date: 2026-05-30
Unit of Work: UOW-1797
Status: Completed, pending commit

## What Changed

- Added Java `CRAFTED_ITEM` inventory add metadata to `SmInventoryAddItem`.
- Added `CraftService.CreateFinishRewardPlan(...)` to compose:
  - Java-shaped product selection
  - inventory add/merge planning
  - crafted add packet metadata for new rows
  - `IncreaseItemCollect` update packet metadata for merged rows
  - creator-name mutation on newly added weapon/armor rows
- Added focused tests for crafted reward packet type, creator-name ownership, stack-merge update semantics, and conservative full-inventory reporting.

## Files Changed

- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1797-Completion.md`
- `docs/Phase-6-Session-1797-Handoff.md`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryAddItem.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService`
- `com.aionemu.gameserver.services.item.ItemService`
- `com.aionemu.gameserver.services.item.ItemPacketService`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem`
- `Aion.GameServer.Services.CraftService`
- `Aion.GameServer.Tests.CraftServiceTests`
- `Aion.GameServer.Tests.GamePacketTests`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~InventoryAddServiceTests|FullyQualifiedName~GamePacketTests"`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag" --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate" --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`

Results:

- Focused craft/add-packet validation passed with 261 tests.
- The first full-suite run hit one unrelated `HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag` failure; its isolated rerun passed with 1 test.
- The second full-suite run hit one unrelated `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` failure; its isolated rerun passed with 1 test.
- The final full-suite rerun passed cleanly with 4794 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4587` game

## Parity Table Summary

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `ItemService.addItem` crafted reward path | `CraftService.CreateFinishRewardPlan` + `InventoryAddService` | Deterministic Inventory Mutation Planner | Partial | Unit Tested | Partial Parity | Crafted rewards now use Java-shaped add/merge semantics in a dedicated craft plan. Live runtime application is still missing. |
| `ItemPacketService.ItemAddType.CRAFTED_ITEM` | `SmInventoryAddItem.CraftedItem` + `CreateCraftedItem(...)` | Packet Metadata Surface | Complete | Regression Tested | Verified Parity | Java mask `0x2D` is now represented and packet-tested directly. |
| Java crafted equipment `changeItem(...)` hook | `CraftService.CreateFinishRewardPlan` creator-name application | Deterministic Mutation Intent | Partial | Unit Tested | Partial Parity | Creator-name mutation now applies only to newly added weapon/armor rows. |
| Java `INC_ITEM_COLLECT` crafted merge path | `CraftService.CreateFinishRewardPlan` update packet output | Packet / Update-Type Surface | Partial | Unit Tested | Partial Parity | Stack-merge updates now use `IncreaseItemCollect`. |

## Known Gaps

- No live `CraftingTask` completion/runtime path consumes the new reward plan yet.
- Recipe deletion, fail-craft quest hook, skill XP, player XP, craft log output, and craft cooldown persistence remain unported.
- The Java inventory-full system message side effect is still only represented as planner intent.

## Risks

- The next live crafting unit can widen quickly if task timing, XP, and cooldown persistence are attempted together.
- Because the crafted reward boundary now reuses shared inventory-add semantics, future failures around crafting vs other item-add paths should compare packet add types first before assuming shared add-plan regressions.

## Next Recommended Unit of Work

- Next sequential task: port the smallest live Java crafting runtime shell that can consume the new reward plan, ideally the `CraftingTask` completion boundary and packet send/application path before widening into XP grants or craft cooldown persistence.

Safe alternative candidates:

- Execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.
- Return to the deferred `TemperingEffect.apply/endEffect` ownership surface only if a narrower deterministic slice becomes obvious.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryAddItem.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before selecting the next UOW.
- Re-inspect Java `CraftingTask`, `CraftService.finishCrafting`, `ItemService.addItem`, and current C# inventory packet send patterns before wiring any live crafting runtime.
- Keep Java as source of truth and continue preferring bounded runtime shells over broad crafting-pipeline ports.
