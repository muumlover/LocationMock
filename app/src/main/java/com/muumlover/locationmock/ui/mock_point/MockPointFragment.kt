package com.muumlover.locationmock.ui.mock_point

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.baidu.mapapi.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.muumlover.locationmock.AddActivity
import com.muumlover.locationmock.R
import com.muumlover.locationmock.delegate.Preference
import com.muumlover.locationmock.delegate.PreferenceRealLocation
import com.muumlover.locationmock.delegate.RealLocation
import com.muumlover.locationmock.mock.ICallback
import com.muumlover.locationmock.mock.MockLocation
import kotlinx.android.synthetic.main.fragment_mock_point.*


class MockPointFragment : Fragment() {

    private lateinit var myGpsService: MockLocation

    private var historyPreference = PreferenceRealLocation("mock_point_history")
    private val history: ArrayList<RealLocation> = historyPreference.getValue()

    private var latitude: Double by Preference("mock_point_latitude", 0.0)
    private var longitude: Double by Preference("mock_point_longitude", 0.0)
    private var address: String by Preference("mock_point_address", "")
    private var city: String by Preference("mock_point_city", "")
    private var country: String by Preference("mock_point_country", "")


    private lateinit var homeViewModel: MockPointViewModel
    private var mSelectPoint: LatLng? = null
    private var mMockStart: Boolean = false

    private var conn: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {

        }

        @SuppressLint("SetTextI18n")
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.e("test", "connected")
            Log.e("test", history.toString())

            text_state.text = "Service connected"
            //返回一个MsgService对象
            myGpsService = (service as MockLocation.MsgBinder).service

            //注册回调接口来接收下载进度的变化
            myGpsService.setCallbackFunc(object : ICallback {
                override fun updateLocationInfo(loc: Location?, info: String) {

                }

                override fun updateLatLngInfo(point: LatLng?, info: String) {
                    if (point != null) {
                        Log.i(
                            "Location",
                            "latitude: " + point.latitude + "\nlongitude: " + point.longitude + " " + info
                        )
                        text_state.text = info
                        //居中，打标记
                        //setCenter(currentPt);
                        //markMap(currentPt);
                    } else {
                        if (info !== "") {
                            if (info === "stop") {
                                //TODO 结束了
                            }
                            Log.i("has a info: ", info)
                            text_state.text = info
                        }
                    }
                }

                override fun updateMockCount(count: Int) {}
            })

            btn_mock.setOnClickListener {
                if (mMockStart) {
                    mMockStart = false
                    myGpsService.stopMock()
                    btn_mock.text = "START MOCK"
                } else {
                    mMockStart = true
                    myGpsService.setLocation(LatLng(latitude, longitude))
                    //检查是不是无休止的来回走下去
                    myGpsService.startMock(true)
                    btn_mock.text = "STOP MOCK"
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProviders.of(this).get(MockPointViewModel::class.java)

        val it = Intent(activity, MockLocation::class.java)
        activity?.startService(it)
        activity?.bindService(it, conn, Context.BIND_AUTO_CREATE)

        val root = inflater.inflate(R.layout.fragment_mock_point, container, false)
        root.findViewById<TextView>(R.id.text_address).text = address
        root.findViewById<TextView>(R.id.text_city).text = city
        root.findViewById<TextView>(R.id.text_country).text = country
        root.findViewById<TextView>(R.id.text_long_lat).text = "$latitude,$longitude"

        root.findViewById<Button>(R.id.btn_mock).setOnClickListener {
            text_state.text = "服务启动未启动"
        }
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val fab: FloatingActionButton = activity!!.findViewById(R.id.btnLocation)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "正在打开页面", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
            val intent = Intent(context, AddActivity::class.java)
            intent.putExtra("location", LatLng(latitude, longitude))
            startActivityForResult(intent, 1)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK || data == null) return
        when (requestCode) {
            1 -> {
                mSelectPoint = data.getParcelableExtra("location") as LatLng
                address = data.getStringExtra("address")
                city = data.getStringExtra("city")
                country = data.getStringExtra("country")
                if (mSelectPoint != null) {
                    latitude = mSelectPoint!!.latitude
                    longitude = mSelectPoint!!.longitude

                    val info = String.format(
                        "latitude : %f longitude : %f \n%s",
                        latitude,
                        longitude,
                        address
                    )

                    text_mock_point.text = info
                    text_address.text = address
                    text_city.text = city
                    text_country.text = country
                    text_long_lat.text = "$latitude,$longitude"

                    val rl = RealLocation()
                    rl.address = address
                    rl.city = city
                    rl.country = country
                    rl.latitude = latitude
                    rl.longitude = longitude
                    history.add(rl)

                    historyPreference.setValue(history)

                    Log.e("test", history.toString())
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.unbindService(conn)
    }
}