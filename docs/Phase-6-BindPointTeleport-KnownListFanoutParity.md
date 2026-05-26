# Phase 6 Bind-Point Teleport Known-List Fanout Parity

Date: May 26, 2026
Unit of Work: UOW-1247
Scope: Characterize bind-point action `3` fanout mismatch between Java known-list broadcast and current C# visible-distance registry fanout.
Source of truth: Java project.

## Summary

UOW-1247 adds an executable characterization for the current C# bind-point action `3` fanout approximation. The test proves that C# routes through `IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync` with `includeSourcePlayer: true`, includes the source and same-world players within `WorldVisibility.DefaultVisibleDistance`, and excludes out-of-range or different-world players.

This is not verified Java parity. Java sends to the source first and then iterates the source player's known-list players, including known-but-not-visible members. C# still lacks persistent known-list membership for this path.

## Java Source Findings

- `BindPointTeleportService.teleport` broadcasts action `3` only after scheduled Kinah decrease and cooldown storage.
- Java uses `PacketSendUtility.broadcastPacket(player, packet, true)`.
- `broadcastPacket(player, packet, true)` sends to the source player first, then calls the known-list broadcast.
- The known-list broadcast iterates `object.getKnownList().forEachPlayer(...)`.
- Java `KnownList` can contain visible and invisible known objects; `forEachPlayer` does not filter by cached visibility.
- The source player is not in its own known list, so the explicit self-send avoids duplicate self delivery.
- After the source send, Java recipient order follows `ConcurrentHashMap.values()` traversal and should not be treated as deterministic.

## C# Characterization

The current C# runtime fanout remains:

```text
BindPointTeleportRuntimeFanoutService
  -> IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync(..., includeSourcePlayer: true)
  -> current registry/world visibility approximation
```

The new test uses a fake registry to characterize the approximation:

- source player included,
- same-world player at 94m included,
- same-world player at 96m excluded,
- different-world player excluded,
- direct `SendPacketToPlayerAsync` is not called,
- packet payload remains action `3`, player id, loc id, cooldown seconds.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "BindPointTeleportRuntimeFanoutServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 187 tests.
- No Java runtime fanout capture was executed.
- No known-list-backed C# fanout implementation was added.

## Migration Parity Table - UOW-1247

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `Aion.GameServer.Services.BindPointTeleportRuntimeFanoutService.BroadcastFanoutPlanAsync` | Network Utility / Fanout | Partial | Regression Tested | Partial Parity | C# includes source and visible same-world players through registry fanout. Java sends self first, then known-list players. C# does not yet model known-list membership or self-first per-recipient ordering. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | current C# registry/`WorldVisibility` approximation | Known-List / Visibility | Not Started | Unit Tested | Needs Verification | Discovered dependency. Java known-list membership can include invisible known players; C# test only characterizes same-world/95m visibility filtering. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | `BindPointTeleportRuntimeFanoutService`; `BindPointTeleportFanoutPlanService` | Service / Callback Fanout | Partial | Regression Tested | Needs Verification | Action `3` packet shape and C# visible-distance fanout are tested. Live scheduled callback dispatch remains disabled and Java known-list parity is not proven. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmBindPointTeleport` | Packet / Serialization | Partial | Regression Tested | Needs Verification | Action `3` payload shape is asserted in C# tests. No Java golden-byte runtime comparison in this unit. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `BroadcastFanoutPlanAsync_TeleportCooldownActionThreeUsesSourceIncludedVisibleDistanceFanoutWithoutDispatch` | Characterization / Regression | Java action `3` fanout and current C# registry fanout | Current C# includes source plus same-world player within 95m, excludes 96m and different-world players, and emits action `3` packet shape. | C# approximation only. | Does not verify Java known-list membership, known-but-not-visible recipients, self-first direct send, or Java runtime bytes. |

## Remaining Risks

- Current C# fanout is still a visible-distance approximation, not Java known-list parity.
- Java sends source first through a direct send before known-list iteration; C# registry broadcast has no executable self-first guarantee.
- Java known-list can include invisible known players; C# `WorldVisibility` excludes them.
- Live scheduled callback dispatch, final movement, and Java runtime fanout capture remain disabled or missing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, known-list membership, and movement parity remain `Needs Verification`.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 new production artifacts; 1 characterization regression test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 1 known-list membership model, 1 self-first fanout executor, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a non-live known-list-backed fanout plan or expected Java trace model for bind-point broadcasts. It should represent source-first delivery plus known-list-player recipients without using distance-only filtering, and it should remain unwired from `GameServerConnection`.

## Update After UOW-1248

UOW-1248 adds `BindPointTeleportKnownListFanoutTraceService`, a non-live expected Java fanout trace model. It represents:

- source-player send first,
- known-list player recipients after source,
- known-but-not-visible recipients retained as represented known-list members,
- duplicate known-list object ids collapsed to mirror `ConcurrentHashMap` object-id keys,
- owner/source exclusion as a normal Java known-list add/update invariant,
- known-list recipient ordering marked unspecified because Java iterates `ConcurrentHashMap.values()`.

