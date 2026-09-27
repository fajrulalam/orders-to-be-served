# Kitchen Display ↔ 375POS: Agent Guide

This file is for AI agents making code changes in this repository (the Kitchen
Display System, "KDS"). The KDS has no data of its own: every order it shows is
created by the **375POS** cashier app, and much of what it writes is read back
by the POS. A change that looks local to one app can stop orders from reaching
the kitchen, block checkout, or lose an unpaid bill. Read this before touching
anything that reads or writes Firestore.

Verified against the code on 2026-09-27: KDS commit `986b51d`, POS commit
`795790d` (the POS had uncommitted edits in `OrderConfirmationService.dart` at
the time). If code and this file disagree, trust the code, then update this file.

---

## 1. The two apps

| | 375POS (cashier) | KDS (this repo) |
|---|---|---|
| Path | `/Users/ghinannavsih/Documents/375POS` | `/Users/ghinannavsih/Documents/orders-to-be-served` |
| Stack | Flutter/Dart, package `point_of_sales_app_v3` | Android, Java 8, minSdk 23 |
| Role | Takes orders and payment, owns money, stock, members, vouchers | Shows pending orders to the kitchen, tracks preparation, removes served orders |
| Firebase | Project `point-of-sales-app-25e2b` | Same project (`app/google-services.json`) |
| Security rules | **Authoritative copy**: `375POS/firestore.rules`, deployed from the POS repo | Only stale snapshots: `Current_Firestore_Rules.md`, `firestore_rules_update.txt`, and the copy in `CLAUDE.md` |

There is no API between the apps. They share **one Firestore document per
order** in the root `Status` collection, plus the root `RecentlyServed` history
collection.

---

## 2. Rules that must not be broken

1. **The POS owns the order. The KDS owns only preparation progress and
   removal.** The only order fields the KDS may change are
   `orderItems[].dineInPreparedQuantity` and
   `orderItems[].takeAwayPreparedQuantity`, plus deleting the document when it
   is served. Never write `total`, `orderRevision`, `isClosed`, `status`,
   `transactionMethod`, or payment fields to `Status`. POS open-bill settlement
   aborts if `total` or `orderRevision` changed under it.
2. **An unsettled open bill must never leave `Status`.** POS settlement
   (`_processOpenBillSettlement`) throws "Open bill tidak ditemukan" if the
   document is gone, and the bill can no longer be paid. The swipe guard in
   `MainActivity.simpleCallback` (`isOpenBill() && !isClosed()`) exists for
   this.
3. **Anything the KDS writes back must carry every field the POS put there.**
   `StatusOrderItemsBuilder` rebuilds `orderItems` from a fixed list of fields.
   A per-item field the POS adds but the builder does not know about is erased
   by the kitchen's first tap.
4. **Change the contract in both repos in the same task, or not at all.** If
   the POS side is out of scope, stop and tell the user what the POS would need.
5. **Keep Firestore types exact.** Times are `Timestamp`. Money and quantities
   are integers. The KDS parses numbers with `Integer.parseInt(String.valueOf(x))`,
   so a double such as `15000.0` throws. If `total` fails to parse, the order is
   silently dropped from the kitchen screen.
6. **Canteen IDs, document-ID formats and collection names are literals
   duplicated in both apps.** Change them everywhere or nowhere (section 6).
7. **Testing mode is a per-device switch.** It is not synced between the POS and
   the kitchen tablet (section 6.3).

---

## 3. Order lifecycle

```
             POS (cashier)                          Firestore                          KDS (kitchen)
 checkout ─── transaction ──────────▶ Status/{n}_plazaUnipdu ──── snapshot listener ───▶ card appears (+ sound)
 open-bill add ─ transaction append ─▶      (same doc)       ◀─── update orderItems ───── tap / long-press item
 edit order ─── transaction update ──▶      (same doc)
 settle bill ── transaction update ──▶      (same doc)
                                            (deleted)        ◀─── delete ──────────────── swipe card (served)
                                      RecentlyServed/{auto}  ◀─── add ─────────────────── same swipe
                                      Status/{sourceId}      ◀─── batch set + delete ──── swipe left in history (restore)
```

