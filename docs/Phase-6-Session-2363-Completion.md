# Phase 6 Session 2363 Completion - Wire Autogroup Looking Party Close Cleanup

## Scope

Added C# runtime state for Java `AutoGroupService.lookingParties` close cleanup and connected periodic close dispatch to that state.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`

Java behavior used:

- `PeriodicInstanceManager.closeRegistration(maskId)` broadcasts the entry-icon close packet, then calls `AutoGroupService.stopRegistrationsByMaskId(maskId)`.
- `stopRegistrationsByMaskId` removes the full `lookingParties` list for the mask id.
- If removed parties exist, Java loops each party and each member key and sends `SM_AUTO_GROUP(maskId, 2)` to online players.
- Java does not deduplicate member ids inside this stop method.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `AutoGroupLookingPartyRegistrationService` to own C# looking-party queue state by mask id.
- Added queue registration and `StopRegistrationsByMaskIdAsync(...)` cleanup that removes the mask bucket and sends Java-shaped cancel-window packets.
- Registered the looking-party service in game-server DI.
- Added a `PeriodicInstanceRegistrationService.CloseRegistrationAndBroadcastAsync(...)` overload that accepts the looking-party service and dispatches stop cleanup after close broadcast.
- Added packet coverage for `SM_AUTO_GROUP` window `2`.

Known limitations:

- `CM_AUTO_GROUP` window `100` still does not populate the new queue.
- Java `startLooking`, `cancelRegistration`, matching, penalties, and quick-entry refill remain unported.
- Missing static `AutoGroupTable` data removes C# queue state without sending packets; the Java production path expects valid mask ids.
- No real scheduled close task is live yet.

## Validation Decision

- Changed surface: production runtime state plus adjacent periodic close dispatch and packet shape.
- Specific behavior/contract: closing a periodic registration removes queued looking-party registrations for that mask and sends `SM_AUTO_GROUP(maskId, 2)` after the Java close broadcast.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests.CloseRegistrationAndBroadcastAsync_StopsLookingPartyRegistrationsAfterCloseBroadcastLikeJava|FullyQualifiedName~GamePacketTests.SmAutoGroup_WritesJavaCancelRegistrationWindowPayload" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `AutoGroupService.stopRegistrationsByMaskId`; Java source review provided the source-of-truth behavior.
- Broad-validation trigger: live runtime state and packet dispatch were touched, but the behavior is isolated to a new service plus an existing callback seam, and focused validation compiled the affected project/dependencies.
- Broad .NET decision: skipped full project/solution validation; no shared packet primitive, serializer, scheduler, persistence, or connection dispatch primitive changed.
- Why this scope is sufficient: tests directly exercise the new queue cleanup state, periodic close ordering, and exact cancel-window payload shape.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.lookingParties` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Runtime State | Partial | Unit Tested | Partial Parity | Mask-bucket storage and close cleanup are modeled. Start/cancel/match/penalty/quick-entry behavior remains missing. |
| `com.aionemu.gameserver.services.AutoGroupService.stopRegistrationsByMaskId(int)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.StopRegistrationsByMaskIdAsync(...)` | Service | Partial | Unit Tested | Partial Parity | Removes the mask bucket and sends window `2` packets to online queued members. C# skips packets when static autogroup data is missing. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.closeRegistration(int)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CloseRegistrationAndBroadcastAsync(...)` | Service | Partial | Unit Tested | Partial Parity | Close broadcast now has a typed path to invoke looking-party cleanup after close packets. Real scheduled close task remains missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Window `2` payload shape is covered. Other Java windows remain covered only where prior tests exist. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.StopRegistrationsByMaskId_RemovesMaskQueueAndSendsCancelWindowLikeJava` | Unit | Java source review | Removes all parties for the mask and sends window `2` packets to queued members. | Focused runtime state/packet test. | Queue population is not live. |
| `AutoGroupLookingPartyRegistrationServiceTests.StopRegistrationsByMaskId_DoesNotDedupeMemberPacketsLikeJavaLoop` | Unit | Java source review | Preserves Java loop semantics by not deduplicating repeated member ids. | Focused runtime state test. | Java normally prevents duplicates earlier. |
| `AutoGroupLookingPartyRegistrationServiceTests.StopRegistrationsByMaskId_MissingMaskIsNoOpLikeJavaRemoveNull` | Unit | Java source review | Missing mask removes nothing and sends nothing. | Focused runtime state test. | None for this branch. |
| `AutoGroupLookingPartyRegistrationServiceTests.StopRegistrationsByMaskId_RemovesQueueEvenWhenAutoGroupDataMissing` | Unit | C# defensive branch with Java production assumption noted | Queue state is cleared even when packet metadata is unavailable. | Focused state test. | Java valid-mask production path should have metadata. |
| `PeriodicInstanceRegistrationServiceTests.CloseRegistrationAndBroadcastAsync_StopsLookingPartyRegistrationsAfterCloseBroadcastLikeJava` | Unit | Java source review | Periodic close sends entry-icon close broadcasts before queued cancel windows. | Focused ordering test. | No live scheduler. |
| `GamePacketTests.SmAutoGroup_WritesJavaCancelRegistrationWindowPayload` | Unit | Java source review | Window `2` payload writes mask id, window id, map id, zero fields, trailing zero, and empty name. | Focused packet serialization test. | Not a Java golden capture. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- `CM_AUTO_GROUP` window `100` still does not call Java-equivalent `AutoGroupService.startLooking`.
- `CM_AUTO_GROUP` windows `101` through `105` remain deferred.
- Autogroup queue matching, penalties, enter/cancel-enter, and quick-entry refill remain missing.
- Java periodic registration cron callbacks and real scheduled close task handles remain missing.
- Full forced-exit packet fanout for instance destruction remains incomplete.
- `ConquerorAndProtectorService.onLeaveMap`, pet position update, and same-map spawn parity remain pending.

## Commit

Commit message:

```text
[Phase 6][UOW-2363] Wire autogroup close queue cleanup
```
