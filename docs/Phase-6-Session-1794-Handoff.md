# Phase 6 Session 1794 Handoff - Wire Tampering Item Use Runtime

Date: 2026-05-30
Unit of Work: UOW-1794
Status: Completed, pending commit

## What Changed

- Ported the narrow live Java `TamperingAction.act(...)` runtime boundary in the C# `CM_USE_ITEM` path without widening into a first-class `TemperingEffect` runtime rewrite.
- Added Java `<tampering/>` action marker parsing and `ItemTemplateSummary.HasTamperingAction` so tampering source items can be recognized from static data.
- Bound Java `gameserver.rates.tampering_chances` into `GameServerOptions.Rates.TamperingChances`.
- Added the missing Java tempering system-message factories and the named `SmInventoryUpdateItem.StatsChange` update-type constant.
- Added `TamperingActionExecutionPlanService` to model Java start-delay metadata, chance calculation, deterministic mutation output, and optional level-10 same-race announce intent.
- Wired live delayed tampering handling in `GameServerConnection`, including cancel flow, source consumption, Java-shaped silent post-consume maxed-target branch, target update packet flow, result messages, delete handling for destroyed plumes, and completion animations.
- Tightened `TamperingMutationService.SetTemperingLevel(...)` so target mutations now promote Java-shaped `UpdateRequired` persistent state.

## Files Changed

- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1794-Completion.md`
- `docs/Phase-6-Session-1794-Handoff.md`
- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/ItemTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryUpdateItem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TamperingActionExecutionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TamperingMutationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionTamperingTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TamperingActionExecutionPlanServiceTests.cs`

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.templates.item.actions.TamperingAction`
- `com.aionemu.gameserver.configs.main.RatesConfig`
- `com.aionemu.commons.utils.Rnd`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`
- `game-server/data/static_data/items/item_templates.xml`

## C# Artifacts Touched

- `Aion.GameServer.Configuration.GameServerOptions`
- `Aion.GameServer.Dataholders.ItemTemplateTable`
- `Aion.GameServer.Dataholders.StaticData`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Services.TamperingActionExecutionPlanService`
- `Aion.GameServer.Services.TamperingMutationService`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TamperingActionExecutionPlanServiceTests|FullyQualifiedName~GameServerConnectionTamperingTests|FullyQualifiedName~TamperingMutationServiceTests|FullyQualifiedName~GamePacketTests"`
- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CompositeStonesConsumesSameStoneStackTwiceInJavaOrder"`
- `dotnet test dotnetConversion\AionServer.slnx`

Results:

- Focused tampering validation passed with 251 tests.
- The first full-suite attempt hit the command timeout boundary before completion.
- The second full-suite attempt failed in unrelated transient `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesConsumesSameStoneStackTwiceInJavaOrder`.
- The isolated rerun of that transient passed with 1 test.
- The final full-suite rerun passed cleanly with 4783 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4576` game

## Parity Table Summary

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `TamperingAction.act` delayed item-use shell | `GameServerConnection.HandleTamperingUseItemAsync` + `CompleteTamperingUseItemAsync` | Live Runtime Boundary | Partial | Regression Tested | Partial Parity | C# now covers the Java-shaped delayed start, cancel, source consume, result message, and animation flow. First-class `TemperingEffect` lifecycle remains out of scope. |
| `TamperingAction.calculateChance` | `TamperingActionExecutionPlanService.CalculateChance` + `GameServerOptions.Rates.TamperingChances` | Chance / Rate Surface | Complete | Unit Tested | Partial Parity | Java plume and membership-rate formulas are represented and tested. |
| Java item XML `<tampering/>` action marker | `StaticData` + `ItemTemplateSummary.HasTamperingAction` | Static Data Action Metadata | Complete | Regression Tested | Partial Parity | Live routing now depends on the parsed Java action marker. |
| Java tempering `SM_SYSTEM_MESSAGE` factories | `SmSystemMessage.ItemAuthorize*` | Packet / Message Surface | Complete | Regression Tested | Verified Parity | Message ids and parameter ordering were reviewed against Java and packet-tested. |
| Java `ItemPacketService.ItemUpdateType.STATS_CHANGE` in tampering flow | `SmInventoryUpdateItem.StatsChange` | Packet Update-Type Surface | Complete | Regression Tested | Partial Parity | The target update now emits Java mask `0` for stats changes. |

## Known Gaps

- C# still does not port a first-class Java `TemperingEffect` object lifecycle.
- The branch where an originally equipped target disappears before delayed completion is not yet explicitly proven with focused regression coverage.
- Same-race level-10 announce fanout is represented in the deterministic planner and live code path but does not yet have dedicated registry-backed packet regression evidence.

## Risks

- The current cancel path still depends on the generic delayed item-use infrastructure, which broadcasts the cancel animation before the tempering cancel message.
- Live tampering persistence currently reuses existing item-action persistence helpers rather than a dedicated tampering-specific repository boundary.
- Equipped-target behavioral parity is still proven primarily through item mutation plus `SM_STATS_INFO` side effects, not through a direct `TemperingEffect` runtime object port.

## Next Recommended Unit of Work

- Next sequential task: port the minimum explicit `TemperingEffect.apply/endEffect` or adjacent equipped-item side-effect proof needed so live equipped tampering can be objectively validated beyond the item blob plus `SM_STATS_INFO` boundary.

Safe alternative candidates:

- Execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`.
- Port Java `CraftService.finishCrafting` product selection.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TamperingActionExecutionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TamperingMutationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/ItemTemplateTable.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before selecting the next UOW.
- Re-inspect Java `TamperingAction`, any related Java `TemperingEffect` implementation surface, and the current C# equipped-item stats update path before widening scope.
- Treat Java as source of truth and avoid claiming effect-controller parity without direct source comparison plus objective test evidence.
