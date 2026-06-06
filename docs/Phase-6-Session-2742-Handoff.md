# Phase 6 Session 2742 Handoff

## Completed UOW

[Phase 6] UOW-2742: Persist default legion emblem changes.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: `CM_LEGION_MODIFY_EMBLEM` now leaves the deferred/parser-only state for active-player default emblem changes.
- Java source/runtime path: `CM_LEGION_MODIFY_EMBLEM.runImpl` -> `LegionService.storeLegionEmblem` -> `legionRestrictions.canStoreLegionEmblem` -> `Inventory.decreaseKinah` -> `LegionDAO.storeLegionEmblem` -> `SM_LEGION_UPDATE_EMBLEM` / `SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_EMBLEM`.
- C# runtime artifact wired: `CmLegionModifyEmblem`, `GameServerConnection.HandleLegionModifyEmblemAsync`, `IPlayerEnterWorldRepository.SaveLegionEmblemMutationAsync`, `MySqlPlayerEnterWorldRepository.SaveLegionEmblemMutationAsync`, `PlayerEnterWorldService.SaveLegionEmblemMutationAsync`, `SmLegionUpdateEmblem`, and legion emblem config binding.
- Client-visible/state/persistence effect: a valid brigade-general request for the active loaded legion mutates player legion emblem fields, decreases cube Kinah, persists `legion_emblems` plus the Kinah row, records legion history when a repository is available, and sends real update/success packets.
- Why this is runtime progress: it wires a deferred live client packet path, mutates live player/legion/inventory state, sends real server packets, and persists runtime state through the existing database shape.
```

## Commit

`[Phase 6][UOW-2742] Persist default legion emblem changes`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/Legion/LegionHistory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmLegionModifyEmblem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionUpdateEmblem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionSendEmblemInfoTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2742-Completion.md`
- `docs/Phase-6-Session-2742-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_MODIFY_EMBLEM.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_UPDATE_EMBLEM.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/configs/main/LegionConfig.java`

## C# Artifacts Touched

- `Aion.GameServer.Configuration.GameServerLegionOptions`
- `Aion.GameServer.Data.IPlayerEnterWorldRepository`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository`
- `Aion.GameServer.Model.Legion.LegionHistoryActions`
- `Aion.GameServer.Network.Aion.ClientPackets.CmLegionModifyEmblem`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Network.Aion.ServerPackets.SmLegionUpdateEmblem`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Services.PlayerEnterWorldService`
- `Aion.GameServer.Tests.CmLegionSendEmblemInfoTests`
- `Aion.GameServer.Tests.PlayerEnterWorldRepositoryDatabaseIntegrationTests`
- `Aion.GameServer.Tests.PlayerEnterWorldServiceTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionSendEmblemInfoTests|FullyQualifiedName~LoadLegionEmblemAsync_HydratesCustomEmblemAgainstJavaSchema_WhenEnabled|FullyQualifiedName~SaveLegionEmblemMutationAsync_PersistsDefaultEmblemAndKinahAgainstJavaSchema_WhenEnabled" --logger "console;verbosity=minimal"
```

Result:

- Passed: 14
- Failed: 0
- Skipped: 0
- Existing nullable/analyzer warnings were emitted outside this UOW.

Java/Maven:

- Not run. Java source was reviewed unchanged; this UOW ports a direct packet/mutation/persistence path with focused C# runtime and byte-level tests.

Broad .NET:

- Not run. The edited live surface is one previously deferred packet branch, one isolated server packet, config binding, and one repository mutation; focused tests compile the affected project and exercise the edited behavior.

## Conservative Parity Status

- `CM_LEGION_MODIFY_EMBLEM` now has partial runtime parity for default emblem changes on the active loaded legion.
- C# persists the default emblem row and Kinah deduction through the existing Java schema.
- `SM_LEGION_UPDATE_EMBLEM` has byte-level payload coverage.
- Broadcast-to-all-online-legion-members and custom emblem upload remain incomplete.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_LEGION_MODIFY_EMBLEM.readImpl/runImpl` | `CmLegionModifyEmblem` / `GameServerConnection.HandleLegionModifyEmblemAsync` | Live packet handler | Partial | Runtime Unit Tested | Partial Parity | Active-player default emblem changes are wired. Custom upload flow and broadcast-to-all-online-members remain incomplete. |
| `LegionService.storeLegionEmblem` | `GameServerConnection.HandleLegionModifyEmblemAsync` / `PlayerEnterWorldService.SaveLegionEmblemMutationAsync` | Service behavior | Partial | Runtime Unit Tested | Partial Parity | Rank, level, Kinah, mutation, persistence, self update, success message, and history are covered. Full `Legion` aggregate broadcast is not ported. |
| `LegionDAO.storeLegionEmblem` | `MySqlPlayerEnterWorldRepository.SaveLegionEmblemMutationAsync` | Persistence write | Partial | DB-Gated Integration Tested | Partial Parity | Uses Java schema and upsert behavior. Does not model Java `PersistentState` object lifecycle. |
| `SM_LEGION_UPDATE_EMBLEM.writeImpl` | `SmLegionUpdateEmblem` | Server packet | Partial | Byte Tested | Partial Parity | Field order/opcode covered. Broadcast routing beyond current connection is pending. |
| `LegionConfig.LEGION_EMBLEM_REQUIRED_KINAH` | `GameServerLegionOptions.EmblemRequiredKinah` | Config | Partial | Runtime Unit Tested via option input | Partial Parity | Key/default are bound. Broader config tests were not expanded in this UOW. |

