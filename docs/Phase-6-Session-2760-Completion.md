# Phase 6 Session 2760 Completion

## Unit of Work

[Phase 6][UOW-2760] Wire live legion dominion selection

## Runtime Progress Gate

- Deferred/live behavior advanced: `CM_LEGION` exOpcode `0x10` now dispatches a live legion dominion selection path instead of being ignored by the C# handler.
- Java source of truth: `CM_LEGION.runImpl`, `LegionService.joinLegionDominion`, `LegionDominionService.join`, `LegionDominionLocation.join`, `LegionDominionDAO.storeNewInfo`, and `LegionDAO.storeLegion`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionAsync`, new dominion join handler/fanout, `SmSystemMessage.MsgGuildApplyDominion`, and `IPlayerEnterWorldRepository` persistence for `legion_dominion_participants` plus `legions.current_legion_dominion`.
- Client-visible/state/persistence effect changed: Brigade General or Deputy selection inserts a live dominion participant row, persists the legion's current dominion, mutates online legion members' current dominion state, and sends real `SM_SYSTEM_MESSAGE(1402902)` plus refreshed `SM_LEGION_INFO` packets to online same-legion players.
- Why this is not preview-only/test-only/documentation-only: this UOW wires a deferred client packet branch and performs live state mutation, persistence, and packet fanout from `GameServerConnection`.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/services/LegionDominionService.java`
- `game-server/src/com/aionemu/gameserver/model/legionDominion/LegionDominionLocation.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDominionDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_INFO.java`

## C# Runtime Changes

- Added `CM_LEGION 0x10` dispatch to `HandleLegionAsync`.
- Added Java-equivalent guards: only Brigade General/Deputy may select, and selection is ignored when the legion already has a current dominion.
- Added repository methods to insert a dominion participant only once and persist `legions.current_legion_dominion`.
- Added `SM_SYSTEM_MESSAGE` helper for Java message id `1402902`.
- Broadcasts the apply-dominion message and refreshed `SM_LEGION_INFO` to the requester and online same-legion players while mutating their loaded `LegionCurrentLegionDominion`.

## Validation Decision

- Changed surface: one live client-packet branch, one system-message helper, repository persistence methods, and legion fanout.
- Specific behavior/contract: Java `CM_LEGION 0x10` accepts a dominion id, rejects non-leader ranks or already selected legions, joins once, stores current dominion, and broadcasts message/info to legion members.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SaveLegionCurrentDominionAsync_WritesJavaLegionDominionColumn_WhenEnabled|FullyQualifiedName~TryAddLegionDominionParticipantAsync_InsertsOnceLikeJavaLocationJoin_WhenEnabled" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this packet/service path.
- Broad-validation trigger: live packet branch with persistence and packet fanout.
- Broad .NET decision: skipped after focused validation because the targeted filter compiled the affected test project and directly exercised the edited handler, packet helper, repository interface, and guarded DB integration method names.
- Why this scope is sufficient: the UOW touched one `CM_LEGION` sub-branch plus its direct repository and packet dependencies; no packet primitive, scheduler, schema migration, or shared combat/world logic changed.

## Validation Result

