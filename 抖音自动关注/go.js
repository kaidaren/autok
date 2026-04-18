/**
 * ================================================================
 *                   扇形菜单按钮管理 - 操作流程
 * ================================================================
 * 
 * 【增加新图标按钮】
 * 
 * 步骤1：在 MENU_BUTTONS_CONFIG 数组中添加配置
 *   位置：找到文件中的 var MENU_BUTTONS_CONFIG = [...] 数组
 *   格式：
 *     {
 *         id: "id_X",              // 按钮唯一ID（必须唯一，如 id_5, id_6）
 *         icon: "图标路径",         // 支持格式：
 *                                  //   - file://路径（如 "file://./data/icon.png"）
 *                                  //   - @drawable名称（如 "@drawable/ic_help_black_48dp"）
 *                                  //   - {{常量名}}（如 "{{UI_START}}"）
 *         margin: "33 0 0 0",      // 位置边距：格式 "上 右 下 左"（单位：dp）
 *                                  //   示例："33 0 0 0"=顶部33dp
 *                                  //        "86 28 0 0"=顶部86dp，左边28dp
 *                                  //        "0 83 0 0"=右边83dp
 *         gravity: "",             // 重力属性（可选）：
 *                                  //   "" = 默认位置
 *                                  //   "bottom" = 底部对齐
 *                                  //   "right" = 右对齐
 *                                  //   "top" = 顶部对齐
 *         rightOffset: 41,         // 右侧展开时的X轴偏移倍数：
 *                                  //   正数 = 向左偏移（如 41）
 *                                  //   负数 = 向右偏移（如 -65, -106）
 *                                  //   常用值：41, -65, -106
 *         onClick: function() {     // 点击事件处理函数==========================================================================直接放点击产生的效果
 *             // 你的功能代码
 *             toast("新功能提示");
 *             img_down();           // 最后必须调用这个来关闭菜单
 *         }
 *     }
 * 
 * 步骤2：在 XML 中添加对应的按钮 frame
 *   位置：找到 var win = floaty.rawWindow(<frame>...</frame>) 中的 <frame id="id_logo">
 *   在现有按钮 frame 后面添加：
 * 
 *     <frame id="id_X" w="44" h="44" margin="位置边距" alpha="1" gravity="重力属性">
 *         <img w="44" h="44" src="#ffffff" circle="true" />
 *         <img w="28" h="28" src="图标路径" tint="#67cbe9" margin="8" />
 *         <img id="id_X_click" w="*" h="*" src="#67cbe9" circle="true" alpha="0" />
 *     </frame>
 * 
 *   注意：
 *     - id="id_X" 必须与配置中的 id 完全一致
 *     - margin 和 gravity 要与配置中一致
 *     - id_X_click 是点击区域，命名规则：{按钮id}_click
 *     - 图标路径要与配置中的 icon 一致
 * 
 * 完成！系统会自动处理：
 *    绑定点击事件
 *    计算按钮位置
 *    处理展开/收起动画
 * 
 * 
 * 【减少/删除图标按钮】
 * 
 * 步骤1：从 MENU_BUTTONS_CONFIG 数组中删除对应配置
 *   位置：找到 var MENU_BUTTONS_CONFIG = [...] 数组
 *   操作：删除整个 {...} 配置对象，包括前面的逗号（如果删除的不是最后一个）
 * 
 * 步骤2：从 XML 中删除对应的按钮 frame
 *   位置：找到 var win = floaty.rawWindow(<frame>...</frame>) 中的 <frame id="id_logo">
 *   操作：删除整个 <frame id="id_X">...</frame> 标签块
 * 
 * 完成！重启脚本后按钮将被移除
 * 
 * 
 * 【修改现有按钮】
 * 
 * 步骤1：修改配置数组中的对应项
 *   - 修改 icon：更改图标
 *   - 修改 margin：更改位置
 *   - 修改 onClick：更改功能
 * 
 * 步骤2：修改 XML 中对应的 frame
 *   - 修改 margin 属性（要与配置一致）
 *   - 修改图标路径（src 属性）
 * 
 * 
 * 【示例：添加一个"帮助"按钮】
 * 
 * 1. 在配置数组中添加：
 *    {
 *        id: "id_5",
 *        icon: "@drawable/ic_help_black_48dp",
 *        margin: "33 0 83 0",
 *        gravity: "",
 *        rightOffset: 41,
 *        onClick: function() {
 *            toast("帮助功能");
 *            img_down();
 *        }
 *    }
 * 
 * 2. 在 XML 中添加：
 *    <frame id="id_5" w="44" h="44" margin="33 0 83 0" alpha="1">
 *        <img w="44" h="44" src="#ffffff" circle="true" />
 *        <img w="28" h="28" src="@drawable/ic_help_black_48dp" tint="#67cbe9" margin="8" />
 *        <img id="id_5_click" w="*" h="*" src="#67cbe9" circle="true" alpha="0" />
 *    </frame>
 * 
 * 
 * 【注意事项】
 * 
 * 1. id 必须唯一，不能重复
 * 2. 配置数组和 XML 必须一一对应
 * 3. XML 中按钮的 margin 和 gravity 必须与配置一致
 * 4. 点击事件函数最后必须调用 img_down() 来关闭菜单
 * 5. 修改后需要重启脚本才能生效
 * 6. 如果按钮位置不对，可以调整 margin 和 rightOffset 参数
 * 
 * ================================================================
 */

