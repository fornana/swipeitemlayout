package com.lots.travel.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.Scroller;
import com.nalan.swipeitem.R;

public class SwipeItemLayout extends ViewGroup {
    enum Mode{
        RESET, DRAG, FLING, CLICK
    }
    private Mode touchMode;

    private View mainItemView;

    private boolean mInLayout =false;

    private int scrollOffset;
    private int maxScrollOffset;

    private ScrollRunnable scrollRunnable;

    public SwipeItemLayout1(Context context) {
        this(context,null);
    }

    public SwipeItemLayout1(Context context, AttributeSet attrs) {
        super(context, attrs);

        touchMode = Mode.RESET;
        scrollOffset = 0;

        scrollRunnable = new ScrollRunnable(context);
    }

    public int getScrollOffset(){
        return scrollOffset;
    }

    public void open(){
        if(scrollOffset!=-maxScrollOffset){
            if(touchMode== Mode.FLING)
                scrollRunnable.abort();

            scrollRunnable.startScroll(scrollOffset,-maxScrollOffset);
        }
    }

    public void close(){
        if(scrollOffset!=0){
            if(touchMode== Mode.FLING)
                scrollRunnable.abort();

            scrollRunnable.startScroll(scrollOffset,0);
        }
    }

    void fling(int xVel){
        scrollRunnable.startFling(scrollOffset,xVel);
    }

    void revise(){
        if(scrollOffset<-maxScrollOffset/2)
            open();
        else
            close();
    }

    private void ensureChildren(){
        int childCount = getChildCount();

        for (int i=0;i<childCount;i++){
            View childView = getChildAt(i);
            ViewGroup.LayoutParams tempLp = childView.getLayoutParams();

            if(tempLp==null || !(tempLp instanceof LayoutParams))
                throw new IllegalStateException("缺少layout参数");

            LayoutParams lp = (LayoutParams) tempLp;
            if(lp.itemType==0x01){
                mainItemView = childView;
            }
        }

        if(mainItemView==null)
            throw new IllegalStateException("main item不能为空");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //确定children
        ensureChildren();

        //先测量main
        LayoutParams lp = (LayoutParams) mainItemView.getLayoutParams();

        measureChildWithMargins(
                mainItemView,
                widthMeasureSpec,getPaddingLeft()+getPaddingRight(),
                heightMeasureSpec,getPaddingTop()+getPaddingBottom());

        setMeasuredDimension(
                mainItemView.getMeasuredWidth()+ getPaddingLeft()+getPaddingRight()+ lp.leftMargin+lp.rightMargin
                ,mainItemView.getMeasuredHeight()+getPaddingTop()+getPaddingBottom()+lp.topMargin+lp.bottomMargin);

        //测试menu
        int menuWidthSpec = MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED);
        int menuHeightSpec = MeasureSpec.makeMeasureSpec(mainItemView.getMeasuredHeight(),MeasureSpec.EXACTLY);
        for(int i=0;i<getChildCount();i++){
            View menuView = getChildAt(i);
            lp = (LayoutParams) menuView.getLayoutParams();

            if(lp.itemType==0x01)
                continue;

            measureChildWithMargins(menuView,menuWidthSpec,0,menuHeightSpec,0);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout = true;

        //确定children
        ensureChildren();

        int pl = getPaddingLeft();
        int pt = getPaddingTop();
        int pr = getPaddingRight();
        int pb = getPaddingBottom();

        LayoutParams lp;

        //layout main
        lp = (LayoutParams) mainItemView.getLayoutParams();
        mainItemView.layout(
                pl+lp.leftMargin,
                pt+lp.topMargin,
                getWidth()-pr-lp.rightMargin,
                getHeight()-pb-lp.bottomMargin);

        //layout menu
        int totalLength = 0;
        int menuLeft = mainItemView.getRight()+lp.rightMargin;
        for(int i=0;i<getChildCount();i++){
            View menuView = getChildAt(i);
            lp = (LayoutParams) menuView.getLayoutParams();

            if(lp.itemType==0x01)
                continue;

            int tempLeft = menuLeft+lp.leftMargin;
            int tempTop = pt+lp.topMargin;
            menuView.layout(
                    tempLeft,
                    tempTop,
                    tempLeft+menuView.getMeasuredWidth()+lp.rightMargin,
                    tempTop+menuView.getMeasuredHeight()+lp.bottomMargin);

            menuLeft = menuView.getRight()+lp.rightMargin;
            totalLength += lp.leftMargin+lp.rightMargin+menuView.getMeasuredWidth();
        }

        maxScrollOffset = totalLength;
        scrollOffset = scrollOffset<-maxScrollOffset/2 ? -maxScrollOffset:0;

        offsetChildrenLeftAndRight(scrollOffset);

        mInLayout = false;
    }

