package uk.co.halfninja.android;

import uk.co.halfninja.android.math.Quaternion;
import uk.co.halfninja.android.math.Vector3;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
/*
   Copyright 2010 Nick howes

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

/**
 * Takes touch input and converts dragging into a rotation
 * value, which lets you rotate an object naturally with
 * your finger. It assumes a camera looking down Z, so drag
 * events are in the X-Y plane.
 * 
 * USAGE:
 * In the setup of your Activity:
 * 
 * dragControl = new DragControl();
 * glView.setOnTouchListener(dragControl);
 * 
 * When rendering:
 * Quaternion rotation = dragControl.currentRotation();
 *
 * Wherever possible this class reuses objects to avoid
 * frequent object creation.
 */
public final class DragControl implements OnTouchListener {

	private static final int DRAG_START = 0;
	private static final int DRAG_END   = 1;

	private static final double FLING_REDUCTION = 5000;
	private static final double FLING_DAMPING = 0.95;
	private static final double DRAG_SLOWING = 90;
	
	private float[] dragX = new float[2];
	private float[] dragY = new float[2];
	
	private boolean dragging;
	
	// Base rotation, before drag
	private Quaternion rotation = new Quaternion(new Vector3(0,1,0), 0);
	
	// The amount of rotation to add as part of drag
	private Quaternion dragRotation = new Quaternion(new Vector3(0,1,0), 0);
	
	// Equal to rotation*dragRotation.
	private Quaternion intermediateRotation = new Quaternion(new Vector3(0,1,0), 0);
	
	// The current axis about which the object is being rotated
	private Vector3 spinAxis = new Vector3(0,0,0);
	
	// Flinging
	// When you flick the screen with your finger it will keep spinning.
	
	// How fast it is spinning on its own
	private double flingSpeed = 0;
	// The axis about which we are being flung, if any
	private Vector3 flingAxis = new Vector3(0,0,0);
	// Fling rotation we most recent added to rotation.
	// Only here to save creating new objects too often.
	private Quaternion flingRotation = new Quaternion(new Vector3(0,1,0), 0);

	private GestureDetector gestureDetector;

	public DragControl() {
		gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener(){
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				flingAxis.set(-velocityY, -velocityX, 0);
				flingSpeed = flingAxis.magnitude()/FLING_REDUCTION;
				flingAxis.normalise();
				return true;
			}
		});
	}
	
	public boolean onTouch(View view, MotionEvent event) {
		
		gestureDetector.onTouchEvent(event);
		
		int action = event.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			dragX[DRAG_START] = dragX[DRAG_END] = event.getX();
			dragY[DRAG_START] = dragY[DRAG_END] = event.getY();			
			dragging = true;		
			flingSpeed = 0;
			return true;
		case MotionEvent.ACTION_MOVE:
			dragX[DRAG_END] = event.getX();
			dragY[DRAG_END] = event.getY();				
			return true;
		case MotionEvent.ACTION_UP:			
			dragging = false;
			
			float rotateX = dragX[DRAG_END] - dragX[DRAG_START];		        
	        float rotateY = dragY[DRAG_END] - dragY[DRAG_START];
	        
	        if (rotateX != 0 || rotateY != 0) {
		        spinAxis = new Vector3(-rotateY, -rotateX, 0);
				double mag = spinAxis.magnitude();
		        spinAxis.normalise();
	
		        intermediateRotation.set(spinAxis, mag/90);
		        rotation.mulThis(intermediateRotation);
	        }
			
			dragX[DRAG_END] = dragX[DRAG_START] = 0;
			dragY[DRAG_END] = dragY[DRAG_START] = 0;
	       
		}
		
		return false;
	}
	
	/**
	 * FIXME do the actual updating in a separate method that
	 * is time-dependant.
	 */
	public Quaternion currentRotation() {
		float rotateX = dragX[DRAG_END] - dragX[DRAG_START];		        
        float rotateY = dragY[DRAG_END] - dragY[DRAG_START];
        
        if (dragging && (rotateX != 0 || rotateY != 0)) {
	        spinAxis.set(-rotateY, -rotateX, 0);
			double mag = spinAxis.magnitude();
			spinAxis.normalise();

	        intermediateRotation.set(spinAxis, mag/DRAG_SLOWING);
	        dragRotation.set(rotation);
	        dragRotation.mulThis(intermediateRotation);

	        return dragRotation;
        } else {
        	if (flingSpeed > 0) {
	        	flingSpeed *= FLING_DAMPING;
	        	flingRotation.set(flingAxis, flingSpeed);
	        	rotation.mulThis(flingRotation);
        	}
        	return rotation;
        }
	}

}
