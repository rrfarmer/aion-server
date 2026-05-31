# Phase 6 Session 1807 Completion - Add Craft Bonus Item Guard Planning

Date: 2026-05-31
Unit of Work: UOW-1807
Status: Complete

## Scope

Port the Java `CraftService.checkCraft` bonus craft item requirement guard at planner level. This unit intentionally does not consume the bonus item, consume materials, spend DP, start scheduler work, send live packets, or complete crafting.

## Completed Work

- Added optional `craftType` input to `CraftService.CreateStartCraftingValidationPlan(...)`.
- Added Java `CraftService.getBonusReqItem(skillId)` mapping for craft skills `40001`, `40002`, `40003`, `40004`, `40007`, `40008`, and `40010`.
- Added `CraftStartValidationStatus.MissingBonusItem`.
- Reused missing-component evidence fields to report the required bonus item id/count.
- Planned Java `STR_COMBINE_NO_COMPONENT_ITEM_SINGLE` failure when `craftType == 1` and the bonus item is missing.
- Added focused tests proving material validation runs before the bonus guard, missing bonus item behavior, and ready continuation when the bonus item exists.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused craft/packet tests passed with 272 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4537 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.services.craft.CraftService.getBonusReqItem`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_COMBINE_NO_COMPONENT_ITEM_SINGLE`

## Migration Parity Table - UOW-1807

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.checkCraft` bonus item guard | `CraftService.CreateStartCraftingValidationPlan` `MissingBonusItem` branch | Validation Guard | Partial | Unit Tested | Partial Parity | C# checks missing bonus item after material validation when `craftType == 1`, but does not consume the item. |
| `CraftService.getBonusReqItem` | `CraftService.GetBonusRequiredItemId` | Mapping | Complete | Unit Tested | Verified Parity | Java skill-to-item mapping ported for known craft skills. |
| `SM_SYSTEM_MESSAGE.STR_COMBINE_NO_COMPONENT_ITEM_SINGLE` reuse | `SmSystemMessage.CombineNoComponentItemSingle` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id was verified in UOW-1806 and reused by this branch. |

## Risks / Gaps

- No live `CraftService.startCrafting` execution.
- The Java branch consumes the bonus item as part of the check; C# only plans the missing-item failure and does not mutate inventory.
- No live failure packet or cancel packet sending.
- Material consumption, DP spend, task interval, scheduler startup, and craft completion remain pending.

## Next Recommended Unit of Work

- Port non-live validation failure orchestration that combines `FailurePacket` and cancel packet plans in Java order, still without sending live packets.
- Safe alternatives:
  - start material and bonus item consumption planning
  - start live CM_CRAFT selected-material/craft-type adapter work
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1807-Completion.md`
- `docs/Phase-6-Session-1807-Handoff.md`
