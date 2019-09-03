package com.muumlover.locationmock

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.CoordType
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.core.SearchResult
import com.baidu.mapapi.search.geocode.*
import com.google.android.material.snackbar.Snackbar
import com.muumlover.locationmock.util.GPSUtil.gcj02ToGps84
import com.muumlover.locationmock.util.GPSUtil.gps84ToGcj02
import kotlinx.android.synthetic.main.activity_add.*


class AddActivity : AppCompatActivity() {

    private var mOriginPoint: LatLng? = null

    private var mLocationClient: LocationClient? = null
    private var mSelectPoint: LatLng? = null
    private var mSelectAddress: String = ""
    private var mSelectCity: String = ""
    private var mSelectCountry: String = ""

    var mCoder: GeoCoder? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mOriginPoint = gps84ToGcj02(this.intent.getParcelableExtra("location") as LatLng)
        Log.i("sys call", "baidu onCreate $mOriginPoint")

        SDKInitializer.initialize(applicationContext)
        SDKInitializer.setCoordType(CoordType.GCJ02)

        setContentView(R.layout.activity_add)

        //禁用缩放按钮
        mMapView.showZoomControls(false)
        //禁用俯视（3D）功能
        mMapView.map.uiSettings.isOverlookingGesturesEnabled = false
        //禁用地图旋转功能
        mMapView.map.uiSettings.isRotateGesturesEnabled = false

        initMapListener()
        initLocationOption()

        btnCenter.setOnClickListener {
            updateMapState(this.mSelectPoint!!)
//            updateMapState(mMapView.map.mapStatus.target)
//            markMap(mMapView.map.mapStatus.target)
        }

        btnLocation.setOnClickListener {
            //开始定位
            when {
                mLocationClient == null -> initLocationOption()
                mLocationClient!!.isStarted -> mLocationClient!!.requestLocation()
                else -> mLocationClient!!.start()
            }
        }

        btnConfirm.setOnClickListener {
            if (mSelectPoint == null) {
                Snackbar.make(addView, "请选择一个位置", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            } else {
                val data = Intent()
                data.putExtra("location", gcj02ToGps84(mSelectPoint!!))
                data.putExtra("address", mSelectAddress)
                data.putExtra("city", mSelectCity)
                data.putExtra("country", mSelectCountry)
                setResult(RESULT_OK, data)
                finish()
            }
        }
    }


