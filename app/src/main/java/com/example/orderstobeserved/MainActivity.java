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
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
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
    private SwipeMenuListView swipeMenuListView;
    private ArrayList<Integer> mCustomerNumber;
    private ArrayList<String> mOrders;
    private ArrayList<String> mQuantity;

    private ArrayList<Integer> NewCustomerNumber;
    private ArrayList<String> NewOrders;
    private ArrayList<String> NewQuantity;
    private ArrayList<String> NewBungkusArrayList;
//    private TextView totalHariIniTextView;
    private DatabaseReference reff;
//    private Query reffToday;
    Query query;
    Query query2;
    MyAdapter adapter;
    Query nestedQuery;
    Query query_udateStatus;

    RecyclerView recyclerView;
    RecyclerAdapter recyclerAdapter;

    DividerItemDecoration dividerItemDecoration;
    ItemTouchHelper itemTouchHelper;

    SwipeRefreshLayout swipeRefreshLayout;

    FirebaseFirestore fs;
    Map<String, Object> pesanan = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        reff = FirebaseDatabase.getInstance("https://point-of-sales-app-25e2b-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("TransacationStatus");
        query = reff.orderByChild("status").equalTo("Serving");
        query2 = reff.child("status").equalTo("Serving");
        query.addListenerForSingleValueEvent(valueEventListener);

        //Test Firestore
        fs = FirebaseFirestore.getInstance();

        fs.collection("Status").whereEqualTo("status", "Serving").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error !=null) {
                    Log.e(TAG, "onEvent", error);
                    return;
                }
                if (value != null){
                    List<DocumentSnapshot> snapshotList = value.getDocuments();
                    for (DocumentSnapshot snapshot : snapshotList) {

                        Map<String, Object> map = (Map<String, Object>) snapshot.getData();
                        Object customerNumber_object = map.get("customerNumber");
                        String customerNumber_string = (String.valueOf(customerNumber_object));
                        Log.i("Customer number:", customerNumber_string);

                        try {
                            int customerNumber_int = Integer.parseInt(customerNumber_string);
                            if(!NewCustomerNumber.contains(customerNumber_int)) {
                                Object pesanan_object = map.get("itemID");
                                String pesanan_String = (String.valueOf(pesanan_object));
                                Object quantity_object = map.get("quantity");
                                String quantity_string = (String.valueOf(quantity_object));
                                Object bungkus_object = map.get("bungkus");
                                String bungkus_string = String.valueOf(bungkus_object);
                                List<String> itemID_uncombined = Arrays.asList(pesanan_String.split("\\s*,\\s"));
                                List<String> quantity_uncombined = Arrays.asList(quantity_string.split("\\s*,\\s"));
                                Log.i("Quantity", quantity_string);
                                int i = 0;
                                String item_quantity_combined = "";
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
                                NewCustomerNumber.add(Integer.parseInt(customerNumber_string));
                                NewOrders.add(item_quantity_combined);
                                NewQuantity.add(quantity_string);
                                NewBungkusArrayList.add(bungkus_string);
                                Log.i("Bungkus", "" + bungkus_string);

                            } else {
                                Log.i("Bug", "sudah tersaring");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Refreshing...", Toast.LENGTH_SHORT).show();
                            Intent refresh = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(refresh);//Start the same Activity
                            finish(); //finish Activity.

                        }

                        Log.i("CustomerNumber Size",  ""+ NewCustomerNumber.size());

                    }
                    recyclerAdapter = new RecyclerAdapter(NewCustomerNumber, NewOrders, NewQuantity, NewBungkusArrayList);
                    recyclerView.setAdapter(recyclerAdapter);

                } else {
                    Log.e(TAG, "onEvent: query snapshot was null");
                }
            }
        });



