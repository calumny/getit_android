package com.get;

import android.animation.ArgbEvaluator;
import android.app.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
//import android.support.v7.app.AppCompatActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.app.ActionBarDrawerToggle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import android.widget.AdapterView;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.*;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

//import android.location.LocationListener;

public class GiveIt extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

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

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            switchActivity(position);
        }
    }

    public void switchActivity(int position) {
        switch (position) {
            case 0:
                Intent statsActivity = new Intent(GiveIt.this, StatsActivity.class);
                mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                startActivity(statsActivity);
                overridePendingTransition(R.anim.activity_slide_in, R.anim.activity_slide_out);
                break;
            case 1:
                Intent mapActivity = new Intent(GiveIt.this, MapActivity.class);
                mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                mapActivity.putExtra("LAT", mLocation.getLatitude());
                mapActivity.putExtra("LON", mLocation.getLongitude());
                startActivity(mapActivity);
                overridePendingTransition(R.anim.activity_slide_in, R.anim.activity_slide_out);
                break;
        }
    }

    private float initialButtonX, initialButtonY;

    private String[] mNavigationDrawerItemTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onStop() {
        mDrawerLayout.closeDrawers();
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gotIt = true;
        setContentView(R.layout.activity_got_it);

        status =  findViewById(R.id.statusText);
        messageView = (TextView) findViewById(R.id.messageText);
        messageView.setVisibility(View.VISIBLE);

        mNavigationDrawerItemTitles= getResources().getStringArray(R.array.navigation_drawer_items_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        DrawerItem[] drawerItem = new DrawerItem[2];

        drawerItem[0] = new DrawerItem(R.drawable.ic_stats, "Statistics");
        drawerItem[1] = new DrawerItem(R.drawable.ic_map, "Map");

        DrawerItemAdapter adapter = new DrawerItemAdapter(this, R.layout.list_item, drawerItem);

        DrawerItemClickListener clickListener = new DrawerItemClickListener();

        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(clickListener);
        mDrawerList.bringToFront();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,
                R.string.title_activity_give_it,  /* "open drawer" description */
                R.string.title_activity_give_it  /* "close drawer" description */
        ) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                Log.d(TAG, "VISIBLE");
                mDrawerList.requestLayout();
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        };


        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        buildGoogleApiClient();
        mGoogleApiClient.connect();

        setButtonListener();

    }

    @Override
    public void onResume() {
        tryCheckStatus();
        super.onResume();
    }

    private void tryCheckStatus() {

        if (!tokenReady) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GiveIt.this);

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

    private void checkStatus() {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GiveIt.this);

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

        service.getStatus(new Callback<Boolean>() {
            @Override
            public void success(Boolean serverGotIt, Response response) {
                if (!serverGotIt) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(getResources().getString(R.string.prefs_got_it_key), serverGotIt);
                    editor.commit();
                    Intent getIt = new Intent(GiveIt.this, GetIt.class);
                    startActivity(getIt);
                    overridePendingTransition(R.anim.activity_slide_in, R.anim.activity_slide_out);
                    finish();
                } else {
                    getChildCount();
                }
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                showError("COULDN'T CONNECT TO SERVER");
                retrofitError.printStackTrace();
                // Log error here since request failed
            }
        });
    }

    private void getChildCount() {

        showGiveIt();
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

        service.checkGaveIt(new Callback<Integer>() {
            @Override
            public void success(Integer children, Response response) {

                mChildren = children;

            }

            @Override
            public void failure(RetrofitError retrofitError) {
                retrofitError.printStackTrace();
                // Log error here since request failed
            }
        });

    }

    private void checkGaveIt() {

        showGiveIt();
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

        service.checkGaveIt(new Callback<Integer>() {
            @Override
            public void success(Integer children, Response response) {

                if (children > mChildren) {
                    int newChildren = children - mChildren;
                    String message = "1 PERSON GOT IT";
                    if (newChildren > 1) {
                        message = String.format("%d PEOPLE GOT IT", newChildren);
                    }
                    resettable = true;
                    showMessage(message);
                } else {
                    resettable = true;
                    showMessage("NOBODY GOT IT");
                }

                mChildren = children;

            }

            @Override
            public void failure(RetrofitError retrofitError) {
                resettable = true;
                showError("COULDN'T CONNECT TO SERVER");
                retrofitError.printStackTrace();
                // Log error here since request failed
            }
        });

    }

    private void resetGiveItButton() {

        Button giveItButton = (Button) findViewById(R.id.give_it);
        giveItButton.setText("GIVE IT");

        serverError = false;
        givingIt = false;
        resettable = false;
        dodgingIt = false;

    }

    private void toggleGiveItButton() {
        Button giveItButton = (Button) findViewById(R.id.give_it);
        String currText = giveItButton.getText().toString();

        if (currText.equals(getResources().getString(R.string.give_it))) {
            giveItButton.setText(getResources().getString(R.string.giving_it));
            dodgingIt = true;
        } else {
            giveItButton.setText(getResources().getString(R.string.give_it));
            dodgingIt = false;
        }
    }

    private void showGiveIt() {

        View giveItButton = findViewById(R.id.give_it);
        giveItButton.setVisibility(View.VISIBLE);

    }

    private SpringSystem springSystem;
    private Spring xrotationSpring;
    private Spring yrotationSpring;
    private Spring xTranslationSpring, yTranslationSpring;
    private Spring messageSpring, statusSpring;
    private Spring depthSpring;
    private Spring popAnimationSpring;
    private View button;
    private int width;


    private void setButtonListener() {

        Button giveItButton = (Button) findViewById(R.id.give_it);
        button = giveItButton;

        initialButtonX = button.getX();
        initialButtonY = button.getY();

        giveItButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() != MotionEvent.ACTION_UP) {
                    float screenX = event.getX();
                    float screenY = event.getY();
                    float viewX = screenX - v.getWidth() / 2;
                    float viewY = screenY - v.getHeight() / 2;


                    if (event.getY() < v.getHeight() + 30 && event.getY() > -30 && event.getX() < v.getWidth() + 30 && event.getX() > -30) {
                        xrotation(true);
                        xrotationSpring.setEndValue(-viewY / 5);
                        yrotation(true);
                        yrotationSpring.setEndValue(viewX / 20);
//                        yTranslationSpring.setEndValue(0);
                        depth(true);
                        popAnimation(true);
                        float scaleProgress = Math.max(0, 1 - 2 * (Math.abs(viewX) + Math.abs(viewY)) / (v.getWidth() + v.getHeight()));
                        depthSpring.setEndValue(scaleProgress);
                        if (dodgingIt) {

                            if (viewX >= 0) {
                                xTranslationSpring.setEndValue(v.getX()  + viewX - v.getWidth() - 60);
                            } else {
                                xTranslationSpring.setEndValue(v.getX() + viewX + 60);
                            }

//                            xTranslationSpring.setEndValue(v.getWidth() + viewX + 30);

                            if (viewY >= 0) {
                                yTranslationSpring.setEndValue(v.getY() - 1200 + screenY - v.getHeight() - 60);
                                Log.d("SET Y: ", "" + (screenY - v.getHeight() - 60));
                            } else {
                                yTranslationSpring.setEndValue(v.getY() - 1200 + screenY + 60);
                                Log.d("SET Y: ", "" + (v.getY() +  screenY + 60));
                            }
                        }

                    } else {
                        xrotation(false);
                        yrotation(false);
//                        yTranslationSpring.setEndValue(0.8);
                        if (givingIt) {
                            dodgingIt = true;
                        }
                        popAnimation(false);
                        depth(false);
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN && !givingIt) {
                        tryGiveIt();
                    }
                    return true;
                } else {
                    xrotation(false);
                    yrotation(false);
                    popAnimation(false);
                    depth(false);
                    if (statusSpring.getEndValue() == 0) {
                        xTranslationSpring.setEndValue(0);
                    }
                    yTranslationSpring.setEndValue(0);
                    if (givingIt) {
                        dodgingIt = true;
                    }
//                    yTranslationSpring.setEndValue(0.8);
//                    tryGetIt();
                    return true;
                }

            }
        });

        giveItButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryGiveIt();
            }
        });

        springSystem = SpringSystem.create();

        xrotationSpring = springSystem.createSpring()
                .setSpringConfig(SpringConfig.fromBouncinessAndSpeed(5, 20))
                .addListener(new SimpleSpringListener() {
                    @Override
                    public void onSpringUpdate(Spring spring) {
                        setXrotationProgress((float) spring.getCurrentValue());
                    }
                });

        xTranslationSpring = springSystem.createSpring()
                .setSpringConfig(SpringConfig.fromBouncinessAndSpeed(5, 20))
                .addListener(new SimpleSpringListener() {
                    @Override
                    public void onSpringUpdate(Spring spring) {
                        setXTranslationProgress((float) spring.getCurrentValue());
                    }
                });

        yTranslationSpring = springSystem.createSpring()
                .setSpringConfig(SpringConfig.fromBouncinessAndSpeed(5, 20))
                .addListener(new SimpleSpringListener() {
                    @Override
                    public void onSpringUpdate(Spring spring) {
                        setYTranslationProgress((float) spring.getCurrentValue());
                    }
                });


        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        width = metrics.widthPixels;
        int height = metrics.heightPixels;

        messageSpring = springSystem.createSpring()
                .setSpringConfig(SpringConfig.fromBouncinessAndSpeed(7, 10))
                .addListener(new SimpleSpringListener() {
                    @Override
                    public void onSpringUpdate(Spring spring) {
                        setMessageTranslationProgress((float) spring.getCurrentValue());
                    }
                    @Override
                    public void onSpringAtRest(Spring spring) {
/*                        if (messageSpring.getEndValue() >= 0) {
                            messageSpring.setEndValue(-width);
                        } else if (messageSpring.getEndValue() < 0) {*/
                        if (messageSpring.getEndValue() == 0 && !serverError) {
                            toggleGiveItButton();
                        } else if (serverError) {
                            resetGiveItButton();
                        }
                        if (resettable && messageSpring.getEndValue() == width) {
                            resetGiveItButton();
                        }
                        messageSpring.setEndValue(width);
//                            messageSpring.setCurrentValue(width);
//                        }
                    }
                });

        statusSpring = springSystem.createSpring()
                .setSpringConfig(SpringConfig.fromBouncinessAndSpeed(7, 10))
                .addListener(new SimpleSpringListener() {
                    @Override
                    public void onSpringUpdate(Spring spring) {
                        setStatusTranslationProgress((float) spring.getCurrentValue());
                    }
                    @Override
                    public void onSpringAtRest(Spring spring) {
/*                        if (statusSpring.getEndValue() < 0) {
                            statusSpring.setCurrentValue(width);*/
                        statusSpring.setEndValue(0);
                        xTranslationSpring.setEndValue(0);
//                        }
                    }
                });

        statusSpring.setEndValue(0);
        statusSpring.setCurrentValue(0);
        messageSpring.setEndValue(width);
        messageSpring.setCurrentValue(width);

        xTranslationSpring.setEndValue(button.getX());
        xTranslationSpring.setCurrentValue(button.getX());
