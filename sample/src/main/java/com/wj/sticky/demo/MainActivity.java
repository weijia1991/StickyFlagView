package com.wj.sticky.demo;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.wj.sticky.StickyFlagView;
import com.wj.utils.ScreenUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final StickyFlagView stickyFlagView1 = (StickyFlagView) findViewById(R.id.sticky_view1);
        final StickyFlagView stickyFlagView2 = (StickyFlagView) findViewById(R.id.sticky_view2);
        stickyFlagView1.setOnFlagDisappearListener(new StickyFlagView.OnFlagDisappearListener() {
            @Override
            public void onFlagDisappear(StickyFlagView view) {
                Toast.makeText(MainActivity.this, "Flag have disappeared.", Toast.LENGTH_SHORT).show();
            }
        });
        stickyFlagView1.setFlagText("6");

        findViewById(R.id.btn_flag).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stickyFlagView1.setFlagText("6");
                stickyFlagView2.setFlagText(null);
            }
        });

        final RelativeLayout rl = (RelativeLayout) findViewById(R.id.rl);
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StickyFlagView sfv = new StickyFlagView(MainActivity.this);
                sfv.setFlagText("9");
                sfv.setFlagTextColor(Color.WHITE);
                sfv.setFlagTextSize(ScreenUtils.spTopx(MainActivity.this, 15));
                sfv.setFlagColor(Color.BLUE);
                sfv.setFlagRadius(ScreenUtils.dpToPx(MainActivity.this, 10));
                sfv.setMinStickRadius(ScreenUtils.dpToPx(MainActivity.this, 3));
                sfv.setMaxStickRadius(ScreenUtils.dpToPx(MainActivity.this, 8));
                sfv.setMaxDistance(ScreenUtils.dpToPx(MainActivity.this, 80));
//                sfv.setFlagDrawable(R.drawable.bubble);

                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        (int) ScreenUtils.dpToPx(MainActivity.this, 40),
                        (int) ScreenUtils.dpToPx(MainActivity.this, 40));
                lp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

                rl.addView(sfv, lp);
            }
        });
    }

}
