# Nota-POS: Data Model (MVP Scope)

**Target Completion:** January 26, 2025  
**Focus:** Core POS functionality + Order Delay Feature  
**Authentication:** None (per instructor guidance)

---

## Entity Relationship Overview

```
TABLES ──┬─< ORDERS ──┬─< ORDER_ITEMS >──┬── MENU_ITEMS
         │            │                    │
         │            └─< PAYMENTS         └── MODIFIERS >── MODIFIER_GROUPS
         │
         └─< TABLE_STATUS_LOG
         
MENU_CATEGORIES ─< MENU_ITEMS

PREP_STATIONS ─< MENU_ITEMS
```

---

## Core Entities (MVP)

### 1. **TABLES**
Physical tables and seating areas in the restaurant.

```sql
CREATE TABLE tables (
    table_id INTEGER PRIMARY KEY AUTOINCREMENT,
    table_number VARCHAR(10) NOT NULL UNIQUE,  -- "F7", "B12", "Bar-3", "Patio-5"
    section VARCHAR(50),                        -- "Front", "Back", "Bar", "Patio"
    seat_count INTEGER,                         -- Capacity
    status VARCHAR(20) DEFAULT 'available',     -- 'available', 'occupied', 'needs_cleaning'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Business Rules:**
- `table_number` must be unique and human-readable
- `status` changes throughout service: available → occupied → needs_cleaning → available
- Sections help with server assignments

---

### 2. **ORDERS**
The check/ticket for a table. Core of the POS system.

```sql
CREATE TABLE orders (
    order_id INTEGER PRIMARY KEY AUTOINCREMENT,
    table_id INTEGER NOT NULL,
    server_name VARCHAR(100) NOT NULL,          -- No auth, so just name entry
    guest_count INTEGER DEFAULT 1,
    
    -- Order timing
    order_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    order_sent_at TIMESTAMP,                    -- When sent to kitchen (after delay)
    order_completed_at TIMESTAMP,               -- When all items ready
    order_closed_at TIMESTAMP,                  -- When paid and closed
    
    -- Order delay feature (THE SIGNATURE FEATURE)
    delay_seconds INTEGER DEFAULT 10,           -- Customizable delay (default 10 sec)
    delay_expires_at TIMESTAMP,                 -- Calculated: created_at + delay_seconds
    is_locked BOOLEAN DEFAULT FALSE,            -- TRUE after delay expires, no more edits
    
    -- Financial
    subtotal DECIMAL(10,2) DEFAULT 0.00,
    tax DECIMAL(10,2) DEFAULT 0.00,
    tip DECIMAL(10,2) DEFAULT 0.00,
    total DECIMAL(10,2) DEFAULT 0.00,
    
    -- Status
    status VARCHAR(20) DEFAULT 'open',          -- 'open', 'sent', 'ready', 'closed'
    notes TEXT,                                 -- General order notes (e.g., "Birthday - bring candle with dessert")
    
    FOREIGN KEY (table_id) REFERENCES tables(table_id) ON DELETE RESTRICT
);

