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

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class DBTimetable extends Activity implements View.OnClickListener {

	private DataBaseHelper mDb;

	private static final String TAG = "MAIN MENU";
	private Button ferry, bus;
	private TextView ferryText, busText;
	public static final int ACTIVITY_NEXT = 1;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainmenu);
        mDb = new DataBaseHelper(this);

        ferry = (Button) findViewById(R.id.Ferry);
        ferryText = (TextView) findViewById(R.id.TextView01);
        bus = (Button) findViewById(R.id.Bus);
        busText = (TextView) findViewById(R.id.TextView02);

        // Click Listeners
        ferry.setOnClickListener(this);
        ferryText.setOnClickListener(this);
        bus.setOnClickListener(this);
        busText.setOnClickListener(this);

        try {
        	mDb.createDataBase();
        }
        catch (IOException ioe) {
        	Log.e(TAG, "Unable to create database:" + ioe);
        }
        mDb.close();
	}

	public void onClick(View v) {
		if(v == ferry || v == ferryText) {
			Intent i = new Intent(this, RouteList.class);
			i.putExtra(DataBaseHelper.KEY_ROUTETYPE, DataBaseHelper.FERRYROUTE);
			startActivityForResult(i, DBTimetable.ACTIVITY_NEXT);
		}

		else if(v == bus || v == busText){
			Intent i = new Intent(this, RouteList.class);
			i.putExtra(DataBaseHelper.KEY_ROUTETYPE, DataBaseHelper.BUSROUTE);
			startActivityForResult(i, DBTimetable.ACTIVITY_NEXT);
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.list_mainmenu, menu);
	    return true;
    }

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.about_menu:
        	Intent i = new Intent(this, AboutPage.class);
        	startActivityForResult(i, DBTimetable.ACTIVITY_NEXT);
        	return true;
        }
        return super.onOptionsItemSelected(item);
	}
}