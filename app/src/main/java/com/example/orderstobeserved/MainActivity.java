package com.example.orderstobeserved;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SwipeMenuListView swipeMenuListView;
    private ArrayList<Integer> mCustomerNumber;
    private ArrayList<String> mOrders;
    private ArrayList<Integer> mQuantity;

    private ArrayList<Integer> NewCustomerNumber;
    private ArrayList<String> NewOrders;
    private ArrayList<Integer> NewQuantity;
//    private TextView totalHariIniTextView;
    private DatabaseReference reff;
//    private Query reffToday;
    Query query;
    MyAdapter adapter;
    Query nestedQuery;

    RecyclerView recyclerView;
    RecyclerAdapter recyclerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        reff = FirebaseDatabase.getInstance("https://point-of-sales-app-25e2b-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference().child("TransacationDetail");
        query = reff.orderByChild("status").equalTo("Serving");
        query.addListenerForSingleValueEvent(valueEventListener);






        //Add data dummy
        mCustomerNumber = new ArrayList<>();
        mOrders = new ArrayList<>();
        mQuantity = new ArrayList<>();

        NewCustomerNumber = new ArrayList<>();
        NewOrders = new ArrayList<>();
        NewQuantity = new ArrayList<>();

        mCustomerNumber.add(1);
        mCustomerNumber.add(2);
        mCustomerNumber.add(3);

        mOrders.add("Tes 1, Nasi Ayam, Bakso, Es Teh, Es jeruk");
        mOrders.add("Siomay, Tes 2, Bakso, Es Teh, Es jeruk");
        mOrders.add("Siomay, Nasi Ayam, Tes 3, Es Teh, Es jeruk");

        mQuantity.add(2);
        mQuantity.add(1);
        mQuantity.add(4);

        //SwipeMenuListView
//        swipeMenuListView =  (SwipeMenuListView) findViewById(R.id.listView);

//        adapter = new MyAdapter(this, mCustomerNumber, mOrders);
//        swipeMenuListView.setAdapter(adapter);
        recyclerView = findViewById(R.id.recyclerView);












    }

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
            if (dataSnapshot.exists()) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    Object noCustomer = map.get("noCustomer");
                    int noCustomerInt = Integer.parseInt(String.valueOf(noCustomer));
                    if(!NewCustomerNumber.contains(noCustomerInt)) {
                        NewCustomerNumber.add(noCustomerInt);
                        Log.i("CustomerID", ""+noCustomerInt);
                    }
                }
                int i = 0;
                while (i<NewCustomerNumber.size()) {
                    nestedQuery = reff.orderByChild("noCustomer").equalTo(NewCustomerNumber.get(i));
                    nestedQuery.addListenerForSingleValueEvent(valueEventListener1);
                    i++;
                    Log.i("query nested", "Berjalan");
                }


            } else {
                Log.i("Query kurang tepat", "Serving");
            }


        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };

    public void removeFirstIndex(){
        NewCustomerNumber.remove(0);
        NewOrders.remove(0);
        mQuantity.remove(0);
        adapter = new MyAdapter(getApplicationContext(), NewCustomerNumber, NewOrders);
        swipeMenuListView.setAdapter(adapter);
    }

    ValueEventListener valueEventListener1 = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            String pesanan = "";
            if (dataSnapshot.exists()) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    Object pesanan_single = map.get("itemID");
                    String pesanan_single_String = (String.valueOf(pesanan_single));
                    pesanan += pesanan_single_String +", ";
                    Log.i("Pesanan", pesanan);
                }
                NewOrders.add(pesanan);
//                Log.i("Pesanan", pesanan);
            }
            Log.i("Size New Orders", ""+ NewOrders.size());
//            adapter = new MyAdapter(getApplicationContext(), NewCustomerNumber, NewOrders);
//            swipeMenuListView.setAdapter(adapter);
            recyclerAdapter = new RecyclerAdapter(NewCustomerNumber, NewOrders);
            recyclerView.setAdapter(recyclerAdapter);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };



}