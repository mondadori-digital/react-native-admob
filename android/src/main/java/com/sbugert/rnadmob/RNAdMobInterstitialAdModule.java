package com.sbugert.rnadmob;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import android.os.Bundle;
import android.location.Location;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.amazon.device.ads.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RNAdMobInterstitialAdModule extends ReactContextBaseJavaModule {

    public static final String REACT_CLASS = "RNAdMobInterstitial";

    public static final String EVENT_AD_LOADED = "interstitialAdLoaded";
    public static final String EVENT_AD_FAILED_TO_LOAD = "interstitialAdFailedToLoad";
    public static final String EVENT_AD_OPENED = "interstitialAdOpened";
    public static final String EVENT_AD_CLOSED = "interstitialAdClosed";
    public static final String EVENT_AD_LEFT_APPLICATION = "interstitialAdLeftApplication";

    PublisherInterstitialAd mInterstitialAd;
    String[] testDevices;
    AdListener adListener;
    Map<String, PublisherInterstitialAd> mInterstitialAds = new HashMap<String, PublisherInterstitialAd>();
    boolean npa;
    ReadableMap location;
    String amazonSlotUUID;

    private Promise mRequestAdPromise;
    private final ReactApplicationContext mReactApplicationContext;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    public RNAdMobInterstitialAdModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mInterstitialAd = new PublisherInterstitialAd(reactContext);
        mReactApplicationContext = reactContext;

        adListener = new AdListener() {
            @Override
            public void onAdClosed() {
                sendEvent(EVENT_AD_CLOSED, null);
            }
            @Override
            public void onAdFailedToLoad(int errorCode) {
                String errorString = "ERROR_UNKNOWN";
                String errorMessage = "Unknown error";
                switch (errorCode) {
                    case PublisherAdRequest.ERROR_CODE_INTERNAL_ERROR:
                        errorString = "ERROR_CODE_INTERNAL_ERROR";
                        errorMessage = "Internal error, an invalid response was received from the ad server.";
                        break;
                    case PublisherAdRequest.ERROR_CODE_INVALID_REQUEST:
                        errorString = "ERROR_CODE_INVALID_REQUEST";
                        errorMessage = "Invalid ad request, possibly an incorrect ad unit ID was given.";
                        break;
                    case PublisherAdRequest.ERROR_CODE_NETWORK_ERROR:
                        errorString = "ERROR_CODE_NETWORK_ERROR";
                        errorMessage = "The ad request was unsuccessful due to network connectivity.";
                        break;
                    case PublisherAdRequest.ERROR_CODE_NO_FILL:
                        errorString = "ERROR_CODE_NO_FILL";
                        errorMessage = "The ad request was successful, but no ad was returned due to lack of ad inventory.";
                        break;
                }
                WritableMap event = Arguments.createMap();
                WritableMap error = Arguments.createMap();
                event.putString("message", errorMessage);
                sendEvent(EVENT_AD_FAILED_TO_LOAD, event);
                mRequestAdPromise.reject(errorString, errorMessage);
            }
            @Override
            public void onAdLeftApplication() {
                sendEvent(EVENT_AD_LEFT_APPLICATION, null);
            }
            @Override
            public void onAdLoaded() {
                sendEvent(EVENT_AD_LOADED, null);
                mRequestAdPromise.resolve(null);
            }
            @Override
            public void onAdOpened() {
                sendEvent(EVENT_AD_OPENED, null);
            }
        };

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mInterstitialAd.setAdListener(adListener);
            }
        });
    }
    private void sendEvent(String eventName, @Nullable WritableMap params) {
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    @ReactMethod
    public void setAdUnitID(String adUnitID, String amazonSlotUUID) {
        
        this.amazonSlotUUID = amazonSlotUUID;

        if (mInterstitialAd.getAdUnitId() == null) {
            mInterstitialAd.setAdUnitId(adUnitID);
            mInterstitialAds.put(adUnitID, mInterstitialAd);
            return;
        }

        // already current
        if( mInterstitialAd.getAdUnitId() == adUnitID ){
            return;
        }

        // check for existing interstitial matching adUnitID, 
        final PublisherInterstitialAd interstitialAd = mInterstitialAds.get(adUnitID);

        // existing found, make current
        if(interstitialAd != null ){
            mInterstitialAd = interstitialAd;
            return;
        }

        // create new interstitial, store and make current
        final PublisherInterstitialAd newInterstitialAd = new PublisherInterstitialAd(mReactApplicationContext);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                newInterstitialAd.setAdListener( adListener );
            }
        });
        newInterstitialAd.setAdUnitId(adUnitID);
        mInterstitialAds.put(adUnitID, newInterstitialAd);
        mInterstitialAd = newInterstitialAd;
    }

    @ReactMethod
    public void setTestDevices(ReadableArray testDevices) {
        ReadableNativeArray nativeArray = (ReadableNativeArray)testDevices;
        ArrayList<Object> list = nativeArray.toArrayList();
        this.testDevices = list.toArray(new String[list.size()]);
    }

    @ReactMethod
    public void setNpa(boolean npa) {
        this.npa = npa;
    }

    @ReactMethod
    public void setLocation(ReadableMap location) {
        this.location = location;
    }

    @ReactMethod
    public void requestAd(final Promise promise) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run () {
                if (mInterstitialAd.isLoaded() || mInterstitialAd.isLoading()) {
                    promise.reject("E_AD_ALREADY_LOADED", "Ad is already loaded.");
                } else {
                    mRequestAdPromise = promise;
                    final PublisherAdRequest.Builder adRequestBuilder = new PublisherAdRequest.Builder();
                    
                    if(amazonSlotUUID != null) {
                        final DTBAdRequest loader = new DTBAdRequest();
                        loader.setSizes(new DTBAdSize.DTBInterstitialAdSize(amazonSlotUUID));
                        loader.loadAd(new DTBAdCallback() {
                            @Override
                            public void onFailure(AdError adError) {
                                Log.e("APP", "Failed to load the interstitial ad" + adError.getMessage());
                                if (testDevices != null) {
                                    for (int i = 0; i < testDevices.length; i++) {
                                        String testDevice = testDevices[i];
                                        if (testDevice == "SIMULATOR") {
                                            testDevice = PublisherAdRequest.DEVICE_ID_EMULATOR;
                                        }
                                        adRequestBuilder.addTestDevice(testDevice);
                                    }
                                }
                                if (npa) {
                                    Bundle extras = new Bundle();
                                    extras.putString("npa", "1");
                                    adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
                                }
            
                                if (location != null && location.hasKey("latitude") && !location.isNull("latitude") && location.hasKey("longitude") && !location.isNull("longitude")) {
                                    Location advLocation = new Location("");
                                    advLocation.setLatitude(location.getDouble("latitude"));
                                    advLocation.setLongitude(location.getDouble("longitude"));
                        
                                    adRequestBuilder.setLocation(advLocation);
                                }
            
                                PublisherAdRequest adRequest = adRequestBuilder.build();
                                mInterstitialAd.loadAd(adRequest);
                            }
                        
                            @Override
                            public void onSuccess(DTBAdResponse dtbAdResponse) {
                                Log.d("loadInterstitialBanner", "success");
                                // Build Google Ad Manager request with APS keywords
                                final PublisherAdRequest adRequest = DTBAdUtil.INSTANCE.createPublisherAdRequestBuilder(dtbAdResponse).build();
                                mInterstitialAd.loadAd(adRequest);
                            }
                        });
                    } else {
                        Log.d("loadBanner", "amazonSlotUUID is null");
                        if (testDevices != null) {
                            for (int i = 0; i < testDevices.length; i++) {
                                String testDevice = testDevices[i];
                                if (testDevice == "SIMULATOR") {
                                    testDevice = PublisherAdRequest.DEVICE_ID_EMULATOR;
                                }
                                adRequestBuilder.addTestDevice(testDevice);
                            }
                        }
                        if (npa) {
                            Bundle extras = new Bundle();
                            extras.putString("npa", "1");
                            adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
                        }
    
                        if (location != null && location.hasKey("latitude") && !location.isNull("latitude") && location.hasKey("longitude") && !location.isNull("longitude")) {
                            Location advLocation = new Location("");
                            advLocation.setLatitude(location.getDouble("latitude"));
                            advLocation.setLongitude(location.getDouble("longitude"));
                
                            adRequestBuilder.setLocation(advLocation);
                        }
    
                        PublisherAdRequest adRequest = adRequestBuilder.build();
                        mInterstitialAd.loadAd(adRequest); 
                    }
                }
            }
        });
    }

    @ReactMethod
    public void showAd(final Promise promise) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run () {
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                    promise.resolve(null);
                } else {
                    promise.reject("E_AD_NOT_READY", "Ad is not ready.");
                }
            }
        });
    }

    @ReactMethod
    public void isReady(final Callback callback) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run () {
                callback.invoke(mInterstitialAd.isLoaded());
            }
        });
    }
}
