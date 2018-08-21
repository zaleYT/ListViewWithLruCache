# ListViewWithLruCache
LruCache

===8-21，2018 修改记录===
1. 增加分段加载的特性
2. 注意在onScroll中计算totalItemCount时，要减去footerView的数量
3. 注意adpater中notifyDataSetInvalidated 和 notifyDataSetChanged的区别

特性：

1、使用了内存缓存LruCache

2、状态栏透明，产生沉浸式状态栏

3、修正图片在列表显示时，图片逆时针旋转了90度的现象
