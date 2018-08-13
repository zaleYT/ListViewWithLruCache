package com.travis.listviewwithlrucache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.Inflater;

/**
 * Created by yutao on 2018/8/13.
 */

public class ListAdapter extends BaseAdapter {

    private Context context;
    private List<String> list;
    private ListView listView;

    private LruCache<String, Bitmap> lruCache;
    public Set<ImageAsyncTask> tasks;

    public ListAdapter(Context context, List<String> list, ListView listView, LruCache<String, Bitmap> memoryCache) {
        this.context = context;
        this.list = list;
        this.listView = listView;

        lruCache = memoryCache;

        tasks = new HashSet<ImageAsyncTask>();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public String getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        ViewHolder holder;
        if (view == null){
            view = LayoutInflater.from(context).inflate(R.layout.list_item,null);
            holder = new ViewHolder();
            holder.imageView = (ImageView)view.findViewById(R.id.item_image);
            view.setTag(holder);
        }else {
            holder = (ViewHolder)view.getTag();
        }

        holder.imageView.setTag(i);

        Bitmap bitmap = lruCache.get(getItem(i));
        if (bitmap != null){
            holder.imageView.setImageBitmap(bitmap);
        }else {
            holder.imageView.setImageBitmap(null);
            holder.imageView.setBackgroundResource(R.color.colorGray);
        }

        return view;
    }

    /**
     * 添加当前key值对应的Bitmap到LRUCache中
     * @param key
     * @param bitmap
     */
    public void addBitmapToMemoryCache(String key, Bitmap bitmap){
        if (getBitmapFromMemoryCache(key) == null){
            lruCache.put(key, bitmap);
        }
    }

    /**
     * 从缓存LRUCache中读取对应于Key的Bitmap对象
     * @param key
     * @return
     */
    public Bitmap getBitmapFromMemoryCache(String key){
        return lruCache.get(key);
    }


    public void loadeImage(int position){
        String url = getItem(position);
        Bitmap bitmap = getBitmapFromMemoryCache(url);

        if (bitmap != null){ // 缓存中有对应的数据

            ImageView imageView = listView.findViewWithTag(position);
            if (imageView != null){
                imageView.setImageBitmap(bitmap);
            }
        }else { // 缓存中没有对应的数据
            ImageAsyncTask task = new ImageAsyncTask(position);
            tasks.add(task);
            task.execute(url);
        }
    }

    public void cancelTask(){
        for (ImageAsyncTask task : tasks){
            task.cancel(false);
        }
    }

    private Bitmap decodeSampleBitmapFromUrl(String url, int reqWidth, int reqHeight){
        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;
        Bitmap beforeB = BitmapFactory.decodeFile(url,options);
        options.inSampleSize = calculateSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        Bitmap afterBitmap = BitmapFactory.decodeFile(url, options);
        return afterBitmap;
    }

    private int calculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        int size = 1;
        int width = options.outWidth;
        int height = options.outHeight;

        if (width >reqWidth || height > reqHeight){
            int widthRatio = Math.round((float) width / (float)reqWidth);
            int heightRatio = Math.round((float)height / (float)reqHeight);

            size = widthRatio < heightRatio ?
                    widthRatio : heightRatio;
        }

        return size;
    }

    public void cleanCache() {

        lruCache.evictAll();
    }

    private class ViewHolder{
        ImageView imageView;
    }

    private class ImageAsyncTask extends AsyncTask<String, Void, Bitmap>{

        private int index;

        public ImageAsyncTask(int position) {
            index = position;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            if (isCancelled()) return null;

            String urlStr = strings[0];
            InputStream in = null;
            Bitmap bitmap = decodeSampleBitmapFromUrl(urlStr, 100, 100);

            if (bitmap != null){
                lruCache.put(urlStr, bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null){
                ImageView imageView = (ImageView)listView.findViewWithTag(index);

                if (imageView != null){
                    imageView.setImageBitmap(bitmap);
                }
            }
            tasks.remove(this);
        }
    }
}
