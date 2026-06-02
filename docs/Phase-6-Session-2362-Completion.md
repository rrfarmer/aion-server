# Phase 6 Session 2362 Completion - Model Periodic Close Task Intent

## Scope

Modeled Java `PeriodicInstanceManager.registrationCloseTasksByMaskId` storage/cancellation intent without starting live timers.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/utils/ThreadPoolManager.java`

Java behavior used:

- Successful scheduled open stores a close task for the mask after adding the mask and broadcasting open packets.
- Duplicate open returns false and does not replace the existing close task.
- Successful close removes the mask, broadcasts close packets, calls `AutoGroupService.stopRegistrationsByMaskId(maskId)`, then removes/cancels the close task if present.
- Duplicate close returns false and does not alter close task state.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added close-task intent tracking to `PeriodicInstanceRegistrationService`.
- Added `CreateScheduledOpenRegistrationBroadcastPlan(...)` to model scheduled open plus close-task intent creation.
- Updated close planning to clear close-task intent on successful close.
- Added focused tests for intent creation, duplicate-open preservation, and close clearing.

Known limitations:

- No real `ThreadPoolManager.Schedule` call is made in this UOW.
- No real `ScheduledTask.Cancel()` call exists because the model stores intent only.
- Concrete stop-registration wiring into C# autogroup queue state remains missing.
- C# config override binding for autogroup schedule/period values remains missing.

## Validation Decision

- Changed surface: production non-live state/intention model plus tests.
- Specific behavior/contract: successful scheduled open should record close-task intent for the mask, duplicate scheduled open should preserve the existing intent, and successful close should clear the intent like Java's `registrationCloseTasksByMaskId`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests" --no-restore
```

Result: passed 12, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for this runtime map path; Java source review identified the map semantics.
- Broad-validation trigger: none. No live scheduler, hosted service, packet primitive, persistence, or shared infrastructure changed.
- Broad .NET decision: skipped full project/solution validation after the focused test compiled the affected project/dependencies and passed.
- Why this scope is sufficient: the edited service state transitions and Java-derived task-intent semantics are directly asserted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.registrationCloseTasksByMaskId` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService` close-task intents | Runtime State | Partial | Unit Tested | Partial Parity | Map semantics are modeled as non-live intent. Actual scheduled task storage/cancellation is not live yet. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.openRegistration(...)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateScheduledOpenRegistrationBroadcastPlan(...)` | Service | Partial | Unit Tested | Partial Parity | Scheduled open records close intent and sends Java-shaped opening packets. Real delayed close scheduling remains missing. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.closeRegistration(int)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateCloseRegistrationBroadcastPlan(...)` | Service | Partial | Unit Tested | Partial Parity | Close now clears close-task intent. Concrete stop-registration state and real task cancellation remain missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `PeriodicInstanceRegistrationServiceTests.CreateScheduledOpenRegistrationBroadcastPlan_StoresCloseTaskIntentLikeJava` | Unit | Java source review | Successful scheduled open stores close-task intent and includes the Java opening message. | Focused state/packet test. | No live timer. |
| `PeriodicInstanceRegistrationServiceTests.CreateScheduledOpenRegistrationBroadcastPlan_DuplicateOpenDoesNotReplaceCloseTaskIntentLikeJava` | Unit | Java source review | Duplicate scheduled open is no-op and preserves existing close delay. | Focused state test. | No live task handle. |
| `PeriodicInstanceRegistrationServiceTests.CreateCloseRegistrationBroadcastPlan_ClearsScheduledCloseTaskIntentLikeJava` | Unit | Java source review | Successful close clears stored close-task intent and duplicate close remains no-op. | Focused state test. | No real task cancellation call. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Java periodic registration cron callback registration.
- Real scheduled close task creation/cancellation.
- C# config override binding for autogroup schedules and periods.
- Concrete stop-registration wiring into C# autogroup queue state.
- Java quick-entry queue refill after autogroup leave.
- Full forced-exit packet fanout for instance destruction.
- `ConquerorAndProtectorService.onLeaveMap` parity.
- Pet position update and same-map spawn behavior in delayed teleport completion.

## Commit

Commit message:

```text
[Phase 6][UOW-2362] Model periodic close task intent
```
