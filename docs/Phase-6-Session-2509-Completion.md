# Phase 6 Session 2509 Completion

## UOW

[Phase 6] UOW-2509: Add guarded Vortex defender batch registration runtime adapter

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAllianceUpdatePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexDefenderInvitationBatchRuntimeAdapterService`, an opt-in batch adapter that applies the existing single-defender registration runtime adapter and question-window intent adapter to each supplied defender `Player`.
- The adapter carries Java source traceability for `Invasion.updateAlliance -> Invasion.updateDefenders` and returns aggregate plus per-defender runtime reports.
- Successful registration stores the pending request through the player response registry and creates a non-sent Vortex question-window intent.
- Occupied request slots, already-present defenders, and full defender alliances keep Java guard outcomes: no replacement of existing requests, no duplicate defender registration, and no packet intent.
- Scope remains guarded. It does not call `SendPacketAsync`, execute callbacks, mutate groups, mutate alliances, mutate defender maps, schedule, spawn, despawn, or start portals.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DefenderInvitationBatchRuntimeAdapter_RegistersMultipleDefendersAndGatesQuestionWindowIntent` | Unit | `Invasion.updateAlliance`, `Invasion.updateDefenders`, `ResponseRequester.putRequest`, `SM_QUESTION_WINDOW` source review | Batch registration applies per defender, stores only successful requests, creates question-window intent only after storage, preserves occupied-slot and already-defender outcomes | Focused C# test validates aggregate counts, per-player statuses, request registry mutation, occupied request preservation, Java source traceability, and disabled side-effect flags | Input players are already supplied as defender candidates; no production location scan |
| `DefenderInvitationBatchRuntimeAdapter_FullAllianceSkipsAllDefendersWithoutRegistryMutation` | Unit | `Invasion.updateDefenders` source review | Full defender alliance guard skips all supplied defenders without registry or packet-intent mutation | Focused C# test validates full-alliance skip status, zero request storage, zero question-window intents, empty registries, and disabled side-effect flags | Does not execute Java runtime |

## Validation Decision

- Changed surface: one C# opt-in batch runtime adapter/report plus focused tests.
- Specific behavior/contract: Java `updateAlliance` loops defender-race candidates and calls `updateDefenders`; each registration stores a request and sends a question window only if `ResponseRequester.putRequest` succeeds. C# now applies the existing single-defender registration and intent adapters per supplied defender player while live side effects stay disabled.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Result: Passed, 123 tests.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and there is no narrow Java batch fixture for this seam.
- Broad-validation trigger: none. This adapter is opt-in and does not send packets or wire live lifecycle dispatch.
- Broad .NET decision: full project/solution validation skipped because the focused command covered the edited Vortex tests and the adjacent response-registry contract.
- Why this scope is sufficient: the changed surface is isolated to guarded batch composition of already-tested registration and packet-intent adapters; focused tests cover successful registration, occupied request slot, already-defender skip, full-alliance skip, disabled live sends, and disabled gameplay mutation.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported expected CRLF conversion warnings for edited C# source.
- `git diff --cached --check` passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance` | `Aion.GameServer.Services.VortexDefenderInvitationBatchRuntimeAdapterService` | Runtime Vortex batch registration adapter | Partial | Unit Tested | Partial Parity | C# accepts defender `Player` candidates and applies guarded update-defenders registration plus non-sent question-window intent per candidate. Production lifecycle wiring, live location filtering, packet sending, and gameplay mutations remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderInvitationBatchRuntimeAdapterService` / `Aion.GameServer.Services.VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService` | Runtime Vortex defender registration adapter | Partial | Unit Tested | Partial Parity | C# preserves existing-defender, full-alliance, and occupied-request-slot guards while storing pending requests only through the response registry. Live callback execution and defender/alliance mutations remain disabled. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest` | `Aion.GameServer.Network.Aion.QuestionResponseRegistry.PutRequest` | Question response registry | Partial | Unit Tested | Partial Parity | C# preserves no-replace registration semantics for occupied request ids and leaves existing requests intact. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` | `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow` / `Aion.GameServer.Services.VortexDefenderQuestionWindowIntentAdapterService` | Server packet intent | Partial | Unit Tested | Partial Parity | C# creates non-sent Vortex question-window intent only after request storage succeeds. Live packet dispatch remains disabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 45%

## Remaining Risks

- Live Vortex `updateAlliance` is not wired to production location-player enumeration.
- The batch adapter expects defender candidates from the caller and does not yet filter live location players by defender race.
- `SM_QUESTION_WINDOW` packet dispatch remains disabled.
- Vortex response-handler callbacks, acceptance flow, add-player transition, and alliance mutation remain metadata-only.
- Production world/location/alliance containers remain absent from the Vortex start path.
