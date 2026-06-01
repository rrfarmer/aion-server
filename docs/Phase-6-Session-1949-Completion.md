# Phase 6 Session 1949 Completion - SM_REPURCHASE Non-Empty Item Golden Capture

Date: 2026-06-01
Unit of Work: UOW-1949
Status: Completed

## Scope

- Captured Java runtime/source bytes for a non-empty `SM_REPURCHASE.writeImpl` payload.
- Kept the item fixture intentionally simple: one non-equipment item that emits only the general-info item blob.
- Tightened C# `SmRepurchase` packet coverage to exact Java payload bytes for that fixture.

## Work Discovery

- Re-read the Session 1948 handoff and inspected Java `SM_REPURCHASE.writeImpl`.
- Inspected Java `ItemInfoBlob.getFullBlob` and `GeneralInfoBlobEntry`.
- Inspected Java `Item` and `ItemTemplate` fields needed for a minimal fixture.
- Reviewed C# `SmRepurchase`, `SmInventoryInfo.WriteItemInfoBlob`, and `SmRepurchaseTests`.

## Changes

- Added Java test `writeImpl_writesSimpleRepurchaseItemWithGeneralInfoBlobAndPrice`.
- The Java fixture uses target object id `9001`, item object id `7001`, template id `100000001`, description id `40000`, mask `1`, count `1`, and repurchase price `12345`.
- The Java test captures the exact payload hex:
  `29230000010000000100591B000001E1F50524008138010000002200000100010000000000000000000000000000000000000000000000000000000012003930000000000000`
- Updated C# `SmRepurchaseTests.WritePayload_WritesRepurchaseItemsWithBlobThenPrice` to assert the exact Java payload bytes.
- C# packet structure assertions remain in place after the byte assertion.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java `SM_REPURCHASE_GoldenTest` passed with 2 test methods.
- Focused C# `SmRepurchaseTests` passed with 3 tests.
- Java/Maven reactor test run passed with 1 commons test and 21 game-server tests.
- Broad C# game-server suite passed with 4989 tests.

## Known Gaps

- The Java golden fixture uses test-only `Unsafe.allocateInstance` and reflection.
- The fixture validates only the simple non-equipment general-info item blob.
- Live `SM_REPURCHASE(Player, npcId)` constructor snapshot behavior through `RepurchaseService`, encrypted frame behavior, live dialog dispatch, item persistence, and real-client validation remain pending.
- Full item-info blob parity for equipment and advanced item state remains partial.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_REPURCHASE_GoldenTest.java`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmRepurchaseTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1949-Completion.md`
- `docs/Phase-6-Session-1949-Handoff.md`

## Parity Position

- Golden File Tested, Partial Parity for the simple non-empty `SM_REPURCHASE` serialization slice.
- No full-artifact verified parity is claimed for `SM_REPURCHASE`, `ItemInfoBlob`, or live repurchase service behavior.
- Java remains the source of truth for packet layout and item-info blob ordering.
