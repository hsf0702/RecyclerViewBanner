package you.chen.banner;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by you on 2017/8/31.
 */

public class BannerPager extends RecyclerView {
    /**
     * 默认切换一次动画间隔
     */
    private static final int DEFAULT_DELAY = 5000;

    static final int SCROLL_MAX = 100;

    static final int SCROLL_COUNT = 10;

    static final int SCROLL_LEFT = SCROLL_MAX - SCROLL_COUNT;

    /**
     * 执行Runnable
     */
    private Handler handler = new Handler();
    /**
     * 定时滑动Runnable
     */
    private PostRunnable runnable = new PostRunnable();

    private int currentItem = NO_POSITION;

    private int currentPosition = NO_POSITION;

    /**
     * 针对RecyclerView的修改
     */
    private boolean hasAttachedToWindow;

    private ProxyAdapter proxyAdapter;

    private BannerAdapter adapter;

    public BannerPager(Context context) {
        super(context);
        init();
    }

    public BannerPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BannerPager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setHasFixedSize(true);
        this.setOnFlingListener(new BannerSnap(this) {
            @Override
            protected void onSnap(View snapView) {
                if (adapter == null) return;
                int itemCount = adapter.getItemCount();
                if (itemCount < 2) return;
                currentPosition = getChildAdapterPosition(snapView);
                currentItem = adapter.getBannerPosition(currentPosition);
                if (currentPosition <= SCROLL_COUNT || currentPosition >= itemCount + SCROLL_LEFT) {
                    int toPosition = SCROLL_COUNT + currentItem;
                    scrollToPosition(toPosition);
                    currentPosition = toPosition;
                }
                if (mOnPageChangeListeners != null) {
                    for (OnPageChangeListener listener : mOnPageChangeListeners) {
                        listener.onPageSelected(currentItem);
                    }
                }
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setAutoRun(false);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setAutoRun(true);
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 设置自动滑动
     */
    public void setAutoRun(boolean isAuto) {
        handler.removeCallbacks(runnable);
        if (isAuto && adapter != null) {
            handler.postDelayed(runnable, DEFAULT_DELAY);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //Log.i("you", "banner onAttachedToWindow");
        hasAttachedToWindow = true;
        setAutoRun(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        //Log.i("you", "banner onDetachedFromWindow");
        hasAttachedToWindow = false;
        setAutoRun(false);
        scrollToPosition(currentPosition);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        setAutoRun(visibility == VISIBLE);
    }

    @Override
    public void setAdapter(Adapter adapter) {
    }

    public void setBannerAdapter(BannerAdapter adapter) {
        if (adapter != null) {
            this.adapter = adapter;
            this.proxyAdapter = new ProxyAdapter(adapter);
            super.setAdapter(this.proxyAdapter);
            int itemCount = adapter.getItemCount();
            if (itemCount > 1) {
                currentPosition = SCROLL_COUNT;
                scrollToPosition(SCROLL_COUNT);
            }
            setAutoRun(true);
        } else {
            setAutoRun(false);
            super.setAdapter(null);
            this.adapter = null;
            this.proxyAdapter = null;
        }
    }

    public BannerAdapter getBannerAdapter() {
        return adapter;
    }

    public int getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(int currentItem) {
        if (adapter == null || this.currentItem == currentItem || currentItem > adapter.getItemCount()) return;
        currentPosition = SCROLL_COUNT + currentItem;
        scrollToPosition(currentPosition);
    }

    public void notifyDataSetChanged() {
        if (proxyAdapter != null && adapter != null) {
            proxyAdapter.notifyDataSetChanged();
            int itemCount = adapter.getItemCount();
            if (itemCount > 1) {
                scrollToPosition(SCROLL_COUNT);
                currentPosition = SCROLL_COUNT;
            }
        }
    }

    /**
     * BannerViewHolder
     */
    public static class BannerViewHolder extends ViewHolder {
        /**
         * ViewHolder所有控件索引
         */
        private final SparseArray<View> views;

        int bannerPosition = NO_POSITION;

        public BannerViewHolder(View itemView) {
            super(itemView);
            this.views = new SparseArray<View>();
        }

        public int getBannerPosition() {
            return bannerPosition;
        }

        /**
         * 获取ViewHolder中的控件
         * @param viewId 控件id
         * @param <T> 转换的类型
         * @return
         */
        public <T extends View> T getView(int viewId) {
            View view = views.get(viewId);
            if (view == null) {
                view = itemView.findViewById(viewId);
                views.put(viewId, view);
            }
            return (T) view;
        }

    }


    public static abstract class BannerAdapter <VH extends BannerViewHolder> {

        public abstract int getItemCount();

        /**
         * 点击事件可以在此方法中实现
         * @param parent
         * @param viewType
         * @return
         */
        public abstract VH onCreateView(ViewGroup parent, int viewType);

        public abstract void onBindViewHolder(VH vh, int position);

        public int getItemViewType(int position) {
            return 0;
        }

        /**
         * 计算代理的adapter的实际位置
         */
        public final int getBannerPosition(int position) {
            int itemCount = getItemCount();
            if (itemCount < 2) return position;
            int subCount = SCROLL_COUNT % itemCount;
            if (subCount  == 0) {
                return position % itemCount;
            }
            int startPosition = itemCount - subCount;
            if (position < subCount) {
                return startPosition + position;
            }
            return (position - subCount) % itemCount;
        }

        public long getItemId(int position) {
            return NO_ID;
        }

        public void setHasStableIds(boolean hasStableIds) {
            //nothing
        }

        public void onViewRecycled(VH holder) {
            holder.bannerPosition = NO_POSITION;
        }

        public void onViewAttachedToWindow(VH holder) {
            //nothing
        }

        public void onViewDetachedFromWindow(VH holder) {
            //nothing
        }

        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            //nothing
        }

        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            //nothing
        }

    }

    static class ProxyAdapter extends Adapter <BannerViewHolder> {

        final BannerAdapter<BannerViewHolder> adapter;

        ProxyAdapter(BannerAdapter<BannerViewHolder> adapter) {
            this.adapter = adapter;
        }

        @Override
        public int getItemCount() {
            int adapterCount = adapter.getItemCount();
            return adapterCount < 2 ? adapterCount : adapterCount + BannerPager.SCROLL_MAX;
        }

        @Override
        public BannerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return adapter.onCreateView(parent, viewType);
        }

        @Override
        public void onBindViewHolder(BannerViewHolder holder, int position) {
            int bannerPosition = adapter.getBannerPosition(position);
            holder.bannerPosition = bannerPosition;
            adapter.onBindViewHolder(holder, bannerPosition);
        }

        @Override
        public void onBindViewHolder(BannerViewHolder holder, int position, List<Object> payloads) {
            super.onBindViewHolder(holder, position, payloads);
        }

        @Override
        public int getItemViewType(int position) {
            return adapter.getItemViewType(adapter.getBannerPosition(position));
        }

        @Override
        public void setHasStableIds(boolean hasStableIds) {
            adapter.setHasStableIds(hasStableIds);
        }

        @Override
        public long getItemId(int position) {
            return adapter.getItemId(position);
        }

        @Override
        public void onViewRecycled(BannerViewHolder holder) {
            adapter.onViewRecycled(holder);
        }

        @Override
        public void onViewAttachedToWindow(BannerViewHolder holder) {
            adapter.onViewAttachedToWindow(holder);
        }

        @Override
        public void onViewDetachedFromWindow(BannerViewHolder holder) {
            adapter.onViewDetachedFromWindow(holder);
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            adapter.onAttachedToRecyclerView(recyclerView);
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            adapter.onDetachedFromRecyclerView(recyclerView);
        }
    }

    private List<OnPageChangeListener> mOnPageChangeListeners;

    public void addOnPageChangeListener(OnPageChangeListener listener) {
        if (mOnPageChangeListeners == null) {
            mOnPageChangeListeners = new ArrayList<>();
        }
        mOnPageChangeListeners.add(listener);
    }

    /**
     * Remove a listener that was previously added via
     *
     * @param listener listener to remove
     */
    public void removeOnPageChangeListener(OnPageChangeListener listener) {
        if (mOnPageChangeListeners != null) {
            mOnPageChangeListeners.remove(listener);
        }
    }

    public interface OnPageChangeListener {

        /**
         * This method will be invoked when a new page becomes selected. Animation is not
         * necessarily complete.
         *
         * @param position Position index of the new selected page.
         */
        void onPageSelected(int position);
    }

    /**
     * 定时器
     */
    class PostRunnable implements Runnable {
        @Override
        public void run() {
            if (adapter != null && hasAttachedToWindow) {
                if (adapter.getItemCount() > 1) {
                    //Log.i("you", "current " + currentPosition);
                    smoothScrollToPosition(currentPosition + 1);
                }
                //还需要继续定时,如果适配器从0个刷新致多个
                handler.postDelayed(this, DEFAULT_DELAY);
            }
        }
    }

}
