# CORDAL Configuration Console — UI Screens Mockup

## Overview

CORDAL (Configuration Orchestrated REST Dynamic API Layer) manages three entity types via YAML configuration:

- **Databases** — JDBC connections + HikariCP pool settings
- **Queries** — SQL templates + parameter bindings + optional cache config
- **Endpoints** — HTTP routes → query mapping + pagination + response schema

These form a chain: **Endpoint → Query → Database**. The console provides screens to manage all three, visualise their relationships, and validate the entire chain.

---

## Screen Map

```
┌─────────────────────────────────────────────────────────────────┐
│ CORDAL Configuration Console                                    │
│                                                                 │
│  [Dashboard]  [Databases]  [Queries]  [Endpoints]  [Validate]   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 1. Dashboard — Overview & Health

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ CORDAL Configuration Console                          [Reload Config] [?]  │
├─────────────────────────────────────────────────────────────────────────────┤
│  Dashboard  │ Databases │ Queries │ Endpoints │ Validate                   │
├═════════════╧═══════════════════════════════════════════════════════════════┤
│                                                                            │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐         │
│  │  DATABASES     3 │  │  QUERIES      12 │  │  ENDPOINTS    10 │         │
│  │  ● 3 connected   │  │  ● 10 cached     │  │  ● 8 active      │         │
│  │  ○ 0 failed      │  │  ○ 2 uncached    │  │  ○ 2 unavailable │         │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘         │
│                                                                            │
│  Configuration Chain Health                                                │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  Endpoint                   Query                    Database       │  │
│  │  ─────────────────────────  ────────────────────     ────────────── │  │
│  │  postgres-trades-list    → stock-trades-all       → stocktrades  ● │  │
│  │  postgres-trades-symbol  → stock-trades-by-symbol → stocktrades  ● │  │
│  │  analytics-daily-volume  → daily-trading-volume   → analytics    ● │  │
│  │  analytics-top-perform.  → top-performers         → analytics    ● │  │
│  │  analytics-market-summ.  → market-summary         → datawarehouse● │  │
│  │  ...                                                               │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  Config Sources          │  Hot Reload                                     │
│  ────────────────────    │  ────────────────────                           │
│  Dir: generic-config/    │  Status: Enabled                                │
│  Files: 8 loaded         │  Last reload: 2 min ago                         │
│  Patterns:               │  Debounce: 300ms                                │
│    *-databases.yml (2)   │  File watcher: active                           │
│    *-queries.yml   (3)   │                                                 │
│    *-endpoints.yml (3)   │                                                 │
│                          │                                                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Purpose:** At-a-glance health of the entire system. The chain table is the centrepiece — it shows every Endpoint → Query → Database link and whether each database is reachable.

**Data sources:**
- Summary counts: `GET /api/management/config/metadata`
- Chain health: `GET /api/management/validation/chain`
- Config files: `GET /api/management/config/files`

---

## 2. Databases — List & Edit

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ CORDAL Configuration Console                          [Reload Config] [?]  │
├─────────────────────────────────────────────────────────────────────────────┤
│  Dashboard │  Databases  │ Queries │ Endpoints │ Validate                  │
├════════════╧════════════════════════════════════════════════════════════════┤
│                                                                            │
│  Databases                                              [+ Add Database]   │
│                                                                            │
│  ┌────────────┬──────────────────────────────┬────────┬──────────────────┐ │
│  │ Name       │ JDBC URL                     │ Driver │ Pool / Status    │ │
│  ├────────────┼──────────────────────────────┼────────┼──────────────────┤ │
│  │ stocktrades│ jdbc:h2:tcp://localhost:9092/ │ H2     │ 5/15  ●connected│ │
│  │ analytics  │ jdbc:h2:../data/analytics    │ H2     │ 3/15  ●connected│ │
│  │ datawareh..│ jdbc:h2:../data/datawarehouse│ H2     │ 5/20  ●connected│ │
│  └────────────┴──────────────────────────────┴────────┴──────────────────┘ │
│                                                                            │
│  ▼ stocktrades                                        [Edit] [Test] [Del] │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ Connection                                                          │  │
│  │  Name _______ [stocktrades          ]                               │  │
│  │  Description _ [Stock trades database using H2                    ] │  │
│  │  URL  _______ [jdbc:h2:tcp://localhost:9092/./data/stocktrades    ] │  │
│  │  Driver _____ [org.h2.Driver         ] ▼                            │  │
│  │  Username ___ [sa                    ]                              │  │
│  │  Password ___ [••••                  ]                              │  │
│  │                                                                     │  │
│  │ Connection Pool (HikariCP)                                          │  │
│  │  Max Pool Size ____ [15    ]    Min Idle _________ [3     ]         │  │
│  │  Connect Timeout __ [30000 ]ms  Idle Timeout _____ [600000]ms       │  │
│  │  Max Lifetime _____ [1800000]ms Leak Detection ___ [60000 ]ms       │  │
│  │  Test Query _______ [SELECT 1                  ]                    │  │
│  │                                                                     │  │
│  │ Used by Queries: stock-trades-all, stock-trades-by-symbol,          │  │
│  │                  stock-trades-count, stock-trades-count-by-symbol    │  │
│  │                                                     [Save] [Cancel] │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Purpose:** Manage JDBC connections and pool settings. The "Used by Queries" cross-reference prevents accidental deletion of a database that is still referenced.

**Fields map to `DatabaseConfig`:**

| UI Field         | YAML Key                    | Type    | Notes                        |
|------------------|-----------------------------|---------|------------------------------|
| Name             | `databases.<key>`           | string  | Identifier key               |
| Description      | `description`               | string  | Optional free-text label     |
| URL              | `url`                       | string  | Supports `${env:default}`    |
| Driver           | `driver`                    | string  | Dropdown: H2, PostgreSQL     |
| Username         | `username`                  | string  |                              |
| Password         | `password`                  | string  | Masked                       |
| Max Pool Size    | `pool.maximumPoolSize`      | int     | Default: 10                  |
| Min Idle         | `pool.minimumIdle`          | int     | Default: 2                   |
| Connect Timeout  | `pool.connectionTimeout`    | long ms | Default: 30000               |
| Idle Timeout     | `pool.idleTimeout`          | long ms | Default: 600000              |
| Max Lifetime     | `pool.maxLifetime`          | long ms | Default: 1800000             |
| Leak Detection   | `pool.leakDetectionThreshold`| long ms | Default: 60000              |
| Test Query       | `pool.connectionTestQuery`  | string  | Default: SELECT 1            |

**Actions:**
- **[Test]** — attempts `getConnection()` and runs the test query; shows latency or error
- **[Del]** — blocked if any query references this database; shows dependents
- **[+ Add Database]** — opens blank form

---

## 3. Queries — List & Edit

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ CORDAL Configuration Console                          [Reload Config] [?]  │
├─────────────────────────────────────────────────────────────────────────────┤
│  Dashboard │ Databases │  Queries  │ Endpoints │ Validate                  │
├════════════╧════════════════════════════════════════════════════════════════┤
│                                                                            │
│  Queries                    Filter: [____________] DB: [All        ▼]      │
│                                                          [+ Add Query]     │
│  ┌───────────────────────┬───────────────┬───────┬───────────────────────┐ │
│  │ Query Name            │ Database      │ Cache │ Used By Endpoint      │ │
│  ├───────────────────────┼───────────────┼───────┼───────────────────────┤ │
│  │ stock-trades-all      │ stocktrades   │ LRU   │ postgres-trades-list  │ │
│  │ stock-trades-count    │ stocktrades   │  --   │ postgres-trades-list  │ │
│  │ stock-trades-by-symbol│ stocktrades   │ LRU   │ postgres-trades-symbol│ │
│  │ daily-trading-volume  │ analytics     │ TIME  │ analytics-daily-vol.  │ │
│  │ top-performers        │ analytics     │  --   │ analytics-top-perf.   │ │
│  │ market-summary        │ datawarehouse │  --   │ analytics-market-sum. │ │
│  └───────────────────────┴───────────────┴───────┴───────────────────────┘ │
│                                                                            │
│  ▼ stock-trades-all                                   [Edit] [Test] [Del] │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ Query Definition                                                    │  │
│  │  Name _____ [stock-trades-all                   ]                   │  │
│  │  Description [Retrieve all stock trades with pagination support   ] │  │
│  │  Database _ [stocktrades  ▼]                                        │  │
│  │                                                                     │  │
│  │  SQL ┌──────────────────────────────────────────────────────────┐   │  │
│  │      │ SELECT id, symb as symbol, trade_type, quantity, price, │   │  │
│  │      │        total_val as total_value, trade_date_time,       │   │  │
│  │      │        trader_id, exchange, created_at, updated_at      │   │  │
│  │      │ FROM stock_trades                                       │   │  │
│  │      │ ORDER BY trade_date_time DESC                           │   │  │
│  │      │ LIMIT ? OFFSET ?                                        │   │  │
│  │      └──────────────────────────────────────────────────────────┘   │  │
│  │                                                                     │  │
│  │ Parameters                                          [+ Add Param]   │  │
│  │  ┌─────────┬──────────┬──────────┐                                  │  │
│  │  │ Name    │ Type     │ Required │                                  │  │
│  │  ├─────────┼──────────┼──────────┤                                  │  │
│  │  │ limit   │ INTEGER  │ yes      │                                  │  │
│  │  │ offset  │ INTEGER  │ yes      │                                  │  │
│  │  └─────────┴──────────┴──────────┘                                  │  │
│  │                                                                     │  │
│  │ Cache Settings                                    [x] Enabled       │  │
│  │  Strategy _ [LRU ▼]  TTL _____ [300  ]s  Max Size [1000  ]         │  │
│  │  Key Pattern [stock_trades:{symbol}:{limit}            ]            │  │
│  │  [ ] Async Refresh    [ ] Preload on Startup                        │  │
│  │                                                                     │  │
│  │ Simple Invalidation Events                       [+ Add Event]      │  │
│  │  ┌──────────────────────────────┐                                   │  │
│  │  │ user_trade_insert            │                                   │  │
│  │  │ user_trade_update            │                                   │  │
│  │  │ user_trade_delete            │                                   │  │
│  │  └──────────────────────────────┘                                   │  │
│  │                                                                     │  │
│  │ Advanced Invalidation Rules                      [+ Add Rule]       │  │
│  │  ┌────────────────┬────────────────┬────────────────┬───────┬──────┐│  │
│  │  │ Event          │ Patterns       │ Condition      │ Delay │ Async││  │
│  │  ├────────────────┼────────────────┼────────────────┼───────┼──────┤│  │
│  │  │ trade_executed │ stock_trades:* │ user_id = ...  │   --  │  yes ││  │
│  │  │ market_close   │ user_port..:*  │       --       │  300s │  yes ││  │
│  │  └────────────────┴────────────────┴────────────────┴───────┴──────┘│  │
│  │                                                     [Save] [Cancel] │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Purpose:** Define SQL queries, bind parameters, and configure caching. The "Database" dropdown references entries from the Databases screen. The "Used By Endpoint" column shows reverse dependencies.

**Fields map to `QueryConfig`:**

| UI Field         | YAML Key                          | Type     | Notes                              |
|------------------|-----------------------------------|----------|------------------------------------|
| Name             | `queries.<key>`                   | string   | Identifier key                     |
| Description      | `description`                     | string   | Optional free-text label           |
| Database         | `database`                        | string   | Dropdown of known databases        |
| SQL              | `sql`                             | text     | Multi-line, `?` placeholders       |
| Parameters       | `parameters[]`                    | list     | Each: name, type, required         |
| Cache Enabled    | `cache.enabled`                   | boolean  |                                    |
| Strategy         | `cache.strategy`                  | enum     | LRU, TIME_BASED                    |
| TTL              | `cache.ttl`                       | int sec  | Default: 300                       |
| Max Size         | `cache.maxSize`                   | int      | Default: 1000                      |
| Key Pattern      | `cache.keyPattern`                | string   | e.g. `trades:{symbol}:{date}`      |
| Async Refresh    | `cache.refreshAsync`              | boolean  |                                    |
| Preload          | `cache.preload`                   | boolean  |                                    |
| Invalidate On    | `cache.invalidateOn[]`            | list     | Simple event name strings          |
| Invalidation     | `cache.invalidationRules[]`       | list     | Each: event, patterns[], condition, delaySeconds, async |

**Parameter types:** STRING, INTEGER, LONG, DECIMAL, TIMESTAMP, BOOLEAN

**Actions:**
- **[Test]** — dry-run: prepares the SQL with sample parameter values against the real database, returns first row or error
- **[Del]** — blocked if any endpoint references this query; shows dependents

---

## 4. Endpoints — List & Edit

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ CORDAL Configuration Console                          [Reload Config] [?]  │
├─────────────────────────────────────────────────────────────────────────────┤
│  Dashboard │ Databases │ Queries │  Endpoints  │ Validate                  │
├════════════╧════════════════════════════════════════════════════════════════┤
│                                                                            │
│  Endpoints                  Filter: [____________]     [+ Add Endpoint]    │
│                                                                            │
│  ┌───────────────────────┬──────────────────────┬──────┬────────┬────────┐ │
│  │ Name                  │ Path                 │Method│Response│ Status │ │
│  ├───────────────────────┼──────────────────────┼──────┼────────┼────────┤ │
│  │ postgres-trades-list  │ /api/postgres/trades │ GET  │ PAGED  │ ●active│ │
│  │ postgres-trades-symbol│ /api/postgres/trades/│ GET  │ PAGED  │ ●active│ │
│  │                       │   symbol/{symbol}    │      │        │        │ │
│  │ analytics-daily-vol.  │ /api/analytics/      │ GET  │ LIST   │ ●active│ │
│  │                       │   daily-volume       │      │        │        │ │
│  │ analytics-top-perf.   │ /api/analytics/      │ GET  │ LIST   │ ○down  │ │
│  │                       │   top-performers     │      │        │        │ │
│  └───────────────────────┴──────────────────────┴──────┴────────┴────────┘ │
│                                                                            │
│  ▼ postgres-trades-list                               [Edit] [Test] [Del] │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ Route                                                               │  │
│  │  Path ___ [/api/postgres/trades      ]  Method [GET ▼]              │  │
│  │  Description [Get all stock trades from PostgreSQL with pagination] │  │
│  │  Query __ [stock-trades-all       ▼]                                │  │
│  │                                                                     │  │
│  │ Pagination                                        [x] Enabled       │  │
│  │  Count Query __ [stock-trades-count ▼]                              │  │
│  │  Default Size _ [50   ]   Max Size [500  ]                          │  │
│  │                                                                     │  │
│  │ Parameters                                          [+ Add Param]   │  │
│  │  ┌─────────┬─────────┬────────┬─────────┬─────────┬────────────────┐│  │
│  │  │ Name    │ Type    │ Source │Required │ Default │ Description    ││  │
│  │  ├─────────┼─────────┼────────┼─────────┼─────────┼────────────────┤│  │
│  │  │ page    │ INTEGER │ QUERY  │ no      │ 0       │ Page number    ││  │
│  │  │ size    │ INTEGER │ QUERY  │ no      │ 50      │ Page size      ││  │
│  │  └─────────┴─────────┴────────┴─────────┴─────────┴────────────────┘│  │
│  │                                                                     │  │
│  │ Response                                   Type: [PAGED ▼]         │  │
│  │  Fields                                             [+ Add Field]   │  │
│  │  ┌──────────────────┬───────────┬──────────────────────────┐        │  │
│  │  │ Name             │ Type      │ Description              │        │  │
│  │  ├──────────────────┼───────────┼──────────────────────────┤        │  │
│  │  │ id               │ LONG      │ Trade ID                 │        │  │
│  │  │ symbol           │ STRING    │ Stock symbol             │        │  │
│  │  │ trade_type       │ STRING    │ Trade type (BUY/SELL)    │        │  │
│  │  │ quantity         │ INTEGER   │ Number of shares         │        │  │
│  │  │ price            │ DECIMAL   │ Price per share          │        │  │
│  │  │ total_value      │ DECIMAL   │ Total trade value        │        │  │
│  │  │ trade_date_time  │ TIMESTAMP │ Trade date and time      │        │  │
│  │  │ trader_id        │ STRING    │ Trader identifier        │        │  │
│  │  │ exchange         │ STRING    │ Exchange name            │        │  │
│  │  │ created_at       │ TIMESTAMP │ Record creation          │        │  │
│  │  │ updated_at       │ TIMESTAMP │ Record update            │        │  │
│  │  └──────────────────┴───────────┴──────────────────────────┘        │  │
│  │                                                     [Save] [Cancel] │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Purpose:** Map HTTP routes to queries. Dropdowns for Query and Count Query reference entries from the Queries screen. The response fields section defines the output contract.

**Fields map to `ApiEndpointConfig`:**

| UI Field         | YAML Key                          | Type    | Notes                           |
|------------------|-----------------------------------|---------|---------------------------------|
| Path             | `path`                            | string  | e.g. `/api/trades/{symbol}`     |
| Method           | `method`                          | enum    | GET, POST, PUT, DELETE          |
| Description      | `description`                     | string  | Optional free-text label        |
| Query            | `query`                           | string  | Dropdown of known queries       |
| Count Query      | `countQuery`                      | string  | Dropdown; required if paginated |
| Pagination       | `pagination.enabled`              | boolean |                                 |
| Default Size     | `pagination.defaultSize`          | int     |                                 |
| Max Size         | `pagination.maxSize`              | int     |                                 |
| Parameters       | `parameters[]`                    | list    | name, type, source, required, default, description |
| Response Type    | `response.type`                   | enum    | SINGLE, PAGED, LIST             |
| Response Fields  | `response.fields[]`               | list    | name, type, description         |

**Parameter source:** PATH (from `{placeholder}`), QUERY (from `?key=val`), BODY (from POST/PUT body)

**Actions:**
- **[Test]** — generates a curl command and executes it against the live endpoint; shows response or error
- **[Del]** — removes endpoint; no dependency check needed (endpoints are leaf nodes)

---

## 5. Validate — Chain & Schema Checks

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ CORDAL Configuration Console                          [Reload Config] [?]  │
├─────────────────────────────────────────────────────────────────────────────┤
│  Dashboard │ Databases │ Queries │ Endpoints │  Validate                   │
├════════════╧════════════════════════════════════════════════════════════════┤
│                                                                            │
│  Validation                            [Run Chain Check] [Run Schema Check]│
│                                                                            │
│  Chain Validation                                   Last run: 30s ago  ●OK │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ ✓ postgres-trades-list → stock-trades-all → stocktrades             │  │
│  │ ✓   pagination: countQuery stock-trades-count exists                 │  │
│  │ ✓ postgres-trades-symbol → stock-trades-by-symbol → stocktrades     │  │
│  │ ✓   pagination: countQuery stock-trades-count-by-symbol exists      │  │
│  │ ✓ analytics-daily-volume → daily-trading-volume → analytics         │  │
│  │ ✓ analytics-top-performers → top-performers → analytics             │  │
│  │ ✓ analytics-market-summary → market-summary → datawarehouse         │  │
│  │                                                                      │  │
│  │ Result: 5 chains validated, 0 errors, 0 warnings                     │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  Schema Validation                              Last run: 2 min ago  ●OK   │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ Database: stocktrades                                                │  │
│  │  ✓ Table stock_trades exists                                         │  │
│  │  ✓ Columns: id, symb, trade_type, quantity, price, total_val,       │  │
│  │             trade_date_time, trader_id, exchange, created_at,        │  │
│  │             updated_at — all present                                  │  │
│  │                                                                      │  │
│  │ Database: analytics                                                  │  │
│  │  ✓ Table trades exists                                               │  │
│  │  ✓ Columns: symbol, trade_date, quantity, price — all present        │  │
│  │                                                                      │  │
│  │ Database: datawarehouse                                              │  │
│  │  ✓ Table market_summary exists                                       │  │
│  │  ✓ Columns: id, summary_date, total_volume, total_trades,           │  │
│  │             avg_trade_value, top_symbol, top_symbol_volume,          │  │
│  │             created_at — all present                                  │  │
│  │                                                                      │  │
│  │ Result: 3 databases, 3 tables, 23 columns — all valid                │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  Errors & Warnings                                                         │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ (none)                                                               │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Purpose:** Run the two validation passes that already exist in the backend and display the results. Chain validation checks referential integrity (Endpoint → Query → Database). Schema validation connects to each database and confirms the SQL references real tables and columns.

**Data sources:**
- Chain validation: `GET /api/management/validation/chain`
- Schema validation: `GET /api/management/validation/schemas`
- Summary: `GET /api/management/validation/summary`

---

## 6. Error States

### 6a. Broken Chain

```
  Chain Validation                                  Last run: 5s ago  ●ERROR
  ┌──────────────────────────────────────────────────────────────────────┐
  │ ✓ postgres-trades-list → stock-trades-all → stocktrades             │
  │ ✗ analytics-daily-volume → daily-trading-volume → analytics         │
  │     ERROR: Database 'analytics' not found in configuration           │
  │ ✗ reports-monthly → monthly-report → ???                             │
  │     ERROR: Query 'monthly-report' not found in configuration         │
  │                                                                      │
  │ Result: 5 chains validated, 2 errors, 0 warnings                     │
  └──────────────────────────────────────────────────────────────────────┘
