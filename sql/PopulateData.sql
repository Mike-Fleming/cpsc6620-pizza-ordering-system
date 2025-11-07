-- Mike Fleming
-- PopulateData.sql
-- Populates the database with starter data for Pizzas-R-Us

USE PizzaDB;

-- Insert base prices
INSERT INTO baseprice VALUES
                          ('Small', 'Thin', 3.00, 0.50),
                          ('Small', 'Original', 3.00, 0.75),
                          ('Small', 'Pan', 3.50, 1.00),
                          ('Small', 'Gluten-Free', 4.00, 2.00),
                          ('Medium', 'Thin', 5.00, 1.00),
                          ('Medium', 'Original', 5.00, 1.50),
                          ('Medium', 'Pan', 6.00, 2.25),
                          ('Medium', 'Gluten-Free', 6.25, 3.00),
                          ('Large', 'Thin', 8.00, 1.25),
                          ('Large', 'Original', 8.00, 2.00),
                          ('Large', 'Pan', 9.00, 3.00),
                          ('Large', 'Gluten-Free', 9.50, 4.00),
                          ('XLarge', 'Thin', 10.00, 2.00),
                          ('XLarge', 'Original', 10.00, 3.00),
                          ('XLarge', 'Pan', 11.50, 4.50),
                          ('XLarge', 'Gluten-Free', 12.50, 6.00);

-- Insert toppings
INSERT INTO topping (topping_TopName, topping_CustPrice, topping_BusPrice, topping_CurINVT, topping_MinINVT,
                     topping_SmallAMT, topping_MedAMT, topping_LgAMT, topping_XLAMT)
VALUES ('Pepperoni', 1.25, 0.20, 136, 50, 2.00, 2.75, 3.50, 4.50),
       ('Sausage', 1.25, 0.15, 108, 50, 2.50, 3.00, 3.50, 4.25),
       ('Ham', 1.50, 0.15, 86, 25, 2.00, 2.50, 3.25, 4.00),
       ('Chicken', 1.75, 0.25, 63, 25, 1.50, 2.00, 2.25, 3.00),
       ('Green Pepper', 0.50, 0.02, 84, 25, 1.00, 1.50, 2.00, 2.50),
       ('Onion', 0.50, 0.02, 90, 25, 1.00, 1.50, 2.00, 2.75),
       ('Roma Tomato', 0.75, 0.03, 94, 10, 2.00, 3.00, 3.50, 4.50),
       ('Mushrooms', 0.75, 0.10, 60, 50, 1.50, 2.00, 2.50, 3.00),
       ('Black Olives', 0.60, 0.10, 42, 25, 0.75, 1.00, 1.50, 2.00),
       ('Pineapple', 1.00, 0.25, 19, 0, 1.00, 1.25, 1.75, 2.00),
       ('Jalapenos', 0.50, 0.05, 64, 0, 0.50, 0.75, 1.25, 1.75),
       ('Banana Peppers', 0.50, 0.05, 38, 0, 0.60, 1.00, 1.30, 1.75),
       ('Regular Cheese', 0.50, 0.12, 297, 50, 2.00, 3.50, 5.00, 7.00),
       ('Four Cheese Blend', 1.00, 0.15, 191, 25, 2.00, 3.50, 5.00, 7.00),
       ('Feta Cheese', 1.50, 0.18, 78, 0, 1.75, 3.00, 4.00, 5.50),
       ('Goat Cheese', 1.50, 0.20, 60, 0, 1.60, 2.75, 4.00, 5.50),
       ('Bacon', 1.50, 0.25, 92, 0, 1.00, 1.50, 2.00, 3.00);

-- Insert discounts
INSERT INTO discount (discount_DiscountName, discount_Amount, discount_IsPercent) VALUES
                                                                                      ('Employee', 15.00, 1),
                                                                                      ('Lunch Special Medium', 1.00, 0),
                                                                                      ('Lunch Special Large', 2.00, 0),
                                                                                      ('Specialty Pizza', 1.50, 0),
                                                                                      ('Happy Hour', 10.00, 1),
                                                                                      ('Gameday Special', 20.00, 1);

-- Insert customers
INSERT INTO customer (customer_FName, customer_LName, customer_PhoneNum) VALUES
                                                                             ('Andrew', 'Wilkes-Krier', '8642545861'),
                                                                             ('Matt', 'Engers', '8644749953'),
                                                                             ('Frank', 'Turner', '8642328944'),
                                                                             ('Milo', 'Auckerman', '8648785679');

