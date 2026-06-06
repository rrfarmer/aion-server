# Phase 6 Session 2725 Handoff

## Completed UOW

[Phase 6] UOW-2725: Use legion level for live legion warehouse size packets.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live storage-3 SM_CUBE_UPDATE packets now use Java's legion warehouse expansion value instead of hard-coded zero.
- Java source/runtime path: SM_CUBE_UPDATE.cubeSize(StorageType.LEGION_WAREHOUSE, Player) -> Legion.getWarehouseExpansions() -> getLegionLevel() - 1.
- C# runtime artifact wired: Player.LegionWarehouseExpansions and GameServerConnection.CreateStorageSizePacket storage type 3.
- Client-visible/state/persistence effect: live move/split packet fanout involving legion warehouse now reports expansion bytes derived from the player's loaded legion level.
- Why this is runtime progress: it changes real server packet payloads emitted by live inventory handlers.
```

## Commit

`[Phase 6][UOW-2725] Use legion level for warehouse size packets`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2725-Completion.md`
- `docs/Phase-6-Session-2725-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`

## C# Artifacts Touched

- `Aion.GameServer.Model.GameObjects.Player.LegionWarehouseExpansions`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateStorageSizePacket`
- `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.LegionWarehouseSizeSnapshot` as the consumed packet helper

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceMovesItemToCubeHistoryLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitsItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceSplitsItemToCubeHistoryLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 4
- Failed: 0
- Skipped: 0
- Existing warnings only.

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched files.

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for this packet branch in this checkout.

Broad .NET:

- Not run. Broad-validation trigger was live packet payload behavior, but the focused command built the affected project and decoded the edited storage-3 packet field from live move/split dispatch.

## Conservative Parity Status

- `SM_CUBE_UPDATE.cubeSize(LEGION_WAREHOUSE)` has partial runtime parity for the expansion byte in live move/split fanout.
- `Legion.getWarehouseExpansions` is represented as a derived C# property using loaded `LegionLevel`.
- Full legion warehouse parity is not claimed because C# still lacks Java's full `LegionWarehouse` aggregate, slot-limit model, open/in-use behavior, and direct Java packet golden evidence.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `SM_CUBE_UPDATE.cubeSize` | `GameServerConnection.CreateStorageSizePacket` / `SmCubeUpdate.LegionWarehouseSizeSnapshot` | Server packet fanout | Partial | Unit Tested | Partial Parity | Storage-3 live fanout now writes Java's level-derived expansion value. |
| `Legion.getWarehouseExpansions` | `Player.LegionWarehouseExpansions` | Model projection | Partial | Unit Tested indirectly | Partial Parity | Uses loaded `LegionLevel - 1`; C# clamps negative values to zero for safe no-legion/default state. |
| `LegionWarehouse.updateLimit` | No full C# equivalent | Runtime storage model | Not Started | No Tests | Unknown | Slot-limit/capacity parity remains missing beyond packet expansion value. |

## Known Gaps / Watchouts

- Direct Java golden bytes were not captured for `SM_CUBE_UPDATE`.
- C# has no complete Java `Legion`/`LegionWarehouse` aggregate yet.
- Storage capacity enforcement for legion warehouse still needs Java-driven discovery.
- Live `OPEN_LEGION_WAREHOUSE` packets remain not fully wired.
- Existing non-live plan/readiness services around legion warehouse should be ignored unless they are paired with live runtime wiring.

## Next Recommended Runtime UOW

Recommended candidate: wire live `OPEN_LEGION_WAREHOUSE` handling if discovery confirms the current C# dialog dispatch has enough runtime state to send the Java packet sequence.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: selecting an NPC's OPEN_LEGION_WAREHOUSE action should send the live legion warehouse packet sequence instead of only producing a non-live plan.
- Java source/runtime path: DialogService.onDialogSelect OPEN_LEGION_WAREHOUSE -> LegionService.openLegionWarehouse -> LegionWhUpdate -> SM_LEGION_EDIT(0x04) -> SM_WAREHOUSE_INFO chunks for storage 3 -> SM_DIALOG_WINDOW(DialogPage.LEGION_WAREHOUSE).
- C# runtime artifact likely involved: GameServerConnection.HandleDialogSelectAsync, NpcDialogServiceSelectPlanService boundary, SmLegionEdit, SmWarehouseInfo or a legion-warehouse update helper, SmDialogWindow, player inventory items with Location 3.
- Client-visible/state/persistence effect expected: a real client selecting the legion warehouse dialog action receives the warehouse kinah, item list/update, and dialog page packets from live code.
- Why this is runtime progress: it wires a deferred live client packet path and sends real server packets.
```

Suggested discovery:

```powershell
rg -n "OPEN_LEGION_WAREHOUSE|OpenLegionWarehouse|LegionWarehouseOpen|HandleDialogSelectAsync|SmLegionEdit|SmWarehouseInfo|SmDialogWindow|DialogPage" game-server/src/com/aionemu/gameserver dotnetConversion/src/Aion.GameServer dotnetConversion/tests/Aion.GameServer.Tests
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"
```

Narrow this after discovery to the exact live dialog test(s) and packet helper tests touched. Java/Maven is not expected unless a narrow Java packet fixture is found. Broad-validation trigger: live dialog packet dispatch; start focused and do not run broad .NET unless focused evidence exposes wider risk.

## Other Safe Runtime Candidates

- Model legion warehouse in-use state if open/close/logout Java paths can be represented in one runtime UOW.
- Add legion warehouse capacity checks to live move/split paths if Java `LegionWarehouse.updateLimit` reveals a concrete missing blocker.
- Extend `CM_LEGION_HISTORY` reward/activity paths only when paired with live row loading and packet send coverage.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `cbf4238b1 [Phase 6][UOW-2724] Persist legion warehouse item history`
  - `1d358d9f8 [Phase 6][UOW-2723] Persist legion warehouse history`
  - `bd4c0b1df [Phase 6][UOW-2722] Withdraw kinah from legion warehouse`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
