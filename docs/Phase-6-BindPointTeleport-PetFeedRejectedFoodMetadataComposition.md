# Phase 6 - Pet Feed Rejected-Food Metadata Composition

Date: May 27, 2026
Unit of Work: UOW-1332

## Scope

This unit adds end-to-end non-live tests for rejected-food metadata composition:

`PetFeedServiceOperationPlan` -> `PetFeedUnlockPacketContextAssembler` -> `PetFeedSupplementalPacketContext` -> `PetFeedPacketMetadataBridge`.

Java source of truth:

- `com.aionemu.gameserver.services.toypet.PetService.checkFeeding`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket`

## Implemented

- Added `PetFeedRejectedFoodMetadataCompositionTests`.
- Covered representative storage paths:
  - cube item unlock
  - regular warehouse item unlock
  - account warehouse item unlock
  - legion warehouse item unlock
  - legion warehouse kinah unlock
  - unsupported known storage id remains blocked
- Verified non-live bridge metadata order: unlock packet sequence before `SmPet` subtype `5`, then `SmEmotion`, then rejected-food `SmSystemMessage`.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedRejectedFoodMetadataComposition|PetFeedUnlockPacketContextAssembler|PetFeedPacketMetadataBridge"` passed 25 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedRejectedFoodMetadataComposition|PetFeedUnlockPacketContextAssembler|PetFeedPacketMetadataBridge|PetFeedServiceOperationPlanner|PetFeedEvaluation|PetFeedXmlProjection|PetFoodTypeLookup|PetFeedPlanner|PetFeedCalculator|PetFeedProgress|PetHungryLevel|SmPet|SmEmotion|SmSystemMessage|SmInventoryAddItem|SmWarehouseAddItem|SmLegionEdit|SmCubeUpdate"` passed 139 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed with 0 warnings and 0 errors.

## Migration Parity Table - UOW-1332

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` rejected-food packet order | `Aion.GameServer.Tests.PetFeedRejectedFoodMetadataCompositionTests` + `PetFeedPacketMetadataBridge` | Test / Packet Metadata Composition | Partial | Unit Tested | Partial Parity | End-to-end non-live tests cover assembler-to-bridge composition for modeled storage families. No live packet dispatch, inventory mutation, or Java runtime packet capture exists. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `PetFeedUnlockPacketContextAssembler` + `PetFeedPacketMetadataBridge` | Test / Context Composition | Partial | Unit Tested | Partial Parity | Tests confirm supplied item location snapshots can feed unlock metadata construction before pet/end/system packets. Live `StorageType` lookup and storage ownership are not wired. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` storage packet families | `SmInventoryAddItem`, `SmWarehouseAddItem`, `SmLegionEdit`, `SmCubeUpdate` through bridge results | Test / Packet Family Composition | Partial | Unit Tested | Partial Parity | Composition tests assert packet family types and order, not full Java runtime bytes. Deep item blob parity remains separately unverified. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Compose_RejectedFoodStorageUnlockContextFlowsIntoBridgeBeforePetPacketsLikeJava` | Unit | `PetService.checkFeeding` rejected-food order; `ItemPacketService.sendStorageUpdatePacket` | Cube, regular warehouse, account warehouse, and legion item contexts produce unlock packet sequence before `SmPet`, `SmEmotion`, and system message. | Source-derived type/order assertions. | No live packet send or Java runtime byte capture. |
| `Compose_RejectedFoodLegionKinahContextUsesLegionEditBeforePetPacketsLikeJava` | Unit | Java legion warehouse kinah branch | Legion kinah context produces `SmLegionEdit` followed by `SmCubeUpdate` before rejected-food pet/emotion/system metadata. | Source-derived type/order assertions. | No live legion object or kinah hydration. |
| `Compose_RejectedFoodUnsupportedStorageKeepsUnlockBlockedWithoutGuessing` | Unit | Java storage-family boundary | Unsupported known storage ids keep unlock blocked while remaining rejected-food packet metadata can still be built from supplied context. | Explicit blocked-boundary assertion. | Pet/house storage unlock behavior remains unported. |

## Remaining Risks

- Tests validate composition order/types, not Java runtime bytes.
- Live storage lookup, item-template lookup, storage ownership, and mutation are not wired.
- Unsupported pet-bag/house storage paths remain blocked.
- Subtype `7` live timing remains blocked by Java mutable queued packet behavior.
- No socket dispatch, scheduler, reward creation, DAO writes, or Java runtime packet comparison is enabled.

## Next Recommended Unit of Work

Create a Java subtype `7` runtime-vector design note from the completed audit, including exact scenarios to capture before enabling live C# feed dispatch timing. Keep this as documentation/design unless Java runtime tooling is available.
