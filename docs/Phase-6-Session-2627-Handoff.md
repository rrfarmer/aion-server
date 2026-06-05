# Phase 6 Session 2627 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2627: Restore owned pets during enter-world. See
[Phase-6-Session-2627-Completion.md](Phase-6-Session-2627-Completion.md).

## Commits Made

- `0671411` - `[Phase 6][UOW-2606] Execute NPC shop kinah buys live`
- `7d70054` - `[Phase 6][UOW-2607] Send NPC shop kinah denial live`
- `e00f057` - `[Phase 6][UOW-2608] Send NPC shop invalid-goods denial live`
- `8c0e2ee` - `[Phase 6][UOW-2609] Send NPC shop full-inventory denial live`
- `e8e1689` - `[Phase 6][UOW-2610] Send NPC shop limited-item denial live`
- `4aae7d1` - `[Phase 6][UOW-2611] Send NPC shop abyss-point denial live`
- `1daf029` - `[Phase 6][UOW-2612] Persist NPC shop kinah buys live`
- `c015455` - `[Phase 6][UOW-2613] Consume NPC shop required items live`
- `40ea371` - `[Phase 6][UOW-2614] Spend NPC shop abyss points live`
- `b848bbb` - `[Phase 6][UOW-2615] Mutate NPC shop limited counters live`
- `867b01a` - `[Phase 6][UOW-2616] Schedule NPC shop limited resets live`
- `3507068` - `[Phase 6][UOW-2617] Execute NPC shop abyss kinah buys live`
- `36b6746` - `[Phase 6][UOW-2618] Execute NPC shop abyss buys live`
- `a56bdbb` - `[Phase 6][UOW-2619] Execute NPC shop reward buys live`
- `816199d` - `[Phase 6][UOW-2620] Execute normal NPC sell-to-shop live`
- `46397e7` - `[Phase 6][UOW-2621] Execute abyss AP sell-to-shop live`
- `0eeb30d` - `[Phase 6][UOW-2622] Execute NPC shop repurchase live`
- `6c31b53` - `[Phase 6][UOW-2623] Execute partial AP sell-to-shop live`
- `e9a5945` - `[Phase 6][UOW-2624] Execute pet merchant sell-to-shop live`
- `b6f1206` - `[Phase 6][UOW-2625] Load pet merchant templates into active pet sell`
- `72c3a94` - `[Phase 6][UOW-2626] Execute pet spawn live from CM_PET`
- Current commit - `[Phase 6][UOW-2627] Restore owned pets during enter-world`

## Session Summary

- Java review confirmed `PetList.loadPets` restores persisted pets from `PlayerPetsDAO.getPlayerPets` and keys them by template id.
- C# repository contract now exposes `LoadPlayerPetsAsync`.
- MySQL enter-world repository now reads `player_pets` rows and maps them through the existing Java-shaped `PlayerPetRowProjection`.
- `PlayerEnterWorldService` now assigns `Player.OwnedPets` before adding the player to the world.
- Focused tests prove successful enter-world restores owned-pet state, and adjacent live tests still prove that shape can drive `CM_PET` spawn and action 17 pet merchant sell.

## Files Changed In UOW-2627

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2627-Completion.md`
- `docs/Phase-6-Session-2627-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerPetRowProjectionTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmPetTests" --no-restore
```

Result: passed, 151/151.

Java/Maven: not run. No narrow Java fixture was discovered for `PlayerPetsDAO.getPlayerPets` or `PetList.loadPets`; Java behavior was verified by source review.

Broad .NET: skipped after focused coverage. Broad trigger existed because live enter-world state and a persistence load boundary changed, but the focused command built affected projects and covered the edited service, row projection, CM_PET parser, live spawn, and downstream action 17 sell.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.PlayerPetsDAO#getPlayerPets` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.LoadPlayerPetsAsync` / `PlayerPetRowProjection` | Repository | Partial | Unit Tested | Partial Parity | Basic row fields are restored into live state; full `PetCommonData` remains partial. |
| `com.aionemu.gameserver.model.gameobjects.player.PetList#loadPets` | `Aion.GameServer.Services.PlayerEnterWorldService.EnterWorldAsync` / `Player.OwnedPets` | Runtime player state | Partial | Unit Tested | Partial Parity | Enter-world restores owned pets; expirable registration and last-used tracking remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `PlayerOwnedPet` plus `PlayerPetLoadedProjection` | Runtime model/projection | Partial | Unit Tested | Partial Parity | Live spawn consumes object/template/name/decoration; other common-data fields are not live on `PlayerOwnedPet`. |

## Known Gaps

- Enter-world still does not send Java `SM_PET(Collection<PetCommonData>)` / `LOAD_PETS`.
- Full `PetCommonData` feed/doping/mood/birthday/expiration state is not represented on the live owned-pet model.
- Pet expirable registration and last-used-pet restoration are not wired.
- `CM_PET` DISMISS, SURRENDER, auto-sell, auto-loot, and adopt flows remain deferred.
- Live MySQL and real-client validation were not run.

## Next Recommended Runtime UOW

**UOW-2628 candidate: send `SM_PET(LOAD_PETS)` during live enter-world when restored pets exist.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: enter-world now restores Player.OwnedPets, but the client is not notified with Java's pet-list login packet.
- Java source method or runtime path: PlayerEnterWorldService.onLogin calls PetService.getInstance().onPlayerLogin(player); PetService.onPlayerLogin sends new SM_PET(player.getPetList().getPets()) when the collection is not empty.
- C# runtime artifact to wire or fix: GameServerConnection enter-world packet sequence and PlayerOwnedPet/SmPetDataSnapshot mapping.
- Client-visible/state/persistence effect expected: a player with restored pets receives a real SM_PET LOAD_PETS packet during login, allowing the client pet UI to know persisted pets before CM_PET SPAWN.
- Why this is not preview-only/test-only/documentation-only if feasible: it sends a real server packet from the live enter-world path using restored runtime state.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests" --no-restore
```

Java/Maven: not expected unless a narrow Java `SM_PET` load-pets golden fixture is discovered.

Broad-validation trigger: live enter-world packet sequence changes. Start focused on `SM_PET` packet shape and the nearest live connection/enter-world tests; broaden only if focused evidence exposes wider risk.

## Safe Runtime Candidates

- Extend `PlayerOwnedPet` with the persisted fields required to build `SmPetDataSnapshot` and send a real `SM_PET.LOAD_PETS`.
- Wire `CM_PET` DISMISS only if it removes active player/world pet state and sends Java-equivalent dismiss packets.
- Restore pet expirable registration if it uses the existing expirable task service to mutate live runtime state.

## Context Needed By Next Session

- Java pet merchant functions load into C# runtime `StaticData.PetTemplates`, and active player pet state can feed action 17 sell-to-shop as of UOW-2625.
- `CM_PET` SPAWN now executes live for owned pets and creates active/world pet state plus `SM_PET` spawn as of UOW-2626.
- `Player.OwnedPets` is restored from `player_pets` during live enter-world as of UOW-2627.
- `PlayerOwnedPet` currently stores only object id, template id, name, and decoration.
- `SmPet.LoadPets` already exists, but live enter-world does not send it yet.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
