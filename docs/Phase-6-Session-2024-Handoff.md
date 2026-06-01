# Phase 6 Session 2024 Handoff - Group Data Exchange Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2024
Status: Completed

## What Changed

- Added Java golden parser coverage for `CM_GROUP_DATA_EXCHANGE.readImpl`.
- Added C# `CmGroupDataExchange` and registered opcode `79` for `InGame`.
- Added C# factory/parser coverage for action `1`, a non-`1` action, byte-array payload preservation, valid `InGame`, and invalid `Authed`.
- Added a documented no-op handler boundary in `GameServerConnection`; live group-data exchange fanout remains unported.

## Validation

- Focused Java `CM_GROUP_DATA_EXCHANGE` parser golden passed with 2 test methods.
- Focused C# group-data exchange factory/parser test passed with 1 test.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 84 game-server tests.
- Broad C# game-server suite passed with 5130 tests.

## Known Gaps

- This unit proves only parser/factory behavior.
- Java live behavior checks active player and empty data, rejects payloads over `AionServerPacket.MAX_USABLE_PACKET_BODY_SIZE - 6`, logs oversize data, broadcasts action `1` to nearby recipients, and sends other actions to group/alliance/league online members except self.
- C# does not yet perform live max-size validation, logging, `SM_GROUP_DATA_EXCHANGE` serialization/fanout, team recipient lookup, encrypted frame handling, or real-client validation for this route.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_GROUP_DATA_EXCHANGE_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmGroupDataExchange.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2024-Completion.md`
- `docs/Phase-6-Session-2024-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect `CM_FIND_GROUP` with a narrow parser-only scope.
- Suggested Java vectors:
  - action `0`: UC `action` only
  - action `2`: UC `action`, D `playerOrTeamId`, S `message`, UC `groupType`
  - action `8` is another possible data-bearing candidate: D `instanceMaskId`, UC ignored unknown, S `message`, UC `minMembers`
- Runtime behavior should remain deferred: all `FindGroupService` calls, recruitment/application/instance group mutation, world broadcasts, and `SM_FIND_GROUP` serialization.

Safe alternative candidates:

- Inspect another compact registered parser boundary with Java golden evidence.
- Inspect Java `SM_UNWRAP_ITEM` writer parity as a server-packet-only unit.
- Inspect `SM_GROUP_DATA_EXCHANGE` writer parity before any live group-data fanout work.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live group-data exchange parity from UOW-2024; only parser/factory coverage is backed by objective evidence.
- UOW-2021 covered `CM_WINDSTREAM`; UOW-2022 covered `CM_LEGION_WH_KINAH`; UOW-2023 covered retired opcode `91`; UOW-2024 covered `CM_GROUP_DATA_EXCHANGE`.
