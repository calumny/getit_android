package com.get;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
//import android.support.v7.app.AppCompatActivity;

//import android.location.LocationListener;

public class ResetActivity extends Activity {

    private boolean hadIt = false;
    private boolean gotIt = false;
    private int total;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_reset);

        Intent intent = getIntent();
        hadIt = intent.getBooleanExtra("HAD_IT", false);
        gotIt = intent.getBooleanExtra("HAS_IT", false);
        total = intent.getIntExtra("TOTAL_COUNT", 0);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!gotIt) {
                    Intent giveIt = new Intent(ResetActivity.this, GetIt.class);
                    startActivity(giveIt);
                    overridePendingTransition(R.anim.activity_slide_in, R.anim.activity_slide_out);
                    finish();
                } else {
                    Intent giveIt = new Intent(ResetActivity.this, GiveIt.class);
                    startActivity(giveIt);
                    overridePendingTransition(R.anim.activity_slide_in, R.anim.activity_slide_out);
                    finish();
                }

            }
        }, 5000);

    }


}
