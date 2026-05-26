# Phase 6 Bind-Point Teleport Known-List Membership Metadata

Date: May 26, 2026
Unit of Work: UOW-1249
Scope: Add a metadata-only C# known-list membership prerequisite for bind-point fanout.
Source of truth: Java project.

## Summary

UOW-1249 adds a small C# membership metadata layer for Java `KnownList.forEachPlayer` parity work. The new service is intentionally isolated from live world state and socket sends. It gives the expected bind-point fanout trace a source of known-player membership that can retain known-but-not-visible players.

## Java Source Findings

- Java `KnownList` stores known objects in an object-id keyed map.
- The normal add/update path excludes the owner/source.
- `KnownObject.visible` is cached visibility metadata and does not define membership.
- `KnownList.updateVisibility` can change visibility without dropping membership.
- `PacketSendUtility.broadcastPacket(player, packet, true)` sends the source first, then iterates known-list players.
- Java send-time online checks and per-recipient exception behavior are outside this metadata unit.

## C# Implementation

- `Aion.GameServer.Services.PlayerKnownListMembershipService` records owner-player to known-player entries.
- `PlayerKnownListMembershipEntry.IsVisibleToOwner` keeps visibility separate from membership.
- `GetKnownPlayerObjectIds` includes invisible members by default and can filter visible-only for future sighted-player surfaces.
- `BindPointTeleportKnownListFanoutMembershipAdapterService` projects snapshots into `BindPointTeleportKnownListFanoutTraceService`.
- No live registry, connection, movement, scheduler, or persistence path was changed.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListMembershipServiceTests|BindPointTeleportKnownListFanoutMembershipAdapterServiceTests" --nologo` passed 6 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 193 tests.
- No Java runtime comparison was executed.

## Migration Parity Table - UOW-1249

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.knownObjects` | `Aion.GameServer.Services.PlayerKnownListMembershipService` | Known-List Membership / Metadata Store | Partial | Unit Tested | Needs Verification | Metadata-only store for owner-player known-player membership. Not populated by live world known-list updates. |
| `com.aionemu.gameserver.world.knownlist.KnownObject.isVisible` and `KnownList.sees` | `PlayerKnownListMembershipEntry.IsVisibleToOwner`; `GetKnownPlayerObjectIds` | Known-List Visibility Metadata | Partial | Unit Tested | Needs Verification | Visibility is separate from membership; invisible members remain included by default. No live `canSee` recomputation. |
| `com.aionemu.gameserver.world.knownlist.KnownList.isAwareOf` | `PlayerKnownListMembershipService.UpsertKnownPlayers` | Known-List Guard | Partial | Unit Tested | Needs Verification | Owner/source is excluded in the normal metadata add path. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | `BindPointTeleportKnownListFanoutMembershipAdapterService` | Known-List Traversal Adapter | Partial | Unit Tested | Needs Verification | Projects membership entries into expected trace metadata. No live send, online filtering, exception policy, or runtime ordering validation. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `BindPointTeleportKnownListFanoutTraceService`; `BindPointTeleportKnownListFanoutMembershipAdapterService` | Network Utility / Expected Trace | Partial | Regression Tested | Needs Verification | Source-first trace can now consume membership snapshots. Socket dispatch remains disabled. |

## Remaining Risks

- Live world known-list population is still missing.
- Java two-way known-list side effects are not implemented.
- Source-online send-time gating and per-recipient log-and-continue handling remain unexecuted.
- `GameServerConnection`, movement, live callback dispatch, and Java runtime capture remain disabled or missing.

## Next Recommended Unit of Work

Add a non-live source-online and per-recipient exception policy model for bind-point known-list fanout, or design a disabled source-first fanout executor that consumes membership snapshots without wiring live dispatch.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 2 metadata services plus 6 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live world known-list population path, 1 source-online send gate executor, 1 per-recipient exception policy executor, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete
