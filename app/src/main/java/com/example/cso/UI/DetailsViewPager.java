package com.example.cso.UI;

import android.content.Context;
import android.graphics.Color;
import android.widget.LinearLayout;

import androidx.viewpager2.widget.ViewPager2;

import com.example.cso.LogHandler;
import com.example.cso.MainActivity;

public class DetailsViewPager {

    public static ViewPager2 createViewerPage(Context context, String buttonId, String type){
        ViewPager2 viewPager = new ViewPager2(MainActivity.activity);
        try{
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            );
            params.setMargins(30,30,30, 30);
            viewPager.setLayoutParams(params);
            viewPager.setBackgroundColor(Color.WHITE);
            viewPager.setContentDescription("account");
            setupViewPager(context, viewPager, buttonId,type);
        }catch (Exception e){
            LogHandler.crashLog(e,"ui");
        }
        return viewPager;
    }

    private static void setupViewPager(Context context, ViewPager2 viewPager, String buttonId, String type) {
        try{
            PagerAdapter adapter = new PagerAdapter(context,0,type,buttonId);
            viewPager.setAdapter(adapter);
        }catch (Exception e){
            LogHandler.crashLog(e,"ui");
        }
    }
}
