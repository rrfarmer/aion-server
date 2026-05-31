# Phase 6 Session 1806 Handoff - Craft Component Material Planning

Date: 2026-05-31
Unit of Work: UOW-1806
Status: Completed

## What Changed

- Added recipe component group records and static XML projection for Java `components_data/component`.
- Extended `CraftService.CreateStartCraftingValidationPlan(...)` with selected material group validation.
- Added `CraftStartValidationStatus.MissingComponentItem` and missing component evidence fields.
- Added `SmSystemMessage.CombineNoComponentItemSingle()` and `SmSystemMessage.CombineNoComponentItemMultiple()`.
- Added focused tests for skill-before-material ordering, selected group behavior, message IDs, and real static recipe component loading.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused craft/packet/static-data tests passed with 291 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4536 tests.

## Known Gaps

- No live `CraftService.startCrafting` execution.
- No live failure packet or cancel packet sending.
- No material or bonus item consumption.
- No live CM_CRAFT selected-material adapter.
- DP spend, task interval, scheduler startup, and craft completion remain pending.

## Next Recommended Unit of Work

- Next sequential task: port the bonus craft item requirement guard from Java `CraftService.checkCraft`, including `getBonusReqItem(skillId)` and missing bonus item message planning, still without consuming inventory.

Safe alternative candidates:

- Wire non-live validation failure orchestration that combines `FailurePacket` and cancel packet plans without live sending.
- Start live CM_CRAFT selected-material data adapter work.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/RecipeTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before choosing the next UOW.
- Re-inspect Java `CraftService.checkCraft`, `CraftService.getBonusReqItem`, bonus craft types, and the live CM_CRAFT packet shape.
- Keep actual inventory mutation out of scope until live validation failure fanout and selected-material parsing are explicit.
