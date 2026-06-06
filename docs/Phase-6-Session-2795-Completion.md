# Phase 6 Session 2795 Completion

## Unit Of Work

`[Phase 6][UOW-2795] Wire live quest finish cube expansion rewards`

## Runtime Progress Gate

- Deferred/live behavior advanced: live self-target reportable auto-reward quest finish now executes Java `QuestService.giveReward` cube expansion rewards.
- Java source of truth: `game-server/src/com/aionemu/gameserver/services/QuestService.java` `giveReward` branch `if (rewards.getExtendInventory() == 1) CubeExpandService.questExpand(player)`, plus `CubeExpandService.questExpand -> expand(player, 3)`.
- C# runtime artifact wired: `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` now allows and applies `QuestFinishRewardNonItemAction.CubeExpansion` descriptors through the existing quest expansion plan, live `Player.QuestExpands` mutation, and live cube packets.
- Client-visible/state/persistence effect changed: supported quest finish cube expansion rewards increment live `Player.QuestExpands`, send `SmSystemMessage.InventorySizeExtended(9)`, send `SmCubeUpdate.CubeSize`, and then send `SmQuestAction.Update`.
- Why this is not preview-only/test-only/documentation-only: the socket handler mutates live player cube expansion state and sends real cube expansion packets during quest finish.

## Java Parity Notes

- Java `CubeExpandService.expand(player, 3)` calls `canExpand`, sends `STR_EXTEND_INVENTORY_SIZE_EXTENDED(9)`, increments `PlayerCommonData.questExpands`, recalculates cube limit, and sends `SM_CUBE_UPDATE.cubeSize(StorageType.CUBE, player)`.
- C# reuses `QuestRewardSideEffectPlanService.CreateCubeExpansionPlan` for the Java `canExpand` boundary, then mutates `Player.QuestExpands` and sends `SmCubeUpdate.CubeSize(player)` from the live handler.
- `Player.QuestExpands` changed from `init` to `set` so live code can mirror Java `PlayerCommonData.setQuestExpands`.
- Direct quest-finish cube expansion persistence was not newly wired. Existing logout and periodic general player saves already write `quest_expands`; Java `CubeExpandService.questExpand` itself does not call a DAO.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live socket-side quest finish reward execution plus one player model setter.
- Specific behavior/contract: Java `QuestService.giveReward` cube expansion branch increments quest cube expansions, emits the size-expanded/cube-update packet chain, and completes the quest.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardSideEffectPlanServiceTests|FullyQualifiedName~CubeExpandNotificationPlanServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/giveReward` live cube expansion packet fanout.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; the filtered command compiled the affected project and exercised the edited socket boundary plus adjacent cube expansion plan/packet intent tests.
- Why this scope is sufficient: the test drives `HandleDialogSelectAsync` with a live cube expansion reward projection and observes player quest-expansion state, decoded `SmCubeUpdate` expansion counts, and quest completion ordering.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardSideEffectPlanServiceTests|FullyQualifiedName~CubeExpandNotificationPlanServiceTests"
```

Result: Passed, 22 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/giveReward` socket-side cube expansion mutation and packet fanout.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_ReportableAutoRewardQuestAppliesCubeExpansionAndCompletesQuest` | Regression | Java source review: `QuestService.giveReward` and `CubeExpandService.questExpand` | Live socket quest finish applies cube expansion reward, sends size-expanded/cube-update packets, mutates `Player.QuestExpands`, and completes quest | Filtered C# boundary test with decoded cube packet fields | Does not assert DB persistence |
| `CreateCubeExpansionPlan_PlansQuestExpandWithoutMutatingPlayer` | Unit | Java source review: `CubeExpandService.questExpand` | Java expansion boundary and intended quest-expansion count/slot intents | Existing focused C# unit test | Plan-level evidence only |
| `CreatePlan_UsesJavaNineSlotMessageForEveryExpandType` | Unit | Java source review: `CubeExpandService.expand` hardcoded slot count | Inventory-size-expanded system message uses Java slot count 9 | Existing focused C# unit test | Notification plan evidence only |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` self-target auto-reward branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Socket handler | Partial | Regression Tested | Partial Parity | Supported XP, kinah, fixed item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, and cube expansion rewards are live for guarded self-target reportable auto-reward quests. |
| `com.aionemu.gameserver.services.QuestService.giveReward` cube expansion branch | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleQuestFinishAutoRewardAsync` | Runtime player state mutation | Partial | Regression Tested | Partial Parity | Calls the existing cube expansion plan from live quest finish, mutates quest-expansion count, and sends cube expansion packets before quest completion. |
| `com.aionemu.gameserver.services.CubeExpandService.questExpand/expand` | `Aion.GameServer.Services.QuestRewardSideEffectPlanService.CreateCubeExpansionPlan` plus live `GameServerConnection.TryApplyQuestFinishCubeExpansionRewardAsync` | Service / socket side effect | Partial | Unit Tested / Regression Tested | Partial Parity | Java `canExpand`, quest-expands increment, 9-slot message, and cube update are covered for quest finish. NPC/item expansion branches remain separate. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.setQuestExpands` | `Aion.GameServer.Model.GameObjects.Player.QuestExpands` | Model property | Partial | Regression Tested | Partial Parity | Property is now mutable so live code can mirror Java common-data mutation. Persistence remains through existing periodic/logout general save paths. |

## Parity Status

Partial parity improved for the self-target/reportable auto-reward quest finish branch. XP, kinah, fixed item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, and cube expansion rewards now run live for the guarded branch. Selectable rewards, warehouse rewards, challenge tasks, quest completion callbacks, NPC faction completion, NPC-target dialog paths, direct quest-finish reward persistence, and broad nearby quest fanout remain incomplete.
