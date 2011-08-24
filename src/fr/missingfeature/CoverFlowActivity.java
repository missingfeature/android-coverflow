package fr.missingfeature;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class CoverFlowActivity extends Activity implements
		CoverFlowView.DataSource, CoverFlowView.Listener {
	private static final String TAG = "CoverFlowActivity";
	private Bitmap[] mBitmaps;
	private CoverFlowView mCoverflow;
	int mIndex = 0;

	public static final int NUMBER_OF_IMAGES = 30;

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

		mCoverflow = (CoverFlowView) findViewById(R.id.coverflow);
		try {
			mBitmaps = getBitmaps(this);
		} catch (IOException e) {
			Log.e(TAG, "Could not load bitmaps", e);
		}
		for (int i = 0; mBitmaps != null && i < 10/* mBitmaps.length */; i++) {
			mCoverflow.setBitmapForIndex(mBitmaps[i], i);
		}
		mCoverflow.setDataSource(this);
		mCoverflow.setListener(this);
		mCoverflow.setNumberOfImages(10/* mBitmaps.length */);
	}

	@Override
	protected void onResume() {
		super.onResume();
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

	public void requestBitmapForIndex(CoverFlowView coverFlow, int index) {
		coverFlow.setBitmapForIndex(mBitmaps[index], index);

	}

	public void onSelectionChanged(CoverFlowView coverFlow, int index) {
		Log.d(TAG, String.format("Selection did change: %d", index));
	}

	public void onClickNext(View v) {
		mCoverflow.setSelectedCover(++mIndex);
	}

	public void onClickPrevious(View v) {
		if (mIndex > 0)
			mCoverflow.setSelectedCover(--mIndex);
	}

}
