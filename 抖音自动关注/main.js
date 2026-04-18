"ui";

importClass(android.graphics.Color);
importClass(android.view.View);
activity.setTheme(com.google.android.material.R$style.Theme_MaterialComponents_DayNight_DarkActionBar);
ui.statusBarColor(Color.parseColor("#F5F7FA"));
activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

var 标题 = '抖音助手';
var 软件版本 = '1.0.0';
var storage = storages.create("douyin_helper");

let currentView = null;
let currentNavIndex = 0;

// ========== 导航栏高亮 ==========
function updateNavHighlight(activeIndex) {
    var navButtons = [
        { img: ui.nav_img_0, text: ui.nav_text_0, pill: ui.nav_pill_0, dot: ui.nav_dot_0 },
        { img: ui.nav_img_1, text: ui.nav_text_1, pill: ui.nav_pill_1, dot: ui.nav_dot_1 },
        { img: ui.nav_img_2, text: ui.nav_text_2, pill: ui.nav_pill_2, dot: ui.nav_dot_2 },
    ];
    var labels = [" 主页", " 日志", " 我的"];
    navButtons.forEach(function(nav, index) {
        if (index === activeIndex) {
            nav.pill.attr("cardBackgroundColor", "#E8F1FD");
            nav.img.attr("tint", "#1E88E5");
            nav.text.attr("textColor", "#1E88E5");
            nav.text.attr("textStyle", "bold");
            nav.text.setText(labels[index]);
            nav.dot.attr("bg", "#1E88E5");
        } else {
            nav.pill.attr("cardBackgroundColor", "#00000000");
            nav.img.attr("tint", "#AAAAAA");
            nav.text.attr("textColor", "#AAAAAA");
            nav.text.attr("textStyle", "normal");
            nav.text.setText(labels[index]);
            nav.dot.attr("bg", "#00000000");
        }
    });
    currentNavIndex = activeIndex;
}

function showView(view, navIndex) {
    if (currentView) currentView.setVisibility(android.view.View.GONE);
    view.setVisibility(android.view.View.VISIBLE);
    currentView = view;
    if (navIndex !== undefined) updateNavHighlight(navIndex);
}

// ========== 日志 ==========
var logData = [];
var logIndex = 0;
function addLog(msg) {
    logIndex++;
    logData.unshift({ 序号: logIndex, 日志内容: msg });
    if (logData.length > 200) logData.pop();
    ui.run(function() {
        ui.logList.setDataSource(logData);
    });
}

// ========== 读取/保存配置 ==========
function saveConfig() {
    storage.put("follow_min", ui.input_follow_count.getText().toString());
    storage.put("follow_max", ui.input_follow_count_max.getText().toString());
    storage.put("interval_min", ui.input_interval_min.getText().toString());
    storage.put("interval_max", ui.input_interval_max.getText().toString());
    storage.put("stay_min", ui.input_stay_min.getText().toString());
    storage.put("stay_max", ui.input_stay_max.getText().toString());
    storage.put("emoji_interval", ui.input_emoji_interval.getText().toString());
    storage.put("switch_count", ui.input_switch_count.getText().toString());
    storage.put("comments", ui.input_comments.getText().toString());
    storage.put("dm_content", ui.input_dm.getText().toString());
    storage.put("sw_follow", ui.sw_follow.isChecked() ? "1" : "0");
    storage.put("sw_like", ui.sw_like.isChecked() ? "1" : "0");
    storage.put("sw_comment", ui.sw_comment.isChecked() ? "1" : "0");
    storage.put("sw_dm", ui.sw_dm.isChecked() ? "1" : "0");
}

