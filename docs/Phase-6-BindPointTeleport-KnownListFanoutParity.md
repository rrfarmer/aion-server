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

## Update After UOW-1255

The player known-list population design audit confirms that the fanout blocker is larger than recipient selection. Java bind-point action `3` depends on known-list membership created by world spawn/update/despawn lifecycle hooks and region-neighbor scans. Current C# registry/distance fanout and precomputed metadata snapshots must remain non-live until region-backed membership and controller packet side-effect dispatch exist.

Next recommended work: add a disabled region/player snapshot model that can feed future known-list membership population without using flat online registry scans as a parity substitute.

## Update After UOW-1256

The disabled region/player snapshot model now exists. It improves future membership inputs by carrying modeled world id, instance id, owner region, neighbor regions, and candidate player object ids, but bind-point action `3` fanout remains non-live because the model does not yet mutate known-list membership or execute Java controller side effects.

## Update After UOW-1257

The region snapshot membership adapter now seeds non-live player membership metadata from modeled region candidates. Bind-point fanout remains blocked because Java action `3` still depends on live known-list state, two-way add/remove, and controller packet side effects rather than precomputed metadata alone.

## Update After UOW-1258

The two-way operation planner now records Java candidate-first add and owner-first remove/clear ordering before future membership mutation. Bind-point fanout remains non-live because operation plans are descriptors and no live known-list state or socket dispatch path consumes them.

## Update After UOW-1259

The two-way membership adapter can now consume operation plans and mutate non-live membership metadata behind an explicit opt-in flag. Bind-point fanout remains blocked because this metadata is still not live world known-list state and controller side effects are not executed.

## Update After UOW-1260

The visibility/range planner now provides Java-shaped range inputs before membership operation planning. Bind-point fanout remains blocked because these are still non-live descriptors and no scheduled action `3` path consumes them.

## Update After UOW-1261

The known-list population composition service can now seed non-live membership metadata from region snapshot/range/operation plans. Bind-point fanout remains blocked because no live scheduled action `3` path consumes that metadata and controller side effects remain descriptors.

## Update After UOW-1262

The player side-effect planner now records descriptor-only packet intent for player-player known-list `see` and `notSee` transitions. Bind-point fanout remains blocked because these descriptors are not attached to a live known-list callback dispatcher, and `SmPlayerInfo` enemy/aggro behavior plus `SmPlayerStance`/`SmAbnormalEffect` packet classes still need dedicated parity work.

## Update After UOW-1263

The operation side-effect attachment service now joins player packet descriptors to two-way known-list `see`/`notSee` operation steps. Bind-point fanout remains blocked because action `3` does not consume these attachments and live socket dispatch remains disabled.

## Update After UOW-1264

Population composition now carries side-effect attachments through candidate results. Bind-point fanout remains blocked because the runtime fanout path still does not consume population results or execute source-first known-list socket sends.

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

## Update After UOW-1251

UOW-1251 adds `BindPointTeleportKnownListFanoutExecutionPlanService`, a disabled source-first executor composition. It combines:

- `BindPointTeleportFanoutPlan`
- `PlayerKnownListMembershipSnapshot`
- `BindPointTeleportKnownListFanoutTraceService`
- `BindPointTeleportKnownListFanoutSendPolicyService`

The result proves the Java-shaped fanout pieces can compose into one non-live execution plan while `SendsPackets` remains `false`.

