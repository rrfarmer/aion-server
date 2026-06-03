# Phase 6 Session 2452 Completion

## UOW

[Phase 6] UOW-2452: Port Vortex defender kick message metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/services/VortexService.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

## Implementation Notes

- Added `SmSystemMessage.InvasionDefenderKick()` for Java `new SM_SYSTEM_MESSAGE(1401476)` when `Invasion.kickPlayer(player, false)` removes an online defender from a defender alliance.
- Added narrow defender participant state to `VortexInvasionRuntime`.
- Added `AddDefender`, `IsDefenderPlayer`, and `RemoveDefenderPlayer`.
- `RemoveDefenderPlayer` removes active defender state, clears any matching passed-player state, and returns defender kick message metadata for online defenders.
- No defender teleport behavior was added because Java only teleports invaders in `Invasion.kickPlayer`.
- Known parity limitation: Java sends `1401476` only inside the defender alliance membership branch. C# does not yet model Vortex defender alliance membership, so active defender state is the current metadata proxy until defender alliance lifecycle is ported.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `RemoveDefenderPlayer_ForOnlineDefender_SendsDefenderKickWithoutTeleportLikeJava` | Unit | `Invasion.kickPlayer(player, false)` | Online defender removal produces message `1401476`, removes defender state, and does not teleport | Java source-reviewed branch covered by C# runtime test | Uses active defender state instead of real defender alliance membership |
| `RemoveDefenderPlayer_ForOfflineDefender_RemovesDefenderWithoutMessagesLikeJavaOnlineGate` | Unit | `Invasion.kickPlayer` online guard | Offline defender removal remains silent | C# runtime test covers Java online gate | Alliance removal/disband remains unported |
| `SmSystemMessage_WritesDialogTooFarMessages` | Unit | `SM_SYSTEM_MESSAGE` raw ID used by `Invasion.kickPlayer` | Packet factory ID includes `1401476` | C# packet serialization helper validates exact message ID | No Java-generated packet golden for this ID |

## Validation Decision

- Changed surface: Vortex defender runtime metadata and packet factory addition.
- Specific behavior/contract: Java `Invasion.kickPlayer(player, false)` can send defender kick message `1401476` to online defenders and does not run the invader home-teleport branch.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~VortexInvaderRemovalPacketDispatchServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

- Result: Passed, 289 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and the Java behavior was directly reviewed in `Invasion.kickPlayer`.
- Broad-validation trigger: none. This UOW added metadata and tests only; no live defender alliance mutation, socket dispatch wiring, scheduler wiring, persistence, packet primitives, or connection dispatch changed.
- Broad .NET decision: skipped; focused validation built the affected project and covered Vortex runtime, Vortex packet dispatch adjacency, and packet serialization.
- Why this scope is sufficient: the change is isolated to Vortex runtime metadata and system-message factory coverage.

## Additional Hygiene

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings for touched C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.VortexService.removeDefenderPlayer` | `Aion.GameServer.Services.VortexInvasionRuntime.RemoveDefenderPlayer` | Service/runtime mutation | Partial | Unit Tested | Partial Parity | Defender lookup/removal is represented in active runtime state. Java active-invasion map traversal is approximated by ordered C# active state traversal. Live zone/alliance callers are not wired. |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexInvasionRuntime.RemoveDefenderPlayer` | Vortex participant mutation | Partial | Unit Tested | Partial Parity | Defender removal message `1401476` and no-teleport behavior are covered. Java defender alliance membership removal/disband and `syncPassed(true)` fanout remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet | Partial | Unit Tested | Partial Parity | Added Vortex defender kick message factory `1401476`; packet primitive shape was unchanged. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Defender alliance membership, prompt acceptance, alliance removal/disband, and live defender zone handler wiring remain incomplete.
- C# active defender state is a proxy for the Java defender alliance branch until the full defender lifecycle is ported.
- `VortexInvaderRemovalPacketDispatchService` does not yet dispatch defender removal packets; it intentionally remains invader-removal specific.
- Vortex controller `syncPassed(true)` packet fanout and full Vortex start/stop spawn lifecycle remain incomplete.
