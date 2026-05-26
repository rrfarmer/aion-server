# Phase 6YU Completion - UOW-1159 Trade-In List Packet Serializer

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by adding a concrete non-live `SM_TRADE_IN_LIST` packet plan and serializer with source-derived payload tests.

Production `TRADE_IN` routing and live sends remain disabled.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/SmTradeInListPacketPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmTradeInList.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmTradeInListPacketPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YU-Completion.md`

## Implementation Notes

- Added `SmTradeInListPacketPlanService` with `Ready`, `UnknownTradeNpcType`, and `InvalidTradeInList` statuses.
- Added `SmTradeInList` with opcode `151`, matching Java `ServerPacketsOpcodes`.
- Serializer writes Java `SM_TRADE_IN_LIST.writeImpl` order: NPC object id, trade NPC type index, buy price modifier, fixed Aion 4.5 modifier `100`, tab count, and tab ids.
- The plan intentionally does not apply goods-list existence, legion-level, buy/sell-tab, or limited-item behavior because Java `SM_TRADE_IN_LIST` does not.
- Non-ready plans are rejected before serialization instead of silently writing an empty body.
- No production socket handler or live send wiring changed.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmTradeInListPacketPlanServiceTests|NpcDialogServiceSelectPlanServiceTests|NpcDialogTradeListFactAdapterServiceTests" --nologo` passed 32 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,177 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,384 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADE_IN_LIST` | `Aion.GameServer.Services.SmTradeInListPacketPlanService` / `Aion.GameServer.Network.Aion.ServerPackets.SmTradeInList` | Packet Plan / Packet | Partial | Unit Tested | Partial Parity | Concrete non-live plan and serializer now follow reviewed Java write order. Not live-wired and no Java runtime golden-vector comparison exists. |
| `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes` | `SmTradeInList.PacketOpCode` | Opcode Registry | Partial | Unit Tested | Partial Parity | C# constant pins opcode `151` from Java source. Central opcode registry parity remains broad and not fully audited. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeNpcType` | `SmTradeInListPacketPlanService` trade NPC type mapping | Enum / Packet Dependency | Partial | Unit Tested | Partial Parity | Mapping matches existing source-reviewed trade-list indexes for known values. Java runtime enum comparison remains missing. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate` | `TradeListTemplateSummary` consumed by `SmTradeInListPacketPlanService` | Static Data DTO | Partial | Unit Tested | Partial Parity | Trade-in packet planning uses NPC id, NPC type, and raw tab ids without goods/legion filtering. Empty-tab and zero-NPC-id guards are modeled as non-ready. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` `TRADE_IN` | `NpcDialogServiceSelectPlanService` descriptor plus standalone `SmTradeInListPacketPlanService` | Service Boundary / Packet Descriptor | Partial | Unit Tested | Partial Parity | Existing descriptor remains non-live. Packet plan exists but is not composed into production `TRADE_IN` routing yet. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmTradeInListPacketPlanServiceTests.CreatePlan_ModelsJavaWriteOrderWithoutGoodsOrLegionFiltering` | Unit / Packet Plan | `SM_TRADE_IN_LIST.writeImpl`; `DialogService` `TRADE_IN` | Ready plan writes object id, trade type, fixed modifiers, and raw tab ids without goods/legion/limited-item behavior. | Source-reviewed Java write order plus deterministic C# test. | No Java runtime payload capture. |
| `SmTradeInListPacketPlanServiceTests.CreatePlan_MapsJavaTradeNpcTypeIndexes` | Unit / Packet Dependency | `TradeNpcType.index()` | Known trade NPC type strings map to source-reviewed Java indexes. | Deterministic C# mapping test. | No Java runtime enum reflection capture. |
| `SmTradeInListPacketPlanServiceTests.CreatePlan_ReportsUnknownTradeNpcTypeWithoutClaimingReady` | Unit / Packet Guard | `TradeNpcType.index()` dependency | Unknown trade NPC type does not produce a ready packet plan. | C# guard test based on source-reviewed dependency risk. | Java valid XML normally avoids unknown enum values. |
| `SmTradeInListPacketPlanServiceTests.CreatePlan_ReportsInvalidTradeInListForJavaWriteGuards` | Unit / Packet Guard | `SM_TRADE_IN_LIST.writeImpl` guard | Zero NPC id and empty tab lists become non-ready plan status instead of serializing a body. | Source-reviewed Java guard represented conservatively. | Java silently writes no body; C# rejects non-ready serialization. |
| `SmTradeInListPacketPlanServiceTests.SmTradeInList_WritesJavaPayloadOrderFromReadyPlan` | Unit / Packet Payload | `SM_TRADE_IN_LIST.writeImpl`; `ServerPacketsOpcodes` | Unencrypted payload matches source-derived Java field order and opcode constant is `151`. | Deterministic C# payload test from Java source review. | Not a Java-generated golden vector. |
| `SmTradeInListPacketPlanServiceTests.SmTradeInList_RejectsNonReadyPlans` | Unit / Packet Guard | `SM_TRADE_IN_LIST.writeImpl` guard | Non-ready plans are rejected before serialization. | C# guard test. | Java invalid-template write behavior is represented as a conservative C# precondition. |

## Remaining Risks

- `SmTradeInList` is not sent by `GameServerConnection`.
- `NpcDialogServiceSelectPlanService` descriptors do not yet carry a concrete trade-in packet plan.
- Java runtime golden-vector comparison is absent.
- Trade-in purchase execution/AP formulas remain separate and unported for this slice.
- Production `TRADE_IN` routing still needs NPC AI/controller coverage.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 concrete non-live trade-in packet plan and serializer
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 4 blocked/partial categories: production routing, Java runtime vectors, trade-in purchase execution, and concrete descriptor composition
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Compose `SmTradeInListPacketPlan` into the non-live `NpcDialogServiceSelectPlanService`/dialog assembly path for `TRADE_IN`, while keeping production sends disabled.

Recommended starting points:
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogServiceSelectPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeListFactAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogServiceSelectPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`

Keep live sends disabled until runtime price/legion facts, limited-item mutation, Java runtime golden vectors, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: compose the new trade-in packet plan into non-live dialog-service planning.
- Why: the packet plan/serializer exists, but descriptors still carry only `PriceModifier` metadata.
- Files: `NpcDialogServiceSelectPlanService.cs`, `QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`, focused tests, progress/handoff docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Static-data limited-item corpus count comparison design | docs/test planning | Low | Do not claim parity until Java-generated counts exist. |
| B | Java runtime artifact tooling sketch for trade-list vectors | docs only | Low | Can extend `TradeList-Java-Golden-Vector-Design.md`. |
| C | Live legion-level lookup discovery | read-only code search/docs | Low | Avoid touching `GameServerConnection` until a C# legion aggregate exists. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Trade-in packet-plan composition | focused service/tests/docs | production live sends |
| Agent A | Static-data limited-item corpus count comparison design | docs only | code files |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
