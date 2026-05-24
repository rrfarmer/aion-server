# Phase 6HN Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6HM and covers Session 710.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionKiskReviveWorkflowTests|PlayerKiskRemovalCleanupServiceTests|PlayerKiskReviveServiceTests|PlayerKiskLifetimeServiceTests"`
  - Result: Passed, 11 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1277 tests.

## Recent Work Completed

### Session 710 - Registry-Backed Depleted Kisk Cleanup Fanout

- Added `GameServerConnectionKiskReviveWorkflowTests.HandleReviveAsync_DepletedKiskRunsRegistryCleanupFanout`.
- Extended the kisk revive workflow fixture with an online-player connection registry test double.
- The production `CM_REVIVE` last-charge kisk route now has coverage for:
  - runtime/world kisk removal,
  - final creator `SM_KISK_UPDATE`,
  - online member bound-kisk clearing,
  - member obelisk `SM_BIND_POINT_INFO`,
  - dead-member `SM_DIE` revive option refresh,
  - pending kisk-bind request clearing,
  - NPC visibility refresh after kisk removal.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_REVIVE` | `Aion.GameServer.Network.Aion.ClientPackets.CmRevive` / `GameServerConnection.HandleReviveAsync` | Client Packet / Handler | Partial | Regression Tested | Needs Verification | Production kisk revive route now covers registry-backed cleanup after the last charge is consumed. Other revive ids, invalid-id behavior, encrypted parser-to-handler execution, and live-client behavior remain outside this unit. |
| `com.aionemu.gameserver.model.gameobjects.Kisk.resurrectionUsed` | `PlayerKiskResurrectionService.UseResurrection` / `PlayerKiskReviveService.TryUseKiskRevive` | Runtime Model / Kisk State | Partial | Regression Tested | Needs Verification | Final-charge route is now covered through handler-level cleanup/fanout after deletion intent. Java's live `Kisk` known-list broadcast, controller deletion timing, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.controllers.KiskController.delete` | `GameServerConnection.RemoveRuntimeKiskAsync` / `PlayerKiskLifetimeService.DespawnExpiredKisk` | Controller / Runtime Cleanup | Partial | Regression Tested | Needs Verification | Handler-level test now verifies registry/world removal under an online-player registry. ID release is still not asserted by this fixture, and Java AI/death hooks, thread/task cancellation, and controller event ordering remain partial. |
| `com.aionemu.gameserver.services.KiskService.removeKisk` | `PlayerKiskRemovalRuntimeCleanupService.ApplyAsync` | Service / Cleanup Fanout | Partial | Regression Tested | Needs Verification | Registry-backed workflow now verifies final creator update, online member bind reset, dead-member `SM_DIE`, pending bind request clearing, and NPC visibility refresh. Offline bind cleanup internals, exact Java collection iteration order, live same-race known-list fanout, and client rendering remain unverified. |
| `com.aionemu.gameserver.services.teleport.TeleportService.sendKiskBindPoint` / `sendObeliskBindPoint` | `SmBindPointInfo.Kisk` / `SmBindPointInfo` from removal cleanup | Service / Packet Helper | Partial | Regression Tested indirectly | Needs Verification | Depleted cleanup proves member fallback bind-point packet is sent after kisk removal. Serialized packet bytes, bind-point source precedence against Java, and live client UI behavior remain unverified. |
| `com.aionemu.gameserver.controllers.PlayerController.showResurrectionOptions` / `SM_DIE` | `SmDie` from `PlayerKiskRemovalRuntimeCleanupService.ApplyAsync` | Controller / Server Packet | Partial | Regression Tested indirectly | Needs Verification | Dead member receives represented `SM_DIE` after kisk removal. Java skill/item/instance revive option flags, remaining-kisk-time fields, invasion flag, exact serialization, and live client behavior remain unverified. |
| `com.aionemu.gameserver.world.World.getNpcs` / known-list refresh after kisk delete | `GameWorld.GetNpcs` / `IGameClientConnectionRegistry.RefreshNpcVisibilityAsync` | World / Visibility Fanout | Partial | Regression Tested indirectly | Needs Verification | Test proves cleanup invokes NPC visibility refresh after kisk removal and includes another NPC still present on the map. Java known-list diff behavior, viewer filtering, threading, and exact packet order remain unverified. |

## Tests Added Or Updated

- `GameServerConnectionKiskReviveWorkflowTests.HandleReviveAsync_DepletedKiskRunsRegistryCleanupFanout`
  - Validates production `CM_REVIVE` last-charge kisk revive cleanup with an online registry, final creator update, member bind reset, dead-member revive option refresh, pending request clearing, and NPC visibility refresh.
- Existing connection-level non-depleted/depleted kisk revive workflow tests and service-level kisk cleanup/revive tests matched by the focused filter were rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated golden bytes, exact known-list fanout, encrypted frames, full socket-order capture, Java controller/AI deletion hooks, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 registry-backed depleted kisk cleanup/fanout coverage slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: encrypted socket processor comparison, Java runtime/golden comparison, exact known-list/socket ordering, Java controller/AI side-effect comparison, other revive-type routing, and live-client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- The new registry is a test double, not the real socket registry, so encrypted frame parsing and live socket dispatch remain unverified.
- Exact Java packet ordering across immediate kisk update, final creator update, member bind reset, `SM_DIE`, stat visual refresh, and teleport packets remains source-inferred rather than captured from Java.
- ID release on depleted revive is still covered only by a separate `RemoveRuntimeKiskAsync` test with `IDFactory`, not this combined workflow.
- Java `KiskController.delete` AI/death hooks and known-list internals may perform additional work not represented by this C# cleanup route.
- Other revive types, invalid revive ids, prison/event kisk branches, no-resurrect-penalty live effect detection, unset res-position state, exact serialization, and live client behavior remain partial.

## Next Recommended Unit of Work

Continue kisk/revive parity with one remaining caller-side gap such as live no-resurrect-penalty effect detection into `HandleReviveAsync`, unset res-position state after kisk revive, or ID release assertion in the combined depleted workflow; otherwise pivot to charge/power-shard/idiani burn hooks or loot/drop handler-side quest/event paths.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6HM-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
