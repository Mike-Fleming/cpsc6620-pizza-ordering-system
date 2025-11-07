# CPSC 6620 - Pizza Restaurant Ordering System

Complete database-driven Java application for managing a pizza restaurant ordering system.

## Project Overview

- **Course**: CPSC 6620 - Database Management Systems
- **Implementation**: Complete Java application across 11 files
- **Primary Implementation**: DBNinja.java

## Features Implemented

### CRUD Operations
- Customer management (create, read, update, delete)
- Order creation with multiple pizzas
- Pizza customization with toppings
- Discount application (pizza-level and order-level)
- Inventory management and tracking

### Order Types (Inheritance)
- **DineinOrder** - Table number management
- **DeliveryOrder** - Address and delivery tracking
- **PickupOrder** - Pickup time management

### Business Logic
- Multi-level discount calculations
- Inventory constraint checking (prevent negative inventory)
- Transaction management with commit/rollback
- Order state management (prepared, delivered, picked up)

### Reporting
- Topping popularity analysis
- Profit by pizza type
- Profit by order type and month

## Technical Implementation

### Key Technologies
- Java 8
- JDBC for database connectivity
- MySQL database
- PreparedStatements for SQL injection prevention
- Transaction management for data integrity

### Architecture
- **DBNinja.java** - Core business logic and database operations
- **Menu.java** - Command-line user interface
- **Entity Classes** - Order, Pizza, Customer, Topping, Discount
- **DBConnector.java** - Database connection management

### Advanced Features
- Object-relational mapping across 6+ database tables
- Complex SQL joins for data retrieval
- Atomic transactions for order creation
- Polymorphic order handling with instanceof checks
- Dynamic discount calculation algorithm

## Project Structure

```
cpsc4620/
├── DBNinja.java          - Main implementation
├── Menu.java             - User interface
├── Order.java            - Base order class (supertype)
├── DineinOrder.java      - Dine-in order subtype
├── DeliveryOrder.java    - Delivery order subtype
├── PickupOrder.java      - Pickup order subtype
├── Pizza.java            - Pizza entity
├── Customer.java         - Customer entity
├── Topping.java          - Topping inventory
├── Discount.java         - Discount business rules
└── DBConnector.java      - Database connection
```

## Setup Instructions

1. **Database Setup**
   - Create MySQL database named `PizzaDB`
   - Run SQL scripts from Part 2 to create tables and views

2. **Configure Database Connection**
   - Copy `cpsc4620/DBConnector.java.template` to `cpsc4620/DBConnector.java`
   - Update with your database credentials:
     ```java
     protected static String user = "YOUR_USERNAME";
     protected static String password = "YOUR_PASSWORD";
     ```

3. **Add MySQL JDBC Driver**
   - Download MySQL Connector/J
   - Add to project classpath as external library

4. **Compile and Run**
   ```bash
   javac cpsc4620/*.java
   java cpsc4620.Menu
   ```

## Constraints

- Cannot modify provided method signatures
- Cannot change database schema
- Must work within existing framework
- Only method signatures provided - all logic implemented independently
