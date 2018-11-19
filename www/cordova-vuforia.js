function CordovaVuforia() {}

CordovaVuforia.prototype.initVuforia = function (options, success, error) {
    cordova.exec(success, error, 'VuforiaPlugin', 'initVuforia', [options]);
}

CordovaVuforia.prototype.startVuforia = function (options, success, error) {
    cordova.exec(success, error, 'VuforiaPlugin', 'startVuforia', [options]);
}

CordovaVuforia.prototype.stopVuforia = function (success, error) {
    cordova.exec(success, error, 'VuforiaPlugin', 'stopVuforia', []);
}

CordovaVuforia.prototype.pauseVuforia = function (success, error) {
    cordova.exec(success, error, 'VuforiaPlugin', 'pauseVuforia', []);
}

CordovaVuforia.prototype.resumeVuforia = function (success, error) {
    cordova.exec(success, error, 'VuforiaPlugin', 'resumeVuforia', []);
}

CordovaVuforia.prototype.setRecognitionType = function (options, success, error) {
    cordova.exec(success, error, 'VuforiaPlugin', 'setVuforiaType', [options]);
}

CordovaVuforia.prototype.setImageTargetModel = function (options, success, error) {
    cordova.exec(success, error, 'VuforiaPlugin', 'setVuforiaImageParam', [options]);
}

CordovaVuforia.prototype.getUserDefinedTarget = function (success, error) {
    cordova.exec(success, error, 'VuforiaPlugin', 'getUserDefinedTargetsFrameQuality', []);
}

CordovaVuforia.prototype.setUserDefinedTargetModel = function (options, success, error) {
    cordova.exec(success, error, 'VuforiaPlugin', 'setUserDefinedTargetsModelParam', [options]);
}

CordovaVuforia.prototype.removeUserDefinedTarget = function (success, error) {
    cordova.exec(success, error, 'VuforiaPlugin', 'cleanUserDefinedTargetsFrameQuality', []);
}

CordovaVuforia.prototype.onTargetFound = function (data) {
    cordova.fireDocumentEvent('CordovaVuforia.onTargetFound', data);
};
var cordovaVuforia = new CordovaVuforia();
if (!window.CordovaVuforia) {
    window.CordovaVuforia = cordovaVuforia;
}

module.exports = cordovaVuforia;