// 导入Android动画类
importClass(java.lang.Runnable);
importClass(android.animation.ObjectAnimator)
importClass(android.animation.PropertyValuesHolder)
importClass(android.animation.ValueAnimator)
importClass(android.animation.AnimatorSet)
importClass(android.view.animation.AccelerateInterpolator)
importClass(android.view.animation.TranslateAnimation)
importClass(android.animation.TimeInterpolator)
importClass(android.view.animation.AccelerateDecelerateInterpolator)
importClass(android.view.animation.BounceInterpolator)
importClass(android.view.animation.DecelerateInterpolator)
importClass(android.view.View)

// 常量定义
const UI_START = './data/s.png'
const UI_CLOSE = './data/t.png'
const UI_LOG = './data/z.png'
const UI_LOG_ALT = "@drawable/ic_show_chart_black_48dp"  // 图表显示图标
const UI_SET_EXIT = "@drawable/ic_cancel_black_48dp";
const USER_ICON = './data/ui.png'
var 屏幕宽 = device.width
var 屏幕高 = device.height

// 全局变量
var logo_switch = false; // 悬浮窗菜单的开启关闭检测
var logo_buys = false; // 开启和关闭时占用状态 防止多次点击触发
var logo_fx = true; // 悬浮按钮所在的方向 真左 假右（false表示右侧）
var XY = [], XY1 = [], TT = [], TT1 = [], img_dp = {}, dpZ = 0, logo_right = 0, dpB = 0;


/**
 * ==========================================
 * 按钮配置模块 - 在这里添加/修改按钮
 * ==========================================
 * 配置说明：
 *   id: 按钮唯一标识（字符串）
 *   icon: 图标路径（支持 file:// 或 @drawable）
 *   margin: 按钮位置边距（字符串，如 "33 0 0 0"）
 *   gravity: 重力属性（字符串，如 "bottom", "right" 等，可选）
 *   rightOffset: 右侧展开时的X轴偏移倍数（数字，相对于dpZ）
 *   onClick: 点击时的回调函数
 */
