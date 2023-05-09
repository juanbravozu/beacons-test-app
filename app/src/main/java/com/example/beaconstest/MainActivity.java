package com.example.beaconstest;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.InternalBeaconConsumer;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;

// Beacons library used https://altbeacon.github.io/android-beacon-library/index.html
public class MainActivity extends AppCompatActivity implements InternalBeaconConsumer {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final long SCAN_PERIOD_MS = 6000L;
    private static final String ALL_BEACONS_REGION = "AllBeaconsRegion";

    // Permite interactuar con los beacons desde una actividad
    private BeaconManager beaconManager;

    // Criterios de busqueda para los beacons
    private Region region;

    private Button startBtn;
    private Button stopBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startBtn = findViewById(R.id.start_button);
        stopBtn = findViewById(R.id.stop_button);

        // Obtiene la instancia del singleton de BeaconManager
        beaconManager = BeaconManager.getInstanceForApplication(this);

        /*
          Fijar un protocolo de beacons, en este caso se fija Eddystone
          que es el protocolo para beacons creado por Google
        */
        beaconManager
            .getBeaconParsers()
            .add(
                new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT)
            );

        // ArrayList vacio para poner los identificadores de los beacons disponibles
        ArrayList<Identifier> identifiers = new ArrayList<>();

        region = new Region(ALL_BEACONS_REGION, identifiers);

        startBtn.setOnClickListener(v -> {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                askForLocationPermissions();
            } else {
                prepareBeaconDetection();
            }
        });

        stopBtn.setOnClickListener(v ->
            stopDetectingBeacons()
        );
    }

    private void askForLocationPermissions() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Acceso a localización");
        builder.setMessage("Por favor conceda permisos de localización para que la aplicación pueda detectar beacons");
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(dialog ->
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION)
        );
        builder.show();
    }

    private void prepareBeaconDetection() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else if (bluetoothAdapter.isEnabled()) {
            startDetectingBeacons();
        } else {
            // Pedir al usuario que active el Bluetooth
            Toast.makeText(this, "Para continuar activa el bluetooth", Toast.LENGTH_LONG).show();
        }
    }

    private void startDetectingBeacons() {
        // Fijar periodo de escaneo
        beaconManager.setForegroundBetweenScanPeriod(SCAN_PERIOD_MS);

        // Enlazar con el servicio de Beacons
        beaconManager.bindInternal(this);

        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
    }

    private void stopDetectingBeacons() {
        // Detener detección de Beacons en la region establecida
        beaconManager.stopRangingBeacons(region);
        Toast.makeText(this, "Deteniendo detección de Beacons", Toast.LENGTH_SHORT).show();

        // Quitar el notifier
        beaconManager.removeAllRangeNotifiers();
        // Desenlazar con el servicio de Beacons
        beaconManager.unbindInternal(this);

        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
    }

    @Override
    public void onBeaconServiceConnect() {
        // Empezar a buscar Beacons de acuerdo a la region establecida
        beaconManager.startRangingBeacons(region);
        Toast.makeText(this, "Empezando a buscar Beacons", Toast.LENGTH_LONG).show();

        // Notifier permite acceder a lo que se detecte
        beaconManager.addRangeNotifier((beacons, region1) -> {
            if (beacons.size() == 0) {
                /*
                    Cada vez que hace un escaneo, se ejecuta este código si no encuentra Beacons.
                    Para este caso particular, notificar al usuario podría resultar invasivo en la experiencia.
                */
                Toast.makeText(this, "No se encontró ningún beacon cerca", Toast.LENGTH_SHORT).show();
            } else {
                for (Beacon beacon : beacons) {
                    /*
                        Acceder a los datos del Beacon, documentación del objeto Beacon:
                        https://altbeacon.github.io/android-beacon-library/javadoc/org/altbeacon/beacon/Beacon.html
                    */
                    Toast.makeText(this, "Se detectó un Beacon a:" + beacon.getDistance() + "metros", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}