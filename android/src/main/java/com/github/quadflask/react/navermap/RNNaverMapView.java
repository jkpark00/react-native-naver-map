package com.github.quadflask.react.navermap;

import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.airbnb.android.react.maps.ViewAttacherGroup;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.*;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.List;

public class RNNaverMapView extends MapView implements OnMapReadyCallback, NaverMap.OnCameraIdleListener, NaverMap.OnMapClickListener, RNNaverMapViewProps {
    private ThemedReactContext themedReactContext;
    private FusedLocationSource locationSource;
    private NaverMap naverMap;
    private ViewAttacherGroup attacherGroup;
    private long lastTouch = 0;
    private final List<RNNaverMapFeature<?>> features = new ArrayList<>();

    public RNNaverMapView(@NonNull ThemedReactContext themedReactContext, ReactApplicationContext appContext, FusedLocationSource locationSource, NaverMapOptions naverMapOptions, Bundle instanceStateBundle) {
        super(ReactUtil.getNonBuggyContext(themedReactContext, appContext), naverMapOptions);
        this.themedReactContext = themedReactContext;
        this.locationSource = locationSource;
        super.onCreate(instanceStateBundle);
//        super.onStart();
        getMapAsync(this);

        // Set up a parent view for triggering visibility in subviews that depend on it.
        // Mainly ReactImageView depends on Fresco which depends on onVisibilityChanged() event
        attacherGroup = new ViewAttacherGroup(this.themedReactContext);
        LayoutParams attacherLayoutParams = new LayoutParams(0, 0);
        attacherLayoutParams.width = 0;
        attacherLayoutParams.height = 0;
        attacherLayoutParams.leftMargin = 99999999;
        attacherLayoutParams.topMargin = 99999999;
        attacherGroup.setLayoutParams(attacherLayoutParams);
        addView(attacherGroup);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        this.naverMap.setLocationSource(locationSource);
        this.naverMap.setOnMapClickListener(this);
        this.naverMap.addOnCameraIdleListener(this);
        this.naverMap.addOnCameraChangeListener((reason, animated) -> {
            if (reason == -1 && System.currentTimeMillis() - lastTouch > 500) { // changed by user
                WritableMap param = Arguments.createMap();
                param.putInt("reason", reason);
                param.putBoolean("animated", animated);
                emitEvent("onTouch", param);
                lastTouch = System.currentTimeMillis();
            }
        });
        onInitialized();
    }

    @Override
    public void setCenter(LatLng latLng) {
        getMapAsync(e -> {
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(latLng).animate(CameraAnimation.Easing);
            naverMap.moveCamera(cameraUpdate);
        });
    }

    @Override
    public void setCenter(LatLng latLng, Double zoom, Double tilt, Double bearing) {
        getMapAsync(e -> {
            CameraPosition cam = naverMap.getCameraPosition();
            double zoomValue = zoom == null ? cam.zoom : zoom;
            double tiltValue = tilt == null ? cam.tilt : tilt;
            double bearingValue = bearing == null ? cam.bearing : bearing;

            naverMap.moveCamera(CameraUpdate.toCameraPosition(new CameraPosition(latLng, zoomValue, tiltValue, bearingValue))
                    .animate(CameraAnimation.Easing));
        });
    }

    @Override
    public void zoomTo(LatLngBounds latLngBounds, int paddingInPx) {
        getMapAsync(e -> {
            CameraUpdate cameraUpdate = CameraUpdate.fitBounds(latLngBounds, paddingInPx)
                    .animate(CameraAnimation.Easing);
            naverMap.moveCamera(cameraUpdate);
        });
    }

    @Override
    public void setTilt(int tilt) {
        getMapAsync(e -> {
            final CameraPosition cameraPosition = naverMap.getCameraPosition();
            naverMap.moveCamera(CameraUpdate.toCameraPosition(
                    new CameraPosition(cameraPosition.target, cameraPosition.zoom, tilt, cameraPosition.bearing)));
        });
    }

