package com.shiyan.flutter.basemapview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.TextureMapView;

import io.flutter.plugin.common.MethodChannel.Result;

import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.model.TileOverlay;
import com.amap.api.maps.model.TileOverlayOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import io.flutter.plugin.common.MethodChannel;

/**
 * Created by shiyan on 2019/3/6
 * dsec:
 */
public class ASMapView extends TextureMapView {

    private UiSettings mUiSettings;

    private LatLng latLng;

    //飞机marker
    private ArrayList<Marker> markers = new ArrayList<>();

    //圆形
    private Circle circle;

    //线段
    private Polyline polyline;

    //多边形
    private Polygon polygon;

    //组件key
    private String key;

    //逆地理编码功能
    private GeocodeSearch geocodeSearch;

    //屏幕中间图标
    private Marker marker;

    //禁飞区图层
    private TileOverlay jfqOverlay;

    //危险区图层
    private TileOverlay wxqOverlay;

    //限制区图层
    private TileOverlay xzqOverlay;

    //机场图层
    private TileOverlay airportOverlay;

    //固定飞场图层
    private TileOverlay gdfcOverlay;

    //临时任务区图层
    private TileOverlay lsrwqOverlay;

    //临时禁飞区
    private TileOverlay lsjfqOverlay;

    private boolean isLocationInit = true;

    private final double originShift = 20037508.34278924;//2*Math.PI*6378137/2.0; 周长的一半

    private final double DEGREE_PER_METER = 180.0 / originShift;//一米多少度

    private final double HALF_PI = Math.PI / 2.0;

    private final double RAD_PER_DEGREE = Math.PI / 180.0;


    public ASMapView(Context context) {
        super(context);
    }

    public ASMapView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public ASMapView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    /**
     * 初始化方法
     */
    public void init(Map<String, Object> mapViewOptions, final MethodChannel methodChannel, int mapWidth, int mapHeight, BaseMapviewPlugin baseMapviewPlugin, boolean showCenterIcon, boolean openFirstLocation) {

        isLocationInit = openFirstLocation;

        boolean[] isInit = {isLocationInit};

        MyLocationStyle myLocationStyle = new MyLocationStyle();

        //连续定位、视角不移动到地图中心点
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER);

        //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        myLocationStyle.interval(1000);

