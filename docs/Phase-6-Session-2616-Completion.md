# Phase 6 Session 2616 Completion

## UOW

[Phase 6] UOW-2616: Schedule NPC shop limited-item resets live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: limited-item counters mutated by successful NPC shop buys now reset from configured sales-time schedules.
- Java source/runtime path: LimitedItemTradeService.start -> CronService.getInstance().schedule(limitedItem::setToDefault, salesTime), and LimitedItem.setToDefault resets sellLimit and clears buyCounts.
- C# runtime artifact wired: LimitedItemTradeService scheduled resets, LimitedItemTradeSchedulerService, JavaQuartzCronExpression range parsing, and Program game-engine startup registration.
- Client-visible/state/persistence effect: after a scheduled reset fires, later buy dialogs and buy attempts observe restored sell limits and cleared player buy counts.
- Why this is runtime progress: this UOW adds a live scheduler path that mutates runtime limited-item state used by client packet handling; it is not preview-only, test-only, or documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/LimitedItemTradeService.java`
  - `start`
  - `getLimitedItem`
- `game-server/src/com/aionemu/gameserver/model/limiteditems/LimitedItem.java`
  - `setToDefault`
  - `getSalesTime`
- `game-server/src/com/aionemu/gameserver/services/cron/CronService.java`
  - `schedule(Runnable, String)`
  - Quartz trigger timezone use
- `game-server/src/com/aionemu/gameserver/GameServer.java`
  - `CronService.initSingleton(..., GSConfig.TIME_ZONE_ID)`
- `game-server/data/static_data/goodslists/goodslists.xml`
  - reviewed sales-time shapes including comma-separated hours and hour ranges such as `09-18`.

## C# Changes

- `LimitedItemTradeService`
  - Added `StartScheduledResets` and `ShutdownScheduledResets`.
  - Schedules each parsed limited-item sales time through `ThreadPoolManager`.
  - Executes Java-equivalent `SetToDefault` behavior by restoring `SellLimit` to `DefaultSellLimit` and clearing player buy counts.
  - Reschedules after each fire to emulate cron recurrence.
- `LimitedItemTradeSchedulerService`
  - Added a `GameEngine` wrapper that starts limited-item reset scheduling after static data is loaded.
  - Uses `GameServerOptions.Core.GetTimeZone()` to mirror Java `GSConfig.TIME_ZONE_ID` scheduling context.
  - Cancels reset scheduling during game-server shutdown.
- `Program.cs`
  - Registers `LimitedItemTradeSchedulerService` as a live game engine.
- `JavaQuartzCronExpression`
  - Added numeric range support for Java goods-list expressions such as `0 0 09-18 ? * MON`.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LimitedItemTradeServiceTests.StartScheduledResets_RestoresSellLimitAndClearsBuyCountsLikeJavaCron` | Unit/runtime scheduler | `LimitedItemTradeService.start` plus `LimitedItem.setToDefault` | A real scheduled callback resets current sell limit and clears the player's buy count, making the item buyable again. | Java source review + focused C# scheduler/state assertions. | Uses a fast test cron; no real multi-hour schedule or real client validation. |
| `LimitedItemTradeServiceTests.StartScheduledResets_SkipsUnsupportedSalesTimeWithoutScheduling` | Unit/service boundary | Java `CronService.schedule` requires a valid Quartz expression | Unsupported sales-time shapes are reported and not scheduled. | Focused C# assertions. | Java would fail through Quartz/CronService for invalid expressions; C# logs/skips to avoid taking down startup. |
| `JavaQuartzCronExpressionTests.TryParse_HandlesJavaGoodsListHourRanges` | Unit/cron parser | Java goods-list `salestime` values parsed by Quartz | The local Quartz subset handles Java data hour ranges such as `09-18`. | Java data review + focused C# assertions. | Full Quartz syntax and DST behavior remain partial. |

## Validation Decision

```text
- Changed surface: live scheduler/state mutation, game-engine startup registration, and Java Quartz cron expression parsing.
- Specific behavior/contract: limited-item reset schedules fire through live ThreadPoolManager callbacks and reset sell limits/player buy counts used by later buy eligibility.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~LimitedItemTradeServiceTests|FullyQualifiedName~JavaQuartzCronExpressionTests|FullyQualifiedName~NpcDialogLimitedItemFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for LimitedItemTradeService cron reset behavior.
- Broad-validation trigger: live scheduler/state boundary changed.
- Broad .NET decision: skipped after focused scheduler/service/live-buy coverage because the changed runtime path was isolated to limited-item reset scheduling and the filtered command built the affected project/dependencies.
- Why this scope is sufficient: the focused tests cover the reset callback mutation, the cron parser shape needed by Java goods-list data, the existing dialog fact adapter, and the live buy path that consumes limited-item state.
```

Result: passed, 50/50.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.LimitedItemTradeService#start` | `Aion.GameServer.Services.LimitedItemTradeService.StartScheduledResets` and `LimitedItemTradeSchedulerService.InitAsync` | Runtime scheduler/service | Partial | Unit Tested | Partial Parity | C# schedules reset jobs from runtime limited-item state and starts them through bootstrap. Unsupported Quartz shapes are skipped/logged instead of failing startup. |
| `com.aionemu.gameserver.model.limiteditems.LimitedItem#setToDefault` | `Aion.GameServer.Services.LimitedItemRuntimeState.SetToDefault` | Runtime state mutation | Partial | Unit Tested | Partial Parity | Scheduled callback restores sell limit and clears buy counts. Threading is protected by the service lock; exact Java object-sharing across reused goods lists remains not deeply verified. |
| `com.aionemu.gameserver.services.cron.CronService#schedule` | `Aion.GameServer.Utils.ThreadPoolManager` plus `JavaQuartzCronExpression` next-run calculation | Scheduler | Partial | Unit Tested | Partial Parity | Supports the Quartz subset currently used by tested limited-item sales-time shapes; full Quartz syntax, misfire handling, and DST edge cases are not verified. |
| `com.aionemu.gameserver.GameServer` cron startup | `Aion.GameServer.Program` and `GameServerBootstrapService` game-engine initialization | Startup wiring | Partial | Compile Covered/Unit Tested indirectly | Partial Parity | Reset scheduler is registered as a game engine and initialized after static data load; real server startup/client validation was not run. |

## Summary Metrics

- Java artifacts discovered/touched: 4.
- C# artifacts changed/touched: 5.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 4.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- The C# cron helper is not a complete Quartz implementation; only simple lists, ranges, wildcard seconds/minutes/hours, wildcard month, and day-of-week forms are covered.
- Unsupported sales-time expressions are skipped and logged in C#; Java `CronService` would throw if Quartz rejects a schedule during startup.
- Exact DST, misfire, and Quartz trigger identity behavior was not verified.
- Limited-item state remains runtime in-memory state.
- Live NPC shop execution remains scoped to `TradeNpcType.NORMAL`, action `13`; `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy branches remain non-live.
- Real client validation was not run.