### 3.1 Regular order (Normal or Self Orders)

1. The cashier confirms. `_runIdempotentSaleTransaction` in
   `375POS/lib/Services/OrderConfirmationService.dart` reads
   `Canteens/canteen375/Metadata/customerNumber`, sets `n = counter + 1`, and
   **refuses to continue if `Status/{n}_plazaUnipdu` already exists**. It then
   writes the `Status` document (section 4) and increments the counter.
2. The KDS listener (`MainActivity.onCreate`) receives `ADDED`, plays the
   notification sound (not on first load), parses the document into an
   `OrderBlock`, and shows a card.
3. A kitchen tap increments one unit of prepared quantity. A long-press resets
   the line to 0. Each change rewrites the document's whole `orderItems` array
   (section 5.1).
4. A kitchen swipe (left or right) deletes the `Status` document and adds a
   `RecentlyServed` document (section 5.2). The swipe applies to **every**
   kitchen tablet: the food/drink filter only affects the confirmation prompt,
   not what gets removed.

### 3.2 Open bill (member tab)

1. First order: the POS creates `Status/{n}_plazaUnipdu` with
   `transactionMethod: "Open Bill"`, `isClosed: false`, `paymentMethod: null`,
   `takeAwayFee: 0`, and `total` = food subtotal.
2. Later orders for the same member **append** to `orderItems` in the same
   document (`_processOpenBillOrder`, using the lock doc to find it), with
   `total` and `subTotal` incremented and `orderRevision + 1`. Each round's
   items share a new `orderedAt`. The KDS groups them under
   "🕒 Order #k (HH:mm)" headers. The append is a `MODIFIED` change, so **the
   KDS plays no sound for it**.
3. While `isClosed == false`, the KDS refuses the swipe and shows a snackbar
   telling staff to settle at the cashier.
4. Settlement (`_processOpenBillSettlement`) sets `isClosed: true`,
   `status: "Settled"`, `settledAt`, `wasOpenBill: true`, `finalTotal`, and
   **replaces `transactionMethod` with the payment method** (`"Cash"`,
   `"QRIS"`, `"Program"`, …). From then on `OrderBlock.isOpenBill()` is false:
   the badge, round headers and swipe block disappear, and the card behaves like
   a normal order.

### 3.3 Edit order (POS)

`EditOrderService._processEditOrderTransactional` rewrites `orderItems`,
`total`, `subTotal`, `takeAwayFee`, `lastEditedAt`, and `orderRevision + 1`. It
keeps kitchen progress by matching old items on the POS `orderKey`
(`menuItemId` or name, plus sorted `groupName:optionName`). Items that no longer
match start again at 0 prepared. The POS "Edit Order" screen only lists
documents still in `Status`, so **orders the kitchen has already swiped can no
longer be edited**.

---

## 4. The `Status` document contract

Document ID: `{customerNumber}_plazaUnipdu`, written only by the POS. The KDS
stores the real ID in `OrderBlock.firestoreDocumentId` and must use that, never
rebuild it. The fallback to a bare `customerNumber` in `performSwipeServe` is
legacy.

KDS query (`MainActivity`):
`collection("Status").whereEqualTo("canteenId", "canteen375_plazaUnipdu").orderBy("waktuPesan", ASC)`.
This needs a composite index (canteenId + waktuPesan), managed in the Firebase
console; neither repo has a `firestore.indexes.json`. If the query changes and
the index is missing, the listener errors (only logged) and the screen stops
updating.

### 4.1 Top-level fields