    @Override
    public void setBearing(int bearing) {
        getMapAsync(e -> {
            final CameraPosition cameraPosition = naverMap.getCameraPosition();
            naverMap.moveCamera(CameraUpdate.toCameraPosition(
                    new CameraPosition(cameraPosition.target, cameraPosition.zoom, cameraPosition.tilt, bearing)));
        });
    }

    @Override
    public void setZoom(float zoom) {
        getMapAsync(e -> {
            naverMap.moveCamera(CameraUpdate.zoomTo(zoom));
        });
    }

    @Override
    public void setMapPadding(int left, int top, int right, int bottom) {
        getMapAsync(e -> {
            naverMap.setContentPadding(left, top, right, bottom);
        });
    }

    @Override
    public void onInitialized() {
        emitEvent("onInitialized", null);
    }

    @Override
    public void showsMyLocationButton(boolean show) {
        getMapAsync(e -> naverMap.getUiSettings().setLocationButtonEnabled(show));
    }

    @Override
    public void setCompassEnabled(boolean show) {
        getMapAsync(e -> naverMap.getUiSettings().setCompassEnabled(show));
    }

    @Override
    public void setScaleBarEnabled(boolean show) {
        getMapAsync(e -> naverMap.getUiSettings().setScaleBarEnabled(show));
    }

    @Override
    public void setZoomControlEnabled(boolean show) {
        getMapAsync(e -> naverMap.getUiSettings().setZoomControlEnabled(show));
    }

    @Override
    public void setLocationTrackingMode(int mode) {
        getMapAsync(e -> naverMap.setLocationTrackingMode(LocationTrackingMode.values()[mode]));
    }

    @Override
    public void setMapType(NaverMap.MapType value) {
        getMapAsync(e -> naverMap.setMapType(value));
    }

    @Override
    public void setMinZoom(float minZoomLevel) {
        getMapAsync(e -> naverMap.setMinZoom(minZoomLevel));
    }

    @Override
    public void setMaxZoom(float maxZoomLevel) {
        getMapAsync(e -> naverMap.setMaxZoom(maxZoomLevel));
    }

    @Override
    public void setBuildingHeight(float height) {
        getMapAsync(e -> naverMap.setBuildingHeight(height));
    }

    @Override
    public void setLayerGroupEnabled(String layerGroup, boolean enable) {
        getMapAsync(e -> naverMap.setLayerGroupEnabled(layerGroup, enable));
    }

    @Override
    public void setNightModeEnabled(boolean enable) {
        getMapAsync(e -> naverMap.setNightModeEnabled(enable));
    }

    @Override
    public void setLogoMargin(int left, int top, int right, int bottom) {
        getMapAsync(e -> naverMap.getUiSettings().setLogoMargin(left, top, right, bottom));
    }

    @Override
    public void setLogoGravity(int gravity) {
        getMapAsync(e -> naverMap.getUiSettings().setLogoGravity(gravity));
    }

    @Override
    public void setScrollGesturesEnabled(boolean enabled) {
        getMapAsync(e -> naverMap.getUiSettings().setScrollGesturesEnabled(enabled));
    }

    @Override
    public void setZoomGesturesEnabled(boolean enabled) {
        getMapAsync(e -> naverMap.getUiSettings().setZoomGesturesEnabled(enabled));
    }

    @Override
    public void setTiltGesturesEnabled(boolean enabled) {
        getMapAsync(e -> naverMap.getUiSettings().setTiltGesturesEnabled(enabled));
    }

    @Override
    public void setRotateGesturesEnabled(boolean enabled) {
        getMapAsync(e -> naverMap.getUiSettings().setRotateGesturesEnabled(enabled));
    }

    @Override
    public void setStopGesturesEnabled(boolean enabled) {
        getMapAsync(e -> naverMap.getUiSettings().setStopGesturesEnabled(enabled));
    }

    @Override
    public void setLiteModeEnabled(boolean enabled) {
        getMapAsync(e -> naverMap.setLiteModeEnabled(enabled));
    }

    @Override
    public void moveCameraFitBound(LatLngBounds bounds, int left, int top, int right, int bottom) {
        getMapAsync(e -> naverMap.moveCamera(CameraUpdate.fitBounds(bounds, left, top, right, bottom).animate(CameraAnimation.Fly, 500)));
    }

