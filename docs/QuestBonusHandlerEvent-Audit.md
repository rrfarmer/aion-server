# Quest Bonus Handler Event Audit

Date: May 26, 2026

## Scope

This audit reviews Java `QuestEngine.onBonusApplyEvent` registrations used by `QuestService.getRewardItems` before `BonusService.getQuestBonus` is called.

This is a read-only Phase 6 parity prerequisite. It does not enable C# handler dispatch, RNG, selected `QuestItems`, inventory mutation, packet sends, persistence, or production quest-finish wiring.

## Java Source Summary

`QuestService.getRewardItems` calls:

```java
HandlerResult result = QuestEngine.getInstance().onBonusApplyEvent(env, template.getBonus().getType(), questItems);
if (result != HandlerResult.FAILED) {
	QuestItems bonusItem = BonusService.getQuestBonus(player, template);
	if (bonusItem != null)
		questItems.add(bonusItem);
}
```

`QuestEngine.onBonusApplyEvent`:

- looks up registered quest ids by `BonusType`;
- iterates in registration order;
- sets `env.questId` to the first registered quest with a loaded handler;
- returns that handler's `onBonusApplyEvent` result immediately;
- returns `HandlerResult.UNKNOWN` when no handler is registered or loaded;
- catches exceptions, logs `QE: exception in onBonusApply`, and returns `HandlerResult.FAILED`.

`AbstractQuestHandler.onBonusApplyEvent` returns `HandlerResult.UNKNOWN` by default.

## Registrations Found

| BonusType | Count | Quest IDs / Handlers | Behavior Pattern |
|---|---:|---|---|
| `MOVIE` | 2 | `80016 _80016EventSockHop`; `80018 _80018EventSockItToEm` | If quest state is `REWARD`, optionally append hat-box item `188051106 x1` when `completeCount == 9`, play one random movie, return `SUCCESS`; otherwise return `FAILED`. |
| `LUNAR` | 6 | `80034 _80034EventGeaterGlories`; `80035 _80035EventOnlyTheBest`; `80036 _80036EventGamblingWithGrace`; `80037 _80037EventFromTheGutter`; `80038 _80038EventMightyAspirations`; `80039 _80039EventTheChosenFew` | If quest state is `START` or `COMPLETE` and var0 is `0`, return `SUCCESS`; otherwise return `FAILED`. No item mutation in handlers. |
| `RIFT` | 5 | `80137 _80137EventSealTheWarpedRift`; `80139 _80139EventCloseTheWarpedRift`; `80145 _80145EventWarpedRiftSealing`; `80147 _80147EventWarpedRiftClosing`; `80149 _80149EventWarpedRiftSecuring` | If quest state is `START` or `COMPLETE` and var0 is `0`, return `SUCCESS`; otherwise return `FAILED`. No item mutation in handlers. |

## Important Parity Notes

- `MOVIE`, `LUNAR`, and `RIFT` are not Java-live `BonusService.getBonusGroups` item-group branches. `MOVIE` is a silent no-op in `BonusService`; `LUNAR` and `RIFT` fall through the default warning/no-op branch.
- Handler `SUCCESS` for those types permits the later `BonusService.getQuestBonus` call, but Java `BonusService` still returns null for these types.
- Handler `FAILED` suppresses the later `BonusService.getQuestBonus` call.
- Handler `UNKNOWN` also permits the later `BonusService.getQuestBonus` call because Java only blocks on `FAILED`.
- `MOVIE` handlers mutate `rewardItems` directly by adding `QuestItems(188051106, 1)` at complete count `9`, and play a random movie as a side effect.
- `LUNAR` and `RIFT` handlers act as gates only. They do not add reward items.
- `QuestEngine` returns after the first registered loaded handler for a bonus type. This matters because the registration map is keyed only by `BonusType`, not by the current quest id. Each handler checks `env.getQuestId()`, but `QuestEngine` overwrites `env.questId` with the registered quest id before calling the handler.

## C# Port Recommendation

The next implementation should start with a disabled, non-live handler outcome model:

- Input: bonus type, current quest id, quest status, quest var0, complete count.
- Output: handler result (`UNKNOWN`, `SUCCESS`, `FAILED`), direct reward item additions, and side-effect intents.
- Explicitly model only these audited event handlers.
- Preserve the Java distinction between handler-added reward items and `BonusService` selected bonus items.
- Keep movie playback as a side-effect intent only until packet/movie side effects are wired and tested.

Do not wire this into production quest finish yet. Keep live dynamic handler dispatch and runtime `QuestEngine` integration disabled until quest handler loading and quest-state context are available.

## Remaining Risks

- This audit is source-review only; Java runtime registration order was not executed.
- Dynamic handler loading/reflection behavior is not compared.
- The first-handler-wins behavior may have surprising runtime effects when multiple handlers share the same `BonusType`; C# should model or explicitly document this before live dispatch.
- Movie random selection uses Java `Rnd.nextBoolean`; C# should not roll it until Java RNG policy is decided.
- `MOVIE` direct item addition depends on quest complete-count state and reward item list mutability.
- Production quest finish still lacks live reward mutation, handler dispatch, item capacity/stacking, packet sends, persistence, rollback, and player-thread ordering.
