# Phase 6 Bind-Point Teleport SM_PLAYER_INFO Viewer Race Projection

Date: May 26, 2026
Unit of Work: UOW-1266
Scope: Add focused C# packet support for Java `SM_PLAYER_INFO` viewer-sensitive race projection.
Source of truth: Java project.

## Summary

UOW-1266 adds a scalar `SmPlayerInfoViewerContext` packet input model for Java's active-viewer race projection branch in `SM_PLAYER_INFO.writeImpl`.

This is still not live player-info parity. C# does not read live active-player state from `GameServerConnection`, does not execute `Player.isEnemy`, and does not model all custom player-state behavior. The packet can now represent the source-derived inputs needed for Java's race byte projection.

## Java Source Findings

- Java `SM_PLAYER_INFO.writeImpl` reads `Player activePlayer = con.getActivePlayer()`.
- Race byte is `activePlayer.isEnemy(player) ? activePlayer.getOppositeRace().getRaceId() : player.getRace().getRaceId()`.
- If either player is in `CustomPlayerState.NEUTRAL_TO_ALL_PLAYERS`, Java overrides the race byte to `activePlayer.getRace().getRaceId()`.
- `Player.getOppositeRace()` returns Asmodians for Elyos and Elyos otherwise.

## C# Implementation

Updated `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPlayerInfo.cs`:

- Added `SmPlayerInfoViewerContext`.
- Added `SmPlayerInfo(Player, bool enemy, SmPlayerInfoViewerContext? viewerContext, PlayerExperienceTable? experienceTable = null)`.
- Default behavior remains unchanged when no viewer context is supplied.
- When `ActivePlayerIsEnemyToPlayer=true`, race byte uses the active viewer's opposite race.
- When `EitherPlayerNeutralToAllPlayers=true`, race byte uses the active viewer's race.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "SmPlayerInfo" --nologo` passed 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo" --nologo` passed 266 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1266

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmPlayerInfo` | Packet / Serialization | Partial | Unit Tested | Partial Parity | C# now models the Java viewer-sensitive race byte and enemy creature-type byte from supplied scalar context. It still lacks live active-player context and Java runtime byte comparison. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.isEnemy` | `SmPlayerInfoViewerContext.ActivePlayerIsEnemyToPlayer` | Viewer Context / Packet Input | Partial | Unit Tested | Needs Verification | C# receives the computed enemy fact as metadata. It does not execute Java `Player.isEnemy`, duel/PvP/custom-state rules, or FFA-team logic. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getOppositeRace` | `SmPlayerInfoViewerContext` race projection in `SmPlayerInfo` | Packet Race Projection | Partial | Unit Tested | Partial Parity | C# maps active viewer Elyos to Asmodians and any non-Elyos/Asmo input through the existing player-race helper. Only playable race projection is modeled. |
| `com.aionemu.gameserver.model.gameobjects.player.CustomPlayerState.NEUTRAL_TO_ALL_PLAYERS` | `SmPlayerInfoViewerContext.EitherPlayerNeutralToAllPlayers` | Custom State Packet Input | Partial | Unit Tested | Needs Verification | C# can represent the neutral override as supplied metadata. It does not store or compute live custom player-state masks. |
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | player known-list side-effect descriptor stack plus `SmPlayerInfoViewerContext` | Controller Packet Dependency | Partial | Unit Tested | Needs Verification | Packet prerequisites improved, but side-effect descriptors still do not instantiate/send packets or compute active viewer facts. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPlayerInfo_ViewerEnemyContextProjectsOppositeRaceLikeJavaActivePlayer` | Unit / Packet | `SM_PLAYER_INFO.writeImpl`; `Player.getOppositeRace` | Active Asmodian viewer with enemy fact projects visible-player race byte to Elyos. | Source-derived byte assertion. | Enemy fact is supplied, not computed by live Java-equivalent rules. |
| `SmPlayerInfo_NeutralToAllViewerContextForcesActivePlayerRace` | Unit / Packet | `SM_PLAYER_INFO.writeImpl`; `CustomPlayerState.NEUTRAL_TO_ALL_PLAYERS` | Neutral override writes active viewer race byte even when enemy fact is true. | Source-derived byte assertion. | Neutral state is supplied metadata, not live player custom-state storage. |

## Remaining Risks

- `SmPlayerInfo` still does not read live active player context from `GameServerConnection`.
- `Player.isEnemy`, PvP/duel/custom-state/FFA-team logic, and `isAggroIconTo` are not ported into packet construction.
- `PlayerController.sendPlayerInfoPackets` remains descriptor-only.
- `SmPlayerStance` and `SmAbnormalEffect` packet classes remain missing.
- No Java runtime packet-order capture or golden-byte comparison was performed.
- Threading, reflection, date/time, precision/rounding, and broader packet serialization behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 focused packet viewer-context model plus 2 focused packet tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live active-player packet context, 1 Java enemy/custom-state computation path, 1 live controller side-effect dispatcher, 1 `SmPlayerStance` packet, 1 `SmAbnormalEffect` packet, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a focused `SmPlayerStance` packet serializer prerequisite, or add a non-live player-info descriptor-to-packet input bridge that carries enemy/viewer-context metadata without sending packets.
