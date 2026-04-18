var global = this;

runtime.init();

(function () {
    //重定向importClass使得其支持字符串参数
    global.importClass =
        (function () {
            var __importClass__ = importClass;
            return function (pack) {
                if (typeof (pack) == "string") {
                    __importClass__(Packages[pack]);
                } else {
                    __importClass__(pack);
                }
            }
        })();

    //内部函数
    global.__asGlobal__ = function (obj, functions) {
        var len = functions.length;
        for (var i = 0; i < len; i++) {
            var funcName = functions[i];
            var func = obj[funcName]
            if (!func) {
                continue;
            }
            global[funcName] = func.bind(obj);
        }
    }


    // 初始化基础模块
    global.timers = require('__timers__.js')(runtime, global);

    //初始化不依赖环境的模块
    global.util = global.$util = require('__util__.js');
    global.device = runtime.device;
    global.keyboard = Object.create(runtime.keyboard);
    // Compatibility: expose v6-style images APIs in global scope.
    if (runtime.getImages) {
        var javaImages = runtime.getImages();
        function parseColorCompat(color) {
            if (typeof color === "number") return color | 0;
            var s = String(color || "").trim();
            if (s.indexOf("#") === 0) s = s.substring(1);
            if (s.length === 6) s = "FF" + s;
            var n = java.lang.Long.parseLong(s, 16);
            return Number(n) | 0;
        }
        function toIntArrayForMultiColors(paths) {
            var list = java.lang.reflect.Array.newInstance(java.lang.Integer.TYPE, paths.length * 3);
            for (var i = 0; i < paths.length; i++) {
                var p = paths[i];
                list[i * 3] = p[0];
                list[i * 3 + 1] = p[1];
                list[i * 3 + 2] = parseColorCompat(p[2]);
            }
            return list;
        }
        function buildRegion(region, img) {
            if (!region || region.length < 2) return null;
            var x = region[0] || 0;
            var y = region[1] || 0;
            var w = region[2];
            var h = region[3];
            if (w === undefined) w = img.width - x;
            if (h === undefined) h = img.height - y;
            return new org.opencv.core.Rect(x, y, w, h);
        }
        function normalizeOrientation(value) {
            var ScreenCapturer = com.stardust.autojs.core.image.capture.ScreenCapturer;
            // v6 style: boolean landscape flag
            if (value === true) return ScreenCapturer.ORIENTATION_LANDSCAPE;
            if (value === false) return ScreenCapturer.ORIENTATION_PORTRAIT;
            // explicit orientation constants or 0/1/2
            if (typeof value === "number") return value;
            return ScreenCapturer.ORIENTATION_AUTO;
        }
        var imagesCompat = {
            requestScreenCapture: function (landscapeOrOrientation) {
                var orientation = normalizeOrientation(landscapeOrOrientation);
                return javaImages.requestScreenCapture(orientation);
            },
            captureScreen: function () {
                var raw = javaImages.captureScreen();
                try {
                    // Always return a script-side safe copy for OpenCV operations.
                    // This keeps v6-style test scripts stable across bitmap backends.
                    return javaImages.copy(raw);
                } finally {
                    if (raw) raw.recycle();
                }
            },
            read: function (path) { return javaImages.read(path); },
            copy: function (img) { return javaImages.copy(img); },
            load: function (src) { return javaImages.load(src); },
            clip: function (img, x, y, w, h) { return javaImages.clip(img, x, y, w, h); },
            pixel: function (img, x, y) { return javaImages.pixel(img, x, y); },
            save: function (img, path, format, quality) {
                return javaImages.save(img, path, format || "png", quality === undefined ? 100 : quality);
            },
            stopScreenCapturer: function () { return javaImages.stopScreenCapturer(); },
            findColor: function (img, color, options) {
                javaImages.initOpenCvIfNeeded();
                options = options || {};
                var threshold = options.threshold === undefined ? 4 : options.threshold;
                var region = options.region ? buildRegion(options.region, img) : null;
                var safeImg = javaImages.copy(img);
                try {
                    return javaImages.colorFinder.findColor(safeImg, parseColorCompat(color), threshold, region);
                } finally {
                    if (safeImg && safeImg !== img) {
                        safeImg.recycle();
                    }
                }
            },
            findColorInRegion: function (img, color, x, y, width, height, threshold) {
                return imagesCompat.findColor(img, color, {
                    region: [x, y, width, height],
                    threshold: threshold
                });
            },
            findColorEquals: function (img, color, x, y, width, height) {
                return imagesCompat.findColor(img, color, {
                    region: [x, y, width, height],
                    threshold: 0
                });
            },
            findMultiColors: function (img, firstColor, paths, options) {
                javaImages.initOpenCvIfNeeded();
                options = options || {};
                var region = options.region ? buildRegion(options.region, img) : null;
                var threshold = options.threshold === undefined ? 4 : options.threshold;
                var list = toIntArrayForMultiColors(paths || []);
                var safeImg = javaImages.copy(img);
                try {
                    return javaImages.colorFinder.findMultiColors(
                        safeImg,
                        parseColorCompat(firstColor),
                        threshold,
                        region,
                        list
                    );
                } finally {
                    if (safeImg && safeImg !== img) {
                        safeImg.recycle();
                    }
                }
            }
        };
        global.images = imagesCompat;
        global.$images = imagesCompat;
        global.requestScreenCapture = imagesCompat.requestScreenCapture;
        global.captureScreen = imagesCompat.captureScreen;
        global.findColor = imagesCompat.findColor;
        global.findColorInRegion = imagesCompat.findColorInRegion;
        global.findColorEquals = imagesCompat.findColorEquals;
        global.findMultiColors = imagesCompat.findMultiColors;
    }

    global.process = require('process')
    global.Promise = require('bluebird');


    //初始化全局函数
    require("__globals__")(runtime, global);

    require("object-observe-lite.min")();
    require("array-observe.min")();
    //初始化一般模块
    (function (scope) {
        var modules = ['console', "RootAutomator"];
        var len = modules.length;
        for (var i = 0; i < len; i++) {
            var m = modules[i];
            let module = require('__' + m + '__')(scope.runtime, scope);
            scope[m] = module;
            if (!m.startsWith('$')) {
                scope['$' + m] = module;
            }
        }
    })(global);

    require("/android_asset/v6modules/init.js")

    // Compatibility: some remote runtimes do not expose global click/press helpers.
    // Fallback to automator methods so legacy scripts can call click(x, y) directly.
    if (typeof global.click !== "function") {
        var autoObj = global.automator || global.$automator || runtime.automator;
        if (autoObj && typeof autoObj.click === "function") {
            global.click = autoObj.click.bind(autoObj);
        }
    }

    function safeBindGlobal(name, sourceObj, sourceKey) {
        if (typeof global[name] === "function" || global[name] !== undefined) return;
        if (!sourceObj) return;
        var fn = sourceObj[sourceKey || name];
        if (typeof fn === "function") {
            global[name] = fn.bind(sourceObj);
        } else if (fn !== undefined) {
            global[name] = fn;
        }
    }

    function resolveSelectorObject() {
        if (global.selector) return global.selector;
        if (global.$selector) return global.$selector;
        if (!runtime) return null;
        try {
            // In some remote execution paths runtime.selector may be a method, not an object.
            if (typeof runtime.selector === "function") {
                return runtime.selector();
            }
        } catch (e) {
        }
        return runtime.selector || null;
    }

    // Common doc globals fallback map for remote/runtime edge cases.
    (function ensureDocGlobals() {
        var autoObj = global.automator || global.$automator || runtime.automator;
        var appObj = global.app || global.$app || runtime.app;
        var selObj = resolveSelectorObject();
        var imagesObj = global.images || global.$images;
        var keys = [
            "click", "longClick", "press", "swipe",
            "gesture", "gestures", "gestureAsync", "gesturesAsync",
            "input", "setText", "scrollUp", "scrollDown",
            "back", "home", "recents", "powerDialog", "notifications", "quickSettings", "splitScreen",
            "Back", "Home", "Power", "Menu", "VolumeUp", "VolumeDown", "Camera", "Up", "Down", "Left", "Right", "OK", "Text", "KeyCode"
        ];
        for (var i = 0; i < keys.length; i++) {
            safeBindGlobal(keys[i], autoObj);
        }
        safeBindGlobal("launch", appObj);
        safeBindGlobal("launchApp", appObj);
        safeBindGlobal("currentPackage", appObj);
        safeBindGlobal("currentActivity", appObj);
        safeBindGlobal("waitForPackage", appObj);
        safeBindGlobal("waitForActivity", appObj);

        if (selObj) {
            var selectorGlobals = [
                "id", "idContains", "idStartsWith", "idEndsWith", "idMatches",
                "text", "textContains", "textStartsWith", "textEndsWith", "textMatches",
                "desc", "descContains", "descStartsWith", "descEndsWith", "descMatches",
                "className", "classNameContains", "classNameStartsWith", "classNameEndsWith", "classNameMatches",
                "packageName", "packageNameContains", "packageNameStartsWith", "packageNameEndsWith", "packageNameMatches",
                "bounds", "boundsInside", "boundsContains"
            ];
            for (var j = 0; j < selectorGlobals.length; j++) {
                safeBindGlobal(selectorGlobals[j], selObj);
            }
        }

        if (imagesObj) {
            safeBindGlobal("requestScreenCapture", imagesObj);
            safeBindGlobal("captureScreen", imagesObj);
            safeBindGlobal("findColor", imagesObj);
            safeBindGlobal("findColorInRegion", imagesObj);
            safeBindGlobal("findColorEquals", imagesObj);
            safeBindGlobal("findMultiColors", imagesObj);
            safeBindGlobal("findImage", imagesObj);
            safeBindGlobal("findImageInRegion", imagesObj);
        }
    })();

    // Machine-readable global API baseline from project docs index.
    (function exposeDocGlobalApiBaseline() {
        if (global.__docGlobalApiBaseline__) return;
        try {
            var text = files.read("/android_asset/indices/all.json");
            var index = JSON.parse(String(text || "[]"));
            var baseline = [];
            for (var i = 0; i < index.length; i++) {
                var group = index[i];
                var props = group && group.properties || [];
                for (var j = 0; j < props.length; j++) {
                    var item = props[j];
                    if (item && item.global === true && item.key) {
                        baseline.push({
                            group: group.name || "",
                            key: item.key,
                            summary: item.summary || "",
                            url: item.url || ""
                        });
                    }
                }
            }
            global.__docGlobalApiBaseline__ = baseline;
        } catch (e) {
            global.__docGlobalApiBaseline__ = [];
        }
    })();

    (function bindMissingFromDocBaseline() {
        var baseline = global.__docGlobalApiBaseline__ || [];
        if (!baseline.length) {
            global.__docGlobalApiMissing__ = [];
            return;
        }
        var autoObj = global.automator || global.$automator || runtime.automator;
        var appObj = global.app || global.$app || runtime.app;
        var selectorObj = resolveSelectorObject();
        var imagesObj = global.images || global.$images;
        var filesObj = global.files || global.$files || runtime.files;
        var timersObj = global.timers || global.$timers;
        var keysObj = autoObj;
        var groupMap = {
            automator: [autoObj],
            selector: [selectorObj],
            images: [imagesObj],
            app: [appObj],
            files: [filesObj],
            timers: [timersObj],
            keys: [keysObj],
            globals: [global, appObj, autoObj, selectorObj, imagesObj, filesObj, timersObj]
        };
        var fallbackSources = [global, autoObj, appObj, selectorObj, imagesObj, filesObj, timersObj];
        for (var i = 0; i < baseline.length; i++) {
            var item = baseline[i];
            var key = item.key;
            if (global[key] !== undefined) continue;
            var sources = groupMap[item.group] || fallbackSources;
            for (var j = 0; j < sources.length; j++) {
                safeBindGlobal(key, sources[j]);
                if (global[key] !== undefined) break;
            }
            if (global[key] === undefined) {
                for (var k = 0; k < fallbackSources.length; k++) {
                    safeBindGlobal(key, fallbackSources[k]);
                    if (global[key] !== undefined) break;
                }
            }
        }
        var missing = [];
        for (var n = 0; n < baseline.length; n++) {
            var b = baseline[n];
            if (global[b.key] === undefined) missing.push(b.key);
        }
        global.__docGlobalApiMissing__ = missing;
    })();

})();


