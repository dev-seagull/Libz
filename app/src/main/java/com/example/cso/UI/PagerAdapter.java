package com.example.cso.UI;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cso.MainActivity;

public class PagerAdapter extends RecyclerView.Adapter<PagerAdapter.PageViewHolder> {

    private Context context;
    public int pageNumber;
    public String buttonType;
    public String buttonId;
    public static int totalPagesForAccount = 5 ;

    public PagerAdapter(Context context, int pageNumber, String buttonType, String buttonId) {
        this.context = context;
        this.pageNumber = pageNumber;
        this.buttonType = buttonType;
        this.buttonId = buttonId;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout pageLayout = Accounts.createChartForStorage(context, buttonId);
        return new PageViewHolder(pageLayout);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        Log.d("viewPager", "onBindViewHolder");
        if (buttonType.equals("account")){
            position = position % totalPagesForAccount ;
            if (position == 0){
                holder.currentPage = Accounts.createChartForStorage(context, buttonId);
                Log.d("viewPager", "page number : " + position + " type : " + buttonType + " buttonId : " + buttonId);
            } else if (position == 1){
                holder.currentPage = Devices.createChartForStorage(context, MainActivity.androidUniqueDeviceIdentifier);
                Log.d("viewPager", "page number : " + position + " type : " + buttonType + " buttonId : " + buttonId);
            } else if (position == 2) {
                holder.currentPage = Accounts.createChartForStorage(context, buttonId);
                Log.d("viewPager", "page number : " + position + " type : " + buttonType + " buttonId : " + buttonId);
            } else if (position == 3) {
                holder.currentPage = Devices.createChartForStorage(context, MainActivity.androidUniqueDeviceIdentifier);
                Log.d("viewPager", "page number : " + position + " type : " + buttonType + " buttonId : " + buttonId);
            }else if (position == 4) {
                holder.currentPage = Accounts.createChartForStorage(context, buttonId);
                Log.d("viewPager", "page number : " + position + " type : " + buttonType + " buttonId : " + buttonId);
            }else{
                holder.currentPage = Devices.createChartForStorage(context, MainActivity.androidUniqueDeviceIdentifier);
                Log.d("viewPager", " this is else : page number : " + position + " type : " + buttonType + " buttonId : " + buttonId);
            }
        }
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