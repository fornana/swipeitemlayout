package com.nalan.swipeitem.recyclerview;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * Author： liyi
 * Date：    2017/2/7.
 */

/*
    SwipeItemLayout处理了滑动，然后内置RecyclerView.OnItemTouchListener，非常完美的解决方案，
    不需要对RecyclerView做任何额外的处理。仅有一个问题，多点触控，如果不仅有RecyclerView的多点触控
    用多个手指滑动时，可能有多个item处于expand状态。这是无法避免的，因为RecyclerView无法处理event，
    event被SwipeItemLayout处理了，此时，所有的RecyclerView的ACTION_POINTER_DOWN都直接作为
    ACTION_DOWN给了item，但是，不会经过RecyclerView。所以，只有禁用RecyclerView的多点触控才行，
    应该不存在其它方法。

    另外一种思路是SwipeItemLayout不处理滑动，在OnItemTouchListener中处理滑动。这样多点触控就不会因为
    item的拦截而分为多个ACTION_DOWN。可以在OnItemTouchListener中根据情况去忽略多点触控
    详细见SlidingItemLayout
 */
//左滑，分为main与side
public class SwipeItemLayout1 extends ViewGroup{
    private static final int MIN_FLING_VELOCITY = 400; // dips per second
    private static final float TOUCH_SLOP_SENSITIVITY = 1.f;

    private ViewGroup mMainView;
    private ViewGroup mSideView;

    private ViewDragHelper mDragHelper;
    private float mSlideOffset;
    private float mInitialMotionX,mInitialMotionY;

    private int mMainLeftMin,mMainLeftMax;
    private int mMainWidth,mSideWidth;//含有margin


    private ScrollRunnable mScrollRunnable;//在没有drag时，用来open、close pane

    private boolean mInLayout;

    public SwipeItemLayout1(Context context) {
        this(context,null);
    }

    public SwipeItemLayout1(Context context, AttributeSet attrs) {
        super(context, attrs);

//        mState = State.RESET;

        final float density = getResources().getDisplayMetrics().density;
        final float minVel = MIN_FLING_VELOCITY * density;

        mDragHelper = ViewDragHelper.create(this, TOUCH_SLOP_SENSITIVITY, new ViewDragCallback());
        mDragHelper.setMinVelocity(minVel);

        mScrollRunnable = new ScrollRunnable(context);
    }

    public boolean isExpand(){
        return mMainView.getLeft()!=mMainLeftMax;
    }

