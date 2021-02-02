//
//  RNAdConfig.h
//  GialloZafferano
//
//  Created by Mauro Di Lalla on 13/01/2021.
//

#import <Foundation/Foundation.h>
#import <CriteoPublisherSdk/CriteoPublisherSdk.h>

@interface RNAdConfig : NSObject {
  NSDictionary *GAM2Criteo;
}

@property (nonatomic) NSDictionary *GAM2Criteo;

+(RNAdConfig *)sharedInstance;

@end
