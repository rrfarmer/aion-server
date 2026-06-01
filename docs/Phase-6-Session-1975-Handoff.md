# Phase 6 Session 1975 Handoff - CM_SPLIT_ITEM Signed Slot

Date: 2026-06-01
Unit of Work: UOW-1975
Status: Completed

## What Changed

- Added parser-only C# `CmSplitItem` for Java `CM_SPLIT_ITEM`.
- Registered opcode `157` as `IN_GAME`, matching Java `[C_SPLIT_ITEM]`.
- Parsed Java `slotNum = readH()` with `PacketBuffer.ReadSignedH()`.
- Added an explicit `GameServerConnection` no-op boundary for Java `ItemSplitService.splitItem` dispatch.
- Added matching Java golden and C# parser/factory coverage for high-bit slot values.
- Kept this parser-only; no split-item service side effects were enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmSplitItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_SPLIT_ITEM_ReadSignedSlotGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# split-item parser/factory slice passed with 2 tests.
- Focused Java split-item golden test passed with 1 test method.
- Broad C# game-server suite passed with 5039 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 32 game-server tests.

## Known Gaps

- This unit proves only `CM_SPLIT_ITEM` parser/factory behavior.
- Live `ItemSplitService.splitItem`, storage mutation, persistence, packet dispatch, encrypted frame capture, and real-client validation remain unverified.
- Other Java signed `readH()` call sites still need separate audits.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_SPLIT_ITEM_ReadSignedSlotGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmSplitItem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmSplitItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1975-Completion.md`
- `docs/Phase-6-Session-1975-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_SPLIT_ITEM`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_SPLIT_ITEM.runImpl`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`
- `com.aionemu.commons.network.packet.BaseClientPacket`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmSplitItem`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.Commons.Network.PacketBuffer`
- `Aion.GameServer.Tests.CmSplitItemTests`

## Parity Table Updates

- Added Session 1975 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_SPLIT_ITEM.readImpl`
  - opcode `157` factory registration
  - explicit no-op `CM_SPLIT_ITEM.runImpl` handler boundary
  - signed `readH()` slot use through `PacketBuffer.ReadSignedH()`

## Next Recommended Unit of Work

- Next sequential task: continue the signed Java `readH()` audit by scanning remaining client packet call sites and selecting the smallest surface with either an existing C# parser or a safe parser-only registration.

Safe alternative candidates:

- Inspect ignored-padding signedness candidates such as `CM_APPEARANCE`, `CM_HOUSE_KICK`, `CM_MANASTONE`, `CM_QUESTION_RESPONSE`, or `CM_PING`.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Continue signedness carefully: `PacketBuffer.ReadH()` is unsigned in the C# port, while `ReadSignedH()` should be used for Java `readH()` fields only with objective evidence.
- Do not claim live split-item parity from UOW-1975; this is parser/factory evidence only.
