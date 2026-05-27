# Phase 6 - Pet Feed Subtype 7 Runtime Vector Design

Date: May 27, 2026
Unit of Work: UOW-1333
Status: Design complete; no Java runtime vectors were generated in this unit.

## Scope

This document defines the runtime-vector capture plan needed before enabling or claiming live parity for Java `SM_PET` FOOD subtype `7` in the rewarded pet-feed flow.

Java source of truth:

- `com.aionemu.gameserver.services.toypet.PetService.checkFeeding`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_PET`
- `com.aionemu.gameserver.model.gameobjects.player.PetCommonData`
- `com.aionemu.gameserver.model.gameobjects.player.PetFeedProgress`
- `com.aionemu.gameserver.dao.PlayerPetsDAO.setTime`

The current C# surface can construct source-derived subtype `7` metadata, but it does not know whether future live dispatch should snapshot feed progress and refeed delay at queue time or packet serialization time.

## Java Behavior Needing Runtime Evidence

Rewarded feed path in `PetService.checkFeeding` sends packet objects in this order when the pet reaches `FULL` and `PetFlavour.processFeedResult` returns a reward:

1. `new SM_PET(2, item.getObjectId(), 0, pet)`
2. `new SM_PET(6, reward.getItem(), 0, pet)`
3. `new SM_PET(5, 0, 0, pet)`
4. `new SM_EMOTION(player, EmotionType.END_FEEDING, 0, player.getObjectId())`
5. `new SM_PET(7, 0, 0, pet)`
6. `ItemService.addItem(player, reward.getItem(), 1)`
7. `commonData.scheduleRefeed(delay)`
8. `commonData.setRefeedTime(System.currentTimeMillis() + delay)`
9. `PlayerPetsDAO.setTime(pet.getObjectId(), refeedTime)`
10. `progress.reset()`

`SM_PET` subtype `7` serializes:

1. FOOD action/header
2. subtype `7`
3. `commonData.getFeedProgress().getDataForPacket()`
4. `(int) commonData.getRefeedDelay() / 1000`
5. `itemObjectId`
6. trailing `0`

The constructor receives `itemObjectId = 0`, but the progress and refeed delay are read from mutable `PetCommonData` during packet writing. If Java socket serialization happens immediately inside `PacketSendUtility.sendPacket`, subtype `7` may observe pre-schedule progress and a zero refeed delay. If serialization happens after the feed method mutates common data, subtype `7` may observe reset progress and the scheduled cooldown.

## Minimum Runtime Vectors

| Vector | Java Path | Required Fixture State | Fields To Capture | Why It Matters |
|---|---|---|---|---|
| Rewarded feed, immediate subtype sequence | `PetService.checkFeeding` full reward branch | Pet one feed away from `FULL`; reward group returns deterministic reward; cooldown non-zero; item count `1` | Packet order and decoded fields for subtypes `2`, `6`, `5`, `7`, plus `SM_EMOTION` id/path | Confirms ordering around reward, feed-end, emotion, and refeed notification. |
| Rewarded feed, subtype `7` mutable state | Same branch | Same fixture, with captured `feedProgressData` before reset and after reset | Subtype `7` progress data, refeed-delay seconds, item object id, trailing zero | Determines queue-time versus serialization-time state observation. |
| Rewarded feed with repeat count greater than one | Same branch with requested count greater than remaining needed to reach `FULL` | Food stack count greater than one; reward triggered before recursive repeat can continue | Subtype `2` count, subtype `7` fields, absence/presence of follow-up scheduled feed packet before cooldown | Guards against accidentally applying the normal repeat-feed path after a reward. |
| Rewarded feed cooldown boundary | Same branch with smallest available positive cooldown | Cooldown minutes known from `pet_feed.xml` or fixture; fixed Java wall-clock metadata if possible | Subtype `7` refeed-delay seconds and persisted DAO time delta | Confirms Java minute-to-millisecond and millisecond-to-second truncation behavior. |

## Preferred Capture Harness Shape

The most useful Java harness should run the real `PetService.checkFeeding` method with a controlled player, pet, feed item, flavour, reward, and packet sink.

Recommended setup:

1. Install a packet observer around `PacketSendUtility.sendPacket` and `broadcastPacket` that records packet class, constructor-facing semantic label if available, and serialized unencrypted body bytes.
2. Force packet serialization at the same point production Java performs it. Do not manually serialize later unless the artifact records that this was delayed serialization.
3. Create a pet whose `PetFeedProgress` is one accepted feed away from `PetHungryLevel.FULL`.
4. Use a deterministic food item and `PetFlavour` entry with:
   - known food type;
   - deterministic reward item id;
   - known positive cooldown in minutes;
   - no random reward ambiguity.
5. Record mutable state snapshots immediately before calling `checkFeeding`, immediately after `sendPacket(new SM_PET(7...))` is observed if instrumentation can hook that point, and immediately after `checkFeeding` returns.
6. Record whether `PlayerPetsDAO.setTime` was called and with which pet object id and timestamp.

Preferred output fields:

- vector name;
- Java class/method path;
- Java git commit/source revision;
- packet sequence index;
- packet class;
- decoded action id and subtype for `SM_PET`;
- canonical unencrypted packet body hex;
- decoded subtype `7` progress data;
- decoded subtype `7` refeed-delay seconds;
- decoded subtype `7` item object id;
- decoded subtype `7` trailing integer;
- pre-call feed progress data;
- post-call feed progress data;
- refeed delay before call;
- refeed delay after call;
- persisted refeed timestamp delta from call start;
- whether serialization was production send-time or delayed manual serialization.

## Fixture Values To Standardize

Use non-zero and non-equal values so field-order mistakes are visible:

| Field | Suggested Value | Notes |
|---|---:|---|
| Player object id | `7001` | Matches nearby C# metadata tests. |
| Pet object id | `7101` | Distinct from player, item, and reward ids. |
| Food item object id | `5001` | Should appear in subtype `2`, not subtype `7`. |
| Food item id | `188000001` | Use a fixture-backed valid feed item if this id is not valid in Java data. |
| Reward item id | `186000001` | Must be deterministic through the selected reward group. |
| Reward cooldown | `1` minute or larger | Positive cooldown is required to expose delay semantics. |
| Starting requested count | `1` and `2` | Capture both single-feed reward and repeat-count boundary. |
| Feed progress data | non-zero pre-reset | Must differ from reset data. |

## Expected C# Decisions After Vectors Exist

Do not enable live C# subtype `7` dispatch until vectors answer these questions:

1. Does Java subtype `7` serialize pre-reset or post-reset feed-progress data?
2. Does Java subtype `7` serialize zero delay, full cooldown delay, or a near-cooldown delay after truncation?
3. Does the packet body always use item object id `0` in the rewarded branch?
4. Does Java socket send ordering place subtype `7` before reward item-add packets on the wire, or can asynchronous serialization reorder observable payloads?
5. Is DAO persistence complete before any client-visible reward item-add packet serialization?

Current C# `PetFeedPacketMetadataBridge` should continue accepting supplied `FeedProgressData` and `RefeedDelaySeconds` rather than deriving live values. `IsJavaRuntimeParity` must remain `false` until generated Java artifacts are compared.

## C# Readiness Mapping

| Java Requirement | Current C# Surface | Status |
|---|---|---|
| FOOD subtype `7` packet serializer | `Aion.GameServer.Network.Aion.ServerPackets.SmPet.Food(SmPetFoodSnapshot)` | Partial, source-derived tests only. |
| Feed operation order | `PetFeedServiceOperationPlanner` | Partial, unit-tested operation intents only. |
| Metadata bridge subtype `7` construction | `PetFeedPacketMetadataBridge` | Partial, non-sending metadata only. |
| Feed progress data | `PetFeedProgress` plus supplied bridge value | Partial; no live common-data wiring. |
| Refeed delay seconds | `PetCommonDataTiming` plus supplied bridge value | Partial; no live scheduling/serialization timing evidence. |
| Scheduler and DAO side effects | operation intents only | Blocked. |
| Java runtime vectors | none for feed subtype `7` | Blocked. |
| Live packet dispatch | none | Blocked. |

## Migration Parity Table - UOW-1333

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` rewarded full branch | `docs/Phase-6-BindPointTeleport-PetFeedSubtype7RuntimeVectorDesign.md`; `PetFeedServiceOperationPlanner` | Service Flow / Design | Partial | Manual Only | Needs Verification | Source review defines packet and side-effect order. Runtime vectors are required because packet objects read mutable pet state during serialization. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` FOOD subtype `7` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet.Food`; `PetFeedPacketMetadataBridge` | Packet / Serializer / Metadata Bridge | Partial | Unit Tested source-derived | Needs Verification | C# can write the source-derived field shape, but no Java runtime byte capture verifies feed-progress or delay values. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.getRefeedDelay` | `PetCommonDataTiming.GetRefeedDelay`; supplied `RefeedDelaySeconds` | Timing / Mutable State | Partial | Unit Tested source-derived | Needs Verification | C# models deterministic timing math, but live subtype `7` must know when Java reads the mutable state relative to `setRefeedTime`. |
| `com.aionemu.gameserver.model.gameobjects.player.PetFeedProgress.reset` | `PetFeedProgress.Reset`; supplied `FeedProgressData` | Progress / Mutable State | Partial | Unit Tested source-derived | Needs Verification | Runtime vectors must show whether subtype `7` sees pre-reset or post-reset packet data. |
| `com.aionemu.gameserver.dao.PlayerPetsDAO.setTime` | `PetFeedServiceOperationKind.PersistRefeedTime` | Persistence Boundary | Not Started | Manual Only | Needs Verification | DAO execution and timestamp binding are not wired. Vector plan records expected persistence timing only. |

## Tests Added

No executable tests were added in UOW-1333. This was a documentation/design unit based on Java source review and the completed read-only subtype `7` audit.

## Remaining Risks

- No Java runtime subtype `7` vectors exist yet.
- Source review cannot prove whether Java serialization observes pre-mutation or post-mutation `PetCommonData`.
- Java wall-clock timing can make cooldown seconds drift by one or more seconds unless capture metadata records call start/end timestamps.
- Java reward selection may be random unless the fixture controls reward groups.
- Java packet observer tooling may need a send-pipeline hook rather than manual packet serialization.
- Live C# scheduler, reward creation, DAO persistence, inventory mutation, and socket dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 runtime-vector design document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime packet observer, deterministic feed fixture, live common-data wiring, scheduler, reward creation, DAO persistence, inventory mutation, socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a guarded Java feed subtype `7` vector artifact schema/reader on the C# side, or investigate whether the existing Java packet-vector harness can be extended to observe production `PacketSendUtility.sendPacket` timing for pet feed packets. Keep live feed dispatch disabled until Java artifacts exist and compare cleanly.
