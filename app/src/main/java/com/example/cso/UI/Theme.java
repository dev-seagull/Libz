package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.widget.LinearLayout;

import com.example.cso.R;

public class Theme {
    public int primaryBackgroundColor;
    public int[] deviceButtonColors;
    public int[] accountButtonColors;
    public int[] addBackupAccountButtonColors;
    public int[] deviceStorageChartColors;
    public int[] deviceAppStorageChartColors;
    public int[] deviceAssetsSyncedStatusChartColors;
    public int[] accountStorageDataChartColors;
    public int menuTextColor;
    public int primaryTextColor;


    public Theme(int primaryBackgroundColor, int[] deviceButtonColors,
                 int[] accountButtonColors, int[] addBackupAccountButtonColors,
                 int[] deviceStorageChartColors, int[] deviceAppStorageChartColors,
                 int[] deviceAssetsSyncedStatusChartColors, int[] accountStorageDataChartColors,
                 int menuTextColor, int primaryTextColor) {
        this.primaryBackgroundColor = primaryBackgroundColor;
        this.deviceButtonColors = deviceButtonColors;
        this.accountButtonColors = accountButtonColors;
        this.addBackupAccountButtonColors = addBackupAccountButtonColors;
        this.deviceStorageChartColors = deviceStorageChartColors;
        this.deviceAppStorageChartColors = deviceAppStorageChartColors;
        this.deviceAssetsSyncedStatusChartColors = deviceAssetsSyncedStatusChartColors;
        this.accountStorageDataChartColors = accountStorageDataChartColors;
        this.menuTextColor = menuTextColor;
        this.primaryTextColor = primaryTextColor;
    }


    public static Theme purpleTheme(Context context) {
        return new Theme(
                context.getResources().getColor(R.color.primary_background),
                new int[] {
                        Color.parseColor("#6D74A1"),
                        Color.parseColor("#4F518C"),
                },
                new int[] {
                        Color.parseColor("#907AD6"),
                        Color.parseColor("#6D5F9C"),
                },
                new int[] {
                        Color.parseColor("#907AD6"),
                        Color.parseColor("#6D5F9C"),
                },
                new int[] {
                    Color.parseColor("#00796B"),
                    Color.parseColor("#004D40"),
                    Color.parseColor("#80CBC4")
                },
                new int[] {
                        Color.parseColor("#00796B"),
                        Color.parseColor("#004D40"),
                        Color.parseColor("#80CBC4")
                },
                new int[] {
                        Color.parseColor("#00796B"),
                        Color.parseColor("#004D40"),
                        Color.parseColor("#80CBC4")
                },
                new int[] {
                        Color.parseColor("#1E88E5"),
                        Color.parseColor("#304194")
                },
                Color.parseColor("#202124"),
                Color.WHITE
        );
    }

    public static void applyTheme(Theme currentTheme, Activity activity){
        applyThemeToPrimaryBackground(currentTheme, activity);
    }

    public static void applyThemeToPrimaryBackground(Theme currentTheme, Activity activity){
        LinearLayout primaryBackgroundLinearLayout = activity.findViewById(R.id.primaryBackground);
        primaryBackgroundLinearLayout.setBackgroundColor(currentTheme.primaryBackgroundColor);
    }
}