CREATE INDEX idx_orders_table ON orders(table_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_server ON orders(server_name);
CREATE INDEX idx_orders_delay_expires ON orders(delay_expires_at);
```

**Business Rules:**
- **Order Delay Logic:**
  - When order created: `delay_expires_at = CURRENT_TIMESTAMP + delay_seconds`
  - Server can edit/add items while `is_locked = FALSE`
  - Background job checks: `IF CURRENT_TIMESTAMP >= delay_expires_at THEN is_locked = TRUE, status = 'sent'`
  - Once locked, order fires to kitchen printers
- Orders cannot be deleted, only voided
- One table can have multiple orders over time, but only one "open" order at a time
- **Order-level notes** are for general instructions that apply to the entire table/order (e.g., "Birthday table - bring candle with dessert", "VIP guest - manager comp appetizer")
- Item-level special instructions go in the `order_items` table

---

### 3. **MENU_CATEGORIES**
Organizational structure for menu items.

```sql
CREATE TABLE menu_categories (
    category_id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_name VARCHAR(100) NOT NULL UNIQUE, -- "Appetizers", "Entrees", "Drinks", "Desserts"
    display_order INTEGER,                      -- Sort order on menus
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Business Rules:**
- Categories help organize the menu UI
- `display_order` controls how they appear on server tablets
- Inactive categories don't show but preserve historical data

---

### 4. **PREP_STATIONS**
Kitchen stations where items are prepared.

```sql
CREATE TABLE prep_stations (
    station_id INTEGER PRIMARY KEY AUTOINCREMENT,
    station_name VARCHAR(50) NOT NULL UNIQUE,   -- "Sauté", "Fryer", "Cold Prep", "Bar"
    printer_name VARCHAR(100),                  -- Network printer name (future integration)
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Default Stations:**
- Sauté (hot line)
- Fryer
- Cold Prep / Salad Station
- Bar
- (Expandable for specific restaurant needs)

**Business Rules:**
- Each menu item assigned to one primary station
- Future: sends tickets to specific printers

---

### 5. **MENU_ITEMS**
Individual dishes and drinks available for order.

```sql
CREATE TABLE menu_items (
    item_id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_name VARCHAR(200) NOT NULL,
    category_id INTEGER NOT NULL,
    prep_station_id INTEGER NOT NULL,
    
    -- Pricing (supports multiple sizes/variants)
    base_price DECIMAL(10,2) NOT NULL,
    
    -- Kitchen info
    prep_time_minutes INTEGER,                  -- Average prep time
    description TEXT,
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,             -- Can be ordered?
    is_86d BOOLEAN DEFAULT FALSE,               -- Temporarily out of stock
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (category_id) REFERENCES menu_categories(category_id) ON DELETE RESTRICT,
    FOREIGN KEY (prep_station_id) REFERENCES prep_stations(station_id) ON DELETE RESTRICT
);

CREATE INDEX idx_menu_items_category ON menu_items(category_id);
CREATE INDEX idx_menu_items_station ON menu_items(prep_station_id);
CREATE INDEX idx_menu_items_active ON menu_items(is_active);
```

**Business Rules:**
- `base_price` is the default price; modifiers can adjust final price
- `is_86d` = TRUE hides item from ordering temporarily
- Items never deleted, only set `is_active = FALSE` to preserve order history

---

### 6. **MODIFIER_GROUPS**
Categories of modifications relevant to sit-down restaurants.

```sql
CREATE TABLE modifier_groups (
    group_id INTEGER PRIMARY KEY AUTOINCREMENT,
    group_name VARCHAR(100) NOT NULL,           -- "Protein", "Side", "Toppings", "Temperature", "Preparation"
    is_required BOOLEAN DEFAULT FALSE,          -- Must customer choose one?
    allow_multiple BOOLEAN DEFAULT FALSE,       -- Can select multiple modifiers?
    display_order INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Examples (Sit-Down Restaurant Context):**
- **Protein**: Required, Single Choice → "Chicken", "Salmon", "Beef", "Tofu"
- **Side Dish**: Required, Single Choice → "Fries", "Mashed Potatoes", "Seasonal Vegetables", "Rice Pilaf"
- **Steak Temperature**: Required, Single Choice → "Rare", "Medium Rare", "Medium", "Medium Well", "Well Done"
- **Additional Toppings**: Optional, Multiple Choice → "Add Cheese", "Add Bacon", "Add Avocado", "Add Sautéed Mushrooms"
- **Preparation Style**: Optional, Single Choice → "Grilled", "Blackened", "Pan-Seared"
- **Sauce Selection**: Optional, Single Choice → "House Sauce", "Garlic Aioli", "Chimichurri", "On the Side"

---

### 7. **MODIFIERS**
Individual modifier options within groups.

```sql
CREATE TABLE modifiers (
    modifier_id INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id INTEGER NOT NULL,
    modifier_name VARCHAR(100) NOT NULL,        -- "Salmon", "Medium Rare", "Add Cheese"
    price_adjustment DECIMAL(10,2) DEFAULT 0.00,-- Can be positive or negative
    is_active BOOLEAN DEFAULT TRUE,
    display_order INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (group_id) REFERENCES modifier_groups(group_id) ON DELETE CASCADE
);

CREATE INDEX idx_modifiers_group ON modifiers(group_id);
```

**Business Rules:**
- `price_adjustment` can be:
  - Positive: +$4.00 for "Upgrade to Salmon"
  - Positive: +$2.00 for "Add Avocado"
  - Zero: No change for "Medium Rare" or "No Onions"
  - Zero: No change for sauce selections
  
**Restaurant-Specific Examples:**
- Protein choices may have different upcharges (Chicken $0, Salmon +$4, Filet +$8)
- Temperature and preparation style typically have no upcharge
- Add-ons like extra protein, cheese, or premium toppings have upcharges
- Substitutions may be free or have small charges

---

### 8. **MENU_ITEM_MODIFIER_GROUPS**
Junction table: Which modifier groups apply to which menu items?

```sql
CREATE TABLE menu_item_modifier_groups (
    item_id INTEGER NOT NULL,
    group_id INTEGER NOT NULL,
    PRIMARY KEY (item_id, group_id),
    FOREIGN KEY (item_id) REFERENCES menu_items(item_id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES modifier_groups(group_id) ON DELETE CASCADE
);
```

**Example - Pan-Seared Salmon:**
- Menu item: "Pan-Seared Salmon" (item_id=12)
- Applies to modifier groups:
  - Temperature group → "Rare", "Medium Rare", "Medium", "Medium Well"
  - Side Dish group → "Fries", "Mashed Potatoes", "Seasonal Vegetables"
  - Sauce group → "Lemon Butter", "Garlic Aioli", "On the Side"
  
**Example - Build Your Own Pasta:**
- Menu item: "Build Your Own Pasta" (item_id=23)
- Applies to modifier groups:
  - Pasta Type → "Penne", "Spaghetti", "Fettuccine"
  - Protein → "Chicken", "Shrimp", "Italian Sausage", "No Protein"
  - Sauce → "Marinara", "Alfredo", "Pesto", "Aglio e Olio"
  - Toppings → "Mushrooms", "Sun-dried Tomatoes", "Fresh Basil"
  
---

### 9. **ORDER_ITEMS**
Line items on an order (dishes/drinks ordered).

```sql
CREATE TABLE order_items (
    order_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    menu_item_id INTEGER NOT NULL,
    
    -- Item details (snapshot at order time)
    item_name VARCHAR(200) NOT NULL,            -- Cached: prevents menu changes breaking history
    base_price DECIMAL(10,2) NOT NULL,
    quantity INTEGER DEFAULT 1,
    
    -- Timing
    item_ordered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    item_fired_at TIMESTAMP,                    -- When sent to kitchen
    item_completed_at TIMESTAMP,                -- When kitchen marks ready
    
    -- Status
    item_status VARCHAR(20) DEFAULT 'pending',  -- 'pending', 'fired', 'preparing', 'ready', 'delivered'
    
    -- Pricing
    modifiers_total DECIMAL(10,2) DEFAULT 0.00, -- Sum of modifier price adjustments
    item_total DECIMAL(10,2) NOT NULL,          -- (base_price + modifiers_total) * quantity
    
    -- Special instructions
    special_instructions TEXT,                  -- "No onions", "Extra crispy", etc.
    
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(item_id) ON DELETE RESTRICT
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_status ON order_items(item_status);
CREATE INDEX idx_order_items_fired ON order_items(item_fired_at);
```

**Business Rules:**
- `item_name` and `base_price` are CACHED from menu_items table
  - This preserves historical accuracy if menu prices change
- `item_status` tracks kitchen progress
- `special_instructions` is free-form text for server notes

---

### 10. **ORDER_ITEM_MODIFIERS**
Which modifiers were applied to each order item?

```sql
CREATE TABLE order_item_modifiers (
    order_item_id INTEGER NOT NULL,
    modifier_id INTEGER NOT NULL,
    modifier_name VARCHAR(100) NOT NULL,        -- Cached for history
    price_adjustment DECIMAL(10,2) NOT NULL,    -- Cached for history
    PRIMARY KEY (order_item_id, modifier_id),
    FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id) ON DELETE CASCADE,
    FOREIGN KEY (modifier_id) REFERENCES modifiers(modifier_id) ON DELETE RESTRICT
);
```

**Example - Order Item: Pan-Seared Salmon**
- Order item #73 (Pan-Seared Salmon, $24.00):
  - Modifier: "Medium Rare" ($0.00)
  - Modifier: "Mashed Potatoes" ($0.00)
  - Modifier: "Add Extra Vegetables" (+$3.00)
  - Modifier: "Lemon Butter Sauce" ($0.00)
  - **Total modifiers:** +$3.00
  - **Item total:** $27.00

---

### 11. **PAYMENTS**
Basic payment tracking for orders. (MVP: Just track that payment happened, not integrated payment processing)

```sql
CREATE TABLE payments (
    payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    payment_method VARCHAR(50) NOT NULL,        -- 'cash', 'credit_card', 'debit_card', 'gift_card'
    amount DECIMAL(10,2) NOT NULL,
    tip_amount DECIMAL(10,2) DEFAULT 0.00,
    payment_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE RESTRICT
);

CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_timestamp ON payments(payment_timestamp);
```

**Business Rules (MVP):**
- Simple tracking of payment method and amount
- No actual payment processing integration required
- One order can have multiple payments (split checks)
- Sum of all payments for an order should equal `orders.total` (validation in business logic, not enforced)

**Future Enhancement:**
- Integration with payment processors (Stripe, Square, etc.)
- Card tokenization for security
- Receipt generation and email delivery

---

### 12. **TABLE_STATUS_LOG** (Optional but useful)
Audit trail of table status changes.

```sql
CREATE TABLE table_status_log (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    table_id INTEGER NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    changed_by VARCHAR(100),                    -- Server name
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (table_id) REFERENCES tables(table_id) ON DELETE CASCADE
);

CREATE INDEX idx_table_log_table ON table_status_log(table_id);
CREATE INDEX idx_table_log_timestamp ON table_status_log(changed_at);
```

**Business Rules:**
- Tracks: available → occupied → needs_cleaning → available
- Useful for analytics: table turnover time, cleaning delays

---

## Database Indexes Strategy

**Performance-Critical Indexes:**
- Orders by table, status, server
- Order items by order, status, fire time
- Menu items by category, station, active status
- Payments by order, timestamp

**Why These Indexes:**
- Servers need fast lookups: "Show me Table 7's order"
- Kitchen needs: "Show me all items that need prep"
- Management needs: "Show me today's sales"

---

## Data Integrity Rules

1. **Cascade Deletes:** Only on junction tables and non-critical logs
2. **Restrict Deletes:** On all core entities (orders, items, menu items)
3. **Soft Deletes:** Use `is_active` flags instead of DELETE for menu items
4. **Historical Preservation:** Cache names/prices in order tables
5. **Referential Integrity:** All foreign keys enforced at database level

