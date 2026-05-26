# Phase 6 Bind-Point Teleport Runtime Owner Design

Date: May 26, 2026
Unit of Work: UOW-1216
Scope: Read-only design audit for the live `TaskId.SKILL_USE` task slot and bind-point cooldown owner.
Source of truth: Java project.

## Result

Do not wire live `CM_BIND_POINT_TELEPORT` dispatch yet. The next code slice should add an isolated runtime owner for the Java task/cooldown state, but this audit confirms that owner must be singleton-scoped and player-id keyed, not connection-local.

The C# port already has non-live state semantics in `BindPointTeleportRuntimeStatePlanService`, and the shared scheduler can represent delayed work through `ThreadPoolManager.Schedule`. The missing piece is an executable owner that combines those primitives with Java `CreatureController` replace/remove/cancel behavior and the static cooldown map used by `BindPointTeleportService`.

## Java Facts

Java sources reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/controllers/CreatureController.java`
- `game-server/src/com/aionemu/gameserver/model/TaskId.java`

Java task facts:

- `TaskId.SKILL_USE` is enum ordinal `16`.
- `BindPointTeleportService.teleport` schedules a delayed `TaskId.SKILL_USE` task with `ThreadPoolManager.getInstance().schedule(..., 10000)`.
- `CreatureController.addTask(TaskId, Future<?>)` stores by `taskId.ordinal()`.
- `addTask` cancels any previous task in the same slot with `oldTask.cancel(false)` before replacing it.
- `cancelTask(TaskId)` removes the task before calling `task.cancel(false)`.
- `cancelTeleport` first checks `hasTask(TaskId.SKILL_USE)`, then cancels and broadcasts action `2` only when the slot is present.
- `cancel(false)` does not interrupt already-running Java tasks. The C# equivalent should cancel pending delay/callback startup, but it must not claim parity for interrupting a callback already executing.

Java cooldown facts:

- `BindPointTeleportService` owns a static `Map<Integer, Cooldown> cooldowns`.
- `addCooldown` stores `player.getObjectId()` -> `(locId, System.currentTimeMillis() + 600000)`.
- `onLogin` and requirement checks read by player object id.
- `Cooldown.getTimeLeft()` returns `(int) ((cooldownEndMillis - System.currentTimeMillis()) / 1000)` when positive, otherwise `0`.
- Expired cooldown rows are not removed by the Java code path reviewed here.

## Current C# Facts

C# surfaces reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeStatePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportHandlerCompositionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Utils/ThreadPoolManager.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`

Existing C# coverage:

- `BindPointTeleportRuntimeStatePlanService` models task schedule/replace/cancel and cooldown lookup/add as non-live plans.
- It pins `TaskId.SKILL_USE` ordinal `16`, the `10000` millisecond delay, and the `600` second cooldown.
- `ThreadPoolManager.Schedule` returns `ScheduledTask`; `ScheduledTask.Cancel()` cancels a linked token and returns false once completed/disposed.
- `GameServerConnection` has unrelated local pending-use patterns, but bind-point teleport requires a shared per-player owner because Java stores the slot on `PlayerController` and cooldowns in a static service map.

Missing C# behavior:

- No live `TaskId.SKILL_USE` slot keyed by player object id.
- No replace-existing scheduled task owner for bind-point teleport.
- No remove-before-cancel operation matching `CreatureController.cancelTask`.
- No singleton/static-equivalent cooldown store.
- No login bridge that reads cooldowns and emits action `3`.
- No live cleanup policy for player delete/logout equivalent to `CreatureController.cancelAllTasks`.
- No Java runtime comparison for cancellation races, date/time truncation, or callback order.

## Proposed Owner

Add a future `Aion.GameServer.Services.BindPointTeleportRuntimeStateOwner` as an isolated service before any `GameServerConnection` dispatch.

Suggested API shape:

