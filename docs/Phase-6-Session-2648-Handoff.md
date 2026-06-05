# Phase 6 Session 2648 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2648: Cancel active pet refeed on dismiss live. See
[Phase-6-Session-2648-Completion.md](Phase-6-Session-2648-Completion.md).

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
- `fed5542` - `[Phase 6][UOW-2644] Execute rewarded pet feeding live`
- `b6f8269` - `[Phase 6][UOW-2645] Execute random loved pet reward selection live`
- `a305c40` - `[Phase 6][UOW-2646] Execute pet refeed replacement scheduler live`
- `62011ea` - `[Phase 6][UOW-2647] Execute pet spawn refeed scheduling live`
- Current commit - `[Phase 6][UOW-2648] Cancel active pet refeed on dismiss live`

## Session Summary

- Active pet cleanup now cancels pending C# refeed tasks before clearing active pet state.
- `SchedulePetRefeed` replacement and active pet delete now share `CancelPetRefeedTask`.
- Focused live packet coverage proves a refeed task scheduled by `CM_PET SPAWN` is canceled by `CM_PET DISMISS` and does not mutate the inactive owned pet after the original delay.

## Files Changed In UOW-2648

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2648-Completion.md`
- `docs/Phase-6-Session-2648-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.controllers.PetController.onDelete`
- `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.cancelRefeedTask`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_PET`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.ClearActivePetAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.CancelPetRefeedTask`
- `Aion.GameServer.Network.Aion.GameServerConnection.SchedulePetRefeed`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`

## Validation

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~ProcessPacketAsync_CmPetDismissCancelsPendingRefeedCallback --no-restore
```

Result: passed, 1/1.

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetDismiss|FullyQualifiedName~ProcessPacketAsync_CmPetSurrender|FullyQualifiedName~SchedulePetRefeed" --no-restore
```

Result: passed, 6/6.

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionBuyItemTests --no-restore
```

Result: passed, 84/84.

Java/Maven: not run. No narrow Java scheduler fixture exists for `PetController.onDelete`; Java source was reviewed directly.

Broad .NET: skipped after focused validation. Broad trigger existed because scheduler/live pet state behavior changed; the focused commands built affected projects and directly exercised dismiss, surrender, scheduler replacement, and adjacent pet paths.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PetController.onDelete` refeed cancellation | `Aion.GameServer.Network.Aion.GameServerConnection.ClearActivePetAsync` / `CancelPetRefeedTask` | Runtime lifecycle | Partial | Unit Tested | Partial Parity | Active pet clear now cancels pending refeed tasks before clearing pet state. Other delete-time persistence and mood behavior remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.cancelRefeedTask` | `Aion.GameServer.Network.Aion.GameServerConnection.CancelPetRefeedTask` | Runtime scheduler cancellation | Partial | Unit Tested | Partial Parity | Cancels the stored `ScheduledTask` handle and removes it from the connection task map. Task ownership still differs from Java's `PetCommonData` field. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` dismiss/surrender active delete path | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetDismissAsync` / `HandlePetSurrenderAsync` | Runtime handler | Partial | Unit Tested | Partial Parity | Both active dismiss and active surrender use `ClearActivePetAsync`; adjacent tests passed. |

## Known Gaps

- Java `PetController.onDelete` persists feed status, doping bag, despawn time, and mood data; C# active pet delete only covers active state/world cleanup and refeed cancellation so far.
- Java `PetSpawnService.summonPet` sends autoloot/autosell special-function packets on spawn when persisted flags are enabled; not yet wired.
- Periodic pet update scheduling and cancellation remain incomplete.
- Real client validation was not run.
- Real MySQL validation was not run.
- No Java/C# runtime timing comparison was run.

## Next Recommended Runtime UOW

**UOW-2649 candidate: send persisted pet auto-loot/auto-sell special-function packets on live pet spawn.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: spawning a pet with persisted auto-loot or auto-sell enabled should immediately send the Java-equivalent special-function activation packets.
- Java source method or runtime path: PetSpawnService.summonPet checks petCommonData.isLooting() and isSelling(), then sends new SM_PET(PetSpecialFunction.AUTOLOOT/AUTOSELL, true).
- C# runtime artifact to wire or fix: GameServerConnection.HandlePetSpawnAsync after the spawn packet, using PlayerOwnedPet.IsLooting and IsSelling plus existing SmPet.SpecialFunction packet support.
- Client-visible/packet effect expected: CM_PET SPAWN sends real SM_PET special-function activation packets for persisted enabled pet flags so the client reflects auto-loot/auto-sell state immediately after spawn.
- Why this is not preview-only/test-only/documentation-only: it sends real server packets from the live CM_PET SPAWN handler based on persisted runtime pet state.
```

Suggested focused validation:

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetSpawn|FullyQualifiedName~ProcessPacketAsync_CmPetAutoLoot|FullyQualifiedName~ProcessPacketAsync_CmPetAutoSell" --no-restore
```

Start with a single new spawn special-function test first, then run the filter above. Java/Maven is not expected unless a narrow Java packet fixture is discovered. Broad-validation trigger: live packet fanout behavior changes; run focused connection packet tests first.

## Safe Runtime Candidates

- Send persisted auto-loot/auto-sell activation packets from live pet spawn.
- Review pet feed status persistence on dismiss against `PetController.onDelete`, but only wire with live persistence behavior and focused repository coverage.
- Review periodic pet update scheduling if it can be tied to live packet/persistence effects rather than a preview.

## Context Needed By Next Session

- UOW-2640: accepted single-count feed consumes item, persists feed/inventory, sends subtype 2/subtype 5/end.
- UOW-2641: rejected food sends item unlock, subtype 5, END_FEEDING, and message `1400618`.
- UOW-2642: accepted multi-count food repeats until count reaches zero.
- UOW-2643: accepted pet feeding partial-stack inventory updates use Java `DEC_PET_FOOD = 0x5E`.
- UOW-2644: rewarded full feed grants inventory reward, persists refeed/feed state, sends subtype 6/7, and schedules refeed reset.
- UOW-2645: multiple valid loved rewards no longer collapse to the first valid reward in the live handler.
- UOW-2646: repeated refeed scheduling cancels stale callbacks and only the current scheduler callback mutates live pet state.
- UOW-2647: pet spawn schedules restored future refeed delays and resets no-delay food pets to hungry.
- UOW-2648: pet dismiss/active surrender cancels pending refeed callbacks.
- Runtime feed evaluation uses `StaticData.PetFeedData`, loaded from Java `pet_feed.xml` and `item_groups.xml`.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, execute a live handler path, or directly unblock one of those behaviors.
