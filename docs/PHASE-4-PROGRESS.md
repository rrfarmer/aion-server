# Phase 4: Port Chat Server to C#

**Status**: Implementation Started - Phase 4A/4B complete, Phase 4C socket layer smoke-covered  
**Start Date**: May 19, 2026  
**Target Completion**: May 22-23, 2026 (4-5 days, 8 sub-phases)

---

## Implementation Log

### May 20, 2026 - Phase 4A Scaffold

Completed:
- Added `ChatServerOptions` loading from Java chat-server config with `mycs.properties` and environment override support.
- Added chat database option loading for `aion_cs` via the existing `DatabaseFactory`.
- Added core models: `ChatClient`, `Message`, `Race`, `ChannelType`, `Channel`, `RaceChannel`, `RegionChannel`, `TradeChannel`, `LfgChannel`, `JobChannel`, `LangChannel`, and `ChatChannels`.
- Added packet base/factory shells for client and game-server protocols, with opcode tables pinned from Java source.
- Added service layer scaffold: `ChatService`, `GameServerService`, `BroadcastService`, and interfaces.
- Added `ChatLogRepository` using direct SQL against `chatlog`.
- Updated `Aion.ChatServer` startup to use Java config, initialize DB options, register services, and use Java-shaped file logging.
- Added focused tests for config loading, channel parsing/reuse, token generation shape, player connection attach, and game-server auth state.

Validation:
- `dotnet test tests\Aion.ChatServer.Tests\Aion.ChatServer.Tests.csproj` - 7 passed.
- `dotnet test AionServer.slnx` - 186 passed total: 7 chat, 57 commons, 1 game, 121 login.

Discoveries:
- Java chat client packets use a Netty `HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN)`, so the C# packet buffer's little-endian behavior is correct for chat client traffic.
- Frame length is a 2-byte little-endian field that includes the two-byte length header.
- Client auth packet is the retail signature-login shape from `CM_PLAYER_AUTH`, not the simplified field list in the initial planning notes.
- Token generation is `16 random bytes + SHA256(accountName UTF-8 bytes)`, with Java hashing only the first `accountName.length()` bytes of the UTF-8 byte array.
- Game-server bridge opcodes from Java are `0x00` auth, `0x01` player auth, `0x02` player logout, and `0x03` player gag.
- Server packet opcodes from Java are `0x02` player auth response, `0x11` channel response, `0x1A` channel message, and `0x31` chat init.

Open follow-up:
- `JobChannel` has the core class alias structure in C#, but the full localized alias table still needs exact Unicode parity extraction before final channel parity signoff.
- Socket listeners and packet handlers are intentionally deferred to the next slice after this scaffold.

### May 20, 2026 - Phase 4B Packets + Phase 4C Socket Spine

Completed:
- Added all concrete chat client packet models:
  - `CM_CHAT_INI`, `CM_PLAYER_AUTH`, `CM_PING`, `CM_PLAYER_INFO`
  - `CM_CHANNEL_CREATE`, `CM_CHANNEL_JOIN`, `CM_CHANNEL_REQUEST`, `CM_CHANNEL_LEAVE`, `CM_CHANNEL_MESSAGE`
- Added all client-facing server packet models:
  - `SM_PLAYER_AUTH_RESPONSE`, `SM_CHAT_INI`, `SM_CHANNEL_RESPONSE`, `SM_CHANNEL_MESSAGE`
- Added chat game-server bridge packet models:
  - inbound: `CM_CS_AUTH`, `CM_PLAYER_AUTH`, `CM_PLAYER_LOGOUT`, `CM_PLAYER_GAG`
  - outbound: `SM_GS_AUTH_RESPONSE`, `SM_PLAYER_AUTH_RESPONSE`
- Replaced packet factory placeholders with concrete state-aware dispatch.
- Added `ClientChannelHandler` and `GsConnection` socket handlers with Java-style frame reading.
- Added `ClientSocketServer`, `GameServerSocketServer`, and `ChatServerHostedService`.
- Wired chat listeners into `Aion.ChatServer` startup.
- Implemented live client auth, channel request/leave, channel message flood/gag handling, broadcast, and optional chat DB logging path.
- Implemented GS auth, player registration/token response, logout cleanup, and gag updates.

Validation:
- `dotnet test tests\Aion.ChatServer.Tests\Aion.ChatServer.Tests.csproj` - 16 passed.
- `dotnet test AionServer.slnx` - 195 passed total: 16 chat, 57 commons, 1 game, 121 login.

New coverage:
- Packet parity tests for all client, server, and game-server bridge packet shapes added so far.
- Client-facing server packet frame length validation.
- Game-server auth and player registration over loopback TCP.
- Client chat init, auth, and channel request over loopback TCP.
- Two-client channel message broadcast over loopback TCP.

Open follow-up:
- Phase 4C still needs full hosted-listener tests through `ClientSocketServer`/`GameServerSocketServer` rather than direct connection-handler harnesses.
- Phase 4D/4E still need formal handler pipeline types for flood/filter/logging even though the Java-equivalent gag/flood/logging behavior is already present in the socket handler.
- Phase 4F still needs DB integration coverage against the Java `chatlog` schema.

## Goal

Port the Java `chat-server` to C# (`Aion.ChatServer`) with 1:1 **full feature parity** 1including:

- Client socket listener (TCP 10241) for real Aion clients
- Game-server bridge listener (TCP 9021) for Java/C# game servers
- All 9 client packet types + 4 server packet types + 4 game-server inter-server packet types
- 6 channel types (Region, Trade, Race, Job, LFG, Language)
- Player authentication and token generation (48-byte: 16 random + SHA256 hash)
- Channel membership and message broadcasting
- Chat log persistence to MySQL `chatlog` table
- Dynamic handler infrastructure (flood protection, filtering, logging)
- Graceful shutdown and connection cleanup

## Critical Dependency: Phase 3 Must Be Complete

