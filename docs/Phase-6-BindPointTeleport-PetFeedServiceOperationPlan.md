# Phase 6 Pet Feed Service Operation Plan

Date: May 27, 2026
Unit of Work: UOW-1325
Status: Non-live pet feed service operation planning added; live feed execution remains disabled.

## Scope

This unit adds a deterministic, non-executing operation plan for the Java `PetService.checkFeeding` feed branch around the offline evaluator.

It covers:

- cancel-feed no-op behavior
- non-eatable/loved-limit rejected food operation order
- accepted-food inventory decrement intent before feed-result handling
- not-full progress packet and repeat-schedule intent
- last-requested-food end-feed packet/emotion intent
- full/reward packet order, reward item add intent, refeed scheduling metadata, refeed timestamp persistence intent, and progress reset intent

It does not mutate inventory, unlock items, create reward items, send packets, schedule tasks, set live refeed timestamps, write `PlayerPetsDAO`, or call live `DataManager`.

## Migration Parity Table - UOW-1325

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` | `Aion.GameServer.Services.ToyPet.PetFeedServiceOperationPlanner.CreatePlan` | Service Operation Planner | Partial | Unit Tested | Partial Parity | Models non-live operation order for cancel, rejected food, accepted not-full feed, final count end-feed, and rewarded feed paths. No live mutation, scheduler execution, packet send, DAO write, or item service call occurs. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `PetFeedServiceOperationKind.UnlockFoodItem` | Packet / Inventory Boundary | Not Started | Unit Tested as operation intent | Needs Verification | Captures unlock intent for rejected food only. Concrete packet serialization/call behavior is not ported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` feed branches | `PetFeedServiceOperationKind.SendPetFeedEndPacket`, `SendPetFeedProgressPacket`, `SendPetRewardItemPacket`, `SendPetRefeedPacket` | Packet Operation Intent | Partial | Unit Tested as operation order | Needs Verification | Captures Java feed packet subtype order and payload-facing ids/counts. Concrete serializer and socket dispatch are not invoked by the planner. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` end-feeding branch | `PetFeedServiceOperationKind.SendEndFeedingEmotion` | Packet Operation Intent | Partial | Unit Tested as operation order | Needs Verification | Captures end-feeding emission points. Concrete emotion packet construction and broadcast context remain unported here. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_TOYPET_FEED_FOOD_NOT_LOVEFLAVOR` | `PetFeedServiceOperationKind.SendFoodNotLovedSystemMessage` | Packet Operation Intent | Partial | Unit Tested as operation order | Needs Verification | Captures rejected-food system-message intent with fed item id. Pet name and localized item name resolution are not modeled. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseItemCount` | `PetFeedServiceOperationKind.DecreaseFoodItemCount` | Inventory Mutation Intent | Not Started | Unit Tested as operation order | Needs Verification | Captures one-item decrement after food acceptance and before post-feed packet handling. Live inventory mutation/failure behavior is not implemented. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` | `PetFeedServiceOperationKind.AddRewardItem` | Inventory Reward Intent | Not Started | Unit Tested as operation order | Needs Verification | Captures reward item add intent after reward packets, matching Java order. Item creation failure behavior is not modeled. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.schedule` feed/refeed calls | `PetFeedServiceOperationKind.ScheduleNextFeedCheck` / `ScheduleRefeed` | Scheduler Intent | Not Started | Unit Tested as operation order | Needs Verification | Captures repeat-feed and refeed scheduling intent only. Threading, cancellation, and time-unit execution are not ported. |
| `com.aionemu.gameserver.dao.PlayerPetsDAO.setTime` | `PetFeedServiceOperationKind.PersistRefeedTime` | Repository Intent | Not Started | Unit Tested as operation order | Needs Verification | Captures refeed-time persistence intent. SQL execution, transaction behavior, and timestamp binding are not run. |
| `com.aionemu.gameserver.services.toypet.PetFeedProgress.reset` reward path caller | `PetFeedServiceOperationKind.ResetFeedProgress` | State Mutation Intent | Partial | Unit Tested as operation order | Needs Verification | Captures the Java reward-path reset point after refeed persistence intent. Planner does not execute the reset. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlan_CancelledFeedReturnsNoOperationsLikeJava` | Unit | `PetService.checkFeeding` cancel guard | Cancelled feed returns no side-effect operations and leaves progress unchanged. | Source-derived deterministic assertion. | No scheduled-task cancellation runtime. |
| `CreatePlan_RejectedFoodPlansUnlockEndAndSystemMessageInJavaOrder` | Unit | `PetService.checkFeeding` non-eatable branch | Rejected food emits unlock, `SM_PET(5)`, end emotion, and not-loved system message in order. | Source-derived deterministic assertion. | Concrete packets not sent. |
| `CreatePlan_LovedLimitRejectedFoodDoesNotDecreaseItemLikeJava` | Unit | `PetService.checkFeeding` loved-limit precheck | Loved food with exhausted limit follows rejected path and does not plan item decrement. | Source-derived deterministic assertion. | Concrete system message not validated. |
| `CreatePlan_ConsumedNotFullWithRemainingCountPlansDecreaseProgressAndRescheduleLikeJava` | Unit | Java accepted not-full branch | Accepted non-full feed plans decrement, progress packet with `--count`, and repeat schedule. | Source-derived deterministic assertion. | No actual scheduler execution. |
| `CreatePlan_ConsumedLastRequestedFoodPlansEndFeedingLikeJava` | Unit | Java accepted no-remaining-count branch | Last accepted feed plans decrement, progress packet with zero count, end packet, and end emotion. | Source-derived deterministic assertion. | No socket dispatch. |
| `CreatePlan_RewardedFeedPlansRewardCooldownDaoAndResetInJavaOrder` | Unit | Java full/reward branch | Rewarded feed plans decrement, progress packet, reward packet, end packet/emotion, refeed packet, add item, schedule, timestamp set/persist, and progress reset in order. | Source-derived deterministic assertion. | No item service, scheduler, DAO, or reset execution. |

## Remaining Risks

- The planner records operation intent only; no live inventory, packet, scheduler, DAO, or reward item side effects execute.
- Concrete `SM_PET` feed packet serialization is already partially available but not composed here.
- Pet name and item localization for the rejected-food system message are not modeled.
- Java scheduler timing/cancellation and thread handoff are not verified.
- `PlayerPetsDAO.setTime` SQL execution and timestamp binding remain unverified.
- Reward item add failure behavior is not modeled.
- Java runtime packet/order comparison is unavailable.

## Summary Metrics

- Total Java artifacts discovered: 10 grouped artifact rows in this unit
- Total artifacts ported: 1 operation planner, 2 operation enums, 2 operation/plan DTOs, and 6 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 10 grouped rows
- Total blocked artifacts: live inventory decrement/unlock, concrete packet construction/dispatch, system-message localization, scheduler execution, reward item creation, DAO writes, reset execution, Java runtime packet comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Bridge the feed operation plan to concrete non-sending packet metadata for the existing `SmPet` feed branch and available emotion/system-message descriptors. Keep repository, scheduler, inventory mutation, reward item creation, and socket dispatch disabled.