//        Log.d("INITIAL X ", "" + button.getX());

        yrotationSpring = springSystem.createSpring()
                .setSpringConfig(SpringConfig.fromBouncinessAndSpeed(5, 20))
                .addListener(new SimpleSpringListener() {
                    @Override
                    public void onSpringUpdate(Spring spring) {
                        setYrotationProgress((float) spring.getCurrentValue());
                    }
                });

        depthSpring = springSystem.createSpring()
                .setSpringConfig(SpringConfig.fromBouncinessAndSpeed(5, 30))
                .addListener(new SimpleSpringListener() {
                    @Override
                    public void onSpringUpdate(Spring spring) {
                        setDepthProgress((float) spring.getCurrentValue());
                    }
                });

        popAnimationSpring = springSystem.createSpring()
                .setSpringConfig(SpringConfig.fromBouncinessAndSpeed(9, 10))
                .addListener(new SimpleSpringListener() {
                    @Override
                    public void onSpringUpdate(Spring spring) {
                        setPopAnimationProgress((float) spring.getCurrentValue());
                    }
                });
    }

    // xrotation transition

    public void xrotation(boolean on) {
        xrotationSpring.setEndValue(on ? 1 : 0);
    }

    public void setXrotationProgress(float progress) {    button.setRotationX(progress);
    }

    public void setMessageTranslationProgress(float progress) {    messageView.setTranslationX(progress);
    }

    public void setStatusTranslationProgress(float progress) {
        status.setTranslationX(progress);
//        button.setTranslationX(progress);
    }

    public void messagetranslation(boolean on) {
        messageSpring.setEndValue(on ? 1 : 0);
    }


    // yrotation transition

    public void yrotation(boolean on) {
        yrotationSpring.setEndValue(on ? 1 : 0);
    }

    public void setYrotationProgress(float progress) {    button.setRotationY(progress);
    }

    public void setXTranslationProgress(float progress) {button.setTranslationX(progress);}

    public void setYTranslationProgress(float progress) {
        button.setTranslationY(progress);
    }

    // depth transition

    public void depth(boolean on) {
        depthSpring.setEndValue(on ? 1 : 0);
    }

    public void setDepthProgress(float progress) {
        button.setScaleX(transition(progress, 1, 0.9f));
        button.setScaleY(transition(progress, 1, 0.9f));
    }

    // popAnimation transition

    public void popAnimation(boolean on) {
        popAnimationSpring.setEndValue(on ? 1 : 0);
    }

    public void setPopAnimationProgress(float progress) {
        float reverse2 = transition(progress, 1f, 0f);
        ArgbEvaluator evaluator = new ArgbEvaluator();
        Integer newColor = (Integer)evaluator.evaluate(progress, getResources().getColor(R.color.giveItDark), getResources().getColor(R.color.giveItLight));
        if (newColor > -1000000 ){
            newColor = getResources().getColor(R.color.giveItDark);
        }
        button.setBackgroundColor(newColor);
    }

    // Utilities

    public float transition (float progress, float startValue, float endValue) {
        return (float) SpringUtil.mapValueFromRangeToRange(progress, 0, 1, startValue, endValue);
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
                    Intent intent = new Intent(GiveIt.this, RegistrationIntentService.class);
                    intent.putExtra("SERVER_TOKEN", mToken);
                    Log.d(TAG, "STARTING SERVICE INTENT");
                    startService(intent);
                }

                tokenReady = true;
                checkStatus();
                // Access user here after response is parsed
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                showError("COULDN'T CONNECT TO SERVER");
                retrofitError.printStackTrace();
                // Log error here since request failed
            }
        });

    }

    private String getId() {
        return Installation.id(this);
    }

    private View status;
    private TextView messageView;
    private boolean serverError = false;

    private void showError(String message) {
        serverError = true;
        showMessage(message);
    }

    private void showMessage(String message) {
        messageView.setText(message);
        messageSpring.setEndValue(0f);
        statusSpring.setEndValue(-width);
        xTranslationSpring.setEndValue(-width);
/*        final TextView text =  (TextView) findViewById(R.id.statusText);
        final TextView messageView = (TextView) findViewById(R.id.messageText);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 60);

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
                text.setText("YOU'VE GOT IT");
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
        messageView.startAnimation(messageIn);*/

    }

    private boolean givingIt = false;
    private boolean dodgingIt = false;
    private boolean resettable = false;


    private void showGivingIt() {
        givingIt = true;
//        dodgingIt = true;
//        messageView.setText("GIVING IT");

        Button giveItButton = (Button) findViewById(R.id.give_it);
//        giveItButton.setText("GIVING IT");
        showMessage("GIVING IT");

//        messageSpring.setEndValue(0f);
//        statusSpring.setEndValue(-width);
/*        final TextView text =  (TextView) findViewById(R.id.statusText);
        final TextView messageView = (TextView) findViewById(R.id.messageText);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 70);

        Animation statusOut = AnimationUtils.loadAnimation(this, R.anim.slide_out);
        statusOut.setFillAfter(false);
        Animation messageIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        messageIn.setFillAfter(false);

        statusOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                text.setText("GIVING IT");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        messageView.setText("GIVING IT");
        messageView.setVisibility(View.VISIBLE);

        text.startAnimation(statusOut);
        messageView.startAnimation(messageIn);
*/
    }


    private void tryGiveIt() {

        if (!locationReady) {
            showError("COULDN'T IDENTIFY LOCATION");
//            Toast.makeText(GetIt.this, "Couldn't identify location", Toast.LENGTH_SHORT).show();

        } else if (!tokenReady) {

            tryCheckStatus();
//            Toast.makeText(GetIt.this, "Couldn't connect to Get It server", Toast.LENGTH_SHORT).show();

        } else {

            View giveItButton = findViewById(R.id.give_it);
//            giveItButton.setVisibility(View.INVISIBLE);

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GiveIt.this);

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

            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            GetItLocation location = new GetItLocation(mLocation.getLatitude(), mLocation.getLongitude());
//                GetItLocation location = new GetItLocation(0, 0);

            service.giveIt(location, new Callback<Boolean>() {
                @Override
                public void success(Boolean serverGotIt, Response response) {
                    showGivingIt();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkGaveIt();
                        }
                    }, 5000);
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    showError("COULDN'T CONNECT TO SERVER");
//                    Toast.makeText(GetIt.this, "Couldn't connect to Get It server", Toast.LENGTH_SHORT).show();
//                    resetGiveItButton();
//                    showGiveIt();
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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
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
