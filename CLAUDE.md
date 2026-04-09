# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Build and install on connected device
./gradlew build                  # Full build (debug + release)
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)
./gradlew lint                   # Run lint checks
```

## Project Overview

This is an Android **Kitchen Display System (KDS)** for a restaurant canteen at Plaza Unipdu. It shows real-time incoming orders to kitchen staff, tracks item preparation progress, and records serving history.

**Tech stack:** Java, Firebase Firestore (primary DB), RecyclerView, Material Design, Flexbox, Gson for SharedPreferences persistence.

## Architecture

### Activities
- **MainActivity** — Primary screen. Listens to Firestore `Status` collection in real time, displays pending orders as swipeable cards, and shows an aggregated kitchen summary panel on the left (collapsible, ~25% width).
- **RecentlyServedActivity** — Read-only history view querying the `RecentlyServed` collection (50 most recent).

### Data Flow
1. Firestore snapshot listener (filtered by `canteenId = "canteen375_plazaUnipdu"`) pushes `Status` documents into `MainActivity`.
2. Documents are parsed into `OrderBlock` objects and stored in `SharedPreferences` (Gson) for offline persistence.
3. `RecyclerAdapter2` renders each `OrderBlock` as a card with per-order count-up timers.
4. `rebuildAggregation()` computes `AggregatedItem` list from all current orders, rendered in `AggregationAdapter` (left panel).
5. Swiping a card deletes it from `Status` and writes it to `RecentlyServed`.

### Key Data Models
- **`OrderBlock`** — Root order entity. Contains `customerNumber`, `namaCustomer`, `orderItems`, `orderTimestamp`, `bungkus` (take-away flag), `waktuPesan`/`waktuPengambilan`.
- **`NewOrderItem`** — Single menu line item with `namaPesanan`, `orderType` ("dine-in"/"take-away"), `quantity`, `preparedQuantity`, and `selectedOptions`.
- **`SelectedOption`** — Customization/add-on (optionId, optionName, groupId, groupName, priceAdjustment).
- **`AggregatedItem`** — Aggregates identical items across all orders (same name + type + options). Holds `ItemReference` list pointing back to source `NewOrderItem` instances. Call `recalculateTotals()` after mutating references.

### Adapters
- **`RecyclerAdapter2`** — Handles both the main pending list and the recently-served view. Manages per-card `Handler`/`Runnable` count-up timers in a `SparseArray`; timers must be stopped in `onViewRecycled` and `stopAllTimers()` to prevent leaks.
- **`AggregationAdapter`** — Left-panel summary. Clicking an item increments `preparedQuantity` on the first unfinished `NewOrderItem` in that aggregation, then triggers a full aggregation rebuild.

### Visual Conventions
- **Yellow background** = take-away order/item
- **Blue background** = dine-in order/item
- Elapsed time shown in MM:SS in red on each card

### Firestore Collections
| Collection | Purpose | Key fields |
|---|---|---|
| `Status` | Active pending orders | `canteenId`, `waktuPesan` (sort), `orderTimestamp` |
| `RecentlyServed` | Completed order history | `canteenId`, `timestampServe` (sort, desc, limit 50) |

### Hardcoded Values
- Canteen ID: `"canteen375_plazaUnipdu"` (in `MainActivity`)
- Timezone: `Asia/Jakarta` for timestamp display

## Current Rules
rules_version = '2';

service cloud.firestore {
match /databases/{database}/documents {

    // ── HELPER FUNCTIONS ──────────────────────────────────────────────────────
    
    function isAuthenticated() {
      return request.auth != null;
    }

    function isAdmin() {
      // UPDATED: Now recognizes your specific email as an Admin
      return isAuthenticated() && (
        request.auth.token.admin == true || 
        request.auth.token.email == "gnavsih1@gmail.com" || 
        request.auth.token.email == "admin@canteen375.com"
      );
    }

    // ── NEW COLLECTIONS FOR POS APP ───────────────────────────────────────────
    
    match /Categories/{id} {
      allow read: if isAuthenticated();
      allow write: if isAdmin();
    }

    match /assets/{id} {
      allow read: if isAuthenticated();
      allow write: if isAdmin();
    }

    match /config/{id} {
      allow read: if isAuthenticated();
      allow write: if isAdmin();
    }

    match /DailyTransaction/{id} { allow read, write: if isAuthenticated(); }
    match /MonthlyTransaction/{id} { allow read, write: if isAuthenticated(); }
    match /YearlyTransaction/{id} { allow read, write: if isAuthenticated(); }
    match /Status/{id} { allow read, write, delete, update: if true; }

    // ── MEMBERS COLLECTION ─────────────────────────────────────────────────────
    match /Members/{uid} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated() 
                    && request.auth.uid == uid 
                    && request.resource.data.uid == uid;
      allow update: if isAdmin() || (isAuthenticated() && request.auth.uid == uid);
      allow delete: if isAdmin();
    }

    // ── VOUCHERS COLLECTIONS ──────────────────────────────────────────────────
    match /voucher/{id} {
      allow read: if isAdmin() || (isAuthenticated() && resource.data.userId == request.auth.uid);
      allow write: if isAdmin();
    }
    match /vouchers/{id} {
      allow read: if isAdmin() || (isAuthenticated() && resource.data.userId == request.auth.uid);
      allow write: if isAdmin();
    }

    // ── VOUCHER GROUP (CAMPAIGNS) ─────────────────────────────────────────────
    match /voucherGroup/{groupId} {
      allow read: if isAuthenticated();
      allow write: if isAdmin();
    }

    // ── COMPETITION RECORDS (LEADERBOARD DATA) ────────────────────────────────
    match /competitionRecords/{monthId} {
      allow read: if isAuthenticated();
      allow write: if isAdmin();
    }

    // ── FEEDBACKS ─────────────────────────────────────────────────────────────
    match /feedbacks/{feedbackId} {
      allow create: if isAuthenticated() && request.resource.data.memberId == request.auth.uid;
      allow read: if isAdmin() || (isAuthenticated() && resource.data.memberId == request.auth.uid);
      allow update, delete: if isAdmin();
    }

    // ── CANTEENS (BRANCH DATA) ────────────────────────────────────────────────
    match /Canteens/{canteenId} {
      
      allow read: if isAuthenticated();
      allow update: if isAuthenticated();
      allow create, delete: if isAdmin();
      
      match /Inventory/{id} {
        allow read: if isAuthenticated();
        allow write: if isAdmin();
      }
      
      match /DailyStockLogs/{id} {
        allow read: if isAuthenticated();
        allow write: if isAdmin();
      }
      
      match /MenuCollection/{menuId} {
        allow read: if isAuthenticated();
        allow write: if isAdmin();
      }
      
      match /Metadata/{configId} {
        allow read: if isAuthenticated();
        allow write: if isAdmin();
      }
      
      match /Metadata/Settings {
      	allow read: if isAuthenticated();
      }
      
      match /OptionGroups/{groupId} {
        allow read: if isAuthenticated();
        allow write: if isAdmin();
      }
      
      match /suppliers/{supplierId} {
        allow read: if isAuthenticated();
        allow write: if isAdmin();
      }
      
      match /shoppingOrders/{orderId} {
        allow read: if isAuthenticated();
        allow write: if isAdmin();
      }
      
      // STATUS & RECENTLY SERVED: Order queue and completed orders
      match /Status/{orderId} {
        allow read, write, update, delete: if true;
      }
      
      match /RecentlyServed/{orderId} {
        allow read, write, update, delete: if true;
      }
      
      match /SelfOrders/{orderId} {
        allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
        allow read: if isAdmin() || (isAuthenticated() && resource.data.userId == request.auth.uid);
        allow update, delete: if isAdmin();
      }
      
      match /OpenBills/{memberId} {
        // Broad access for all authenticated POS users to read and write to standard bills
        allow read, write, update, delete: if isAuthenticated();
        
        // Explicitly allow writing to the nested subcollection where individual tab entries live
        match /Orders/{tabOrderId} {
          allow read, write, update, delete: if isAuthenticated();
        }
      }
      
      match /SettledBills/{billId} {
      	allow read, write, update, delete: if isAuthenticated();
      }
      
      match /Orders/{orderID} {
      	allow read, write, update, delete: if isAuthenticated();
      }
      
    }

    // ── PUBLIC PRODUCTS (OLD VERSION) ─────────────────────────────────────────
    match /products/{productId} {
      allow read: if true;
      allow write: if isAdmin();
    }
    match /products_test/{productId} {
      allow read: if true;
      allow write: if isAdmin();
    }
}
}
