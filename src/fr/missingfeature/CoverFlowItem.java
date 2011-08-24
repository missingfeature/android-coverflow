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
import android.view.ViewGroup;
import android.widget.ImageView;

public class CoverFlowItem extends ImageView {

	int mNumber;
	int mHorizontalPosition = 0;
	int mVerticalPosition = 0;
	int mOriginalImageHeight;
	int mBitmapWidth = 0;

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
		mHorizontalPosition = n * CoverFlowConstants.COVER_SPACING;
	}

	public int getNumber() {
		return mNumber;
	}

	public int getOriginalImageHeight() {
		return mOriginalImageHeight;
	}
	
	public void setHorizontalPosition(int hp) {
		mHorizontalPosition = hp;
	}

	public int getHorizontalPosition() {
		return mHorizontalPosition - (int)(mBitmapWidth / 2.0f);
	}

	public void setVerticalPosition(int vp) {
		mVerticalPosition = vp;
	}

	public int getVerticalPosition() {
		return mVerticalPosition;
	}

	public void setImageBitmap(Bitmap bitmap, int originalImageHeight,
			float reflectionFraction) {
		mVerticalPosition = (int) (originalImageHeight * reflectionFraction / 2);
		mOriginalImageHeight = originalImageHeight;
		mBitmapWidth = bitmap.getWidth();
		setLayoutParams(new ViewGroup.LayoutParams(mBitmapWidth, bitmap
				.getHeight()));
		// CHECK: this calls invalidate() and requestLayout(), should be enough
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

	static Bitmap createReflectedBitmap(Bitmap b, float reflectionFraction) {
		Bitmap reflection;
		Bitmap result;
		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);

		int padding = 15;

		// reflection = Bitmap.createBitmap(source, x, y, width, height, m,
		// filter);
		reflection = Bitmap.createBitmap(b, 0, (int)(b.getHeight() * (1 - reflectionFraction)), b
				.getWidth(), (int)(b.getHeight() * reflectionFraction), matrix, false);

		result = Bitmap.createBitmap(b.getWidth() + padding * 2, 2 * padding
				+ (int)(b.getHeight() * (1 + reflectionFraction)), Config.ARGB_8888);

		Canvas canvas = new Canvas(result);

		Paint dropShadow = new Paint();
		dropShadow.setShadowLayer(5.0f, 0, 0, 0xFF000000);
		canvas.drawRect(padding, padding, b.getWidth() + padding, result
				.getHeight()
				- padding, dropShadow);

		// draw the original image
		canvas.drawBitmap(b, padding, padding, null);

		// draw the reflection
		canvas.drawBitmap(reflection, padding, padding + b.getHeight(), null);

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
