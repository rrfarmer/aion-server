# Phase 6 Session 1794 Completion - Wire Tampering Item Use Runtime

Date: 2026-05-30
Unit of Work: UOW-1794
Status: Complete

## Scope

Port the narrow live Java `TamperingAction.act(...)` runtime boundary, including the `<tampering/>` item-action marker, Java `tampering_chances` config surface, delayed use/cancel flow, source consumption, success/failure packet fanout, and focused parity tests, without claiming first-class `TemperingEffect` runtime parity.

## Completed Work

- Updated `dotnetConversion/src/Aion.GameServer/Dataholders/ItemTemplateTable.cs`:
  - `ItemTemplateSummary` now exposes `HasTamperingAction`
- Updated `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`:
  - parses the Java `<tampering/>` item-action marker
- Updated `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`:
  - binds Java `gameserver.rates.tampering_chances` into `GameServerOptions.Rates.TamperingChances`
- Updated `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`:
  - added Java tempering cancel/success/failure/max/destroy message factories
- Updated `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryUpdateItem.cs`:
  - names Java `STATS_CHANGE` as `StatsChange`
- Updated `dotnetConversion/src/Aion.GameServer/Services/TamperingMutationService.cs`:
  - now transitions the updated target item into Java-shaped `UpdateRequired` persistent state
- Added `dotnetConversion/src/Aion.GameServer/Services/TamperingActionExecutionPlanService.cs`:
  - ports Java tampering start metadata
  - ports Java `calculateChance(...)`
  - ports deterministic success/failure outcome planning
  - emits level-10 same-race world-announce packet intent
- Updated `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`:
  - routes `<tampering/>` source items through delayed `CM_USE_ITEM` handling
  - ports Java start/cancel/completion tampering behavior
  - consumes the source item before applying the target mutation
  - preserves the Java silent post-consume maxed-target branch
  - sends Java-shaped target `STATS_CHANGE`, result messages, end animations, and plume delete packets
- Added focused parity evidence:
  - `dotnetConversion/tests/Aion.GameServer.Tests/TamperingActionExecutionPlanServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionTamperingTests.cs`
  - updated `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TamperingActionExecutionPlanServiceTests|FullyQualifiedName~GameServerConnectionTamperingTests|FullyQualifiedName~TamperingMutationServiceTests|FullyQualifiedName~GamePacketTests"`
- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CompositeStonesConsumesSameStoneStackTwiceInJavaOrder"`
- `dotnet test dotnetConversion\AionServer.slnx`

Result:

