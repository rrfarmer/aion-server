# Phase 6 Session 2638 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2638: Execute active pet doping add/remove live. See
[Phase-6-Session-2638-Completion.md](Phase-6-Session-2638-Completion.md).

## Commits Made

- `f50f024` - `[Phase 6][UOW-2627] Restore owned pets during enter-world`
- `f071686` - `[Phase 6][UOW-2628] Send restored pet list during enter-world`
- `f46d5d8` - `[Phase 6][UOW-2629] Execute active pet dismiss live`
- `0650121` - `[Phase 6][UOW-2630] Execute owned pet surrender live`
- `74b48ab` - `[Phase 6][UOW-2631] Execute active pet rename live`
- `1aaf094` - `[Phase 6][UOW-2632] Execute active pet feed cancel live`
- `7c25ce4` - `[Phase 6][UOW-2633] Send active pet not-hungry response live`
- `b9ec634` - `[Phase 6][UOW-2634] Start active pet feeding live`
- `a33935b` - `[Phase 6][UOW-2635] Execute pet auto-loot activation live`
- `32cd2d0` - `[Phase 6][UOW-2636] Execute pet auto-sell activation live`
- `0cf6fce` - `[Phase 6][UOW-2637] Execute pet doping slot switch live`
- Current commit - `[Phase 6][UOW-2638] Execute pet doping add remove live`

## Session Summary

- Added runtime `StaticData.PetDopings` loaded from Java-shaped `<dopings><doping .../>` XML.
- `CM_PET` FOOD actionType 2 now executes dopeActions 0/1 live for active DOPING-capable pets:
  - resolves the pet template's DOPING function id,
  - resolves the matching pet-doping capability row,
  - validates food/drink/scroll slot capability like Java,
  - mutates runtime `PlayerOwnedPet.DopingItemIds`,
  - sends dedicated DOPING special-function packets.
- The actionType 3 and 4 pet activation branches now also fall back to runtime `StaticData.PetTemplates`, instead of relying only on the test-injected pet template table.
- Existing reflection-based `StaticData` test helpers were updated with empty pet-doping tables to match the constructor shape.

## Files Changed In UOW-2638

- `dotnetConversion/src/Aion.GameServer/Dataholders/PetDopingTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataPetTemplateTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionOpenStaticDoorTests.cs`
- `docs/Phase-6-Session-2638-Completion.md`
- `docs/Phase-6-Session-2638-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests|FullyQualifiedName~StaticDataPetTemplateTests" --no-restore
```

Result: passed, 392/392. Existing nullable/analyzer warnings were emitted in unrelated surfaces.

Java/Maven: not run. No narrow Java fixture was discovered for `PetDopingData` or `PetService.useDoping` actions 0/1; Java behavior was verified by source review.

Broad .NET: skipped after focused coverage. Broad trigger existed because runtime static-data loading, live pet state, and packet dispatch changed, but focused validation built affected projects and covered the new static-data table, live connection dispatch, parser shape, packet shape, and packet bytes.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.PetDopingData` | `PetDopingTable` | Dataholder | Partial | Unit Tested | Partial Parity | Runtime lookup by id is ported and consumed by live code. Duplicate-id last-write behavior was not explicitly tested. |
| `com.aionemu.gameserver.model.templates.pet.PetDopingEntry` | `PetDopingEntrySummary` | DTO | Partial | Unit Tested | Partial Parity | Required XML attributes id/usedrink/usefood/usescroll are parsed. JAXB exception details are not mirrored. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` FOOD actionType 2 | `GameServerConnection.HandlePetFoodAsync` | Client handler | Partial | Unit Tested | Partial Parity | DopeActions 0/1/2 now execute live. DopeAction 3 remains deferred. |
| `com.aionemu.gameserver.services.toypet.PetService#useDoping` | `GameServerConnection.HandlePetDopingAsync` | Service behavior in handler | Partial | Unit Tested | Partial Parity | Add/remove validation, slot mutation, switch, and packet send are ported. Item use, scheduling, cooldown, skill effects, inventory decrement, save triggers, and audit logging remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` DOPING special function | `SmPet` | Packet | Partial | Unit Tested | Partial Parity | Existing serializer writes DOPING actions 0/1/2 and live actionType 2 now sends them. |

## Known Gaps

- Doping use action 3 remains deferred because Java uses player spawned-state checks, delayed scheduling, item-use restrictions, item cooldown, skill effects, and inventory decrement.
- Doping bag dirty-state persistence is not wired from live mutation to `player_pets.dopings`.
- Java audit logging for unsupported doping configuration attempts is not implemented.
- Duplicate `PetDopingData` ids would use Java last-write map behavior; C# currently uses dictionary construction and was not tested for duplicates.
- Java invalid high-slot exception behavior was not validated for live connection handling.
- Full Java static-data corpus validation was not run.
- Real client validation was not run.
- Real MySQL validation was not run.

## Next Recommended Runtime UOW

**UOW-2639 candidate: persist live pet doping bag mutations to `player_pets.dopings`.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: live pet doping slot mutations from dopeActions 0/1/2 are runtime-only in C# and do not save to the existing `player_pets.dopings` column.
- Java source method or runtime path: PlayerPetsDAO.saveDopingBag(int petObjectId, PetDopingBag bag) writes Java slot order to `player_pets.dopings`; Java PetDopingBag dirty state indicates save need.
- C# runtime artifact to wire or fix: existing PlayerPetsRepositoryPlan.SaveDopingBag SQL shape, live repository execution if available or a minimal repository method, and GameServerConnection.HandlePetDopingAsync after successful slot mutations.
- Client-visible/state/persistence effect expected: after live doping add/remove/switch, the active pet's doping slot CSV should be persisted/restored through the existing database shape on next enter-world.
- Why this is not preview-only/test-only/documentation-only if feasible: it would persist live runtime state using the existing database shape and directly close a documented live mutation gap.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerPetRowProjectionTests|FullyQualifiedName~PetDopingBagTests" --no-restore
```

