package com.firebase.androidchat.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.firebase.androidchat.ChatApplication;
import com.firebase.androidchat.R;
import com.firebase.androidchat.adapter.ChatListAdapter;
import com.firebase.androidchat.bean.Channel;
import com.firebase.androidchat.bean.Chat;
import com.firebase.androidchat.bean.User;
import com.firebase.client.*;

import java.util.ArrayList;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {

    // TODO: change this to your own Firebase URL


    private String mChannelName;
    private Firebase mFirebase;
    private Firebase mFirebaseChat;
    private ValueEventListener mConnectedListener;
    private ChatListAdapter mChatListAdapter;
    private String mUserName;
    private ArrayList<String> userList;
    private Firebase mFirebaseUser;

    public void setmFirebaseChat(Firebase mFirebaseChat) {
        this.mFirebaseChat = mFirebaseChat;
    }

    public void setmChatListAdapter(final ChatListAdapter mChatListAdapter) {
        this.mChatListAdapter = mChatListAdapter;
        final ListView listView = (ListView) findViewById(R.id.listview);
        listView.setAdapter(mChatListAdapter);
        mChatListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(mChatListAdapter.getCount() - 1);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        // Make sure we have a mChannelName
        setupChannelname();

        setupUsername();

        setTitle("Chatting in " + mChannelName);

        // Setup our Firebase mFirebaseChat
        mFirebase = new Firebase(ChatApplication.FIREBASE_URL);
        mFirebaseChat = mFirebase.child("channel").child(mChannelName.replace(".", ",")).child("chat");
        mFirebaseUser = mFirebase.child("channel").child(mChannelName.replace(".", ",")).child("user");
        getUserList();

        // Setup our input methods. Enter key on the keyboard or pushing the send button
        EditText inputText = (EditText) findViewById(R.id.messageInput);
        inputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_NULL && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    sendMessage();
                }
                return true;
            }
        });

        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat_activity, menu);
        MenuItem map = menu.getItem(0);
        map.setIcon(R.drawable.map);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.change_username:
                Intent intent = new Intent(ChatActivity.this, MapsActivity.class);
                startActivity(intent);
                return true;
            case R.id.logout:
                backToLogin();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void backToLogin() {
        Intent intent = new Intent(getApplication(),LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Setup our view and list adapter. Ensure it scrolls to the bottom as data changes
        final ListView listView = (ListView) findViewById(R.id.listview);
        // Tell our list adapter that we only want 50 messages at a time
        mChatListAdapter = new ChatListAdapter(mFirebaseChat.limit(50), this, R.layout.chat_message, mUserName);
        listView.setAdapter(mChatListAdapter);
        mChatListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(mChatListAdapter.getCount() - 1);
            }
        });

        // Finally, a little indication of connection status
        mConnectedListener = mFirebase.getRoot().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = (Boolean) dataSnapshot.getValue();
                if (connected) {
                    Toast.makeText(ChatActivity.this, "Connected to Firebase", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChatActivity.this, "Disconnected from Firebase", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                // No-op
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        mFirebaseChat.getRoot().child(".info/connected").removeEventListener(mConnectedListener);
        mChatListAdapter.cleanup();
    }

    private void setupChannelname() {
        SharedPreferences prefs = getApplication().getSharedPreferences("ChatPrefs", 0);
        mChannelName = prefs.getString("channel", null);
    }

    private void setupUsername() {
        SharedPreferences prefs = getApplication().getSharedPreferences("ChatPrefs", 0);
        mUserName = prefs.getString("username", null);
    }

    private void userLoginAlertDialog(){
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.username_alert_dialog, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userName = (EditText) promptsView
                .findViewById(R.id.editTextDialogUserInput);
        if(mChannelName != null)
            userName.setText(mChannelName);

        final EditText userPassword = (EditText) promptsView
                .findViewById(R.id.editTextDialogUserPassword);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, int id) {

                            }
                        })
                .setNegativeButton("Exit",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                backToLogin();
                            }
                        });

        // create alert dialog
        final AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = userName.getText().toString();
                String password = userPassword.getText().toString();
                mFirebase.authWithPassword(username, password, new Firebase.AuthResultHandler() {
                    @Override
                    public void onAuthenticated(AuthData authData) {
                        System.out.println("User ID: " + authData.getUid() + ", Provider: " + authData.getProvider());
                        alertDialog.dismiss();
                    }

                    @Override
                    public void onAuthenticationError(FirebaseError firebaseError) {
                        Toast.makeText(getBaseContext(), "User not exist or password wrong.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    private void sendMessage() {
        EditText inputText = (EditText) findViewById(R.id.messageInput);
        String input = inputText.getText().toString();
        if (!input.equals("")) {
            // Create our 'model', a Chat object
            Chat chat = new Chat(input, mUserName);
            // Create a new, auto-generated child of that chat location, and save our chat data there
            mFirebaseChat.push().setValue(chat);
            inputText.setText("");
            if(!userList.contains(mUserName)){
                mFirebaseUser.push().setValue(new User(mUserName));
            }
        }
    }

    private void getUserList(){
        userList = new ArrayList<>();
        mFirebaseUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                GenericTypeIndicator<HashMap<String, User>> t = new GenericTypeIndicator<HashMap<String, User>>() {
                };
                HashMap<String, User> map = snapshot.getValue(t);
                if(map == null)
                    return;
                for (User u : map.values()) {
                    userList.add(u.getName());
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }
}
