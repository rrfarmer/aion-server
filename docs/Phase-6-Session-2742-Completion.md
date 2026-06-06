# Phase 6 Session 2742 Completion

## UOW

[Phase 6] UOW-2742: Persist default legion emblem changes.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: `CM_LEGION_MODIFY_EMBLEM` now leaves the deferred/parser-only state for active-player default emblem changes.
- Java source/runtime path: `CM_LEGION_MODIFY_EMBLEM.runImpl` -> `LegionService.storeLegionEmblem` -> `legionRestrictions.canStoreLegionEmblem` -> `Inventory.decreaseKinah` -> `LegionDAO.storeLegionEmblem` -> `SM_LEGION_UPDATE_EMBLEM` / `SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_EMBLEM`.
- C# runtime artifact wired: `CmLegionModifyEmblem`, `GameServerConnection.HandleLegionModifyEmblemAsync`, `IPlayerEnterWorldRepository.SaveLegionEmblemMutationAsync`, `MySqlPlayerEnterWorldRepository.SaveLegionEmblemMutationAsync`, `PlayerEnterWorldService.SaveLegionEmblemMutationAsync`, `SmLegionUpdateEmblem`, and legion emblem config binding.
- Client-visible/state/persistence effect: a valid brigade-general request for the active loaded legion mutates player legion emblem fields, decreases cube Kinah, persists `legion_emblems` plus the Kinah row, records legion history when a repository is available, and sends real update/success packets.
- Why this is runtime progress: it wires a deferred live client packet path, mutates live player/legion/inventory state, sends real server packets, and persists runtime state through the existing database shape.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_MODIFY_EMBLEM.java`
  - Reads legion id, emblem id, emblem type, and ARGB channels; runs only when active player belongs to the requested legion.
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `storeLegionEmblem` checks `canStoreLegionEmblem`, adds `EMBLEM_MODIFIED` history, decreases Kinah via `PricesService.getPriceForService`, mutates the legion emblem, broadcasts `SM_LEGION_UPDATE_EMBLEM`, and sends `STR_GUILD_CHANGE_EMBLEM`.
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `canStoreLegionEmblem` checks emblem id range `0..49`, brigade-general rank, legion level `>= 2`, and sufficient Kinah.
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
  - `storeLegionEmblem` inserts or updates `legion_emblems` and skips invalid custom-emblem rows without data.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_UPDATE_EMBLEM.java`
  - Writes legion id, emblem id/type, and ARGB channels.
- `game-server/src/com/aionemu/gameserver/configs/main/LegionConfig.java`
  - `gameserver.legion.emblemrequiredkinah` defaults to `800000`.

## C# Changes

