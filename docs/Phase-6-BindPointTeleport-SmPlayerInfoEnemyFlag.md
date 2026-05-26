# Phase 6 Bind-Point Teleport SM_PLAYER_INFO Enemy Flag

Date: May 26, 2026
Unit of Work: UOW-1265
Scope: Add focused C# packet support for Java `SM_PLAYER_INFO(Player, boolean enemy)` creature-type flag.
Source of truth: Java project.

## Summary

UOW-1265 adds a focused `SmPlayerInfo` constructor path for Java's `enemy` flag and a packet test proving that the C# serializer writes creature type `0x00` when the flag is true.

This is not full `SM_PLAYER_INFO` parity. Java also derives race from the active viewer, including enemy/opposite-race and neutral-to-all-player custom states. C# still serializes race from the visible player itself because the packet does not yet receive viewer context.

## Java Source Findings

- Java `SM_PLAYER_INFO(Player)` delegates to `SM_PLAYER_INFO(Player, false)`.
- Java `SM_PLAYER_INFO(Player, boolean enemy)` stores the flag.
- `writeImpl` writes `enemy ? 0x00 : 0x26` for the creature-type byte after transform type.
- `PlayerController.sendPlayerInfoPackets(Player)` supplies `!player.equals(getOwner()) && getOwner().isAggroIconTo(player)`.
- Java race output is viewer-sensitive: `activePlayer.isEnemy(player)` uses the viewer's opposite race unless either player is neutral-to-all.

## C# Implementation

Updated `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPlayerInfo.cs`:

- Kept the existing `SmPlayerInfo(Player, PlayerExperienceTable?)` constructor as friendly/default.
- Added `SmPlayerInfo(Player, bool enemy, PlayerExperienceTable? experienceTable = null)`.
- Writes `0x00` for the creature-type byte when `enemy=true`; otherwise writes existing `0x26`.

Updated descriptor note in `PlayerKnownListPlayerSideEffectPlanService` to reflect that the enemy/aggro creature-type flag now exists, while viewer-sensitive race remains unverified.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "SmPlayerInfo" --nologo` passed 2 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo" --nologo` passed 264 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1265

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmPlayerInfo` | Packet / Serialization | Partial | Unit Tested | Partial Parity | C# now models Java's `enemy ? 0x00 : 0x26` creature-type byte and preserves the friendly default. Viewer-sensitive race projection, custom neutral state, active-player context, and Java runtime byte comparison remain missing. |
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `PlayerKnownListPlayerSideEffectPlanService` plus `SmPlayerInfo(enemy)` constructor | Controller Packet Intent / Packet Dependency | Partial | Unit Tested | Partial Parity | Side-effect descriptors can now point to a C# packet constructor capable of carrying the enemy flag. The known-list planner still does not instantiate/send live packets or compute `getOwner().isAggroIconTo(player)`. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.isEnemy` / custom neutral race handling | current `SmPlayerInfo` race serialization | Viewer-Sensitive Packet Dependency | Not Started | No Tests | Needs Verification | Discovered dependency. C# still serializes visible-player race directly and does not receive active viewer context, opposite-race projection, or neutral-to-all custom state. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | player known-list packet descriptor stack with improved `SmPlayerInfo` dependency | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Packet prerequisite improved for future player-see dispatch. Live scheduled callbacks, sockets, movement, cooldown, and dispatch remain disabled. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPlayerInfo_EnemyFlagWritesJavaAttackableCreatureType` | Unit / Packet | `SM_PLAYER_INFO.writeImpl` | `enemy=true` writes creature type `0x00` after transform type. | Source-derived byte assertion. | No Java golden-byte capture; race projection still unverified. |

Existing test retained:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmPlayerInfo_WritesJavaShapedBaseline` | Unit / Packet | `SM_PLAYER_INFO.writeImpl` | Friendly/default constructor writes creature type `0x26` and existing Java-shaped baseline fields. | Source-derived byte assertions. | No active viewer context or Java runtime comparison. |

## Remaining Risks

- `SmPlayerInfo` still lacks active-viewer context.
- Viewer-sensitive race projection and neutral-to-all-player custom state are not ported.
- `PlayerController.sendPlayerInfoPackets` is still descriptor-only; no live packet send occurs.
- `SmPlayerStance` and `SmAbnormalEffect` packet classes remain missing.
- No Java runtime packet-order capture or golden-byte comparison was performed.
- Threading, reflection, date/time, precision/rounding, and broader packet serialization behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 focused packet flag path plus 1 focused packet test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 1 active-viewer packet context, 1 neutral/custom-state race projection, 1 live controller side-effect dispatcher, 1 `SmPlayerStance` packet, 1 `SmAbnormalEffect` packet, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a focused `SmPlayerInfo` viewer-context race projection audit/model, or move to `SmPlayerStance` serializer prerequisite if viewer context is judged too broad for the next safe slice.

## Update After UOW-1266

`SmPlayerInfo` now accepts a scalar `SmPlayerInfoViewerContext` so source-derived packet tests can cover Java's active-viewer race projection and neutral-to-all-player override. The context is still supplied metadata: C# does not yet compute `Player.isEnemy`, custom player-state masks, duel/PvP/FFA logic, or live active-player state from `GameServerConnection`.
