# Phase 6 Session 2653 Completion

## UOW

[Phase 6] UOW-2653: Execute live `CM_PET MOOD` subtype 1 interaction packets.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_PET MOOD subtype 1 with a shuggle emotion now executes the live Java pet interaction path instead of being parsed and ignored.
- Java source/runtime path: CM_PET.runImpl routes MOOD when emotionId != 0; PetMoodService.checkMood subtype 1 calls interactWithPet, which calls PetCommonData.increaseShuggleCounter and, when accepted, sends SM_PET(pet, 2, shuggleEmotion) followed by SM_PET(pet, 4, 0).
- C# runtime artifact wired: GameServerConnection.HandlePetMoodAsync now dispatches subtype 1 to HandlePetMoodInteractionAsync, using PlayerOwnedPet mood timing fields and existing SmPet.Mood subtype 2/subtype 4 packet writers.
- Client-visible/state effect: live CM_PET MOOD subtype 1 mutates active pet shuggle counter, mood cooldown, last-sent mood points, and cooldown expiry state, then sends Java-shaped SM_PET MOOD subtype 2 and subtype 4 packets when cooldown allows.
- Why this is runtime progress: this wires a parsed client packet action to live handler behavior, mutates active pet state, and sends real server packets from live code.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - `MOOD` routes when `emotionId != 0`, even for subtype 1.
- `game-server/src/com/aionemu/gameserver/services/toypet/PetMoodService.java`
  - `checkMood` subtype 1 calls `interactWithPet`.
  - `interactWithPet` calls `increaseShuggleCounter()` and sends `new SM_PET(pet, 2, shuggleEmotion)` then `new SM_PET(pet, 4, 0)` only when the counter increase succeeds.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
  - `increaseShuggleCounter` refuses active mood cooldown, otherwise starts cooldown and increments `shuggleCounter`.
  - `getMoodPoints(true)` lazily initializes mood start and clamps packet mood points to 9000.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - MOOD subtype 2 writes subtype, zero, mood points, and shuggle emotion, then updates last-sent points and mood cooldown.
  - MOOD subtype 4 writes subtype, mood points, mood cooldown remaining, and gift cooldown remaining, then updates last-sent points.

## C# Changes

- Split `GameServerConnection.HandlePetMoodAsync` into subtype-specific live branches.
- Preserved subtype 0 mood-start behavior from UOW-2652.
- Added `HandlePetMoodInteractionAsync` for subtype 1 when `EmotionId != 0`:
  - hydrates `PetCommonDataTiming` from active `PlayerOwnedPet`,
  - applies `IncreaseShuggleCounter`,
  - sends `SmPet.Mood` subtype 2 with current mood points and emotion id,
  - updates last-sent mood points and mood cooldown like Java `SM_PET` subtype 2,
  - sends `SmPet.Mood` subtype 4 with current mood points and cooldown seconds,
  - writes mutated timing fields back to active `PlayerOwnedPet`.
