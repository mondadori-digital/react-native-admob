//
//  RNAdConfig.m
//  GialloZafferano
//
//  Created by Mauro Di Lalla on 13/01/2021.
//

#import "RNAdConfig.h"
#import <UIKit/UIKit.h>
#import <CriteoPublisherSdk/CriteoPublisherSdk.h>

@implementation RNAdConfig

@synthesize GAM2Criteo;

+ (RNAdConfig *)sharedInstance {
    static RNAdConfig *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
      sharedInstance = [[self alloc] init];
    });
    return sharedInstance;
}

- (void)dealloc {
  // Should never be called, but just here for clarity really.
}
@end