# Phase 6HM Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6HL and covers Session 709.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionKiskReviveWorkflowTests|PlayerKiskReviveServiceTests|PlayerKiskRemovalRuntimeCleanupServiceTests|PlayerKiskLifetimeServiceTests"`
  - Result: Passed, 8 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1276 tests.

## Recent Work Completed

### Session 709 - Depleted Kisk Revive Cleanup Coverage

- Added `GameServerConnectionKiskReviveWorkflowTests.HandleReviveAsync_LastKiskReviveChargeRemovesKiskAfterUpdate`.
- Refactored the connection workflow test fixture to register runtime/world kisk state with explicit `maxResurrects`.
- The production `CM_REVIVE` last-charge kisk route now has coverage for:
  - final resurrection charge consumption,
  - caller `SM_KISK_UPDATE`,
  - runtime kisk registry removal,
  - kisk world-object removal,
  - revive restore after deletion intent,
  - direct teleport to the previously captured kisk position,
  - same-map no-registry teleport packet intent.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_REVIVE` | `Aion.GameServer.Network.Aion.ClientPackets.CmRevive` / `GameServerConnection.HandleReviveAsync` | Client Packet / Handler | Partial | Regression Tested | Needs Verification | Production kisk revive route now covers both non-depleted and last-charge depleted branches. Other revive ids, invalid-id behavior, encrypted parser-to-handler execution, and live-client behavior remain outside this unit. |
| `com.aionemu.gameserver.model.gameobjects.Kisk.resurrectionUsed` | `PlayerKiskResurrectionService.UseResurrection` / `PlayerKiskReviveService.TryUseKiskRevive` | Runtime Model / Kisk State | Partial | Regression Tested | Needs Verification | Last-charge route now proves final charge consumption flips the revive result to deletion intent and leaves remaining resurrections at zero. Java method mutates a live `Kisk`, broadcasts from known-list helpers, and calls controller deletion; C# coverage is source-derived but not Java-runtime compared. |
| `com.aionemu.gameserver.controllers.KiskController.delete` | `GameServerConnection.RemoveRuntimeKiskAsync` / `PlayerKiskLifetimeService.DespawnExpiredKisk` | Controller / Runtime Cleanup | Partial | Regression Tested | Needs Verification | `CM_REVIVE` depleted branch now proves registry and world-object removal after final revive charge. ID release is not asserted in this no-IDFactory fixture, and controller AI/death hooks, threading/task cancellation, and live known-list cleanup remain partial. |
| `com.aionemu.gameserver.services.KiskService.removeKisk` | `PlayerKiskRemovalRuntimeCleanupService.ApplyAsync` | Service / Cleanup Fanout | Partial | Regression Tested indirectly | Needs Verification | Handler reaches removal through the depleted branch, but this no-registry workflow only verifies registry/world cleanup. Creator final update, member bind reset, dead-member `SM_DIE`, offline bind cleanup, pending request clearing, NPC visibility refresh, and race/member fanout require registry-backed tests or live-client comparison. |
| `com.aionemu.gameserver.services.player.PlayerReviveService.kiskRevive` | `GameServerConnection.HandleReviveAsync` plus `PlayerKiskReviveService.TryUseKiskRevive` | Service / Revive Workflow | Partial | Regression Tested | Needs Verification | Last-charge test proves Java's delete-after-resurrection-used path does not prevent C# restore/teleport using the captured kisk position. Prison/event branches, stat visual refresh through live registry, unset res-position state, no-resurrect-penalty live effect detection, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_KISK_UPDATE` / teleport packets | `SmKiskUpdate`, `SmChannelInfo`, `SmPlayerSpawn`, `SmPlayerInfo`, `SmStatsInfo`, `SmMotion` | Server Packet Fanout | Partial | Regression Tested indirectly | Needs Verification | Packet type sequence is asserted for the direct no-registry depleted route. Java golden bytes, encrypted frames, live visible-player/member broadcasts, exact socket order with registry fanout, serialization comparisons, and real client rendering remain unverified. |

## Tests Added Or Updated

- `GameServerConnectionKiskReviveWorkflowTests.HandleReviveAsync_LastKiskReviveChargeRemovesKiskAfterUpdate`
  - Validates that a production `CM_REVIVE` kisk route using the final resurrection charge sends the caller update, removes runtime/world kisk state, and still restores/teleports the player.
- `GameServerConnectionKiskReviveWorkflowTests.HandleReviveAsync_KiskReviveConsumesChargeRestoresAndTeleports`
  - Updated to share an explicit kisk registration helper.
- Existing `PlayerKiskReviveServiceTests`, kisk lifetime/removal tests matched by the focused filter, and the connection workflow tests were rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated golden bytes, registry-backed final creator/member fanout, encrypted frames, full socket-order capture, Java controller/AI deletion hooks, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 6
- Total artifacts ported or partially modeled in this handoff window: 1 depleted kisk revive cleanup coverage slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: encrypted socket processor comparison, registry-backed creator/member fanout, Java controller/AI side-effect comparison, other revive-type routing, and live-client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- The depleted workflow test uses a direct handler call and no connection registry, so it does not prove encrypted frame parsing, visible-player broadcasts, final creator update, member bind reset, dead-member revive option refresh, or NPC visibility refresh.
- ID release on depleted revive is covered elsewhere through `RemoveRuntimeKiskAsync` with an `IDFactory`, but not asserted by this direct revive workflow fixture.
- Java `KiskController.delete` may run AI/death side effects not represented by the current C# removal path.
- Other revive types, invalid revive ids, prison/event kisk branches, live no-resurrect-penalty effect detection, and unset res-position state remain partial.
- Packet serialization, exact socket order with registry fanout, and live client behavior remain unverified.

## Next Recommended Unit of Work

Continue kisk lifecycle parity with a registry-backed depleted-kisk cleanup/fanout workflow test that verifies final creator `SM_KISK_UPDATE`, member bind-point reset, dead-member `SM_DIE`, pending request clearing, and NPC visibility refresh where supported, or move to another Phase 6 gap such as charge/power-shard/idiani burn hooks or loot/drop handler-side quest/event paths.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6HL-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
