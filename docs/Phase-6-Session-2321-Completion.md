# Phase 6 Session 2321 Completion - Beshmundir Difficulty Accept Movement

## Scope

Implemented the accepted-response movement branch for Beshmundir's difficulty question when a registered group instance already exists.

Java source reviewed:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/ai/AIActions.java`
- `game-server/src/com/aionemu/gameserver/ai/AIRequest.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/ResponseRequester.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_QUESTION_RESPONSE.java`

Java behavior used:

- `SELECT_NONE_1` / `SELECT_NONE_2` register question id `902050`.
- `CM_QUESTION_RESPONSE` delegates to `ResponseRequester.respond`, which removes the pending request.
- Nonzero response invokes `acceptRequest`.
- The current Java `acceptRequest` branch calls `moveToInstance(responder, (byte) 2)` for registered request id `902050`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Implemented:

- Beshmundir difficulty question responses now use async handling.
- Accepted responses with a pending `BeshmundirDifficultyEnter` request call the Beshmundir portal-use movement helper.
- Denied, missing, or mismatched requests only remove/ignore the request and do not move the responder.
- The focused test sends an actual opcode `50` `CM_QUESTION_RESPONSE` payload.

Known limitation:

- Fresh group-instance allocation is still unported, so this unit proves accepted-response movement only for an already registered group instance.
- Java's difficulty parameter affects fresh allocation selection; the current C# registered-instance path does not consume it yet.

## Validation Decision

- Changed surface: Beshmundir question-response dispatch plus registered group portal continuation reuse.
- Specific behavior/contract: accepted Java Beshmundir difficulty response removes the pending request and calls `moveToInstance`; C# now moves the responder through the portal-use path when a registered group instance exists.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptMovesResponderWhenRegisteredInstanceExists|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultySelectionRegistersQuestionAndReopensDialog|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryMovesNonLeaderWhenGroupMemberInside|FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupInstanceTransfersAndAppliesCooldown|FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupReentryTransfersWithoutCooldown" --no-restore
```

Result: passed 6, failed 0, skipped 0. The filtered command supplied the compile signal for the affected C# project and tests.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this runtime handler request branch; Java source review was the practical source-of-truth evidence.
- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: question-response dispatch and live portal continuation reuse.
- Broad .NET decision: skipped full project/solution validation after focused tests covered the edited request branch, the selection branch, and registered group continuation. No packet primitive or shared serialization code changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `data.handlers.ai.instance.beshmundirTemple.BeshmundirsWalkAI` `AIRequest.acceptRequest` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBeshmundirDifficultyQuestionResponseAsync` | Runtime Handler / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Accepted response now calls movement for registered group instances. Fresh group allocation and difficulty-specific allocation remain unported. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.respond` | `Aion.GameServer.Model.GameObjects.QuestionResponseRegistry.Respond` via `HandleQuestionResponseAsync` | Request Registry / Boundary | Partial | Focused Boundary Tested | Partial Parity | Existing removal semantics reused. This UOW did not re-audit all requester kinds. |
| `data.handlers.ai.instance.beshmundirTemple.BeshmundirsWalkAI.moveToInstance` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBeshmundirsWalkMoveToInstanceAsync` | Runtime Handler Helper | Partial | Focused Boundary Tested | Partial Parity | Reused for accepted difficulty response. Difficulty byte is not yet used because fresh allocation remains unsupported. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptMovesResponderWhenRegisteredInstanceExists` | Boundary Runtime | Java source review of `BeshmundirsWalkAI`, `AIActions`, and `CM_QUESTION_RESPONSE` | Accepted difficulty response removes pending request and moves responder through registered group portal path. | Focused C# runtime packet execution plus Java source review. | Does not cover fresh group allocation, difficulty-specific new instance selection, range observer auto-deny, or real-client encrypted bytes. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported/extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: unchanged, conservatively partial.

## Remaining Gaps

- Fresh group-instance allocation and `registerTeam(group)` remain unported.
- Beshmundir difficulty-specific fresh instance selection remains unverified/unported.
- Java range observer auto-deny behavior for AI requests remains unported.
- Generic portal dialog live routing for registered team plans remains incomplete.
- Real-client or encrypted socket bytes for these Beshmundir branches remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2321] Move Beshmundir accepted difficulty response
```

## Next Recommended UOW

Implement fresh group-instance allocation for `PortalService.port(...)` group paths, including `registerTeam(group)`, so Beshmundir leader difficulty acceptance can create the instance Java creates when no registered group instance exists.
