# Phase 6 Session 1977 Handoff - CM_HOUSE_KICK Signed Padding

Date: 2026-06-01
Unit of Work: UOW-1977
Status: Completed

## What Changed

- Updated C# `CmHouseKick` to consume Java `CM_HOUSE_KICK` ignored padding `readH()` with `PacketBuffer.ReadSignedH()`.
- Added C# parser/factory coverage for opcode `72` state gating and high-bit padding after the option byte.
- Added matching Java golden coverage for high-bit padding field alignment and full packet consumption.
- Kept live house-kick behavior out of scope.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmHouseKickSignedPaddingTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_HOUSE_KICK_ReadSignedPaddingGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# house-kick padding parser/factory slice passed with 2 tests.
- Focused Java house-kick padding golden test passed with 1 test method.
- Broad C# game-server suite passed with 5043 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 34 game-server tests.

## Known Gaps

- This unit proves only `CM_HOUSE_KICK` parser padding consumption and opcode state gating for the focused test.
- Live visitor kick side effects, active-house lookup parity, friend exclusion, audit logging, packet dispatch, encrypted frame capture, and real-client validation remain unverified.
- Other Java signed `readH()` call sites still need separate audits.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_HOUSE_KICK_ReadSignedPaddingGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmHouseKick.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmHouseKickSignedPaddingTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1977-Completion.md`
- `docs/Phase-6-Session-1977-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_HOUSE_KICK`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_HOUSE_KICK.runImpl`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`
- `com.aionemu.commons.network.packet.BaseClientPacket`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmHouseKick`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.Commons.Network.PacketBuffer`
- `Aion.GameServer.Tests.CmHouseKickSignedPaddingTests`

## Parity Table Updates

- Added Session 1977 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_HOUSE_KICK.readImpl`
  - opcode `72` factory registration
  - `CM_HOUSE_KICK.runImpl` live handler boundary
  - ignored signed `readH()` padding use through `PacketBuffer.ReadSignedH()`

## Next Recommended Unit of Work

- Next sequential task: continue the signed Java `readH()` audit by inspecting another ignored-padding or parser-only call site.

Safe alternative candidates:

- Inspect `CM_MANASTONE`, `CM_QUESTION_RESPONSE`, `CM_PING`, `CM_TELEPORT_SELECT`, `CM_UI_SETTINGS`, or unported `CM_TOGGLE_SKILL_DEACTIVATE`.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Continue signedness carefully: `PacketBuffer.ReadH()` is unsigned in the C# port, while `ReadSignedH()` should be used for Java `readH()` fields only with objective evidence.
- Do not claim live house-kick parity from UOW-1977; this is parser/factory/padding evidence only.
