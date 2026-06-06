# Phase 6 Session 2729 Handoff

## Completed UOW

[Phase 6] UOW-2729: Lock live legion warehouse opens.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: OPEN_LEGION_WAREHOUSE now claims live shared in-use state, blocks a different current opener with Java's in-use denial, and CM_CLOSE_DIALOG releases the lock for the owning legion member.
- Java source/runtime path: LegionWarehouse.setInUse/getCurrentUser/unsetInUse, LegionService.canOpenWarehouse, CM_CLOSE_DIALOG, and DialogService.onCloseDialog.
- C# runtime artifact wired: LegionWarehouseRuntime, GameServerRuntimeContext.LegionWarehouses, GameServerConnection.HandleOpenLegionWarehouseDialogAsync, GameServerConnection.HandleCloseDialog, and SmSystemMessage.GuildWarehouseInUse.
- Client-visible/state/persistence effect: first opener receives open packets, concurrent different same-legion opener receives `STR_GUILD_WAREHOUSE_IN_USE` id `1300280`, current opener can reopen, and close-dialog releases the runtime lock.
- Why this is runtime progress: it mutates shared live runtime state and sends a real server packet from live client packet paths.
```

## Commit

`[Phase 6][UOW-2729] Lock live legion warehouse opens`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/GameServerRuntimeContext.cs`
- `dotnetConversion/src/Aion.GameServer/Services/LegionWarehouseRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogSideEffectService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/LegionWarehouseRuntimeTests.cs`
- `docs/Phase-6-Session-2729-Completion.md`
- `docs/Phase-6-Session-2729-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_CLOSE_DIALOG.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Artifacts Touched

- `Aion.GameServer.Services.LegionWarehouseRuntime`
- `Aion.GameServer.Services.GameServerRuntimeContext`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleOpenLegionWarehouseDialogAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleCloseDialog`
- `Aion.GameServer.Services.NpcDialogCloseSideEffectPlan`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Tests.GameServerConnectionStorageExpansionDialogTests`
- `Aion.GameServer.Tests.LegionWarehouseRuntimeTests`
- `Aion.GameServer.Tests.GamePacketTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~NpcDialogSideEffectServiceTests|FullyQualifiedName~LegionWarehouseRuntimeTests" --logger "console;verbosity=minimal"
```

Result:

- Passed: 320
- Failed: 0
- Skipped: 0
- Existing nullable warnings only.

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched files.

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for the `LegionWarehouse` in-use CAS behavior or `DialogService.onCloseDialog` release behavior in this checkout.

Broad .NET:

- Not run. Broad-validation trigger was live runtime state mutation plus live dialog packet dispatch, but the focused command compiled the affected project and exercised open, denial, reopen, close-release, packet, and runtime contracts.

## Conservative Parity Status

- `OPEN_LEGION_WAREHOUSE` now has partial runtime parity for success, no-legion denial, no-permission denial, unsupported-action denial, in-use denial, same-user reopen, and close-dialog lock release.
- Full `LegionService.canOpenWarehouse` parity is not claimed because config-disabled and disbanding checks remain missing.
- Full close-dialog parity is not claimed because Java also emits NPC AI `DIALOG_FINISH`, which is still non-live in C#.
- The touched system message id matches reviewed Java source, but exact Java golden bytes were not captured.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `LegionWarehouse.setInUse/getCurrentUser/unsetInUse` | `LegionWarehouseRuntime` | Runtime state | Partial | Unit Tested | Partial Parity | In-use owner semantics are modeled by legion id; full Java `Storage` and history behavior are not modeled here. |
| `LegionService.canOpenWarehouse` | `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` | Service side effect / live handler branch | Partial | Unit Tested | Partial Parity | Membership, unsupported-action, permission, success, and in-use branches are live; config-disabled and disbanding checks remain gaps. |
| `DialogService.onCloseDialog` | `GameServerConnection.HandleCloseDialog` | Live close-dialog side effect | Partial | Unit Tested | Partial Parity | Legion warehouse lock release is live; AI finish event remains non-live. |
| `CM_CLOSE_DIALOG` | `CmCloseDialog` plus `GameServerConnection.HandleCloseDialog` | Client packet handler | Partial | Unit Tested | Partial Parity | Target object id drives close side effects; C# currently resolves through world lookup rather than Java known-list lookup. |
| `SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_IN_USE` | `SmSystemMessage.GuildWarehouseInUse` | Server packet helper | Complete | Unit Tested | Partial Parity | Java id `1300280` verified by packet decoder and live dispatch; no Java golden bytes. |

## Known Gaps / Watchouts

- Logout/disconnect can still leave a C# in-use lock stale. Java releases it in `PlayerLeaveWorldService.leaveWorld` through `LegionService.onLogout`.
- Legion leave/kick/member removal can still leave a C# in-use lock stale. Java `LegionService.removeLegionMember` releases it.
- No live C# equivalent for Java `LegionConfig.LEGION_WAREHOUSE` is checked in this path.
- No live C# legion disbanding state is wired for `STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE`.
- C# checks item-template availability before acquiring the lock to avoid stale C# runtime state when static item templates are unavailable.
- C# still represents legion warehouse items through `Player.InventoryItems` location `3`.
- AI `DIALOG_FINISH` close-dialog behavior remains non-live.

## Next Recommended Runtime UOW

Recommended candidate: release the live legion warehouse in-use lock on logout/disconnect, matching Java logout cleanup.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: a player who disconnects or leaves world while holding the legion warehouse lock should release it.
- Java source/runtime path: PlayerLeaveWorldService.leaveWorld calls LegionService.onLogout(player), and LegionService.onLogout calls legion.getLegionWarehouse().unsetInUse(player.getObjectId()).
- C# runtime artifact likely involved: GameServerConnection logout/disconnect cleanup path around the existing leave-world/logout section, GameServerRuntimeContext.LegionWarehouses, and LegionWarehouseRuntime.UnsetInUse.
- Client-visible/state/persistence effect expected: after the holder disconnects/logs out, another same-legion player can open the warehouse instead of receiving stale `STR_GUILD_WAREHOUSE_IN_USE`.
- Why this is runtime progress: it mutates live runtime state from the actual logout/disconnect path and prevents a client-visible stale denial.
```

Suggested discovery:

```powershell
rg -n "HandleDisconnect|CmDisconnect|LeaveWorld|leaveWorld|logout|onLogout|NotifyAccountDisconnected|LegionWarehouseRuntime|LegionWarehouses|unsetInUse\\(|getLegionWarehouse\\(\\)\\.unsetInUse" game-server/src/com/aionemu/gameserver dotnetConversion/src/Aion.GameServer dotnetConversion/tests/Aion.GameServer.Tests
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~LegionWarehouseRuntimeTests|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"
```

Narrow after discovery. Java/Maven is not expected unless a narrow Java fixture is found. Broad-validation trigger: live logout/disconnect runtime state mutation.

## Other Safe Runtime Candidates

- Release the live lock from legion leave/kick/member-removal paths once the corresponding C# runtime path is located.
- Add live disbanding-state denial once a C# legion disbanding flag/source exists.
- Add Java-derived legion warehouse capacity checks to move/split if current C# can overfill location `3`.
- Add exact Java golden bytes for legion warehouse system messages if a narrow Java fixture is created.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `e6db023a4 [Phase 6][UOW-2728] Send cannot-use legion warehouse denial`
  - `72f4a7abb [Phase 6][UOW-2727] Send legion warehouse open denials`
  - `1960af595 [Phase 6][UOW-2726] Open live legion warehouse dialog`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
