# Phase 6 Session 2741 Completion

## UOW

[Phase 6] UOW-2741: Send requested legion emblem data.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: `CM_LEGION_SEND_EMBLEM_INFO` now resolves requested legion emblem metadata from the repository, and `CM_LEGION_SEND_EMBLEM` now leaves the deferred/parser-only state to send metadata plus custom emblem data chunks.
- Java source/runtime path: `CM_LEGION_SEND_EMBLEM_INFO.runImpl`, `CM_LEGION_SEND_EMBLEM.runImpl`, `LegionService.getLegion(legionId)`, `LegionService.sendEmblemData`, `LegionDAO.loadLegion`, `LegionDAO.loadLegionEmblem`, and `SM_LEGION_SEND_EMBLEM_DATA.writeImpl`.
- C# runtime artifact wired: `GameServerConnection` legion emblem handlers, `IPlayerEnterWorldRepository.LoadLegionEmblemAsync`, `MySqlPlayerEnterWorldRepository.LoadLegionEmblemAsync`, `LegionEmblemSnapshot`, and `SmLegionSendEmblemData`.
- Client-visible/state/persistence effect: clients requesting an available legion emblem receive the Java-shaped emblem metadata packet, and custom emblem bytes are restored from the existing `legion_emblems.emblem_data` column and sent as `SM_LEGION_SEND_EMBLEM_DATA` chunks.
- Why this is runtime progress: it wires a deferred live client/server packet path, sends real server packets from live connection code, and restores runtime state through the existing database shape.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_SEND_EMBLEM_INFO.java`
  - Reads `legionId`, gets the legion through `LegionService`, and sends `SM_LEGION_SEND_EMBLEM` with `emblemDataSize` `0`.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_SEND_EMBLEM.java`
  - Reads `legionId`, gets the legion through `LegionService`, and calls `LegionService.sendEmblemData`.
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `getLegion` loads a legion from DAO when missing from cache; `loadLegionInfo` hydrates emblem data; `sendEmblemData` sends metadata and `7993` byte chunks for custom emblem bytes.
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
  - `loadLegion` reads `legions`; `loadLegionEmblem` reads `legion_emblems` and returns default emblem values when no emblem row exists.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_SEND_EMBLEM_DATA.java`
  - Writes the chunk size as `D` followed by raw chunk bytes.

## C# Changes

- Added `LegionEmblemSnapshot` as the narrow runtime DTO for requested emblem metadata and custom emblem bytes.
- Added `IPlayerEnterWorldRepository.LoadLegionEmblemAsync` and implemented MySQL hydration from `legions` plus `legion_emblems`.
- Added `SmLegionSendEmblemData` with Java chunk payload shape and opcode `214`.
- Wired `CmLegionSendEmblem` to live connection handling instead of deferral.
- Broadened `CmLegionSendEmblemInfo` from active-player-only data to repository-backed requested legion data, with active-player fallback when no repository exists.
- Added Java-sized custom emblem chunking at `7993` bytes.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ClientPacketFactory_ParsesLegionSendEmblemAsInGameOnly` | Unit | Client packet registration for `CM_LEGION_SEND_EMBLEM` | C# parses the live request packet only in-game. | Packet opcode/state contract is covered. | Parser only. |
| `SmLegionSendEmblemData_WritesJavaChunkPayload` | Unit | `SM_LEGION_SEND_EMBLEM_DATA.writeImpl` | Server packet writes chunk size followed by raw bytes. | Byte-level payload assertions. | Single chunk shape only. |
| `HandleInfrastructurePacketAsync_LegionSendEmblemInfoUsesRepositoryForOtherLegionLikeJava` | Runtime unit | `CM_LEGION_SEND_EMBLEM_INFO.runImpl` -> `LegionService.getLegion` | Live connection asks the repository for a requested legion and emits emblem metadata. | Packet is emitted through `GameServerConnection` and payload is re-read. | Repository is narrowed to emblem facts, not full legion aggregate parity. |
| `HandleInfrastructurePacketAsync_LegionSendEmblemSendsCustomDataChunksLikeJava` | Runtime unit | `CM_LEGION_SEND_EMBLEM.runImpl` -> `LegionService.sendEmblemData` | Live connection emits metadata and multiple custom data chunks. | Packet sequence and chunk payload bytes are asserted. | Does not validate a real client renderer. |
| `LoadLegionEmblemAsync_HydratesCustomEmblemAgainstJavaSchema_WhenEnabled` | DB-gated integration | `LegionDAO.loadLegion` and `LegionDAO.loadLegionEmblem` | Repository maps Java schema rows into runtime emblem snapshot data. | Existing DB shape and custom bytes are exercised when DB integration is enabled. | Skipped unless `AION_GAMESERVER_DB_INTEGRATION=1`. |

