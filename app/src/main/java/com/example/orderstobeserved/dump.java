package com.example.orderstobeserved;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

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


}