| Field | Type | Written by | KDS use / failure mode |
|---|---|---|---|
| `canteenId` | string | POS: literal `"canteen375_plazaUnipdu"`; self orders copy `selfOrder.canteenId` | Query filter. Any other value means the order **never reaches the kitchen**. |
| `waktuPesan` | Timestamp (server) | POS on create | Sort order, `HH:mm` label (Asia/Jakarta), count-up timer. A document without it is excluded by `orderBy` and never shown. |
| `customerNumber` | int | POS | Card number **and** the KDS's local identity key for merging snapshots. Null or empty: order skipped. |
| `total` | int | POS (all paths) | Carried to history. **Required**: missing or non-integer throws, and the order is silently skipped. |
| `namaCustomer` | string | POS | Card header, read aloud by TTS (id-ID) when tapped. |
| `isMember` | bool | POS | Member badge styling. |
| `memberId`, `customerPhone` | string | POS (optional) | Carried to history/restore only. |
| `transactionMethod` | string | POS: `"Normal"`, `"Self Orders"`, `"Open Bill"`; after settlement the payment method | KDS only checks `equalsIgnoreCase("Open Bill")`. |
| `isClosed` | bool | POS, open-bill flows only | Swipe guard. KDS defaults to `true` when absent. |
| `paymentMethod` | string/null | POS | Carried to history. See gap 8.5. |
| `takeAwayFee` | int | POS | Carried to history/restore. |
| `waktuPengambilan` | string | POS: `"Tidak Memesan"` or a self-order pickup time | Parsed and carried, **not displayed** on the card. |
| `orderHistory` | list | Nothing current (legacy open bills) | Carried if present. |
| `bungkus` | int | Nothing current (legacy) | Defaults to 0. `RecentlyServedActivity` skips `bungkus == 2` (legacy "order later"). |

POS-only fields. The KDS ignores these. They survive in `Status` (the KDS
updates only `orderItems`) but are **dropped on swipe and missing after
restore**: `status` (`"Serving"`/`"Settled"`), `subTotal`, `discountAmount`,
`voucherCode`, `orderRevision`, `inventoryAuditFlags`, `lastEditedAt`,
`isSplitPayment`, `splitDetails`, `selfOrderId`, `selfOrderShortCode`,
`settledAt`, `wasOpenBill`, `finalTotal`; member-program fields
(`memberProgramOperationId`, `pointsOperationId`, `competitionPeriodId`,
`grossTotal`, `finalBillForPoints`, `ordinaryVoucherDiscount`,
`b2bSponsoredNominal`, `pointsAwarded`, `eligibleAmountForPoints`,
`memberProgramStatus`, `memberProgramAuditFlags`, `memberProgramSchemaVersion`);
and B2B voucher-program fields (`voucherProgramId`, `voucherProgram`,
`voucherProgramOperationId`, `programNominal`, `programExtraPaymentMethod`,
`programExtraSplitDetails`, …).

### 4.2 `orderItems[]` element

Built by `_buildSaleOrderItems` (checkout, self order, open bill) and by the
edit transaction in `EditOrderService`. One POS basket line becomes one element.

| Field | Type | Notes |
|---|---|---|
| `namaPesanan` | string | Display name. Also used by aggregation, and as the fallback key for menu-group matching. |
| `menuItemId` | string | `MenuCollection` doc ID. Primary key for menu-group matching; otherwise round-tripped unchanged. |
| `harga` | int | Unit price. Round-tripped. |
| `dineInQuantity` / `takeAwayQuantity` | int | One element with both is shown by the KDS as **two** lines: blue dine-in and yellow take-away. |
| `dineInPreparedQuantity` / `takeAwayPreparedQuantity` | int | **Owned by the KDS.** The POS writes 0 on create and preserves them on edit. |
| `selectedOptions` | list of `{optionId, optionName, groupId, groupName, priceAdjustment}` | Chips on the card. Option IDs are part of the grouping keys. |
| `isMakanan` | bool | Food (true) or drink. The POS writes `menu?.isMakanan ?? false`, so an unknown menu counts as a drink. The KDS defaults to `true` only when the key is absent. |
| `customerNote` | string | Orange note box. Part of the grouping keys. |
| `status` | string | Always `""`. Round-tripped. |
| `orderedAt` | int, epoch ms (POS device clock) | Sort order and open-bill round grouping. Part of the write-back key. |

