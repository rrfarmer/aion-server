# Phase 6 Session 1946 Completion - CM_BUY_ITEM Negative Count Audit Guard Capture

Date: 2026-06-01
Unit of Work: UOW-1946
Status: Completed

## Work Discovery

- Re-read the latest UOW-1945 handoff before choosing work.
- Inspected Java `CM_BUY_ITEM.readImpl` item guard behavior and C# `CmBuyItemTests` negative-count parser coverage.
- Selected action `13` negative-count capture to exercise the shared Java item/count audit guard without repurchase singleton filtering.

## What Changed

- Extended Java `CM_BUY_ITEM_ReadGuardGoldenTest` with `readImpl_negativeCountSetsAuditAndLeavesPriorTradeListItems`.
- The Java test reads seller `7001`, action `13`, amount `2`, item `(101, 1)`, then item `(102, -1)`, and asserts `isAudit=true`, last-read item/count `102/-1`, and a `TradeList` containing only the prior valid item.
- Broadened C# `CmBuyItemTests.ReadFrom_NegativeCountAuditsAndLeavesOnlyPriorValidItems` into a theory for actions `2` and `13`.
- Kept this unit parser-only. Live audit notification, punishment/logging, `runImpl`, service mutation, socket dispatch, and real-client validation were not enabled.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java read-capture test passed with 5 test methods.
- Focused C# `CmBuyItemTests` passed with 15 tests.
- Java/Maven reactor test run passed with 1 commons test and 18 game-server tests.
- Broad C# game-server suite passed with 4978 tests.

## Known Gaps

- Java non-positive item id and count-above-20000 audit branches remain uncaptured in Java runtime tests.
- Java audit side effects are neutralized and not validated.
- The Java test uses test-only `Unsafe.allocateInstance`, reflection, and empty `SkillData`; the expected `GMService` missing-skills warning appears.
- Live `CM_BUY_ITEM.runImpl`, target validation, service mutation, encrypted frame decoding, and real-client validation remain pending.

## Parity Table Updates

- Added Session 1946 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.readImpl` negative-count item audit guard
  - `AuditLogger.log` call path under neutralized audit setup
- Full `CM_BUY_ITEM` remains Partial Parity. The negative-count audit guard now has Java runtime/source-capture evidence aligned with C# parser tests.
