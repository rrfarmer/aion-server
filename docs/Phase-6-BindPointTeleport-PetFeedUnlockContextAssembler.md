# Phase 6 - Pet Feed Unlock Context Assembler

Date: May 27, 2026
Unit of Work: UOW-1331

## Scope

This unit adds a non-live assembler for rejected-food unlock packet context. It maps supplied item location and storage snapshots into the `PetFeedUnlockPacketContext` consumed by the packet metadata bridge.

Java source of truth:

- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket`
- `com.aionemu.gameserver.model.items.storage.StorageType.getStorageTypeById`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket`

## Implemented

- Added `PetFeedUnlockPacketContextAssembler`.
- Added assembler input/result DTOs and status enum.
- Mapped Java storage ids:
  - `0` -> cube
  - `1` -> regular warehouse
  - `2` -> account warehouse
  - `3` -> legion warehouse
- Preserved Java no-send boundary for unknown storage ids.
- Kept Java-known but unmodeled pet bag, house storage, broker, and mailbox ids unsupported instead of guessing packet shape.
- Detected legion warehouse kinah by supplied item id `182400001` and allowed context creation without item-template snapshot for the Java `SM_LEGION_EDIT` special case.
- Required item-template snapshots for all non-kinah unlock packet metadata.

## Not Implemented

- No live `StorageType` object model.
- No live player inventory/warehouse/legion lookup.
- No live item-template lookup.
- No live packet send.
- No storage mutation.
- No Java runtime packet comparison.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedUnlockPacketContextAssembler|PetFeedPacketMetadataBridge"` passed 19 tests.

## Migration Parity Table - UOW-1331

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `Aion.GameServer.Services.ToyPet.PetFeedUnlockPacketContextAssembler.Assemble` | Context Assembler / Service Boundary | Partial | Unit Tested | Partial Parity | Maps supplied item location ids to the modeled unlock packet context families without live sends. Unknown ids return no context to match Java's null `StorageType` no-send behavior. |
| `com.aionemu.gameserver.model.items.storage.StorageType` | `PetFeedUnlockPacketContextAssembler.GetStorageKind` | Enum / Mapping Boundary | Partial | Unit Tested | Partial Parity | Supports cube, regular warehouse, account warehouse, and legion warehouse ids. Pet bag, house storage, broker, and mailbox ids are recognized as unsupported because packet behavior is not modeled in this slice. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` legion warehouse kinah branch | `PetFeedUnlockPacketContextAssembler` + `PetFeedUnlockPacketContext.IsKinah` | Context Assembler | Partial | Unit Tested | Partial Parity | Detects legion warehouse kinah by item id `182400001` and carries supplied legion warehouse kinah amount for `SmLegionEdit.WarehouseKinah`. Java `ItemTemplate.isKinah()` is not live-hydrated. |
| `com.aionemu.gameserver.model.gameobjects.Item.getItemTemplate` | supplied `ItemTemplateSummary` in `PetFeedUnlockPacketContextAssemblerInput` | Dependency Boundary | Not Started | Unit Tested as missing snapshot | Needs Verification | Non-kinah packet context requires a supplied template snapshot. Live `DataManager.ITEM_DATA` lookup is not wired. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Assemble_CubeLocationCreatesCubeContextWithSuppliedSnapshots` | Unit | `StorageType.CUBE`, `sendItemUnlockPacket` | Cube item location creates cube unlock context with supplied cube count/expands. | Source-derived mapping assertion. | No live inventory snapshot. |
| `Assemble_WarehouseLocationsMapJavaStorageIds` | Unit | `StorageType` ids | Storage ids `1`, `2`, and `3` map to regular/account/legion warehouse context kinds. | Source-derived mapping assertion. | No live storage object lookup. |
| `Assemble_LegionWarehouseKinahDoesNotRequireTemplateLikeJavaSpecialCase` | Unit | `sendStorageUpdatePacket` legion kinah branch | Legion kinah context can be created without a template and carries supplied warehouse kinah. | Source-derived special-case assertion. | Uses item id rather than live `ItemTemplate.isKinah()`. |
| `Assemble_MissingTemplateBlocksNonKinahPacketContext` | Unit | `SM_INVENTORY_ADD_ITEM` / `SM_WAREHOUSE_ADD_ITEM` item-template dependency | Non-kinah context creation blocks without a supplied template snapshot. | Boundary assertion. | No live template lookup. |
| `Assemble_UnsupportedKnownStorageLocationsDoNotGuessPacketShape` | Unit | `StorageType` pet-bag/house ids | Known but unmodeled storage ids stay unsupported. | Explicit no-guess assertion. | Pet/house storage unlock packet behavior remains unported. |
| `Assemble_UnknownStorageLocationMatchesJavaNoSendBoundary` | Unit | `StorageType.getStorageTypeById` null branch | Unknown storage ids produce no context, matching Java no-send behavior. | Source-derived no-send assertion. | No Java runtime comparison. |

## Remaining Risks

- Live item-template, player storage, account storage, and legion storage hydration are still missing.
- The assembler does not execute Java's `StorageType` enum; it mirrors only the ids needed by the modeled packet families.
- Pet bag and house-storage unlock behavior is explicitly unsupported.
- Kinah detection uses the canonical kinah item id instead of live Java `ItemTemplate.isKinah()`.
- Snapshot counts/expands/kinah can be stale if captured at the wrong point in a future live adapter.
- No socket dispatch, storage mutation, scheduler, reward creation, DAO writes, or Java runtime packet comparison is enabled.

## Next Recommended Unit of Work

Use the assembler output in an end-to-end non-live rejected-food metadata composition test: operation plan -> unlock context assembler -> supplemental packet context -> packet metadata bridge. Keep live storage lookup, mutation, packet send, scheduler, DAO, reward creation, and Java runtime parity claims disabled.
