# Phase 6 Session 1991 Completion - Private Store Client Packet Parsers

Date: 2026-06-01
Unit of Work: UOW-1991
Status: Completed

## Work Discovery

- Re-read the required migration docs and latest Phase 6 handoff before choosing work.
- Inspected Java `CM_PRIVATE_STORE`, `CM_PRIVATE_STORE_NAME`, and `AionClientPacketFactory` opcode registration for `119`/`120`.
- Inspected C# `GameClientPacketFactory`, `GameServerConnection`, and existing private-store open/close/item-validation/purchase planners and tests.

## What Changed

- Added C# `CmPrivateStore` for Java opcode `119`, preserving Java field order and unsigned count reads.
- Added C# `CmPrivateStoreName` for Java opcode `120`, preserving Java `readS()` store-message parsing.
- Registered both packets as `InGame` only.
- Added parser-only handler comments in `GameServerConnection` for the deferred Java `runImpl` side effects.
- Added C# parser/factory tests and Java golden tests for both packet layouts.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmPrivateStore" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_PRIVATE_STORE_ReadPayloadGoldenTest,CM_PRIVATE_STORE_NAME_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# `CmPrivateStore` slice passed with 4 tests.
- Focused Java private-store golden slice passed with 3 test methods.
- Broad C# game-server suite passed with 5086 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 51 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PRIVATE_STORE.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PRIVATE_STORE_NAME.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`.
- Objective evidence is limited to parser golden coverage, C# parser/factory unit coverage, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for private-store close/open/create-store behavior.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_PRIVATE_STORE_ReadPayloadGoldenTest.java`
- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_PRIVATE_STORE_NAME_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmPrivateStore.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmPrivateStoreName.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmPrivateStoreTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1991-Completion.md`
- `docs/Phase-6-Session-1991-Handoff.md`

## Remaining Risks

- Live private-store close/open/create-store handler wiring remains unported.
- Store mutation, item validation integration, player state flags, broadcast ordering, persistence, concurrency, encrypted frame capture, and real-client private-store behavior remain unverified.
- Java's unbounded array allocation behavior for huge private-store item counts was not stress-tested beyond unsigned field semantics.

## Next Recommended Unit

- Continue Work Discovery for `CM_PRIVATE_STORE` create/close composition only if it can remain disabled and source-reviewed, or choose another compact parser/factory/model boundary with Java golden evidence.

Safe alternatives:

- Inspect private-store `createStoreWithItems` ordering against existing item-validation/open/close planners without enabling live mutation.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect another `CM_PET` sub-branch only if it can remain disabled and source-reviewed.
- Inspect another compact unported parser/factory or enum/model dependency with Java golden evidence.
