package fr.missingfeature;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ImageView.ScaleType;

public class CoverFlowView extends LinearLayout {

	public static class Config {
		public int COVER_SPACING = 40;
		public int CENTER_COVER_OFFSET = 70;
		public float SIDE_COVER_ANGLE = 65;
		public int SIDE_COVER_ZPOSITION = 120;
		public float REFLECTION_FRACTION = 0.15f;
		public long FOCUS_ANIMATION_DURATION = 200;
		public int COVER_BUFFER = 6;
		public long BLUR_ANIMATION_DURATION = 120;
		public int DROP_SHADOW_RADIUS = 15;
		public boolean HORIZONTAL_SCROLLBAR_ENABLED = false;
		public boolean FADING_EDGES_ENABLED = true;
		public float IMAGE_SCALE_X = 1;
		public float IMAGE_SCALE_Y = 1;
	}

	Config mConfig = new Config();
	WeakReference<DataSource> mDataSource;
	WeakReference<Listener> mListener;
	Set<CoverFlowItem> mOffscreenCovers = new HashSet<CoverFlowItem>();
	Map<Integer, CoverFlowItem> mOnscreenCovers = new HashMap<Integer, CoverFlowItem>();
	Map<Integer, Bitmap> mCoverImages = new HashMap<Integer, Bitmap>();
	Map<Integer, Integer> mCoverImageHeights = new HashMap<Integer, Integer>();
	Bitmap mDefaultBitmap;
	int mDefaultBitmapHeight;
	float mDefaultImageHeight;
	ScrollView mScrollView;
	ViewGroup mItemContainer;

	int mLowerVisibleCover = -1;
	int mUpperVisibleCover = -1;
	int mNumberOfImages;
	int mBeginningCover;
	CoverFlowItem mSelectedCoverView = null;

	int mHalfScreenHeight;
	int mHalfScreenWidth;

	boolean mIsSingleTap;
	boolean mIsDraggingCover;
	float mStartScrollX;
	float mStartX;

	SortedSet<Integer> mTouchedCovers = new TreeSet<Integer>();

