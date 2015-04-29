package com.alexjing.loadingdrawable.demo;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.alexjing.loadingdrawable.LoadingDrawable;


public class MainActivity extends ActionBarActivity {

    private LoadingDrawable mSuccessDrawable;
    private LoadingDrawable mErrorDrawable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSuccessDrawable = new LoadingDrawable(this);
        mErrorDrawable = new LoadingDrawable(this);
        findViewById(R.id.success_v).setBackgroundDrawable(mSuccessDrawable);
        findViewById(R.id.error_v).setBackgroundDrawable(mErrorDrawable);
        findViewById(R.id.start_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSuccessDrawable.start();
                mErrorDrawable.start();
            }
        });
        findViewById(R.id.stop_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSuccessDrawable.setLoadingState(LoadingDrawable.LoadingState.SUCCESS);
                mErrorDrawable.setLoadingState(LoadingDrawable.LoadingState.ERROR);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
}
