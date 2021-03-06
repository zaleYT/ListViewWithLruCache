package com.travis.listviewwithlrucache;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AbsListView.OnScrollListener {

    private static final String TAG = "main";

    private List<String> list;

    private ListView listView;
    private ListAdapter adapter;

    public int scrollFirstID;
    public int scrollEndID;

    private boolean isInit = false;

    public static final int INDEX_IMAGE_ID = 0;
    public static final int INDEX_IMAGE_PATH = 1;
    public static final int INDEX_IMAGE_SIZE = 2;
    public static final int INDEX_IMAGE_DISPLAY_NAME = 3;

    public static final int EXTERNAL_STORAGE_REQ_CODE = 15 ;

    private View loadMoreView;
    private LayoutInflater inflater;
    private List<String> listSeg; // 分次加载用的list
    private boolean isLoading = false;
    private int totalListCount; // list中item的总数，随着分段的显示，total会变化
    private final int LOAD_COUNT_ONCE = 10; // 每次加载的数量

    String[] projImage = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA, // 路径
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DISPLAY_NAME
    };

    Uri mImangeUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT > 23){
            getRuntimePermission();
        }else {
            init();
        }
    }

    private void init() {
        initImagePath();

        LruCache<String, Bitmap> memoryCache = getCache();

        inflater = LayoutInflater.from(this);
        loadMoreView = inflater.inflate(R.layout.load_more, null);
        //loadMoreView.setVisibility(View.VISIBLE);

        listSeg = new ArrayList<String>();
        updateListSeg();

        listView = (ListView)findViewById(R.id.list);
        listView.setOnScrollListener(this);
        listView.addFooterView(loadMoreView,null, false);

        adapter = new ListAdapter(this, listSeg, listView, memoryCache);
        listView.setAdapter(adapter);
    }

    /**
     * 更新分次加载的list
     */
    private void updateListSeg() {
        int j = 0;
        int segSize =  listSeg.size();

        for (int i = segSize; j < LOAD_COUNT_ONCE && i < list.size(); i++,j++){
            listSeg.add(list.get(i));
        }
    }

    /**
     * 创建缓存，缓存空间是进程最大内存的1/8
     * @return
     */
    private LruCache<String, Bitmap> getCache() {
        LruCache<String, Bitmap> cache = null;

        // maxMemory 返回值，单位是字节，下面得到的maxMemory是262144kB，即256MB
        int maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;

        cache = new LruCache<String, Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                // 返回当前bitmap所占用的内存字节数
                return value.getByteCount() / 1024;
            }
        };

        return cache;
    }

    private void initImagePath() {
        list = new ArrayList<String>();

        final Cursor cursor = getContentResolver().query(
                mImangeUri,
                projImage,
                null,
                null,
                null
        );
        if (cursor != null){
            String path;
            while (cursor.moveToNext()){
                path = cursor.getString(INDEX_IMAGE_PATH);
                list.add(path);
                Log.d(TAG, "image, path:" + cursor.getString(INDEX_IMAGE_PATH));
            }
        }
        cursor.close();
    }

    private void loadImage(){
        if (scrollEndID == list.size()) { // 最后一页，scrollEndID应该减一，并且应该移除footerView
            scrollEndID--;
            listView.removeFooterView(loadMoreView);
        }
        // 不到最后一页的时候，scrollEndID不用减一，这样可以加载下一页的一个item
        for (int i = scrollFirstID; i <= scrollEndID; i++){
            adapter.loadeImage(i);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {

        if (scrollState == SCROLL_STATE_IDLE){
            if (scrollEndID == totalListCount){
                if (!isLoading){
                    isLoading = true;
                    onLoad();
                }
            }else {
                loadImage();
            }
        }else {
            adapter.cancelTask();
        }
    }

    @Override
    public void onScroll(AbsListView absListView,
                         int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        int footerViewCount = listView.getFooterViewsCount(); // item总数不包含footerView的个数

        scrollFirstID = firstVisibleItem;
        scrollEndID = scrollFirstID +visibleItemCount - footerViewCount;

        totalListCount = totalItemCount - footerViewCount;

        if (!isInit && totalItemCount > 0){
            loadImage();
            isInit = true;
        }
    }

    private void onLoad() {
        try {
            Thread.sleep(1000); // 模拟耗时
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        updateListSeg();

        if (adapter == null){
            adapter = new ListAdapter(this, listSeg, listView, getCache());
            listView.setAdapter(adapter);
        }else {
            adapter.updateAdapter(listSeg);
        }

        loadComplete(); // 布局刷新结束
    }

    private void loadComplete() {
        //loadMoreView.setVisibility(View.INVISIBLE);
        isLoading = false;
        invalidateOptionsMenu();

        // 布局刷新结束后，加载数据
        loadImage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.cancelTask();
        adapter.cleanCache();
    }

    private void getRuntimePermission() {
        // 没有获得权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            // 如果APP的权限曾经被用户拒绝过，就需要在这里更用户做出解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)){
                //Toast.makeText(this, "please give me the permission", Toast.LENGTH_LONG).show();
            }
            // 请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    EXTERNAL_STORAGE_REQ_CODE);
        }else {
            // 获得了权限
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == EXTERNAL_STORAGE_REQ_CODE){
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                init();
            }else{
                finish();
            }
        }
    }


}
