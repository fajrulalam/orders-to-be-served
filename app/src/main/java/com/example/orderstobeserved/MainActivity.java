package com.example.orderstobeserved;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ArrayList<OrderBlock> orderBlockArrayList;
    private TextView jumlahPesanan;
    private RelativeLayout halamanPesananButton;
    private ImageButton recentlyServedButton;
    private RecyclerView recyclerView;
//    private RecyclerAdapter recyclerAdapter;
    private RecyclerAdapter2 recyclerAdapter;
    private DividerItemDecoration dividerItemDecoration;
    private ItemTouchHelper itemTouchHelper;
    private FirebaseFirestore fs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        orderBlockArrayList = new ArrayList<>();
        fs = FirebaseFirestore.getInstance();

        halamanPesananButton = findViewById(R.id.halamanPesananButton);
        recentlyServedButton = findViewById(R.id.recentlyServed);
        jumlahPesanan = findViewById(R.id.jumlahPesanan);
        recyclerView = findViewById(R.id.recyclerView);

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

                                    OrderBlock orderBlock = new OrderBlock(bungkus, customerNumber, namaCustomer, newOrderItems, waktuPengambilan, waktuPesan);
                                    orderBlockArrayList.add(orderBlock);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            recyclerAdapter.notifyDataSetChanged();
                        } else {
                            Log.e(TAG, "onEvent: query snapshot was null");
                        }
                    }
                });

        // Listener for count (orders where bungkus == 2)
        fs.collection("Status")
                .whereEqualTo("bungkus", 2)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Error fetching count", error);
                            return;
                        }
                        if (value != null) {
                            int count = value.getDocuments().size();
                            if (count > 0) {
                                jumlahPesanan.setVisibility(View.VISIBLE);
                            }
                            jumlahPesanan.setText(String.valueOf(count));
                        }
                    }
                });

        dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(getApplicationContext().getResources().getDrawable(R.drawable.line_divider));
        itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        recyclerAdapter = new RecyclerAdapter2(MainActivity.this, orderBlockArrayList);
        recyclerView.setAdapter(recyclerAdapter);
        halamanPesananButton.setOnClickListener(v -> openBackEnd());
        recentlyServedButton.setOnClickListener(v -> {
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
            fs.collection("Status")
                    .document(customerNumberToBeRemoved)
                    .delete()
                    .addOnSuccessListener(unused ->
                            Toast.makeText(getApplicationContext(), "Order " + customerNumberToBeRemoved + " served", Toast.LENGTH_SHORT).show());

            // Build a summary string for the served order (example: "Item1 (total), Item2 (total)")
            StringBuilder combinedOrder = new StringBuilder();
            for (int i = 0; i < servedOrder.getOrderItems().size(); i++) {
                NewOrderItem item = servedOrder.getOrderItems().get(i);
                int totalQty = item.getQuantity();
                combinedOrder.append(item.getNamaPesanan())
                        .append(" (")
                        .append(totalQty)
                        .append(")");
                if (i < servedOrder.getOrderItems().size() - 1) {
                    combinedOrder.append(", ");
                }
            }

            RecentlyServed recentlyServed = new RecentlyServed(
                    servedOrder.getCustomerNumber(),
                    combinedOrder.toString(),
                    servedOrder.getBungkus(),
                    servedOrder.getWaktuPengambilan(),
                    servedOrder.getWaktuPesan(),
                    FieldValue.serverTimestamp()
            );
            fs.collection("RecentyServed").add(recentlyServed);
            orderBlockArrayList.remove(position);
            recyclerAdapter.notifyItemRemoved(position);
        }
    };

//    public String getDate() {
//        Long datetime = System.currentTimeMillis();
//        Timestamp timestamp = new Timestamp(datetime);
//        String date_full = String.valueOf(timestamp);
//        return date_full.substring(0, 10);
//    }

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