---

## 5. What the KDS writes, and what the POS loses

### 5.1 Preparation progress: `MainActivity.updatePreparedQuantitiesInFirestore`

- `update("orderItems", StatusOrderItemsBuilder.toFirestoreArrayList(items))`
  replaces the **whole array** from the tablet's local copy. It is not a
  transaction.
- The builder merges the KDS's split lines back into one element per key:
  `namaPesanan + "_" + orderedAt + sorted optionIds + "\u0001" + customerNote`.
  If two POS elements share a key, the second **overwrites** the first's
  quantity; they are not summed. Today the POS basket merges identical lines,
  so collisions should not occur. Keep it that way.
- The builder writes exactly the fields in 4.2. New per-item fields must be
  added to `NewOrderItem`, both parsers (`MainActivity` and
  `RecentlyServedActivity.applyItemMetaFromFirestoreMap`), and the builder.
- Race: the local copy is refreshed on every snapshot. If the POS appends an
  open-bill round or edits the order after the tablet's last snapshot, the next
  tap writes the stale array and **reverts the POS change**. Firestore's offline
  queue widens this window when the tablet loses connectivity.

### 5.2 Swipe to serve: `MainActivity.performSwipeServe`

- Blocked for unsettled open bills. If any line *visible under the current
  filter* is unfinished, a confirmation dialog appears first.
- `Status/{id}.delete()` and `RecentlyServed.add(...)` are two separate writes,
  not a batch.
- The `RecentlyServed` payload is a whitelist: `canteenId`, `bungkus`,
  `customerNumber`, `namaCustomer`, `customerPhone`, `isMember`, `memberId`,
  `takeAwayFee`, `orderHistory`, `transactionMethod`, `paymentMethod`,
  `orderItems` (Status shape), `waktuPengambilan`, `total`, `status: "Served"`,
  `timestampServe` (server timestamp), `sourceStatusDocumentId`, and
  `waktuPesan` written as a **string**
  `"Timestamp(seconds=N, nanoseconds=317000000)"`, not a Timestamp.
- Effects in the POS once a document leaves `Status`: it is gone from Edit
  Order, from the settled-bills stream (`Status where isClosed == true`), and
  from the Inventory, VoucherProgram and MemberProgram audits, which scan only
  `Status`. The POS printer dialog's "completed" tab reads `RecentlyServed` by
  `timestampServe`. POS reprint from `RecentlyServed` cannot parse the string
  `waktuPesan` and prints the current time instead.

### 5.3 Restore: `RecentlyServedActivity.restoreOrderToStatus`

- Swipe left on a history card. This requires `sourceStatusDocumentId`, and it
  refuses if that `Status` ID is already in use (the queue number was reused).
- A batch writes `Status/{sourceId}` from `buildStatusPayloadForRestore`
  (`waktuPesan` converted back to a Timestamp, `canteenId` defaulting to
  `canteen375_plazaUnipdu`) and deletes the history row.
- The restored document lacks every POS-only field in 4.1. The POS treats a
  missing `orderRevision` as 0. MemberProgram audit will flag member orders
  (`member_order_operation_marker_missing`). B2B, voucher and split-payment
  details are gone. Treat a restored order as display-only; editing it in the
  POS is unsafe.

### 5.4 Menu groups (KDS-owned configuration)

The custom filter applies a named **menu group** (e.g. "Telur", "Gorengan")
defined in the kitchen and shared by every tablet. `MenuGroupManager` keeps a
live listener on the groups and runs the picker and editor dialogs. `MenuGroup`
holds the matching logic.

- Groups live at `Canteens/canteen375/KdsMenuGroups/{autoId}` with
  `name` (string, 1–60 chars, unique case-insensitively, checked in the app
  only), `menuItemIds` (MenuCollection doc IDs), `menuNames` (`namaMenu` at
  save time), `createdAt` and `updatedAt`. Only the KDS reads or writes them.
