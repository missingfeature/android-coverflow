package fr.missingfeature;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CoverFlowItem extends ImageView {

	int mNumber;
	int mOriginalImageHeight;
	int mBitmapWidth = 0;
	int mBitmapHeight = 0;

	public CoverFlowItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public CoverFlowItem(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CoverFlowItem(Context context) {
		super(context);
	}

	public void setNumber(int n) {
		mNumber = n;
	}

	public int getBitmapWidth() {
		return mBitmapWidth;
	}
	
	public int getBitmapHeight() {
		return mBitmapHeight;
	}
	
	public int getNumber() {
		return mNumber;
	}

	public int getOriginalImageHeight() {
		return mOriginalImageHeight;
	}
	
	public void setImageBitmap(Bitmap bitmap, int originalImageHeight,
			float reflectionFraction) {
		mOriginalImageHeight = originalImageHeight;
		mBitmapWidth = bitmap.getWidth();
		mBitmapHeight = bitmap.getHeight();
		setImageBitmap(bitmap);
	}

	public Size calculateNewSize(Size originalImageSize, Size boundingBox) {
		float boundingRatio = (float) boundingBox.getWidth()
				/ boundingBox.getHeight();
		float originalImageRatio = (float) originalImageSize.getWidth()
				/ originalImageSize.getHeight();

		int newWidth, newHeight;

		if (originalImageRatio > boundingRatio) {
			newWidth = boundingBox.getWidth();
			newHeight = (int) ((float) boundingBox.getWidth()
					* originalImageSize.getHeight() / originalImageSize
					.getWidth());
		} else {
			newHeight = boundingBox.getHeight();
			newWidth = (int) ((float) boundingBox.getHeight()
					* originalImageSize.getWidth() / originalImageSize
					.getHeight());
		}
		return new Size(newWidth, newHeight);
	}

	public static class Size {
		int mWidth, mHeight;

		public Size() {
		}

		public Size(int w, int h) {
			mWidth = w;
			mHeight = h;
		}

		public void setWidth(int w) {
			mWidth = w;
		}

		public void setHeight(int h) {
			mHeight = h;
		}

		public int getWidth() {
			return mWidth;
		}

		public int getHeight() {
			return mHeight;
		}
	}

	static Bitmap createReflectedBitmap(Bitmap b, float reflectionFraction, int dropShadowRadius) {
		if (0 == reflectionFraction && 0 == dropShadowRadius)
			return b;
		
		Bitmap reflection;
		Bitmap result;
		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);

		int padding = dropShadowRadius;

		// Create the reflection
		reflection = Bitmap.createBitmap(b, 0, (int)(b.getHeight() * (1 - reflectionFraction)), b
				.getWidth(), (int)(b.getHeight() * reflectionFraction), matrix, false);

		// Create the result bitmap, in which we'll print the
		// original bitmap and its reflection
		result = Bitmap.createBitmap(b.getWidth() + padding * 2, 2 * padding
				+ (int)(b.getHeight() * (1 + reflectionFraction)), Config.ARGB_8888);

		// We'll work in a canvas
		Canvas canvas = new Canvas(result);

		// Add a drop shadow
		Paint dropShadow = new Paint();
		dropShadow.setShadowLayer(padding, 0, 0, 0xFF000000);
		canvas.drawRect(padding, padding, b.getWidth() + padding, result
				.getHeight()
				- padding, dropShadow);

		// draw the original image
		canvas.drawBitmap(b, padding, padding, null);

		// draw the reflection
		canvas.drawBitmap(reflection, padding, padding + b.getHeight(), null);

		// recycle reflection
		reflection.recycle();
		reflection = null;

		// draw the gradient
		LinearGradient shader = new LinearGradient(0, b.getHeight(), 0, result
				.getHeight(), 0x40000000, 0xff000000, TileMode.CLAMP);
		Paint paint = new Paint();
		paint.setShader(shader);
		paint.setXfermode(new PorterDuffXfermode(Mode.DARKEN));
		canvas.drawRect(padding, padding + b.getHeight(), padding
				+ b.getWidth(), padding + b.getHeight() * (1 + reflectionFraction), paint);
		return result;
	}
}
