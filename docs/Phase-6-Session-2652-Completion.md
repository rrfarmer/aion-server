# Phase 6 Session 2652 Completion

## UOW

[Phase 6] UOW-2652: Execute live `CM_PET MOOD` subtype 0 mood-start packet.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_PET MOOD subtype 0 now executes the live Java mood-start path instead of being parsed and ignored.
- Java source/runtime path: CM_PET.runImpl gates active-pet MOOD subtype 0 on PetCommonData.getMoodRemainingTime() == 0 and routes to PetMoodService.checkMood; PetMoodService.startCheckingMood sends new SM_PET(pet, 0, 0).
- C# runtime artifact wired: GameServerConnection.HandlePetAsync now dispatches PetAction.Mood to HandlePetMoodAsync; PlayerOwnedPet carries transient last-sent mood points; SmPet.Mood writes the existing Java-shaped subtype 0 payload.
- Client-visible/state effect: a live active-pet CM_PET MOOD subtype 0 sends SM_PET MOOD subtype 0 when no mood cooldown is active and initializes/restores in-memory pet mood timing like Java PetCommonData.getMoodPoints(true).
- Why this is runtime progress: this wires a parsed client packet action to live handler behavior, mutates active owned-pet mood timing, and sends a real server packet from live code.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - `MOOD` requires an active pet and routes subtype 0 only when `pet.getCommonData().getMoodRemainingTime() == 0`.
- `game-server/src/com/aionemu/gameserver/services/toypet/PetMoodService.java`
  - `checkMood` subtype 0 calls `startCheckingMood`.
  - `startCheckingMood` sends `new SM_PET(pet, 0, 0)`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - MOOD subtype 0 writes action id, subtype byte, and delta between current mood points and last sent points.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
  - `getMoodPoints(true)` lazily initializes `startMoodTime`, clamps packet points to 9000, and uses Java rounding.
  - `getMoodRemainingTime()` clears expired cooldowns and returns remaining cooldown seconds.

## C# Changes

- Added transient `PlayerOwnedPet.LastSentMoodPoints` for Java `PetCommonData.lastSentPoints` runtime state.
- Added live `PetAction.Mood` dispatch in `GameServerConnection.HandlePetAsync`.
- Added `HandlePetMoodAsync` for subtype 0 only:
  - requires an active summoned pet,
  - hydrates `PetCommonDataTiming` from active `PlayerOwnedPet`,
  - applies the Java mood cooldown gate,
  - computes packet mood points with `GetMoodPoints(forPacket: true)`,
  - sends `SmPet.Mood(new SmPetMoodSnapshot(SubType: 0, ...))`,
  - writes mutated timing fields back to the active owned pet.