- The editor lists `Canteens/canteen375/MenuCollection`. New POS menus have
  **auto-generated document IDs**, so the display name must come from the
  `namaMenu` field, never `doc.getId()`. Using the ID caused the bug where
  menus showed as IDs and never matched an order.
- An order line matches a group if its `menuItemId` is in `menuItemIds`
  (survives renames) **or** its trimmed `namaPesanan` is in `menuNames`,
  ignoring case (orders without `menuItemId`, menus re-created with a new ID,
  and case variants such as "Penyetan lele"/"Penyetan Lele", which both exist
  in MenuCollection). Testing-mode orders often reference menu IDs that exist
  only in `zTesting_Canteens`, so they depend on this name fallback. The editor
  ticks entries by exact ID or exact name only, so it shows what was saved.
- If another tablet edits the active group, the filter updates live. If the
  group is deleted, the filter falls back to "all".
- Both paths are always the production `Canteens` root, even in testing mode.

### 5.5 Ingredient mode (back kitchen)

The "BAHAN" toggle in the top bar switches a tablet to showing orders as
ingredients. The choice is saved per tablet (`ingredient_mode_enabled` in
`shared_prefs`). Code: `IngredientCatalog` (recipes), `IngredientBoard` (rows
and summary, no Android code), `IngredientProgressStore` (shared ticks),
`IngredientSummaryAdapter`, and the ingredient branch of `RecyclerAdapter2`
(`bindIngredientRows`).

- **Recipes come from the POS**, read live from `{Canteens}/canteen375`:
  `MenuCollection[].ingredients`, `OptionGroups[].options[].ingredients`
  (`{inventoryItemId, inventoryItemName, quantityNeeded}`), and `Inventory`
  (current name and `unit`). `IngredientCatalog.ingredientsFor` copies the POS
  stock deduction (`InventoryService.calculateOrderStockDeltas` and
  `StockRequirementCalculator.aggregateIngredients`). A set `menuItemId` is
  authoritative, with no name fallback. Otherwise the menu is found by its unique
  trimmed name. Options are found by ID, else by unique name. Ingredients are
  merged by `inventoryItemId` and quantities summed; `quantityNeeded <= 0` is
  dropped. **If the POS changes that logic, change `IngredientCatalog` too**, or
  the kitchen will prep something different from what the POS subtracted from
  stock.
- Unlike menu groups, the recipe root **follows testing mode**
  (`TestingModeManager.col(prefs, "Canteens")`), because test orders reference
  menu IDs that exist only in `zTesting_Canteens`.
- Lines whose menu has no ingredients are not shown, and neither are orders
  without any ingredient row. On 2026-09-27 only 10 of 87 production menus had
  ingredients (packaged goods, quantity 1). Adding ingredients in the POS also
  turns on POS stock deduction and blocks sales when that stock runs out.
- One row per ingredient per menu line (dine-in and take-away separately),
  labelled "untuk {menu} · {options}", with the customer note. A tap marks one
  more portion ready and a long-press resets. The summary panel ("Bahan") sums
  what is left per ingredient across shown orders; tapping it ticks one portion
  on the oldest open order. Filters apply as in menu mode.
- **Progress never touches `Status`.** It lives at
  `{Canteens}/canteen375/KdsIngredientProgress/{statusDocId}` as
  `{progress: {progressKey: portions}, updatedAt}`. `progressKey` includes
  `orderedAt`, so open-bill rounds are tracked separately and a reused Status
  doc ID never inherits old ticks. Writes are absolute values (they work
  offline; last writer wins). Ready portions shown =
  `min(portions, max(ticked, menu preparedQuantity))`: a portion the cooks
  marked prepared counts as having its ingredients, but ticking an ingredient
  never changes the menu line.
- **Swiping is off in ingredient mode** (`getSwipeDirs` returns 0), because a
  back-kitchen swipe would serve the order on every tablet. `performSwipeServe`
  (menu mode) deletes the order's progress document. Documents for orders that
  are no longer in Status are pruned after 10 idle minutes, using only a server
  (not cached) Status snapshot.

