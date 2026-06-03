# Phase 6 Session 2517 Completion

## UOW

[Phase 6] UOW-2517: Add VortexDefenderAcceptanceInputResolverService for production wiring

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `com.aionemu.gameserver.services.vortex.Invasion` — `defenders` map and `defAlliance` field
- `com.aionemu.gameserver.services.vortex.Invasion.addPlayer(player, false)` — alliance created when exactly 1 prior defender exists; `defenders.put` follows
- `com.aionemu.gameserver.model.team.alliance.PlayerAlliance.isFull()` — max 24 members

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAddPlayerTransitionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexDefenderAcceptanceInputResolverService` with a single `Resolve(VortexInvasionSnapshot?, Func<int, Player?>)` method.
- Maps `DefenderObjectIds` from the snapshot to `VortexDefenderAddPlayerSnapshot.FromPlayer(player)` for each resolved player; falls back to `IsInGroup: false, IsInAlliance: false` when the world lookup returns null.
- Approximates `VortexDefenderAllianceSnapshot` from defender count: `Missing` for 0 or 1 defenders (Java only creates `defAlliance` on the 2nd defender joining), `Full` for ≥ 24 (Java max `PlayerAlliance` size), `Open` otherwise. This is documented as a partial-parity approximation — the true alliance state requires tracking `defAlliance` in the C# runtime.
- Added `VortexDefenderAcceptanceInputs` result record carrying `LocationId`, `ExistingDefenders`, `DefenderAlliance`, `DefenderCount`, `HasExistingDefenders`, and `JavaSource`.
- `VortexDefenderAcceptanceInputResolverService.DefenderAllianceMaxSize = 24` is a named constant matching Java `PlayerAlliance` max.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DefenderAcceptanceInputResolver_DerivesExistingDefenderSnapshotsFromRuntimeSnapshot` | Unit | `Invasion.defenders` map + `VortexDefenderAddPlayerSnapshot.FromPlayer` | Two-defender runtime snapshot resolves both player snapshots with correct `IsInGroup`/`IsInAlliance` flags and derives `Open` alliance state | Focused C# unit test validates snapshot derivation, group/alliance flags, and alliance-state approximation | Alliance state is approximated from count, not from live `defAlliance` reference |
| `DefenderAcceptanceInputResolver_ReturnsMissingAllianceForZeroOrOneDefender` | Unit | `Invasion.addPlayer` — defAlliance not created until 2nd defender | Null snapshot returns empty inputs; single-defender snapshot returns `Missing` alliance | Focused C# unit test validates the Java rule that alliance is absent before 2nd defender | Same approximation caveat |

## Validation Decision

- Changed surface: new non-live input resolver service + tests.
- Specific behavior/contract: Java `Invasion.defenders.get(objectId)` → player state, and `defAlliance != null` / `isFull()` / `isDisbanded()` → alliance state.
- Focused C# command:

```powershell
dotnet test "dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj" --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~GameServerConnectionVortexQuestionResponseTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Result: Passed, 142 tests (140 prior + 2 new).
- Focused Java/Maven command: skipped; no Java source or fixtures changed.
- Broad-validation trigger: none. New service is read-only; no live mutation.
- Broad .NET decision: skipped.
- Why this scope is sufficient: tests cover two-defender snapshot derivation, group/alliance flags, absent-player fallback, null-snapshot guard, and the 0/1-defender → `Missing` alliance approximation.

## Additional Hygiene

```powershell
git diff --check
```

- Passed. Expected CRLF conversion warnings for edited C# source.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `Invasion.defenders` (read path) | `VortexDefenderAcceptanceInputResolverService.Resolve` | Read-only input collector | Partial | Unit Tested | Partial Parity | C# maps snapshot ids to player snapshots via world lookup. Absent-player fallback returns IsInGroup/IsInAlliance false. |
| `Invasion.defAlliance` (state read) | `VortexDefenderAcceptanceInputs.DefenderAlliance` | Alliance state approximation | Partial | Unit Tested | Partial Parity | Alliance state derived from defender count, not live alliance reference. Missing for 0/1 defenders, Open for 2-23, Full for 24+. Disbandment not modeled. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 45%

## Remaining Risks

- `defAlliance` disbandment state is not modeled; `Disbanded` snapshot can only come from explicitly supplied state.
- Alliance `Full` approximation assumes all 24 slots are used; Java checks `PlayerAlliance.isFull()` dynamically.
- Absent-player fallback (not found in world) assumes not-in-group and not-in-alliance; could differ from actual departed player state.
