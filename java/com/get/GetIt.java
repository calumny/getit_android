package com.get;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
//import android.location.LocationListener;
import android.preference.PreferenceManager;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;

import org.joda.time.DateTime;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class GetIt extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private boolean gotIt;

    private boolean locationReady = false;

    private boolean tokenReady = false;

    private String mToken = "";

    private GoogleApiClient mGoogleApiClient;

    private Location mLocation;

    private LocationRequest mLocationRequest;

    private Integer mChildren = 0;

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "GetIt";

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLocation == null) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(60 * 1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            mLocationRequest.setSmallestDisplacement(10f);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            locationReady = true;
        }
    }

    @Override
    public void onLocationChanged (Location location) {
        mLocation = location;
        locationReady = true;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GetIt.this);
// then you use
        Boolean lastGotIt = prefs.getBoolean(getResources().getString(R.string.prefs_got_it_key), false);
        Integer gotItYear = prefs.getInt(getResources().getString(R.string.prefs_year_key), 2015);
        Integer gotItMonth = prefs.getInt(getResources().getString(R.string.prefs_month_key), 0);
        DateTime currTime = new DateTime();
        Integer currMonth = currTime.getMonthOfYear();
        Integer currYear = currTime.getYear();

        gotIt = lastGotIt && gotItYear.equals(currYear) && gotItMonth.equals(currMonth);

        setContentView(R.layout.activity_get_it);

        if (!gotIt) {
            getCount();
        } else {
            Intent giveIt = new Intent(GetIt.this, GiveIt.class);
            startActivity(giveIt);
            overridePendingTransition(R.anim.activity_slide_in, R.anim.activity_slide_out);
            finish();
        }
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        View map = findViewById(R.id.map);
        map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapActivity = new Intent(GetIt.this, MapActivity.class);
                startActivity(mapActivity);
                overridePendingTransition(R.anim.activity_slide_in, R.anim.activity_slide_out);
            }
        });

        setButtonListener();

    }

    @Override
    public void onResume() {
        tryCheckStatus();
        super.onResume();
    }

    private void tryCheckStatus() {

        if (!tokenReady) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GetIt.this);

            String token = prefs.getString(getResources().getString(R.string.prefs_token_key), "");

            if (token.trim().length() == 0) {

                loginAndGetStatus();
            } else {

                mToken = token;
                tokenReady = true;
                checkStatus();

            }
        } else {
            checkStatus();
        }

    }

    private void getCount() {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GetIt.this);

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(getResources().getString(R.string.api_endpoint))
                .build();

        GetItService service = restAdapter.create(GetItService.class);

        service.getCount(new Callback<Integer>() {
            @Override
            public void success(Integer totalCount, Response response) {
                TextView count = (TextView) findViewById(R.id.totalCount);
                if (totalCount == 1) {
                    count.setText(String.format("%d OTHER PERSON HAS IT", totalCount));
                } else {
                    count.setText(String.format("%d OTHER PEOPLE HAVE IT", totalCount));
                }
                count.setVisibility(View.VISIBLE);
            }

            @Override
            public void failure(RetrofitError retrofitError) {

                retrofitError.printStackTrace();
                // Log error here since request failed
            }
        });

    }

    private void checkStatus() {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GetIt.this);

        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestInterceptor.RequestFacade request) {
                request.addHeader("Authorization", "Token " + mToken);
            }
        };

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(getResources().getString(R.string.api_endpoint))
                .setRequestInterceptor(requestInterceptor)
                .build();

        GetItService service = restAdapter.create(GetItService.class);

        service.getStatus(new Callback<Boolean>() {
            @Override
            public void success(Boolean serverGotIt, Response response) {
                if (serverGotIt) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(getResources().getString(R.string.prefs_got_it_key), serverGotIt);

                    DateTime currTime = new DateTime();
                    Integer currMonth = currTime.getMonthOfYear();
                    Integer currYear = currTime.getYear();

                    editor.putInt(getResources().getString(R.string.prefs_year_key), currYear);
                    editor.putInt(getResources().getString(R.string.prefs_month_key), currMonth);
                    editor.commit();

                    Intent giveIt = new Intent(GetIt.this, GiveIt.class);
                    startActivity(giveIt);
                    overridePendingTransition(R.anim.activity_slide_in, R.anim.activity_slide_out);
                    finish();
                }
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                showMessage("COULDN'T CONNECT TO SERVER");
                retrofitError.printStackTrace();
                // Log error here since request failed
            }
        });
    }

    private void setButtonListener() {

        Button getItButton = (Button) findViewById(R.id.get_it);
        getItButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryGetIt();
            }
        });

    }

    private void loginAndGetStatus() {

            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(getResources().getString(R.string.api_endpoint))
                    .build();

            AuthenticationService service = restAdapter.create(AuthenticationService.class);

            Credentials credentials = new Credentials(getId(), getId());

        service.getToken(credentials, new Callback<TokenResponse>() {
            @Override
            public void success(TokenResponse token, Response response) {
                SharedPreferences settings = PreferenceManager
                        .getDefaultSharedPreferences(getApplication());
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(getResources().getString(R.string.prefs_token_key), token.getKey());
                editor.commit();

                mToken = token.getKey();
                tokenReady = true;
                checkStatus();
                // Access user here after response is parsed
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                registerAndCheckStatus();
                retrofitError.printStackTrace();

                // Log error here since request failed
                }
            });

    }

    private void registerAndCheckStatus() {

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(getResources().getString(R.string.api_endpoint))
                .build();

        AuthenticationService service = restAdapter.create(AuthenticationService.class);

        Credentials credentials = new Credentials(getId(), getId());

        service.register(credentials, new Callback<TokenResponse>() {
            @Override
            public void success(TokenResponse token, Response response) {
                SharedPreferences settings = PreferenceManager
                        .getDefaultSharedPreferences(getApplication());
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(getResources().getString(R.string.prefs_token_key), token.getKey());
                editor.commit();

                mToken = token.getKey();

                if (checkPlayServices()) {
                    // Start IntentService to register this application with GCM.
                    Intent intent = new Intent(GetIt.this, RegistrationIntentService.class);
                    intent.putExtra("SERVER_TOKEN", mToken);
                    startService(intent);
                }

                tokenReady = true;
                checkStatus();
                // Access user here after response is parsed
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                showMessage("COULDN'T CONNECT TO SERVER");
                retrofitError.printStackTrace();
                // Log error here since request failed
            }
        });

    }

    private String getId() {
        return Installation.id(this);
    }

    private void showMessage(String message) {
        final View text =  findViewById(R.id.statusText);
        final TextView messageView = (TextView) findViewById(R.id.messageText);
        Animation statusOut = AnimationUtils.loadAnimation(this, R.anim.slide_out);
        final Animation messageOut = AnimationUtils.loadAnimation(this, R.anim.slide_out);
        messageOut.setStartOffset(600);
        final Animation statusIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        statusIn.setStartOffset(600);
        Animation messageIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        statusOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                text.startAnimation(statusIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        messageIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                messageView.startAnimation(messageOut);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        messageView.setText(message);
        messageView.setVisibility(View.VISIBLE);

        text.startAnimation(statusOut);
        messageView.startAnimation(messageIn);

    }

    private void tryGetIt() {

        if (!locationReady) {
            showMessage("COULDN'T IDENTIFY LOCATION");
//            Toast.makeText(GetIt.this, "Couldn't identify location", Toast.LENGTH_SHORT).show();

        } else if (!tokenReady) {

            tryCheckStatus();
//            Toast.makeText(GetIt.this, "Couldn't connect to Get It server", Toast.LENGTH_SHORT).show();

        } else {

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GetIt.this);

            RequestInterceptor requestInterceptor = new RequestInterceptor() {
                @Override
                public void intercept(RequestInterceptor.RequestFacade request) {
                    request.addHeader("Authorization", "Token " + mToken);
                }
            };

            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(getResources().getString(R.string.api_endpoint))
                    .setRequestInterceptor(requestInterceptor)
                    .build();

            GetItService service = restAdapter.create(GetItService.class);

            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            GetItLocation location = new GetItLocation(mLocation.getLatitude(), mLocation.getLongitude());


            service.getIt(location, new Callback<Boolean>() {
                @Override
                public void success(Boolean serverGotIt, Response response) {
                    if (serverGotIt) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(getResources().getString(R.string.prefs_got_it_key), serverGotIt);
                        DateTime currTime = new DateTime();
                        Integer currMonth = currTime.getMonthOfYear();
                        Integer currYear = currTime.getYear();

                        editor.putInt(getResources().getString(R.string.prefs_year_key), currYear);
                        editor.putInt(getResources().getString(R.string.prefs_month_key), currMonth);
                        editor.commit();
                        Intent giveIt = new Intent(GetIt.this, GiveIt.class);
                        startActivity(giveIt);
                        overridePendingTransition(R.anim.activity_slide_in, R.anim.activity_slide_out);
                        finish();
                    }
                    if (!serverGotIt) {
                        showMessage("SOMEONE NEEDS TO GIVE IT TO YOU");
//                        Toast.makeText(GetIt.this, "Somebody needs to give it to you", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    showMessage("COULDN'T CONNECT TO SERVER");
                    retrofitError.printStackTrace();
                    // Log error here since request failed
                }
            });
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_get_it, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
//        Toast.makeText(GetIt.this, "CONNECTION FAILED", Toast.LENGTH_SHORT).show();

    }
}