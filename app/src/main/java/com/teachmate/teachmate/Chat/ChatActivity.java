
package com.teachmate.teachmate.Chat;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.teachmate.teachmate.DBHandlers.ChatIdMapDBHandler;
import com.teachmate.teachmate.DBHandlers.ChatInfoDBHandler;
import com.teachmate.teachmate.DBHandlers.UserModelDBHandler;
import com.teachmate.teachmate.R;
import com.teachmate.teachmate.TempDataClass;
import com.teachmate.teachmate.models.ChatInfo;
import com.teachmate.teachmate.models.UserModel;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatActivity extends ActionBarActivity {

    public static final int MESSAGE_WRITE = 1;
    private static String TAG = "ChatActivity";
    ArrayList<Message> messages;
    ChatAdapter adapter;
    EditText text;
    Button send;
    boolean sentBy = true;
    static String chatId;
    String receivedFrom;
    String receivedAt;
    String time;
    public static final int CHAT_NOTIFICATION = 5;
    ListView chatMessages;
    IntentFilter intentFilter;
    private String receiverGuy;


//    private BroadcastReceiver receiver = new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String newMessage = "";
//            String messageFrom = "";
//            Bundle bundle = intent.getExtras();
//            if (bundle != null && bundle.containsKey("Message")) {
//                newMessage = bundle.getString("Message");
//            }
//            if (bundle != null && bundle.containsKey("UserName")) {
//                messageFrom = bundle.getString("UserName");
//            }
//            if (messageFrom.equalsIgnoreCase(receivedFrom)) {
//                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                Date date = new Date();
//                time = dateFormat.format(date);
//                time = time.substring(11, time.lastIndexOf(':'));
//                text.setText("");
//                addNewMessage(new Message(newMessage, false, time));
//                TempDataClass.alreadyAdded = true;
//            }
//
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_acitivity);

        UserModel user =
                UserModelDBHandler.ReturnValue(getApplicationContext());
        TempDataClass.userName = user.FirstName + " " + user.LastName;
        TempDataClass.serverUserId = user.ServerUserId;
        TempDataClass.userProfession = user.Profession;
        TempDataClass.emailId = user.EmailId;

        chatMessages = (ListView) findViewById(R.id.list);
        messages = new ArrayList<Message>();
        adapter = new ChatAdapter(this, messages);
        chatMessages.setAdapter(adapter);


        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        // chatId bundle from intent service
        if (bundle != null && bundle.containsKey("ChatId")) {
            chatId = bundle.getString("ChatId");
        }
        // username bundle from intent service
        if (bundle != null && bundle.containsKey("UserName")) {
            receivedFrom = bundle.getString("UserName");
        }
        // previousConversation bundle from previous chat
        if (bundle != null && bundle.containsKey("previousConversation")) {
            chatId = bundle.getString("previousConversation");
        }
        // fetching receiver id
        receiverGuy = ChatIdMapDBHandler.chattingWithId(getApplicationContext(),chatId);
        ChatXMPPService.setHandler(mHandler,receiverGuy);
        text = (EditText) this.findViewById(R.id.text);
        send = (Button) this.findViewById(R.id.send_button);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String newMessage = text.getText().toString().trim();
                if (!TextUtils.isEmpty(newMessage)) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date();
                    time = dateFormat.format(date);
                    time = time.substring(11, time.lastIndexOf(':'));
                    text.setText("");
                    addNewMessage(new Message(newMessage, sentBy, time));
                    // sentBy = !sentBy;
                    // sends the notification to hte other user
