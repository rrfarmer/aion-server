# Phase 6YR Completion - UOW-1156 Trade-List Java Golden-Vector Design

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity with a docs-only Java runtime golden-vector design for `SM_TRADELIST` and the no-sell `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM`.

The current C# packet tests are source-derived and should remain Partial Parity / Needs Verification until Java runtime artifacts exist and are compared.

## Files Changed

- `docs/TradeList-Java-Golden-Vector-Design.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YR-Completion.md`

## Implementation Notes

- Added Java breadcrumbs for the full `BUY` path: `CM_DIALOG_SELECT`, `NpcController`, `DialogService`, `PricesService`, `PricesConfig`, `SM_TRADELIST`, `SM_SYSTEM_MESSAGE`, `LimitedItemTradeService`, and `LimitedItem`.
- Defined a packet observation schema with ordered packet sequence, packet class, opcode, payload bytes, semantic key, message fields, player id, NPC object id, and NPC template id.
- Defined decoded `SM_TRADELIST` semantic fields needed to separate fact mismatches from byte serialization mismatches.
- Defined no-sell system-message semantic fields including message id `1300336` and NPC-name parameter.
- Listed required Java scenarios before live `BUY` send enablement, including missing trade list, missing goods, legion-restricted goods, mixed tabs, limited items, non-default vendor modifier, and live legion level.
- No production code changed in this unit.

## Validation

- No test run was needed for this docs-only design unit.
- `git diff --check` passed with only existing line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `Aion.GameServer.Network.Aion.ServerPackets.SmTradeList` / `docs/TradeList-Java-Golden-Vector-Design.md` | Packet / Verification Design | Partial | Unit Tested | Needs Verification | Design now specifies exact Java runtime packet artifact requirements. Existing C# tests are source-derived; Java payload artifacts are not generated yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM` | `SmSystemMessage.BuySellHeDoesNotSellItem` / design doc | Packet Helper / Verification Design | Partial | Unit Tested | Needs Verification | Design captures message id `1300336`, NPC-name parameter, packet schema, and no-sell scenarios. No Java runtime payload comparison yet. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `GameServerConnection.CreateNonLiveBuyDialogSelectPlan` / staged planners / design doc | Service Boundary / Verification Design | Partial | Unit Tested | Partial Parity | Runtime sequence and scenarios are documented for `BUY`. Live C# packet sends and Java runtime comparison remain disabled/missing. |
| `com.aionemu.gameserver.services.trade.PricesService.getVendorBuyModifier` | `NpcDialogTradeRuntimeFactAdapterService` / design doc | Service Dependency / Verification Design | Partial | Unit Tested | Needs Verification | Design requires a non-default vendor modifier scenario. C# still uses explicit staged default unless injected. |
| `com.aionemu.gameserver.model.team.legion.Legion.getLegionLevel` | `NpcDialogTradeRuntimeFactAdapterService` / design doc | Model Dependency / Verification Design | Partial | Unit Tested | Needs Verification | Design requires no-legion and live-legion scenarios. C# still lacks live legion object lookup. |
| `com.aionemu.gameserver.services.LimitedItemTradeService` | `NpcDialogLimitedItemFactAdapterService` / design doc | Service Dependency / Verification Design | Partial | Unit Tested | Needs Verification | Design requires limited-item rows with buy count and sell limit in Java runtime artifacts. C# lifecycle/mutation remains staged. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Manual / Docs Only | `SM_TRADELIST`; `SM_SYSTEM_MESSAGE`; `DialogService`; `PricesService`; `Legion`; `LimitedItemTradeService` | No executable tests were added; the unit defines future runtime/golden verification requirements. | Java source reviewed and documented. | Java artifacts still need generation and C# comparison tests. |

## Remaining Risks

- No Java runtime artifacts were generated in this unit.
- C# live `BUY` sends remain disabled.
- C# price config, live legion lookup, limited-item mutation, and NPC AI/controller routing remain staged or missing.
- Exact packet framing convention for Java payload extraction still needs implementation in tooling.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 verification design artifact added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: Java runtime artifact generation, payload comparison tests, live send wiring, live price/legion facts, and limited-item lifecycle
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Implement a narrow non-live `PricesConfig.VENDOR_BUY_MODIFIER` config surface feeding `NpcDialogTradeRuntimeFactAdapterService`, or perform the read-only `SM_TRADE_IN_LIST` Java audit.

Recommended starting points:
- `game-server/src/com/aionemu/gameserver/configs/main/PricesConfig.java`
- `game-server/src/com/aionemu/gameserver/services/trade/PricesService.java`
- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeRuntimeFactAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogTradeRuntimeFactAdapterServiceTests.cs`

Keep live sends disabled until runtime price/legion facts, limited-item mutation, Java runtime golden vectors, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: add a narrow non-live config surface for Java `PricesConfig.VENDOR_BUY_MODIFIER`.
- Why: the runtime fact seam exists, but the vendor modifier still uses an explicit staged default.
- Files: `GameServerOptions.cs`, `NpcDialogTradeRuntimeFactAdapterService.cs`, `GameServerConnection.cs`, focused tests, progress/handoff docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Read-only `SM_TRADE_IN_LIST` Java audit | docs only | Low | Safe analysis for later trade-in packet work. |
| B | Static-data limited-item corpus count comparison design | docs/test planning | Low | Do not claim parity until Java-generated counts exist. |
| C | Java runtime artifact tooling sketch for trade-list vectors | docs only | Low | Can extend the new design doc without touching code. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | `PricesConfig.VENDOR_BUY_MODIFIER` non-live config surface | focused config/service/tests/docs | live packet sends |
| Agent A | Read-only `SM_TRADE_IN_LIST` Java audit | docs only | code files |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
