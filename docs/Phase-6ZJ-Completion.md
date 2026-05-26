# Phase 6ZJ Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1174
Status: Phase 6 continues; trade-list/trade-in live sends remain disabled.

## Session Summary

UOW-1174 broadened the guarded C# reader tests for the future trade-list Java vector artifact schema. The reader now has inline schema-v1 coverage for `SM_TRADELIST`, `SM_TRADE_IN_LIST`, and the no-sell `SM_SYSTEM_MESSAGE` fallback while Java generator execution remains blocked.

Files changed:

- `dotnetConversion/tests/Aion.GameServer.Tests/TradeListJavaVectorArtifactReaderTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ZJ-Completion.md`

No Java generator artifacts, packet byte comparisons, live sends, or production runtime behavior changed.

## What Changed

- Added `ParseTradeInArtifact_ReadsSchemaV1PacketFields`.
- Added `ParseNoSellArtifact_ReadsSystemMessageFields`.
- Extended decoded artifact DTOs with optional system-message fields:
  - `messageId`
  - `npcNameParam`
  - `messageParams`
- Kept `FindTradeListJavaArtifacts_IsGuardedUntilGeneratorOutputExists` guarded so missing `parity-artifacts/trade-list/java` output does not create a false parity claim.

## Validation

- `git diff --check` passed with an existing line-ending warning only.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "TradeListJavaVectorArtifactReaderTests" --nologo` passed 4 tests.

## Migration Parity Table - UOW-1174

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| future `com.aionemu.gameserver.parity.trade.TradeListVectorGenerator` artifact schema | `Aion.GameServer.Tests.TradeListJavaVectorArtifactReaderTests` | Test Utility / Artifact Reader | Partial | Unit Tested | Needs Verification | C# now parses schema-v1 samples for trade-list, trade-in, and no-sell packet families. No Java generator output exists yet, so this remains a contract reader rather than runtime parity evidence. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | future C# artifact verifier using `SmTradeList` | Packet / Verifier Target | Partial | Unit Tested | Needs Verification | Inline sample coverage remains schema-only. Missing Java-generated `bodyHex` comparison against live source-of-truth output. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADE_IN_LIST` | future C# artifact verifier using `SmTradeInList` | Packet / Verifier Target | Partial | Unit Tested | Needs Verification | Inline schema-v1 trade-in packet sample is parsed, including opcode, semantic key, modifier, and tab ids. No generated Java bytes or C# packet comparison yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | future C# artifact verifier using `SmSystemMessage` | Packet / Verifier Target | Partial | Unit Tested | Needs Verification | Inline no-sell system-message sample is parsed, including message id and NPC parameter fields. Java message serialization and parameter encoding are not yet compared. |

Tests added or updated:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `TradeListJavaVectorArtifactReaderTests.ParseTradeInArtifact_ReadsSchemaV1PacketFields` | Unit / Artifact Contract | `DialogService` `TRADE_IN`; future Java generator schema | Validates C# can parse the planned `SM_TRADE_IN_LIST` artifact metadata, opcode, semantic key, modifier, and trade tab ids. | Deterministic schema-contract test, not Java runtime evidence. | No Java-generated artifact and no packet-byte comparison. |
| `TradeListJavaVectorArtifactReaderTests.ParseNoSellArtifact_ReadsSystemMessageFields` | Unit / Artifact Contract | `DialogService` BUY/TRADE_IN no-sell fallback; `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM`; future Java generator schema | Validates C# can parse the planned no-sell `SM_SYSTEM_MESSAGE` artifact metadata, message id, NPC-name parameter, and empty trade arrays. | Deterministic schema-contract test, not Java runtime evidence. | No Java-generated artifact, no Java string/parameter serialization comparison, and no C# packet-byte comparison. |
| Existing `TradeListJavaVectorArtifactReaderTests.ParseTradeListArtifact_ReadsSchemaV1PacketFields` | Unit / Artifact Contract | `docs/TradeList-Java-Golden-Vector-Design.md`; future Java generator schema | Still validates schema-v1 `SM_TRADELIST` sample parsing. | Deterministic schema-contract test. | No generated Java artifact and no packet-byte comparison. |
| Existing `TradeListJavaVectorArtifactReaderTests.FindTradeListJavaArtifacts_IsGuardedUntilGeneratorOutputExists` | Unit / Guarded Artifact Reader | Future `parity-artifacts/trade-list/java/*.json` output | Still returns cleanly when artifacts are absent and parses schema-v1 files if future artifacts exist. | Guarded test prevents false parity claims while preserving future comparison entry point. | Does not compare semantic fields or bytes yet. |

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 runtime artifacts; 1 guarded C# artifact reader test file extended
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 4 blocked/partial categories: Java generator, generated artifacts, packet-byte comparison, and live packet sends
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Java runtime vector generator and artifacts are still missing because local Java 25/Maven tooling remains unavailable.
- The reader still does not build C# packets or compare `bodyHex`, `canonicalPayloadHex`, or Java system-message parameter serialization.
- Trade-list live sends remain disabled: `SM_TRADELIST`, `SM_TRADE_IN_LIST`, and no-sell `SM_SYSTEM_MESSAGE` are not emitted from the production socket path.
- The inline JSON samples are deliberately illustrative contract samples and must not be treated as Java runtime output.

## Next Recommended Unit of Work

Primary next unit:

- Build the first semantic comparison layer for schema-v1 `SM_TRADELIST` artifacts, guarded behind the same missing-artifact check.

Suggested scope:

- Keep the reader safe when Java artifacts are absent.
- Compare decoded trade-list fields first: target object id, trade NPC type, buy modifier, fixed client modifier, buy/sell tab flags, tab ids, and limited-item rows.
- Do not claim byte parity unless generated Java `bodyHex` / `canonicalPayloadHex` artifacts exist and C# packet bytes are compared deterministically.
- Do not enable live packet sends.

Safe parallel candidates:

- DB integration proof for `Player.LegionLevel` hydration if an integration database is available.
- Broader `PricesService` parity audit for `SM_PRICES`, taxes, influence, service prices, and sell rewards.
- Java generator skeleton only in an environment with Java 25 JDK and Maven.

Do not start live packet send wiring until Java vector artifacts and C# verifier tests exist.
