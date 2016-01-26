package org.dolphin.secret.browser;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import org.dolphin.job.Job;
import org.dolphin.job.Operator;
import org.dolphin.lib.ValueUtil;
import org.dolphin.secret.core.FileInfo;
import org.dolphin.secret.core.FileInfoContentCache;

/**
 * Created by yananh on 2016/1/23.
 */
public class ThumbnailImageVIew extends ImageView {
    private boolean attached = false;
    private boolean visible = false;
    private String filePath = null;
    private FileInfo fileInfo = null;
    private Job loadJob = null;

    public ThumbnailImageVIew(Context context) {
        super(context);
    }

    public ThumbnailImageVIew(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThumbnailImageVIew(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setFile(String path, FileInfo fileInfo) {
        notifyPropertyChanged(this.attached, this.visible, path, fileInfo);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ThumbnailImageVIew(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onAttachedToWindow() {
        notifyPropertyChanged(true, true, this.filePath, this.fileInfo);
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        notifyPropertyChanged(false, false, this.filePath, this.fileInfo);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        notifyPropertyChanged(this.attached, VISIBLE == visibility, this.filePath, this.fileInfo);
        super.onVisibilityChanged(changedView, visibility);
    }

    private void notifyPropertyChanged(boolean newAttached, boolean newVisible, String newPath, FileInfo newFileInfo) {
        int nextOperation = 0;  // 0什么都不做，1加载新的图片，2，终止所有操作
        do {
            if (!newAttached || null == newPath) {
                nextOperation = 3;
                break;
            }

            // attached and has path
            if (newAttached != this.attached) {
                nextOperation = 1;
            }

            if (!ValueUtil.isEquals(newPath, this.filePath)) {
                nextOperation = 1;
            }
        } while (false);

        switch (nextOperation) {
            case 0:
                break;
            case 1:
                if (loadJob != null) {
                    loadJob.abort();
                    loadJob = null;
                }
                loadThumbnail(newPath, newFileInfo);
                break;
            case 2:
                if (loadJob != null) {
                    loadJob.abort();
                    loadJob = null;
                }
                break;
        }

        this.visible = newVisible;
        this.attached = newAttached;
        this.fileInfo = newFileInfo;
        this.filePath = newPath;
    }


    private void loadThumbnail(final String filePath, final FileInfo fileInfo) {
        if (loadJob != null) {
            loadJob.abort();
            loadJob = null;
        }
        FileInfoContentCache cache = CacheManager.getInstance().getCache(filePath);
        if (null != cache) {
            Bitmap bm = cache.thumbnail;
            if (null != bm) {
                this.setImageBitmap(bm);
                return;
            }
        }

        Job job = new Job(filePath);
        job.then(new Operator<String, Bitmap>() {
            @Override
            public Bitmap operate(String input) throws Throwable {


                return null;
            }
        });


        return;
    }


}
