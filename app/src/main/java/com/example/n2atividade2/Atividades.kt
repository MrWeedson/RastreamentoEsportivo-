package com.example.n2atividade2

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.n2atividade2.entity.Movimento
import android.provider.Settings
import com.example.n2atividade2.entity.createGpxDocument
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class Atividades : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var spinner: Spinner
    var mSensorManager: SensorManager? = null
    var mAccelerometer: Sensor? = null
    var mLocationManager: LocationManager? = null
    val LOCATION_PERMISSION_REQUEST_CODE = 100

    private val id = 0
    private var accelX = 0.0
    private var accelY = 0.0
    private var accelZ = 0.0
    private var latitude = 0.0
    private var longitude = 0.0
    private val atleta = "claudioneto"
    private var isRecording = false
    private var selectedOption: String = ""
    var movimentos: MutableList<Movimento> = mutableListOf()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startRecordActivityData()
                } else {
                    Toast.makeText(this, "Permissão negada", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.atividades)

        spinner = findViewById(R.id.spnOptions)

        val opcoes = arrayOf("Corrida", "Caminhada", "Pedalada")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opcoes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        mSensorManager!!.flush(this)
        mSensorManager!!.registerListener(this, mAccelerometer, 16666)

        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedOption = parent?.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            startRecordActivity()
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopActivity()
        }

    }

    override fun onResume() {
        super.onResume()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mLocationManager!!.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                0f,
                this
            )
        }
    }

    fun startRecordActivity() {
        isRecording = true

        if (!isGpsEnabled()) {
            showGpsDisabledDialog()
        }

        requestLocationPermission()
    }

    fun stopActivity() {
        stopRecordActivityData()

        val gpxContent: String = createGpxDocument(movimentos, selectedOption)
        println(gpxContent)
    }

    fun startRecordActivityData() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && isRecording
        ) {
            mLocationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                0f,
                this
            )
        }
    }

    fun stopRecordActivityData() {
        isRecording = false
        mLocationManager?.removeUpdates(this)
    }

    private fun requestLocationPermission() {
        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, fineLocationPermission)
            || ActivityCompat.shouldShowRequestPermissionRationale(this, coarseLocationPermission)
        ) {
            Toast.makeText(
                this,
                "Permita o acesso à localização",
                Toast.LENGTH_LONG
            ).show()
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(fineLocationPermission, coarseLocationPermission),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onSensorChanged(p0: SensorEvent) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            accelX = p0.values[0].toString().toDouble()
            accelY = p0.values[1].toString().toDouble()
            accelZ = p0.values[2].toString().toDouble()
        }
    }

    override fun onLocationChanged(p0: Location) {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            latitude = p0.latitude.toString().toDouble()
            longitude = p0.longitude.toString().toDouble()

            var movimento = Movimento(
                id,
                getCurrentDateTime(),
                accelX,
                accelY,
                accelZ,
                latitude,
                longitude,
                atleta
            )

            movimentos.add(movimento)
        }
    }

    fun getCurrentDateTime(): String {
        val currentTime = System.currentTimeMillis()
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
        formatter.timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
        val date = Date(currentTime)
        return formatter.format(date)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    private fun isGpsEnabled(): Boolean {
        return mLocationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    override fun onProviderDisabled(provider: String) {

    }

    private fun showGpsDisabledDialog() {
        AlertDialog.Builder(this)
            .setMessage("O GPS está desligado. Por favor, habilite-o para usar esta função.")
            .setPositiveButton("Habilitar") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(settingsIntent)
            }
            .setNegativeButton("Cancelar") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                // Lidar com a ação de cancelamento, se necessário
                // Por exemplo, fechar a activity ou exibir outra mensagem para o usuário
                Toast.makeText(
                    this,
                    "A função requer GPS. O GPS não está habilitado.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setCancelable(false)
            .show()
    }

}