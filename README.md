# cordova-vuforia
Vuforia SDK 插件（暂时只支持Android）

## 安装 Installation
`cordova plugin add cordova-vuforia`

## 使用 Usage
### 初始化

`CordovaVuforia.initVuforia(options, success, error)`
* `options` 初始化参数，包含vuforiaLicense
* `options.vuforiaLicense` vuforia证书
* `success` 初始化成功回调
* `error` 初始化失败回调

>调用initVuforia方法需要在准备开始使用vuforia之前

### 开启Vuforia

`CordovaVuforia.startVuforia(options, success, error)`
* `options` 开启参数,包含camera，type
* `options.camera` 摄像头类型 0-后置摄像头 1-前置摄像头
* `options.type` Vuforia识别模式 0-本地图像识别 1-用户自定义图像识别
* `success` 开启成功回调
* `error` 开启失败回调
  
>在调用startVuforia成功之后，需要将前端网页的背景颜色设置为透明，并保证没有多余的元素，否则会遮挡住vuforia画面。这是因为WebView层在原生层的上层，如果WebView有背景色，就会挡住下面的层。这么做也是为了方便前端可以在上层添加一些操作按钮等。

>参照以下设置
```
body.vuforia-show {
  background-color: transparent !important;
}
```

### 关闭vuforia

`CordovaVuforia.stopVuforia(success, error)`
* `success` 关闭成功回调
* `error` 关闭失败回调

### 设置vuforia识别类型
`CordovaVuforia.setRecognitionType(options, success, error)`
* `options` 识别类型参数，包含type
* `options.type` Vuforia识别模式 0-本地图像识别 1-用户自定义图像识别
* `success` 设置成功回调
* `error` 设置失败回调

### 设置本地图像识别模型
`CordovaVuforia.setImageTargetModel(options, success, error)`
* `options` 本地识别参数，包含imageName,matrix,map,filepath
* `options.imageName` 识别的图片名字
* `options.matrix` 识别的矩阵包含 posX,posY,posZ,scaleX,scaleY,scaleZ,rotate
* `options.matrix.posX` x轴偏移量
* `options.matrix.posY` y轴偏移量
* `options.matrix.posZ` z轴偏移量
* `options.matrix.scaleX` x轴缩放比
* `options.matrix.scaleY` y轴缩放比
* `options.matrix.scaleZ` z轴缩放比
* `options.matrix.rotate` 旋转角度(逆时针)
* `options.map` 贴图文件地址
* `options.filepath` 识别配置文件地址
* `success` 设置成功回调
* `error` 设置失败回调

### 创建自定义识别目标
`CordovaVuforia.getUserDefinedTarget(success, error)`
* `success` 创建成功回调
* `error` 创建失败回调

### 设置自定义识别模型
`CordovaVuforia.setUserDefinedTargetModel(options, success, error)`
* `options` 本地识别参数，包含matrix,map
* `options.matrix` 识别的矩阵包含 posX,posY,posZ,scaleX,scaleY,scaleZ,rotate
* `options.matrix.posX` x轴偏移量
* `options.matrix.posY` y轴偏移量
* `options.matrix.posZ` z轴偏移量
* `options.matrix.scaleX` x轴缩放比
* `options.matrix.scaleY` y轴缩放比
* `options.matrix.scaleZ` z轴缩放比
* `options.matrix.rotate` 旋转角度(逆时针)
* `options.map` 贴图文件地址
* `success` 设置成功回调
* `error` 设置失败回调

### 清除自定义识别目标
`CordovaVuforia.removeUserDefinedTarget(success, error)`
* `success` 清除成功回调
* `error` 清除失败回调

### 识别事件 CordovaVuforia.onTargetFound
`document.addEventListener('CordovaVuforia.onTargetFound', function(data) {})`
* `data` 识别之后的回调，包含status,imageName
* `data.status` 识别状态 1-识别到目标 0-当前识别目标丢失
* `data.imageName` 识别到目标的名字（仅在status为1时存在）