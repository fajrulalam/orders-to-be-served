package com.example.orderstobeserved;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class RecentlyServedActivity extends AppCompatActivity {

    private static final String TAG_RS = "RecentlyServedActivity";
    private ArrayList<OrderBlock> orderBlockArrayList;
    private FloatingActionButton toggleActivityFab;
    private RecyclerView recyclerView;
    private RecyclerAdapter2 recyclerAdapter;
    private FirebaseFirestore fs;

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

        // Fetch recently served orders
        fetchRecentlyServed();

        // Single toggle FAB - switches back to MainActivity
        toggleActivityFab.setOnClickListener(v -> {
            Intent intent = new Intent(RecentlyServedActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }
    
    private void fetchRecentlyServed() {
        fs.collection("RecentlyServed").orderBy("timestampServe", Query.Direction.DESCENDING).limit(50).addSnapshotListener(new EventListener<QuerySnapshot>() {
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
                                        
                                        int quantity = item.containsKey("quantity") ?
                                                Integer.parseInt(String.valueOf(item.get("quantity"))) : 1;
                                        
                                        int preparedQuantity = item.containsKey("preparedQuantity") ?
                                                Integer.parseInt(String.valueOf(item.get("preparedQuantity"))) : quantity;
                                        
                                        String status = item.containsKey("status") ?
                                                String.valueOf(item.get("status")) : "completed";
                                        
                                        Log.d("RecentlyServed", "Item: " + namaPesanan + 
                                               " (" + orderType + ") - " + preparedQuantity + "/" + quantity + 
                                               " Status: " + status);
                                        
                                        // Create order item directly from the fields
                                        NewOrderItem orderItem = new NewOrderItem(
                                            namaPesanan,
                                            orderType,
                                            quantity,
                                            status
                                        );
                                        orderItem.setPreparedQuantity(preparedQuantity);
                                        orderItems.add(orderItem);
                                        
                                    } else {
                                        // This is Status collection format with dineInQuantity/takeAwayQuantity
                                        String namaPesanan = String.valueOf(item.get("namaPesanan"));
                                        
                                        // Get dineInQuantity and takeAwayQuantity
                                        int dineInQuantity = item.containsKey("dineInQuantity") ?
                                            Integer.parseInt(String.valueOf(item.get("dineInQuantity"))) : 0;
                                        
                                        int takeAwayQuantity = item.containsKey("takeAwayQuantity") ?
                                            Integer.parseInt(String.valueOf(item.get("takeAwayQuantity"))) : 0;
                                        
                                        // Create dine-in order item if quantity > 0
                                        if (dineInQuantity > 0) {
                                            NewOrderItem orderItem = new NewOrderItem(
                                                namaPesanan,
                                                "dine-in",
                                                dineInQuantity,
                                                "completed"
                                            );
                                            orderItem.setPreparedQuantity(dineInQuantity); // Mark as fully served
                                            orderItems.add(orderItem);
                                        }
                                        
                                        // Create take-away order item if quantity > 0
                                        if (takeAwayQuantity > 0) {
                                            NewOrderItem orderItem = new NewOrderItem(
                                                namaPesanan,
                                                "take-away",
                                                takeAwayQuantity,
                                                "completed"
                                            );
                                            orderItem.setPreparedQuantity(takeAwayQuantity); // Mark as fully served
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
                                    "completed"
                                );
                                orderItem.setPreparedQuantity(1); // Mark as served
                                orderItems.add(orderItem);
                            }
                            
                            // Format timestamp for display and calculate duration
                            String hourSecond = "";
                            String durationStr = "...";
                            
                            if (map.containsKey("waktuPesan")) {
                                Object waktuPesanObj = map.get("waktuPesan");
                                if (waktuPesanObj instanceof Timestamp) {
                                    // Handle Timestamp format
                                    Timestamp timestamp = (Timestamp) waktuPesanObj;
                                    Date date = timestamp.toDate();
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
}