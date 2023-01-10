package com.example.orderstobeserved;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecentlyServedActivity extends AppCompatActivity {

    private ArrayList<NewPesanan> newPesananArrayList;

    TextView jumlahPesanan;

    RelativeLayout halamanPesananButton;
    ImageButton mainActivityButton;

    RecyclerView recyclerView;
    RecyclerAdapter recyclerAdapter;

    DividerItemDecoration dividerItemDecoration;
    ItemTouchHelper itemTouchHelper;


    FirebaseFirestore fs;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recently_served);

        newPesananArrayList = new ArrayList<NewPesanan>();


        //Test Firestore
        fs = FirebaseFirestore.getInstance();
        halamanPesananButton = findViewById(R.id.halamanPesananButton);
        mainActivityButton = findViewById(R.id.mainMenuButton);
        jumlahPesanan = findViewById(R.id.jumlahPesanan);
        recyclerView = findViewById(R.id.recyclerView);

        fs.collection("RecentyServed").limit(10).orderBy("timestampServe", Query.Direction.DESCENDING).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error !=null) {
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

                        try {
                            customerNumber_int = Integer.parseInt(String.valueOf(customerNumber_object));
//                            if(!NewCustomerNumber.contains(customerNumber_int)) {
                            Object pesanan_object = map.get("rincianPesanan");
                            String pesanan_String = (String.valueOf(pesanan_object));
                            Object quantity_object = map.get("quantity");
                            String quantity_string = (String.valueOf(quantity_object));
                            Object bungkus_object = map.get("bungkus_or_not");
                            String bungkus_string = String.valueOf(bungkus_object);
                            int bungkus_int = Integer.parseInt(bungkus_string);
                            Object waktuPengambilan_object = map.get("waktuPengambilan");
                            String waktuPengambilan_string = String.valueOf(waktuPengambilan_object);
                            Log.i("Quantity", quantity_string);
                            int i = 0;
                            String waktuPesan = map.get("waktuPesan").toString();
                            String waktuServe = map.get("timestampServe").toString();


                            newPesananArrayList.add(
                                    new NewPesanan(customerNumber_int, pesanan_String, bungkus_int, waktuPengambilan_string, waktuPesan, waktuServe)
                            );

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Refreshing...", Toast.LENGTH_SHORT).show();
                            Intent refresh = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(refresh);//Start the same Activity
                            finish(); //finish Activity.

                        }


                    }
                    recyclerAdapter = new RecyclerAdapter(newPesananArrayList);
                    recyclerView.setAdapter(recyclerAdapter);

                } else {
                }
            }
        });


        mainActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                overridePendingTransition(0,0);
            }
        });

        halamanPesananButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Pesanan.class);
                startActivity(intent);
                overridePendingTransition(0,0);
            }
        });




    }
}