//        Add Value Listener
//        reff.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.i("Hello", "hi");
//                        if (dataSnapshot.exists()) {
//
//                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//                                Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
//                                Object customerNumber_object = map.get("customerNumber");
//                                String customerNumber_string = (String.valueOf(customerNumber_object));
//                                Log.i("Customer number:", customerNumber_string);
//
//                                try {
//                                    int customerNumber_int = Integer.parseInt(customerNumber_string);
//                                    if(!NewCustomerNumber.contains(customerNumber_int)) {
//                                        Object pesanan_object = map.get("itemID");
//                                        String pesanan_String = (String.valueOf(pesanan_object));
//                                        Object quantity_object = map.get("quantity");
//                                        String quantity_string = (String.valueOf(quantity_object));
//                                        Object bungkus_object = map.get("bungkus");
//                                        String bungkus_string = String.valueOf(bungkus_object);
//                                        List<String> itemID_uncombined = Arrays.asList(pesanan_String.split("\\s*,\\s"));
//                                        List<String> quantity_uncombined = Arrays.asList(quantity_string.split("\\s*,\\s"));
//                                        Log.i("Quantity", quantity_string);
//                                        int i = 0;
//                                        String item_quantity_combined = "";
//                                        while (i<itemID_uncombined.size()) {
//                                            String item_container = itemID_uncombined.get(i);
//                                            String quantiy_container = quantity_uncombined.get(i);
//                                            if (i == itemID_uncombined.size() -1) {
//                                                item_quantity_combined += item_container + " (" + quantiy_container + ")";
//                                            } else {
//                                                item_quantity_combined += item_container + " (" + quantiy_container + ") , ";
//                                            }
//                                            i++;
//                                        }
//                                        NewCustomerNumber.add(Integer.parseInt(customerNumber_string));
//                                        NewOrders.add(item_quantity_combined);
//                                        NewQuantity.add(quantity_string);
//                                        NewBungkusArrayList.add(bungkus_string);
//                                        Log.i("Bungkus", "" + bungkus_string);
//
//                                    } else {
//                                        Log.i("Bug", "sudah tersaring");
//                                    }
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                    Toast.makeText(getApplicationContext(), "Refreshing...", Toast.LENGTH_SHORT).show();
//                                    Intent refresh = new Intent(getApplicationContext(), MainActivity.class);
//                                    startActivity(refresh);//Start the same Activity
//                                    finish(); //finish Activity.
//
//                                }
//
//                                Log.i("CustomerNumber Size",  ""+ NewCustomerNumber.size());
//                            }
//                            recyclerAdapter = new RecyclerAdapter(NewCustomerNumber, NewOrders, NewQuantity, NewBungkusArrayList);
//                            recyclerView.setAdapter(recyclerAdapter);
//
//                        }
//                    }
//                },100);
//
//
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });

//        reff.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                NewQuantity.clear();
//                NewOrders.clear();
//                NewQuantity.clear();
//
//
//                NewCustomerNumber.add(customerNumber_string);
//                NewOrders.add(pesanan_String);
//                NewQuantity.add(quantity_string);
//                recyclerAdapter = new RecyclerAdapter(NewCustomerNumber, NewOrders, NewQuantity);
//                recyclerView.setAdapter(recyclerAdapter);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });


        //Add data dummy
        mCustomerNumber = new ArrayList<>();
        mOrders = new ArrayList<>();
        mQuantity = new ArrayList<>();

        NewCustomerNumber = new ArrayList<>();
        NewOrders = new ArrayList<>();
        NewQuantity = new ArrayList<>();
        NewBungkusArrayList = new ArrayList<>();



        mCustomerNumber.add(1);
        mCustomerNumber.add(2);
        mCustomerNumber.add(3);

        mOrders.add("Tes 1, Nasi Ayam, Bakso, Es Teh, Es jeruk");
        mOrders.add("Siomay, Tes 2, Bakso, Es Teh, Es jeruk");
        mOrders.add("Siomay, Nasi Ayam, Tes 3, Es Teh, Es jeruk");

        mQuantity.add("2");
        mQuantity.add("1");
        mQuantity.add("4");

        //SwipeMenuListView
//        swipeMenuListView =  (SwipeMenuListView) findViewById(R.id.listView);

//        adapter = new MyAdapter(this, mCustomerNumber, mOrders);
//        swipeMenuListView.setAdapter(adapter);
        recyclerView = findViewById(R.id.recyclerView);
        dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(getApplicationContext().getResources().getDrawable(R.drawable.line_divider));
        itemTouchHelper = new ItemTouchHelper(simpleCallback);



        itemTouchHelper.attachToRecyclerView(recyclerView);

        Log.i("CustomerNumber DH",  ""+ NewCustomerNumber.size());
        recyclerAdapter = new RecyclerAdapter(NewCustomerNumber, NewOrders, NewQuantity, NewBungkusArrayList);
        recyclerView.setAdapter(recyclerAdapter);







    }

    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
