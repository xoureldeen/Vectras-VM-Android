package android.androidVNC;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.vectras.vm.R;

public class VncCursorView extends View {

    private Bitmap cursor;
    private float x, y;

    public VncCursorView(Context context) {
        super(context);
        init(context);
    }

    public VncCursorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VncCursorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        cursor = BitmapFactory.decodeResource(
                context.getResources(),
                R.drawable.xc_left_ptr
        );
    }

    public void update(float x, float y) {
        this.x = x;
        this.y = y;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (cursor != null) {
            canvas.drawBitmap(cursor, x, y, null);
        }
    }
}
