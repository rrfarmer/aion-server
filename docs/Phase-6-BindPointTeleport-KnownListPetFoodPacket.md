# Phase 6 Known-List Pet Food Packet

Date: May 27, 2026
Unit of Work: UOW-1311
Status: Packet serializer added from supplied snapshots; live food runtime remains disabled.

## Scope

This unit ports the Java `SM_PET.FOOD` server-packet branch to C# packet serialization only. Java remains the source of truth:

- `com.aionemu.gameserver.network.aion.serverpackets.SM_PET`
- `com.aionemu.gameserver.services.toypet.PetFeedProgress`
- `com.aionemu.gameserver.model.gameobjects.player.PetCommonData`

The C# packet uses `SmPetFoodSnapshot` because live `PetCommonData`, `PetFeedProgress`, feed scheduling, inventory mutation, and persistence are not ported yet.

## Java Packet Shape

After the `FOOD` action id, Java writes:

- `H 1`
- `C 1`
- `C subType`

Subtype bodies:

| Subtype | Java Meaning | Body |
|---|---|---|
| `1` | eat | `D feedProgress`, `D 0`, `D itemObjectId`, `D count` |
| `2` | eating successful | subtype `1` body plus `C 0` |
| `3` | not hungry | `D feedProgress`, `D refeedDelaySeconds` |
| `4` | cancel feed | `D feedProgress`, `D refeedDelaySeconds` |
| `5` | clean feed task | `D feedProgress`, `D refeedDelaySeconds` |
| `6` | give item | `D feedProgress`, `D 0`, `D itemObjectId`, `C 0` |
| `7` | present notification | `D feedProgress`, `D refeedDelaySeconds`, `D itemObjectId`, `D 0` |
| `8` | is full | `D feedProgress`, `D refeedDelaySeconds`, `D itemObjectId`, `D count` |

The C# writer preserves Java's no-default-body behavior for unknown subtypes: the common food header is written and no body follows. This is not claimed as a valid gameplay scenario; it is preserved so packet behavior does not silently diverge from the Java switch.

## Migration Parity Table - UOW-1311

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet / Serializer | Partial | Unit Tested | Partial Parity | `FOOD` now writes Java subtype bodies `1` through `8` from supplied snapshots. `MOOD`, live dispatch, and Java runtime vectors remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET(int subType, int itemObjectId, int count, Pet)` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet.Food` | Packet Factory | Complete for supplied-snapshot shape | Unit Tested | Partial Parity | C# takes packet-facing feed progress and refeed delay fields directly because live `PetCommonData`/`PetFeedProgress` are not ported. Unknown subtype behavior intentionally mirrors Java header-only output. |
| `com.aionemu.gameserver.services.toypet.PetFeedProgress` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetFoodSnapshot.FeedProgressData` | Model / Bit Packing | Partial | No Tests for bit packing | Needs Verification | Packet consumes supplied packed data only. Java bit packing, loved/regular counters, hungry level reset, saved-data decode, and precision/rounding around shifts remain unported. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | future C# pet common-data model plus `SmPetFoodSnapshot.RefeedDelaySeconds` | Model | Partial | Manual Only | Needs Verification | Packet consumes supplied refeed-delay seconds. Java wall-clock `getRefeedDelay() / 1000`, volatile task state, date/time handling, and scheduled feed state are unported. |
| `com.aionemu.gameserver.services.toypet.PetService.feedPet` | future C# live pet feeding runtime | Service | Not Started | Manual Only | Needs Verification | Inventory removal, feed progress mutation, scheduler cancellation/refeed, reward/present flow, persistence, and packet dispatch are not ported. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPet_FoodEatWritesJavaShape` | Packet Unit | `SM_PET.writeImpl` `FOOD` subtype `1` | Header, feed progress, zero placeholder, item object id, and count. | Source-derived field-order assertions. | No Java runtime vector or live feed state. |
| `SmPet_FoodEatingSuccessWritesTrailingStatusLikeJava` | Packet Unit | `SM_PET.writeImpl` `FOOD` subtype `2` | Subtype `2` body and trailing status byte. | Source-derived field-order assertions. | No Java runtime vector or live feed state. |
| `SmPet_FoodDelaySubtypesWriteProgressAndRefeedLikeJava` | Packet Unit | `SM_PET.writeImpl` `FOOD` subtypes `3`, `4`, `5` | Progress and refeed-delay seconds for not-hungry/cancel/clean branches. | Source-derived field-order assertions. | Does not verify Java wall-clock delay calculation. |
| `SmPet_FoodGiveItemWritesJavaShape` | Packet Unit | `SM_PET.writeImpl` `FOOD` subtype `6` | Progress, zero placeholder, item object id, and status byte. | Source-derived field-order assertions. | No Java runtime vector or reward item flow. |
| `SmPet_FoodPresentNotificationWritesJavaShape` | Packet Unit | `SM_PET.writeImpl` `FOOD` subtype `7` | Progress, refeed delay, item object id, and zero placeholder. | Source-derived field-order assertions. | No Java runtime vector or scheduled notification state. |
| `SmPet_FoodFullWritesJavaShape` | Packet Unit | `SM_PET.writeImpl` `FOOD` subtype `8` | Progress, refeed delay, item object id, and count. | Source-derived field-order assertions. | No Java runtime vector or live hunger/full calculation. |
| `SmPet_FoodUnknownSubtypeWritesJavaHeaderOnly` | Packet Unit | `SM_PET.writeImpl` switch default absence | Preserves Java header-only output for unknown subtype. | Source-derived field-order assertions. | Unknown subtype is not a validated gameplay path. |

## Remaining Risks

- Java runtime packet vectors are still unavailable locally.
- `PetFeedProgress.getDataForPacket()` bit packing is not implemented in C#.
- Java `commonData.getRefeedDelay() / 1000` date/time behavior is not implemented or verified.
- Live feed service behavior remains unported: inventory mutation, feed counters, scheduler state, present/reward item flow, persistence, and packet dispatch.
- Threading differences remain open around Java volatile cancel/refeed tasks and scheduled feed updates.
- Serialization is source-derived only and not yet validated through Java runtime capture.

## Next Recommended Unit of Work

Port or audit `SM_PET.MOOD` from supplied snapshots before live pet runtime work. Keep mood-point/cooldown mutation and reward-item flow as a later service/model unit unless the required `PetCommonData`, `PetMoodService`, inventory, and DAO surfaces are present.
