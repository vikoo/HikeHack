package com.teachmate.teachmate.Chat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.teachmate.teachmate.DBHandlers.ChatIdMapDBHandler;
import com.teachmate.teachmate.DBHandlers.ChatInfoDBHandler;
import com.teachmate.teachmate.DBHandlers.UserModelDBHandler;
import com.teachmate.teachmate.R;
import com.teachmate.teachmate.TempDataClass;
import com.teachmate.teachmate.models.ChatIdMap;
import com.teachmate.teachmate.models.ChatInfo;
import com.teachmate.teachmate.models.UserModel;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by VVekariya on 28-Mar-15.
 */
public class ChatXMPPService extends Service {

    private static final String TAG = "ChatXMPPService";
    public static XMPPTCPConnection connection;
    public static boolean isRunning = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    XMPPTCPConnectionConfiguration.Builder conf = XMPPTCPConnectionConfiguration.builder().allowEmptyOrNullUsernames();
                    conf.setHost("hackathon.hike.in");
                    conf.setPort(8282);
                    conf.setServiceName("hackathon.hike.in");
                    conf.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);

                    connection = new XMPPTCPConnection(conf.build());

                    connection.connect();
                    System.out.println("hack xmpp connected");
                    UserModel model = UserModelDBHandler.ReturnValue(getApplicationContext());
                    connection.login(model.ServerUserId, "123");
                    System.out.println("hack xmpp logged in");


                    ChatManager.getInstanceFor(connection).addChatListener(new ChatManagerListener() {
                        @Override
                        public void chatCreated(Chat chat, boolean createdLocally) {
                            if (!createdLocally) {
                                chat.addMessageListener(new ChatMessageListener() {
                                    @Override
                                    public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {
                                        Log.i(TAG, "hack Xmpp message received: '" + chat.getParticipant() + ":" + message.getBody());
                                        sendNotification(message.getBody());
                                    }
                                });
                            }
                        }
                    });
                    isRunning = true;
                } catch (XMPPException e) {
                    e.printStackTrace();
                } catch (SmackException s) {
                    s.printStackTrace();
                } catch (IOException i) {
                    i.printStackTrace();
                }
                return null;
            }
        }.execute();

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        connection.disconnect();
        isRunning = false;
        Log.i(TAG, "hack- service disconnected");
    }

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    private void sendNotification(String msg) {
        String chatChatId = "";
        String chatMessage = "";
        String chatMessageTime = "";
        String chatSenderId = "";
        String chatSenderName = "";
        try {
            JSONObject reader = new JSONObject(msg);
            chatChatId = reader.getString("ChatId");
            chatMessage = reader.getString("Message");
            chatMessageTime = reader.getString("SentOn");
            chatSenderId = reader.getString("SenderId");
            chatSenderName = reader.getString("userName");


        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        if(mHandler !=null && mReceiverId!=null && mReceiverId.equals(chatSenderId)){
            mHandler.obtainMessage(ChatActivity.MESSAGE_WRITE, -1, -1, chatMessage.getBytes())
                    .sendToTarget();
        }

        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.notif_icon_big);

        Intent chatIntent = new Intent(getApplicationContext(), ChatActivity.class);
        chatIntent.putExtra("ChatId", chatChatId);
        chatIntent.putExtra("Message", chatMessage);
        chatIntent.putExtra("SentOn", chatMessageTime);
        chatIntent.putExtra("SenderId", chatSenderId);
        chatIntent.putExtra("UserName", chatSenderName);
        chatIntent.putExtra("received", true);
        ChatIdMap chatIdMap = new ChatIdMap();
        chatIdMap.chatId = chatChatId;
        chatIdMap.userId = chatSenderId;
        chatIdMap.userName = chatSenderName;
        ChatIdMapDBHandler.InsertChatIdMap(getApplicationContext(), chatIdMap);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                Integer.parseInt(chatChatId),
                chatIntent,
                PendingIntent.FLAG_ONE_SHOT);


        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String time = dateFormat.format(date);
        time = time.substring(11, time.lastIndexOf(':'));
        ChatInfo newMessage = new ChatInfo();
        newMessage.setMessage(chatMessage);
        newMessage.setSentBy(false);
        newMessage.setTimeStamp(time);
        newMessage.setChatId(chatChatId);

        ChatInfoDBHandler.InsertChatInfo(getApplicationContext(), newMessage);

        if (TempDataClass.alreadyAdded == true) {
            TempDataClass.alreadyAdded = false;
            return;
        }else {

            Notification mBuilder =
                    new NotificationCompat.Builder(getApplicationContext())
                            .setSmallIcon(R.drawable.notif_icon)
                            //.setLargeIcon(bitmap)
                            .setContentTitle("Chat Notification")
                            .setContentIntent(pendingIntent)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(chatSenderName + ":" + chatMessage))
                            .setContentText(chatSenderName + ":" + chatMessage)
                            .setAutoCancel(true)
                            .build();


            mNotificationManager =
                    (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder);
        }
    }

    private static Handler mHandler;
    private static String mReceiverId;
    public static void setHandler(Handler handler, String receiverId){
        mHandler = handler;
        mReceiverId = receiverId;
    }

}
