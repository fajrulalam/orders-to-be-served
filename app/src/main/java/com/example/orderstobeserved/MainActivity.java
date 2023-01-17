package com.example.orderstobeserved;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.sql.Ref;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";


    private ArrayList<NewPesanan> newPesananArrayList;

    TextView jumlahPesanan;

    RelativeLayout halamanPesananButton;
    ImageButton recentlyServedButton;

    RecyclerView recyclerView;
    RecyclerAdapter recyclerAdapter;

    DividerItemDecoration dividerItemDecoration;
    ItemTouchHelper itemTouchHelper;


    FirebaseFirestore fs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        getSupportActionBar().hide();


        newPesananArrayList = new ArrayList<NewPesanan>();

        //Test Firestore
        fs = FirebaseFirestore.getInstance();
        halamanPesananButton = findViewById(R.id.halamanPesananButton);
        recentlyServedButton = findViewById(R.id.recentlyServed);
        jumlahPesanan = findViewById(R.id.jumlahPesanan);
        recyclerView = findViewById(R.id.recyclerView);


        fs.collection("Status").orderBy("waktuPesan", Query.Direction.ASCENDING).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error !=null) { 
                    Log.e(TAG, "onEvent", error);
                    return;
                }
                if (value != null){
                    List<DocumentSnapshot> snapshotList = value.getDocuments();
                    newPesananArrayList.clear();
                    for (DocumentSnapshot snapshot : snapshotList) {

                        Map<String, Object> map = (Map<String, Object>) snapshot.getData();
                        Object customerNumber_object = map.get("customerNumber");
                        int customerNumber_int;

//                        Log.i("MAP UNCHECKED:", map.toString());

//                        try {
                        int bungkus = Integer.parseInt(String.valueOf(map.get("bungkus")));
                        if (bungkus != 2) {
                            customerNumber_int = Integer.parseInt(String.valueOf(customerNumber_object));
                            Object pesanan_object = map.get("itemID");
                            String pesanan_String = (String.valueOf(pesanan_object));
                            Object quantity_object = map.get("quantity");
                            String quantity_string = (String.valueOf(quantity_object));
                            Object bungkus_object = map.get("bungkus");
                            String bungkus_string = String.valueOf(bungkus_object);
                            int bungkus_int = Integer.parseInt(bungkus_string);
                            Object waktuPengambilan_object = map.get("waktuPengambilan");
                            String waktuPengambilan_string = String.valueOf(waktuPengambilan_object);
                            List<String> itemID_uncombined = Arrays.asList(pesanan_String.split("\\s*,\\s"));
                            List<String> quantity_uncombined = Arrays.asList(quantity_string.split("\\s*,\\s"));
                            Log.i("Quantity", quantity_string);
                            int i = 0;
                            String item_quantity_combined = "";
                            String waktuPesan = map.get("waktuPesan").toString();
                            Log.i("WaktuPesan", waktuPesan);
                            while (i<itemID_uncombined.size()) {
                                String item_container = itemID_uncombined.get(i);
                                String quantiy_container = quantity_uncombined.get(i);
                                if (i == itemID_uncombined.size() -1) {
                                    item_quantity_combined += item_container + " (" + quantiy_container + ")";
                                } else {
                                    item_quantity_combined += item_container + " (" + quantiy_container + ") , ";
                                }
                                i++;
                            }
                            Log.i("Bungkus_int", ""+bungkus_int);

                            newPesananArrayList.add(
                                    new NewPesanan(customerNumber_int, item_quantity_combined, bungkus_int, waktuPengambilan_string, waktuPesan,"")
                            );
                        }




                    }

                    recyclerAdapter.notifyDataSetChanged();

                } else {
                    Log.e(TAG, "onEvent: query snapshot was null");
                }
            }
        });

        fs.collection("Status").whereEqualTo("bungkus", 2).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e(TAG, "onEvent", error);
                    return;
                }
                if (value != null) {

                    List<DocumentSnapshot> snapshotList = value.getDocuments();
                    int berapaPesanan = 0;
                    for (DocumentSnapshot snapshot : snapshotList) {
                        if (snapshot.exists()){
                            jumlahPesanan.setVisibility(View.VISIBLE);
                        }
                        berapaPesanan += 1;


                    }
                    jumlahPesanan.setText(String.valueOf(berapaPesanan));

                }
            }
        });







        dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(getApplicationContext().getResources().getDrawable(R.drawable.line_divider));
        itemTouchHelper = new ItemTouchHelper(simpleCallback);



        itemTouchHelper.attachToRecyclerView(recyclerView);

        recyclerAdapter = new RecyclerAdapter(newPesananArrayList);
        recyclerView.setAdapter(recyclerAdapter);

        halamanPesananButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openBackEnd();
            }
        });

        recentlyServedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), RecentlyServedActivity.class);
                startActivity(intent);
                overridePendingTransition(0,0);
            }
        });







    }

    public void openBackEnd() {
        Intent intent = new Intent(this, Pesanan.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();

            HashMap status_update = new HashMap();
            status_update.put("status", "Served");
            String customerNumberToBeRemoved = String.valueOf(newPesananArrayList.get(position).customerNumber);

            fs.collection("Status").document(customerNumberToBeRemoved).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Toast.makeText(getApplicationContext(), customerNumberToBeRemoved, Toast.LENGTH_SHORT).show();
                }
            });

            RecentlyServed recentlyServed = new RecentlyServed(
                    newPesananArrayList.get(position).customerNumber,
                    newPesananArrayList.get(position).rincianPesanan,
                    newPesananArrayList.get(position).bungkus_or_not,
                    newPesananArrayList.get(position).waktuPengambilan,
                    newPesananArrayList.get(position).waktuPesan,
                    FieldValue.serverTimestamp()
            );
            fs.collection("RecentyServed").add(recentlyServed);
            newPesananArrayList.remove(position);