```csharp
public sealed class BindPointTeleportRuntimeStateOwner
{
	public BindPointTeleportTaskOwnerResult ScheduleSkillUseTask(
		int playerObjectId,
		int locId,
		Func<CancellationToken, ValueTask> callback,
		TimeSpan? delay = null);

	public bool HasSkillUseTask(int playerObjectId);

	public BindPointTeleportTaskOwnerResult CancelSkillUseTask(int playerObjectId, int locId);

	public BindPointTeleportCooldownFact AddCooldown(
		int playerObjectId,
		int locId,
		long currentTimeMillis);

	public BindPointTeleportCooldownFact? GetCooldown(int playerObjectId);

	public BindPointTeleportCooldownPlan CreateLookupCooldownPlan(
		int playerObjectId,
		long currentTimeMillis);

	public void ClearPlayer(int playerObjectId);
}
```

Design constraints:

- Scope it as a singleton service to match Java's service/static-map lifetime.
- Key all task and cooldown state by player object id.
- Store one `TaskId.SKILL_USE` task per player.
- Replacement must cancel the old task before storing the new task, matching Java observable intent.
- Cancellation must remove the task entry before cancellation is requested.
- Completed tasks should keep the slot present until cancel, replace, or player cleanup, because Java `hasTask(TaskId.SKILL_USE)` checks map presence and `CreatureController.addTask` does not remove the `Future` after completion.
- Cooldown lookup must preserve Java whole-second truncation and keep expired entries unless a later cleanup unit intentionally documents a difference.
- Date/time input should be injectable for tests; production can use Unix epoch milliseconds.
- Threading should use a lock or `ConcurrentDictionary` compare/remove pattern. This is an intentional C# implementation safety choice because Java's reviewed `HashMap` cooldown storage is not synchronized, while the C# server can execute scheduled callbacks concurrently.

## Suggested Tests

Add a focused test file before live dispatch:

- `ScheduleSkillUseTask_ReplacesExistingTaskAndCancelsOldTask`
- `CancelSkillUseTask_RemovesThenCancelsExistingTask`
- `CancelSkillUseTask_NoopsWhenTaskMissing`
- `CompletedSkillUseTask_DoesNotRemoveReplacement`
- `AddCooldown_StoresJavaTenMinuteEndMillisByPlayer`
- `LookupCooldown_UsesJavaWholeSecondTruncation`
- `LookupCooldown_ExpiredCooldownReturnsZeroAndKeepsFact`
- `ClearPlayer_CancelsPendingTaskWithoutRemovingOtherPlayers`

These tests should compare against source-derived Java behavior only. Do not mark verified parity unless a Java runtime harness or deterministic runtime comparison is added.

