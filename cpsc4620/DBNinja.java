package cpsc4620;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 * This file is where you will implement the methods needed to support this application.
 * You will write the code to retrieve and save information to the database and use that
 * information to build the various objects required by the applicaiton.
 *
 * The class has several hard coded static variables used for the connection, you will need to
 * change those to your connection information
 *
 * This class also has static string variables for pickup, delivery and dine-in.
 * DO NOT change these constant values.
 *
 * You can add any helper methods you need, but you must implement all the methods
 * in this class and use them to complete the project.  The autograder will rely on
 * these methods being implemented, so do not delete them or alter their method
 * signatures.
 *
 * Make sure you properly open and close your DB connections in any method that
 * requires access to the DB.
 * Use the connect_to_db below to open your connection in DBConnector.
 * What is opened must be closed!
 */

/*
 * A utility class to help add and retrieve information from the database
 */
public final class DBNinja {
    private static Connection conn;

    // DO NOT change these variables!
    public final static String pickup = "pickup";
    public final static String delivery = "delivery";
    public final static String dine_in = "dinein";

    public final static String size_s = "Small";
    public final static String size_m = "Medium";
    public final static String size_l = "Large";
    public final static String size_xl = "XLarge";

    public final static String crust_thin = "Thin";
    public final static String crust_orig = "Original";
    public final static String crust_pan = "Pan";
    public final static String crust_gf = "Gluten-Free";

    public enum order_state
    {
        PREPARED,
        DELIVERED,
        PICKEDUP
    }

