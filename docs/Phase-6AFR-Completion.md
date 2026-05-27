# Phase 6AFR Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1338
Latest Commit: included in the UOW-1338 unit commit
Status: Full Java house-storage id coverage is now explicit for rejected-food unlock boundaries; live pet/house/broker/mailbox unlocks remain disabled.

## What Changed

- Added `docs/Phase-6-BindPointTeleport-PetFeedFullHouseStorageCoverage.md`.
- Expanded `PetFeedUnlockPacketContextAssemblerTests.Assemble_UnsupportedKnownStorageLocationsDoNotGuessPacketShape`.
- Added every missing Java house storage id:
  - `62` through `67`
  - `69` through `73`
  - `75` through `78`
- The unsupported theory now covers:
  - pet bag ids `32` through `43`
  - all house cabinet ids `60` through `79`
  - broker id `126`
  - mailbox id `127`
- Integrated read-only sidecar findings for the next live-adapter design:
  - Java queues add/unlock packet metadata first.
  - Java queues `SM_CUBE_UPDATE.cubeSize` second.
  - `SM_CUBE_UPDATE.cubeSize` only fills real counts/expands for cube, regular warehouse, and legion warehouse.
  - Account warehouse, pet bags, house storage, broker, and mailbox keep zero counts/expands while still writing `StorageType.ordinal()`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Added follow-up notes to `docs/Phase-6-BindPointTeleport-PetFeedUnsupportedStorageAudit.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

## Code Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedUnlockPacketContextAssemblerTests.cs`

## Documentation Changed

- `docs/Phase-6-BindPointTeleport-PetFeedFullHouseStorageCoverage.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-PetFeedUnsupportedStorageAudit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AFR-Completion.md`

## Validation Completed

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedUnlockPacketContextAssembler"` passed 41 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedSubtype7JavaVectorArtifactReader|PetFeedRejectedFoodMetadataComposition|PetFeedUnlockPacketContextAssembler|PetFeedPacketMetadataBridge|PetFeedServiceOperationPlanner|PetFeedEvaluation|PetFeedXmlProjection|PetFoodTypeLookup|PetFeedPlanner|PetFeedCalculator|PetFeedProgress|PetHungryLevel|SmPet|SmEmotion|SmSystemMessage|SmInventoryAddItem|SmWarehouseAddItem|SmLegionEdit|SmCubeUpdate"` passed 174 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed with 0 warnings and 0 errors.

No live storage lookup, inventory mutation, packet send, live item/template/player/account/legion/house/pet hydration, scheduler execution, reward item creation, DAO write, or Java runtime packet comparison was enabled.

## Migration Parity Table - UOW-1338

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.StorageType` house storage ids `60` through `79` | `Aion.GameServer.Services.ToyPet.PetFeedUnlockPacketContextAssembler.GetStorageKind`; `Aion.GameServer.Tests.PetFeedUnlockPacketContextAssemblerTests` | Enum / Mapping Boundary | Partial | Unit Tested | Partial Parity | All Java house cabinet ids are now tested as known but unsupported. C# intentionally blocks packet context rather than guessing generic warehouse behavior without house ownership/runtime bytes. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `Aion.GameServer.Services.ToyPet.PetFeedUnlockPacketContextAssembler.Assemble` | Service Boundary | Partial | Unit Tested | Partial Parity | Java null-storage sends nothing; known unsupported house ids now consistently return `UnsupportedStorageLocation`. No live send or runtime comparison exists. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` default branch | future pet/house/broker/mailbox unlock adapter | Packet Service / Unsupported Branch | Not Started | Manual Only | Needs Verification | Java would send `SM_WAREHOUSE_ADD_ITEM` followed by `SM_CUBE_UPDATE` for house ids. C# still blocks until live house storage context and Java bytes exist. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | future pet/house/broker/mailbox unlock adapter | Packet / Future Runtime Comparison | Not Started | Manual Only | Needs Verification | Java serializes item/template/blob/slot using the supplied item and storage id. Existing C# modeled warehouse families do not prove house-storage behavior. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` unusual storage fallback | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`; future live adapter | Packet / Size Snapshot | Partial | Unit Tested for modeled families | Needs Verification | Java zero-fills counts/expands for account warehouse, pet bags, house storage, broker, and mailbox but uses `StorageType.ordinal()`. Future adapter must preserve this unless an intentional difference is documented. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Assemble_UnsupportedKnownStorageLocationsDoNotGuessPacketShape` | Unit | `StorageType` id range and `sendItemUnlockPacket` source review | Every Java house storage id `60` through `79`, plus pet bags/broker/mailbox, returns `UnsupportedStorageLocation` with no packet context. | Source-derived conservative boundary assertion. | Does not execute Java default warehouse branch, compare runtime bytes, or model live house ownership. |

