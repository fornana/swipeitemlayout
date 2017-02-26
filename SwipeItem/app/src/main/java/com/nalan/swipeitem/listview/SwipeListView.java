package com.nalan.swipeitem.listview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ListView;

import com.nalan.swipeitem.listview.SwipeItemLayout.Mode;

/**
 * Author： liyi
 * Date：    2017/2/24.
 */

public class SwipeListView extends ListView{
    private SwipeItemLayout mCaptureItem;
    private float mLastMotionX;
    private float mLastMotionY;
    private VelocityTracker mVelocityTracker;

    private int mActivePointerId;

    private int mTouchSlop;
    private int mMaximumVelocity;

    private boolean mDragHandleBySuper;
    private boolean mDragHandleByThis;
    //如果item open，此时点击其它地方就close item，并且完全不处理后续的所以消息（即此时滑动不起作用了）。
    private boolean mIsCancelEvent;

    public SwipeListView(Context context) {
        this(context,null);
    }

    public SwipeListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mActivePointerId = -1;
        mDragHandleBySuper = false;
        mDragHandleByThis = false;
        mIsCancelEvent = false;
    }

    @Override
    public boolean canScrollVertically(int direction) {
        return mDragHandleByThis
                || (mCaptureItem!=null && mCaptureItem.isOpen())
                || mIsCancelEvent
                ||super.canScrollVertically(direction);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();

        if(mIsCancelEvent && action!=MotionEvent.ACTION_UP && action!=MotionEvent.ACTION_CANCEL)
            return true;
        else if(mIsCancelEvent){
            cancel();
            return true;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        switch (action){
            case MotionEvent.ACTION_DOWN:{
                mIsCancelEvent = false;
                mActivePointerId = ev.getPointerId(0);
                final float x = ev.getX(0);
                final float y = ev.getY(0);
                mLastMotionX = x;
                mLastMotionY = y;

                boolean pointOther = false;
                SwipeItemLayout pointItem = null;
                //首先知道ev针对的是哪个item
                View pointView = SwipeItemLayout.findTopChildUnder(this,(int)x,(int)y);
                if(pointView==null || !(pointView instanceof SwipeItemLayout))
                    pointOther = true;//可能是head view、bottom view或者其它类型的item
                else
                    pointItem = (SwipeItemLayout) pointView;

                //此时的pointOther=true，意味着点击的view为空或者点击的不是item
                //还没有把点击的是item但是不是capture item给过滤出来
                if(!pointOther && (mCaptureItem==null || mCaptureItem!=pointItem))
                    pointOther = true;

                //点击的是capture item
                if(!pointOther){
                    Mode touchMode = mCaptureItem.getTouchMode();

                    //如果它在fling，就转为drag
                    //需要拦截，并且requestDisallowInterceptTouchEvent
                    boolean disallowIntercept = false;
                    if(touchMode== Mode.FLING){
                        mCaptureItem.setTouchMode(Mode.DRAG);
                        disallowIntercept = true;
                        mDragHandleByThis = true;
                    }else {//如果是expand的，就不允许parent拦截
                        mCaptureItem.setTouchMode(Mode.TAP);
                        if(mCaptureItem.isOpen())
                            disallowIntercept = true;
                    }

                    if(disallowIntercept){
                        final ViewParent parent = getParent();
                        if (parent!= null)
                            parent.requestDisallowInterceptTouchEvent(true);
                    }
                }else{//capture item为null或者与point item不一样
                    //直接将其close掉
                    if(mCaptureItem!=null && mCaptureItem.isOpen()) {
                        mCaptureItem.close();
                        mIsCancelEvent = true;
                        return true;
                    }

                    if(pointItem!=null) {
                        mCaptureItem = pointItem;
                        mCaptureItem.setTouchMode(Mode.TAP);
                    }
                }

                //如果parent处于fling状态，此时，parent就会转为drag。此时，应该将后续move都交给parent处理
                if(!mDragHandleByThis)
                    mDragHandleBySuper = super.onInterceptTouchEvent(ev);
                return mDragHandleByThis || mDragHandleBySuper;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int actionIndex = ev.getActionIndex();
                final int pointerId = ev.getPointerId(actionIndex);
                if (pointerId == mActivePointerId) {
                    final int newIndex = actionIndex == 0 ? 1 : 0;
                    mActivePointerId = ev.getPointerId(newIndex);

                    mLastMotionX = ev.getX(newIndex);
                    mLastMotionY = ev.getY(newIndex);
                }

                return super.onInterceptTouchEvent(ev);
            }

            //down时，已经将capture item定下来了。所以，后面可以安心考虑event处理
            case MotionEvent.ACTION_MOVE: {
                //在down时，就被认定为parent的drag，所以，直接交给parent处理即可
                if(mDragHandleBySuper) {
                    if(mCaptureItem!=null)
                        mCaptureItem.close();
                    return super.onInterceptTouchEvent(ev);
                }

                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1)
                    break;

                final int x = (int) (ev.getX(activePointerIndex)+.5f);
                final int y = (int) ((int) ev.getY(activePointerIndex)+.5f);

                int deltaX = (int) (x - mLastMotionX);
                int deltaY = (int)(y-mLastMotionY);
                final int xDiff = Math.abs(deltaX);
                final int yDiff = Math.abs(deltaY);

                if(mCaptureItem!=null){
                    Mode touchMode = mCaptureItem.getTouchMode();

                    if(touchMode== Mode.TAP ){
                        //如果capture item是open的，下拉有两种处理方式：
                        //  1、下拉后，直接close item
                        //  2、只要是open的，就拦截所有它的消息，这样如果点击open的，就只能滑动该capture item
                        //网易邮箱，在open的情况下，下拉直接close
                        //QQ，在open的情况下，下拉也是close。但是，做的不够好，没有达到该效果。
                        if(xDiff>mTouchSlop && xDiff>yDiff){
                            mDragHandleByThis = true;
                            mCaptureItem.setTouchMode(Mode.DRAG);
                            final ViewParent parent = getParent();
                            parent.requestDisallowInterceptTouchEvent(true);

                            deltaX = deltaX>0 ? deltaX-mTouchSlop:deltaX+mTouchSlop;
                        }else{
                            //表明不是水平滑动，即不判定为SwipeItemLayout的滑动
                            //但是，可能是下拉刷新SwipeRefreshLayout或者RecyclerView的滑动
                            //一般的下拉判定，都是yDiff>mTouchSlop，所以，此处这么写不会出问题
                            //这里这么做以后，如果判定为下拉，就直接close
                            mDragHandleBySuper = super.onInterceptTouchEvent(ev);
                        }
                    }

                    touchMode = mCaptureItem.getTouchMode();
                    if(touchMode== Mode.DRAG){
                        mLastMotionX = x;
                        mLastMotionY = y;

                        //对capture item进行拖拽
                        mCaptureItem.trackMotionScroll(deltaX);
                    }
                }else
                    mDragHandleBySuper = super.onInterceptTouchEvent(ev);

                if(mDragHandleBySuper && mCaptureItem!=null)
                    mCaptureItem.close();
                return mDragHandleByThis || mDragHandleBySuper;
            }

            case MotionEvent.ACTION_UP:
                boolean ret = false;
                if(mDragHandleByThis && mCaptureItem!=null/**起始一定不为null*/){
                    Mode touchMode = mCaptureItem.getTouchMode();
                    if(touchMode== Mode.DRAG){
                        final VelocityTracker velocityTracker = mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                        int xVel = (int) velocityTracker.getXVelocity(mActivePointerId);
                        mCaptureItem.fling(xVel);
                        ret = true;
                    }
                }else
                    ret = super.onInterceptTouchEvent(ev);

                cancel();
                return ret;

            case MotionEvent.ACTION_CANCEL:
                if(mCaptureItem!=null)
                    mCaptureItem.revise();
                super.onInterceptTouchEvent(ev);
                cancel();
                break;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        final int actionIndex = ev.getActionIndex();

        if(mIsCancelEvent && action!=MotionEvent.ACTION_UP && action!=MotionEvent.ACTION_CANCEL)
            return true;
        else if(mIsCancelEvent){
            cancel();
            return true;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        switch (action){
            case MotionEvent.ACTION_DOWN:
                //如果调用super.onTouchEvent(ev);必然导致super.onInterceptTouchEvent(ev)无法返回true，所以不能调用它，直接返回true
                //但是super.onTouchEvent(ev)就无法处理DOWN消息，从而无法处理item click。所以，只能通过click listener的方式来处理点击消息！！！
                //不调用的问题在于会导致没有item click效果、没有flywheel效果
                 return super.onTouchEvent(ev);

            case MotionEvent.ACTION_POINTER_DOWN:
                mActivePointerId = ev.getPointerId(actionIndex);

                mLastMotionX = ev.getX(actionIndex);
                mLastMotionY = ev.getY(actionIndex);
                return super.onTouchEvent(ev);

            case MotionEvent.ACTION_POINTER_UP:
                final int pointerId = ev.getPointerId(actionIndex);
                if(pointerId==mActivePointerId){
                    final int newIndex = actionIndex == 0 ? 1 : 0;
                    mActivePointerId = ev.getPointerId(newIndex);

                    mLastMotionX = ev.getX(newIndex);
                    mLastMotionY = ev.getY(newIndex);
                }
                return super.onTouchEvent(ev);

            case MotionEvent.ACTION_MOVE: {
                //在down时，就被认定为parent的drag，所以，直接交给parent处理即可
                if(mDragHandleBySuper) {
                    if(mCaptureItem!=null)
                        mCaptureItem.close();
                    return super.onTouchEvent(ev);
                }

                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1)
                    break;

                final int x = (int) (ev.getX(activePointerIndex)+.5f);
                final int y = (int) ((int) ev.getY(activePointerIndex)+.5f);

                int deltaX = (int) (x - mLastMotionX);
                int deltaY = (int)(y-mLastMotionY);
                final int xDiff = Math.abs(deltaX);
                final int yDiff = Math.abs(deltaY);

                if(mCaptureItem!=null){
                    Mode touchMode = mCaptureItem.getTouchMode();

                    if(touchMode== Mode.TAP ){
                        //如果capture item是open的，下拉有两种处理方式：
                        //  1、下拉后，直接close item
                        //  2、只要是open的，就拦截所有它的消息，这样如果点击open的，就只能滑动该capture item
                        //网易邮箱，在open的情况下，下拉直接close
                        //QQ，在open的情况下，下拉也是close。但是，做的不够好，没有达到该效果。
                        if(xDiff>mTouchSlop && xDiff>yDiff){
                            mDragHandleByThis = true;
                            mCaptureItem.setTouchMode(Mode.DRAG);
                            final ViewParent parent = getParent();
                            parent.requestDisallowInterceptTouchEvent(true);

                            deltaX = deltaX>0 ? deltaX-mTouchSlop:deltaX+mTouchSlop;
                        }else if(yDiff>mTouchSlop){
                            //表明不是水平滑动，即不判定为SwipeItemLayout的滑动
                            //但是，可能是下拉刷新SwipeRefreshLayout或者RecyclerView的滑动
                            //一般的下拉判定，都是yDiff>mTouchSlop，所以，此处这么写不会出问题
                            //这里这么做以后，如果判定为下拉，就直接close
                            //不能调用onTouchEvent()，因为它一定返回true
                            mDragHandleBySuper = true;//super.onInterceptTouchEvent(ev);
                            super.onTouchEvent(ev);
                        }
                    }

                    touchMode = mCaptureItem.getTouchMode();
                    if(touchMode== Mode.DRAG){
                        mLastMotionX = x;
                        mLastMotionY = y;

                        //对capture item进行拖拽
                        mCaptureItem.trackMotionScroll(deltaX);
                    }
                }else
                    mDragHandleBySuper = super.onTouchEvent(ev);

                if(mDragHandleBySuper && mCaptureItem!=null)
                    mCaptureItem.close();
                return true;
            }

            case MotionEvent.ACTION_UP:
                if(mDragHandleByThis && mCaptureItem!=null/**起始一定不为null*/){
                    Mode touchMode = mCaptureItem.getTouchMode();
                    if(touchMode== Mode.DRAG){
                        final VelocityTracker velocityTracker = mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                        int xVel = (int) velocityTracker.getXVelocity(mActivePointerId);
                        mCaptureItem.fling(xVel);
                    }
                }else
                    super.onTouchEvent(ev);

                cancel();
                return true;

            case MotionEvent.ACTION_CANCEL:
                if(mCaptureItem!=null)
                    mCaptureItem.revise();
                super.onTouchEvent(ev);
                cancel();
                return true;
        }
        return true;
    }

    void cancel(){
        mDragHandleBySuper = false;
        mDragHandleByThis = false;
        mIsCancelEvent = false;
        mActivePointerId = -1;
        if(mVelocityTracker!=null){
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    public void closeAllItems(ViewGroup parent){
        if(mCaptureItem!=null && mCaptureItem.isOpen())
            mCaptureItem.close();
    }

}
