package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by lavanya on 10/19/16.
 */
public class WatchFaceService extends CanvasWatchFaceService {

	private static final String TAG = WatchFaceService.class.getSimpleName();

	@Override
	public Engine onCreateEngine() {
		return new Engine();
	}

	private class Engine extends CanvasWatchFaceService.Engine
			implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {
		private Typeface WATCH_TEXT_TYPEFACE = Typeface.create(Typeface.SERIF, Typeface.NORMAL);

		private static final int MSG_UPDATE_TIME_ID = 42;
		private long mUpdateRateMs = 1000;
		private Calendar mCalendar = Calendar.getInstance();
		SharedPreferences sharedPreferences;
		private Paint mBackgroundColorPaint;
		private Paint mTextColorPaint;
		private Paint mDateColorPaint;
		private Paint mDataColorPaint;
		private Paint mBitmapColorPaint;
		private Paint mBitmapwhite;
		private boolean mHasTimeZoneReceiverBeenRegistered = false;
		private boolean mIsMutemode;
		private boolean mIsLowBitAmbient;
		private float mXoffset;
		private float mYoffset;
		private GoogleApiClient mGoogleApiClient;
		private ArrayList<Integer> data = null;
		private static final String FORECAST_PATH = "/forecast";
		private int mBackgroundColor = R.color.background;
		private int mTextColor = R.color.textcolor;
		private Bitmap mBackgroundBitmap;
		String tempma = "12" + "\u00B0";
		String tempmi = "5" + "\u00B0";
		int wtid = 800;
		final BroadcastReceiver mTimeZoneBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				mCalendar.setTimeZone(TimeZone.getDefault());
				invalidate();
			}
		};

		final android.os.Handler mUpdateTimeHandler = new android.os.Handler() {
			@Override
			public void handleMessage(Message message) {
				switch (message.what) {
					case MSG_UPDATE_TIME_ID:
						if (Log.isLoggable(TAG, Log.VERBOSE)) {
							Log.v(TAG, "updating time");
						}
						invalidate();
						if (isVisible() && !isInAmbientMode()) {
							long timeMs = System.currentTimeMillis();
							long delay =
									mUpdateRateMs - (timeMs % mUpdateRateMs);
							mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME_ID, delay);
						}

						break;
				}
			}
		};

		@Override
		public void onCreate(SurfaceHolder holder) {
			if (Log.isLoggable(TAG, Log.DEBUG)) {
				Log.d(TAG, "onCreate");
			}
			super.onCreate(holder);

			setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
					.setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
					.setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
					.setShowSystemUiTime(false)
					.setAcceptsTapEvents(true)
					.build());
			mCalendar = Calendar.getInstance();
			initBackground();
			initDisplayText();

			mGoogleApiClient = new GoogleApiClient.Builder(WatchFaceService.this)
					.addApi(Wearable.API)
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.build();
			mGoogleApiClient.connect();


		}

		private void initBackground() {
			mBackgroundColorPaint = new Paint();
			mBackgroundColorPaint.setColor(getResources().getColor(mBackgroundColor));
		}

		private void initDisplayText() {
			/*for time create paint object*/
			mTextColorPaint = new Paint();
			mTextColorPaint.setColor(getResources().getColor(mTextColor));
			mTextColorPaint.setTypeface(WATCH_TEXT_TYPEFACE);
			mTextColorPaint.setAntiAlias(true);
			mTextColorPaint.setTextSize(45);
			/*for date create paint object*/
			mDateColorPaint = new Paint();
			mDateColorPaint.setColor(getResources().getColor(mTextColor));
			mDateColorPaint.setTypeface(WATCH_TEXT_TYPEFACE);
			mDateColorPaint.setAntiAlias(true);
			mDateColorPaint.setTextSize(22);
			/*for temperature data*/
			mDataColorPaint = new Paint();
			mDataColorPaint.setColor(getResources().getColor(mTextColor));
			mDataColorPaint.setTypeface(WATCH_TEXT_TYPEFACE);
			mDataColorPaint.setAntiAlias(true);
			mDataColorPaint.setTextSize(32);
			/*for image create paint object*/
			mBitmapColorPaint = new Paint();
			mBitmapColorPaint.setColor(getResources().getColor(mTextColor));
			mBitmapColorPaint.setTypeface(WATCH_TEXT_TYPEFACE);
			mBitmapColorPaint.setAntiAlias(true);
			mBitmapColorPaint.setTextSize(32);

			mBitmapwhite = new Paint();
			mBitmapwhite.setColor(getResources().getColor(android.R.color.white));

		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			super.onVisibilityChanged(visible);

			if (visible) {
				if (!mHasTimeZoneReceiverBeenRegistered) {
					IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
					WatchFaceService.this.registerReceiver(mTimeZoneBroadcastReceiver, filter);
					mHasTimeZoneReceiverBeenRegistered = true;
				}

                /* Update time zone in case it changed while we weren't visible. */
				mCalendar.setTimeZone(TimeZone.getDefault());
				invalidate();
			} else {
				if (mHasTimeZoneReceiverBeenRegistered) {
					WatchFaceService.this.unregisterReceiver(mTimeZoneBroadcastReceiver);
					mHasTimeZoneReceiverBeenRegistered = false;
				}
			}
			/* Check and trigger whether or not timer should be running (only in active mode). */
			updateTimer();
		}

		private void updateTimer() {
			if (Log.isLoggable(TAG, Log.DEBUG)) {
				Log.d(TAG, "updateTimer");
			}
			mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME_ID);
			if (isVisible() && !isInAmbientMode()) {
				mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME_ID);
			}
		}

		@Override
		public void onApplyWindowInsets(WindowInsets insets) {
			super.onApplyWindowInsets(insets);
			mYoffset = 100;
			mXoffset = 100;

		}

		@Override
		public void onPropertiesChanged(Bundle properties) {
			super.onPropertiesChanged(properties);
			if (properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false)) {
				mIsLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
			}

		}

		public void onAmbientModeChanged(boolean inAmbientMode) {
			super.onAmbientModeChanged(inAmbientMode);
			if (inAmbientMode) {
				mTextColorPaint.setColor(getResources().getColor(R.color.textcolor));
			} else {
				mTextColorPaint.setColor(getResources().getColor(R.color.textcolor));
			}
			if (mIsLowBitAmbient) {
				mTextColorPaint.setAntiAlias(!inAmbientMode);
			}
			invalidate();
			updateTimer();
		}

		@Override
		public void onInterruptionFilterChanged(int interruptionFilter) {
			super.onInterruptionFilterChanged(interruptionFilter);
			boolean isDeviceMuted = (interruptionFilter == android.support.wearable.watchface.WatchFaceService.INTERRUPTION_FILTER_NONE);
			if (isDeviceMuted) {
				mUpdateRateMs = TimeUnit.MINUTES.toMillis(1);
			} else {
				mUpdateRateMs = 1000;
			}
			if (mIsMutemode != isDeviceMuted) {
				mIsMutemode = isDeviceMuted;
				int alpha = (isDeviceMuted) ? 100 : 255;
				mTextColorPaint.setAlpha(alpha);
				invalidate();
				updateTimer();
			}

		}

		public void onTimeTick() {
			super.onTimeTick();
			invalidate();
		}

		/*computing x position*/
		private float computeXOffset(String text, Paint paint, Rect watchBounds) {
			float centerX = watchBounds.exactCenterX();
			float timeLength = paint.measureText(text);
			return centerX - (timeLength / 2.0f);
		}

		/*computing y position for drawing*/
		private float computeTimeYOffset(String timeText, Paint timePaint, Rect watchBounds) {
			float centerY = watchBounds.exactCenterY();
			Rect textBounds = new Rect();
			timePaint.getTextBounds(timeText, 0, timeText.length(), textBounds);
			int textHeight = textBounds.height();
			return centerY + (textHeight / 2.0f);
		}

		/*computing y position for date*/
		private float computeDateYOffset(String dateText, Paint datePaint) {
			Rect textBounds = new Rect();
			datePaint.getTextBounds(dateText, 0, dateText.length(), textBounds);
			return textBounds.height() + 60.0f;
		}

		/*computing y position for temperature data*/
		private float computeDataYOffset(String datatext, Paint dataPaint) {
			Rect textBounds = new Rect();
			dataPaint.getTextBounds(datatext, 0, datatext.length(), textBounds);
			return textBounds.height() + 70.0f;
		}

		@Override
		public void onDraw(Canvas canvas, Rect Bounds) {
			super.onDraw(canvas, Bounds);
			Date date = mCalendar.getTime();
			SimpleDateFormat sdf = new SimpleDateFormat("EEE,MMM d yyyy ");
			String datetext = sdf.format(date);
			Bitmap scaled=null;
			//	mBackgroundBitmap = BitmapFactory.decodeResource(getResources(),getIconResourceForWeatherCondition(Integer.parseInt(MainActivity.datalist.get(3))));

			String datatext = tempma + " " + tempmi + "                      ";
			//	String datatext=12+"\u00B0" +"  " + 3+"\u00B0";
			canvas.drawRect(0, 0, Bounds.width(), Bounds.height(), mBackgroundColorPaint);
			mCalendar.setTimeInMillis(System.currentTimeMillis());
			int seconds = mCalendar.get(Calendar.SECOND);
			int minutes = mCalendar.get(Calendar.MINUTE);
			int hours = mCalendar.get(Calendar.HOUR);
			String timeText;
			if (isInAmbientMode() || mIsMutemode) {
				timeText = String.format(Locale.US,"%d:%02d", hours, minutes);

				canvas.drawColor(Color.BLACK);
				int h = 80; // height in pixels
				int w = 80; // width in pixels

				mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), getIconResourceForWeatherCondition(wtid));
				 scaled = Bitmap.createScaledBitmap(mBackgroundBitmap, h, w, true);
			} else {
				int h = 80; // height in pixels
				int w = 80; // width in pixels
				timeText = String.format(Locale.US,"%d:%02d:%02d", hours, minutes, seconds);
				mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), getartresourceforweather(wtid));
				scaled = Bitmap.createScaledBitmap(mBackgroundBitmap, h, w, true);
			}
			mXoffset = computeXOffset(datetext, mDateColorPaint, Bounds);
			mYoffset = computeDateYOffset(datatext, mDateColorPaint);
			canvas.drawText(datetext, mXoffset + 10, mYoffset + 45, mDateColorPaint);


			mYoffset = computeTimeYOffset(timeText, mTextColorPaint, Bounds);
			canvas.drawText(timeText, mXoffset + 10, mYoffset - 80, mTextColorPaint);

			canvas.drawLine((mXoffset + 60), (mYoffset - 20), (mXoffset + 110), (mYoffset - 20), mBitmapwhite);


			mYoffset = computeDataYOffset(datatext, mDataColorPaint);
			datatext += String.format("%s", datatext);
			canvas.drawBitmap(scaled, mXoffset-10, mYoffset + 60, mBitmapColorPaint);//x and y position
			canvas.drawText(datatext, mXoffset + 90, mYoffset + 110, mDataColorPaint);
		}

		@Override
		public void onConnected(@Nullable Bundle bundle) {
			Wearable.DataApi.addListener(mGoogleApiClient, this);
		}

		@Override
		public void onConnectionSuspended(int i) {

		}

		@Override
		public void onDataChanged(DataEventBuffer dataEventBuffer) {
			Log.d(TAG, "data changed in watchface");
			for (DataEvent event : dataEventBuffer) {
				if (event.getType() == DataEvent.TYPE_CHANGED) {
					String path = event.getDataItem().getUri().getPath();
					if (FORECAST_PATH.equals(path)) {
						DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
						tempma = dataMapItem.getDataMap()
								.getString(getString(R.string.tempmax));
						tempmi = dataMapItem.getDataMap().getString(getString(R.string.tempmin));
						wtid = dataMapItem.getDataMap().getInt(getString(R.string.idweath));
						//	datalist=dataMapItem.getDataMap().getStringArrayList("datas");
						//	for(int i=0;i<datalist.size();i++) {
						Log.d(TAG, "got the data" + tempma + tempmi + wtid);
						invalidate();
					}

				}
			}
		}

		@Override
		public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

		}
	}


	public int getIconResourceForWeatherCondition(int weatherId) {
		// Based on weather code data found at:
		// http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
		if (weatherId >= 200 && weatherId <= 232) {
			return R.drawable.ic_storm;
		} else if (weatherId >= 300 && weatherId <= 321) {
			return R.drawable.ic_light_rain;
		} else if (weatherId >= 500 && weatherId <= 504) {
			return R.drawable.ic_rain;
		} else if (weatherId == 511) {
			return R.drawable.ic_snow;
		} else if (weatherId >= 520 && weatherId <= 531) {
			return R.drawable.ic_rain;
		} else if (weatherId >= 600 && weatherId <= 622) {
			return R.drawable.ic_snow;
		} else if (weatherId >= 701 && weatherId <= 761) {
			return R.drawable.ic_fog;
		} else if (weatherId == 761 || weatherId == 781) {
			return R.drawable.ic_storm;
		} else if (weatherId == 800) {
			return R.drawable.ic_clear;
		} else if (weatherId == 801) {
			return R.drawable.ic_light_clouds;
		} else if (weatherId >= 802 && weatherId <= 804) {
			return R.drawable.ic_cloudy;
		}
		return -1;
	}

	private static int getartresourceforweather(int weatherId) {
		if (weatherId >= 200 && weatherId <= 232) {
			return R.drawable.art_storm;
		} else if (weatherId >= 300 && weatherId <= 321) {
			return R.drawable.art_light_rain;
		} else if (weatherId >= 500 && weatherId <= 504) {
			return R.drawable.art_rain;
		} else if (weatherId == 511) {
			return R.drawable.art_snow;
		} else if (weatherId >= 520 && weatherId <= 531) {
			return R.drawable.art_rain;
		} else if (weatherId >= 600 && weatherId <= 622) {
			return R.drawable.art_snow;
		} else if (weatherId >= 701 && weatherId <= 761) {
			return R.drawable.art_fog;
		} else if (weatherId == 761 || weatherId == 781) {
			return R.drawable.art_storm;
		} else if (weatherId == 800) {
			return R.drawable.art_clear;
		} else if (weatherId == 801) {
			return R.drawable.art_light_clouds;
		} else if (weatherId >= 802 && weatherId <= 804) {
			return R.drawable.art_clouds;
		}
		return -1;
	}
}


