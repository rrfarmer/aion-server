# Phase 6 Session 2507 Completion

## UOW

[Phase 6] UOW-2507: Wire Vortex defender invitation registration into a guarded update-defenders adapter

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/ResponseRequester.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderUpdateDefendersPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService`, an opt-in adapter that snapshots a live defender's Vortex request slot, composes the update-defenders invitation plan, delegates to `VortexDefenderInvitationRegistrationRuntimeAdapterService`, and returns observer-friendly metadata.
- Refined `VortexDefenderInvitationRequestPayloadPlanService` so `RequestNotStored` still creates the pending handler payload when Java would create `RequestResponseHandler` before calling `putRequest`.
- The adapter records Java question-window intent only after actual C# registry storage succeeds.
- Scope remains guarded. It does not send packets, execute live callbacks, mutate groups, mutate alliances, mutate defender maps, schedule, spawn, despawn, or start portals.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DefenderInvitationRequestPayloadPlan_CreatesQuestionResponseRequestForJavaHandlerBeforePutRequest` | Unit | `Invasion.updateDefenders` source review | C# creates pending request payload whenever Java would create the response handler, including request-storage rejection paths | Focused C# test validates payload creation for planned and request-not-stored invitations, and no payload for existing-defender guard | Does not execute Java runtime |
| `DefenderUpdateDefendersRegistrationRuntimeAdapter_RegistersEligibleDefenderFromLivePlayerSlot` | Unit | `Invasion.updateDefenders`, `ResponseRequester.putRequest`, and `SM_QUESTION_WINDOW` source review | Eligible defender flows through update-defenders planning to registry storage and question-window intent | Focused C# test validates live slot snapshot, pending request storage, existing defender metadata, and disabled live side effects | Packet dispatch remains disabled |
| `DefenderUpdateDefendersRegistrationRuntimeAdapter_RejectsOccupiedLiveRequestSlotWithoutReplacingRequest` | Unit | `ResponseRequester.putRequest` source review | Occupied live request slot is attempted and rejected without replacing the existing request | Focused C# test validates request-slot snapshot, payload presence, `PutRequest` false-equivalent metadata, no question-window intent, and preserved existing request | Does not run Java `ConcurrentHashMap.putIfAbsent` directly |
| `DefenderUpdateDefendersRegistrationRuntimeAdapter_SkipsExistingDefenderBeforeRegistryMutation` | Unit | `Invasion.updateDefenders` source review | Existing defender guard stops before request registration | Focused C# test validates skipped status, no registry mutation, and disabled packet/callback/team side effects | Full-alliance guard remains covered by adjacent planner/report tests |

## Validation Decision

- Changed surface: one C# opt-in runtime adapter/report, one Vortex payload-planner parity refinement, and focused tests.
- Specific behavior/contract: Java `Invasion.updateDefenders` checks existing defenders, checks defender-alliance fullness, creates `RequestResponseHandler`, calls `ResponseRequester.putRequest(904306, handler)`, and sends `SM_QUESTION_WINDOW(904306, 0, 0)` only when storage succeeds; C# now composes that guarded path from live player slot state and delegates registration through `Player.ResponseRequester.PutRequest` while keeping packet dispatch, callback execution, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Result: Passed, 119 tests. Existing nullable/analyzer warnings were emitted from unrelated files.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and the narrow source-of-truth behavior was reviewed directly.
- Broad-validation trigger: none. This adapter is opt-in and is not wired into live start/update-defenders dispatch or packet sending.
- Broad .NET decision: full project/solution validation skipped because the focused command covered the edited Vortex tests plus adjacent registry contract.
- Why this scope is sufficient: the changed surface is isolated to update-defenders registration metadata and pending-request storage; focused tests cover eligible registration, occupied-slot rejection, Java handler payload ordering, skipped guard behavior, and disabled live side effects.

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
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService` | Runtime Vortex update-defenders adapter | Partial | Unit Tested | Partial Parity | C# now snapshots live request-slot state, composes invitation planning, creates handler payload before storage rejection, and delegates pending-request storage. Live packet dispatch, callback execution, defender map mutation, alliance mutation, and production start/update-defenders wiring remain disabled. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest` | `Aion.GameServer.Model.GameObjects.QuestionResponseRegistry.PutRequest` | Runtime request registry | Partial | Unit Tested | Partial Parity | C# adapter uses put-if-absent-equivalent storage and preserves existing requests when question id `904306` is occupied. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` | `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow` | Packet intent | Partial | Unit Tested | Partial Parity | C# records Java question id `904306` and args `0,0` only after successful storage. Actual packet sending remains disabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 43%

## Remaining Risks

- Live defender update/start path is still not wired to production Vortex lifecycle.
- `SM_QUESTION_WINDOW` packet dispatch remains disabled.
- Vortex response-handler callback side effects remain metadata-only and do not mutate groups, alliances, or defender maps.
- Production world/location/alliance containers remain absent from the Vortex start path.
- Java runtime comparison was not run; parity evidence is source review plus focused C# tests.