- Completed `CmLegionModifyEmblem` parsing for emblem type and ARGB bytes.
- Added `GameServerLegionOptions.EmblemRequiredKinah` bound from `gameserver.legion.emblemrequiredkinah`.
- Added `SmLegionUpdateEmblem` with opcode `215` and Java field order.
- Added `SmSystemMessage` helpers for Java `STR_GUILD_CHANGE_EMBLEM_DONT_HAVE_RIGHT` and `STR_GUILD_CHANGE_EMBLEM`.
- Wired `CmLegionModifyEmblem` into `GameServerConnection`.
- Added live guard/mutation logic for active legion id, brigade-general rank, legion level, emblem id range, Java price-service Kinah fee, Kinah mutation, emblem mutation, persistence rollback, update packet send, success system message, and history insertion.
- Added repository/service persistence for atomically updating the Kinah row and upserting `legion_emblems`.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ClientPacketFactory_ParsesLegionModifyEmblemLikeJava` | Unit | `CM_LEGION_MODIFY_EMBLEM.readImpl` | Packet parser reads legion id, emblem id/type, and ARGB bytes in Java order. | Source-derived field assertions. | Parser only. |
| `SmLegionUpdateEmblem_WritesJavaPayload` | Unit | `SM_LEGION_UPDATE_EMBLEM.writeImpl` | Server packet writes legion id, emblem id/type, and ARGB bytes. | Byte-level payload assertions. | Packet only. |
| `HandleInfrastructurePacketAsync_LegionModifyEmblemMutatesPersistsAndSendsUpdateLikeJava` | Runtime unit | `CM_LEGION_MODIFY_EMBLEM.runImpl` / `LegionService.storeLegionEmblem` | Live handler mutates emblem fields, decreases Kinah, persists mutation, records history, and sends update/success packets. | Runtime dispatch through `GameServerConnection` plus repository captures and packet payload checks. | Active connection only; no broadcast registry fanout. |
| `HandleInfrastructurePacketAsync_LegionModifyEmblemRejectsNonBrigadeGeneralLikeJava` | Runtime unit | `legionRestrictions.canStoreLegionEmblem` | Non-brigade-general request sends no-right message and has no mutation/persistence side effects. | Java message id and no-side-effect assertions. | Other rejection branches are source-reviewed but not all individually tested. |
| `SaveLegionEmblemMutationAsync_PersistsDefaultEmblemAndKinahAgainstJavaSchema_WhenEnabled` | DB-gated integration | `LegionDAO.storeLegionEmblem` / `Inventory.decreaseKinah` | Repository updates Kinah and writes default emblem row using Java schema columns. | Existing MySQL schema exercised when DB integration is enabled. | Skipped unless `AION_GAMESERVER_DB_INTEGRATION=1`. |

## Validation Decision

```text
- Changed surface: live packet dispatch, client packet parser, server packet, system messages, config binding, repository persistence, and active player/inventory mutation.
- Specific behavior/contract: Java `CM_LEGION_MODIFY_EMBLEM` default emblem change path checks active legion/rank/level/Kinah, decreases Kinah, stores `legion_emblems`, sends `SM_LEGION_UPDATE_EMBLEM`, and sends `STR_GUILD_CHANGE_EMBLEM`.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionSendEmblemInfoTests|FullyQualifiedName~LoadLegionEmblemAsync_HydratesCustomEmblemAgainstJavaSchema_WhenEnabled|FullyQualifiedName~SaveLegionEmblemMutationAsync_PersistsDefaultEmblemAndKinahAgainstJavaSchema_WhenEnabled" --logger "console;verbosity=minimal"
- Result: passed; 14 tests passed. Existing nullable/analyzer warnings were emitted outside this UOW.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and this UOW ports a direct packet/mutation/persistence path with focused C# runtime and byte-level tests.
- Broad-validation trigger: live packet dispatch, player inventory mutation, and repository persistence changed.
- Broad .NET decision: skipped after focused tests compiled the affected project and exercised parser, packet byte shape, live handler mutation, persistence capture, rejection branch, and DB-gated mapping.
- Why this scope is sufficient: the UOW is a narrow default-emblem mutation path; the focused tests cover the edited runtime branch and persistence contract without changing shared packet primitives.
```

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_LEGION_MODIFY_EMBLEM.readImpl/runImpl` | `CmLegionModifyEmblem` / `GameServerConnection.HandleLegionModifyEmblemAsync` | Live packet handler | Partial | Runtime Unit Tested | Partial Parity | Active-player default emblem changes are wired. Custom upload flow and broadcast-to-all-online-members remain incomplete. |
| `LegionService.storeLegionEmblem` | `GameServerConnection.HandleLegionModifyEmblemAsync` / `PlayerEnterWorldService.SaveLegionEmblemMutationAsync` | Service behavior | Partial | Runtime Unit Tested | Partial Parity | Rank, level, Kinah, mutation, persistence, self update, success message, and history are covered. Full `Legion` aggregate broadcast is not ported. |
| `LegionDAO.storeLegionEmblem` | `MySqlPlayerEnterWorldRepository.SaveLegionEmblemMutationAsync` | Persistence write | Partial | DB-Gated Integration Tested | Partial Parity | Uses Java schema and upsert behavior. Does not model Java `PersistentState` object lifecycle. |
| `SM_LEGION_UPDATE_EMBLEM.writeImpl` | `SmLegionUpdateEmblem` | Server packet | Partial | Byte Tested | Partial Parity | Field order/opcode covered. Broadcast routing beyond current connection is pending. |
| `LegionConfig.LEGION_EMBLEM_REQUIRED_KINAH` | `GameServerLegionOptions.EmblemRequiredKinah` | Config | Partial | Runtime Unit Tested via option input | Partial Parity | Key/default are bound. Broader config tests were not expanded in this UOW. |

## Known Gaps

- Java broadcasts emblem updates to all online legion members; C# currently sends the update to the requesting connection only because no full live legion member registry/aggregate is present.
- Java custom emblem upload remains deferred: `CM_LEGION_UPLOAD_INFO` and `CM_LEGION_UPLOAD_EMBLEM` are not wired.
- Java `LegionEmblem` upload/persistent-state lifecycle is not modeled as a full object aggregate.
- The DB integration test is gated and was not run against a live MySQL instance in this session.
- No Java/Maven test was run; Java source was unchanged and used as reference.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 6
- Total artifacts ported or extended in this UOW: 8
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 3
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Wire `CM_LEGION_UPLOAD_INFO` and the first safe part of `CM_LEGION_UPLOAD_EMBLEM` only if C# can model the active-player upload buffer and persist completed custom bytes in one runtime UOW.
2. Broaden legion emblem update fanout only when a live registry of online legion members exists or can be discovered from existing connection/world state without adding a standalone registry scaffold.
3. Continue from another deferred legion packet with a concrete live side effect, such as `CM_LEGION` sub-opcode behavior that sends an existing server packet or mutates loaded legion/player state.
