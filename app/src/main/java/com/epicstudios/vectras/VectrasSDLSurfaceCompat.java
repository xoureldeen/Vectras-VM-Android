package com.epicstudios.vectras;

public class VectrasSDLSurfaceCompat //extends SDLSurface
//implements View.OnGenericMotionListener
{
//	public VectrasSDLSurfaceCompat(Context context) {
//		super(context);
////		this.setOnGenericMotionListener(this);
//		// TODO Auto-generated constructor stub
//	}

//	@Override
//	public boolean onGenericMotion(View v, MotionEvent event) {
//		
//
//		if (VectrasSDLActivity.enablebluetoothmouse == 0) {
//			return false;
//		}
//		float x = event.getX();
//		float y = event.getY();
//		float p = event.getPressure();
//		int action = event.getAction();
//
//		if (x < (VectrasSDLActivity.width - VectrasSDLActivity.vm_width) / 2) {
//			return true;
//		} else if (x > VectrasSDLActivity.width
//				- (VectrasSDLActivity.width - VectrasSDLActivity.vm_width) / 2) {
//			return true;
//		}
//
//		if (y < (VectrasSDLActivity.height - VectrasSDLActivity.vm_height) / 2) {
//			return true;
//		} else if (y > VectrasSDLActivity.height
//				- (VectrasSDLActivity.height - VectrasSDLActivity.vm_height) / 2) {
//			return true;
//		}
//
//		if (action == MotionEvent.ACTION_HOVER_MOVE) {
////			Log.v("onGenericMotion", "Moving to (X,Y)=(" + x
////					* VectrasSDLActivity.width_mult + "," + y
////					* VectrasSDLActivity.height_mult + ")");
//
//			VectrasSDLActivity.onNativeTouch(0, 1, MotionEvent.ACTION_MOVE, x
//					* VectrasSDLActivity.width_mult, y * VectrasSDLActivity.height_mult, p);
//		}
//
//		if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
////			Log.v("onGenericMotion", "Right Click (X,Y)=" + x
////					* VectrasSDLActivity.width_mult + "," + y
////					* VectrasSDLActivity.height_mult + ")");
//			rightClick(event);
//		}
//
//		// save current
//		old_x = x * VectrasSDLActivity.width_mult;
//		old_y = y * VectrasSDLActivity.height_mult;
//		return true;
//	}
//	

}
