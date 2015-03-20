package com.mridang.address;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.acra.ACRA;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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

import com.google.android.apps.dashclock.api.ExtensionData;

/*
 * This class is the main class that provides the widget
 */
public class AddressWidget extends ImprovedExtension {

	/*
	 * (non-Javadoc)
	 * @see com.mridang.address.ImprovedExtension#getIntents()
	 */
	@Override
	protected IntentFilter getIntents() {
		return new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.address.ImprovedExtension#getTag()
	 */
	@Override
	protected String getTag() {
		return getClass().getSimpleName();
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.address.ImprovedExtension#getUris()
	 */
	@Override
	protected String[] getUris() {
		return null;
	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int intReason) {

		Log.d(getTag(), "Calculating the phone's address");
		final ExtensionData edtInformation = new ExtensionData();
		setUpdateWhenScreenOn(false);

		try {

			ConnectivityManager cmrConnectivity = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo nifNetwork = cmrConnectivity.getActiveNetworkInfo();

			Log.d(getTag(), "Checking if the device has connectivity");
			if (nifNetwork != null && nifNetwork.isConnected()) {

				Document docPage = new AsyncTask<Void, Void, Document>() {

					@Override
					protected Document doInBackground(Void... params) {

						Log.d(getTag(), "Fetching external address since the device has connectivity");
						try {

							return Jsoup.connect("http://whatismyip.akamai.com/").get();

						} catch (UnknownHostException e) {
							Log.w(getTag(), "Unknown host when fetching the external address");
							setUpdateWhenScreenOn(true);
							return null;
						} catch (ConnectException e) {
							Log.w(getTag(), "Connection error when fetching the external address");
							setUpdateWhenScreenOn(true);
							return null;
						} catch (SocketTimeoutException e) {
							Log.w(getTag(), "Socket timed out when fetching the external address");
							setUpdateWhenScreenOn(true);
							return null;
						} catch (SocketException e) {
							Log.w(getTag(), "Unable to get the external address", e);
							setUpdateWhenScreenOn(true);
							return null;
						} catch (IOException e) {
							Log.w(getTag(), "Unable to get the external address", e);
							setUpdateWhenScreenOn(true);
							return null;
						} catch (Exception e) {
							Log.w(getTag(), "Unable to get the external address", e);
							ACRA.getErrorReporter().handleSilentException(e);
							return null;
						}

					}

				}.execute().get();

				String strAddress = docPage != null ? docPage.text() : "Unknown";
				Log.d(getTag(), "Checking if connected via Wifi");
				if (nifNetwork.getType() == ConnectivityManager.TYPE_WIFI) {

					Log.d(getTag(), "The device is connected by a wireless network.");
					edtInformation.expandedTitle(getString(R.string.wireless));
					WifiInfo wifWireless = ((WifiManager) getSystemService(WIFI_SERVICE)).getConnectionInfo();
					edtInformation.expandedBody(getString(R.string.external, strAddress) + "\n"
							+ getString(R.string.internal, Formatter.formatIpAddress(wifWireless.getIpAddress())));

				} else {

					Log.d(getTag(), "The device is connected by a mobile network.");
					edtInformation.expandedTitle(getString(R.string.cellular));
					edtInformation.expandedBody(strAddress);

				}

				Log.d(getTag(), "External IP: " + strAddress);

			} else {

				Log.d(getTag(), "The device doesn't have connectivity");
				edtInformation.expandedTitle(getString(R.string.disconnected));
				edtInformation.expandedBody(getString(R.string.no_connectivity));

			}

			edtInformation.visible(true);

		} catch (Exception e) {
			edtInformation.visible(false);
			Log.e(getTag(), "Encountered an error", e);
			ACRA.getErrorReporter().handleSilentException(e);
		}

		edtInformation.icon(R.drawable.ic_dashclock);
		doUpdate(edtInformation);

	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.address.ImprovedExtension#onReceiveIntent(android.content.Context, android.content.Intent)
	 */
	@Override
	protected void onReceiveIntent(Context ctxContext, Intent ittIntent) {
		onUpdateData(UPDATE_REASON_MANUAL);
	}

}