## Remaining Risks

- Java would still route known house ids through the generic warehouse add/update branch; C# intentionally blocks this until runtime context exists.
- `SM_CUBE_UPDATE.cubeSize` uses `StorageType.ordinal()` rather than the storage id, so future unusual-storage packet metadata needs ordinal parity checks.
- Java packet objects hold item/player references; without runtime artifacts, serialization-time mutation risk remains unverified.
- Taking live storage counts too early could produce stale cube-size metadata; taking item fields too late could serialize mutated item state.
- Account warehouse zero-size behavior is Java-compatible for this source but still needs future packet-byte comparison.
- Live storage lookup, mutation, packet dispatch, scheduler execution, reward creation, DAO writes, and socket dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 expanded test theory now covering 34 known unsupported storage ids
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: house storage live context, generic warehouse runtime bytes for house ids, unusual-storage cube-update ordinal comparison, live storage lookup/mutation/dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

# Next Work Options

## Recommended Sequential Task

- Task: Add a docs-only warehouse live-adapter capture design for rejected-food unlocks.
- Why: The sidecar audit identified the next correctness boundary without requiring risky Java network-core changes. A design unit should lock down snapshot timing, ordinal-vs-id behavior, zero-count unusual storage updates, and no-yield ordering before live adapter code exists.
- Files:
  - `docs/Phase-6-BindPointTeleport-PetFeedWarehouseLiveAdapterCaptureDesign.md` or similar
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
  - `docs/PHASE-6-PROGRESS.md`
  - next completion handoff

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Warehouse live-adapter capture design | docs only | Low | Use UOW-1338 sidecar findings; no code changes required. |
| B | Inert Java serialization observer hook | Java network core + new observer classes | High | Exclusive owner only; do not batch with other Java network writes. |
| C | Pet/house storage runtime byte vector design | docs only | Medium | Useful after capture design names schema fields for unusual storage. |
| D | Account warehouse zero-cube-update byte comparator follow-up | C# tests/artifact reader docs | Medium | Needs careful opcode/body comparison; no Java artifacts yet. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Verify `StorageType.ordinal()` values for cube/account/house/pet/broker/mailbox and summarize expected `SM_CUBE_UPDATE` action values | Read-only Java/C# inspection | All writes |
| Orchestrator | Draft warehouse live-adapter capture design and update shared docs | Docs listed in recommended task | Java network core, C# production/test files unless new evidence requires a separate unit |

## Do Not Parallelize

- Java network core files (`AionServerPacket.java`, `AionConnection.java`): observer hook must be exclusive.
- `PetFeedUnlockPacketContextAssembler.cs` and `PetFeedPacketMetadataBridge.cs`: one writer only if moving from design to implementation.
- Shared progress/handoff docs: orchestrator-owned only.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/model/items/storage/StorageType.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_WAREHOUSE_ADD_ITEM.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedUnlockPacketContextAssembler.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedUnlockPacketContextAssemblerTests.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedPacketMetadataBridge.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnsupportedStorageAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedFullHouseStorageCoverage.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
