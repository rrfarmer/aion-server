# Phase 6 Session 2740 Completion

## UOW

[Phase 6] UOW-2740: Send active legion emblem info.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: `CM_LEGION_SEND_EMBLEM_INFO` now leaves the deferred/parser-only state and sends a real `SM_LEGION_SEND_EMBLEM` packet for the active player's hydrated legion.
- Java source/runtime path: `CM_LEGION_SEND_EMBLEM_INFO.runImpl` -> `LegionService.getLegion(legionId)` -> `new SM_LEGION_SEND_EMBLEM(legionId, legion.getLegionEmblem(), 0, legion.getName())`.
- C# runtime artifact wired: `GameServerConnection.HandleInfrastructurePacketAsync`, new `HandleLegionSendEmblemInfoAsync`, and new `SmLegionSendEmblem`.
- Client-visible/state/persistence effect: a client requesting its loaded legion's emblem metadata receives the Java-shaped emblem info packet with legion id, emblem id/type/colors, zero data size, name, and final flag.
- Why this is runtime progress: it wires a deferred live client/server packet path and sends a real server packet from live connection code.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_SEND_EMBLEM_INFO.java`
  - Reads `legionId`, guards missing active player, fetches the legion, and sends `SM_LEGION_SEND_EMBLEM` with `emblemDataSize` `0`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_SEND_EMBLEM.java`
  - Writes legion id, emblem id/type, data size, ARGB color bytes, legion name, and trailing `0x01`.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_SEND_EMBLEM.java`
  - Reviewed but not wired; full custom emblem image dispatch requires custom emblem data and chunk packets.
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `sendEmblemData` sends `SM_LEGION_SEND_EMBLEM` plus optional `SM_LEGION_SEND_EMBLEM_DATA` chunks for custom emblems.

## C# Changes

- Added `SmLegionSendEmblem` with Java field order and opcode `213`.
- Replaced the deferred `CmLegionSendEmblemInfo` switch branch with live dispatch.
- Added `HandleLegionSendEmblemInfoAsync`, which sends emblem metadata for the active player's already hydrated legion when the requested id matches.
- Kept other-legion lookup and custom emblem data chunking explicitly blocked on a real legion registry/repository and custom emblem data model.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ClientPacketFactory_ParsesLegionSendEmblemInfoAsInGameOnly` | Unit | `AionClientPacketFactory` opcode `16` | C# parses the live request packet only in-game. | Packet opcode/state contract matches Java registration. | Parser only. |
| `SmLegionSendEmblem_WritesJavaEmblemInfoPayload` | Unit | `SM_LEGION_SEND_EMBLEM.writeImpl` | Server packet writes Java field order and trailing flag. | Byte-level payload assertions. | Does not cover custom data chunks. |
| `HandleInfrastructurePacketAsync_LegionSendEmblemInfoSendsActivePlayerLegionEmblemLikeJava` | Runtime unit | `CM_LEGION_SEND_EMBLEM_INFO.runImpl` | Live connection dispatch sends `SmLegionSendEmblem` for active loaded legion data. | Packet is emitted through `GameServerConnection` and payload is re-read. | Active player's loaded legion only. |
| `HandleInfrastructurePacketAsync_LegionSendEmblemInfoSkipsUnknownLegionUntilRegistryExists` | Runtime unit | `LegionService.getLegion(legionId)` null guard | C# does not fabricate unrelated legion data without a registry. | No-send guard prevents false parity. | Other-legion lookup remains blocked. |

## Validation Decision

```text
- Changed surface: live infrastructure packet dispatch and a new server packet.
- Specific behavior/contract: Java `CM_LEGION_SEND_EMBLEM_INFO` sends `SM_LEGION_SEND_EMBLEM` metadata with data size 0.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionSendEmblemInfoTests" --logger "console;verbosity=minimal"
- Result: passed; 4 tests passed. Existing nullable/analyzer warnings were emitted outside this UOW.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and this UOW ports a direct packet contract with C# byte/runtime tests.
- Broad-validation trigger: live packet dispatch changed, but it is limited to one previously deferred client packet and a new isolated server packet.
- Broad .NET decision: skipped; focused tests compile the project and exercise the edited branch plus byte contract.
- Why this scope is sufficient: the UOW has no persistence/model side effects and the tests prove parser registration, byte layout, live send, and no-send guard.
```

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_LEGION_SEND_EMBLEM_INFO.runImpl` | `GameServerConnection.HandleLegionSendEmblemInfoAsync` | Live packet handler | Partial | Runtime Unit Tested | Partial Parity | Sends active player's hydrated legion metadata. Other legion lookup needs a runtime legion registry/repository. |
| `SM_LEGION_SEND_EMBLEM.writeImpl` | `SmLegionSendEmblem` | Server packet | Partial | Byte Tested | Partial Parity | Java field order for metadata is covered. Custom emblem data size/chunk flow is not modeled here. |

## Known Gaps

- C# still lacks a runtime `LegionService.getLegion(legionId)` equivalent for arbitrary legion id lookups.
- `CM_LEGION_SEND_EMBLEM` remains deferred because C# does not currently carry custom emblem binary data or `SM_LEGION_SEND_EMBLEM_DATA` chunks.
- `CM_LEGION_MODIFY_EMBLEM`, upload info, and upload data remain deferred.
- No Java/Maven test was run; Java source was unchanged and used as reference.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 2
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Wire `CM_LEGION_SEND_EMBLEM` only after a C# runtime source for custom emblem bytes exists; the UOW should send `SM_LEGION_SEND_EMBLEM` plus `SM_LEGION_SEND_EMBLEM_DATA` chunks like Java.
2. Discover a real C# legion registry/repository path for arbitrary `LegionService.getLegion(legionId)` lookups, then broaden emblem-info requests beyond the active player's loaded legion.
3. Continue inspecting deferred legion packet paths for a narrow runtime side effect, such as emblem modify/upload persistence, only if the existing database shape and loaded runtime state can support it.
