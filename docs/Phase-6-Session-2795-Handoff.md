# Phase 6 Session 2795 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2795] Wire live quest finish cube expansion rewards`

Commit made in this session:

- `[Phase 6][UOW-2795] Wire live quest finish cube expansion rewards`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles the Java self-target/reportable quest auto-reward branch for supported XP, kinah, fixed item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, and cube expansion rewards.
- Cube expansion rewards are now admitted by the live auto-reward descriptor allow-list.
- Live cube expansion quest rewards call `QuestRewardSideEffectPlanService.CreateCubeExpansionPlan`, mutate `Player.QuestExpands`, send `SmSystemMessage.InventorySizeExtended(9)`, send `SmCubeUpdate.CubeSize(player)`, and then send `SmQuestAction.Update`.
- `Player.QuestExpands` is now mutable so live code can mirror Java `PlayerCommonData.setQuestExpands`.
- Direct quest-finish cube expansion persistence was not newly wired. Java's quest expansion branch mutates online common data and does not call a DAO; C# logout/periodic general saves already write `quest_expands`.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` self-target `SELECTED_QUEST_AUTO_REWARD*` branch.
- `com.aionemu.gameserver.services.QuestService.finishQuest`.
- `com.aionemu.gameserver.services.QuestService.giveReward`.
- `com.aionemu.gameserver.services.CubeExpandService.questExpand`.
- `com.aionemu.gameserver.services.CubeExpandService.expand`.
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`.
- `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.setQuestExpands`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2795-Completion.md`.
- `docs/Phase-6-Session-2795-Handoff.md`.

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

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DIALOG_SELECT` self-target auto-reward branch | `GameServerConnection.HandleDialogSelectAsync` | Socket handler | Partial | Regression Tested | Partial Parity | Supported XP, kinah, fixed item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, and cube expansion rewards are live for guarded self-target reportable auto-reward quests. |
| `QuestService.giveReward` cube expansion branch | `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` | Runtime player state mutation | Partial | Regression Tested | Partial Parity | Calls existing cube expansion plan from live quest finish, mutates quest-expansion count, and sends cube expansion packets before quest completion. |
| `CubeExpandService.questExpand/expand` | `QuestRewardSideEffectPlanService.CreateCubeExpansionPlan` plus `GameServerConnection.TryApplyQuestFinishCubeExpansionRewardAsync` | Service / socket side effect | Partial | Unit Tested / Regression Tested | Partial Parity | Java `canExpand`, quest-expands increment, 9-slot message, and cube update are covered for quest finish. NPC/item expansion branches remain separate. |
| `PlayerCommonData.setQuestExpands` | `Player.QuestExpands` | Model property | Partial | Regression Tested | Partial Parity | Property is now mutable so live code can mirror Java common-data mutation. Persistence remains through existing periodic/logout general save paths. |

## Known Gaps

- Live quest finish still does not support selectable item rewards, class-selectable rewards, bonus rewards, warehouse expansions, challenge task completion, quest-completed callbacks, NPC faction completion, NPC-target dialog quest paths, or broad nearby quest refresh fanout.
- Direct reward persistence is not newly wired in quest finish for AP/DP/GP/cube expansions. Existing later player-save behavior may persist mutated runtime state, but these UOWs did not add direct quest-finish persistence calls.
- Cube expansion `canExpand` negative-overflow no-packet branch is represented in code but not covered by the socket fixture because normal player expansion counts do not hit it.
- No Java runtime/golden fixture exists for quest finish socket cube expansion behavior.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2796] Wire live quest finish warehouse expansion rewards`

- Deferred/live behavior advanced: expand quest finish live execution to Java `QuestService.giveReward` warehouse expansion rewards.
- Java source of truth: `QuestService.giveReward` branch `else if (rewards.getExtendInventory() == 2) WarehouseService.expand(player, false)`, plus `WarehouseService.expand` and `WarehouseService.sendWarehouseInfo(player, false)`.
- C# runtime artifact to wire/fix: `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` should admit `QuestFinishRewardNonItemAction.WarehouseExpansion`, apply `QuestRewardSideEffectPlanService.CreateWarehouseExpansionPlan`, mutate `Player.WarehouseBonusExpands`, send `SmSystemMessage.WarehouseSizeExtended(8)`, send regular warehouse update packets through `SmWarehouseInfo.CreateRegularWarehouseUpdatePackets`, and then complete the quest.
- Client-visible/state/persistence effect expected: completing a supported auto-reward quest increments live warehouse bonus expansion count, refreshes regular warehouse info, and then sends `SmQuestAction.Update`; persistence should remain through existing player save/general common-data paths unless a Java-equivalent immediate DAO path is found.
- Why this is not preview-only/test-only/documentation-only: it will mutate live player warehouse expansion state and send real warehouse expansion packets from the socket handler for a currently deferred reward path.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: Java `QuestService.giveReward` warehouse expansion branch increments bonus warehouse expansions, sends warehouse-size-expanded message and warehouse info refresh packets, then completes the quest.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardSideEffectPlanServiceTests|FullyQualifiedName~WarehouseExpandNotificationPlanServiceTests"`.
- Focused Java/Maven command: not expected unless a narrow Java fixture is added; no existing Java socket fixture was found.
- Broad-validation trigger: run broader tests only if implementation touches shared warehouse packet serialization, inventory capacity, or repository persistence.
- Broad .NET decision: do not run unless focused evidence exposes wider risk or a broad trigger is introduced by the implementation.

## Safe Runtime Candidates

- Wire warehouse expansion quest finish rewards through existing quest expansion planning helpers plus live `Player.WarehouseBonusExpands` mutation and warehouse packets.
- Wire challenge task completion only if an existing live challenge task service can execute the Java quest-complete side effect without planner-only scaffolding.
- Wire quest completion callbacks or NPC faction completion only after source review identifies an existing runtime service path that can execute live behavior safely.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 7.
- Total artifacts ported or wired in latest UOW: 1 live socket branch extension for cube expansion rewards.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 4.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