//            NewCustomerNumber.remove(position);
//            NewOrders.remove(position);
//            NewBungkusArrayList.remove(position);
//            NewWaktuPengambilan.remove(position);
            recyclerAdapter.notifyDataSetChanged();




        }
    };


    public String getDate() {
        Long datetime = System.currentTimeMillis();
        Timestamp timestamp = new Timestamp(datetime);
        String date_full = (String) String.valueOf(timestamp);
        String date = date_full.substring(0, 10);
        return date;
    }


    class MyAdapter extends ArrayAdapter<Integer> {

        Context context;
        ArrayList<Integer> rCustomerNumber;
        ArrayList<String> rOrders;
//        ArrayList<Integer> rQuantity;

        MyAdapter (Context context, ArrayList<Integer> customerNumber, ArrayList<String> orders) {
            super(context, R.layout.orders_to_be_served, R.id.noCustomerTextView, customerNumber);
            this.context = context;
            this.rCustomerNumber = customerNumber;
            this.rOrders = orders;
//            this.rQuantity = quantity;

        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = layoutInflater.inflate(R.layout.orders_to_be_served, parent, false);
            TextView noCustomerTextView = (TextView) row.findViewById(R.id.noCustomerTextView);
            TextView pesananTextView = (TextView) row.findViewById(R.id.pesananTextView);

            noCustomerTextView.setText("" +rCustomerNumber.get(position));
            pesananTextView.setText("" +rOrders.get(position));

            return row;

        }
    }


    public class RecentlyServed {
        int customerNumber;
        String rincianPesanan;
        int bungkus_or_not;
        String waktuPengambilan;
        String waktuPesan;
        FieldValue timestampServe;

        public RecentlyServed(int customerNumber, String rincianPesanan, int bungkus_or_not, String waktuPengambilan, String waktuPesan, FieldValue timestampServe) {
            this.customerNumber = customerNumber;
            this.rincianPesanan = rincianPesanan;
            this.bungkus_or_not = bungkus_or_not;
            this.waktuPengambilan = waktuPengambilan;
            this.waktuPesan = waktuPesan;
            this.timestampServe = timestampServe;
        }

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

        public int getBungkus_or_not() {
            return bungkus_or_not;
        }

        public void setBungkus_or_not(int bungkus_or_not) {
            this.bungkus_or_not = bungkus_or_not;
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