---

## 6. Shared constants and environment

### 6.1 Literals duplicated across the apps

| Value | POS | KDS |
|---|---|---|
| `"canteen375_plazaUnipdu"` | Hardcoded in `OrderConfirmationService` (every create path); `SelfOrderService` filters self orders by it | `MainActivity.CANTEEN_ID`, `RecentlyServedActivity.DEFAULT_CANTEEN_ID` |
| `"canteen375"` (canteen doc) | `Canteens/canteen375/...` everywhere | `MenuGroupManager.CANTEEN_DOC_ID` (menu groups and MenuCollection) |
| `{n}_plazaUnipdu` (Status doc ID) | `OrderConfirmationService` | Not constructed; read from the snapshot |
| `"Open Bill"` | `transactionMethod` on open bills | `OrderBlock.isOpenBill()` |
| `"Tidak Memesan"` | Default `waktuPengambilan` | Default in `RecentlyServedActivity` |
| `Status`, `RecentlyServed` (root) | `Col.name('Status')`, `Col.name('RecentlyServed')` | `TestingModeManager.col(prefs, ...)` |

### 6.2 Customer number counter

`Canteens/canteen375/Metadata/customerNumber` is incremented only by the POS.
The POS reset button (`HomeController.resetCustomerNumber`) sets it to 0.
Because the POS refuses to overwrite an existing `Status` doc, **any unswiped
card whose number comes up again blocks checkout** with "Nomor pelanggan N
sudah digunakan". The kitchen must clear the board before a reset. The same
reset deletes `Canteens/canteen375/RecentlyServed` (a subcollection), which is
**not** where the KDS writes history (root `RecentlyServed`), so KDS history is
not cleared.

### 6.3 Testing mode

- Both apps prefix root collections with `zTesting_` when enabled. Preference
  key `testing_mode_enabled` in both apps: Flutter `SharedPreferences` on the
  POS, `shared_prefs` on the KDS.
- The switch is **per device and not synced**. A POS left in testing mode
  writes real orders to `zTesting_Status`, and a production KDS never shows
  them. The KDS shows a red banner when its own testing mode is on; nothing on
  the KDS reveals the POS's mode.
- The KDS prefixes only `Status` and `RecentlyServed`. The POS prefixes many
  more (including `Canteens`, so it has a separate counter and menu in testing).
  Menu groups always use the production `MenuCollection` and `KdsMenuGroups`,
  so groups are the same in both modes. A menu that exists only in the POS's
  testing copy (`Col.migrateMenuCollection`) can't be added to a group.
  Ingredient mode is different: recipes and `KdsIngredientProgress` follow the
  testing prefix (section 5.5).
- Toggling on the KDS clears the local order cache and recreates the activity.

### 6.4 Auth and rules

The KDS requires Firebase email/password login (`LoginActivity`), but the
deployed rules allow `read, write, update, delete: if true` on `Status`,
`RecentlyServed` and their `zTesting_` twins. Rule changes are made and
deployed from the POS repo.

`Canteens/canteen375/KdsMenuGroups` allows read, write and delete for any
signed-in user, with a shape check on `name`, `menuItemIds` and `menuNames`.
Kitchen tablets use non-admin staff accounts, so this must not require
`isAdmin()`. Every `Canteens` subcollection needs its own `match` block. That
rule was deployed on 2026-09-27.

`Canteens/canteen375/KdsIngredientProgress` allows read and delete for any
signed-in user, and create/update when `progress` is a map. It was added to
`375POS/firestore.rules` on 2026-09-28. Until it is deployed, ticks in
production fail with a permission message (testing mode works, because
`zTesting_Canteens` is open). The KDS also reads `OptionGroups` and
`Inventory`, which require sign-in and are already allowed.

---

## 7. KDS code map

