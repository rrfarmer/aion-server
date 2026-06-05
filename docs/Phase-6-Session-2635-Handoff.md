# Phase 6 Session 2635 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2635: Execute active pet auto-loot activation live. See
[Phase-6-Session-2635-Completion.md](Phase-6-Session-2635-Completion.md).

## Commits Made

- `f50f024` - `[Phase 6][UOW-2627] Restore owned pets during enter-world`
- `f071686` - `[Phase 6][UOW-2628] Send restored pet list during enter-world`
- `f46d5d8` - `[Phase 6][UOW-2629] Execute active pet dismiss live`
- `0650121` - `[Phase 6][UOW-2630] Execute owned pet surrender live`
- `74b48ab` - `[Phase 6][UOW-2631] Execute active pet rename live`
- `1aaf094` - `[Phase 6][UOW-2632] Execute active pet feed cancel live`
- `7c25ce4` - `[Phase 6][UOW-2633] Send active pet not-hungry response live`
- `b9ec634` - `[Phase 6][UOW-2634] Start active pet feeding live`
- Current commit - `[Phase 6][UOW-2635] Execute pet auto-loot activation live`

## Session Summary

- The previous handoff suggested delayed pet feeding check next, but discovery showed C# feed-data runtime wiring is not yet obviously loaded into `StaticData` and the existing pet-feed packet bridge is explicitly non-live metadata. That candidate remains valid but needs a careful runtime-data/scheduler slice.
- Java review confirmed `PetService.activateLoot` is a smaller direct runtime branch with no feed-data dependency.
- `CM_PET` FOOD actionType 3 now executes live for active pets:
  - enable requires the pet template to contain LOOT,
  - successful enable sends Java message `1400876`, mutates `PlayerOwnedPet.IsLooting`, and sends `SM_PET` AUTOLOOT true,
  - disable clears `IsLooting` and sends `SM_PET` AUTOLOOT false without requiring the LOOT template function,
  - enable on a non-loot pet remains silent, matching Java's return after audit logging.

## Files Changed In UOW-2635

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerOwnedPet.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2635-Completion.md`
- `docs/Phase-6-Session-2635-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests" --no-restore
```

Result: passed, 381/381. Existing nullable/analyzer warnings were emitted in unrelated surfaces.

Java/Maven: not run. No narrow Java fixture was discovered for `PetService.activateLoot` or `SM_PET` AUTOLOOT activation; Java behavior was verified by source review.

Broad .NET: skipped after focused coverage. Broad trigger existed because live pet state and packet dispatch changed, but focused validation built affected projects and covered live connection dispatch, parser shape, packet shape, and packet bytes.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` FOOD actionType 3 | `GameServerConnection.HandlePetFoodAsync` | Client handler | Partial | Unit Tested | Partial Parity | Live auto-loot activation/deactivation now executes for active pets. Doping and auto-sell action types remain deferred. |
| `com.aionemu.gameserver.services.toypet.PetService#activateLoot` | `GameServerConnection.HandlePetAutoLootActivationAsync` | Service behavior in handler | Partial | Unit Tested | Partial Parity | LOOT function guard, enable message, state mutation, and AUTOLOOT packet are ported. Java audit logging and team free-for-all denial are not live yet. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData#isLooting` | `PlayerOwnedPet.IsLooting` | Runtime model | Partial | Unit Tested | Partial Parity | Runtime-only looting flag exists and is mutated by live auto-loot activation. Persistence is not expected for this Java field. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` AUTOLOOT special function | `SmPet` | Packet | Partial | Unit Tested | Partial Parity | Existing serializer writes AUTOLOOT activation shape and live actionType 3 now sends it. |

## Known Gaps

- Java team free-for-all denial branch (`STR_MSG_LOOTING_PET_MESSAGE03`) is not live because this slice did not inspect/wire active team loot-rule state into pet activation.
- Java audit logging for enabling auto-loot on non-loot pets is not implemented.
- FOOD actionType 2 doping and actionType 4 auto-sell remain deferred/partial.
- Delayed pet `checkFeeding` remains deferred; static feed-data runtime wiring was not safe enough for this UOW.
- Real client validation was not run.
- Real MySQL validation was not relevant to this runtime-only state/packet UOW and was not run.

## Next Recommended Runtime UOW

**UOW-2636 candidate: execute active pet auto-sell activation live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: CM_PET FOOD actionType 4 still returns silently in C# live code.
- Java source method or runtime path: CM_PET.runImpl FOOD actionType 4 calls PetService.activateAutoSell(pet, activateSpecialFunction != 0); PetService.activateAutoSell validates MERCHANT support only on enable, sets PetCommonData.isSelling, and sends SM_PET(PetSpecialFunction.AUTOSELL, activate).
- C# runtime artifact to wire or fix: GameServerConnection.HandlePetFoodAsync actionType 4 branch, PlayerOwnedPet runtime selling state, PetTemplateTable MERCHANT function guard, SmPet AUTOSELL packet emission.
- Client-visible/state/persistence effect expected: enabling auto-sell on a MERCHANT-capable active pet should mutate runtime pet selling state and send Java-shaped SM_PET AUTOSELL true; disabling should clear selling state and send AUTOSELL false.
- Why this is not preview-only/test-only/documentation-only if feasible: it would mutate live active-pet runtime state and send a real server packet from the live client handler.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests" --no-restore
```

Java/Maven: run only if a narrow Java `PetService.activateAutoSell` or `SM_PET` AUTOSELL fixture is discovered.

Broad-validation trigger: live pet state and packet dispatch may change. Start focused on connection actionType 4 tests and packet serializer coverage; broaden only if focused evidence exposes wider risk.

## Safe Runtime Candidates

- Execute active pet auto-sell activation for actionType 4, mutating runtime selling state and sending Java-shaped AUTOSELL packets.
- Wire auto-loot free-for-all denial if active team loot-rule state can be read safely from current group/alliance runtime.
- Execute delayed pet feeding check only after identifying a safe runtime-loaded `PetFeedEvaluationContext` source or adding one in the same runtime UOW.
- Execute one eatable feed-check item decrement/progress path after pet feed data and item template levels are runtime available.

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
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
