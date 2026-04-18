/**
 * log_win.js - 日志悬浮窗（穿透点击版）
 */

var storage = storages.create("douyin_helper");
var LOG_FILE = "/sdcard/dy_log.txt";

// 确保日志文件存在
if (!files.exists(LOG_FILE)) {
    files.create(LOG_FILE);
}

var transparency = "#00000000";
var bgColor = "#FFFFFFFF";  // 白色背景

var win = floaty.rawWindow(
    <card cardBackgroundColor={bgColor} cardCornerRadius="10" cardElevation="2">
        <vertical id="root" w="280" h="120" bg={bgColor} padding="6 4 6 4">
            <horizontal w="*" h="28" gravity="center_vertical">
                <text text=" 运行日志" textColor="#1E88E5" textSize="11sp" textStyle="bold" layout_weight="1" />
                <text id="followCountText" text="已关注: 0 人" textColor="#F44336" textSize="11sp" textStyle="bold" />
            </horizontal>
            <horizontal w="*" h="28" gravity="left_vertical">
                {/* 倒计时单独一行，实时刷新 */}
                <text id="countdownText" textSize="12sp" text="" textColor="#FF9800" textStyle="bold" />
            </horizontal>
            <scroll id="scrollView" layout_weight="1" w="*" h="*">
                <text id="logText" textSize="10sp" text="" textColor="#333333" lineSpacingExtra="2dp" />
            </scroll>
        </vertical>
    </card>
);

win.setPosition(10, 50);
win.setTouchable(false);

// ========== 读取日志文件最后N行 ==========
function readLastLines(n) {
    if (!files.exists(LOG_FILE)) return "";
    var content = files.read(LOG_FILE);
    if (!content) return "";
    var lines = content.split("\n").filter(function (l) { return l.trim() !== ""; });
    if (lines.length > n) lines = lines.slice(lines.length - n);
    return lines.join("\n");
}

// ========== 定时刷新 ==========
var scrollView = win.scrollView;

setInterval(function () {
    var txt = readLastLines(20);
    var countdown = storage.get("log_countdown") || "";
    var followCount = storage.get("follow_count") || "0";
    ui.run(function () {
        win.countdownText.setText(countdown);
        win.logText.setText(txt);
        win.followCountText.setText("已关注: " + followCount + " 人");
        scrollView.post(function () {
            scrollView.fullScroll(android.view.View.FOCUS_DOWN);
        });
    });
}, 800);

storage.put("log_win_open", "1");
storage.put("log_win_engines", engines.myEngine().toString());