//                    new SendMessage().execute("http://teach-mate.azurewebsites.net/Chat/ChatMsg",
//                            newMessage);
                    // sending xmpp message
                    if(chat != null){
                        try {
                            chat.sendMessage(createMessage(newMessage));
                            Log.i(TAG,"hack sent:"+newMessage);
                        } catch (SmackException.NotConnectedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        List<ChatInfo> previousChatMessages = ChatInfoDBHandler.GetPreviousChat(
                getApplicationContext(), chatId);
        if (previousChatMessages != null) {
            for (ChatInfo chatmessages : previousChatMessages) {
                messages.add(new Message(chatmessages.getMessage(), chatmessages.isSentBy(),
                        chatmessages.getTimeStamp()));
            }
        }

        setActionBarLayout();

        adapter = new ChatAdapter(this, messages);
        chatMessages.setAdapter(adapter);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                establishXMPP();
                return null;
            }
        }.execute();



    }  @Override
       protected void onPause() {
        super.onPause();
//        unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.google.android.c2dm.intent.RECEIVE");
//        registerReceiver(receiver, new IntentFilter(intentFilter));
        adapter.notifyDataSetChanged();
    }

    /*
         * Adding message to the list and storing the message in the db
         */
    void addNewMessage(Message m) {
        messages.add(m);
        adapter.notifyDataSetChanged();
        chatMessages.setSelection(messages.size() - 1);

        if (m.isMine() == true) {

        } else {
            time = receivedAt;
        }

        ChatInfo newMessage = new ChatInfo();
        newMessage.setMessage(m.getMessage());
        newMessage.setSentBy(m.isMine());
        newMessage.setTimeStamp(time);
        newMessage.setChatId(chatId);

        ChatInfoDBHandler.InsertChatInfo(getApplicationContext(), newMessage);

    }

    /*
     * Preparing JSON Object and sending it to the server
     */
    public String POST(String url, String message) {
        InputStream inputStream = null;
        String result = "";
        try {

            HttpClient httpclient = new DefaultHttpClient();

            HttpPost httpPost = new HttpPost(url);

            String json = "";

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ChatId", chatId);
            jsonObject.put("Message", message);
            jsonObject.put("SenderId", Integer.parseInt(TempDataClass.serverUserId));
            // jsonObject.put("userName", TempDataClass.userName);
            jsonObject.put("Type", CHAT_NOTIFICATION);
            jsonObject.put("SentOn", time);

            json = jsonObject.toString();
            StringEntity se = new StringEntity(json);

            httpPost.setEntity(se);

            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            HttpResponse httpResponse = httpclient.execute(httpPost);
            inputStream = httpResponse.getEntity().getContent();
            result = convertInputStreamToString(inputStream);

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    /*
     * Network call to send message to server
     */
    private class SendMessage extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return POST(urls[0], urls[1]);
        }
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;
    }

    /*
     * sets a custom view on the action bar to show user you are chatting with
     * and their image
     */
    private void setActionBarLayout() {

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);

        // Inflate the custom view
        LayoutInflater inflater = LayoutInflater.from(this);
        View header = inflater.inflate(R.layout.action_bar_chat, null);
        TextView userChattingWith = (TextView) header.findViewById(R.id.action_bar_user_name);
        ImageView imageChattingWith = (ImageView) header.findViewById(R.id.action_bar_user_image);
        userChattingWith.setText(ChatIdMapDBHandler.chattingWith(getApplicationContext(), chatId));
        ActionBar.LayoutParams layout = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(header, layout);

    }

    private Chat chat;

    private void establishXMPP() {
        try {
            chat = ChatManager.getInstanceFor(ChatXMPPService.connection).createChat(receiverGuy + "@hackathon.hike.in", new ChatMessageListener() {
                @Override
                public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {
                    System.out.println("hack Received message: " + message);
                }
            });
            if (chat != null) {
                chat.addMessageListener(new ChatMessageListener() {
                    @Override
                    public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {

                        System.out.println("hack received message from: " + chat.getParticipant() + "  message" + message);
                    }
                });
            }
        } catch (Exception s) {
            s.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        ChatXMPPService.setHandler(null,null);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        ChatXMPPService.setHandler(null,null);
        super.onDestroy();
    }

    private String createMessage(String message){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("ChatId", chatId);
            jsonObject.put("Message", message);
            jsonObject.put("SenderId", TempDataClass.serverUserId);
            jsonObject.put("userName", TempDataClass.userName);
            jsonObject.put("SentOn", time);

            return jsonObject.toString();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String newMessage = new String(writeBuf);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date();
                    time = dateFormat.format(date);
                    time = time.substring(11, time.lastIndexOf(':'));
                    text.setText("");
                    addNewMessage(new Message(newMessage, false, time));
                    TempDataClass.alreadyAdded = true;
                    break;
            }
        }
    };
}
