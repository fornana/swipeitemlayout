# swipeitemlayout
轻量级RecyclerView左滑实现，以及ExpandableListView、ListView的左滑实现

--------------------------  2017-10-23  ---------------------------------

最近项目有用到RecyclerView左滑，代码稍作修改。具体为SwipeItemLayout-2017-10-23.java。
左滑+长按拖拽都可以不会冲突，先添加ItemTouchHelper做长按拖拽，再添加
addOnItemTouchListener(new SwipeItemLayout.OnSwipeItemTouchListener(this))即可。
项目太赶没时间维护，有兴趣的朋友可以看看。


使用方法稍有不同，具体如下：
<SwipeItemLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
	
	//main部分
    <RelativeLayout
        app:layout_itemType="main"
        android:layout_width="match_parent"
        android:layout_height="wrap_content">
		
	</RelativeLayout>
	
	//menu部分
	<Button 
		app:layout_itemType="menu`"
        android:layout_width="60dp"
        android:layout_height="match_parent"/>
	
	<Button 
		app:layout_itemType="menu`"
        android:layout_width="60dp"
        android:layout_height="match_parent"/>
	
</SwipeItemLayout>
	即指定layout_itemType即可，“main”只能是一个，“menu”可以多个。


--------------------------  2017-4-20  ---------------------------------
	
RecyclerView左滑：http://www.jianshu.com/p/f2a9b860858e

ExpandableListView、ListView左滑：http://www.jianshu.com/p/a00073b55f77

RecyclerView左滑，不需要继承特地的adapter，不需要使用重写过的RecyclerView，只需要一个类即可。

以QQ的左滑为参考设计的，与SwipeRefreshLayout无冲突。

使用方法如下：
  
  1、recyclerView.addOnItemTouchListener(new SwipeItemLayout.OnSwipeItemTouchListener(this));
  
  2、item layout文件
    以SwipeItemLayout为item的root view，添加两个ViewGroup，第一个为main部分，即显示在中心，第二个为menu部分，即显示在右侧
  
  3、click、long click点击效果以及listener
    将item里的child当作普通的view来对待即可。使用setOnClickListener以及setOnLongClickListener。至于显示效果，通常怎么设置，这里也怎么设置。
  
RecyclerView与ListView左滑效果图：
	![Image text](https://raw.githubusercontent.com/fornana/swipeitemlayout/master/img/example1.png)