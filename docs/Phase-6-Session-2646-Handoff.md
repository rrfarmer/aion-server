# Phase 6 Session 2646 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2646: Execute pet refeed replacement scheduler live. See
[Phase-6-Session-2646-Completion.md](Phase-6-Session-2646-Completion.md).

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
- Current commit - `[Phase 6][UOW-2646] Execute pet refeed replacement scheduler live`

## Session Summary

- Live C# pet refeed scheduling now keeps `ScheduledTask` handles per pet object id.
- Repeated reward-feed scheduling cancels the previous delayed task before replacing it, matching Java `PetCommonData.scheduleRefeed`.
- The delayed callback only mutates `Player.OwnedPets` if it is still the current registered task, preventing stale callbacks from overwriting newer pet state.
- Focused scheduler coverage proves the latest callback resets refeed/hungry state and the canceled previous callback does not later mutate the pet.

## Files Changed In UOW-2646

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2646-Completion.md`
- `docs/Phase-6-Session-2646-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.scheduleRefeed`
- `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.cancelRefeedTask`
- `com.aionemu.gameserver.services.toypet.PetService.checkFeeding`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.SchedulePetRefeed`
- `Aion.GameServer.Network.Aion.GameServerConnection.ExecutePetFeedingCheckAsync`
- `Aion.GameServer.Utils.ThreadPoolManager`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`

## Validation

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~SchedulePetRefeed_CancelsPreviousTaskBeforeReplacementCallbackMutatesPetState --no-restore
```

Result: passed, 1/1.

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionBuyItemTests --no-restore
```

Result: passed, 82/82.

Java/Maven: not run. No narrow Java scheduler fixture exists for `PetCommonData.scheduleRefeed` in this checkout; Java source was reviewed directly.

Broad .NET: skipped after focused validation. Broad trigger existed because scheduler/live pet state behavior changed; the focused commands built affected projects and directly exercised the changed scheduler method and adjacent live pet-feed paths.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.scheduleRefeed` | `Aion.GameServer.Network.Aion.GameServerConnection.SchedulePetRefeed` | Runtime scheduler | Partial | Unit Tested | Partial Parity | Live replacement scheduling now cancels the previous task and the current callback resets pet state to hungry. C# task ownership is connection-scoped rather than pet-common-data-scoped. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.cancelRefeedTask` | `Aion.GameServer.Network.Aion.GameServerConnection.SchedulePetRefeed` replacement branch | Runtime scheduler cancellation | Partial | Unit Tested | Partial Parity | Replacement cancellation is covered; pet dismiss/logout lifecycle cancellation still needs targeted review. |
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` refeed scheduling branch | `Aion.GameServer.Network.Aion.GameServerConnection.ExecutePetFeedingCheckAsync` | Runtime handler | Partial | Unit Tested | Partial Parity | Rewarded feed invokes the scheduler; repeated scheduling no longer leaves stale callback mutation. |

## Known Gaps

- Java task ownership is on `PetCommonData`; C# task ownership is currently on `GameServerConnection`.
- Pet dismiss/logout lifecycle cancellation is not yet reviewed against Java `PetController` cleanup.
- Existing future refeed delays on restored pets are not yet scheduled during enter-world/summon restoration.
- Real client validation was not run.
- Real MySQL validation was not run.
- No Java/C# runtime timing comparison was run.

## Next Recommended Runtime UOW

**UOW-2647 candidate: execute restored pet refeed scheduling during live pet restoration/summon.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: pets restored with a future refeed time should schedule a delayed hungry reset when they become live, matching Java spawn behavior.
- Java source method or runtime path: PetSpawnService.spawnPet checks petCommonData.getRefeedDelay() > 0 and calls petCommonData.scheduleRefeed(petCommonData.getRefeedDelay()).
- C# runtime artifact to wire or fix: PlayerEnterWorldService/GameServerConnection restored pet handling and SchedulePetRefeed invocation for restored active/summoned pet state.
- Client-visible/state effect expected: a restored pet with a future RefeedTimeMillis becomes hungry after the remaining delay, so later CM_PET feed attempts observe the Java-equivalent state.
- Why this is not preview-only/test-only/documentation-only: it would schedule a real ThreadPoolManager callback that mutates live player pet state after restore/summon.
```

Suggested focused validation:

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Start narrower if a single restored-pet enter-world test covers the edited path. Java/Maven is not expected unless a narrow Java pet-spawn scheduler fixture is discovered. Broad-validation trigger: scheduler/live pet state behavior changes; run focused connection or enter-world scheduler tests first.

## Safe Runtime Candidates

- Wire restored active/summoned pet refeed delay scheduling from Java `PetSpawnService`.
- Review pet dismiss/logout task cancellation against `PetController` and only implement if it changes live state/lifecycle behavior.
- Pair any DB validation for pet refeed fields with a live restore/scheduler fix; do not do evidence-only validation as a standalone UOW.

## Context Needed By Next Session

- UOW-2640: accepted single-count feed consumes item, persists feed/inventory, sends subtype 2/subtype 5/end.
- UOW-2641: rejected food sends item unlock, subtype 5, END_FEEDING, and message `1400618`.
- UOW-2642: accepted multi-count food repeats until count reaches zero.
- UOW-2643: accepted pet feeding partial-stack inventory updates use Java `DEC_PET_FOOD = 0x5E`.
- UOW-2644: rewarded full feed grants inventory reward, persists refeed/feed state, sends subtype 6/7, and schedules refeed reset.
- UOW-2645: multiple valid loved rewards no longer collapse to the first valid reward in the live handler.
- UOW-2646: repeated refeed scheduling cancels stale callbacks and only the current scheduler callback mutates live pet state.
- Runtime feed evaluation uses `StaticData.PetFeedData`, loaded from Java `pet_feed.xml` and `item_groups.xml`.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, execute a live handler path, or directly unblock one of those behaviors.
