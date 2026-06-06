# Phase 6 Session 2786 Completion

## Completed UOW

[Phase 6][UOW-2786] Apply active legion bonus to quest XP reward rate

## Runtime Progress Gate

- Deferred/live behavior advanced: Java `Rates.calcXpRate` applies the active legion bonus multiplier to XP rewards; C# quest XP reward application now resolves `LegionBonusRuntime` and mutates player XP state with that 1.1x multiplier.
- Java source of truth: `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Rates.java` `calcXpRate(Player, float[], StatEnum)` and `game-server/src/com/aionemu/gameserver/services/QuestService.java` `giveReward`.
- C# runtime artifact wired/fixed: `QuestRewardService.CreateXpRewardPlan` resolves active runtime legion bonus by player legion id, and `QuestRewardService.ApplyXpReward` mutates `Player.Exp`, `Player.Level`, and `Player.ReposeEnergy` while producing the existing XP stat/system packets.
- Client-visible/state effect: a legion member whose `LegionBonusRuntime` state is active receives 10% more quest XP, changing live player XP/repose/level state and the resulting XP stat/system packet payloads.
- Why this is runtime progress: this is not a preview or evidence-only change; it adds a live XP application boundary that mutates player state using runtime legion bonus state.

## Implementation

- Injected optional `GameServerRuntimeContext` into `QuestRewardService` and used `LegionBonuses.IsActive(player.LegionId)` as the default Java-equivalent `hasLegionBonus` source.
- Added `ApplyXpReward` to apply the existing Java-derived XP reward plan to the mutable player state and produce `SmStatUpdateExp` plus XP system messages.
- Changed `Player.ReposeEnergy` from `init` to `set` so Java `PlayerCommonData.setExp` repose consumption/reclamp can be represented by live XP application.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ApplyXpReward_UsesRuntimeLegionBonusAndMutatesPlayerState` | Regression | `Rates.calcXpRate` legion bonus branch and `QuestService.giveReward` XP path | Active `LegionBonusRuntime` gives 1.1x quest XP, mutates player XP/level state, and creates XP packets | C# runtime service test | No Java executable fixture for this exact branch |
| `ApplyXpReward_LeavesJavaRateUnboostedWhenRuntimeLegionBonusIsInactive` | Regression | `Rates.calcXpRate` inactive/no-bonus branch | Inactive runtime legion state leaves quest XP unboosted while still applying normal XP mutation and packets | C# runtime service test | Quest finish socket dispatch remains a later wiring slice |

## Validation Decision

- Changed surface: quest XP reward runtime service, player XP/repose state mutability, XP packet creation.
- Specific behavior/contract: Java `Rates.calcXpRate` active legion bonus multiplier feeds quest XP reward application and state mutation.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestRewardServiceTests
```

- Focused Java/Maven command: not run; no narrow Java fixture exists for `Rates.calcXpRate` plus `QuestService.giveReward` legion bonus runtime state.
- Broad-validation trigger: live player XP/repose state mutation and XP packet creation.
- Broad .NET decision: skipped unfiltered project/solution validation; the filtered command compiled the affected project and covered the edited reward service behavior directly.
- Why this scope is sufficient: the tests exercise the active/inactive Java branch through the runtime-backed service method and assert actual player state mutation plus XP packet objects.

Result: Passed, 21 total, 0 failed, 0 skipped. Existing nullable/analyzer warnings remain outside this UOW.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.Rates` `calcXpRate(Player, float[], StatEnum)` | `Aion.GameServer.Services.QuestRewardService.CreateXpRewardPlan` / `ApplyQuestXpRate` | Rate/runtime service | Partial | Regression Tested | Partial Parity | Quest XP now consumes active `LegionBonusRuntime`; hunting, group hunting, gathering, crafting, and PvP XP surfaces are not covered by this UOW. |
| `com.aionemu.gameserver.services.QuestService` `giveReward` XP branch | `Aion.GameServer.Services.QuestRewardService.ApplyXpReward` | Runtime state/packet application | Partial | Regression Tested | Partial Parity | Service mutates player XP/level/repose and produces XP packets; live quest finish socket dispatch still needs to call this application boundary. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData` `setExp` repose mutation | `Aion.GameServer.Model.GameObjects.Player.ReposeEnergy` | Runtime state model | Partial | Regression Tested | Partial Parity | Repose can now mutate during XP application; full Java `setExp` side-effect fanout is still broader than this slice. |

## Summary Metrics

- Total Java artifacts touched/discovered: 3.
- Total artifacts ported or wired this UOW: 2 runtime service paths plus 1 player state model fix.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged conservatively; this is a narrow Phase 6 runtime slice.

## Remaining Gaps

- Live quest finish socket/handler dispatch still needs to call the XP application boundary.
- Other Java `calcXpRate` consumers still need active legion bonus runtime wiring: hunting, group hunting, gathering, crafting, and PvP XP surfaces.
- No Java runtime/golden fixture exists for the legion XP multiplier branch.
