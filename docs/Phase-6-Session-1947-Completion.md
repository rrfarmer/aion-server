# Phase 6 Session 1947 Completion - CM_BUY_ITEM Non-Positive Item Id Audit Guard Capture

Date: 2026-06-01
Unit of Work: UOW-1947
Status: Completed

## Scope

- Captured Java runtime/source behavior for the `CM_BUY_ITEM.readImpl` non-positive item id guard.
- Kept audit side effects neutralized and out of scope.
- Aligned C# parser coverage across the non-private-store action ids that share the same guard.

## Work Discovery

- Re-read the current Phase 6 handoff and inspected the Java `CM_BUY_ITEM.readImpl` guard order.
- Reviewed the existing neutralized audit setup from the previous audit-capture units.
- Inspected C# `CmBuyItem.ReadPayload` behavior and `CmBuyItemTests.ReadFrom_NonPositiveItemObjectIdAuditsForNonPrivateStoreActions`.

## Changes

- Added Java test `readImpl_nonPositiveItemIdSetsAuditForNonPrivateStoreAction`.
- The Java capture uses seller `7001`, action `13`, amount `1`, item id `0`, and count `1`.
- The Java test verifies `isAudit=true`, the invalid last-read item/count values, seller/action/amount fields, and an empty `TradeList`.
- Broadened C# non-positive item-object-id theory coverage to actions `1`, `2`, `13`, `14`, `15`, `16`, and `17`.
- Left action `0` covered separately because Java treats that branch as private-store list-index parsing.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java read-capture test passed with 6 test methods.
- Focused C# `CmBuyItemTests` passed with 19 tests.
- Java/Maven reactor test run passed with 1 commons test and 19 game-server tests.
- Broad C# game-server suite passed with 4982 tests.

## Known Gaps

- Java count-above-20000 audit branch remains uncaptured in Java runtime tests.
- Java audit side effects are deliberately neutralized and not validated.
- The Java test uses test-only `Unsafe.allocateInstance`, reflection, and empty `SkillData`; the expected `GMService` "No GM skills found" warning appears.
- Live `CM_BUY_ITEM.runImpl`, target validation, service mutation, encrypted frame decoding, socket dispatch, and real-client validation remain pending.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM_ReadGuardGoldenTest.java`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1947-Completion.md`
- `docs/Phase-6-Session-1947-Handoff.md`

## Parity Position

- Runtime compared, Partial Parity for the non-positive item id parser guard.
- No full-artifact verified parity is claimed for `CM_BUY_ITEM` or `AuditLogger`.
- Java remains the source of truth for guard order and side effects.
