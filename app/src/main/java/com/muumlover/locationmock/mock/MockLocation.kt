package com.muumlover.locationmock.mock


import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.LocationProvider
import android.os.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.route.WalkingRouteLine
import com.baidu.mapapi.utils.DistanceUtil
import com.muumlover.locationmock.MainActivity
import com.muumlover.locationmock.R
import java.util.*

//import android.support.v7.app.ActionBarActivity;

class MockLocation : Service(), Runnable {
    //timer handle
    internal val taskHandle = Handler()

    //location updates, callback to do sth.
    lateinit var callback: ICallback

    private var runFlag = java.lang.Boolean.FALSE
    private var mockForever = java.lang.Boolean.FALSE
    private var interval = 10000.0
    //4公里每小时，不做配置了
    private var speed = 4.0
    private var currIndex = 0
    private var totalCount = 0

    private val activity: AppCompatActivity? = null
    private val listPoints = ArrayList<LatLng>()

    private var locationManager: LocationManager? = null
    private var notificationManager: NotificationManager? = null

    fun setCallbackFunc(callback: ICallback) {
        this.callback = callback
    }

    override fun onCreate() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Display a notification about us starting.    We put an icon in the status bar.
        showNotification()
        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        super.onCreate()
    }

    /**
     * Show a notification while this service is running.
     */
    private fun showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        val text = "I'm running..."
        val CHANNEL_ID = "location_mock" //通道渠道id
        val CHANEL_NAME = "正在运行" //通道渠道名称
        val channel: NotificationChannel
        var notification: Notification? = null
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel =
                NotificationChannel(CHANNEL_ID, CHANEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
            //            new Intent(this, MainActivity.class)

            val intent = Intent()
                .setClass(this, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val contentIntent = PendingIntent.getActivity(this, 0, intent, 0)

            notification = Notification.Builder(this, CHANNEL_ID)
                .setAutoCancel(false)// 点击不自动消失
                .setColor(Color.parseColor("#FEDA26"))
                .setContentTitle("Location Mock")
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.ic_icon))
                .setSmallIcon(R.drawable.ic_icon_small)
                .setWhen(System.currentTimeMillis())
                .build()
            notification!!.flags = notification.flags or Notification.FLAG_NO_CLEAR
        }
        notificationManager.notify(244, notification)
    }


    fun setRouteLine(routeline: WalkingRouteLine, sp: Double) {
        //baidu api
        speed = sp
        val totalDistance = routeline.distance.toDouble()
        this.callback.updateLocationInfo(null, "路程全程 $totalDistance 米")
        for (i in routeline.allStep) {
            //Log.i("WalkingStep","^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            for (j in i.wayPoints) {
                //Log.i("latLng", "lat:"+Double.toString(j.latitude)+" lng:"+Double.toString(j.longitude));
                //add
                listPoints.add(j)
                totalCount += 1
            }
        }
    }

    fun setRouteLineManual(routeList: List<LatLng>, sp: Double) {
        listPoints.addAll(routeList)
        totalCount = listPoints.size
        speed = sp
        this.callback.updateLocationInfo(null, "手工路程全程 $totalCount 个点")
    }

    fun setLocation(location: LatLng) {
        listPoints.clear()
        listPoints.add(location)
        totalCount = 1
        this.callback.updateLocationInfo(null, "手工路程全程 $totalCount 个点")
    }

    @SuppressLint("NewApi")
    fun mockLocation(point: LatLng) {
        try {

            Log.i("Mock", "latitude: " + point.latitude + "\nlongitude: " + point.longitude)
            val newLocation = Location(LocationManager.GPS_PROVIDER)
            newLocation.latitude = point.latitude // 纬度（度）
            newLocation.longitude = point.longitude // 经度（度）
            newLocation.altitude = 30.0  // 高程（米）
            newLocation.bearing = 180f  // 方向（度）
            newLocation.speed = 0f  // 速度（米/秒）
            newLocation.accuracy = Criteria.ACCURACY_FINE.toFloat() // 精度（米）
            newLocation.time = System.currentTimeMillis() // 本地时间
            newLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos() // 实时运行时间

            locationManager!!.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
            locationManager!!.setTestProviderStatus(
                LocationManager.GPS_PROVIDER,
                LocationProvider.AVAILABLE,
                null,
                System.currentTimeMillis()
            )
            //set location at last
            locationManager!!.setTestProviderLocation(LocationManager.GPS_PROVIDER, newLocation)
        } catch (e: Exception) {
            // 防止用户在软件运行过程中关闭模拟位置或选择其他应用
            stopMock()
        }

    }

    fun startMock(foreverFlag: Boolean) {
        try {
            if (locationManager == null) {
                locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            }

            //if has , remove first
            //            if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
            //                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            //            }

            if (listPoints.size <= 0) {
                //无路径，不走路
                callback.updateLatLngInfo(null, "无路径，不走路")
                return
            }
            //赋值，是不是一直走下去
            mockForever = foreverFlag
            val provider = locationManager!!.getProvider(LocationManager.GPS_PROVIDER)

            if (provider != null) {
                locationManager!!.addTestProvider(
                    provider.name,
                    provider.requiresNetwork(),
                    provider.requiresSatellite(),
                    provider.requiresCell(),
                    provider.hasMonetaryCost(),
                    provider.supportsAltitude(),
                    provider.supportsSpeed(),
                    provider.supportsBearing(),
                    provider.powerRequirement,
                    provider.accuracy
                )
            } else {
                locationManager!!.addTestProvider(
                    LocationManager.GPS_PROVIDER,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    android.location.Criteria.POWER_LOW,
                    android.location.Criteria.ACCURACY_FINE
                )
            }

            //start handle runable
            taskHandle.postDelayed(this, 2)
            runFlag = java.lang.Boolean.TRUE
        } catch (ex: Exception) {
            Log.e("StartMock", ex.message)
            this.callback.updateLocationInfo(null, "ERROR: " + ex.message)
            runFlag = java.lang.Boolean.FALSE
        }

    }

    fun stopMock() {
        try {
            callback.updateLatLngInfo(null, "stop")
            if (locationManager == null) {
                locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            }
            if (locationManager!!.getProvider(LocationManager.GPS_PROVIDER) != null) {
                taskHandle.removeCallbacks(this)
                locationManager!!.clearTestProviderLocation(LocationManager.GPS_PROVIDER)
                locationManager!!.clearTestProviderEnabled(LocationManager.GPS_PROVIDER)
                locationManager!!.removeTestProvider(LocationManager.GPS_PROVIDER)
                this.callback.updateLocationInfo(null, "Stop mocking location!")
            }
            //            locationManager = null;
            runFlag = java.lang.Boolean.FALSE
        } catch (ex: Exception) {
            Log.e("StopMock", ex.message)
            this.callback.updateLocationInfo(null, "ERROR:" + ex.message)
            runFlag = java.lang.Boolean.FALSE
        }

    }

    override fun run() {
        //计算当前点到下一点的距离
        if (currIndex < totalCount - 1) {
            //计算p1、p2两点之间的直线距离，单位：米
            val curDistance = DistanceUtil.getDistance(
                listPoints[currIndex],
                listPoints[currIndex + 1]
            )
            interval = curDistance / speed * 3600//米，公里每小时, 微秒
            //Log.i("sleep","下一点距离现在"+curDistance+"米，休眠"+interval+"微秒");
            callback.updateLatLngInfo(
                listPoints[currIndex],
                "下一点距离现在" + curDistance + "米，休眠" + interval + "微秒"
            )
            //interval = 3000;
        } else if (currIndex == totalCount - 1) {
            //最后一个了，随便休眠了10秒
            interval = 1000.0
            callback.updateLatLngInfo(null, "目的马上到了，休眠最后一次1秒")
        } else {
            //如果一直走，就往回走
            if (mockForever) {
                //翻转集合，往回走
                Collections.reverse(listPoints)
                currIndex = 0
                callback.updateLatLngInfo(null, "回头走，开始")
            } else {
                //结束了，stop
                this.stopMock()
                callback.updateLatLngInfo(null, "走路结束")
                return
            }
        }

        taskHandle.postDelayed(this, Math.round(interval))
        //callback.updatecurrIndex(currIndex);
        mockLocation(listPoints[currIndex])
        currIndex = currIndex + 1
    }


    inner class MsgBinder : Binder() {
        //获取当前实例
        val service: MockLocation
            get() = this@MockLocation
    }

    override fun onBind(it: Intent): IBinder? {
        Log.e("service", "onBind")
        return MsgBinder()
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.e("service", "onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.e("service", "onDestroy")
        notificationManager!!.cancelAll()
        if (runFlag != java.lang.Boolean.FALSE) {
            this.stopMock()
        }
        super.onDestroy()
    }
}
