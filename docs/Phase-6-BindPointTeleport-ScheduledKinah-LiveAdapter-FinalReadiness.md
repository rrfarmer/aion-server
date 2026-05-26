# Phase 6 Bind-Point Teleport Scheduled Kinah Live Adapter Final Readiness

Date: May 26, 2026
Unit of Work: UOW-1244
Scope: Documentation-only readiness audit for scheduled Kinah live adapter prerequisites.
Source of truth: Java project.

## Readiness Result

The scheduled bind-point Kinah callback is still not ready for live `GameServerConnection` dispatch. The C# port now has a strong non-live chain for mutation ownership, persistence decisions, packet intent, send-result gating, rollback/commit policy, and Java send-before-runtime ordering. The remaining blockers are executable live adapters and Java known-list/movement parity.

Do not wire live action `1` scheduled Kinah execution yet.

## Satisfied Non-Live Gates

| Gate | Current C# Artifact | Evidence | Status |
|---|---|---|---|
| In-memory owner mutation/rollback | `BindPointTeleportKinahInventoryOwnerService` | Unit tested | Satisfied as non-live owner only |
| Owner result callback bridge | `BindPointTeleportKinahInventoryOwnerCallbackBridgeService` | Unit tested | Satisfied as metadata |
| Persistence operation contract | `BindPointTeleportKinahPersistenceOperationPlanService` | Unit tested | Satisfied as supplied-result contract |
| Persistence decision gate | `BindPointTeleportKinahPersistenceDecisionBridgeService` | Unit tested | Satisfied as metadata |
| Packet intent | `BindPointTeleportKinahInventoryUpdatePacketPlanService` | Unit tested | Satisfied as non-sending intent |
| Disabled send boundary | `BindPointTeleportKinahInventorySendAdapterPlanService` | Unit tested | Satisfied as disabled boundary |
| Send-result decision | `BindPointTeleportKinahInventorySendResultPlanService` | Unit tested | Satisfied as supplied-result decision |
| Owner rollback/commit | `BindPointTeleportKinahOwnerRollbackPlanService` | Unit tested | Satisfied as metadata plus owner rollback execution in integration |
| Full owner outcome integration | `BindPointTeleportKinahOwnerCallbackOutcomeIntegrationService` | Unit tested | Satisfied as non-live integration |
| Send-before-runtime ordering | `BindPointTeleportKinahSendBeforeRuntimeOrderingService` | Unit tested | Satisfied as non-live ordering gate |

## Remaining Live Blockers

| Blocker | Why It Blocks Live Wiring | Next Safe Work |
|---|---|---|
| SQL repository execution | Java dirty inventory persistence differs from C# owner-checked row contract; C# has no executable adapter here. | Add a disabled/opt-in repository adapter seam with integration tests, still not called from `GameServerConnection`. |
| Packet send adapter | C# has disabled and supplied send metadata only. | Add an opt-in send adapter that can call `SendPacketAsync` behind an explicit disabled-by-default gate. |
| Known-list fanout parity | Java bind-point broadcasts are self-first plus known-list membership; current C# registry/distance fanout is only an approximation. | Add known-list-backed design or characterization tests before using runtime fanout for live parity claims. |
| Final movement execution | Java movement includes action abort/despawn/spawn/pet/callback packet ordering through `TeleportService.teleportTo`; C# has metadata only. | Defer live movement until movement side-effect adapter work is complete. |
| `GameServerConnection` dispatch | Live dispatch would combine all unresolved side effects in one high-risk path. | Keep disabled until SQL, send, fanout, and movement gates are isolated and tested. |

## Migration Parity Table - UOW-1244

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | non-live bind-point Kinah planner/owner/outcome/ordering chain | Service / Callback Chain | Partial | Regression Tested | Needs Verification | Non-live chain is audited as ready for live-adapter work, not live dispatch. |
| `com.aionemu.gameserver.dao.InventoryDAO` | future bind-point Kinah repository adapter | Repository / Persistence | Partial | Unit Tested | Needs Verification | Persistence contract exists, but no SQL executes. Java dirty persistence behavior differs. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | future live inventory send adapter | Packet Utility / Send Boundary | Partial | Unit Tested | Needs Verification | Packet intent/send-result metadata exists; no live send or Java runtime comparison. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket` | `BindPointTeleportRuntimeFanoutService`; future known-list parity gate | Network Utility / Fanout | Partial | Regression Tested | Needs Verification | Registry/distance fanout is not proven equivalent to Java known-list self-first fanout. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo` | `BindPointTeleportFinalMovementPlanService`; `BindPointTeleportTeleportToSideEffectPlanService` | Movement Service | Partial | Unit Tested | Needs Verification | Movement remains metadata-only. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 readiness audit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live SQL adapter, 1 live send adapter, 1 known-list fanout gate, 1 live movement adapter, and 1 `GameServerConnection` dispatch path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- This unit is documentation-only.
- No Java runtime comparison was executed.
- Live side effects remain disabled.
- C# persistence/send/rollback policy remains intentionally different from Java dirty storage timing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, packet-order, dirty-state persistence, threading, known-list fanout, and movement parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add the bind-point Kinah SQL repository adapter seam as disabled/opt-in metadata execution. It should consume `BindPointTeleportKinahPersistenceOperationPlan`, map affected rows and exceptions through the existing result statuses, and remain unwired from `GameServerConnection`.

## Update After UOW-1245

The disabled/opt-in owner-checked SQL repository adapter seam now exists:

- `BindPointTeleportKinahSqlPersistenceAdapterService`
- `IBindPointTeleportKinahPersistenceRepository`
- `EmptyBindPointTeleportKinahPersistenceRepository`
- `MySqlBindPointTeleportKinahPersistenceRepository`

It remains unwired from `GameServerConnection` and is not registered as a live scheduled Kinah path. The SQL blocker is reduced from "no executable seam" to "no opt-in live wiring or DB integration validation." The next safe blocker is a disabled/opt-in inventory packet send adapter.

## Update After UOW-1246

The disabled/opt-in inventory packet send adapter seam now exists:

- `BindPointTeleportKinahInventorySendAdapterService`
- extended `BindPointTeleportKinahInventorySendAdapterStatus` for `MissingConnection`, `Sent`, and `Failed`

It consumes existing packet intent and can call `IGameClientConnectionRegistry.SendPacketToPlayerAsync` only when explicitly enabled. It remains unwired from `GameServerConnection`. The packet-send blocker is reduced from "no executable seam" to "no live dispatch wiring and no Java packet capture validation."

## Update After UOW-1248

The expected Java known-list fanout shape is now modeled as non-live metadata, but live known-list membership and a source-first executor are still missing. The known-list blocker remains active for live scheduled Kinah dispatch.

## Update After UOW-1249

The C# port now has a metadata-only known-list membership prerequisite:

- `PlayerKnownListMembershipService`
- `BindPointTeleportKnownListFanoutMembershipAdapterService`

This reduces the known-list blocker from "no membership representation" to "no live population/execution." Live scheduled Kinah dispatch remains blocked because source-online gating, per-recipient exception handling, socket send ordering, and `GameServerConnection` wiring are still absent.