    void offsetChildrenLeftAndRight(int delta){
        for(int i=0;i<getChildCount();i++){
            View childView = getChildAt(i);
            ViewCompat.offsetLeftAndRight(childView,delta);
        }
    }

    @Override
    public void requestLayout() {
        if (!mInLayout) {
            super.requestLayout();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(scrollRunnable);
        touchMode = Mode.RESET;
        scrollOffset = 0;
    }

    //展开的情况下，拦截down event，避免触发点击main事件
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                View pointView = findTopChildUnder(this,x,y);
                if(pointView!=null && pointView==mainItemView && scrollOffset !=0)
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
                if(pointView!=null && pointView==mainItemView && touchMode== Mode.CLICK && scrollOffset !=0)
                    return true;
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                View pointView = findTopChildUnder(this,x,y);
                if(pointView!=null && pointView==mainItemView && scrollOffset !=0)
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
                if(pointView!=null && pointView==mainItemView && touchMode== Mode.CLICK && scrollOffset !=0) {
                    close();
                    return true;
                }
            }
        }

        return false;
    }

    void setTouchMode(Mode mode){
        if(mode==touchMode)
            return;

        if(touchMode== Mode.FLING)
            removeCallbacks(scrollRunnable);

        touchMode = mode;
    }

    public Mode getTouchMode(){
        return touchMode;
    }

    boolean trackMotionScroll(int deltaX){
        if(deltaX==0)
            return true;

        boolean over = false;
        int newLeft = scrollOffset+deltaX;
        if((deltaX>0 && newLeft>0) || (deltaX<0 && newLeft<-maxScrollOffset)){
            over = true;
            newLeft = Math.min(newLeft,0);
            newLeft = Math.max(newLeft,-maxScrollOffset);
        }

        offsetChildrenLeftAndRight(newLeft-scrollOffset);
        scrollOffset = newLeft;
        return over;
    }

    private static final Interpolator sInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    private class ScrollRunnable implements Runnable{
        private Scroller scroller;
        private boolean abort;
        private int minVelocity;

        ScrollRunnable(Context context){
            scroller = new Scroller(context,sInterpolator);
            abort = false;

            ViewConfiguration configuration = ViewConfiguration.get(context);
            minVelocity = configuration.getScaledMinimumFlingVelocity();
        }

        void startScroll(int startX,int endX){
            if(startX!=endX){
                Log.e("scroll - startX - endX",""+startX+" "+endX);
                setTouchMode(Mode.FLING);
                abort = false;

                scroller.startScroll(startX,0,endX-startX,0, 400);
                ViewCompat.postOnAnimation(SwipeItemLayout1.this,this);
            }
        }

        void startFling(int startX,int xVel){
            Log.e("fling - startX",""+startX);

            if(xVel> minVelocity && startX!=0) {
                startScroll(startX, 0);
                return;
            }

            if(xVel<-minVelocity && startX!=-maxScrollOffset) {
                startScroll(startX, -maxScrollOffset);
                return;
            }

            startScroll(startX,startX>-maxScrollOffset/2 ? 0:-maxScrollOffset);
        }

        void abort(){
            if(!abort){
                abort = true;
                if(!scroller.isFinished()){
                    scroller.abortAnimation();
                    removeCallbacks(this);
                }
            }
        }

        @Override
        public void run() {
            Log.e("abort",Boolean.toString(abort));
            if(!abort){
                boolean more = scroller.computeScrollOffset();
                int curX = scroller.getCurrX();
                Log.e("curX",""+curX);

                boolean atEdge = false;
                if(curX!=scrollOffset)
                    atEdge = trackMotionScroll(curX-scrollOffset);

                if(more && !atEdge) {
                    ViewCompat.postOnAnimation(SwipeItemLayout1.this, this);
                    return;
                }else{
                    removeCallbacks(this);
                    if(!scroller.isFinished())
                        scroller.abortAnimation();
                    setTouchMode(Mode.RESET);
                }
            }
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams ? (LayoutParams) p : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public static class LayoutParams extends MarginLayoutParams{
        public int itemType = -1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.SwipeItemLayout_Layout);
            itemType = a.getInt(R.styleable.SwipeItemLayout_Layout_layout_itemType,-1);
            a.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            itemType = source.itemType;
        }

        public LayoutParams(int width, int height) {
            super(width, height);
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


    public static class OnSwipeItemTouchListener implements RecyclerView.OnItemTouchListener {
        private SwipeItemLayout1 captureItem;
        private float lastMotionX;
        private float lastMotionY;
        private VelocityTracker velocityTracker;

        private int activePointerId;

        private int touchSlop;
        private int maximumVelocity;

        private boolean parentHandled;
        private boolean probingParentProcess;

        private boolean ignoreActions = false;

        public OnSwipeItemTouchListener(Context context){
            ViewConfiguration configuration = ViewConfiguration.get(context);
            touchSlop = configuration.getScaledTouchSlop();
            maximumVelocity = configuration.getScaledMaximumFlingVelocity();
            activePointerId = -1;
            parentHandled = false;
            probingParentProcess = false;
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent ev) {
            if(probingParentProcess)
                return false;

            boolean intercept = false;
            final int action = ev.getActionMasked();

            if(action!=MotionEvent.ACTION_DOWN && ignoreActions)
                return true;

            if(action!=MotionEvent.ACTION_DOWN && (captureItem==null||parentHandled))
                return false;

            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain();
            }
            velocityTracker.addMovement(ev);

            switch (action){
                case MotionEvent.ACTION_DOWN:{
                    ignoreActions = false;
                    parentHandled = false;
                    activePointerId = ev.getPointerId(0);
                    final float x = ev.getX();
                    final float y = ev.getY();
                    lastMotionX = x;
                    lastMotionY = y;

                    boolean pointOther = false;
                    SwipeItemLayout1 pointItem = null;
                    //首先知道ev针对的是哪个item
                    View pointView = findTopChildUnder(rv,(int)x,(int)y);
                    if(pointView==null || !(pointView instanceof SwipeItemLayout1)){
                        //可能是head view或bottom view
                        pointOther = true;
                    }else
                        pointItem = (SwipeItemLayout1) pointView;

                    //此时的pointOther=true，意味着点击的view为空或者点击的不是item
                    //还没有把点击的是item但是不是capture item给过滤出来
                    if(!pointOther && (captureItem ==null || captureItem !=pointItem))
                        pointOther = true;

                    //点击的是capture item
                    if(!pointOther){
                        Mode mode = captureItem.getTouchMode();

                        //如果它在fling，就转为drag
                        //需要拦截，并且requestDisallowInterceptTouchEvent
                        boolean disallowIntercept = false;
                        if(mode== Mode.FLING){
                            captureItem.setTouchMode(Mode.DRAG);
                            disallowIntercept = true;
                            intercept = true;
                        }else {//如果是expand的，就不允许parent拦截
                            captureItem.setTouchMode(Mode.CLICK);
                            if(captureItem.getScrollOffset()!=0)
                                disallowIntercept = true;
                        }

                        if(disallowIntercept){
                            final ViewParent parent = rv.getParent();
                            if (parent!= null)
                                parent.requestDisallowInterceptTouchEvent(true);
                        }
                    }else{//capture item为null或者与point item不一样
                        //直接将其close掉
                        if(captureItem !=null &&
                                captureItem.getScrollOffset()!=0) {
                            captureItem.close();
                            ignoreActions = true;
                            return true;
                        }

                        captureItem = null;

                        if(pointItem!=null) {
                            captureItem = pointItem;
                            captureItem.setTouchMode(Mode.CLICK);
                        }
                    }

                    //如果parent处于fling状态，此时，parent就会转为drag。应该将后续move都交给parent处理
                    probingParentProcess = true;
                    parentHandled = rv.onInterceptTouchEvent(ev);
                    probingParentProcess = false;
                    if(parentHandled) {
                        intercept = false;
                        //在down时，就被认定为parent的drag，所以，直接交给parent处理即可
                        if(captureItem !=null && captureItem.getScrollOffset()!=0)
                            captureItem.close();
                    }
                    break;
                }

                case MotionEvent.ACTION_POINTER_DOWN: {
                    final int actionIndex = ev.getActionIndex();
                    activePointerId = ev.getPointerId(actionIndex);

                    lastMotionX = ev.getX(actionIndex);
                    lastMotionY = ev.getY(actionIndex);
                    break;
                }

                case MotionEvent.ACTION_POINTER_UP: {
                    final int actionIndex = ev.getActionIndex();
                    final int pointerId = ev.getPointerId(actionIndex);
                    if (pointerId == activePointerId) {
                        final int newIndex = actionIndex == 0 ? 1 : 0;
                        activePointerId = ev.getPointerId(newIndex);

                        lastMotionX = ev.getX(newIndex);
                        lastMotionY = ev.getY(newIndex);
                    }
                    break;
                }

                //down时，已经将capture item定下来了。所以，后面可以安心考虑event处理
                case MotionEvent.ACTION_MOVE: {
                    final int activePointerIndex = ev.findPointerIndex(activePointerId);
                    if (activePointerIndex == -1)
                        break;

                    final int x = (int) (ev.getX(activePointerIndex)+.5f);
                    final int y = (int) ((int) ev.getY(activePointerIndex)+.5f);

                    int deltaX = (int) (x - lastMotionX);
                    int deltaY = (int)(y- lastMotionY);
                    final int xDiff = Math.abs(deltaX);
                    final int yDiff = Math.abs(deltaY);

                    Mode mode = captureItem.getTouchMode();

                    if(mode== Mode.CLICK){
                        //如果capture item是open的，下拉有两种处理方式：
                        //  1、下拉后，直接close item
                        //  2、只要是open的，就拦截所有它的消息，这样如果点击open的，就只能滑动该capture item
                        if(xDiff> touchSlop && xDiff>yDiff){
                            captureItem.setTouchMode(Mode.DRAG);
                            final ViewParent parent = rv.getParent();
                            parent.requestDisallowInterceptTouchEvent(true);

                            deltaX = deltaX>0 ? deltaX-touchSlop:deltaX+touchSlop;
                        }else/* if(yDiff>touchSlop)*/{
                            probingParentProcess = true;
                            parentHandled = rv.onInterceptTouchEvent(ev);
                            probingParentProcess = false;

                            if(parentHandled && captureItem.getScrollOffset() != 0)
                                captureItem.close();
                        }
                    }

                    mode = captureItem.getTouchMode();
                    if(mode== Mode.DRAG){
                        intercept = true;
                        lastMotionX = x;
                        lastMotionY = y;

                        //对capture item进行拖拽
                        captureItem.trackMotionScroll(deltaX);
                    }
                    break;
                }

                case MotionEvent.ACTION_UP:
                    Mode mode = captureItem.getTouchMode();
                    if(mode== Mode.DRAG){
                        final VelocityTracker velocityTracker = this.velocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
                        int xVel = (int) velocityTracker.getXVelocity(activePointerId);
                        captureItem.fling(xVel);

                        intercept = true;
                    }
                    cancel();
                    break;

                case MotionEvent.ACTION_CANCEL:
                    captureItem.revise();
                    cancel();
                    break;
            }

            return intercept;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent ev) {
            if(ignoreActions)
                return;

            final int action = ev.getActionMasked();
            final int actionIndex = ev.getActionIndex();

            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain();
            }
            velocityTracker.addMovement(ev);

            switch (action){
                case MotionEvent.ACTION_POINTER_DOWN:
                    activePointerId = ev.getPointerId(actionIndex);

                    lastMotionX = ev.getX(actionIndex);
                    lastMotionY = ev.getY(actionIndex);
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    final int pointerId = ev.getPointerId(actionIndex);
                    if(pointerId== activePointerId){
                        final int newIndex = actionIndex == 0 ? 1 : 0;
                        activePointerId = ev.getPointerId(newIndex);

                        lastMotionX = ev.getX(newIndex);
                        lastMotionY = ev.getY(newIndex);
                    }
                    break;

                //down时，已经将capture item定下来了。所以，后面可以安心考虑event处理
                case MotionEvent.ACTION_MOVE: {
                    final int activePointerIndex = ev.findPointerIndex(activePointerId);
                    if (activePointerIndex == -1)
                        break;

                    final float x = ev.getX(activePointerIndex);
                    final float y = (int) ev.getY(activePointerIndex);

                    int deltaX = (int) (x - lastMotionX);

                    if(captureItem !=null && captureItem.getTouchMode()== Mode.DRAG){
                        lastMotionX = x;
                        lastMotionY = y;

                        //对capture item进行拖拽
                        captureItem.trackMotionScroll(deltaX);
                    }
                    break;
                }

                case MotionEvent.ACTION_UP:
                    if(captureItem !=null){
                        Mode mode = captureItem.getTouchMode();
                        if(mode== Mode.DRAG){
                            final VelocityTracker velocityTracker = this.velocityTracker;
                            velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
                            int xVel = (int) velocityTracker.getXVelocity(activePointerId);
                            captureItem.fling(xVel);
                        }
                    }
                    cancel();
                    break;

                case MotionEvent.ACTION_CANCEL:
                    if(captureItem !=null)
                        captureItem.revise();

                    cancel();
                    break;

            }
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}

        void cancel(){
            parentHandled = false;
            activePointerId = -1;
            if(velocityTracker !=null){
                velocityTracker.recycle();
                velocityTracker = null;
            }
        }

    }

}
