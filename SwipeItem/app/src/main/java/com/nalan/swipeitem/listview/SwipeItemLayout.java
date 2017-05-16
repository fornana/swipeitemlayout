package com.nalan.swipeitem.listview;

import android.content.Context;
import android.content.res.TypedArray;
import android.icu.util.Measure;
import android.support.annotation.Px;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Scroller;

/**
 * Author： liyi
 * Date：    2017/2/24.
 */

public class SwipeItemLayout extends ViewGroup {
    enum Mode{
        RESET,DRAG,FLING,TAP
    }

    private Mode mTouchMode;

    private ViewGroup mMainView;
    private ViewGroup mSideView;

    private ScrollRunnable mScrollRunnable;
    private int mScrollOffset;
    private int mMaxScrollOffset;

    private boolean mInLayout;
    private boolean mIsLaidOut;

    public SwipeItemLayout(Context context) {
        this(context,null);
    }

    public SwipeItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchMode = Mode.RESET;
        mScrollOffset = 0;

        mScrollRunnable = new ScrollRunnable(context);
    }

    public boolean isOpen(){
        return mScrollOffset !=0;
    }

    Mode getTouchMode(){
        return mTouchMode;
    }

    void setTouchMode(Mode mode){
        switch (mTouchMode){
            case FLING:
                mScrollRunnable.abort();
                break;
            case RESET:
                break;
        }

        mTouchMode = mode;
    }

    public void open(){
        if(mScrollOffset!=-mMaxScrollOffset){
            //正在open，不需要处理
            if(mTouchMode== Mode.FLING && mScrollRunnable.isScrollToLeft())
                return;

            //当前正在向右滑，abort
            if(mTouchMode== Mode.FLING /*&& !mScrollRunnable.mScrollToLeft*/)
                mScrollRunnable.abort();

            mScrollRunnable.startScroll(mScrollOffset,-mMaxScrollOffset);
        }
    }

    public void close(){
        if(mScrollOffset!=0){
            //正在close，不需要处理
            if(mTouchMode== Mode.FLING && !mScrollRunnable.isScrollToLeft())
                return;

            //当前正向左滑，abort
            if(mTouchMode== Mode.FLING /*&& mScrollRunnable.mScrollToLeft*/)
                mScrollRunnable.abort();

            mScrollRunnable.startScroll(mScrollOffset,0);
        }
    }

    void fling(int xVel){
        mScrollRunnable.startFling(mScrollOffset,xVel);
    }

    void revise(){
        if(mScrollOffset<-mMaxScrollOffset/2)
            open();
        else
            close();
    }

    boolean trackMotionScroll(int deltaX){
        if(deltaX==0)
            return false;

        boolean over = false;
        int newLeft = mScrollOffset+deltaX;
        if((deltaX>0 && newLeft>0) || (deltaX<0 && newLeft<-mMaxScrollOffset)){
            over = true;
            newLeft = Math.min(newLeft,0);
            newLeft = Math.max(newLeft,-mMaxScrollOffset);
        }

        offsetChildrenLeftAndRight(newLeft-mScrollOffset);
        mScrollOffset = newLeft;
        return over;
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

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        MarginLayoutParams lp = null;
        int horizontalMargin,verticalMargin;
        int horizontalPadding = getPaddingLeft()+getPaddingRight();
        int verticalPadding = getPaddingTop()+getPaddingBottom();

        lp = (MarginLayoutParams) mMainView.getLayoutParams();
        horizontalMargin = lp.leftMargin+lp.rightMargin;
        verticalMargin = lp.topMargin+lp.bottomMargin;
        measureChildWithMargins(mMainView,
                widthMeasureSpec,horizontalMargin+horizontalPadding,
                heightMeasureSpec,verticalMargin+verticalPadding);

        if(widthMode==MeasureSpec.AT_MOST)
            widthSize = Math.min(widthSize,mMainView.getMeasuredWidth()+horizontalMargin+horizontalPadding);
        else if(widthMode==MeasureSpec.UNSPECIFIED)
            widthSize = mMainView.getMeasuredWidth()+horizontalMargin+horizontalPadding;

        if(heightMode==MeasureSpec.AT_MOST)
            heightSize = Math.min(heightSize,mMainView.getMeasuredHeight()+verticalMargin+verticalPadding);
        else if(heightMode==MeasureSpec.UNSPECIFIED)
            heightSize = mMainView.getMeasuredHeight()+verticalMargin+verticalPadding;

        setMeasuredDimension(widthSize,heightSize);

        //side layout大小为自身实际大小
        lp = (MarginLayoutParams) mSideView.getLayoutParams();
        verticalMargin = lp.topMargin+lp.bottomMargin;
        mSideView.measure(MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight()-verticalMargin-verticalPadding,MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(!ensureChildren())
            throw new RuntimeException("SwipeItemLayout的子视图不符合规定");

        mInLayout = true;

        int pl = getPaddingLeft();
        int pt = getPaddingTop();
        int pr = getPaddingRight();
        int pb = getPaddingBottom();

        MarginLayoutParams mainLp = (MarginLayoutParams) mMainView.getLayoutParams();
        MarginLayoutParams sideParams = (MarginLayoutParams) mSideView.getLayoutParams();

        int childLeft = pl+mainLp.leftMargin;
        int childTop = pt+mainLp.topMargin;
        int childRight = getWidth()-(pr+mainLp.rightMargin);
        int childBottom = getHeight()-(mainLp.bottomMargin+pb);
        mMainView.layout(childLeft,childTop,childRight,childBottom);

        childLeft = childRight+sideParams.leftMargin;
        childTop = pt+sideParams.topMargin;
        childRight = childLeft+sideParams.leftMargin+sideParams.rightMargin+mSideView.getMeasuredWidth();
        childBottom = getHeight()-(sideParams.bottomMargin+pb);
        mSideView.layout(childLeft,childTop,childRight,childBottom);

        mMaxScrollOffset = mSideView.getWidth()+sideParams.leftMargin+sideParams.rightMargin;
        mScrollOffset = 0;//mScrollOffset<-mMaxScrollOffset/2 ? -mMaxScrollOffset:0;

        //offsetChildrenLeftAndRight(mScrollOffset);
        mInLayout = false;
        mIsLaidOut = true;
    }

    void offsetChildrenLeftAndRight(int delta){
        ViewCompat.offsetLeftAndRight(mMainView,delta);
        ViewCompat.offsetLeftAndRight(mSideView,delta);
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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if(mScrollOffset!=0 && mIsLaidOut){
            offsetChildrenLeftAndRight(-mScrollOffset);
            mScrollOffset = 0;
        }else
            mScrollOffset = 0;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if(mScrollOffset!=0 && mIsLaidOut){
            offsetChildrenLeftAndRight(-mScrollOffset);
            mScrollOffset = 0;
        }else
            mScrollOffset = 0;
        removeCallbacks(mScrollRunnable);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        //click main view，但是它处于open状态，所以，不需要点击效果，直接拦截不调用click listener
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                View pointView = findTopChildUnder(this,x,y);
                if(pointView!=null && pointView==mMainView && mScrollOffset !=0)
                    return true;
                break;
            }

            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_CANCEL:
                break;

            case MotionEvent.ACTION_UP:{
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                View pointView = findTopChildUnder(this,x,y);
                if(pointView!=null && pointView==mMainView && mTouchMode== Mode.TAP && mScrollOffset !=0)
                    return true;
                break;
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        //click main view，但是它处于open状态，所以，不需要点击效果，直接拦截不调用click listener
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                View pointView = findTopChildUnder(this,x,y);
                if(pointView!=null && pointView==mMainView && mScrollOffset !=0)
                    return true;
                break;
            }

            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_CANCEL:
                break;

            case MotionEvent.ACTION_UP:{
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                View pointView = findTopChildUnder(this,x,y);
                if(pointView!=null && pointView==mMainView && mTouchMode== Mode.TAP && mScrollOffset !=0) {
                    close();
                    return true;
                }
                break;
            }
        }

        return false;
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if(getVisibility()!=View.VISIBLE){
            mScrollOffset = 0;
            invalidate();
        }
    }

    private static final Interpolator sInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    class ScrollRunnable implements Runnable{
        private static final int FLING_DURATION = 200;
        private Scroller mScroller;
        private boolean mAbort;
        private int mMinVelocity;
        private boolean mScrollToLeft;

        ScrollRunnable(Context context){
            mScroller = new Scroller(context,sInterpolator);
            mAbort = false;
            mScrollToLeft = false;

            ViewConfiguration configuration = ViewConfiguration.get(context);
            mMinVelocity = configuration.getScaledMinimumFlingVelocity();
        }

        void startScroll(int startX,int endX){
            if(startX!=endX){
                Log.e("scroll - startX - endX",""+startX+" "+endX);
                setTouchMode(Mode.FLING);
                mAbort = false;
                mScrollToLeft = endX<startX;
                mScroller.startScroll(startX,0,endX-startX,0, 400);
                ViewCompat.postOnAnimation(SwipeItemLayout.this,this);
            }
        }

        void startFling(int startX,int xVel){
            Log.e("fling - startX",""+startX);

            if(xVel>mMinVelocity && startX!=0) {
                startScroll(startX, 0);
                return;
            }

            if(xVel<-mMinVelocity && startX!=-mMaxScrollOffset) {
                startScroll(startX, -mMaxScrollOffset);
                return;
            }

            startScroll(startX,startX>-mMaxScrollOffset/2 ? 0:-mMaxScrollOffset);
        }

        void abort(){
            if(!mAbort){
                mAbort = true;
                if(!mScroller.isFinished()){
                    mScroller.abortAnimation();
                    removeCallbacks(this);
                }
            }
        }

        //是否正在滑动需要另外判断
        boolean isScrollToLeft(){
            return mScrollToLeft;
        }

        @Override
        public void run() {
            Log.e("abort",Boolean.toString(mAbort));
            if(!mAbort){
                boolean more = mScroller.computeScrollOffset();
                int curX = mScroller.getCurrX();
                Log.e("curX",""+curX);

                boolean atEdge = trackMotionScroll(curX-mScrollOffset);
                if(more && !atEdge) {
                    ViewCompat.postOnAnimation(SwipeItemLayout.this, this);
                    return;
                }

                if(atEdge){
                    removeCallbacks(this);
                    if(!mScroller.isFinished())
                        mScroller.abortAnimation();
                    setTouchMode(Mode.RESET);
                }

                if(!more){
                    setTouchMode(Mode.RESET);
                    //绝对不会出现这种意外的！！！可以注释掉
                    if(mScrollOffset!=0){
                        if(Math.abs(mScrollOffset)>mMaxScrollOffset/2)
                            mScrollOffset = -mMaxScrollOffset;
                        else
                            mScrollOffset = 0;
                        ViewCompat.postOnAnimation(SwipeItemLayout.this,this);
                    }
                }
            }
        }
    }

    static View findTopChildUnder(ViewGroup parent,int x, int y) {
        final int childCount = parent.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            final View child = parent.getChildAt(i);
            if (x >= child.getLeft() && x < child.getRight()
                    && y >= child.getTop() && y < child.getBottom()) {
                return child;
            }
        }
        return null;
    }

}
