# Phase 6 Session 2429 Completion

Status: Phase 6 continues; UOW-2429 fixed group disconnected logout offline-recipient dispatch to match Java `PacketSendUtility.sendPacket(Player, ...)`.

## Scope

- UOW: UOW-2429 group disconnected offline-recipient dispatch parity.
- Reviewed Java `PlayerDisconnectedEvent` and `PacketSendUtility.sendPacket(Player, packet)`.
- Audited C# `PlayerEnterWorldService.DispatchGroupDisconnectedLogoutAsync`.
- Updated live group logout dispatch to skip offline non-self recipients as well as the disconnected player.
- Added focused logout tests for offline recipients in group disconnected fanout and group leader-change fanout.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerDisconnectedEvent.java`
  - Iterates every other group member and attempts offline system/member-info sends.
  - Also attempts member-info sends back to the disconnected player.
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
  - `sendPacket(Player, packet)` sends only when `player.isOnline()`.
  - Therefore offline remaining members and the semi-offline disconnected player receive no live packets.

## Implemented

- Added `ShouldSkipTeamLogoutRecipient(...)` in `PlayerEnterWorldService`.
- Group disconnected live dispatch now builds a group member online-state map before leader mutation.
- Leader-change packet sends skip:
  - the disconnected player,
  - offline remaining members,
  - recipients no longer present in runtime state.
- Disconnected fanout packet sends use the same skip rule.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `LeaveWorld_SkipsOfflineGroupRecipientsDuringDisconnectedFanoutLikeJavaPacketSendUtility` | Regression | Java `PlayerDisconnectedEvent` plus `PacketSendUtility.sendPacket(Player, packet)` source review | Non-leader group logout sends offline/member-info packets only to online remaining members and skips offline recipients. | Focused C# logout/registry assertions over recipients, packet types, and absence of offline/self sends. | Real client connection not exercised. |
| `LeaveWorld_SkipsOfflineGroupRecipientsDuringLeaderChangeLikeJavaPacketSendUtility` | Regression | Java `ChangeGroupLeaderEvent`, `PlayerDisconnectedEvent`, and `PacketSendUtility` source review | Leader logout sends leader-change and disconnected fanout packets only to online remaining members while still mutating fallback leader. | Focused C# logout/registry assertions over recipient order, packet types, leader message id `1300155`, offline message id `1300175`, and runtime leader mutation. | Java `ConcurrentHashMap` ordering remains not deterministic. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.group.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerGroupDisconnectedPlanner` plus `PlayerEnterWorldService.DispatchGroupDisconnectedLogoutAsync` | Event / Live Dispatch | Partial | Regression Tested | Partial Parity | Live dispatch now skips offline non-self recipients in addition to the disconnected player, matching Java `PacketSendUtility`. Planner still records attempted sends for Java-source metadata. |
| `com.aionemu.gameserver.model.team.group.events.ChangeGroupLeaderEvent` | `Aion.GameServer.Services.PlayerGroupRuntime.ChangeLeader` plus logout dispatch | Event / Runtime Mutation | Partial | Regression Tested | Partial Parity | Leader-disconnect fallback mutation remains live; leader-change packet sends now skip offline recipients. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Services.PlayerEnterWorldService.ShouldSkipTeamLogoutRecipient` plus `IGameClientConnectionRegistry.SendPacketToPlayerAsync` | Packet Send Utility / Boundary | Partial | Regression Tested | Partial Parity | C# now models the Java `player.isOnline()` send guard for group disconnected logout. Broader PacketSendUtility/broadcast parity is not implied. |

## Validation Decision

- Changed surface: live group logout connection dispatch filtering.
- Specific behavior/contract: Java `PacketSendUtility.sendPacket(Player, ...)` skips offline group recipients during disconnected logout, including both disconnected fanout and leader-change fanout.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerGroupDisconnectedPlannerTests" --no-restore`
- Result:
  - Passed: 58
  - Failed: 0
  - Skipped: 0
- Hygiene:
  - `git diff --check`
  - Passed; only Git CRLF conversion warnings.
- Focused Java/Maven command:
  - Not run; no Java source or fixtures changed, and Java evidence came from source review.
- Broad-validation trigger:
  - Live connection dispatch behavior.
- Broad .NET decision:
  - Full project/solution validation skipped after focused evidence. The filtered command compiled affected dependencies and asserted the exact live registry-recipient behavior; no packet primitive, registry implementation, persistence repository, scheduler, or serialization helper changed.
- Why this scope is sufficient:
  - The changed effect is isolated to recipient filtering inside `DispatchGroupDisconnectedLogoutAsync`, and focused tests exercise both affected packet-send loops.

## Known Remaining Gaps

- Alliance league broadcasts from disconnected and leader-change events remain metadata-only.
- Java league-left notification after alliance disband is not live.
- Java `EventService.onLeftTeam` remains metadata-only for group and alliance disconnected disband.
- Manual group/alliance disband command parity and offline timeout disband parity still need review.
- C# logout ordering still differs from Java around world removal and persistence timing.

## Summary Metrics

- Java artifacts reviewed: 2.
- C# artifacts reviewed: 2.
- Production files changed: 1.
- Test files changed: 1.
- Focused validation commands passed: 1.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 3.
- Blocked artifacts: 0.
- Estimated Phase 6 completion: unchanged, still conservatively partial.