## Validation Decision

```text
- Changed surface: live packet dispatch, new server packet, and repository lookup.
- Specific behavior/contract: Java `CM_LEGION_SEND_EMBLEM_INFO` resolves legion data and sends metadata; Java `CM_LEGION_SEND_EMBLEM` sends metadata plus `7993` byte data chunks; Java `LegionDAO.loadLegionEmblem` maps stored custom emblem bytes.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionSendEmblemInfoTests|FullyQualifiedName~LoadLegionEmblemAsync_HydratesCustomEmblemAgainstJavaSchema_WhenEnabled" --logger "console;verbosity=minimal"
- Result: passed; 9 tests passed. Existing nullable/analyzer warnings were emitted outside this UOW.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and this UOW ports direct packet and repository mapping contracts with focused C# runtime and byte-level tests.
- Broad-validation trigger: live packet dispatch and repository lookup changed.
- Broad .NET decision: skipped; focused tests compile the affected project and exercise the edited dispatch, packet byte shapes, chunking, fallback, and DB-gated mapping.
- Why this scope is sufficient: the UOW touches one legion emblem lookup path and one previously deferred packet branch; tests cover the live packet sequence and repository contract without requiring unrelated subsystem breadth.
```

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `LegionService.getLegion(legionId)` | `GameServerConnection.ResolveLegionEmblemSnapshotAsync` / `MySqlPlayerEnterWorldRepository.LoadLegionEmblemAsync` | Runtime lookup | Partial | Runtime Unit Tested | Partial Parity | Resolves emblem/name facts for packet paths, not a cached full legion aggregate. |
| `LegionDAO.loadLegionEmblem` | `MySqlPlayerEnterWorldRepository.LoadLegionEmblemAsync` | Persistence read | Partial | DB-Gated Integration Tested | Partial Parity | Maps emblem fields and custom bytes; default no-row behavior is represented by zero/default values in live fallback paths. |
| `CM_LEGION_SEND_EMBLEM_INFO.runImpl` | `GameServerConnection.HandleLegionSendEmblemInfoAsync` | Live packet handler | Partial | Runtime Unit Tested | Partial Parity | Sends repository-backed metadata for requested legion ids. |
| `CM_LEGION_SEND_EMBLEM.runImpl` / `LegionService.sendEmblemData` | `GameServerConnection.HandleLegionSendEmblemAsync` / `SendLegionEmblemDataAsync` | Live packet handler | Partial | Runtime Unit Tested | Partial Parity | Sends metadata plus Java-sized custom data chunks when bytes exist. |
| `SM_LEGION_SEND_EMBLEM_DATA.writeImpl` | `SmLegionSendEmblemData` | Server packet | Partial | Byte Tested | Partial Parity | Payload field order and raw byte body are covered. |

## Known Gaps

- C# still lacks a full `LegionService` cached aggregate equivalent with complete legion members, warehouse, announcements, and legion lifecycle behavior.
- `CM_LEGION_MODIFY_EMBLEM`, `CM_LEGION_UPLOAD_INFO`, and `CM_LEGION_UPLOAD_EMBLEM` remain deferred.
- Repository disband handling returns no emblem for expired disband records, but does not perform Java's disband cleanup or member updates.
- The DB integration test is gated and was not run against a live MySQL instance in this session.
- No Java/Maven test was run; Java source was unchanged and used as reference.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 7
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 4
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Inspect `CM_LEGION_MODIFY_EMBLEM` against Java `LegionService.storeLegionEmblem`; implement only if active legion membership, rank/permission checks, kinah cost, and `legion_emblems` persistence can be honored in live code.
2. Inspect `CM_LEGION_UPLOAD_INFO` / `CM_LEGION_UPLOAD_EMBLEM` for the custom emblem upload buffer and persistence flow, now that requested custom emblem data chunks can be sent.
3. Continue broadening runtime legion lookup only through a concrete live behavior, such as a packet that needs full `LegionService.getLegion` semantics, rather than adding a standalone registry scaffold.