- Focused C# result: Passed, 60 total, 0 failed, 0 skipped.
- `git diff --check`: passed; line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.
- DB integration tests were added behind the existing `AION_GAMESERVER_DB_INTEGRATION=1` guard; they compiled in the focused command but did not hit MySQL because the env flag was not set.
- Java/Maven was not run for the reason above.

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ReadFrom_LegionDominionSelectionConsumesJavaLocationId` | Unit | `CM_LEGION.readImpl` case `0x10` | C# parser reads the dominion id like Java. | Packet readback assertion. | Does not execute runtime branch. |
| `SmSystemMessage_LegionNoticeHelpersUseJavaIdsAndParameters` | Unit | `SM_SYSTEM_MESSAGE.STR_MSG_GUILD_APPLY_DOMINION` | Message id `1402902` and parameter shape. | Java source review plus helper assertion. | Uses scoped id string until l10n static loading is ported. |
| `HandleInfrastructurePacketAsync_DominionJoinRejectsNonDeputyOrBrigadeGeneralLikeJava` | Unit | `LegionService.joinLegionDominion` rank guard | Non-leader ranks do not persist or send packets. | Live handler assertion. | No Java fixture. |
| `HandleInfrastructurePacketAsync_DominionJoinRejectsAlreadySelectedLegionLikeJava` | Unit | `legion.getCurrentLegionDominion() > 0` guard | Already selected legions do not insert participants or mutate state. | Live handler assertion. | No Java fixture. |
| `HandleInfrastructurePacketAsync_DominionJoinDuplicateParticipantDoesNotMutateLikeJava` | Unit | `LegionDominionLocation.join` duplicate participant guard | Duplicate participant insert failure aborts current-dominion persistence and packets. | Repository-call and live state assertions. | Uses fake repository. |
| `HandleInfrastructurePacketAsync_DominionJoinPersistsStateAndBroadcastsInfoLikeJava` | Unit | `LegionService.joinLegionDominion` success branch | Participant insert, current dominion persistence, online same-legion state mutation, and message/info fanout. | Live handler and packet assertions. | Dominion l10n parameter is fallback id string. |
| `SaveLegionCurrentDominionAsync_WritesJavaLegionDominionColumn_WhenEnabled` | Guarded DB Integration | `LegionDAO.storeLegion` | Writes `legions.current_legion_dominion` against Java schema when DB integration is enabled. | Compiled guarded integration test. | Not run without `AION_GAMESERVER_DB_INTEGRATION=1`. |
| `TryAddLegionDominionParticipantAsync_InsertsOnceLikeJavaLocationJoin_WhenEnabled` | Guarded DB Integration | `LegionDominionLocation.join` / `LegionDominionDAO.storeNewInfo` | Inserts one participant row and rejects duplicates when DB integration is enabled. | Compiled guarded integration test. | Not run without `AION_GAMESERVER_DB_INTEGRATION=1`. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` case `0x10` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | Dominion selection branch is live. Legion create/invite/brigade-general transfer/dominion ranking remain outside this UOW. |
| `com.aionemu.gameserver.services.LegionService.joinLegionDominion` | `HandleLegionDominionJoinAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Rank/current-dominion/join/store/fanout path is ported. Java location l10n lookup is not yet backed by C# static dominion data. |
| `com.aionemu.gameserver.model.legionDominion.LegionDominionLocation.join` | `TryAddLegionDominionParticipantAsync` | Persistence / Runtime State | Partial | Guarded DB Test Added | Needs Verification | Inserts only when participant is not already present. No in-memory dominion service map is maintained yet. |
| `com.aionemu.gameserver.dao.LegionDAO.storeLegion` | `SaveLegionCurrentDominionAsync` | Repository | Partial | Guarded DB Test Added | Needs Verification | This scoped method persists current dominion only, not every `storeLegion` column. |
| `SM_SYSTEM_MESSAGE.STR_MSG_GUILD_APPLY_DOMINION` | `SmSystemMessage.MsgGuildApplyDominion` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Message id/parameter count match. Parameter currently uses dominion id string instead of Java `LegionDominionLocation.getL10n()`. |
| `SM_LEGION_INFO` | `SmLegionInfo.FromPlayer` | Server Packet | Previously Ported / Reused | Unit Tested | Partial Parity | This UOW reuses and sends the packet after mutating current dominion. |

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported or advanced in this UOW: 6 partial runtime/packet/repository artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 6
- Total blocked artifacts: 2 (live quest-finish challenge progress hook, dominion l10n/static service map)
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Dominion message parameter uses the selected dominion id string; Java uses `LegionDominionLocation.getL10n()` from `legion_dominion_template.xml`.
- C# does not yet load Java legion dominion static data into a runtime dominion service map.
- C# does not yet maintain Java's in-memory `LegionDominionService` participant/location state beyond the database-backed selection path.
- DB integration tests were not executed against MySQL in this session because `AION_GAMESERVER_DB_INTEGRATION` was not enabled.
- The previously suggested challenge quest-finish UOW is currently blocked as a runtime UOW because C# quest finish remains socket-guarded/planner-only; wiring challenge progress there would not pass the Runtime Progress Gate.
