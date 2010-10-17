package info.a_iv.carcontrol.ui;

import info.a_iv.carcontrol.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.gsm.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity implements View.OnClickListener {
	public static final String SMS_FOR_START = "00Y %s";
	public static final String SMS_FOR_AUDIT = "00M %s";
	public static final String SMS_FOR_LOCATION = "000 %s 6630####### GDE";
	public static final String SMS_FOR_LOCK = "006 %s";
	public static final String SMS_FOR_UNLOCK = "007 %s";

	public static final String ACTION_START = "info.a_iv.carcontrol.START_ACTION";
	public static final String ACTION_AUDIT = "info.a_iv.carcontrol.AUDIT_ACTION";
	public static final String ACTION_LOCATION = "info.a_iv.carcontrol.LOCATION_ACTION";
	public static final String ACTION_LOCK = "info.a_iv.carcontrol.LOCK_ACTION";
	public static final String ACTION_UNLOCK = "info.a_iv.carcontrol.UNLOCK_ACTION";

	private static final int OPTION_MENU_EXIT = 1;
	private static final int OPTION_MENU_SETTINGS = 2;

	private static final int DIALOG_SETTINGS = 1;
	private static final int DIALOG_SENDING = 2;

	private String phone;
	private String password;
	private boolean lockEnabled;
	private SentReceiver sentReceiver;

	private class SentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean error = true;
			switch (getResultCode()) {
			case Activity.RESULT_OK:
				error = false;
				break;
			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
				break;
			case SmsManager.RESULT_ERROR_NO_SERVICE:
				break;
			case SmsManager.RESULT_ERROR_NULL_PDU:
				break;
			case SmsManager.RESULT_ERROR_RADIO_OFF:
				break;
			}
			if (!error
					&& (ACTION_LOCK.equals(intent.getAction()) || ACTION_UNLOCK
							.equals(intent.getAction()))) {
				lockEnabled = !lockEnabled;
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(getBaseContext());
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(getString(R.string.settings_lock),
						lockEnabled);
				editor.commit();
				drawLockButton();
			}
			Toast.makeText(Main.this, error ? R.string.failed : R.string.send,
					Toast.LENGTH_SHORT);
			removeDialog(DIALOG_SENDING);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		findViewById(R.id.start).setOnClickListener(this);
		findViewById(R.id.audit).setOnClickListener(this);
		findViewById(R.id.location).setOnClickListener(this);
		findViewById(R.id.lock).setOnClickListener(this);
		sentReceiver = new SentReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		phone = settings.getString(getString(R.string.settings_phone),
				getString(R.string.default_phone));
		password = settings.getString(getString(R.string.settings_password),
				getString(R.string.default_password));
		lockEnabled = settings.getBoolean(getString(R.string.settings_lock),
				false);
		if ("".equals(phone))
			showDialog(DIALOG_SETTINGS);

		((TextView) findViewById(R.id.title)).setText(String.format(
				getString(R.string.title), phone));
		drawLockButton();

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_START);
		intentFilter.addAction(ACTION_AUDIT);
		intentFilter.addAction(ACTION_LOCATION);
		intentFilter.addAction(ACTION_LOCK);
		intentFilter.addAction(ACTION_UNLOCK);
		registerReceiver(sentReceiver, intentFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(sentReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, OPTION_MENU_SETTINGS, 0, getResources().getText(
				R.string.settings));
		menu.add(0, OPTION_MENU_EXIT, 0, getResources().getText(R.string.exit));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case OPTION_MENU_EXIT:
			finish();
			return true;
		case OPTION_MENU_SETTINGS:
			startActivity(new Intent(this, Settings.class));
			return true;
		}
		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_SETTINGS:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.configure));
			builder.setCancelable(false);
			builder.setPositiveButton(getString(android.R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							startActivity(new Intent(Main.this, Settings.class));
						}
					});
			builder.setNegativeButton(getString(android.R.string.cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							finish();
						}
					});
			return builder.create();
		case DIALOG_SENDING:
			ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setMessage(getString(R.string.sending));
			progressDialog.setCancelable(false);
			progressDialog.setIndeterminate(true);
			return progressDialog;
		default:
			return null;
		}
	}

	@Override
	public void onClick(View view) {
		String command;
		String action;
		switch (view.getId()) {
		case R.id.start:
			command = SMS_FOR_START;
			action = ACTION_START;
			break;
		case R.id.audit:
			command = SMS_FOR_AUDIT;
			action = ACTION_AUDIT;
			break;
		case R.id.location:
			command = SMS_FOR_LOCATION;
			action = ACTION_LOCATION;
			break;
		case R.id.lock:
			if (lockEnabled) {
				command = SMS_FOR_UNLOCK;
				action = ACTION_UNLOCK;
			} else {
				command = SMS_FOR_LOCK;
				action = ACTION_LOCK;
			}
			break;
		default:
			return;
		}
		String message = String.format(command, password);
		SmsManager smsManager = SmsManager.getDefault();
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				new Intent(action), 0);
		smsManager.sendTextMessage(phone, null, message, pendingIntent, null);
		showDialog(DIALOG_SENDING);
	}

	private void drawLockButton() {
		((Button) findViewById(R.id.lock))
				.setText(getString(lockEnabled ? R.string.unlock
						: R.string.lock));
	}
}