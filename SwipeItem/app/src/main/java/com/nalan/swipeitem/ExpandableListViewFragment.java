package com.nalan.swipeitem;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.nalan.swipeitem.listview.ExpandableListViewDividerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Author： liyi
 * Date：    2017/2/26.
 */

public class ExpandableListViewFragment extends Fragment{
    private View root;
    private ExpandableListView expandableListView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(root==null){
            root = inflater.inflate(R.layout.fragment_expandablelistview,container,false);
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

            expandableListView = (ExpandableListView) root.findViewById(R.id.expandable_listview);
            expandableListView.setAdapter(new ExpandableListViewDividerAdapter(new MyAdapter()));
        }
        return root;
    }

    class MyAdapter extends BaseExpandableListAdapter{
        int[] resIds = new int[]{R.drawable.bmp_qq_1,R.drawable.bmp_qq_2,R.drawable.bmp_qq_3,R.drawable.bmp_qq_4,R.drawable.bmp_qq_5};
        List<List<Integer>> data;
        Handler handler;

        MyAdapter(){
            data = new ArrayList<>();
            for(int i=0;i<10;i++){
                List<Integer> ids = new ArrayList<>();
                for(int j=0;j<resIds.length;j++)
                    ids.add(resIds[j]);
                data.add(ids);
            }

            handler = new Handler(){

                @Override
                public void handleMessage(Message msg) {
                    notifyDataSetChanged();
                    super.handleMessage(msg);
                }
            };
        }

        void delete(int groupPosition,int childPosition){
            data.get(groupPosition).remove(childPosition);
            notifyDataSetInvalidated();
//            expandableListView.collapseGroup(groupPosition);
//            expandableListView.expandGroup(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return data.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return data.get(groupPosition).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return "Group "+groupPosition;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return data.get(groupPosition).get(childPosition);
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if(convertView==null)
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_expandablelistview_group,parent,false);
            ((TextView)convertView).setText(String.format(Locale.getDefault(),"Group %d",groupPosition));
            return convertView;
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if(convertView==null)
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_expandablelistview_child,parent,false);

            ImageView bmp = (ImageView) convertView.findViewById(R.id.bmp);
            bmp.setImageResource(data.get(groupPosition).get(childPosition));
            convertView.findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delete(groupPosition,childPosition);
                }
            });

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return getCombinedGroupId(groupPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return getCombinedChildId(groupPosition,childPosition);
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }

}
