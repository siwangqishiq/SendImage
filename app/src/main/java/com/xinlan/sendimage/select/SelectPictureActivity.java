
package com.xinlan.sendimage.select;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.decode.BaseImageDecoder;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;


public class SelectPictureActivity extends AppCompatActivity {

    public static void start(Activity activity, int requestCode){
        Intent it = new Intent(activity,SelectPictureActivity.class);
        activity.startActivityForResult(it,requestCode);
    }


    @Override
    protected void onCreate(final Bundle b) {
        super.onCreate(b);
        checkInitImageLoader();
        // Create new fragment and transaction
        Fragment newFragment = new BucketsFragment();
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();

        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack
        transaction.replace(android.R.id.content, newFragment);

        // Commit the transaction
        transaction.commit();
    }

    void showBucket(final int bucketId) {
        Bundle b = new Bundle();
        b.putInt("bucket", bucketId);
        Fragment f = new ImagesFragment();
        f.setArguments(b);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, f).addToBackStack(null).commit();
    }

    void imageSelected(final String imgPath, final String imgTaken, final long imageSize) {
        returnResult(imgPath, imgTaken, imageSize);
    }

    private void returnResult(final String imgPath, final String imageTaken, final long imageSize) {
        Intent result = new Intent();
        result.putExtra("imgPath", imgPath);
        result.putExtra("dateTaken", imageTaken);
        result.putExtra("imageSize", imageSize);
        setResult(RESULT_OK, result);
        finish();
    }

    /**
     * 检测图片载入框架是否导入  若没有 则导入并初始化
     */
    protected void checkInitImageLoader() {
        if (!ImageLoader.getInstance().isInited()) {
            initImageLoader();
        }//end if
    }

    /**
     * 初始化图片载入框架
     */
    private void initImageLoader() {
        File cacheDir = StorageUtils.getCacheDirectory(this);
        int MAXMEMONRY = (int) (Runtime.getRuntime().maxMemory());
        // System.out.println("dsa-->"+MAXMEMONRY+"   "+(MAXMEMONRY/5));//.memoryCache(new
        // LruMemoryCache(50 * 1024 * 1024))
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                this).memoryCacheExtraOptions(480, 800).defaultDisplayImageOptions(defaultOptions)
                .diskCacheExtraOptions(480, 800, null).threadPoolSize(3)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .tasksProcessingOrder(QueueProcessingType.FIFO)
                .denyCacheImageMultipleSizesInMemory()
                .memoryCache(new LruMemoryCache(MAXMEMONRY / 5))
                .diskCache(new UnlimitedDiskCache(cacheDir))
                .diskCacheFileNameGenerator(new HashCodeFileNameGenerator()) // default
                .imageDownloader(new BaseImageDownloader(this)) // default
                .imageDecoder(new BaseImageDecoder(false)) // default
                .defaultDisplayImageOptions(DisplayImageOptions.createSimple()).build();

        ImageLoader.getInstance().init(config);
    }
}