//                    NewCustomerNumber.remove(position);
//                    NewOrders.remove(position);
//                    recyclerAdapter.notifyDataSetChanged();
                HashMap status_update = new HashMap();
                status_update.put("status", "Served");
                String customerNumberToBeRemoved = String.valueOf(NewCustomerNumber.get(position));
                String itemIDToBeRemoved = String.valueOf(NewOrders.get(position));
                String quantityToBeRemoved = String.valueOf(NewQuantity.get(position));
                reff.child(customerNumberToBeRemoved).removeValue();
                fs.collection("Status").document(customerNumberToBeRemoved).update("status", "served").addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(getApplicationContext(), customerNumberToBeRemoved, Toast.LENGTH_SHORT).show();
                    }
                });
            NewCustomerNumber.remove(position);
            NewOrders.remove(position);
            NewBungkusArrayList.remove(position);
            recyclerAdapter.notifyDataSetChanged();

//                reff.child(customerNumberToBeRemoved).child("status").setValue("Served");
//                reff.child(customerNumberToBeRemoved).child("customerNumber").setValue(Integer.parseInt(customerNumberToBeRemoved));
//                reff.child(customerNumberToBeRemoved).child("itemID").setValue(itemIDToBeRemoved);
//                reff.child(customerNumberToBeRemoved).child("quantity").setValue(quantityToBeRemoved);
//                query_udateStatus = reff.orderByChild("customerNumber").equalTo(NewCustomerNumber.get(position));


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

        ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (dataSnapshot.exists()) {

                        //Get the raw data
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                            Object customerNumber_object = map.get("customerNumber");
                            String customerNumber_string = (String.valueOf(customerNumber_object));
                            Log.i("Customer number:", customerNumber_string);
                            int customerNumber_int = Integer.parseInt(customerNumber_string);
                            if(!NewCustomerNumber.contains(customerNumber_int)) {
                                Object pesanan_object = map.get("itemID");
                                String pesanan_String = (String.valueOf(pesanan_object));

                                Object quantity_object = map.get("quantity");
                                String quantity_string = (String.valueOf(quantity_object));

                                Object bungkus_object = map.get("bungkus");
                                String bungkus_string = String.valueOf(bungkus_object);

                                List<String> itemID_uncombined = Arrays.asList(pesanan_String.split("\\s*,\\s"));
                                List<String> quantity_uncombined = Arrays.asList(quantity_string.split("\\s*,\\s"));
                                Log.i("Quantity", quantity_string);
                                int i = 0;
                                String item_quantity_combined = "";
                                while (i<itemID_uncombined.size()) {
                                    String item_container = itemID_uncombined.get(i);
                                    String quantiy_container = quantity_uncombined.get(i);
                                    item_quantity_combined += item_container + "(" + quantiy_container + "), ";
                                    i++;
                                }
                                NewCustomerNumber.add(Integer.parseInt(customerNumber_string));
                                NewOrders.add(item_quantity_combined);
                                NewQuantity.add(quantity_string);
                                NewBungkusArrayList.add(bungkus_string);

                            } else {
                                Log.i("Bug", "sudah tersaring");
                            }
                        }

                        //Combining the item with the quantity

                    }
                }
            }, 5000);

//            recyclerAdapter = new RecyclerAdapter(NewCustomerNumber, NewOrders, NewQuantity);
//            recyclerView.setAdapter(recyclerAdapter);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };


    public void Refresh() {
        swipeRefreshLayout.setRefreshing(true);

        NewCustomerNumber.clear();
        NewOrders.clear();
        query.addListenerForSingleValueEvent(valueEventListener);
        recyclerAdapter = new RecyclerAdapter(NewCustomerNumber, NewOrders, NewQuantity, NewBungkusArrayList);
        recyclerView.setAdapter(recyclerAdapter);
//        NewCustomerNumber.add(1);
//        NewOrders.add("HALOO");
        recyclerView.setAdapter(recyclerAdapter);
//        swipeMenuListView.setAdapter(adapter);
//        recyclerAdapter.notifyDataSetChanged();


        swipeRefreshLayout.setRefreshing(false);
    }






}