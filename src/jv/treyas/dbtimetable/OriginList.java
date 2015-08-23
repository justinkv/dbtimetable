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

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class OriginList extends ListActivity {

	private TextView mTitleText;
	private DataBaseHelper mDb;
	private Long mRouteId;
	//private static final String TAG = "ORIGINLIST";
	private Cursor routeTitleCursor, originsCursor;
	private SimpleCursorAdapter origins;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDb = new DataBaseHelper(this);
		setContentView(R.layout.origin_layout);

		mTitleText = (TextView) findViewById(R.id.routeTitle);

		mRouteId = (savedInstanceState == null) ? null :
        	(Long) savedInstanceState.getSerializable(DataBaseHelper.KEY_ROUTEID);
        if (mRouteId == null) {
        	Bundle extras = getIntent().getExtras();
        	mRouteId = extras != null ? extras.getLong(DataBaseHelper.KEY_ROUTEID)
        			: null;
        }
        //Log.d(TAG, new String ("RouteID is " + mRouteId.toString()));
	}

	// Populates fields
	private void populateOrigins() {

		class OriginViewBinder implements SimpleCursorAdapter.ViewBinder {

			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

				if(!cursor.getString(columnIndex).contentEquals("to Plaza")) {
					TextView row = (TextView) view;
					row.setText("From " + cursor.getString(columnIndex));
					return true;
				}
				else if(cursor.getString(columnIndex).contentEquals("to Plaza")) {
					TextView row = (TextView) view;
					row.setText("To Plaza");
					return true;
				}
				else return false;
			}
		}

		String[] originsId = parseOriginsID();
		if (mRouteId != null && mDb.isOpen())
		{
			routeTitleCursor = mDb.fetchRouteName(mRouteId);
			originsCursor = mDb.fetchOriginName(originsId);

			mTitleText.setText(routeTitleCursor.getString(routeTitleCursor.getColumnIndexOrThrow(DataBaseHelper.KEY_NAME)));

			String[] originList = new String[] {DataBaseHelper.KEY_NAME};
			int[] rows = new int[] {R.id.routerow};

			origins =
				new SimpleCursorAdapter(this, R.layout.route_row, originsCursor, originList, rows);
			origins.setViewBinder(new OriginViewBinder());
			setListAdapter(origins);
		}
	}

	// Pulls Distinct OriginID's From a Route's Timetable and returns it as a String Array (for use in Query SelectionArgs)
	private String[] parseOriginsID() {
		int idcount = 0;
		if (mRouteId != null) {
			Cursor originsIdCursor = mDb.fetchOriginsID(mRouteId);
			startManagingCursor(originsIdCursor);
			String[] originsId = new String[originsIdCursor.getCount()];
			while(originsIdCursor.isAfterLast() != true) {
				int columnIndex = originsIdCursor.getColumnIndex(DataBaseHelper.KEY_ORIGINID);
				originsId[idcount] = originsIdCursor.getString(columnIndex);
				originsIdCursor.moveToNext();
				idcount++;
			}
			//Log.d(TAG, "Number of OriginIDs=" + originsId.length);
			return originsId;
		}
		else return null;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, TimeList.class);
		i.putExtra(DataBaseHelper.KEY_ROUTEID, mRouteId);
		i.putExtra(DataBaseHelper.KEY_ORIGINID, id);
		//Log.d(TAG, "List items row id =" + Long.toString(id));
		startActivityForResult(i, DBTimetable.ACTIVITY_NEXT);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			if(mDb.isOpen())
				mDb.close();
		}
		return super.onKeyDown(keyCode, event);
	}

	/*@Override
	protected void onPause() {
		super.onPause();
		if(!routeTitleCursor.isClosed())
			routeTitleCursor.close();
		if(!originsCursor.isClosed())
			originsCursor.close();
		if(mDb.isOpen())
		mDb.close();
	}*/



	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();

		if (this.origins != null) {
			this.origins.getCursor().close();
			this.origins = null;
		}

		if (this.routeTitleCursor != null) {
			this.routeTitleCursor.close();
			this.routeTitleCursor = null;
		}

		if (this.originsCursor != null) {
			this.originsCursor.close();
			this.originsCursor = null;
		}

		if (this.mDb != null) {
			this.mDb.close();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(!mDb.isOpen()) {
			mDb.openDataBase();
			populateOrigins();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(DataBaseHelper.KEY_ROUTEID, mRouteId);
		//mDb.close();
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
}