```

### 6b. Database Connection Failure

```
  ┌────────────┬──────────────────────────────┬────────┬──────────────────┐
  │ Name       │ JDBC URL                     │ Driver │ Pool / Status    │
  ├────────────┼──────────────────────────────┼────────┼──────────────────┤
  │ stocktrades│ jdbc:h2:tcp://localhost:9092/ │ H2     │ 5/15  ●connected│
  │ analytics  │ jdbc:h2:../data/analytics    │ H2     │ 0/15  ○failed   │
  │            │                              │        │ Connection       │
  │            │                              │        │ refused: :9092   │
  │ datawareh..│ jdbc:h2:../data/datawarehouse│ H2     │ 5/20  ●connected│
  └────────────┴──────────────────────────────┴────────┴──────────────────┘
```

### 6c. Unavailable Endpoints

```
  ┌───────────────────────┬──────────────────────┬──────┬────────┬────────┐
  │ Name                  │ Path                 │Method│Response│ Status │
  ├───────────────────────┼──────────────────────┼──────┼────────┼────────┤
  │ postgres-trades-list  │ /api/postgres/trades │ GET  │ PAGED  │ ●active│
  │ analytics-daily-vol.  │ /api/analytics/      │ GET  │ LIST   │ ○down  │
  │                       │   daily-volume       │      │        │ DB     │
  │                       │                      │      │        │ n/a    │
  └───────────────────────┴──────────────────────┴──────┴────────┴────────┘
