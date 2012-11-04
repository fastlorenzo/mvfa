/*
	Copyright (C) 2011 Lorenzo Bernardi (fastlorenzo@gmail.com)
	2010 Ben Van Daele (vandaeleben@gmail.com)

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.bernardi.mvforandroid.beta.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import be.bernardi.mvforandroid.beta.Log;
import be.bernardi.mvforandroid.beta.mvapi.SimBalance;
import be.bernardi.mvforandroid.beta.mvapi.TopUpHistory;
import be.bernardi.mvforandroid.beta.mvapi.TopUpHistory.TopUp;
import be.bernardi.mvforandroid.beta.mvapi.Usage;
import be.bernardi.mvforandroid.beta.mvapi.Usage.UsageItem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String	DATABASE_NAME	= "mvforandroid.db";
	private static final int	SCHEMA_VERSION	= 5;

	public final UsageDB		usage;
	public final Credit			credit;
	public final Topups			topups;
	public final Msisdns		msisdns;
	public final PointsStat		pointsstat;

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, SCHEMA_VERSION);

		this.usage = new UsageDB();
		this.credit = new Credit();
		this.topups = new Topups();
		this.msisdns = new Msisdns();
		this.pointsstat = new PointsStat();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS " + UsageDB.TABLE_NAME + " (" + "_id INTEGER PRIMARY KEY AUTOINCREMENT, " + "timestamp INTEGER NOT NULL, "
				+ "duration INTEGER, " + "type INTEGER, " + "incoming INTEGER, " + "contact TEXT, " + "cost REAL" + ", fk_msisdn_id INTEGER);");
		db.execSQL("CREATE TABLE IF NOT EXISTS " + Topups.TABLE_NAME + " (" + "_id INTEGER PRIMARY KEY AUTOINCREMENT, " + "amount REAL NOT NULL, "
				+ "method TEXT NOT NULL, " + "executed_on INTEGER NOT NULL, " + "received_on INTEGER, " + "status TEXT NOT NULL, " + "fk_msisdn_id INTEGER);");
		db.execSQL("CREATE TABLE IF NOT EXISTS " + Credit.TABLE_NAME + " (" + "_id INTEGER PRIMARY KEY AUTOINCREMENT, " + "valid_until INTEGER NULL, "
				+ "expired INTEGER NOT NULL, " + "sms INTEGER NOT NULL, " + "data INTEGER NOT NULL, " + "credits REAL NOT NULL, "
				+ "price_plan TEXT NOT NULL, " + "sms_son INTEGER NOT NULL, " + "fk_msisdn_id INTEGER);");
		db.execSQL("CREATE TABLE IF NOT EXISTS " + PointsStat.TABLE_NAME + " (" + "_id INTEGER PRIMARY KEY AUTOINCREMENT, " + "used_points INTEGER NOT NULL, "
				+ "unused_points INTEGER NOT NULL, " + "waiting_points INTEGER NOT NULL, " + "topups_used INTEGER NOT NULL, "
				+ "earned_points INTEGER NOT NULL, " + "fk_msisdn_id INTEGER);");

		db.execSQL("CREATE TABLE IF NOT EXISTS " + Msisdns.TABLE_NAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, msisdn TEXT);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d("[DB onUpgrade] old: " + oldVersion + " new: " + newVersion);
		db.execSQL("DROP TABLE IF EXISTS " + Credit.TABLE_NAME + ";");
		db.execSQL("DROP TABLE IF EXISTS " + UsageDB.TABLE_NAME + ";");
		db.execSQL("DROP TABLE IF EXISTS " + Topups.TABLE_NAME + ";");
		db.execSQL("DROP TABLE IF EXISTS " + Msisdns.TABLE_NAME + ";");
		db.execSQL("DROP TABLE IF EXISTS " + PointsStat.TABLE_NAME + ";");

		db.execSQL("CREATE TABLE IF NOT EXISTS " + UsageDB.TABLE_NAME + " (" + "_id INTEGER PRIMARY KEY AUTOINCREMENT, " + "timestamp INTEGER NOT NULL, "
				+ "duration INTEGER, " + "type INTEGER, " + "incoming INTEGER, " + "contact TEXT, " + "cost REAL" + ", fk_msisdn_id INTEGER);");
		db.execSQL("CREATE TABLE IF NOT EXISTS " + Topups.TABLE_NAME + " (" + "_id INTEGER PRIMARY KEY AUTOINCREMENT, " + "amount REAL NOT NULL, "
				+ "method TEXT NOT NULL, " + "executed_on INTEGER NOT NULL, " + "received_on INTEGER, " + "status TEXT NOT NULL, " + "fk_msisdn_id INTEGER);");
		db.execSQL("CREATE TABLE IF NOT EXISTS " + Credit.TABLE_NAME + " (" + "_id INTEGER PRIMARY KEY AUTOINCREMENT, " + "valid_until INTEGER NULL, "
				+ "expired INTEGER NOT NULL, " + "sms INTEGER NOT NULL, " + "data INTEGER NOT NULL, " + "credits REAL NOT NULL, "
				+ "price_plan TEXT NOT NULL, " + "sms_son INTEGER NOT NULL, " + "fk_msisdn_id INTEGER);");
		db.execSQL("CREATE TABLE IF NOT EXISTS " + PointsStat.TABLE_NAME + " (" + "_id INTEGER PRIMARY KEY AUTOINCREMENT, " + "used_points INTEGER NOT NULL, "
				+ "unused_points INTEGER NOT NULL, " + "waiting_points INTEGER NOT NULL, " + "topups_used INTEGER NOT NULL, "
				+ "earned_points INTEGER NOT NULL, " + "fk_msisdn_id INTEGER);");

		db.execSQL("CREATE TABLE IF NOT EXISTS " + Msisdns.TABLE_NAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, msisdn TEXT);");
	}

	public class Credit {

		private static final String	TABLE_NAME	= "credit";

		public void update(SimBalance balance) {
			int msisdn_id = msisdns.getMsisdnId(balance.getMsisdn());
			Cursor query = getWritableDatabase().query(TABLE_NAME, new String[] { "_id" }, "fk_msisdn_id="+msisdn_id, null, null, null, null, null);
			ContentValues values = new ContentValues();
			
			values.put("valid_until", balance.getValidUntil().getTime());
			values.put("expired", balance.isExpired() ? 1 : 0);
			values.put("data", balance.getData());
			values.put("price_plan", balance.getPricePlan());
			values.put("credits", balance.getCredits());
			values.put("sms", balance.getSms());
			values.put("sms_son", balance.getSmsSon());
			values.put("fk_msisdn_id", msisdn_id);

			if(query.getCount() == 0) {
				// No credit info stored yet, insert a row
				getWritableDatabase().insert(TABLE_NAME, "valid_until", values);
			}
			else {
				// Credit info present already, so update it
				getWritableDatabase().update(TABLE_NAME, values, "fk_msisdn_id="+msisdn_id, null);
			}
			query.close();
		}

		public long getValidUntil() {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
			long result;

			if(c.moveToFirst())
				result = c.getLong(1);
			else
				result = 0;

			c.close();
			return result;
		}

		public boolean isExpired() {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
			boolean result;

			if(c.moveToFirst())
				result = c.getLong(2) == 1;
			else
				result = true;

			c.close();
			return result;
		}

		public int getRemainingSms() {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
			int result;

			if(c.moveToFirst())
				result = c.getInt(3);
			else
				result = 0;

			c.close();
			return result;
		}

		public long getRemainingData() {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
			long result;

			if(c.moveToFirst())
				result = c.getLong(4);
			else
				result = 0;

			c.close();
			return result;
		}

		public float getRemainingCredit() {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
			float result;

			if(c.moveToFirst())
				result = c.getFloat(5);
			else
				result = 0;

			c.close();
			return result;
		}

		public int getPricePlan() {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
			int result;

			if(c.moveToFirst())
				result = c.getInt(6);
			else
				result = 0;

			c.close();
			return result;
		}

		public int getRemainingSmsSuperOnNet() {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
			int result;

			if(c.moveToFirst())
				result = c.getInt(7);
			else
				result = 0;

			c.close();
			return result;
		}
	}

	public class UsageDB {

		private static final String	TABLE_NAME		= "usage";

		public static final int		ORDER_BY_DATE	= 1;

		public void update(Usage usage) {
			int msisdn_id = msisdns.getMsisdnId(usage.getMsisdn());

			getWritableDatabase().delete(TABLE_NAME, "fk_msisdn_id = " + msisdn_id, null);

			getWritableDatabase().beginTransaction();
			for(UsageItem item : usage.getUsages()) {
				insert(item, msisdn_id);
			}
			getWritableDatabase().setTransactionSuccessful();
			getWritableDatabase().endTransaction();
		}

		public void insert(UsageItem item, int msisdn_id) {

			ContentValues values = new ContentValues();

			if(item.getStart_timestamp() != null)
				values.put("timestamp", item.getStart_timestamp().getTime());
			values.put("duration", item.getDuration_connection());
			values.put("type", item.getType());
			values.put("incoming", item.isIncoming() ? 1 : 0);
			values.put("contact", item.getTo());
			values.put("cost", item.getPrice());
			values.put("fk_msisdn_id", msisdn_id);

			getWritableDatabase().insert(TABLE_NAME, "timestamp", values);
		}

		public Cursor get(long id) {
			return getReadableDatabase().query(TABLE_NAME, null, "_id=" + id, null, null, null, null);
		}

		/**
		 * Returns a cursor over the Usage table.
		 * 
		 * @param isSearch
		 *            Whether to include usage records obtained by a search, or
		 *            (xor) those obtained through auto-updating.
		 * @param order
		 *            The constant representing the field to order the cursor
		 *            by.
		 * @param ascending
		 *            Whether the order should be ascending or descending.
		 */
		public Cursor get(int order, boolean ascending, String msisdn) {
			String orderBy = null;
			switch(order) {
				case ORDER_BY_DATE:
					orderBy = "timestamp " + (ascending ? "asc" : "desc");
			}
			return getReadableDatabase().query(TABLE_NAME, null, "fk_msisdn_id = "+msisdns.getMsisdnId(msisdn), null, null, null, orderBy);
		}

		public long getTimestamp(Cursor c) {
			return c.getLong(1);
		}

		public long getduration(Cursor c) {
			return c.getLong(2);
		}

		public int getType(Cursor c) {
			return c.getInt(3);
		}

		public boolean isIncoming(Cursor c) {
			return c.getInt(4) == 1;
		}

		public String getContact(Cursor c) {
			return c.getString(5);
		}

		public double getCost(Cursor c) {
			return c.getDouble(6);
		}

	}

	public class Topups {

		private static final String	TABLE_NAME	= "topups";

		public void update(TopUpHistory topups) throws JSONException {
			getWritableDatabase().delete(TABLE_NAME, null, null);
			for(TopUp item : topups.getTopups()) {
				insert(item);
			}
		}

		private void insert(TopUp topup) throws JSONException {
			ContentValues values = new ContentValues();

			values.put("amount", topup.getAmount());
			values.put("method", topup.getMethod());
			values.put("executed_on", topup.getExecuted_on().getTime());
			if(topup.getPayment_received_on() != null)
				values.put("received_on", topup.getPayment_received_on().getTime());
			values.put("status", topup.getStatus());

			getWritableDatabase().insert(TABLE_NAME, "timestamp", values);
		}

		public Cursor getAll() {
			return getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
		}

		public double getAmount(Cursor c) {
			return c.getDouble(1);
		}

		public String getMethod(Cursor c) {
			return c.getString(2);
		}

		public long getExecutedOn(Cursor c) {
			return c.getLong(3);
		}

		public long getReceivedOn(Cursor c) {
			return c.getLong(4);
		}

		public String getStatus(Cursor c) {
			return c.getString(5);
		}

	}

	public class Msisdns {
		private static final String	TABLE_NAME	= "msisdns";

		public void update(JSONArray jsonArray) throws JSONException {
			getWritableDatabase().delete(TABLE_NAME, null, null);
			for(int i = 0; i < jsonArray.length(); i++) {
				String msisdn = jsonArray.getString(i);
				insert(msisdn);
			}
		}

		private void insert(String msisdn) throws JSONException {
			ContentValues values = new ContentValues();
			values.put("msisdn", msisdn);
			getWritableDatabase().insert(TABLE_NAME, "_id", values);
		}

		public Cursor getAll() {
			return getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
		}

		public String[] getMsisdnList() {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
			String[] result = null;
			int i = 0;
			if(c.moveToFirst()) {
				do {
					i++;
				}
				while(c.moveToNext());
				result = new String[i];
				c.moveToFirst();
				i = 0;
				do {
					if(c.getString(1) != null) {
						result[i] = c.getString(1);
						i++;
					}
				}
				while(c.moveToNext());
			}
			c.close();
			return result;
		}

		public int getMsisdnId(String msisdn) {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, "msisdn LIKE \"" + msisdn + "\"", null, null, null, null);
			int result = 0;
			if(c.moveToFirst()) {
				result = c.getInt(0);
			}
			c.close();
			return result;
		}

		public String getMsisdn(int msisdn_id) {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, "_id" + msisdn_id, null, null, null, null);
			String result = "";
			if(c.moveToFirst()) {
				result = c.getString(1);
			}
			c.close();
			return result;
		}

	}

	public class PointsStat {

		private static final String	TABLE_NAME	= "pointsstat";

		public void update(JSONObject json, boolean data_only) throws JSONException, NumberFormatException {
			Cursor query = getWritableDatabase().query(TABLE_NAME, new String[] { "_id" }, null, null, null, null, null, null);

			ContentValues values = new ContentValues();

			values.put("used_points", json.getInt("used_points"));
			values.put("unused_points", json.getInt("unused_points"));
			values.put("waiting_points", json.getInt("waiting_points"));
			values.put("topups_used", json.getInt("topups_used"));
			values.put("earned_points", json.getInt("earned_points"));

			if(query.getCount() == 0) {
				// No info stored yet, insert a row
				getWritableDatabase().insert(TABLE_NAME, null, values);
			}
			else {
				// Info present already, so update it
				getWritableDatabase().update(TABLE_NAME, values, null, null);
			}
			query.close();
		}

		public int getUsedPoints() {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
			int result;
			if(c.moveToFirst())
				result = c.getInt(1);
			else
				result = 0;
			c.close();
			return result;
		}

		public int getUnusedPoints() {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
			int result;
			if(c.moveToFirst())
				result = c.getInt(2);
			else
				result = 0;
			c.close();
			return result;
		}

		public int getWaitingPoints() {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
			int result;
			if(c.moveToFirst())
				result = c.getInt(3);
			else
				result = 0;
			c.close();
			return result;
		}

		public int getUsedTopups() {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
			int result;
			if(c.moveToFirst())
				result = c.getInt(4);
			else
				result = 0;
			c.close();
			return result;
		}

		public int getEarnedPoints() {
			Cursor c = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
			int result;
			if(c.moveToFirst())
				result = c.getInt(5);
			else
				result = 0;
			c.close();
			return result;
		}

	}

}
