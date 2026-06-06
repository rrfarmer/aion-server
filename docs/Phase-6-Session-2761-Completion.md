# Phase 6 Session 2761 Completion

## Unit of Work

[Phase 6][UOW-2761] Use dominion l10n in live legion selection

## Runtime Progress Gate

- Deferred/live behavior advanced: the live `CM_LEGION 0x10` dominion-selection path now consumes Java legion dominion static data for its client-visible apply-dominion message.
- Java source of truth: `LegionDominionService.initLocations`, `LegionDominionLocationTemplate.getL10nId`, `L10n.getL10n`, `ChatUtil.l10n`, and `LegionService.joinLegionDominion`.
- C# runtime artifact wired/fixed: `StaticData` legion dominion location loading, new `LegionDominionTable`, and `GameServerConnection.BroadcastLegionDominionJoinedAsync`.
- Client-visible/state/persistence effect changed: successful live dominion selection now sends `SM_SYSTEM_MESSAGE(1402902)` with Java's encoded location l10n parameter from `legion_dominion_template.xml` when runtime static data is available.
- Why this is not preview-only/test-only/documentation-only: Java XML/static data is loaded into a runtime C# structure consumed by a live packet handler that sends a real server packet.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionDominionService.java`
- `game-server/src/com/aionemu/gameserver/model/templates/LegionDominionLocationTemplate.java`
- `game-server/src/com/aionemu/gameserver/model/templates/L10n.java`
- `game-server/src/com/aionemu/gameserver/utils/ChatUtil.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/data/static_data/legion_dominion_template.xml`

## C# Runtime Changes

- Added `LegionDominionTable` and `LegionDominionLocationSummary` with Java `ChatUtil.l10n(name_id)` encoding.
- Extended `StaticData.LoadFromCacheAsync` to load `legion_dominion_template/legion_dominion_location` `id` and `name_id`.
- Updated live `CM_LEGION 0x10` dominion fanout to use `StaticData.LegionDominions.GetLocation(id).L10n`.
- Preserved the previous id-string fallback when static data is unavailable or missing the requested location.

## Validation Decision

- Changed surface: runtime static-data loading plus a live client-packet side effect.
- Specific behavior/contract: Java dominion `name_id` is loaded and encoded through `ChatUtil.l10n`, then emitted by the live `SM_SYSTEM_MESSAGE(1402902)` dominion-selection path.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~StaticData_LoadsLegionDominion" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java test fixture exists for `L10n.getL10n`/dominion dataholder behavior.
- Broad-validation trigger: live packet branch consumes newly loaded runtime static data.
- Broad .NET decision: skipped after focused validation because the filtered command compiled the affected project and directly exercised the edited static loader and live handler branch.
- Why this scope is sufficient: this UOW touched one static data table and one live legion branch; no shared packet primitive, crypto primitive, scheduler, schema, or broad world state changed.

## Validation Result

- Focused C# result: Passed, 60 total, 0 failed, 0 skipped.
- `git diff --check`: passed; line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.
- Java/Maven was not run for the reason above.

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StaticData_LoadsLegionDominionLocationsWithJavaL10nEncoding` | Unit | `LegionDominionLocationTemplate.getL10nId`, `L10n.getL10n`, `ChatUtil.l10n` | Static data loads dominion id/name_id and exposes Java encoded l10n strings. | Java source review plus loader assertion. | Minimal XML fixture, not full XML count comparison. |
| `HandleInfrastructurePacketAsync_DominionJoinPersistsStateAndBroadcastsInfoLikeJava` | Unit | `LegionService.joinLegionDominion` success branch | Live dominion selection sends `SM_SYSTEM_MESSAGE(1402902)` with Java encoded l10n when runtime static data is present, then sends `SM_LEGION_INFO`. | Live handler assertion and packet parameter check. | Uses test fixture static data; no real client validation. |
| `HandleInfrastructurePacketAsync_DominionJoinFallsBackToIdWhenStaticDataMissing` | Unit | C# runtime fallback guard | Missing static data preserves a deterministic fallback instead of dropping the live packet. | Live handler assertion. | Fallback is a C# safety behavior, not Java's normal loaded-data path. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.LegionDominionService.initLocations` | `Aion.GameServer.Dataholders.StaticData` / `LegionDominionTable` | Static Data / Runtime Loading | Partial | Unit Tested | Partial Parity | Loads location id and name_id needed by live selection. It does not yet build the full in-memory location/participant service map, rewards, rifts, or occupation state. |
| `com.aionemu.gameserver.model.templates.LegionDominionLocationTemplate` | `LegionDominionLocationSummary` | DTO / Static Data | Partial | Unit Tested | Partial Parity | Only `id` and `name_id` are represented because those are used by the live packet path in this UOW. World, race, zone, rewards, and invasion rift remain unported. |
| `com.aionemu.gameserver.model.templates.L10n` | `LegionDominionLocationSummary.L10n` | Utility Contract | Partial | Unit Tested | Partial Parity | Uses existing C# `ChatUtil.L10n` to match Java encoded client string. Null id behavior remains in `ChatUtil` and was not broadened in this UOW. |
| `com.aionemu.gameserver.services.LegionService.joinLegionDominion` | `GameServerConnection.BroadcastLegionDominionJoinedAsync` | Live Handler Side Effect | Partial | Unit Tested | Partial Parity | Success branch now uses Java static-data l10n for the system message. Rank/persistence/fanout behavior came from UOW-2760. |
| `com.aionemu.gameserver.utils.ChatUtil.l10n` | `Aion.GameServer.Utils.ChatUtil.L10n` | Utility | Previously Ported / Reused | Unit Tested Through Caller | Partial Parity | This UOW reused the helper for dominion names; did not audit every ChatUtil method. |

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported or advanced in this UOW: 5 partial runtime/static-data artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 5
- Total blocked artifacts: 2 (full dominion service map/ranking, live quest-finish challenge progress hook)
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Full Java legion dominion static model is not ported: world id, race, zone, rewards, invasion rifts, occupation dates, and participant ranking state remain missing.
- `CM_LEGION_DOMINION_REQUEST_RANKING` remains deferred and has no `SM_LEGION_DOMINION_RANK` C# packet yet.
- C# does not yet maintain Java's in-memory `LegionDominionService` participant/location state beyond database-backed selection and static l10n lookup.
- No full Java/C# runtime comparison was performed for the encoded string; the C# helper is source-reviewed against Java.
- Quest-finish challenge progress remains blocked as a runtime UOW until a live C# quest-finish execution hook exists.
