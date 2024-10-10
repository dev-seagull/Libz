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
    public int deviceIconId;
    public int threeDotButtonId;
    public int syncProgressTextColor;
    public int loadingImageId;
    public String name;
    public static ArrayList<Theme> themes = new ArrayList<>();
    public int[] syncDetailsPieChartColors;

    public static void initializeThemes(){
        purpleTheme();
        grayTheme();
        darkTheme();
        tealTheme();
        MainActivity.currentTheme = getThemeByName(SharedPreferencesHandler.getCurrentTheme());
        Log.d("ui","theme size : "+ themes.size());
    }

    public Theme(String name, int primaryBackgroundColor, int[] deviceButtonColors, int[] accountButtonColors,
                 int[] addBackupAccountButtonColors, int[] deviceStorageChartColors,
                 int[] deviceAppStorageChartColors, int[] deviceAssetsSyncedStatusChartColors,
                 int[] accountStorageDataChartColors, int menuTextColor, int primaryTextColor,
                 int onSyncButtonGradientStart, int onSyncButtonGradientEnd, int offSyncButtonGradientStart,
                 int offSyncButtonGradientEnd,int syncProgressTextColor, int warningTextColor,
                 int toolbarBackgroundColor, int toolbarElementsColor
                , int deviceIconId, int threeDotButtonId,
                int loadingImageId, int[] syncDetailsPieChartColors) {
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
        this.deviceIconId = deviceIconId;
        this.threeDotButtonId = threeDotButtonId;
        this.loadingImageId = loadingImageId;
        this.syncDetailsPieChartColors = syncDetailsPieChartColors;
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
                        //                        Color.parseColor("#00796B"),
                        Color.GRAY,
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
                R.drawable.white_device,
                R.drawable.three_dot_white,
                R.drawable.yellow_loading,
                new int[]{
                Color.parseColor("#80CBC4"), //synced color for details chart
                Color.parseColor("#B0BEC5"), //unsynced color for details chart
                }
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
                R.drawable.white_device, // Device icon
                R.drawable.three_dot_white,
                R.drawable.yellow_loading,
                new int[]{
                        Color.parseColor("#388E3C"), //synced color for details chart
                        Color.parseColor("#80CBC4"), //unsynced color for details chart
                }
        );
    }

    public static Theme tealTheme() {
        return new Theme(
                "teal",
                Color.parseColor("#E0F7FA"), // Primary background color (light cyan)
                new int[]{
                        Color.parseColor("#26A69A"), // Device button colors (vibrant teal)
                        Color.parseColor("#00897B")  // Slightly darker teal for contrast
                },
                new int[]{
                        Color.parseColor("#00897B"), // Account button colors (dark teal)
                        Color.parseColor("#00695C")  // Darker teal for stronger contrast
                },
                new int[]{
                        Color.parseColor("#00897B"), // Add backup account button colors
                        Color.parseColor("#00695C")
                },
                new int[]{
                        Color.parseColor("#B0BEC5"), // Device storage chart colors (total storage, light gray for balance)
                        Color.parseColor("#BBDEFB"), // Used storage (light blue for clarity)
                        Color.parseColor("#00796B"), // Media (teal, consistent with the theme)
                        Color.parseColor("#FAB34B")  // Synced (bright yellow for strong contrast)
                },
                new int[]{
                        Color.parseColor("#1E88E5"), // Device app storage chart colors (blue for distinct contrast)
                        Color.parseColor("#304194"), // Dark blue for consistency
                        Color.parseColor("#BBDEFB")  // Light blue for clarity
                },
                new int[]{
                        Color.parseColor("#1E88E5"), // Device assets synced status chart colors (blue for contrast)
                        Color.parseColor("#304194"), // Dark blue
                        Color.parseColor("#FAB34B")  // Bright yellow for synced status
                },
                new int[]{
                        Color.parseColor("#1E88E5"), // Account storage data chart colors (strong blue)
                        Color.parseColor("#304194")  // Darker blue for contrast
                },
                Color.parseColor("#004D40"), // Menu text color (dark teal)
                Color.BLACK,  // Primary text color (black for readability on lighter backgrounds)
                Color.parseColor("#00796B"), // On sync button gradient start (teal)
                Color.parseColor("#004D40"), // On sync button gradient end (dark teal)
                Color.parseColor("#90A4AE"), // Off sync button gradient start (grayish tone)
                Color.parseColor("#B0BEC5"), // Off sync button gradient end (light gray)
                Color.parseColor("#00B8D4"), // Sync progress text color (bright cyan for visibility)
                Color.parseColor("#FF5722"), // Warning text color (orange remains unchanged)
                Color.parseColor("#00796B"), // Toolbar background (deep teal)
                Color.parseColor("#FFFFFF"), // Toolbar elements (white for clarity)
                R.drawable.black_device, // Device icon
                R.drawable.three_dot_black, // Three dot button
                R.drawable.yellow_loading,
                new int[]{
                        Color.parseColor("#388E3C"), //synced color for details chart
                        Color.parseColor("#80CBC4"), //unsynced color for details chart
                }
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
                R.drawable.black_device, // Device icon
                R.drawable.three_dot_blue,
                R.drawable.yellow_loading,
                new int[]{
                        Color.parseColor("#388E3C"), //synced color for details chart
                        Color.parseColor("#80CBC4"), //unsynced color for details chart
                }
        );
    }

    public static void applyTheme(Theme theme){
        SharedPreferencesHandler.setCurrentTheme(theme.name);
        MainActivity.currentTheme = theme;
        LinearLayout mainLayout = MainActivity.activity.findViewById(R.id.mainLayout);
        mainLayout.removeAllViews();
        UI.initAppUI(MainActivity.activity);
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
