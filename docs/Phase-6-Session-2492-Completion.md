# Phase 6 Session 2492 Completion

## UOW

[Phase 6] UOW-2492: Compose Vortex kickPlayer removal metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexKickPlayerRemovalPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added non-live `VortexKickPlayerRemovalPlanService`.
- The planner models Java `Invasion.kickPlayer` removal intent for supplied snapshots:
  - participant removal from invader or defender maps;
  - alliance-member message and `PlayerAllianceService.removePlayer` intent;
  - disbanded-alliance reference clearing intent;
  - invader direct-portal message and home teleport intent when online in the invasion world;
  - passed-player removal and `syncPassed(true)` metadata.
- Scope remains metadata-only. It does not mutate live participant maps, alliances, passed-player maps, send packets, teleport players, or perform sync writes.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `KickPlayerRemovalPlan_OfflineInvaderRemovesParticipantPassedPlayerAndSyncsWithoutPackets` | Unit | `Invasion.kickPlayer` source review | Offline invader removal records participant, alliance, passed-player, and sync intent without packet/teleport intent | Focused C# test validates removed passed-player state, remaining sync count, disabled live mutation, and no online-only messages | Does not mutate live maps |
| `KickPlayerRemovalPlan_OnlineInvaderInInvasionWorldSendsPortalOutAndTeleportsHome` | Unit | `Invasion.kickPlayer` source review | Online invader in invasion world records alliance kick, direct-portal message, teleport, passed-player removal, and alliance nulling intent | Focused C# test validates message ids `1401452` and `1401474`, home point, disbanded alliance clearing, and disabled live packet/teleport | Does not send packets or teleport |
| `KickPlayerRemovalPlan_OnlineDefenderRemovesAllianceAndSendsDefenderKickOnly` | Unit | `Invasion.kickPlayer` source review | Online defender records defender alliance kick and alliance clearing without invader teleport/message | Focused C# test validates message id `1401476`, no direct-portal message, no teleport, sync intent, and disabled live mutation | Does not remove live alliance member |
| `KickPlayerRemovalPlan_NonParticipantStillRecordsPassedSyncIntentLikeJavaTailCall` | Unit | `Invasion.kickPlayer` source review | Non-participant snapshot records no participant removal while preserving Java tail-call sync metadata | Focused C# test validates no participant/alliance/passed removal and sync-count metadata | Java callers normally pass active participants; this covers planner guard shape |

## Validation Decision

- Changed surface: non-live Vortex kickPlayer removal metadata and focused tests.
- Specific behavior/contract: C# metadata mirrors Java `Invasion.kickPlayer` branch routing for participant removal, alliance-member message/removal, disbanded-alliance nulling, invader direct-portal message/teleport, passed-player removal, `syncPassed(true)` intent, and no live mutation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 90 tests. Existing nullable/analyzer warnings were emitted from unrelated files.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex kick/removal fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds non-live composition metadata and tests, without enabling live packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, scheduler dispatch, or zone-player/Kisk map mutation.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited Vortex kick/removal metadata paths.
- Why this scope is sufficient: the new code is an inert planner over supplied player, alliance, passed-player, invasion-world, and home-point snapshots.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported an expected CRLF conversion warning for the edited test file.
- `git diff --cached --check` passed after staging. Git reported expected CRLF conversion warnings for edited/new files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexKickPlayerRemovalPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# composes kick/removal branches as metadata. Live participant, alliance, packet, teleport, passed-player, and sync mutation remain disabled. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Services.VortexKickPlayerAllianceSnapshot` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records only alliance existence, membership, and disband-after-removal snapshots needed by kickPlayer branches. Full alliance runtime behavior remains elsewhere. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Services.VortexKickPlayerRemovalPlan` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records Java message ids `1401452`, `1401476`, and `1401474` as send intent only. Live packet dispatch remains disabled. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo` | `Aion.GameServer.Services.VortexKickPlayerRemovalPlan` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records invader home-teleport intent only when online in the invasion world. Live teleport remains disabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 1
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live participant, alliance, packet, teleport, passed-player, and sync mutation remain disabled.
- Java concurrent map behavior is represented only by supplied snapshots.
- Alliance disbanding after live removal is represented only by a supplied boolean.
- Existing runtime removal has some live-state behavior, but this UOW does not wire the planner into production runtime paths.
- Production Vortex lifecycle adapters still need real player, alliance, passed-player, and location inputs before live use.