    @Override
    public void addFeature(View child, int index) {
        // ─────────────────────────────────────────────
        // 1) 전달된 View 타입 확인
        // ─────────────────────────────────────────────
        if (!(child instanceof RNNaverMapFeature)) {
            return;   // Marker, Path 같은 피처가 아니면 무시
        }
        RNNaverMapFeature<?> annotation = (RNNaverMapFeature<?>) child;

        // ─────────────────────────────────────────────
        // 2) 내부 리스트(features) 먼저 업데이트
        //    → JS-side와 Native-side가 즉시 동일한 상태가 되도록
        // ─────────────────────────────────────────────
        if (index < 0 || index > features.size()) {
            // 잘못된 인덱스가 들어오면 뒤에 붙인다(예외 예방)
            features.add(annotation);
        } else {
            features.add(index, annotation);
        }

        // ─────────────────────────────────────────────
        // 3) 지도 객체 준비 후 실제로 addToMap
        //    → 이후 과정은 getMapAsync 콜백에서 실행
        // ─────────────────────────────────────────────
        getMapAsync(map -> {
            // 지도 객체가 준비된 시점에 피처를 추가
            annotation.addToMap(this);

            // 화면에 보이도록 하기 위한 기존 로직 유지
            int visibility = annotation.getVisibility();
            annotation.setVisibility(View.INVISIBLE);

            ViewGroup parent = (ViewGroup) annotation.getParent();
            if (parent != null) {
                parent.removeView(annotation);
            }
            attacherGroup.addView(annotation);
            annotation.setVisibility(visibility);
        });
    }

    @Override
    public void removeFeatureAt(int index) {
        // 1) 인덱스 유효성 검사 ──────────────────────────────
        if (index < 0 || index >= features.size()) {
            // 잘못된 요청이면 아무것도 하지 않고 반환
            return;
        }

        // 2) 리스트에서 먼저 제거해 JS·Native 동기화 유지 ──
        RNNaverMapFeature<?> feature = features.remove(index);
        if (feature == null) return;

        // 3) 지도 객체에서 제거  ─────────────────────────────
        feature.removeFromMap();

        // 4) attacherGroup(또는 parent)에서 뷰도 분리 ──────
        ViewGroup parent = (ViewGroup) feature.getParent();
        if (parent != null) {
            parent.removeView(feature);
        }
    }

    @Override
    public int getFeatureCount() {
        return features.size();
    }

    @Override
    public View getFeatureAt(int index) {
//        return features.get(index);
        if(index < 0 || index >= features.size()){
            return null;
        }
        return features.get(index);
    }

    @Override
    public void onCameraIdle() {
        CameraPosition cameraPosition = naverMap.getCameraPosition();

        WritableMap param = Arguments.createMap();
        param.putDouble("latitude", cameraPosition.target.latitude);
        param.putDouble("longitude", cameraPosition.target.longitude);
        param.putDouble("zoom", cameraPosition.zoom);
        param.putArray("contentRegion", ReactUtil.toWritableArray(naverMap.getContentRegion()));
        param.putArray("coveringRegion", ReactUtil.toWritableArray(naverMap.getCoveringRegion()));

        emitEvent("onCameraChange", param);
    }

    @Override
    public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
        WritableMap param = Arguments.createMap();
        param.putDouble("x", pointF.x);
        param.putDouble("y", pointF.y);
        param.putDouble("latitude", latLng.latitude);
        param.putDouble("longitude", latLng.longitude);

        emitEvent("onMapClick", param);
    }

    @Override
    public void onDestroy() {
        removeAllViews();
        themedReactContext = null;
        locationSource = null;
        naverMap = null;
        attacherGroup = null;
        for (RNNaverMapFeature<?> feature : features)
            feature.removeFromMap();
        features.clear();
        super.onDestroy();
    }

    private void emitEvent(String eventName, WritableMap param) {
        themedReactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), eventName, param);
    }

    public NaverMap getMap() {
        return naverMap;
    }
}
