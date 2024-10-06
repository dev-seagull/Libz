package com.example.cso.UI;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cso.R;

public class PagerAdapter extends RecyclerView.Adapter<PagerAdapter.PageViewHolder> {

    private Context context;
    public int pageNumber;
    public String buttonType;
    public String buttonId;
    public static int totalPagesForAccount = 2 ;
    public static int totalPagesForDevice = 3 ;

    public PagerAdapter(Context context, int pageNumber, String buttonType, String buttonId) {
        this.context = context;
        this.pageNumber = pageNumber;
        this.buttonType = buttonType;
        this.buttonId = buttonId;
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
        Log.d("viewPager", "onBindViewHolder");
        holder.currentPage.removeAllViews();
        LinearLayout newPage = null;
        if (buttonType.equals("account")){
            position = position % totalPagesForAccount ;
            if (position == 0){
                newPage = Accounts.createChartForStorageStatus(context, buttonId);
                Log.d("viewPager", "page number : " + position + " type : " + buttonType + " buttonId : " + buttonId);
            } else if (position == 1){
                newPage = Accounts.createChartForSyncAndSourceStatus(context, buttonId);
                Log.d("viewPager", "page number : " + position + " type : " + buttonType + " buttonId : " + buttonId);
            }
        }else if (buttonType.equals("device")){
            position = position % totalPagesForDevice ;
            if (position == 0){
                newPage = Devices.createChartForStorageStatus(context, buttonId);
                Log.d("viewPager", "StorageStatus = page number : " + position + " type : " + buttonType + " buttonId : " + buttonId);
            } else if (position == 1) {
                newPage = Devices.createChartForSyncedAssetsLocationStatus(context, buttonId);
                Log.d("viewPager", "SyncedAssetsLocationStatus = page number : " + position + " type : " + buttonType + " buttonId : " + buttonId);
            } else if (position == 2) {
                newPage = Devices.createChartForSourceStatus(context, buttonId);
                Log.d("viewPager", "SourceStatus = page number : " + position + " type : " + buttonType + " buttonId : " + buttonId);
            }
        }
        holder.currentPage.addView(newPage);
    }

    @Override
    public int getItemCount() {
        return 10000;
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout currentPage;
        public PageViewHolder(@NonNull LinearLayout itemView) {
            super(itemView);
            currentPage = itemView;
        }
    }

}