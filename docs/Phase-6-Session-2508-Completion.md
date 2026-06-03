# Phase 6 Session 2508 Completion

## UOW

[Phase 6] UOW-2508: Add guarded Vortex defender question-window packet intent adapter

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderUpdateDefendersPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

## Implementation Notes

- Added `VortexDefenderQuestionWindowIntentAdapterService`, an opt-in adapter that accepts a `VortexDefenderUpdateDefendersRegistrationRuntimeReport` and creates a non-sent `SmQuestionWindow` intent only when request registration succeeded.
- The intent preserves Java `new SM_QUESTION_WINDOW(904306, 0, 0)` metadata: question id `904306`, sender id `0`, and range/cooldown `0`.
- Added a packet serialization assertion for `SmQuestionWindow.VortexDefenderInvitation` to pin the Java write order for the Vortex constructor.
- Scope remains guarded. It does not call `SendPacketAsync`, execute callbacks, mutate groups, mutate alliances, mutate defender maps, schedule, spawn, despawn, or start portals.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DefenderQuestionWindowIntentAdapter_CreatesNonSentPacketIntentOnlyAfterRegistration` | Unit | `Invasion.updateDefenders` source review | Successful registration creates non-sent Vortex question-window intent | Focused C# test validates recipient, question id `904306`, sender `0`, range/cooldown `0`, packet object code, and disabled live send flag | Does not send packet to a client |
| `DefenderQuestionWindowIntentAdapter_OmitsPacketIntentWhenRegistrationRejected` | Unit | `Invasion.updateDefenders` and `ResponseRequester.putRequest` source review | Rejected registration does not create packet intent | Focused C# test validates occupied request slot produces `NotCreated` intent and no packet object | Does not execute Java runtime |
| `GamePacketTests` Vortex `SmQuestionWindow` row | Unit | `SM_QUESTION_WINDOW.writeImpl` source review | C# packet writes id, three empty strings, unknown dword, range flag, sender, and range/cooldown like Java | Focused packet serialization assertion validates the Vortex `904306,0,0` payload shape | Not a Java-generated golden file |

## Validation Decision

- Changed surface: one C# opt-in packet-intent adapter plus focused Vortex service tests and an adjacent `SmQuestionWindow` packet serialization row.
- Specific behavior/contract: Java `Invasion.updateDefenders` sends `SM_QUESTION_WINDOW(904306, 0, 0)` only when `ResponseRequester.putRequest` succeeds; C# now creates a non-sent packet intent only after successful registration and preserves the Java packet payload shape for question id `904306`, sender id `0`, and range/cooldown `0`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

- Result: Passed, 391 tests. Existing nullable/analyzer warnings were emitted from unrelated files.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and the narrow source-of-truth behavior was reviewed directly from `SM_QUESTION_WINDOW.writeImpl`.
- Broad-validation trigger: none. This adapter is opt-in and does not send packets.
- Broad .NET decision: full project/solution validation skipped because the focused command covered the edited Vortex tests and adjacent packet serialization contract.
- Why this scope is sufficient: the changed surface is isolated to non-live packet intent metadata and `SmQuestionWindow` serialization; focused tests cover successful intent creation, rejected-registration omission, disabled live sending, and the exact Vortex question-window payload row.

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
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderQuestionWindowIntentAdapterService` | Runtime Vortex packet-intent adapter | Partial | Unit Tested | Partial Parity | C# now composes non-sent question-window intent only after registration success. Production start/update-defenders wiring and live packet dispatch remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` | `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow` | Server packet | Partial | Unit Tested | Partial Parity | C# Vortex row writes code `904306`, three empty string slots, unknown dword `0`, range flag `0`, sender `0`, and range/cooldown `0` per reviewed Java write order. Not yet Java golden-file verified. |

## Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 44%

## Remaining Risks

- Live defender update/start path is still not wired to production Vortex lifecycle.
- `SM_QUESTION_WINDOW` packet dispatch remains disabled.
- Vortex response-handler callback side effects remain metadata-only and do not mutate groups, alliances, or defender maps.
- Production world/location/alliance containers remain absent from the Vortex start path.
- Java packet golden/runtime comparison was not run; parity evidence is source review plus focused C# serialization tests.