## Migration Parity Table - UOW-1251

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | `Aion.GameServer.Services.BindPointTeleportKnownListFanoutExecutionPlanService` | Service / Disabled Execution Plan | Partial | Unit Tested | Needs Verification | Composes packet fanout plan, membership snapshot, source-first trace, and send-policy metadata. It does not execute the scheduled callback or send packets. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `BindPointTeleportKnownListFanoutExecutionPlanService`; trace/send-policy services | Network Utility / Expected Fanout Composition | Partial | Regression Tested | Needs Verification | Source-first ordering, known-list membership, online gating, and failure continuation are composed as metadata. No live socket send or Java runtime capture. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | `PlayerKnownListMembershipService`; membership adapter; execution plan service | Known-List Traversal Composition | Partial | Regression Tested | Needs Verification | Membership snapshots feed the trace and execution plan. Live world known-list population and runtime ordering remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket(Player,AionServerPacket)` | `BindPointTeleportKnownListFanoutSendPolicyService` inside execution plan | Packet Utility / Send Policy Composition | Partial | Regression Tested | Needs Verification | Online/failure policy is composed but no actual send occurs. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateDisabledPlan_ComposesMembershipTraceAndSendPolicyWithoutSendingPackets` | Unit / Disabled Execution Plan | `broadcastPacket(..., true)`; `KnownList.forEachPlayer`; `sendPacket` | Membership, source-first trace, and online-gated send policy compose while `SendsPackets=false`. | Source-derived metadata assertion. | No live socket send or Java runtime comparison. |
| `CreateDisabledPlan_PropagatesRecipientFailurePolicyAndContinuesProjection` | Unit / Disabled Execution Plan | `CollectionUtil.forEach` | Recipient failure policy remains failed-and-continued inside the composed plan. | Source-derived metadata assertion. | Does not execute Java logging or real exceptions. |
| `CreateDisabledPlan_WithoutPacketPlanReturnsNoPacketAndNoRecipients` | Unit / Disabled Execution Plan | C# staging guard | No packet plan produces no trace/send recipients and stays non-live. | C# safety guard. | Java helper requires a packet. |

Remaining risks:

- The composed plan is still disabled and never sends packets.
- Live known-list population, connection lookup, and Java logging are not executed.
- Scheduled Kinah callback dispatch, final movement, `GameServerConnection`, and Java runtime fanout capture remain disabled or missing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, live known-list membership, and movement parity remain `Needs Verification`.

Summary metrics:

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled execution-plan composition service plus 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live world known-list population path, 1 live socket send adapter, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

Next recommended unit of work:

- Audit live C# world/player state to identify the safest future source for real known-list population, or add a disabled opt-in socket executor boundary that consumes the execution plan but remains unwired from `GameServerConnection`.

## Update After UOW-1252

UOW-1252 adds `BindPointTeleportKnownListFanoutSocketExecutorService`, a disabled-by-default opt-in socket executor boundary. It consumes the disabled execution plan and can call `IGameClientConnectionRegistry.SendPacketToPlayerAsync` only when explicitly enabled by the caller.

Important Java nuance captured by this unit:

- `PacketSendUtility.broadcastPacket(player, packet, true)` sends the source before known-list traversal.
- Source self-send is outside `KnownList.forEachPlayer`, so a source send exception can stop traversal.
- Known-list recipient sends run through `KnownList.forEachPlayer -> CollectionUtil.forEach`, so recipient exceptions are modeled as failed-and-continued.

The executor is not wired into `GameServerConnection`, not registered in DI, and not used by live scheduled Kinah callback dispatch.

