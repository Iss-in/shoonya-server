--CREATE TABLE IF NOT EXISTS employee
--(
-- employeeName varchar(100) NOT NULL,
--  employeeId varchar(11) NOT NULL ,
-- employeeAddress varchar(100) DEFAULT NULL,
-- employeeEmail varchar(100) DEFAULT NULL,
-- PRIMARY KEY (employeeId)
--);

--CREATE TABLE IF NOT EXISTS daily_records (
--  "date" date PRIMARY KEY,
--  "trades" smallint ,
--  "successful_trades" smallint,
--  "account_value" integer,
--  "peak_value" integer ,
--  "drawdown" integer,
--  "pnl" integer ,
--  "max_loss" integer NOT NULL
--);
--
DROP TABLE IF EXISTS nfo_symbols;
DROP TABLE IF EXISTS nse_symbols;

--
----DROP TABLE IF EXISTS  trade;
--CREATE TABLE IF NOT EXISTS trade (
--  "timestamp" timestamp PRIMARY KEY,
--  "trading_symbol" varchar(30),
--  "qty" int,
--  "order_type" varchar(30),
--  "price" double precision
--);

--DROP TABLE IF EXISTS  parsed_trade;
--CREATE TABLE IF NOT EXISTS parsed_trade (
--  "id" SERIAL PRIMARY KEY,
--  "buy_time" timestamp ,
--  "sell_time" timestamp ,
--  "qty" int,
--  "order_type" varchar(30),
--  "points" double precision,
--   CONSTRAINT unique_buy_time UNIQUE (buy_time)
--);
