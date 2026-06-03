# Phase 6 Session 2518 Completion

## UOW

[Phase 6] UOW-2518: Wire VortexDefenderAcceptanceInputResolverService into GameServerConnection

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `com.aionemu.gameserver.services.vortex.Invasion.defenders` and `defAlliance` — state read by acceptance handler
- `com.aionemu.gameserver.network.aion.clientpackets.CM_QUESTION_RESPONSE.runImpl` — connection-level dispatch

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionVortexQuestionResponseTests.cs`

## Implementation Notes

- Added `_worldPlayerLookup` field (`Func<int, Player?>?`) and constructor parameter `worldPlayerLookup` to `GameServerConnection`.
- Updated `HandleVortexDefenderInvitationQuestionResponse` to call `VortexDefenderAcceptanceInputResolverService.Resolve(snapshot, _worldPlayerLookup)` when both `_vortexInvasionRuntime` and `_worldPlayerLookup` are injected:
  - Looks up snapshot via `FindDefenderLocationId` → `GetSnapshot(locationId)`
  - Passes `resolvedInputs.ExistingDefenders` and `resolvedInputs.DefenderAlliance` to `VortexDefenderAcceptanceRuntimeObserverService.Observe`
  - Falls back to `existingDefenders: null` and `defenderAlliance: null` (Missing) when runtime/lookup are absent
- Updated `TestConnectionPair.CreateAsync` to accept optional `worldPlayerLookup` parameter.
- Test design note: when the responder is already registered in the runtime defenders map (needed for `FindDefenderLocationId` to resolve), the resolver includes them in `existingDefenders`, causing the participant report to return `AlreadyParticipant`. This is the correct behavior — Java's `updateDefenders` guard (checking `defenders.containsKey`) prevents already-registered defenders from receiving a new invitation, so this scenario reflects an edge case.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleQuestionResponseAsync_VortexDefenderAcceptanceObserverUsesResolvedDefenderInputsWhenRuntimeAndLookupAvailable` | Integration | `Invasion.defenders` read + `VortexDefenderAcceptanceInputResolverService` | Connection resolves real defender snapshots via runtime + world lookup; resolver inputs reach the observer report; already-registered-defender scenario returns `AlreadyParticipant` | Integration test validates `locationId`, `AlreadyParticipant` status, resolver-derived `before` ids contain both registered defenders, and `ShouldMutateLiveDefenders = false` | Java guard prevents double-invitation; this test exercises an edge case |

## Validation Decision

- Changed surface: `GameServerConnection` wiring update + integration test.
- Specific behavior/contract: Java `Invasion.defenders` map read provides `existingDefenders` for `addPlayer` context; C# resolver now derives this from runtime snapshot and player lookup.
- Focused C# command:

```powershell
dotnet test "dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj" --filter "FullyQualifiedName~GameServerConnectionVortexQuestionResponseTests|FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Result: Passed, 143 tests (142 prior + 1 new).
- Focused Java/Maven command: skipped; no Java source or fixtures changed.
- Broad-validation trigger: none. Connection change is non-live; all mutation guards remain false.
- Broad .NET decision: skipped.
- Why this scope is sufficient: focused filter covers the edited connection integration tests and adjacent Vortex observer/response-registry contract.

## Additional Hygiene

```powershell
git diff --check
```

- Passed. Expected CRLF conversion warnings for edited C# source.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `Invasion.defenders` / `defAlliance` read in `acceptRequest` context | `GameServerConnection.HandleVortexDefenderInvitationQuestionResponse` + `VortexDefenderAcceptanceInputResolverService` | Connection dispatch with resolver | Partial | Integration Tested | Partial Parity | Resolver-derived inputs now flow through connection to acceptance observer; alliance state is still a count-based approximation. |

## Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 1 (connection wiring)
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 1
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 45%

## Remaining Risks

- The `AlreadyParticipant` edge case in the integration test highlights that in production, the Java guard (`defenders.containsKey`) prevents already-registered defenders from getting a second invitation; the C# path needs to respect this invariant.
- `worldPlayerLookup` must be injected in the production `GameServerConnection` constructor; not yet wired at startup.
- Alliance state remains a count-based approximation; live `defAlliance` tracking is needed for accurate Full/Disbanded states.