## Migration Parity Table - UOW-1252

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `Aion.GameServer.Services.BindPointTeleportKnownListFanoutSocketExecutorService` | Network Utility / Disabled Socket Boundary | Partial | Unit Tested | Needs Verification | Opt-in executor preserves source-first ordering and known-list traversal order from the execution plan. It is disabled by default and not wired to dispatch. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket(Player,AionServerPacket)` | `BindPointTeleportKnownListFanoutSocketExecutorService.ExecuteAsync` | Packet Utility / Socket Send Boundary | Partial | Unit Tested | Needs Verification | Enabled path can call `SendPacketToPlayerAsync` per recipient. Missing connection, exception, and cancellation behavior are C# boundary metadata, not Java runtime comparison. |
| `com.aionemu.gameserver.utils.collections.CollectionUtil.forEach` | `BindPointTeleportKnownListFanoutSocketExecutorService` known-list recipient failure handling | Utility / Exception Policy | Partial | Unit Tested | Needs Verification | Known-list recipient exceptions continue traversal. Java logging text and real server exception path are not executed. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | execution plan plus socket executor recipient loop | Known-List Traversal / Socket Boundary | Partial | Regression Tested | Needs Verification | Uses precomputed membership snapshot order. Live known-list population, `ConcurrentHashMap` runtime ordering, and two-way membership mutation remain missing. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | known-list fanout execution plan plus socket executor boundary | Service / Callback Fanout Boundary | Partial | Regression Tested | Needs Verification | Fanout socket boundary exists but is not connected to scheduled callback execution, cooldown storage, movement, or `GameServerConnection`. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ExecuteAsync_DisabledExecutorDoesNotCallRegistryAndRecordsRecipients` | Unit / Socket Boundary | Java fanout socket boundary | Disabled default records recipients without calling registry. | C# safety guard plus source-derived boundary. | Java has no disabled equivalent. |
| `ExecuteAsync_EnabledSendsSourceFirstThenKnownListRecipientsAndSkipsOfflinePolicyRecipients` | Unit / Socket Boundary | `broadcastPacket(..., true)` and `sendPacket` online gate | Enabled opt-in path sends source first, then known-list recipient, and skips offline-policy recipient. | Source-derived ordering assertion. | No Java runtime capture. |
| `ExecuteAsync_EnabledContinuesAfterKnownListRecipientException` | Unit / Socket Boundary | `KnownList.forEachPlayer`; `CollectionUtil.forEach` | Known-list recipient exception is failed-and-continued and later recipient is attempted. | Source-derived exception-policy assertion. | Does not execute Java logging. |
| `ExecuteAsync_EnabledStopsBeforeKnownListWhenSourceSendThrows` | Unit / Socket Boundary | `broadcastPacket(..., true)` source send before traversal | Source-send exception stops known-list traversal. | Source-derived control-flow assertion. | Java runtime send exception behavior not captured. |
| `ExecuteAsync_NoPacketPlanReturnsNoPacketWithoutRegistryCall` | Unit / Socket Boundary | C# staging guard | No-packet execution plan performs no sends. | C# safety guard. | Java helper requires a packet. |

Remaining risks:

- The executor is opt-in and not used by live bind-point flows.
- Live known-list population remains missing; current C# registry/distance visibility is not Java known-list parity.
- Online state still comes from supplied send policy rather than direct Java-equivalent player connection state.
- Java logging, `ConcurrentHashMap` ordering, two-way known-list mutation, scheduled callback dispatch, movement, and Java runtime capture remain missing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, live known-list membership, and movement parity remain `Needs Verification`.

Summary metrics:

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled/opt-in socket executor service plus 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live world known-list population path, 1 live scheduled callback dispatch path, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

Next recommended unit of work:

- Add a small, test-first `PlayerKnownListMembershipRefreshService` that uses supplied online player candidates and `WorldVisibility` to seed approximate player-player membership metadata, while explicitly documenting that this is not full Java region/known-list parity.

## Update After UOW-1253

UOW-1253 adds `PlayerKnownListMembershipRefreshService`, a test-first approximation seam for player-player membership refresh. It uses supplied online `Player` candidates and `WorldVisibility` to populate `PlayerKnownListMembershipService`.

This reduces the blocker from "no population seam" to "only distance-based approximation exists." It does not implement Java's region-neighbor scan or controller side effects.