| File | Role |
|---|---|
| `LoginActivity` | Launcher. Email/password sign-in, then opens `MainActivity`. |
| `MainActivity` | Status listener and parser, local merge, SharedPreferences cache, filters, aggregation panel, swipe-to-serve, testing toggle, logout. |
| `RecentlyServedActivity` | History (`RecentlyServed` ordered by `timestampServe` desc, limit 50, **no canteenId filter**), restore swipe. Parses both the Status item shape and the older `quantity`/`orderType` shape. |
| `RecyclerAdapter2` | Order cards for both screens: item sorting, open-bill round headers, food→drink divider, tap (+1) and long-press (reset), grey-out when complete, per-card timers, TTS. Taps are handled only when the card has no served time, i.e. on the main screen. |
| `AggregationAdapter` / `AggregatedItem` | Left panel. Totals across all orders keyed by `name + orderType + optionIds + note` (no `orderedAt`, unlike the write-back key). A tap increments the first unfinished referenced line, then persists that order. |
| `StatusOrderItemsBuilder` | The only serializer for `orderItems`. Used by prepared-count updates, swipe and restore. |
| `OrderBlock`, `NewOrderItem`, `SelectedOption` | In-memory models. Also the Gson shape of the local cache. |
| `MenuGroup`, `MenuGroupManager` | Shared menu groups for the custom filter: model and matching, plus the Firestore listener and the picker/editor dialogs (section 5.4). Matching is covered by `MenuGroupTest`. |
| `IngredientCatalog`, `IngredientBoard`, `IngredientProgressStore`, `IngredientSummaryAdapter` | Ingredient mode (section 5.5): POS recipe resolution, rows and summary, shared ticks, summary panel. Covered by `IngredientBoardTest`. |
| `TestingModeManager` | `zTesting_` prefix helper. |
| Legacy, unreachable: `Pesanan` (Realtime DB, deletes `Status/{customerNumber}`), `RecyclerAdapter`, `dump`, `transactionDetail`, `OrderItem`, `MainActivity.MyAdapter` and `MainActivity.RecentlyServed` | Do not revive or copy patterns from these. |

Local state in `MainActivity`:

- `orderBlockArrayList` is the full list, merged by `customerNumber`: removed if
  absent from the snapshot, updated field-by-field if present, appended if new.
  The field-by-field sync block is a list you must extend when adding fields.
  It currently does not sync `paymentMethod`, `bungkus`, `waktuPesan`,
  `waktuPengambilan` or `orderTimestamp`.
- `displayedOrders` is the filtered view given to the adapter.
- Persisted as JSON in SharedPreferences `shared_prefs` / `order_list` on every
  snapshot, tap and `onPause`. It is loaded at startup, so the screen shows
  cached orders before the first snapshot arrives.

Filters (`FILTER_ALL` / `FOOD` / `DRINK` / `CUSTOM`) apply to the cards, the
aggregation panel and the "unfinished items" prompt on swipe. `CUSTOM` means
"the active menu group" (`MainActivity.activeMenuGroup`, mirrored into
`RecyclerAdapter2`). The groups are shared, but the filter chosen on a tablet
is in-memory only and resets when the app restarts.

---

## 8. Known gaps

These are verified in the code and not fixed. Do not fix them as a side effect
of another task. Mention them to the user when a task touches the area.

1. **Whole-array overwrite race** on `orderItems` (5.1).
2. **Swipe is not atomic** (5.2). If `add` fails after `delete` succeeds, the
   history row is lost.
3. **History and restore drop POS fields** (5.2, 5.3).
4. **`RecentlyServed.waktuPesan` is a string**, which breaks POS reprint time.
5. **`paymentMethod` goes stale on the KDS.** It is not in the sync block, so a
   settled open bill is archived with the empty payment method it had when
   first seen.
6. **The "unlocked" open-bill badge is effectively unreachable.** Settlement
   changes `transactionMethod`, so `isOpenBill()` is false once `isClosed` is
   true.
7. **`isMakanan` defaults differ**: POS `false`, KDS `true` when absent.
8. **No sound for open-bill appends or POS edits.** Only `ADDED` triggers it.
9. **Activity stacking.** The history FAB starts a new `MainActivity` instead of
   returning, and the order snapshot listener is never removed. Each round
   trip adds another live listener (duplicate sounds and writes are possible).