Java/Maven: run only if a narrow Java `PlayerPetsDAO.saveDopingBag` or pet-row persistence fixture is discovered.

Broad-validation trigger: live pet persistence and database-shape behavior may change. Start focused on repository/row projection plus live connection tests; broaden only if shared repository infrastructure changes.

## Safe Runtime Candidates

- Persist live pet doping bag mutations to `player_pets.dopings` using the existing database shape.
- Execute dopeAction 3's smallest safe item-use branch if item-use restrictions, cooldowns, skill effects, and inventory decrement dependencies are available.
- Execute delayed pet feeding check only after identifying a safe runtime-loaded `PetFeedEvaluationContext` source or adding one in the same runtime UOW.
- Wire auto-loot free-for-all denial if active team loot-rule state can be read safely from current group/alliance runtime.

## Context Needed By Next Session

- Java pet merchant functions load into C# runtime `StaticData.PetTemplates`, and active player pet state can feed action 17 sell-to-shop as of UOW-2625.
- `CM_PET` SPAWN executes live for owned pets and creates active/world pet state plus `SM_PET` spawn as of UOW-2626.
- `Player.OwnedPets` is restored from `player_pets` during live enter-world as of UOW-2627.
- `SM_PET LOAD_PETS` is sent from live enter-world for restored pets as of UOW-2628.
- `CM_PET` DISMISS clears active player/world pet state and sends owner-visible `SM_PET DISMISS` as of UOW-2629.
- `CM_PET` SURRENDER deletes persistence, removes owned state, clears active/world pet state if needed, and sends `SM_PET SURRENDER` as of UOW-2630.
- `CM_PET` RENAME validates, persists, mutates active owned/world pet names, and broadcasts `SM_PET RENAME` as of UOW-2631.
- `CM_PET` FOOD cancel-feeding mutates `PlayerOwnedPet.CancelFeed` and sends `SM_PET` subtype 4 plus `SM_EMOTION END_FEEDING` as of UOW-2632.
- `CM_PET` FOOD not-hungry/refeed-delay sends `SM_PET` subtype 8 as of UOW-2633.
- `CM_PET` FOOD regular feed-start validates inventory/count, clears `CancelFeed`, and sends `SM_PET` subtype 1 plus `SM_EMOTION START_FEEDING` as of UOW-2634.
- `CM_PET` FOOD actionType 3 auto-loot activation/deactivation mutates `PlayerOwnedPet.IsLooting` and sends live owner packets as of UOW-2635.
- `CM_PET` FOOD actionType 4 auto-sell activation/deactivation mutates `PlayerOwnedPet.IsSelling` and sends live owner packets as of UOW-2636.
- `CM_PET` FOOD actionType 2 dopeAction 2 mutates active pet `DopingItemIds` and sends live DOPING packets as of UOW-2637.
- `CM_PET` FOOD actionType 2 dopeActions 0/1 load runtime pet-doping data, mutate active pet `DopingItemIds`, and send live DOPING packets as of UOW-2638.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
