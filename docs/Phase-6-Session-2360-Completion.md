# Phase 6 Session 2360 Completion - Add Periodic Opening Messages

## Scope

Added exact C# system-message helpers for the Java periodic registration opening messages used by `PeriodicInstanceManager`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

Java behavior used:

- Periodic registration scheduling passes fixed `SM_SYSTEM_MESSAGE` helpers for mask ids `1`, `2`, `3`, `107`, `108`, `109`, and `111`.
- Those helpers have message ids `1400252`, `1400628`, `1401398`, `1401730`, `1401947`, `1402032`, and `1402192`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added named `SmSystemMessage` helpers for the seven Java scheduled periodic registration opening messages.
- Added `PeriodicInstanceRegistrationService.CreateOpeningMessageForMaskId(int)` to map Java periodic mask ids to those helpers.
- Added focused tests for packet helper IDs and mask-id mapping.

Known limitations:

- Cron start expressions and timed close scheduling remain missing.
- Production scheduler callers are not wired yet.
- Concrete stop-registration wiring into C# autogroup queue state remains missing.

## Validation Decision

- Changed surface: packet helper methods plus non-live service mapping.
- Specific behavior/contract: C# scheduled periodic opening message IDs and mask-id mapping must match Java `PeriodicInstanceManager` and `SM_SYSTEM_MESSAGE`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests.CreateOpeningMessageForMaskId_ReturnsJavaScheduledOpeningMessages|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests.OpenRegistrationAndBroadcastAsync_SendsPlannedPacketsToOnlineLevelRangeLikeJavaWorldFanout" --no-restore
```

Result: passed 3, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for these generated system-message helpers; Java source review identified the exact IDs.
- Broad-validation trigger: none. Packet serialization primitives were not changed; only helper factories and a service mapper were added.
- Broad .NET decision: skipped full project/solution validation after the focused test compiled the affected project/dependencies and passed.
- Why this scope is sufficient: the tests assert the exact IDs and the mapping that future scheduler code will use.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` periodic opening helpers | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` periodic opening helpers | Packet Helper | Partial | Unit Tested | Partial Parity | Seven helper IDs used by Java periodic registration scheduling are covered. This does not verify unrelated `SM_SYSTEM_MESSAGE` helpers. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager` constructor scheduled message selection | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateOpeningMessageForMaskId(int)` | Service Mapping | Partial | Unit Tested | Partial Parity | Java mask-to-message mapping is covered for masks `1`, `2`, `3`, `107`, `108`, `109`, and `111`. Cron scheduling remains missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages` | Unit | Java source review | Newly added periodic opening message helpers return the exact Java message IDs. | Focused packet helper assertions. | Method name is broad from existing test grouping. |
| `PeriodicInstanceRegistrationServiceTests.CreateOpeningMessageForMaskId_ReturnsJavaScheduledOpeningMessages` | Unit | Java source review | Periodic registration mask ids map to the exact Java opening system messages. | Focused service mapping test. | Scheduler activation remains future work. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 2
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Java periodic registration cron scheduling and close-task cancellation.
- Concrete stop-registration wiring into C# autogroup queue state.
- Java quick-entry queue refill after autogroup leave.
- Full forced-exit packet fanout for instance destruction.
- `ConquerorAndProtectorService.onLeaveMap` parity.
- Pet position update and same-map spawn behavior in delayed teleport completion.

## Commit

Commit message:

```text
[Phase 6][UOW-2360] Add periodic opening messages
```
