package com.muumlover.locationmock.util

import com.baidu.mapapi.model.LatLng
import kotlin.math.*

/**火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的互转
 * Created by macremote on 16/5/3.
 */
object GPSUtil {
    var pi = 3.1415926535897932384626
    var x_pi = 3.14159265358979324 * 3000.0 / 180.0
    var a = 6378245.0
    var ee = 0.00669342162296594323

    private fun transformLat(x: Double, y: Double): Double {
        var ret = (-100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y
                + 0.2 * sqrt(abs(x)))
        ret += (20.0 * sin(6.0 * x * pi) + 20.0 * sin(2.0 * x * pi)) * 2.0 / 3.0
        ret += (20.0 * sin(y * pi) + 40.0 * sin(y / 3.0 * pi)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * pi) + 320 * sin(y * pi / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLon(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * pi) + 20.0 * sin(2.0 * x * pi)) * 2.0 / 3.0
        ret += (20.0 * sin(x * pi) + 40.0 * sin(x / 3.0 * pi)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * pi) + 300.0 * sin(x / 30.0 * pi)) * 2.0 / 3.0
        return ret
    }

    private fun transform(point: LatLng): LatLng {
        if (outOfChina(point)) {
            return point
        }
        val lat = point.latitude
        val lon = point.longitude
        var dLat = transformLat(lon - 105.0, lat - 35.0)
        var dLon = transformLon(lon - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * pi
        var magic = sin(radLat)
        magic = 1 - ee * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = dLat * 180.0 / (a * (1 - ee) / (magic * sqrtMagic) * pi)
        dLon = dLon * 180.0 / (a / sqrtMagic * cos(radLat) * pi)
        val latitude = lat + dLat
        val longitude = lon + dLon
        return LatLng(latitude, longitude)
    }

    private fun outOfChina(point: LatLng): Boolean {
        val lat = point.latitude
        val lon = point.longitude
        if (lon < 72.004 || lon > 137.8347)
            return true
        return lat < 0.8293 || lat > 55.8271
    }

    /**
     * 84 to 火星坐标系 (GCJ-02) World Geodetic System ==> Mars Geodetic System
     *
     * @param point
     * @return
     */
    fun gps84ToGcj02(point: LatLng): LatLng {
        if (outOfChina(point)) {
            return point
        }
        val lat = point.latitude
        val lon = point.longitude
        var dLat = transformLat(lon - 105.0, lat - 35.0)
        var dLon = transformLon(lon - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * pi
        var magic = sin(radLat)
        magic = 1 - ee * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = dLat * 180.0 / (a * (1 - ee) / (magic * sqrtMagic) * pi)
        dLon = dLon * 180.0 / (a / sqrtMagic * cos(radLat) * pi)
        val latitude = lat + dLat
        val longitude = lon + dLon
        return LatLng(latitude, longitude)
    }

    /**
     * * 火星坐标系 (GCJ-02) to 84 * * @param lon * @param lat * @return
     */
    fun gcj02ToGps84(point: LatLng): LatLng {
        val lat = point.latitude
        val lon = point.longitude
        val gps = transform(point)
        val latitude = lat * 2 - gps.latitude
        val longitude = lon * 2 - gps.longitude
        return LatLng(latitude, longitude)
    }

    /**
     * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法 将 GCJ-02 坐标转换成 BD-09 坐标
     *
     * @param point
     *
     */
    fun gcj02_To_Bd09(point: LatLng): LatLng {
        val lat = point.latitude
        val lon = point.longitude
        val z = sqrt(lon * lon + lat * lat) + 0.00002 * sin(lat * x_pi)
        val theta = atan2(lat, lon) + 0.000003 * cos(lon * x_pi)
        val latitude = z * sin(theta) + 0.006
        val longitude = z * cos(theta) + 0.0065
        return LatLng(latitude, longitude)
    }

    /**
     * * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法 * * 将 BD-09 坐标转换成GCJ-02 坐标 * * @param
     * bd_lat * @param bd_lon * @return
     */
    fun bd09ToGcj02(point: LatLng): LatLng {
        val lat = point.latitude
        val lon = point.longitude
        val x = lon - 0.0065
        val y = lat - 0.006
        val z = sqrt(x * x + y * y) - 0.00002 * sin(y * x_pi)
        val theta = atan2(y, x) - 0.000003 * cos(x * x_pi)
        val latitude = z * sin(theta)
        val longitude = z * cos(theta)
        return LatLng(latitude, longitude)
    }

    /**将gps84转为bd09
     * @param point
     *
     * @return
     */
    fun gps84ToBd09(point: LatLng): LatLng {
        val gcj02 = gps84ToGcj02(point)
        return gcj02_To_Bd09(gcj02)
    }

    fun bd09ToGps84(point: LatLng): LatLng {
        val gcj02 = bd09ToGcj02(point)
        val gps84 = gcj02ToGps84(gcj02)
        //保留小数点后六位
        val latitude = retain6(gps84.latitude)
        val longitude = retain6(gps84.longitude)
        return LatLng(latitude, longitude)
    }

    /**保留小数点后六位
     * @param num
     * @return
     */
    private fun retain6(num: Double): Double {
        val result = String.format("%.6f", num)
        return java.lang.Double.valueOf(result)
    }

}