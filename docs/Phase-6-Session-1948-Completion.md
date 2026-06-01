# Phase 6 Session 1948 Completion - CM_BUY_ITEM Count Above Maximum Audit Guard Capture

Date: 2026-06-01
Unit of Work: UOW-1948
Status: Completed

## Scope

- Captured Java runtime/source behavior for the `CM_BUY_ITEM.readImpl` count-above-20000 item guard.
- Kept audit side effects neutralized and out of scope.
- Aligned C# parser coverage across all currently modeled `CM_BUY_ITEM` action ids.

## Work Discovery

- Re-read the Session 1947 handoff and inspected Java `CM_BUY_ITEM.readImpl`.
- Confirmed the item guard order is `count < 0`, non-positive item id for non-private-store actions, then `count > 20000`.
- Reviewed C# `CmBuyItem.ReadPayload` and `CmBuyItemTests.ReadFrom_CountAboveJavaMaximumAudits`.

## Changes

- Added Java test `readImpl_countAboveMaximumSetsAuditAndLeavesTradeListEmpty`.
- The Java capture uses seller `7001`, action `13`, amount `1`, item id `101`, and count `20001`.
- The Java test verifies `isAudit=true`, the invalid last-read item/count values, seller/action/amount fields, and an empty `TradeList`.
- Broadened C# count-above-maximum theory coverage to actions `0`, `1`, `2`, `13`, `14`, `15`, `16`, and `17`.
- Documented through tests that private-store action `0` can accept item id `0` but still rejects count `20001`.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java read-capture test passed with 7 test methods.
- Focused C# `CmBuyItemTests` passed with 26 tests.
- Java/Maven reactor test run passed with 1 commons test and 20 game-server tests.
- Broad C# game-server suite passed with 4989 tests.

## Known Gaps

- Java audit side effects are deliberately neutralized and not validated.
- The Java test uses test-only `Unsafe.allocateInstance`, reflection, and empty `SkillData`; the expected `GMService` "No GM skills found" warning appears.
- Live `CM_BUY_ITEM.runImpl`, target validation, service mutation, encrypted frame decoding, socket dispatch, and real-client validation remain pending.
- Full `CM_BUY_ITEM` parity is not claimed.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM_ReadGuardGoldenTest.java`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1948-Completion.md`
- `docs/Phase-6-Session-1948-Handoff.md`

## Parity Position

- Runtime compared, Partial Parity for the count-above-20000 parser guard.
- No full-artifact verified parity is claimed for `CM_BUY_ITEM` or `AuditLogger`.
- Java remains the source of truth for guard order and side effects.
