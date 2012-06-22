package ioio.connection_tester;

import java.util.Formatter;

import ioio.lib.spi.IOIOConnectionFactory;
import ioio.lib.util.IOIOConnectionRegistry;
import ioio.lib.util.IOIOConnectionManager.IOIOConnectionThreadProvider;
import ioio.lib.util.IOIOConnectionManager.Thread;
import ioio.lib.util.android.AndroidIOIOConnectionManager;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class IOIOConnectionTesterActivity extends Activity implements
		IOIOConnectionThreadProvider {
	static {
		IOIOConnectionRegistry
				.addBootstraps(new String[] {
						"ioio.lib.impl.SocketIOIOConnectionBootstrap",
						"ioio.lib.android.accessory.AccessoryConnectionBootstrap",
						"ioio.lib.android.bluetooth.BluetoothIOIOConnectionBootstrap" });
	}

	AndroidIOIOConnectionManager manager_ = new AndroidIOIOConnectionManager(
			this, this);
	ViewGroup mainLayout_;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		manager_.create();
		mainLayout_ = (ViewGroup) findViewById(R.id.main_layout);
	}

	@Override
	public Thread createThreadFromFactory(final IOIOConnectionFactory factory) {
		final TestResults results = new TestResults();
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				addConnection(factory.getType(), factory.getExtra(), results);
			}
		});
		return new TestThread(factory, results);
	}

	// Must be called from UI thread.
	private void addConnection(String type, Object extra, TestResults results) {
		LayoutInflater inflater = getLayoutInflater();
		View view = inflater.inflate(R.layout.connection_stats, mainLayout_,
				false);
		mainLayout_.addView(view);
		TextView typeTextView = (TextView) view
				.findViewById(R.id.connection_type);
		TextView extraTextView = (TextView) view
				.findViewById(R.id.connection_extra);
		if (type.equals("ioio.lib.impl.SocketIOIOConnection")) {
			typeTextView.setText("Socket (ADB)");
			extraTextView.setText(extra.toString());
		} else if (type
				.equals("ioio.lib.android.bluetooth.BluetoothIOIOConnection")) {
			typeTextView.setText("Bluetooth");
			extraTextView.setText((String) ((Object[]) extra)[0]);
		} else if (type
				.equals("ioio.lib.android.accessory.AccessoryConnectionBootstrap.Connection")) {
			typeTextView.setText("OpenAccessory");
			extraTextView.setVisibility(View.GONE);
		}
		createResultListener(view, results);
	}

	private void createResultListener(final View view, final TestResults results) {
		new java.lang.Thread() {
			@Override
			public void run() {
				final TextView upThroughputTextView = (TextView) view.findViewById(R.id.up_throughput);
				final TextView downThroughputTextView = (TextView) view.findViewById(R.id.down_throughput);
				final TextView bidiThroughputTextView = (TextView) view.findViewById(R.id.bidi_throughput);
				
				try {
					synchronized (results) {
						while (!results.dead) {
							results.wait();
							// process results
							final double upThroughput = results.uplink.bytes / results.uplink.time / 1024.;
							final double downThroughput = results.downlink.bytes / results.downlink.time / 1024.; 
							final double bidiThroughput = results.bidi.bytes / results.bidi.time / 1024.; 
							// update UI
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									upThroughputTextView.setText(String.format("%.2f[KB/s]", upThroughput));
									downThroughputTextView.setText(String.format("%.2f[KB/s]", downThroughput));
									bidiThroughputTextView.setText(String.format("%.2f[KB/s]", bidiThroughput));
								}
							});
						}
					}
				} catch (InterruptedException e) {
				}
			}
		}.start();
	}

	@Override
	protected void onDestroy() {
		manager_.destroy();
		super.onDestroy();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		manager_.restart();
	}

	@Override
	protected void onStart() {
		super.onStart();
		manager_.start();
	}

	@Override
	protected void onStop() {
		manager_.stop();
		super.onStop();
	}

}