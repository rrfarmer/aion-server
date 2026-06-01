# Phase 6 Session 2025 Handoff - Find Group Parser Golden Slice

Date: 2026-06-01
Unit of Work: UOW-2025
Status: Completed

## What Changed

- Added Java golden parser coverage for representative `CM_FIND_GROUP.readImpl` branches.
- Added C# `CmFindGroup` and registered opcode `77` for `InGame`.
- Added C# factory/parser coverage for action `0`, action `2`, action `8`, valid `InGame`, and invalid `Authed`.
- Added a documented no-op handler boundary in `GameServerConnection`; live find-group behavior remains unported.

## Validation

- Focused Java `CM_FIND_GROUP` parser golden passed with 3 test methods.
- Focused C# find-group factory/parser test passed with 1 test.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 87 game-server tests.
- Broad C# game-server suite passed with 5131 tests.

## Known Gaps

- This unit proves only representative parser/factory behavior.
- Java live behavior dispatches `FindGroupService` recruitment, application, instance-group, applicant-response, and member-info actions.
- C# does not yet perform live find-group service calls, world broadcasts, `SM_FIND_GROUP` serialization, encrypted frame handling, socket dispatch, or real-client validation for this route.
- Parser branches beyond action `0`, action `2`, and action `8` are represented from Java source but do not yet have focused golden vectors.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmFindGroup.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2025-Completion.md`
- `docs/Phase-6-Session-2025-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: extend `CM_FIND_GROUP` parser golden coverage to remaining action layouts while keeping `FindGroupService` live behavior deferred.
- Suggested vectors:
  - action `1`: D `playerOrTeamId`, C `serverId`, C `unk1`, C `unk2`, C `unk3`
  - action `3`: action `1` fields plus S `message`, UC `groupType`
  - action `5`: D `playerOrTeamId`
  - action `6` or `7`: D `playerOrTeamId`, S `message`, UC `groupType`, UC `classId`, UC `level`
  - action `9`, `11`, or `15`: D `playerOrTeamId`, D `instanceMaskId`
  - action `12`: D `playerOrTeamId`, C `instanceApplicationReply`
  - action `17`: D `playerOrTeamId`, D `instanceMaskId`, S `message`
  - action `20`: UC `action` only
  - action `25`: D `playerOrTeamId`, D `instanceMaskId`, D `bannedPlayerId`

Safe alternative candidates:

- Inspect `SM_FIND_GROUP` writer parity as a server-packet-only unit.
- Inspect `SM_GROUP_DATA_EXCHANGE` writer parity before any live group-data fanout work.
- Inspect another compact registered parser boundary with Java golden evidence.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live find-group parity from UOW-2025; only representative parser/factory coverage is backed by objective evidence.
- UOW-2024 covered `CM_GROUP_DATA_EXCHANGE`; UOW-2025 covered representative `CM_FIND_GROUP` parser coverage.
