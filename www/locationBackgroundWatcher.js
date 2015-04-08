var exec = require("cordova/exec");
module.exports = {

    config: {},

    configure: function(success, failure, config) {
        this.config = config;
        var latitude            = config.latitude,
            longitude		    = config.longitude,
            distanceToAlarm     = config.distanceToAlarm,
            debug               = config.debug || false,
            notificationTitle   = config.notificationTitle || "Background tracking",
            notificationText    = config.notificationText || "ENABLED";
            stopOnTerminate     = config.stopOnTerminate || false;

        exec(success || function() {},
             failure || function() {},
             'locationBackgroundWatcher',
             'configure',
             [latitude, longitude, distanceToAlarm, debug, notificationTitle, notificationText, stopOnTerminate]
        );
    },
    start: function(success, failure, config) {
        exec(success || function() {},
             failure || function() {},
             'locationBackgroundWatcher',
             'start',
             []);
    },
    stop: function(success, failure, config) {
        exec(success || function() {},
            failure || function() {},
            'locationBackgroundWatcher',
            'stop',
            []);
    },
    getlocation: function(success, failure, config) {
        exec(success || function() {},
            failure || function() {},
            'locationBackgroundWatcher',
            'location',
            []);
    }


};
