package com.example.cso.UI;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
            viewPager.setLayoutParams(params);
            setupViewPager(context, viewPager, buttonId,type);
            FrameLayout.LayoutParams pagerLayoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            pagerLayoutParams.gravity = Gravity.CENTER;
            viewPager.setLayoutParams(pagerLayoutParams);
        }catch (Exception e){
            LogHandler.crashLog(e,"ui");
        }
        return viewPager;
    }

    private static void setupViewPager(Context context, ViewPager2 viewPager, String buttonId, String type) {
        try{
            int page = 0;
            if (type.equals("device")){
                page = PagerAdapter.totalPagesForDevice * 10 + 1 ;
            }else if(type.equals("account")){
                page = PagerAdapter.totalPagesForAccount * 10 + 1 ;
            }
            PagerAdapter adapter = new PagerAdapter(context,page,type,buttonId);
            viewPager.setAdapter(adapter);
        }catch (Exception e){
            LogHandler.crashLog(e,"ui");
        }
    }
}
