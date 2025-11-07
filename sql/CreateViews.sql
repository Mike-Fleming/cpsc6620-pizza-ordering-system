-- Mike Fleming
-- CreateViews.sql

USE PizzaDB;

-- Ranks all toppings from most popular to least popular, accounting for extra toppings
CREATE VIEW ToppingPopularity AS 
SELECT 
    t.topping_TopName AS Topping,
    COUNT(pt.topping_TopID) + SUM(IFNULL(pt.pizza_topping_isDouble, 0)) AS ToppingCount
FROM 
    topping t
LEFT JOIN 
    pizza_topping pt ON t.topping_TopID = pt.topping_TopID
GROUP BY 
    t.topping_TopID, t.topping_TopName
ORDER BY 
    ToppingCount DESC, Topping ASC;

-- Summary of profit by pizza size and crust type over time
CREATE VIEW ProfitByPizza AS
SELECT
    p.pizza_Size AS Size,
    p.pizza_CrustType AS Crust,
    SUM(p.pizza_CustPrice - p.pizza_BusPrice) AS Profit,
    DATE_FORMAT(o.ordertable_OrderDateTime, '%c/%Y') AS OrderMonth
FROM
    pizza p
        JOIN
    ordertable o ON p.ordertable_OrderID = o.ordertable_OrderID
GROUP BY
    p.pizza_Size, p.pizza_CrustType, DATE_FORMAT(o.ordertable_OrderDateTime, '%c/%Y')
ORDER BY
    Profit;

-- Summary of profit for each order type by month with grand totaleftl
CREATE VIEW ProfitByOrderType AS
WITH OrderProfits AS (
    SELECT
        o.ordertable_OrderType AS CustomerType,
        DATE_FORMAT(o.ordertable_OrderDateTime, '%c/%Y') AS OrderMonth,
        SUM(o.ordertable_CustPrice) AS TotalOrderPrice,
        SUM(o.ordertable_BusPrice) AS TotalOrderCost,
        SUM(o.ordertable_CustPrice - o.ordertable_BusPrice) AS Profit
    FROM
        ordertable o
    GROUP BY
        o.ordertable_OrderType, DATE_FORMAT(o.ordertable_OrderDateTime, '%c/%Y')
)
SELECT
    CustomerType,
    OrderMonth,
    TotalOrderPrice,
    TotalOrderCost,
    Profit
FROM
    OrderProfits
UNION ALL
SELECT
    '' AS CustomerType,
    'Grand Total' AS OrderMonth,
    SUM(TotalOrderPrice) AS TotalOrderPrice,
    SUM(TotalOrderCost) AS TotalOrderCost,
    SUM(Profit) AS Profit
FROM
    OrderProfits
;

select * from ProfitByOrderType PBOT;
