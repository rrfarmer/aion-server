# Phase 6 Session 1799 Completion - Add CM_CRAFT Runtime Plan Foundation

Date: 2026-05-31
Unit of Work: UOW-1799
Status: Complete

## Scope

Port the missing Java `CM_CRAFT` client packet surface and the narrow non-live `CM_CRAFT.runImpl` guard-planning boundary without widening into live dispatch, `CraftService.startCrafting`, or scheduler work.

## Completed Work

- Added `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmCraft.cs`:
  - mirrors Java `CM_CRAFT.readImpl`
  - reads `unk`, `targetTemplateId`, `recipeId`, `targetObjId`, `materialsCount`, `craftType`, and repeated material `(itemId, count)` pairs
  - preserves material quantities as `long`
- Updated `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`:
  - registers opcode `141` as `InGame` only for `CmCraft`
- Added `dotnetConversion/src/Aion.GameServer/Services/CmCraftRuntimePlanService.cs`:
  - models Java `CM_CRAFT.runImpl` pre-`CraftService.startCrafting` guard flow as a deterministic non-live planner
  - preserves the silent missing-player and unspawned-player return
  - preserves the silent shutdown-soon return before target validation
  - preserves the Java non-morph static-target validation gate
  - preserves the Java morph-substance bypass when `unk == 129`
  - carries recipe id, target object id, craft type, and materials into a start intent without mutation
- Added `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftTests.cs`:
  - verifies opcode `141` registration and state gating
  - verifies direct Java-shaped packet parsing order and material-pair payloads
- Added `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftRuntimePlanServiceTests.cs`:
  - verifies silent player/unspawned guard behavior
  - verifies shutdown guard precedence
  - verifies non-morph target-validation failures
  - verifies morph-substance target-bypass behavior
  - verifies start-intent payload preservation

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraftTests|FullyQualifiedName~CmCraftRuntimePlanServiceTests|FullyQualifiedName~GamePacketTests"`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`
- `dotnet test dotnetConversion\AionServer.slnx --no-restore`

Result:

- Focused `CM_CRAFT` packet/planner validation passed with 250 tests.
- The first full-suite attempt hit the command timeout boundary before a final result was produced.
- The second full-suite rerun passed cleanly with 4807 total tests:
  - `57` commons
  - `29` chat
  - `121` login
  - `4600` game

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_CRAFT`
- `com.aionemu.gameserver.services.craft.CraftService`
- `com.aionemu.gameserver.skillengine.task.CraftingTask`
- `com.aionemu.gameserver.skillengine.task.AbstractCraftTask`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`

## Migration Parity Table - UOW-1799

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_CRAFT.readImpl` | `CmCraft` | Packet / Parsing | Complete | Regression Tested | Verified Parity | Java field order, repeated material-pair parsing, and quantity width are directly represented and tested. |
| `AionClientPacketFactory` `CM_CRAFT` registration | `GameClientPacketFactory` opcode `141` registration | Packet Factory / Registration | Complete | Regression Tested | Verified Parity | `CmCraft` is now registered at opcode `141` as `InGame` only, matching the reviewed Java entry. |
| `CM_CRAFT.runImpl` pre-`CraftService.startCrafting` guard flow | `CmCraftRuntimePlanService.CreatePlan` | Deterministic Runtime Planner | Partial | Unit Tested | Partial Parity | Silent player/shutdown/target-validation branches and the morph bypass are represented, but no live dispatch or `CraftService.startCrafting` call exists yet. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `TryCreatePacket_RegistersJavaCraftOpcodeAsInGameOnly` | Opcode `141` resolves to `CmCraft` only for in-game connections. | Java `AionClientPacketFactory` `CM_CRAFT` entry | Regression | Does not prove live dispatch. |
| `ReadFrom_ReadsJavaCraftFieldsAndMaterials` | The packet parser preserves Java field order and repeated material pairs. | Java `CM_CRAFT.readImpl` | Regression | No runtime behavior. |
| `CreatePlan_ReturnsNoPlayerOrNotSpawnedWhenPlayerMissingOrUnspawned` | Missing or unspawned player returns a silent no-op plan. | Java `CM_CRAFT.runImpl` opening guard | Unit | No connection wiring. |
| `CreatePlan_ReturnsShuttingDownSoonBeforeTargetValidation` | Shutdown-soon short-circuits before any target validation. | Java `CM_CRAFT.runImpl` shutdown guard | Unit | No live server-state integration. |
| `CreatePlan_ReturnsInvalidNonMorphTargetForJavaStaticObjectGuardFailures` | Non-morph requests fail silently when static-target checks fail. | Java `CM_CRAFT.runImpl` target validation | Unit | No live world lookup. |
| `CreatePlan_AllowsMorphMarkerToBypassStaticTargetChecks` | Morph-substance requests bypass the static-target gate when `unk == 129`. | Java `CM_CRAFT.runImpl` morph branch | Unit | No live morph crafting flow. |
| `CreatePlan_StartCraftingPreservesRecipeTargetCraftTypeAndMaterials` | The start intent preserves recipe, target object id, craft type, and materials unchanged. | Java `CM_CRAFT.runImpl` call arguments | Unit | No downstream `CraftService.startCrafting` parity yet. |

## Risks / Gaps

- C# still lacks live `CmCraft` dispatch in `GameServerConnection`.
- The larger Java `CraftService.startCrafting` guard, material-consumption, DP-spend, cooldown, and scheduler path remains unported.
- Java silent-return behavior is modeled only in a planner, so live side effects and packet ordering remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped rows in this unit.
- Total artifacts ported: 1 craft client packet model, 1 packet-factory registration, 1 deterministic runtime planner, and 7 focused tests/regressions.
- Total artifacts with verified parity: 2 grouped rows.
- Total artifacts needing verification: 1 grouped row.
- Total blocked artifacts: live `CmCraft` dispatch and the broader `CraftService.startCrafting` / `CraftingTask` runtime shell.
- Estimated overall migration completion: Phase 6 remains about 73%.

## Next Recommended Unit of Work

- Port the narrow live `CmCraft` dispatch shell in `GameServerConnection`, consuming `CmCraftRuntimePlanService` and preserving the silent Java guard behavior before widening into `CraftService.startCrafting`.
- Safe alternatives if a different isolated slice is preferred:
  - execute the opt-in MySQL logout delete/retuning persistence path in an environment with `AION_GAMESERVER_DB_INTEGRATION=1`
  - `DropRegistrationService.calculateBoostDropRate`
  - return to the deferred `TemperingEffect.apply/endEffect` ownership surface only if a narrower deterministic slice becomes obvious

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmCraft.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmCraftRuntimePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftRuntimePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1799-Completion.md`
- `docs/Phase-6-Session-1799-Handoff.md`