    private static boolean connect_to_db() throws SQLException, IOException
    {
        try {
            conn = DBConnector.make_connection();
            if (conn == null) {
                System.err.println("Connection failed: DBConnector.make_connection() returned null");
                return false;
            }
            return true;
        } catch (SQLException e) {
            System.err.println("SQL Exception in connect_to_db(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            System.err.println("IO Exception in connect_to_db(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static void addOrder(Order o) throws SQLException, IOException
    {
        /*
         * add code to add the order to the DB. Remember that we're not just
         * adding the order to the order DB table, but we're also recording
         * the necessary data for the delivery, dinein, pickup, pizzas, toppings
         * on pizzas, order discounts and pizza discounts.
         *
         * This is a KEY method as it must store all the data in the Order object
         * in the database and make sure all the tables are correctly linked.
         *
         * Remember, if the order is for Dine In, there is no customer...
         * so the cusomter id coming from the Order object will be -1.
         *
         */
        connect_to_db();

        try {
            // Start transaction
            conn.setAutoCommit(false);

            String orderSQL = "INSERT INTO ordertable (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_isComplete) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement os = conn.prepareStatement(orderSQL, Statement.RETURN_GENERATED_KEYS);

            os.setInt(1, o.getCustID());
            os.setString(2, o.getOrderType());
            os.setString(3, o.getDate());
            os.setDouble(4, o.getCustPrice());
            os.setDouble(5, o.getBusPrice());
            os.setBoolean(6, o.getIsComplete());

            os.executeUpdate();

            // Get the generated order ID
            ResultSet rs = os.getGeneratedKeys();
            int orderID = -1;
            if(rs.next()) {
                orderID = rs.getInt(1);
                o.setOrderID(orderID);
            }

            // Insert into the appropriate subtype table
            if(o.getOrderType().equals(dine_in)) {
                DineinOrder dineIn = (DineinOrder) o;
                String dineInSQL = "INSERT INTO dinein (ordertable_OrderID, dinein_TableNum) VALUES (?, ?)";
                PreparedStatement ds = conn.prepareStatement(dineInSQL);
                ds.setInt(1, orderID);
                ds.setInt(2, dineIn.getTableNum());
                ds.executeUpdate();
                ds.close();
            }
            else if(o.getOrderType().equals(delivery)) {
                DeliveryOrder delivery = (DeliveryOrder) o;
                String deliverySQL = "INSERT INTO delivery (ordertable_OrderID, delivery_Street, delivery_isDelivered) VALUES (?, ?, ?)";
                PreparedStatement ds = conn.prepareStatement(deliverySQL);
                ds.setInt(1, orderID);
                ds.setString(2, delivery.getAddress());
                ds.setBoolean(3, false); // Not delivered yet
                ds.executeUpdate();
                ds.close();
            }
            else if(o.getOrderType().equals(pickup)) {
                PickupOrder pickup = (PickupOrder) o;
                String pickupSQL = "INSERT INTO pickup (ordertable_OrderID, pickup_isPickedUp) VALUES (?, ?)";
                PreparedStatement ps = conn.prepareStatement(pickupSQL);
                ps.setInt(1, orderID);
                ps.setBoolean(2, false); // Not picked up yet
                ps.executeUpdate();
                ps.close();
            }

            // Add all pizzas in the order
            for(Pizza p : o.getPizzaList()) {
                p.setOrderID(orderID);
                int pizzaID = addPizza(java.sql.Timestamp.valueOf(p.getPizzaDate()), orderID, p);
            }

            // Add all order discounts
            for(Discount d : o.getDiscountList()) {
                String discountSQL = "INSERT INTO order_discount (ordertable_OrderID, discount_DiscountID) VALUES (?, ?)";
                PreparedStatement ds = conn.prepareStatement(discountSQL);
                ds.setInt(1, orderID);
                ds.setInt(2, d.getDiscountID());
                ds.executeUpdate();
                ds.close();
            }

            conn.commit();
        } catch(SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public static int addPizza(java.util.Date d, int orderID, Pizza p) throws SQLException, IOException
    {
        /*
         * Add the code needed to insert the pizza into into the database.
         * Keep in mind you must also add the pizza discounts and toppings
         * associated with the pizza.
         *
         * NOTE: there is a Date object passed into this method so that the Order
         * and ALL its Pizzas can be assigned the same DTS.
         *
         * This method returns the id of the pizza just added.
         *
         */
        if(conn == null || conn.isClosed()) {
            connect_to_db();
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = dateFormat.format(d);

            String pizzaSQL = "INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(pizzaSQL, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, p.getSize());
            ps.setString(2, p.getCrustType());
            ps.setString(3, p.getPizzaState());
            ps.setString(4, formattedDate);
            ps.setDouble(5, p.getCustPrice());
            ps.setDouble(6, p.getBusPrice());
            ps.setInt(7, orderID);

            ps.executeUpdate();

            // Get the generated pizza ID
            ResultSet rs = ps.getGeneratedKeys();
            int pizzaID = -1;
            if(rs.next()) {
                pizzaID = rs.getInt(1);
                p.setPizzaID(pizzaID);
            }

            // Add pizza toppings
            for(Topping t : p.getToppings()) {
                boolean isExtra = t.getDoubled();
                String toppingSQL = "INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_isDouble) VALUES (?, ?, ?)";
                PreparedStatement ts = conn.prepareStatement(toppingSQL);
                ts.setInt(1, pizzaID);
                ts.setInt(2, t.getTopID());
                ts.setBoolean(3, isExtra);
                ts.executeUpdate();

                // Update inventory
                double unitsToRemove = 0.0;
                if(p.getSize().equals(size_s)) {
                    unitsToRemove = t.getSmallAMT();
                } else if(p.getSize().equals(size_m)) {
                    unitsToRemove = t.getMedAMT();
                } else if(p.getSize().equals(size_l)) {
                    unitsToRemove = t.getLgAMT();
                } else {
                    unitsToRemove = t.getXLAMT();
                }

                if(isExtra) {
                    unitsToRemove *= 2; // Double the amount for extra toppings
                }

                String updateInventorySQL = "UPDATE topping SET topping_CurINVT = topping_CurINVT - ? WHERE topping_TopID = ?";
                PreparedStatement us = conn.prepareStatement(updateInventorySQL);
                us.setDouble(1, unitsToRemove);
                us.setInt(2, t.getTopID());
                us.executeUpdate();
                us.close();
                ts.close();
            }

            for(Discount disc : p.getDiscounts()) {
                String discountSQL = "INSERT INTO pizza_discount (pizza_PizzaID, discount_DiscountID) VALUES (?, ?)";
                PreparedStatement ds = conn.prepareStatement(discountSQL);
                ds.setInt(1, pizzaID);
                ds.setInt(2, disc.getDiscountID());
                ds.executeUpdate();
                ds.close();
            }

            ps.close();
            return pizzaID;
        } catch(SQLException e) {
            throw e;
        }
    }

    public static int addCustomer(Customer c) throws SQLException, IOException
    {
        /*
         * This method adds a new customer to the database.
         *
         */
        connect_to_db();

        try {
            String customerSQL = "INSERT INTO customer (customer_FName, customer_LName, customer_PhoneNum) VALUES (?, ?, ?)";
            PreparedStatement cs = conn.prepareStatement(customerSQL, Statement.RETURN_GENERATED_KEYS);

            cs.setString(1, c.getFName());
            cs.setString(2, c.getLName());
            cs.setString(3, c.getPhone());

            cs.executeUpdate();

            // Get the generated customer ID
            ResultSet rs = cs.getGeneratedKeys();
            int custID = -1;
            if(rs.next()) {
                custID = rs.getInt(1);
                c.setCustID(custID);
            }

            cs.close();
            conn.close();
            return custID;
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static void completeOrder(int OrderID, order_state newState) throws SQLException, IOException
    {
        /*
         * Mark that order as complete in the database.
         * Note: if an order is complete, this means all the pizzas are complete as well.
         * However, it does not mean that the order has been delivered or picked up!
         *
         * For newState = PREPARED: mark the order and all associated pizza's as completed
         * For newState = DELIVERED: mark the delivery status
         * FOR newState = PICKEDUP: mark the pickup status
         *
         */
        connect_to_db();

        try {
            if(newState == order_state.PREPARED) {
                // Mark the order as complete
                String updateOrderSQL = "UPDATE ordertable SET ordertable_isComplete = 1 WHERE ordertable_OrderID = ?";
                PreparedStatement os = conn.prepareStatement(updateOrderSQL);
                os.setInt(1, OrderID);
                os.executeUpdate();
                os.close();

                // Mark all pizzas as complete
                String updatePizzaSQL = "UPDATE pizza SET pizza_PizzaState = 'Completed' WHERE ordertable_OrderID = ?";
                PreparedStatement ps = conn.prepareStatement(updatePizzaSQL);
                ps.setInt(1, OrderID);
                ps.executeUpdate();
                ps.close();
            }
            else if(newState == order_state.DELIVERED) {
                // Mark the delivery as delivered
                String updateDeliverySQL = "UPDATE delivery SET delivery_isDelivered = 1 WHERE ordertable_OrderID = ?";
                PreparedStatement ds = conn.prepareStatement(updateDeliverySQL);
                ds.setInt(1, OrderID);
                ds.executeUpdate();
                ds.close();
            }
            else if(newState == order_state.PICKEDUP) {
                // Mark the pickup as picked up
                String updatePickupSQL = "UPDATE pickup SET pickup_isPickedUp = 1 WHERE ordertable_OrderID = ?";
                PreparedStatement ps = conn.prepareStatement(updatePickupSQL);
                ps.setInt(1, OrderID);
                ps.executeUpdate();
                ps.close();
            }

            conn.close();
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static ArrayList<Order> getOrders(int status) throws SQLException, IOException
    {
        /*
         * Return an ArrayList of orders.
         *      status   == 1 => return a list of open (ie oder is not completed)
         *           == 2 => return a list of completed orders (ie order is complete)
         *           == 3 => return a list of all the orders
         * Remember that in Java, we account for supertypes and subtypes
         * which means that when we create an arrayList of orders, that really
         * means we have an arrayList of dineinOrders, deliveryOrders, and pickupOrders.
         *
         * You must fully populate the Order object, this includes order discounts,
         * and pizzas along with the toppings and discounts associated with them.
         *
         * Don't forget to order the data coming from the database appropriately.
         *
         */
        connect_to_db();
        ArrayList<Order> orders = new ArrayList<>(); // Ensure orders is always initialized

        try {
            String query;

            // Determine query based on status
            if (status == 1) {
                query = "SELECT * FROM ordertable WHERE ordertable_isComplete = 0 ORDER BY ordertable_OrderDateTime";
            } else if (status == 2) {
                query = "SELECT * FROM ordertable WHERE ordertable_isComplete = 1 ORDER BY ordertable_OrderDateTime";
            } else {
                query = "SELECT * FROM ordertable ORDER BY ordertable_OrderDateTime";
            }

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                int orderID = rs.getInt("ordertable_OrderID");
                int custID = rs.getInt("customer_CustID");
                String orderType = rs.getString("ordertable_OrderType");
                String date = rs.getString("ordertable_OrderDateTime");
                double custPrice = rs.getDouble("ordertable_CustPrice");
                double busPrice = rs.getDouble("ordertable_BusPrice");
                boolean isComplete = rs.getBoolean("ordertable_isComplete");

                Order order;

                // Handle different order types
                if (orderType.equals(dine_in)) {
                    String dineInQuery = "SELECT * FROM dinein WHERE ordertable_OrderID = ?";
                    PreparedStatement ds = conn.prepareStatement(dineInQuery);
                    ds.setInt(1, orderID);
                    ResultSet dineInRS = ds.executeQuery();

                    int tableNum = 0;
                    if (dineInRS.next()) {
                        tableNum = dineInRS.getInt("dinein_TableNum");
                    }

                    order = new DineinOrder(orderID, custID, date, custPrice, busPrice, isComplete, tableNum);
                    dineInRS.close();
                    ds.close();
                } else if (orderType.equals(delivery)) {
                    String deliveryQuery = "SELECT * FROM delivery WHERE ordertable_OrderID = ?";
                    PreparedStatement ds = conn.prepareStatement(deliveryQuery);
                    ds.setInt(1, orderID);
                    ResultSet deliveryRS = ds.executeQuery();

                    String address = "";
                    boolean isDelivered = false;
                    if (deliveryRS.next()) {
                        address = deliveryRS.getString("delivery_Street");
                        isDelivered = deliveryRS.getBoolean("delivery_isDelivered");
                    }

                    order = new DeliveryOrder(orderID, custID, date, custPrice, busPrice, isComplete, isDelivered, address);
                    deliveryRS.close();
                    ds.close();
                } else {
                    String pickupQuery = "SELECT * FROM pickup WHERE ordertable_OrderID = ?";
                    PreparedStatement ps = conn.prepareStatement(pickupQuery);
                    ps.setInt(1, orderID);
                    ResultSet pickupRS = ps.executeQuery();

                    boolean isPickedUp = false;
                    if (pickupRS.next()) {
                        isPickedUp = pickupRS.getBoolean("pickup_isPickedUp");
                    }

                    order = new PickupOrder(orderID, custID, date, custPrice, busPrice, isComplete, isPickedUp);
                    pickupRS.close();
                    ps.close();
                }

                // Populate pizzas and discounts for the order
                ArrayList<Pizza> pizzas = getPizzas(order);
                order.setPizzaList(pizzas);

                ArrayList<Discount> discounts = getDiscounts(order);
                order.setDiscountList(discounts);

                orders.add(order);
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
            throw e;
        }

        return orders; // Always return a non-null list
    }

    public static Order getLastOrder() throws SQLException, IOException
    {
        /*
         * Query the database for the LAST order added
         * then return an Order object for that order.
         * NOTE...there will ALWAYS be a "last order"!
         */

        connect_to_db();
        Order order = null;

        try {
            String query = "SELECT * FROM ordertable ORDER BY ordertable_OrderID DESC LIMIT 1";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            if(rs.next()) {
                int orderID = rs.getInt("ordertable_OrderID");
                int custID = rs.getInt("customer_CustID");
                String orderType = rs.getString("ordertable_OrderType");
                String date = rs.getString("ordertable_OrderDateTime");
                double custPrice = rs.getDouble("ordertable_CustPrice");
                double busPrice = rs.getDouble("ordertable_BusPrice");
                boolean isComplete = rs.getBoolean("ordertable_isComplete");

                if(orderType.equals(dine_in)) {
                    String dineInQuery = "SELECT * FROM dinein WHERE ordertable_OrderID = ?";
                    PreparedStatement ds = conn.prepareStatement(dineInQuery);
                    ds.setInt(1, orderID);
                    ResultSet dineInRS = ds.executeQuery();

                    int tableNum = 0;
                    if(dineInRS.next()) {
                        tableNum = dineInRS.getInt("dinein_TableNum");
                    }

                    order = new DineinOrder(orderID, custID, date, custPrice, busPrice, isComplete, tableNum);
                    dineInRS.close();
                    ds.close();
                }
                else if(orderType.equals(delivery)) {
                    String deliveryQuery = "SELECT * FROM delivery WHERE ordertable_OrderID = ?";
                    PreparedStatement ds = conn.prepareStatement(deliveryQuery);
                    ds.setInt(1, orderID);
                    ResultSet deliveryRS = ds.executeQuery();

                    String address = "";
                    boolean isDelivered = false;
                    if(deliveryRS.next()) {
                        address = deliveryRS.getString("delivery_Street");
                        isDelivered = deliveryRS.getBoolean("delivery_isDelivered");
                    }

                    order = new DeliveryOrder(orderID, custID, date, custPrice, busPrice, isComplete, isDelivered, address);
                    deliveryRS.close();
                    ds.close();
                }
                else {
                    String pickupQuery = "SELECT * FROM pickup WHERE ordertable_OrderID = ?";
                    PreparedStatement ps = conn.prepareStatement(pickupQuery);
                    ps.setInt(1, orderID);
                    ResultSet pickupRS = ps.executeQuery();

                    boolean isPickedUp = false;
                    if(pickupRS.next()) {
                        isPickedUp = pickupRS.getBoolean("pickup_isPickedUp");
                    }

                    order = new PickupOrder(orderID, custID, date, custPrice, busPrice, isComplete, isPickedUp);
                    pickupRS.close();
                    ps.close();
                }

                ArrayList<Pizza> pizzas = getPizzas(order);
                order.setPizzaList(pizzas);

                ArrayList<Discount> discounts = getDiscounts(order);
                order.setDiscountList(discounts);
            }

            rs.close();
            stmt.close();
            conn.close();
            return order;
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static ArrayList<Order> getOrdersByDate(String date) throws SQLException, IOException
    {
        /*
         * Query the database for ALL the orders placed on a specific date
         * and return a list of those orders.
         *
         */

        connect_to_db();
        ArrayList<Order> orders = new ArrayList<Order>();

        try {
            // Ensure the date is in the right format: YYYY-MM-DD
            String query = "SELECT * FROM ordertable WHERE DATE(ordertable_OrderDateTime) = ? ORDER BY ordertable_OrderDateTime";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, date);
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                int orderID = rs.getInt("ordertable_OrderID");
                int custID = rs.getInt("customer_CustID");
                String orderType = rs.getString("ordertable_OrderType");
                String orderDate = rs.getString("ordertable_OrderDateTime");
                double custPrice = rs.getDouble("ordertable_CustPrice");
                double busPrice = rs.getDouble("ordertable_BusPrice");
                boolean isComplete = rs.getBoolean("ordertable_isComplete");

                Order order;

                if(orderType.equals(dine_in)) {
                    String dineInQuery = "SELECT * FROM dinein WHERE ordertable_OrderID = ?";
                    PreparedStatement ds = conn.prepareStatement(dineInQuery);
                    ds.setInt(1, orderID);
                    ResultSet dineInRS = ds.executeQuery();

                    int tableNum = 0;
                    if(dineInRS.next()) {
                        tableNum = dineInRS.getInt("dinein_TableNum");
                    }

                    order = new DineinOrder(orderID, custID, orderDate, custPrice, busPrice, isComplete, tableNum);
                    dineInRS.close();
                    ds.close();
                }
                else if(orderType.equals(delivery)) {
                    String deliveryQuery = "SELECT * FROM delivery WHERE ordertable_OrderID = ?";
                    PreparedStatement ds = conn.prepareStatement(deliveryQuery);
                    ds.setInt(1, orderID);
                    ResultSet deliveryRS = ds.executeQuery();

                    String address = "";
                    boolean isDelivered = false;
                    if(deliveryRS.next()) {
                        address = deliveryRS.getString("delivery_Street");
                        isDelivered = deliveryRS.getBoolean("delivery_isDelivered");
                    }

                    order = new DeliveryOrder(orderID, custID, orderDate, custPrice, busPrice, isComplete, isDelivered, address);
                    deliveryRS.close();
                    ds.close();
                }
                else {
                    String pickupQuery = "SELECT * FROM pickup WHERE ordertable_OrderID = ?";
                    PreparedStatement ps2 = conn.prepareStatement(pickupQuery);
                    ps2.setInt(1, orderID);
                    ResultSet pickupRS = ps2.executeQuery();

                    boolean isPickedUp = false;
                    if(pickupRS.next()) {
                        isPickedUp = pickupRS.getBoolean("pickup_isPickedUp");
                    }

                    order = new PickupOrder(orderID, custID, orderDate, custPrice, busPrice, isComplete, isPickedUp);
                    pickupRS.close();
                    ps2.close();
                }

                ArrayList<Pizza> pizzas = getPizzas(order);
                order.setPizzaList(pizzas);

                ArrayList<Discount> discounts = getDiscounts(order);
                order.setDiscountList(discounts);

                orders.add(order);
            }

            rs.close();
            ps.close();
            conn.close();
            return orders;
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static ArrayList<Discount> getDiscountList() throws SQLException, IOException
    {
        /*
         * Query the database for all the available discounts and
         * return them in an arrayList of discounts ordered by discount name.
         *
         */

        connect_to_db();
        ArrayList<Discount> discounts = new ArrayList<Discount>();

        try {
            String query = "SELECT * FROM discount ORDER BY discount_DiscountName";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while(rs.next()) {
                int discountID = rs.getInt("discount_DiscountID");
                String discountName = rs.getString("discount_DiscountName");
                double amount = rs.getDouble("discount_Amount");
                boolean isPercent = rs.getBoolean("discount_isPercent");

                Discount discount = new Discount(discountID, discountName, amount, isPercent);
                discounts.add(discount);
            }

            rs.close();
            stmt.close();
            conn.close();
            return discounts;
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static Discount findDiscountByName(String name) throws SQLException, IOException
    {
        /*
         * Query the database for a discount using it's name.
         * If found, then return an OrderDiscount object for the discount.
         * If it's not found....then return null
         *
         */

        connect_to_db();
        Discount discount = null;

        try {
            String query = "SELECT * FROM discount WHERE discount_DiscountName = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if(rs.next()) {
                int discountID = rs.getInt("discount_DiscountID");
                String discountName = rs.getString("discount_DiscountName");
                double amount = rs.getDouble("discount_Amount");
                boolean isPercent = rs.getBoolean("discount_isPercent");

                discount = new Discount(discountID, discountName, amount, isPercent);
            }

            rs.close();
            ps.close();
            conn.close();
            return discount;
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static ArrayList<Customer> getCustomerList() throws SQLException, IOException
    {
        /*
         * Query the data for all the customers and return an arrayList of all the customers.
         * Don't forget to order the data coming from the database appropriately.
         *
         */

        connect_to_db();
        ArrayList<Customer> customers = new ArrayList<Customer>();

        try {
            String query = "SELECT * FROM customer ORDER BY customer_LName, customer_FName, customer_PhoneNum";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while(rs.next()) {
                int custID = rs.getInt("customer_CustID");
                String fName = rs.getString("customer_FName");
                String lName = rs.getString("customer_LName");
                String phone = rs.getString("customer_PhoneNum");

                Customer customer = new Customer(custID, fName, lName, phone);
                customers.add(customer);
            }

            rs.close();
            stmt.close();
            conn.close();
            return customers;
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static Customer findCustomerByPhone(String phoneNumber) throws SQLException, IOException
    {
        /*
         * Query the database for a customer using a phone number.
         * If found, then return a Customer object for the customer.
         * If it's not found....then return null
         *
         */

        connect_to_db();
        Customer customer = null;

        try {
            String query = "SELECT * FROM customer WHERE customer_PhoneNum = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, phoneNumber);
            ResultSet rs = ps.executeQuery();

            if(rs.next()) {
                int custID = rs.getInt("customer_CustID");
                String fName = rs.getString("customer_FName");
                String lName = rs.getString("customer_LName");
                String phone = rs.getString("customer_PhoneNum");

                customer = new Customer(custID, fName, lName, phone);
            }

            rs.close();
            ps.close();
            conn.close();
            return customer;
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static String getCustomerName(int CustID) throws SQLException, IOException
    {
        /*
         * COMPLETED...WORKING Example!
         *
         * This is a helper method to fetch and format the name of a customer
         * based on a customer ID. This is an example of how to interact with
         * your database from Java.
         *
         * Notice how the connection to the DB made at the start of the
         *
         */

        connect_to_db();

        /*
         * an example query using a constructed string...
         * remember, this style of query construction could be subject to sql injection attacks!
         *
         */
        String cname1 = "";
        String cname2 = "";
        String query = "Select customer_FName, customer_LName From customer WHERE customer_CustID=" + CustID + ";";
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery(query);

        while(rset.next())
        {
            cname1 = rset.getString(1) + " " + rset.getString(2);
        }

        /*
         * an BETTER example of the same query using a prepared statement...
         * with exception handling
         *
         */
        try {
            PreparedStatement os;
            ResultSet rset2;
            String query2;
            query2 = "Select customer_FName, customer_LName From customer WHERE customer_CustID=?;";
            os = conn.prepareStatement(query2);
            os.setInt(1, CustID);
            rset2 = os.executeQuery();
            while(rset2.next())
            {
                cname2 = rset2.getString("customer_FName") + " " + rset2.getString("customer_LName"); // note the use of field names in the getSting methods
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // process the error or re-raise the exception to a higher level
        }

        conn.close();

        return cname1;
        // OR
        // return cname2;
    }

    public static ArrayList<Topping> getToppingList() throws SQLException, IOException
    {
        /*
         * Query the database for the aviable toppings and
         * return an arrayList of all the available toppings.
         * Don't forget to order the data coming from the database appropriately.
         *
         */

        connect_to_db();
        ArrayList<Topping> toppings = new ArrayList<Topping>();

        try {
            String query = "SELECT * FROM topping ORDER BY topping_TopName";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while(rs.next()) {
                int topID = rs.getInt("topping_TopID");
                String topName = rs.getString("topping_TopName");
                double smallAmt = rs.getDouble("topping_SmallAMT");
                double medAmt = rs.getDouble("topping_MedAMT");
                double lgAmt = rs.getDouble("topping_LgAMT");
                double xlAmt = rs.getDouble("topping_XLAMT");
                double custPrice = rs.getDouble("topping_CustPrice");
                double busPrice = rs.getDouble("topping_BusPrice");
                int minInvt = rs.getInt("topping_MinINVT");
                int curInvt = rs.getInt("topping_CurINVT");

                Topping topping = new Topping(topID, topName, smallAmt, medAmt, lgAmt, xlAmt, custPrice, busPrice, minInvt, curInvt);
                toppings.add(topping);
            }

            rs.close();
            stmt.close();
            conn.close();
            return toppings;
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static Topping findToppingByName(String name) throws SQLException, IOException
    {
        /*
         * Query the database for the topping using it's name.
         * If found, then return a Topping object for the topping.
         * If it's not found....then return null
         *
         */

        connect_to_db();
        Topping topping = null;

        try {
            String query = "SELECT * FROM topping WHERE topping_TopName = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if(rs.next()) {
                int topID = rs.getInt("topping_TopID");
                String topName = rs.getString("topping_TopName");
                double smallAmt = rs.getDouble("topping_SmallAMT");
                double medAmt = rs.getDouble("topping_MedAMT");
                double lgAmt = rs.getDouble("topping_LgAMT");
                double xlAmt = rs.getDouble("topping_XLAMT");
                double custPrice = rs.getDouble("topping_CustPrice");
                double busPrice = rs.getDouble("topping_BusPrice");
                int minInvt = rs.getInt("topping_MinINVT");
                int curInvt = rs.getInt("topping_CurINVT");

                topping = new Topping(topID, topName, smallAmt, medAmt, lgAmt, xlAmt, custPrice, busPrice, minInvt, curInvt);
            }

            rs.close();
            ps.close();
            conn.close();
            return topping;
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static ArrayList<Topping> getToppingsOnPizza(Pizza p) throws SQLException, IOException
    {
        /*
         * This method builds an ArrayList of the toppings ON a pizza.
         * The list can then be added to the Pizza object elsewhere in the
         */

        connect_to_db();
        ArrayList<Topping> toppings = new ArrayList<Topping>();

        try {
            String query = "SELECT t.*, pt.pizza_topping_isDouble FROM topping t " +
                    "JOIN pizza_topping pt ON t.topping_TopID = pt.topping_TopID " +
                    "WHERE pt.pizza_PizzaID = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, p.getPizzaID());
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                int topID = rs.getInt("topping_TopID");
                String topName = rs.getString("topping_TopName");
                double smallAmt = rs.getDouble("topping_SmallAMT");
                double medAmt = rs.getDouble("topping_MedAMT");
                double lgAmt = rs.getDouble("topping_LgAMT");
                double xlAmt = rs.getDouble("topping_XLAMT");
                double custPrice = rs.getDouble("topping_CustPrice");
                double busPrice = rs.getDouble("topping_BusPrice");
                int minInvt = rs.getInt("topping_MinINVT");
                int curInvt = rs.getInt("topping_CurINVT");
                boolean isDouble = rs.getBoolean("pizza_topping_isDouble");

                Topping topping = new Topping(topID, topName, smallAmt, medAmt, lgAmt, xlAmt, custPrice, busPrice, minInvt, curInvt);
                topping.setDoubled(isDouble);
                toppings.add(topping);
            }

            rs.close();
            ps.close();
            conn.close();
            return toppings;
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static void addToInventory(int toppingID, double quantity) throws SQLException, IOException
    {
        /*
         * Updates the quantity of the topping in the database by the amount specified.
         *
         * */

        connect_to_db();

        try {
            String query = "UPDATE topping SET topping_CurINVT = topping_CurINVT + ? WHERE topping_TopID = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setDouble(1, quantity);
            ps.setInt(2, toppingID);
            ps.executeUpdate();

            ps.close();
            conn.close();
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static ArrayList<Pizza> getPizzas(Order o) throws SQLException, IOException
    {
        /*
         * Build an ArrayList of all the Pizzas associated with the Order.
         *
         */
        if(conn == null || conn.isClosed()) {
            connect_to_db();
        }
        ArrayList<Pizza> pizzas = new ArrayList<Pizza>();

        try {
            String query = "SELECT * FROM pizza WHERE ordertable_OrderID = ? ORDER BY pizza_PizzaID";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, o.getOrderID());
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                int pizzaID = rs.getInt("pizza_PizzaID");
                String size = rs.getString("pizza_Size");
                String crustType = rs.getString("pizza_CrustType");
                int orderID = rs.getInt("ordertable_OrderID");
                String pizzaState = rs.getString("pizza_PizzaState");
                String pizzaDate = rs.getString("pizza_PizzaDate");
                double custPrice = rs.getDouble("pizza_CustPrice");
                double busPrice = rs.getDouble("pizza_BusPrice");

                Pizza pizza = new Pizza(pizzaID, size, crustType, orderID, pizzaState, pizzaDate, custPrice, busPrice);
                pizza.setToppings(new ArrayList<Topping>());
                pizza.setDiscounts(new ArrayList<Discount>());

                // Add toppings to the pizza
                String toppingQuery = "SELECT t.*, pt.pizza_topping_isDouble FROM topping t " +
                        "JOIN pizza_topping pt ON t.topping_TopID = pt.topping_TopID " +
                        "WHERE pt.pizza_PizzaID = ?";
                PreparedStatement tps = conn.prepareStatement(toppingQuery);
                tps.setInt(1, pizzaID);
                ResultSet trs = tps.executeQuery();

                while(trs.next()) {
                    int topID = trs.getInt("topping_TopID");
                    String topName = trs.getString("topping_TopName");
                    double smallAmt = trs.getDouble("topping_SmallAMT");
                    double medAmt = trs.getDouble("topping_MedAMT");
                    double lgAmt = trs.getDouble("topping_LgAMT");
                    double xlAmt = trs.getDouble("topping_XLAMT");
                    double custToppingPrice = trs.getDouble("topping_CustPrice");
                    double busToppingPrice = trs.getDouble("topping_BusPrice");
                    int minInvt = trs.getInt("topping_MinINVT");
                    int curInvt = trs.getInt("topping_CurINVT");
                    boolean isDouble = trs.getBoolean("pizza_topping_isDouble");

                    Topping topping = new Topping(topID, topName, smallAmt, medAmt, lgAmt, xlAmt, custToppingPrice, busToppingPrice, minInvt, curInvt);
                    topping.setDoubled(isDouble);
                    pizza.addToppings(topping, isDouble);
                }
                trs.close();
                tps.close();

                // Add discounts to the pizza
                String discountQuery = "SELECT d.* FROM discount d " +
                        "JOIN pizza_discount pd ON d.discount_DiscountID = pd.discount_DiscountID " +
                        "WHERE pd.pizza_PizzaID = ?";
                PreparedStatement dps = conn.prepareStatement(discountQuery);
                dps.setInt(1, pizzaID);
                ResultSet drs = dps.executeQuery();

                while(drs.next()) {
                    int discountID = drs.getInt("discount_DiscountID");
                    String discountName = drs.getString("discount_DiscountName");
                    double amount = drs.getDouble("discount_Amount");
                    boolean isPercent = drs.getBoolean("discount_isPercent");

                    Discount discount = new Discount(discountID, discountName, amount, isPercent);
                    pizza.addDiscounts(discount);
                }
                drs.close();
                dps.close();

                pizzas.add(pizza);
            }

            rs.close();
            ps.close();
            return pizzas;
        } catch(SQLException e) {
            throw e;
        }
    }

    public static ArrayList<Discount> getDiscounts(Order o) throws SQLException, IOException
    {
        /*
         * Build an array list of all the Discounts associted with the Order.
         *
         */
        if(conn == null || conn.isClosed()) {
            connect_to_db();
        }
        ArrayList<Discount> discounts = new ArrayList<Discount>();

        try {
            String query = "SELECT d.* FROM discount d " +
                    "JOIN order_discount od ON d.discount_DiscountID = od.discount_DiscountID " +
                    "WHERE od.ordertable_OrderID = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, o.getOrderID());
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                int discountID = rs.getInt("discount_DiscountID");
                String discountName = rs.getString("discount_DiscountName");
                double amount = rs.getDouble("discount_Amount");
                boolean isPercent = rs.getBoolean("discount_isPercent");

                Discount discount = new Discount(discountID, discountName, amount, isPercent);
                discounts.add(discount);
            }

            rs.close();
            ps.close();
            return discounts;
        } catch(SQLException e) {
            throw e;
        }
    }

    public static ArrayList<Discount> getDiscounts(Pizza p) throws SQLException, IOException
    {
        /*
         * Build an array list of all the Discounts associted with the Pizza.
         *
         */

        connect_to_db();
        ArrayList<Discount> discounts = new ArrayList<Discount>();

        try {
            String query = "SELECT d.* FROM discount d " +
                    "JOIN pizza_discount pd ON d.discount_DiscountID = pd.discount_DiscountID " +
                    "WHERE pd.pizza_PizzaID = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, p.getPizzaID());
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                int discountID = rs.getInt("discount_DiscountID");
                String discountName = rs.getString("discount_DiscountName");
                double amount = rs.getDouble("discount_Amount");
                boolean isPercent = rs.getBoolean("discount_isPercent");

                Discount discount = new Discount(discountID, discountName, amount, isPercent);
                discounts.add(discount);
            }

            rs.close();
            ps.close();
            conn.close();
            return discounts;
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static double getBaseCustPrice(String size, String crust) throws SQLException, IOException
    {
        /*
         * Query the database fro the base customer price for that size and crust pizza.
         *
         */

        connect_to_db();
        double price = 0.0;

        try {
            String query = "SELECT baseprice_CustPrice FROM baseprice WHERE baseprice_Size = ? AND baseprice_CrustType = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, size);
            ps.setString(2, crust);
            ResultSet rs = ps.executeQuery();

            if(rs.next()) {
                price = rs.getDouble("baseprice_CustPrice");
            }

            rs.close();
            ps.close();
            conn.close();
            return price;
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static double getBaseBusPrice(String size, String crust) throws SQLException, IOException
    {
        /*
         * Query the database fro the base business price for that size and crust pizza.
         *
         */

        connect_to_db();
        double price = 0.0;

        try {
            String query = "SELECT baseprice_BusPrice FROM baseprice WHERE baseprice_Size = ? AND baseprice_CrustType = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, size);
            ps.setString(2, crust);
            ResultSet rs = ps.executeQuery();

            if(rs.next()) {
                price = rs.getDouble("baseprice_BusPrice");
            }

            rs.close();
            ps.close();
            conn.close();
            return price;
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }


    public static void printToppingReport() throws SQLException, IOException
    {
        /*
         * Prints the ToppingPopularity view. Remember that this view
         * needs to exist in your DB, so be sure you've run your createViews.sql
         * files on your testing DB if you haven't already.
         *
         * The result should be readable and sorted as indicated in the prompt.
         *
         * HINT: You need to match the expected output EXACTLY....I would suggest
         * you look at the printf method (rather that the simple print of println).
         * It operates the same in Java as it does in C and will make your code
         * better.
         *
         */

        connect_to_db();

        try {
            String query = "SELECT * FROM ToppingPopularity ORDER BY ToppingCount DESC, Topping";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            System.out.printf("%-20s %-15s\n", "Topping", "ToppingCount");
            System.out.printf("%-20s %-15s\n", "-------", "-------------");

            while(rs.next()) {
                String topping = rs.getString("Topping");
                int count = rs.getInt("ToppingCount");
                System.out.printf("%-20s %-15d\n", topping, count);
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static void printProfitByPizzaReport() throws SQLException, IOException
    {
        /*
         * Prints the ProfitByPizza view. Remember that this view
         * needs to exist in your DB, so be sure you've run your createViews.sql
         * files on your testing DB if you haven't already.
         *
         * The result should be readable and sorted as indicated in the prompt.
         *
         * HINT: You need to match the expected output EXACTLY....I would suggest
         * you look at the printf method (rather that the simple print of println).
         * It operates the same in Java as it does in C and will make your code
         * better.
         *
         */

        connect_to_db();

        try {
            String query = "SELECT * FROM ProfitByPizza ORDER BY Profit DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            System.out.printf("%-10s %-15s %-15s %-20s\n", "Pizza Size", "Pizza Crust", "Profit", "Last Order Date");
            System.out.printf("%-10s %-15s %-15s %-20s\n", "----------", "-----------", "------", "---------------");

            while(rs.next()) {
                String size = rs.getString("Size");
                String crust = rs.getString("Crust");
                double profit = rs.getDouble("Profit");
                String orderMonth = rs.getString("OrderMonth");

                System.out.printf("%-10s %-15s %-15.2f %-20s\n", size, crust, profit, orderMonth);
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static void printProfitByOrderTypeReport() throws SQLException, IOException
    {
        /*
         * Prints the ProfitByOrderType view. Remember that this view
         * needs to exist in your DB, so be sure you've run your createViews.sql
         * files on your testing DB if you haven't already.
         *
         * The result should be readable and sorted as indicated in the prompt.
         *
         * HINT: You need to match the expected output EXACTLY....I would suggest
         * you look at the printf method (rather that the simple print of println).
         * It operates the same in Java as it does in C and will make your code
         * better.
         *
         */

        connect_to_db();

        try {
            String query = "SELECT * FROM ProfitByOrderType ORDER BY customerType, OrderMonth";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            System.out.printf("%-15s %-15s %-20s %-20s %-15s\n", "Customer Type", "Order Month", "Total Order Price", "Total Order Cost", "Profit");
            System.out.printf("%-15s %-15s %-20s %-20s %-15s\n", "-------------", "-----------", "-----------------", "----------------", "------");

            double grandTotalPrice = 0.0;
            double grandTotalCost = 0.0;
            double grandTotalProfit = 0.0;

            while(rs.next()) {
                String custType = rs.getString("customerType");
                String orderMonth = rs.getString("OrderMonth");
                double totalPrice = rs.getDouble("TotalOrderPrice");
                double totalCost = rs.getDouble("TotalOrderCost");
                double profit = rs.getDouble("Profit");

                grandTotalPrice += totalPrice;
                grandTotalCost += totalCost;
                grandTotalProfit += profit;

                System.out.printf("%-15s %-15s %-20.2f %-20.2f %-15.2f\n", custType, orderMonth, totalPrice, totalCost, profit);
            }

            System.out.printf("%-15s %-15s %-20.2f %-20.2f %-15.2f\n", "Grand Total", "", grandTotalPrice, grandTotalCost, grandTotalProfit);

            rs.close();
            stmt.close();
            conn.close();
        } catch(SQLException e) {
            conn.close();
            throw e;
        }
    }

    /*
     * These private methods help get the individual components of an SQL datetime object.
     * You're welcome to keep them or remove them....but they are usefull!
     */
    private static int getYear(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
    {
        return Integer.parseInt(date.substring(0,4));
    }
    private static int getMonth(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
    {
        return Integer.parseInt(date.substring(5, 7));
    }
    private static int getDay(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
    {
        return Integer.parseInt(date.substring(8, 10));
    }

    public static boolean checkDate(int year, int month, int day, String dateOfOrder)
    {
        if(getYear(dateOfOrder) > year)
            return true;
        else if(getYear(dateOfOrder) < year)
            return false;
        else
        {
            if(getMonth(dateOfOrder) > month)
                return true;
            else if(getMonth(dateOfOrder) < month)
                return false;
            else
            {
                if(getDay(dateOfOrder) >= day)
                    return true;
                else
                    return false;
            }
        }
    }
}