        //自定义精度范围的圆形边框颜色
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));

        //圆圈的颜色,设为透明的时候就可以去掉园区区域了
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));

        //设置定位蓝点的Style
        getMap().setMyLocationStyle(myLocationStyle);

        //启用定位蓝点
        getMap().setMyLocationEnabled(true);

        //实例化UiSettings类对象
        mUiSettings = getMap().getUiSettings();

        //去掉地图右下角放大缩小地图按钮
        mUiSettings.setZoomControlsEnabled(false);

        //将logo放到底部居中
        mUiSettings.setLogoPosition(AMapOptions.LOGO_POSITION_BOTTOM_CENTER);

        //隐藏logo
        mUiSettings.setLogoBottomMargin(-100);

        //旋转手势不可用
        mUiSettings.setRotateGesturesEnabled(false);

        //控制比例尺控件是否显示
        mUiSettings.setScaleControlsEnabled(false);

        //显示默认的定位按钮
        mUiSettings.setMyLocationButtonEnabled(false);

        //定位监听
        getMap().setOnMyLocationChangeListener(new AMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                //获取当前经度纬度
                latLng = new LatLng(location.getLatitude(), location.getLongitude());

                if (isInit[0]) {
                    baseMapviewPlugin.locationAction();
                    isInit[0] = false;
                }

                //回调通知
                Map<String, Object> map = new HashMap<>();
                //经度
                map.put("latitude", location.getLatitude());
                //纬度
                map.put("longitude", location.getLongitude());
                //id
                map.put("id", key);

                methodChannel.invokeMethod("locationUpdate", map);
            }
        });

        //监听地图拖动和缩放事件
        getMap().setOnCameraChangeListener(new AMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                //得到屏幕中心的经纬度
                LatLng target = cameraPosition.target;

                //回调通知
                Map<String, Object> map = new HashMap<>();
                //经度
                map.put("latitude", target.latitude);
                //纬度
                map.put("longitude", target.longitude);
                //id
                map.put("id", key);

                methodChannel.invokeMethod("cameraChange", map);
            }

            @Override
            public void onCameraChangeFinish(CameraPosition cameraPosition) {

                LatLonPoint latLonPoint = new LatLonPoint(cameraPosition.target.latitude, cameraPosition.target.longitude);

                //第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
                RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.AMAP);

                geocodeSearch.getFromLocationAsyn(query);
            }
        });

        if (mapViewOptions == null) return;

        //设置地图模式
        if (mapViewOptions.containsKey("mapType")) {

            int mapType = (int) mapViewOptions.get("mapType");

            Log.e("plugin", "mapType:" + mapType);

            setMapType(mapType + 1);
        }

        //中心点设置
        if (mapViewOptions.containsKey("centerCoordinate")) {

            Map<String, Object> coordinateMap = (Map<String, Object>) mapViewOptions.get("centerCoordinate");

            if (coordinateMap != null) {

                animateCamera(new LatLng((double) coordinateMap.get("latitude"), (double) coordinateMap.get("longitude")), "17.6");

            }
        }

        //设置地图缩放级别
        if (mapViewOptions.containsKey("zoomLevel")) {

            double zoomLevel = (double) mapViewOptions.get("zoomLevel");

            getMap().moveCamera(CameraUpdateFactory.zoomTo((float) zoomLevel));

        }

        //初始化逆向地理编码功能  地图拖动通过坐标点得到地址信息得到这个坐标点
        geocodeSearch = new GeocodeSearch(getContext());

        geocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            /**
             * 逆地理编码(坐标转地址)
             */
            @Override
            public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int rCode) {
                //逆地理编码结果回调
                RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();

                if (rCode == AMapException.CODE_AMAP_SUCCESS) {
                    if (TextUtils.isEmpty(regeocodeAddress.getProvince())) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("id", key);
                        map.put("province", "");
                        methodChannel.invokeMethod("regeocodeSearched", map);
                        return;
                    }

                    //省
                    String province = regeocodeAddress.getProvince();

                    //市
                    String city = regeocodeAddress.getCity();

                    //县级
                    String district = regeocodeAddress.getDistrict();

                    //乡镇
                    String township = regeocodeAddress.getTownship();

                    HashMap<String, Object> map = new HashMap<>();

                    map.put("province", province);

                    map.put("city", city);

                    map.put("district", district);

                    map.put("township", township);

                    map.put("id", key);

                    methodChannel.invokeMethod("regeocodeSearched", map);

                } else {

                }
            }

            @Override
            public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

            }
        });

        if (showCenterIcon) {
            initScreenMarker(mapWidth, mapHeight);
        }

        getMap().setOnMarkerClickListener(marker -> {

            LatLng position = marker.getPosition();

            //返回marker当前数据给flutter
            //回调通知
            Map<String, Object> map = new HashMap<>();
            //经度
            map.put("latitude", position.latitude);
            //纬度
            map.put("longitude", position.longitude);
            //id
            map.put("id", key);

            methodChannel.invokeMethod("markerClick", map);

            Log.e("地图测试", "onMarkerClick");
            if (marker.isInfoWindowShown()) {

                marker.hideInfoWindow();

            } else {

                marker.showInfoWindow();

            }
            return true;
        });

        getMap().setInfoWindowAdapter(new InfoWinAdapter());

        //设置是否显示wms图层
        if (mapViewOptions.containsKey("wms")) {

            Map wmsMap = (Map) mapViewOptions.get("wms");

            if (wmsMap != null) {
                //机场净空区
                boolean airport = (boolean) wmsMap.get("airport");

                //禁飞区
                boolean jfq = (boolean) wmsMap.get("jfq");

                //限制区
                boolean xzq = (boolean) wmsMap.get("xzq");

                //危险区
                boolean wxq = (boolean) wmsMap.get("wxq");

                //固定飞场
                boolean gdfc = (boolean) wmsMap.get("gdfc");

                //临时任务区
                boolean lsrwq = (boolean) wmsMap.get("lsrwq");

                //临时禁飞区
                boolean lsjfq = (boolean) wmsMap.get("lsjfq");

                initWms(airport, jfq, xzq, wxq, gdfc, lsrwq, lsjfq);

            }
        }
    }

    /**
     * 获取key
     *
     * @return
     */
    public String getKey() {
        return key;
    }

    /**
     * 设置key
     *
     * @param key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * 获取当前经度和纬度
     *
     * @return
     */
    public LatLng getLatLng() {
        return latLng;
    }

    /**
     * 安卓o没办法适配刘海屏 过一秒获取地图的高度来配饰
     * 初始化 并添加屏幕中间的marker
     */
    @SuppressLint("CheckResult")
    public void initScreenMarker(int mapWidth, int mapHeight) {
        if (marker == null) {
            post(() -> {

                MarkerOptions markerOption = new MarkerOptions();

                //设置Marker不可拖动
                markerOption.draggable(false);

                markerOption.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                        .decodeResource(getResources(), R.mipmap.dw)));

                marker = getMap().addMarker(markerOption);

                marker.setPositionByPixels(mapWidth / 2
                        , getTop() + mapHeight / 2);
            });

        }
    }

    /**
     * 地图定位到中心点
     *
     * @param latlng
     */
    public void animateCamera(LatLng latlng, String v) {

        if (getMap() == null) return;

        if (TextUtils.isEmpty(v)) {
            v = "17.6";
        }
        Log.e("plugin", "v:" + v);
        getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, Float.parseFloat(v)));
    }

    /**
     * 地图定位
     */
    public void animateUpdateCamera(LatLng latLng, String v) {
        if (getMap() == null) return;
        if (TextUtils.isEmpty(v)) {
            v = "300";
        }
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boundsBuilder.include(latLng);
        boundsBuilder.include(getLatLng());
        getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), Integer.parseInt(v)));
    }

    /**
     * 添加marker
     *
     * @param latlngList
     */
    public void addMarker(List<LatLng> latlngList) {

        removeMarker();

        for (int i = 0; i < latlngList.size(); i++) {

            LatLng latLng = latlngList.get(i);

            MarkerOptions markerOptions = new MarkerOptions();

//            markerOptions.position(GdLatlngUtil.getGdLatlngFormat(getContext(), String.valueOf(latLng.latitude), String.valueOf(latLng.longitude)));

            markerOptions.position(latLng);

            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                    .decodeResource(getContext().getResources(), R.mipmap.plane_marker)));

            Marker marker = getMap().addMarker(markerOptions);

            markers.add(marker);
        }
    }


    /**
     * 添加marker
     *
     * @param latlngList
     */
    public void addFindDeviceMarker(List<LatLng> latlngList, Result result) {

        removeMarker();

        for (int i = 0; i < latlngList.size(); i++) {

            LatLng latLng = latlngList.get(i);

            MarkerOptions markerOptions = new MarkerOptions();

            markerOptions.position(Util.getGdLatlngFormat(String.valueOf(latLng.latitude), String.valueOf(latLng.longitude), getContext()));

            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                    .decodeResource(getContext().getResources(), R.mipmap.marker_dot)));

            Marker marker = getMap().addMarker(markerOptions);

            markers.add(marker);
        }

        result.success(null);
    }

    /**
     * 添加marker
     *
     * @param latlngList
     */
    public void addAreaDetailMarker(List<LatLng> latlngList) {

        removeMarker();

        for (int i = 0; i < latlngList.size(); i++) {

            LatLng latLng = latlngList.get(i);

            MarkerOptions markerOptions = new MarkerOptions();

            markerOptions.position(latLng);

            Log.e("地图测试", Util.getFormatLatLng(latLng.latitude, latLng.longitude));

            markerOptions.title(Util.getFormatLatLng(latLng.latitude, latLng.longitude));

            markerOptions.anchor(0.5f, 0.5f);

            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                    .decodeResource(getContext().getResources(), R.mipmap.area_dot)));


            Marker marker = getMap().addMarker(markerOptions);

            markers.add(marker);
        }
    }

    /**
     * 移除marker
     */
    public void removeMarker() {

        if (markers == null) {

            markers = new ArrayList<>();

        }

        for (int i = 0; i < markers.size(); i++) {

            markers.get(i).remove();

        }

        markers.clear();
    }

    /**
     * 初始化点的坐标
     */
    public void addPointMarker(List<LatLng> latlngList) {

        for (int i = 0; i < latlngList.size(); i++) {

            LatLng latLng = latlngList.get(i);

            MarkerOptions markerOptions = new MarkerOptions();

//            markerOptions.position(GdLatlngUtil.getGdLatlngFormat(getContext(), String.valueOf(latLng.latitude), String.valueOf(latLng.longitude)));

            markerOptions.position(latLng);

            markerOptions.anchor(0.5f, 0.5f);

            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                    .decodeResource(getContext().getResources(), R.mipmap.area_dot)));

            Marker marker = getMap().addMarker(markerOptions);

        }
    }

    /**
     * 绘制圆形
     *
     * @param latLng
     */
    public void drawCircle(LatLng latLng, double radius, String color) {
        if (circle != null) {
            circle.remove();
            circle = null;
        }
        circle = getMap().addCircle(new CircleOptions().
                center(latLng).
                radius(radius).
                fillColor(Color.parseColor(color)).
                strokeColor(Color.parseColor(color)).
                strokeWidth(1).zIndex(2));
    }

    /**
     * 绘制线
     *
     * @param latLngList
     */
    public void drawPolylin(List<LatLng> latLngList, String color) {

        polyline = getMap().addPolyline(new PolylineOptions().
                addAll(latLngList).width(12).color(Color.parseColor(color)));
    }

    /**
     * 绘制飞行轨迹
     *
     * @param latLngList
     */
    public void drawFlyPolylin(List<LatLng> latLngList) {

        polyline = getMap().addPolyline(new PolylineOptions().
                addAll(latLngList).width(8).color(Color.argb(255, 1, 1, 255)));

    }

    /**
     * 绘制飞行轨迹
     */
    public void clearFlyPolylin() {
        if (polyline != null) {
            polyline.remove();
            polyline = null;
        }
        getMap().clear();
    }

    /**
     * 缩放
     *
     * @param latLngList
     */
    public void animateCamera(List<LatLng> latLngList) {
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (int i = 0; i < latLngList.size(); i += 2) {
            boundsBuilder.include(latLngList.get(i));
        }
        getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 300), 50, null);
    }

    /**
     * 缩放
     *
     * @param latLngList
     */
    public void areaScaleCamera(List<LatLng> latLngList, boolean includeSelf, String areaType, double radius, double latitude, double longitude, boolean first) {
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (int i = 0; i < latLngList.size(); i++) {
            boundsBuilder.include(latLngList.get(i));
        }

        //如果包includeSelf=true并且自身坐标!=null
        if (includeSelf && latLng != null) {
            boundsBuilder.include(latLng);
        }
        Log.e("地图测试", "areaType-----" + areaType);
        //如果空域类型是圆形的  应该把圆形的上下左右四个经纬度添加进来
        if (areaType.equals("CIRCULAR")) {
            Log.e("地图测试", "areaType");
            LatLng l = new LatLng(latitude
                    , longitude);
            calculateLl(boundsBuilder, l, radius);
        }
        if (includeSelf || first) {
            if (first) {             //如果是刚刚进来  动画缩放到相应比例
                Log.e("地图测试", "动画缩放");
                getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 300));//第二个参数为四周留空宽度
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 300));
//                    }
//                }, 1000);
            } else {                //如果要切换到总览的状态 包含自身的点
                boolean aleradyInArae = false;   //自己是否在当前空域里
                if (areaType == "CIRCULAR") {  //圆形
                    if (circle.contains(latLng)) aleradyInArae = true;     //如果自身位置已经在圆形空域里
                }
                if (areaType == "POLYGON") { //不规则多边形
                    if (polygon.contains(latLng)) aleradyInArae = true;    //如果自身位置已经在不规则多边形里
                }
                if (aleradyInArae) //如果自己当前空域里  直接移动 四周间隔没变  相当于没有变动
                    getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 300));//第二个参数为四周留空宽度
                else   //如果自己不在当前空域   动画缩放 并且改变了四周的间隔
                    getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 360));//第二个参数为四周留空宽度
            }
        } else {
            //如果不是刚刚进来 或者是切换到空域状态  不用动画移动
            getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 300));
        }
    }

    /**
     * 绘制多边形
     *
     * @param latLngList
     */
    public void drawPolygon(List<LatLng> latLngList, String color) {

        PolygonOptions polygonOptions = new PolygonOptions();

        for (int i = 0; i < latLngList.size(); i++) {

            polygonOptions.add(latLngList.get(i));

        }

        if (polygon != null) {
            polygon.remove();
            polygon = null;
        }
        polygonOptions.fillColor(Color.parseColor(color)).
                strokeColor(Color.parseColor(color)).
                strokeWidth(1).zIndex(9999);
        polygon = getMap().addPolygon(polygonOptions);
    }

    /**
     * 设置地图图层
     *
     * @param mapType
     */
    public void setMapType(int mapType) {
        getMap().setMapType(mapType);

    }

    /**
     * 放大地图级别
     */
    public void zoomOut() {
        getMap().moveCamera(CameraUpdateFactory.zoomOut());
    }

    /**
     * 缩小地图级别
     */
    public void zoomIn() {
        getMap().moveCamera(CameraUpdateFactory.zoomIn());
    }


    /**
     * 显示禁飞区图层
     */
    public void initWms(boolean airport, boolean jfq, boolean xzq, boolean wxq, boolean gdfc, boolean lsrwq, boolean lsjfq) {
        //机场净空区
        if (airport) {

            if (airportOverlay == null) {

                HeritageScopeTileProvider airportProvider = new HeritageScopeTileProvider(HeritageScopeTileProvider.AIRPORT);

                TileOverlayOptions airportOptions = new TileOverlayOptions().tileProvider(airportProvider);

                airportOptions
                        .diskCacheDir("/storage/amap/cache")
                        .diskCacheEnabled(true)
                        .diskCacheSize(100000)
                        .zIndex(-1);

                airportOverlay = getMap().addTileOverlay(airportOptions);
            }

        } else {

            if (airportOverlay != null) {

                airportOverlay.remove();

                airportOverlay = null;
            }

        }

        if (jfq) {

            if (jfqOverlay == null) {

                HeritageScopeTileProvider jfqProvider = new HeritageScopeTileProvider(HeritageScopeTileProvider.JFQ);

                TileOverlayOptions jfqOptions = new TileOverlayOptions().tileProvider(jfqProvider);

                jfqOptions
                        .diskCacheDir("/storage/amap/cache")
                        .diskCacheEnabled(true)
                        .diskCacheSize(100000);

                jfqOverlay = getMap().addTileOverlay(jfqOptions);

            }

        } else {

            if (jfqOverlay != null) {

                jfqOverlay.remove();

                jfqOverlay = null;

            }

        }

        if (wxq) {

            if (wxqOverlay == null) {

                HeritageScopeTileProvider wxqProvider = new HeritageScopeTileProvider(HeritageScopeTileProvider.WXQ);

                TileOverlayOptions wxqOptions = new TileOverlayOptions().tileProvider(wxqProvider);

                wxqOptions
                        .diskCacheDir("/storage/amap/cache")
                        .diskCacheEnabled(true)
                        .diskCacheSize(100000);

                wxqOverlay = getMap().addTileOverlay(wxqOptions);

            }

        } else {

            if (wxqOverlay != null) {

                wxqOverlay.remove();

                wxqOverlay = null;

            }

        }

        if (xzq) {

            if (xzqOverlay == null) {

                HeritageScopeTileProvider xzqProvider = new HeritageScopeTileProvider(HeritageScopeTileProvider.XZQ);

                TileOverlayOptions xzqOptions = new TileOverlayOptions().tileProvider(xzqProvider);

                xzqOptions
                        .diskCacheDir("/storage/amap/cache")
                        .diskCacheEnabled(true)
                        .diskCacheSize(100000);

                xzqOverlay = getMap().addTileOverlay(xzqOptions);

            }

        } else {

            if (xzqOverlay != null) {

                xzqOverlay.remove();

                xzqOverlay = null;

            }

        }

        //固定飞场
        if (gdfc) {

            if (gdfcOverlay == null) {

                HeritageScopeTileProvider gdfcProvider = new HeritageScopeTileProvider(HeritageScopeTileProvider.GDFC);

                TileOverlayOptions gdfcOptions = new TileOverlayOptions().tileProvider(gdfcProvider);

                gdfcOptions
                        .diskCacheDir("/storage/amap/cache")
                        .diskCacheEnabled(true)
                        .diskCacheSize(100000);

                gdfcOverlay = getMap().addTileOverlay(gdfcOptions);

            }

        } else {

            if (gdfcOverlay != null) {

                gdfcOverlay.remove();

                gdfcOverlay = null;

            }

        }

        //临时任务区
        if (lsrwq) {

            if (lsrwqOverlay == null) {

                HeritageScopeTileProvider lsrwqProvider = new HeritageScopeTileProvider(HeritageScopeTileProvider.LSRWQ);

                TileOverlayOptions lsrwqOptions = new TileOverlayOptions().tileProvider(lsrwqProvider);

                lsrwqOptions
                        .diskCacheDir("/storage/amap/cache")
                        .diskCacheEnabled(true)
                        .diskCacheSize(100000);

                lsrwqOverlay = getMap().addTileOverlay(lsrwqOptions);

            }

        } else {

            if (lsrwqOverlay != null) {

                lsrwqOverlay.remove();

                lsrwqOverlay = null;

            }

        }

        //临时禁飞区
        if (lsjfq) {

            if (lsjfqOverlay == null) {

                HeritageScopeTileProvider lsjfqProvider = new HeritageScopeTileProvider(HeritageScopeTileProvider.LSJFQ);

                TileOverlayOptions lsjfqOptions = new TileOverlayOptions().tileProvider(lsjfqProvider);

                lsjfqOptions
                        .diskCacheDir("/storage/amap/cache")
                        .diskCacheEnabled(true)
                        .diskCacheSize(100000);

                lsjfqOverlay = getMap().addTileOverlay(lsjfqOptions);

            }

        } else {

            if (lsjfqOverlay != null) {

                lsjfqOverlay.remove();

                lsjfqOverlay = null;

            }

        }
    }

    //如果是一个圆形 添加上下左右的点
    private void calculateLl(LatLngBounds.Builder boundsBuilder, LatLng center, double radius) {
        Log.e("地图测试", "calculateLl----1");
        double latitude = center.latitude;//维度
        double longitude = center.longitude;//经度

        Log.e("地图测试", "calculateLl----2");

        LatLng left = new LatLng(latitude, longitude + Meters2Lon(radius));
        LatLng right = new LatLng(latitude, longitude - Meters2Lon(radius));
        LatLng top = new LatLng(latitude + Meters2Lat(radius), longitude);
        LatLng bottom = new LatLng(latitude - Meters2Lat(radius), longitude);
        boundsBuilder.include(left);
        boundsBuilder.include(right);
        boundsBuilder.include(top);
        boundsBuilder.include(bottom);
    }

    /**
     * X米转经纬度
     */
    private double Meters2Lon(double mx) {
        double lon = mx * DEGREE_PER_METER;
        return lon;
    }

    /**
     * Y米转经纬度
     */
    private double Meters2Lat(double my) {
        double lat = my * DEGREE_PER_METER;
        lat = 180.0 / Math.PI * (2 * Math.atan(Math.exp(lat * RAD_PER_DEGREE)) - HALF_PI);
        return lat;
    }
}
