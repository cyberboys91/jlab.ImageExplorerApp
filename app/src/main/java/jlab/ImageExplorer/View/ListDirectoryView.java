package jlab.ImageExplorer.View;

import android.view.View;
import android.os.Handler;
import android.content.Context;
import jlab.ImageExplorer.R;
import android.widget.ListView;
import android.widget.ImageView;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.AdapterView;

import jlab.ImageExplorer.Resource.AlbumDirectory;
import jlab.ImageExplorer.Utils;
import jlab.ImageExplorer.Interfaces;
import jlab.ImageExplorer.Resource.Resource;
import jlab.ImageExplorer.Resource.Directory;
import jlab.ImageExplorer.Resource.FileResource;
import jlab.ImageExplorer.Activity.DirectoryActivity;

/*
 * Created by Javier on 27/08/2017.
 */

public class ListDirectoryView extends ListView implements AbsListView.OnScrollListener,
        Interfaces.IListContent {

    private int last;
    private int first;
    private int antFirst;
    private boolean scrolling = false, up = false;
    private Handler handler = new Handler();
    private Directory mdirectory;
    private String relUrlDirectoryRoot;
    private String nameDirectoryRoot;
    private ResourceDetailsAdapter mAdapter;
    private DirectoryActivity mListener;

    public ListDirectoryView(Context context) {
        super(context);
        mAdapter = new ResourceDetailsAdapter();
        setAdapter(mAdapter);
    }

    public ListDirectoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAdapter = new ResourceDetailsAdapter();
        setAdapter(mAdapter);
    }

    public ListDirectoryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mAdapter = new ResourceDetailsAdapter();
        setAdapter(mAdapter);
    }

    public void loadItemClickListener() {
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openResource(mdirectory.getResource(position), position);
            }
        });
        setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int position, long id) {
                return mListener.onResourceLongClick(mdirectory.getResource(position), position);
            }
        });
        setOnScrollListener(this);
    }

    @Override
    public int getFirstVisiblePosition() {
        return first;
    }

    @Override
    public void openResource(Resource res, int position) {
        scrolling = false;
        try {
            Utils.Variables var = Utils.stackVars.get(Utils.stackVars.size() - 1);
            var.BeginPosition = getFirstVisiblePosition();
            if (res.isDir()) {
                Utils.stackVars.add(new Utils.Variables(res.getRelUrl(), res.getName(), 0));
                loadDirectory();
                mListener.onDirectoryClick(res.getName(), res.getRelUrl());
            } else
                mListener.onFileClick((FileResource) res, position);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setNumColumns(int i) {

    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        if (scrollState == SCROLL_STATE_IDLE && mdirectory != null && mdirectory.loaded()) {
            scrollingStop(absListView);
            scrolling = false;
        }
        scrolling = scrollState == SCROLL_STATE_FLING || scrollState == SCROLL_STATE_TOUCH_SCROLL;
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        antFirst = first != firstVisibleItem ? first : antFirst;
        first = firstVisibleItem;
        last = firstVisibleItem + visibleItemCount - 1;
        up = antFirst < first;
    }

    @Override
    public void onViewRemoved(View child) {
        freeIconDrawable(child);
        super.onViewRemoved(child);
    }

    private void freeIconDrawable(View view) {
        if (view != null) {
            ImageView ivIcon = (ImageView) view.findViewById(R.id.ivResourceIcon);
            ivIcon.setImageDrawable(null);
        }
    }

    private void scrollingStop(AbsListView view) {
        try {
            int length = getChildCount();
            int index = up ? first : last;
            for (int i = up ? 0 : length - 1; (up ? i < length : i >= 0)
                    && (up ? index <= last : index >= 0); i += up ? 1 : -1) {
                Resource elem = mdirectory.getResource(index);
                index += up ? 1 : -1;
                if (!elem.isDir() && ((FileResource) elem).isThumbnailer()) {
                    View child = view.getChildAt(i);
                    mListener.loadThumbnailForFile((FileResource) elem,
                            (ImageView) child.findViewById(R.id.ivResourceIcon),
                            (ImageView) view.getChildAt(i).findViewById(R.id.ivFavorite),true, false);
                }
                else if (elem instanceof AlbumDirectory && ((AlbumDirectory) elem).getCountElements() > 0)
                    mListener.loadThumbnailForFile((FileResource) ((AlbumDirectory) elem).getResource(0),
                            (ImageView) view.getChildAt(i).findViewById(R.id.ivResourceIcon),
                            (ImageView) view.getChildAt(i).findViewById(R.id.ivFavorite),true, true);
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setRelUrlDirectoryRoot(String nameRoot, String relUrlRoot) {
        this.relUrlDirectoryRoot = relUrlRoot;
        this.nameDirectoryRoot = nameRoot;
    }

    public void loadParentDirectory() {
        if (!Utils.stackVars.isEmpty()) {
            Utils.stackVars.remove(Utils.stackVars.size() - 1);
            loadDirectory();
        }
    }

    @Override
    public boolean scrolling() {
        return scrolling;
    }

    @Override
    public ResourceDetailsAdapter getResourceDetailsAdapter() {
        return mAdapter;
    }

    public void loadDirectory() {
        if (Utils.stackVars.isEmpty()) {
            Utils.stackVars.add(new Utils.Variables(Utils.RELURL_SPECIAL_DIR, getContext().getString(R.string.all_images), 0));
            mListener.onDirectoryClick(getContext().getString(R.string.all_images), Utils.RELURL_SPECIAL_DIR);
        }

        Utils.Variables vars = Utils.stackVars.get(Utils.stackVars.size() - 1);
        mdirectory = mListener.getDirectory(vars.NameDirectory, vars.RelUrlDirectory);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(Utils.TIME_WAIT_LOADING);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                synchronized (Directory.monitor) {
                    if (!mdirectory.loaded())
                        handler.sendEmptyMessage(Utils.LOADING_VISIBLE);
                }
            }
        }).start();
        mAdapter.clear();
        handler.sendEmptyMessage(Utils.REFRESH_LISTVIEW);
        mdirectory.openAsynchronic(handler);
    }

    @Override
    public int getNumColumns() {
        return 1;
    }

    @Override
    public void loadContent() {
        mAdapter.clear();
        mAdapter.addAll(mdirectory.getContent());
    }

    public final boolean isEmpty() {
        return mdirectory.getContent().isEmpty();
    }

    public Directory getDirectory() {
        return mdirectory;
    }

    @Override
    public void setDirectory(Directory directory) {
        this.mdirectory = directory;
    }

    public void setListeners(DirectoryActivity activityDirectory) {
        this.mAdapter.setonGetSetViewListener(activityDirectory);
        this.mListener = activityDirectory;
    }
}