var MENU_BUTTONS_CONFIG = [
    {
        id: "id_0",
        icon: "file://{{UI_START}}",
        margin: "33 0 0 0",
        gravity: "",
        rightOffset: 41, // 左侧展开时向上偏移
        onClick: function () {
            if (isPlaying) {
                stopScript();
                // 切换为"启动"图标
                ui.run(function() {
                    win.id_0_icon.attr("src", "file://" + UI_START);
                    win.id_0_icon.attr("tint", "#67cbe9");
                });
            } else {
                startScript();
                // 切换为"停止"图标
                ui.run(function() {
                    win.id_0_icon.attr("src", "file://" + UI_CLOSE);
                    win.id_0_icon.attr("tint", "#FF5252");
                });
            }
            img_down();
        }
    },
    {
        id: "id_1",
        icon: "file://{{UI_LOG}}",
        margin: "86 28 0 0",
        gravity: "",
        rightOffset: -65,
        onClick: function () {
            if (!isZxtRunning) {
                // 第一次点击：打开日志窗口
                isZxtRunning = true;
                storage.put("log_win_open", "1");
                engines.execScriptFile("./log_win.js");
                toast("日志窗口已打开");
            } else {
                // 第二次点击：关闭日志窗口
                isZxtRunning = false;
                storage.put("log_win_open", "0");
                // 遍历引擎找到 log_win.js 并停止
                try {
                    var all = engines.all();
                    for (var i = 0; i < all.length; i++) {
                        var src = all[i].getSource();
                        if (src && src.toString().indexOf("log_win.js") !== -1) {
                            all[i].forceStop();
                        }
                    }
                } catch(e) {}
                toast("日志窗口已关闭");
            }
            img_down();
        }
    },
    {
        id: "id_2",
        icon: "{{UI_SET_EXIT}}",
        margin: "0 83 0 0",
        gravity: "right",
        rightOffset: -106, // 左侧展开时向右偏移
        onClick: function () {
            engines.stopAll();
            img_down();
        }
    },

    // {
    //     id: "id_3",
    //     icon: "@drawable/ic_settings_black_48dp",
    //     margin: "86 0 0 28",
    //     gravity: "bottom",
    //     rightOffset: -65, // 左侧展开时向右下偏移
    //     onClick: function() {
    //         toast("设置功能");
    //         img_down();
    //     }
    // },
    // {
    //     id: "id_4",
    //     icon: "@drawable/ic_info_black_48dp",
    //     margin: "33 0 0 0",
    //     gravity: "bottom",
    //     rightOffset: 41, // 左侧展开时向下偏移
    //     onClick: function() {
    //         toast("信息功能");
    //         img_down();
    //     }
    // }



    // 在这里添加更多按钮配置...
    // {
    //     id: "id_5",
    //     icon: "你的图标路径",
    //     margin: "位置边距",
    //     gravity: "位置属性",
    //     rightOffset: 偏移倍数,
    //     onClick: function() {
    //         // 你的点击处理代码
    //         img_down();
    //     }
    // }
];

/**
 * 根据配置动态生成按钮视图
 * 注意：由于Auto.js的限制，XML必须在代码中直接定义
 * 但我们可以通过配置来动态绑定事件和计算位置
 */




// 初始化存储
let storage = storages.create("kaidaren13@163.com");
storage.put("go", "true");


// 动画参数
var logo_ms = 200; // 动画播放时间

/**
 * 需要三个悬浮窗一起协作达到扇形菜单效果
 * win  子菜单悬浮窗 处理子菜单选项点击事件（扇形布局）
 * win_1  主悬浮按钮 
 * win_2  悬浮按钮动画替身,只有在手指移动主按钮的时候才会被触发 
 * 触发时,替身Y值会跟主按钮Y值绑定一起,手指弹起时代替主按钮显示跳动的小球动画
 * 
 * 注意：按钮的XML结构需要手动添加，但位置和点击事件通过配置自动处理
 */
