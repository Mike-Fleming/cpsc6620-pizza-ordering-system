-- Mike Fleming
-- CreateTables.sql

DROP DATABASE IF EXISTS PizzaDB;
CREATE DATABASE PizzaDB;
USE PizzaDB;

CREATE TABLE baseprice (
    baseprice_Size VARCHAR(30),
    baseprice_CrustType VARCHAR(30),
    baseprice_CustPrice DECIMAL(5,2) NOT NULL,
    baseprice_BusPrice DECIMAL(5,2) NOT NULL,
    PRIMARY KEY (baseprice_Size, baseprice_CrustType)
);

CREATE TABLE topping (
    topping_TopID INT AUTO_INCREMENT,
    topping_TopName VARCHAR(30) NOT NULL,
    topping_SmallAMT DECIMAL(5,2) NOT NULL,
    topping_MedAMT DECIMAL(5,2) NOT NULL,
    topping_LgAMT DECIMAL(5,2) NOT NULL,
    topping_XLAMT DECIMAL(5,2) NOT NULL,
    topping_CustPrice DECIMAL(5,2) NOT NULL,
    topping_BusPrice DECIMAL(5,2) NOT NULL,
    topping_MinINVT INT NOT NULL,
    topping_CurINVT INT NOT NULL,
    PRIMARY KEY (topping_TopID)
);

CREATE TABLE customer (
    customer_CustID INT AUTO_INCREMENT,
    customer_FName VARCHAR(30) NOT NULL,
    customer_LName VARCHAR(30) NOT NULL,
    customer_PhoneNum VARCHAR(30) NOT NULL,
    PRIMARY KEY (customer_CustID)
);

CREATE TABLE discount (
    discount_DiscountID INT AUTO_INCREMENT,
    discount_DiscountName VARCHAR(30) NOT NULL,
    discount_Amount DECIMAL(5,2) NOT NULL,
    discount_IsPercent TINYINT NOT NULL,
    PRIMARY KEY (discount_DiscountID)
);

CREATE TABLE ordertable (
    ordertable_OrderID INT AUTO_INCREMENT,
    customer_CustID INT,
    ordertable_OrderType VARCHAR(30) NOT NULL,
    ordertable_OrderDateTime DATETIME NOT NULL,
    ordertable_CustPrice DECIMAL(5,2) NOT NULL,
    ordertable_BusPrice DECIMAL(5,2) NOT NULL,
    ordertable_isComplete TINYINT(1) DEFAULT 0,
    PRIMARY KEY (ordertable_OrderID),
    FOREIGN KEY (customer_CustID) REFERENCES customer(customer_CustID)
);

CREATE TABLE dinein (
    ordertable_OrderID INT,
    dinein_TableNum INT NOT NULL,
    PRIMARY KEY (ordertable_OrderID),
    FOREIGN KEY (ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID)
);

CREATE TABLE pickup (
    ordertable_OrderID INT,
    pickup_isPickedUp TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (ordertable_OrderID),
    FOREIGN KEY (ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID)
);

CREATE TABLE delivery (
    ordertable_OrderID INT,
    delivery_HouseNum INT NOT NULL,
    delivery_Street VARCHAR(30) NOT NULL,
    delivery_City VARCHAR(30) NOT NULL,
    delivery_State VARCHAR(2) NOT NULL,
    delivery_Zip INT NOT NULL,
    delivery_isDelivered TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (ordertable_OrderID),
    FOREIGN KEY (ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID)
);

CREATE TABLE pizza (
    pizza_PizzaID INT AUTO_INCREMENT,
    pizza_Size VARCHAR(30),
    pizza_CrustType VARCHAR(30),
    pizza_PizzaState VARCHAR(30),
    pizza_PizzaDate DATETIME,
    pizza_CustPrice DECIMAL(5,2),
    pizza_BusPrice DECIMAL(5,2),
    ordertable_OrderID INT,
    PRIMARY KEY (pizza_PizzaID),
    FOREIGN KEY (ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID),
    FOREIGN KEY (pizza_Size, pizza_CrustType) REFERENCES baseprice(baseprice_Size, baseprice_CrustType)
);

CREATE TABLE pizza_topping (
    pizza_PizzaID INT,
    topping_TopID INT,
    pizza_topping_isDouble INT NOT NULL,
    PRIMARY KEY (pizza_PizzaID, topping_TopID),
    FOREIGN KEY (pizza_PizzaID) REFERENCES pizza(pizza_PizzaID),
    FOREIGN KEY (topping_TopID) REFERENCES topping(topping_TopID)
);

CREATE TABLE pizza_discount (
    pizza_PizzaID INT,
    discount_DiscountID INT,
    PRIMARY KEY (pizza_PizzaID, discount_DiscountID),
    FOREIGN KEY (pizza_PizzaID) REFERENCES pizza(pizza_PizzaID),
    FOREIGN KEY (discount_DiscountID) REFERENCES discount(discount_DiscountID)
);

CREATE TABLE order_discount (
    ordertable_OrderID INT,
    discount_DiscountID INT,
    PRIMARY KEY (ordertable_OrderID, discount_DiscountID),
    FOREIGN KEY (ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID),
    FOREIGN KEY (discount_DiscountID) REFERENCES discount(discount_DiscountID)
);