package com.example.cso.UI;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.gson.JsonObject;

public class CustomTreeMapChart {
    public static LinearLayout createChart(Context context, JsonObject data){
        int width =(int) (UI.getDeviceWidth(context) * 0.65);
        int height = (int) (UI.getDeviceWidth(context) * 0.35);
        double unsynced = 200;
        double acc1 = 4;
        double acc2 = 0;
        double acc3 = 2;
        double total = unsynced + acc1 + acc2 + acc3;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
        layoutParams.setMargins(0, 10, 0, 10);
        layout.setLayoutParams(layoutParams);


        LinearLayout temp = new LinearLayout(context);
        temp.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tempParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        tempParams.setMargins(0, 10, 0, 10);
        temp.setLayoutParams(tempParams);


        LinearLayout unsyncedView = createUnSyncedView(context,width,height,total,unsynced);

        LinearLayout accountsView = createAccountsView(context,new double[]{acc1,acc2,acc3},new String[]{},height,width - unsyncedView.getWidth(),total-unsynced);
        temp.addView(unsyncedView);
        temp.addView(accountsView);
        layout.addView(temp);
        return layout;
    }

    private static LinearLayout createUnSyncedView(Context context, int width,int height, double total, double unsynced) {
        double relation = Math.min(0.4,(unsynced / total));
        width = (int) (relation * width);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.GRAY);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                width, height
        ));
        return layout;
    }

    private static LinearLayout createAccountsView(Context context, double[] accountsSize, String[] accountsLabel, int height, int remainingWidth, double totalAccountSize) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                remainingWidth, height
        ));
        int[] colors = new int[]{Color.GREEN, Color.RED, Color.BLUE, Color.YELLOW, Color.BLACK};
        for(int i = 0; i < accountsSize.length; i++) {
            View view = new View(context);
            int viewHeight = (int) (accountsSize[i] * height/ totalAccountSize);
            view.setBackgroundColor(colors[i]);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    remainingWidth, viewHeight
            );
            view.setLayoutParams(layoutParams);
            layout.addView(view);
        }
        return layout;
    }
}