## Known Gaps / Watchouts

- Java broadcasts `SM_LEGION_UPDATE_EMBLEM` to every online legion member. C# sends it to the requester only until a live online-legion-member lookup is available.
- `CM_LEGION_UPLOAD_INFO` and `CM_LEGION_UPLOAD_EMBLEM` remain deferred.
- Custom emblem data upload, corrupt-file handling, and upload-state reset remain unported.
- C# does not model Java `LegionEmblem.PersistentState`; repository upsert is the narrow persistence behavior used here.
- The DB integration test is gated and was not run against a live MySQL instance in this session.
- Earlier discovery confirmed Java `ItemSplitService.moveKinah` only handles cube/account warehouse kinah, so do not widen C# split-kinah to legion warehouse as a parity change.

## Next Recommended Runtime UOW

Recommended candidate: inspect `CM_LEGION_UPLOAD_INFO` and `CM_LEGION_UPLOAD_EMBLEM`, then wire the smallest safe custom emblem upload path if the active player's upload buffer can be represented in live C# state and completed uploads can persist through `SaveLegionEmblemMutationAsync`.

Runtime progress gate for that candidate must be confirmed from fresh discovery. Likely shape:

```text
- Deferred/live behavior to advance: custom legion emblem upload packets should initialize and append upload bytes, then persist completed custom emblem data instead of remaining deferred.
- Java source/runtime path: Java `CM_LEGION_UPLOAD_INFO.runImpl`, `CM_LEGION_UPLOAD_EMBLEM.runImpl`, `LegionService.uploadEmblemInfo`, `LegionService.uploadEmblemData`, `LegionDAO.storeLegionEmblem`, and upload success/failure system messages.
- C# runtime artifact likely involved: `CmLegionUploadInfo`, `CmLegionUploadEmblem`, active connection/player upload state, `GameServerConnection`, `SaveLegionEmblemMutationAsync`, and `SmSystemMessage` upload messages.
- Client-visible/state/persistence effect expected: completed custom upload decreases Kinah, persists custom emblem bytes, sends success/failure packets, and later `CM_LEGION_SEND_EMBLEM` returns the uploaded bytes.
- Why this is runtime progress: it wires deferred live client packet paths, mutates player/legion/inventory state, persists custom emblem data, and changes later packet output.
```

Suggested focused validation after discovery and implementation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionSendEmblemInfoTests|FullyQualifiedName~SaveLegionEmblemMutationAsync" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless Java source or fixtures change. Broad-validation trigger: live packet dispatch, upload-state mutation, player currency mutation, or repository persistence changes; start with focused runtime tests.

## Other Safe Runtime Candidates

- Broaden legion emblem update fanout only after discovering an existing live connection/world lookup for online legion members.
- Inspect `CM_LEGION` sub-opcodes for a narrow live packet send that can be backed by already loaded player/legion fields.
- Continue from legion warehouse runtime paths only when discovery finds a Java-backed packet/state/persistence mismatch; avoid split-kinah widening because Java does not support it for legion warehouse.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `b844e9e9f [Phase 6][UOW-2741] Send requested legion emblem data`
  - `dde54997f [Phase 6][UOW-2740] Send active legion emblem info`
  - `8d84e61fd [Phase 6][UOW-2739] Track legion warehouse deleted rows`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
