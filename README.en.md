# cordova-vuforia
Vuforia plugin for Cordova.(The first version only support Android.)

![npm](https://img.shields.io/npm/v/cordova-vuforia.svg)
![taobaonpm](https://npm.taobao.org/badge/v/cordova-vuforia.svg)

## Installation
`cordova plugin add cordova-vuforia`

## Usage
### Initialize Vufofia

`CordovaVuforia.initVuforia(options, success, error)`
* `options` 
* `options.vuforiaLicense` license of vuforia developer
* `success` callback when success
* `error` callback when failure

>`initVuforia` should be used at first when you decide to start vuforia.

### Start Vuforia

`CordovaVuforia.startVuforia(options, success, error)`
* `options`
* `options.camera` which camera to use, "0" for back-camera, "1" for front-camera
* `options.type` target recognition mode, "0" for image-target-recognition, "1" for user-defined-target-recognition
* `success` callback when success
* `error` callback when failure
  
>You should set the background of the webpage to "transparent" after `startVuforia` to make sure the camera view can be shown. Because the "WebView" layer is above the native layer and the camera layer. So if there is any background colors or elements, it will block the camera, and you might see nothing worked.

>stylesheet example
```
body.vuforia-show {
  background-color: transparent !important;
}
```

### Stop Vuforia

`CordovaVuforia.stopVuforia(success, error)`
* `success` callback when success
* `error` callback when failure

### Set the recognition mode of vuforia
`CordovaVuforia.setRecognitionType(options, success, error)`
* `options`
* `options.type` target recognition mode, "0" for image-target-recognition, "1" for user-defined-target-recognition
* `success` callback when success
* `error` callback when failure

### Set the Image-Target model
`CordovaVuforia.setImageTargetModel(options, success, error)`
* `options`
* `options.imageName` Image-Target name
* `options.matrix` matrix
* `options.matrix.posX` offset of "x" axis
* `options.matrix.posY` offset of "y" axis
* `options.matrix.posZ` offset of "z" axis
* `options.matrix.scaleX` scale percent of "x" axis
* `options.matrix.scaleY` scale percent of "y" axis
* `options.matrix.scaleZ` scale percent of "z" axis
* `options.matrix.rotate` rotation angle (anticlockwise)
* `options.map` the model's filepath, stored at `/models/`
* `options.filepath` the filepath of target's configure, stored at `/targets/`, such as `StoneAndChips.xml`
* `success` callback when success
* `error` callback when failure

### Get User-Defined-Target
`CordovaVuforia.getUserDefinedTarget(success, error)`
* `success` callback when success
* `error` callback when failure

### Set User-Defined-Target model
`CordovaVuforia.setUserDefinedTargetModel(options, success, error)`
* `options.matrix` matrix
* `options.matrix.posX` offset of "x" axis
* `options.matrix.posY` offset of "y" axis
* `options.matrix.posZ` offset of "z" axis
* `options.matrix.scaleX` scale percent of "x" axis
* `options.matrix.scaleY` scale percent of "y" axis
* `options.matrix.scaleZ` scale percent of "z" axis
* `options.matrix.rotate` rotation angle (anticlockwise)
* `options.map` the model's filepath, stored at `/models/`
* `success` callback when success
* `error` callback when failure

### Remove User-Defined-Target
`CordovaVuforia.removeUserDefinedTarget(success, error)`
* `success` callback when success
* `error` callback when failure

### Event when target be found
`document.addEventListener('CordovaVuforia.onTargetFound', function(data) {})`
* `data`
* `data.status` the status for whether target was found or not. "1" for found, "0" for not or losing target.
* `data.imageName` Image-Target name. (only return when found, and "status" is "1")

### TODO
* support iOS

## Demo
Ionic 3 Demo [https://github.com/ztl19930409/ionic-for-cordova-vuforia](https://github.com/ztl19930409/ionic-for-cordova-vuforia)