```

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Tab per entity type** | Matches the three YAML file types (`*-databases.yml`, `*-queries.yml`, `*-endpoints.yml`) and the mental model of the config chain |
| **List + expandable detail** | Quick scan then drill in. Avoids modal dialogs that lose context |
| **"Used by" cross-references** | Databases show which queries reference them; queries show which endpoints use them. Makes safe deletion obvious and prevents orphaned configs |
| **Inline [Test] button** | Databases: test connection. Queries: dry-run SQL. Endpoints: curl equivalent. Aligns with CORDAL's "real integrations" philosophy |
| **Validate as its own tab** | Chain + schema validation is a first-class operation, not buried in a menu. Matches existing `/api/management/validation/*` endpoints |
| **Dashboard shows the full chain** | The Endpoint → Query → Database chain is the core abstraction — it should be visible at a glance |
| **Dropdowns reference siblings** | Endpoint Query field = dropdown of known queries; Query Database field = dropdown of known databases. Enforces referential integrity visually before save |
| **[Reload Config] global action** | Triggers hot-reload of all YAML files. Matches existing `fileWatcher` / hot-reload capability |
| **Error states are inline** | Failures surface directly in the list (red status, inline message) rather than in a separate log. Problems are visible where they occur |

---

## Interaction Patterns

### Creating a New Endpoint (typical flow)

```
1. User clicks [Databases] tab
   → Confirms target database exists and is connected
   → If not, clicks [+ Add Database], fills form, [Test], [Save]

2. User clicks [Queries] tab
   → Clicks [+ Add Query]
   → Selects database from dropdown
   → Writes SQL with ? placeholders
   → Adds parameter definitions (name, type, required)
   → Optionally enables cache settings
   → [Save]

3. User clicks [Endpoints] tab
   → Clicks [+ Add Endpoint]
   → Sets path, method
   → Selects query from dropdown
   → If paginated: enables pagination, selects count query
   → Adds endpoint parameters (name, type, source, default)
   → Defines response fields
   → [Save]

4. User clicks [Validate] tab
   → [Run Chain Check] — confirms new chain is valid
   → [Run Schema Check] — confirms SQL tables/columns exist

5. User clicks [Test] on the new endpoint
   → Sees live response from the API
```

### Editing (in-place)

```
1. User clicks row in list → detail panel expands below
2. Clicks [Edit] → fields become editable
3. Makes changes → [Save] writes updated YAML
4. Hot-reload picks up the change automatically
   (or user clicks global [Reload Config])
```

### Deletion (with safety)

```
1. User clicks [Del] on a database
   → If queries reference it: "Cannot delete. Used by: stock-trades-all, ..."
   → If no references: "Delete database 'analytics'? [Confirm] [Cancel]"

2. User clicks [Del] on a query
   → If endpoints reference it: "Cannot delete. Used by: postgres-trades-list, ..."
   → If no references: "Delete query 'old-report'? [Confirm] [Cancel]"

3. User clicks [Del] on an endpoint
   → Always allowed (leaf node): "Delete endpoint 'test-endpoint'? [Confirm] [Cancel]"
```
