# Phase 6 Session 1797 Completion - Plan Crafted Reward Mutation Boundary

Date: 2026-05-30
Unit of Work: UOW-1797
Status: Complete

## Scope

Port the next smallest Java `CraftService.finishCrafting` slice by modeling the crafted reward inventory mutation boundary: crafted add packet metadata, stack-merge update semantics, and creator-name mutation on newly added equipment rows.

## Completed Work

- Updated `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryAddItem.cs`:
  - added Java `ItemAddType.CRAFTED_ITEM` mask `0x2D`
  - added `CreateCraftedItem(...)`
- Updated `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`:
  - added `CreateFinishRewardPlan(...)`
  - composed the existing product-selection plan with `InventoryAddService.CreateAddItemPlan(...)`
  - emitted crafted-item add packets for new rows and `IncreaseItemCollect` update packets for merged rows
  - applied Java creator-name mutation only to newly added weapon/armor rows
  - surfaced conservative `InventoryFull` / `PartialOverflow` statuses plus a `ShouldSendInventoryFullMessage` intent flag
- Updated `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`:
  - added crafted equipment add/creator/packet coverage
  - added stack-merge update coverage
  - added partial merge + full inventory coverage
  - added missing reward template coverage
- Updated `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`:
  - added crafted-item packet serialization coverage for add type `0x2D` and cleanup-seal flag retention

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~InventoryAddServiceTests|FullyQualifiedName~GamePacketTests"`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag" --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate" --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`

Result:

- Focused craft/add-packet validation passed with 261 tests.
- The first full-suite attempt failed in unrelated `HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag`; that test then passed in isolation with 1 test.
- The second full-suite attempt failed in unrelated `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate`; that test then passed in isolation with 1 test.
- The final full-suite rerun passed cleanly with 4794 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4587` game

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService`
- `com.aionemu.gameserver.services.item.ItemService`
- `com.aionemu.gameserver.services.item.ItemPacketService`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM`

## Migration Parity Table - UOW-1797

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `ItemService.addItem` crafted reward path | `CraftService.CreateFinishRewardPlan` + `InventoryAddService` | Deterministic Inventory Mutation Planner | Partial | Unit Tested | Partial Parity | The planner now reuses Java-shaped add-item semantics for crafted rewards, including stack merge/new-row behavior and overflow reporting. Live craft runtime application is still missing. |
| `ItemPacketService.ItemAddType.CRAFTED_ITEM` | `SmInventoryAddItem.CraftedItem` + `CreateCraftedItem(...)` | Packet Metadata Surface | Complete | Regression Tested | Verified Parity | Java mask `0x2D` and packet shape were reviewed and regression-tested through direct serialization. |
| Java crafted equipment `changeItem(...)` hook inside `finishCrafting` | `CraftService.CreateFinishRewardPlan` creator-name application | Deterministic Mutation Intent | Partial | Unit Tested | Partial Parity | Creator-name mutation now applies only to newly added weapon/armor rows, matching the Java placement on newly created items. |
| Java `INC_ITEM_COLLECT` merge path during crafted add | `CraftService.CreateFinishRewardPlan` update-packet output | Packet / Update-Type Surface | Partial | Unit Tested | Partial Parity | Stack merges emit `IncreaseItemCollect`, matching the Java update predicate behavior. Live runtime ordering is still unverified. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreateFinishRewardPlan_AddsCraftedEquipmentWithCreatorAndCraftedAddPacket` | New crafted equipment rows receive creator-name ownership and serialize as crafted-item add packets. | Java `CraftService.finishCrafting`, `ItemService.addItem`, and `changeItem(...)` | Unit | No live craft dispatch. |
| `CreateFinishRewardPlan_MergesStackUsingIncreaseItemCollectUpdate` | Crafting into an existing stack emits an update packet instead of a crafted add packet. | Java `ItemService.addItem` stackable merge branch | Unit | No runtime persistence. |
| `CreateFinishRewardPlan_ReportsInventoryFullAndPreservesPartialMerge` | Partial merges survive inventory-full overflow and are reported conservatively. | Java `ItemService.addItem` remaining-count/full-inventory behavior | Unit | Does not yet prove the Java system-message side effect. |
| `CreateFinishRewardPlan_ReportsMissingItemTemplate` | Missing reward template does not emit packets and remains conservative. | Java precondition review around item-template lookup | Unit | Not a direct malformed-data runtime clone. |
| `SmInventoryAddItem_CraftedItemWritesCraftedAddTypeAndCleanupSealFlagLikeJava` | Crafted-item packets serialize add type `0x2D` and preserve cleanup-seal metadata. | Java `ItemPacketService` + `SM_INVENTORY_ADD_ITEM` | Regression | Does not prove full craft runtime ordering. |

## Risks / Gaps

- C# still lacks the live `CraftingTask` completion/runtime application path.
- Recipe deletion, fail-craft quest hook, skill XP, player XP, craft log output, and craft cooldown persistence remain unported.
- The planner now exposes `ShouldSendInventoryFullMessage`, but the Java `STR_MSG_DICE_INVEN_ERROR` packet is not yet wired in a live crafting runtime.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows in this unit.
- Total artifacts ported: 1 craft reward planner, 1 crafted packet metadata surface, 1 creator mutation branch, and 5 focused tests/regressions.
- Total artifacts with verified parity: 1 grouped row.
- Total artifacts needing verification: 3 grouped rows.
- Total blocked artifacts: live `CraftingTask` runtime wiring and remaining `finishCrafting` XP/cooldown/log branches.
- Estimated overall migration completion: Phase 6 remains about 73%.

## Next Recommended Unit of Work

- Port the smallest live Java crafting runtime shell that can consume the new reward plan, most likely the `CraftingTask` completion boundary and packet send/application path before widening into XP grants or craft cooldown persistence.
- Safe alternatives if a different isolated slice is preferred:
  - execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`
  - `DropRegistrationService.calculateBoostDropRate`
  - return to the deferred `TemperingEffect.apply/endEffect` ownership surface only if a narrower deterministic slice becomes obvious

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryAddItem.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1797-Completion.md`
- `docs/Phase-6-Session-1797-Handoff.md`
