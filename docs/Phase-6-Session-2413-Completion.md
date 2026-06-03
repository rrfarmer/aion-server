# Phase 6 Session 2413 Completion - Leave-World Pending Request Denial

## Scope
- Audited Java leave-world request cancellation around `player.getResponseRequester().denyAll()`.
- Confirmed C# already models pending question denial and typed pending-slot cleanup in `PlayerEnterWorldService.LeaveWorldAsync(...)`.
- Added connection-boundary regression coverage proving `GameServerConnection.LeavePlayerWorldAsync(...)` reaches that Java-style pending request cleanup when an enter-world service is wired.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - `leaveWorld(Player player)`
- `game-server/src/com/aionemu/gameserver/utils/response/ResponseRequester.java`
  - `denyAll()`
- `game-server/src/com/aionemu/gameserver/utils/response/RequestResponseHandler.java`
  - `handle(Player responder, int response)`

## Implemented
- Extended `GameServerConnectionKiskReviveWorkflowTests.KiskReviveWorkflowFixture` with optional `PlayerEnterWorldService` injection.
- Added `LeavePlayerWorldAsync_WithEnterWorldServiceDeniesPendingQuestionsLikeJavaDenyAll`.
- The new test seeds a pending friend invite question, logs out through `GameServerConnection.LeavePlayerWorldAsync(...)`, and verifies:
  - player offline state is applied;
  - the player is removed from the world through `PlayerEnterWorldService.LeaveWorldAsync(...)`;
  - `ResponseRequester` is cleared;
  - the typed `PendingFriendRequest` bridge slot is cleared;
  - the requester receives the modeled denial side-effect packet.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Connection logout now has boundary coverage proving it delegates to service-level pending question denial when the enter-world service is injected. |
| `com.aionemu.gameserver.utils.response.ResponseRequester` | `Aion.GameServer.Services.QuestionResponseRegistry` | Runtime request registry | Partial | Unit Tested / Regression Tested | Partial Parity | Existing `DenyAll()` clears active requests and returns denial dispatches for modeled C# side effects. |
| `com.aionemu.gameserver.utils.response.RequestResponseHandler` | `Aion.GameServer.Services.PlayerEnterWorldService` | Logout cleanup bridge | Partial | Regression Tested | Partial Parity | Existing logout cleanup sends migrated denial side effects for modeled request kinds and clears typed pending request fields. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LeavePlayerWorldAsync_WithEnterWorldServiceDeniesPendingQuestionsLikeJavaDenyAll` | Regression | Java `PlayerLeaveWorldService.leaveWorld` calls `ResponseRequester.denyAll()` during logout. | Connection logout reaches `PlayerEnterWorldService.LeaveWorldAsync(...)` pending question denial and clears the modeled friend-invite request state. | Focused connection workflow test plus existing service/registry tests. | Covers one modeled request kind at the connection boundary; broader request-kind side effects remain service-level coverage. |

## Validation Decision
- Changed surface: test harness and regression test only.
- Specific behavior/contract: connection leave-world path delegates to service-level Java-style pending request denial when a `PlayerEnterWorldService` is present.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests|FullyQualifiedName~QuestionResponseRegistryTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no Java source or fixture changed.
- Broad-validation trigger: none; no production code changed.
- Broad .NET decision: skipped because the test-only change was validated with the directly affected workflow and adjacent service/registry tests.
- Why this scope is sufficient: the new regression drives the exact connection path under test, while adjacent service and registry tests cover the already-modeled cleanup semantics.

## Validation Result
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests|FullyQualifiedName~QuestionResponseRegistryTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore`
  - Passed: 62 tests, 0 failed, 0 skipped.
  - Pre-existing nullable/analyzer warnings were emitted during the compile-producing run.

## Known Remaining Gaps
- Full Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial.
- The connection-boundary test covers friend-invite denial; other modeled pending request kinds remain covered at the service/registry level.
- Unmodeled Java request handlers still need targeted slices before they can be claimed.
- Inventory/warehouse/account-warehouse owner cleanup at the end of Java leave-world remains unpinned.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 3.
- Total C# artifacts changed in this UOW: 1.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
