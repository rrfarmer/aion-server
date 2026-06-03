# Phase 6 Session 2450 Completion

## UOW

[Phase 6] UOW-2450: Port online Vortex invader kick packet and teleport side effects

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

## Implementation Notes

- Added `SmSystemMessage.InvasionInvaderKick()` for Java `new SM_SYSTEM_MESSAGE(1401452)`.
- Added `SmSystemMessage.InvasionDirectPortalOutCompulsion()` for Java `STR_MSG_INVADE_DIRECT_PORTAL_OUT_COMPULSION()` message `1401474`.
- Added `PlayerTeleportService.TeleportToWorldPosition` as the C# immediate position/movement reset equivalent for the Java `TeleportService.teleportTo(Player, WorldPosition)` slice used by Vortex kick.
- Extended `VortexInvasionRuntime.RemoveInvaderPlayer` so an online removed invader returns the Java kick message, and an online removed invader still in the invasion world also returns the portal-out message and teleports to the Vortex home point.
- Scope remains partial: C# returns packet intents in the removal result, but live socket dispatch and alliance removal/disband behavior from Java `Invasion.kickPlayer` are still not fully wired.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `RemoveInvaderPlayer_ForOnlineInvaderInInvasionWorld_SendsKickAndPortalOutThenTeleportsHomeLikeJava` | Unit | `Invasion.kickPlayer`; `TeleportService.teleportTo(Player, WorldPosition)` | Online invader in invasion world gets messages `1401452`, `1401474`, and is moved to the Vortex home point | Java source-reviewed branch covered by C# runtime test | Does not send over a live client connection |
| `RemoveInvaderPlayer_ForOnlineInvaderOutsideInvasionWorld_SendsKickWithoutPortalOutTeleportLikeJava` | Unit | `Invasion.kickPlayer` | Online invader outside invasion world gets only message `1401452` and is not teleported | Java source-reviewed conditional covered by C# runtime test | Does not cover defender message `1401476` |
| `RemoveInvaderPlayer_RemovesActiveInvaderAndPassedPortalStateLikeJava` | Unit | `Invasion.kickPlayer` | Offline invader removal remains silent and clears invader/passed-player state | Existing test extended to assert no messages/teleport | Alliance removal still unported |
| `SmSystemMessage_WritesDialogTooFarMessages` | Unit | `SM_SYSTEM_MESSAGE` constants and `Invasion.kickPlayer` raw IDs | Packet factory IDs include `1401452` and `1401474` | C# packet serialization helper validates exact message IDs | No Java-generated packet golden for these two IDs |

## Validation Decision

- Changed surface: Vortex runtime removal mutation, packet factory additions, and a focused teleport helper.
- Specific behavior/contract: Java `Invasion.kickPlayer` sends invader kick message `1401452`, conditionally sends portal-out message `1401474`, and teleports online invaders home when their world is the invasion world.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~Vortex|FullyQualifiedName~GamePacketTests" --no-restore
```

- Result: Passed, 292 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or Java fixtures changed, and the Java behavior was directly reviewed in `Invasion.kickPlayer`.
- Broad-validation trigger: present because live Vortex runtime removal now mutates player position through `PlayerTeleportService`.
- Broad .NET decision: full project/solution validation was skipped after focused validation passed because the mutation is isolated to Vortex invader removal and the focused command built the affected project while covering Vortex runtime behavior plus packet serialization.
- Why this scope is sufficient: no packet primitives, persistence mapping, database schema, crypto, scheduler, or connection dispatch code changed. Live socket send remains explicitly unclaimed.

## Additional Hygiene

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings for touched C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexInvasionRuntime.RemoveInvaderPlayer` | Vortex participant mutation | Partial | Unit Tested | Partial Parity | Invader removal, passed-player cleanup, online invader message `1401452`, portal-out message `1401474`, and home teleport are covered. Defender message `1401476`, alliance removal/disband, sync packet fanout, and live socket dispatch remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet | Partial | Unit Tested | Partial Parity | Added factories for Vortex invader kick `1401452` and direct portal-out compulsion `1401474`; packet primitive shape was unchanged. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo(Player, WorldPosition)` | `Aion.GameServer.Services.PlayerTeleportService.TeleportToWorldPosition` | Service helper | Partial | Unit Tested | Partial Parity | Immediate position/movement reset is represented for the Vortex branch. Java cross-world live spawn/channel/player-info packet fanout remains outside this helper. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- `VortexInvasionRuntime` now produces packet intents but does not send them over a live client connection.
- Java alliance removal/disband behavior inside `Invasion.kickPlayer` remains only represented by adjacent group/alliance timeout slices, not by Vortex runtime kick itself.
- Defender kick message `1401476` and defender prompt/alliance update behavior remain unported.
- Full Vortex start/stop spawn lifecycle and live zone handler wiring remain incomplete.