## Migration Parity Table - UOW-1253

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.update` | `Aion.GameServer.Services.PlayerKnownListMembershipRefreshService` | Known-List Refresh / Approximation | Partial | Unit Tested | Partial Parity | C# refreshes from supplied online players and current `WorldVisibility`, removes stale out-of-range entries, and excludes owner. It does not perform Java region-neighbor scans, synchronized update, two-way atomic-ish add/remove, or controller packet side effects. |
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `PlayerKnownListMembershipRefreshService.RefreshOwnerFromOnlinePlayers` | Known-List Population | Partial | Unit Tested | Partial Parity | Uses supplied candidates plus 95m same-world visibility only. No Java `MapRegion` scan, object visible distance negotiation, or `canSee` state. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forgetObjectsOrUpdateVisibility` | `RefreshOwnerFromOnlinePlayers` stale removal | Known-List Cleanup | Partial | Unit Tested | Needs Verification | Removes entries absent from current distance-visible candidate set. Does not distinguish out-of-range removal from in-range invisible visibility changes; Java hidden known objects can remain known with `visible=false`. |
| `com.aionemu.gameserver.world.knownlist.KnownList.clear` | `ClearOwnerForLogout`; `RemoveDepartingPlayerFromKnownLists` | Known-List Cleanup / Logout Metadata | Partial | Unit Tested | Needs Verification | Clears owner metadata and removes departing player from supplied owners. Does not send Java `notSee`/`notKnow`, does not walk all world objects automatically, and remains unwired from logout. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | membership refresh plus known-list fanout metadata stack | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Fanout can now be seeded from an approximate membership refresh in tests. Live bind-point dispatch still uses no Java-equivalent known-list population. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `RefreshOwnerFromOnlinePlayers_UsesWorldVisibilityApproximationAndExcludesOwner` | Unit / Membership Refresh | `KnownList.isAwareOf`; `findVisibleObjects` | Owner excluded, near same-world candidate added, far/different-world skipped, approximation flags false for Java parity. | Source-derived approximation assertion. | No region scan or Java runtime comparison. |
| `RefreshOwnerFromOnlinePlayers_RemovesStaleOutOfRangeMembership` | Unit / Membership Refresh | `forgetObjectsOrUpdateVisibility` | Previously known out-of-range entry is removed during refresh. | Source-derived cleanup approximation. | Does not model hidden-but-known visibility changes. |
| `RefreshAllFromOnlinePlayers_ProducesBidirectionalDistanceApproximation` | Unit / Membership Refresh | Java two-way known-list add relation | Supplied online players near each other become known to each other. | Approximation only. | Java two-way add order and concurrency are not implemented. |
| `ClearOwnerForLogoutAndRemoveDepartingPlayerFromKnownLists_RemoveMembershipMetadata` | Unit / Membership Cleanup | `KnownList.clear`; despawn/remove behavior | Owner snapshot is cleared and departing player is removed from supplied remaining owners. | Source-derived metadata cleanup. | No `SM_DELETE`, `notKnow`, or global world scan. |

Remaining risks:

- This service is an approximation and is explicitly not Java region known-list parity.
- It is not registered or wired into enter/move/logout.
- Hidden/invisible-but-known Java behavior is not modeled; C# distance refresh can remove entries Java might retain as invisible.
- Java controller packet side effects, ordering, synchronization, and region lifecycle remain missing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, live known-list membership, and movement parity remain `Needs Verification`.

Summary metrics:

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 membership refresh approximation service plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 Java-equivalent region known-list population path, 1 live enter/move/logout wiring path, 1 controller see/notSee/notKnow packet side-effect path, 1 live scheduled callback dispatch path, 1 live movement adapter, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

Next recommended unit of work:

- Add a documentation/design audit for full Java-equivalent player known-list population requirements, or add a disabled adapter that converts `IGameClientConnectionRegistry.ForEachOnlinePlayer` snapshots into `PlayerKnownListMembershipRefreshService` inputs without wiring live dispatch.

## Update After UOW-1254

UOW-1254 adds `PlayerKnownListMembershipRegistryRefreshAdapterService`, a disabled-by-default adapter from `IGameClientConnectionRegistry.ForEachOnlinePlayer` snapshots into the existing membership refresh approximation.