- Added focused live connection tests and local payload/assertion helpers for `CM_PET MOOD` subtype 0.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetMoodStartSendsMoodCheckPacketAndInitializesTiming` | Unit/live connection | `CM_PET.runImpl`, `PetMoodService.startCheckingMood`, `SM_PET.writeImpl`, `PetCommonData.getMoodPoints(true)` source review | Live `CM_PET MOOD` subtype 0 initializes active pet mood timing and sends `SM_PET MOOD` subtype 0 with delta 0 for a new timer. | Runs actual connection packet dispatch and serializes the emitted server packet. | Does not cover subtype 1 interaction, subtype 3 gift, subtype 4 periodic updates, or real client timing. |
| `ProcessPacketAsync_CmPetMoodStartDuringCooldownDoesNotSendPacket` | Unit/live connection | `CM_PET.runImpl` mood remaining-time gate | Live `CM_PET MOOD` subtype 0 sends no packet while Java mood cooldown is active. | Runs actual connection packet dispatch with active pet cooldown state. | Does not validate expiry on a real scheduler tick. |

## Validation Decision

```text
- Changed surface: live CM_PET MOOD dispatch, active owned-pet mood timing state, and SM_PET mood packet emission.
- Specific behavior/contract: Java CM_PET MOOD subtype 0 calls PetMoodService.startCheckingMood only when mood cooldown remaining time is zero, sending SM_PET(pet, 0, 0).
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetMood" --no-restore
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetMood|FullyQualifiedName~CmPetTests|FullyQualifiedName~SmPet_Mood" --no-restore
- Focused Java/Maven command: not run; no narrow Java unit fixture exists for PetMoodService.startCheckingMood in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: live connection dispatch and live side-effect packet send changed.
- Broad .NET decision: skipped after focused validation because the filtered live connection, parser, and server-packet tests built affected projects and directly exercised the scoped packet/state contract.
- Why this scope is sufficient: the edited runtime path is isolated to CM_PET MOOD subtype 0; the focused tests verify the active-pet cooldown gate, timing mutation, and serialized SM_PET mood payload.
```

Results:

- New live mood-path filter: passed, 2/2.
- Adjacent parser/packet filter: passed, 37/37.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces; no failures remained.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` MOOD subtype 0 branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetAsync` / `HandlePetMoodAsync` | Runtime packet path | Partial | Unit Tested | Partial Parity | Subtype 0 active-pet cooldown gate now sends live `SM_PET MOOD`; subtype 1 interaction, subtype 3 gift, subtype 4 periodic update scheduling, and nonzero-emotion routing remain incomplete. |
| `com.aionemu.gameserver.services.toypet.PetMoodService.startCheckingMood` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetMoodAsync` | Runtime service behavior | Partial | Unit Tested | Partial Parity | C# sends the Java-shaped mood-check packet from live code. Remaining `PetMoodService.checkMood` subtypes are not wired. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` mood timing fields | `Aion.GameServer.Services.ToyPet.PetCommonDataTiming` / `Aion.GameServer.Model.GameObjects.PlayerOwnedPet` | Runtime state | Partial | Unit Tested | Partial Parity | C# reuses Java-shaped timing math and now carries transient last-sent mood points. Persisted mood fields are saved on dismiss from UOW-2651; last-sent points are intentionally runtime-only like Java. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` MOOD subtype 0 | `Aion.GameServer.Network.Aion.ServerPackets.SmPet.Mood` | Server packet | Partial | Unit Tested | Partial Parity | Existing packet writer emits Java-shaped subtype 0 payload and is now called by live dispatch. Other mood subtypes remain packet-tested but not all are live-wired. |

## Known Gaps

- `CM_PET MOOD` subtype 1 interaction path is still not wired; Java increments shuggle counter, sends subtype 2 and subtype 4 packets, and starts mood cooldown.
- `CM_PET MOOD` subtype 3 gift path is still not wired; Java resolves condition reward and starts gift cooldown.
- Java periodic `TaskId.PET_UPDATE` mood update scheduling/cancellation is still incomplete in C#.
- Last-sent mood points are modeled as runtime-only state and are not persisted, matching Java's non-DAO field.
- Real client validation was not run.
- Real MySQL validation was not run.

## Next Recommended Runtime UOW

**UOW-2653 candidate: execute live `CM_PET MOOD` subtype 1 interaction packets.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: CM_PET MOOD subtype 1 with a shuggle emotion should execute Java PetMoodService.interactWithPet instead of being parsed but ignored.
- Java source method or runtime path: CM_PET.runImpl routes MOOD when emotionId != 0; PetMoodService.checkMood subtype 1 calls interactWithPet, which calls PetCommonData.increaseShuggleCounter and, when accepted, sends SM_PET(pet, 2, shuggleEmotion) followed by SM_PET(pet, 4, 0).
- C# runtime artifact to wire or fix: GameServerConnection.HandlePetMoodAsync subtype 1 branch, active PlayerOwnedPet mood timing fields, and existing SmPet.Mood subtype 2/subtype 4 packet writer.
- Client-visible/state effect expected: live CM_PET MOOD subtype 1 mutates active pet shuggle counter/cooldown state and sends the Java-shaped subtype 2 and subtype 4 mood packets when cooldown allows.
- Why this is not preview-only/test-only/documentation-only: it wires a parsed client packet action to live handler behavior, mutates active pet state, and sends real server packets from live code.
```

Suggested focused validation:

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetMood|FullyQualifiedName~SmPet_Mood|FullyQualifiedName~PetCommonDataTimingTests" --no-restore
```

Start narrower with new `ProcessPacketAsync_CmPetMoodInteraction...` tests if the combined filter is slow. Java/Maven is not expected unless a narrow Java packet fixture is discovered; Java source review should drive the packet/state contract.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
