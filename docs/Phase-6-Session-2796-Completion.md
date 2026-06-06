# Phase 6 Session 2796 Completion

## Unit Of Work

`[Phase 6][UOW-2796] Wire live quest finish warehouse expansion rewards`

## Runtime Progress Gate

- Deferred/live behavior advanced: live self-target reportable auto-reward quest finish now executes Java `QuestService.giveReward` warehouse expansion rewards.
- Java source of truth: `game-server/src/com/aionemu/gameserver/services/QuestService.java` `giveReward` branch `else if (rewards.getExtendInventory() == 2) WarehouseService.expand(player, false)`, plus `WarehouseService.expand` and `WarehouseService.sendWarehouseInfo(player, false)`.
- C# runtime artifact wired: `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` now allows and applies `QuestFinishRewardNonItemAction.WarehouseExpansion` descriptors through the existing warehouse expansion plan, live `Player.WarehouseBonusExpands` mutation, and live warehouse packets.
- Client-visible/state/persistence effect changed: supported quest finish warehouse expansion rewards increment live `Player.WarehouseBonusExpands`, send `SmSystemMessage.WarehouseSizeExtended(8)`, send `SmWarehouseInfo.CreateRegularWarehouseUpdatePackets`, and then send `SmQuestAction.Update`.
- Why this is not preview-only/test-only/documentation-only: the socket handler mutates live player warehouse expansion state and sends real warehouse expansion packets during quest finish.

## Java Parity Notes

- Java `WarehouseService.expand(player, false)` calls `canExpand`, sends `STR_EXTEND_CHAR_WAREHOUSE_SIZE_EXTENDED(8)`, increments `PlayerCommonData.whBonusExpands`, recalculates warehouse limit, and calls `sendWarehouseInfo(player, false)`.
- Java `sendWarehouseInfo(player, false)` sends regular warehouse item chunks, a regular warehouse terminator packet, and an account warehouse terminator packet.
- C# reuses `QuestRewardSideEffectPlanService.CreateWarehouseExpansionPlan` for the Java `canExpand` boundary, then mutates `Player.WarehouseBonusExpands` and sends `SmWarehouseInfo.CreateRegularWarehouseUpdatePackets(player, staticData.ItemTemplates, staticData.ItemRestrictionCleanups)` from the live handler.
- Direct quest-finish warehouse expansion persistence was not newly wired. Existing logout and periodic general player saves already write `wh_bonus_expands`; Java `WarehouseService.expand(player, false)` itself does not call a DAO.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live socket-side quest finish reward execution.
- Specific behavior/contract: Java `QuestService.giveReward` warehouse expansion branch increments bonus warehouse expansions, emits the warehouse-size-expanded/warehouse-info packet chain, and completes the quest.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardSideEffectPlanServiceTests|FullyQualifiedName~WarehouseExpandNotificationPlanServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/giveReward` live warehouse expansion packet fanout.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; the filtered command compiled the affected project and exercised the edited socket boundary plus adjacent warehouse expansion plan/packet intent tests.
- Why this scope is sufficient: the test drives `HandleDialogSelectAsync` with a live warehouse expansion reward projection and observes player warehouse expansion state, decoded warehouse info packet fields, and quest completion ordering.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardSideEffectPlanServiceTests|FullyQualifiedName~WarehouseExpandNotificationPlanServiceTests"
```

Result: Passed, 22 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/giveReward` socket-side warehouse expansion mutation and packet fanout.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_ReportableAutoRewardQuestAppliesWarehouseExpansionAndCompletesQuest` | Regression | Java source review: `QuestService.giveReward` and `WarehouseService.expand(player, false)` | Live socket quest finish applies warehouse expansion reward, sends size-expanded/warehouse-info packets, mutates `Player.WarehouseBonusExpands`, and completes quest | Filtered C# boundary test with decoded `SmWarehouseInfo` fields | Does not assert DB persistence |
| `CreateWarehouseExpansionPlan_PlansBonusExpansionAndPackets` | Unit | Java source review: `WarehouseService.expand(player, false)` | Java expansion boundary and intended bonus-expansion count/packet intents | Existing focused C# unit test | Plan-level evidence only |
| `CreatePlan_UsesJavaEightSlotMessageForEveryExpandType` | Unit | Java source review: `WarehouseService.expand` hardcoded slot count | Warehouse-size-expanded system message uses Java slot count 8 | Existing focused C# unit test | Notification plan evidence only |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` self-target auto-reward branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Socket handler | Partial | Regression Tested | Partial Parity | Supported XP, kinah, fixed item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, cube expansion rewards, and warehouse expansion rewards are live for guarded self-target reportable auto-reward quests. |
| `com.aionemu.gameserver.services.QuestService.giveReward` warehouse expansion branch | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleQuestFinishAutoRewardAsync` | Runtime player state mutation | Partial | Regression Tested | Partial Parity | Calls the existing warehouse expansion plan from live quest finish, mutates bonus expansion count, and sends warehouse expansion packets before quest completion. |
| `com.aionemu.gameserver.services.WarehouseService.expand/sendWarehouseInfo` | `Aion.GameServer.Services.QuestRewardSideEffectPlanService.CreateWarehouseExpansionPlan` plus live `GameServerConnection.TryApplyQuestFinishWarehouseExpansionRewardAsync` | Service / socket side effect | Partial | Unit Tested / Regression Tested | Partial Parity | Java `canExpand`, wh-bonus increment, 8-slot message, and regular warehouse info refresh are covered for quest finish. NPC and item expansion branches remain separate. |

## Parity Status

Partial parity improved for the self-target/reportable auto-reward quest finish branch. XP, kinah, fixed item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, cube expansion rewards, and warehouse expansion rewards now run live for the guarded branch. Selectable rewards, challenge tasks, quest completion callbacks, NPC faction completion, NPC-target dialog paths, direct quest-finish reward persistence, and broad nearby quest fanout remain incomplete.