var win = floaty.rawWindow(
    <frame >//子菜单悬浮窗
        <frame id="id_logo" w="150" h="210" alpha="0"  >
            {/* 按钮通过配置自动生成，但需要在这里手动添加XML */}
            {/* 请根据 MENU_BUTTONS_CONFIG 配置手动添加对应的按钮frame */}
            <frame id="id_0" w="44" h="44" margin="33 0 0 0" alpha="1">
                <img w="44" h="44" src="#ffffff" circle="true" />
                <img id="id_0_icon" w="28" h="28" src="file://{{UI_START}}" tint="#67cbe9" gravity="center" layout_gravity="center" />
                <img id="id_0_click" w="*" h="*" src="#67cbe9" circle="true" alpha="0" />
            </frame>
            <frame id="id_1" w="44" h="44" margin="86 28 0 0" alpha="1">
                <img w="44" h="44" src="#ffffff" circle="true" />
                <img w="28" h="28" src="file://{{UI_LOG}}" tint="#67cbe9" gravity="center" layout_gravity="center" />
                <img id="id_1_click" w="*" h="*" src="#67cbe9" circle="true" alpha="0" />
            </frame>
            <frame id="id_2" w="44" h="44" margin="0 83 0 0" alpha="1" gravity="right" layout_gravity="right">
                <img w="44" h="44" src="#ffffff" circle="true" />
                <img w="28" h="28" src="{{UI_SET_EXIT}}" tint="#67cbe9" margin="8" />
                <img id="id_2_click" w="*" h="*" src="#67cbe9" circle="true" alpha="0" />
            </frame>


            {/* <frame id="id_3" w="44" h="44" margin="86 0 0 28" alpha="1" gravity="bottom" layout_gravity="bottom">
                <img w="44" h="44" src="#ffffff" circle="true" />
                <img w="28" h="28" src="@drawable/ic_settings_black_48dp" tint="#67cbe9" margin="8" />
                <img id="id_3_click" w="*" h="*" src="#67cbe9" circle="true" alpha="0" />
            </frame>
            <frame id="id_4" w="44" h="44" margin="33 0 0 0" alpha="1" gravity="bottom" layout_gravity="bottom">
                <img w="44" h="44" src="#ffffff" circle="true" />
                <img w="28" h="28" src="@drawable/ic_info_black_48dp" tint="#67cbe9" margin="8" />
                <img id="id_4_click" w="*" h="*" src="#67cbe9" circle="true" alpha="0" />
            </frame> */}



            {/* 
                添加新按钮的模板（复制以下代码，修改id、margin、gravity和图标）：
                <frame id="id_X" w="44" h="44" margin="位置" alpha="1" gravity="位置属性">
                    <img w="44" h="44" src="#ffffff" circle="true" />
                    <img w="28" h="28" src="图标路径" tint="#67cbe9" margin="8" />
                    <img id="id_X_click" w="*" h="*" src="#67cbe9" circle="true" alpha="0" />
                </frame>
            */}
        </frame>
        <frame id="logo" w="44" h="44" marginTop="83" alpha="1" />
        <frame id="logo_1" w="44" h="44" margin="0 83 22 0" alpha="1" layout_gravity="right" />
    </frame>
)
win.setTouchable(false);//设置子菜单不接收触摸消息

/**
 * 绑定所有按钮的点击事件
 */
function bindButtonClickEvents() {
    MENU_BUTTONS_CONFIG.forEach(function (config) {
        try {
            var clickView = win[config.id + "_click"];
            if (clickView && config.onClick) {
                clickView.on("click", config.onClick);
            }
        } catch (e) {
            console.error("绑定按钮 " + config.id + " 点击事件失败:", e);
        }
    });
}

var win_1 = floaty.rawWindow(
    <frame id="logo" w="44" h="44" alpha="0.4" >//悬浮按钮
        <img w="44" h="44" src="#00000000" circle="true" alpha="0.8" />
        <img id="img_logo" w="32" h="32" src="file://{{USER_ICON}}" gravity="center" layout_gravity="center" />
        <img id="logo_click" w="*" h="*" src="#ffffff" alpha="0" />
    </frame>
)
// 初始化位置：默认在屏幕右侧
win_1.setPosition(0 - 14, device.height / 2) // 悬浮按钮定位（左侧）

var win_2 = floaty.rawWindow(
    <frame id="logo" w="{{device.width}}px" h="44" alpha="0" >//悬浮按钮 弹性替身
        <img w="44" h="44" src="#00000000" circle="true" alpha="0.8" />
        <img id="img_logo" w="32" h="32" src="file://{{USER_ICON}}" margin="6 6" />
    </frame>
)
win_2.setTouchable(false);//设置弹性替身不接收触摸消息

/**
 * 脚本广播事件
 */
events.broadcast.on("定时器关闭", function (X) { clearInterval(X) })
events.broadcast.on("悬浮开关", function (X) {
    ui.run(function () {
        if (X === true || X === "true") {
            win.id_logo.setVisibility(0)
            win.setTouchable(true);
            // 确保所有点击区域都可点击（根据配置动态设置）
            MENU_BUTTONS_CONFIG.forEach(function (config) {
                try {
                    var clickView = win[config.id + "_click"];
                    if (clickView) {
                        clickView.setClickable(true);
                    }
                } catch (e) { }
            });
            logo_switch = true
        } else if (X === false || X === "false" || X === null || X === undefined) {
            win.id_logo.setVisibility(4)
            win.setTouchable(false);
            logo_switch = false
        }
    })
});