10. **Timer clock skew.** Elapsed time is tablet clock minus server
    `waktuPesan`, and round headers use the POS device clock (`orderedAt`)
    formatted in the tablet's default timezone.
11. **`total` parse fragility** (rule 5).

---

## 9. Change checklists

**Changing what the KDS reads from `Status`:** confirm that every POS writer
sets the field with the expected type:
`OrderConfirmationService._processOrder`, `_processSelfOrder`,
`_processOpenBillOrder` (create and append branches),
`_processOpenBillSettlement`, and
`EditOrderService._processEditOrderTransactional`. Ignore the commented-out
`*Legacy` methods. Handle absence gracefully; old documents exist.

**Adding a per-item field:** update `NewOrderItem`, the `MainActivity` parser
(both the dine-in and take-away branches), `RecentlyServedActivity`
(`applyItemMetaFromFirestoreMap` and both item shapes), and
`StatusOrderItemsBuilder`. Otherwise the first tap deletes it from Firestore.

**Adding a top-level field to carry through:** update `OrderBlock`, the
`MainActivity` parser, the **sync block** in the listener, the
`performSwipeServe` payload, the `RecentlyServedActivity` parser, and
`buildStatusPayloadForRestore`.

**Changing anything the KDS writes:** check the POS readers:
`EditOrderService` (prepared-count preservation by `orderKey`), `OpenBill.fromStatusDoc`
/ `OpenBillService`, the settlement guards (`isClosed`, `status`,
`orderRevision`, `total` equality), `ConnectPrinterDialog` (active and completed
lists), `HomeController.reprintFromOrder`, and the three audit services.

**Changing ingredient mode:** keep `IngredientCatalog` in step with the POS
stock deduction (section 5.5), keep ingredient progress out of `Status`, and
keep swipe disabled in that mode. When the POS changes `MenuIngredient`,
`OptionItem` or `calculateOrderStockDeltas`, update the catalog and
`IngredientBoardTest`.

**Changing queries, collection names or IDs:** check section 6.1, the
composite index (section 4), and the rules in `375POS/firestore.rules`.

**Before calling it done:** build (`./gradlew assembleDebug`), then test with
**both** devices in testing mode: a normal order, a take-away plus dine-in
line, an order with options and a note, an open bill with two rounds (swipe
blocked, then settle, then swipe), a POS edit after partial preparation, and a
restore from history.

---

## 10. Where to look in the POS

| Concern | File (under `375POS/lib/`) |
|---|---|
| Create `Status` (checkout, self order, open bill, settlement) | `Services/OrderConfirmationService.dart`: `_runIdempotentSaleTransaction`, `_buildSaleOrderItems`, `_processOrder`, `_processSelfOrder`, `_processOpenBillOrder`, `_processOpenBillSettlement` |
| Edit an order | `Services/EditOrderService.dart`, `Screens/EditOrderScreen.dart`, `Controllers/HomeController.dart` (`loadOrderForEdit`) |
| Open bills | `Services/OpenBillService.dart`, `Models/OpenBill.dart` |
| Basket line identity (`orderKey`) | `Classes/Pesanan.dart` |
| Counter and reset, reprint | `Controllers/HomeController.dart` (`getMenu`, `resetCustomerNumber`, `reprintFromOrder`) |
| Active and completed lists, reprint UI | `AlertDialogs/ConnectPrinterDialog.dart` |
| Testing prefix | `Services/TestingModeService.dart` (`Col`) |
| Menu documents | `BottomSheets/AddOrEditMenu.dart`, `Classes/Menu.dart` |
| Ingredients and stock deduction | `Classes/Inventory.dart` (`MenuIngredient`), `Classes/OptionGroup.dart` (`OptionItem`), `Services/InventoryService.dart` (`StockRequirementCalculator`, `calculateOrderStockDeltas`) |
| Rules | `firestore.rules` (repo root) |
