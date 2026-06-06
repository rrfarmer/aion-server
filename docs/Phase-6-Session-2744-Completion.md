# Phase 6 Session 2744 Completion

## Unit of Work

[Phase 6][UOW-2744] Send live legion info refresh

## Runtime Progress Gate

- Deferred/live behavior advanced: `CM_LEGION` subopcode `0x08` now sends a live legion info server packet for active legion members instead of remaining parser-only.
- Java source of truth: `CM_LEGION.readImpl`, `CM_LEGION.runImpl` case `0x08`, and `SM_LEGION_INFO.writeImpl`.
- C# runtime artifact wired/fixed: `CmLegion`, `GameServerConnection.HandleLegionAsync`, and new `SmLegionInfo`.
- Client-visible/runtime effect changed: a client refresh request for the active player's loaded legion now receives `SM_LEGION_INFO` fields from live C# runtime state.
- Why this is not preview-only/test-only/documentation-only: the UOW wires a deferred client/server packet path and sends a real server packet from live connection code.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_INFO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`

## C# Runtime Changes

- Added `SmLegionInfo` with Java opcode `110` and Java-equivalent payload order.
- Wired `CmLegion` dispatch in `GameServerConnection`.
- Added live handling for exOpcode `0x08` only: active legion members receive `SmLegionInfo.FromPlayer(player)`.
- Left other `CM_LEGION` mutation/service subactions deferred until their runtime services and shared legion state exist.

## Validation Decision

- Changed surface: live connection dispatch plus one server packet serializer.
- Specific behavior/contract: Java `CM_LEGION` `0x08` consumes the empty `D/H` fields, checks active legion membership, and sends `SM_LEGION_INFO` with name, level, ranking, permissions, contribution, disband, dominion, and announcement fields.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests" --logger "console;verbosity=minimal"
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit test exists for this packet send path in the repo.
- Broad-validation trigger: live connection dispatch changed.
- Broad .NET decision: skipped after focused validation because the edited branch is isolated to `CmLegion` exOpcode `0x08`, the focused command compiles the game-server project, verifies packet parsing/serialization, and exercises the live connection send/no-op behavior. No packet primitive or shared serializer was changed.
- Why this scope is sufficient: the narrow tests directly exercise the changed live path and byte payload contract; remaining risk is missing runtime data fields, not untested shared infrastructure.

## Validation Result

- Passed: 7
- Failed: 0
- Skipped: 0
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ReadFrom_RefreshInfoConsumesJavaEmptyFields` | Unit | `CM_LEGION.readImpl` case `0x08` | C# parser consumes the Java empty `D/H` fields and exposes exOpcode `0x08`. | Java source review plus parser assertion. | Does not execute the client socket parser. |
| `SmLegionInfo_WritesJavaPayloadWithCurrentRuntimeFields` | Unit | `SM_LEGION_INFO.writeImpl` | Packet payload order for name, level, ranking, permissions, contribution, unknowns, disband, dominion, and announcements. | Java source review plus byte-level reader assertions. | Packet uses supplied values; live runtime cannot yet supply every Java field. |
| `HandleInfrastructurePacketAsync_RefreshInfoSendsActivePlayerLegionInfoLikeJava` | Unit | `CM_LEGION.runImpl` case `0x08` | Live connection sends `SmLegionInfo` for an active legion member. | Java source review plus live connection packet observer. | Ranking, contribution, dominion, and announcement are defaulted. |
| `HandleInfrastructurePacketAsync_RefreshInfoSkipsPlayerWithoutLegionLikeJava` | Unit | `CM_LEGION.runImpl` member guard | Non-legion active players receive no packet for refresh info. | Java source review plus no-packet assertion. | Does not cover create-legion exOpcode `0x00`. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcode `0x08` is live; mutation/service subactions remain deferred. Java member guard is mirrored with `LegionId > 0`; C# lacks full `Player.getLegion()` aggregate. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionInfo` | Server Packet | Partial | Unit Tested | Partial Parity | Payload order and opcode are ported. Live runtime supplies name, level, permissions, and disband time; ranking, contribution, dominion, and announcement default until shared legion aggregate/ranking/dominion services are ported. |
| `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes` | `SmLegionInfo.PacketOpCode` | Opcode Metadata | Complete | Unit Tested indirectly | Partial Parity | Opcode `110` was reviewed from Java and used by the packet class; no full server-packet opcode registry golden was run. |

## Summary Metrics

- Total Java artifacts discovered: 3
- Total artifacts ported in this UOW: 2 partial runtime artifacts plus 1 opcode constant
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- C# still lacks Java's shared `Legion` aggregate, so live `SmLegionInfo.FromPlayer` defaults ranking, contribution points, occupied/last/current legion dominion, and announcements.
- Other `CM_LEGION` exOpcodes remain deferred, including create, invite, leave, kick, rank changes, notice changes, permissions, level-up, nickname, and dominion join.
- No real client renderer validation was performed.
- No Java runtime/golden output was generated for `SM_LEGION_INFO`.
