package com.nalan.swipeitem;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Author： liyi
 * Date：    2017/2/26.
 */

public class ListViewFragment extends Fragment{
    private View root;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(root==null){
            root = inflater.inflate(R.layout.fragment_listview,container,false);
            final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipe_refresh);
            swipeRefreshLayout.setColorSchemeColors(Color.RED);
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    swipeRefreshLayout.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    },2000);
                }
            });

            ListView content = (ListView) root.findViewById(R.id.listview);
            content.setAdapter(new MyAdapter());
        }
        return root;
    }

    class MyAdapter extends BaseAdapter {
        List<Integer> mResIds;

        MyAdapter(){
            int[] array = new int[]{R.drawable.bmp_qq_1,R.drawable.bmp_qq_2,R.drawable.bmp_qq_3,R.drawable.bmp_qq_4,R.drawable.bmp_qq_5};

            mResIds = new ArrayList<>();
            for(int i=0;i<25;i++)
                mResIds.add(array[i%5]);
        }

        public void delete(int position){
            mResIds.remove(position);
            notifyDataSetInvalidated();
        }

        @Override
        public int getCount() {
            return mResIds.size();
        }

        @Override
        public Object getItem(int position) {
            return mResIds.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;
            if(convertView==null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_listview, parent, false);
                holder = new Holder(convertView,position);
                convertView.setTag(holder);
            }else
                holder = (Holder) convertView.getTag();

            holder.bmp.setImageResource(mResIds.get(position));
            return convertView;
        }

        class Holder{
            private ImageView bmp;

            Holder(View main,final int position){
                bmp = (ImageView) main.findViewById(R.id.bmp);
                main.findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        delete(position);
                    }
                });
            }
        }
    }

}