events.broadcast.on("悬浮显示", function (X1) {
    ui.run(function () {
        win_2.logo.attr("alpha", "0");
        win_1.logo.attr("alpha", "0.4");
    })
});

/**
 * 等待悬浮窗初始化
 */
var terid = setInterval(() => {
    if (TT.length == 0 && win.logo.getY() > 0) {
        ui.run(function () {
            TT = [win.logo.getX(), win.logo.getY()], TT1 = [win.logo_1.getLeft(), win.logo_1.getTop()], anX = [], anY = []

            // 根据配置自动计算所有按钮的位置
            XY = [];
            XY1 = [];

            MENU_BUTTONS_CONFIG.forEach(function (config, index) {
                try {
                    var buttonView = win[config.id];
                    if (buttonView) {
                        // 计算左侧展开时的位置
                        var deltaX = TT[0] - buttonView.getX();
                        var deltaY = config.id === "id_2" ? 0 : (TT[1] - buttonView.getY()); // id_2 特殊处理
                        XY.push([buttonView, deltaX, deltaY]);

                        // 计算右侧展开时的位置（使用配置中的rightOffset）
                        var rightOffsetX = parseInt(dpZ * config.rightOffset);
                        var rightOffsetY = TT1[1] - buttonView.getTop();
                        XY1.push([rightOffsetX, TT1[0] - buttonView.getLeft(), rightOffsetY]);
                    }
                } catch (e) {
                    console.error("计算按钮 " + config.id + " 位置失败:", e);
                }
            });

            if (XY.length === 0 || XY1.length === 0) {
                log("按钮位置计算失败，重试...");
                return;
            }

            log("上下Y值差值:" + XY[0][2] + "DP值:" + (XY[0][2] / 83))
            dpZ = XY[0][2] / 83
            dpB = dpZ * 22

            // 重新计算XY1（需要先有dpZ）
            XY1 = [];
            MENU_BUTTONS_CONFIG.forEach(function (config) {
                try {
                    var buttonView = win[config.id];
                    if (buttonView) {
                        var rightOffsetX = parseInt(dpZ * config.rightOffset);
                        XY1.push([rightOffsetX, TT1[0] - buttonView.getLeft(), TT1[1] - buttonView.getTop()]);
                    }
                } catch (e) { }
            });

            img_dp.h_b = XY[0][2]
            img_dp.w = parseInt(dpZ * 9)
            img_dp.ww = parseInt(dpZ * (44 - 9))

            // 找到最右侧的按钮（通常是id_2）来计算logo_right
            var rightmostButton = null;
            MENU_BUTTONS_CONFIG.forEach(function (config) {
                try {
                    var btn = win[config.id];
                    if (btn && (!rightmostButton || btn.getX() > rightmostButton.getX())) {
                        rightmostButton = btn;
                    }
                } catch (e) { }
            });
            if (rightmostButton) {
                logo_right = rightmostButton.getX() - parseInt(dpZ * 22);
            }

            // 根据logo_fx设置初始位置：false（右侧）或true（左侧）
            if (logo_fx === true) {
                win_1.setPosition(0 - img_dp.w, device.height / 2) // 左侧
            } else {
                win_1.setPosition(device.width - img_dp.ww, device.height / 2) // 右侧
            }
            // 同步子菜单位置
            win.setPosition(win_1.getX() - (logo_fx === true ? 0 : logo_right), win_1.getY() - img_dp.h_b)
            win.id_logo.setVisibility(4)
            win.id_logo.attr("alpha", "1")

            // 绑定所有按钮的点击事件
            bindButtonClickEvents();

            events.broadcast.emit("定时器关闭", terid)
        })
    }
}, 100)

var time_0 = setInterval(() => {
    // 监听日志窗口被自己关闭，同步状态
    if (storage.get("log_win_closed_by_self") === "1") {
        storage.put("log_win_closed_by_self", "0");
        isZxtRunning = false;
    }
}, 1000)