    /**
     * 初始化地图组件事件
     */
    private fun initMapListener() {
        mMapView.map.isMyLocationEnabled = true
        mMapView.map.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
            override fun onMapClick(point: LatLng) {
                updateMapState(point)
            }

            override fun onMapPoiClick(poi: MapPoi): Boolean {
                updateMapState(poi)
                return true
            }
        })
    }

    /**
     * 初始化定位参数配置
     */
    private fun initLocationOption() {
        //定位服务的客户端。宿主程序在客户端声明此类，并调用，目前只支持在主线程中启动
        mLocationClient = LocationClient(applicationContext)
        //声明LocationClient类实例并配置定位参数
        val locationOption = LocationClientOption()
        val myLocationListener = MyLocationListener()
        //注册监听函数
        mLocationClient!!.registerLocationListener(myLocationListener)
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        locationOption.locationMode = LocationClientOption.LocationMode.Hight_Accuracy
        //可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
        locationOption.setCoorType("gcj02")
        //可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的
        locationOption.setScanSpan(1000)
        //可选，设置是否需要地址信息，默认不需要
        locationOption.setIsNeedAddress(true)
        //可选，设置是否需要地址描述
        locationOption.setIsNeedLocationDescribe(true)
        //可选，设置是否需要设备方向结果
        locationOption.setNeedDeviceDirect(false)
        //可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        locationOption.isLocationNotify = true
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        locationOption.setIgnoreKillProcess(true)
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        locationOption.setIsNeedLocationDescribe(true)
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        locationOption.setIsNeedLocationPoiList(true)
        //可选，默认false，设置是否收集CRASH信息，默认收集
        locationOption.SetIgnoreCacheException(false)
        //可选，默认false，设置是否开启Gps定位
        locationOption.isOpenGps = true
        //可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        locationOption.setIsNeedAltitude(false)
        //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者，该模式下开发者无需再关心定位间隔是多少，定位SDK本身发现位置变化就会及时回调给开发者
        locationOption.setOpenAutoNotifyMode()
        //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者
        locationOption.setOpenAutoNotifyMode(3000, 1, LocationClientOption.LOC_SENSITIVITY_HIGHT)
        //需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
        mLocationClient!!.locOption = locationOption

        if (mOriginPoint == null)
            mLocationClient!!.start() //开始定位
        else
            updateMapState(mOriginPoint!!)

    }

    /**
     * 实现定位回调
     */
    inner class MyLocationListener : BDAbstractLocationListener() {
        override fun onReceiveLocation(bdLocation: BDLocation) {
            //开始定位
            mLocationClient!!.stop()

            //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
            //以下只列举部分获取经纬度相关（常用）的结果信息
            //更多结果信息获取说明，请参照类参考中BDLocation类中的说明

            //获取纬度信息
            val latitude = bdLocation.latitude
            //获取经度信息
            val longitude = bdLocation.longitude
            //获取定位精度，默认值为0.0f
            val radius = bdLocation.radius
            //获取经纬度坐标类型，以LocationClientOption中设置过的坐标类型为准
            val coorType = bdLocation.coorType
            //获取定位类型、定位错误返回码，具体信息可参照类参考中BDLocation类中的说明
            val errorCode = bdLocation.locType

            //定位数据-->将bdlocation中的信息转到MyLocationData中
            val data = MyLocationData.Builder()//
                .accuracy(bdLocation.radius)
                .latitude(bdLocation.latitude)
                .longitude(bdLocation.longitude)
                .build()

            //添加定位信息
            mMapView.map.setMyLocationData(data)

            //将地图中心定义到当前位置
            updateMapState(bdLocation)
        }
    }


    private fun updateMapState(point: LatLng) {
        mSelectPoint = point
        if (mCoder == null) {
            mCoder = GeoCoder.newInstance()
            mCoder!!.setOnGetGeoCodeResultListener(GeoCoderResultListener())
        }
        mCoder!!.reverseGeoCode(
            ReverseGeoCodeOption()
                .location(point)
                // POI召回半径，允许设置区间为0-1000米，超过1000米按1000米召回。默认值为1000
                .radius(100)
        )
        setCenter(point)
        markMap(point)
    }

    private fun updateMapState(poi: MapPoi) {
        updateMapState(poi.position)
    }

    private fun updateMapState(bdLocation: BDLocation) {
        val point = LatLng(bdLocation.latitude, bdLocation.longitude)
        mSelectPoint = point
        mSelectAddress = bdLocation.address.address
        mSelectCountry = bdLocation.address.country
        mSelectCity = bdLocation.address.city
        setCenter(point)
        markMap(point)
        showNotice()
    }

    inner class GeoCoderResultListener : OnGetGeoCoderResultListener {
        override fun onGetGeoCodeResult(p0: GeoCodeResult?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onGetReverseGeoCodeResult(reverseGeoCodeResult: ReverseGeoCodeResult) {
            if (reverseGeoCodeResult.error == SearchResult.ERRORNO.NO_ERROR) {
                //详细地址
                mSelectAddress = reverseGeoCodeResult.address
                //行政区号
                val adCode = reverseGeoCodeResult.cityCode
                showNotice()
            }
        }
    }

    private fun showNotice() {
        val info = String.format(
            "longitude : %f latitude : %f\n%s",
            mSelectPoint?.longitude,
            mSelectPoint?.latitude,
            mSelectAddress
        )
        Snackbar.make(addView, info, Snackbar.LENGTH_INDEFINITE)
            .setAction("Action", null).show()
    }

    private fun setCenter(point: LatLng) {
        val mMapStatus = MapStatus.Builder()
            .target(point)
            .build()
        val mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus)
        mMapView.map.setMapStatus(mMapStatusUpdate)
    }

    private fun markMap(point: LatLng) {
        Log.i("test", "mark:" + point.latitude + ":" + point.longitude)
        //清空现有标记
        mMapView.map.clear()
        //构建Marker图标
        val bitmap = BitmapDescriptorFactory
            .fromResource(R.drawable.ic_mark)
        //构建MarkerOption，用于在地图上添加Marker
        val option = MarkerOptions()
            .position(point)
            .icon(bitmap)
        //在地图上添加Marker，并显示
        mMapView.map.addOverlay(option)
    }

    override fun onResume() {
        super.onResume()
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy()
    }

}