    private boolean ensureChildren(){
        int childCount = getChildCount();

        if(childCount!=2)
            return false;

        View childView = getChildAt(0);
        if(!(childView instanceof ViewGroup))
            return false;
        mMainView = (ViewGroup) childView;

        childView = getChildAt(1);
        if(!(childView instanceof ViewGroup))
            return false;
        mSideView = (ViewGroup) childView;
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(!ensureChildren())
            throw new RuntimeException("SwipeItemLayout的子视图不符合规定");

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if(widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
            /*
                一般确实就是match_parent，但是，抛异常是否太粗暴，此处直接调用super方法，当然没任何意义
                throw new IllegalArgumentException("SwipeItemLayout must be measured with MeasureSpec.EXACTLY.");
             */
            super.onMeasure(widthMeasureSpec,heightMeasureSpec);
        }

        setMeasuredDimension(widthSize,heightSize);

        int childWidthSpec;
        int childHeightSpec;
        MarginLayoutParams lp;
        int childWidth = widthSize-getPaddingLeft()-getPaddingRight();
        int childHeight = heightSize-getPaddingTop()-getPaddingBottom();

        //main layout占据真个layout frame
        lp = (MarginLayoutParams) mMainView.getLayoutParams();
        childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth-lp.leftMargin-lp.rightMargin,MeasureSpec.EXACTLY);
        childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight-lp.topMargin-lp.bottomMargin,MeasureSpec.EXACTLY);
        mMainView.measure(childWidthSpec,childHeightSpec);

        //side layout大小为自身实际大小
        lp = (MarginLayoutParams) mSideView.getLayoutParams();
        childWidthSpec = MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED);
        childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight-lp.topMargin-lp.bottomMargin,MeasureSpec.EXACTLY);
        mSideView.measure(childWidthSpec,childHeightSpec);
    }

    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
        if(!ensureChildren())
            throw new RuntimeException("SwipeItemLayout的子视图不符合规定");

        mInLayout = true;

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int layoutLeft;
        int layoutRight;
        int visibleWidth;
        int totalWidth;

        MarginLayoutParams mainParams = (MarginLayoutParams) mMainView.getLayoutParams();
        MarginLayoutParams sideParams = (MarginLayoutParams) mSideView.getLayoutParams();

        mMainWidth = mainParams.leftMargin+mainParams.rightMargin+mMainView.getMeasuredWidth();
        mSideWidth = sideParams.leftMargin+sideParams.rightMargin+mSideView.getMeasuredWidth();
        totalWidth = mMainWidth+mSideWidth;
        visibleWidth = getWidth()-paddingLeft-paddingRight;

        layoutLeft = paddingLeft+mainParams.leftMargin;
        layoutRight = layoutLeft+mMainView.getMeasuredWidth();
        mMainView.layout(layoutLeft,paddingTop+mainParams.topMargin,layoutRight,getHeight()-paddingBottom-mainParams.bottomMargin);

        layoutLeft = layoutRight+mainParams.rightMargin+mainParams.leftMargin;
        layoutRight = layoutLeft+mSideView.getMeasuredWidth();
        mSideView.layout(layoutLeft,paddingTop+mainParams.topMargin,layoutRight,getHeight()-paddingBottom-mainParams.bottomMargin);

        mSlideOffset = mSlideOffset>0.5 ? 1:0;
        int dx = (int) ((visibleWidth-totalWidth)*mSlideOffset);
        ViewCompat.offsetLeftAndRight(mMainView,dx);
        ViewCompat.offsetLeftAndRight(mSideView,dx);

        mMainLeftMin = paddingLeft+visibleWidth-totalWidth;
        mMainLeftMax = paddingLeft;

        mInLayout = false;
    }

    @Override
    public void requestLayout() {
        if (!mInLayout) {
            super.requestLayout();
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return p instanceof MarginLayoutParams ? p : new MarginLayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof MarginLayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    public void openPane(){
        int currentLeft = mMainView.getLeft();
        mScrollRunnable.start(currentLeft,mMainLeftMin-currentLeft);
    }

    public void closePane(){
        int currentLeft = mMainView.getLeft();
        mScrollRunnable.start(currentLeft, mMainLeftMax-currentLeft);
        Log.e("移动了",""+currentLeft);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mSlideOffset = 0;//detach以后就还原
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mDragHelper.cancel();
            return false;
        }

        //拦截，但是不做任何处理，因为此时在动画，不允许children有机会处理click
        if((action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_MOVE) && mScrollRunnable.isRunning())
            return true;

        boolean interceptTap = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mInitialMotionX = x;
                mInitialMotionY = y;

                if (/*mDragHelper.isViewUnder(mMainView, (int) x, (int) y) && */(mMainView.getLeft()==mMainLeftMin)) {
                    interceptTap = true;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final float x = ev.getX();
                final float y = ev.getY();
                final float adx = Math.abs(x - mInitialMotionX);
                final float ady = Math.abs(y - mInitialMotionY);
                final int slop = mDragHelper.getTouchSlop();
                if (adx > slop && ady > adx) {
                    mDragHelper.cancel();
                    return false;
                }
            }
        }

        final boolean interceptForDrag = mDragHelper.shouldInterceptTouchEvent(ev);
        if(interceptForDrag|| interceptTap && getParent()!=null){
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return interceptForDrag || interceptTap;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();

        if((action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_MOVE) && mScrollRunnable.isRunning())
            return true;

        mDragHelper.processTouchEvent(ev);

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mInitialMotionX = x;
                mInitialMotionY = y;
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (mMainView.getLeft()==mMainLeftMin) {
                    final float x = ev.getX();
                    final float y = ev.getY();
                    final float dx = x - mInitialMotionX;
                    final float dy = y - mInitialMotionY;
                    final int slop = mDragHelper.getTouchSlop();
                    if (dx * dx + dy * dy < slop * slop && mDragHelper.isViewUnder(mMainView, (int) x, (int) y)) {
                        closePane();
                        break;
                    }
                }
                break;
            }
        }

        return true;
    }

    //ViewDragHelper的continueSettling()进行了计算，并且执行了相应的callback
    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true))
            ViewCompat.postInvalidateOnAnimation(this);
    }

    private class ScrollRunnable implements Runnable {
        private boolean mRunning;
        private Scroller mScroller;

        ScrollRunnable(Context context){
            mScroller = new Scroller(context);
            mRunning = false;
        }

        void start(int startX,int dx){
            if(!mScroller.isFinished()){
                removeCallbacks(this);
                mScroller.abortAnimation();
            }

            mScroller.startScroll(startX,0,dx,0);
            mRunning = true;
            ViewCompat.postOnAnimation(SwipeItemLayout1.this,this);
        }

        boolean isRunning(){
            return mRunning;
        }

        @Override
        public void run() {
            boolean more = mScroller.computeScrollOffset();
            final int x = mScroller.getCurrX();
            if(more){
                int dx = x-mMainView.getLeft();
                ViewCompat.offsetLeftAndRight(mMainView,dx);
                ViewCompat.offsetLeftAndRight(mSideView,dx);
                ViewCompat.postOnAnimation(SwipeItemLayout1.this, this);
            }else{
                mRunning = false;
                removeCallbacks(this);
            }
        }
    }

    private class ViewDragCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child==mMainView || child==mSideView;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return child.getTop();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int startBound;
            int endBound;
            int newLeft = 0;

            if(child==mMainView){
                startBound = mMainLeftMin;
                endBound = mMainLeftMax;
                newLeft = Math.min(Math.max(left,startBound),endBound);
            }

            if(child==mSideView){
                startBound = mMainLeftMin+mMainWidth;
                endBound = mMainLeftMax+mMainWidth;
                newLeft = Math.min(Math.max(left,startBound),endBound);
            }

            return newLeft;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mMainLeftMax-mMainLeftMin;
        }

        @Override
        public void onViewDragStateChanged(int state) {}

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {}

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if(changedView==mMainView){
                ViewCompat.offsetLeftAndRight(mSideView,dx);
            }else if(changedView==mSideView){
                ViewCompat.offsetLeftAndRight(mMainView,dx);
            }
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xVel, float yVel) {
            int childLeft = releasedChild.getLeft();
            int newLeft;

            if(releasedChild==mMainView){
                newLeft = mMainLeftMax;
                if(xVel<0 || (xVel==0 && childLeft<mMainLeftMax-mSideWidth/2))
                    newLeft = mMainLeftMin;
                mDragHelper.settleCapturedViewAt(newLeft, releasedChild.getTop());
            }else if(releasedChild==mSideView){
                newLeft = mMainLeftMax+mMainWidth;
                if(xVel<0 || (xVel==0 && childLeft<mMainLeftMax+mMainWidth-mSideWidth/2))
                    newLeft = mMainLeftMin+mMainWidth;
                mDragHelper.settleCapturedViewAt(newLeft, releasedChild.getTop());
            }

            invalidate();
        }

    }


    public static class OnSwipeItemTouchListener implements RecyclerView.OnItemTouchListener{
        private RecyclerView mRecyclerView;
        private float mInitialMotionX,mInitialMotionY;
        private int mTouchSlop;
        private boolean mIsMove;

        public OnSwipeItemTouchListener(RecyclerView recyclerView){
            mRecyclerView = recyclerView;
            mTouchSlop = ViewConfiguration.get(recyclerView.getContext()).getScaledTouchSlop();
            mRecyclerView.setMotionEventSplittingEnabled(false);
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            final int action = e.getAction();
            final float x = e.getX();
            final float y = e.getY();
            boolean intercept = false;

            if(action==MotionEvent.ACTION_DOWN){
                mIsMove = false;
                mInitialMotionX = x;
                mInitialMotionY = y;

                //当前是否有expand的
                boolean hasExpand = false;
                SwipeItemLayout1 expandChild = null;
                SwipeItemLayout1 captureChild = null;

                for(int i=0;i<mRecyclerView.getChildCount();i++){
                    SwipeItemLayout1 child = (SwipeItemLayout1) mRecyclerView.getChildAt(i);
                    if(child.isExpand()) {
                        hasExpand = true;
                        expandChild = child;
                    }

                    if (x >= child.getLeft() && x < child.getRight() && y >= child.getTop() && y < child.getBottom())
                        captureChild = child;
                }

                if(hasExpand && captureChild!=expandChild) {
                    intercept = true;
                    expandChild.closePane();//关闭
                }
            }

            //能够执行到此处说明item没有滑动，如果RecyclerView可以滑动了，是偏上下移动,查找是否有展开的item，一律closePane
            if(action==MotionEvent.ACTION_MOVE){
                final int adx = (int) Math.abs(x - mInitialMotionX);
                final int ady = (int) Math.abs(y - mInitialMotionY);
                if (ady > mTouchSlop && ady>adx && !mIsMove) {
                    mIsMove = true;
                    for(int i=0;i<mRecyclerView.getChildCount();i++){
                        SwipeItemLayout1 child = (SwipeItemLayout1) mRecyclerView.getChildAt(i);
                        if(child.isExpand())
                            child.closePane();
                    }
                }
            }

            return intercept;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}

    }

}
