# Phase 6 Session 2506 Completion

## UOW

[Phase 6] UOW-2506: Add guarded Vortex defender invitation registration runtime adapter

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/ResponseRequester.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexDefenderInvitationRegistrationRuntimeAdapterService` as an opt-in runtime adapter for Java `Invasion.updateDefenders`.
- The adapter accepts a `Player` and `VortexDefenderInvitationPlan`, creates the existing pending Vortex request payload, calls `Player.ResponseRequester.PutRequest`, and reports actual registry storage.
- The adapter records the Java question-window intent for `SM_QUESTION_WINDOW(904306, 0, 0)` only when request storage succeeds.
- Scope remains guarded. It does not send packets, execute live callbacks, mutate groups, mutate alliances, mutate defender maps, schedule, spawn, despawn, or start portals.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DefenderInvitationRegistrationRuntimeAdapter_StoresPendingRequestAndRecordsQuestionWindowIntent` | Unit | `Invasion.updateDefenders`, `ResponseRequester.putRequest`, and `SM_QUESTION_WINDOW` source review | Successful C# registration stores a Vortex pending request, preserves payload metadata, and records question-window intent | Focused C# test validates registry count, request kind, payload fields, question id `904306`, `0,0` window args, and disabled live side effects | Does not send the packet yet |
| `DefenderInvitationRegistrationRuntimeAdapter_RejectsOccupiedRequestSlotLikeJavaPutIfAbsent` | Unit | `ResponseRequester.putRequest` source review | Occupied request id rejects the Vortex request and preserves the existing request | Focused C# test validates `PutRequest` false-equivalent metadata and that the original request remains in the registry | Does not execute Java runtime |
| `DefenderInvitationRegistrationRuntimeAdapter_SkipsGuardedDefenderWithoutRegistryMutation` | Unit | `Invasion.updateDefenders` source review | Existing defender guard skips request registration | Focused C# test validates no registry mutation and disabled packet/callback/team side effects | Full-alliance guard remains covered by adjacent planner/report tests |

## Validation Decision

- Changed surface: one C# opt-in runtime adapter/report plus focused tests.
- Specific behavior/contract: Java `Invasion.updateDefenders` creates a Vortex response handler, calls `ResponseRequester.putRequest(904306, handler)`, and sends `SM_QUESTION_WINDOW(904306, 0, 0)` only when storage succeeds; C# now stores the pending Vortex request through `Player.ResponseRequester.PutRequest` and records the question-window intent while keeping packet dispatch, callback execution, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests|FullyQualifiedName~GameServerConnectionVortexQuestionResponseTests" --no-restore
```

- Result: Passed, 118 tests. Existing nullable/analyzer warnings were emitted from unrelated files.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and Java behavior was reviewed directly from the narrow source methods.
- Broad-validation trigger: none. This adapter is opt-in and is not wired into live start/update-defenders dispatch or packet sending.
- Broad .NET decision: full project/solution validation skipped because the focused command covered the edited Vortex tests plus adjacent registry and connection response contracts.
- Why this scope is sufficient: the changed surface is isolated to Vortex registration metadata and actual pending-request storage; the focused tests prove success, occupied-slot rejection, skipped guard behavior, payload preservation, and disabled live side effects.

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
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderInvitationRegistrationRuntimeAdapterService` | Runtime Vortex registration adapter | Partial | Unit Tested | Partial Parity | C# now performs guarded pending-request storage and records question-window intent. Live packet dispatch, callback execution, defender map mutation, alliance mutation, and start/update-defenders wiring remain disabled. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest` | `Aion.GameServer.Model.GameObjects.QuestionResponseRegistry.PutRequest` | Runtime request registry | Partial | Unit Tested | Partial Parity | C# adapter uses the registry's null-rejecting, put-if-absent-equivalent behavior; occupied request slot preserves the existing request. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` | `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow` | Packet intent | Partial | Unit Tested | Partial Parity | C# records Java question id `904306` and args `0,0` only after successful storage, but packet sending remains intentionally disabled in this guarded adapter. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 42%

## Remaining Risks

- Live defender update/start path is not wired to the registration adapter.
- `SM_QUESTION_WINDOW` packet dispatch remains disabled.
- Vortex response-handler callback side effects remain metadata-only and do not mutate groups, alliances, or defender maps.
- Production world/location/alliance containers remain absent from the Vortex start path.
- Java runtime comparison was not run; parity evidence is source review plus focused C# tests.
