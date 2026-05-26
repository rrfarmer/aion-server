# Phase 6YI Completion - UOW-1147 Trade-List Packet Plan Assembly

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by automatically assembling the non-live `SmTradeListPacketPlan` inside the staged `CM_DIALOG_SELECT` NPC target branch input planner. This completes the current non-live path from static trade-list facts to a `BUY` descriptor carrying packet-shape metadata, while keeping production socket sends disabled.

The Java implementation is the source of truth:
- `CM_DIALOG_SELECT.runImpl` reaches `NpcController.onDialogSelect` for valid NPC dialog selects.
- `NpcController.onDialogSelect` checks talk range, lets AI handle first, then falls back to `DialogService.onDialogSelect`.
- `DialogService.onDialogSelect` `BUY` sends `SM_TRADELIST` only when a trade list exists and at least one goods list is allowed by legion level; otherwise it sends the no-sell system message.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogControllerDispatchPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YI-Completion.md`

## Implementation Notes

- Extended `QuestDialogNpcTargetBranchInputAssemblyPlan` with optional `TradeListPacketPlan`.
- Added automatic `SmTradeListPacketPlanService.CreatePlan` invocation when:
  - branch planning reaches controller dispatch;
  - static trade-list facts were derived;
  - the dialog action is `BUY`;
  - at least one goods list is sellable;
  - target NPC template and goods-list table are available.
- Extended `NpcDialogControllerDispatchInput` with optional `TradeListPacketPlan`.
- The packet plan flows through `NpcDialogControllerDispatchPlanService` into `NpcDialogServiceSelectPlanService`, then onto the `TradeListPacket` descriptor.
- Java `Npc.canSell/canBuy` flag behavior is approximated non-live from target template `BUY` / `SELL` support and trade-list availability. Live DataManager-backed methods remain unwired.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests|NpcDialogControllerDispatchPlanServiceTests|NpcDialogServiceSelectPlanServiceTests|SmTradeListPacketPlanServiceTests" --nologo` passed 50 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,156 tests.
- First `dotnet test dotnetConversion/AionServer.slnx --nologo` attempt hit an unrelated `WorldNpcRandomWalkServiceTests.StartRandomWalkingAsync_InterpolatesToTargetAndSchedulesNextRandomPointAfterArrival` timeout; rerun passed 2,363 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Services.QuestDialogNpcTargetBranchInputAssemblyPlanService` | Packet / Planner | Partial | Unit Tested | Partial Parity | Non-live branch assembly now produces a staged `BUY` descriptor with `SM_TRADELIST` packet-shape metadata when static facts allow it. Production socket routing remains disabled. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` | `Aion.GameServer.Services.NpcDialogControllerDispatchPlanService` | Controller Planner | Partial | Unit Tested | Partial Parity | Controller dispatch input now carries the non-live packet plan into the dialog-service fallback. AI/live controller calls remain descriptor-only. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `NpcDialogServiceSelectPlanService` via branch assembly | Service Planner | Partial | Unit Tested | Partial Parity | Java `BUY` success and no-sell split is modeled through static facts and packet-plan presence. Live no-sell packet send and live `SM_TRADELIST` send remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `Aion.GameServer.Services.SmTradeListPacketPlan` assembled by branch input planner | Packet Plan | Partial | Unit Tested | Partial Parity | Packet plan is now automatically assembled for staged `BUY` success. No binary serialization, opcode `253`, live limited-item rows, or Java byte comparison yet. |
| `com.aionemu.gameserver.model.gameobjects.Npc.canSell` / `Npc.canBuy` | `QuestDialogNpcTargetBranchInputAssemblyPlanService` flag derivation | Runtime Fact Dependency | Partial | Unit Tested | Needs Verification | C# derives flags from target template `BUY` and `SELL` support plus trade-list availability. Live NPC method behavior and DataManager-backed calculation remain unwired. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_ComposesDialogServiceFactsFromStaticTradeData` | Unit / Planner Composition | `CM_DIALOG_SELECT`; `NpcController.onDialogSelect`; `DialogService.onDialogSelect` `BUY`; `SM_TRADELIST` | Static trade-list facts create a non-live packet plan, attach it to the service descriptor, preserve the Java price modifier, and set Java-style buy/sell tab flags. | Source-reviewed Java route plus deterministic C# planner test. | No live packet serialization or runtime player/NPC fact sourcing. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_DoesNotCreateTradeListPacketPlanWhenNoGoodsAreSellable` | Unit / Branch Regression | `DialogService.onDialogSelect` `BUY` no-sell branch | Restricted goods lists produce `BuyUnavailable` and no packet plan. | Source-reviewed Java `hasAnythingToSell` branch plus deterministic C# test. | No live no-sell system message send. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not route `BUY` into the staged planner.
- No live `SM_TRADELIST` packet class, opcode `253`, binary serialization, or Java byte comparison exists.
- Live limited-item service lookup, player legion lookup, `Npc.canSell/canBuy` runtime method, no-sell system message send, and NPC AI/controller dispatch remain disabled or unverified.
- The packet plan has empty limited items unless explicit inputs are supplied by future runtime composition.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 automatic non-live packet-plan assembly path
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 6 blocked/partial categories: live socket route, packet serialization/opcode, limited-item lookup, runtime player/NPC facts, no-sell live send, and Java byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Audit the live `GameServerConnection.HandleDialogSelectAsync` prerequisites for safely replacing the current `BUY` no-op boundary with a non-sending planner invocation, or begin the dedicated `SmTradeList` `GameServerPacket` serializer only after byte expectations are available.

Recommended starting points:
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SmTradeListPacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`

Keep live `SM_TRADELIST` sends disabled until byte serialization, opcode `253`, Java comparison evidence, runtime limited items, player legion lookup, `Npc.canSell/canBuy`, no-sell fallback, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: audit and, if safe, add a production `BUY` non-sending planner invocation at the current socket boundary.
- Why: staged planning now contains trade-list facts and packet-shape metadata, but production `BUY` still returns with no planner evidence.
- Files: `GameServerConnection.cs`, focused socket-boundary tests, docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Read-only `SM_TRADE_IN_LIST` Java audit | none | Low | Safe analysis for a later trade-in slice. |
| B | Static-data trade NPC type validation test | focused static-data test file | Medium | Avoid shared loader edits unless required. |
| C | Limited-item dependency mapping | none | Low | Read-only inspection of `LimitedItemTradeService` and related model classes. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Production non-sending planner boundary audit | `GameServerConnection.cs`, focused tests, docs | live packet sends, `StaticData.cs` |
| Agent A | Read-only limited-item dependency map | read-only Java files | all writes |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler; keep single owner.
- `StaticData.cs`: shared XML loader.
- Progress/handoff docs: orchestrator-owned.
