# Phase 6ZD Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1168
Status: Phase 6 continues; trade-list/trade-in live sends remain disabled.

## Session Summary

UOW-1168 continued the trade-list parity track by turning the existing Java runtime vector artifact contract into a concrete generator implementation plan.

Files changed:

- `docs/TradeList-Java-Vector-Generator-Implementation-Plan.md`
- `docs/TradeList-Java-Golden-Vector-Design.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ZD-Completion.md`

No production Java, C# runtime code, packet send wiring, or socket behavior changed.

## What Changed

- Added a Java generator implementation plan covering:
  - proposed test-only Java runner/package layout;
  - CLI inputs and deterministic output contract;
  - fixture strategy for player, NPC, trade list, goods list, price, legion, and limited-item facts;
  - preferred and fallback packet capture points;
  - canonical byte writer requirements for `bodyHex`, `canonicalPayloadHex`, and optional `wireFrameHex`;
  - output artifact layout under a future `parity-artifacts/trade-list/java/`;
  - C# verifier follow-up steps;
  - readiness gates before enabling live sends;
  - known Java fixture/reflection/static-singleton risks.
- Linked `TradeList-Java-Golden-Vector-Design.md` to the new implementation plan.
- Added UOW-1168 progress, parity table, metrics, risks, and next recommended work to `PHASE-6-PROGRESS.md`.

## Validation

- `git diff --check` passed.

No `dotnet test` run was required because this was documentation-only and changed no executable code.

## Migration Parity Table - UOW-1168

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.DialogService` | `Aion.GameServer.Services.NpcDialogServiceSelectPlanService` / future vector verifier | Service / Tooling Target | Partial | Manual Only | Needs Verification | Implementation plan documents dialog-service capture as the preferred evidence level for `BUY` and `TRADE_IN`. No Java generator or C# vector verifier exists yet. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | future Java capture hook and C# verifier inputs | Utility / Tooling Target | Not Started | Manual Only | Needs Verification | Preferred packet capture point is documented. No test-only Java hook or player-connection double has been implemented. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | future canonical packet writer comparison convention | Packet Framework / Tooling Target | Not Started | Manual Only | Needs Verification | Canonical `bodyHex`, `canonicalPayloadHex`, and optional `wireFrameHex` byte forms are planned. Reflection/accessor details remain unimplemented. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `Aion.GameServer.Network.Aion.ServerPackets.SmTradeList` / future artifact verifier | Packet | Partial | Manual Only | Needs Verification | Runner plan covers dialog-service and constructor-level vectors, decoded fields, tab filtering, limited item rows, and integer price math. No generated Java bytes exist yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADE_IN_LIST` | `Aion.GameServer.Network.Aion.ServerPackets.SmTradeInList` / future artifact verifier | Packet | Partial | Manual Only | Needs Verification | Runner plan documents fixed modifier, raw tab order, and the empty-body constructor risk. No generated Java bytes exist yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` / future artifact verifier | Packet | Partial | Unit Tested | Partial Parity | Existing C# no-sell payload test is source-derived. Plan adds generator requirements for runtime no-sell artifacts, but none exist yet. |
| `com.aionemu.gameserver.services.LimitedItemTradeService` | `Aion.GameServer.Services.NpcDialogLimitedItemFactAdapterService` / future artifact verifier | Service Dependency | Partial | Manual Only | Needs Verification | Limited-item vector setup and risks are documented. Java sales-window timing, buy-count mutation, and reset behavior remain outside the minimum artifact set. |

Tests added or updated:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Manual / Docs Only | `DialogService`; `PacketSendUtility`; `AionServerPacket`; `SM_TRADELIST`; `SM_TRADE_IN_LIST`; `SM_SYSTEM_MESSAGE`; `LimitedItemTradeService` | No executable tests were added; this unit defines a concrete future Java generator implementation plan. | Java source reviewed and documented; `git diff --check` passed. | Generator classes, generated Java JSON artifacts, and C# verifier tests remain missing. |

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 generator implementation-plan document added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 6 blocked/partial categories: Java generator implementation, generated Java artifacts, C# artifact verifier, live legion-level lookup, limited-item lifecycle, and NPC AI/controller routing
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Java runtime vector generator is still not implemented.
- No Java-generated `SM_TRADELIST`, `SM_TRADE_IN_LIST`, or no-sell `SM_SYSTEM_MESSAGE` artifacts exist.
- C# verifier tests cannot be written until generated artifact shape/files are available.
- Live `SM_TRADELIST`, `SM_TRADE_IN_LIST`, and no-sell sends remain disabled.
- Live legion-level lookup, limited-item lifecycle, and NPC AI/controller routing are not verified for production dialog handling.
- Constructor-level vectors, if used later, must not be mistaken for full `DialogService` branch parity evidence.

## Next Recommended Unit of Work

Primary next unit:

- Audit live legion-level lookup prerequisites for moving beyond staged `BUY` runtime facts.

Suggested scope:

- Read Java `Player.getLegion()`, `Legion.getLegionLevel()`, related legion data holders, and any login/hydration path that populates player legion state.
- Compare against C# player/session/legion models and current `NpcDialogTradeRuntimeFactAdapterService`.
- Produce a docs-only audit that identifies the minimum safe C# runtime fact source for `playerLegionLevel`.
- Keep live `BUY` sends disabled.

Safe parallel candidates:

- Trade-list live-send readiness audit across packet vectors, legion facts, limited items, and NPC AI/controller routing.
- Java generator skeleton feasibility audit, focused only on Maven/test layout and compile path.
- C# verifier artifact reader design, without adding tests until Java artifacts exist.

Do not start live packet send wiring until Java vector artifacts and C# verifier tests exist.
