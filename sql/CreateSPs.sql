-- Mike Fleming
-- CreateSPs.sql
-- Creates stored procedures, functions, and triggers for Pizzas-R-Us

USE PizzaDB;

-- Stored Procedure: Calculate pizza price based on size, crust, and toppings
DELIMITER //
CREATE PROCEDURE CalculatePizzaPrice(
    IN p_size VARCHAR(30),
    IN p_crust VARCHAR(30),
    IN p_pizza_id INT,
    OUT p_cust_price DECIMAL(5,2),
    OUT p_bus_price DECIMAL(5,2)
)
BEGIN
    DECLARE base_cust_price DECIMAL(5,2);
    DECLARE base_bus_price DECIMAL(5,2);
    DECLARE topping_cust_price DECIMAL(5,2) DEFAULT 0;
    DECLARE topping_bus_price DECIMAL(5,2) DEFAULT 0;
    
    SELECT baseprice_CustPrice, baseprice_BusPrice
    INTO base_cust_price, base_bus_price
    FROM baseprice
    WHERE baseprice_Size = p_size AND baseprice_CrustType = p_crust;
    
    SELECT
        SUM(CASE 
            WHEN pizza_topping_isDouble = 1 THEN t.topping_CustPrice * 2
            ELSE t.topping_CustPrice
        END),
        SUM(CASE
            WHEN pizza_topping_isDouble = 1 THEN 
                CASE
                    WHEN p_size = 'Small' THEN t.topping_BusPrice * t.topping_SmallAMT * 2
                    WHEN p_size = 'Medium' THEN t.topping_BusPrice * t.topping_MedAMT * 2
                    WHEN p_size = 'Large' THEN t.topping_BusPrice * t.topping_LgAMT * 2
                    WHEN p_size = 'XLarge' THEN t.topping_BusPrice * t.topping_XLAMT * 2
                END
            ELSE
                CASE
                    WHEN p_size = 'Small' THEN t.topping_BusPrice * t.topping_SmallAMT
                    WHEN p_size = 'Medium' THEN t.topping_BusPrice * t.topping_MedAMT
                    WHEN p_size = 'Large' THEN t.topping_BusPrice * t.topping_LgAMT
                    WHEN p_size = 'XLarge' THEN t.topping_BusPrice * t.topping_XLAMT
                END
        END)
    INTO topping_cust_price, topping_bus_price
    FROM pizza_topping pt
    JOIN topping t ON pt.topping_TopID = t.topping_TopID
    WHERE pt.pizza_PizzaID = p_pizza_id;
    
    SET p_cust_price = base_cust_price + IFNULL(topping_cust_price, 0);
    SET p_bus_price = base_bus_price + IFNULL(topping_bus_price, 0);
END //
DELIMITER ;

-- Stored Procedure: Apply discounts to a pizza
DELIMITER //
CREATE PROCEDURE ApplyPizzaDiscounts(
    IN p_pizza_id INT
)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE d_id INT;
    DECLARE d_amount DECIMAL(5,2);
    DECLARE d_is_percent TINYINT;
    DECLARE original_price DECIMAL(5,2);
    DECLARE current_price DECIMAL(5,2);
    
    -- Cursor for discounts (dollar amounts first, then percentages)
    DECLARE discount_cursor CURSOR FOR
        SELECT d.discount_DiscountID, d.discount_Amount, d.discount_IsPercent
        FROM pizza_discount pd
        JOIN discount d ON pd.discount_DiscountID = d.discount_DiscountID
        WHERE pd.pizza_PizzaID = p_pizza_id
        ORDER BY d.discount_IsPercent;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    SELECT pizza_CustPrice INTO original_price FROM pizza WHERE pizza_PizzaID = p_pizza_id;
    SET current_price = original_price;
    
    OPEN discount_cursor;
    
    discount_loop: LOOP
        FETCH discount_cursor INTO d_id, d_amount, d_is_percent;
        IF done THEN
            LEAVE discount_loop;
        END IF;
        
        IF d_is_percent = 1 THEN
            SET current_price = current_price * (1 - d_amount/100);
        ELSE
            SET current_price = current_price - d_amount;
        END IF;
        
        IF current_price < 0 THEN
            SET current_price = 0;
        END IF;
    END LOOP;
    
    CLOSE discount_cursor;
    
    UPDATE pizza SET pizza_CustPrice = current_price WHERE pizza_PizzaID = p_pizza_id;
END //
DELIMITER ;

-- Calculate order total (customer price)
DELIMITER //
DROP FUNCTION IF EXISTS CalculateOrderTotal; //
CREATE FUNCTION CalculateOrderTotal(p_order_id INT) RETURNS DECIMAL(7,2) -- Increased precision slightly
    DETERMINISTIC