function loadConfig() {
    ui.input_follow_count.setText(storage.get("follow_min") || "130");
    ui.input_follow_count_max.setText(storage.get("follow_max") || "150");
    ui.input_interval_min.setText(storage.get("interval_min") || "3");
    ui.input_interval_max.setText(storage.get("interval_max") || "8");
    ui.input_stay_min.setText(storage.get("stay_min") || "5");
    ui.input_stay_max.setText(storage.get("stay_max") || "15");
    ui.input_emoji_interval.setText(storage.get("emoji_interval") || "5");
    ui.input_switch_count.setText(storage.get("switch_count") || "150");
    var comments = storage.get("comments");
    if (comments) ui.input_comments.setText(comments);
    var dm = storage.get("dm_content");
    if (dm) ui.input_dm.setText(dm);
    ui.sw_follow.setChecked(storage.get("sw_follow") !== "0");
    ui.sw_like.setChecked(storage.get("sw_like") !== "0");
    ui.sw_comment.setChecked(storage.get("sw_comment") !== "0");
    ui.sw_dm.setChecked(storage.get("sw_dm") !== "0");
}

// ========== 布局 ==========
ui.layout(
<vertical>
        <vertical w="*" h="0dp" layout_weight="1">

            {/* ===== 主页 ===== */}
            <vertical id="view1" h="*" bg="#F5F7FA" visibility="gone">
                <vertical w="*" h="auto" bg="#FFFFFF" padding="14 12 14 12" elevation="2">
                    <horizontal gravity="center">
                        <text text={标题} textColor="#000000" textSize="17sp" textStyle="bold" gravity="center" />
                        <text text={" v" + 软件版本} textColor="#1E88E5" textSize="11sp" gravity="center" margin="4 0 0 0" />
                    </horizontal>
                </vertical>

                <ScrollView w="*" h="*">
                    <vertical w="*" padding="12 12 12 80">

                        {/* 运行参数 */}
                        <card cardBackgroundColor="#ffffff" margin="0 0 0 10" cardCornerRadius="12" cardElevation="2">
                            <vertical w="*" padding="14 14 14 14">
                                <horizontal gravity="center_vertical" margin="0 0 0 12">
                                    <View w="3dp" h="16dp" bg="#1E88E5" margin="0 0 8 0" />
                                    <text text="运行参数" textSize="14sp" textColor="#333333" textStyle="bold" />
                                </horizontal>
                                <horizontal gravity="center_vertical" margin="0 0 0 10">
                                    <text text="目标关注数" textSize="13sp" textColor="#555555" layout_weight="1" />
                                    <input id="input_follow_count" w="70dp" h="36dp" text="130" inputType="number" bg="#F8FAFE" textSize="13sp" gravity="center" />
                                    <text text=" ~ " textSize="13sp" textColor="#555555" />
                                    <input id="input_follow_count_max" w="70dp" h="36dp" text="150" inputType="number" bg="#F8FAFE" textSize="13sp" gravity="center" />
                                    <text text=" 个" textSize="13sp" textColor="#555555" margin="4 0 0 0" />
                                </horizontal>
                                <horizontal gravity="center_vertical" margin="0 0 0 10">
                                    <text text="操作间隔(秒)" textSize="13sp" textColor="#555555" layout_weight="1" />
                                    <input id="input_interval_min" w="60dp" h="36dp" text="1" inputType="number" bg="#F8FAFE" textSize="13sp" gravity="center" />
                                    <text text=" ~ " textSize="13sp" textColor="#555555" />
                                    <input id="input_interval_max" w="60dp" h="36dp" text="3" inputType="number" bg="#F8FAFE" textSize="13sp" gravity="center" />
                                    <text text=" 秒" textSize="13sp" textColor="#555555" margin="4 0 0 0" />
                                </horizontal>
                                <horizontal gravity="center_vertical">
                                    <text text="视频停留(秒)" textSize="13sp" textColor="#555555" layout_weight="1" />
                                    <input id="input_stay_min" w="60dp" h="36dp" text="5" inputType="number" bg="#F8FAFE" textSize="13sp" gravity="center" />
                                    <text text=" ~ " textSize="13sp" textColor="#555555" />
                                    <input id="input_stay_max" w="60dp" h="36dp" text="15" inputType="number" bg="#F8FAFE" textSize="13sp" gravity="center" />
                                    <text text=" 秒" textSize="13sp" textColor="#555555" margin="4 0 0 0" />
                                </horizontal>
                            </vertical>
                        </card>

                        {/* 操作开关 */}
                        <card cardBackgroundColor="#ffffff" margin="0 0 0 10" cardCornerRadius="12" cardElevation="2">
                            <vertical w="*" padding="14 14 14 14">
                                <horizontal gravity="center_vertical" margin="0 0 0 12">
                                    <View w="3dp" h="16dp" bg="#1E88E5" margin="0 0 8 0" />
                                    <text text="操作开关" textSize="14sp" textColor="#333333" textStyle="bold" />
                                </horizontal>
                                <horizontal gravity="center_vertical" margin="0 0 0 8">
                                    <text text="自动关注" textSize="13sp" textColor="#555555" layout_weight="1" />
                                    <Switch id="sw_follow" checked="true" thumbTint="#1E88E5" trackTint="#E3F2FD" />
                                </horizontal>
                                <horizontal gravity="center_vertical" margin="0 0 0 8">
                                    <text text="自动点赞" textSize="13sp" textColor="#555555" layout_weight="1" />
                                    <Switch id="sw_like" checked="true" thumbTint="#1E88E5" trackTint="#E3F2FD" />
                                </horizontal>
                                <horizontal gravity="center_vertical" margin="0 0 0 8">
                                    <text text="自动评论" textSize="13sp" textColor="#555555" layout_weight="1" />
                                    <Switch id="sw_comment" checked="true" thumbTint="#1E88E5" trackTint="#E3F2FD" />
                                </horizontal>
                                <horizontal gravity="center_vertical">
                                    <text text="回关私信" textSize="13sp" textColor="#555555" layout_weight="1" />
                                    <Switch id="sw_dm" checked="true" thumbTint="#1E88E5" trackTint="#E3F2FD" />
                                </horizontal>
                            </vertical>
                        </card>

                        {/* 评论内容 */}
                        <card cardBackgroundColor="#ffffff" margin="0 0 0 10" cardCornerRadius="12" cardElevation="2">
                            <vertical w="*" padding="14 14 14 14">
                                <horizontal gravity="center_vertical" margin="0 0 0 8">
                                    <View w="3dp" h="16dp" bg="#1E88E5" margin="0 0 8 0" />
                                    <text text="评论内容" textSize="14sp" textColor="#333333" textStyle="bold" layout_weight="1" />
                                    <text text="一行一条" textSize="11sp" textColor="#757575" />
                                </horizontal>
                                <card cardBackgroundColor="#F8FAFE" cardCornerRadius="8" cardElevation="0">
                                    <input id="input_comments" h="100dp" w="*" bg="#F8FAFE" textSize="13sp" textColor="#333333"
                                        gravity="left|top" padding="10 8 10 8"
                                        text="哇好厉害！&#10;太棒了！&#10;支持一下&#10;学到了&#10;继续加油" />
                                </card>
                                <horizontal gravity="center_vertical" margin="0 8 0 0">
                                    <text text="每" textSize="13sp" textColor="#555555" />
                                    <input id="input_emoji_interval" w="48dp" h="42dp" text="5" inputType="number" bg="#F8FAFE" textSize="13sp" gravity="center" margin="4 0 4 0" />
                                    <text text="条穿插一个表情包" textSize="13sp" textColor="#555555" />
                                </horizontal>
                            </vertical>
                        </card>

                        {/* 私信内容 */}
                        <card cardBackgroundColor="#ffffff" margin="0 0 0 10" cardCornerRadius="12" cardElevation="2">
                            <vertical w="*" padding="14 14 14 14">
                                <horizontal gravity="center_vertical" margin="0 0 0 8">
                                    <View w="3dp" h="16dp" bg="#1E88E5" margin="0 0 8 0" />
                                    <text text="私信内容（回关后发送）" textSize="14sp" textColor="#333333" textStyle="bold" layout_weight="1" />
                                    <text text="一行一条" textSize="11sp" textColor="#757575" />
                                </horizontal>
                                <card cardBackgroundColor="#F8FAFE" cardCornerRadius="8" cardElevation="0">
                                    <input id="input_dm" h="80dp" w="*" bg="#F8FAFE" textSize="13sp" textColor="#333333"
                                        gravity="left|top" padding="10 8 10 8"
                                        text="你好，感谢关注！" />
                                </card>
                            </vertical>
                        </card>

                        {/* 换号配置 */}
                        <card cardBackgroundColor="#ffffff" margin="0 0 0 10" cardCornerRadius="12" cardElevation="2">
                            <vertical w="*" padding="14 14 14 14">
                                <horizontal gravity="center_vertical" margin="0 0 0 8">
                                    <View w="3dp" h="16dp" bg="#FF9800" margin="0 0 8 0" />
                                    <text text="换号配置" textSize="14sp" textColor="#333333" textStyle="bold" layout_weight="1" />
                                </horizontal>
                                <horizontal gravity="center_vertical">
                                    <text text="关注满" textSize="13sp" textColor="#555555" />
                                    <input id="input_switch_count" w="60dp" h="36dp" text="150" inputType="number" bg="#F8FAFE" textSize="13sp" gravity="center" margin="4 0 4 0" />
                                    <text text="个后自动换号" textSize="13sp" textColor="#555555" />
                                </horizontal>
                            </vertical>
                        </card>

                        {/* 启动/停止按钮 */}
                        <card id="btn_start_card" cardBackgroundColor="#1E88E5" cardCornerRadius="12" cardElevation="3" margin="0 0 0 16">
                            <button w="*" h="54dp" bg="#1E88E5" id="btn_start" text="  开始运行" textColor="#FFFFFF" textStyle="bold" textSize="17sp" />
                        </card>

                    </vertical>
                </ScrollView>
            </vertical>

            {/* ===== 日志页 ===== */}
            <vertical bg="#F5F7FA" id="view2" h="*" padding="12" visibility="gone">
                <horizontal bg="#FFFFFF" w="*" h="48dp" padding="14 0 8 0" gravity="center_vertical" margin="0 0 0 10" elevation="1">
                    <text text="运行日志" textSize="15sp" textColor="#000000" textStyle="bold" layout_weight="1" />
                    <horizontal id="btn_clear_log" gravity="center_vertical" padding="8 0 12 0">
                        <img src="file://./data/icon_43gkh6zdy4a/clear.png" w="16dp" h="16dp" />
                        <text text=" 清空" textSize="13sp" textColor="#1E88E5" textStyle="bold" />
                    </horizontal>
                </horizontal>
                <card cardBackgroundColor="#ffffff" cardCornerRadius="12" cardElevation="2">
                    <list id="logList" h="*">
                        <horizontal bg="#FFFFFF" padding="14 10 14 10">
                            <View w="3dp" h="*" bg="#1E88E5" margin="0 0 10 0" />
                            <text id="序号" text="{{序号}}:" textColor="#1E88E5" textStyle="bold" textSize="11sp" w="28dp" />
                            <text id="日志内容" text="{{日志内容}}" textColor="#333333" textSize="12sp" layout_weight="1" />
                        </horizontal>
                    </list>
                </card>
            </vertical>

            {/* ===== 我的 ===== */}
            <vertical id="view3" h="*" bg="#F5F7FA" visibility="gone">
                <vertical w="*" h="auto" bg="#FFFFFF" padding="14 12 14 12" elevation="2">
                    <text text="个人中心" textSize="15sp" textColor="#000000" textStyle="bold" gravity="center" />
                </vertical>
                <ScrollView>
                    <vertical padding="12 10 12 80">
                        <card cardBackgroundColor="#ffffff" margin="0 0 0 10" cardCornerRadius="12" cardElevation="2">
                            <vertical w="*" h="auto">
                                <horizontal w="*" padding="16 14 16 14" gravity="center_vertical">
                                    <vertical layout_weight="1">
                                        <text text="无障碍服务" textColor="#222222" textStyle="bold" textSize="14sp" />
                                        <text text="提供自动操作功能(点击、长按、滑动等)" textColor="#999999" textSize="11sp" margin="0 2 0 0" />
                                    </vertical>
                                    <Switch id="switch_acc" checked="false" thumbTint="#1E88E5" trackTint="#E3F2FD" />
                                </horizontal>
                                <View w="*" h="1dp" bg="#F5F5F5" />
                                <horizontal w="*" padding="16 14 16 14" gravity="center_vertical">
                                    <vertical layout_weight="1">
                                        <text text="悬浮球权限" textColor="#222222" textStyle="bold" textSize="14sp" />
                                        <text text="增加脚本后台运行时的存活率" textColor="#999999" textSize="11sp" margin="0 2 0 0" />
                                    </vertical>
                                    <Switch id="switch_ball" checked="false" thumbTint="#FF5722" trackTint="#FFEBEE" />
                                </horizontal>
                                <View w="*" h="1dp" bg="#F5F5F5" />
                                <horizontal w="*" padding="16 14 16 14" gravity="center_vertical">
                                    <vertical layout_weight="1">
                                        <text text="忽略电池优化" textColor="#222222" textStyle="bold" textSize="14sp" />
                                        <text text="防止手机异常关闭本软件" textColor="#999999" textSize="11sp" margin="0 2 0 0" />
                                    </vertical>
                                    <Switch id="settings_youhua" checked="false" thumbTint="#FF9800" trackTint="#FFF3E0" />
                                </horizontal>
                            </vertical>
                        </card>
                        <text h="100dp" />
                    </vertical>
                </ScrollView>
            </vertical>

        </vertical>

        {/* ===== 底部导航栏 ===== */}
        <vertical w="*" h="auto" bg="#FFFFFF" elevation="8">
            <View w="*" h="1dp" bg="#EEEEEE" />
            <horizontal w="*" h="58dp">
                {/* 主页 */}
                <vertical id="nav_btn_0" w="0dp" h="*" layout_weight="1" gravity="center" padding="0 6 0 4">
                    <card id="nav_pill_0" cardBackgroundColor="#E8F1FD" cardCornerRadius="16" cardElevation="0" w="auto">
                        <horizontal gravity="center" padding="14 3 14 3">
                            <img id="nav_img_0" src="file://./data/icon_43gkh6zdy4a/shouye1-xuanzhong.png" w="18dp" h="18dp" tint="#1E88E5" />
                            <text id="nav_text_0" text=" 主页" textSize="12sp" textColor="#1E88E5" textStyle="bold" />
                        </horizontal>
                    </card>
                    <text id="nav_dot_0" text="" h="3dp" />
                </vertical>
                {/* 日志 */}
                <vertical id="nav_btn_1" w="0dp" h="*" layout_weight="1" gravity="center" padding="0 6 0 4">
                    <card id="nav_pill_1" cardBackgroundColor="#00000000" cardCornerRadius="16" cardElevation="0" w="auto">
                        <horizontal gravity="center" padding="14 3 14 3">
                            <img id="nav_img_1" src="file://./data/icon_43gkh6zdy4a/faxian-weixuanzhong.png" w="18dp" h="18dp" tint="#AAAAAA" />
                            <text id="nav_text_1" text=" 日志" textSize="12sp" textColor="#AAAAAA" />
                        </horizontal>
                    </card>
                    <text id="nav_dot_1" text="" h="3dp" />
                </vertical>
                {/* 我的 */}
                <vertical id="nav_btn_2" w="0dp" h="*" layout_weight="1" gravity="center" padding="0 6 0 4">
                    <card id="nav_pill_2" cardBackgroundColor="#00000000" cardCornerRadius="16" cardElevation="0" w="auto">
                        <horizontal gravity="center" padding="14 3 14 3">
                            <img id="nav_img_2" src="file://./data/icon_43gkh6zdy4a/huiyuan-weixuanzhong.png" w="18dp" h="18dp" tint="#AAAAAA" />
                            <text id="nav_text_2" text=" 我的" textSize="12sp" textColor="#AAAAAA" />
                        </horizontal>
                    </card>
                    <text id="nav_dot_2" text="" h="3dp" />
                </vertical>
            </horizontal>
        </vertical>
    </vertical>
);

