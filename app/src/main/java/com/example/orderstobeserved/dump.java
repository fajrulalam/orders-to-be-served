package com.example.orderstobeserved;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;

import androidx.annotation.NonNull;

//import com.baoyz.swipemenulistview.SwipeMenu;
//import com.baoyz.swipemenulistview.SwipeMenuCreator;
//import com.baoyz.swipemenulistview.SwipeMenuItem;
//import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class dump {

//    SwipeMenuCreator creator = new SwipeMenuCreator() {
//
//        @Override
//        public void create(SwipeMenu menu) {
//
//
//            // create "delete" item
//            SwipeMenuItem deleteItem = new SwipeMenuItem(
//                    getApplicationContext());
//            // set item background
//            deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
//                    0x3F, 0x25)));
//            // set item width
//            deleteItem.setWidth(145);
//            // set a icon
//            deleteItem.setIcon(R.drawable.ic_check);
//            // add to menu
//            menu.addMenuItem(deleteItem);
//        }
//    };
//
//// set creator
//        swipeMenuListView.setMenuCreator(creator);
//
//        swipeMenuListView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
//        @Override
//        public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
//            Log.i("Button clicked:", "Position " + position);
//            Log.i("Button clicked:", "Index " + index);
//            if (position == 0) {
//                try {
//                    removeFirstIndex();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            } else {
//                try {
//                    NewCustomerNumber.remove(position);
//                    NewOrders.remove(position);
//                    adapter.notifyDataSetChanged();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//
////                    mQuantity.remove(position);
//
//            }
//
//
//            // false : close the menu; true : not close the menu
//            return false;
//        }
//    });
//
//        swipeMenuListView.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);







//    ValueEventListener valueEventListener = new ValueEventListener() {
//        @Override
//        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//            if (dataSnapshot.exists()) {
//                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
//                    Object noCustomer = map.get("noCustomer");
//                    int noCustomerInt = Integer.parseInt(String.valueOf(noCustomer));
//                    if(!NewCustomerNumber.contains(noCustomerInt)) {
//                        NewCustomerNumber.add(noCustomerInt);
//                        Log.i("CustomerID", ""+noCustomerInt);
//                    }
//                }
//                int i = 0;
//                while (i<NewCustomerNumber.size()) {
//                    nestedQuery = reff.orderByChild("noCustomer").equalTo(NewCustomerNumber.get(i));
//                    nestedQuery.addListenerForSingleValueEvent(valueEventListener1);
//                    i++;
//                    Log.i("query nested", "Berjalan");
//                }
//
//
//            } else {
//                Log.i("Query kurang tepat", "Serving");
//            }
//
//
//
//
//        }
//
//        @Override
//        public void onCancelled(@NonNull DatabaseError error) {
//
//        }
//    };
//
//    public void removeFirstIndex(){
//        NewCustomerNumber.remove(0);
//        NewOrders.remove(0);
//        mQuantity.remove(0);
//        adapter = new MainActivity.MyAdapter(getApplicationContext(), NewCustomerNumber, NewOrders);
//        swipeMenuListView.setAdapter(adapter);
//    }
//
//    ValueEventListener valueEventListener1 = new ValueEventListener() {
//        @Override
//        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//            String pesanan = "";
//            if (dataSnapshot.exists()) {
//                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
//                    Object pesanan_single = map.get("itemID");
//                    String pesanan_single_String = (String.valueOf(pesanan_single));
//                    pesanan += pesanan_single_String +", ";
//                    Log.i("Pesanan", pesanan);
//                }
//
//                if (NewOrders.size() <= NewCustomerNumber.size()) {
//                    NewOrders.add(pesanan);
//                } else {
//                    Log.i("Bug!", "tapi udah tertolong");
//                }
////
//            }
//            Log.i("Size New Orders", ""+ NewOrders.size());
//            String NewCustomerNumbers = "";
//            for (int i = 0; i < NewCustomerNumber.size(); i++) {
//                String container = String.valueOf(NewCustomerNumber.get(i));
//                NewCustomerNumbers += container + ", ";
//            }
//            Log.i("List of Customer Number", NewCustomerNumbers);
//
//            String NewOrderss = "";
//            for (int i = 0; i < NewOrders.size(); i++) {
//                String container = String.valueOf(NewOrders.get(i));
//                NewOrderss += container + "|||  ";
//            }
//            Log.i("List of Orders", "Size: "+ NewOrders.size() + "->" +  NewOrderss);
//
//            adapter = new MainActivity.MyAdapter(getApplicationContext(), NewCustomerNumber, NewOrders);
//            recyclerView.setAdapter(recyclerAdapter);
////            swipeMenuListView.setAdapter(adapter);
////            recyclerView.addItemDecoration(dividerItemDecoration);
//
//        }
//
//        @Override
//        public void onCancelled(@NonNull DatabaseError error) {
//
//        }
//    };
//
//
//    public void Refresh(){
//        swipeRefreshLayout.setRefreshing(true);
//
//        NewCustomerNumber.clear();
//        NewOrders.clear();
//        query.addListenerForSingleValueEvent(valueEventListener);
//        recyclerAdapter = new RecyclerAdapter(NewCustomerNumber, NewOrders);
////        NewCustomerNumber.add(1);
////        NewOrders.add("HALOO");
//        recyclerView.setAdapter(recyclerAdapter);
////        swipeMenuListView.setAdapter(adapter);
////        recyclerAdapter.notifyDataSetChanged();
//
//
//        swipeRefreshLayout.setRefreshing(false);








//    ValueEventListener valueEventListener = new ValueEventListener() {
//        @Override
//        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//            if (dataSnapshot.exists()) {
//
//                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
//                    Object customerNumber_object = map.get("CustomerNumber");
//                    String customerNumber_string = (String.valueOf(customerNumber_object));
//                    Log.i("Customer number:", customerNumber_string);
//                    if(!NewCustomerNumber.contains(customerNumber_string)) {
//                        Object pesanan_object = map.get("itemID");
//                        String pesanan_String = (String.valueOf(pesanan_object));
//                        Object quantity_object = map.get("quantity");
//                        String quantity_string = (String.valueOf(quantity_object));
//
//                        NewCustomerNumber.add(customerNumber_string);
//                        NewOrders.add(pesanan_String);
//                        NewQuantity.add(quantity_string);
//                    }
//                }
//            }
//            recyclerAdapter = new RecyclerAdapter(NewCustomerNumber, NewOrders, NewQuantity);
//            recyclerView.setAdapter(recyclerAdapter);
//        }
//
//        @Override
//        public void onCancelled(@NonNull DatabaseError error) {
//
//        }
//    };
    }


