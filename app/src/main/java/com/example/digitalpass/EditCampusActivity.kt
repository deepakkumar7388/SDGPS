package com.example.digitalpass

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import okhttp3.ResponseBody
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos

class EditCampusActivity : BaseActivity() {

    private lateinit var mapView: MapView
    private lateinit var campusSpinner: Spinner
    private lateinit var newCampusEditText: EditText
    private lateinit var latEditText: EditText
    private lateinit var lonEditText: EditText
    private lateinit var moveMapButton: MaterialButton
    private lateinit var radiusEditText: EditText
    private lateinit var saveButton: MaterialButton
    private lateinit var customProgressBar: CustomProgressBar

    private var centerMarker: Marker? = null
    private var radiusMarker: Marker? = null
    private var circlePolygon: Polygon? = null

    private var currentCenter: GeoPoint? = null
    private var currentRadius: Double = 50.0

    private val campusList = ArrayList<String>()
    private val campusDataList = ArrayList<HashMap<String, Any>>()

    private var isProgrammaticUpdate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize osmdroid configuration
        val ctx: Context = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        
        setContentView(R.layout.activity_edit_campus)

        mapView = findViewById(R.id.mapView)
        campusSpinner = findViewById(R.id.campusSpinner)
        newCampusEditText = findViewById(R.id.newCampusEditText)
        latEditText = findViewById(R.id.latEditText)
        lonEditText = findViewById(R.id.lonEditText)
        moveMapButton = findViewById(R.id.moveMapButton)
        radiusEditText = findViewById(R.id.radiusEditText)
        saveButton = findViewById(R.id.saveButton)
        customProgressBar = findViewById(R.id.customProgressBar)