// ========== 事件绑定 ==========
var isRunning = false;

// 导航切换
ui.nav_btn_0.on("click", function() { showView(ui.view1, 0); });
ui.nav_btn_1.on("click", function() { showView(ui.view2, 1); });
ui.nav_btn_2.on("click", function() { showView(ui.view3, 2); });

// 清空日志
ui.btn_clear_log.on("click", function() {
    logData = [];
    logIndex = 0;
    ui.logList.setDataSource(logData);
});

// 权限开关
ui.switch_acc.on("check", function(checked) {
    if (checked) {
        app.startActivity({ action: "android.settings.ACCESSIBILITY_SETTINGS" });
    }
});
ui.switch_ball.on("check", function(checked) {
    if (checked) {
        app.startActivity({ action: "android.settings.action.MANAGE_OVERLAY_PERMISSION" });
    }
});
ui.settings_youhua.on("check", function(checked) {
    if (checked) {
        try {
            app.startActivity({
                action: "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
                data: "package:com.douyiomn.sports.cn"
            });
        } catch (e) {
            // 部分设备不支持直接跳转，退回到电池优化列表页
            try {
                app.startActivity({ action: "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS" });
            } catch (e2) {
                toast("请手动在系统设置中关闭电池优化");
            }
        }
    }
});

