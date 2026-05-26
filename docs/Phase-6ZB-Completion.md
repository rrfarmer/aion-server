# Phase 6ZB Completion - UOW-1166 Trade No-Sell System Message Payload

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by adding a concrete no-sell `SM_SYSTEM_MESSAGE` packet-byte prerequisite test for staged BUY and TRADE_IN fallback readiness.

No production code changed, and live sends remain disabled.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ZB-Completion.md`

## Implementation Notes

- Added `SmSystemMessage_BuySellDoesNotSellItem_WritesJavaNoSellPayload`.
- The test pins Java `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM(String) -> new SM_SYSTEM_MESSAGE(1300336, value0)`.
- The exact source-derived C# payload body covers:
  - chat type `25`
  - dialect byte `0`
  - sender object id `0`
  - message id `1300336`
  - one UTF-16LE string parameter
  - zero special parameters
- This is a packet prerequisite only; no production no-sell send wiring was enabled.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmSystemMessage_BuySellDoesNotSellItem_WritesJavaNoSellPayload|SmSystemMessage_WritesDialogTooFarMessages|GameServerConnectionStorageExpansionDialogTests" --nologo` passed 12 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,181 tests.
- First `dotnet test dotnetConversion/AionServer.slnx --nologo` run failed in unrelated `LoginServerHostedServiceTests.StartAsync_LoadsRegistryAndBanListsBeforeOpeningSocketListeners`.
- `dotnet test dotnetConversion/tests/Aion.LoginServer.Tests/Aion.LoginServer.Tests.csproj --filter "StartAsync_LoadsRegistryAndBanListsBeforeOpeningSocketListeners" --nologo` passed 1 test.
- Rerunning `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,388 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet | Partial | Unit Tested | Partial Parity | Exact source-derived no-sell payload body is pinned for default `GOLDEN_YELLOW` constructor path. Java runtime golden vector is still absent. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM` | `SmSystemMessage.BuySellHeDoesNotSellItem` | Packet Factory / Helper | Complete | Unit Tested | Partial Parity | Message id `1300336` and single string parameter serialization are covered by exact C# payload test. Runtime Java packet artifact remains missing, so not Verified Parity. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` `BUY` no-sell branches | `NpcDialogServiceDescriptorKind.SystemMessageDoesNotSellItem` plus `SmSystemMessage.BuySellHeDoesNotSellItem` prerequisite | Service Boundary / Packet Dependency | Partial | Unit Tested | Partial Parity | Descriptor and concrete packet prerequisite are covered separately. Production no-sell sends remain disabled. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` `TRADE_IN` no-template branch | `NpcDialogServiceDescriptorKind.SystemMessageDoesNotSellItem` plus `SmSystemMessage.BuySellHeDoesNotSellItem` prerequisite | Service Boundary / Packet Dependency | Partial | Unit Tested | Partial Parity | Trade-in missing-list boundary and concrete no-sell packet prerequisite are covered separately. Production no-sell sends remain disabled. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | `GamePacketTests.SerializeUnencryptedPayload` comparison convention | Packet Framework / Test Harness | Partial | Unit Tested | Needs Verification | Test compares unencrypted packet body after C# frame header. Java runtime canonical payload convention remains designed but not generated. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.SmSystemMessage_BuySellDoesNotSellItem_WritesJavaNoSellPayload` | Unit / Packet Payload | `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM`; `SM_SYSTEM_MESSAGE.writeImpl` | Exact unencrypted C# payload body for no-sell message id `1300336`, `Merchant` parameter, default chat/sender fields, and no special params. | Deterministic source-derived payload test from reviewed Java write order. | Not a Java runtime-generated golden vector; no production send wiring. |

## Remaining Risks

- Java runtime no-sell packet artifact is still absent.
- Production BUY/TRADE_IN no-sell sends remain disabled.
- C# comparison still uses source-derived body bytes, not Java-generated `canonicalPayloadHex`.
- NPC AI/controller live routing remains incomplete.
- Live `SM_TRADELIST` and `SM_TRADE_IN_LIST` sends remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 concrete no-sell packet prerequisite regression
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: Java runtime no-sell vector, production no-sell sends, Java canonical payload generator, NPC AI/controller routing, and live trade-list sends
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a duplicate-id source fixture regression for `TradeListData`/`GoodsListData` Java last-write-wins behavior, or continue toward Java runtime vector generator implementation planning.

Recommended starting points:
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/TradeListTable.cs`
- Java `game-server/src/com/aionemu/gameserver/dataholders/TradeListData.java`
- Java `game-server/src/com/aionemu/gameserver/dataholders/GoodsListData.java`
- `docs/TradeList-Java-Golden-Vector-Design.md`

Keep live `SM_TRADELIST`, `SM_TRADE_IN_LIST`, and no-sell system-message sends disabled until Java runtime vectors and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: duplicate-id source fixture regression for trade and goods list lookups.
- Why: current corpus tests compare counts and selected field rows, but Java `afterUnmarshal` maps overwrite duplicates by id. A small XML fixture can lock that collection-order behavior without requiring Java runtime tooling.
- Files: `StaticDataLoadingTests.cs`, progress/handoff docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java vector generator implementation sketch | docs only | Low | Continue from `TradeList-Java-Golden-Vector-Design.md`. |
| B | Live legion-level lookup discovery | read-only code search/docs | Low | Avoid production wiring until a C# legion aggregate exists. |
| C | Trade-list live-send readiness audit | docs only | Low | Must keep sends disabled. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Duplicate-id fixture regression | `StaticDataLoadingTests.cs`, docs | packet files, live send wiring |
| Agent A | Java vector generator sketch | separate docs file only | code files, progress/handoff docs |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
