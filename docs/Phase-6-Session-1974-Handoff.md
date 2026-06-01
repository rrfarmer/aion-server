# Phase 6 Session 1974 Handoff - CM_LEGION Signed Permissions

Date: 2026-06-01
Unit of Work: UOW-1974
Status: Completed

## What Changed

- Added parser-only C# `CmLegion` for Java `CM_LEGION`.
- Registered opcode `45` as `IN_GAME`, matching Java `[C_GUILD]`.
- Modeled Java `CM_LEGION.readImpl` read branches for known exOpcodes.
- Parsed exOpcode `0x0D` permission fields with `PacketBuffer.ReadSignedH()`.
- Added an explicit `GameServerConnection` no-op boundary for Java `LegionService` dispatch.
- Added matching Java golden and C# parser/factory coverage for high-bit permission values.
- Kept this parser-only; no legion service side effects were enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_LEGION_ReadSignedPermissionsGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# legion parser/factory slice passed with 3 tests.
- Focused Java legion golden test passed with 1 test method.
- Broad C# game-server suite passed with 5037 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 31 game-server tests.

## Known Gaps

- This unit proves only `CM_LEGION` parser/factory behavior.
- Live legion service behavior, DB persistence, packet dispatch, encrypted frame capture, and real-client validation remain unverified.
- Unknown exOpcode Java warning behavior is not modeled.
- Other Java signed `readH()` call sites still need separate audits.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_ReadSignedPermissionsGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmLegion.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1974-Completion.md`
- `docs/Phase-6-Session-1974-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION.runImpl`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`
- `com.aionemu.commons.network.packet.BaseClientPacket`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmLegion`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.Commons.Network.PacketBuffer`
- `Aion.GameServer.Tests.CmLegionTests`

## Parity Table Updates

- Added Session 1974 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_LEGION.readImpl`
  - opcode `45` factory registration
  - explicit no-op `CM_LEGION.runImpl` handler boundary
  - signed `readH()` permission use through `PacketBuffer.ReadSignedH()`

## Next Recommended Unit of Work

- Next sequential task: continue the signed Java `readH()` audit by scanning remaining client packet call sites and selecting the smallest surface with either an existing C# parser or a safe parser-only registration.

Safe alternative candidates:

- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
- Run a full Maven reactor validation if the prior login-server `PlayerTransferService.java:42` compile observation needs root-cause proof.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Continue signedness carefully: `PacketBuffer.ReadH()` still means unsigned in the C# port, while `ReadSignedH()` is now available for Java `readH()` fields with objective evidence.
- Do not claim live legion parity from UOW-1974; this is parser/factory evidence only.
