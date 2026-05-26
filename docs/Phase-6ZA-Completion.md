# Phase 6ZA Completion - UOW-1165 Trade Packet Runtime Vector Design

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by refining the Java runtime vector tooling design for `SM_TRADELIST`, `SM_TRADE_IN_LIST`, and no-sell system-message scenarios.

This was a docs-only evidence-gate unit. No production code changed, and live sends remain disabled.

## Files Changed

- `docs/TradeList-Java-Golden-Vector-Design.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ZA-Completion.md`

## Implementation Notes

- Added a UOW-1165 runtime vector artifact contract to `TradeList-Java-Golden-Vector-Design.md`.
- Documented Java `AionServerPacket.write` packet framing and encryption boundaries.
- Recommended `canonicalPayloadHex` as the primary serializer comparison artifact before encrypted socket frame comparison.
- Defined a JSON schema shape for future Java artifacts, including input fields, runtime facts, packet order, byte forms, decoded fields, semantic keys, and notes.
- Added required scenarios for BUY, TRADE_IN, no-template, missing goods, legion-restricted goods, mixed filtered tabs, limited items, and non-default pricing.
- Mapped Java artifact fields to existing C# comparison targets.

## Validation

- `git diff --check` passed with only existing line-ending warnings.
- No test run was needed for this docs-only design unit.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.AionServerPacket` | `docs/TradeList-Java-Golden-Vector-Design.md` / future verifier | Packet Framework / Tooling Design | Not Started | Manual Only | Needs Verification | Framing/encryption boundary is documented for future vector capture. No Java byte artifacts or C# verifier exist yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `SmTradeList` / `SmTradeListPacketPlanService` comparison target | Packet / Tooling Design | Partial | Manual Only | Needs Verification | Artifact schema now defines canonical payload/body/wire bytes and decoded fields. No Java runtime vectors generated yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADE_IN_LIST` | `SmTradeInList` / `SmTradeInListPacketPlanService` comparison target | Packet / Tooling Design | Partial | Manual Only | Needs Verification | Trade-in-specific decoded fields and scenario are documented. No Java runtime vectors generated yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | future no-sell concrete packet comparison target | Packet / Tooling Design | Partial | Manual Only | Needs Verification | No-sell semantic keys and required decoded fields are documented, but concrete C# packet-byte comparison remains open. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | future Java capture hook design | Utility / Tooling Design | Not Started | Manual Only | Needs Verification | Suggested capture points are documented. No instrumentation, reflection helper, or runner exists yet. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Manual / Docs Only | `AionServerPacket`; `PacketSendUtility`; `SM_TRADELIST`; `SM_TRADE_IN_LIST`; `SM_SYSTEM_MESSAGE` | No executable tests were added; this unit defines a future runtime-vector artifact contract and comparison plan. | Java source reviewed and documented. | Artifact generator, Java JSON artifacts, and C# verifier remain missing. |

## Remaining Risks

- Java runtime vector generator does not exist yet.
- No Java-generated packet payload artifacts have been captured.
- C# comparison tests still use source-derived expectations rather than Java runtime vectors.
- No-sell `SM_SYSTEM_MESSAGE` concrete packet comparison remains open.
- Live `SM_TRADELIST`, `SM_TRADE_IN_LIST`, and no-sell sends remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 runtime-vector design extension added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: Java vector generator, Java artifacts, C# artifact verifier, no-sell packet-byte comparison, and live send wiring
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Audit or implement the concrete no-sell `SM_SYSTEM_MESSAGE` packet prerequisite for trade-list and trade-in fallback scenarios, keeping production sends disabled.

Recommended starting points:
- Java `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- Java `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`

Keep live `SM_TRADELIST`, `SM_TRADE_IN_LIST`, and no-sell system-message sends disabled until Java runtime vectors and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: no-sell `SM_SYSTEM_MESSAGE` packet prerequisite audit or focused source-derived packet test.
- Why: staged trade-list/trade-in fallback descriptors exist, but concrete no-sell packet-byte behavior has not been brought into the trade-list readiness gate.
- Files: likely `SmSystemMessage.cs`, packet tests, and docs if implementing; docs only if auditing.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Duplicate-id source fixture regression | `StaticDataLoadingTests.cs` | Low | Focus on Java last-write-wins behavior with a small XML fixture. |
| B | Java vector generator implementation sketch | docs only | Low | Extend the design into a concrete runner plan without code. |
| C | Live legion-level lookup discovery | read-only code search/docs | Low | Avoid production wiring until a C# legion aggregate exists. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | No-sell packet prerequisite audit/test | packet docs/tests or exclusive packet file ownership | live socket sends |
| Agent A | Duplicate-id source fixture regression | `StaticDataLoadingTests.cs` only | packet files, progress/handoff docs |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
