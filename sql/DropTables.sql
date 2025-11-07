-- Mike Fleming
-- DropTables.sql

USE PizzaDB;

-- Drop views first
DROP VIEW IF EXISTS ToppingPopularity;
DROP VIEW IF EXISTS ProfitByPizza;
DROP VIEW IF EXISTS ProfitByOrderType;

-- Drop junction tables first to avoid foreign key constraints
DROP TABLE IF EXISTS pizza_topping;
DROP TABLE IF EXISTS pizza_discount;
DROP TABLE IF EXISTS order_discount;

-- Drop specialized order tables
DROP TABLE IF EXISTS pickup;
DROP TABLE IF EXISTS delivery;
DROP TABLE IF EXISTS dinein;

-- Drop pizza table
DROP TABLE IF EXISTS pizza;

-- Drop order table
DROP TABLE IF EXISTS ordertable;

-- Drop remaining tables
DROP TABLE IF EXISTS baseprice;
DROP TABLE IF EXISTS topping;
DROP TABLE IF EXISTS discount;
DROP TABLE IF EXISTS customer;