## Migration Parity Table - UOW-1254

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.World` / `WorldMapInstance` player storage | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.ForEachOnlinePlayer`; `PlayerKnownListMembershipRegistryRefreshAdapterService` | Registry / Player Snapshot Adapter | Partial | Unit Tested | Intentional Difference | C# adapter reads online connection registry snapshots, not Java world/map-region object storage. This is a disabled approximation seam, not live parity. |
| `com.aionemu.gameserver.world.knownlist.KnownList.update` | `PlayerKnownListMembershipRegistryRefreshAdapterService` -> `PlayerKnownListMembershipRefreshService` | Known-List Refresh Adapter | Partial | Unit Tested | Partial Parity | Adapter can refresh one owner or all supplied online players through current-distance approximation. It does not perform region-neighbor scan or controller side effects. |
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | registry snapshot plus `WorldVisibility` refresh | Known-List Population | Partial | Unit Tested | Needs Verification | Uses online registry snapshot and 95m same-world visibility. No Java `MapRegion` lifecycle, `canSee`, or object visible-distance negotiation. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | registry refresh adapter plus known-list fanout metadata stack | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Registry snapshots can now seed approximate membership metadata when explicitly enabled. Live scheduled callback dispatch and Java-equivalent known-list population remain disabled. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `RefreshOwner_DisabledAdapterDoesNotReadRegistry` | Unit / Adapter Guard | C# live-safety gate | Disabled default does not call `ForEachOnlinePlayer`. | C# safety guard. | Java has no disabled equivalent. |
| `RefreshOwner_EnabledUsesRegistrySnapshotAndWorldVisibilityApproximation` | Unit / Adapter | Java `KnownList.update` approximation | Enabled owner refresh reads registry snapshot and seeds near candidate only. | Source-derived approximation. | No Java world/region scan. |
| `RefreshAll_EnabledRefreshesBidirectionalSnapshotApproximation` | Unit / Adapter | Java two-way known-list relation | Enabled all-player refresh creates bidirectional distance approximation. | Approximation only. | No Java two-way add order/concurrency parity. |
| `RefreshAll_EnabledWithoutRegistryReturnsMissingRegistry` | Unit / Adapter Guard | C# runtime boundary | Enabled adapter without registry reports missing registry and does not refresh. | C# safety guard. | Java always has world storage context. |

Remaining risks:

- Adapter remains disabled/unwired.
- Registry snapshots are not Java region known-list storage.
- Hidden/invisible-but-known state, controller packets, region lifecycle, and concurrency remain missing.
- Live scheduled callback dispatch, movement, `GameServerConnection`, and Java runtime capture remain disabled.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, live known-list membership, and movement parity remain `Needs Verification`.

Summary metrics:

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled registry refresh adapter service plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 1 Java-equivalent region known-list population path, 1 live enter/move/logout wiring path, 1 controller see/notSee/notKnow side-effect path, 1 live scheduled callback dispatch path, 1 live movement adapter, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

Next recommended unit of work:

- Add a documentation/design audit for full Java-equivalent player known-list population requirements before wiring registry snapshot refresh or socket executor paths into live server flows.

## Update After UOW-1266

The player-info packet prerequisite now covers Java's enemy creature-type flag plus scalar viewer-race projection and neutral override inputs. Known-list fanout parity is still blocked on live region population, controller side-effect execution, active-player context computation, `SmPlayerStance`, `SmAbnormalEffect`, and Java runtime packet-order validation.

## Update After UOW-1267

`SmPlayerStance` now exists as a focused C# packet serializer and the descriptor stack records it as available. Known-list fanout parity is still blocked on live region population, controller side-effect execution, active-player context computation, `SmAbnormalEffect`, descriptor-to-packet construction, and Java runtime packet-order validation.

## Update After UOW-1268

`SmAbnormalEffect` now exists as a partial C# serializer for supplied abnormal-effect facts. Known-list fanout parity is still blocked on live region population, controller side-effect execution, active-player context computation, live effect-controller hydration, descriptor-to-packet construction, and Java runtime packet-order validation.

## Update After UOW-1269

Descriptor-to-packet construction metadata now exists for an individual player side-effect plan. Known-list fanout parity is still blocked on applying that bridge across population attachments, hydrating live runtime facts, live region population, controller side-effect execution, socket dispatch, and Java runtime packet-order validation.
