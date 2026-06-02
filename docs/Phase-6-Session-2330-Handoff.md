# Phase 6 Session 2330 Handoff - Spawn Fresh Portal Instances

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2330-Completion.md`
- `docs/Phase-6-Session-2330-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger and should not be used as reassurance checks.

## Current State

Last completed UOW: UOW-2330, fresh portal allocation now invokes the C# spawn-instance bridge when static data and `WorldNpcSpawnService` are available.

Relevant completed portal/instance/spawn slices:

- Fresh group, alliance, league, and player-object portal continuation can allocate the next runtime instance, register the relevant team/player id, transfer the current player, apply cooldown, and preserve nonzero difficulty metadata.
- Group bypass and no-group bypass paths can allocate through the player object id path without team registration.
- `WorldNpcSpawnService.SpawnWorldNpcsForInstance(...)` filters nonzero spawn `DifficultId` by the runtime instance difficulty and places spawned NPCs into the runtime instance id.
- Fresh portal allocation calls `SpawnWorldNpcsForInstance(...)`; a focused test proves difficulty `2` plus default spawns appear in allocated instance id `2` while difficulty `1` is skipped.

Still not proven or not implemented:

- Java static door spawning, housing spawning, event spawns, walker organization, temporary spawn scheduling, non-NPC object side effects, and full object materialization.
- Java `InstanceHandler.onInstanceCreate()` and empty-instance checker behavior.
- Alliance/league live fanout.
- League leave/disband snapshot clearing for every Java lifecycle branch.
- Java range observer auto-deny behavior for AI requests.
- Real-client/encrypted socket bytes for these portal branches.

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
- `b8c7c528b [Phase 6][UOW-2329] Add spawn instance difficulty bridge`
- `[Phase 6][UOW-2330] Spawn fresh portal instances`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `docs/Phase-6-Session-2330-Completion.md`
- `docs/Phase-6-Session-2330-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` was already dirty and should remain unstaged unless the user explicitly asks to edit it.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` normal spawn call | `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalTeamContinueTransferAsync` plus `Aion.GameServer.Services.WorldNpcSpawnService.SpawnWorldNpcsForInstance` | Service Boundary | Partial | Unit Tested | Partial Parity | Fresh C# portal allocation now invokes the spawn bridge when static data and spawn service are available. Java static doors, event spawns, housing, walkers, temporary spawn scheduling, full object materialization, `onInstanceCreate`, and empty-instance task side effects remain partial or missing. |
| `com.aionemu.gameserver.spawnengine.SpawnEngine.spawnInstance` difficulty filter and instance placement | `Aion.GameServer.Services.WorldNpcSpawnService.SpawnWorldNpcsForInstance` | Service | Partial | Unit Tested | Partial Parity | Focused tests prove matching/default NPC spawn groups are placed in the allocated instance id and nonmatching nonzero difficulty groups are skipped. Static doors, housing, event, walker, temporary, and non-NPC object side effects remain gaps. |
| `com.aionemu.gameserver.network.aion.GameConnectionListener` connection construction dependency flow | `Aion.GameServer.Network.Aion.GameClientSocketServer` to `GameServerConnection` | Service Boundary | Partial | Unit Tested Through Connection Helper | Needs Verification | Optional spawn service is forwarded to per-client connections. Hosted socket startup was not broadly tested; constructor shape was kept source-compatible by appending the optional parameter. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_FreshGroupAllocationSpawnsDifficultyFilteredNpcsLikeJavaInstanceService|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance" --no-restore
```

Result: passed 3, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

First focused run failed at compile because the new test was missing `Aion.GameServer.Utils.IdFactory`; the import was added and the same focused command passed.

Hygiene:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this runtime branch. Java source review was used as source-of-truth evidence.

Broad-validation trigger: live side-effect enabling in the portal allocation path.

Broad .NET decision: skipped full project/solution validation because the changed side effect was isolated behind an optional injected service and directly covered by focused connection/spawn tests. No shared packet primitive, serialization helper, persistence schema, scheduler primitive, or startup lifecycle contract was changed.

## Next Sequential UOW

Recommended next production scope: continue Java `SpawnEngine.spawnInstance(...)` side effects beyond basic NPC spawning, choosing one small side effect with available C# infrastructure.

Best first candidate: static door instance spawning.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/spawnengine/SpawnEngine.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/StaticDoorSpawnManager.java`
- Java door/static-door dataholders and world object registration paths reached by `StaticDoorSpawnManager.spawnTemplate(instance)`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- Existing C# static door/placeable services and dataholders, if present
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs` only if allocation needs another call site hook
- Focused tests under `dotnetConversion/tests/Aion.GameServer.Tests`

Specific behavior to prove: when Java would call `StaticDoorSpawnManager.spawnTemplate(instance)` as part of normal `SpawnEngine.spawnInstance(...)`, the C# fresh instance path applies the equivalent door/static-object state for the allocated instance id, or documents a missing service/data blocker if no C# equivalent exists.

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_FreshGroupAllocationSpawnsDifficultyFilteredNpcsLikeJavaInstanceService" --no-restore
```

Narrow this further after Work Discovery to the exact new/edited static-door test name plus the fresh allocation-spawn test. Do not run a full project/solution test unless the active notes name a broad-validation trigger.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence.

Broad-validation trigger: none expected for discovery or a narrow non-live static-door planner/service test. If the next UOW enables additional live side effects, mutates shared world state broadly, or changes hosted startup/DI lifecycle, document that trigger before any broad validation decision.

## Safe Candidates

- Static door instance spawning parity for the normal `SpawnEngine.spawnInstance(...)` branch.
- Event-specific instance spawn branch for Java `spawnEventSpawns(...)` if matching C# event/spawn data exists.
- Java `InstanceHandler.onInstanceCreate()` bridge/planner if C# instance-handler infrastructure is already present.
- League leave/disband snapshot clearing for Java `PlayerAlliance.setLeague(null)` lifecycle branches.

Avoid:

- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Evidence/reporting-only units.
- Beshmundir-only shortcuts that bypass Java `PortalService.port(...)`.
- Updating `docs/PHASE-6-PROGRESS.md`.
