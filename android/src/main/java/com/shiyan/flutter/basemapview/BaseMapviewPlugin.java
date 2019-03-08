package com.shiyan.flutter.basemapview;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.amap.api.maps.model.LatLng;
import com.mylhyl.acp.Acp;
import com.mylhyl.acp.AcpListener;
import com.mylhyl.acp.AcpOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * Created by shiyan on 2019/3/6
 * <p>
 * desc:BaseMapView插件
 */
public class BaseMapviewPlugin implements MethodCallHandler {

    //当前Activity环境
    private FlutterActivity root;

    //当前地图控件
    private ASMapView mapView;

    //地图参数配置
    Map<String, Object> mapViewOptions;

    private String[] maniFests = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    //构造函数
    public BaseMapviewPlugin(FlutterActivity activity, MethodChannel channel) {
        this.root = activity;
        //处理生命周期
        this.root.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                if (activity != root) return;
                if (mapView != null) {
                    mapView.onResume();
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (activity != root) return;
                if (mapView != null) {
                    mapView.onPause();
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (activity != root) return;
                if (mapView != null) {
                    mapView.onDestroy();
                }
            }
        });

        requestPermission();
    }

    /**
     * 获取权限(ACCESS_COARSE_LOCATION,WRITE_EXTERNAL_STORAGE)
     */
    private void requestPermission() {
        Acp.getInstance(root).request(new AcpOptions.Builder()
                        .setPermissions(maniFests)
                        .build(),
                new AcpListener() {
                    @Override
                    public void onGranted() {

                    }

                    @Override
                    public void onDenied(List<String> permissions) {
                    }
                });
    }

    /**
     * 插件注册
     *
     * @param registrar
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_amap");
        channel.setMethodCallHandler(new BaseMapviewPlugin((FlutterActivity) registrar.activity(), channel));

    }

    /**
     * 方法回调
     *
     * @param call
     * @param result
     */
    @Override
    public void onMethodCall(MethodCall call, Result result) {
        //获取参数
        Map<String, Object> args = (Map<String, Object>) call.arguments;

        //获取地图参数配置信息
        if (args != null && args.containsKey("mapView")) {
            mapViewOptions = (Map<String, Object>) args.get("mapView");
        }

        //显示地图
        if (call.method.equals("showMapView")) {

            root.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapView = new ASMapView(root);

                    mapView.onCreate(new Bundle());

                    mapView.onResume();

                    mapView.init(mapViewOptions);

                    root.addContentView(mapView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 1000));
                }
            });

        }
        //定位
        else if (call.method.equals("location")) {
            locationAction();
        }
        //添加marker
        else if (call.method.equals("addmarker")) {

            List<LatLng> latlngList = new ArrayList<>();

            LatLng latLng = new LatLng(39.833876, 116.301886);

            latlngList.add(latLng);

            mapView.addMarker(latlngList);

        }
        //移除marker
        else if (call.method.equals("removemarker")) {

            mapView.removeMarker();

        }
        //绘制圆形
        else if (call.method.equals("drawcircle")) {
            //116.297241,39.825262
            mapView.drawCircle(new LatLng(39.825262, 116.297241), 100);
        }
        //绘制线
        else if (call.method.equals("drawpolylin")) {
            //116.30102,39.82588 116.302554,39.825847 116.300612,39.823853
            List<LatLng> latlngList = new ArrayList<>();

            LatLng latLng = new LatLng(39.82588, 116.30102);

            latlngList.add(latLng);

            LatLng latLng2 = new LatLng(39.825847, 116.302554);

            latlngList.add(latLng2);

            LatLng latLng3 = new LatLng(39.823853, 116.300612);

            latlngList.add(latLng3);

            mapView.drawPolylin(latlngList);
        }

        //绘制多边形
        else if (call.method.equals("drawPolygon")) {
            //116.295385,39.823862  116.296299,39.823734 116.294776,39.823083
            List<LatLng> latlngList = new ArrayList<>();

            LatLng latLng = new LatLng(39.823862, 116.295385);

            latlngList.add(latLng);

            LatLng latLng2 = new LatLng(39.823734, 116.296299);

            latlngList.add(latLng2);

            LatLng latLng3 = new LatLng(39.823083, 116.294776);

            latlngList.add(latLng3);

            mapView.addPointMarker(latlngList);

            mapView.drawPolygon(latlngList);
        }
        //切换地图图层
        else if (call.method.equals("setMapType")) {
            int mapType = (int) mapViewOptions.get("mapType");
            mapView.setMapType(mapType);
        }
        //放大地图级别
        else if (call.method.equals("zoomOut")) {
            mapView.zoomOut();
        }
        //缩小地图级别
        else if (call.method.equals("zoomIn")) {
            mapView.zoomIn();
        }
        //无消息
        else {
            result.notImplemented();
        }
    }

    /**
     * 定位
     */
    private void locationAction() {
        Acp.getInstance(root).request(new AcpOptions.Builder()
                        .setPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
                        .build(),
                new AcpListener() {
                    @Override
                    public void onGranted() {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mapView.animateCamera(mapView.getLatLng());
                            }
                        }, 2000);
                    }

                    @Override
                    public void onDenied(List<String> permissions) {
                    }
                });
    }

}
