/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.get;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.joda.time.DateTime;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MyGcmListenerService extends GcmListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "MyGcmListenerService";

    private GoogleApiClient mGoogleApiClient;

    private Location mLocation;

    private String mMessage;

    private boolean locationReady, sendLocation;

    private LocationRequest mLocationRequest;

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnectionSuspended(int i) {
//        Toast.makeText(GetIt.this, "CONNECTION FAILED", Toast.LENGTH_SHORT).show();

    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
//        Toast.makeText(GetIt.this, "CONNECTION FAILED", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.d(TAG, "ON CONNECTED");
        if (mLocation == null) {
            Log.d(TAG, "REQUESTING LOCATION");
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(60 * 1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
            mLocationRequest.setSmallestDisplacement(10f);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            Log.d(TAG, "LOCATION NOT NULL");
            locationReady = true;
            if (sendLocation) {
                sendLocationToServer();
            }
        }
    }

    @Override
    public void onLocationChanged (Location location) {
        mLocation = location;
        locationReady = true;
        if (sendLocation) {
            sendLocationToServer();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    private void sendLocationToServer() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MyGcmListenerService.this);

        final String mToken = prefs.getString(getResources().getString(R.string.prefs_token_key), "");

        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("Authorization", "Token " + mToken);
            }
        };

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(getResources().getString(R.string.api_endpoint))
                .setRequestInterceptor(requestInterceptor)
                .build();

        GetItService service = restAdapter.create(GetItService.class);
        Log.d(TAG, "LAT:  " + mLocation.getLatitude());
        Log.d(TAG, "LON:  " + mLocation.getLongitude());

//            GetItLocation location = new GetItLocation(42, -71);
        GetItLocation location = new GetItLocation(mLocation.getLatitude(), mLocation.getLongitude());
//                GetItLocation location = new GetItLocation(0, 0);

        service.confirmLocation(location, new Callback<Boolean>() {
            @Override
            public void success(Boolean serverGotIt, Response response) {
                if (serverGotIt) {
                    sendNotification(mMessage);

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MyGcmListenerService.this);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(getResources().getString(R.string.prefs_got_it_key), true);
                    DateTime currTime = new DateTime();
                    Integer currMonth = currTime.getMonthOfYear();
                    Integer currYear = currTime.getYear();

                    editor.putInt(getResources().getString(R.string.prefs_year_key), currYear);
                    editor.putInt(getResources().getString(R.string.prefs_month_key), currMonth);
                    editor.commit();

                    sendLocation = false;
                    Log.d(TAG, "SERVER GOT IT");
                } else {
                    Log.d(TAG, "SERVER DIDN'T GET IT");
                }
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                retrofitError.printStackTrace();
                // Log error here since request failed
            }
        });

    }

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        final String message = data.getString("message");
        mMessage = message;
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);

        if (message.equals("YOU'RE STARTING WITH IT")) {

            Log.d(TAG, "WILL SEND LOCATION");
            sendLocation = true;

            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (mLocation != null) {
                sendLocationToServer();
            } else {
                Log.d(TAG, "LOCATION IS NULL");
            }


        } else {
            sendNotification(message);
        }
        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MyGcmListenerService.this);

        Boolean gotIt = prefs.getBoolean(getResources().getString(R.string.prefs_got_it_key), false);

        Intent intent;
        if (gotIt) {
            intent =  new Intent(this, GiveIt.class);
        } else {
            intent =  new Intent(this, GetIt.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("GET IT")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
