# Phase 6YH Completion - UOW-1146 Trade-List Descriptor Packet Plan

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by attaching the non-live `SmTradeListPacketPlan` from UOW-1145 to the staged `DialogService` `BUY` descriptor. This keeps live socket sends disabled while making the staged `BUY` plan carry both the Java price-modifier calculation and the future packet-shape prerequisite.

The Java implementation is the source of truth:
- `DialogService.onDialogSelect` `BUY` sends `new SM_TRADELIST(player, npc, tradeListTemplate, PricesService.getVendorBuyModifier() * tradeModifier / 100)` when at least one goods tab is allowed.
- `SM_TRADELIST` then filters tabs and writes the packet fields modeled by UOW-1145.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogServiceSelectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogServiceSelectPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YH-Completion.md`

## Implementation Notes

- Extended `NpcDialogServiceSelectInput` with optional `SmTradeListPacketPlan`.
- Extended `NpcDialogServiceDescriptor` with optional `SmTradeListPacketPlan`.
- `CreateBuyPlan` now attaches the supplied packet plan to the `TradeListPacket` descriptor.
- The descriptor remains `IsLive = false`; no packet bytes are emitted and no production send path is enabled.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "NpcDialogServiceSelectPlanServiceTests|SmTradeListPacketPlanServiceTests|QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests" --nologo` passed 41 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,155 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,362 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `Aion.GameServer.Services.NpcDialogServiceSelectPlanService` | Service Planner | Partial | Unit Tested | Partial Parity | `BUY` descriptors can now carry the non-live `SM_TRADELIST` packet plan. Production controller routing, no-sell send behavior, and live socket effects remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `Aion.GameServer.Services.SmTradeListPacketPlan` carried by `NpcDialogServiceDescriptor` | Packet Plan / Descriptor | Partial | Unit Tested | Partial Parity | Packet-plan output is now connected to the staged dialog descriptor, but still no live `GameServerPacket`, opcode `253`, binary serialization, send path, or Java byte comparison. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `NpcDialogServiceSelectPlanServiceTests.CreatePlan_PlansBuyTradeListWhenTradeListAndSellableGoodsExist` | Unit / Planner Composition | `DialogService.onDialogSelect` `BUY`; `SM_TRADELIST` constructor | `BUY` service descriptor keeps the Java price-modifier calculation and carries the supplied non-live `SmTradeListPacketPlan`. | Source-reviewed Java route plus deterministic C# planner test. | Packet plan is supplied explicitly; assembly from runtime player/NPC/static data and live send remain unwired. |

## Remaining Risks

- `QuestDialogNpcTargetBranchInputAssemblyPlanService` does not yet create/pass `SmTradeListPacketPlan` automatically from `NpcDialogTradeListFactAdapterPlan`.
- Production `BUY` routing remains disabled at `GameServerConnection`.
- No live `SM_TRADELIST` packet class, opcode `253`, binary serialization, limited-item service lookup, player legion lookup, `Npc.canSell/canBuy`, or Java byte comparison exists.
- Descriptor-level plumbing is non-live and must not be mistaken for packet parity.

## Summary Metrics

- Total Java artifacts discovered: 2 grouped artifact rows in this unit
- Total artifacts ported: 1 descriptor-level non-live composition path
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 2 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: automatic packet-plan assembly, live socket route, packet serialization/opcode, runtime limited item/player/NPC facts, and Java byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Compose `SmTradeListPacketPlanService` inside `QuestDialogNpcTargetBranchInputAssemblyPlanService` when static trade-list facts are derived, still non-live, so the staged branch input can produce a full `BUY` descriptor plus packet-shape plan.

Recommended starting points:
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeListFactAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SmTradeListPacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmTradeListPacketPlanServiceTests.cs`

Keep `GameServerConnection.HandleDialogSelectAsync` and live `SM_TRADELIST` sends disabled until automatic non-live assembly, runtime player/NPC facts, binary serialization, opcode `253`, no-sell fallback, and Java byte evidence are ready.

# Next Work Options

## Recommended Sequential Task

- Task: automatic non-live packet-plan assembly in `QuestDialogNpcTargetBranchInputAssemblyPlanService`.
- Why: it completes the staged path from static trade-list facts to a `BUY` descriptor that carries packet shape metadata.
- Files: `QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`, focused tests, docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Read-only Java `SM_TRADE_IN_LIST` audit | none | Low | Safe supporting analysis for future trade-in parity. |
| B | Static-data trade NPC type validation test | focused static-data test file | Medium | Avoid editing shared `StaticData.cs` concurrently. |
| C | Limited item dependency map | none | Low | Read-only mapping of `LimitedItemTradeService` dependencies. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Automatic packet-plan assembly | `QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`, focused tests, docs | `GameServerConnection.cs`, `StaticData.cs` |
| Agent A | Read-only trade-in audit | read-only Java files | all writes |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared live socket path and still intentionally disabled for `BUY`.
- `StaticData.cs`: shared XML loader.
- Progress/handoff docs: orchestrator-owned.
