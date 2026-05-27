# Phase 6 Pet Feed Progress Helper

Date: May 27, 2026
Unit of Work: UOW-1313
Status: Deterministic feed-progress and hungry-level helpers ported; live pet food runtime remains disabled.

## Scope

This unit ports the compact Java helper behavior behind feed progress packet data:

- `com.aionemu.gameserver.services.toypet.PetFeedProgress`
- `com.aionemu.gameserver.services.toypet.PetHungryLevel`

The helper is not wired into live `PetCommonData`, `PetService.feedPet`, DAO persistence, or packet factories yet.

## Migration Parity Table - UOW-1313

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedProgress` | `Aion.GameServer.Services.ToyPet.PetFeedProgress` | Model / Bit Packing | Complete for standalone helper | Unit Tested | Partial Parity | Ports total-point masking, regular/loved counters, loved-food limit masking, loved-feed reset behavior, packet packing, and saved-data decode. Not wired into live common data, DAO, feed calculator, or packets. |
| `com.aionemu.gameserver.services.toypet.PetHungryLevel` | `Aion.GameServer.Services.ToyPet.PetHungryLevel` / `PetHungryLevelExtensions` | Enum / Utility | Complete for enum helper | Unit Tested | Partial Parity | Java ids and cycle order are preserved. C# `FromId` throws `ArgumentOutOfRangeException`; Java indexes `values()[value]` and throws an array bounds exception for unknown ids. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | future C# pet common-data model | Model | Not Started for live wiring | Manual Only | Needs Verification | Still needs ownership of `PetFeedProgress`, saved data loading, hungry-level restore, refeed timing, and packet-facing projection. |
| `com.aionemu.gameserver.services.toypet.PetFeedCalculator` | future C# feed calculator | Service / Calculator | Not Started | Manual Only | Needs Verification | Discovered dependency for changing hungry levels and feed counters. Calculator thresholds, reward selection, and loved-food behavior remain unported. |
| `com.aionemu.gameserver.dao.PlayerPetsDAO` | future C# pet repository | Repository | Not Started | Manual Only | Needs Verification | Persisted feed status and hungry-level load/save are not wired. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GetDataForPacketPacksJavaBitFields` | Unit | `PetFeedProgress.getDataForPacket` | Regular count, total points, loved count, and low unknown bits packing. | Source-derived deterministic assertion. | No Java runtime vector. |
| `SetDataDecodesJavaSavedDataAndReencodesWithPointLowBitsCleared` | Unit | `PetFeedProgress.setData` | Saved-data decode and re-encode of packet data. | Source-derived deterministic assertion. | Does not load from DAO. |
| `TotalPointsMasksToJavaFourteenBits` | Unit | `PetFeedProgress.setTotalPoints` | Fourteen-bit total-point mask and packet output. | Source-derived deterministic assertion. | No feed calculator integration. |
| `RegularCountPreservesJavaUnsignedByteView` | Unit | `PetFeedProgress.getRegularCount` | Unsigned byte view of Java short counter. | Source-derived deterministic assertion. | Counter overflow behavior beyond this view is not runtime-tested. |
| `ResetPreservesJavaLovedFeededOneShotBehavior` | Unit | `PetFeedProgress.reset` | Loved-feed reset clears only the flag once, then resets total/regular counters. | Source-derived deterministic assertion. | No service scheduling integration. |
| `PetHungryLevelCyclesLikeJava` | Unit | `PetHungryLevel.getNextValue` / `fromId` | Hungry-level ids, cycle order, and unknown-id rejection. | Source-derived deterministic assertion. | C# exception type intentionally differs from Java array bounds exception. |

## Remaining Risks

- The helper is not yet connected to `SmPetFoodSnapshot` or pet common data.
- Live feed calculator thresholds and reward selection remain unported.
- DAO loading/saving of feed status and hungry level remains unported.
- Java runtime packet vectors are still unavailable locally.
- Unknown hungry-level exception type differs intentionally by C# convention.
- Threading and scheduled feed-task behavior remains outside this helper.

## Next Recommended Unit of Work

Audit and port the next deterministic pet prerequisite, likely `PetCommonData` feed/mood timing helpers or the pet repository SQL map, before enabling live pet service mutation.
