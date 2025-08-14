-- PostgreSQL Dual Database Cleanup Script
-- This script removes the test databases and cleans up resources
--
-- Usage:
-- psql -h localhost -U postgres -f cleanup-dual-postgresql-databases.sql

-- Connect to postgres database as admin
\c postgres;

\echo 'Starting cleanup of dual PostgreSQL databases...'
\echo ''

-- Terminate any active connections to the databases
\echo 'Terminating active connections to test databases...'

-- Terminate connections to trades_db_1
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE datname = 'trades_db_1' AND pid <> pg_backend_pid();

-- Terminate connections to trades_db_2
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE datname = 'trades_db_2' AND pid <> pg_backend_pid();

\echo 'Active connections terminated.'
\echo ''

-- Drop the databases
\echo 'Dropping test databases...'

DROP DATABASE IF EXISTS trades_db_1;
\echo 'Dropped database: trades_db_1'

DROP DATABASE IF EXISTS trades_db_2;
\echo 'Dropped database: trades_db_2'

\echo ''
\echo 'Database cleanup completed successfully!'
\echo ''

-- Optional: Remove the test user (uncomment if you want to remove the user)
-- \echo 'Removing test user...'
-- DROP ROLE IF EXISTS testuser;
-- \echo 'Removed user: testuser'
-- \echo ''

\echo 'Cleanup summary:'
\echo '- Terminated active connections to test databases'
\echo '- Dropped databases: trades_db_1, trades_db_2'
\echo '- Test user "testuser" retained (uncomment lines above to remove)'
\echo ''
\echo 'Cleanup completed successfully!'