## Migration Parity Table - UOW-1216

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.TaskId.SKILL_USE` | `Aion.GameServer.Services.BindPointTeleportRuntimeStatePlanService`; future `BindPointTeleportRuntimeStateOwner` | Enum / Scheduler Key | Partial | Unit Tested | Needs Verification | Non-live C# plan pins ordinal `16` and name `TaskId.SKILL_USE`. No live task slot exists. Reflection behavior is not involved; threading behavior remains unverified. |
| `com.aionemu.gameserver.controllers.CreatureController.addTask(TaskId, Future<?>)` | future `Aion.GameServer.Services.BindPointTeleportRuntimeStateOwner.ScheduleSkillUseTask` | Controller / Task Owner | Not Started | Manual Only | Needs Verification | Java replaces any old task in the same ordinal slot and calls `cancel(false)`. C# needs exact replace/cancel ownership before scheduling bind-point callbacks. |
| `com.aionemu.gameserver.controllers.CreatureController.cancelTask(TaskId)` | future `Aion.GameServer.Services.BindPointTeleportRuntimeStateOwner.CancelSkillUseTask` | Controller / Task Owner | Not Started | Manual Only | Needs Verification | Java removes the task before `cancel(false)`. C# owner must preserve that ordering and avoid deleting newer replacements during callback cleanup. |
| `com.aionemu.gameserver.controllers.CreatureController.hasTask(TaskId)` | future `Aion.GameServer.Services.BindPointTeleportRuntimeStateOwner.HasSkillUseTask` | Controller / Task Lookup | Not Started | Manual Only | Needs Verification | Java checks slot presence, not `Future.isDone()`, in `cancelTeleport`. C# should document whether completed-but-not-cleaned slots can be observed. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.cooldowns` | future `Aion.GameServer.Services.BindPointTeleportRuntimeStateOwner` cooldown map | Service / Runtime State | Not Started | Manual Only | Needs Verification | Java static `HashMap<Integer, Cooldown>` is keyed by player object id. C# should use singleton lifetime and an explicit concurrency policy. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.addCooldown` | `BindPointTeleportRuntimeStatePlanService.CreateAddCooldownPlan`; future runtime owner | Service / Cooldown Mutation | Partial | Unit Tested | Needs Verification | Non-live plan stores `now + 600000`. No live state mutation exists. Date/time source must be injectable for deterministic tests. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.Cooldown.getTimeLeft` | `BindPointTeleportRuntimeStatePlanService.CalculateJavaTimeLeftSeconds`; future runtime owner lookup | DTO / Date-Time Utility | Partial | Unit Tested | Needs Verification | Non-live helper mirrors whole-second truncation and non-positive-to-zero behavior. Java runtime time comparison not executed. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.schedule` | `Aion.GameServer.Utils.ThreadPoolManager.Schedule` / `ScheduledTask` | Scheduler Dependency | Partial | Manual Only | Needs Verification | C# scheduler can delay and cancel through tokens, but `ScheduledTask.Cancel()` is not a Java `Future.cancel(false)` runtime comparison. Cancellation races and callback-start semantics remain unverified. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.cancelTeleport` | `BindPointTeleportControlPlanService`; future runtime owner integration | Service / Control Flow | Partial | Unit Tested | Needs Verification | Non-live cancel plan exists. Live `hasTask`/`cancelTask` owner and action `2` fanout remain unported. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.onLogin` | `BindPointTeleportControlPlanService`; future runtime owner login bridge | Service / Login Flow | Partial | Unit Tested | Needs Verification | Non-live active-cooldown packet intent exists. Live cooldown lookup and source-included fanout remain disabled. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None in UOW-1216 | Documentation/design audit | Java `CreatureController`, `TaskId`, and `BindPointTeleportService` source | Defines the runtime owner requirements before implementation. | Manual source/C# inspection only. | No executable owner or Java runtime comparison in this unit. |

## Remaining Risks

- Live bind-point teleport remains disabled.
- Runtime owner implementation is still missing.
- Java `cancel(false)` and C# token cancellation are not identical; future tests must explicitly cover pending-delay cancellation and replacement cleanup.
- Java cooldown storage uses wall-clock milliseconds; C# date/time injection must avoid precision/rounding drift while preserving integer division truncation.
- Java's reviewed cooldown `HashMap` is static and unsynchronized. C# should use concurrency-safe storage as an intentional implementation difference, documented in owner tests/docs.
- Serialization behavior did not change in this unit. Reflection behavior did not change. Threading, date/time, inventory persistence, fanout, movement, and Java runtime packet parity remain unverified.

## Next Recommended Unit of Work

Implement `BindPointTeleportRuntimeStateOwner` as an isolated service/test pair. Do not wire it into `GameServerConnection` yet. The implementation should cover per-player `TaskId.SKILL_USE` schedule/replace/cancel semantics, exact-instance cleanup, cooldown add/lookup, Java whole-second time-left truncation, and player cleanup.

Update after UOW-1217: `BindPointTeleportRuntimeStateOwner` now exists as an isolated service with tests. It keeps completed task slots present until cancel/replace/clear to match Java `hasTask` map-presence behavior. Live dispatch remains disabled.

## Summary Metrics

- Total Java artifacts discovered: 10 grouped artifact rows in this unit
- Total artifacts ported: 0 executable artifacts; 1 runtime-owner design audit completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 10 grouped rows
- Total blocked artifacts: 5 grouped categories: runtime task owner, cooldown owner, live dispatch, inventory mutation/persistence, and live movement/fanout
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit reduces implementation ambiguity but does not add executable parity.