	public CoverFlowView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		setUpInitialState();
	}

	public CoverFlowView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setUpInitialState();
	}

	public CoverFlowView(Context context) {
		super(context);
		setUpInitialState();
	}

	public Config getConfig() {
		return mConfig;
	}

	void setUpInitialState() {

		// Create the scrollView
		mScrollView = new ScrollView(getContext()) {
			// Catch trackball events
			@Override
			public boolean onTrackballEvent(MotionEvent event) {
				return CoverFlowView.this.onTrackballEvent(event);
			}

		};
		mScrollView.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return CoverFlowView.this.onTouchEvent(event);
			}
		});
		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		mScrollView.setLayoutParams(params);
		mScrollView
				.setHorizontalScrollBarEnabled(mConfig.HORIZONTAL_SCROLLBAR_ENABLED);
		mScrollView
				.setHorizontalFadingEdgeEnabled(mConfig.FADING_EDGES_ENABLED);
		addView(mScrollView);

		// Create an intermediate LinearLayout
		LinearLayout linearLayout = new LinearLayout(getContext());
		mScrollView.addView(linearLayout);

		// Create the item container
		mItemContainer = new FrameLayout(getContext());
		linearLayout.addView(mItemContainer);

	}

	CoverFlowItem coverForIndex(int coverIndex) {
		CoverFlowItem coverItem = dequeueReusableCover();
		if (null == coverItem) {
			coverItem = new CoverFlowItem(getContext());
			coverItem.setScaleType(ImageView.ScaleType.FIT_XY);
			coverItem.setScaleX(mConfig.IMAGE_SCALE_X);
			coverItem.setScaleY(mConfig.IMAGE_SCALE_Y);
			coverItem.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					onTouchItem((CoverFlowItem) v, event);
					return false;
				}
			});
		}
		coverItem.setNumber(coverIndex);
		return coverItem;
	}

	void updateCoverBitmap(CoverFlowItem cover) {
		int coverNumber = cover.getNumber();
		Bitmap bitmap = mCoverImages.get(coverNumber);
		if (null != bitmap) {
			Integer coverImageHeight = mCoverImageHeights.get(coverNumber);
			if (null != coverImageHeight)
				cover.setImageBitmap(bitmap, coverImageHeight,
						mConfig.REFLECTION_FRACTION);
		} else {
			cover.setImageBitmap(mDefaultBitmap, mDefaultBitmapHeight,
					mConfig.REFLECTION_FRACTION);
			mDataSource.get().requestBitmapForIndex(this, coverNumber);
		}
	}

	void layoutCover(CoverFlowItem cover, int selectedCover, boolean animated) {
		if (null == cover)
			return;

		int coverNumber = cover.getNumber();
		int newX = mHalfScreenWidth + cover.getNumber() * mConfig.COVER_SPACING
				- (int) (cover.getCoverWidth() / 2.0f);
		int newY = mHalfScreenHeight - cover.getCoverHeight() / 2;

		ItemAnimation oldAnimation = (ItemAnimation) cover.getAnimation();
		float oldAngle = oldAnimation != null ? oldAnimation
				.getStopAngleDegrees() : 0;
		int oldZOffset = oldAnimation != null ? oldAnimation.getStopZOffset()
				: 0;
		int oldXOffset = oldAnimation != null ? oldAnimation.getStopXOffset()
				: 0;

		ItemAnimation anim = null;

		if (coverNumber < selectedCover) {
			if (oldAngle != mConfig.SIDE_COVER_ANGLE
					|| oldXOffset != -mConfig.CENTER_COVER_OFFSET
					|| oldZOffset != mConfig.SIDE_COVER_ZPOSITION) {
				anim = new ItemAnimation();
				anim.setRotation(oldAngle, mConfig.SIDE_COVER_ANGLE);
				anim.setViewDimensions(cover.getCoverWidth(), cover
						.getOriginalCoverHeight());
				anim.setXTranslation(oldXOffset, -mConfig.CENTER_COVER_OFFSET);
				anim.setZTranslation(oldZOffset, mConfig.SIDE_COVER_ZPOSITION);
				if (animated)
					anim.setDuration(mConfig.BLUR_ANIMATION_DURATION);
				else
					anim.setStatic();
			}
		} else if (coverNumber > selectedCover) {
			if (oldAngle != -mConfig.SIDE_COVER_ANGLE
					|| oldXOffset != mConfig.CENTER_COVER_OFFSET
					|| oldZOffset != mConfig.SIDE_COVER_ZPOSITION) {
				anim = new ItemAnimation();
				anim.setRotation(oldAngle, -mConfig.SIDE_COVER_ANGLE);
				anim.setViewDimensions(cover.getCoverWidth(), cover
						.getOriginalCoverHeight());
				anim.setXTranslation(oldXOffset, mConfig.CENTER_COVER_OFFSET);
				anim.setZTranslation(oldZOffset, mConfig.SIDE_COVER_ZPOSITION);
				if (animated)
					anim.setDuration(mConfig.BLUR_ANIMATION_DURATION);

				else
					anim.setStatic();
			}
		} else {
			if (oldAngle != 0 || oldXOffset != 0 || oldZOffset != 0) {
				anim = new ItemAnimation();
				anim.setRotation(oldAngle, 0);
				anim.setViewDimensions(cover.getCoverWidth(), cover
						.getOriginalCoverHeight());
				anim.setXTranslation(oldXOffset, 0);
				anim.setZTranslation(oldZOffset, 0);
				if (animated)
					anim.setDuration(mConfig.FOCUS_ANIMATION_DURATION);
				else
					anim.setStatic();
				anim.setAnimationListener(new Animation.AnimationListener() {
					public void onAnimationStart(Animation animation) {
					}

					public void onAnimationRepeat(Animation animation) {
					}

					public void onAnimationEnd(Animation animation) {
						mSelectedCoverView.bringToFront();
						layoutZ(mSelectedCoverView.getNumber(),
								mLowerVisibleCover, mUpperVisibleCover);
					}
				});
			}
		}

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(cover
				.getLayoutParams());
		params.setMargins(newX, newY, 0, 0);
		params.gravity = Gravity.LEFT | Gravity.TOP;
		cover.setLayoutParams(params);

		if (null != anim)
			cover.startAnimation(anim);
	}

	void layoutCovers(int selected, int lowerBound, int upperBound) {
		CoverFlowItem cover;

		for (int i = lowerBound; i <= upperBound; i++) {
			cover = mOnscreenCovers.get(i);
			layoutCover(cover, selected, true);
		}
	}

	void layoutZ(int selected, int lowerBound, int upperBound) {
		CoverFlowItem cover;
		for (int i = upperBound; i > selected; i--) {
			cover = mOnscreenCovers.get(i);
			if (null != cover)
				mItemContainer.bringChildToFront(cover);
		}
		for (int i = lowerBound; i <= selected; i++) {
			cover = mOnscreenCovers.get(i);
			if (null != cover)
				mItemContainer.bringChildToFront(cover);
		}

	}

	CoverFlowItem dequeueReusableCover() {
		CoverFlowItem item = null;
		if (!mOffscreenCovers.isEmpty()) {
			item = mOffscreenCovers.iterator().next();
			mOffscreenCovers.remove(item);
		}
		return item;
	}

	public void setBitmapForIndex(Bitmap bitmap, int index) {
		Bitmap bitmapWithReflection = CoverFlowItem
				.createReflectedBitmap(bitmap, mConfig.REFLECTION_FRACTION,
						mConfig.DROP_SHADOW_RADIUS);
		setReflectedBitmapForIndex(bitmapWithReflection, index);
	}
	
	public void setReflectedBitmapForIndex(Bitmap bitmapWithReflection, int index) {
		mCoverImages.put(index, bitmapWithReflection);
		int originalHeight = (int)((int)(bitmapWithReflection.getHeight() - 2 * mConfig.DROP_SHADOW_RADIUS) / (1 + mConfig.REFLECTION_FRACTION));
		mCoverImageHeights.put(index, originalHeight);

		// If this cover is onscreen, set its image and call layoutCover.
		CoverFlowItem cover = mOnscreenCovers.get(index);
		if (null != cover) {
			cover.setImageBitmap(bitmapWithReflection, originalHeight,
					mConfig.REFLECTION_FRACTION);
			layoutCover(cover, mSelectedCoverView.getNumber(), false);
		}
	}
	
	public Bitmap[] getReflectedBitmaps() {
		Bitmap[] result = new Bitmap[mCoverImages.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = mCoverImages.get(i);
		return result;
	}

	//
	// @Override
	// protected void onLayout(boolean changed, int l, int t, int r, int b) {
	// mScrollView.layout(l, t, r, b);
	// }
	//
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(
				mNumberOfImages * mConfig.COVER_SPACING
						+ MeasureSpec.getSize(widthMeasureSpec),
				LayoutParams.FILL_PARENT);
		mItemContainer.setLayoutParams(params);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mHalfScreenWidth = w / 2;
		mHalfScreenHeight = h / 2;

		int lowerBound = Math.max(-1,
				(mSelectedCoverView != null ? mSelectedCoverView.getNumber()
						: 0)
						- mConfig.COVER_BUFFER);
		int upperBound = Math.min(mNumberOfImages - 1,
				(mSelectedCoverView != null ? mSelectedCoverView.getNumber()
						: 0)
						+ mConfig.COVER_BUFFER);
		layoutCovers(mSelectedCoverView != null ? mSelectedCoverView
				.getNumber() : 0, lowerBound, upperBound);

		centerOnSelectedCover(false);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mIsSingleTap = event.getPointerCount() == 1;
			if (mIsSingleTap)
				mStartX = event.getX(0);
			mBeginningCover = mSelectedCoverView.getNumber();
			mStartScrollX = event.getX(0) + mScrollView.getScrollX();
			break;
		case MotionEvent.ACTION_MOVE:
			int scrollOffset = (int) (mStartScrollX - event.getX(0));
			int xOffset = (int) Math.abs(event.getX(0) - mStartX);

			// If finger moves too much, not a single tap anymore:
			mIsSingleTap = mIsSingleTap && (xOffset < 20);

			if (!mIsSingleTap) {
				// Update the scroll position
				mScrollView.scrollTo(scrollOffset, mScrollView.getScrollY());

				// Select new cover
				int newCover = scrollOffset / mConfig.COVER_SPACING;

				// make sure we're not out of bounds:
				if (newCover < 0)
					newCover = 0;
				else if (newCover >= mNumberOfImages)
					newCover = mNumberOfImages - 1;

				// Select newCover if appropriate
				if (newCover != mSelectedCoverView.getNumber()) {
					setSelectedCover(newCover);
					// Notify listener
					if (null != mListener && null != mListener.get())
						mListener.get().onSelectionChanging(this,
								mSelectedCoverView.getNumber());
				}
			}

			break;
		case MotionEvent.ACTION_UP:
			if (mIsSingleTap && 0 < mTouchedCovers.size()) {
				int lowest = mTouchedCovers.first();
				int highest = mTouchedCovers.last();
				if (mSelectedCoverView.getNumber() < lowest)
					setSelectedCover(lowest);
				else if (mSelectedCoverView.getNumber() > highest)
					setSelectedCover(highest);
				else if (lowest <= mSelectedCoverView.getNumber()
						&& highest >= mSelectedCoverView.getNumber()
						&& null != mListener && null != mListener.get())
					mListener.get().onSelectionClicked(this,
							mSelectedCoverView.getNumber());

			}
			// Smooth scroll to the center of the cover
			mScrollView.smoothScrollTo(mSelectedCoverView.getNumber()
					* mConfig.COVER_SPACING, mScrollView.getScrollY());

			if (mBeginningCover != mSelectedCoverView.getNumber()) {
				// Notify listener
				if (null != mListener && null != mListener.get())
					mListener.get().onSelectionChanged(this,
							mSelectedCoverView.getNumber());
			}

			// Clear touched covers
			mTouchedCovers.clear();

			break;
		}

		return true;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_MOVE:
			int newCover = -1;
			if (event.getX(0) > 0)
				newCover = mSelectedCoverView.getNumber() + 1;
			else if (event.getX(0) < 0)
				newCover = mSelectedCoverView.getNumber() - 1;

			if (0 <= newCover && mNumberOfImages > newCover
					&& mSelectedCoverView.getNumber() != newCover) {
				setSelectedCover(newCover);
				mScrollView.smoothScrollTo(newCover * mConfig.COVER_SPACING,
						mScrollView.getScrollY());
				// Notify listener
				if (null != mListener && null != mListener.get())
					mListener.get().onSelectionChanged(this,
							mSelectedCoverView.getNumber());

			}
			break;
		}
		return true;
	}

	void onTouchItem(CoverFlowItem cover, MotionEvent event) {
		mTouchedCovers.add(cover.getNumber());
	}

	public void clear() {
		mNumberOfImages = 0;
		mSelectedCoverView = null;
		mOffscreenCovers.clear();
		mOnscreenCovers.clear();
		mCoverImages.clear();
		mCoverImageHeights.clear();
		mDefaultBitmap = null;
		mLowerVisibleCover = -1;
		mUpperVisibleCover = -1;
		
		// Recreate the item container to force free memory
		LinearLayout parent = (LinearLayout)mItemContainer.getParent();
		parent.removeView(mItemContainer);
		mItemContainer = new FrameLayout(getContext());
		parent.addView(mItemContainer);


	}

	public void setNumberOfImages(int numberOfImages) {
		mNumberOfImages = numberOfImages;

		int lowerBound = Math.max(-1,
				(mSelectedCoverView != null ? mSelectedCoverView.getNumber()
						: 0)
						- mConfig.COVER_BUFFER);
		int upperBound = Math.min(mNumberOfImages - 1,
				(mSelectedCoverView != null ? mSelectedCoverView.getNumber()
						: 0)
						+ mConfig.COVER_BUFFER);
		if (null != mSelectedCoverView)
			layoutCovers(mSelectedCoverView.getNumber(), lowerBound, upperBound);
		else
			setSelectedCover(0);

		centerOnSelectedCover(false);
	}

	public void setDefaultBitmap(Bitmap bitmap) {
		mDefaultBitmapHeight = null != bitmap ? bitmap.getHeight() : 0;
		mDefaultBitmap = bitmap;
	}

	public void setDataSource(DataSource dataSource) {
		mDataSource = new WeakReference<DataSource>(dataSource);
		setDefaultBitmap(dataSource.defaultBitmap());
	}

	public void setListener(Listener listener) {
		mListener = new WeakReference<Listener>(listener);
	}

	public void centerOnSelectedCover(final boolean animated) {
		if (null == mSelectedCoverView)
			return;
		
		final int offset = mConfig.COVER_SPACING
				* mSelectedCoverView.getNumber();
		mScrollView.post(new Runnable() {
			public void run() {
				if (animated)
					mScrollView.smoothScrollTo(offset, 0);
				else
					mScrollView.scrollTo(offset, 0);
			}
		});
	}

	public void setSelectedCover(int newSelectedCover) {
		if (null != mSelectedCoverView
				&& newSelectedCover == mSelectedCoverView.getNumber())
			return;

		if (newSelectedCover >= mNumberOfImages)
			return;

		CoverFlowItem cover;
		int newLowerBound = Math
				.max(0, newSelectedCover - mConfig.COVER_BUFFER);
		int newUpperBound = Math.min(mNumberOfImages - 1, newSelectedCover
				+ mConfig.COVER_BUFFER);
		if (null == mSelectedCoverView) {
			// Allocate and display covers from newLower to newUpper bounds.
			for (int i = newLowerBound; i <= newUpperBound; i++) {
				cover = coverForIndex(i);
				mOnscreenCovers.put(i, cover);
				updateCoverBitmap(cover);
				if (i == newSelectedCover) {
					// We'll add it later
					continue;
				} else if (i < newSelectedCover) {
					mItemContainer.addView(cover);
				} else {
					mItemContainer.addView(cover, 0);
				}
				layoutCover(cover, newSelectedCover, false);
			}
			// Add the selected cover
			cover = mOnscreenCovers.get(newSelectedCover);
			mItemContainer.addView(cover);
			layoutCover(cover, newSelectedCover, false);

			mLowerVisibleCover = newLowerBound;
			mUpperVisibleCover = newUpperBound;
			mSelectedCoverView = cover;
			return;
		} else {
			layoutZ(mSelectedCoverView.getNumber(), mLowerVisibleCover,
					mUpperVisibleCover);

		}

		if ((newLowerBound > mUpperVisibleCover)
				|| (newUpperBound < mLowerVisibleCover)) {
			// They do not overlap at all.
			// This does not animate--assuming it's programmatically set from
			// view controller.
			// Recycle all onscreen covers.
			for (int i = mLowerVisibleCover; i <= mUpperVisibleCover; i++) {
				cover = mOnscreenCovers.get(i);
				mOffscreenCovers.add(cover);
				mItemContainer.removeView(cover);
				mOnscreenCovers.remove(i);
			}

			// Move all available covers to new location.
			for (int i = newLowerBound; i <= newUpperBound; i++) {
				cover = coverForIndex(i);
				mOnscreenCovers.put(i, cover);
				updateCoverBitmap(cover);
				if (i == newSelectedCover) {
					// We'll add it later
					continue;
				} else if (i < newSelectedCover) {
					mItemContainer.addView(cover);
				} else {
					mItemContainer.addView(cover, 0);
				}
			}
			cover = mOnscreenCovers.get(newSelectedCover);
			mItemContainer.addView(cover);

			mLowerVisibleCover = newLowerBound;
			mUpperVisibleCover = newUpperBound;
			mSelectedCoverView = cover;
			layoutCovers(newSelectedCover, newLowerBound, newUpperBound);

			return;

		} else if (newSelectedCover > mSelectedCoverView.getNumber()) {
			// Move covers that are now out of range on the left to the right
			// side,
			// but only if appropriate (within the range set by newUpperBound).
			for (int i = mLowerVisibleCover; i < newLowerBound; i++) {
				cover = mOnscreenCovers.get(i);
				if (mUpperVisibleCover < newUpperBound) {
					// Tack it on the right side.
					mUpperVisibleCover++;
					cover.setNumber(mUpperVisibleCover);
					updateCoverBitmap(cover);
					mOnscreenCovers.put(cover.getNumber(), cover);
					layoutCover(cover, newSelectedCover, false);
				} else {
					// Recycle this cover.
					mOffscreenCovers.add(cover);
					mItemContainer.removeView(cover);
				}
				mOnscreenCovers.remove(i);
			}

			mLowerVisibleCover = newLowerBound;

			// Add in any missing covers on the right up to the newUpperBound.
			for (int i = mUpperVisibleCover + 1; i <= newUpperBound; i++) {
				cover = coverForIndex(i);
				mOnscreenCovers.put(i, cover);
				updateCoverBitmap(cover);
				mItemContainer.addView(cover, 0);
				layoutCover(cover, newSelectedCover, false);
			}
			mUpperVisibleCover = newUpperBound;
		} else {
			// Move covers that are now out of range on the right to the left
			// side,
			// but only if appropriate (within the range set by newLowerBound).
			for (int i = mUpperVisibleCover; i > newUpperBound; i--) {
				cover = mOnscreenCovers.get(i);
				if (mLowerVisibleCover > newLowerBound) {
					// Tack it on the left side.
					mLowerVisibleCover--;
					cover.setNumber(mLowerVisibleCover);
					updateCoverBitmap(cover);
					mOnscreenCovers.put(cover.getNumber(), cover);
					layoutCover(cover, newSelectedCover, false);

				} else {
					// Recycle this cover.
					mOffscreenCovers.add(cover);
					mItemContainer.removeView(cover);
				}
				mOnscreenCovers.remove(i);
			}

			mUpperVisibleCover = newUpperBound;

			// Add in any missing covers on the left down to the newLowerBound.
			for (int i = mLowerVisibleCover - 1; i >= newLowerBound; i--) {
				cover = coverForIndex(i);
				mOnscreenCovers.put(i, cover);
				updateCoverBitmap(cover);
				mItemContainer.addView(cover, 0);
				layoutCover(cover, newSelectedCover, false);
			}

			mLowerVisibleCover = newLowerBound;
		}

		if (mSelectedCoverView.getNumber() > newSelectedCover) {
			layoutCovers(newSelectedCover, newSelectedCover, mSelectedCoverView
					.getNumber());
		} else if (newSelectedCover > mSelectedCoverView.getNumber()) {
			layoutCovers(newSelectedCover, mSelectedCoverView.getNumber(),
					newSelectedCover);
		}

		mSelectedCoverView = mOnscreenCovers.get(newSelectedCover);

	}

	private static class ScrollView extends HorizontalScrollView {

		public ScrollView(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
		}

		public ScrollView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public ScrollView(Context context) {
			super(context);
		}

	}

	public interface DataSource {
		public void requestBitmapForIndex(CoverFlowView coverFlow, int index);

		public Bitmap defaultBitmap();
	}

	public interface Listener {
		public void onSelectionChanging(CoverFlowView coverFlow, int index);

		public void onSelectionChanged(CoverFlowView coverFlow, int index);

		public void onSelectionClicked(CoverFlowView coverFlow, int index);
	}

	public static class ItemAnimation extends Animation {
		private int mViewWidth;
		private int mViewHeight;
		private int mStartZOffset;
		private int mStopZOffset;
		private int mStartXOffset;
		private int mStopXOffset;
		private float mStopAngleDegrees = 0;
		// private double mStopAngleRadians = 0;
		private float mStartAngleDegrees = 0;
		private boolean mStatic = false;

		// private double mStartAngleRadians = 0;

		public ItemAnimation() {
			super();
			setFillAfter(true);
			setFillBefore(true);
		}

		public void setStatic() {
			mStatic = true;
			setDuration(0);
		}

		public void setRotation(float start, float stop) {
			mStartAngleDegrees = start;
			mStopAngleDegrees = stop;
		}

		public void setXTranslation(int start, int stop) {
			mStartXOffset = start;
			mStopXOffset = stop;
		}

		public void setZTranslation(int start, int stop) {
			mStartZOffset = start;
			mStopZOffset = stop;
		}

		public void setViewDimensions(int width, int height) {
			mViewWidth = width;
			mViewHeight = height;
		}

		public float getStopAngleDegrees() {
			return mStopAngleDegrees;
		}

		public float getStartAngleDegrees() {
			return mStartAngleDegrees;
		}

		public int getStartXOffset() {
			return mStartXOffset;
		}

		public int getStopXOffset() {
			return mStopXOffset;
		}

		public int getStopZOffset() {
			return mStopZOffset;
		}

		@Override
		protected void applyTransformation(float interpolatedTime,
				Transformation t) {
			t.setTransformationType(mStatic ? Transformation.TYPE_BOTH
					: Transformation.TYPE_MATRIX);

			if (mStatic)
				t.setAlpha(interpolatedTime < 1.0f ? 0 : 1);

			float angleDegrees = mStartAngleDegrees + interpolatedTime
					* (mStopAngleDegrees - mStartAngleDegrees);
			float zOffset = mStartZOffset + interpolatedTime
					* (mStopZOffset - mStartZOffset);
			int xOffset = mStartXOffset
					+ (int) (interpolatedTime * (mStopXOffset - mStartXOffset));
			Matrix m = new Matrix();
			Camera camera = new Camera();
			camera.translate(0, 0, zOffset);

			camera.rotateY(angleDegrees);

			camera.getMatrix(m);
			m.preTranslate(-(mViewWidth / 2), -(mViewHeight / 2));
			m.postTranslate((mViewWidth / 2) + xOffset, (mViewHeight / 2));

			t.getMatrix().set(m);
			super.applyTransformation(interpolatedTime, t);
		}

	}
}
