# Phase 6 Session 2650 Completion

## UOW

[Phase 6] UOW-2650: Persist active pet feed status on dismiss live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: active pet delete now preserves current feed status instead of losing hungry/progress/refeed data when a food pet is dismissed.
- Java source/runtime path: PetController.onDelete calls PetCommonData.cancelRefeedTask(), sets cancelFeed true when feed progress exists, and calls PlayerPetsDAO.saveFeedStatus(objectId, hungryLevel, progressData, refeedTime).
- C# runtime artifact wired: GameServerConnection.ClearActivePetAsync now sets PlayerOwnedPet.CancelFeed and calls PlayerEnterWorldService.SavePlayerPetFeedStatusAsync for active food pets.
- Client-visible/state/persistence effect: live CM_PET DISMISS mutates owned-pet cancel-feed state, persists player_pets.hungry_level/feed_progress/reuse_time, clears the world pet, and still sends the dismiss packet.
- Why this is runtime progress: this changes live packet-handler state mutation and database persistence for active pet deletion.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/controllers/PetController.java`
  - `onDelete` cancels the pet refeed task, sets cancel-feed when feed progress exists, and persists feed status through `PlayerPetsDAO.saveFeedStatus`.
- `game-server/src/com/aionemu/gameserver/dao/PlayerPetsDAO.java`
  - `saveFeedStatus` updates `player_pets.hungry_level`, `feed_progress`, and `reuse_time` by pet object id.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - `DISMISS` deletes the active pet controller when `player.getPet()` is present.
- `game-server/src/com/aionemu/gameserver/services/toypet/PetAdoptionService.java`
  - `surrenderPet` removes the owned pet row first, then deletes the active pet controller when the surrendered pet is active.

## C# Changes

- Added `IPlayerEnterWorldRepository.SavePlayerPetFeedStatusAsync`.
- Added `PlayerEnterWorldService.SavePlayerPetFeedStatusAsync`.
- Implemented `MySqlPlayerEnterWorldRepository.SavePlayerPetFeedStatusAsync` using the existing `player_pets` feed columns.
- Extended `EmptyPlayerEnterWorldRepository` capture state for focused live-handler assertions.
- Extended `GameServerConnection.ClearActivePetAsync` to set `CancelFeed` and persist feed status for active food pets before clearing the active summon.
- Added focused live `CM_PET DISMISS` coverage that proves feed status persistence, cancel-feed mutation, world removal, and dismiss packet send.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetDismissPersistsFeedStatusAndSetsCancelFeed` | Unit/live connection | `PetController.onDelete` and `PlayerPetsDAO.saveFeedStatus` source review | Live `CM_PET DISMISS` for a food pet sets cancel-feed, persists hungry/progress/refeed fields, removes the world pet, clears active pet state, and sends the dismiss packet. | Runs actual `CM_PET DISMISS` packet path and captures the repository feed-status update. | Does not validate a real MySQL row; active surrender deletes the row before the delete-time feed-status update as Java does. |

## Validation Decision

```text
- Changed surface: live CM_PET DISMISS active-pet state and player_pets persistence.
- Specific behavior/contract: Java PetController.onDelete sets cancelFeed and calls PlayerPetsDAO.saveFeedStatus for food pets during active pet delete.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~ProcessPacketAsync_CmPetDismissPersistsFeedStatusAndSetsCancelFeed --no-restore
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetDismiss|FullyQualifiedName~ProcessPacketAsync_CmPetSurrender" --no-restore
- Focused Java/Maven command: not run; no narrow Java test fixture exists for PetController.onDelete feed-status persistence in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: live handler state and persistence changed.
- Broad .NET decision: skipped after focused validation because the filtered connection tests built affected projects and directly exercised dismiss and active surrender cleanup.
- Why this scope is sufficient: the edited behavior is isolated to active pet cleanup and repository feed-status persistence; the new live packet-path test captures the exact feed fields written from runtime state.
```

Results:

- New focused dismiss feed-status test: passed, 1/1.
- Targeted dismiss/surrender filter: passed, 6/6.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces during the first compile; no failures remained.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PetController.onDelete` feed-status branch | `Aion.GameServer.Network.Aion.GameServerConnection.ClearActivePetAsync` / `PersistActivePetFeedStatusOnDeleteAsync` | Runtime lifecycle | Partial | Unit Tested | Partial Parity | Active food-pet dismiss now sets cancel-feed and persists feed fields. Mood/despawn persistence, pet-update task cancellation, and dirty doping-bag-on-delete behavior remain incomplete. |
| `com.aionemu.gameserver.dao.PlayerPetsDAO.saveFeedStatus` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SavePlayerPetFeedStatusAsync` / `MySqlPlayerEnterWorldRepository.SavePlayerPetFeedStatusAsync` | Repository | Partial | Unit Tested via live handler fake | Partial Parity | C# updates the same feed columns and adds a `player_id` predicate like nearby C# pet repository methods. No real MySQL validation was run. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` dismiss active delete path | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetDismissAsync` / `ClearActivePetAsync` | Runtime packet path | Partial | Unit Tested | Partial Parity | Dismiss now flows through cancel refeed, feed-status persistence, world removal, active state clear, and dismiss packet send. Other Java delete-time side effects are still partial. |

## Known Gaps

- Java `PetController.onDelete` persists mood/despawn data and cancels `TaskId.PET_UPDATE`; C# pet mood/update lifecycle remains incomplete.
- Java `PetController.onDelete` persists a dirty doping bag on delete; C# currently persists doping mutations when they happen but does not model delete-time dirty-bag persistence.
- Active pet surrender deletes the `player_pets` row before controller delete in Java; C# preserves that order, so delete-time feed-status persistence is meaningful for dismiss and best-effort/no-row for active surrender.
- Real client validation was not run.
- Real MySQL validation was not run.

## Next Recommended Runtime UOW

**UOW-2651 candidate: persist pet despawn/mood state on active pet dismiss/delete live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: active pet delete should store despawn/mood state instead of only clearing the active summon.
- Java source method or runtime path: PetController.onDelete sets commonData.despawnTime to the current timestamp and calls PlayerPetsDAO.savePetMoodData(commonData) after cancelling TaskId.PET_UPDATE.
- C# runtime artifact to wire or fix: PlayerOwnedPet or a companion runtime state type for pet mood/despawn fields, GameServerConnection.ClearActivePetAsync, and PlayerEnterWorldService/repository savePetMoodData persistence.
- Client-visible/state/persistence effect expected: CM_PET DISMISS and active-pet SURRENDER update live owned-pet despawn/mood state and persist the existing player_pets mood/despawn columns.
- Why this is not preview-only/test-only/documentation-only: it mutates live active-pet state and writes runtime database state from live packet handlers.
```

Suggested focused validation:

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetDismiss|FullyQualifiedName~ProcessPacketAsync_CmPetSurrender" --no-restore
```

Start by inspecting `PlayerPetRowProjection`, `PlayerPetsRepositoryPlan.SavePetMoodData`, and current `PlayerOwnedPet` fields. If the model cannot carry mood/despawn state without a broader lifecycle change, document the blocker and choose a smaller live runtime candidate.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
