# Phase 6 Session 1950 Completion - SM_REPURCHASE Equipment Item Golden Capture

Date: 2026-06-01
Unit of Work: UOW-1950
Status: Completed

## Scope

- Captured Java runtime/source bytes for a simple equipment `SM_REPURCHASE.writeImpl` payload.
- Kept the fixture intentionally narrow: one unequipped SWORD item with default equipment state and enchant level `3`.
- Added C# `SmRepurchase` coverage against the exact Java payload bytes.

## Work Discovery

- Re-read Session 1949 handoff and inspected Java `SM_REPURCHASE.writeImpl`.
- Inspected Java `ItemInfoBlob` equipment paths: equipped slot, weapon slot, enchant info, premium option, and general info.
- Inspected Java `ItemSlot` slot masks for `ItemGroup.SWORD`.
- Reviewed C# `SmRepurchase`, `SmInventoryInfo.WriteItemInfoBlob`, and `SmRepurchaseTests`.

## Changes

- Added Java test `writeImpl_writesSimpleEquipmentRepurchaseItemWithEquipmentBlobAndPrice`.
- The Java fixture uses target object id `9001`, item object id `7002`, template id `100000001`, description id `40000`, mask `1`, item group `SWORD`, count `1`, enchant `3`, and repurchase price `12345`.
- The Java test captures the exact payload hex:
  `292300000100000001005A1B000001E1F5052400813801000000CB0006000000000000000001010000000000000002000000000000000B000301E1F50500000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000100010000000000000000000000000000000000000000000000000000000012003930000000000000`
- Added C# test `WritePayload_WritesEquipmentRepurchaseItemWithEquipmentBlobThenPrice` asserting the exact Java bytes.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java `SM_REPURCHASE_GoldenTest` passed with 3 test methods.
- Focused C# `SmRepurchaseTests` passed with 4 tests.
- Java/Maven reactor test run passed with 1 commons test and 22 game-server tests.
- Broad C# game-server suite passed with 4990 tests.

## Known Gaps

- The Java golden fixture uses test-only `Unsafe.allocateInstance` and reflection.
- Equipment coverage is limited to a simple unequipped SWORD with default sockets/godstone/conditioning/polish/stat-bonus/wrap state.
- Live `SM_REPURCHASE(Player, npcId)` constructor snapshot behavior through `RepurchaseService`, encrypted frame behavior, live dialog dispatch, item persistence, and real-client validation remain pending.
- Full item-info blob parity for additional equipment groups and advanced item state remains partial.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_REPURCHASE_GoldenTest.java`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmRepurchaseTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1950-Completion.md`
- `docs/Phase-6-Session-1950-Handoff.md`

## Parity Position

- Golden File Tested, Partial Parity for the simple equipment `SM_REPURCHASE` serialization slice.
- No full-artifact verified parity is claimed for `SM_REPURCHASE`, `ItemInfoBlob`, or live repurchase service behavior.
- Java remains the source of truth for packet layout and item-info blob ordering.