BEGIN
    DECLARE v_pizza_total DECIMAL(7,2);
    DECLARE v_final_total DECIMAL(7,2);
    DECLARE v_discount_amount DECIMAL(5,2);
    DECLARE v_is_percent BOOLEAN;
    DECLARE done INT DEFAULT FALSE;

    -- Cursor for dollar amount discounts
    DECLARE dollar_discount_cursor CURSOR FOR
        SELECT d.discount_Amount
        FROM order_discount od
                 JOIN discount d ON od.discount_DiscountID = d.discount_DiscountID
        WHERE od.ordertable_OrderID = p_order_id AND d.discount_IsPercent = 0; -- Dollar discounts first

    -- Cursor for percentage discounts
    DECLARE percent_discount_cursor CURSOR FOR
        SELECT d.discount_Amount
        FROM order_discount od
                 JOIN discount d ON od.discount_DiscountID = d.discount_DiscountID
        WHERE od.ordertable_OrderID = p_order_id AND d.discount_IsPercent = 1; -- Percentage discounts second

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- 1. Calculate the sum of pizza prices (before order discounts)
    SELECT IFNULL(SUM(pizza_CustPrice), 0) INTO v_pizza_total
    FROM pizza
    WHERE ordertable_OrderID = p_order_id;

    SET v_final_total = v_pizza_total;

    -- 2. Apply dollar amount discounts
    OPEN dollar_discount_cursor;
    dollar_loop: LOOP
        FETCH dollar_discount_cursor INTO v_discount_amount;
        IF done THEN
            LEAVE dollar_loop;
        END IF;
        SET v_final_total = v_final_total - v_discount_amount;
    END LOOP;
    CLOSE dollar_discount_cursor;

    -- Reset done flag for next cursor
    SET done = FALSE;

    -- 3. Apply percentage discounts
    OPEN percent_discount_cursor;
    percent_loop: LOOP
        FETCH percent_discount_cursor INTO v_discount_amount;
        IF done THEN
            LEAVE percent_loop;
        END IF;
        -- Apply percentage to the current total after dollar discounts
        SET v_final_total = v_final_total * (1 - v_discount_amount / 100.0);
    END LOOP;
    CLOSE percent_discount_cursor;

    -- Ensure total doesn't go below zero
    IF v_final_total < 0 THEN
        SET v_final_total = 0;
    END IF;

    -- Return the final calculated price including order discounts
    RETURN v_final_total;
END //
DELIMITER ;

-- Calculate order cost (business cost)
DELIMITER //
CREATE FUNCTION CalculateOrderCost(p_order_id INT) RETURNS DECIMAL(5,2)
DETERMINISTIC
BEGIN
    DECLARE total DECIMAL(5,2);
    
    SELECT SUM(pizza_BusPrice) INTO total
    FROM pizza
    WHERE ordertable_OrderID = p_order_id;
    
    RETURN IFNULL(total, 0);
END //
DELIMITER ;

-- Insert Trigger: Update inventory when adding topping to a pizza
DELIMITER //
CREATE TRIGGER after_pizza_topping_insert
    AFTER INSERT ON pizza_topping
    FOR EACH ROW
BEGIN
    -- DECLARE statements MUST come immediately after BEGIN
    DECLARE p_size VARCHAR(30);
    DECLARE t_amount DECIMAL(5,2);
    DECLARE multiplier INT;

    -- Original executable trigger logic starts here
    SELECT p.pizza_Size INTO p_size -- Selecting the 'Size' column from pizza table
    FROM pizza p
    WHERE p.pizza_PizzaID = NEW.pizza_PizzaID;

    IF NEW.pizza_topping_isDouble = 1 THEN
        SET multiplier = 2;
    ELSE
        SET multiplier = 1;
    END IF;

    -- Determine the amount based on pizza size (using the p_size variable)
    CASE p_size
        WHEN 'Small' THEN
            SELECT t.topping_SmallAMT INTO t_amount
            FROM topping t
            WHERE t.topping_TopID = NEW.topping_TopID;
        WHEN 'Medium' THEN
            SELECT t.topping_MedAMT INTO t_amount
            FROM topping t
            WHERE t.topping_TopID = NEW.topping_TopID;
        WHEN 'Large' THEN
            SELECT t.topping_LgAMT INTO t_amount
            FROM topping t
            WHERE t.topping_TopID = NEW.topping_TopID;
        WHEN 'XLarge' THEN
            SELECT t.topping_XLAMT INTO t_amount
            FROM topping t
            WHERE t.topping_TopID = NEW.topping_TopID;
        END CASE;

    -- Update the inventory if an amount was found
    IF t_amount IS NOT NULL THEN
        UPDATE topping
        SET topping_CurINVT = topping_CurINVT - (t_amount * multiplier)
        WHERE topping_TopID = NEW.topping_TopID;
    END IF;
END //
DELIMITER ;

-- Insert Trigger: Update order totals when adding a new pizza
DELIMITER //
CREATE TRIGGER after_pizza_insert
AFTER INSERT ON pizza
FOR EACH ROW
BEGIN
    UPDATE ordertable
    SET ordertable_CustPrice = CalculateOrderTotal(NEW.ordertable_OrderID),
        ordertable_BusPrice = CalculateOrderCost(NEW.ordertable_OrderID)
    WHERE ordertable_OrderID = NEW.ordertable_OrderID;
END //
DELIMITER ;

-- Update Trigger: Update order totals when updating a pizza
DELIMITER //
CREATE TRIGGER after_pizza_update
AFTER UPDATE ON pizza
FOR EACH ROW
BEGIN
    UPDATE ordertable
    SET ordertable_CustPrice = CalculateOrderTotal(NEW.ordertable_OrderID),
        ordertable_BusPrice = CalculateOrderCost(NEW.ordertable_OrderID)
    WHERE ordertable_OrderID = NEW.ordertable_OrderID;
END //
DELIMITER ;

-- Update Trigger: Check complete status when updating pizza state
DELIMITER //
CREATE TRIGGER after_pizza_state_update
AFTER UPDATE ON pizza
FOR EACH ROW
BEGIN
    DECLARE all_completed BOOLEAN;
    
    IF NEW.pizza_PizzaState = 'completed' AND OLD.pizza_PizzaState != 'completed' THEN
        SELECT COUNT(*) = 0 INTO all_completed
        FROM pizza
        WHERE ordertable_OrderID = NEW.ordertable_OrderID
        AND pizza_PizzaState != 'completed';
        
        IF all_completed THEN
            UPDATE ordertable
            SET ordertable_isComplete = 1
            WHERE ordertable_OrderID = NEW.ordertable_OrderID;
        END IF;
    END IF;
END //
DELIMITER ;
