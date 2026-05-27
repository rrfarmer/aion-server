# Phase 6 Known-List Pet Mood Packet

Date: May 27, 2026
Unit of Work: UOW-1312
Status: Packet serializer added from supplied snapshots; live mood runtime remains disabled.

## Scope

This unit ports the Java `SM_PET.MOOD` server-packet branch to C# packet serialization only. Java remains the source of truth:

- `com.aionemu.gameserver.network.aion.serverpackets.SM_PET`
- `com.aionemu.gameserver.services.toypet.PetMoodService`
- `com.aionemu.gameserver.model.gameobjects.player.PetCommonData`

The C# packet uses `SmPetMoodSnapshot` because live `PetCommonData`, mood point timing, cooldown mutation, inventory reward flow, and persistence are not ported yet.

## Java Packet Shape

After the `MOOD` action id, Java writes only subtype-specific bodies:

| Subtype | Java Meaning | Body |
|---|---|---|
| `0` | check pet status | `C subType`, `D delta`, where delta is `moodPoints - lastSentPoints` only when mood increased; otherwise `0` |
| `2` | emotion sent | `C subType`, `D 0`, `D moodPoints`, `D shuggleEmotion` |
| `3` | give gift | `C subType`, `D conditionReward` |
| `4` | periodic update | `C subType`, `D moodPoints`, `D moodRemainingSeconds`, `D giftRemainingSeconds` |

Java has no default case for unknown subtypes, so the C# writer preserves action-only output for unknown `MOOD` subtypes.

## Migration Parity Table - UOW-1312

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet / Serializer | Partial | Unit Tested | Partial Parity | `MOOD` now writes Java subtype bodies `0`, `2`, `3`, and `4` from supplied snapshots. Live dispatch and Java runtime vectors remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET(Pet pet, int subType, int shuggleEmotion)` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet.Mood` | Packet Factory | Complete for supplied-snapshot shape | Unit Tested | Partial Parity | C# takes packet-facing mood/cooldown/reward fields directly. Java packet-time mutations of `lastSentPoints`, `moodCdStarted`, and `giftCdStarted` are not performed. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | future C# pet common-data model plus `SmPetMoodSnapshot` | Model | Partial | Manual Only | Needs Verification | Packet consumes supplied mood points, last-sent points, and remaining cooldown seconds. Java lazy start-time initialization, `Math.round((now-start)/1000f)`, 9000 packet cap, and cooldown reset behavior are unported. |
| `com.aionemu.gameserver.services.toypet.PetMoodService` | future C# live pet mood runtime | Service | Not Started | Manual Only | Needs Verification | Interaction, present request validation, inventory-full handling, mood clear/increment, reward item add, and packet ordering are not ported. |
| `com.aionemu.gameserver.services.item.ItemService` | future C# item reward service | Service | Not Started for this flow | Manual Only | Needs Verification | Mood gift packet may precede Java reward-item add; live inventory mutation and system-message behavior remain outside this packet slice. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPet_MoodCheckWritesDeltaWhenMoodIncreasedLikeJava` | Packet Unit | `SM_PET.writeImpl` `MOOD` subtype `0` | Writes positive mood delta. | Source-derived field-order assertions. | Does not mutate `lastSentPoints`. |
| `SmPet_MoodCheckWritesZeroWhenMoodDidNotIncreaseLikeJava` | Packet Unit | `SM_PET.writeImpl` `MOOD` subtype `0` | Writes zero when mood did not increase. | Source-derived field-order assertions. | Does not mutate `lastSentPoints`. |
| `SmPet_MoodEmotionWritesMoodAndEmotionLikeJava` | Packet Unit | `SM_PET.writeImpl` `MOOD` subtype `2` | Writes zero placeholder, mood points, and shuggle emotion. | Source-derived field-order assertions. | Does not mutate last-sent points or mood cooldown start. |
| `SmPet_MoodGiftWritesConditionRewardLikeJava` | Packet Unit | `SM_PET.writeImpl` `MOOD` subtype `3` | Writes condition reward id. | Source-derived field-order assertions. | Does not mutate gift cooldown or add item. |
| `SmPet_MoodPeriodicUpdateWritesCooldownsLikeJava` | Packet Unit | `SM_PET.writeImpl` `MOOD` subtype `4` | Writes mood points, mood remaining time, and gift remaining time. | Source-derived field-order assertions. | Does not mutate last-sent points. |
| `SmPet_MoodUnknownSubtypeWritesActionOnlyLikeJava` | Packet Unit | `SM_PET.writeImpl` switch default absence | Preserves action-only output for unknown subtype. | Source-derived field-order assertions. | Unknown subtype is not a validated gameplay path. |

## Remaining Risks

- Java runtime packet vectors are still unavailable locally.
- Java packet construction mutates `PetCommonData`; the snapshot writer does not.
- Mood timing and cooldown date/time behavior is not implemented in C#.
- Java `Math.round` mood-point calculation and 9000 packet cap are not implemented.
- Live `PetMoodService` behavior remains unported: validation, inventory checks, reward item add, packet ordering, audit logging, and persistence.
- Serialization is source-derived only and not yet validated through Java runtime capture.

## Next Recommended Unit of Work

Move from packet-only prerequisites into a small deterministic model helper, preferably `PetFeedProgress.getDataForPacket()` bit packing, before enabling live food/mood service behavior. Keep live mutation disabled until `PetCommonData`, inventory, timers, and DAO surfaces exist.