**This phase CANNOT proceed without Phase 3 (C# Login Server) complete and passing all tests.**

Mixed-mode validation and inter-server communication require:
- C# LoginServer running and accepting client connections
- LoginServer correctly forwarding players to Java GameServer
- Phase 3 tests (180 tests) all passing

If Phase 3 is not complete, skip mixed-mode validation (4G) and focus on integration tests (4G packet/service tests) using mock sockets.

## Validation Strategy

1. **Byte-level packet parity**: Golden tests enforce identical serialization to Java
2. **Mixed-mode integration**: C# login server (Phase 3) + Java game server + C# chat server with real client
3. **Real client smoke tests**: Full auth → join channel → send message → leave flow
4. **No regressions**: Phase 3 (login server) tests remain passing
5. **Automated test suite**: Run from Visual Studio; Docker for MySQL and Java game server

---

## Prerequisites & Operational Setup

### Before Starting Phase 4A

1. **Java Chat Server Reference**
   - Source: `chat-server/src/com/aionemu/chatserver/`
   - Entry point: `ChatServer.java`
   - Config: `chat-server/config/main/` and `chat-server/config/network/`
   - Schema: `chat-server/sql/aion_cs.sql`
   - Must be runnable for comparison and golden test vector capture

2. **MySQL Database**
   - Database: `aion_cs` (must exist)
   - Table: `chatlog` (auto-create or pre-populate from `chat-server/sql/aion_cs.sql`)
   - Connection: Via `DatabaseFactory` from `Aion.Commons`
   - For testing: Use Docker MySQL instance (same as Phase 3) or local instance

3. **Configuration Files**
   - **Java defaults** (reference only):
     - `chat-server/config/network/network.properties`: Client socket address (0.0.0.0:10241), GS socket address (0.0.0.0:9021), GS password, NIO thread count
     - `chat-server/config/network/database.properties`: MySQL connection details
   - **C# precedence** (mirror Java):
     1. Load defaults from above Java config files via `ConfigLoader`
     2. Override with `chat-server/config/mycs.properties` if it exists
     3. Override with environment variables if set
   - **Inter-server password**: The `chatserver.network.gameserver.password` MUST match what Java GameServer is configured to send when connecting. Verify this value before startup.

4. **Ports & Connectivity**
   - **C# Chat Server**: Binds to 0.0.0.0:10241 (client connections) and 0.0.0.0:9021 (game-server bridge)
   - **Java Game Server**: Must be able to connect outbound to C# chat on 9021
   - **Real Aion Client**: Must be able to connect to C# chat on 10241
   - **MySQL**: Must be accessible (localhost:3306 by default, or Docker alias)
   - For mixed-mode: C# LoginServer on 2104, Java GS on 7777, C# Chat on 10241/9021, MySQL on 3306

5. **Phase 3 Requirement**
   - C# LoginServer must be running and operational
   - Phase 3 tests must all pass (180 tests)
   - Mixed-mode validation depends on this

6. **Real Client (Optional but Recommended)**
   - Real Aion client binary available for smoke testing
   - Can connect to localhost:10241
   - Can join channels and send messages
   - If not available: Skip Phase 4G real client smoke test, rely on integration tests with mock sockets

### Java Chat Server Config Reference

```properties
# chat-server/config/network/network.properties
chatserver.network.client.socket_address=0.0.0.0:10241
chatserver.network.client.connect_address=0.0.0.0:10241
chatserver.network.gameserver.socket_address=0.0.0.0:9021
chatserver.network.gameserver.password=<PASSWORD>
chatserver.network.nio.threads=1

# chat-server/config/network/database.properties
database.url=jdbc:mysql://localhost:3306/aion_cs?serverTimezone=&characterEncoding=UTF-8
database.user=root
database.password=
database.connectionpool.max=5
database.connectionpool.timeout=5
```

---

## Build & Run Instructions

### Building Aion.ChatServer

```bash
# From project root (c:\Users\ryanf\Documents\GitHub\aion-server)
cd dotnetConversion

# Build the chat server (and dependencies)
dotnet build -c Release src/Aion.ChatServer/Aion.ChatServer.csproj

# Or build entire solution
dotnet build -c Release AionServer.slnx
```

### Running Aion.ChatServer Locally

```bash
# Run directly
dotnet run --project src/Aion.ChatServer/Aion.ChatServer.csproj --no-build

# Or from executable after build
.\src\Aion.ChatServer\bin\Release\net10.0\Aion.ChatServer.exe
```

**Expected output** (successful startup):
```
Aion Chat Server starting...
Loading configuration from chat-server/config/network/network.properties
Initializing database connection to aion_cs
Starting client socket server on 0.0.0.0:10241
Starting game-server bridge socket server on 0.0.0.0:9021
Aion Chat Server started.
```

### Running Tests

```bash
# All chat server tests
dotnet test src/Aion.ChatServer/Aion.ChatServer.csproj -v normal

# Specific test class
dotnet test src/Aion.ChatServer/Aion.ChatServer.csproj --filter ClassName=ChatPacketParityTests

# With code coverage
dotnet test src/Aion.ChatServer/Aion.ChatServer.csproj /p:CollectCoverage=true
```

### Docker Setup (For Mixed-Mode Testing)

Use existing `start-mixed-mode-java.ps1` script (from Phase 3) to start:
- MySQL 8.4 on 3306
- Java GameServer on 7777 connected to MySQL
- Java ChatServer on 10241/9021 (OPTIONAL: can replace with C# once ready)

```bash
# Start Docker services (MySQL + Java GS + Java CS)
powershell -ExecutionPolicy Bypass -File dotnetConversion/scripts/start-mixed-mode-java.ps1 -Build -Detached

# Then start C# LoginServer (Phase 3)
cd dotnetConversion && dotnet run --project src/Aion.LoginServer/Aion.LoginServer.csproj --no-build

# In another terminal, start C# ChatServer (Phase 4)
cd dotnetConversion && dotnet run --project src/Aion.ChatServer/Aion.ChatServer.csproj --no-build

# Connect real Aion client to localhost:2104 (C# login) → 7777 (Java game) → 10241 (C# chat)
```

---

## Implementation Plan: 8 Sub-Phases

### Phase 4A: Infrastructure & Scaffolding

**Objective**: Create config, models, packet base classes, services (stubs), and DI setup.

**Deliverables**:

1. **ChatServerOptions** (`Configuration/ChatServerOptions.cs`)
   - Load from `chat-server/config/network/network.properties` and database properties
   - Keys: `chatserver.network.client.socket_address`, `chatserver.network.gameserver.socket_address`, `chatserver.network.gameserver.password`, `chatserver.network.nio.threads`
   - Implement `LoadFromJavaConfig()` using `ConfigLoader` from `Aion.Commons`

2. **Model Classes**:
   - `ChatClient.cs`: playerId, accountName, nickname, raceId, accessLevel, channels, messageTime tracking, connection state
   - `Message.cs`: channel, sender, text (UTF-16LE bytes), timestamp
   - `Channel.cs` (abstract): ChannelType, GameServerId, ChannelId, members collection, abstract `Matches()` and `Name()`
   - `Channels/RegionChannel.cs`, `TradeChannel.cs`, `RaceChannel.cs`, `JobChannel.cs`, `LfgChannel.cs`, `LangChannel.cs`
   - `ChatChannels.cs` (static factory): ConcurrentDictionary registry, `GetOrCreate(identifier)` with Java pattern matching

3. **Packet Base Classes**:
   - `AbstractClientPacket.cs`: Extends `PacketBuffer`, holds opcode, abstract `Task Run(ChatClient)`, deserialization
   - `AbstractServerPacket.cs`: Extends `PacketBuffer`, holds opcode, serialization, write factory
   - `ClientPacketFactory.cs`: Static dispatcher by opcode → packet type
   - `ServerPacketFactory.cs`: Static dispatcher for server-side packets
   - `GsClientPacket.cs`, `GsServerPacket.cs`: Base for game-server inter-server protocol
   - `GsPacketFactory.cs`: Dispatcher for GS packets

4. **Service Stubs** (to be filled in Phase 4D):
   - `IChatService` + `ChatService`: Player registration, token generation, auth, logout
   - `IGameServerService` + `GameServerService`: GS auth, registration, status
   - `IBroadcastService` + `BroadcastService`: Message distribution, single-client send

5. **Database Layer Stubs**:
   - `Data/Repositories/IChatLogRepository.cs`: `InsertChatLogAsync(sender, message, type)`
   - `Data/Repositories/ChatLogRepository.cs`: Direct SQL via MySqlConnector (to be implemented in Phase 4F)

6. **Program.cs Updates**:
   - Register `ChatServerOptions` singleton from config
   - Register all repositories and services in DI container
   - Placeholder socket server registration (stubs; will implement in 4C)
   - Graceful shutdown handling via `CancellationToken`

**Files to Create**:
```
Configuration/ChatServerOptions.cs
Models/ChatClient.cs
Models/Message.cs
Models/Channel.cs
Models/Channels/RegionChannel.cs
Models/Channels/TradeChannel.cs
Models/Channels/RaceChannel.cs
Models/Channels/JobChannel.cs
Models/Channels/LfgChannel.cs
Models/Channels/LangChannel.cs
Models/Channels/ChatChannels.cs
Network/Packets/AbstractClientPacket.cs
Network/Packets/AbstractServerPacket.cs
Network/Packets/ClientPacketFactory.cs
Network/Packets/ServerPacketFactory.cs
Network/Packets/GameServer/GsClientPacket.cs
Network/Packets/GameServer/GsServerPacket.cs
Network/Packets/GameServer/GsPacketFactory.cs
Services/IChatService.cs
Services/ChatService.cs
Services/IGameServerService.cs
Services/GameServerService.cs
Services/IBroadcastService.cs
Services/BroadcastService.cs
Data/Repositories/IChatLogRepository.cs
Data/Repositories/ChatLogRepository.cs
Program.cs (modified)
```

**Reference Implementation**: Copy DI patterns, packet dispatch structure, and repository interface style from `Aion.LoginServer`.

---

### Phase 4B: Packet Golden Tests

**Objective**: Analyze Java packet opcodes, create golden test vectors, implement all 17 packet types with byte-level parity.

**Deliverables**:

1. **Opcode Discovery** (requires Java source analysis):
   - Extract exact opcode constants for all 9 client packets (CM_*), 4 server packets (SM_*), 4 GS packets (CM_CS_AUTH, etc.)
   - Recommended: `grep -r "OPCODE\|0x[0-9A-F]" chat-server/src/com/aionemu/chatserver/network/aion/`
   - Document in `TEMP-OPCODES.md` (temporary reference; delete after implementation)

2. **Client Packets** (9 types):
   ```
   CM_PLAYER_AUTH        - playerId (D), accountName (S), nickname (S), raceId (C), accessLevel (C)
   CM_CHAT_INI           - settings data (analyze from Java)
   CM_PING               - keep-alive (opcode only)
   CM_PLAYER_INFO        - playerId (D)
   CM_CHANNEL_REQUEST    - channelId (D)
   CM_CHANNEL_CREATE     - channel identifier (S)
   CM_CHANNEL_JOIN       - channelId (D)
   CM_CHANNEL_MESSAGE    - channelId (D), message text (S UTF-16LE)
   CM_CHANNEL_LEAVE      - channelId (D)
   ```

3. **Server Packets** (4 types):
   ```
   SM_PLAYER_AUTH_RESPONSE   - Opcode 0x02, playerId (D), token (48 bytes), success flag (C)
   SM_CHAT_INI               - Opcode ?, settings echo (S)
   SM_CHANNEL_RESPONSE       - Opcode ?, channelId (D), success flag (C)
   SM_CHANNEL_MESSAGE        - Opcode 0x1A, channelId (D), sender nickname (S), message text (S UTF-16LE)
   ```

4. **Game-Server Packets** (4 types):
   ```
   CM_CS_AUTH            - Opcode 0x00, gameServerId (D), password (S)
   CM_PLAYER_AUTH        - Opcode 0x01, playerId (D), accountName (S), nickname (S), raceId (C), accessLevel (C)
   CM_PLAYER_LOGOUT      - Opcode 0x02, playerId (D)
   CM_PLAYER_GAG         - Opcode ?, playerId (D), duration (H)
   SM_GS_AUTH_RESPONSE   - Opcode ?, success flag (C), reason (S)
   SM_PLAYER_AUTH_RESPONSE - Opcode ?, playerId (D), token (48 bytes)
   ```

5. **Golden Test File** (`tests/Aion.ChatServer.Tests/Network/ChatPacketParityTests.cs`):
   - Reference byte vectors (captured from real Java implementation or handcrafted from spec)
   - Test: Deserialize from bytes → validate fields → serialize → compare bytes
   - Edge cases: empty strings, long names (UTF-16LE), special characters
   - Coverage: All 17 packet types (9 client + 4 server + 4 GS)
   - Expected result: All tests pass with 100% byte parity

6. **Implementation** (9 client + 4 server + 4 GS packets):
   - Each class in `Network/Packets/Client/`, `Network/Packets/Server/`, `Network/Packets/GameServer/` subdirectories
   - Deserialization: `ReadC()`, `ReadH()`, `ReadS()` in exact Java order
   - Serialization: `WriteC()`, `WriteH()`, `WriteS()` in exact Java order
   - String encoding: UTF-16LE, null-terminated
   - Integer encoding: Little-endian shorts (H), ints (D), longs (Q)

**Files to Create**:
```
tests/Aion.ChatServer.Tests/Network/ChatPacketParityTests.cs
Network/Packets/Client/CM_PLAYER_AUTH.cs
Network/Packets/Client/CM_CHAT_INI.cs
Network/Packets/Client/CM_PING.cs
Network/Packets/Client/CM_PLAYER_INFO.cs
Network/Packets/Client/CM_CHANNEL_REQUEST.cs
Network/Packets/Client/CM_CHANNEL_CREATE.cs
Network/Packets/Client/CM_CHANNEL_JOIN.cs
Network/Packets/Client/CM_CHANNEL_MESSAGE.cs
Network/Packets/Client/CM_CHANNEL_LEAVE.cs
Network/Packets/Server/SM_PLAYER_AUTH_RESPONSE.cs
Network/Packets/Server/SM_CHAT_INI.cs
Network/Packets/Server/SM_CHANNEL_RESPONSE.cs
Network/Packets/Server/SM_CHANNEL_MESSAGE.cs
Network/Packets/GameServer/CM_CS_AUTH.cs
Network/Packets/GameServer/CM_PLAYER_AUTH.cs
Network/Packets/GameServer/CM_PLAYER_LOGOUT.cs
Network/Packets/GameServer/CM_PLAYER_GAG.cs
Network/Packets/GameServer/SM_GS_AUTH_RESPONSE.cs
Network/Packets/GameServer/SM_PLAYER_AUTH_RESPONSE.cs
```

**Temporary Reference** (to be created during implementation):
- `docs/TEMP-OPCODES.md` — Document extracted opcode constants, string formats, packet layouts. Delete after Phase 4B complete.

**Verification**: `dotnet test ChatPacketParityTests` — all 17 packet types deserialize/serialize with byte parity to golden vectors.

---

### Phase 4C: Network Layer & Socket Servers

**Objective**: Implement client and game-server socket listeners with packet dispatch.

**Connection State Machines**:

**ClientChannelHandler (Client Connection)**:
```
┌─────────────┐
│  CONNECTED  │  ← Initial state after TCP handshake
└──────┬──────┘
       │ CM_PLAYER_AUTH received + token verified
       ▼
┌─────────────┐
│   AUTHED    │  ← Can join channels, send messages
└──────┬──────┘
       │ Connection closed or CM_CHANNEL_LEAVE on last channel
       ▼
┌──────────────────┐
│  DISCONNECTED    │  ← Cleanup: remove from channels, logout
└──────────────────┘
```

**GsConnection (Game-Server Connection)**:
```
┌─────────────┐
│  CONNECTED  │  ← Initial state after TCP handshake from GS
└──────┬──────┘
       │ CM_CS_AUTH received + password verified
       ▼
┌─────────────┐
│   AUTHED    │  ← Can register/logout players
└──────┬──────┘
       │ Connection closed or auth failure
       ▼
┌──────────────────┐
│  DISCONNECTED    │  ← Mark GS offline, cleanup
└──────────────────┘
```

**Deliverables**:

1. **Client Socket Server** (`Network/ClientSocketServer.cs`):
   - Extends `BaseSocketServer` from `Aion.Commons`
   - Listener: `ChatServerOptions.ClientSocketAddress` (default: 0.0.0.0:10241)
   - Per-connection state machine:
     - `CONNECTED`: Initial state, awaiting `CM_PLAYER_AUTH`
     - `AUTHED`: Authenticated, can join channels and message
     - `DISCONNECTED`: Cleanup on close
   - Frame codec: 2-byte LE length prefix (length includes itself), max 16384 bytes
   - Packet dispatch: Deserialize opcode, lookup in `ClientPacketFactory`, run `packet.Run(chatClient)`
   - Error handling: Malformed packets → log warning, optional disconnect or skip
   - Graceful close: Notify `ChatService.PlayerLogoutAsync()` on socket close

2. **Game-Server Bridge Socket Server** (`Network/GameServerSocketServer.cs`):
   - Extends `BaseSocketServer` from `Aion.Commons`
   - Listener: `ChatServerOptions.GameServerSocketAddress` (default: 0.0.0.0:9021)
   - Per-connection handler (`GsConnection.cs`):
     - State: `CONNECTED` → `AUTHED`
     - On `CM_CS_AUTH`: Verify password via `GameServerService.RegisterGameServerAsync()`, respond with `SM_GS_AUTH_RESPONSE`
     - On `CM_PLAYER_AUTH`: Register player via `ChatService.RegisterPlayerWithChannelAsync()`, respond with token
     - On `CM_PLAYER_LOGOUT`: Remove player from `ChatService`, cleanup
     - Async packet executor: 1 cached thread pool for packet handling
   - Graceful close: Notify `GameServerService.SetGameServerOfflineAsync()` on disconnect

3. **Client Channel Handler** (`Network/Handlers/ClientChannelHandler.cs`):
   - Netty-style pipeline handler for per-client connection
   - Deserialize incoming packets, dispatch via `ClientPacketFactory`
   - Catch exceptions: Log malformed packets, don't crash connection
   - Serialize outgoing packets via `ServerPacketFactory`
   - Track connection state (CONNECTED → AUTHED)

4. **Game-Server Connection Handler** (`Network/Handlers/GsConnection.cs`):
   - Netty-style handler for game-server peer connection
   - Deserialize incoming GS packets, dispatch via `GsPacketFactory`
   - Serialize outgoing GS responses
   - Track GS authentication state

5. **Register Hosted Services** (modify `Program.cs`):
   - Add `ClientSocketServer` as `IHostedService`
   - Add `GameServerSocketServer` as `IHostedService`
   - Start on `IHostApplicationLifetime.ApplicationStarted`
   - Stop on `IHostApplicationLifetime.ApplicationStopping`
   - Graceful shutdown: drain connections, flush pending packets

**Files to Create**:
```
Network/ClientSocketServer.cs
Network/GameServerSocketServer.cs
Network/Handlers/ClientChannelHandler.cs
Network/Handlers/GsConnection.cs
```

**Reference Implementation**: Mirror `Aion.LoginServer.Network.ClientSocketServer` and `GameServerSocketServer` structure.

**Verification**: Sockets start on correct ports; malformed packets logged without crash; graceful shutdown works.

---

### Phase 4D: Core Services & Channel Logic

**Objective**: Implement player registration, channel management, message broadcasting, and game-server bridge logic.

**Deliverables**:

1. **ChatService** (`Services/ChatService.cs`):
   - `RegisterPlayerAsync(playerId, accountName, nickname, raceId, accessLevel)`: 
     - Create `ChatClient` instance
     - Generate 48-byte token: 16 random bytes + SHA256(accountName)
     - Store in internal registry
     - Return token and client
   - `RegisterPlayerConnectionAsync(playerId, token)`: 
     - Verify token matches stored value
     - Set client state to `AUTHED`
     - Return client
   - `RegisterPlayerWithChannelAsync(playerId, channelId)`: 
     - Get client from registry
     - Get or create channel via `ChatChannels.GetOrCreate()`
     - Add client to channel members
     - Notify channel of new member (optional broadcast)
   - `PlayerLogoutAsync(playerId)`: 
     - Get client, remove from all channels
     - Delete from registry
     - Notify `BroadcastService` to cleanup
   - `GetPlayerAsync(playerId)`: Retrieve from registry
   - Internal registry: `ConcurrentDictionary<int, ChatClient>`
   - **Parity**: Token generation must match Java's SHA256 algorithm exactly

2. **GameServerService** (`Services/GameServerService.cs`):
   - `RegisterGameServerAsync(gameServerId, password)`: 
     - Verify password against `ChatServerOptions.GameServerPassword`
     - Store GS info in registry
     - Return success/failure
   - `SetGameServerOfflineAsync(gameServerId)`: Mark GS as disconnected
   - `GetGameServerAsync(gameServerId)`: Retrieve from registry
   - `IsGameServerOnlineAsync(gameServerId)`: Boolean check
   - Internal registry: `ConcurrentDictionary<int, GameServerInfo>`
   - **Parity**: Password validation must match Java byte-by-byte

3. **BroadcastService** (`Services/BroadcastService.cs`):
   - `BroadcastMessageAsync(channel, message)`: 
     - Iterate channel members
     - For each member, create `SM_CHANNEL_MESSAGE` packet
     - Send packet to client connection
     - Handle client disconnection gracefully (remove from channel on send failure)
   - `SendMessageAsync(client, packet)`: Send packet to single client
   - `SendMessageToChannelAsync(channelId, packet)`: Send packet to all clients in channel
   - `NotifyChannelMemberAsync(client, channel)`: Notify channel of new member join/leave
   - Maintain active client registry for efficient broadcast

4. **Channel Types** (complete implementation):
   - `Channel.cs` (abstract base):
     - Properties: `ChannelType`, `GameServerId`, `ChannelId`
     - Members collection: `ConcurrentBag<ChatClient>`
     - Abstract methods: `Matches(identifier: string): bool`, `Name(): string`
     - Methods: `AddMember(client)`, `RemoveMember(client)`, `GetMembers()`
   - `RegionChannel.cs`: Matches region patterns (e.g., `@region_ALL`)
   - `TradeChannel.cs`: Matches map identifiers (e.g., `@trade_210010000`)
   - `RaceChannel.cs`: Matches race restrictions (e.g., `@race_Asmodian`)
   - `JobChannel.cs`: Matches class filters (e.g., `@job_Cleric`)
   - `LfgChannel.cs`: Matches LFG group IDs
   - `LangChannel.cs`: Matches language codes (e.g., `@lang_ENG`)

5. **ChatChannels Factory** (`Models/Channels/ChatChannels.cs`):
   - Static registry: `ConcurrentDictionary<int, Channel>`
   - `GetOrCreateAsync(identifier: string): Task<Channel>`:
     - Parse identifier format: `@<type>_<meta>\u0001<gameServerId>.<raceId>.AION.KOR`
     - Iterate channel types, call `Matches(identifier)` on each
     - First match: return existing or create new
     - Fallback: create `RegionChannel` as default
   - **Parity**: Identifier parsing must match Java regex/pattern exactly

6. **Packet Handler Dispatch** (wire into `ClientChannelHandler`):
   - `ClientPacketFactory.Create(opcode, buffer)`: Deserialize and return packet instance
   - Call `packet.Run(chatClient)` → await result
   - Catch exceptions: Log malformed packet, optionally disconnect
   - Handlers call appropriate service methods (e.g., `CM_CHANNEL_JOIN` → `ChatService.RegisterPlayerWithChannelAsync()`)
   - Background tasks (channel join, token generation) run via `Task` without blocking connection

**Files to Create/Modify**:
```
Services/ChatService.cs (full implementation)
Services/GameServerService.cs (full implementation)
Services/BroadcastService.cs (full implementation)
Models/Channels/ChatChannels.cs (full implementation with factory logic)
Network/Packets/ClientPacketFactory.cs (finalize dispatch)
```

**Reference Implementation**: `Aion.LoginServer.Services` for DI and service pattern.

**Verification**: 
- `ChatService.RegisterPlayerAsync()` generates valid 48-byte tokens
- `ChatChannels.GetOrCreateAsync()` identifier parsing matches Java
- `BroadcastService` sends to all members; handles disconnections

---

### Phase 4E: Dynamic Handler Support

**Objective**: Implement handler discovery, registration, and execution pipeline for extensible chat behaviors.

**Deliverables**:

1. **Handler Interface & Registry**:
   - `Handlers/IChatMessageHandler.cs`: 
     - Interface method: `Task HandleAsync(Message message, ChatClient sender, Channel channel)`
     - Returns: `Task` (can throw to veto broadcast)
   - `Handlers/ChatHandlerRegistry.cs`:
     - Static registry: `ConcurrentDictionary<string, IChatMessageHandler>`
     - Scan assemblies at startup for types implementing `IChatMessageHandler`
     - Support attribute-based registration: `[ChatHandler("flood_prevent")]`
     - `RegisterHandler(name, handler): void`
     - `GetHandlers(): IEnumerable<IChatMessageHandler>`
     - `ExecuteHandlersAsync(message, sender, channel): Task` — iterate and execute all handlers

2. **Built-in Handlers**:
   - `Handlers/Built-in/FloodProtectionHandler.cs`:
     - Track message count per player in time window (e.g., max 10 msgs/10s)
     - Throw `HandlerVetoException` if limit exceeded
     - Reset counter on timer
   - `Handlers/Built-in/FilterHandler.cs`:
     - Load banned keywords from config or database
     - Check message text, replace/block if matched
     - Log violations
   - `Handlers/Built-in/LoggingHandler.cs`:
     - Call `ChatLogRepository.InsertChatLogAsync(sender.AccountName, message.Text, channel.ChannelType)`
     - Log all messages to database

3. **Handler Pipeline**:
   - Update `BroadcastService.BroadcastMessageAsync()` to:
     - Call `ChatHandlerRegistry.ExecuteHandlersAsync()` before broadcast
     - If handler vetoes (throws), abort broadcast and log
     - Otherwise, proceed with `SM_CHANNEL_MESSAGE` to all members

4. **Configuration** (optional):
   - Add config keys for handler settings (e.g., `chat.flood.limit`, `chat.filter.keywords`)
   - Load from `ChatServerOptions` or separate `ChatHandlerConfig.cs`

**Files to Create**:
```
Handlers/IChatMessageHandler.cs
Handlers/ChatHandlerRegistry.cs
Handlers/HandlerVetoException.cs
Handlers/Built-in/FloodProtectionHandler.cs
Handlers/Built-in/FilterHandler.cs
Handlers/Built-in/LoggingHandler.cs
```

**Reference Implementation**: Not directly in LoginServer; design inspired by game-server handler model.

**Verification**: Handlers execute in order; flood protection blocks excessive messages; logging inserts to database; veto aborts broadcast.

---

### Phase 4F: Database & Persistence

**Objective**: Validate schema, implement chat log persistence.

**Deliverables**:

1. **Schema Validation**:
   - Verify `chatlog` table exists in `aion_cs` database
   - Schema: `CREATE TABLE chatlog (id INT AUTO_INCREMENT PRIMARY KEY, sender VARCHAR(255), message TEXT, type VARCHAR(255))`
   - Reference: `chat-server/sql/aion_cs.sql`

2. **ChatLogRepository** (`Data/Repositories/ChatLogRepository.cs`):
   - `InsertChatLogAsync(sender: string, message: string, type: string): Task`:
     - Direct SQL: `INSERT INTO chatlog (sender, message, type) VALUES (@sender, @message, @type)`
     - Use `DatabaseFactory.GetConnection()` from `Aion.Commons`
     - Handle exceptions: Log error, don't break broadcast pipeline
     - Connection pool size: reuse from config
   - **Parity**: INSERT behavior must match Java DAO exactly (no transformations)

3. **Register in DI** (update `Program.cs`):
   - `services.AddSingleton<IChatLogRepository, ChatLogRepository>()`
   - Verify `DatabaseFactory` initialized before ChatLogRepository

4. **Integration Test**:
   - Verify connection pool works
   - Insert test record, verify in database
   - Test NULL handling, long text, special characters

**Files to Create/Modify**:
```
Data/Repositories/ChatLogRepository.cs (full implementation)
Program.cs (register repository)
```

**Verification**: Chat logs inserted to database; schema valid; no connection leaks; handle exceptions gracefully.

---

### Phase 4G: Integration & Smoke Tests

**Objective**: Test socket servers, service logic, real client connections, mixed-mode operation.

**Deliverables**:

1. **Integration Test Suite** (`tests/Aion.ChatServer.Tests/Integration/ChatServerIntegrationTests.cs`):
   - **Socket Server Startup**:
     - Verify `ClientSocketServer` binds to 0.0.0.0:10241
     - Verify `GameServerSocketServer` binds to 0.0.0.0:9021
     - Verify graceful startup and shutdown
   - **Packet Serialization**:
     - Create `CM_PLAYER_AUTH` packet, serialize, deserialize, validate fields
     - Repeat for all 17 packet types
   - **Service Logic**:
     - `ChatService.RegisterPlayerAsync()` → verify token is 48 bytes
     - `ChatChannels.GetOrCreateAsync("@region_ALL\u00011.1.AION.KOR")` → verify `RegionChannel` created
     - `ChatChannels.GetOrCreateAsync("@trade_210010000\u00012.1.AION.KOR")` → verify `TradeChannel` created
     - `BroadcastService.BroadcastMessageAsync()` → verify all members receive packet
   - **Database**:
     - Insert test chat log, verify in database
     - Verify connection pooling works

2. **Real Client Socket Smoke Test** (`tests/Aion.ChatServer.Tests/Integration/RealClientSmokeTest.cs`):
   - **Preconditions**: Real Aion client available, C# chat server running on 10241
   - **Flow**:
     1. Client connects to 0.0.0.0:10241
     2. Client sends `CM_PLAYER_AUTH` (playerId=1, accountName="testaccount", nickname="TestNick", token=48-byte)
     3. Server responds with `SM_PLAYER_AUTH_RESPONSE` (success)
     4. Client sends `CM_CHANNEL_JOIN` for region channel
     5. Server responds with `SM_CHANNEL_RESPONSE` (success)
     6. Client sends `CM_CHANNEL_MESSAGE` ("Hello world")
     7. Server broadcasts `SM_CHANNEL_MESSAGE` to all members (including sender)
     8. Client receives message, verifies content
     9. Client sends `CM_CHANNEL_LEAVE`
     10. Server responds with removal from channel
     11. Connection closes cleanly
   - **Validation**: All steps succeed; logs show no errors

3. **Game-Server Bridge Smoke Test** (`tests/Aion.ChatServer.Tests/Integration/GameServerBridgeSmokeTest.cs`):
   - **Preconditions**: C# chat server running on 9021; simulate Java game server
   - **Flow**:
     1. Mock GS connects to 0.0.0.0:9021
     2. Mock GS sends `CM_CS_AUTH` (gameServerId=1, password="test")
     3. Server responds with `SM_GS_AUTH_RESPONSE` (success)
     4. Mock GS sends `CM_PLAYER_AUTH` (register player from Java GS)
     5. Server responds with `SM_PLAYER_AUTH_RESPONSE` (token)
     6. Verify player registered in `ChatService`
     7. Verify player can join channels via bridge
     8. Mock GS sends `CM_PLAYER_LOGOUT`
     9. Server removes player, responds
     10. Connection closes cleanly
   - **Validation**: All GS bridge operations succeed; token matches expected format

4. **Mixed-Mode Validation Runbook** (`tests/Aion.ChatServer.Tests/MIXED_MODE_VALIDATION.md`):
   - **Setup**:
     - Java game server running in Docker on 7777
     - MySQL running in Docker
     - C# login server running locally (from Phase 3)
     - C# chat server running locally
   - **Steps**:
     1. Real Aion client connects to C# login server on 2104
     2. Client authenticates via `CM_LOGIN` (account/password)
     3. C# login server verifies in MySQL
     4. Client receives `SM_SERVER_LIST` → Java game server listed
     5. Client sends `CM_PLAY` to select game server
     6. C# login server connects to Java GS on 9898
     7. C# login server forwards player to Java GS
     8. Java GS receives player, starts game session
     9. Java GS sends `CM_PLAYER_AUTH` to C# chat server on 9021
     10. C# chat server generates token, stores player
     11. Client connects to C# chat server on 10241
     12. Client sends `CM_PLAYER_AUTH` with token from step 10
     13. C# chat server verifies token, sets AUTHED
     14. Client joins region channel
     15. Client sends message → C# chat broadcasts to all members
     16. Other connected clients (if any) receive message
     17. Java GS remains operational throughout
     18. Client logs out via Java GS → C# chat server receives `CM_PLAYER_LOGOUT`
     19. All connections close cleanly
   - **Success Criteria**: All steps succeed; Java GS operational before, during, after; no crashes

**Files to Create**:
```
tests/Aion.ChatServer.Tests/Integration/ChatServerIntegrationTests.cs
tests/Aion.ChatServer.Tests/Integration/RealClientSmokeTest.cs
tests/Aion.ChatServer.Tests/Integration/GameServerBridgeSmokeTest.cs
tests/Aion.ChatServer.Tests/MIXED_MODE_VALIDATION.md
```

**Verification**: All tests pass; mixed-mode runbook executes without errors; real client successfully logs in, chats, logs out.

---

### Phase 4H: Verification & Final Checks

**Objective**: Confirm all deliverables, no regressions, documentation complete.

**Deliverables**:

1. **Packet Parity Validation**:
   - `dotnet test ChatPacketParityTests` → All 17 packet types serialize/deserialize identically to golden vectors
   - Confirm opcodes documented and correct

2. **Service Logic Validation**:
   - `ChatService`: Token generation, player registration, logout → all correct
   - `GameServerService`: Password auth, GS registration → all correct
   - `BroadcastService`: Channel broadcast, member tracking → all correct
   - `ChatChannels`: Identifier parsing, channel creation → all correct

3. **Socket Server Validation**:
   - Client server (10241): Accepts connections, frames packets, dispatches handlers
   - GS bridge (9021): Accepts GS connections, authenticates password, manages player registration
   - Graceful shutdown: All clients disconnected, connections flushed, no resource leaks

4. **Database Validation**:
   - `ChatLogRepository`: Inserts to `chatlog` table, handles exceptions
   - Connection pooling: No leaks, pool size correct

5. **Handler Validation**:
   - Flood protection: Blocks excessive messages
   - Filtering: Blocks/replaces keywords
   - Logging: Inserts to database
   - Handler pipeline: Executes in order, veto aborts broadcast

6. **No Regressions** (Phase 3):
   - `dotnet test AionLoginServer.Tests` → All tests pass
   - Login server functionality: authentication, game-server registration, session keys → all work

7. **Documentation**:
   - Update `docs/csharp-port.md`: Phase 4 marked COMPLETE
   - Document any intentional differences or gaps discovered
   - Cleanup: Delete `TEMP-OPCODES.md` if created

**Verification Checklist**:
- [ ] All 17 packet types have golden tests passing
- [ ] Socket servers accept connections, handle packets, close gracefully
- [ ] All services functional: ChatService, GameServerService, BroadcastService
- [ ] All 6 channel types working: RegionChannel, TradeChannel, RaceChannel, JobChannel, LfgChannel, LangChannel
- [ ] Database: chatlog table exists, ChatLogRepository inserts records
- [ ] Handlers: FloodProtectionHandler, FilterHandler, LoggingHandler all functional
- [ ] Integration tests passing
- [ ] Real client smoke test: Full auth → join → message → leave flow works
- [ ] Mixed-mode validation: C# login + Java game + C# chat with real client succeeds
- [ ] Phase 3 tests: No regressions, all LoginServer tests still passing
- [ ] Documentation: Phase 4 status updated in `docs/csharp-port.md`

---

## Java Reference & Parity Keys

### Packet Opcodes

**CRITICAL**: Extract exact opcode values from Java source during Phase 4B using these grep commands:

```bash
# Client packets (CM_*)
grep -r "class CM_" chat-server/src/com/aionemu/chatserver/network/aion/ | head -20
grep -r "getOpcode\|OPCODE" chat-server/src/com/aionemu/chatserver/network/aion/ | grep -i "0x\|[0-9]"

# Server packets (SM_*)
grep -r "class SM_" chat-server/src/com/aionemu/chatserver/network/aion/ | head -20

# Game-server packets (CM_CS_*, CM_PLAYER_*, etc.)
grep -r "class CM_\|class SM_" chat-server/src/com/aionemu/chatserver/network/gameserver/ | head -20

# Look for opcode constants in AbstractPacket classes
grep -A5 "public.*getOpcode\|private.*opcode" chat-server/src/com/aionemu/chatserver/network/aion/AbstractClientPacket.java
grep -A5 "public.*getOpcode\|private.*opcode" chat-server/src/com/aionemu/chatserver/network/gameserver/GsClientPacket.java
```

**Reference files**:
- `chat-server/src/com/aionemu/chatserver/network/aion/AbstractClientPacket.java` — Base class with opcode field
- `chat-server/src/com/aionemu/chatserver/network/aion/AbstractServerPacket.java` — Base class with opcode field
- `chat-server/src/com/aionemu/chatserver/network/aion/` directory — All client/server packet implementations
- `chat-server/src/com/aionemu/chatserver/network/gameserver/GsClientPacket.java` — GS client packet base
- `chat-server/src/com/aionemu/chatserver/network/gameserver/GsServerPacket.java` — GS server packet base

**Document extracted opcodes in `TEMP-OPCODES.md` during Phase 4B**, then delete after implementation.

### Token Generation Algorithm
*Java implementation*: `SHA256(accountName + 16 random bytes)` = 48 bytes total
- **Exact location**: `chat-server/src/com/aionemu/chatserver/service/ChatService.java`, method `generateToken(String accountName): byte[]`
- **Byte order**: Confirm whether concatenation is `accountName.getBytes() + randomBytes` or `randomBytes + accountName.getBytes()`
- **Output**: Must be exactly 48 bytes (16 random + 32 SHA256 digest)
- **String encoding**: Likely UTF-8 or UTF-16LE; verify in Java source
- **C# equivalent**: Use `System.Security.Cryptography.SHA256` with exact same byte concatenation order

### Channel Identifier Format
*Java pattern*: `@<type>_<meta>\u0001<gameServerId>.<raceId>.AION.KOR`
- Example: `@region_ALL\u00011.1.AION.KOR` for region channel on GS 1
- **Critical detail**: The `\u0001` is a **non-ASCII character** (vertical tab / SOH control character). Ensure C# string parsing handles UTF-16LE correctly.
- **Parsing location**: `chat-server/src/com/aionemu/chatserver/model/channel/ChatChannels.java`, method `getOrCreate(String identifier): Channel`
- **Type matching logic**: Review each channel type's `matches()` method to understand regex/pattern matching (RegionChannel, TradeChannel, RaceChannel, JobChannel, LfgChannel, LangChannel)
- **Fallback**: If no type matches, which channel type is default? Likely `RegionChannel`.
- **Create test case** during Phase 4B with this exact identifier to verify C# parsing matches Java

### Configuration Keys
*From Java config files*:
- `chat-server/config/network/network.properties`: client/GS socket addresses, GS password, NIO threads
- `chat-server/config/network/database.properties`: MySQL connection details

### String Encoding
- **All packets**: UTF-16LE, null-terminated strings (2-byte null terminator `\u0000`)
- **Reference**: 
  - `chat-server/src/com/aionemu/chatserver/network/netty/coder/LoginPacketDecoder.java` — Framing and string parsing
  - `chat-server/src/com/aionemu/chatserver/network/aion/AbstractClientPacket.java` — `readS()` method
  - `chat-server/src/com/aionemu/chatserver/network/aion/AbstractServerPacket.java` — `writeS()` method
- **C# equivalent**: Use `Encoding.Unicode` (UTF-16LE) for all string read/write operations in packet classes
- **Gotcha**: Java `String.getBytes(Charset.forName("UTF-16LE"))` may include BOM; verify in actual Java packet encoding

---

## Testing Strategy

### Golden Packet Tests
- **Location**: `tests/Aion.ChatServer.Tests/Network/ChatPacketParityTests.cs`
- **Approach**: Reference byte vectors (captured or Java-generated) vs. C# serialization
- **Coverage**: All 17 packet types
- **Frequency**: Run before each phase advancement

### Integration Tests
- **Location**: `tests/Aion.ChatServer.Tests/Integration/`
- **Approach**: In-process socket server, mock clients, mock database
- **Coverage**: Service logic, packet dispatch, graceful shutdown
- **Frequency**: Run before phase handoff

### Real Client Smoke Tests
- **Location**: Real Aion client, captured session or manual replay
- **Approach**: Connect to C# chat server, full auth → join → message → leave flow
- **Coverage**: End-to-end user journey
- **Frequency**: Manual, before final validation

### Mixed-Mode Validation
- **Location**: C# login + Java game + C# chat, Docker MySQL
- **Approach**: Real client, automated runbook or manual steps
- **Coverage**: Interoperability, Java GS bridge, token handoff
- **Frequency**: Before handoff to Phase 5

---

## Known Gaps & Critical Discovery Points

1. **Opcode Constants** ⚠️ **CRITICAL**
   - Exact opcode values TBD during Phase 4B
   - Extract from Java source using grep commands in "Java Reference" section
   - Document in temporary `docs/TEMP-OPCODES.md`
   - Delete `TEMP-OPCODES.md` after Phase 4B complete
   - **DO NOT GUESS OPCODES** — use captured Java traffic or source inspection

2. **Token Algorithm Details** ⚠️ **CRITICAL**
   - Exact SHA256 concatenation order (accountName first vs. random first) TBD
   - Verify in Java `ChatService.generateToken()` 
   - Confirm string encoding (UTF-8 vs. UTF-16LE)
   - **Phase 4B golden test must validate token generation** against captured Java output

3. **Channel Identifier Parsing** ⚠️ **CRITICAL**
   - Java's identifier matching uses regex/pattern logic
   - Each channel type has different `matches()` implementation
   - Verify format and pattern matching in all 6 channel types in `ChatChannels.java`
   - **Test with exact identifier format**: `@region_ALL\u00011.1.AION.KOR`

4. **GS Password Matching**
   - The `chatserver.network.gameserver.password` value must match exactly what Java GS sends
   - Verify in `chat-server/config/network/network.properties`
   - If mismatch, GS connection will fail with auth error
   - **Document the password value in TEMP-OPCODES.md for testing**

5. **Frame Codec Length Field**
   - Verify: Is length field 2 bytes LE and includes itself in length count?
   - Expected: `[length:2LE][opcode][data]` where `length` includes the 2-byte length field
   - Reference: `chat-server/src/com/aionemu/chatserver/network/netty/coder/LoginPacketDecoder.java`
   - **Phase 4B golden test must validate framing**

6. **Inter-Server Packet Format**
   - Game-server to chat-server uses same frame format as client-to-chat?
   - Opcode size: 1 byte, 2 bytes, or 4 bytes for GS packets?
   - **Extract from `GsClientPacket.java` and `GsServerPacket.java`**

7. **Handler Reload**
   - Phase 4E does NOT support hot-reloading (handlers compiled at startup)
   - If needed later, can be added in Phase 7 using `AssemblyLoadContext`

8. **No ORM**
   - Direct SQL via MySqlConnector, matching Phase 2 pattern
   - Do NOT use Entity Framework during parity work

9. **No Message Encryption**
   - Chat packets sent unencrypted (unlike login protocol with RSA/Blowfish)
   - Confirm this matches Java behavior by inspecting `ChatServer.java` network initialization
   - If Java does encrypt chat, Phase 4B must discover cipher algorithm

10. **Database Connection Pool**
    - Verify MySQL connection pool size and timeout settings
    - Reference: `database.connectionpool.max` and `database.connectionpool.timeout`
    - C# must use same pool size to avoid connection exhaustion under load

11. **Real Client Testing**
    - Aion client may require specific packet responses in specific order
    - If real client testing fails, capture session with Wireshark and compare to Java
    - Have Visual Studio debugger running for breakpoint inspection if needed

---

## File Structure

```
dotnetConversion/src/Aion.ChatServer/
├── Configuration/
│   └── ChatServerOptions.cs
├── Models/
│   ├── ChatClient.cs
│   ├── Message.cs
│   ├── Channel.cs
│   └── Channels/
│       ├── RegionChannel.cs
│       ├── TradeChannel.cs
│       ├── RaceChannel.cs
│       ├── JobChannel.cs
│       ├── LfgChannel.cs
│       ├── LangChannel.cs
│       └── ChatChannels.cs
├── Network/
│   ├── ClientSocketServer.cs
│   ├── GameServerSocketServer.cs
│   ├── Handlers/
│   │   ├── ClientChannelHandler.cs
│   │   └── GsConnection.cs
│   └── Packets/
│       ├── AbstractClientPacket.cs
│       ├── AbstractServerPacket.cs
│       ├── ClientPacketFactory.cs
│       ├── ServerPacketFactory.cs
│       ├── Client/
│       │   ├── CM_PLAYER_AUTH.cs
│       │   ├── CM_CHAT_INI.cs
│       │   ├── CM_PING.cs
│       │   ├── CM_PLAYER_INFO.cs
│       │   ├── CM_CHANNEL_REQUEST.cs
│       │   ├── CM_CHANNEL_CREATE.cs
│       │   ├── CM_CHANNEL_JOIN.cs
│       │   ├── CM_CHANNEL_MESSAGE.cs
│       │   └── CM_CHANNEL_LEAVE.cs
│       ├── Server/
│       │   ├── SM_PLAYER_AUTH_RESPONSE.cs
│       │   ├── SM_CHAT_INI.cs
│       │   ├── SM_CHANNEL_RESPONSE.cs
│       │   └── SM_CHANNEL_MESSAGE.cs
│       └── GameServer/
│           ├── GsClientPacket.cs
│           ├── GsServerPacket.cs
│           ├── GsPacketFactory.cs
│           ├── CM_CS_AUTH.cs
│           ├── CM_PLAYER_AUTH.cs
│           ├── CM_PLAYER_LOGOUT.cs
│           ├── CM_PLAYER_GAG.cs
│           ├── SM_GS_AUTH_RESPONSE.cs
│           └── SM_PLAYER_AUTH_RESPONSE.cs
├── Services/
│   ├── IChatService.cs
│   ├── ChatService.cs
│   ├── IGameServerService.cs
│   ├── GameServerService.cs
│   ├── IBroadcastService.cs
│   └── BroadcastService.cs
├── Data/
│   └── Repositories/
│       ├── IChatLogRepository.cs
│       └── ChatLogRepository.cs
├── Handlers/
│   ├── IChatMessageHandler.cs
│   ├── ChatHandlerRegistry.cs
│   ├── HandlerVetoException.cs
│   └── Built-in/
│       ├── FloodProtectionHandler.cs
│       ├── FilterHandler.cs
│       └── LoggingHandler.cs
└── Program.cs

dotnetConversion/tests/Aion.ChatServer.Tests/
├── Network/
│   └── ChatPacketParityTests.cs
└── Integration/
    ├── ChatServerIntegrationTests.cs
    ├── RealClientSmokeTest.cs
    ├── GameServerBridgeSmokeTest.cs
    └── MIXED_MODE_VALIDATION.md
```

---

## Success Criteria

| Criterion | Status | Notes |
|-----------|--------|-------|
| All 17 packet types have golden tests | Pending | Phase 4B deliverable |
| Socket servers accept, frame, dispatch packets | Pending | Phase 4C deliverable |
| All services functional | Pending | Phase 4D deliverable |
| All 6 channel types working | Pending | Phase 4D deliverable |
| Database persistence working | Pending | Phase 4F deliverable |
| Handlers execute correctly | Pending | Phase 4E deliverable |
| Integration tests passing | Pending | Phase 4G deliverable |
| Real client smoke test succeeds | Pending | Phase 4G deliverable |
| Mixed-mode validation succeeds | Pending | Phase 4G deliverable |
| Phase 3 tests not regressed | Pending | Phase 4H verification |
| Documentation complete | Pending | Phase 4H verification |

---

## Debugging & Troubleshooting

### Real Client Connection Issues
- **Connection refused (10241)**: Verify `ClientSocketServer` started and bound to 10241. Check firewall.
- **Auth failure**: Verify token was correctly generated by Java GS or chat server. Enable verbose logging.
- **Packet read timeout**: Check packet framing (length field). Use Wireshark to capture and compare to Java.
- **Disconnect after join**: Check `ChatChannels.GetOrCreateAsync()` identifier parsing. Verify channel type matches.

### Game-Server Bridge Issues
- **Connection refused (9021)**: Verify `GameServerSocketServer` started and bound to 9021. Check firewall.
- **GS auth failure**: Verify password in `ChatServerOptions.GameServerPassword` matches Java config. Enable logging.
- **Player not appearing in channel**: Verify `ChatService.RegisterPlayerWithChannelAsync()` completed. Check channel member list.

### Database Issues
- **Connection timeout**: Verify MySQL is running and accessible. Check connection string in `DatabaseFactory`.
- **Chat logs not persisting**: Verify `chatlog` table exists. Check `ChatLogRepository` insert logic. Enable SQL logging.
- **Connection pool exhausted**: Verify connection leak in handlers. Check pool size config matches Java.

### Packet Parity Issues
- **Golden test fails**: Capture actual bytes with Wireshark. Compare to expected vector. Check string encoding and field order.
- **Opcode mismatch**: Verify extracted opcode from Java matches packet handler dispatch. Print opcode in logs.
- **Length field wrong**: Verify frame codec: length should include 2-byte header. Reference `BaseSocketServer` from `Aion.Commons`.

## Next Steps

**Phase 4A begins**: Create config, models, packet base classes, services (stubs), DI setup.

**Estimated duration**: 4-5 days (sub-phases 4A → 4H with pauses between).

**Testing validation**: Golden tests first (4B), then integration (4G), then mixed-mode (4G).

**Handoff**: Upon Phase 4H completion, Phase 5 (Port Game Infrastructure) begins.

---

## Handoff Checklist for Agent

Before starting Phase 4A, verify:
- [ ] Phase 3 (C# LoginServer) is complete and all 180 tests pass
- [ ] Java chat-server source is available and runnable
- [ ] MySQL instance available (Docker or local)
- [ ] Real Aion client available (optional but recommended)
- [ ] Visual Studio or VS Code with C# debugging capability
- [ ] This document has been read in full
- [ ] Any questions about prerequisites answered above

**Ready to proceed with Phase 4A** ✓
