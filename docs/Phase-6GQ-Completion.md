# Phase 6GQ Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6GP and covers Session 687.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter PlayerExperienceRecoveryServiceTests`
  - Result: Passed, 10 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1220 tests.

## Recent Work Completed

### Session 687 - Experience Recovery Dialog Slice

- Added recovery dialog action id `35` and routed `CM_DIALOG_SELECT` recovery through represented NPC target/function validation.
- Added experience recovery question id `160011`, typed pending request metadata, and `QuestionResponseRequestKind.ExperienceRecovery`.
- Added `PlayerExperienceRecoveryService` for Java `DialogService` recovery behavior:
  - source-derived recovery price formula,
  - no-recoverable-XP message,
  - duplicate-question busy message,
  - `ResponseRequester` registration and response removal,
  - deny cleanup,
  - accepted Kinah validation,
  - recoverable XP transfer into current XP,
  - recoverable XP reset,
  - represented Kinah decrease and success/stat/inventory packets.
- Added Java system-message factories for recovery-related ids.
- Added logout cleanup for pending experience recovery metadata.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.DialogAction.RECOVERY` | `CmDialogSelect.Recovery` / `GameServerConnection.HandleDialogSelectAsync` | Dialog Action / Handler | Partial | Regression Tested | Needs Verification | Action id `35` is routed after represented target/function validation. Full Java dialog-engine ordering is unverified. |
| `com.aionemu.gameserver.services.DialogService` recovery branch | `PlayerExperienceRecoveryService.RequestDialog` / `HandleResponse` | Service / Request Handler | Partial | Regression Tested | Needs Verification | Fee formula, question registration, deny, Kinah check, XP restore/reset, and represented packets are modeled. SPECIAL2 effect cleanup and death-count reset are missing. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.resetRecoverableExp` | `Player.Exp` / `Player.RecoverableExp` mutation | Model Mutation | Partial | Regression Tested | Needs Verification | Adds recoverable XP to current XP and clears recoverable XP. Full Java level-change side effects are not represented. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester` | `QuestionResponseRegistry` with `ExperienceRecovery` | Request Registry | Partial | Regression Tested | Needs Verification | Uses put-if-absent and response removal. Anonymous Java callback identity/reflection behavior is unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW.STR_ASK_RECOVER_EXPERIENCE` | `SmQuestionWindow.AskRecoverExperience` | Server Packet / Question Id | Partial | Regression Tested | Needs Verification | Question id `160011` and price parameter are represented. Golden bytes/encrypted frames not compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` recovery constants | `SmSystemMessage` recovery factories | Server Packet / System Message | Partial | Regression Tested | Needs Verification | Adds ids `1300671`, `1300674`, `1300682`, and `1370002`; reuses existing `901285` not-enough-Kinah message. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_STATUPDATE_EXP` | `SmStatUpdateExp` from recovery success | Server Packet / Stat Update | Partial | Regression Tested | Needs Verification | Emitted when experience table is available. Java level/repose/onLevelChange side effects need runtime comparison. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory.decreaseKinah` | `PlayerExperienceRecoveryService` Kinah mutation / `SmInventoryUpdateItem` | Inventory Mutation | Partial | Regression Tested | Needs Verification | Decreases represented cube Kinah item and emits inventory update when Kinah template exists. Broader Kinah storage/persistence semantics unverified. |
| `com.aionemu.gameserver.controllers.effect.EffectController.removeByDispelSlotType` | Not represented | Effect Runtime Dependency | Not Started | No Tests | Unknown | Java clears `DispelSlotType.SPECIAL2`; C# lacks that runtime surface in this slice. |

## Tests Added Or Updated

- `PlayerExperienceRecoveryServiceTests.CalculateRecoveryPrice_UsesJavaDialogServiceFormula`
- `PlayerExperienceRecoveryServiceTests.RequestDialog_RegistersResponseRequesterAndQuestionWindow`
- `PlayerExperienceRecoveryServiceTests.RequestDialog_NoRecoverableExperienceSendsJavaMessage`
- `PlayerExperienceRecoveryServiceTests.RequestDialog_DuplicateQuestionUsesBusyMessageAndLeavesOriginalPendingRequest`
- `PlayerExperienceRecoveryServiceTests.HandleResponse_DenyConsumesPendingRequestWithoutChangingExpOrKinah`
- `PlayerExperienceRecoveryServiceTests.HandleResponse_NotEnoughKinahConsumesQuestionButKeepsRecoverableExp`
- `PlayerExperienceRecoveryServiceTests.HandleResponse_AcceptRestoresExpClearsRecoverableAndDecreasesKinah`

These tests are source-derived from Java. They do not compare against Java runtime execution, golden bytes, encrypted frames, full dialog-engine ordering, effect/death-count cleanup, level-change callback behavior, reflection callback behavior, date/time behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 13
- Total artifacts ported or partially modeled in this handoff window: 1 experience recovery dialog/request/response slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 12
- Total blocked/not-started artifacts: effect dispel-slot cleanup, death-count reset, full level-change side effects, complete inventory Kinah semantics, real socket-order validation, and client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- Recovery-specific SPECIAL2 effect cleanup and death-count reset are still missing.
- C# XP recovery does not yet perform full Java level recalculation callbacks or `PlayerController.onLevelChange` side effects.
- Kinah mutation uses represented cube inventory items; Java Kinah storage/persistence timing needs deeper audit.
- Packet-byte, encrypted-frame, production socket-order, packet-capture, and real-client validation remain unperformed.

## Next Recommended Unit of Work

Continue compact production-reachable `ResponseRequester` parity with another small dialog/handler slice, such as exchange request response or direct portal/RV variants that are not fully covered yet. If staying near experience recovery, add the missing C# runtime fields for soul-sickness/death-count cleanup before deepening level-change side effects.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6GP-Completion.md`
   - this handoff
3. Inspect the selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