// 脚本状态
let isPlaying = false;
let isZxtRunning = false;
let mainThread;
let zxtThread;
let logWinEngine = null;

// 折线图图标切换状态
let currentChartIcon = UI_LOG; // 当前使用的图标

/**
 * 子菜单点击事件
 */
function img_down() {
    win_1.logo.attr("alpha", "0.4")
    logo_switch = false
    动画()
}

/**
 * 补间动画 - 扇形菜单展开/收起
 */
function 动画() {
    // 确保XY数组已初始化
    if (!XY || XY.length === 0 || !XY1 || XY1.length === 0) {
        return;
    }

    var anX = [], anY = [], slX = [], slY = []
    if (logo_switch === true) {
        if (logo_fx === true) {
            for (let i = 0; i < XY.length; i++) {
                if (XY[i] && XY[i][0]) {
                    anX[i] = ObjectAnimator.ofFloat(XY[i][0], "translationX", parseInt(XY[i][1] || 0), 0);
                    anY[i] = ObjectAnimator.ofFloat(XY[i][0], "translationY", parseInt(XY[i][2] || 0), 0);
                    slX[i] = ObjectAnimator.ofFloat(XY[i][0], "scaleX", 0, 1)
                    slY[i] = ObjectAnimator.ofFloat(XY[i][0], "scaleY", 0, 1)
                }
            }
        } else {
            for (let i = 0; i < XY.length; i++) {
                if (XY[i] && XY[i][0] && XY1[i]) {
                    anX[i] = ObjectAnimator.ofFloat(XY[i][0], "translationX", XY1[i][1] || 0, XY1[i][0] || 0);
                    anY[i] = ObjectAnimator.ofFloat(XY[i][0], "translationY", XY1[i][2] || 0, 0);
                    slX[i] = ObjectAnimator.ofFloat(XY[i][0], "scaleX", 0, 1)
                    slY[i] = ObjectAnimator.ofFloat(XY[i][0], "scaleY", 0, 1)
                }
            }
        }
    } else {
        if (logo_fx === true) {
            for (let i = 0; i < XY.length; i++) {
                if (XY[i] && XY[i][0]) {
                    anX[i] = ObjectAnimator.ofFloat(XY[i][0], "translationX", 0, parseInt(XY[i][1] || 0));
                    anY[i] = ObjectAnimator.ofFloat(XY[i][0], "translationY", 0, parseInt(XY[i][2] || 0));
                    slX[i] = ObjectAnimator.ofFloat(XY[i][0], "scaleX", 1, 0)
                    slY[i] = ObjectAnimator.ofFloat(XY[i][0], "scaleY", 1, 0)
                }
            }
    } else {
            for (let i = 0; i < XY.length; i++) {
                if (XY[i] && XY[i][0] && XY1[i]) {
                    anX[i] = ObjectAnimator.ofFloat(XY[i][0], "translationX", XY1[i][0] || 0, XY1[i][1] || 0);
                    anY[i] = ObjectAnimator.ofFloat(XY[i][0], "translationY", 0, XY1[i][2] || 0);
                    slX[i] = ObjectAnimator.ofFloat(XY[i][0], "scaleX", 1, 0)
                    slY[i] = ObjectAnimator.ofFloat(XY[i][0], "scaleY", 1, 0)
                }
            }
        }
    }

    // 过滤掉undefined的动画器
    var animators = [];
    for (let i = 0; i < anX.length; i++) {
        if (anX[i] && anY[i] && slX[i] && slY[i]) {
            animators.push(anX[i], anY[i], slX[i], slY[i]);
        }
    }

    if (animators.length === 0) {
        return;
    }

    set = new AnimatorSet();
    // 确保所有动画器都被添加
    if (animators.length > 0) {
        set.playTogether.apply(set, animators);
    }
    set.setDuration(logo_ms);
    threads.start(function () {
        logo_buys = true
        if (logo_switch === true) {
            events.broadcast.emit("悬浮开关", true)
            sleep(logo_ms)
        } else {
            sleep(logo_ms + 100)
            events.broadcast.emit("悬浮开关", false)
        }
        logo_buys = false
    });
    set.start();
}

