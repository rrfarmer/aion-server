# Phase 6 Session 2500 Completion

## UOW

[Phase 6] UOW-2500: Capture Vortex defender request-slot metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/ResponseRequester.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/RequestResponseHandler.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/QuestionResponseRegistry.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmQuestionWindow.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionRuntimeSnapshotCollectorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestionResponseRegistryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added read-only `ContainsRequest` and `IsRequestSlotAvailable` helpers to `QuestionResponseRegistry`, matching Java `ResponseRequester.putRequest` same-message-id guard.
- Added `SmQuestionWindow.VortexDefenderInvitation` for Java question id `904306`.
- Added `VortexDefenderInvitationRequestSlotSnapshotService` to snapshot defender request-slot availability from `Player.ResponseRequester` without storing requests.
- `VortexStartInvasionRuntimeSnapshotCollectorService` now derives defender request-slot metadata from supplied zone-player `Player` objects when an explicit request-slot dictionary is not supplied.
- Scope remains metadata-only. It does not store live defender requests, send `SM_QUESTION_WINDOW`, execute acceptance callbacks, mutate groups, mutate alliances, mutate defender maps, despawn NPCs, spawn NPCs, start portals, or schedule stops.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `IsRequestSlotAvailable_ReflectsJavaPutIfAbsentMessageIdGuard` | Unit | `ResponseRequester.putRequest` source review | Read-only C# request-slot availability mirrors Java same-message-id `putIfAbsent` guard | Focused C# test validates open, occupied, and unrelated question id states | Does not execute Java runtime |
| `DefenderInvitationRequestSlotSnapshot_UsesPlayerResponseRequesterForJavaQuestionId` | Unit | `Invasion.updateDefenders` and `ResponseRequester.putRequest` source review | Vortex adapter snapshots question id `904306` availability from `Player.ResponseRequester` | Focused C# test validates open and occupied defender request-slot snapshots | Does not store the live Vortex request handler |
| `StartSnapshotCollector_PreparesRuntimeStaticRequestWithDefenderAllianceMetadata` | Unit | `Invasion.startInvasion`, `Invasion.updateAlliance`, `Invasion.updateDefenders`, and `ResponseRequester.putRequest` source review | Start collector derives request-not-stored metadata from the defender player's existing response request | Focused C# test validates collector-fed request-slot availability and disabled live flags | Supplied zone players still come from test fixtures |

## Validation Decision

- Changed surface: read-only question-response registry metadata, Vortex defender request-slot snapshot adapter, start collector metadata, and focused tests.
- Specific behavior/contract: C# Vortex defender invitation adapter metadata captures Java request-slot availability and question-window intent while keeping live request storage, packet dispatch, acceptance, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Result: Passed, 106 tests. Existing nullable/analyzer warnings were emitted from unrelated files.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex defender-request fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds read-only metadata/adapters and tests, without enabling live request storage, packet dispatch, acceptance callbacks, alliance mutation, group mutation, defender mutation, portal spawn, NPC despawn, NPC spawn, or scheduler dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited registry/adapter/request path.
- Why this scope is sufficient: the new code reads existing registry state and carries inert Java-shaped request-slot metadata for Vortex defender invitation planning.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported expected CRLF conversion warnings for edited C# source and test files.
- `git diff --cached --check` passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester` | `Aion.GameServer.Model.GameObjects.QuestionResponseRegistry` | Runtime request registry | Partial | Unit Tested | Partial Parity | C# exposes read-only same-question availability metadata matching Java `putIfAbsent`; live Vortex request storage remains disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` | `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow` | Packet constant | Partial | Unit Tested | Partial Parity | C# names Vortex defender invitation question id `904306`; packet send remains disabled in Vortex path. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderInvitationRequestSlotSnapshotService` | Runtime metadata adapter | Partial | Unit Tested | Partial Parity | C# snapshots defender request-slot availability from `Player.ResponseRequester` and feeds batch invitation metadata. Acceptance callback and live packet dispatch remain disabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live defender request storage and `SM_QUESTION_WINDOW` dispatch remain disabled.
- Java `RequestResponseHandler.acceptRequest` acceptance flow remains represented only by existing metadata plans.
- Prepared start requests still depend on supplied runtime candidates rather than production Vortex location/world/alliance containers.
- Full production XML coverage for Vortex static INVASION spawns has not been compared against Java output.
