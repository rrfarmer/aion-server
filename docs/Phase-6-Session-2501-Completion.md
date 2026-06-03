# Phase 6 Session 2501 Completion

## UOW

[Phase 6] UOW-2501: Compose Vortex defender acceptance request payload metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/RequestResponseHandler.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/ResponseRequester.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/QuestionResponseRegistry.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationAcceptancePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `QuestionResponseRequestKind.VortexDefenderInvitation` so Vortex defender request metadata can identify the Java request type.
- Added `VortexDefenderInvitationRequestPayloadPlanService` to compose an inert `QuestionResponseRequest` payload only when the defender invitation plan would send Java question id `904306`.
- Added `PendingVortexDefenderInvitationRequest` to capture requester object id, question id, defender alliance snapshot, and existing defender ids for later response handling.
- Added `VortexDefenderInvitationResponseDispatchPlanService` to mirror Java `RequestResponseHandler.handle`: response code `0` denies, any nonzero response accepts and delegates to the existing acceptance plan.
- Scope remains metadata-only. It does not register live requests, send `SM_QUESTION_WINDOW`, remove live requests, execute live callbacks, mutate groups, mutate alliances, mutate defender maps, despawn NPCs, spawn NPCs, start portals, or schedule stops.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DefenderInvitationRequestPayloadPlan_CreatesQuestionResponseRequestOnlyForQuestionWindowIntent` | Unit | `Invasion.updateDefenders` and `ResponseRequester.putRequest` source review | C# creates a Vortex defender pending request payload only for invitation plans with Java question-window intent | Focused C# test validates request kind, question id, requester/defender ids, alliance snapshot, existing defender ids, and disabled live flags | Does not store the request in a live registry or send the packet |
| `DefenderInvitationResponseDispatchPlan_MapsZeroToDenyAndNonZeroToAcceptLikeJavaHandle` | Unit | `RequestResponseHandler.handle` and `Invasion.updateDefenders.acceptRequest` source review | C# response dispatch metadata maps zero to deny and nonzero to existing acceptance metadata | Focused C# test validates deny/accept status, response code, Java source markers, acceptance plan creation, and disabled live mutation flags | Does not remove the live request or execute the callback |

## Validation Decision

- Changed surface: non-live Vortex defender invitation request payload/dispatch metadata plus focused tests.
- Specific behavior/contract: request payload and accept/deny dispatch shape from Java `RequestResponseHandler.handle`, with live request storage, packet dispatch, group mutation, alliance mutation, defender-map mutation, scheduler, spawn, despawn, and portal effects disabled.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Result: Passed, 108 tests.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex defender-request fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds inert metadata/adapters and tests, without enabling live request storage, packet dispatch, request removal, callback execution, alliance mutation, group mutation, defender mutation, portal spawn, NPC despawn, NPC spawn, or scheduler dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited registry/adapter/request path.
- Why this scope is sufficient: the new code composes Java-shaped pending request metadata and response dispatch metadata while keeping all production side effects explicitly disabled.

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
| `com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler` | `Aion.GameServer.Services.VortexDefenderInvitationResponseDispatchPlanService` | Runtime response metadata | Partial | Unit Tested | Partial Parity | C# maps response code `0` to deny and nonzero to accept; live request removal and callback invocation remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderInvitationRequestPayloadPlanService` | Runtime request metadata adapter | Partial | Unit Tested | Partial Parity | C# creates Vortex defender pending request payload metadata for question-window intent; live request storage and packet dispatch remain disabled. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester` | `Aion.GameServer.Model.GameObjects.QuestionResponseRequestKind.VortexDefenderInvitation` and `Aion.GameServer.Model.GameObjects.QuestionResponseRequest` | Runtime request metadata | Partial | Unit Tested | Partial Parity | C# now carries a Vortex-specific request kind and payload; Java `putRequest` storage remains disabled in Vortex path. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live defender request storage and `SM_QUESTION_WINDOW` dispatch remain disabled.
- Acceptance callback side effects remain inert and do not mutate groups, alliances, or defender maps.
- Live request removal from the registry is not enabled.
- Production world/location/alliance containers remain absent from the Vortex start path.
