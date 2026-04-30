package com.example.orderstobeserved;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth; // ADDED
import com.google.firebase.auth.FirebaseUser; // ADDED
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class RecentlyServedActivity extends AppCompatActivity {

    private static final String TAG_RS = "RecentlyServedActivity";
    private static final String DEFAULT_CANTEEN_ID = "canteen375_plazaUnipdu";
    private ArrayList<OrderBlock> orderBlockArrayList;
    private FloatingActionButton toggleActivityFab;
    private RecyclerView recyclerView;
    private RecyclerAdapter2 recyclerAdapter;
    private FirebaseFirestore fs;
    private FirebaseAuth mAuth; // ADDED

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
        
        setContentView(R.layout.activity_recently_served);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Not logged in, redirect to LoginActivity
            startActivity(new Intent(RecentlyServedActivity.this, LoginActivity.class));
            finish();
            return;
        }

        fs = FirebaseFirestore.getInstance();
        orderBlockArrayList = new ArrayList<>();

        // Initialize UI components
        toggleActivityFab = findViewById(R.id.toggleActivityFab);
        recyclerView = findViewById(R.id.recyclerView);

        // Set a LayoutManager for the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter with an empty list
        recyclerAdapter = new RecyclerAdapter2(RecentlyServedActivity.this, orderBlockArrayList);
        recyclerView.setAdapter(recyclerAdapter);

        // Show testing mode banner if active
        SharedPreferences prefs = getSharedPreferences("shared_prefs", MODE_PRIVATE);
        TextView testModeBanner = findViewById(R.id.testModeBanner);
        testModeBanner.setVisibility(
                TestingModeManager.isEnabled(prefs) ? View.VISIBLE : View.GONE);

        // Fetch recently served orders
        fetchRecentlyServed();

        ItemTouchHelper restoreSwipeHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= orderBlockArrayList.size()) {
                    return;
                }
                restoreOrderToStatus(position, orderBlockArrayList.get(position));
            }
        });
        restoreSwipeHelper.attachToRecyclerView(recyclerView);

        // Single toggle FAB - switches back to MainActivity
        toggleActivityFab.setOnClickListener(v -> {
            Intent intent = new Intent(RecentlyServedActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }
    
    private void fetchRecentlyServed() {
        SharedPreferences prefs = getSharedPreferences("shared_prefs", MODE_PRIVATE);
        fs.collection(TestingModeManager.col(prefs, "RecentlyServed"))
                .orderBy("timestampServe", Query.Direction.DESCENDING).limit(50).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.i("Error", "onEvent", error);
                    return;
                }

                if (value != null) {
                    List<DocumentSnapshot> snapshotList = value.getDocuments();
                    orderBlockArrayList.clear();
                    
                    for (DocumentSnapshot snapshot : snapshotList) {
                        Map<String, Object> map = snapshot.getData();

                        if (map == null) continue;
                        
                        try {
                            // Parse bungkus/take-away status
                            int bungkus = 0;
                            if (map.containsKey("bungkus_or_not")) {
                                bungkus = Integer.parseInt(String.valueOf(map.get("bungkus_or_not")));
                            } else if (map.containsKey("bungkus")) {
                                bungkus = Integer.parseInt(String.valueOf(map.get("bungkus")));
                            }
                            
                            if (bungkus == 2) continue; // Skip certain orders based on existing logic
                            
                            // Parse customer info
                            int customerNumber = Integer.parseInt(String.valueOf(map.get("customerNumber")));
                            String namaCustomer = map.containsKey("namaCustomer") ? 
                                    String.valueOf(map.get("namaCustomer")) : "Customer";
                            
                            // Get order items
                            ArrayList<NewOrderItem> orderItems = new ArrayList<>();
                            
                            // Handle orderItems array format for RecentlyServed collection
                            if (map.containsKey("orderItems") && map.get("orderItems") instanceof List) {
                                List<Map<String, Object>> orderItemsList = (List<Map<String, Object>>) map.get("orderItems");
                                
                                Log.d("RecentlyServed", "Found " + orderItemsList.size() + " order items");
                                
                                for (Map<String, Object> item : orderItemsList) {
                                    // Check if this is the RecentlyServed format with direct fields
                                    if (item.containsKey("namaPesanan") && item.containsKey("quantity") && 
                                        (item.containsKey("preparedQuantity") || item.containsKey("orderType"))) {
                                        
                                        // This is RecentlyServed format
                                        String namaPesanan = String.valueOf(item.get("namaPesanan"));
                                        String orderType = item.containsKey("orderType") ? 
                                                String.valueOf(item.get("orderType")) : "take-away";

                                        String customerNote = "";
                                        if (item.containsKey("customerNote") && item.get("customerNote") != null) {
                                            String cn = String.valueOf(item.get("customerNote")).trim();
                                            if (!cn.isEmpty() && !cn.equalsIgnoreCase("null")) {
                                                customerNote = cn;
                                            }
                                        }
                                        
                                        int quantity = item.containsKey("quantity") ?
                                                Integer.parseInt(String.valueOf(item.get("quantity"))) : 1;
                                        
                                        int preparedQuantity = item.containsKey("preparedQuantity") ?
                                                Integer.parseInt(String.valueOf(item.get("preparedQuantity"))) : quantity;
                                        
                                        String status = item.containsKey("status") ?
                                                String.valueOf(item.get("status")) : "completed";
                                        
                                        Log.d("RecentlyServed", "Item: " + namaPesanan + 
                                               " (" + orderType + ") - " + preparedQuantity + "/" + quantity + 
                                               " Status: " + status);

                                        ArrayList<SelectedOption> flatOpts = parseSelectedOptionsFromItemMap(item);
                                        NewOrderItem orderItem = new NewOrderItem(
                                            namaPesanan,
                                            orderType,
                                            quantity,
                                            status,
                                            flatOpts
                                        );
                                        orderItem.setCustomerNote(customerNote);
                                        orderItem.setPreparedQuantity(preparedQuantity);
                                        applyItemMetaFromFirestoreMap(item, orderItem);
                                        orderItems.add(orderItem);

                                    } else {
                                        // This is Status collection format with dineInQuantity/takeAwayQuantity
                                        String namaPesanan = String.valueOf(item.get("namaPesanan"));

                                        String customerNote = "";
                                        if (item.containsKey("customerNote") && item.get("customerNote") != null) {
                                            String cn = String.valueOf(item.get("customerNote")).trim();
                                            if (!cn.isEmpty() && !cn.equalsIgnoreCase("null")) {
                                                customerNote = cn;
                                            }
                                        }
                                        
                                        // Get dineInQuantity and takeAwayQuantity
                                        int dineInQuantity = item.containsKey("dineInQuantity") ?
                                            Integer.parseInt(String.valueOf(item.get("dineInQuantity"))) : 0;
                                        
                                        int takeAwayQuantity = item.containsKey("takeAwayQuantity") ?
                                            Integer.parseInt(String.valueOf(item.get("takeAwayQuantity"))) : 0;
                                        
                                        ArrayList<SelectedOption> statusFmtOpts = parseSelectedOptionsFromItemMap(item);

                                        // Create dine-in order item if quantity > 0
                                        if (dineInQuantity > 0) {
                                            NewOrderItem orderItem = new NewOrderItem(
                                                namaPesanan,
                                                "dine-in",
                                                dineInQuantity,
                                                "completed",
                                                statusFmtOpts
                                            );
                                            orderItem.setCustomerNote(customerNote);
                                            int dineInPrepared = dineInQuantity;
                                            if (item.containsKey("dineInPreparedQuantity")) {
                                                try {
                                                    dineInPrepared = Integer.parseInt(
                                                            String.valueOf(item.get("dineInPreparedQuantity")));
                                                } catch (NumberFormatException ignored) {
                                                    dineInPrepared = dineInQuantity;
                                                }
                                            }
                                            orderItem.setPreparedQuantity(dineInPrepared);
                                            applyItemMetaFromFirestoreMap(item, orderItem);
                                            orderItems.add(orderItem);
                                        }

                                        // Create take-away order item if quantity > 0
                                        if (takeAwayQuantity > 0) {
                                            NewOrderItem orderItem = new NewOrderItem(
                                                namaPesanan,
                                                "take-away",
                                                takeAwayQuantity,
                                                "completed",
                                                statusFmtOpts
                                            );
                                            orderItem.setCustomerNote(customerNote);
                                            int takeAwayPrepared = takeAwayQuantity;
                                            if (item.containsKey("takeAwayPreparedQuantity")) {
                                                try {
                                                    takeAwayPrepared = Integer.parseInt(
                                                            String.valueOf(item.get("takeAwayPreparedQuantity")));
                                                } catch (NumberFormatException ignored) {
                                                    takeAwayPrepared = takeAwayQuantity;
                                                }
                                            }
                                            orderItem.setPreparedQuantity(takeAwayPrepared);
                                            applyItemMetaFromFirestoreMap(item, orderItem);
                                            orderItems.add(orderItem);
                                        }
                                    }
                                }
                            } else if (map.containsKey("rincianPesanan")) {
                                // Fallback to old rincianPesanan format 
                                String rincianPesanan = map.get("rincianPesanan").toString();
                                NewOrderItem orderItem = new NewOrderItem(
                                    rincianPesanan,
                                    bungkus == 1 ? "take-away" : "dine-in",
                                    1,
                                    "completed",
                                    null
                                );
                                orderItem.setPreparedQuantity(1); // Mark as served
                                orderItems.add(orderItem);
                            }
                            
                            // Format timestamp for display and calculate duration
                            String hourSecond = "";
                            String durationStr = "...";
                            long orderTimestampMs = 0;

                            if (map.containsKey("waktuPesan")) {
                                Object waktuPesanObj = map.get("waktuPesan");
                                if (waktuPesanObj instanceof Timestamp) {
                                    // Handle Timestamp format
                                    Timestamp timestamp = (Timestamp) waktuPesanObj;
                                    Date date = timestamp.toDate();
                                    orderTimestampMs = date.getTime();
                                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
                                    sdf.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Asia/Jakarta")));
                                    hourSecond = sdf.format(date);

                                    // Calculate duration if timestampServe is available
                                    if (map.containsKey("timestampServe") && map.get("timestampServe") instanceof Timestamp) {
                                        Timestamp serveTimestamp = (Timestamp) map.get("timestampServe");
                                        long durationSeconds = serveTimestamp.getSeconds() - timestamp.getSeconds();
                                        int minutes = (int) (durationSeconds / 60);
                                        int seconds = (int) (durationSeconds % 60);
                                        durationStr = String.format("%02d:%02d", minutes, seconds);
                                    }
                                } else {
                                    // Fallback to legacy timestamp format
                                    try {
                                        String waktuPesan = waktuPesanObj.toString();
                                        waktuPesan = waktuPesan.substring(waktuPesan.indexOf("=")+1, waktuPesan.indexOf(","));
                                        int waktuPesan_int = Integer.parseInt(waktuPesan);
                                        orderTimestampMs = waktuPesan_int * 1000L;

                                        Date date = new Date(waktuPesan_int * 1000);
                                        SimpleDateFormat sdf = new SimpleDateFormat("EEEE,MMMM d,yyyy HH:mm:ss", Locale.ENGLISH);
                                        sdf.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Asia/Jakarta")));
                                        String formattedDate = sdf.format(date);
                                        hourSecond = formattedDate.substring(formattedDate.length()-8, formattedDate.length()-3);
                                        
                                        // Calculate duration if timestampServe is available
                                        if (map.containsKey("timestampServe")) {
                                            String waktuServe = map.get("timestampServe").toString();
                                            waktuServe = waktuServe.substring(waktuServe.indexOf("=")+1, waktuServe.indexOf(","));
                                            int waktuServe_int = Integer.parseInt(waktuServe);
                                            
                                            int duration = waktuServe_int - waktuPesan_int;
                                            int second = duration % 60;
                                            int minute = duration / 60;
                                            durationStr = String.format("%02d:%02d", minute, second);
                                        }
                                    } catch (Exception e) {
                                        Log.e("ParseError", "Error parsing legacy timestamp", e);
                                    }
                                }
                            }
                            
                            // Get waktuPengambilan if available
                            String waktuPengambilan = map.containsKey("waktuPengambilan") ? 
                                    String.valueOf(map.get("waktuPengambilan")) : "Tidak Memesan";
                            
                            // Debug log for order items
                            Log.d("ServedOrders", "Customer #" + customerNumber + " has " + orderItems.size() + " items");
                            for (NewOrderItem item : orderItems) {
                                Log.d("ServedOrders", "Item: " + item.getNamaPesanan() + 
                                      " (" + item.getOrderType() + ") - " + 
                                      item.getPreparedQuantity() + "/" + item.getQuantity());
                            }
                            
                            // Create OrderBlock with servingTime and add to list
                            OrderBlock orderBlock = new OrderBlock(
                                    bungkus,
                                    customerNumber,
                                    namaCustomer,
                                    orderItems,
                                    waktuPengambilan,
                                    hourSecond,  // Display time
                                    durationStr  // Serving duration
                            );

                            orderBlock.setRecentlyServedDocumentId(snapshot.getId());
                            if (map.containsKey("sourceStatusDocumentId") && map.get("sourceStatusDocumentId") != null) {
                                String sid = String.valueOf(map.get("sourceStatusDocumentId")).trim();
                                if (!sid.isEmpty() && !sid.equalsIgnoreCase("null")) {
                                    orderBlock.setSourceStatusDocumentId(sid);
                                }
                            }

                            int totalVal = 0;
                            if (map.containsKey("total")) {
                                try {
                                    totalVal = Integer.parseInt(String.valueOf(map.get("total")));
                                } catch (NumberFormatException ignored) {
                                    totalVal = 0;
                                }
                            }
                            orderBlock.setTotal(totalVal);
                            orderBlock.setCanteenId(map.get("canteenId") == null ? ""
                                    : String.valueOf(map.get("canteenId")));
                            orderBlock.setTransactionMethod(map.get("transactionMethod") == null ? ""
                                    : String.valueOf(map.get("transactionMethod")));
                            orderBlock.setPaymentMethod(map.get("paymentMethod") == null ? ""
                                    : String.valueOf(map.get("paymentMethod")));
                            boolean isClosedVal = true;
                            if (map.containsKey("isClosed")) {
                                Object isClosedObj = map.get("isClosed");
                                if (isClosedObj instanceof Boolean) {
                                    isClosedVal = (Boolean) isClosedObj;
                                } else if (isClosedObj != null) {
                                    isClosedVal = Boolean.parseBoolean(String.valueOf(isClosedObj));
                                }
                            }
                            orderBlock.setClosed(isClosedVal);
                            orderBlock.setCustomerPhone(map.get("customerPhone") == null ? ""
                                    : String.valueOf(map.get("customerPhone")));
                            boolean isMemberVal = false;
                            if (map.containsKey("isMember")) {
                                Object isMemberObj = map.get("isMember");
                                if (isMemberObj instanceof Boolean) {
                                    isMemberVal = (Boolean) isMemberObj;
                                } else if (isMemberObj != null) {
                                    isMemberVal = Boolean.parseBoolean(String.valueOf(isMemberObj));
                                }
                            }
                            orderBlock.setMember(isMemberVal);
                            orderBlock.setMemberId(map.get("memberId") == null ? ""
                                    : String.valueOf(map.get("memberId")));

                            long takeAwayFeeVal = 0;
                            if (map.containsKey("takeAwayFee")) {
                                try {
                                    takeAwayFeeVal = Long.parseLong(String.valueOf(map.get("takeAwayFee")));
                                } catch (NumberFormatException ignored) {}
                            }
                            orderBlock.setTakeAwayFee(takeAwayFeeVal);

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

                            orderBlock.setOrderTimestamp(orderTimestampMs);

                            orderBlockArrayList.add(orderBlock);
                        } catch (Exception e) {
                            Log.e("ParseError", "Error parsing served order data: " + e.getMessage(), e);
                        }
                    }

                    recyclerAdapter.notifyDataSetChanged();
                } else {
                    Log.e("NULL", "onEvent: query snapshot was null");
                }
            }
        });
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop all running timers to avoid memory leaks
        if (recyclerAdapter != null) {
            recyclerAdapter.stopAllTimers();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop all running timers to avoid memory leaks
        if (recyclerAdapter != null) {
            recyclerAdapter.stopAllTimers();
        }
    }

    private static ArrayList<SelectedOption> parseSelectedOptionsFromItemMap(Map<String, Object> itemMap) {
        ArrayList<SelectedOption> selectedOptions = new ArrayList<>();
        Object selectedOptionsObj = itemMap.get("selectedOptions");
        if (!(selectedOptionsObj instanceof List)) {
            return selectedOptions;
        }
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
                    try {
                        priceAdj = Integer.parseInt(String.valueOf(priceAdjObj));
                    } catch (NumberFormatException ignored) {
                        priceAdj = 0;
                    }
                }
                selectedOptions.add(new SelectedOption(optionId, optionName, groupId, groupName, priceAdj));
            }
        }
        return selectedOptions;
    }

    private static void applyItemMetaFromFirestoreMap(Map<String, Object> itemMap, NewOrderItem orderItem) {
        boolean isMakanan = true;
        if (itemMap.containsKey("isMakanan")) {
            Object isMakananObj = itemMap.get("isMakanan");
            if (isMakananObj instanceof Boolean) {
                isMakanan = (Boolean) isMakananObj;
            } else if (isMakananObj != null) {
                isMakanan = Boolean.parseBoolean(String.valueOf(isMakananObj));
            }
        }
        orderItem.setIsMakanan(isMakanan);
        int harga = 0;
        if (itemMap.containsKey("harga")) {
            try {
                harga = Integer.parseInt(String.valueOf(itemMap.get("harga")));
            } catch (NumberFormatException ignored) {
                harga = 0;
            }
        }
        orderItem.setHarga(harga);
    }

    private void restoreOrderToStatus(int position, OrderBlock order) {
        SharedPreferences prefs = getSharedPreferences("shared_prefs", MODE_PRIVATE);
        String sourceId = order.getSourceStatusDocumentId();
        String rsId = order.getRecentlyServedDocumentId();

        if (sourceId == null || sourceId.isEmpty()) {
            Toast.makeText(this,
                    "Tidak dapat dikembalikan (riwayat lama tanpa tautan Status).",
                    Toast.LENGTH_LONG).show();
            recyclerAdapter.notifyItemChanged(position);
            return;
        }
        if (rsId == null || rsId.isEmpty()) {
            recyclerAdapter.notifyItemChanged(position);
            return;
        }

        String statusCol = TestingModeManager.col(prefs, "Status");
        String rsCol = TestingModeManager.col(prefs, "RecentlyServed");
        DocumentReference statusRef = fs.collection(statusCol).document(sourceId);
        DocumentReference rsRef = fs.collection(rsCol).document(rsId);

        statusRef.get().addOnSuccessListener(snap -> {
            if (snap.exists()) {
                Toast.makeText(this,
                        "Slot Status sudah dipakai pesanan lain. Tidak dapat dikembalikan.",
                        Toast.LENGTH_LONG).show();
                recyclerAdapter.notifyItemChanged(position);
                return;
            }
            Map<String, Object> data = buildStatusPayloadForRestore(order);
            WriteBatch batch = fs.batch();
            batch.set(statusRef, data);
            batch.delete(rsRef);
            batch.commit()
                    .addOnSuccessListener(unused -> Toast.makeText(this,
                            "Pesanan dikembalikan ke antrian",
                            Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> {
                        Log.e(TAG_RS, "restore failed", e);
                        Toast.makeText(this,
                                "Gagal mengembalikan: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        recyclerAdapter.notifyItemChanged(position);
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG_RS, "restore get Status failed", e);
            Toast.makeText(this,
                    "Gagal memeriksa Status: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            recyclerAdapter.notifyItemChanged(position);
        });
    }

    private Map<String, Object> buildStatusPayloadForRestore(OrderBlock order) {
        Map<String, Object> data = new HashMap<>();
        long ms = order.getOrderTimestamp();
        Timestamp waktuPesanTs;
        if (ms > 0) {
            waktuPesanTs = new Timestamp(ms / 1000, (int) ((ms % 1000) * 1_000_000));
        } else {
            waktuPesanTs = Timestamp.now();
        }
        data.put("waktuPesan", waktuPesanTs);

        String canteenId = order.getCanteenId();
        if (canteenId == null || canteenId.trim().isEmpty() || "null".equalsIgnoreCase(canteenId)) {
            canteenId = DEFAULT_CANTEEN_ID;
        }
        data.put("canteenId", canteenId);
        data.put("customerNumber", order.getCustomerNumber());
        data.put("namaCustomer", order.getNamaCustomer() != null ? order.getNamaCustomer() : "");
        data.put("bungkus", order.getBungkus());
        data.put("waktuPengambilan", order.getWaktuPengambilan() != null ? order.getWaktuPengambilan() : "");
        data.put("total", order.getTotal());
        data.put("orderItems", StatusOrderItemsBuilder.toFirestoreArrayList(order.getOrderItems()));
        data.put("transactionMethod", order.getTransactionMethod() != null ? order.getTransactionMethod() : "");
        data.put("paymentMethod", order.getPaymentMethod() != null ? order.getPaymentMethod() : "");
        data.put("isClosed", order.isClosed());
        data.put("customerPhone", order.getCustomerPhone() != null ? order.getCustomerPhone() : "");
        data.put("isMember", order.isMember());
        data.put("memberId", order.getMemberId() != null ? order.getMemberId() : "");
        data.put("takeAwayFee", order.getTakeAwayFee());
        if (order.getOrderHistory() != null) {
            data.put("orderHistory", order.getOrderHistory());
        }
        return data;
    }
}