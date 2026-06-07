package com.example.orderstobeserved;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Dialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.util.DisplayMetrics;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth; // ADDED
import com.google.firebase.auth.FirebaseUser; // ADDED
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentChange; // ADDED
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson; // ADDED
import com.google.gson.reflect.TypeToken; // ADDED

import android.media.Ringtone; // ADDED
import android.media.RingtoneManager; // ADDED
import android.net.Uri; // ADDED

import java.lang.reflect.Type; // ADDED
import java.util.ArrayList;
import java.util.Collections; // ADDED
import java.util.Comparator; // ADDED
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String CANTEEN_ID = "canteen375_plazaUnipdu";
    private ArrayList<OrderBlock> orderBlockArrayList;
    private ArrayList<OrderBlock> displayedOrders;
    private FloatingActionButton toggleActivityFab;
    private RecyclerView recyclerView;
    private boolean isToggling = false;
    private RecyclerAdapter2 recyclerAdapter;
    private DividerItemDecoration dividerItemDecoration;
    private ItemTouchHelper itemTouchHelper;
    private FirebaseFirestore fs;
    private FirebaseAuth mAuth; // ADDED

    private SharedPreferences sharedPreferences;
    private Gson gson;

    // Aggregation
    private LinearLayout aggregationSection;
    private RecyclerView aggregationRecyclerView;
    private AggregationAdapter aggregationAdapter;
    private ArrayList<AggregatedItem> aggregatedItemsList;
    private ImageButton toggleAggregationButton;
    private FloatingActionButton showAggregationFab;
    private boolean isAggregationVisible = true;

    // Filter
    private int currentFilter = RecyclerAdapter2.FILTER_ALL;
    private ImageButton filterAllBtn, filterFoodBtn, filterDrinkBtn, filterCustomBtn;
    private List<String> customFilterMenus = new ArrayList<>();

    // Testing Mode
    private TextView testModeToggleBtn;
    private TextView testModeBanner;
    private boolean isTestingMode;
    private boolean isInitialLoad = true; // ADDED

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide action bar and make fullscreen
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        // Hide system UI bars for maximum screen real estate
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        // KDS should never let the screen turn off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Not logged in, redirect to LoginActivity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        fs = FirebaseFirestore.getInstance();

        // Initialize SharedPreferences and Gson
        sharedPreferences = getSharedPreferences("shared_prefs", MODE_PRIVATE);
        gson = new Gson();

        // Load saved orders from SharedPreferences
        String json = sharedPreferences.getString("order_list", null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<OrderBlock>>() {}.getType();
            orderBlockArrayList = gson.fromJson(json, type);
        } else {
            orderBlockArrayList = new ArrayList<>();
        }
        displayedOrders = new ArrayList<>();

        toggleActivityFab = findViewById(R.id.toggleActivityFab);
        recyclerView = findViewById(R.id.recyclerView);

        // Initialize aggregation views
        aggregationSection = findViewById(R.id.aggregationSection);
        aggregationRecyclerView = findViewById(R.id.aggregationRecyclerView);
        toggleAggregationButton = findViewById(R.id.toggleAggregationButton);
        showAggregationFab = findViewById(R.id.showAggregationFab);
        aggregatedItemsList = new ArrayList<>();

        // Set aggregation section width to 25% of screen width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        ViewGroup.LayoutParams params = aggregationSection.getLayoutParams();
        params.width = (int) (screenWidth * 0.25); // 25% of screen width
        aggregationSection.setLayoutParams(params);

        // Setup aggregation adapter with click listener
        aggregationAdapter = new AggregationAdapter(this, aggregatedItemsList, new AggregationAdapter.OnAggregatedItemClickListener() {
            @Override
            public void onAggregatedItemClick(AggregatedItem aggregatedItem) {
                handleAggregatedItemClick(aggregatedItem);
            }
        });
        aggregationRecyclerView.setAdapter(aggregationAdapter);

        // Setup toggle button
        toggleAggregationButton.setOnClickListener(v -> toggleAggregationVisibility());

        // Setup FAB to show aggregation when hidden
        showAggregationFab.setOnClickListener(v -> toggleAggregationVisibility());

        // Setup filter buttons
        filterAllBtn = findViewById(R.id.filterAllBtn);
        filterFoodBtn = findViewById(R.id.filterFoodBtn);
        filterDrinkBtn = findViewById(R.id.filterDrinkBtn);
        filterCustomBtn = findViewById(R.id.filterCustomBtn);
        filterAllBtn.setOnClickListener(v -> setFilter(RecyclerAdapter2.FILTER_ALL));
        filterFoodBtn.setOnClickListener(v -> setFilter(RecyclerAdapter2.FILTER_FOOD));
        filterDrinkBtn.setOnClickListener(v -> setFilter(RecyclerAdapter2.FILTER_DRINK));
        filterCustomBtn.setOnClickListener(v -> showCustomFilterDialog());

        // Setup testing mode toggle
        testModeToggleBtn = findViewById(R.id.testModeToggleBtn);
        testModeBanner = findViewById(R.id.testModeBanner);
        isTestingMode = TestingModeManager.isEnabled(sharedPreferences);
        updateTestModeUI();
        testModeToggleBtn.setOnClickListener(v -> toggleTestingMode());

        // Setup logout button
        findViewById(R.id.logoutBtn).setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        // Listener for "Status" collection using the new data structure
        fs.collection(TestingModeManager.col(sharedPreferences, "Status"))
                .whereEqualTo("canteenId", CANTEEN_ID)
                .orderBy("waktuPesan", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Error fetching data", error);
                            return;
                        }
                        if (value != null) {
                            // Check for new orders to play sound
                            boolean hasNewOrder = false;
                            for (DocumentChange dc : value.getDocumentChanges()) {
                                if (dc.getType() == DocumentChange.Type.ADDED) {
                                    hasNewOrder = true;
                                    break;
                                }
                            }

                            if (hasNewOrder && !isInitialLoad) {
                                playNotificationSound();
                            }
                            isInitialLoad = false;

                            // Build a new list from the latest snapshot.
                            List<DocumentSnapshot> snapshotList = value.getDocuments();
                            Log.d(TAG, "Firestore snapshot received: " + snapshotList.size() + " document(s)");
                            ArrayList<OrderBlock> orderBlockArrayListComparator = new ArrayList<>();

                            for (DocumentSnapshot snapshot : snapshotList) {
                                Map<String, Object> map = snapshot.getData();
                                Log.d(TAG, "Document ID: " + snapshot.getId() + " | Raw data: " + map);
                                try {
                                    // Parse customerNumber (our unique ID for an order)
                                    Object customerNumberObj = map.get("customerNumber");
                                    if (customerNumberObj == null ||
                                            String.valueOf(customerNumberObj).trim().isEmpty() ||
                                            String.valueOf(customerNumberObj).equalsIgnoreCase("null")) {
                                        continue;
                                    }
                                    int customerNumber = Integer.parseInt(String.valueOf(customerNumberObj));

                                    // Parse bungkus
                                    Object bungkusObj = map.get("bungkus");
                                    int bungkus = 0;
                                    if (bungkusObj != null && !String.valueOf(bungkusObj).trim().isEmpty() &&
                                            !String.valueOf(bungkusObj).equalsIgnoreCase("null")) {
                                        try {
                                            bungkus = Integer.parseInt(String.valueOf(bungkusObj));
                                        } catch (NumberFormatException e) {
                                            bungkus = 0;
                                        }
                                    }

                                    // Parse namaCustomer
                                    String namaCustomer = map.get("namaCustomer") == null ? "" : String.valueOf(map.get("namaCustomer"));

                                    // Parse waktuPengambilan and waktuPesan
                                    String waktuPengambilan = map.get("waktuPengambilan") == null ? "" : String.valueOf(map.get("waktuPengambilan"));
                                    String waktuPesan = "";
                                    int total = Integer.parseInt(Objects.requireNonNull(map.get("total")).toString());
                                    
                                    // Get timestamp for count-up timer
                                    long orderTimestampMs = 0;
                                    
                                    if (map.containsKey("waktuPesan")) {
                                        Object waktuPesanObj = map.get("waktuPesan");
                                        if (waktuPesanObj instanceof com.google.firebase.Timestamp) {
                                            com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) waktuPesanObj;
                                            java.util.Date date = timestamp.toDate();
                                            
                                            // Get the display time
                                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.ENGLISH);
                                            sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Jakarta"));
                                            waktuPesan = sdf.format(date);
                                            
                                            // Get the timestamp in milliseconds for count-up timer
                                            orderTimestampMs = date.getTime();
                                        } else {
                                            // Fallback to old format
                                            waktuPesan = String.valueOf(waktuPesanObj);
                                            
                                            try {
                                                // Try to extract timestamp from old format if possible
                                                String timestampStr = waktuPesan.substring(waktuPesan.indexOf("=")+1, waktuPesan.indexOf(","));
                                                int waktuPesanInt = Integer.parseInt(timestampStr);
                                                orderTimestampMs = waktuPesanInt * 1000L;
                                            } catch (Exception e) {
                                                Log.e(TAG, "Error parsing timestamp: " + e.getMessage());
                                            }
                                        }
                                    }

                                    // Parse orderItems as a list of maps
                                    ArrayList<NewOrderItem> newOrderItems = new ArrayList<>();
                                    Object orderItemsObj = map.get("orderItems");
                                    if (orderItemsObj != null && orderItemsObj instanceof List) {
                                        List<?> orderItemsList = (List<?>) orderItemsObj;
                                        for (Object itemObj : orderItemsList) {
                                            if (itemObj instanceof Map) {
                                                Map<String, Object> itemMap = (Map<String, Object>) itemObj;
                                                int dineInQuantity = 0;
                                                Object dineInObj = itemMap.get("dineInQuantity");
                                                if (dineInObj != null && !String.valueOf(dineInObj).trim().isEmpty() &&
                                                        !String.valueOf(dineInObj).equalsIgnoreCase("null")) {
                                                    try {
                                                        dineInQuantity = Integer.parseInt(String.valueOf(dineInObj));
                                                    } catch (Exception e) {
                                                        dineInQuantity = 0;
                                                    }
                                                }
                                                String namaPesanan = itemMap.get("namaPesanan") == null ? "" : String.valueOf(itemMap.get("namaPesanan"));
                                                String customerNote = "";
                                                Object customerNoteObj = itemMap.get("customerNote");
                                                if (customerNoteObj != null && !String.valueOf(customerNoteObj).trim().isEmpty()
                                                        && !String.valueOf(customerNoteObj).equalsIgnoreCase("null")) {
                                                    customerNote = String.valueOf(customerNoteObj).trim();
                                                }
                                                int takeAwayQuantity = 0;
                                                Object takeAwayObj = itemMap.get("takeAwayQuantity");
                                                if (takeAwayObj != null && !String.valueOf(takeAwayObj).trim().isEmpty() &&
                                                        !String.valueOf(takeAwayObj).equalsIgnoreCase("null")) {
                                                    try {
                                                        takeAwayQuantity = Integer.parseInt(String.valueOf(takeAwayObj));
                                                    } catch (Exception e) {
                                                        takeAwayQuantity = 0;
                                                    }
                                                }
                                                String status = itemMap.get("status") == null ? "" : String.valueOf(itemMap.get("status"));

                                                // Parse selectedOptions
                                                ArrayList<SelectedOption> selectedOptions = new ArrayList<>();
                                                Object selectedOptionsObj = itemMap.get("selectedOptions");
                                                if (selectedOptionsObj instanceof List) {
                                                    List<?> selectedOptionsList = (List<?>) selectedOptionsObj;
                                                    for (Object optObj : selectedOptionsList) {
                                                        if (optObj instanceof Map) {
                                                            Map<String, Object> optMap = (Map<String, Object>) optObj;
                                                            String optionId = optMap.get("optionId") == null ? "" : String.valueOf(optMap.get("optionId"));
                                                            String optionName = optMap.get("optionName") == null ? "" : String.valueOf(optMap.get("optionName"));
                                                            String groupId = optMap.get("groupId") == null ? "" : String.valueOf(optMap.get("groupId"));
                                                            String groupName = optMap.get("groupName") == null ? "" : String.valueOf(optMap.get("groupName"));
                                                            int priceAdj = 0;
                                                            Object priceAdjObj = optMap.get("priceAdjustment");
                                                            if (priceAdjObj != null) {
                                                                try { priceAdj = Integer.parseInt(String.valueOf(priceAdjObj)); } catch (Exception ignored) { }
                                                            }
                                                            selectedOptions.add(new SelectedOption(optionId, optionName, groupId, groupName, priceAdj));
                                                        }
                                                    }
                                                }

                                                // Parse isMakanan (default true if absent)
                                                boolean isMakanan = true;
                                                if (itemMap.containsKey("isMakanan")) {
                                                    Object isMakananObj = itemMap.get("isMakanan");
                                                    if (isMakananObj instanceof Boolean) {
                                                        isMakanan = (Boolean) isMakananObj;
                                                    } else if (isMakananObj != null) {
                                                        isMakanan = Boolean.parseBoolean(String.valueOf(isMakananObj));
                                                    }
                                                }

                                                // Parse harga (unit price)
                                                int harga = 0;
                                                Object hargaObj = itemMap.get("harga");
                                                if (hargaObj != null) {
                                                    try { harga = Integer.parseInt(String.valueOf(hargaObj)); } catch (Exception ignored) { }
                                                }

                                                // If the same item is ordered for both dine-in and take-away, they should be separate.
                                                if (dineInQuantity > 0) {
                                                    NewOrderItem dineInItem = new NewOrderItem(namaPesanan, "dine-in", dineInQuantity, status, selectedOptions);
                                                    dineInItem.setCustomerNote(customerNote);
                                                    dineInItem.setIsMakanan(isMakanan);
                                                    dineInItem.setHarga(harga);
                                                    int dineInPrepared = 0;
                                                    Object dineInPrepObj = itemMap.get("dineInPreparedQuantity");
                                                    if (dineInPrepObj != null) {
                                                        try { dineInPrepared = Integer.parseInt(String.valueOf(dineInPrepObj)); } catch (Exception ignored) { }
                                                    }
                                                    dineInItem.setPreparedQuantity(dineInPrepared);
                                                    newOrderItems.add(dineInItem);
                                                }
                                                if (takeAwayQuantity > 0) {
                                                    NewOrderItem takeAwayItem = new NewOrderItem(namaPesanan, "take-away", takeAwayQuantity, status, selectedOptions);
                                                    takeAwayItem.setCustomerNote(customerNote);
                                                    takeAwayItem.setIsMakanan(isMakanan);
                                                    takeAwayItem.setHarga(harga);
                                                    int takeAwayPrepared = 0;
                                                    Object takeAwayPrepObj = itemMap.get("takeAwayPreparedQuantity");
                                                    if (takeAwayPrepObj != null) {
                                                        try { takeAwayPrepared = Integer.parseInt(String.valueOf(takeAwayPrepObj)); } catch (Exception ignored) { }
                                                    }
                                                    takeAwayItem.setPreparedQuantity(takeAwayPrepared);
                                                    newOrderItems.add(takeAwayItem);
                                                }
                                            }
                                        }
                                    }
                                    // Create OrderBlock with timestamp for count-up timer
                                    OrderBlock orderBlock = new OrderBlock(bungkus, customerNumber, namaCustomer, newOrderItems, waktuPengambilan, waktuPesan, orderTimestampMs, total);

                                    // Parse transactionMethod and isClosed for Open Bill support
                                    String transactionMethod = map.get("transactionMethod") == null ? "" : String.valueOf(map.get("transactionMethod"));
                                    orderBlock.setTransactionMethod(transactionMethod);

                                    String paymentMethod = map.get("paymentMethod") == null ? "" : String.valueOf(map.get("paymentMethod"));
                                    orderBlock.setPaymentMethod(paymentMethod);

                                    boolean isClosed = true;
                                    if (map.containsKey("isClosed")) {
                                        Object isClosedObj = map.get("isClosed");
                                        if (isClosedObj instanceof Boolean) {
                                            isClosed = (Boolean) isClosedObj;
                                        } else if (isClosedObj != null) {
                                            isClosed = Boolean.parseBoolean(String.valueOf(isClosedObj));
                                        }
                                    }
                                    orderBlock.setClosed(isClosed);

                                    // Parse additional fields for RecentlyServed transfer
                                    orderBlock.setCanteenId(map.get("canteenId") == null ? "" : String.valueOf(map.get("canteenId")));
                                    orderBlock.setCustomerPhone(map.get("customerPhone") == null ? "" : String.valueOf(map.get("customerPhone")));
                                    orderBlock.setMemberId(map.get("memberId") == null ? "" : String.valueOf(map.get("memberId")));
                                    boolean isMember = false;
                                    if (map.containsKey("isMember")) {
                                        Object isMemberObj = map.get("isMember");
                                        if (isMemberObj instanceof Boolean) {
                                            isMember = (Boolean) isMemberObj;
                                        } else if (isMemberObj != null) {
                                            isMember = Boolean.parseBoolean(String.valueOf(isMemberObj));
                                        }
                                    }
                                    orderBlock.setMember(isMember);

                                    // Grab takeAwayFee
                                    long takeAwayFee = 0;
                                    if (map.containsKey("takeAwayFee")) {
                                        Object taf = map.get("takeAwayFee");
                                        if (taf != null) {
                                            try {
                                                takeAwayFee = Long.parseLong(String.valueOf(taf));
                                            } catch (NumberFormatException ignored) {}
                                        }
                                    }
                                    orderBlock.setTakeAwayFee(takeAwayFee);

                                    // Grab orderHistory
                                    if (map.containsKey("orderHistory")) {
                                        Object oh = map.get("orderHistory");
                                        if (oh instanceof java.util.List) {
                                            try {
                                                @SuppressWarnings("unchecked")
                                                java.util.List<java.util.Map<String, Object>> historyList = (java.util.List<java.util.Map<String, Object>>) oh;
                                                orderBlock.setOrderHistory(historyList);
                                            } catch (Exception ignored) {}
                                        }
                                    }

                                    // Store the actual Firestore document ID so we can delete correctly
                                    orderBlock.setFirestoreDocumentId(snapshot.getId());

                                    orderBlockArrayListComparator.add(orderBlock);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            // ADDED: Sort using the comparator (always use orderBlockArrayListComparator)
                            Collections.sort(orderBlockArrayListComparator, new Comparator<OrderBlock>() {
                                @Override
                                public int compare(OrderBlock o1, OrderBlock o2) {
                                    return o1.getWaktuPesan().compareTo(o2.getWaktuPesan());
                                }
                            });

                            // Now compare orderBlockArrayListComparator with the current orderBlockArrayList.
                            // Remove any OrderBlock from orderBlockArrayList that is no longer present.
                            for (int i = orderBlockArrayList.size() - 1; i >= 0; i--) {
                                OrderBlock existing = orderBlockArrayList.get(i);
                                boolean found = false;
                                for (OrderBlock comp : orderBlockArrayListComparator) {
                                    if (comp.getCustomerNumber() == existing.getCustomerNumber()) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    orderBlockArrayList.remove(i);
                                }
                            }
                            // Append new orders or update existing ones (e.g. isClosed, appended items)
                            for (OrderBlock comp : orderBlockArrayListComparator) {
                                boolean exists = false;
                                for (OrderBlock existing : orderBlockArrayList) {
                                    if (existing.getCustomerNumber() == comp.getCustomerNumber()) {
                                        exists = true;
                                        // Sync all fields from latest Firestore snapshot
                                        existing.setClosed(comp.isClosed());
                                        existing.setTransactionMethod(comp.getTransactionMethod());
                                        existing.setOrderItems(comp.getOrderItems());
                                        existing.setCanteenId(comp.getCanteenId());
                                        existing.setCustomerPhone(comp.getCustomerPhone());
                                        existing.setMember(comp.isMember());
                                        existing.setMemberId(comp.getMemberId());
                                        existing.setTotal(comp.getTotal());
                                        existing.setTakeAwayFee(comp.getTakeAwayFee());
                                        existing.setOrderHistory(comp.getOrderHistory());
                                        existing.setNamaCustomer(comp.getNamaCustomer());
                                        existing.setFirestoreDocumentId(comp.getFirestoreDocumentId());
                                        break;
                                    }
                                }
                                if (!exists) {
                                    orderBlockArrayList.add(comp);
                                }
                            }

                            // Save the updated list to SharedPreferences
                            String updatedJson = gson.toJson(orderBlockArrayList);
                            sharedPreferences.edit().putString("order_list", updatedJson).apply();

                            // Apply filter and refresh both views
                            applyFilterAndRefresh();
                        } else {
                            Log.e(TAG, "onEvent: query snapshot was null");
                        }
                    }
                });

        // Build initial displayedOrders from saved data
        applyFilterAndRefresh();

        itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        recyclerAdapter = new RecyclerAdapter2(MainActivity.this, displayedOrders);
        recyclerView.setAdapter(recyclerAdapter);
        
        // Single toggle FAB - switches to RecentlyServedActivity
        toggleActivityFab.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), RecentlyServedActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            if (r != null) {
                r.play();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing notification sound", e);
        }
    }

    public void openBackEnd() {
        Intent intent = new Intent(this, Pesanan.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Restart count-up timers that were stopped in onPause()
        if (recyclerAdapter != null) {
            recyclerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recyclerAdapter != null) {
            recyclerAdapter.stopAllTimers();
            recyclerAdapter.shutdownTTS();
        }
    }

    // Rebuild aggregation from current orders, respecting current filter
    private void rebuildAggregation() {
        Map<String, AggregatedItem> aggregationMap = new HashMap<>();

        for (OrderBlock order : orderBlockArrayList) {
            ArrayList<NewOrderItem> items = order.getOrderItems();
            if (items != null) {
                for (NewOrderItem item : items) {
                    if (!matchesCurrentFilter(item)) continue;

                    String key = item.getAggregationKey();
                    AggregatedItem aggregatedItem = aggregationMap.get(key);
                    if (aggregatedItem == null) {
                        aggregatedItem = new AggregatedItem(key, item.getNamaPesanan(), item.getOrderType());
                        aggregatedItem.setIsMakanan(item.getIsMakanan());
                        aggregationMap.put(key, aggregatedItem);
                    }
                    aggregatedItem.addItemReference(order.getCustomerNumber(), item);
                }
            }
        }

        aggregatedItemsList.clear();
        for (AggregatedItem item : aggregationMap.values()) {
            if (!item.isFullyServed()) {
                aggregatedItemsList.add(item);
            }
        }

        Collections.sort(aggregatedItemsList, new Comparator<AggregatedItem>() {
            @Override
            public int compare(AggregatedItem o1, AggregatedItem o2) {
                if (o1.getIsMakanan() != o2.getIsMakanan()) {
                    return Boolean.compare(o2.getIsMakanan(), o1.getIsMakanan());
                }
                return o1.getItemName().compareToIgnoreCase(o2.getItemName());
            }
        });

        if (aggregationAdapter != null) {
            aggregationAdapter.notifyDataSetChanged();
        }
    }

    // Handle click on aggregated item - increment one item in the orders
    private void handleAggregatedItemClick(AggregatedItem aggregatedItem) {
        List<AggregatedItem.ItemReference> references = aggregatedItem.getItemReferences();
        
        // Find the first item that is not fully prepared
        for (AggregatedItem.ItemReference ref : references) {
            NewOrderItem orderItem = ref.getOrderItem();
            if (orderItem.getPreparedQuantity() < orderItem.getQuantity()) {
                orderItem.incrementPrepared();
                aggregatedItem.recalculateTotals();
                recyclerAdapter.notifyDataSetChanged();

                if (aggregatedItem.isFullyServed()) {
                    aggregatedItemsList.remove(aggregatedItem);
                }
                aggregationAdapter.notifyDataSetChanged();

                // Persist to Firestore and SharedPreferences
                for (OrderBlock order : orderBlockArrayList) {
                    if (order.getCustomerNumber() == ref.getCustomerNumber()) {
                        onPreparedQuantityChanged(order);
                        break;
                    }
                }
                
                break; // Only increment one item per click
            }
        }
    }

    // Toggle aggregation section visibility with crash prevention
    private void toggleAggregationVisibility() {
        // Prevent rapid toggling that causes crashes
        if (isToggling) {
            return;
        }
        
        isToggling = true;
        
        // Use a handler to reset the toggle flag after animation completes
        new android.os.Handler().postDelayed(() -> isToggling = false, 300);
        
        if (isAggregationVisible) {
            // Hide aggregation section
            aggregationSection.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    ViewGroup.LayoutParams params = aggregationSection.getLayoutParams();
                    params.width = 0;
                    aggregationSection.setLayoutParams(params);
                    aggregationSection.setVisibility(View.GONE);
                    isAggregationVisible = false;
                    
                    // Show FAB when aggregation is hidden
                    if (showAggregationFab != null) {
                        showAggregationFab.show();
                    }
                })
                .start();
        } else {
            // Show aggregation section (25% of screen width)
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            ViewGroup.LayoutParams params = aggregationSection.getLayoutParams();
            params.width = (int) (screenWidth * 0.25);
            aggregationSection.setLayoutParams(params);
            aggregationSection.setVisibility(View.VISIBLE);
            aggregationSection.setAlpha(0f);
            
            aggregationSection.animate()
                .alpha(1f)
                .setDuration(200)
                .withEndAction(() -> {
                    isAggregationVisible = true;
                    
                    // Hide FAB when aggregation is visible
                    if (showAggregationFab != null) {
                        showAggregationFab.hide();
                    }
                })
                .start();
        }
    }

    // Public method to notify aggregation when items change in RecyclerAdapter
    public void notifyAggregationChanged() {
        rebuildAggregation();
    }

    /**
     * Called by adapters after preparedQuantity changes.
     * Persists the updated state to Firestore + SharedPreferences and rebuilds aggregation.
     */
    public void onPreparedQuantityChanged(OrderBlock order) {
        rebuildAggregation();
        updatePreparedQuantitiesInFirestore(order);
        String json = gson.toJson(orderBlockArrayList);
        sharedPreferences.edit().putString("order_list", json).apply();
    }

    /**
     * Write the current preparedQuantity values for every item in the order
     * back to the Firestore Status document so the state survives snapshot refreshes.
     */
    private void updatePreparedQuantitiesInFirestore(OrderBlock order) {
        String docId = order.getFirestoreDocumentId();
        if (docId == null || docId.isEmpty()) return;

        ArrayList<Map<String, Object>> firestoreItems =
            StatusOrderItemsBuilder.toFirestoreArrayList(order.getOrderItems());

        fs.collection(TestingModeManager.col(sharedPreferences, "Status"))
            .document(docId)
            .update("orderItems", firestoreItems)
            .addOnFailureListener(e -> Log.e(TAG, "Failed to update prepared quantities", e));
    }

    // Filter: check if item matches current filter
    private boolean matchesCurrentFilter(NewOrderItem item) {
        if (currentFilter == RecyclerAdapter2.FILTER_ALL) return true;
        if (currentFilter == RecyclerAdapter2.FILTER_FOOD) return item.getIsMakanan();
        if (currentFilter == RecyclerAdapter2.FILTER_DRINK) return !item.getIsMakanan();
        if (currentFilter == RecyclerAdapter2.FILTER_CUSTOM) return customFilterMenus != null && customFilterMenus.contains(item.getNamaPesanan());
        return true;
    }

    /** True if any line currently visible under the food/drink filter is not fully prepared. */
    private boolean hasIncompleteVisibleItems(OrderBlock order) {
        ArrayList<NewOrderItem> items = order.getOrderItems();
        if (items == null) return false;
        for (NewOrderItem item : items) {
            if (!matchesCurrentFilter(item)) continue;
            if (item.getPreparedQuantity() < item.getQuantity()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Completes swipe-to-serve: delete Status, append RecentlyServed, update local lists.
     */
    private void performSwipeServe(OrderBlock servedOrder) {
        int position = displayedOrders.indexOf(servedOrder);
        if (position < 0) {
            recyclerAdapter.notifyDataSetChanged();
            return;
        }

        final String customerNumberToBeRemoved = String.valueOf(servedOrder.getCustomerNumber());

        final String docId = servedOrder.getFirestoreDocumentId() != null && !servedOrder.getFirestoreDocumentId().isEmpty()
                ? servedOrder.getFirestoreDocumentId()
                : customerNumberToBeRemoved;

        fs.collection(TestingModeManager.col(sharedPreferences, "Status"))
                .document(docId)
                .delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(getApplicationContext(), "Order " + customerNumberToBeRemoved + " served", Toast.LENGTH_SHORT).show());

        ArrayList<Map<String, Object>> formattedOrderItems = StatusOrderItemsBuilder.toFirestoreArrayList(servedOrder.getOrderItems());

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("canteenId", servedOrder.getCanteenId());
        orderData.put("bungkus", servedOrder.getBungkus());
        orderData.put("customerNumber", servedOrder.getCustomerNumber());
        orderData.put("namaCustomer", servedOrder.getNamaCustomer());
        orderData.put("customerPhone", servedOrder.getCustomerPhone());
        orderData.put("isMember", servedOrder.isMember());
        orderData.put("memberId", servedOrder.getMemberId());
        orderData.put("takeAwayFee", servedOrder.getTakeAwayFee());
        if (servedOrder.getOrderHistory() != null) {
            orderData.put("orderHistory", servedOrder.getOrderHistory());
        }
        orderData.put("transactionMethod", servedOrder.getTransactionMethod());
        orderData.put("paymentMethod", servedOrder.getPaymentMethod());
        orderData.put("orderItems", formattedOrderItems);
        orderData.put("waktuPengambilan", servedOrder.getWaktuPengambilan());

        long timestamp = servedOrder.getOrderTimestamp() / 1000;
        String formattedTimestamp = "Timestamp(seconds=" + timestamp + ", nanoseconds=317000000)";
        orderData.put("waktuPesan", formattedTimestamp);

        orderData.put("timestampServe", FieldValue.serverTimestamp());
        orderData.put("status", "Served");
        orderData.put("total", servedOrder.getTotal());
        orderData.put("sourceStatusDocumentId", docId);

        fs.collection(TestingModeManager.col(sharedPreferences, "RecentlyServed")).add(orderData);

        displayedOrders.remove(position);
        orderBlockArrayList.remove(servedOrder);
        recyclerAdapter.notifyItemRemoved(position);
        rebuildAggregation();
    }

    private void showIncompleteConfirmDialog(int position, OrderBlock servedOrder) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_serve);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.setCancelable(true);
        dialog.setOnCancelListener(d -> recyclerAdapter.notifyItemChanged(position));

        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> {
            dialog.dismiss();
            recyclerAdapter.notifyItemChanged(position);
        });

        dialog.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            performSwipeServe(servedOrder);
        });

        dialog.show();
    }

    // Rebuild displayedOrders from orderBlockArrayList based on filter, then refresh views
    private void applyFilterAndRefresh() {
        displayedOrders.clear();
        if (currentFilter == RecyclerAdapter2.FILTER_ALL) {
            displayedOrders.addAll(orderBlockArrayList);
        } else {
            for (OrderBlock order : orderBlockArrayList) {
                ArrayList<NewOrderItem> items = order.getOrderItems();
                if (items != null) {
                    boolean hasMatch = false;
                    for (NewOrderItem item : items) {
                        if (matchesCurrentFilter(item)) {
                            hasMatch = true;
                            break;
                        }
                    }
                    if (hasMatch) {
                        displayedOrders.add(order);
                    }
                }
            }
        }

        rebuildAggregation();
        if (recyclerAdapter != null) {
            recyclerAdapter.notifyDataSetChanged();
        }
    }

    // Set the active filter mode
    private void setFilter(int filter) {
        // Always re-apply FILTER_CUSTOM even if it was already active,
        // because the user may have changed their menu selection in the dialog.
        boolean sameFilter = (currentFilter == filter);
        if (sameFilter && filter != RecyclerAdapter2.FILTER_CUSTOM) return;
        currentFilter = filter;
        if (recyclerAdapter != null) {
            recyclerAdapter.setFilterMode(filter);
        }
        updateFilterButtons();
        applyFilterAndRefresh();
    }

    // Update filter button visuals
    private void updateFilterButtons() {
        ImageButton[] buttons = { filterAllBtn, filterFoodBtn, filterDrinkBtn, filterCustomBtn };
        int[] filters = { RecyclerAdapter2.FILTER_ALL, RecyclerAdapter2.FILTER_FOOD, RecyclerAdapter2.FILTER_DRINK, RecyclerAdapter2.FILTER_CUSTOM };

        for (int i = 0; i < buttons.length; i++) {
            if (filters[i] == currentFilter) {
                buttons[i].setBackgroundResource(R.drawable.filter_toggle_selected_bg);
                buttons[i].setColorFilter(Color.WHITE);
            } else {
                buttons[i].setBackgroundResource(R.drawable.filter_toggle_unselected_bg);
                buttons[i].setColorFilter(Color.parseColor("#666666"));
            }
        }
    }

    private void showCustomFilterDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(60, 60, 60, 60);
        root.setBackgroundColor(Color.WHITE);

        TextView titleView = new TextView(this);
        titleView.setText("Select Menus Output");
        titleView.setTextSize(20f);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(Color.BLACK);
        root.addView(titleView);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.setMargins(0, 30, 0, 30);
        scrollView.setLayoutParams(scrollParams);
        
        LinearLayout checkboxContainer = new LinearLayout(this);
        checkboxContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(checkboxContainer);
        root.addView(scrollView);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.END);

        android.widget.Button cancelBtn = new android.widget.Button(this);
        cancelBtn.setText("Cancel");
        cancelBtn.setBackgroundColor(Color.TRANSPARENT);
        cancelBtn.setTextColor(Color.parseColor("#888888"));
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(cancelBtn);

        android.widget.Space space = new android.widget.Space(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(40, 1));
        btnRow.addView(space);

        android.widget.Button applyBtn = new android.widget.Button(this);
        applyBtn.setText("Apply Base Filter");
        applyBtn.setBackgroundColor(Color.TRANSPARENT);
        applyBtn.setTextColor(Color.parseColor("#007AFF"));
        btnRow.addView(applyBtn);

        root.addView(btnRow);
        dialog.setContentView(root);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.4), (int)(getResources().getDisplayMetrics().heightPixels * 0.8));
        }

        // MenuCollection is always under the real Canteens root — it is NOT
        // duplicated under zTesting_Canteens, so never apply the testing prefix here.
        fs.collection("Canteens")
            .document("canteen375")
            .collection("MenuCollection")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    Toast.makeText(this, "MenuCollection is empty for " + CANTEEN_ID, Toast.LENGTH_SHORT).show();
                }
                checkboxContainer.removeAllViews();
                List<android.widget.CheckBox> checkBoxes = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    String namaMenu = doc.getId();
                    android.widget.CheckBox cb = new android.widget.CheckBox(this);
                    cb.setText(namaMenu);
                    cb.setTextColor(Color.BLACK);
                    cb.setTextSize(18f);
                    cb.setPadding(0, 15, 0, 15);
                    cb.setChecked(customFilterMenus.contains(namaMenu));
                    checkBoxes.add(cb);
                    checkboxContainer.addView(cb);
                }

                applyBtn.setOnClickListener(v -> {
                    customFilterMenus.clear();
                    for (android.widget.CheckBox cb : checkBoxes) {
                        if (cb.isChecked()) {
                            customFilterMenus.add(cb.getText().toString());
                        }
                    }
                    if (recyclerAdapter != null) {
                        recyclerAdapter.setCustomFilterMenus(customFilterMenus);
                    }
                    setFilter(RecyclerAdapter2.FILTER_CUSTOM);
                    dialog.dismiss();
                });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load MenuCollection: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error loading MenuCollection", e);
            });

        dialog.show();
    }

    // Toggle testing mode on/off. Clears local order cache and restarts the activity
    // so that the Firestore listener reconnects to the correct collection.
    private void toggleTestingMode() {
        boolean newMode = !TestingModeManager.isEnabled(sharedPreferences);
        TestingModeManager.setEnabled(sharedPreferences, newMode);
        // Clear local cache to avoid mixing production and testing data
        sharedPreferences.edit().remove("order_list").apply();
        recreate();
    }

    // Update test mode button and banner to reflect current state
    private void updateTestModeUI() {
        if (isTestingMode) {
            testModeToggleBtn.setText("TESTING");
            testModeToggleBtn.setTextColor(Color.WHITE);
            testModeToggleBtn.setBackgroundResource(R.drawable.test_mode_active_bg);
            testModeBanner.setVisibility(View.VISIBLE);
        } else {
            testModeToggleBtn.setText("TEST");
            testModeToggleBtn.setTextColor(Color.parseColor("#888888"));
            testModeToggleBtn.setBackgroundResource(R.drawable.filter_bar_bg);
            testModeBanner.setVisibility(View.GONE);
        }
    }

    // Adapted swipe callback
    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            if (position < 0 || position >= displayedOrders.size()) {
                return;
            }
            OrderBlock servedOrder = displayedOrders.get(position);

            // Block swipe on unclosed Open Bill orders
            if (servedOrder.isOpenBill() && !servedOrder.isClosed()) {
                recyclerAdapter.notifyItemChanged(position);
                Snackbar.make(recyclerView,
                    "Open bill masih terbuka. Selesaikan pembayaran di kasir terlebih dahulu.",
                    Snackbar.LENGTH_LONG).show();
                return;
            }

            if (hasIncompleteVisibleItems(servedOrder)) {
                showIncompleteConfirmDialog(position, servedOrder);
                return;
            }

            performSwipeServe(servedOrder);
        }
    };

    // Save the current list when leaving the activity and stop timers
    @Override
    protected void onPause() {
        super.onPause();
        // Save to shared preferences
        String json = gson.toJson(orderBlockArrayList);
        sharedPreferences.edit().putString("order_list", json).apply();
        
        // Stop all running timers to avoid memory leaks
        if (recyclerAdapter != null) {
            recyclerAdapter.stopAllTimers();
        }
    }

    // Remove legacy ArrayAdapter inner class if not needed.
    class MyAdapter extends ArrayAdapter<Integer> {
        Context context;
        ArrayList<Integer> rCustomerNumber;
        ArrayList<String> rOrders;

        MyAdapter(Context context, ArrayList<Integer> customerNumber, ArrayList<String> orders) {
            super(context, R.layout.orders_to_be_served, R.id.noCustomerTextView, customerNumber);
            this.context = context;
            this.rCustomerNumber = customerNumber;
            this.rOrders = orders;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = layoutInflater.inflate(R.layout.orders_to_be_served, parent, false);
            TextView noCustomerTextView = row.findViewById(R.id.noCustomerTextView);
            noCustomerTextView.setText(String.valueOf(rCustomerNumber.get(position)));
            return row;
        }
    }

    public class RecentlyServed {
        int customerNumber;
        String rincianPesanan;
        int bungkus;
        String waktuPengambilan;
        String waktuPesan;
        FieldValue timestampServe;

        public RecentlyServed(int customerNumber, String rincianPesanan, int bungkus, String waktuPengambilan, String waktuPesan, FieldValue timestampServe) {
            this.customerNumber = customerNumber;
            this.rincianPesanan = rincianPesanan;
            this.bungkus = bungkus;
            this.waktuPengambilan = waktuPengambilan;
            this.waktuPesan = waktuPesan;
            this.timestampServe = timestampServe;
        }

        // Getters and setters (if needed)...
        public int getCustomerNumber() {
            return customerNumber;
        }

        public void setCustomerNumber(int customerNumber) {
            this.customerNumber = customerNumber;
        }

        public String getRincianPesanan() {
            return rincianPesanan;
        }

        public void setRincianPesanan(String rincianPesanan) {
            this.rincianPesanan = rincianPesanan;
        }

        public int getBungkus() {
            return bungkus;
        }

        public void setBungkus(int bungkus) {
            this.bungkus = bungkus;
        }

        public String getWaktuPengambilan() {
            return waktuPengambilan;
        }

        public void setWaktuPengambilan(String waktuPengambilan) {
            this.waktuPengambilan = waktuPengambilan;
        }

        public String getWaktuPesan() {
            return waktuPesan;
        }

        public void setWaktuPesan(String waktuPesan) {
            this.waktuPesan = waktuPesan;
        }

        public FieldValue getTimestampServe() {
            return timestampServe;
        }

        public void setTimestampServe(FieldValue timestampServe) {
            this.timestampServe = timestampServe;
        }
    }
}