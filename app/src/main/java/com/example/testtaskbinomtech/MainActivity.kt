package com.example.testtaskbinomtech

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.testtaskbinomtech.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import de.hdodenhof.circleimageview.CircleImageView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var locationManager: LocationManager
    private val appName = "BinomTech"
    private val appVersion = "1.0"
    private val requestedLocationPermissions = 1
    private var currentMarkerIndex = 0
    private val childrenInfoList = listOf(
        ChildrenLocationInfo(
            "Илья",
            55.19722852742109,
            61.34997831764827,
            R.drawable._daed9e19578a7b917c383110971b403_1,
            "GPS",
            "02.11.23",
            "13:00"
        ),
        ChildrenLocationInfo(
            "Варвара",
            55.199815358287964,
            61.33487456598302,
            R.drawable.hero_article_enuk_baby_sunglasses_678x448_compressor_1,
            "WiFi",
            "01.03.23",
            "19:00"
        )
    )
    private val childrenMarkers = mutableListOf<Marker>()

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            binding.mapview.controller.animateTo(geoPoint)
            binding.mapview.invalidate()
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            Configuration.getInstance().userAgentValue = "$appName / $appVersion"
            mapview.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
            mapview.controller.setZoom(15.0)
            mapview.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    requestedLocationPermissions)
            } else {
                initLocationOverlay()
            }
        }
        initBottomSheet()
    }

    /**
     * Инициализация BottomSheet
     */
    private fun initBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.bottomSheetRoot).apply {
            state = BottomSheetBehavior.STATE_HIDDEN
            isHideable = true
        }
    }

    /**
     * Отображение информации о ребенке на BottomSheet
     */
    private fun showBottomSheet(childInfo: ChildrenLocationInfo) {
        binding.bottomSheet.root.apply {
            binding.bottomSheet.apply {
                val image = DrawableCompat.wrap(ContextCompat.getDrawable(this@MainActivity, childInfo.image)!!)
                nameChild.text = childInfo.name
                childImage.setImageDrawable(image)
                tvCompound.text = childInfo.compound
                tvDate.text = childInfo.date
                tvTime.text = childInfo.time
            }
            visibility = View.VISIBLE
        }
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /**
     * Инициализация наложения местоположения
     */
    private fun initLocationOverlay() {

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), binding.mapview)
        myLocationOverlay.enableMyLocation()

        val customDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(this, R.drawable.ic_my_tracker_46dp)!!)
        val customBitmap = Bitmap.createBitmap(customDrawable.intrinsicWidth, customDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(customBitmap)
        customDrawable.setBounds(0, 0, canvas.width, canvas.height)
        customDrawable.draw(canvas)

        myLocationOverlay.setPersonIcon(customBitmap)
        myLocationOverlay.setDirectionIcon(customBitmap)

        binding.mapview.overlays.add(myLocationOverlay)
        childrenInfoList.forEach { item ->
            addCustomMarker(item)
        }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val provider = locationManager.getBestProvider(Criteria(), true)
        if (provider != null) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            locationManager.requestLocationUpdates(provider, 5000L, 10f, locationListener)
            setupButtons()
        }
    }

    /**
     * Создание маркера на карте для отображения местоположения ребенка
     */
    private fun addCustomMarker(item: ChildrenLocationInfo) {
        val customMarker = Marker(binding.mapview)
        customMarker.position = GeoPoint(item.latitude, item.longitude)
        customMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        customMarker.icon = BitmapDrawable(resources, createCustomMarkerBitmap(item))
        customMarker.title = "${item.name}\n${item.compound}, ${item.time}"
        customMarker.setOnMarkerClickListener { marker, _ ->
            val childInfo = childrenInfoList.find { it.name == marker.title.split("\n")[0] }
            if (childInfo != null) {
                customMarker.showInfoWindow()
                showBottomSheet(childInfo)
            }
            true
        }
        binding.mapview.overlays.add(customMarker)
        childrenMarkers.add(customMarker)
    }

    /**
     * Создание кастомного маркера из View
     */
    @SuppressLint("MissingInflatedId")
    private fun createCustomMarkerBitmap(item: ChildrenLocationInfo): Bitmap {
        val customMarkerView = LayoutInflater.from(this).inflate(R.layout.sample_custom_view_marker, null)

        val imageView1 = customMarkerView.findViewById<ImageView>(R.id.backgroundMarker)
        val imageView2 = customMarkerView.findViewById<CircleImageView>(R.id.foregroundMarker)
        imageView1.setImageResource(R.drawable.ic_tracker_75dp)
        imageView2.setImageResource(item.image)

        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        customMarkerView.layout(0, 0, customMarkerView.measuredWidth, customMarkerView.measuredHeight)
        val bitmap = Bitmap.createBitmap(customMarkerView.measuredWidth, customMarkerView.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        customMarkerView.draw(canvas)
        return bitmap
    }

    /**
     * Инициализация кнопок управления картой
     */
    private fun setupButtons() {
        binding.apply {
            zoomIn.setOnClickListener {
                val zoom = mapview.zoomLevelDouble + 1
                mapview.controller.zoomTo(zoom)
            }
            zoomOut.setOnClickListener {
                val zoom = mapview.zoomLevelDouble - 1
                mapview.controller.zoomTo(zoom)
            }
            myLocation.setOnClickListener {
                myLocationOverlay.let {
                    mapview.controller.animateTo(it.myLocation)
                }
            }
            binding.nextMarker.setOnClickListener {
                if (childrenMarkers.isNotEmpty()) {
                    currentMarkerIndex = (currentMarkerIndex + 1) % childrenMarkers.size
                    val marker = childrenMarkers[currentMarkerIndex]
                    binding.mapview.controller.animateTo(marker.position)
                    marker.showInfoWindow()

                    val childInfo = childrenInfoList[currentMarkerIndex]
                    showBottomSheet(childInfo)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == requestedLocationPermissions) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                initLocationOverlay()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        binding.mapview.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapview.onPause()
    }

    /**
     * Вспомогательный data-класс для хранения информации о местоположении детей
     */
    data class ChildrenLocationInfo(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val image: Int,
        val compound: String,
        val date: String,
        val time: String
    )
}