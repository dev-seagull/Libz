package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.LinearLayout;

import com.example.cso.R;

import com.example.cso.MainActivity;
import com.example.cso.SharedPreferencesHandler;

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
    public int toolbarBackgroundColor;
    public int toolbarElementsColor;
    public Drawable deviceIcon;
    public int threeDotButtonId;
    public int syncProgressTextColor;
    public String name;
    public static ArrayList<Theme> themes = new ArrayList<>();

    public static void initializeThemes(){
        whiteTheme();
        purpleTheme();
        blueTheme();
        grayTheme();
        blackTheme();
        greenTealTheme();
        darkTheme();
        darkBlueTheme();
        tealTheme();
        MainActivity.currentTheme = getThemeByName(SharedPreferencesHandler.getCurrentTheme());
        Log.d("ui","theme size : "+ themes.size());
    }

    public Theme(String name, int primaryBackgroundColor, int[] deviceButtonColors, int[] accountButtonColors,
                 int[] addBackupAccountButtonColors, int[] deviceStorageChartColors,
                 int[] deviceAppStorageChartColors, int[] deviceAssetsSyncedStatusChartColors,
                 int[] accountStorageDataChartColors, int menuTextColor, int primaryTextColor,
                 int onSyncButtonGradientStart, int onSyncButtonGradientEnd,
                 int offSyncButtonGradientStart, int offSyncButtonGradientEnd,int syncProgressTextColor,
                 int warningTextColor, int toolbarBackgroundColor,
                 int toolbarElementsColor, Drawable deviceIcon, int threeDotButtonId) {
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
        this.syncProgressTextColor = syncProgressTextColor;
        this.warningTextColor = warningTextColor;
        this.toolbarBackgroundColor = toolbarBackgroundColor;
        this.toolbarElementsColor = toolbarElementsColor;
        this.deviceIcon = deviceIcon;
        this.threeDotButtonId = threeDotButtonId;

        themes.add(this);
    }

    public static Theme purpleTheme() {
        return new Theme(
                "purple",
                Color.parseColor("#2C2A4A"), //primaryBackgroundColor
                new int[] {
                        Color.parseColor("#6D74A1"), //deviceButtonColors
                        Color.parseColor("#4F518C"),
                },
                new int[] {
                        Color.parseColor("#907AD6"), //accountButtonColors
                        Color.parseColor("#6D5F9C"),
                },
                new int[] {
                        Color.parseColor("#907AD6"), //addBackupAccountButtonColors
                        Color.parseColor("#6D5F9C"),
                },
                new int[] {
                    Color.parseColor("#B0BEC5"), //deviceStorageChartColors , total
                    Color.parseColor("#BBDEFB"), // used
                    Color.parseColor("#72098E"), //media
                    Color.parseColor("#FAB34B") // synced
                },
                new int[] {
                        Color.parseColor("#00796B"),
                        Color.parseColor("#388E3C"),
                        Color.parseColor("#80CBC4")
                },
                new int[] {
                        Color.parseColor("#00796B"),
                        Color.parseColor("#388E3C"),
                        Color.parseColor("#80CBC4")
                },
                new int[] {
                        Color.parseColor("#1E88E5"),
                        Color.parseColor("#304194")
                },
                Color.parseColor("#202124"),
                Color.WHITE,  //primaryTextColor
                Color.parseColor("#388E3C"), // onSyncButtonGradientStart (for sync progress)
                Color.parseColor("#80CBC4"), // onSyncButtonGradientEnd (for sync progress)
                Color.parseColor("#90A4AE"),
                Color.parseColor("#B0BEC5"),
                Color.parseColor("#00FFB3"), // syncProgressTextColor
                Color.parseColor("#FF5722"), // warningTextColor
                Color.parseColor("#6A5ACD"), // toolbar background
                Color.parseColor("#FFFFFF"), // toolbar elements
                MainActivity.activity.getResources().getDrawable((R.drawable.white_device)),
                R.drawable.three_dot_white
        );
    }

    public static Theme whiteTheme() {
        return new Theme(
                "white",
                Color.parseColor("#F7F9FC"), // Light subtle background color
                new int[] {
                        Color.parseColor("#F0F0F0"), // Device button background: soft gray
                        Color.parseColor("#EDEDED"), // Slightly darker for pressed state
                },
                new int[] {
                        Color.parseColor("#FFE4B5"), // Account button: muted pale gold
                        Color.parseColor("#FFD27F"), // Slightly darker accent for interactions
                },
                new int[] {
                        Color.parseColor("#FFD700"), // Add backup account button: gold highlight
                        Color.parseColor("#FFC107"), // Slightly deeper gold for contrast
                },
                new int[] {
                        Color.parseColor("#00897B"), // Device storage chart: teal green
                        Color.parseColor("#00574B"), // Darker teal for clarity
                        Color.parseColor("#4DB6AC"),  // Lighter teal for highlights
                        Color.parseColor("#4DB6AC")
                },
                new int[] {
                        Color.parseColor("#00796B"), // Device app storage chart: same green palette
                        Color.parseColor("#004D40"),
                        Color.parseColor("#80CBC4")
                },
                new int[] {
                        Color.parseColor("#00796B"), // Device assets synced chart
                        Color.parseColor("#004D40"),
                        Color.parseColor("#80CBC4")
                },
                new int[] {
                        Color.parseColor("#2196F3"), // Account storage data chart: cool blue
                        Color.parseColor("#1976D2")  // Slightly darker shade for differentiation
                },
                Color.parseColor("#212121"), // Menu text color: almost black for contrast
                Color.parseColor("#333333"), // Primary text color: dark gray for readability
                Color.parseColor("#26A69A"), // On sync button gradient start: bright teal
                Color.parseColor("#4DB6AC"), // On sync button gradient end: softer teal
                Color.parseColor("#B0BEC5"), // Off sync button gradient start: cool gray
                Color.parseColor("#CFD8DC"), // Off sync button gradient end: lighter gray
                Color.parseColor("#00E5FF"), // syncProgressTextColor
                Color.parseColor("#FF5252"),  // Warning text color: red for high visibility
                Color.parseColor("#5C6BC0"), // Toolbar background: soft, muted indigo
                Color.parseColor("#FFFFFF"), // toolbar elements
                MainActivity.activity.getResources().getDrawable((R.drawable.white_device)),
                R.drawable.three_dot_black
        );
    }


    public static Theme blueTheme() {
        return new Theme(
                "blue",
                Color.parseColor("#E3F2FD"), // Primary background color (light blue)
                new int[]{
                        Color.parseColor("#64B5F6"), // Device button colors
                        Color.parseColor("#42A5F5")
                },
                new int[]{
                        Color.parseColor("#2196F3"), // Account button colors (blue)
                        Color.parseColor("#1976D2")
                },
                new int[]{
                        Color.parseColor("#2196F3"), // Add backup account button colors
                        Color.parseColor("#1976D2")
                },
                new int[]{
                        Color.parseColor("#2196F3"), // Device storage chart colors (blue)
                        Color.parseColor("#1976D2"),
                        Color.parseColor("#BBDEFB"),
                        Color.parseColor("#BBDEFB")
                },
                new int[]{
                        Color.parseColor("#2196F3"), // Device app storage chart colors
                        Color.parseColor("#1976D2"),
                        Color.parseColor("#BBDEFB")
                },
                new int[]{
                        Color.parseColor("#2196F3"), // Device assets synced status chart colors
                        Color.parseColor("#1976D2"),
                        Color.parseColor("#BBDEFB")
                },
                new int[]{
                        Color.parseColor("#1565C0"), // Account storage data chart colors
                        Color.parseColor("#0D47A1")
                },
                Color.parseColor("#0D47A1"), // Menu text color (dark blue)
                Color.WHITE, // Primary text color (white)
                Color.parseColor("#1E88E5"), // On sync button gradient start
                Color.parseColor("#1565C0"), // On sync button gradient end
                Color.parseColor("#90A4AE"), // Off sync button gradient start
                Color.parseColor("#B0BEC5"), // Off sync button gradient end
                Color.parseColor("#00E5FF"), // syncProgressTextColor
                Color.parseColor("#FF5722"), // Warning text color (orange)
                Color.parseColor("#1E88E5"), // Toolbar background (blue)
                Color.parseColor("#FFFFFF"), // Toolbar elements (white)
                MainActivity.activity.getResources().getDrawable((R.drawable.black_device)), // Device icon
                R.drawable.three_dot_blue // Three dot button
        );
    }

    public static Theme darkTheme() {
        return new Theme(
                "dark",
                Color.parseColor("#121212"), // Primary background color (dark gray/black)
                new int[]{
                        Color.parseColor("#424242"), // Device button colors (dark gray)
                        Color.parseColor("#333333")
                },
                new int[]{
                        Color.parseColor("#607D8B"), // Account button colors (cool gray)
                        Color.parseColor("#455A64")
                },
                new int[]{
                        Color.parseColor("#607D8B"), // Add backup account button colors
                        Color.parseColor("#455A64")
                },
                new int[]{
                        Color.parseColor("#212121"), // Device storage chart colors (black)
                        Color.parseColor("#424242"),
                        Color.parseColor("#757575"),
                        Color.parseColor("#757575")
                },
                new int[]{
                        Color.parseColor("#212121"), // Device app storage chart colors
                        Color.parseColor("#424242"),
                        Color.parseColor("#757575")
                },
                new int[]{
                        Color.parseColor("#212121"), // Device assets synced status chart colors
                        Color.parseColor("#424242"),
                        Color.parseColor("#757575")
                },
                new int[]{
                        Color.parseColor("#1E88E5"), // Account storage data chart colors (blue for contrast)
                        Color.parseColor("#1565C0")
                },
                Color.parseColor("#E0E0E0"), // Menu text color (light gray)
                Color.WHITE, // Primary text color (white)
                Color.parseColor("#1E88E5"), // On sync button gradient start (blue)
                Color.parseColor("#1565C0"), // On sync button gradient end (darker blue)
                Color.parseColor("#90A4AE"), // Off sync button gradient start (gray)
                Color.parseColor("#B0BEC5"), // Off sync button gradient end (lighter gray)
                Color.parseColor("#00E5FF"), // syncProgressTextColor
                Color.parseColor("#FF5722"), // Warning text color (orange)
                Color.parseColor("#333333"), // Toolbar background (dark)
                Color.parseColor("#FFFFFF"), // Toolbar elements (white)
                MainActivity.activity.getResources().getDrawable((R.drawable.white_device)), // Device icon
                R.drawable.three_dot_white
        );
    }

    public static Theme tealTheme() {
        return new Theme(
                "teal",
                Color.parseColor("#E0F2F1"), // Primary background color (light teal)
                new int[]{
                        Color.parseColor("#80CBC4"), // Device button colors (teal)
                        Color.parseColor("#4DB6AC")
                },
                new int[]{
                        Color.parseColor("#009688"), // Account button colors (teal)
                        Color.parseColor("#00796B")
                },
                new int[]{
                        Color.parseColor("#009688"), // Add backup account button colors
                        Color.parseColor("#00796B")
                },
                new int[]{
                        Color.parseColor("#004D40"), // Device storage chart colors (dark teal)
                        Color.parseColor("#00796B"),
                        Color.parseColor("#80CBC4"),
                        Color.parseColor("#80CBC4")
                },
                new int[]{
                        Color.parseColor("#004D40"), // Device app storage chart colors
                        Color.parseColor("#00796B"),
                        Color.parseColor("#80CBC4")
                },
                new int[]{
                        Color.parseColor("#004D40"), // Device assets synced status chart colors
                        Color.parseColor("#00796B"),
                        Color.parseColor("#80CBC4")
                },
                new int[]{
                        Color.parseColor("#00897B"), // Account storage data chart colors
                        Color.parseColor("#00695C")
                },
                Color.parseColor("#004D40"), // Menu text color (dark teal)
                Color.BLACK, // Primary text color (black)
                Color.parseColor("#00796B"), // On sync button gradient start
                Color.parseColor("#004D40"), // On sync button gradient end
                Color.parseColor("#90A4AE"), // Off sync button gradient start
                Color.parseColor("#B0BEC5"), // Off sync button gradient end
                Color.parseColor("#00E5FF"), // syncProgressTextColor
                Color.parseColor("#FF5722"), // Warning text color (orange)
                Color.parseColor("#00796B"), // Toolbar background (teal)
                Color.parseColor("#FFFFFF"), // Toolbar elements (white)
                MainActivity.activity.getResources().getDrawable((R.drawable.white_device)), // Device icon
                R.drawable.three_dot_white // Three dot button
        );
    }

    public static Theme greenTealTheme() {
        return new Theme(
                "greenTeal",
                Color.parseColor("#000000"), // Light teal background
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
                        Color.parseColor("#80CBC4"),
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
                Color.parseColor("#00E5FF"), // syncProgressTextColor
                Color.parseColor("#FF5722"),  // Warning text color
                Color.parseColor("#6A5ACD"), // toolbar background
                                Color.parseColor("#FFFFFF"), // toolbar elements
                MainActivity.activity.getResources().getDrawable((R.drawable.white_device)),
                R.drawable.three_dot_black
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
                        Color.parseColor("#546E7A"),
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
                Color.parseColor("#00E5FF"), // syncProgressTextColor
                Color.parseColor("#FF5722"),  // Warning text color
                Color.parseColor("#6A5ACD"), // toolbar background
                                Color.parseColor("#FFFFFF"), // toolbar elements
                MainActivity.activity.getResources().getDrawable((R.drawable.white_device)),
                R.drawable.three_dot_black
        );
    }

    public static Theme grayTheme() {
        return new Theme(
                "gray",
                Color.parseColor("#F0F0F0"), // Primary background color (light gray)
                new int[]{
                        Color.parseColor("#D6D6D6"), // Device button colors
                        Color.parseColor("#BDBDBD")
                },
                new int[]{
                        Color.parseColor("#B0BEC5"), // Account button colors (gray)
                        Color.parseColor("#78909C")
                },
                new int[]{
                        Color.parseColor("#B0BEC5"), // Add backup account button colors
                        Color.parseColor("#78909C")
                },
                new int[]{
                        Color.parseColor("#757575"), // Device storage chart colors (gray)
                        Color.parseColor("#616161"),
                        Color.parseColor("#BDBDBD"),
                        Color.parseColor("#BDBDBD")
                },
                new int[]{
                        Color.parseColor("#757575"), // Device app storage chart colors
                        Color.parseColor("#616161"),
                        Color.parseColor("#BDBDBD")
                },
                new int[]{
                        Color.parseColor("#757575"), // Device assets synced status chart colors
                        Color.parseColor("#616161"),
                        Color.parseColor("#BDBDBD")
                },
                new int[]{
                        Color.parseColor("#546E7A"), // Account storage data chart colors
                        Color.parseColor("#455A64")
                },
                Color.parseColor("#424242"), // Menu text color (dark gray)
                Color.BLACK, // Primary text color (black)
                Color.parseColor("#616161"), // On sync button gradient start
                Color.parseColor("#757575"), // On sync button gradient end
                Color.parseColor("#90A4AE"), // Off sync button gradient start
                Color.parseColor("#B0BEC5"), // Off sync button gradient end
                Color.parseColor("#00E5FF"), // syncProgressTextColor
                Color.parseColor("#FF5722"), // Warning text color (orange)
                Color.parseColor("#E0E0E0"), // Toolbar background (light gray)
                Color.parseColor("#424242"), // Toolbar elements (dark gray)
                MainActivity.activity.getResources().getDrawable((R.drawable.black_device)), // Device icon
                R.drawable.three_dot_blue // Three dot button
        );
    }

    public static void applyTheme(Theme theme){
        SharedPreferencesHandler.setCurrentTheme(theme.name);
        MainActivity.currentTheme = theme;
        LinearLayout mainLayout = MainActivity.activity.findViewById(R.id.mainLayout);
        mainLayout.removeAllViews();
        UI.initAppUI(MainActivity.activity);
    }

    public static Theme darkBlueTheme() {
        return new Theme(
                "dark_blue",
                Color.parseColor("#0D47A1"), // Primary background color (dark blue)
                new int[]{
                        Color.parseColor("#1565C0"), // Device button colors
                        Color.parseColor("#1E88E5")
                },
                new int[]{
                        Color.parseColor("#1976D2"), // Account button colors (blue)
                        Color.parseColor("#1565C0")
                },
                new int[]{
                        Color.parseColor("#1976D2"), // Add backup account button colors
                        Color.parseColor("#1565C0")
                },
                new int[]{
                        Color.parseColor("#0D47A1"), // Device storage chart colors (dark blue)
                        Color.parseColor("#1565C0"),
                        Color.parseColor("#42A5F5"),
                        Color.parseColor("#42A5F5")
                },
                new int[]{
                        Color.parseColor("#0D47A1"), // Device app storage chart colors
                        Color.parseColor("#1565C0"),
                        Color.parseColor("#42A5F5")
                },
                new int[]{
                        Color.parseColor("#0D47A1"), // Device assets synced status chart colors
                        Color.parseColor("#1565C0"),
                        Color.parseColor("#42A5F5")
                },
                new int[]{
                        Color.parseColor("#1E88E5"), // Account storage data chart colors
                        Color.parseColor("#1976D2")
                },
                Color.parseColor("#BBDEFB"), // Menu text color (light blue)
                Color.WHITE,
                Color.parseColor("#1E88E5"), // On sync button gradient start
                Color.parseColor("#1565C0"), // On sync button gradient end
                Color.parseColor("#90A4AE"), // Off sync button gradient start
                Color.parseColor("#B0BEC5"), // Off sync button gradient end
                Color.parseColor("#00E5FF"), // syncProgressTextColor
                Color.parseColor("#FF5722"), // Warning text color (orange)
                Color.parseColor("#1565C0"), // Toolbar background (blue)
                Color.parseColor("#FFFFFF"), // Toolbar elements (white)
                MainActivity.activity.getResources().getDrawable((R.drawable.black_device)), // Device icon
                R.drawable.three_dot_white
        );
    }


    public static Theme getThemeByName(String name){
        for (Theme theme : themes){
            if (theme.name.equals(name)){
                return theme;
            }
        }
        return null;
    }
}
