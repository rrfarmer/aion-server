# Phase 6 Session 2686 Completion

## UOW

[Phase 6] UOW-2686: Use nearest Java-style spawn for object search.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_OBJECT_SEARCH now resolves NPC map markers through the Java-style nearest/current-map and race-ordered fallback spawn search before sending SM_SHOW_NPC_ON_MAP.
- Java source/runtime path: CM_OBJECT_SEARCH.runImpl -> DataManager.SPAWNS_DATA.getNearestSpawnByNpcId(player, npcId, activePlayer.getWorldId()) -> SM_SHOW_NPC_ON_MAP.
- C# runtime artifact wired: GameServerConnection.HandleObjectSearchAsync now calls NpcSpawnTable.GetNearestSpawnByNpcId, with WorldMapSummary.WorldType loaded from world_maps.xml.
- Client-visible/state/persistence effect: live object-search packets can now point to the nearest NPC spawn on the player's current map, or to the first same-race fallback map before other maps, instead of the first loaded spawn.
- Why this is runtime progress: it changes a real in-game server packet path and its emitted map id/coordinates; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_OBJECT_SEARCH.java`
  - Reads `npcId`, gets the active player, calls `SpawnsData.getNearestSpawnByNpcId`, sends `SM_SHOW_NPC_ON_MAP` or `STR_FIND_POS_UNKNOWN_NAME`.
- `game-server/src/com/aionemu/gameserver/dataholders/SpawnsData.java`
  - Searches the current map first.
  - If missing, scans world maps of the player's race (`ELYSEA` for `ELYOS`, `ASMODAE` for `ASMODIANS`).
  - If still missing, scans remaining maps.
  - For current-map hits, selects the nearest spawn by distance and keeps the first spawn on equal distance.
  - For off-map hits, returns the first spawn from the selected map.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SHOW_NPC_ON_MAP.java`
  - Serializes NPC id, world id, instance id, and spawn coordinates.

## C# Changes

- Added `WorldMapSummary.WorldType` and loaded it from `world_maps.xml`.
- Added `NpcSpawnTable.GetNearestSpawnByNpcId`, mirroring Java's current-map, same-race-map, other-map search order and same-map nearest-distance selection.
- Wired `GameServerConnection.HandleObjectSearchAsync` to use the new nearest-spawn helper for live `CM_OBJECT_SEARCH`.
- Left `GetFirstSpawnByNpcId` unchanged for Java paths that still use `SpawnsData.getFirstSpawnByNpcId`, such as teleport-to-NPC requests.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GetNearestSpawnByNpcId_SearchesNearestSpawnOnPlayerWorld` | Unit / live dataholder contract | `SpawnsData.getNearestSpawnByNpcId` current-map nearest branch | Same-map object search chooses the nearest spawn instead of the first loaded spawn. | Exercises the runtime `NpcSpawnTable` helper now called by `HandleObjectSearchAsync`. | Does not serialize `SM_SHOW_NPC_ON_MAP`. |
| `GetNearestSpawnByNpcId_SearchesSameRaceWorldsBeforeOtherWorlds` | Unit / live dataholder contract | `SpawnsData.getNearestSpawnByNpcId` same-race fallback branch | Elyos players prefer `ELYSEA` fallback maps before `ASMODAE` maps. | Uses loaded-style `WorldMapSummary.WorldType` values and runtime spawn table ordering. | Does not cover Asmodian fallback separately. |
| `GetNearestSpawnByNpcId_WhenOffWorldUsesFirstSpawnWithoutDistanceSort` | Unit / live dataholder contract | `SpawnsData.getNearestSpawn` off-world branch | Off-current-map fallback returns the first spawn from the selected map without distance sorting. | Exercises the exact branch feeding live object-search packets. | Does not cover empty spawn-group internals because C# flattens spawn spots. |

## Validation Decision

```text
- Changed surface: world map static-data loading, runtime NPC spawn table lookup, live object-search packet handler.
- Specific behavior/contract: Java object search chooses current-map nearest spawn, same-race fallback maps, then other maps before SM_SHOW_NPC_ON_MAP.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerTeleportToNpcRequestServiceTests" --logger "console;verbosity=minimal"
- Focused Java/Maven command: not run; Java source was reviewed and no narrow Java unit fixture exists for this lookup in this checkout.
- Broad-validation trigger: none. The change is isolated to object-search spawn selection plus one static-data field already consumed by the touched helper.
- Broad .NET decision: skipped; the filtered test built the affected projects and exercised both existing first-spawn behavior and the new nearest-spawn contract.
- Why this scope is sufficient: the focused tests prove the runtime selection behavior that determines `SM_SHOW_NPC_ON_MAP` map id and coordinates, while adjacent teleport tests ensure the old first-spawn path remains intact.
```

Result:

- Focused C# validation passed: 10/10.
- Existing unrelated nullable/analyzer warnings were emitted by the C# projects.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_OBJECT_SEARCH.runImpl` | `GameServerConnection.HandleObjectSearchAsync` | Live client packet handler | Partial | Unit Tested indirectly | Partial Parity | Handler now uses Java-style nearest spawn lookup and still sends unknown-name message `1300747` when no spawn exists. Full packet serialization comparison was not run. |
| `SpawnsData.getNearestSpawnByNpcId` | `NpcSpawnTable.GetNearestSpawnByNpcId` | Runtime data lookup | Partial | Unit Tested | Partial Parity | Current-map nearest, same-race fallback, other-map fallback, and off-world first-spawn behavior are covered. C# flattened spawn spots do not model Java `SpawnGroup` boundaries exactly. |
| `WorldMapTemplate.worldType` | `WorldMapSummary.WorldType` | Static data | Partial | Unit Tested indirectly | Partial Parity | `world_type` is loaded from Java XML and consumed by object-search fallback ordering. Other consumers are not claimed. |
| `SM_SHOW_NPC_ON_MAP` | `SmShowNpcOnMap` | Server packet | Partial | Existing packet implementation reviewed | Needs Verification | This UOW changes the coordinates supplied to the live packet; byte-for-byte packet serialization was not newly tested. |

## Known Gaps

- Complete `SpawnsData` parity is not claimed.
- C# spawn summaries are flattened, so exact Java `SpawnGroup` boundary behavior is only approximated by load order for off-map first-spawn selection.
- Asmodian same-race fallback ordering is not separately covered.
- No Java runtime comparison or golden packet capture was run.
- `SM_SHOW_NPC_ON_MAP` serialization was reviewed but not newly tested in this UOW.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Add a focused live object-search connection test that serializes `SM_SHOW_NPC_ON_MAP` only if paired with another runtime packet-path fix, not as standalone test-only work.
2. Move to a deferred item-use, inventory, quest, AI, zone, command, or dynamic handler path where Java source contains a concrete live mutation/packet and C# has enough runtime infrastructure to wire it.
3. Revisit Passport request-map iteration only if a concrete packet/state/persistence ordering gap is confirmed beyond audit/logging.

Post-UOW discovery note:

- `CM_INSTANCE_LEAVE` and `CM_STOP_TRAINING` were checked after this UOW. In this Java source, both delegate to instance handler methods, but `GeneralInstanceHandler.leaveInstance` and `GeneralInstanceHandler.onStopTraining` are empty and no overrides were found under `game-server/src/com/aionemu/gameserver/instance/handlers`. Wiring a C# no-op would not pass the Runtime Progress Gate.
