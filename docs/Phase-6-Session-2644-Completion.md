# Phase 6 Session 2644 Completion

## UOW

[Phase 6] UOW-2644: Execute rewarded pet feeding live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: accepted pet food that fills the pet and yields a reward now executes the Java reward/refeed branch instead of falling out of live handling.
- Java source/runtime path: PetService.checkFeeding branch where progress.getHungryLevel() == FULL and reward != null sends SM_PET subtype 2, subtype 6, subtype 5, END_FEEDING, subtype 7, ItemService.addItem, scheduleRefeed, PlayerPetsDAO.setTime, and progress.reset().
- C# runtime artifact wired: GameServerConnection.ExecutePetFeedingCheckAsync now handles PetFeedServiceOperationPlanStatus.Rewarded; PlayerEnterWorldService/MySqlPlayerEnterWorldRepository persist source item, reward inventory, and pet feed/refeed state in one mutation.
- Client-visible/state/persistence effect: rewarded feed consumes food, adds or updates a real inventory reward item, stores refeed/feed state, sends reward/refeed pet packets, sends inventory reward packets, and schedules runtime refeed reset when ThreadPoolManager is available.
- Why this is runtime progress: it mutates live inventory and pet state, persists through the existing inventory/player_pets tables, sends real server packets from the live CM_PET feed path, and schedules runtime state reset.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
  - `checkFeeding` reward branch after accepted food consumption.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
  - `scheduleRefeed` clears refeed time and sets hungry level back to `HUNGRY` after delay.
- `game-server/src/com/aionemu/gameserver/dao/PlayerPetsDAO.java`
  - `setTime` persists `reuse_time`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemService.java`
  - `addItem` reward inventory path, represented in C# by existing `InventoryAddService`.

## C# Changes

- Live rewarded pet feeding now:
  - consumes one food item using existing pet-food source mutation,
  - plans reward inventory through `InventoryAddService.CreateAddItemPlan`,
  - persists food source, reward inventory updates/adds, hungry level, feed progress, and refeed time in one repository call,
  - applies the same mutations to `Player.InventoryItems` and `Player.OwnedPets`,
  - sends Java-order pet packets: subtype 2, subtype 6, subtype 5, end-feeding emotion, subtype 7,
  - sends reward inventory add/update packets after the Java-equivalent `ItemService.addItem` boundary,
  - schedules Java-equivalent refeed reset when the runtime `ThreadPoolManager` is injected.
- Extended `SavePlayerPetFeedConsumeMutationAsync` through service/repository/fakes with optional reward item updates/adds.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetFoodRewardedFullFeedAddsRewardPersistsRefeedAndSendsRewardPackets` | Unit/live connection | `PetService.checkFeeding` rewarded branch and `PetCommonData.scheduleRefeed` source review | Live `CM_PET` feed path consumes food, creates a reward item, persists pet refeed/feed state, and sends subtype 2/6/5/7 plus inventory add packet. | Direct packet/state/persistence capture from live `ProcessPacketAsync`; deterministic loved-food fixture selects one reward. | Does not wait for the delayed refeed callback; no real DB or real-client validation. |

## Validation Decision

```text
- Changed surface: live pet feeding inventory/pet persistence and packet fanout.
- Specific behavior/contract: full rewarded feed should consume food, add reward item, persist refeed/feed state, and send Java reward/refeed packet sequence from live CM_PET handling.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionBuyItemTests --no-restore
- Focused Java/Maven command: not run; no narrow Java unit fixture exists for this branch in the checkout, and Java source was reviewed directly.
- Broad-validation trigger: live side effects, persistence, scheduler, and packet fanout changed.
- Broad .NET decision: skipped after focused validation because the filtered connection tests built affected projects and directly exercised the changed live handler, persistence fake, and packets.
- Why this scope is sufficient: the edited branch is inside the existing pet-feed connection test class; the command covers prior accepted/rejected/multi-count feed paths plus the new reward branch.
```

Result: passed, 80/80. Existing nullable/analyzer warnings were emitted in unrelated surfaces; no failures.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` rewarded branch | `Aion.GameServer.Network.Aion.GameServerConnection.ExecutePetFeedingCheckAsync` | Runtime handler | Partial | Unit Tested | Partial Parity | Reward/refeed branch is live for deterministic reward selection, packet fanout, inventory mutation, and persistence. Multiple loved reward random selection remains first-result in C#. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.scheduleRefeed` | `Aion.GameServer.Network.Aion.GameServerConnection.SchedulePetRefeed` | Scheduler callback | Partial | Unit Tested indirectly | Partial Parity | Runtime callback is scheduled when a ThreadPoolManager exists; focused connection test does not wait for the delayed callback. |
| `com.aionemu.gameserver.dao.PlayerPetsDAO.setTime` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePlayerPetFeedConsumeMutationAsync` | Repository | Partial | Unit Tested through fake | Partial Parity | C# persists refeed time together with feed reset/source/reward transaction. Java immediately calls setTime and leaves feed-status persistence to normal pet save paths. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` reward path | `Aion.GameServer.Services.InventoryAddService` plus `MySqlPlayerEnterWorldRepository` reward item persistence | Service/Repository | Partial | Unit Tested | Partial Parity | Reward item add/update is live for cube inventory using existing add-plan behavior; real DB validation not run. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` food subtype 6/7 | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet | Partial | Unit Tested | Partial Parity | Live reward/refeed packets are emitted and parsed by focused test; standalone golden byte comparison remains absent. |

## Known Gaps

- Loved reward selection still uses the first valid reward in the live connection selector; Java uses `Rnd.get(validRewards)` when multiple loved rewards are valid.
- Delayed refeed callback was implemented but not waited on in the live connection fixture.
- Real MySQL persistence validation was not run for reward item insert/update rows.
- Real client validation was not run.
- Packet byte golden coverage for reward/refeed `SM_PET` subtypes remains absent.

## Next Recommended Runtime UOW

**UOW-2645 candidate: execute Java random loved-reward selection live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: when a loved-food reward group has multiple valid rewards, C# live feeding currently selects the first valid reward while Java randomly chooses one with Rnd.get(validRewards).
- Java source method or runtime path: PetFeedCalculator.getReward loved-food branch returns Rnd.get(validRewards) after max-player-level filtering.
- C# runtime artifact to wire or fix: the lovedRewardSelector passed by GameServerConnection.ExecutePetFeedingCheckAsync to PetFeedServiceOperationPlanner.CreatePlan.
- Client-visible/state/persistence effect expected: rewarded pet feeding may grant and persist any Java-valid loved reward instead of always the first one; subtype 6 and inventory reward packets reflect the selected item.
- Why this is not preview-only/test-only/documentation-only: it changes the real reward item added to live inventory, persisted to the database, and sent to the client.
```

Suggested focused validation:

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionBuyItemTests --no-restore
```

Java/Maven: not expected unless a narrow Java random-selection fixture is added or discovered. Broad-validation trigger: live reward inventory/packet behavior changes, but focused connection coverage should run first.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
