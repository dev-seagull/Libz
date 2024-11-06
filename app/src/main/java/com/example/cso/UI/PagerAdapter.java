package com.example.cso.UI;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class PagerAdapter extends RecyclerView.Adapter<PagerAdapter.PageViewHolder> {

    private Context context;
    public String buttonType;
    public String buttonId;
    public static int totalPagesForAccount = 1;
    public static int totalPagesForDevice = 3;
    public static ArrayList<PagerAdapter> pagerAdapters = new ArrayList<>();

    public PagerAdapter(Context context, String buttonType, String buttonId) {
        this.context = context;
        this.buttonType = buttonType;
        this.buttonId = buttonId;
        pagerAdapters.add(this);
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout pageLayout = new LinearLayout(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        pageLayout.setLayoutParams(params);
        pageLayout.setOrientation(LinearLayout.VERTICAL);
        pageLayout.setGravity(Gravity.CENTER);

        pageLayout.setElevation(4f);
        GradientDrawable gradientDrawable = UI.createBorderInnerLayoutDrawable(context);
        pageLayout.setBackground(gradientDrawable);
        return new PageViewHolder(pageLayout);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        Log.d("viewPager", "onBindViewHolder : " + position);
        holder.currentPage.removeAllViews();
        LinearLayout newPage = null;

        int pos = 0;
        if (buttonType.equals("account")){
            pos = position % totalPagesForAccount ;
            newPage = Accounts.createChartForStorageStatus(context, buttonId);
            Log.d("viewPager", "page number : " + pos + " type : " + buttonType + " buttonId : " + buttonId);
        }else if (buttonType.equals("device")){
            pos = position % totalPagesForDevice;
            if (pos == 0){
                newPage = Devices.createChartForStorageStatus(context, buttonId);
                Log.d("viewPager", "StorageStatus = page number : " + pos + " type : " + buttonType + " buttonId : " + buttonId);
            } else if (pos == 1) {
                newPage = Devices.createChartForSyncedAssetsLocationStatus(context, buttonId);
                Log.d("viewPager", "SyncedAssetsLocationStatus = page number : " + pos + " type : " + buttonType + " buttonId : " + buttonId);
            } else if (pos == 2) {
                newPage = Devices.createChartForSourceStatus(context, buttonId);
                Log.d("viewPager", "SourceStatus = page number : " + pos + " type : " + buttonType + " buttonId : " + buttonId);
            }
        }
        holder.currentPage.addView(newPage);
    }

    @Override
    public int getItemCount() {
        if (buttonType.equals("account")) {
            return totalPagesForAccount;
        } else if (buttonType.equals("device")) {
            return totalPagesForDevice;
        }
        return 0;
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout currentPage;
        public PageViewHolder(@NonNull LinearLayout itemView) {
            super(itemView);
            currentPage = itemView;
        }
    }

    public static PagerAdapter getPagerAdapterByButtonId(String buttonId) {
        for (PagerAdapter adapter : pagerAdapters){
            if (adapter.buttonId.equals(buttonId)){
                return adapter;
            }
        }
        return null;
    }

}