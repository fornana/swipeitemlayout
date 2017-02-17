package com.nalan.swipeitemlayout;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Author： liyi
 * Date：    2017/2/17.
 */

public class DemoAdapter extends RecyclerView.Adapter<DemoAdapter.Holder>{
    private Context mContext;
    private List<Integer> mResIds;

    public DemoAdapter(Context context){
        mContext = context;
        int[] array = new int[]{R.drawable.bmp_qq_1,R.drawable.bmp_qq_2,R.drawable.bmp_qq_3,R.drawable.bmp_qq_4,R.drawable.bmp_qq_5};

        mResIds = new ArrayList<>();
        for(int i=0;i<25;i++)
            mResIds.add(array[i%5]);
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(mContext).inflate(R.layout.item_recycler_demo,parent,false);
        return new Holder(root);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        holder.mBmp.setImageResource(mResIds.get(position));
    }

    @Override
    public int getItemCount() {
        return mResIds.size();
    }

    class Holder extends RecyclerView.ViewHolder implements View.OnClickListener,View.OnLongClickListener{
        private ImageView mBmp;

        Holder(View itemView) {
            super(itemView);

            mBmp = (ImageView) itemView.findViewById(R.id.bmp);
            View main = itemView.findViewById(R.id.main);
            main.setOnClickListener(this);
            main.setOnLongClickListener(this);

            View delete = itemView.findViewById(R.id.delete);
            delete.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.main:
                    Toast.makeText(v.getContext(),"点击了main，位置为："+getAdapterPosition(),Toast.LENGTH_SHORT).show();
                    break;

                case R.id.delete:
                    int pos = getAdapterPosition();
                    mResIds.remove(pos);
                    notifyItemRemoved(pos);
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            switch (v.getId()){
                case R.id.main:
                    Toast.makeText(v.getContext(),"长按了main，位置为："+getAdapterPosition(),Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    }

}
