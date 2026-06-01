# Phase 6 Session 2020 Handoff - Open Static Door Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2020
Status: Completed

## What Changed

- Added Java golden parser coverage for `CM_OPEN_STATICDOOR.readImpl`.
- Added C# `CmOpenStaticDoor` and registered opcode `23` for `InGame`.
- Added C# factory/parser coverage for D `doorId`, valid `InGame`, and invalid `Authed`.
- Added a documented no-op handler boundary in `GameServerConnection`; live static-door behavior remains unported.

## Validation

- Focused Java `CM_OPEN_STATICDOOR` parser golden passed with 1 test method.
- Focused C# open-static-door factory test passed with 1 test.
- Broad C# game-server suite passed with 5126 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 79 game-server tests.

## Known Gaps

- This unit proves only `doorId` parsing, opcode registration, and source-reviewed deferred handler behavior.
- Java live behavior retrieves the active player and calls `StaticDoorService.openStaticDoor(player, doorId)`.
- C# does not yet perform live static-door service dispatch, door state mutation, packet fanout, persistence, encrypted frame handling, or real-client validation for this route.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_OPEN_STATICDOOR_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmOpenStaticDoor.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2020-Completion.md`
- `docs/Phase-6-Session-2020-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: continue packet-factory gap discovery from the missing registered-opcode list, preferring another compact parser with deferrable runtime behavior. `CM_WINDSTREAM` is parser-compact but live-heavy; `CM_FIND_GROUP` can be considered only with a small action-specific parser vector.

Safe alternative candidates:

- Inspect another compact unregistered parser boundary with Java golden evidence.
- Inspect Java `SM_UNWRAP_ITEM` writer parity as a server-packet-only unit.
- Inspect static-door service behavior read-only before any runtime wiring.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live static-door parity from UOW-2020; only parser/factory coverage is backed by objective evidence.
- UOW-2019 covered `CM_UPGRADE_ARCADE`; UOW-2020 covered `CM_OPEN_STATICDOOR` parser/factory coverage.
