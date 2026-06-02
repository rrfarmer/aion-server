# Phase 6 Session 2329 Handoff - Spawn Instance Difficulty Bridge

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2329-Completion.md`
- `docs/Phase-6-Session-2329-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2329, narrow C# bridge for Java `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)`.

Completed portal/instance/spawn slices relevant to the next work:

- Fresh group, alliance, and league portal continuation allocates the next runtime instance, registers the team id when Java does, transfers the player, applies cooldown, and preserves nonzero difficulty metadata.
- Bypassed, ungrouped, group-sized and alliance-sized portal entry can allocate/transfer through the player object id path without team registration.
- Alliance-sized portal validation uses alliance id, league id when present, or player object id when group requirement is bypassed and no alliance exists.
- `WorldNpcSpawnService` already filters spawn groups by nonzero `DifficultId`.
- `WorldNpcSpawnService.SpawnWorldNpcsForInstance(...)` now filters by the runtime instance difficulty id and places spawned NPCs into the runtime instance id.

Still not proven:

- Fresh instance allocation invoking the spawn bridge.
- Java static door spawning, housing spawning, event spawns, walker organization, and full object materialization.
- Alliance/league live fanout.
- League leave/disband snapshot clearing for every Java lifecycle branch.
- Java range observer auto-deny behavior for AI requests.
- Real-client/encrypted socket bytes for these branches.

## Commits Made

- `c12d00c4c [Phase 6][UOW-2315] Reject solo Beshmundir walk entry`
- `10b528b55 [Phase 6][UOW-2316] Show Beshmundir leader path dialog`
- `ff1354d09 [Phase 6][UOW-2317] Reject Beshmundir non-leader closed entry`
- `7a644b31f [Phase 6][UOW-2318] Register Beshmundir difficulty request`
- `c95ad6c8b [Phase 6][UOW-2319] Transfer registered group portals`
- `9d49df0a2 [Phase 6][UOW-2320] Move Beshmundir non-leader into open instance`
- `03fd0b3fa [Phase 6][UOW-2321] Move Beshmundir accepted difficulty response`
- `dd42ddc77 [Phase 6][UOW-2322] Allocate fresh group portal instances`
- `cc83107b0 [Phase 6][UOW-2323] Prove Beshmundir fresh group allocation`
- `f449bcee2 [Phase 6][UOW-2324] Carry portal difficulty into allocation`
- `db322f7cb [Phase 6][UOW-2325] Reuse group member solo portal instance`
- `888432ded [Phase 6][UOW-2326] Wire portal group bypass thresholds`
- `fb273cd01 [Phase 6][UOW-2327] Allocate no-group bypass portal instances`
- `b3ad8b8e9 [Phase 6][UOW-2328] Allocate alliance portal instances`
- `[Phase 6][UOW-2329] Add spawn instance difficulty bridge`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSpawnServiceTests.cs`
- `docs/Phase-6-Session-2329-Completion.md`
- `docs/Phase-6-Session-2329-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.spawnengine.SpawnEngine.spawnInstance` difficulty filter | `Aion.GameServer.Services.WorldNpcSpawnService.SpawnWorldNpcsForInstance` | Service | Partial | Unit Tested | Partial Parity | C# now filters instance spawns by nonzero difficulty id and uses the target instance id. Java static door, housing, event, walker, and object-type side effects remain partial. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` spawn call | C# portal/world allocation paths plus `WorldNpcSpawnService.SpawnWorldNpcsForInstance` | Service Boundary | Partial | No Direct Allocation Test | Needs Verification | The C# spawn bridge exists, but allocation paths do not yet invoke it. |

## Validation From Last UOW

Focused C# spawn validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcs_FiltersByDifficultId|FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Hygiene:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this runtime spawn branch. Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation after focused tests passed and supplied the compile signal for the affected project/dependencies.

## Next Sequential UOW

Recommended next production scope: wire fresh instance allocation to invoke `WorldNpcSpawnService.SpawnWorldNpcsForInstance(...)`.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/SpawnEngine.java`
- Portal allocation callers that invoke `InstanceService.getNextAvailableInstance(...)`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerRuntimeContext.cs` or constructor/dependency wiring as needed
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSpawnServiceTests.cs`

Specific behavior to prove: when a fresh portal instance is allocated with nonzero difficulty id and static spawn data is available, the C# allocation path invokes the spawn bridge so matching/default NPC spawns are materialized in the allocated instance id.

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers" --no-restore
```

Add exact new allocation-spawn test names after implementation. If dependency injection or hosted runtime startup is changed, document the broad-validation trigger before any full project/solution validation.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence.

Broad-validation trigger: none expected if the next UOW only adds a nullable spawn-service dependency and focused allocation tests. If service registration or startup lifecycle changes, name the trigger first.

## Safe Candidates

- Wire fresh portal allocation to invoke `WorldNpcSpawnService.SpawnWorldNpcsForInstance(...)`.
- Add targeted league leave/disband snapshot clearing for Java `PlayerAlliance.setLeague(null)` lifecycle branches.
- Continue documenting real-client/encrypted socket gaps only when tied to a concrete runtime slice.

Avoid:

- Full .NET project tests or solution builds without a documented broad-validation trigger.
- Evidence/reporting-only units.
- Beshmundir-only teleport shortcuts that bypass Java `PortalService.port(...)`.
- Updating `docs/PHASE-6-PROGRESS.md`.