//记录按键被按下时的触摸坐标
var x = 0, y = 0;
//记录按键被按下时的悬浮窗位置
var windowX = 0, windowY = 0; G_Y = 0
//记录按键被按下的时间以便判断长按等动作
var downTime = 0; yd = false;
//防双击标志
var lastClickTime = 0;
var CLICK_DOUBLE_THRESHOLD = 300; // 双击间隔阈值（毫秒）

/**
 * 主按钮拖动事件 - 优化后的拖动逻辑
 */
win_1.logo.setOnTouchListener(function (view, event) {
    if (logo_buys === true) { return false; }
    switch (event.getAction()) {
        case event.ACTION_DOWN:
            x = event.getRawX();
            y = event.getRawY();
            windowX = win_1.getX();
            windowY = win_1.getY();
            downTime = new Date().getTime();
            return true;
        case event.ACTION_MOVE:
            if (logo_switch === true) { return true; }
            if (yd !== true) {
                // 如果移动的距离大于30像素 则判断为移动
                if (Math.abs(event.getRawY() - y) > 30 || Math.abs(event.getRawX() - x) > 30) {
                    win_1.logo.attr("alpha", "1");
                    yd = true
                }
    } else {
                // 确保 windowX 和 windowY 已初始化
                if (windowX === undefined || windowY === undefined) {
                    return false;
                }
                // 移动手指时调整两个悬浮窗位置
                win_1.setPosition(windowX + (event.getRawX() - x),
                    windowY + (event.getRawY() - y));
                win_2.setPosition(0, windowY + (event.getRawY() - y));
                // 同时移动子菜单悬浮窗
                if (img_dp !== undefined && img_dp.h_b !== undefined && logo_right !== undefined) {
                    var offsetX = (logo_fx === true ? 0 : (logo_right || 0));
                    win.setPosition(windowX + (event.getRawX() - x) - offsetX,
                        windowY + (event.getRawY() - y) - (img_dp.h_b || 0));
                }
            }
            return true;
        case event.ACTION_UP:
            if (logo_buys === true) { return false; }

            // 检查是否双击（防止快速双击导致崩溃）
            var currentTime = Date.now();
            if (currentTime - lastClickTime < CLICK_DOUBLE_THRESHOLD) {
                lastClickTime = 0; // 重置，忽略这次点击
                return false;
            }
            lastClickTime = currentTime;

            // 确保 x 和 y 已初始化
            if (x === undefined || y === undefined) {
                return false;
            }

            // 触摸时间小于 200毫秒 并且移动距离小于30 则判断为 点击
            if (Math.abs(event.getRawY() - y) < 30 && Math.abs(event.getRawX() - x) < 30) {
                if (logo_switch === true) {
                    logo_switch = false
                    win_1.logo.attr("alpha", "0.4")
                    动画()
                } else if (logo_fx === true) {
                    if (img_dp !== undefined && img_dp.h_b !== undefined) {
                        win.setPosition(windowX + (event.getRawX() - x),
                            windowY + (event.getRawY() - y) - (img_dp.h_b || 0));
                        win.id_logo.setVisibility(0)
                        logo_switch = true
                        win_1.logo.attr("alpha", "0.9")
                        动画()
                    }
                } else {
                    if (img_dp !== undefined && img_dp.h_b !== undefined && logo_right !== undefined) {
                        win.setPosition(win_1.getX() + (event.getRawX() - x) - (logo_right || 0),
                            win_1.getY() + (event.getRawY() - y) - (img_dp.h_b || 0));
                        win.id_logo.setVisibility(0)
                        logo_switch = true
                        win_1.logo.attr("alpha", "0.9")
                        动画()
                    }
                }
            } else if (logo_switch !== true) {
                // 移动后弹起 - 使用BounceInterpolator弹跳效果
                G_Y = windowY + (event.getRawY() - y)
                win_1.logo.attr("alpha", "0.4")

                if (img_dp !== undefined && img_dp.w !== undefined && img_dp.ww !== undefined && img_dp.h_b !== undefined && logo_right !== undefined) {
                    // 确保 windowX 和 x 已初始化
                    if (windowX === undefined || x === undefined) {
                        return false;
                    }

                    if (windowX + (event.getRawX() - x) < device.width / 2) {
                        // 吸附到左边
                        logo_fx = true
                        var targetX = 0 - (img_dp.w || 0);
                        animator = ObjectAnimator.ofFloat(win_2.logo, "translationX", windowX + (event.getRawX() - x), targetX);
                        mTimeInterpolator = new BounceInterpolator();
                        animator.setInterpolator(mTimeInterpolator);
                        animator.setDuration(300);
                        win_2.logo.attr("alpha", "0.4")
                        win_1.logo.attr("alpha", "0");
                        win_1.setPosition(targetX, G_Y)
                        animator.start();
                    } else {
                        // 吸附到右边
                        logo_fx = false
                        var targetX = device.width - (img_dp.ww || 0);
                        animator = ObjectAnimator.ofFloat(win_2.logo, "translationX", windowX + (event.getRawX() - x), targetX);
                        mTimeInterpolator = new BounceInterpolator();
                        animator.setInterpolator(mTimeInterpolator);
                        animator.setDuration(300);
                        win_2.logo.attr("alpha", "0.4")
                        win_1.logo.attr("alpha", "0");
                        win_1.setPosition(targetX, G_Y)
                        animator.start();
                    }
                    threads.start(function () {
                        logo_buys = true
                        sleep(300 + 100)
                        events.broadcast.emit("悬浮显示", 0)
                        // 同步子菜单位置
                        if (img_dp !== undefined && img_dp.h_b !== undefined && logo_right !== undefined) {
                            var offsetX = (logo_fx === true ? 0 : (logo_right || 0));
                            win.setPosition(win_1.getX() - offsetX, win_1.getY() - (img_dp.h_b || 0));
                        }
                        logo_buys = false
                    });
                }
            }
            yd = false
            return true;
    }
    return true;
});

