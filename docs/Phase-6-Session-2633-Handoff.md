# Phase 6 Session 2633 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2633: Send active pet not-hungry response live. See
[Phase-6-Session-2633-Completion.md](Phase-6-Session-2633-Completion.md).

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
- `f50f024` - `[Phase 6][UOW-2627] Restore owned pets during enter-world`
- `f071686` - `[Phase 6][UOW-2628] Send restored pet list during enter-world`
- `f46d5d8` - `[Phase 6][UOW-2629] Execute active pet dismiss live`
- `0650121` - `[Phase 6][UOW-2630] Execute owned pet surrender live`
- `74b48ab` - `[Phase 6][UOW-2631] Execute active pet rename live`
- `1aaf094` - `[Phase 6][UOW-2632] Execute active pet feed cancel live`
- Current commit - `[Phase 6][UOW-2633] Send active pet not-hungry response live`

## Session Summary

- Java review confirmed `CM_PET` FOOD sends `SM_PET(8, objectId, count, player.getPet())` when active pet refeed delay is positive.
- `GameServerConnection.HandlePetFoodAsync` now sends live `SmPet.Food` subtype 8 for active pets with positive `PlayerOwnedPet.RefeedDelaySeconds`.
- The existing cancel-feeding branch remains first, matching Java ordering.
- Regular no-delay food consumption remains deferred and silent in this UOW.
- Focused tests prove the live subtype 8 packet and no-delay no-op behavior.

## Files Changed In UOW-2633

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2633-Completion.md`
- `docs/Phase-6-Session-2633-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests" --no-restore
```

Result: passed, 376/376.

Java/Maven: not run. No narrow Java fixture was discovered for `CM_PET` food not-hungry or `SM_PET` FOOD subtype 8; Java behavior was verified by source review.

Broad .NET: skipped after focused coverage. Broad trigger existed because live pet packet dispatch changed, but focused validation built affected projects and covered live connection dispatch, parser shape, packet shape, and packet bytes.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` FOOD not-hungry | `GameServerConnection.HandlePetFoodAsync` | Client handler | Partial | Unit Tested | Partial Parity | Live positive-refeed branch sends Java-shaped subtype 8. Regular feeding, doping, auto-loot, and auto-sell remain partial/deferred. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData#getRefeedDelay` | `PlayerOwnedPet.RefeedDelaySeconds` | Runtime model | Partial | Unit Tested | Partial Parity | C# computes remaining delay from restored `RefeedTimeMillis`; unlike Java, it does not clear expired negative refeed time in this helper. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` FOOD subtype 8 | `SmPet` | Packet | Partial | Unit Tested | Partial Parity | Existing serializer writes subtype 8 shape and live not-hungry response now sends it. |

## Known Gaps

- Regular pet feeding remains partial: food item lookup, inventory decrement, feed progress mutation, loved-food/reward handling, scheduling, and persistence are not live.
- C# `RefeedDelaySeconds` does not clear expired refeed time the way Java `PetCommonData.getRefeedDelay` clears negative delays.
- FOOD doping, auto-loot, and auto-sell branches remain deferred/partial.
- Real client validation was not run.
- Real MySQL validation was not relevant to this packet-only UOW and was not run.

## Next Recommended Runtime UOW

**UOW-2634 candidate: start regular active pet feeding live for one consumed food item.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: no-delay CM_PET FOOD with a real food item still does not consume inventory, start feed progress, or send Java feed-start packets.
- Java source method or runtime path: CM_PET.runImpl FOOD falls through to PetService.removeObject(objectId, count, player); PetService.removeObject checks inventory item and count, clears cancelFeed, sends SM_PET subtype 1, sends SM_EMOTION START_FEEDING, and schedules later feed checks.
- C# runtime artifact to wire or fix: GameServerConnection.HandlePetFoodAsync, Player.InventoryItems mutation, PlayerOwnedPet.CancelFeed/FeedProgressData update if needed, SmPet.Food subtype 1 packet emission, SmEmotion START_FEEDING emission, and persistence only if the selected slice mutates durable inventory.
- Client-visible/state/persistence effect expected: feeding an active pet with a valid inventory item should at minimum clear CancelFeed, decrement or prepare to consume one food item in live inventory according to Java scope, and send Java-shaped SM_PET subtype 1 plus SM_EMOTION START_FEEDING.
- Why this is not preview-only/test-only/documentation-only if feasible: it would mutate live pet/inventory state and send real server packets from the live client handler.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests" --no-restore
```

Java/Maven: run only if a narrow Java `PetService.removeObject` or `SM_PET` FOOD subtype 1 fixture is discovered.

Broad-validation trigger: live pet/inventory state plus packet dispatch may change. Start focused on connection food-start tests, inventory mutation assertions, parser coverage, and packet serializer coverage; broaden only if focused evidence exposes wider risk.

## Safe Runtime Candidates

- Start regular active pet feeding for one valid food item with Java-equivalent owner packets, if inventory mutation can be safely scoped.
- Wire FOOD auto-loot or auto-sell activation only if it mutates active pet special-function state and sends Java-equivalent `SM_PET` packets.
- Port enough `PetController.onDelete` persistence to save despawn time/mood data when dismissing, if it can use the existing `player_pets` table shape.
- Improve active pet refeed runtime by clearing expired `RefeedTimeMillis` only if paired with a live packet/state behavior in the same UOW.

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
- `PlayerOwnedPet` stores fields needed by load-pets and feed packet snapshots, but full pet common-data runtime behavior remains partial.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
