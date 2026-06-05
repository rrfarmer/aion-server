# Phase 6 Session 2644 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2644: Execute rewarded pet feeding live. See
[Phase-6-Session-2644-Completion.md](Phase-6-Session-2644-Completion.md).

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
- `1fc49f8` - `[Phase 6][UOW-2639] Persist pet doping bag mutations live`
- `57a404f` - `[Phase 6][UOW-2640] Execute single pet feeding check live`
- `e94c02d` - `[Phase 6][UOW-2641] Execute rejected pet feeding live`
- `426e709` - `[Phase 6][UOW-2642] Execute multi-count pet feeding live`
- `26a2ca1` - `[Phase 6][UOW-2643] Send pet-food inventory update type live`
- Current commit - `[Phase 6][UOW-2644] Execute rewarded pet feeding live`

## Session Summary

- Live rewarded pet feeding now handles `PetFeedServiceOperationPlanStatus.Rewarded`.
- Full loved-food feed consumes one food item, adds a real reward inventory item through `InventoryAddService`, persists the source/reward/pet mutation, sends subtype 2/6/5/7 `SM_PET` packets, sends END_FEEDING, sends reward inventory packets, and schedules Java-equivalent refeed reset when the runtime thread pool exists.
- `SavePlayerPetFeedConsumeMutationAsync` now carries optional reward item updates/adds so the reward branch can save inventory and `player_pets` state in one transaction.

## Files Changed In UOW-2644

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2644-Completion.md`
- `docs/Phase-6-Session-2644-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.toypet.PetService.checkFeeding`
- `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.scheduleRefeed`
- `com.aionemu.gameserver.dao.PlayerPetsDAO.setTime`
- `com.aionemu.gameserver.services.item.ItemService.addItem`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_PET`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.ExecutePetFeedingCheckAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.SchedulePetRefeed`
- `Aion.GameServer.Services.PlayerEnterWorldService.SavePlayerPetFeedConsumeMutationAsync`
- `Aion.GameServer.Data.IPlayerEnterWorldRepository.SavePlayerPetFeedConsumeMutationAsync`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePlayerPetFeedConsumeMutationAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmPet`
- `Aion.GameServer.Services.InventoryAddService`

## Validation

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionBuyItemTests --no-restore
```

Result: passed, 80/80. Existing nullable/analyzer warnings were emitted in unrelated surfaces.

Java/Maven: not run. No narrow Java unit fixture for this reward/refeed branch was found; Java source was reviewed directly.

Broad .NET: skipped after focused validation. Broad trigger existed because live side effects, persistence, scheduler, and packet fanout changed; the focused command built affected projects and directly exercised the changed live handler and adjacent pet-feed paths.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` rewarded branch | `Aion.GameServer.Network.Aion.GameServerConnection.ExecutePetFeedingCheckAsync` | Runtime handler | Partial | Unit Tested | Partial Parity | Reward/refeed branch is live for deterministic loved reward selection. Multiple loved rewards still select first in C#. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.scheduleRefeed` | `Aion.GameServer.Network.Aion.GameServerConnection.SchedulePetRefeed` | Scheduler callback | Partial | Unit Tested indirectly | Partial Parity | Callback schedules runtime reset when ThreadPoolManager exists; focused test does not wait for the delayed callback. |
| `com.aionemu.gameserver.dao.PlayerPetsDAO.setTime` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePlayerPetFeedConsumeMutationAsync` | Repository | Partial | Unit Tested through fake | Partial Parity | C# stores refeed/feed state with the reward transaction. Java calls setTime immediately and normal pet save persists feed status later. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` reward path | `Aion.GameServer.Services.InventoryAddService` plus repository reward persistence | Service/Repository | Partial | Unit Tested | Partial Parity | Reward item add/update is live for cube inventory using existing add-plan behavior; real DB validation not run. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` food subtype 6/7 | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet | Partial | Unit Tested | Partial Parity | Live reward/refeed packets are emitted; standalone golden byte comparison remains absent. |

## Known Gaps

- Multiple valid loved rewards still pick the first valid C# reward; Java uses random `Rnd.get(validRewards)`.
- Delayed refeed callback behavior is implemented but not waited on by the focused live connection test.
- Real MySQL validation for reward item insert/update rows was not run.
- Real client validation was not run.
- Raw byte golden coverage for rewarded `SM_PET` subtype 6/7 and reward inventory packets remains absent.

## Next Recommended Runtime UOW

**UOW-2645 candidate: execute Java random loved-reward selection live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: rewarded loved-food feeding currently grants the first valid reward, but Java randomly chooses among all valid loved rewards.
- Java source method or runtime path: PetFeedCalculator.getReward loved-food branch filters rewards by player level, keeps max-level valid rewards, and returns Rnd.get(validRewards).
- C# runtime artifact to wire or fix: GameServerConnection.ExecutePetFeedingCheckAsync lovedRewardSelector passed into PetFeedServiceOperationPlanner.CreatePlan.
- Client-visible/state/persistence effect expected: live rewarded feeding can grant, persist, and send any Java-valid loved reward instead of always the first reward.
- Why this is not preview-only/test-only/documentation-only: the selected reward item affects live inventory mutation, database persistence, subtype 6 packet payload, and inventory add/update packets.
```

Suggested focused validation:

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionBuyItemTests --no-restore
```

Java/Maven: not expected unless a narrow Java random-selection fixture is discovered or added. Broad-validation trigger: live reward inventory/packet behavior changes; run the focused command first and document whether broader validation is still needed.

## Safe Runtime Candidates

- Wire Java-equivalent random loved-reward selection into the live reward branch.
- Add a targeted scheduler test for `SchedulePetRefeed` only if paired with a live runtime callback change or if the next runtime branch depends on it.
- Add real DB validation for the rewarded pet feed transaction only if a narrow schema-backed fixture is already available; do not make it a standalone evidence-only UOW.

## Context Needed By Next Session

- UOW-2640: accepted single-count feed consumes item, persists feed/inventory, sends subtype 2/subtype 5/end.
- UOW-2641: rejected food sends item unlock, subtype 5, END_FEEDING, and message `1400618`.
- UOW-2642: accepted multi-count food repeats until count reaches zero.
- UOW-2643: accepted pet feeding partial-stack inventory updates use Java `DEC_PET_FOOD = 0x5E`.
- UOW-2644: rewarded full feed grants inventory reward, persists refeed/feed state, sends subtype 6/7, and schedules refeed reset.
- Runtime feed evaluation uses `StaticData.PetFeedData`, loaded from Java `pet_feed.xml` and `item_groups.xml`.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, execute a live handler path, or directly unblock one of those behaviors.
