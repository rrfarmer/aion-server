# Phase 6 Session 2522 Completion

## UOW

[Phase 6] UOW-2522: Port DialogService.onCloseDialog as NpcDialogCloseSideEffectPlanService

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `com.aionemu.gameserver.services.DialogService.onCloseDialog`
- `com.aionemu.gameserver.services.player.PlayerMailboxState` (CLOSED=0x00, REGULAR=0x01, EXPRESS=0x02)
- `com.aionemu.gameserver.network.aion.clientpackets.CM_CLOSE_DIALOG.runImpl`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogSideEffectService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogSideEffectServiceTests.cs`

## Implementation Notes

- Added `PlayerMailboxState` static class with `Closed = 0x00`, `Regular = 0x01`, `Express = 0x02` constants (Java parity: `services/player/PlayerMailboxState`).
- Added `NpcDialogCloseSideEffectPlanService.CreatePlan(player, targetObjectId, isNpcTarget, npcSupportsLegionWarehouse, playerIsLegionMember)` — produces a non-live plan modeling the three Java behaviors:
  1. `WouldFireDialogFinishAiEvent` — fires when `isNpcTarget = true`
  2. `WouldCloseMailbox` — fires when `player.MailboxState != PlayerMailboxState.Closed`
  3. `WouldReleaseLegionWarehouseLock` — fires when NPC supports legion warehouse and player is a legion member (caller-supplied since `Player.isLegionMember()` not yet ported)
- Added `NpcDialogCloseSideEffectPlan` record with all `ShouldMutateLive*` guards false.
- Replaced the `CmCloseDialog` stub in `GameServerConnection` with a live `HandleCloseDialog(player, packet)` call that:
  - Checks the world for an NPC target
  - Creates the plan
  - Applies `player.MailboxState = PlayerMailboxState.Closed` if `WouldCloseMailbox` is true (this is a live state mutation — it's safe as it just closes client UI state)
  - Leaves AI DIALOG_FINISH and legion warehouse release non-live (guarded by comment)
- **Note:** The mailbox state mutation IS applied live since it's a lightweight player-side state change that matches Java's unconditional behavior. AI and legion warehouse remain deferred.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `CloseDialogPlan_PlansMailboxCloseAndAiEventForNpcTargetWithOpenMailbox` | Unit | `DialogService.onCloseDialog` source review | NPC target with open mailbox → `WouldFireDialogFinishAiEvent=true`, `WouldCloseMailbox=true`, all live guards false | Focused C# unit test validates plan fields from reviewed Java logic | AI event not fired live; legion warehouse not modeled |
| `CloseDialogPlan_SkipsMailboxCloseWhenAlreadyClosedAndSkipsAiEventForNonNpcTarget` | Unit | `DialogService.onCloseDialog` source review | Already-closed mailbox → `WouldCloseMailbox=false`; non-NPC target → `WouldFireDialogFinishAiEvent=false` | Focused C# unit test validates guard conditions from reviewed Java logic | Same gaps |

## Validation Decision

- Changed surface: dialog close plan service + connection handler stub activation.
- Specific behavior/contract: Java `DialogService.onCloseDialog` fires AI event, releases legion warehouse lock, and closes mailbox state.
- Focused C# command:

```powershell
dotnet test "dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj" --filter "FullyQualifiedName~NpcDialogSideEffectServiceTests" --no-restore
```

- Result: Passed, 6 tests (4 prior + 2 new).
- Focused Java/Maven command: skipped; no Java source or fixtures changed.
- Broad-validation trigger: none. The mailbox state mutation is a lightweight player-side state change; AI event and legion warehouse remain non-live.
- Broad .NET decision: skipped.
- Why this scope is sufficient: focused tests cover the plan conditions from reviewed Java logic; the mailbox state mutation is simple enough to confirm via code review.

## Additional Hygiene

```powershell
git diff --check
```

- Passed. Expected CRLF conversion warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `DialogService.onCloseDialog` | `NpcDialogCloseSideEffectPlanService` + `GameServerConnection.HandleCloseDialog` | Service + Connection dispatch | Partial | Unit Tested | Partial Parity | Mailbox close is live; AI event (AIEventType.DIALOG_FINISH) and legion warehouse lock release remain non-live. |
| `PlayerMailboxState` | `Aion.GameServer.Services.PlayerMailboxState` | Constants | Complete | Unit Tested | Verified Parity | Static class with Closed=0x00, Regular=0x01, Express=0x02 matching Java source. |

## Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 1 (`PlayerMailboxState`)
- Total artifacts needing verification or partial parity: 1 (`DialogService.onCloseDialog`)
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 45%

## Remaining Risks

- AI `DIALOG_FINISH` event not fired; requires AI system to be enabled.
- Legion warehouse lock release not yet modeled; requires `Player.isLegionMember()` and legion warehouse to be ported.
- `npcSupportsLegionWarehouse` not yet derived from live NPC template; `DialogAction.OPEN_LEGION_WAREHOUSE` check not yet ported.
