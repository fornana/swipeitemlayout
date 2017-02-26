# swipeitemlayout
非常轻量级的RecyclerView左滑菜单选项控件。

使用很简单，只需要一个类文件即可。将SwipeItemLayout拷贝到项目下即可使用。

以QQ的左滑为参考设计的，与SwipeRefreshLayout无冲突。

使用方法如下：
  
  1、recyclerView.addOnItemTouchListener(new SwipeItemLayout.OnSwipeItemTouchListener(this));
  
  2、item layout文件
    以SwipeItemLayout为item的root view，添加两个ViewGroup，第一个为main部分，即显示在中心，第二个为menu部分，即显示在右侧
  
  3、click、long click点击效果以及listener
    将item里的child当作普通的view来对待即可。使用setOnClickListener以及setOnLongClickListener。至于显示效果，通常怎么设置，这里也怎么设置。
  
效果图：
	![Image text](https://raw.githubusercontent.com/fornana/swipeitemlayout/master/img/example.png)