// 启动/停止
var goEngine = null; // 记录 go.js 的引擎实例

function stopGoEngine() {
    // 先用记录的实例停
    if (goEngine !== null) {
        try { goEngine.forceStop(); } catch(e) {}
        goEngine = null;
    }
    // 再遍历兜底，杀掉所有名为 go.js 的引擎
    try {
        var all = engines.all();
        for (var i = 0; i < all.length; i++) {
            var e = all[i];
            if (e.getSource() && e.getSource().toString().indexOf("go.js") !== -1) {
                e.forceStop();
            }
        }
    } catch(e) {}
}

ui.btn_start.on("click", function() {
    if (!isRunning) {
        saveConfig();
        // 启动前先确保旧的 go.js 已关闭
        stopGoEngine();
        isRunning = true;
        ui.btn_start.setText("  停止运行");
        ui.btn_start_card.attr("cardBackgroundColor", "#F44336");
        ui.btn_start.attr("bg", "#F44336");
        addLog(" 开始运行...");
        storage.put("cmd", "start");
        storage.put("star_running", "1");
        goEngine = engines.execScriptFile("./go.js");
    } else {
        isRunning = false;
        ui.btn_start.setText("  开始运行");
        ui.btn_start_card.attr("cardBackgroundColor", "#1E88E5");
        ui.btn_start.attr("bg", "#1E88E5");
        addLog(" 已停止");
        storage.put("cmd", "stop");
        storage.put("star_running", "0");
        // 直接强制停止 go.js
        stopGoEngine();
    }
});

