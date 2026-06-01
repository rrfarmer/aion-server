# Phase 6 Session 1974 Completion - CM_LEGION Signed Permissions

Date: 2026-06-01
Unit of Work: UOW-1974
Status: Completed

## What Changed

- Reviewed Java `CM_LEGION.readImpl`, where exOpcode `0x0D` reads four signed `short` permission fields with `readH()`.
- Added parser-only C# `CmLegion`.
- Registered opcode `45` as `IN_GAME`, matching Java `AionClientPacketFactory`.
- Modeled known Java `CM_LEGION.readImpl` read branches so parser consumption stays Java-shaped for common exOpcodes.
- Added an explicit parser-only no-op boundary in `GameServerConnection` for Java `CM_LEGION.runImpl -> LegionService`.
- Added Java golden and C# parser/factory tests for high-bit permission values reading as signed shorts.
- Kept this parser-only. No legion service side effects were enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_LEGION_ReadSignedPermissionsGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# legion parser/factory slice passed with 3 tests.
- Focused Java `CM_LEGION_ReadSignedPermissionsGoldenTest` passed with 1 test method.
- Broad C# game-server suite passed with 5037 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 31 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- Objective evidence is limited to Java golden test, C# parser/factory tests, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for `CM_LEGION.runImpl` or `LegionService`.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_ReadSignedPermissionsGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmLegion.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1974-Completion.md`
- `docs/Phase-6-Session-1974-Handoff.md`

## Remaining Risks

- Live legion create/invite/leave/kick/rank/announcement/permission/dominion behavior remains unported/unverified.
- Active-player legion membership guards, name normalization, DB persistence, packet dispatch, audit/logging behavior, encrypted frame capture, and real-client validation remain unverified.
- Unknown exOpcode Java warning behavior is not modeled in C#.
- Other Java signed `readH()` call sites remain separate work.

## Next Recommended Unit

- Continue the signed Java `readH()` audit by scanning remaining client packet call sites and selecting the smallest surface with either an existing C# parser or a safe parser-only registration.

Safe alternatives:

- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
- Run a full Maven reactor validation if the prior login-server `PlayerTransferService.java:42` compile observation needs root-cause proof.
