# Phase 6 Session 2637 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2637: Execute active pet doping slot switch live. See
[Phase-6-Session-2637-Completion.md](Phase-6-Session-2637-Completion.md).

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
- Current commit - `[Phase 6][UOW-2637] Execute pet doping slot switch live`

## Session Summary

- Java review showed `PetService.useDoping` has a safe narrow live branch for dopeAction 2:
  - it only requires a non-null doping bag,
  - switches scroll slots via `PetDopingBag.switchItems`,
  - sends the dedicated DOPING special-function packet.
- C# does not currently load Java `PetDopingData`, so add/remove actions 0/1 were not safe to port in this UOW.
- Doping use action 3 remains too broad for this slice because it touches scheduling, item-use restrictions, cooldowns, skill effects, and inventory decrement.
- `CM_PET` FOOD actionType 2 now executes live for dopeAction 2 on active DOPING-capable pets:
  - swaps runtime `PlayerOwnedPet.DopingItemIds` scroll slots,
  - preserves Java's no-mutation behavior when either slot is food/drink,
  - sends `SM_PET` SPECIAL_FUNCTION DOPING action 2.

## Files Changed In UOW-2637

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2637-Completion.md`
- `docs/Phase-6-Session-2637-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests" --no-restore
```

Result: passed, 387/387. Existing nullable/analyzer warnings were emitted in unrelated surfaces.

Java/Maven: not run. No narrow Java fixture was discovered for `PetService.useDoping` action 2 or `SM_PET` DOPING action 2; Java behavior was verified by source review.

Broad .NET: skipped after focused coverage. Broad trigger existed because live pet state and packet dispatch changed, but focused validation built affected projects and covered live connection dispatch, parser shape, packet shape, and packet bytes.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` FOOD actionType 2 | `GameServerConnection.HandlePetFoodAsync` | Client handler | Partial | Unit Tested | Partial Parity | Live actionType 2 now executes dopeAction 2 slot switching. Dope actions 0/1/3 remain deferred. |
| `com.aionemu.gameserver.services.toypet.PetService#useDoping` | `GameServerConnection.HandlePetDopingAsync` | Service behavior in handler | Partial | Unit Tested | Partial Parity | Null-bag/DOPING-function guard and action 2 switch/send are ported. Add/remove validation, item use, scheduling, cooldown, skill effects, inventory decrement, and save triggers remain missing. |
| `com.aionemu.gameserver.model.templates.pet.PetDopingBag#switchItems` | `PetDopingBag.SwitchItems` used by live handler | Runtime helper | Partial | Unit Tested | Partial Parity | Existing helper is now used by live code for scroll-slot switching. Threading parity remains limited to deterministic tests. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` DOPING special function | `SmPet` | Packet | Partial | Unit Tested | Partial Parity | Existing serializer writes DOPING action 2 shape and live actionType 2 now sends it. |

## Known Gaps

- Doping add/remove actions 0/1 remain deferred because Java requires runtime `PetDopingData` food/drink/scroll-capacity validation.
- Doping use action 3 remains deferred because Java uses player spawned-state checks, delayed scheduling, item-use restrictions, item cooldown, skill effects, and inventory decrement.
- Doping bag dirty-state persistence is not wired from live mutation to `player_pets.dopings`.
- Java invalid high-slot exception behavior was not validated for live connection handling.
- Java audit logging for unsupported branches is not implemented.
- Delayed pet `checkFeeding` and auto-loot free-for-all denial remain deferred.
- Real client validation was not run.
- Real MySQL validation was not run.

## Next Recommended Runtime UOW

**UOW-2638 candidate: load Java pet-doping static data into runtime C# and execute dopeAction 0/1 add/remove live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: CM_PET FOOD actionType 2 dopeActions 0/1 still return silently in C# live code.
- Java source method or runtime path: PetService.useDoping actions 0/1 call validateSetDopeItem, which reads PetFunctionType.DOPING id and DataManager.PET_DOPING_DATA, mutates PetDopingBag.setItem(itemId, slot), and sends SM_PET(action, itemId, slot).
- C# runtime artifact to wire or fix: StaticData pet-doping XML loader/runtime table if absent, GameServerConnection.HandlePetDopingAsync actions 0/1, active PlayerOwnedPet.DopingItemIds mutation, and SmPet DOPING add/remove packet emission.
- Client-visible/state/persistence effect expected: configuring or removing a pet doping item on an active DOPING-capable pet should mutate runtime doping slots and send Java-shaped DOPING action 0/1 packets; invalid slot capability should return silently.
- Why this is not preview-only/test-only/documentation-only if feasible: it would load Java XML/static data into runtime C# structures used by live code, mutate active pet runtime state, and send real owner packets.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests|FullyQualifiedName~StaticData" --no-restore
```

Java/Maven: run only if a narrow Java `PetDopingData`, `PetService.useDoping`, or `SM_PET` doping fixture is discovered.

Broad-validation trigger: live pet state, runtime static-data loading, and packet dispatch may change. Start focused on static-data parser/table tests, connection actionType 2 tests, and packet serializer coverage; broaden only if loader changes touch shared XML infrastructure.

## Safe Runtime Candidates

- Load Java pet-doping static data into runtime C# and execute dopeActions 0/1 add/remove with live state mutation and packet send.
- Wire dirty-state persistence for live pet doping slot mutations to `player_pets.dopings` using the existing database shape.
- Execute the delayed pet feeding check only after identifying a safe runtime-loaded `PetFeedEvaluationContext` source or adding one in the same runtime UOW.
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
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
