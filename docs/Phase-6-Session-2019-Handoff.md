# Phase 6 Session 2019 Handoff - Upgrade Arcade Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2019
Status: Completed

## What Changed

- Added Java golden parser coverage for `CM_UPGRADE_ARCADE.readImpl`.
- Added C# `CmUpgradeArcade` and registered opcode `246` for `InGame`.
- Added C# factory/parser coverage for C `action`, D `sessionId`, valid `InGame`, and invalid `Authed`.
- Added a documented no-op handler boundary in `GameServerConnection`; live upgrade-arcade behavior remains unported.

## Validation

- Focused Java `CM_UPGRADE_ARCADE` parser golden passed with 1 test method.
- Focused C# upgrade-arcade factory test passed with 1 test.
- Broad C# game-server suite passed with 5125 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 78 game-server tests.

## Known Gaps

- This unit proves only `action`/`sessionId` parsing, opcode registration, and source-reviewed deferred handler behavior.
- Java live behavior checks `EventsConfig.ENABLE_EVENT_ARCADE`, gets the active player, dispatches actions `0`-`5` to `UpgradeArcadeService`, and warning-logs unknown actions.
- C# does not yet perform live event gating, arcade service dispatch, warning logging, arcade packet sends, persistence, encrypted frame handling, or real-client validation for this route.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_UPGRADE_ARCADE_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmUpgradeArcade.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2019-Completion.md`
- `docs/Phase-6-Session-2019-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: continue packet-factory discovery with source-reviewed opcode `23` `CM_OPEN_STATICDOOR` as a parser candidate. Java reads D `doorId`; live behavior calls `StaticDoorService.openStaticDoor(player, doorId)`, so runtime wiring should remain deferred unless separately scoped.

Safe alternative candidates:

- Inspect another compact unregistered parser boundary with Java golden evidence.
- Inspect Java `SM_UNWRAP_ITEM` writer parity as a server-packet-only unit.
- Inspect `CM_WINDSTREAM` parser only if its live flight/quest/emotion side effects remain deferred.
- Inspect `CM_FIND_GROUP` only if a small action-specific parser vector is selected.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live upgrade-arcade parity from UOW-2019; only parser/factory coverage is backed by objective evidence.
- UOW-2018 covered `CM_UNWRAP_ITEM`; UOW-2019 covered `CM_UPGRADE_ARCADE` parser/factory coverage.
