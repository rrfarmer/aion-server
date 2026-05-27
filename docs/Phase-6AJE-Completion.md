# Phase 6AJE Completion - Skill Title Emotion Direct Delete Parity

Date: 2026-05-27
Unit of Work: UOW-1429
Status: Complete and committed after validation.

## Scope

Address the skill/title/emotion source-consumption parity gap discovered during UOW-1428. These Java actions direct-delete their source item and do not emit a remaining-stack full item blob, so this unit corrects packet semantics rather than adding cleanup/seal metadata.

## Completed Work

- Performed local parallel work discovery from the UOW-1428 Java explorer findings and current C# handler seams.
- Changed `GameServerConnection.HandleSkillLearnUseItemAsync`, `HandleTitleAddUseItemAsync`, and `HandleEmotionLearnUseItemAsync` to persist direct source deletion instead of stack decrement/update.
- Added `DeleteDirectSourceItemAsync` to model the packet-visible Java direct-delete behavior: remove source object, send `SM_DELETE_ITEM` default delete mask `0x00`, then send `SM_CUBE_UPDATE`.
- Added focused connection tests using stackable fixture sources to prove skill/title/emotion remove the whole source object and do not send `SM_INVENTORY_UPDATE_ITEM`.
- Updated `docs/PHASE-6-PROGRESS.md`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_SkillBookDirectDeletesSourceLikeJava|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_TitleCardDirectDeletesSourceLikeJava|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_EmotionCardDirectDeletesSourceLikeJava"`.
- Result: passed 3 tests.

## Migration Parity Table - UOW-1429

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.SkillLearnAction` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleSkillLearnUseItemAsync` / `Aion.GameServer.Services.SkillLearnService` | Item Action / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Java broadcasts usage animation, learns skill, then direct-deletes the source item with default delete mask `0x00` plus cube update. C# now removes the whole source object and sends the same delete/cube packet family. Skill effect application and Java runtime bytes remain unverified. |
| `com.aionemu.gameserver.model.templates.item.actions.TitleAddAction` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleTitleAddUseItemAsync` / `Aion.GameServer.Services.TitleAddService` | Item Action / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Java broadcasts animation, adds title, sends title side effects, then direct-deletes the item. C# now direct-deletes the source with default mask and cube update. Title expiration timing/lifecycle and runtime bytes remain unverified. |
| `com.aionemu.gameserver.model.templates.item.actions.EmotionLearnAction` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleEmotionLearnUseItemAsync` / `Aion.GameServer.Services.EmotionLearnService` | Item Action / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Java broadcasts animation, adds emotion, sends emotion list update, then direct-deletes the item. C# now direct-deletes the source with default mask and cube update. Expirable emotion lifecycle and runtime bytes remain unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage.delete` | `Aion.GameServer.Network.Aion.GameServerConnection.DeleteDirectSourceItemAsync` | Storage / Packet Caller | Partial | Regression Tested | Partial Parity | Helper models the packet-visible Java direct-delete behavior for these item actions: remove object, `SM_DELETE_ITEM` mask `0x00`, then cube-size update. Repository transaction semantics and Java failure edges remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` default delete type | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` default constructor path | Packet Serialization | Complete | Regression Tested | Partial Parity | Focused tests assert default delete type `0` for skill/title/emotion source deletes. No Java runtime byte capture was available. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_SkillBookDirectDeletesSourceLikeJava` | Regression / connection packet serialization | `SkillLearnAction`, `Inventory.delete`, `SM_DELETE_ITEM`, `SM_CUBE_UPDATE` | Skill book source object is removed entirely, no source full update is sent, delete mask is `0`, and cube size refresh follows. | C# packet parsing against reviewed Java direct-delete packet shape. | No Java runtime bytes; skill effects and broader packet side effects not covered. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_TitleCardDirectDeletesSourceLikeJava` | Regression / connection packet serialization | `TitleAddAction`, `Inventory.delete`, `SM_DELETE_ITEM`, `SM_CUBE_UPDATE` | Title card source object is removed entirely after title packets, delete mask is `0`, and cube size refresh follows. | C# packet parsing against reviewed Java direct-delete packet shape. | No Java runtime bytes; expiration lifecycle not covered. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_EmotionCardDirectDeletesSourceLikeJava` | Regression / connection packet serialization | `EmotionLearnAction`, `Inventory.delete`, `SM_DELETE_ITEM`, `SM_CUBE_UPDATE` | Emotion card source object is removed entirely after emotion list packet, delete mask is `0`, and cube size refresh follows. | C# packet parsing against reviewed Java direct-delete packet shape. | No Java runtime bytes; expiration lifecycle not covered. |

## Remaining Risks

- Skill/title/emotion side-effect ordering is only packet-shape tested at the connection level; Java runtime byte capture remains unavailable.
- Repository transaction behavior can still differ from Java's direct in-memory delete sequence.
- Title/emotion expiration lifecycle and skill effect application remain broader Phase 6 gaps.
- Toy-pet source cleanup/seal metadata remains unwired and needs dedicated scheduling/world-spawn packet-order analysis.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 C# direct-delete source helper and 3 handler call sites changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, title/emotion expiration lifecycle, skill effect application, repository transaction semantics, toy-pet scheduling/source packet convergence
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: inspect and wire another Java-confirmed remaining-stack full-update source caller.
- Why: direct-delete/no-blob paths are now separated from cleanup/seal metadata; the next cleanup/seal work should target paths that truly emit `SM_INVENTORY_UPDATE_ITEM` full blobs.
- Candidate files:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - targeted test file for selected path, likely `GameServerConnectionInventoryExpansionUseItemTests.cs`

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Toy-pet source consume readiness | read-only Java/C# toy-pet/kisk sources | Medium | Map 10s scheduling, cancel observer, completion animation before consume, source update/delete/cube, and kisk spawn/register order. |
| B | Extraction/assembly/composition source update audit | read-only Java/C# item-action sources | Low/Medium | Existing C# source/part `SmInventoryUpdateItem` calls still appear to need cleanup/seal context; confirm Java masks first. |
| C | Admin/house dye source cleanup audit | read-only Java/C# source | Medium | Candidate source full update callers, but may cross admin/housing seams. |
| D | Armsfusion readiness audit | read-only Java/C# source | Medium | Service planner exists, but live handler availability still needs confirmation. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Analyze extraction/assembly/composition Java source update packet order | Java item-action/storage/packet sources, read-only | all writes, docs, commits |
| Explorer B | Analyze toy-pet Java/C# readiness | Java/C# toy-pet/kisk sources, read-only | all writes, docs, commits |
| Orchestrator | Implement one selected cleanup/seal source full-update slice after analysis | selected production/test files only | shared docs until validation; unrelated files |

## Do Not Parallelize

- `GameServerConnection.cs` implementation changes.
- Shared item-use test fixture edits.
- Shared progress/handoff/audit docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1429] Align skill title emotion source deletion`.
- Java source of truth for this unit:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/SkillLearnAction.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/TitleAddAction.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/EmotionLearnAction.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_DELETE_ITEM.java`
- C# files changed in this unit:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJE-Completion.md`
