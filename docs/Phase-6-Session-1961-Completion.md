# Phase 6 Session 1961 Completion - Repurchase Cube-Size Packet Intent Diagnostic

Date: 2026-06-01
Unit of Work: UOW-1961
Status: Completed

## Scope

- Added a disabled cube-size packet-intent diagnostic for Java `RepurchaseService.repurchaseFromShop` success paths that add a new cube item.
- Kept the work diagnostic-only and non-live, without sending packets or mutating inventory, Kinah, cube counters, repurchase singleton state, or persistence.

## Work Discovery

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, Session 1960 completion, and Session 1960 handoff.
- Inspected Java `ItemPacketService.sendItemPacket`, `sendItemDeletePacket`, `sendItemUpdatePacket`, and `sendStorageUpdatePacket`.
- Inspected Java `SM_CUBE_UPDATE.cubeSize`.
- Inspected Java `Storage.decreaseItemCount`, `Storage.add`, and `Storage.delete`.
- Inspected Java `RepurchaseService.repurchaseFromShop`.
- Inspected C# `SmCubeUpdate`, `RepurchaseOutcomePlanService`, and existing repurchase/CM_BUY_ITEM/cube packet tests.

## Changes

- Added `RepurchasePacketIntentKind.SendCubeSizeUpdate`.
- Successful disabled repurchase outcomes now record a cube-size packet intent after each new cube item add diagnostic.
- The cube-size intent uses `SmCubeUpdate.PacketOpCode` as a marker because this non-live planner does not yet construct a concrete post-mutation cube-size payload.
- Stack-merge repurchase outcomes explicitly omit cube-size update intents, matching Java `sendItemUpdatePacket`.
- Inventory-full repurchase outcomes still record only `STR_MSG_DICE_INVEN_ERROR`; Kinah decrease still records only `DEC_KINAH_BUY`.
- Extended repurchase tests to assert the new add-followed-by-cube intent and the absence of cube-size intent for stack merges.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~SmInventory|FullyQualifiedName~SmCube" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest,CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `mvn test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# repurchase/inventory/cube slice passed with 167 tests.
- Focused Java repurchase packet/parser tests passed with 11 test methods.
- Broad C# game-server suite passed with 5008 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 23 game-server tests.
- Full Maven reactor passed in the current workspace. This run did not force a clean login-server recompile.

## Known Gaps

- Packet intents are disabled and informational only.
- No live `SM_CUBE_UPDATE`, `SM_INVENTORY_UPDATE_ITEM`, `SM_INVENTORY_ADD_ITEM`, or system-message dispatch is performed.
- The cube-size intent is an opcode marker only; exact post-mutation cube item count and expansion fields are not captured here.
- Java storage/item side effects such as quest callbacks, warehouse branches, delete-path cube-size updates, unusual pet-feed storage capture, persistent-state flags, and deleted-item queues remain unmodeled in this repurchase slice.
- Java `HashSet` bucket iteration order and returned set mutability are not emulated.
- Live BUY_AGAIN socket dispatch, live `CM_BUY_ITEM` repurchase execution, persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/RepurchasePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchasePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1961-Completion.md`
- `docs/Phase-6-Session-1961-Handoff.md`

## Parity Position

- Partial Parity for disabled repurchase cube-size packet-intent diagnostics.
- The implementation is source-reviewed and C# unit-tested, with Java packet/parser tests rerun, but live Java/C# packet construction, cube-size payload contents, socket ordering, storage mutation, transaction behavior, concurrency, and complete repurchase behavior remain unverified.