- Added focused live connection tests and packet assertion helpers for subtype 2 and subtype 4 emissions.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetMoodInteractionMutatesCounterCooldownAndSendsEmotionPackets` | Unit/live connection | `CM_PET.runImpl`, `PetMoodService.interactWithPet`, `PetCommonData.increaseShuggleCounter`, `SM_PET.writeImpl` source review | Live subtype 1 mutates shuggle/cooldown/last-sent state and sends subtype 2 then subtype 4 mood packets. | Runs actual connection packet dispatch and serializes emitted server packets. | Uses timing ranges for wall-clock cooldown seconds; does not run real client. |
| `ProcessPacketAsync_CmPetMoodInteractionDuringCooldownDoesNotSendPacket` | Unit/live connection | `PetCommonData.increaseShuggleCounter` source review | Active mood cooldown blocks subtype 1 interaction packet sends and counter mutation. | Runs actual connection packet dispatch with active pet cooldown state. | Does not validate audit/log behavior because Java only returns false here. |
| `ProcessPacketAsync_CmPetMoodInteractionWithoutEmotionDoesNothing` | Unit/live connection | `CM_PET.runImpl` `emotionId != 0` guard | Subtype 1 with zero emotion does not dispatch interaction behavior. | Runs actual connection packet dispatch. | None for scoped branch. |

## Validation Decision

```text
- Changed surface: live CM_PET MOOD subtype 1 dispatch, active owned-pet mood timing state, and SM_PET mood subtype 2/subtype 4 packet emission.
- Specific behavior/contract: Java PetMoodService.interactWithPet increments shuggle counter and sends subtype 2 then subtype 4 only when mood cooldown allows and CM_PET supplied a nonzero emotion id.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetMood" --no-restore
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetMood|FullyQualifiedName~SmPet_Mood|FullyQualifiedName~PetCommonDataTimingTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java unit fixture exists for PetMoodService.interactWithPet in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: live connection dispatch, active pet runtime state, and live side-effect packet sends changed.
- Broad .NET decision: skipped after focused validation because the filtered live connection, packet, and timing tests built affected projects and directly exercised the scoped packet/state contract.
- Why this scope is sufficient: the edited runtime path is isolated to CM_PET MOOD subtype 1; the focused tests verify the accepted interaction, cooldown rejection, zero-emotion guard, serialized subtype 2/subtype 4 payloads, and active pet timing mutation.
```

Results:

- Initial narrow run exposed an invalid test fixture for gift cooldown age; the fixture was corrected from 10 minutes to 70 minutes to exceed Java's 1-hour gift cooldown.
- Live mood-path filter after correction: passed, 5/5.
- Adjacent packet/timing filter: passed, 20/20.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces; no failures remained.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` MOOD subtype 1 branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetMoodAsync` / `HandlePetMoodInteractionAsync` | Runtime packet path | Partial | Unit Tested | Partial Parity | Subtype 1 with nonzero emotion now mutates active pet state and sends live subtype 2/subtype 4 packets. Subtype 3 gift and periodic task scheduling remain incomplete. |
| `com.aionemu.gameserver.services.toypet.PetMoodService.interactWithPet` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetMoodInteractionAsync` | Runtime service behavior | Partial | Unit Tested | Partial Parity | C# models the accepted and cooldown-rejected branches. Java null common-data guard is irrelevant for C# `PlayerOwnedPet` but active-pet lookup still gates the path. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.increaseShuggleCounter` | `Aion.GameServer.Services.ToyPet.PetCommonDataTiming.IncreaseShuggleCounter` / `PlayerOwnedPet` | Runtime state | Partial | Unit Tested | Partial Parity | Existing timing helper supplies Java-shaped cooldown mutation; live handler now applies it to active pet state. Wall-clock precision is range-tested. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` MOOD subtype 2/subtype 4 | `Aion.GameServer.Network.Aion.ServerPackets.SmPet.Mood` | Server packet | Partial | Unit Tested | Partial Parity | Existing packet writer emits Java-shaped subtype 2 and subtype 4 payloads and is now called by live dispatch. Subtype 3 gift packet remains not live-wired. |

## Known Gaps

- `CM_PET MOOD` subtype 3 gift path is still not wired; Java resolves condition reward, clears mood statistics, sends subtype 4 then subtype 3, starts gift cooldown, and grants the reward item when present.
- Java periodic `TaskId.PET_UPDATE` mood update scheduling/cancellation remains incomplete in C#.
- Java gift inventory-full rejection and audit logging are not yet modeled for subtype 3.
- Real client validation was not run.
- Real MySQL validation was not run.

## Next Recommended Runtime UOW

**UOW-2654 candidate: execute live `CM_PET MOOD` subtype 3 gift request rejection/packet path.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: CM_PET MOOD subtype 3 should execute Java PetMoodService.requestPresent instead of being parsed but ignored.
- Java source method or runtime path: CM_PET.runImpl routes subtype 3 when gift remaining time is zero; PetMoodService.requestPresent checks mood points, gift cooldown, inventory full, clears mood statistics, sends SM_PET(pet, 4, 0), sends SM_PET(pet, 3, 0), and grants condition reward through ItemService.addItem when nonzero.
- C# runtime artifact to wire or fix: GameServerConnection.HandlePetMoodAsync subtype 3 branch, active PlayerOwnedPet mood/gift timing fields, item template/inventory add path if reward grant is in scope, and existing SmPet.Mood subtype 3/subtype 4 packet writer.
- Client-visible/state/persistence effect expected: live CM_PET MOOD subtype 3 should at least mutate active pet mood/gift cooldown state and send Java-shaped subtype 4/subtype 3 packets when mood is full and gift cooldown allows; if safe, it should also grant the condition reward through existing inventory mutation.
- Why this is not preview-only/test-only/documentation-only: it wires a parsed client packet action to live handler behavior, mutates active pet mood state, sends real server packets, and may mutate live inventory state.
```

Suggested focused validation:

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetMood|FullyQualifiedName~SmPet_Mood|FullyQualifiedName~PetCommonDataTimingTests" --no-restore
```

Start narrower with new `ProcessPacketAsync_CmPetMoodGift...` tests if the combined filter is slow. Java/Maven is not expected unless a narrow Java packet or item-grant fixture is discovered; Java source review should drive the packet/state contract.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
