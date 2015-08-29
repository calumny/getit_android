package com.getit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.joda.time.DateTime;

import java.util.List;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

//import android.location.LocationListener;

public class GiveIt extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private boolean gotIt;

    private boolean locationReady = false;

    private boolean tokenReady = false;

    private String mToken = "";

    private GoogleApiClient mGoogleApiClient;

    private Location mLocation;

    private LocationRequest mLocationRequest;

    private Integer mChildren = 0;

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

        gotIt = true;
        setContentView(R.layout.activity_got_it);
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
                    getGenerationCounts();
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
                    showMessage(message);
                } else {
                    showMessage("NOBODY GOT IT");
                }

                mChildren = children;

            }

            @Override
            public void failure(RetrofitError retrofitError) {
                showMessage("COULDN'T CONNECT TO SERVER");
                retrofitError.printStackTrace();
                // Log error here since request failed
            }
        });

    }

    private void showGiveIt() {

        View giveItButton = findViewById(R.id.give_it);
        giveItButton.setVisibility(View.VISIBLE);

    }

    private void getGenerationCounts() {

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

        service.getGenerations(new Callback<List<Generation>>() {
            @Override
            public void success(List<Generation> generations, Response response) {

                Boolean firstGeneration = true;
                Boolean prevSingle = false;
                LinearLayout generationsLayout = (LinearLayout) findViewById(R.id.generations);
                generationsLayout.removeAllViews();

                if (generations.size() == 0) {

                    mChildren = 0;

                    TextView generationView = new TextView(GiveIt.this);

                    generationView.setText("YOU'VE GIVEN IT TO 0 PEOPLE");
                    generationView.setTextAppearance(GiveIt.this, R.style.Base_TextAppearance_AppCompat_Medium);
                    generationView.setTextColor(getResources().getColor(R.color.gotItText));

                    generationsLayout.addView(generationView);

                } else {

                    mChildren = generations.get(0).getCount();

                    for (Generation generation : generations) {

                        int count = generation.getCount();

                        String locale = "WHO'VE GIVEN IT TO %d PEOPLE";

                        if (firstGeneration && count != 1) {
                            locale = "YOU'VE GIVEN IT TO %d PEOPLE";
                            prevSingle = false;
                        } else if (firstGeneration) {
                            locale = "YOU'VE GIVEN IT TO %d PERSON";
                            prevSingle = true;
                        } else if (count != 1) {
                            if (prevSingle) {
                                locale = "WHO'S GIVEN IT TO %d PEOPLE";
                            }
                            prevSingle = false;
                        } else {
                            if (prevSingle) {
                                locale = "WHO'S GIVEN IT TO %d PERSON";
                            }
                            locale = "WHO'VE GIVEN IT TO %d PERSON";
                            prevSingle = true;
                        }

                        firstGeneration = false;

                        String generationText = String.format(locale, count);

                        TextView generationView = new TextView(GiveIt.this);

                        generationView.setText(generationText);
                        generationView.setTextAppearance(GiveIt.this, R.style.Base_TextAppearance_AppCompat_Medium);
                        generationView.setTextColor(getResources().getColor(R.color.gotItText));

                        generationsLayout.addView(generationView);

                    }
                }

            }

            @Override
            public void failure(RetrofitError retrofitError) {
                retrofitError.printStackTrace();
                // Log error here since request failed
            }
        });

    }

    private void setButtonListener() {

        Button giveItButton = (Button) findViewById(R.id.give_it);
        giveItButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryGiveIt();
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

    private void tryGiveIt() {

        if (!locationReady) {
            showMessage("COULDN'T IDENTIFY LOCATION");
//            Toast.makeText(GetIt.this, "Couldn't identify location", Toast.LENGTH_SHORT).show();

        } else if (!tokenReady) {

            tryCheckStatus();
//            Toast.makeText(GetIt.this, "Couldn't connect to Get It server", Toast.LENGTH_SHORT).show();

        } else {

            View giveItButton = findViewById(R.id.give_it);
            giveItButton.setVisibility(View.INVISIBLE);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkGaveIt();
                }
            }, 5000);

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
                    if (serverGotIt) {

                    }
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    showMessage("COULDN'T CONNECT TO SERVER");
//                    Toast.makeText(GetIt.this, "Couldn't connect to Get It server", Toast.LENGTH_SHORT).show();
                    showGiveIt();
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
