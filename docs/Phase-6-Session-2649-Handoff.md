# Phase 6 Session 2649 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2649: Send persisted pet special-function spawn packets live. See
[Phase-6-Session-2649-Completion.md](Phase-6-Session-2649-Completion.md).

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
- `49c5a09` - `[Phase 6][UOW-2648] Cancel active pet refeed on dismiss live`
- Current commit - `[Phase 6][UOW-2649] Send persisted pet spawn function packets live`

## Session Summary

- Live `CM_PET SPAWN` now sends persisted AutoLoot and AutoSell activation packets after the spawn packet.
- The C# handler follows Java `PetSpawnService.summonPet` packet order for the reviewed special-function fanout.
- Focused live packet coverage proves spawn, AutoLoot active, and AutoSell active packets are emitted and serialized for an owned pet with persisted flags enabled.

## Files Changed In UOW-2649

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2649-Completion.md`
- `docs/Phase-6-Session-2649-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.toypet.PetSpawnService.summonPet`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_PET`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_PET`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetSpawnAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmPet.SpecialFunction`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`

## Validation

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~ProcessPacketAsync_CmPetSpawnSendsPersistedSpecialFunctionPackets --no-restore
```

Result: passed, 1/1.

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetSpawn|FullyQualifiedName~ProcessPacketAsync_CmPetAutoLoot|FullyQualifiedName~ProcessPacketAsync_CmPetAutoSell" --no-restore
```

Result: passed, 10/10.

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionBuyItemTests --no-restore
```

Result: passed, 85/85.

Java/Maven: not run. No narrow Java packet fixture exists for `PetSpawnService.summonPet` special-function fanout; Java source was reviewed directly.

Broad .NET: skipped after focused validation. Broad trigger existed because live packet fanout changed; the focused commands built affected projects and directly exercised spawn, auto-loot, auto-sell, and adjacent pet paths.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetSpawnService.summonPet` persisted special-function fanout | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetSpawnAsync` | Runtime handler | Partial | Unit Tested | Partial Parity | Live spawn now sends persisted AutoLoot and AutoSell activation packets after spawn. Other spawn lifecycle behavior remains incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET(PetSpecialFunction, boolean, int)` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet.SpecialFunction` / `SmPetSpecialFunctionSnapshot` | Packet | Partial | Unit Tested | Partial Parity | Packet writer is exercised from live spawn and asserted for the reviewed field shape. No raw Java golden byte comparison was run. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` spawn path | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetSpawnAsync` | Runtime packet path | Partial | Unit Tested | Partial Parity | SPAWN restores active-pet state, sends spawn, schedules restored refeed delay where applicable, and sends persisted special-function packets. Remaining Java spawn side effects are not complete. |

## Known Gaps

- Java `PetController.onDelete` persists feed status, doping bag, despawn time, and mood data; C# active pet delete only covers active state/world cleanup, refeed cancellation, and previously ported runtime pet mutations so far.
- Periodic pet update scheduling and cancellation remain incomplete.
- No raw Java golden packet comparison was run for special-function packets.
- Real client validation was not run.
- Real MySQL validation was not run.

## Next Recommended Runtime UOW

**UOW-2650 candidate: persist active pet feed status on dismiss/delete live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: active pet delete should preserve current feed status instead of losing hungry/progress/refeed data when the pet is dismissed or surrendered while active.
- Java source method or runtime path: PetController.onDelete sets PetCommonData cancel-feed state and calls PlayerPetsDAO.saveFeedStatus(objectId, hungryLevel, progressData, refeedTime).
- C# runtime artifact to wire or fix: GameServerConnection.ClearActivePetAsync and the player/pet repository persistence path for owned pet feed fields.
- Client-visible/state/persistence effect expected: CM_PET DISMISS and active-pet SURRENDER mutate live owned-pet cancel-feed/feed state and persist feed status through the existing database shape.
- Why this is not preview-only/test-only/documentation-only: it changes live pet delete state and database persistence from live packet handlers.
```

Suggested focused validation:

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetDismiss|FullyQualifiedName~ProcessPacketAsync_CmPetSurrender" --no-restore
```

Start by inspecting existing pet feed persistence APIs and Java `PlayerPetsDAO.saveFeedStatus`. If the existing repository shape cannot persist feed status safely, document that blocker and pick the next runtime candidate rather than adding preview-only scaffolding.

## Safe Runtime Candidates

- Persist active pet feed status on live dismiss/delete using the existing database shape if the repository layer already exposes or can safely add the Java-equivalent update.
- Review periodic pet update scheduling only if it can be tied to live packet or persistence effects.
- Review pet mood/despawn persistence only as a live delete/spawn state mutation, not as metadata readiness work.

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
- UOW-2649: pet spawn sends persisted AutoLoot/AutoSell activation packets after the spawn packet.
- Runtime feed evaluation uses `StaticData.PetFeedData`, loaded from Java `pet_feed.xml` and `item_groups.xml`.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, execute a live handler path, or directly unblock one of those behaviors.
