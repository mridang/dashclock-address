package com.mridang.address;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
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
		BugSenseHandler.initAndStartSession(this, getString(R.string.bugsense));

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected void onUpdateData(int arg0) {

		Log.d("AddressWidget", "Calculating the phone's address");
		final ExtensionData edtInformation = new ExtensionData();
		setUpdateWhenScreenOn(false);

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

						} catch (UnknownHostException e) {
							Log.w("AddressWidget", "Unable to get the external address", e);
							setUpdateWhenScreenOn(true);
							return null;
						} catch (SocketException e) {
							Log.w("AddressWidget", "Unable to get the external address", e);
							setUpdateWhenScreenOn(true);
							return null;
						} catch (IOException e) {
							Log.w("AddressWidget", "Unable to get the external address", e);
							setUpdateWhenScreenOn(true);
							return null;
						} catch (Exception e) {
							Log.w("AddressWidget", "Unable to get the external address", e);
							BugSenseHandler.sendException(e);
							return null;
						}

					}

				}.execute().get();				

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

				Log.d("AddressWidget", "External IP: " + strAddress);

			} else {

				Log.d("AddressWidget", "The device doesn't have connectivity");

				edtInformation.expandedTitle(getString(R.string.disconnected));
				edtInformation.expandedBody(getString(R.string.no_connectivity));

			}
			
			edtInformation.visible(true);
			
			if (new Random().nextInt(5) == 0) {

				PackageManager mgrPackages = getApplicationContext().getPackageManager();

				try {

					mgrPackages.getPackageInfo("com.mridang.donate", PackageManager.GET_META_DATA);

				} catch (NameNotFoundException e) {

					Integer intExtensions = 0;
				    Intent ittFilter = new Intent("com.google.android.apps.dashclock.Extension");
				    String strPackage;

				    for (ResolveInfo info : mgrPackages.queryIntentServices(ittFilter, 0)) {

				    	strPackage = info.serviceInfo.applicationInfo.packageName;
						intExtensions = intExtensions + (strPackage.startsWith("com.mridang.") ? 1 : 0); 

					}

					if (intExtensions > 1) {

						edtInformation.visible(true);
						edtInformation.clickIntent(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=com.mridang.donate")));
						edtInformation.expandedTitle("Please consider a one time purchase to unlock.");
						edtInformation.expandedBody("Thank you for using " + intExtensions + " extensions of mine. Click this to make a one-time purchase or use just one extension to make this disappear.");
						setUpdateWhenScreenOn(true);

					}

				}

			} else {
				setUpdateWhenScreenOn(false);
			}

		} catch (Exception e) {
			edtInformation.visible(false);
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