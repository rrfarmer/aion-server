# Phase 6 Session 2445 Completion

## UOW

[Phase 6] UOW-2445: Wire shared league runtime through game socket composition

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/league/LeagueService.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupServiceCollectionExtensionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameClientSocketServerSmokeTests.cs`

## Implementation Notes

- `GameClientSocketServer` now owns a shared `PlayerLeagueRuntime`, mirroring the existing socket-owned `PlayerGroupRuntime` and `PlayerAllianceRuntime`.
- Socket-created `GameServerConnection` instances now receive that shared league runtime instead of constructing per-connection fallback league state.
- The find-group singleton graph composition test now verifies the DI-provided `PlayerLeagueRuntime` reaches the socket.
- A socket smoke test opens a real accepted connection and verifies the created `GameServerConnection` receives the same shared league runtime.
- This UOW did not enable the alliance offline-timeout scheduler callback in production startup.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AddFindGroupSingletonGraph_RegistersSharedSocketRuntimeServices` | Unit/composition | `LeagueService` static league map source review | Shared find-group graph provides the socket with the same singleton `PlayerLeagueRuntime` used by timeout dispatch composition | Reflection confirms the socket private runtime field is the service provider instance | Production scheduler callback remains disabled |
| `GameClientSocketServer_AcceptedConnectionUsesSharedLeagueRuntimeLikeJavaService` | Socket smoke/composition | `LeagueService` static `ConcurrentHashMap<Integer, League>` | Accepted game client connections receive the socket-owned shared league runtime | Test connects to the socket, locates the accepted `GameServerConnection`, and verifies the private runtime instance is shared | Does not run login/player packet workflows |

## Validation Decision

- Changed surface: socket composition and runtime pass-through only.
- Specific behavior/contract: game socket composition uses one shared league runtime for accepted connections, aligning C# state ownership with Java's shared `LeagueService` map.
- Focused C# commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupServiceCollectionExtensionsTests|FullyQualifiedName~GameClientSocketServerSmokeTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests" --no-restore
```

- Result: Passed, 12 tests. Existing nullable/analyzer warnings were emitted.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore
```

- Result: Passed, 69 tests.
- Note: an earlier parallel run of the second command failed with `CS2012` because another `dotnet test` process held `Aion.GameServer.dll`; rerunning sequentially passed.
- Focused Java/Maven command: skipped; no Java source or Java fixtures changed, and Java source review identified the singleton league-state contract under test.
- Broad-validation trigger: none. This was a non-live composition audit and did not enable production scheduler startup or alter packet serialization.
- Broad .NET decision: skipped; the focused tests exercised the affected socket path, DI graph, timeout dispatcher adjacency, and existing league player-status behavior.

## Additional Hygiene

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings for touched C# and docs files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.league.LeagueService.leagues` | `Aion.GameServer.Services.PlayerLeagueRuntime` through `GameClientSocketServer` and `GameServerConnection` | Runtime state/composition | Partial | Unit/Socket Tested | Partial Parity | Socket-created connections now share the same league runtime instance instead of falling back to per-connection league state. |
| `com.aionemu.gameserver.model.team.league.LeagueService` | `Aion.GameServer.Services.FindGroupServiceCollectionExtensions` | DI/service graph | Partial | Unit Tested | Partial Parity | Shared graph exposes one `PlayerLeagueRuntime` to the socket and timeout dispatcher composition. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.removePlayer` | `Aion.GameServer.Services.PlayerAllianceOfflineTimeoutDispatchService` | Timeout dispatch dependency | Partial | Adjacent Unit Tested | Partial Parity | Dispatcher already consumes `PlayerLeagueRuntime`; this UOW ensures socket-created league mutations can converge on the same runtime before live scheduling is enabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Production `Program.cs` still calls `AddFindGroupSingletonGraph()` without the alliance timeout scheduler callback.
- No production lifecycle owner has been added for starting/canceling the alliance offline-timeout scheduler.
- Offence-invader VortexService removal remains a result flag and does not execute the Java side effect.
- Group offline timeout checker remains a likely parallel gap.