- Focused tampering validation passed with 251 tests.
- The first full-suite attempt hit the command timeout boundary before completion.
- The second full-suite attempt failed in the recurring unrelated transient `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesConsumesSameStoneStackTwiceInJavaOrder`.
- The isolated rerun of that transient passed with 1 test.
- The final full-suite rerun passed cleanly with 4783 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4576` game

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.templates.item.actions.TamperingAction`
- `com.aionemu.gameserver.configs.main.RatesConfig`
- `com.aionemu.commons.utils.Rnd`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`
- `game-server/data/static_data/items/item_templates.xml`

## Migration Parity Table - UOW-1794

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.TamperingAction.act` delayed item-use shell | `Aion.GameServer.Network.Aion.GameServerConnection.HandleTamperingUseItemAsync` + `CompleteTamperingUseItemAsync` | Live Runtime Boundary | Partial | Regression Tested | Partial Parity | C# now runs the Java-shaped delayed tampering start, cancel, source consume, and success/failure packet flow. First-class `TemperingEffect` object lifecycle parity remains out of scope. |
| `com.aionemu.gameserver.model.templates.item.actions.TamperingAction.calculateChance` | `Aion.GameServer.Services.TamperingActionExecutionPlanService.CalculateChance` + `GameServerOptions.Rates.TamperingChances` | Chance / Rate Surface | Complete | Unit Tested | Partial Parity | Java plume and membership-rate chance logic is represented, and the Java config key is loaded. No Java runtime comparison yet. |
| Java item XML `<tampering/>` action marker | `Aion.GameServer.Dataholders.StaticData` + `ItemTemplateSummary.HasTamperingAction` | Static Data Action Metadata | Complete | Regression Tested | Partial Parity | C# now recognizes the Java tampering action marker and routes those items into the live path. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` tempering factories | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.ItemAuthorize*` | Packet / Message Surface | Complete | Regression Tested | Verified Parity | Message ids and parameter ordering are reviewed against Java and packet-tested. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.STATS_CHANGE` in the tampering path | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem.StatsChange` | Packet Update-Type Surface | Complete | Regression Tested | Partial Parity | The tampering path now emits Java `STATS_CHANGE` update mask `0` for target item info changes. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreateStartPlan_WritesJavaDelayAnimation` | The tampering start planner emits Java `5000ms` usage animation metadata. | Java `TamperingAction.act` source | Unit | Does not prove runtime dispatch alone |
| `CreateMutationPlan_SuccessAtZeroTemperingRaisesLevelAndBuildsSuccessMessage` | `+0 -> +1` always succeeds, marks the target dirty, and builds the Java success message. | Java `TamperingAction.calculateChance` + success branch | Unit | No Java runtime comparison |
| `CreateMutationPlan_FailedPlumeResetsBonusBuildsDestroyMessageAndAnnouncementAtTen` | Level-10 announce intent is produced on success, and plume failure resets random bonus while producing the Java destroy message. | Java `TamperingAction.act` + `setTemperingLevel` | Unit | Live plume-destroy runtime still depends on random chance |
| `CalculateChance_UsesJavaPlumeCurveAndMembershipRates` | The planner uses Java plume and membership-rate chance formulas. | Java `TamperingAction.calculateChance` + `RatesConfig.TEMPERING_CHANCES` | Unit | No runtime comparison against Java |
| `ProcessPacketAsync_TamperingSuccessConsumesSourceUpdatesTargetAndSendsStatsChange` | The live `CM_USE_ITEM` path consumes the source, sends `STATS_CHANGE`, success message, and completion animation on guaranteed `+0 -> +1` success. | Java `CM_USE_ITEM` -> `TamperingAction.act` | Regression | Does not cover the level-10 announce branch |
| `ProcessPacketAsync_TamperingFailureResetsNonPlumeAndConsumesSource` | A forced non-plume failure consumes the source, resets tempering to `0`, and sends the Java failure packet flow. | Java `TamperingAction.act` failure branch | Regression | No Java runtime comparison |
| `CancelPendingTamperingUse_SendsAuthorizeCancelAndRemovesCooldown` | Canceling delayed tampering removes the source cooldown and sends the Java tempering cancel message plus end-state `3`. | Java `ItemUseObserver.abort` | Regression | Generic pending-use infrastructure still broadcasts cancel animation before the cancel message |

## Risks / Gaps

- C# still does not port a first-class Java `TemperingEffect` object lifecycle; this unit validates the live item mutation and packet boundary, not the underlying effect-controller implementation.
- The branch where an originally equipped target disappears before delayed completion is still not explicitly proven in C#.
- Same-race level-10 announce fanout is represented in code and the deterministic planner, but does not yet have a dedicated registry-backed packet regression.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows in this unit.
- Total artifacts ported: 1 live tampering runtime branch, 1 deterministic execution planner, 1 static-data action marker, 1 config rate surface, 5 message factories, 1 named update-type constant, and 7 focused tests/regressions.
- Total artifacts with verified parity: 1 grouped row.
- Total artifacts needing verification: 4 grouped rows.
- Total blocked artifacts: first-class `TemperingEffect` runtime lifecycle parity and deeper Java runtime comparison for the live tampering branch.
- Estimated overall migration completion: Phase 6 remains about 73%.

## Next Recommended Unit of Work

- Port the minimum explicit `TemperingEffect.apply/endEffect` or adjacent equipped-item side-effect proof needed so live equipped tampering can be objectively validated beyond the item blob plus `SM_STATS_INFO` boundary.
- Safe alternatives if a different isolated slice is preferred:
  - execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`
  - `CraftService.finishCrafting` product selection
  - `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/ItemTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryUpdateItem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TamperingMutationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TamperingActionExecutionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionTamperingTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TamperingActionExecutionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1794-Completion.md`
- `docs/Phase-6-Session-1794-Handoff.md`
