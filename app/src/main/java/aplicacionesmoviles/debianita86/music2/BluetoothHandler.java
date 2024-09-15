package aplicacionesmoviles.debianita86.music2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

public class BluetoothHandler {
    private static BluetoothAdapter bluetoothAdapter;
    private static BroadcastReceiver bluetoothReceiver;

    public static void setupBluetooth(Context context, ActivityResultLauncher<Intent> launcher) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth no soportado en este dispositivo", Toast.LENGTH_SHORT).show();
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            launcher.launch(enableBtIntent);
        }

        setupReceiver(context);
    }
    private static void setupReceiver(Context context) {
        bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("BluetoothReceiver", "Action received: " + action);

                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    Log.d("BluetoothReceiver", "Device connected");
                    Toast.makeText(context, "Dispositivo Bluetooth conectado", Toast.LENGTH_SHORT).show();
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    Log.d("BluetoothReceiver", "Device disconnected");
                    Toast.makeText(context, "Dispositivo Bluetooth desconectado", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    public static void registerReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        context.registerReceiver(bluetoothReceiver, filter);
    }

    public static void unregisterReceiver(Context context) {
        context.unregisterReceiver(bluetoothReceiver);
    }
}