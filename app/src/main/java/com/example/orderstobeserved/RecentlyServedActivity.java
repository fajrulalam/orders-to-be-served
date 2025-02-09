package com.example.orderstobeserved;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecentlyServedActivity extends AppCompatActivity {

    private ArrayList<OrderBlock> orderBlockArrayList;
    private TextView jumlahPesanan;
    private RelativeLayout halamanPesananButton;
    private ImageButton mainActivityButton;
    private RecyclerView recyclerView;
    private RecyclerAdapter recyclerAdapter;
    private FirebaseFirestore fs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recently_served);

        orderBlockArrayList = new ArrayList<>();
        fs = FirebaseFirestore.getInstance();

        halamanPesananButton = findViewById(R.id.halamanPesananButton);
        mainActivityButton = findViewById(R.id.mainMenuButton);
        jumlahPesanan = findViewById(R.id.jumlahPesanan);
        recyclerView = findViewById(R.id.recyclerView);

        fs.collection("RecentyServed")
                .limit(10)
                .orderBy("timestampServe", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("RecentlyServed", "Error fetching data", error);
                            return;
                        }
                        if (value != null) {
                            List<DocumentSnapshot> snapshotList = value.getDocuments();
                            orderBlockArrayList.clear();
                            for (DocumentSnapshot snapshot : snapshotList) {
                                Map<String, Object> map = snapshot.getData();
                                try {
                                    // Parse customerNumber
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
                                    String waktuPesan = map.get("waktuPesan") == null ? "" : String.valueOf(map.get("waktuPesan"));

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
                                                int total = 0;
                                                Object totalObj = itemMap.get("total");
                                                if (totalObj != null && !String.valueOf(totalObj).trim().isEmpty() &&
                                                        !String.valueOf(totalObj).equalsIgnoreCase("null")) {
                                                    try {
                                                        total = Integer.parseInt(String.valueOf(totalObj));
                                                    } catch (Exception e) {
                                                        total = 0;
                                                    }
                                                }
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

                                    // Create the NewPesanan object
                                    OrderBlock orderBlock = new OrderBlock(bungkus, customerNumber, namaCustomer, newOrderItems, waktuPengambilan, waktuPesan);
                                    orderBlockArrayList.add(orderBlock);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(getApplicationContext(), "Refreshing...", Toast.LENGTH_SHORT).show();
                                    Intent refresh = new Intent(getApplicationContext(), MainActivity.class);
                                    startActivity(refresh);
                                    finish();
                                }
                            }
                            recyclerAdapter = new RecyclerAdapter(orderBlockArrayList);
                            recyclerView.setAdapter(recyclerAdapter);
                        }
                    }
                });

        // Navigation button listeners
        mainActivityButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecentlyServedActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        halamanPesananButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecentlyServedActivity.this, Pesanan.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }
}