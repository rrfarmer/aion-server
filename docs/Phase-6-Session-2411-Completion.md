# Phase 6 Session 2411 Completion - Leave-World Dead Player Revive

## Scope
- Audited Java leave-world dead-player handling after kisk logout.
- Wired the C# leave-world path to revive dead players before removal from the world.
- Added connection workflow coverage for open-world bind revive, instance-start revive, and the special `400030000` fallback.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - `leaveWorld(Player player)`
- `game-server/src/com/aionemu/gameserver/services/player/PlayerReviveService.java`
  - `bindRevive(Player player)`
  - `instanceRevive(Player player)`
  - `revive(Player player, int hpPercent, int mpPercent, boolean setSoulSickness, int resurrectionSkill)`

## Implemented
- `GameServerConnection.LeavePlayerWorldAsync(...)` now applies the Java dead-player logout branch after kisk offline binding.
- Open-world dead logout uses bind revive semantics: 25% HP/MP restore, DP reset when no no-resurrect-penalty effect is present, dead-state clear, live aggro clear, resurrection-position clear, and bind-location move when modeled bind data is available.
- Instance-map dead logout uses the modeled instance start position when the current runtime instance has one; otherwise it falls back to bind revive.
- The Java `400030000` branch is represented without treating the map as an instance type for start-position teleport; it falls back to bind revive when the runtime map is not marked as an instance.
- Added bind/instance revive restore constants and helper methods beside the existing kisk revive restore path.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Dead-player logout branch now revives after kisk offline binding. Full logout service ordering, duel-loss branch, packet fanout, soul sickness, and instance handler `onReviveEvent` remain partial. |
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `Aion.GameServer.Services.PlayerReviveRestoreService` | Service | Partial | Unit Tested / Regression Tested | Partial Parity | Bind/instance 25% restore constants and state/resource restore are covered; full Java revive side effects remain incomplete. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `Aion.GameServer.Services.PlayerTeleportService` | Service | Partial | Unit Tested / Regression Tested | Partial Parity | Logout revive uses existing bind-location and same-instance teleport mutations. Full teleport packet/task lifecycle remains partial. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LeavePlayerWorldAsync_DeadOpenWorldPlayerBindRevivesLikeJavaLogoutBranch` | Regression | Java `PlayerLeaveWorldService.leaveWorld` and `PlayerReviveService.bindRevive`. | Dead open-world logout restores 25% resources, clears dead/resurrection/aggro state, resets DP, and moves to bind point. | Focused connection workflow test. | Does not cover soul-sickness packet/effect side effects. |
| `LeavePlayerWorldAsync_DeadInstancePlayerUsesInstanceStartBeforeBindLikeJavaInstanceRevive` | Regression | Java `PlayerReviveService.instanceRevive` start-position branch. | Dead instance logout uses modeled runtime instance start position instead of bind point. | Focused connection workflow test with runtime map state. | Does not cover instance handler `onReviveEvent` consuming the revive. |
| `LeavePlayerWorldAsync_DeadSpecialMapFallsBackToBindWhenMapIsNotInstanceTypeLikeJava` | Regression | Java leave-world special `400030000` routing plus `PlayerReviveService.instanceRevive` map-type check. | Special map does not use a start position unless the map is an instance type; non-instance map falls back to bind revive. | Focused connection workflow test. | Does not prove actual Java map template metadata beyond reviewed branch logic. |

## Validation Decision
- Changed surface: production connection dispatch plus revive restore helper and focused tests.
- Specific behavior/contract: dead-player leave-world revive branch follows Java routing after kisk logout, including instance-map start-position revive and bind fallback.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests|FullyQualifiedName~PlayerReviveRestoreServiceTests|FullyQualifiedName~PlayerReviveCleanupAdapterServiceTests|FullyQualifiedName~PlayerTeleportServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: production connection/world-state behavior changed.
- Broad .NET decision: skipped after focused connection, revive-restore, cleanup-adapter, and teleport-service tests passed; no focused evidence exposed wider risk.
- Why this scope is sufficient: the edited connection tests exercise the new logout branch and adjacent revive/teleport classes cover the helper behavior used by the branch.

## Validation Result
- Initial focused command failed during test compile because `Assert.NotNull(...)` was incorrectly used as an expression in two new tests; fixed before rerun.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests|FullyQualifiedName~PlayerReviveRestoreServiceTests|FullyQualifiedName~PlayerReviveCleanupAdapterServiceTests" --no-restore`
  - Passed after fix: 24 tests, 0 failed, 0 skipped.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests|FullyQualifiedName~PlayerReviveRestoreServiceTests|FullyQualifiedName~PlayerReviveCleanupAdapterServiceTests|FullyQualifiedName~PlayerTeleportServiceTests" --no-restore`
  - Passed: 30 tests, 0 failed, 0 skipped.
  - Pre-existing nullable/analyzer warnings were emitted during the compile-producing run.

## Known Remaining Gaps
- Full Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside the dead-player branch.
- Java `PlayerReviveService.instanceRevive` can be consumed by `InstanceHandler.onReviveEvent`; the C# logout branch does not yet model that hook.
- Soul sickness, prison/event/Panesterra/vortex revive routing, full revive packet fanout, and exact teleport task/socket ordering remain incomplete.
- Duel-loss logout branch is still not pinned.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 3.
- Total C# artifacts changed in this UOW: 3.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
