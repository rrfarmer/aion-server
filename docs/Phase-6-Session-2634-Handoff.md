# Phase 6 Session 2634 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2634: Start active pet feeding live. See
[Phase-6-Session-2634-Completion.md](Phase-6-Session-2634-Completion.md).

## Commits Made

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
- `7c25ce4` - `[Phase 6][UOW-2633] Send active pet not-hungry response live`
- Current commit - `[Phase 6][UOW-2634] Start active pet feeding live`

## Session Summary

- Java review confirmed `PetService.removeObject` starts feeding synchronously, but delays actual item consumption and reward/feed-progress work into scheduled `checkFeeding`.
- `GameServerConnection.HandlePetFoodAsync` now validates regular no-delay feed requests against live inventory, clears active owned-pet `CancelFeed`, sends `SM_PET` FOOD subtype 1, and sends `SM_EMOTION` START_FEEDING.
- Invalid missing item and requested-count-above-stack cases return silently, matching Java.
- Inventory count is intentionally not decremented in this UOW because Java does not decrement until scheduled `checkFeeding`.

## Files Changed In UOW-2634

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2634-Completion.md`
- `docs/Phase-6-Session-2634-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests" --no-restore
```

Result: passed, 378/378. Existing nullable/analyzer warnings were emitted in unrelated surfaces.

Java/Maven: not run. No narrow Java fixture was discovered for `PetService.removeObject` or `CM_PET` FOOD feed-start; Java behavior was verified by source review.

Broad .NET: skipped after focused coverage. Broad trigger existed because live pet state and packet dispatch changed, but focused validation built affected projects and covered live connection dispatch, parser shape, packet shape, and packet bytes.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` FOOD regular feed-start | `GameServerConnection.HandlePetFoodAsync` | Client handler | Partial | Unit Tested | Partial Parity | Live regular feed-start now validates item/count, clears cancel feed, and sends subtype 1 plus START_FEEDING. Doping, auto-loot, auto-sell, and delayed feeding completion remain partial/deferred. |
| `com.aionemu.gameserver.services.toypet.PetService#removeObject` | `GameServerConnection.HandlePetFoodAsync` | Service behavior in handler | Partial | Unit Tested | Partial Parity | First synchronous Java effects are ported. Java's scheduled `checkFeeding` inventory decrement, food acceptance, rewards, refeed persistence, and end-feeding packets are not live yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` FOOD subtype 1 | `SmPet` | Packet | Partial | Unit Tested | Partial Parity | Existing serializer writes subtype 1 shape and live feed-start now sends it. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` START_FEEDING | `SmEmotion` | Packet | Partial | Unit Tested | Partial Parity | Existing serializer writes simple START_FEEDING shape and live feed-start now sends it. |

## Known Gaps

- Delayed pet `checkFeeding` is not live: food-type validation, item unlock, inventory decrement, feed progress mutation, reward grant, refeed time persistence, and end-feeding packets remain deferred.
- FOOD doping, auto-loot, and auto-sell branches remain deferred/partial.
- C# `RefeedDelaySeconds` does not clear expired refeed time the way Java `PetCommonData.getRefeedDelay` clears negative delays.
- Real client validation was not run.
- Real MySQL validation was not relevant to this non-persistent feed-start UOW and was not run.

## Next Recommended Runtime UOW

**UOW-2635 candidate: execute the delayed pet feeding check for a non-eatable food item live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: after feed-start, C# still has no Java-equivalent delayed checkFeeding path, so unsupported food items are not rejected, feeding is not ended, and clients do not receive cleanup packets/messages.
- Java source method or runtime path: PetService.schedule -> PetService.checkFeeding; when FoodType is null, Java sends item unlock, SM_PET subtype 5, SM_EMOTION END_FEEDING, and STR_MSG_TOYPET_FEED_FOOD_NOT_LOVEFLAVOR.
- C# runtime artifact to wire or fix: a small live pet feeding continuation path reachable after CM_PET FOOD start, likely using ThreadPoolManager or an injectable scheduler plus GameServerConnection/SmPet/SmEmotion/SmSystemMessage surfaces.
- Client-visible/state/persistence effect expected: after the delayed check for an unsupported item, the owner should receive Java-shaped feed cleanup/end packets and system message, without inventory decrement.
- Why this is not preview-only/test-only/documentation-only if feasible: it would execute a live scheduled pet-feeding handler path and send real owner-visible packets from runtime code.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests" --no-restore
```

Java/Maven: run only if a narrow Java `PetService.checkFeeding` fixture or relevant system-message packet fixture is discovered.

Broad-validation trigger: live scheduler/pet state/packet dispatch may change. Start focused on connection food continuation tests, packet serializer coverage, and scheduler determinism; broaden only if focused evidence exposes wider risk.

## Safe Runtime Candidates

- Execute delayed pet feeding check for non-eatable food items, sending Java cleanup/end packets and message without consuming inventory.
- Execute delayed pet feeding check for one eatable item, decrementing inventory and sending subtype 2/progress/end packets if feed data can be loaded safely.
- Wire FOOD auto-loot or auto-sell activation if it mutates active pet special-function state and sends Java-equivalent `SM_PET` packets.
- Port enough `PetController.onDelete` persistence to save despawn time/mood data when dismissing, if it can use the existing `player_pets` table shape.

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
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