        // Setup toolbar back button
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        setupMap()
        setupListeners()
        fetchCampusLocations()
    }

    private fun setupMap() {
        mapView.setMultiTouchControls(true)
        val mapController = mapView.controller
        mapController.setZoom(16.0)
        // Default center
        val startPoint = GeoPoint(23.182763, 77.302432) // Bhopal, will move if data fetched
        mapController.setCenter(startPoint)

        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    setCenterPoint(p)
                    return true
                }
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        }
        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        mapView.overlays.add(0, mapEventsOverlay)
    }

    private fun setCenterPoint(p: GeoPoint) {
        currentCenter = p
        
        if (centerMarker == null) {
            centerMarker = Marker(mapView)
            centerMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(centerMarker)
        }
        centerMarker?.position = p

        latEditText.setText(String.format(Locale.US, "%.6f", p.latitude))
        lonEditText.setText(String.format(Locale.US, "%.6f", p.longitude))

        updateCircle()
        updateRadiusMarker()
        mapView.invalidate()
    }

    private fun updateCircle() {
        val center = currentCenter ?: return

        if (circlePolygon == null) {
            circlePolygon = Polygon(mapView)
            circlePolygon?.fillPaint?.color = Color.parseColor("#33052E92") // Semi-transparent blue
            circlePolygon?.outlinePaint?.color = Color.parseColor("#052E92")
            circlePolygon?.outlinePaint?.strokeWidth = 3f
            mapView.overlays.add(circlePolygon)
        }

        val circlePoints = Polygon.pointsAsCircle(center, currentRadius)
        circlePolygon?.points = circlePoints
        mapView.invalidate()
    }

    private fun updateRadiusMarker() {
        val center = currentCenter ?: return

        if (radiusMarker == null) {
            radiusMarker = Marker(mapView)
            radiusMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            // Use a simple icon or let it be default
            radiusMarker?.isDraggable = true
            radiusMarker?.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDragStart(marker: Marker?) {}

                override fun onMarkerDrag(marker: Marker?) {
                    marker?.let {
                        val distance = center.distanceToAsDouble(it.position)
                        currentRadius = distance
                        isProgrammaticUpdate = true
                        radiusEditText.setText(String.format(Locale.getDefault(), "%.1f", distance))
                        isProgrammaticUpdate = false
                        updateCircle()
                    }
                }

                override fun onMarkerDragEnd(marker: Marker?) {
                    updateCircle()
                }
            })
            mapView.overlays.add(radiusMarker)
        }
        
        // Place the radius marker exactly due East of the center to start
        val earthRadius = 6378137.0
        val lat = center.latitude
        val lon = center.longitude
        val newLon = lon + (currentRadius / earthRadius) * (180 / PI) / cos(lat * PI / 180)
        radiusMarker?.position = GeoPoint(lat, newLon)
    }

    private fun setupListeners() {
        radiusEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isProgrammaticUpdate && !s.isNullOrEmpty()) {
                    try {
                        currentRadius = s.toString().toDouble()
                        updateCircle()
                        updateRadiusMarker()
                        mapView.invalidate()
                    } catch (e: NumberFormatException) {

                    }
                }
            }
        })

        campusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == campusList.size - 1) { // "Add New Campus" selected
                    newCampusEditText.visibility = View.VISIBLE
                    currentCenter = null
                    centerMarker?.let { mapView.overlays.remove(it) }
                    centerMarker = null
                    radiusMarker?.let { mapView.overlays.remove(it) }
                    radiusMarker = null
                    circlePolygon?.let { mapView.overlays.remove(it) }
                    circlePolygon = null
                    latEditText.setText("")
                    lonEditText.setText("")
                    mapView.invalidate()
                } else if (position > 0) { // Existing campus selected
                    newCampusEditText.visibility = View.GONE
                    val data = campusDataList[position - 1]
                    val lat = data["latitude"].toString().toDoubleOrNull() ?: 23.182964
                    val lon = data["longitude"].toString().toDoubleOrNull() ?: 77.302434
                    val radius = data["radius"].toString().toDoubleOrNull() ?: 50.0

                    currentRadius = radius
                    isProgrammaticUpdate = true
                    radiusEditText.setText(radius.toString())
                    isProgrammaticUpdate = false

                    val point = GeoPoint(lat, lon)
                    mapView.controller.animateTo(point)
                    setCenterPoint(point)
                } else { // "Select Campus"
                    newCampusEditText.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        saveButton.setOnClickListener {
            saveCampusLocation()
        }

        moveMapButton.setOnClickListener {
            val latStr = latEditText.text.toString()
            val lonStr = lonEditText.text.toString()
            val lat = latStr.toDoubleOrNull()
            val lon = lonStr.toDoubleOrNull()
            
            if (lat != null && lon != null) {
                val point = GeoPoint(lat, lon)
                mapView.controller.animateTo(point)
                setCenterPoint(point)
            } else {
                Toast.makeText(this@EditCampusActivity, "Please enter valid latitude and longitude", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchCampusLocations() {
        customProgressBar.visibility = View.VISIBLE
        val token = LoginUserDataHolder.token

        val call = RetrofitClient.instance.getCampusLocation(token)
        call.enqueue(object : Callback<ArrayList<HashMap<String, Any>>> {
            override fun onResponse(
                call: Call<ArrayList<HashMap<String, Any>>>,
                response: Response<ArrayList<HashMap<String, Any>>>
            ) {
                customProgressBar.visibility = View.GONE
                campusList.clear()
                campusDataList.clear()
                
                campusList.add("Select Campus")
                
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!
                    for (item in list) {
                        val name = item["campus"]?.toString() ?: ""
                        if (name.isNotEmpty()) {
                            campusList.add(name)
                            campusDataList.add(item)
                        }
                    }
                }
                
                campusList.add("Add New Campus")
                
                val adapter = ArrayAdapter(this@EditCampusActivity, android.R.layout.simple_spinner_item, campusList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                campusSpinner.adapter = adapter
            }

            override fun onFailure(call: Call<ArrayList<HashMap<String, Any>>>, t: Throwable) {
                customProgressBar.visibility = View.GONE
                Toast.makeText(this@EditCampusActivity, "Failed to fetch campuses: ${t.message}", Toast.LENGTH_SHORT).show()
                
                // Set fallback values
                campusList.add("Select Campus")
                campusList.add("Add New Campus")
                val adapter = ArrayAdapter(this@EditCampusActivity, android.R.layout.simple_spinner_item, campusList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                campusSpinner.adapter = adapter
            }
        })
    }

    private fun saveCampusLocation() {
        val selectedPos = campusSpinner.selectedItemPosition
        if (selectedPos == 0) {
            Toast.makeText(this, "Please select or add a campus", Toast.LENGTH_SHORT).show()
            return
        }

        val center = currentCenter
        if (center == null) {
            Toast.makeText(this, "Please tap on the map to set a location", Toast.LENGTH_SHORT).show()
            return
        }

        var isNewCampus = false
        val campusName: String

        if (selectedPos == campusList.size - 1) {
            isNewCampus = true
            val name = newCampusEditText.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter new campus name", Toast.LENGTH_SHORT).show()
                return
            }
            campusName = name
        } else {
            campusName = campusList[selectedPos]
        }

        customProgressBar.visibility = View.VISIBLE
        val data = HashMap<String, Any>()
        data["token"] = LoginUserDataHolder.token
        data["campus"] = campusName
        data["latitude"] = center.latitude
        data["longitude"] = center.longitude
        data["radius"] = currentRadius

        val call = if (isNewCampus) {
            RetrofitClient.instance.createCampusLocation(data)
        } else {
            RetrofitClient.instance.saveCampusLocation(data)
        }

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                customProgressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@EditCampusActivity, "Location saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EditCampusActivity, "Failed to save: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                customProgressBar.visibility = View.GONE
                Toast.makeText(this@EditCampusActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
