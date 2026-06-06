# Phase 6 Session 2743 Completion

## Unit of Work

[Phase 6][UOW-2743] Complete custom legion emblem upload

## Runtime Progress Gate

- Deferred/live behavior advanced: `CM_LEGION_UPLOAD_INFO` and `CM_LEGION_UPLOAD_EMBLEM` now leave deferred state for completed custom legion emblem uploads.
- Java source of truth: `CM_LEGION_UPLOAD_INFO.runImpl`, `CM_LEGION_UPLOAD_EMBLEM.runImpl`, `LegionService.uploadEmblemInfo`, `LegionService.uploadEmblemData`, `LegionService.canUploadEmblem`, `LegionEmblem`, `LegionDAO.storeLegionEmblem`, and the upload success/failure/corrupt `SM_SYSTEM_MESSAGE` helpers.
- C# runtime artifact wired/fixed: `GameServerConnection`, `CmLegionUploadInfo`, `CmLegionUploadEmblem`, `SmSystemMessage`, active player legion emblem fields, and `SaveLegionEmblemMutationAsync`.
- Client-visible/state/persistence effect changed: upload info initializes live connection upload state, upload data appends chunks, exact completion deducts Kinah, updates player emblem fields to custom type, persists custom emblem bytes through the existing database shape, records `EMBLEM_REGISTER`, sends emblem update/data packets, and sends Java-equivalent success/failure/corrupt system messages.
- Why this is not preview-only/test-only/documentation-only: the UOW wires deferred client packet paths into live code and mutates/persists player inventory plus legion emblem runtime state.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_UPLOAD_INFO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_UPLOAD_EMBLEM.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/legion/LegionEmblem.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Runtime Changes

- Wired `CM_LEGION_UPLOAD_INFO` and `CM_LEGION_UPLOAD_EMBLEM` dispatch in `GameServerConnection`.
- Added connection-local pending custom emblem upload state matching Java upload size/chunk accumulation semantics closely enough for the current single-connection runtime.
- Added Java-style upload permission, level, Kinah, duplicate-init, and missing-init guards.
- Completed exact-size custom uploads by deducting Kinah, updating live player emblem fields to custom type `0x80`, persisting custom bytes through `SaveLegionEmblemMutationAsync`, recording `EMBLEM_REGISTER`, and sending `SmLegionUpdateEmblem`, `SmLegionSendEmblem`, custom emblem data chunks, and success.
- Added upload success, failure, and corrupt emblem `SmSystemMessage` helpers with Java message ids.
- Added the `EMBLEM_REGISTER` legion history action constant.

## Validation

Changed surface:
- Live packet dispatch, connection upload state, player inventory/emblem mutation, repository custom emblem persistence, system messages, and legion history recording.

Specific behavior:
- Java upload info initializes a custom emblem upload.
- Java upload data appends chunks.
- Exact completion deducts Kinah, stores custom emblem bytes/type, records history, sends success/failure/corrupt messages, and emits emblem update/data packets.

Focused command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionSendEmblemInfoTests|FullyQualifiedName~SaveLegionEmblemMutationAsync" --logger "console;verbosity=minimal"
```

Result:
- Passed: 17
- Failed: 0
- Skipped: 0
- Existing nullable/analyzer warnings remain outside this UOW.

Java/Maven:
- Not run. Java source was unchanged and this UOW directly ported the reviewed Java runtime behavior into C#.

Broad validation decision:
- Broad .NET validation was skipped because the focused command compiles the test project and covers packet parsing, live upload success, live missing-init failure, packet ordering/content, runtime persistence capture, and the DB-gated custom emblem data path.

## Tests Added

- `ClientPacketFactory_ParsesLegionUploadPacketsLikeJava`
- `HandleInfrastructurePacketAsync_LegionUploadCompletesCustomEmblemLikeJava`
- `HandleInfrastructurePacketAsync_LegionUploadDataWithoutInfoFailsLikeJava`
- `SaveLegionEmblemMutationAsync_PersistsCustomEmblemDataAgainstJavaSchema_WhenEnabled`

## Conservative Parity Status

| Java Runtime Path | C# Runtime Artifact | Status | Evidence | Notes |
| --- | --- | --- | --- | --- |
| `CM_LEGION_UPLOAD_INFO.readImpl/runImpl` | `CmLegionUploadInfo`, `GameServerConnection.HandleLegionUploadInfoAsync` | Partial Parity | Runtime unit tested | Initializes connection-local pending upload state. |
| `CM_LEGION_UPLOAD_EMBLEM.readImpl/runImpl` | `CmLegionUploadEmblem`, `GameServerConnection.HandleLegionUploadEmblemAsync` | Partial Parity | Runtime unit tested | Completion and missing-init failure paths covered. |
| `LegionService.uploadEmblemInfo/uploadEmblemData/canUploadEmblem` | `GameServerConnection` upload guards and completion flow | Partial Parity | Runtime unit tested | Shared legion aggregate and online-member fanout are not complete. |
| `LegionEmblem` upload state | `PendingLegionEmblemUpload` | Partial Parity | Runtime unit tested | Connection-local rather than shared legion state. |
| `LegionDAO.storeLegionEmblem` | `MySqlPlayerEnterWorldRepository.SaveLegionEmblemMutationAsync` | Partial Parity | DB-gated integration test added | DB test compiles but was not run against live MySQL in this session. |
| Upload `SM_SYSTEM_MESSAGE` helpers | `SmSystemMessage` helpers | Partial Parity | Runtime unit tested | Message ids are asserted for success/failure paths. |

## Known Gaps

- The full Java shared `LegionEmblem` aggregate and all-online-member broadcast/fanout remain incomplete.
- Pending upload state is connection-local; Java stores the state on the shared legion emblem object.
- The corrupt upload branch sends the corrupt-file message and leaves pending state, matching Java's early return before reset; broader client recovery was not verified.
- The custom emblem DB integration test is gated behind `AION_GAMESERVER_DB_INTEGRATION=1` and was not executed against live MySQL in this session.
- No real client renderer validation was performed.
