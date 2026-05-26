# Phase 6YV Completion - UOW-1160 Trade-In Packet Plan Composition

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by composing the concrete non-live `SM_TRADE_IN_LIST` packet plan into staged dialog-service planning.

Production `TRADE_IN` routing and live sends remain disabled.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogServiceSelectPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogControllerDispatchPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogServiceSelectPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YV-Completion.md`

## Implementation Notes

- Added optional `SmTradeInListPacketPlan` metadata to `NpcDialogServiceSelectInput`, `NpcDialogServiceDescriptor`, and `NpcDialogControllerDispatchInput`.
- `QuestDialogNpcTargetBranchInputAssemblyPlanService` now creates a `SmTradeInListPacketPlan` for `TRADE_IN` when a staged trade-in list is available.
- The same packet plan instance now flows into the non-live dialog-service descriptor.
- No production socket send behavior changed.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmTradeInListPacketPlanServiceTests|NpcDialogServiceSelectPlanServiceTests|QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests|NpcDialogTradeListFactAdapterServiceTests" --nologo` passed 51 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,178 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,385 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` `TRADE_IN` | `NpcDialogServiceSelectPlanService` / `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Service Boundary / Planner | Partial | Unit Tested | Partial Parity | Non-live descriptor composition now carries a concrete trade-in packet plan. Production socket routing and packet sends remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADE_IN_LIST` | `SmTradeInListPacketPlan` through dialog-service descriptor | Packet Plan / Packet Dependency | Partial | Unit Tested | Partial Parity | Ready packet plan can now flow into staged dialog planning. Serializer exists, but no live sends or Java runtime vectors exist. |
| `com.aionemu.gameserver.dataholders.TradeListData.getTradeInListTemplate` | `TradeListTable.GetTradeInListTemplate` via `NpcDialogTradeListFactAdapterService` | Static Data Repository | Partial | Unit Tested | Partial Parity | Static trade-in list lookup feeds composed packet planning. Full Java corpus/runtime comparison remains unverified. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate` | `TradeListTemplateSummary` consumed by `SmTradeInListPacketPlanService` | Static Data DTO | Partial | Unit Tested | Partial Parity | Trade-in template tab ids flow into the packet plan without goods/legion filtering, matching source-reviewed Java behavior. |
| `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes` | `SmTradeInList.PacketOpCode` carried by plan/descriptor tests indirectly | Opcode Registry | Partial | Unit Tested | Partial Parity | Opcode was pinned in UOW-1159; this unit composes the plan into descriptors but still lacks Java runtime golden vectors. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `NpcDialogServiceSelectPlanServiceTests.CreatePlan_PlansTradeInFromExplicitTradeInListAvailability` | Unit / Planner Descriptor | `DialogService.onDialogSelect` `TRADE_IN`; `SM_TRADE_IN_LIST` | Trade-in descriptors carry the concrete non-live packet plan when available and omit it for unavailable trade-in lists. | Source-reviewed Java route plus deterministic C# planner test. | No production routing or Java runtime bytes. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_ComposesTradeInPacketPlanFromStaticTradeData` | Unit / Planner Composition | `DialogService.onDialogSelect` `TRADE_IN`; `TradeListData.getTradeInListTemplate`; `SM_TRADE_IN_LIST` | Static trade-in facts produce a ready packet plan and the same plan is attached to the dialog-service descriptor. | Source-reviewed Java route plus deterministic staged composition test. | No live `GameServerConnection` path or Java runtime capture. |

## Remaining Risks

- Production `TRADE_IN` socket routing remains disabled.
- `SmTradeInList` is still not sent by `GameServerConnection`.
- Java runtime golden-vector comparison is absent.
- Trade-in purchase execution/AP formulas are separate and remain outside this packet-list slice.
- NPC AI/controller live routing remains a prerequisite before sends can go live.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 staged trade-in packet-plan composition path
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 4 blocked/partial categories: production routing, Java runtime vectors, live send wiring, and trade-in purchase execution
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add production non-sending `TRADE_IN` boundary observation in `GameServerConnection.HandleDialogSelectAsync`, or design static-data limited-item corpus count comparison if avoiding shared socket routing.

Recommended starting points:
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADE_IN_LIST.java`

Keep live sends disabled until runtime facts, Java runtime golden vectors, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: add production non-sending `TRADE_IN` plan observation in `GameServerConnection`.
- Why: staged planner composition exists, but the production socket boundary only observes `BUY`.
- Files: `GameServerConnection.cs`, `GameServerConnectionStorageExpansionDialogTests.cs`, progress/handoff docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Static-data limited-item corpus count comparison design | docs/test planning | Low | Do not claim parity until Java-generated counts exist. |
| B | Java runtime artifact tooling sketch for trade-list vectors | docs only | Low | Can extend `TradeList-Java-Golden-Vector-Design.md`. |
| C | Live legion-level lookup discovery | read-only code search/docs | Low | Avoid touching `GameServerConnection` until a C# legion aggregate exists. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Production non-sending `TRADE_IN` observation | `GameServerConnection.cs`, focused tests, docs | live packet sends |
| Agent A | Static-data limited-item corpus count comparison design | docs only | code files |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
