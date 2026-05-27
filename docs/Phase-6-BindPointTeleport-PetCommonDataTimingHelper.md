# Phase 6 Pet Common Data Timing Helper

Date: May 27, 2026
Unit of Work: UOW-1314
Status: Deterministic pet common-data timing helper ported; live pet common-data model remains disabled.

## Scope

This unit ports deterministic timing and counter behavior from Java `PetCommonData` into an isolated C# helper:

- `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.getBirthday`
- `getRefeedDelay`
- `getMoodPoints`
- `increaseShuggleCounter`
- `clearMoodStatistics`
- `getMoodRemainingTime`
- `getGiftRemainingTime`

The helper is not a full live `PetCommonData` model. It does not own pet templates, `PetFeedProgress`, `PetDopingBag`, expirable behavior, packet sending, scheduled tasks, or DAO persistence.

## Migration Parity Table - UOW-1314

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `Aion.GameServer.Services.ToyPet.PetCommonDataTiming` | Model Helper / Timing Utility | Partial | Unit Tested | Partial Parity | Ports deterministic birthday epoch conversion, refeed delay mutation, mood point calculation, cooldown remaining seconds, shuggle counter increment, and mood-stat reset. Full common-data ownership, template-driven feed/doping setup, expirable callbacks, volatile task fields, scheduler integration, packet dispatch, and persistence remain unported. |
| `java.sql.Timestamp` usage in `PetCommonData.getBirthday` | `System.DateTimeOffset?` in `PetCommonDataTiming.ToBirthdayEpochSeconds` | Date/Time | Refactored | Unit Tested | Intentional Difference | Java stores `Timestamp` and divides epoch milliseconds by 1000. C# helper accepts `DateTimeOffset?` to keep epoch conversion explicit. Null maps to `0` like Java. |
| `java.lang.Math.round(float)` in `PetCommonData.getMoodPoints` | `PetCommonDataTiming.GetMoodPoints` | Precision / Rounding | Complete for helper | Unit Tested | Partial Parity | Uses Java-style `floor(floatSeconds + 0.5)` behavior for deterministic millisecond inputs. Live wall-clock source is supplied by caller and not wired to runtime. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` refeed task in `PetCommonData.scheduleRefeed` | future C# scheduled refeed service | Scheduler / Threading | Not Started | Manual Only | Needs Verification | This helper only models `getRefeedDelay` timestamp mutation. Java `Future<?>`, volatile task field, cancellation, and scheduled hungry-level reset remain unported. |
| `com.aionemu.gameserver.services.toypet.PetAdoptionService` via `PetCommonData.onExpire` | future C# pet expiration runtime | Service | Not Started | Manual Only | Needs Verification | Expiration system-message send and surrender call are not represented in this timing helper. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ToBirthdayEpochSecondsReturnsZeroForMissingTimestampLikeJava` | Unit | `PetCommonData.getBirthday` | Null birthday maps to `0`. | Source-derived deterministic assertion. | Does not verify DB timestamp loading. |
| `ToBirthdayEpochSecondsConvertsTimestampMillisLikeJava` | Unit | `PetCommonData.getBirthday` | Epoch milliseconds divide to epoch seconds. | Source-derived deterministic assertion. | Uses `DateTimeOffset` instead of Java `Timestamp`. |
| `GetRefeedDelayReturnsRemainingMillisAndClearsExpiredTimeLikeJava` | Unit | `PetCommonData.getRefeedDelay` | Remaining delay and expired timestamp reset. | Source-derived deterministic assertion. | No scheduled refeed task integration. |
| `GetMoodPointsLazilyStartsClockAndCapsPacketValueLikeJava` | Unit | `PetCommonData.getMoodPoints` | Lazy start time, packet cap at `9000`, non-packet uncapped value. | Source-derived deterministic assertion. | No live wall-clock wiring. |
| `GetMoodPointsUsesJavaRoundFloatSeconds` | Unit | `PetCommonData.getMoodPoints` | Java-style half-up float-second rounding at millisecond boundaries. | Source-derived deterministic assertion. | Negative elapsed times are not separately covered. |
| `GetMoodRemainingTimeReturnsSecondsAndClearsExpiredCooldownLikeJava` | Unit | `PetCommonData.getMoodRemainingTime` | 600-second mood cooldown and expired cooldown reset. | Source-derived deterministic assertion. | No live packet-time mutation wiring. |
| `GetGiftRemainingTimeReturnsSecondsAndClearsExpiredCooldownLikeJava` | Unit | `PetCommonData.getGiftRemainingTime` | 3600-second gift cooldown and expired cooldown reset. | Source-derived deterministic assertion. | No reward service integration. |
| `IncreaseShuggleCounterHonorsMoodCooldownAndSetsCooldownStartLikeJava` | Unit | `PetCommonData.increaseShuggleCounter` | Cooldown gate, counter increment, and cooldown timestamp set. | Source-derived deterministic assertion. | No `PetMoodService` packet ordering. |
| `ClearMoodStatisticsResetsStartAndCounterOnlyLikeJava` | Unit | `PetCommonData.clearMoodStatistics` | Resets only start mood time and counter. | Source-derived deterministic assertion. | Full common-data lifecycle remains unported. |

## Remaining Risks

- Full C# `PetCommonData` model still does not exist.
- Java volatile fields and `Future<?>` refeed task behavior are not represented.
- Scheduler integration for `scheduleRefeed` and hungry-level reset remains unported.
- Template-driven feed/doping initialization remains unported.
- DAO load/save wiring for mood, gift, refeed, and despawn timestamps remains unported.
- Expirable callback behavior and packet sending remain unported.
- Java runtime vectors are still unavailable locally.

## Next Recommended Unit of Work

Map or port the pet repository SQL contract for `PlayerPetsDAO`, focusing first on non-executing command plans and DTOs for `player_pets` rows before adding live DB execution.
