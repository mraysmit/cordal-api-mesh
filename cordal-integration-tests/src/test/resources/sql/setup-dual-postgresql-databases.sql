-- PostgreSQL Dual Database Setup Script
-- This script sets up two PostgreSQL databases for integration testing
-- 
-- Prerequisites:
-- 1. PostgreSQL server running on localhost:5432
-- 2. Admin user 'postgres' with password 'postgres'
-- 3. Application user 'testuser' with password 'testpass'
--
-- Usage:
-- psql -h localhost -U postgres -f setup-dual-postgresql-databases.sql

-- Connect to postgres database as admin
\c postgres;

-- Create application user if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'testuser') THEN
        CREATE ROLE testuser LOGIN PASSWORD 'testpass';
        RAISE NOTICE 'Created user: testuser';
    ELSE
        RAISE NOTICE 'User testuser already exists';
    END IF;
END
$$;

-- Grant necessary privileges to testuser
ALTER ROLE testuser CREATEDB;
GRANT CONNECT ON DATABASE postgres TO testuser;

-- Drop databases if they exist (cleanup from previous runs)
DROP DATABASE IF EXISTS trades_db_1;
DROP DATABASE IF EXISTS trades_db_2;

-- Create the first database
CREATE DATABASE trades_db_1 
    OWNER testuser 
    ENCODING 'UTF8' 
    LC_COLLATE = 'en_US.UTF-8' 
    LC_CTYPE = 'en_US.UTF-8';

-- Create the second database  
CREATE DATABASE trades_db_2 
    OWNER testuser 
    ENCODING 'UTF8' 
    LC_COLLATE = 'en_US.UTF-8' 
    LC_CTYPE = 'en_US.UTF-8';

-- Grant all privileges on both databases to testuser
GRANT ALL PRIVILEGES ON DATABASE trades_db_1 TO testuser;
GRANT ALL PRIVILEGES ON DATABASE trades_db_2 TO testuser;

\echo 'Database creation completed successfully'
\echo 'Created databases: trades_db_1, trades_db_2'
\echo 'Owner: testuser'
\echo ''
\echo 'Next steps:'
\echo '1. Run setup-schema-trades-db-1.sql to set up schema for trades_db_1'
\echo '2. Run setup-schema-trades-db-2.sql to set up schema for trades_db_2'
