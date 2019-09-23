import React, { Fragment } from 'react';
import { SafeAreaView, ScrollView, StatusBar } from 'react-native';
import { AdMobBanner } from 'react-native-admob';

const App = () => {
  return (
    <Fragment>
      <StatusBar barStyle="dark-content" />
      <SafeAreaView>
        <ScrollView
          contentInsetAdjustmentBehavior="automatic"
          style={styles.scrollView}
        >
          <AdMobBanner
            adSize="banner"
            adUnitID="ca-app-pub-3940256099942544/2934735716"
          />
        </ScrollView>
      </SafeAreaView>
    </Fragment>
  );
};

export default App;
