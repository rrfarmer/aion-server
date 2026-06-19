#!/bin/bash
# Runs ONCE on first MySQL container boot (placed in /docker-entrypoint-initdb.d).
# Creates the three Aion databases and loads each schema + seed into the correct DB.
# The schema .sql files are pure table DDL (no CREATE DATABASE / USE), so we create
# the databases here and pipe each file into its target DB.
#
# The repo SQL trees are mounted read-only into the container by docker-compose:
#   /sql/login  -> login-server/sql
#   /sql/game   -> game-server/sql
#   /sql/chat   -> chat-server/sql
set -euo pipefail

mysql_exec() { mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" "$@"; }

echo "[aion-init] creating databases aion_ls / aion_gs / aion_cs ..."
mysql_exec <<'SQL'
CREATE DATABASE IF NOT EXISTS aion_ls CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS aion_gs CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS aion_cs CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
SQL

load() { # load <db> <file>  (skips missing files gracefully)
  local db="$1" file="$2"
  if [ -f "$file" ]; then
    echo "[aion-init] loading $file -> $db"
    mysql_exec "$db" < "$file"
  else
    echo "[aion-init] (skip, not found: $file)"
  fi
}

# Login server
load aion_ls /sql/login/aion_ls.sql
load aion_ls /sql/login/update.sql
load aion_ls /sql/login/seed_gameservers.sql

# Game server
load aion_gs /sql/game/aion_gs.sql
load aion_gs /sql/game/update.sql

# Chat server
load aion_cs /sql/chat/aion_cs.sql

echo "[aion-init] done. Databases ready."