// 启动脚本
function startScript() {
    isPlaying = true;
    toast("开始运行");

    mainThread = threads.start(function () {
        device.keepScreenOn();
        startNewScript("LOCATION", "star.js");
        files.write("/sdcard/s.txt", "1");
        waitForScriptEnd();
        toastLog("结束运行");
        isPlaying = false;
    });

    monitorScriptStatus();
}

// 停止脚本
function stopScript() {
    isPlaying = false;
    toastLog("停止脚本...");

    // 如果main.js正在运行，也一起停止
    if (isZxtRunning) {
        isZxtRunning = false;

        // 停止zxt线程
        if (zxtThread) {
            zxtThread.interrupt();
            zxtThread = null;
        }

        // 发送停止信号给main.js
        try {
            storage.put("zxt_control_command", "stop");
            console.log("已发送停止信号给main.js");
        } catch (e) {
            console.error("发送停止信号失败:", e);
        }

        toast("同时停止数据可视化窗口");
    }

    stopOtherScripts();
    threads.shutDownAll();
}

// 等待脚本结束
function waitForScriptEnd() {
    storage.put("star_id", 'true')
    sleep(1000);
    while (true) {
        if (storage.get("star_id") == 'false') {
            break
        }
        sleep(10);
    }
}

// 停止其他脚本
function stopOtherScripts() {
    try {
        events.setKeyInterceptionEnabled("volume_up", false);
        let scriptEngines = engines.all();
        for (let engine of scriptEngines) {
            let src = engine.getSource();
            if (src && src.toString().indexOf("star.js") !== -1) {
                engine.forceStop();
            }
            // 兼容 execScript 启动的 LOCATION 名称
            if (engine.toString().indexOf("LOCATION") !== -1) {
                engine.forceStop();
            }
        }
    } catch (e) {
        console.error(e);
    }
}

// 启动新脚本
function startNewScript(name, path) {
    stopOtherScripts();
    threads.start(function () {
        let code = files.read(path);
        engines.execScript(name, code);
    });
}

// 监控脚本状态
function monitorScriptStatus() {
    let monitorInterval = setInterval(function () {
        if (!isPlaying) {
            clearInterval(monitorInterval);
        }
    }, 1000);
}



// 保持脚本运行
setInterval(() => { }, 1000);