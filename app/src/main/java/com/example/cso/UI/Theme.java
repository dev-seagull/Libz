package com.example.cso.UI;

import android.graphics.Color;
import android.util.Log;
import android.widget.LinearLayout;

import com.example.cso.MainActivity;
import com.example.cso.R;
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
        if (!themes.isEmpty()){
            return;
        }
        purpleTheme();
        grayTheme();
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
        if(!themes.contains(this)){
            themes.add(this);
        }
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
                    Color.parseColor("#B0BEC5"), //free
                    Color.parseColor("#858F95"), // others
                    Color.parseColor("#FFD166"), //lagging behind
                    Color.parseColor("#80CBC4") // buzzing along
                },
                new int[] {
                        Color.parseColor("#FFC107"),
                        Color.parseColor("#FFF176"),
                        Color.parseColor("#FFF9C4"),
                        Color.parseColor("#BCAD2E"),
                        Color.parseColor("#FFD54F"),
                        Color.parseColor("#FFF100"),
                        Color.parseColor("#B6A45F"),
                        Color.parseColor("#F6933C"),
                        Color.parseColor("#B1ACA8")
                },
                new int[] {
                        Color.parseColor("#64B5F6"),
                        Color.parseColor("#1565C0"),
                        Color.parseColor("#3192E8"),
                        Color.parseColor("#80D8FF"),
                        Color.parseColor("#0288D1"),
                        Color.parseColor("#0D47A1"),
                        Color.parseColor("#82B1FF"),
                        Color.parseColor("#42A5F5"),
                        Color.parseColor("#1976D2"),
                        Color.parseColor("#B3E5FC"),
                },
                new int[] {
                        Color.parseColor("#FAB34B"),
                        Color.parseColor("#80CBC4")
                },
                Color.parseColor("#202124"),
                Color.WHITE,  //primaryTextColor
                Color.parseColor("#388E3C"), // onSyncButtonGradientStart (for sync progress)
                Color.parseColor("#388E3C"), // onSyncButtonGradientEnd (for sync progress)
                Color.parseColor("#90A4AE"),
                Color.parseColor("#90A4AE"),
                Color.parseColor("#00FFB3"), // syncProgressTextColor
                Color.parseColor("#FF5722"), // warningTextColor
                Color.parseColor("#6A5ACD"), // toolbar background
                Color.parseColor("#FFFFFF"), // toolbar elements
                R.drawable.white_device,
                R.drawable.three_dot_white,
                R.drawable.yellow_loading,
                new int[] {
                        Color.parseColor("#80CBC4"),
                        Color.parseColor("#FFD166")
                }
        );
    }

    public static Theme grayTheme() {
        return new Theme(
                "gray",
                Color.parseColor("#F0F0F0"), // Primary background color (light gray)
                new int[]{
                        Color.parseColor("#B0BEC5"), // Device button colors
                        Color.parseColor("#78909C")
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
                        Color.parseColor("#607D8B"), // Device storage chart colors (gray)
                        Color.parseColor("#374850"),
                        Color.parseColor("#FFC107"),
                        Color.parseColor("#05A694")
                },
                new int[] {
                        Color.parseColor("#FFC107"),
                        Color.parseColor("#FFF176"),
                        Color.parseColor("#FFF9C4"),
                        Color.parseColor("#BCAD2E"),
                        Color.parseColor("#FFD54F"),
                        Color.parseColor("#FFF100"),
                        Color.parseColor("#B6A45F"),
                        Color.parseColor("#F6933C"),
                        Color.parseColor("#B1ACA8")
                },
                new int[] {
                        Color.parseColor("#64B5F6"),
                        Color.parseColor("#1565C0"),
                        Color.parseColor("#3192E8"),
                        Color.parseColor("#80D8FF"),
                        Color.parseColor("#0288D1"),
                        Color.parseColor("#0D47A1"),
                        Color.parseColor("#82B1FF"),
                        Color.parseColor("#42A5F5"),
                        Color.parseColor("#1976D2"),
                        Color.parseColor("#B3E5FC"),
                },
                new int[]{
                        Color.parseColor("#546E7A"), // Account storage data chart colors
                        Color.parseColor("#455A64")
                },
                Color.parseColor("#424242"), // Menu text color (dark gray)
                Color.BLACK, // Primary text color (black)
                Color.parseColor("#6A8E93"), // On sync button gradient start
                Color.parseColor("#6A8E93"), // On sync button gradient end
                Color.parseColor("#B0BEC5"), // Off sync button gradient start
                Color.parseColor("#B0BEC5"), // Off sync button gradient end
                Color.parseColor("#00E5FF"), // syncProgressTextColor
                Color.parseColor("#FF5722"), // Warning text color (orange)
                Color.parseColor("#78909C"), // Toolbar background (light gray)
                Color.WHITE, // Toolbar elements (dark gray)
                R.drawable.black_device, // Device icon
                R.drawable.three_dot_black,
                R.drawable.yellow_loading,
                new int[]{
                        Color.parseColor("#05A694"),
                        Color.parseColor("#FFC107") //unsynced color for details chart
                }
        );
    }

    public static void applyTheme(Theme theme){
        SharedPreferencesHandler.setCurrentTheme(theme.name);
        MainActivity.currentTheme = theme;
        LinearLayout mainLayout = MainActivity.activity.findViewById(R.id.mainLayout);
        if(Accounts.accountMap != null && !Accounts.accountMap.isEmpty()){
            int i = 0;
            for(String key:  Accounts.accountMap.keySet()) {
                if(i < MainActivity.currentTheme.deviceAssetsSyncedStatusChartColors.length){
                    Accounts.accountMap.put(key,MainActivity.currentTheme.deviceAssetsSyncedStatusChartColors[i]);
                }else{
                    Accounts.accountMap.put(key,Color.BLUE);
                }
            }
        }
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
