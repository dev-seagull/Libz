package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.Image;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.BubbleDataEntry;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.charts.Scatter;
import com.anychart.core.Text;
import com.anychart.core.cartesian.series.Bubble;
import com.anychart.data.Set;
import com.anychart.enums.TooltipPositionMode;
import com.example.cso.DBHelper;
import com.example.cso.DeviceHandler;
import com.example.cso.DeviceStatusSync;
import com.example.cso.MainActivity;
import com.example.cso.R;
import com.example.cso.StorageHandler;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Details {

    public static LinearLayout createDetailsLayout(Context context) {
        LinearLayout layout = new LinearLayout(context);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        params.setMargins(40,16,40,0);
        layout.setLayoutParams(params);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        layout.setElevation(4f);
        GradientDrawable gradientDrawable = UI.createBorderInnerLayoutDrawable(context);
        layout.setBackground(gradientDrawable);
        layout.setVisibility(View.GONE);
        return layout;
    }

    public static LinearLayout createInnerDetailsLayout(Context context) {
        LinearLayout layout = new LinearLayout(context);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                UI.getDeviceWidth(context),
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        layout.setLayoutParams(params);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.setElevation(4f);
        GradientDrawable gradientDrawable = UI.createBorderInnerLayoutDrawable(context);
        layout.setBackground(gradientDrawable);
        return layout;
    }

    public static FrameLayout getDetailsView(Button button){
        ViewParent deviceButtonView = button.getParent();
        if (deviceButtonView instanceof RelativeLayout){
            RelativeLayout deviceButtonRelativeLayout = (RelativeLayout) deviceButtonView;
            ViewParent parentLayout = deviceButtonRelativeLayout.getParent();
            if (parentLayout instanceof LinearLayout){
                LinearLayout parentLinearLayout = (LinearLayout) parentLayout;
                int deviceViewChildrenCount = parentLinearLayout.getChildCount();
                for (int j = 0; j < deviceViewChildrenCount; j++) {
                    View view = parentLinearLayout.getChildAt(j);
                    if (!(view instanceof RelativeLayout)) {
                        return (FrameLayout) view;
                    }
                }
            }
        }
        return null;
    }

    public static ImageButton createRightArrowButton(Context context){
        ImageButton rightImageButton = new ImageButton(context);
        rightImageButton.setImageResource(R.drawable.right);
        rightImageButton.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams rightButtonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rightImageButton.setPadding(0,0,25,0);
        rightImageButton.setScaleX(1.75f);
        rightImageButton.setScaleY(1.75f);
        rightImageButton.setLayoutParams(rightButtonParams);
        FrameLayout.LayoutParams rightButtonLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rightButtonLayoutParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        rightImageButton.setLayoutParams(rightButtonLayoutParams);
        return rightImageButton;
    }

    public static ImageButton createLeftArrowButton(Context context){
        ImageButton leftImageButton = new ImageButton(context);
        leftImageButton.setImageResource(R.drawable.left);
        leftImageButton.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams leftButtonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        leftImageButton.setPadding(25,0,0,0);
        leftImageButton.setScaleX(1.75f);
        leftImageButton.setScaleY(1.75f);
        leftImageButton.setLayoutParams(leftButtonParams);
        FrameLayout.LayoutParams leftButtonLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        leftButtonLayoutParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        leftImageButton.setLayoutParams(leftButtonLayoutParams);
        return leftImageButton;
    }

    public static FrameLayout createFrameLayoutForButtonDetails(Context context, String type, String buttonId){
        FrameLayout frameLayout = new FrameLayout(context);
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        frameParams.setMargins(40,16,40,0);
        frameLayout.setLayoutParams(frameParams);
        frameLayout.setElevation(4f);
        frameLayout.setVisibility(View.GONE);


        ViewPager2 viewPager = DetailsViewPager.createViewerPage(context, buttonId, type);
        frameLayout.addView(viewPager);
        viewPager.setOffscreenPageLimit(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT);  // Only keep one page on either side of the current one

        ImageButton leftArrowButton = createLeftArrowButton(context);
        leftArrowButton.setOnClickListener(v -> {
            int previousPage = viewPager.getCurrentItem() - 1;
            viewPager.setCurrentItem(previousPage, true);
        });
        frameLayout.addView(leftArrowButton);

        ImageButton rightArrowButton = createRightArrowButton(context);
        rightArrowButton.setOnClickListener(v -> {
            int nextPage = viewPager.getCurrentItem() + 1;
            viewPager.setCurrentItem(nextPage, true);
        });
        frameLayout.addView(rightArrowButton);

        return frameLayout;

    }

    public static void createTitleTextView(Context context,LinearLayout layout, String text) {
        TextView title = new TextView(context);
        title.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        title.setLayoutParams(params);

        layout.addView(title);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        title.setLayoutParams(titleParams);
        title.setText(text);
        title.setContentDescription("title");
        title.setPadding(0,50,0,0);
        title.setTextColor(MainActivity.currentTheme.primaryTextColor);
    }

    public static TextView createUpdateTimeTextView(Context context, String deviceId) {
        String updateTime = DeviceStatusSync.getDeviceStatusLastUpdateTime(deviceId);
        TextView title = new TextView(context);
        title.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        title.setPadding(0,0,0,10);
        title.setLayoutParams(params);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
        title.setLayoutParams(titleParams);
        title.setText(updateTime);
        title.setTextColor(MainActivity.currentTheme.primaryTextColor);
        title.setContentDescription("updateTime");
        title.setScaleX(0.75f);
        title.setScaleY(0.75f);
        return title;
    }

    public static void addUpdateTimeTextView(LinearLayout layout, String deviceId){
        String updateTime = DeviceStatusSync.getDeviceStatusLastUpdateTime(deviceId);
        int childCount = layout.getChildCount();
        for (int i = 0; i < childCount; i++){
            View child = layout.getChildAt(i);
            CharSequence contentDescription = child.getContentDescription();
            if(contentDescription != null){
                if(contentDescription.toString().equalsIgnoreCase("title")) {
                    TextView title = (TextView) child;
                    title.setText(title.getText() + " " +  updateTime);
                    break;
                }
            }
        }
    }




    public static TextView getErrorAsChartAlternative(Context context){
        TextView view = new TextView(context);

        view.setText("Unable to get Data");
        view.setTextColor(MainActivity.currentTheme.warningTextColor);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,128);
        params.setMargins(0,64,0,64);
        view.setGravity(Gravity.CENTER);
        view.setLayoutParams(params);
        return view;
    }

}









