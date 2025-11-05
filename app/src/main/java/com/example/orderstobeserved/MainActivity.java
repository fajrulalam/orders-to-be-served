package com.example.orderstobeserved;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences; // ADDED
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.DisplayMetrics;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson; // ADDED
import com.google.gson.reflect.TypeToken; // ADDED

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
    private ArrayList<OrderBlock> orderBlockArrayList;
    private FloatingActionButton toggleActivityFab;
    private RecyclerView recyclerView;
    private boolean isToggling = false; // Prevent rapid toggle crashes
    // private RecyclerAdapter recyclerAdapter;
    private RecyclerAdapter2 recyclerAdapter;
    private DividerItemDecoration dividerItemDecoration;
    private ItemTouchHelper itemTouchHelper;
    private FirebaseFirestore fs;

    // ADDED: SharedPreferences and Gson for saving/loading data
    private SharedPreferences sharedPreferences;
    private Gson gson;

    // ADDED: Aggregation functionality
    private LinearLayout aggregationSection;
    private RecyclerView aggregationRecyclerView;
    private AggregationAdapter aggregationAdapter;
    private ArrayList<AggregatedItem> aggregatedItemsList;
    private ImageButton toggleAggregationButton;
    private FloatingActionButton showAggregationFab;
    private boolean isAggregationVisible = true;

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
        
        setContentView(R.layout.activity_main);

        fs = FirebaseFirestore.getInstance();

        // Initialize SharedPreferences and Gson
        sharedPreferences = getSharedPreferences("shared_prefs", MODE_PRIVATE);
        gson = new Gson();

        // LOAD the saved orderBlockArrayList (REMOVED the old "new ArrayList<>()" initialization)
        String json = sharedPreferences.getString("order_list", null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<OrderBlock>>() {}.getType();
            orderBlockArrayList = gson.fromJson(json, type);
        } else {
            orderBlockArrayList = new ArrayList<>();
        }

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

        // Listener for "Status" collection using the new data structure
        fs.collection("Status")
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
                            // Build a new list from the latest snapshot.
                            List<DocumentSnapshot> snapshotList = value.getDocuments();
                            ArrayList<OrderBlock> orderBlockArrayListComparator = new ArrayList<>();

                            for (DocumentSnapshot snapshot : snapshotList) {
                                Map<String, Object> map = snapshot.getData();
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
                                                // If the same item is ordered for both dine-in and take-away, they should be separate.
                                                if (dineInQuantity > 0) {
                                                    NewOrderItem dineInItem = new NewOrderItem(namaPesanan, "dine-in", dineInQuantity, status);
                                                    newOrderItems.add(dineInItem);
                                                }
                                                if (takeAwayQuantity > 0) {
                                                    NewOrderItem takeAwayItem = new NewOrderItem(namaPesanan, "take-away", takeAwayQuantity, status);
                                                    newOrderItems.add(takeAwayItem);
                                                }
                                            }
                                        }
                                    }
                                    // Create OrderBlock with timestamp for count-up timer
                                    OrderBlock orderBlock = new OrderBlock(bungkus, customerNumber, namaCustomer, newOrderItems, waktuPengambilan, waktuPesan, orderTimestampMs, total);
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
                            // Append any new OrderBlock from comparator that is not already in orderBlockArrayList.
                            for (OrderBlock comp : orderBlockArrayListComparator) {
                                boolean exists = false;
                                for (OrderBlock existing : orderBlockArrayList) {
                                    if (existing.getCustomerNumber() == comp.getCustomerNumber()) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    orderBlockArrayList.add(comp);
                                }
                            }

                            // ADDED: Save the updated list to SharedPreferences
                            String updatedJson = gson.toJson(orderBlockArrayList);
                            sharedPreferences.edit().putString("order_list", updatedJson).apply();

                            // Rebuild aggregation after data changes
                            rebuildAggregation();

                            // Finally, notify the adapter that the data has changed.
                            recyclerAdapter.notifyDataSetChanged();
                        } else {
                            Log.e(TAG, "onEvent: query snapshot was null");
                        }
                    }
                });

        itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        recyclerAdapter = new RecyclerAdapter2(MainActivity.this, orderBlockArrayList);
        recyclerView.setAdapter(recyclerAdapter);
        
        // Single toggle FAB - switches to RecentlyServedActivity
        toggleActivityFab.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), RecentlyServedActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }

    public void openBackEnd() {
        Intent intent = new Intent(this, Pesanan.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop all running timers to avoid memory leaks
        if (recyclerAdapter != null) {
            recyclerAdapter.stopAllTimers();
        }
    }

    // Rebuild aggregation from current orders
    private void rebuildAggregation() {
        Map<String, AggregatedItem> aggregationMap = new HashMap<>();

        // Aggregate items from all orders
        for (OrderBlock order : orderBlockArrayList) {
            ArrayList<NewOrderItem> items = order.getOrderItems();
            if (items != null) {
                for (NewOrderItem item : items) {
                    String key = AggregatedItem.createKey(item.getNamaPesanan(), item.getOrderType());
                    
                    AggregatedItem aggregatedItem = aggregationMap.get(key);
                    if (aggregatedItem == null) {
                        aggregatedItem = new AggregatedItem(item.getNamaPesanan(), item.getOrderType());
                        aggregationMap.put(key, aggregatedItem);
                    }
                    aggregatedItem.addItemReference(order.getCustomerNumber(), item);
                }
            }
        }

        // Convert to list and sort by total quantity (descending)
        aggregatedItemsList.clear();
        for (AggregatedItem item : aggregationMap.values()) {
            // Only show items that are not fully served
            if (!item.isFullyServed()) {
                aggregatedItemsList.add(item);
            }
        }

        // Sort by total quantity (descending)
        Collections.sort(aggregatedItemsList, new Comparator<AggregatedItem>() {
            @Override
            public int compare(AggregatedItem o1, AggregatedItem o2) {
                return Integer.compare(o2.getTotalQuantity(), o1.getTotalQuantity());
            }
        });

        // Update adapter
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
                // Increment this item
                orderItem.incrementPrepared();
                
                // Recalculate aggregation totals
                aggregatedItem.recalculateTotals();
                
                // Update both adapters
                recyclerAdapter.notifyDataSetChanged();
                
                // If the aggregated item is fully served, remove it from the list
                if (aggregatedItem.isFullyServed()) {
                    aggregatedItemsList.remove(aggregatedItem);
                }
                
                aggregationAdapter.notifyDataSetChanged();
                
                // Save to shared preferences
                String json = gson.toJson(orderBlockArrayList);
                sharedPreferences.edit().putString("order_list", json).apply();
                
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
            OrderBlock servedOrder = orderBlockArrayList.get(position);
            final String customerNumberToBeRemoved = String.valueOf(servedOrder.getCustomerNumber());

            // Remove the served order from the "Status" collection.
            fs.collection("Status")
                    .document(customerNumberToBeRemoved)
                    .delete()
                    .addOnSuccessListener(unused ->
                            Toast.makeText(getApplicationContext(), "Order " + customerNumberToBeRemoved + " served", Toast.LENGTH_SHORT).show());

            // Format the order items for RecentlyServed collection
            ArrayList<Map<String, Object>> formattedOrderItems = new ArrayList<>();
            for (NewOrderItem item : servedOrder.getOrderItems()) {
                Map<String, Object> formattedItem = new HashMap<>();
                formattedItem.put("namaPesanan", item.getNamaPesanan());
                formattedItem.put("orderType", item.getOrderType());
                formattedItem.put("quantity", item.getQuantity());
                formattedItem.put("preparedQuantity", item.getQuantity()); // Set prepared to full quantity for served orders
                formattedItem.put("status", "");  // Empty status field per requirement
                
                formattedOrderItems.add(formattedItem);
            }

            // Build a map with the exact same data as servedOrder plus a new field "timestampServe".
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("bungkus", servedOrder.getBungkus());
            orderData.put("customerNumber", servedOrder.getCustomerNumber());
            orderData.put("namaCustomer", servedOrder.getNamaCustomer());
            orderData.put("orderItems", formattedOrderItems);
            orderData.put("waktuPengambilan", servedOrder.getWaktuPengambilan());
            
            // Format waktuPesan as a Timestamp string for RecentlyServed collection
            long timestamp = servedOrder.getOrderTimestamp() / 1000; // Convert ms to seconds
            String formattedTimestamp = "Timestamp(seconds=" + timestamp + ", nanoseconds=317000000)";
            orderData.put("waktuPesan", formattedTimestamp);
            
            orderData.put("timestampServe", FieldValue.serverTimestamp());
            orderData.put("status", "Served");
            orderData.put("total", servedOrder.getTotal());

            // Add the map to the "RecentlyServed" collection.
            fs.collection("RecentlyServed").add(orderData);

            orderBlockArrayList.remove(position);
            recyclerAdapter.notifyItemRemoved(position);
            
            // Rebuild aggregation after removing order
            rebuildAggregation();
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