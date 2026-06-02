# Phase 6 Session 2358 Completion - Plan Periodic Registration Broadcasts

## Scope

Modeled the Java `PeriodicInstanceManager.openRegistration(...)` and `closeRegistration(...)` broadcast decision slice in C# without enabling cron scheduling or live socket fanout yet.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`

Java behavior used:

- `openRegistration` returns false when the mask is already open.
- A successful open adds the mask to `openedRegistrations`, broadcasts `SM_AUTO_GROUP(maskId, WND_ENTRY_ICON, false)`, then sends the optional opening system message to the same eligible players.
- `closeRegistration` returns false when the mask is not open.
- A successful close removes the mask, broadcasts `SM_AUTO_GROUP(maskId, WND_ENTRY_ICON, true)`, and calls `AutoGroupService.stopRegistrationsByMaskId(maskId)`.
- `broadcastRegistrationUpdate` filters by autogroup level range only; it does not check portal cooldowns. Cooldown filtering remains limited to `checkAndSendOpenRegistrations(player)`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Extended `PeriodicInstanceRegistrationService` with open/close broadcast plan methods that mutate registration state once and return per-player packet plans.
- Preserved Java's level-only broadcast filter for open/close updates.
- Added a close-plan flag for the Java `AutoGroupService.stopRegistrationsByMaskId(maskId)` side effect.
- Exposed `SmAutoGroup.IsClosed` for focused assertions without changing packet serialization.
- Added focused tests for open, duplicate open, close, duplicate close, missing autogroup data, level filtering, optional opening message ordering, and close flags.

Known limitations:

- Cron start expressions and timed close scheduling are still not ported.
- Live fanout to online players is not wired yet.
- Exact Java periodic opening-message helper methods are not added yet; the broadcast plan accepts an already-created `SmSystemMessage`.
- Java quick-entry queue refill remains unwired.

## Validation Decision

- Changed surface: production non-live service planning plus packet assertion surface.
- Specific behavior/contract: Java periodic registration open/close should mutate opened-registration state once, create entry-icon `SM_AUTO_GROUP` packets for level-eligible players, include optional opening system messages on open, and mark close as requiring `stopRegistrationsByMaskId`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup_WritesJavaEntryIconOpenAndClosePayload" --no-restore
```

Result: passed 5, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for this periodic registration service path; Java source review plus focused C# service/packet tests were used as evidence.
- Broad-validation trigger: none. This unit did not enable live fanout, scheduler work, packet primitive changes, persistence, or shared infrastructure.
- Broad .NET decision: skipped full project/solution validation. The filtered `dotnet test` command compiled the affected project/dependencies and covered the scoped behavior.
- Why this scope is sufficient: the tests exercise the edited service and directly adjacent packet shape; broader validation would not add Java parity evidence for this non-live planning slice.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.openRegistration(SM_SYSTEM_MESSAGE,int,long)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateOpenRegistrationBroadcastPlan(...)` | Service | Partial | Unit Tested | Partial Parity | State transition and broadcast packet planning are covered. Cron scheduling, close task storage, and exact opening-message factory helpers remain missing. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.closeRegistration(int)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateCloseRegistrationBroadcastPlan(...)` | Service | Partial | Unit Tested | Partial Parity | State transition, close packet planning, and `stopRegistrationsByMaskId` intent flag are covered. Live stop-registration call and scheduled task cancellation remain missing. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.broadcastRegistrationUpdate(SM_SYSTEM_MESSAGE,int,boolean)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateRegistrationBroadcastPlan(...)` | Service Helper | Partial | Unit Tested | Partial Parity | Level-range filtering, entry-icon packet, optional open message, and close flag are tested. Live `World.forEachPlayer`/`PacketSendUtility` fanout remains missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Added `IsClosed` inspection only; serialization unchanged. Entry-icon open/close payload remains covered by packet test. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `PeriodicInstanceRegistrationServiceTests.CreateOpenRegistrationBroadcastPlan_MutatesOnceAndSendsEntryIconPlusOpeningMessageToLevelRangeLikeJava` | Unit | Java source review | Open state mutation, duplicate-open no-op, level-only player filtering, entry-icon open packet, and optional system message ordering. | Focused service test tied to Java method review. | Does not prove exact scheduled opening message IDs. |
| `PeriodicInstanceRegistrationServiceTests.CreateCloseRegistrationBroadcastPlan_MutatesOnceAndMarksStopRegistrationsLikeJava` | Unit | Java source review | Close state mutation, duplicate-close no-op, level-only player filtering, close packet flag, and stop-registration side-effect intent. | Focused service test tied to Java method review. | Does not invoke live `AutoGroupService.stopRegistrationsByMaskId`. |
| `PeriodicInstanceRegistrationServiceTests.CreateRegistrationBroadcastPlan_PreservesStateChangeWhenAutoGroupDataMissingLikeJavaUnknownTypeNoBroadcast` | Unit | Java source review | Successful state transitions still produce no player broadcasts when mask metadata is unavailable. | Focused edge test. | Java invalid-mask behavior is not expected in scheduled production data. |
| `GamePacketTests.SmAutoGroup_WritesJavaEntryIconOpenAndClosePayload` | Unit | Java source review | Entry-icon open/close serialization remains aligned with Java `SM_AUTO_GROUP.writeImpl`. | Focused packet serialization test. | Other window ids are not newly tested here. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Java periodic registration cron scheduling and close-task cancellation.
- Live open/close broadcast fanout to online players.
- Exact scheduled opening system-message helper methods.
- Java quick-entry queue refill after autogroup leave.
- Full forced-exit packet fanout for instance destruction.
- `ConquerorAndProtectorService.onLeaveMap` parity.
- Pet position update and same-map spawn behavior in delayed teleport completion.

## Commit

Commit message:

```text
[Phase 6][UOW-2358] Plan periodic registration broadcasts
```
