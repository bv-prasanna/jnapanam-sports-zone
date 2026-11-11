# Jnapanam — Core logical ER diagrams and key constraints

> Derived from V1, V3–V9, V20, V47 and V49 in apps/backend/src/main/resources/db/migration. **Logical core overview, not the deployed physical schema.** Later ALTER/DROP changes through V61 must be reconciled before signing off a physical ER.

## Identity, catalog, booking and settlement

```mermaid
erDiagram
  USERS {
    UUID id PK
    VARCHAR email UK
    VARCHAR status
  }
  ROLES {
    BIGINT id PK
    VARCHAR name UK
  }
  USER_ROLES {
    UUID user_id PK,FK
    BIGINT role_id PK,FK
  }
  REFRESH_TOKENS {
    UUID id PK
    UUID user_id FK
    CHAR token_hash UK
  }
  SPORTS {
    UUID id PK
    VARCHAR code UK
  }
  FACILITIES {
    UUID id PK
    UUID sport_id FK
    INTEGER capacity
  }
  BOOKING_SLOTS {
    UUID id PK
    UUID facility_id FK
    DATE slot_date
    INTEGER held_count
    INTEGER booked_count
  }
  BOOKING_HOLDS {
    UUID id PK
    UUID user_id FK
    UUID slot_id FK
    TIMESTAMPTZ expires_at
  }
  BOOKINGS {
    UUID id PK
    UUID user_id FK
    UUID slot_id FK
    VARCHAR booking_number UK
  }
  PAYMENT_ORDERS {
    UUID id PK
    UUID user_id FK
    UUID reference_id
    BIGINT amount_paise
    VARCHAR status
  }
  PAYMENT_REFUNDS {
    UUID id PK
    UUID payment_order_id FK
    BIGINT amount_paise
    BIGINT deduction_paise
  }
  PAYMENT_WEBHOOK_EVENTS {
    UUID id PK
    VARCHAR provider_event_id UK
    VARCHAR payload_hash
  }
  VENUE_BILLING_DOCUMENTS {
    UUID id PK
    UUID payment_order_id FK
    UUID refund_id FK
    VARCHAR kind
  }
  USERS ||--o{ USER_ROLES : member
  ROLES ||--o{ USER_ROLES : assigns
  USERS ||--o{ REFRESH_TOKENS : sessions
  SPORTS ||--o{ FACILITIES : offers
  FACILITIES ||--o{ BOOKING_SLOTS : schedule
  USERS ||--o{ BOOKING_HOLDS : holds
  BOOKING_SLOTS ||--o{ BOOKING_HOLDS : reserves
  USERS ||--o{ BOOKINGS : books
  BOOKING_SLOTS ||--o{ BOOKINGS : used_by
  USERS ||--o{ PAYMENT_ORDERS : pays
  PAYMENT_ORDERS ||--o{ PAYMENT_REFUNDS : refunds
  PAYMENT_ORDERS ||--o{ VENUE_BILLING_DOCUMENTS : invoices
  PAYMENT_REFUNDS o|--o{ VENUE_BILLING_DOCUMENTS : credits
```

**Important:** payment_orders.reference_id is a polymorphic purpose/reference pointer, *not* an enforced FK to bookings. payment_webhook_events/provider_event_id is unique for replay defenses. booking_slots has a facility/date/start unique constraint and a CHECK ensuring held_count + booked_count <= capacity. payment_refunds gains deduction_paise in V60 and deduction_description in V61.

## Memberships, training, tournaments, commerce

```mermaid
erDiagram
  USERS {
    UUID id PK
  }
  SPORTS {
    UUID id PK
  }
  FACILITIES {
    UUID id PK
  }
  MEMBERSHIP_PLANS {
    UUID id PK
    VARCHAR code UK
  }
  MEMBERSHIP_PLAN_SPORTS {
    UUID plan_id PK,FK
    UUID sport_id PK,FK
  }
  CUSTOMER_MEMBERSHIPS {
    UUID id PK
    UUID user_id FK
    UUID plan_id FK
  }
  TRAINING_PROGRAMS {
    UUID id PK
    UUID sport_id FK
  }
  TRAINING_BATCHES {
    UUID id PK
    UUID program_id FK
    UUID facility_id FK
    UUID coach_user_id FK
  }
  TRAINING_ENROLLMENTS {
    UUID id PK
    UUID batch_id FK
    UUID user_id FK
  }
  TOURNAMENTS {
    UUID id PK
    UUID sport_id FK
    UUID organizer_user_id FK
  }
  TOURNAMENT_TEAMS {
    UUID id PK
    UUID tournament_id FK
    UUID captain_user_id FK
  }
  COMMERCE_CATEGORIES {
    UUID id PK
  }
  COMMERCE_PRODUCTS {
    UUID id PK
    UUID category_id FK
  }
  COMMERCE_VARIANTS {
    UUID id PK
    UUID product_id FK
  }
  COMMERCE_ORDERS {
    UUID id PK
    UUID user_id FK
  }
  COMMERCE_ORDER_ITEMS {
    UUID id PK
    UUID order_id FK
  }
  MEMBERSHIP_PLANS ||--o{ MEMBERSHIP_PLAN_SPORTS : includes
  SPORTS ||--o{ MEMBERSHIP_PLAN_SPORTS : available_for
  USERS ||--o{ CUSTOMER_MEMBERSHIPS : holds
  MEMBERSHIP_PLANS ||--o{ CUSTOMER_MEMBERSHIPS : subscribes
  SPORTS ||--o{ TRAINING_PROGRAMS : supports
  TRAINING_PROGRAMS ||--o{ TRAINING_BATCHES : schedules
  FACILITIES ||--o{ TRAINING_BATCHES : hosts
  USERS ||--o{ TRAINING_ENROLLMENTS : enrolls
  TRAINING_BATCHES ||--o{ TRAINING_ENROLLMENTS : contains
  SPORTS ||--o{ TOURNAMENTS : type
  TOURNAMENTS ||--o{ TOURNAMENT_TEAMS : teams
  COMMERCE_CATEGORIES ||--o{ COMMERCE_PRODUCTS : groups
  COMMERCE_PRODUCTS ||--o{ COMMERCE_VARIANTS : options
  USERS ||--o{ COMMERCE_ORDERS : orders
  COMMERCE_ORDERS ||--o{ COMMERCE_ORDER_ITEMS : contains
```

The commerce_order_items table carries product/variant snapshot IDs and labels; V20 does **not** declare foreign keys on those snapshot product/variant columns. Do not invent physical FK edges in ER exports.

## Physical schema sign-off

The full migration-wise column inventory is in SCHEMA_INDEX.md and SCHEMA_MIGRATIONS_01.md through SCHEMA_MIGRATIONS_05.md. Reconcile against the running database with DB_SCHEMA_AND_MIGRATION.md and db-introspection.sql before calling the result a production ER.
