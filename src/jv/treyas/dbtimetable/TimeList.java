// DB Timetable
// Copyright (C) 2011  Justin Vogel

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

package jv.treyas.dbtimetable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class TimeList extends Activity implements View.OnClickListener {

	private TextView mTitleText, zeroTimes;
	private Button tmrButton;
	private Long mRouteId;
	private Long mOriginId;
	private Long nextDeparture = (long) 100;  // For determining text color -- set high to protect when there's no new times
	private static final String TAG = "TIMELIST";
	private final Handler mHandler = new Handler();

	private DataBaseHelper mDb;
	private Calendar currentTime;
	private ListView listView;
	private SimpleCursorAdapter timeTableList;
	private boolean nextDay;


	final String[] matrix  = { "_id", "time", "departsIn" };
    final String[] columns = { "time", "departsIn" };
    final int[]    layouts = { R.id.timesLeft, R.id.timesRight };

	private MatrixCursor mCursor;

	final Runnable mUpdateResults = new Runnable() {
        public void run() {
            updateAdapter();

        }
    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDb = new DataBaseHelper(this);
		mDb.openDataBase();


		setContentView(R.layout.timelist);

		listView = (ListView) findViewById(R.id.timeListView);
		tmrButton = new Button(this);
		tmrButton.setText("Check tomorrow's times");
		listView.addFooterView(tmrButton);


		mTitleText = (TextView) findViewById(R.id.routeOriginTitle);
		zeroTimes = (TextView) findViewById(R.id.InactiveRouteText);

		mRouteId = (savedInstanceState == null) ? null :
        	(Long) savedInstanceState.getSerializable(DataBaseHelper.KEY_ROUTEID);
		mOriginId = (savedInstanceState == null) ? null :
        	(Long) savedInstanceState.getSerializable(DataBaseHelper.KEY_ORIGINID);
        if (mRouteId == null || mOriginId == null) {
        	Bundle extras = getIntent().getExtras();
        	mRouteId = extras != null ? extras.getLong(DataBaseHelper.KEY_ROUTEID)
        			: null;
        	mOriginId = extras != null ? extras.getLong(DataBaseHelper.KEY_ORIGINID)
        			: null;

        }

        tmrButton.setOnClickListener(this);


        currentTime = Calendar.getInstance();

        Cursor routeName = mDb.fetchRouteName(mRouteId);
		Cursor originName = mDb.fetchOriginName(mOriginId);
		startManagingCursor(originName);
		startManagingCursor(routeName);


		if(!originName.getString(originName.getColumnIndexOrThrow(DataBaseHelper.KEY_NAME)).contentEquals("to Plaza")) {

			mTitleText.setText(
					routeName.getString(routeName.getColumnIndexOrThrow(DataBaseHelper.KEY_NAME)) +
					" from " +
					originName.getString(originName.getColumnIndexOrThrow(DataBaseHelper.KEY_NAME)));
		}
		else {
			mTitleText.setText(
					routeName.getString(routeName.getColumnIndexOrThrow(DataBaseHelper.KEY_NAME)) + " " +
					originName.getString(originName.getColumnIndexOrThrow(DataBaseHelper.KEY_NAME)));
		}
		populateTimes();
		if(mCursor.getCount() > 0)
			setAdapter();
	}

	private void populateTimes() {
		mCursor = new MatrixCursor(matrix);

		if (mRouteId != null) {
			mCursor = createList(mCursor, false);
			if(mCursor.getCount() < 3 || currentTime.get(Calendar.HOUR_OF_DAY) >= 20) {
				mCursor = createList(mCursor, true);
				tmrButton.setVisibility(View.GONE);
				nextDay = true;
			}
			else nextDay = false;
			// Add text if no times on current route
			if(mCursor.getCount() < 1) {
				zeroTimes.setVisibility(View.VISIBLE);
			}
		}
	}

	private MatrixCursor createList(MatrixCursor timesMatrix, Boolean nextDay) {
	    Cursor timesCursor;
		int rowKey = 0; //
		String dayOfWeek = parseDay(nextDay); // grabs the day of the week code, use next day to check tomorrows times
		Calendar departTime = Calendar.getInstance();

		// last parameter is for parsing already departed times, don't use if checking tomorrow's times
		timesCursor = mDb.fetchTimes(mRouteId, dayOfWeek, mOriginId, (!nextDay ? timeSelection() : null));

		startManagingCursor(timesCursor);
		String[] times = new String[timesCursor.getCount()]; // List of Times
		String[] departsIn = new String[timesCursor.getCount()]; // Time until Departure
		int columnIndex = timesCursor.getColumnIndex(DataBaseHelper.KEY_TIME);

		int i = 0;
		while(!timesCursor.isAfterLast()) {
			times[i] = timesCursor.getString(columnIndex);

			// Format Time String from Database to a Calendar Object (to Compare to Current Time)
			try
			{
				DateFormat timeParse = new SimpleDateFormat("HH:mm");
				Date departDateTime = timeParse.parse(times[i]);
				departTime.setTime(departDateTime);
				departTime.set(Calendar.SECOND, 59); // Fix for minute calculation appearing to be early.

				// Check tomorrow's time if next day.  Calendar class handles leap years and 31+1 day of the month, luckily.
				departTime.set(
						currentTime.get(Calendar.YEAR),
						currentTime.get(Calendar.MONTH),
						( !nextDay ? currentTime.get(Calendar.DATE) : currentTime.get(Calendar.DATE) + 1 ) );
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			// Calculate the difference in time from now
			Long difference = departTime.getTimeInMillis() - currentTime.getTimeInMillis();
			difference = (difference/1000)/60; // From milliseconds to minutes.

			if(i == 0 && !nextDay) // If this is the first value + not checking next days departures, get the difference for setting a pretty text color
				nextDeparture = difference.longValue();

			// Format that correctly
			if(difference<60) {
				departsIn[i] = difference + " mins";
			} else {
				// if the minutes are below ten, we want a leading 0
				String mins = String.format("%02d", (difference % 60));
				departsIn[i] = (difference / 60) + " hr " + mins + " mins";
			}

			timesMatrix.addRow(new Object[] { rowKey++, times[i], departsIn[i] });
			timesCursor.moveToNext();
			i++;
		}
		timesMatrix.moveToFirst();
		return timesMatrix;
	}

	private void setAdapter() {

		class TimeViewBinder implements SimpleCursorAdapter.ViewBinder {

			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				boolean viewHandled = false;
				TextView row = (TextView) view;
				if(cursor.getPosition() == 0 && nextDeparture <= 30) {
					row.setTextColor(Color.YELLOW);
					row.setText(cursor.getString(columnIndex));
					viewHandled = true;
				}
				else {
					row.setTextColor(Color.WHITE);
				}
				return viewHandled;
			}
		}

		timeTableList =
			new SimpleCursorAdapter(this, R.layout.timetablelist2items, mCursor, columns, layouts);
		timeTableList.setViewBinder(new TimeViewBinder());
		listView.setAdapter(timeTableList);
		if(nextDay)
			listView.removeFooterView(tmrButton);
	}

	private void updateAdapter() {
		listView.removeFooterView(tmrButton);
		timeTableList.notifyDataSetChanged();
	}

	// Parse Current Day of the Week to match the 3 possible Ferry Schedules
	private String parseDay(Boolean nextDay) {
		String ferryDay = "";
		int currentDay = currentTime.get(Calendar.DAY_OF_WEEK);

		String holidayCheck;

		holidayCheck = parseDate(nextDay);

		if(!mDb.checkHoliday(holidayCheck)) { // Check if Public Holiday
			switch (currentDay) {

			case 1:
				ferryDay = DataBaseHelper.KEY_DAYID + "=3";
				break;
			case 2:
				// this is to handle that ONE time on Sunday night (but to Timetable... its Monday morning...)
				ferryDay = "(" + DataBaseHelper.KEY_DAYID + "=1 OR " + DataBaseHelper.KEY_DAYID + "=4)";
				break;
			case 3:
			case 4:
			case 5:
			case 6:
				ferryDay = DataBaseHelper.KEY_DAYID + "=1";
				break;
			case 7:
				// this is to handle that ONE time on Friday night (but to Timetable... its Saturday morning...)
				ferryDay = "(" + DataBaseHelper.KEY_DAYID + "=2 OR " + DataBaseHelper.KEY_DAYID + "=4)";
				break;
			default:
				Log.e(TAG, "Apparently no day of the week.");
				break;
			}


			if(nextDay) {
				if(currentDay == 1) {
					ferryDay = "(" + DataBaseHelper.KEY_DAYID + "=1 OR " + DataBaseHelper.KEY_DAYID + "=4)";
				}
				else if(currentDay == 7) {
					ferryDay = DataBaseHelper.KEY_DAYID + "=3";
				}
				else if(currentDay == 6) {
					ferryDay = "(" + DataBaseHelper.KEY_DAYID + "=2 OR " + DataBaseHelper.KEY_DAYID + "=4)";
				}
			}
		}
		else ferryDay = DataBaseHelper.KEY_DAYID + "=3"; // Public Holiday

		return ferryDay;
	}

	// Format Date
	private String parseDate(Boolean nextDay) {
		int dayOfMonth, month;
		String monthFormatted, dayOfMonthFormatted;
		if(!nextDay) {
			dayOfMonth = currentTime.get(Calendar.DAY_OF_MONTH);
			month = currentTime.get(Calendar.MONTH) + 1;
		}
		else {
			Calendar tomorrowDate = Calendar.getInstance();
			tomorrowDate.set(Calendar.DAY_OF_MONTH, (tomorrowDate.get(Calendar.DAY_OF_MONTH)+1));
			dayOfMonth = tomorrowDate.get(Calendar.DAY_OF_MONTH);
			month = tomorrowDate.get(Calendar.MONTH) + 1;
		}

		if(dayOfMonth<10)
			dayOfMonthFormatted = "0" + Integer.toString(dayOfMonth);
		else dayOfMonthFormatted = Integer.toString(dayOfMonth);

		if(month<10)
			monthFormatted = "0" + Integer.toString(month);
		else monthFormatted = Integer.toString(month);

		String date = currentTime.get(Calendar.YEAR) + "-" +
			monthFormatted + "-" + dayOfMonthFormatted;

		return date;
	}

	// Format Time to send to Database (to Filter already departed results)
	private String timeSelection() {
		String timeSelectionMinuteFormated;
		String timeSelectionHourFormated;
		int timeSelectionHour = currentTime.get(Calendar.HOUR_OF_DAY);
		int timeSelectionMinute = currentTime.get(Calendar.MINUTE);
		if(timeSelectionMinute<10) { // Add 0 to beginning of minutes if under 10
			timeSelectionMinuteFormated = "0" + timeSelectionMinute;
		}
		else timeSelectionMinuteFormated = Integer.toString(timeSelectionMinute);

		if(timeSelectionHour == 0) // Add 0 to beginning of hour if midnight
			timeSelectionHourFormated = "00";
		else if(timeSelectionHour < 10)
			timeSelectionHourFormated = "0" + timeSelectionHour;
		else timeSelectionHourFormated = Integer.toString(timeSelectionHour);
		String timeSelection = timeSelectionHourFormated + ":" + timeSelectionMinuteFormated;
		return timeSelection;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(mDb.isOpen())
			mDb.close();
	}

	@Override
	protected void onResume() {
		super.onResume();

	}


	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "on Start Called");
		currentTime = Calendar.getInstance();
		if(!mDb.isOpen()) {
			if(listView.getFooterViewsCount() < 1) {
				tmrButton.setVisibility(View.VISIBLE);
				listView.addFooterView(tmrButton);
			}
			mDb.openDataBase();
			populateTimes();
			setAdapter();
			Log.d(TAG, "Pop Times IF");
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(DataBaseHelper.KEY_ROUTEID, mRouteId);
		outState.putSerializable(DataBaseHelper.KEY_ORIGINID, mOriginId);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			if(mDb.isOpen())
				mDb.close();
		}
		return super.onKeyDown(keyCode, event);
	}

	//@Override
	public void onClick(View v) {
		Thread t = new Thread() {
            public void run() {
            	mCursor = createList(mCursor, true);
                mHandler.post(mUpdateResults);
            }
        };
        t.start();
        tmrButton.setVisibility(View.GONE);
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.list_menu, menu);
	    return true;
    }

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.main_menu:
        	if(mDb.isOpen())
        		mDb.close();
        	setResult(RESULT_FIRST_USER);
        	finish();
            return true;

        case R.id.about_menu:
        	if(mDb.isOpen())
        		mDb.close();
        	Intent i = new Intent(this, AboutPage.class);
        	startActivityForResult(i, DBTimetable.ACTIVITY_NEXT);
        	return true;
        }

        return super.onOptionsItemSelected(item);
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(resultCode) {
		case RESULT_FIRST_USER:
			if(mDb.isOpen())
				mDb.close();
			setResult(RESULT_FIRST_USER);
			finish();
		}
	}

}