-- Order #1: Dine-in order on March 5th
INSERT INTO ordertable (ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_isComplete)
VALUES ('dinein', '2025-03-05 12:03:00', 19.75, 3.68, 1);

SET @order1_id = LAST_INSERT_ID();

INSERT INTO dinein (ordertable_OrderID, dinein_TableNum) VALUES (@order1_id, 21);

INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('Large', 'Thin', 'completed', '2025-03-05 12:03:00', 19.75, 3.68, @order1_id);

SET @pizza1_id = LAST_INSERT_ID();

-- Get topping IDs
SET @cheese_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Regular Cheese');
SET @pepperoni_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Pepperoni');
SET @sausage_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Sausage');

-- Add toppings to pizza
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza1_id, @cheese_id, 1),
                                                                                     (@pizza1_id, @pepperoni_id, 0),
                                                                                     (@pizza1_id, @sausage_id, 0);

-- Add discount to pizza
SET @lunch_large_id = (SELECT discount_DiscountID FROM discount WHERE discount_DiscountName = 'Lunch Special Large');
INSERT INTO pizza_discount (pizza_PizzaID, discount_DiscountID) VALUES (@pizza1_id, @lunch_large_id);

-- Order #2: Dine-in order on April 3rd
INSERT INTO ordertable (ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_isComplete)
VALUES ('dinein', '2025-04-03 12:05:00', 19.78, 4.63, 1);

SET @order2_id = LAST_INSERT_ID();

INSERT INTO dinein (ordertable_OrderID, dinein_TableNum) VALUES (@order2_id, 4);

-- First pizza in order #2
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('Medium', 'Pan', 'completed', '2025-04-03 12:05:00', 12.85, 3.23, @order2_id);

SET @pizza2_id = LAST_INSERT_ID();

-- Get topping IDs
SET @feta_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Feta Cheese');
SET @olives_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Black Olives');
SET @tomato_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Roma Tomato');
SET @mushroom_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Mushrooms');
SET @banana_peppers_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Banana Peppers');

-- Add toppings to first pizza in order #2
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza2_id, @feta_id, 0),
                                                                                     (@pizza2_id, @olives_id, 0),
                                                                                     (@pizza2_id, @tomato_id, 0),
                                                                                     (@pizza2_id, @mushroom_id, 0),
                                                                                     (@pizza2_id, @banana_peppers_id, 0);

-- Add discounts to first pizza in order #2
SET @lunch_medium_id = (SELECT discount_DiscountID FROM discount WHERE discount_DiscountName = 'Lunch Special Medium');
SET @specialty_id = (SELECT discount_DiscountID FROM discount WHERE discount_DiscountName = 'Specialty Pizza');
INSERT INTO pizza_discount (pizza_PizzaID, discount_DiscountID) VALUES
                                                                    (@pizza2_id, @lunch_medium_id),
                                                                    (@pizza2_id, @specialty_id);

-- Second pizza in order #2
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('Small', 'Original', 'completed', '2025-04-03 12:05:00', 6.93, 1.40, @order2_id);

SET @pizza3_id = LAST_INSERT_ID();

-- Get topping IDs
SET @chicken_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Chicken');

-- Add toppings to second pizza in order #2
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza3_id, @cheese_id, 0),
                                                                                     (@pizza3_id, @chicken_id, 0),
                                                                                     (@pizza3_id, @banana_peppers_id, 0);

-- Order #3: Pickup order on March 3rd
SET @andrew_id = (SELECT customer_CustID FROM customer WHERE customer_FName = 'Andrew' AND customer_LName = 'Wilkes-Krier');
INSERT INTO ordertable (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_isComplete)
VALUES (@andrew_id, 'pickup', '2025-03-03 21:30:00', 89.28, 19.80, 1);

SET @order3_id = LAST_INSERT_ID();

INSERT INTO pickup (ordertable_OrderID, pickup_isPickedUp) VALUES (@order3_id, 1);

-- Add 6 identical pizzas to order #3
SET @pizza_price = 14.88;
SET @pizza_cost = 3.30;

-- Create 6 identical pizzas (replacing loop with explicit statements)
-- Pizza 1
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('Large', 'Original', 'completed', '2025-03-03 21:30:00', @pizza_price, @pizza_cost, @order3_id);
SET @pizza_id = LAST_INSERT_ID();
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza_id, @cheese_id, 0),
                                                                                     (@pizza_id, @pepperoni_id, 0);

