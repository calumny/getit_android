package com.get;

import android.animation.ArgbEvaluator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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

import org.joda.time.DateTime;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

//import android.support.v7.app.AppCompatActivity;

//import android.location.LocationListener;

public class StatsActivity extends Activity {

    private boolean gotIt;

    private boolean tokenReady = false;

    private String mToken = "";

    private Integer mChildren = 0;

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "GetIt";

    private CountDownTimer countDownTimer;
    private int mInterval = 15000; // 15 seconds by default, can be changed later
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(StatsActivity.this);
// then you use
        Boolean gotIt = prefs.getBoolean(getResources().getString(R.string.prefs_got_it_key), false);
        String token = prefs.getString(getResources().getString(R.string.prefs_token_key), "");

        if (gotIt) {
            setContentView(R.layout.stats_page);
            mToken = token;
            getGenerationCounts();
        } else {
            setTheme(R.style.RedAppTheme);
            setContentView(R.layout.red_stats_page);
        }

        status =  findViewById(R.id.statusText);

        mHandler = new Handler();
        startCheckingCountdown();
    }


    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                getStats(); //this function can change value of mInterval.
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    void startCheckingCountdown() {
        mStatusChecker.run();
    }

    void stopCheckingCountdown() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    @Override
    public void onResume() {
        startCheckingCountdown();
        super.onResume();
    }

    @Override
    public void onPause() {
        stopCheckingCountdown();
        super.onPause();
    }

    @Override
    public void onStop() {
        stopCheckingCountdown();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        stopCheckingCountdown();
        super.onDestroy();
    }

    private void getGenerationCounts() {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(StatsActivity.this);

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

                    TextView generationView = new TextView(StatsActivity.this);

                    generationView.setText("YOU'VE GIVEN IT TO 0 PEOPLE");
//                    generationView.setTextAppearance(GiveIt.this, R.style.Base_TextAppearance_AppCompat_Medium);
                    generationView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
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

                        TextView generationView = new TextView(StatsActivity.this);

                        generationView.setText(generationText);
                        generationView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
//                        generationView.setTextAppearance(GiveIt.this, R.style.Base_TextAppearance_AppCompat_Medium);
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

    private void getStats() {

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(getResources().getString(R.string.api_endpoint))
                .build();

        GetItService service = restAdapter.create(GetItService.class);

        service.getStats(new Callback<Statistics>() {
            @Override
            public void success(Statistics statistics, Response response) {

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                final Date resetDate;
                Date currentDate = new Date();
                try {
                    resetDate = dateFormat.parse(statistics.getResetDateString());
                    updateCountdown(resetDate);

                    Calendar calendar = Calendar.getInstance();
                    long now = calendar.getTimeInMillis();

                    //milliseconds
                    long different = resetDate.getTime() - now;

                    countDownTimer = new CountDownTimer(different, 1000) {

                        public void onTick(long millisUntilFinished) {
                            updateCountdown(resetDate);
                        }

                        public void onFinish() {

                        }
                    };
                    countDownTimer.start();

                    TextView total = (TextView) findViewById(R.id.totalText);
                    total.setText(String.format("%d PEOPLE HAVE IT", statistics.getHasItCount()));

                    TextView distance = (TextView) findViewById(R.id.distance);
                    distance.setText(String.format("%d MILES", statistics.getDistance().intValue()));

                    TextView mph = (TextView) findViewById(R.id.velocity);
                    mph.setText(String.format("%.2f MPH", statistics.getMilesPerHour()));

                    TextView playersPerSecond = (TextView) findViewById(R.id.frequency);
                    float timePerPlayer = 1/statistics.getPlayersPerSecond();
                    if (timePerPlayer > 60) {
                        timePerPlayer = timePerPlayer/60;
                        if (timePerPlayer>60) {
                            timePerPlayer = timePerPlayer/60;
                            if (timePerPlayer >24) {
                                timePerPlayer = timePerPlayer/24;
                                playersPerSecond.setText(String.format("%.2f DAYS", (timePerPlayer)));
                            } else {
                                playersPerSecond.setText(String.format("%.2f HOURS", (timePerPlayer)));
                            }
                        } else {
                            playersPerSecond.setText(String.format("%.2f MINUTES", (timePerPlayer)));
                        }
                    } else {
                        playersPerSecond.setText(String.format("%.2f SECONDS", (timePerPlayer)));
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                retrofitError.printStackTrace();
                // Log error here since request failed
            }
        });
    }

    private void updateCountdown(Date date) {

        Date nowDate = new Date();
        long now = nowDate.getTime();

        //milliseconds
        long different = date.getTime() - now;

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;

        String remainingTime = String.format("%dd %dh %dm %ds",
                elapsedDays,
                elapsedHours, elapsedMinutes, elapsedSeconds);

        TextView resetDateView = (TextView) findViewById(R.id.countdownText);

        resetDateView.setText(remainingTime);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_slide_in_reverse, R.anim.activity_slide_out_reverse);
    }

    private String getId() {
        return Installation.id(this);
    }

    private View status;
    private TextView messageView;
    private boolean serverError = false;

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

        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            overridePendingTransition(R.anim.activity_slide_in_reverse, R.anim.activity_slide_out_reverse);
            return true;
        }

        return false;
//        return super.onOptionsItemSelected(item);
    }
}
