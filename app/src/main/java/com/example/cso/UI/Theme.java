package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.LinearLayout;

import com.example.cso.R;

import com.example.cso.MainActivity;

import java.util.ArrayList;

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
    public int OnSyncButtonGradientStart;
    public int OnSyncButtonGradientEnd;
    public int OffSyncButtonGradientStart;
    public int OffSyncButtonGradientEnd;
    public int warningTextColor;
    public String name;
    public static ArrayList<Theme> themes = new ArrayList<>();

    public static void initializeThemes(){
        MainActivity.currentTheme = purpleTheme();
        whitePurpleTheme();
        blueTheme();
        grayTheme();
        blackTheme();
        grayTheme();
        greenTealTheme();
        Log.d("ui","theme size : "+ themes.size());
    }

    public Theme(String name, int primaryBackgroundColor, int[] deviceButtonColors, int[] accountButtonColors,
                 int[] addBackupAccountButtonColors, int[] deviceStorageChartColors,
                 int[] deviceAppStorageChartColors, int[] deviceAssetsSyncedStatusChartColors,
                 int[] accountStorageDataChartColors, int menuTextColor, int primaryTextColor,
                 int onSyncButtonGradientStart, int onSyncButtonGradientEnd,
                 int offSyncButtonGradientStart, int offSyncButtonGradientEnd, int warningTextColor) {
        this.name = name;
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
        this.OnSyncButtonGradientStart = onSyncButtonGradientStart;
        this.OnSyncButtonGradientEnd = onSyncButtonGradientEnd;
        this.OffSyncButtonGradientStart = offSyncButtonGradientStart;
        this.OffSyncButtonGradientEnd = offSyncButtonGradientEnd;
        this.warningTextColor = warningTextColor;
        themes.add(this);
    }

    public static Theme purpleTheme() {
        return new Theme(
                "purple",
                Color.parseColor("#2C2A4A"),
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
                Color.WHITE,
                Color.parseColor("#004D40"),
                Color.parseColor("#80CBC4"),
                Color.parseColor("#90A4AE"),
                Color.parseColor("#B0BEC5"),
                Color.parseColor("#FF5722")
        );
    }

    public static Theme whitePurpleTheme() {
        return new Theme(
                "whitePurple",
                Color.WHITE, // Primary background color set to white
                new int[] {
                        Color.parseColor("#6D74A1"), // Device button colors
                        Color.parseColor("#4F518C"),
                },
                new int[] {
                        Color.parseColor("#907AD6"), // Account button colors
                        Color.parseColor("#6D5F9C"),
                },
                new int[] {
                        Color.parseColor("#907AD6"), // Add backup account button colors
                        Color.parseColor("#6D5F9C"),
                },
                new int[] {
                        Color.parseColor("#00796B"), // Device storage chart colors
                        Color.parseColor("#004D40"),
                        Color.parseColor("#80CBC4")
                },
                new int[] {
                        Color.parseColor("#00796B"), // Device app storage chart colors
                        Color.parseColor("#004D40"),
                        Color.parseColor("#80CBC4")
                },
                new int[] {
                        Color.parseColor("#00796B"), // Device assets synced status chart colors
                        Color.parseColor("#004D40"),
                        Color.parseColor("#80CBC4")
                },
                new int[] {
                        Color.parseColor("#1E88E5"), // Account storage data chart colors
                        Color.parseColor("#304194")
                },
                Color.parseColor("#202124"), // Menu text color
                Color.BLACK, // Primary text color (contrast with white background)
                Color.parseColor("#004D40"), // On sync button gradient start
                Color.parseColor("#80CBC4"), // On sync button gradient end
                Color.parseColor("#90A4AE"), // Off sync button gradient start
                Color.parseColor("#B0BEC5"), // Off sync button gradient end
                Color.parseColor("#FF5722")  // Warning text color
        );
    }

    public static Theme blueTheme() {
        return new Theme(
                "blue",
                Color.parseColor("#E3F2FD"), // Light blue primary background
                new int[] {
                        Color.parseColor("#42A5F5"), // Device button colors
                        Color.parseColor("#1E88E5"),
                },
                new int[] {
                        Color.parseColor("#64B5F6"), // Account button colors
                        Color.parseColor("#1976D2"),
                },
                new int[] {
                        Color.parseColor("#64B5F6"), // Add backup account button colors
                        Color.parseColor("#1976D2"),
                },
                new int[] {
                        Color.parseColor("#29B6F6"), // Device storage chart colors
                        Color.parseColor("#0288D1"),
                        Color.parseColor("#81D4FA")
                },
                new int[] {
                        Color.parseColor("#29B6F6"), // Device app storage chart colors
                        Color.parseColor("#0288D1"),
                        Color.parseColor("#81D4FA")
                },
                new int[] {
                        Color.parseColor("#29B6F6"), // Device assets synced status chart colors
                        Color.parseColor("#0288D1"),
                        Color.parseColor("#81D4FA")
                },
                new int[] {
                        Color.parseColor("#1565C0"), // Account storage data chart colors
                        Color.parseColor("#0D47A1")
                },
                Color.parseColor("#202124"), // Menu text color
                Color.BLACK, // Primary text color
                Color.parseColor("#0288D1"), // On sync button gradient start
                Color.parseColor("#81D4FA"), // On sync button gradient end
                Color.parseColor("#B0BEC5"), // Off sync button gradient start
                Color.parseColor("#90A4AE"), // Off sync button gradient end
                Color.parseColor("#FF5722")  // Warning text color
        );
    }

    public static Theme greenTealTheme() {
        return new Theme(
                "greenTeal",
                Color.parseColor("#E0F2F1"), // Light teal background
                new int[] {
                        Color.parseColor("#00796B"), // Device button colors
                        Color.parseColor("#004D40"),
                },
                new int[] {
                        Color.parseColor("#4DB6AC"), // Account button colors
                        Color.parseColor("#00796B"),
                },
                new int[] {
                        Color.parseColor("#4DB6AC"), // Add backup account button colors
                        Color.parseColor("#00796B"),
                },
                new int[] {
                        Color.parseColor("#00796B"), // Device storage chart colors
                        Color.parseColor("#004D40"),
                        Color.parseColor("#80CBC4")
                },
                new int[] {
                        Color.parseColor("#00796B"), // Device app storage chart colors
                        Color.parseColor("#004D40"),
                        Color.parseColor("#80CBC4")
                },
                new int[] {
                        Color.parseColor("#00796B"), // Device assets synced status chart colors
                        Color.parseColor("#004D40"),
                        Color.parseColor("#80CBC4")
                },
                new int[] {
                        Color.parseColor("#1DE9B6"), // Account storage data chart colors
                        Color.parseColor("#00BFA5")
                },
                Color.parseColor("#202124"), // Menu text color
                Color.BLACK, // Primary text color
                Color.parseColor("#004D40"), // On sync button gradient start
                Color.parseColor("#80CBC4"), // On sync button gradient end
                Color.parseColor("#B0BEC5"), // Off sync button gradient start
                Color.parseColor("#90A4AE"), // Off sync button gradient end
                Color.parseColor("#FF5722")  // Warning text color
        );
    }

    public static Theme blackTheme() {
        return new Theme(
                "black",
                Color.BLACK, // Primary background color set to black
                new int[] {
                        Color.parseColor("#424242"), // Device button colors (dark gray)
                        Color.parseColor("#616161"),
                },
                new int[] {
                        Color.parseColor("#757575"), // Account button colors
                        Color.parseColor("#9E9E9E"),
                },
                new int[] {
                        Color.parseColor("#757575"), // Add backup account button colors
                        Color.parseColor("#9E9E9E"),
                },
                new int[] {
                        Color.parseColor("#37474F"), // Device storage chart colors
                        Color.parseColor("#263238"),
                        Color.parseColor("#546E7A")
                },
                new int[] {
                        Color.parseColor("#37474F"), // Device app storage chart colors
                        Color.parseColor("#263238"),
                        Color.parseColor("#546E7A")
                },
                new int[] {
                        Color.parseColor("#37474F"), // Device assets synced status chart colors
                        Color.parseColor("#263238"),
                        Color.parseColor("#546E7A")
                },
                new int[] {
                        Color.parseColor("#757575"), // Account storage data chart colors
                        Color.parseColor("#424242")
                },
                Color.parseColor("#E0E0E0"), // Menu text color (light gray)
                Color.WHITE, // Primary text color (for contrast)
                Color.parseColor("#212121"), // On sync button gradient start
                Color.parseColor("#484848"), // On sync button gradient end
                Color.parseColor("#616161"), // Off sync button gradient start
                Color.parseColor("#757575"), // Off sync button gradient end
                Color.parseColor("#FF5722")  // Warning text color
        );
    }

    public static Theme grayTheme() {
        return new Theme(
                "gray",
                Color.parseColor("#F5F5F5"), // Light gray background
                new int[] {
                        Color.parseColor("#9E9E9E"), // Device button colors
                        Color.parseColor("#BDBDBD"),
                },
                new int[] {
                        Color.parseColor("#B0BEC5"), // Account button colors
                        Color.parseColor("#90A4AE"),
                },
                new int[] {
                        Color.parseColor("#B0BEC5"), // Add backup account button colors
                        Color.parseColor("#90A4AE"),
                },
                new int[] {
                        Color.parseColor("#9E9E9E"), // Device storage chart colors
                        Color.parseColor("#BDBDBD"),
                        Color.parseColor("#CFD8DC")
                },
                new int[] {
                        Color.parseColor("#9E9E9E"), // Device app storage chart colors
                        Color.parseColor("#BDBDBD"),
                        Color.parseColor("#CFD8DC")
                },
                new int[] {
                        Color.parseColor("#9E9E9E"), // Device assets synced status chart colors
                        Color.parseColor("#BDBDBD"),
                        Color.parseColor("#CFD8DC")
                },
                new int[] {
                        Color.parseColor("#78909C"), // Account storage data chart colors
                        Color.parseColor("#455A64")
                },
                Color.parseColor("#202124"), // Menu text color
                Color.BLACK, // Primary text color
                Color.parseColor("#B0BEC5"), // On sync button gradient start
                Color.parseColor("#CFD8DC"), // On sync button gradient end
                Color.parseColor("#90A4AE"), // Off sync button gradient start
                Color.parseColor("#B0BEC5"), // Off sync button gradient end
                Color.parseColor("#FF5722")  // Warning text color
        );
    }

    public static void applyTheme(Theme theme){
        MainActivity.currentTheme = theme;
        LinearLayout mainLayout = MainActivity.activity.findViewById(R.id.mainLayout);
        mainLayout.removeAllViews();
        UI.initAppUI(MainActivity.activity);
    }
}
