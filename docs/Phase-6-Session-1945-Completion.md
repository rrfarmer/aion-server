# Phase 6 Session 1945 Completion - CM_BUY_ITEM Amount Audit Guard Capture

Date: 2026-06-01
Unit of Work: UOW-1945
Status: Completed

## Work Discovery

- Re-read the latest UOW-1944 handoff before choosing work.
- Inspected Java `CM_BUY_ITEM.readImpl`, `AuditLogger`, `GMService`, `PunishmentConfig`, `LoggingConfig`, `DataManager.SKILL_DATA`, and C# `CmBuyItemTests`.
- Determined the amount-above-maximum audit guard could be isolated by disabling punishment/log-audit side effects and providing empty `SkillData` for a staff-free `GMService`.

## What Changed

- Extended Java `CM_BUY_ITEM_ReadGuardGoldenTest` with `readImpl_amountAboveMaximumSetsAuditBeforeCreatingLists`.
- The Java test reads seller `7001`, action `13`, and amount `37`, then asserts `isAudit=true` with no `TradeList` or `RepurchaseList` created.
- The test restores `PunishmentConfig.PUNISHMENT_ENABLE`, `LoggingConfig.LOG_AUDIT`, and `DataManager.SKILL_DATA`.
- No C# code change was needed because `CmBuyItemTests.ReadFrom_AmountAboveJavaMaximumAuditsBeforeReadingItems` already covers the matching parser behavior.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java read-capture test passed with 4 test methods.
- Focused C# `CmBuyItemTests` passed with 14 tests.
- Java/Maven reactor test run passed with 1 commons test and 17 game-server tests.
- Broad C# game-server suite passed with 4977 tests.

## Known Gaps

- Java invalid item/count audit branches remain uncaptured in Java runtime tests.
- The Java test uses test-only audit side-effect suppression and initializes `GMService` with empty `SkillData`, producing an expected warning about missing GM skills.
- The Java test still uses test-only `Unsafe.allocateInstance` and reflection for isolated setup.
- Live audit notification, punishment/logging behavior, `CM_BUY_ITEM.runImpl`, target validation, service mutation, encrypted frame decoding, and real-client validation remain pending.

## Parity Table Updates

- Added Session 1945 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.readImpl` amount `> 36` audit guard
  - `AuditLogger.log` call path under test-only neutralized audit setup
- Full `CM_BUY_ITEM` remains Partial Parity. The amount audit guard now has Java runtime/source-capture evidence aligned with existing C# parser tests.
