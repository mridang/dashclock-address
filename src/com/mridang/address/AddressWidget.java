package com.mridang.address;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.format.Formatter;
import android.util.Log;

import com.bugsense.trace.BugSenseHandler;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

/*
 * This class is the main class that provides the widget
 */
public class AddressWidget extends DashClockExtension {

	/* This is the instance of the receiver that deals with connectivity */
	private ConnectivityReceiver objConnectivityReceiver;

	/*
	 * This class is the receiver for getting connectivity events
	 */
	private class ConnectivityReceiver extends BroadcastReceiver {

		/*
		 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
		 */
		@Override
		public void onReceive(Context ctxContext, Intent ittIntent) {

			onUpdateData(0);

		}

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onInitialize(boolean)
	 */
	@Override
	protected void onInitialize(boolean booReconnect) {

		super.onInitialize(booReconnect);

		if (objConnectivityReceiver != null) {

			try {

				Log.d("AddressWidget", "Unregistered any existing status receivers");
				unregisterReceiver(objConnectivityReceiver);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		IntentFilter itfIntents = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

		objConnectivityReceiver = new ConnectivityReceiver();
		registerReceiver(objConnectivityReceiver, itfIntents);
		Log.d("AddressWidget", "Registered the status receiver");

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onCreate()
	 */
	public void onCreate() {

		super.onCreate();
		Log.d("AddressWidget", "Created");
		BugSenseHandler.initAndStartSession(this, "55a4b929");

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int arg0) {

		setUpdateWhenScreenOn(true);

		Log.d("AddressWidget", "Calculating the phone's address");
		final ExtensionData edtInformation = new ExtensionData();
		edtInformation.visible(false);

		try {

			ConnectivityManager cmrConnectivity = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo nifNetwork = cmrConnectivity.getActiveNetworkInfo();

			Log.d("AddressWidget", "Checking if the device has connectivity");
			if (nifNetwork != null && nifNetwork.isConnected()) {

				Document docPage = new AsyncTask<Void, Void, Document>() {

					@Override
					protected Document doInBackground(Void... params) {

						Log.d("AddressWidget", "Fetching external address since the device has connectivity");
						try {

							return Jsoup.connect("http://api.exip.org/?call=ip").get();

						} catch (IOException e) {
							throw new RuntimeException(e);
						}

					}

				}.get();				

				String strAddress = docPage != null ? docPage.body().text() : "Unknown";

				Log.d("AddressWidget", "Checking if is connvected via Wifi");
				if (nifNetwork.getType() == ConnectivityManager.TYPE_WIFI) {

					Log.d("AddressWidget", "The device is connected by a wireless network.");

					edtInformation.expandedTitle(getString(R.string.wireless));
					WifiInfo wifWireless = ((WifiManager) getSystemService(WIFI_SERVICE)).getConnectionInfo();
					edtInformation.expandedBody(getString(R.string.external, strAddress) + "\n" + getString(R.string.internal, Formatter.formatIpAddress(wifWireless.getIpAddress())));

				} else {

					Log.d("AddressWidget", "The device is connected by a mobile network.");

					edtInformation.expandedTitle(getString(R.string.cellular));
					edtInformation.expandedBody(strAddress);
				}

				Log.d("AddressWidget", "External IP: " + docPage.body().text());

			} else {

				Log.d("AddressWidget", "The device doesn't have connectivity");

				edtInformation.expandedTitle("Disconnected");
				edtInformation.expandedBody("No internet access");

			}

			edtInformation.visible(true); 

		} catch (Exception e) {
			Log.e("AddressWidget", "Encountered an error", e);
			BugSenseHandler.sendException(e);
		}

		edtInformation.icon(R.drawable.ic_dashclock);
		publishUpdate(edtInformation);
		Log.d("AddressWidget", "Done");

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onDestroy()
	 */
	public void onDestroy() {

		super.onDestroy();

		if (objConnectivityReceiver != null) {

			try {

				Log.d("AddressWidget", "Unregistered the status receiver");
				unregisterReceiver(objConnectivityReceiver);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		Log.d("AddressWidget", "Destroyed");
		BugSenseHandler.closeSession(this);

	}

}