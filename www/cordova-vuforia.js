var exec = require('cordova/exec');
var channel = require('cordova/channel');

function CordovaVuforia() {
    console.log('插件调用');
    this.pluginClass = 'VuforiaPlugin';
}

/**
 * 初始化vuforia服务
 * @param {*} options 传参
 * @param {*} options.vuforiaLicense vuforia证书
 * @param {*} success 成功回调
 * @param {*} error   失败回调
 */
CordovaVuforia.prototype.initVuforia = function (options, success, error) {
	console.log('插件初始化-initVuforia', this.pluginClass);
    cordova.exec(success, error, this.pluginClass, 'initVuforia', [options]);
}

/**
 * 启动vuforia服务
 * @param {*} options 传参
 * @param {*} options.camera 摄像头类型 0-后置摄像头 1-前置摄像头
 * @param {*} options.type Vuforia识别模式 0-图像识别 1-用户自定义图像识别 2-云图像识别 3-圆柱体识别 4-预指定框识别 5-多源识别 6-文字识别 7-虚拟按钮 8-智能地形
 * @param {*} success 成功回调
 * @param {*} error   失败回调
 */
CordovaVuforia.prototype.startVuforia = function (options, success, error) {
    console.log('插件初始化-startVuforia');
    cordova.exec(success, error, this.pluginClass, 'startVuforia', [options]);
}

/**
 * 关闭vuforia服务
 * @param {*} success 成功回调
 * @param {*} error   失败回调
 */
CordovaVuforia.prototype.stopVuforia = function (success, error) {
    cordova.exec(success, error, this.pluginClass, 'stopVuforia', []);
}

/**
 * 暂停vuforia服务
 * @param {*} success 成功回调
 * @param {*} error   失败回调
 */
CordovaVuforia.prototype.pauseVuforia = function (success, error) {
    cordova.exec(success, error, this.pluginClass, 'pauseVuforia', []);
}

/**
 * 恢复vuforia服务
 * @param {*} success 成功回调
 * @param {*} error   失败回调
 */
CordovaVuforia.prototype.resumeVuforia = function (success, error) {
    cordova.exec(success, error, this.pluginClass, 'resumeVuforia', []);
}

/**
 * 设置vuforia识别类型
 * @param {*} options 传参
 * @param {*} options.type Vuforia识别模式 0-图像识别 1-用户自定义图像识别 2-云图像识别 3-圆柱体识别 4-预指定框识别 5-多源识别 6-文字识别 7-虚拟按钮 8-智能地形
 * @param {*} success 成功回调
 * @param {*} error   失败回调
 */
CordovaVuforia.prototype.setVuforiaType = function (options, success, error) {
    cordova.exec(success, error, this.pluginClass, 'setVuforiaType', [options]);
}

/**
 * 设置本地识别模型
 * @param {*} options 传参
 * @param {List} options.target 识别对象列表
 * @param {List} options.target.targetList 需要识别的图片以及跟踪模型
 * @param {*} options.target.targetList.imageName 识别图片
 * @param {*} options.target.targetList.model 跟踪模型 矩阵、贴图
 * @param {*} options.target.targetList.model.matrix 矩阵
 * @param {*} options.target.targetList.model.map 贴图
 * @param {*} options.target.filepath 文件路径
 * @param {*} success 成功回调
 * @param {*} error   失败回调
 */
CordovaVuforia.prototype.setVuforiaImageParam = function (options, success, error) {
    cordova.exec(success, error, this.pluginClass, 'setVuforiaImageParam', [options]);
}

/**
 * 更新本地识别模型
 * @param {*} options 传参
 * @param {List} options.target 识别对象列表
 * @param {List} options.target.targetList 需要识别的图片以及跟踪模型
 * @param {*} options.target.targetList.imageName 识别图片
 * @param {*} options.target.targetList.model 跟踪模型 矩阵、贴图
 * @param {*} options.target.targetList.model.matrix 矩阵
 * @param {*} options.target.targetList.model.map 贴图
 * @param {*} options.target.filepath 文件路径
 * @param {*} success 成功回调
 * @param {*} error   失败回调
 */
CordovaVuforia.prototype.updateVuforiaModelParam = function (options, success, error) {
    cordova.exec(success, error, this.pluginClass, 'updateVuforiaModelParam', [options]);
}

/**
 * 创建自定义识别
 * @param {*} success 成功回调
 * @param {*} error   失败回调
 */
CordovaVuforia.prototype.getUserDefinedTargetsFrameQuality = function (success, error) {
    cordova.exec(success, error, this.pluginClass, 'getUserDefinedTargetsFrameQuality', []);
}

/**
 * 设置自定义识别模型
 * @param {*} options 传参
 * @param {List} options.target 识别对象列表
 * @param {List} options.target.targetList 需要识别的图片以及跟踪模型
 * @param {*} options.target.targetList.model 跟踪模型 矩阵、贴图
 * @param {*} options.target.targetList.model.matrix 矩阵
 * @param {*} options.target.targetList.model.map 贴图
 * @param {*} success 成功回调
 * @param {*} error   失败回调
 */
CordovaVuforia.prototype.setUserDefinedTargetsModelParam = function (options, success, error) {
    cordova.exec(success, error, this.pluginClass, 'setUserDefinedTargetsModelParam', [options]);
}

/**
 * 清除自定义识别
 * @param {*} success 成功回调
 * @param {*} error   失败回调
 */
CordovaVuforia.prototype.cleanUserDefinedTargetsFrameQuality = function (success, error) {
    cordova.exec(success, error, this.pluginClass, 'cleanUserDefinedTargetsFrameQuality', []);
}

CordovaVuforia.prototype.onNetStatusChange = function (status, imageName) {
    CordovaVuforia.fireDocumentEvent('VuforiaPlugin.onNetStatusChange', {
        status: status,
        imageName: imageName
    });
};

if (!window.CordovaVuforia) {
    window.CordovaVuforia = new CordovaVuforia();
}

module.exports = new CordovaVuforia();