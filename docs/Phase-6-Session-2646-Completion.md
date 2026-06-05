# Phase 6 Session 2646 Completion

## UOW

[Phase 6] UOW-2646: Execute pet refeed replacement scheduler live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: repeated pet reward feeds now replace an existing delayed refeed reset instead of allowing stale scheduler callbacks to mutate live pet state later.
- Java source/runtime path: PetCommonData.scheduleRefeed calls cancelRefeedTask before storing a new ThreadPoolManager task; the callback sets refeedTime = 0 and hungryLevel = HUNGRY.
- C# runtime artifact wired: GameServerConnection.SchedulePetRefeed now stores ScheduledTask handles per pet object id, cancels the previous task on replacement, and only lets the currently registered callback mutate player.OwnedPets.
- Client-visible/state effect: after the latest refeed delay, live pet state becomes hungry again; a canceled older callback cannot later overwrite a newer live pet state and affect future CM_PET feed handling.
- Why this is runtime progress: this changes live scheduled player-pet state mutation through ThreadPoolManager, not a preview, metadata, or documentation path.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
  - `scheduleRefeed(long)` cancels the previous task, schedules a delayed callback, sets `refeedTime = 0`, and sets `feedProgress.hungryLevel = HUNGRY`.
  - `cancelRefeedTask()` calls `Future.cancel(false)` when a previous task exists.
- `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
  - `checkFeeding` schedules the refeed delay after reward completion.

## C# Changes

- Added a per-connection `ConcurrentDictionary<int, ScheduledTask>` for live pet refeed tasks keyed by pet object id.
- Updated `GameServerConnection.SchedulePetRefeed` to cancel any previous task before scheduling a replacement.
- Guarded the delayed callback so only the currently registered scheduled task can remove itself and mutate `player.OwnedPets`.
- Made `SchedulePetRefeed` internal for focused runtime coverage through the existing `InternalsVisibleTo` test assembly.
- Extended the buy-item/pet-feed fixture to inject a real `ThreadPoolManager`.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SchedulePetRefeed_CancelsPreviousTaskBeforeReplacementCallbackMutatesPetState` | Unit/live scheduler | `PetCommonData.scheduleRefeed` and `cancelRefeedTask` source review | The latest scheduled callback clears refeed time and sets hungry; the canceled previous callback does not later overwrite the pet after the test restores it to full. | Runs the real `ThreadPoolManager` callback against live `Player.OwnedPets` state through `GameServerConnection.SchedulePetRefeed`. | Does not compare Java thread timing at runtime; no real client or MySQL validation. |

## Validation Decision

```text
- Changed surface: live pet refeed scheduler state mutation.
- Specific behavior/contract: replacement scheduling must cancel stale refeed callbacks and the latest callback must reset RefeedTimeMillis to 0 and HungryLevel to Hungry.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~SchedulePetRefeed_CancelsPreviousTaskBeforeReplacementCallbackMutatesPetState --no-restore
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionBuyItemTests --no-restore
- Focused Java/Maven command: not run; no narrow Java scheduler fixture exists for PetCommonData.scheduleRefeed in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: scheduler/live pet state behavior changed.
- Broad .NET decision: skipped after focused validation because the filtered connection tests built affected projects and directly exercised the changed scheduler method plus adjacent live pet-feed paths.
- Why this scope is sufficient: the edited behavior is isolated to GameServerConnection pet refeed scheduling and is covered by a real ThreadPoolManager callback test plus the existing pet-feed connection class.
```

Results:

- New focused scheduler test: passed, 1/1.
- `GameServerConnectionBuyItemTests`: passed, 82/82.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces during the first compile; no failures remained.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.scheduleRefeed` | `Aion.GameServer.Network.Aion.GameServerConnection.SchedulePetRefeed` | Runtime scheduler | Partial | Unit Tested | Partial Parity | Live C# now cancels replacement tasks and the latest callback mutates live pet state to hungry. Java stores the task on `PetCommonData`; C# stores per-connection handles keyed by pet object id. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.cancelRefeedTask` | `Aion.GameServer.Network.Aion.GameServerConnection.SchedulePetRefeed` replacement branch | Runtime scheduler cancellation | Partial | Unit Tested | Partial Parity | Replacement cancellation is covered with a stale-callback suppression test. Broader lifecycle cancellation on pet dismiss/logout still needs source-driven review. |
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` refeed scheduling branch | `Aion.GameServer.Network.Aion.GameServerConnection.ExecutePetFeedingCheckAsync` reward branch | Runtime handler | Partial | Unit Tested | Partial Parity | Rewarded feed schedules the live reset and replacement semantics now match reviewed Java behavior. Real client/DB validation not run. |

## Known Gaps

- Java stores the refeed task on `PetCommonData`; C# currently stores handles on `GameServerConnection`, so lifecycle behavior during disconnect, pet dismiss, and summon restoration still needs targeted review.
- Real client validation was not run.
- Real MySQL validation was not run.
- No runtime comparison against Java scheduler timing was run.
- Pet spawn/enter-world restoration of an existing future refeed delay remains to be verified or wired.

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

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
