# Phase 6 Session 1973 Handoff - CM_MOVE_ITEM Signed Slot

Date: 2026-06-01
Unit of Work: UOW-1973
Status: Completed

## What Changed

- Added parser-only C# `CmMoveItem` for Java `CM_MOVE_ITEM`.
- Registered opcode `156` as `IN_GAME`, matching Java `[C_MOVE_ITEM_TO_ANOTHER_SLOT]`.
- Parsed `slot` with `PacketBuffer.ReadSignedH()`.
- Added an explicit `GameServerConnection` no-op boundary for Java `ItemMoveService.moveItem`.
- Added matching Java golden and C# parser/factory coverage for `slot = 0xFFFF` -> signed `-1`.
- Kept this parser-only; no inventory or warehouse move side effects were enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmMoveItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_MOVE_ITEM_ReadSignedSlotGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# move-item parser/factory slice passed with 2 tests.
- Focused Java move-item golden test passed with 1 test method.
- Broad C# game-server suite passed with 5034 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 30 game-server tests.

## Known Gaps

- This unit proves only `CM_MOVE_ITEM` parser/factory behavior.
- Live inventory/warehouse move behavior, storage mutation, DB persistence, packet dispatch, encrypted frame capture, and real-client validation remain unverified.
- Java `ItemMoveService.moveItem` has not been ported.
- Other Java signed `readH()` call sites still need separate audits.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_MOVE_ITEM_ReadSignedSlotGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmMoveItem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmMoveItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1973-Completion.md`
- `docs/Phase-6-Session-1973-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE_ITEM`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE_ITEM.runImpl`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`
- `com.aionemu.commons.network.packet.BaseClientPacket`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmMoveItem`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.Commons.Network.PacketBuffer`
- `Aion.GameServer.Tests.CmMoveItemTests`

## Parity Table Updates

- Added Session 1973 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_MOVE_ITEM.readImpl`
  - opcode `156` factory registration
  - explicit no-op `CM_MOVE_ITEM.runImpl` handler boundary
  - signed `readH()` slot use through `PacketBuffer.ReadSignedH()`

## Next Recommended Unit of Work

- Next sequential task: continue the signed Java `readH()` audit with `CM_LEGION` permission fields if a parser-only C# packet and Java golden test can be added safely without enabling legion mutation side effects.

Safe alternative candidates:

- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
- Run a full Maven reactor validation if the prior login-server `PlayerTransferService.java:42` compile observation needs root-cause proof.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Continue signedness carefully: `PacketBuffer.ReadH()` still means unsigned in the C# port, while `ReadSignedH()` is now available for Java `readH()` fields with objective evidence.
- Do not claim live item move parity from UOW-1973; this is parser/factory evidence only.
