package com.example.rusheta.view.ui;


import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rusheta.R;
import com.example.rusheta.service.model.Chat;
import com.example.rusheta.view.adapter.MyChatsAdapter;
import com.example.rusheta.view.viewmodel.ChatViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private ChatViewModel chatViewModel;
    private RecyclerView recyclerView;
    private MyChatsAdapter myChatsAdapter;
    private ArrayList<Chat> contacts;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        chatViewModel.deleteAll();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        recyclerView = findViewById(R.id.chatRecyclerView);
        LinearLayoutManager myLinearLayoutManager = new LinearLayoutManager(MainActivity.this);
        recyclerView.setLayoutManager(myLinearLayoutManager);
        contacts = new ArrayList<>();

        //Set View Model
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        chatViewModel.getAllChats().observe(this, chats -> {
            contacts = (ArrayList<Chat>) chats;
            myChatsAdapter = new MyChatsAdapter(MainActivity.this, contacts);
            recyclerView.setAdapter(myChatsAdapter);
        });

        myChatsAdapter = new MyChatsAdapter(MainActivity.this, contacts);
        recyclerView.setAdapter(myChatsAdapter);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent i = new Intent(MainActivity.this, Contacts2Activity.class);
            startActivityForResult(i, 1);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            try {
                Chat chat = (Chat) data.getSerializableExtra("Chat");
                chatViewModel.insert(chat);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (requestCode == 2 && resultCode == RESULT_OK && data != null) {

        } else
            Toast.makeText(this, "Please select Contact", Toast.LENGTH_SHORT).show();
    }
}