-- Pizza 2
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('Large', 'Original', 'completed', '2025-03-03 21:30:00', @pizza_price, @pizza_cost, @order3_id);
SET @pizza_id = LAST_INSERT_ID();
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza_id, @cheese_id, 0),
                                                                                     (@pizza_id, @pepperoni_id, 0);

-- Pizza 3
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('Large', 'Original', 'completed', '2025-03-03 21:30:00', @pizza_price, @pizza_cost, @order3_id);
SET @pizza_id = LAST_INSERT_ID();
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza_id, @cheese_id, 0),
                                                                                     (@pizza_id, @pepperoni_id, 0);

-- Pizza 4
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('Large', 'Original', 'completed', '2025-03-03 21:30:00', @pizza_price, @pizza_cost, @order3_id);
SET @pizza_id = LAST_INSERT_ID();
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza_id, @cheese_id, 0),
                                                                                     (@pizza_id, @pepperoni_id, 0);

-- Pizza 5
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('Large', 'Original', 'completed', '2025-03-03 21:30:00', @pizza_price, @pizza_cost, @order3_id);
SET @pizza_id = LAST_INSERT_ID();
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza_id, @cheese_id, 0),
                                                                                     (@pizza_id, @pepperoni_id, 0);

-- Pizza 6
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('Large', 'Original', 'completed', '2025-03-03 21:30:00', @pizza_price, @pizza_cost, @order3_id);
SET @pizza_id = LAST_INSERT_ID();
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza_id, @cheese_id, 0),
                                                                                     (@pizza_id, @pepperoni_id, 0);

-- Order #4: Delivery order on April 20th
INSERT INTO ordertable (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_isComplete)
VALUES (@andrew_id, 'delivery', '2025-04-20 19:11:00', 69.15, 16.79, 1);

SET @order4_id = LAST_INSERT_ID();

INSERT INTO delivery (ordertable_OrderID, delivery_HouseNum, delivery_Street, delivery_City, delivery_State, delivery_Zip, delivery_isDelivered)
VALUES (@order4_id, 115, 'Party Blvd', 'Anderson', 'SC', 29621, 1);

-- First pizza in order #4 (pepperoni and sausage)
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('XLarge', 'Original', 'completed', '2025-04-20 19:11:00', 27.94, 9.19, @order4_id);

SET @pizza4_1_id = LAST_INSERT_ID();

-- Get topping ID
SET @four_cheese_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Four Cheese Blend');

-- Add toppings to pizza
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza4_1_id, @four_cheese_id, 0),
                                                                                     (@pizza4_1_id, @pepperoni_id, 0),
                                                                                     (@pizza4_1_id, @sausage_id, 0);

-- Second pizza in order #4 (ham and pineapple)
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('XLarge', 'Original', 'completed', '2025-04-20 19:11:00', 31.50, 6.25, @order4_id);

SET @pizza4_2_id = LAST_INSERT_ID();

-- Get topping IDs
SET @ham_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Ham');
SET @pineapple_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Pineapple');

-- Add toppings to pizza
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza4_2_id, @four_cheese_id, 0),
                                                                                     (@pizza4_2_id, @ham_id, 1),
                                                                                     (@pizza4_2_id, @pineapple_id, 1);

-- Add discount to pizza
INSERT INTO pizza_discount (pizza_PizzaID, discount_DiscountID) VALUES (@pizza4_2_id, @specialty_id);

-- Third pizza in order #4 (chicken and bacon)
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('XLarge', 'Original', 'completed', '2025-04-20 19:11:00', 26.75, 5.55, @order4_id);

SET @pizza4_3_id = LAST_INSERT_ID();

-- Get topping ID
SET @bacon_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Bacon');

-- Add toppings to pizza
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza4_3_id, @four_cheese_id, 0),
                                                                                     (@pizza4_3_id, @chicken_id, 0),
                                                                                     (@pizza4_3_id, @bacon_id, 0);

-- Add order discount
SET @gameday_id = (SELECT discount_DiscountID FROM discount WHERE discount_DiscountName = 'Gameday Special');
INSERT INTO order_discount (ordertable_OrderID, discount_DiscountID) VALUES (@order4_id, @gameday_id);

