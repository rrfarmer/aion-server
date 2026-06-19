# GameServer image. Build context = repo root.  Build:  docker build -f docker/gameserver/Dockerfile .
# ---- build stage ----
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src
COPY dotnetConversion/ ./dotnetConversion/
RUN dotnet publish dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj \
      -c Release -o /publish -p:UseAppHost=false

# ---- runtime stage ----
FROM mcr.microsoft.com/dotnet/runtime:10.0 AS runtime
WORKDIR /app
# The GameServer bootstrap walks up from the binary dir (AppContext.BaseDirectory=/app) to find
# <dir>/game-server/config and re-roots CWD there, then loads game-server/{config,data,cache}.
COPY game-server/config/ ./game-server/config/
COPY game-server/data/   ./game-server/data/
COPY game-server/cache/  ./game-server/cache/
# Published server binaries at /app (so BaseDirectory=/app and the walk-up finds /app/game-server/config).
COPY --from=build /publish/ ./
COPY docker/gameserver/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
# Client port (host-mapped by compose); GS<->LS bridge is internal.
EXPOSE 7777
ENTRYPOINT ["/entrypoint.sh"]
