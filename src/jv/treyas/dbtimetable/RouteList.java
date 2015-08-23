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

public class RouteList extends ListActivity {

	private DataBaseHelper mDb;
	private SimpleCursorAdapter routes;
	private Cursor routesCursor;

	//private static final String TAG = "ROUTELIST";
	private int routeType = -1;
	private TextView mTitleBar;



	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timetablemain);
        mDb = new DataBaseHelper(this);
        mDb.openDataBase();

        routeType = (savedInstanceState == null) ? -1 :
        	(Integer) savedInstanceState.getSerializable(DataBaseHelper.KEY_ROUTETYPE);

        if(routeType == -1) {
        	Bundle extras = getIntent().getExtras();
        	routeType = extras != null ? extras.getInt(DataBaseHelper.KEY_ROUTETYPE)
        			: null;
        }

        mTitleBar = (TextView) findViewById(R.id.routeTypeTitle);

        switch(routeType) {
        case 0:
        	mTitleBar.setText("Ferry / Kai-to Routes");
        	break;
        case 1:
        	mTitleBar.setText("Bus Routes");
        	break;
        default:
        	mTitleBar.setText("Routes");
        	break;
        }

        fillRows();

        registerForContextMenu(getListView());
    }

	private void fillRows() {
		routesCursor = mDb.fetchRoutes(routeType);
		startManagingCursor(routesCursor);

		String[] routesList = new String[] {DataBaseHelper.KEY_NAME};

		// array of fields to bind list to
		int[] field = new int[] {R.id.routerow};

		routes = new SimpleCursorAdapter(this, R.layout.route_row, routesCursor, routesList, field);
		setListAdapter(routes);

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, OriginList.class);
		i.putExtra(DataBaseHelper.KEY_ROUTEID, id);
		//mDb.close();
		startActivityForResult(i, DBTimetable.ACTIVITY_NEXT);
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (this.routes !=null) {
	        this.routes.getCursor().close();
	        this.routes = null;
	    }

		if (this.routesCursor != null) {
			this.routesCursor.close();
			this.routesCursor = null;
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
			fillRows();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(DataBaseHelper.KEY_ROUTETYPE, routeType);
		if(mDb.isOpen())
			mDb.close();
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
			mDb.close();
			setResult(RESULT_FIRST_USER);
			finish();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			mDb.close();
		}

		return super.onKeyDown(keyCode, event);
	}




}