-- Order #5: Pickup order on March 2nd
SET @matt_id = (SELECT customer_CustID FROM customer WHERE customer_FName = 'Matt' AND customer_LName = 'Engers');
INSERT INTO ordertable (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_isComplete)
VALUES (@matt_id, 'pickup', '2025-03-02 17:30:00', 28.70, 7.84, 1);

SET @order5_id = LAST_INSERT_ID();

INSERT INTO pickup (ordertable_OrderID, pickup_isPickedUp) VALUES (@order5_id, 1);

-- Pizza for order #5
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('XLarge', 'Gluten-Free', 'completed', '2025-03-02 17:30:00', 28.70, 7.84, @order5_id);

SET @pizza5_id = LAST_INSERT_ID();

-- Get topping ID
SET @goat_cheese_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Goat Cheese');
SET @green_pepper_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Green Pepper');
SET @onion_id = (SELECT topping_TopID FROM topping WHERE topping_TopName = 'Onion');

-- Add toppings to pizza
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza5_id, @goat_cheese_id, 0),
                                                                                     (@pizza5_id, @green_pepper_id, 0),
                                                                                     (@pizza5_id, @onion_id, 0),
                                                                                     (@pizza5_id, @tomato_id, 0),
                                                                                     (@pizza5_id, @mushroom_id, 0),
                                                                                     (@pizza5_id, @olives_id, 0);

-- Add discount
INSERT INTO pizza_discount (pizza_PizzaID, discount_DiscountID) VALUES (@pizza5_id, @specialty_id);

-- Order #6: Delivery order on March 2nd
SET @frank_id = (SELECT customer_CustID FROM customer WHERE customer_FName = 'Frank' AND customer_LName = 'Turner');
INSERT INTO ordertable (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_isComplete)
VALUES (@frank_id, 'delivery', '2025-03-02 18:17:00', 25.81, 3.64, 1);

SET @order6_id = LAST_INSERT_ID();

INSERT INTO delivery (ordertable_OrderID, delivery_HouseNum, delivery_Street, delivery_City, delivery_State, delivery_Zip, delivery_isDelivered)
VALUES (@order6_id, 6745, 'Wessex St', 'Anderson', 'SC', 29621, 1);

-- Pizza for order #6
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('Large', 'Thin', 'completed', '2025-03-02 18:17:00', 25.81, 3.64, @order6_id);

SET @pizza6_id = LAST_INSERT_ID();

-- Add toppings to pizza
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza6_id, @four_cheese_id, 1),
                                                                                     (@pizza6_id, @chicken_id, 0),
                                                                                     (@pizza6_id, @green_pepper_id, 0),
                                                                                     (@pizza6_id, @onion_id, 0),
                                                                                     (@pizza6_id, @mushroom_id, 0);

-- Order #7: Delivery order on April 13th
SET @milo_id = (SELECT customer_CustID FROM customer WHERE customer_FName = 'Milo' AND customer_LName = 'Auckerman');
INSERT INTO ordertable (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_isComplete)
VALUES (@milo_id, 'delivery', '2025-04-13 20:32:00', 31.66, 6.00, 1);

SET @order7_id = LAST_INSERT_ID();

INSERT INTO delivery (ordertable_OrderID, delivery_HouseNum, delivery_Street, delivery_City, delivery_State, delivery_Zip, delivery_isDelivered)
VALUES (@order7_id, 8879, 'Suburban', 'Anderson', 'SC', 29621, 1);

-- First pizza in order #7 (four cheese)
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('Large', 'Thin', 'completed', '2025-04-13 20:32:00', 18.00, 2.75, @order7_id);

SET @pizza7_1_id = LAST_INSERT_ID();

-- Add topping to pizza
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
    (@pizza7_1_id, @four_cheese_id, 1);

-- Second pizza in order #7 (cheese and pepperoni)
INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES ('Large', 'Thin', 'completed', '2025-04-13 20:32:00', 19.25, 3.25, @order7_id);

SET @pizza7_2_id = LAST_INSERT_ID();

-- Add toppings to pizza
INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES
                                                                                     (@pizza7_2_id, @cheese_id, 0),
                                                                                     (@pizza7_2_id, @pepperoni_id, 1);

-- Add order discount
SET @employee_id = (SELECT discount_DiscountID FROM discount WHERE discount_DiscountName = 'Employee');
INSERT INTO order_discount (ordertable_OrderID, discount_DiscountID) VALUES (@order7_id, @employee_id);