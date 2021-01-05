#import "RNAdMobInterstitial.h"
#import "RNAdMobUtils.h"

#if __has_include(<React/RCTUtils.h>)
#import <React/RCTUtils.h>
#else
#import "RCTUtils.h"
#endif

static NSString *const kEventAdLoaded = @"interstitialAdLoaded";
static NSString *const kEventAdFailedToLoad = @"interstitialAdFailedToLoad";
static NSString *const kEventAdOpened = @"interstitialAdOpened";
static NSString *const kEventAdFailedToOpen = @"interstitialAdFailedToOpen";
static NSString *const kEventAdClosed = @"interstitialAdClosed";
static NSString *const kEventAdLeftApplication = @"interstitialAdLeftApplication";

@implementation RNAdMobInterstitial
{
    DFPInterstitial  *_interstitial;
    NSString *_adUnitID;
    NSString *_amazonSlotUUID;
    NSArray *_testDevices;
    RCTPromiseResolveBlock _requestAdResolve;
    RCTPromiseRejectBlock _requestAdReject;
    BOOL hasListeners;
    BOOL _npa;
    NSDictionary *_location;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup
{
    return NO;
}

RCT_EXPORT_MODULE();

- (NSArray<NSString *> *)supportedEvents
{
    return @[
             kEventAdLoaded,
             kEventAdFailedToLoad,
             kEventAdOpened,
             kEventAdFailedToOpen,
             kEventAdClosed,
             kEventAdLeftApplication ];
}

#pragma mark exported methods

RCT_EXPORT_METHOD(setAdUnitID:(NSString *)adUnitID amazonSlotUUID:(NSString *)amazonSlotUUID)
{
    _adUnitID = adUnitID;
    _amazonSlotUUID = amazonSlotUUID;
}

RCT_EXPORT_METHOD(setTestDevices:(NSArray *)testDevices)
{
    _testDevices = RNAdMobProcessTestDevices(testDevices, kGADSimulatorID);
}

RCT_EXPORT_METHOD(setNpa:(BOOL *)npa)
{
    _npa = npa;
}

RCT_EXPORT_METHOD(setLocation:(NSDictionary *)location)
{
    _location = location;
}

RCT_EXPORT_METHOD(requestAd:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    _requestAdResolve = nil;
    _requestAdReject = nil;

    if ([_interstitial hasBeenUsed] || _interstitial == nil) {
        _requestAdResolve = resolve;
        _requestAdReject = reject;

        //NSLog(@"DTB - andSlotUUID:%@", self.amazonSlotUUID);
        // AMAZON request
        if (_amazonSlotUUID) {
            DTBAdSize *size = [[DTBAdSize alloc] initInterstitialAdSizeWithSlotUUID:_amazonSlotUUID];
            DTBAdLoader *adLoader = [DTBAdLoader new];
            [adLoader setSizes:size, nil];
            [adLoader loadAd:self];
        } else {
            [self requestInterstitial];
        }
    } else {
        reject(@"E_AD_ALREADY_LOADED", @"Ad is already loaded.", nil);
    }
}

RCT_EXPORT_METHOD(showAd:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    if ([_interstitial isReady]) {
        [_interstitial presentFromRootViewController:[UIApplication sharedApplication].delegate.window.rootViewController];
        resolve(nil);
    }
    else {
        reject(@"E_AD_NOT_READY", @"Ad is not ready.", nil);
    }
}

RCT_EXPORT_METHOD(isReady:(RCTResponseSenderBlock)callback)
{
    callback(@[[NSNumber numberWithBool:[_interstitial isReady]]]);
}

- (void)startObserving
{
    hasListeners = YES;
}

- (void)stopObserving
{
    hasListeners = NO;
}

- (void)requestInterstitial {
    _interstitial = [[DFPInterstitial alloc] initWithAdUnitID:_adUnitID];
    _interstitial.delegate = self;

    DFPRequest *request = [DFPRequest request];
    
    // adv consent
    if (_npa) {
        GADExtras *extras = [[GADExtras alloc] init];
        extras.additionalParameters = @{@"npa": @"1"};
        [request registerAdNetworkExtras:extras];
    }

    // localizzazione
    if (_location) {
        [request setLocationWithLatitude:[_location[@"latitude"] doubleValue]
        longitude:[_location[@"longitude"] doubleValue]
        accuracy:[_location[@"accuracy"] doubleValue]];
    }
    
    request.testDevices = _testDevices;
    [_interstitial loadRequest:request];
}

#pragma mark - <DTBAdCallback>
- (void)onFailure: (DTBAdError)error {
    NSLog(@"DBT - Interstitial Failed to load ad :(");
    [self requestInterstitial];
}
 
- (void)onSuccess: (DTBAdResponse *)adResponse {
    NSLog(@"DTB - Interstitial Loaded :)");
    // Code from Google Ad Manager to set up Google Ad Manager's ad view.
    _interstitial = [[DFPInterstitial alloc] initWithAdUnitID:_adUnitID];
    _interstitial.delegate = self;

    DFPRequest *request = [DFPRequest request];
 
    // Add APS Keywords.
    request.customTargeting = adResponse.customTargeting;
    [_interstitial loadRequest:request];
}

#pragma mark DFPInterstitialDelegate

- (void)interstitialDidReceiveAd:(__unused DFPInterstitial *)ad
{
    if (hasListeners) {
        [self sendEventWithName:kEventAdLoaded body:nil];
    }
    _requestAdResolve(nil);
}

- (void)interstitial:(__unused DFPInterstitial *)interstitial didFailToReceiveAdWithError:(GADRequestError *)error
{
    if (hasListeners) {
        NSDictionary *jsError = RCTJSErrorFromCodeMessageAndNSError(@"E_AD_REQUEST_FAILED", error.localizedDescription, error);
        [self sendEventWithName:kEventAdFailedToLoad body:jsError];
    }
    _interstitial = nil;
    _requestAdReject(@"E_AD_REQUEST_FAILED", error.localizedDescription, error);
}

- (void)interstitialWillPresentScreen:(__unused DFPInterstitial *)ad
{
    if (hasListeners){
        [self sendEventWithName:kEventAdOpened body:nil];
    }
}

- (void)interstitialDidFailToPresentScreen:(__unused DFPInterstitial *)ad
{
    if (hasListeners){
        [self sendEventWithName:kEventAdFailedToOpen body:nil];
    }
    _interstitial = nil;
}

- (void)interstitialWillDismissScreen:(__unused DFPInterstitial *)ad
{
    if (hasListeners) {
        [self sendEventWithName:kEventAdClosed body:nil];
    }
}

- (void)interstitialWillLeaveApplication:(__unused DFPInterstitial *)ad
{
    if (hasListeners) {
        [self sendEventWithName:kEventAdLeftApplication body:nil];
    }
}

@end
