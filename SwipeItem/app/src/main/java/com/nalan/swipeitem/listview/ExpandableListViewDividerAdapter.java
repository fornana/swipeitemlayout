package com.nalan.swipeitem.listview;

import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import com.nalan.swipeitem.R;

/**
 * Author： liyi
 * Date：    2017/2/26.
 */

public class ExpandableListViewDividerAdapter extends BaseExpandableListAdapter {
    private BaseExpandableListAdapter mMainAdapter;
    private int mGroupDivider;
    private int mChildDivider;

    public ExpandableListViewDividerAdapter(BaseExpandableListAdapter mainAdapter){
        this(mainAdapter, R.layout.expandablelistview_group_divider,R.layout.expandablelistview_child_divider);
    }

    public ExpandableListViewDividerAdapter(BaseExpandableListAdapter mainAdapter, int groupDivider, int childDivider){
        mMainAdapter = mainAdapter;
        mGroupDivider = groupDivider;
        mChildDivider = childDivider;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return mMainAdapter.areAllItemsEnabled();
    }

    @Override
    public boolean isEmpty() {
        return mMainAdapter.isEmpty();
    }

    @Override
    public void notifyDataSetChanged() {
        mMainAdapter.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        mMainAdapter.notifyDataSetInvalidated();
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        mMainAdapter.onGroupCollapsed(groupPosition);
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        mMainAdapter.onGroupExpanded(groupPosition);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mMainAdapter.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mMainAdapter.unregisterDataSetObserver(observer);
    }

    @Override
    public int getGroupCount() {
        int groupCount = mMainAdapter.getGroupCount();
        if(groupCount<2)
            return groupCount;
        else
            return groupCount*2-1;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if(isEven(groupPosition)){
            int childCount = mMainAdapter.getChildrenCount(groupPosition/2);
            return childCount*2;
        }else
            return 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        if(isEven(groupPosition))
            return mMainAdapter.getGroup(groupPosition/2);
        else
            return groupPosition;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        if(isEven(groupPosition) && !isEven(childPosition))//child奇数位置才是因为它的头尾都会添加分隔线
            return mMainAdapter.getChild(groupPosition/2,childPosition/2);
        else
            return "";
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
        return mMainAdapter.hasStableIds();
    }

    @Override
    public int getChildType(int groupPosition, int childPosition) {
        return isEven(groupPosition) && !isEven(childPosition) ? mMainAdapter.getChildType(groupPosition/2,childPosition/2):getChildTypeCount()-1;
    }

    @Override
    public int getChildTypeCount() {
        return mMainAdapter.getChildTypeCount()+1;
    }

    @Override
    public int getGroupTypeCount() {
        return mMainAdapter.getGroupTypeCount()+1;
    }

    @Override
    public int getGroupType(int groupPosition) {
        return isEven(groupPosition) ? mMainAdapter.getGroupType(groupPosition/2):getGroupTypeCount()-1;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if(isEven(groupPosition))
            return mMainAdapter.getGroupView(groupPosition/2,isExpanded,convertView,parent);

        if(convertView==null && getGroupType(groupPosition)==getGroupTypeCount()-1)
            return  LayoutInflater.from(parent.getContext()).inflate(mGroupDivider,parent,false);
        else
            return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if(isEven(groupPosition) && !isEven(childPosition))
            return mMainAdapter.getChildView(groupPosition/2,childPosition/2,isLastChild,convertView,parent);

        if(convertView==null && getChildType(groupPosition,childPosition)==getChildTypeCount()-1)
            return LayoutInflater.from(parent.getContext()).inflate(mChildDivider,parent,false);
        else
            return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return mMainAdapter.isChildSelectable(groupPosition/2,childPosition/2);
    }

    private boolean isEven(int index){
        return index%2==0;
    }

}