// 监听 star.js 回传日志
threads.start(function() {
    var lastLog = "";
    while (true) {
        var msg = storage.get("log_msg");
        if (msg && msg !== lastLog) {
            lastLog = msg;
            addLog(msg);
        }
        // star.js 自然结束时重置按钮
        if (isRunning && storage.get("star_running") === "0") {
            isRunning = false;
            stopGoEngine();
            ui.run(function() {
                ui.btn_start.setText("  开始运行");
                ui.btn_start_card.attr("cardBackgroundColor", "#1E88E5");
                ui.btn_start.attr("bg", "#1E88E5");
            });
        }
        sleep(500);
    }
});

// ========== 试用期校验 ==========
var EXPIRE_DATE = "2026-04-18"; // 修改此处设置到期日期 (年-月-日)

function checkExpire() {
    var now = new Date();
    var expire = new Date(EXPIRE_DATE);
    if (now >= expire) {
        ui.post(function() {
            dialogs.alert("授权已过期", "试用期已于 " + EXPIRE_DATE + " 到期，请联系开发者续期。").then(function() {
                activity.finish();
            });
        });
        return false;
    }
    // 计算剩余天数
    var days = Math.ceil((expire - now) / (1000 * 60 * 60 * 24));
    toast("试用期剩余 " + days + " 天");
    return true;
}

// ========== 初始化 ==========
ui.post(function() {
    if (!checkExpire()) return;
    loadConfig();
    showView(ui.view1, 0);
});
