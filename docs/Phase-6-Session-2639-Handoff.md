# Phase 6 Session 2639 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2639: Persist live pet doping bag mutations. See
[Phase-6-Session-2639-Completion.md](Phase-6-Session-2639-Completion.md).

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
- `608dc7a` - `[Phase 6][UOW-2638] Execute pet doping add remove live`
- Current commit - `[Phase 6][UOW-2639] Persist pet doping bag mutations live`

## Session Summary

- Added a live repository/service method to save active pet doping slots into `player_pets.dopings`.
- `CM_PET` FOOD actionType 2 now persists changed dopeActions 0/1/2 slot lists before mutating active owned-pet state and before sending success packets.
- Persistence failure now blocks C# in-memory mutation and owner success packet for changed live doping slots.
- Valid unchanged doping operations keep Java dirty semantics: no persistence write when slot contents do not change.
- Added gated DB integration coverage for the Java CSV shape written to `player_pets.dopings`.

## Files Changed In UOW-2639

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2639-Completion.md`
- `docs/Phase-6-Session-2639-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerPetRowProjectionTests|FullyQualifiedName~PetDopingBagTests|FullyQualifiedName~PlayerPetsRepositoryPlanTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests" --no-restore
```

Result: passed, 120/120. Existing nullable/analyzer warnings were emitted in unrelated surfaces.

Java/Maven: not run. There is no `game-server/src/test` directory or narrow Java `PlayerPetsDAO.saveDopingBag`/pet persistence fixture in this checkout; Java behavior was verified by source review.

Broad .NET: skipped after focused coverage. Broad trigger existed because live pet persistence and a shared repository interface changed, but the filtered command built affected projects and covered live connection behavior, repository capture/failure ordering, row projection restore, bag dirty semantics, SQL plan shape, and the gated DB integration path.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `PlayerPetsDAO.saveDopingBag` | `MySqlPlayerEnterWorldRepository.SavePlayerPetDopingBagAsync` | Persistence | Partial | Unit + Gated Integration | Partial Parity | Java CSV order is ported. C# includes a player_id guard consistent with local pet repository methods. |
| `PetController.onDespawn` | `GameServerConnection.HandlePetDopingAsync` save hook | Runtime trigger | Intentional Difference | Unit Tested | Partial Parity | Java saves dirty bags on despawn; C# saves changed live slot lists immediately to close the current restore gap. |
| `PetDopingBag` | `PetDopingBag` plus changed-list guard | State behavior | Partial | Unit Tested | Partial Parity | Changed lists persist; valid unchanged switch does not persist. |
| `PlayerPetsDAO.getPlayerPets` | `PlayerPetRowProjection` and enter-world pet load | Restore | Partial | Unit Tested | Partial Parity | Existing restore path now has a live save source. |

## Known Gaps

- Java save timing differs: Java persists dirty doping bags on pet despawn, while C# persists changed slot lists immediately in the live handler.
- Doping use action 3 remains deferred because Java requires spawned-state checks, delayed scheduling, item-use restrictions, item cooldowns, skill effects, inventory decrement, and additional packets.
- Java audit logging for unsupported doping configuration attempts is not implemented.
- Duplicate pet-doping static-data id last-write behavior remains unverified.
- Full Java static-data corpus validation was not run.
- Real client validation was not run.
- Real MySQL validation was not run locally; the new DB integration test is gated behind `AION_GAMESERVER_DB_INTEGRATION=1`.

## Next Recommended Runtime UOW

**UOW-2640 candidate: execute the smallest safe pet feeding check runtime slice.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: CM_PET FOOD regular feed-start currently sends start packets but never executes Java PetService.checkFeeding, so food is not consumed and feed progress/hungry state is not advanced.
- Java source method or runtime path: PetService.removeObject schedules checkFeeding; PetService.checkFeeding validates the active food item, consumes inventory count, mutates FeedProgress, persists feed status through PlayerPetsDAO.saveFeedStatus, and sends SM_PET/SM_EMOTION results.
- C# runtime artifact to wire or fix: GameServerConnection.HandlePetFoodAsync after feed-start, ToyPet feed evaluation/service artifacts if suitable, inventory mutation, PlayerEnterWorldService repository persistence for feed status if available or minimal live method.
- Client-visible/state/persistence effect expected: accepted pet food should decrement live inventory, advance active pet feed progress/hungry state, persist player_pets hungry/feed/reuse fields, and send the Java-shaped feed result packets for the selected smallest safe branch.
- Why this is not preview-only/test-only/documentation-only if feasible: it would mutate live inventory/pet state, persist runtime pet feed state, and send real server packets from the live client handler.
```

Start with the smallest branch where Java dependencies are already modeled. If food type/data dependencies are not runtime-loadable yet, choose one of the alternative safe candidates below instead of doing readiness-only work.

Suggested focused validation, adjust after discovery:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PetFeed|FullyQualifiedName~PlayerPetsRepositoryPlanTests" --no-restore
```

Java/Maven: run only if a narrow Java `PetService.checkFeeding` fixture or vector generator exists; none was confirmed in this UOW.

Broad-validation trigger: live inventory mutation, pet feed persistence, and packet dispatch would change. Start focused; broaden only if shared inventory/persistence infrastructure changes.

## Safe Runtime Candidates

- Execute the smallest live `PetService.checkFeeding` branch that consumes food, mutates feed progress, persists feed status, and sends the result packets.
- Execute dopeAction 3's smallest safe item-use branch if item-use restrictions, cooldowns, skill effects, and inventory decrement dependencies are available.
- Wire auto-loot free-for-all denial if active team loot-rule state can be read safely from current group/alliance runtime.
- Persist active pet auto-loot/auto-sell activation state only if Java has a matching DB/runtime source of truth; otherwise avoid inventing persistence.

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
- `CM_PET` FOOD actionType 2 changed dopeActions 0/1/2 now persist active pet `DopingItemIds` to `player_pets.dopings` before mutation/packet success as of UOW-2639.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
