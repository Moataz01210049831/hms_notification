/*
    Copyright 2020-2023. Huawei Technologies Co., Ltd. All rights reserved.

    Licensed under the Apache License, Version 2.0 (the "License")
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.huawei.hms.cordova.push.remote;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import com.huawei.hms.cordova.push.constants.Core;
import com.huawei.hms.cordova.push.utils.RemoteMessageUtils;
import com.huawei.hms.cordova.push.utils.ResolvableExceptionUtils;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;
import com.huawei.hms.push.SendException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import static com.huawei.hms.cordova.push.utils.HtmlUtils.getItemResponseListener;
import static com.huawei.hms.cordova.push.utils.HtmlUtils.onBackgroundRemoteMessageReceived;
import static com.huawei.hms.cordova.push.utils.HtmlUtils.readFile;
import static com.huawei.hms.cordova.push.utils.HtmlUtils.wrapInsideScriptTag;
import androidx.annotation.NonNull;
import android.media.RingtoneManager;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.Context;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

import android.os.Build;

import androidx.core.app.NotificationCompat;

import capacitor.android.plugins.R;


public class HmsPushMessageService extends HmsMessageService {
  private final static String TAG = HmsPushMessageService.class.getSimpleName();

  private static Boolean isApplicationRunning = false;

  private WebView webView;

  public static void setApplicationRunningStatus(boolean isRunning) {
    isApplicationRunning = isRunning;
  }

  @Override
  public void onMessageReceived(RemoteMessage message) {
    if (isApplicationRunning) {
      HmsPushMessagePublisher.sendMessageReceivedEvent(message);
    }
    else {
      SharedPreferences sharedPref = getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE);

      String keyLang = sharedPref.getString("keyLang", "ar");

      Log.d("myMessage: ", String.valueOf(sharedPref.getAll()));
      Log.d("keyLang: ", String.valueOf(keyLang));
//        String title = message.getNotification().getTitle();
//        String body = message.getNotification().getBody();
      Log.d("myMessage2: ", String.valueOf(message.getData()));
      // Display a local notification
//        showLocalNotification(title, body);

      if (keyLang.equals("ar")) {
//      Log.d("myMessage: ", String.valueOf(remoteMessage.getData()));
        String jsonPayload = message.getData();
        try {
          Log.d("myMessage2: ", String.valueOf(message.getData()));
          JSONObject jsonObject = new JSONObject(jsonPayload);
          String titleAr = jsonObject.optString("titleAr");
          String bodyAr = jsonObject.optString("bodyAr");

          Log.d("myMessage2: ", (titleAr));
          showLocalNotification(bodyAr,titleAr,message);
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
        // Access individual values using keys



      }else{

        String jsonPayload = message.getData();
        try {
          Log.d("myMessageEn: ", String.valueOf(message.getData()));
          JSONObject jsonObject = new JSONObject(jsonPayload);
          String titleAr = jsonObject.optString("titleAr");
          String titleEn = jsonObject.optString("titleEn");
          String bodyEn = jsonObject.optString("bodyEn");
          Log.d("myMessage2: ", (titleAr));
          showLocalNotification(bodyEn,titleEn,message);
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
//
      }
    }
  }
  private void showLocalNotification(String title, String body,RemoteMessage remoteMessage) {

    // Create a notification channel (for Android O and above)
    createNotificationChannel();

    Log.d("myMessage511: ", String.valueOf(remoteMessage.getData()));


    // Create an intent for the notification
    Intent intent = null;
    PendingIntent pendingIntent = null;
    Bundle data = new Bundle();
    data.putString("notification" , remoteMessage.getData().toString());
    Log.d("myMessage51: ", String.valueOf(remoteMessage));
    try {
      intent = new Intent(this,
        Class.forName("com.splonline.services.MainActivity"));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    intent.putExtra("notification" , data);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    pendingIntent = PendingIntent.getActivity(
      this, 0 /* request code */, intent,PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
    );
    Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"+ getApplicationContext().getPackageName() + "/" + R.raw.ss);

    // Build the notification
    //Log.d("myMessage2: ", String.valueOf(alarmSound));
    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "spl.sa.com")
      .setContentTitle(title)
      .setContentText(body)
      .setSmallIcon(R.drawable.ic_launcher)
      .setContentIntent(pendingIntent)
      .setSound(soundUri)
      .setAutoCancel(true);

    // Show the notification
    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    if (notificationManager != null) {
      notificationManager.notify(0, notificationBuilder.build());
    }


  }
  private void createNotificationChannel() {
    NotificationChannel mChannel;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      mChannel = new NotificationChannel("spl.sa.com", "spl_channel", NotificationManager.IMPORTANCE_HIGH);
      mChannel.setLightColor(Color.GRAY);
      mChannel.enableLights(true);
      mChannel.setDescription("description");
      AudioAttributes audioAttributes = new AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .build();
      Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"+ getApplicationContext().getPackageName() + "/" + R.raw.ss);
      mChannel.setSound(soundUri, audioAttributes);
      NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      if (mNotificationManager != null) {
        mNotificationManager.createNotificationChannel( mChannel );
      }
    }

  }

  @Override
  public void onMessageSent(String msgId) {
    super.onMessageSent(msgId);
    HmsPushMessagePublisher.sendOnMessageSentEvent(msgId);
  }

  @Override
  public void onMessageDelivered(String msgId, Exception e) {
    HmsPushMessagePublisher.sendOnMessageDeliveredEvent(msgId, ((SendException) e).getErrorCode(),
      e.getLocalizedMessage());
  }

  @Override
  public void onSendError(String msgId, Exception e) {
    HmsPushMessagePublisher.sendOnMessageSentErrorEvent(msgId, ((SendException) e).getErrorCode(),
      e.getLocalizedMessage());
  }

  @Override
  public void onNewToken(String token) {
    HmsPushMessagePublisher.sendOnNewTokenEvent(token);
  }

  @Override
  public void onTokenError(Exception e) {
    HmsPushMessagePublisher.sendTokenErrorEvent(e);
  }

  @Override
  public void onNewToken(String s, Bundle bundle) {
    HmsPushMessagePublisher.sendOnNewMultiSenderTokenEvent(s, bundle);
  }

  @Override
  public void onTokenError(Exception e, Bundle bundle) {
    HmsPushMessagePublisher.sendMultiSenderTokenErrorEvent(e, bundle);
    new ResolvableExceptionUtils(e, webView, TAG);
  }


}