This model does not call `IGameClientConnectionRegistry`, does not send packets, and does not implement persistent known-list membership. It is an executable Java expectation for a future fanout executor.

## Update After UOW-1249

UOW-1249 adds a metadata-only player known-list membership prerequisite for bind-point fanout:

- `PlayerKnownListMembershipService` records owner-player to known-player membership snapshots.
- Membership explicitly excludes the owner/source through the normal Java add/update path.
- Duplicate known-player object ids collapse to one entry, matching Java object-id keyed map behavior.
- Known-but-not-visible players remain members and can still be projected into bind-point fanout traces.
- `TrySetKnownPlayerVisibility` updates cached visibility without removing membership, mirroring `KnownList.updateVisibility` metadata behavior.
- `BindPointTeleportKnownListFanoutMembershipAdapterService` projects membership snapshots into the existing source-first trace model.

This is still not live fanout. It does not register C# players into a shared world known-list, does not call socket sends, does not enforce Java `player.isOnline()` at send time, and does not implement per-recipient log-and-continue exception behavior.

## Migration Parity Table - UOW-1249

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.knownObjects` | `Aion.GameServer.Services.PlayerKnownListMembershipService` | Known-List Membership / Metadata Store | Partial | Unit Tested | Needs Verification | C# now records owner-player to known-player membership snapshots and collapses duplicate known-player ids. It is metadata-only, not integrated with world add/remove/update flows or two-way known-list maintenance. |
| `com.aionemu.gameserver.world.knownlist.KnownObject.isVisible` and `KnownList.sees` | `PlayerKnownListMembershipEntry.IsVisibleToOwner`; `PlayerKnownListMembershipService.GetKnownPlayerObjectIds(..., includeInvisible)` | Known-List Visibility Metadata | Partial | Unit Tested | Needs Verification | Visibility is stored separately from membership, and invisible known players remain in default fanout projection. No live `owner.canSee(object)` recomputation or sighted-player broadcast behavior is implemented. |
| `com.aionemu.gameserver.world.knownlist.KnownList.isAwareOf` | `PlayerKnownListMembershipService.UpsertKnownPlayers` owner/source exclusion | Known-List Guard | Partial | Unit Tested | Needs Verification | Normal add/update path excludes the owner/source. Corrupt/manual Java states where the owner is already present are not modeled. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | `BindPointTeleportKnownListFanoutMembershipAdapterService` -> `BindPointTeleportKnownListFanoutTraceService` | Known-List Traversal Adapter | Partial | Unit Tested | Needs Verification | Adapter projects membership entries into source-first trace recipients, including invisible members. It does not execute live sends, online gating, exception handling, or Java `ConcurrentHashMap` runtime ordering. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `BindPointTeleportKnownListFanoutTraceService`; `BindPointTeleportKnownListFanoutMembershipAdapterService` | Network Utility / Expected Trace | Partial | Regression Tested | Needs Verification | Source-first expected trace now has a membership metadata source. No socket send, registry replacement, Java runtime capture, or `GameServerConnection` dispatch was added. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `UpsertKnownPlayers_ExcludesOwnerAndDeduplicatesKnownPlayerObjectIds` | Unit | `KnownList.isAwareOf`; `ConcurrentHashMap` object-id keys | Owner/source candidate is skipped and duplicate known-player ids collapse with the latest metadata. | Source-derived metadata assertion. | No live world known-list integration or Java runtime comparison. |
| `GetKnownPlayerObjectIds_IncludesInvisibleByDefaultAndCanFilterVisibleOnly` | Unit | `KnownList.forEachPlayer`; `KnownObject.isVisible` | Default membership projection includes invisible known players, while explicit visible-only filtering can exclude them for future sighted-player surfaces. | Source-derived metadata assertion. | No `owner.canSee(object)` recomputation. |
| `TrySetKnownPlayerVisibility_UpdatesExistingMembershipWithoutDroppingInvisibleEntry` | Unit | `KnownList.updateVisibility` | Visibility changes do not remove known-list membership. | Source-derived metadata assertion. | Does not send Java see/notSee packets. |
| `RemoveAndClearKnownPlayers_RemoveMembershipEntries` | Unit | `KnownList.del`; known-list cleanup | Remove/clear operations drop membership metadata. | Source-derived metadata assertion. | Does not execute two-way removal, `notKnow`, or `notSee` behavior. |
| `CreateTrace_ProjectsMembershipSnapshotIntoSourceFirstKnownListTrace` | Unit / Expected Trace | `PacketSendUtility.broadcastPacket(..., true)`; `KnownList.forEachPlayer` | Membership snapshot projects source first, then visible and invisible known players. | Source-derived deterministic C# trace. | No online gate, socket send, exception policy, or Java runtime capture. |
| `CreateTrace_WithoutMembershipSnapshotReturnsSourceOnlyProjectedTrace` | Unit / Expected Trace | C# staging guard | Missing membership snapshot still produces source-only expected metadata when a packet plan exists. | C# safety guard. | Java would use the owner's actual live known list. |

Remaining risks:

- C# membership metadata is isolated and not populated by live world visibility/add/remove flows.
- Java two-way known-list maintenance, `sendSee`, `notSee`, `notKnow`, and out-of-range cleanup remain unimplemented.
- Source-online gating and per-recipient exception log-and-continue behavior remain documented but not executed.
- Live scheduled callback dispatch, source-first socket fanout, final movement, and Java runtime fanout capture remain disabled or missing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, live known-list membership, and movement parity remain `Needs Verification`.

Summary metrics:

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 2 metadata services plus 6 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live world known-list population path, 1 source-online send gate executor, 1 per-recipient exception policy executor, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

Next recommended unit of work:

- Add a non-live source-online and per-recipient exception policy model for bind-point known-list fanout, or start an isolated source-first fanout executor design that remains disabled and consumes the new membership snapshots without wiring `GameServerConnection`.

## Update After UOW-1250

UOW-1250 adds `BindPointTeleportKnownListFanoutSendPolicyService`, a metadata-only model for the remaining Java send-policy details around known-list fanout:

- `PacketSendUtility.sendPacket` gates each source/known-list recipient on `player.isOnline()`.
- Offline recipients are modeled as skipped, not failed.
- Recipient send failures are modeled as `FailedAndContinued` because known-list traversal is wrapped by `CollectionUtil.forEach`.
- The policy consumes the existing source-first trace and remains non-live.

This still does not send packets, replace the registry fanout, execute Java logging, or wire `GameServerConnection`.

## Migration Parity Table - UOW-1250

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket(Player,AionServerPacket)` | `Aion.GameServer.Services.BindPointTeleportKnownListFanoutSendPolicyService` | Packet Utility / Send Policy | Partial | Unit Tested | Needs Verification | C# now models the Java online gate as metadata: online recipients would send, offline recipients are skipped. No socket send or Java runtime comparison occurs. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.isOnline` | `BindPointTeleportKnownListFanoutRecipientSendPolicy.UsesPlayerIsOnlineGate`; supplied online-player facts | Runtime State / Online Gate | Partial | Unit Tested | Needs Verification | Online state is supplied as test metadata. It is not read from live `Player` or connection state. |
| `com.aionemu.gameserver.utils.collections.CollectionUtil.forEach` | `BindPointTeleportKnownListFanoutSendPolicyService` failure projection | Utility / Exception Policy | Partial | Unit Tested | Needs Verification | Per-recipient failures are modeled as `FailedAndContinued`. Java logging text and real exception handling are not executed. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | `BindPointTeleportKnownListFanoutTraceService`; `BindPointTeleportKnownListFanoutSendPolicyService` | Known-List Traversal / Send Policy | Partial | Regression Tested | Needs Verification | Source-first trace plus send policy now records traversal continuation semantics. Live known-list population and runtime ordering remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `BindPointTeleportKnownListFanoutTraceService`; `BindPointTeleportKnownListFanoutSendPolicyService` | Network Utility / Expected Fanout Policy | Partial | Regression Tested | Needs Verification | Expected source-first recipients plus online/failure policy are represented. No live executor or packet send was added. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePolicy_UsesOnlineGateForSourceAndKnownListRecipients` | Unit / Send Policy | `PacketSendUtility.sendPacket`; `Player.isOnline` | Online source/known recipients would send, offline known recipient is skipped. | Source-derived metadata assertion. | Online state is supplied; no live connection lookup. |
| `CreatePolicy_ModelsKnownListRecipientFailureAsLogAndContinue` | Unit / Send Policy | `KnownList.forEachPlayer`; `CollectionUtil.forEach` | A failing known recipient is marked failed-and-continued and later recipients still project as sendable. | Source-derived metadata assertion. | Does not execute Java logging or real send exceptions. |
| `CreatePolicy_NoPacketTraceDoesNotProjectRecipientSends` | Unit / Send Policy | C# staging guard | No-packet trace produces no recipient sends and stays non-live. | C# safety guard. | Java helper requires a packet. |

Remaining risks:

- Send policy is metadata only and does not call sockets.
- Online facts are supplied, not read from live `Player`/connection state.
- Java logging and real exception handling are not executed.
- Live known-list population, source-first executor, scheduled callback dispatch, final movement, and Java runtime fanout capture remain disabled or missing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, live known-list membership, and movement parity remain `Needs Verification`.

Summary metrics:

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 metadata send-policy service plus 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live world known-list population path, 1 disabled source-first fanout executor, 1 live socket send adapter, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

Next recommended unit of work:

- Add a disabled source-first known-list fanout executor design that composes membership snapshots and send-policy projections, but still does not call sockets or wire `GameServerConnection`.
