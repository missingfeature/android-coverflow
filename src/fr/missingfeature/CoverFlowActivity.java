package fr.missingfeature;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

public class CoverFlowActivity extends Activity implements
		CoverFlowView.Listener {
	private static final String TAG = "CoverFlowActivity";
	private CoverFlowView mCoverflow;
	private Bitmap[] mReflectedBitmaps;
	private boolean mCoverflowCleared = false;

	public static final int NUMBER_OF_IMAGES = 30;

	/**
	 * Get an array of Bitmaps for our sample images
	 * 
	 * @param c
	 * @return
	 * @throws IOException
	 */
	public static Bitmap[] getBitmaps(Context c) throws IOException {
		Bitmap[] result = new Bitmap[NUMBER_OF_IMAGES];
		for (int i = 0; i < NUMBER_OF_IMAGES; i++) {
			Bitmap b = BitmapFactory.decodeStream(c.getAssets().open(
					String.format("images/%d.jpg", i)));
			result[i] = b;
		}
		return result;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.coverflow);

		// Find the coverflow
		mCoverflow = (CoverFlowView) findViewById(R.id.coverflow);

		// Get the bitmaps
		Bitmap[] bitmaps = null;
		try {
			bitmaps = getBitmaps(this);
		} catch (IOException e) {
			Log.e(TAG, "Could not load bitmaps", e);
		}

		// Listen to the coverflow
		mCoverflow.setListener(this);

		// Fill in images
		for (int i = 0; bitmaps != null && i < bitmaps.length; i++) {
			mCoverflow.setBitmapForIndex(bitmaps[i], i);
		}
		mCoverflow.setNumberOfImages(bitmaps.length);

		// Cache the reflected bitmaps
		mReflectedBitmaps = mCoverflow.getReflectedBitmaps();
	}

	@Override
	protected void onResume() {

		// If we cleared the coverflow in onPause, resurrect it
		if (mCoverflowCleared) {
			for (int i = 0; i < mReflectedBitmaps.length; i++)
				mCoverflow.setReflectedBitmapForIndex(mReflectedBitmaps[i], i);
			mCoverflow.setNumberOfImages(mReflectedBitmaps.length);
		}
		mCoverflowCleared = false;
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Clear the coverflow to save memory
		mCoverflow.clear();
		mCoverflowCleared = true;
	}

	public Bitmap defaultBitmap() {
		try {
			return BitmapFactory.decodeStream(getAssets().open(
					"images/default.png"));
		} catch (IOException e) {
			Log.e(TAG, "Unable to get default image", e);
		}
		return null;
	}

	public void onSelectionChanged(CoverFlowView coverFlow, int index) {
		Log.d(TAG, String.format("Selection did change: %d", index));
	}

	public void onSelectionChanging(CoverFlowView coverFlow, int index) {
		Log.d(TAG, String.format("Selection is changing: %d", index));
	}

	public void onSelectionClicked(CoverFlowView coverFlow, int index) {
		Log.d(TAG, String.format("Selection clicked: %d", index));
	}
}
