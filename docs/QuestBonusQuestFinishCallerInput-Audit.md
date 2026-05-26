# Quest Bonus Quest-Finish Caller Input Audit

Date: May 26, 2026

## Scope

This audit maps the inputs needed to invoke `QuestBonusRewardPlanningInputAdapterService` from the staged quest-finish flow.

No production quest-finish invocation, reward mutation, packet send, persistence write, RNG selection, or handler dispatch is enabled by this unit.

Java remains the source of truth:

- `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT.runImpl`
- `com.aionemu.gameserver.services.QuestService.finishQuest`
- `com.aionemu.gameserver.services.QuestService.getRewardItems`
- `com.aionemu.gameserver.services.reward.BonusService`
- `com.aionemu.gameserver.questEngine.QuestEngine.onBonusApplyEvent`

## Current Production Boundary

`GameServerConnection.HandleDialogSelectAsync` is the current C# packet boundary for `CM_DIALOG_SELECT`.

It handles live portal entry, recovery, crafting learn, storage expansion, and item charge paths. A source search found no live production call from `HandleDialogSelectAsync` into `QuestFinishOperationPlanService.CreatePlan`.

The quest-finish operation planner remains staged/test-driven. It accepts `PlayerQuestState`, `NearbyQuestTemplateSummary`, NPC faction snapshots, time/options, optional reward projection, optional callback/persistence plans, and optional reward side-effect context. It does not currently assemble or invoke `QuestBonusRewardPlanningInputAdapterService`.

## Adapter Input Availability

| Adapter Input | Current C# Source | Availability | Notes |
|---|---|---|---|
| `RewardProjection` | `QuestFinishRewardTemplateProjection` from XML projection tests and staged planner inputs | Staged only | `QuestFinishOperationPlanService.CreatePlan` accepts this as an optional argument, but no live socket/quest-finish caller currently supplies it. Production reward-projection lookup must be explicit before invoking the bonus adapter. |
| `QuestTemplate` | `NearbyQuestTemplateSummary` and `QuestDialogAutoRewardGuardPlanService.CreatePlanFromTemplateSummary` | Partially available | Template summaries include `CanReport`, `HasRewards`, `HasExtendedRewards`, and `HasBonus`. The production socket boundary does not yet call the quest-finish planner, so availability is conditional on the future caller. |
| `CurrentQuestState` | `Player.Quests`; `QuestFinishOperationPlanService.CreatePlan` accepts `PlayerQuestState?` | Partially available | The player model carries quest state lists and the planner accepts the current state. The audited production dialog path still needs an explicit lookup for the selected quest id before adapter invocation. |
| `PlayerRace` | `Player.Race` from the active player and `QuestFinishRewardSideEffectContext.Player` | Available if context is passed | Race can be sourced from the active player at the socket boundary. The adapter should use the same player instance as the finish operation to avoid snapshot drift. |
| `ItemTemplates` | `StaticData.ItemTemplates` through `runtimeContext.DataManager?.StaticData.ItemTemplates` | Runtime available, missing finish contract | Runtime static data is accessible in `GameServerConnection`, but `QuestFinishRewardSideEffectContext` does not currently expose item templates to bonus planning. |
| `ItemGroups` | `StaticData.QuestBonusItemGroups.Groups` added in UOW-1107 | Runtime available, missing finish contract | Supported quest bonus item groups are now loaded, but no finish-side context or caller passes them to the adapter. |
| `BonusHandlerQuestStates` | `Player.Quests` can be keyed by quest id; adapter accepts explicit handler states | Missing at finish boundary | Java `QuestEngine.onBonusApplyEvent` checks registered handler quest ids against player quest states. C# can model this with a dictionary, but the future caller must assemble it deliberately. |
| `LoadedBonusHandlerQuestIds` | Adapter accepts an explicit loaded-id set | Missing | Java uses runtime-loaded quest handlers. C# has no production handler registry/loaded-state source here. Passing null means the adapter treats audited registrations as loadable, which is useful for diagnostics but not full Java parity. |

## Recommended Integration Boundary

The next code unit should add a disabled input-assembly planner rather than calling the adapter directly from production.

Recommended shape:

- Accept the same core inputs already used by `QuestFinishOperationPlanService`.
- Accept optional `ItemTemplateTable`, optional `QuestBonusItemGroupTable` or group list, optional full player quest states, and optional loaded handler quest ids.
- Return either a complete `QuestBonusRewardPlanningInput` plus diagnostics or a deterministic missing-input report.
- Keep `IsLive` false and do not add reward action descriptors to the live finish flow.

This gives the future production caller a narrow place to source and validate inputs before any live bonus reward behavior is considered.

## Parity Notes

- Java dynamic quest-handler dispatch is not ported. Reflection/registration ordering and handler-load failures remain unknown.
- Java `BonusService` RNG and `Chance` behavior remain disabled. No selected group, selected item, random count roll, or `QuestItems` mutation is performed.
- C# static-data access is instance-based through `DataManager.StaticData`; Java uses static fields such as `DataManager.ITEM_DATA` and `ITEM_GROUPS_DATA`.
- C# player quest states are immutable record snapshots at the planner boundary; Java mutates `QuestState` during finish. Ordering around mutation, bonus planning, packets, and persistence needs explicit tests before live use.
- Date/time ordering remains staged through explicit `DateTimeOffset now`; Java runtime ordering around finish and repeat timing still needs comparison.
- Serialization differences are out of scope for this unit; no packet payloads or persistence records are emitted.

## Remaining Risks

- Production `CM_DIALOG_SELECT` does not yet route quest-finish auto-reward actions into the staged operation planner.
- Reward projection availability at the production call site is unverified.
- `ItemTemplates` and `QuestBonusItemGroups` are runtime available but not part of the finish-side bonus-planning contract.
- Full handler quest-state lookup and loaded-handler state are missing.
- Dynamic handler dispatch, exception handling, Java RNG/Chance selection, selected reward materialization, live inventory mutation, packet ordering, persistence, rollback, threading/player-ordering, and Java runtime comparison remain disabled.
