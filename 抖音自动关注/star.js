/**
 * star.js - 抖音自动化执行框架
 * 具体节点操作由使用者填充（标注了 TODO 的位置）
 */

auto();

// ========== 试用期校验 ==========
var EXPIRE_DATE = "2026-04-18"; // 与 main.js 保持一致
(function () {
    var now = new Date();
    var expire = new Date(EXPIRE_DATE);
    if (now >= expire) {
        toast("授权已过期，请联系开发者续期");
        exit();
    }
})();

if (!requestScreenCapture(0)) {
    console.log("请求截图权限失败");
    exit();
}
sleep(2000); // 等待截图权限授予

let ocr = $ocr.create({
    models: "slim", // 指定精度相对低但速度更快的模型，若不指定则为default模型，精度高一点但速度慢一点
});


// ========== 读取配置 ==========
var storage = storages.create("douyin_helper");

var cfg = {
    followMin: parseInt(storage.get("follow_min") || "130"),
    followMax: parseInt(storage.get("follow_max") || "150"),
    intervalMin: parseInt(storage.get("interval_min") || "3") * 1000,
    intervalMax: parseInt(storage.get("interval_max") || "8") * 1000,
    stayMin: parseInt(storage.get("stay_min") || "5") * 1000,
    stayMax: parseInt(storage.get("stay_max") || "15") * 1000,
    emojiInterval: parseInt(storage.get("emoji_interval") || "5"),
    switchCount: parseInt(storage.get("switch_count") || "150"),
    doFollow: storage.get("sw_follow") !== "0",
    doLike: storage.get("sw_like") !== "0",
    doComment: storage.get("sw_comment") !== "0",
    doDM: storage.get("sw_dm") !== "0",
    comments: (storage.get("comments") || "哇好厉害！\n太棒了！\n支持一下").split("\n").filter(function (s) { return s.trim() !== ""; }),
    dmContent: (storage.get("dm_content") || "你好，感谢关注！").split("\n").filter(function (s) { return s.trim() !== ""; }),
};

// 目标关注数（随机取范围内的值）
var targetFollowCount = cfg.followMin + Math.floor(Math.random() * (cfg.followMax - cfg.followMin + 1));

// ========== 状态变量 ==========
var followCount = 0;      // 本轮已关注数
var commentCount = 0;     // 本轮已评论数（用于穿插表情包）
var usedEmojis = [];      // 已使用的表情包索引（防重复）
var isRunning = true;
var commentPingPongDir = 1;  // 乒乓方向：1=正向，-1=反向
var commentPingPongIdx = 0;  // 当前乒乓索引

// ========== 工具函数 ==========

var LOG_FILE = "/sdcard/dy_log.txt";

/** 向日志文件和 storage 写日志 */
function logMsg(msg) {
    var line = "[" + new Date().toLocaleTimeString() + "] " + msg;
    log(line);
    storage.put("log_msg", line);
    try {
        files.append(LOG_FILE, line + "\n");
    } catch (e) { }
}

// 启动时清空日志文件和关注计数
try { files.write(LOG_FILE, ""); } catch (e) { }
storage.put("follow_count", "0");

/** 随机整数 [min, max] */
function randInt(min, max) {
    return min + Math.floor(Math.random() * (max - min + 1));
}

/** 随机延时 */
function randSleep(minMs, maxMs) {
    sleep(randInt(minMs, maxMs));
}

/** 检查是否收到停止指令 */
function checkStop() {
    if (storage.get("cmd") === "stop") {
        isRunning = false;
        return true;
    }
    return false;
}

/**
 * 获取下一条评论内容
 * 每隔 cfg.emojiInterval 条穿插一个不重复的表情包
 */
/**
 * 乒乓索引取评论：1→2→3→3→2→1→1→2→3...
 */
function getNextComment() {
    var len = cfg.comments.length;
    var text = cfg.comments[commentPingPongIdx];

    // 推进乒乓索引
    commentPingPongIdx += commentPingPongDir;
    if (commentPingPongIdx >= len) {
        // 到达末尾，反向，停在最后一个
        commentPingPongDir = -1;
        commentPingPongIdx = len - 1;
    } else if (commentPingPongIdx < 0) {
        // 到达开头，正向，停在第一个
        commentPingPongDir = 1;
        commentPingPongIdx = 0;
    }

    commentCount++;
    // 每隔 N 条穿插表情包
    if (commentCount % cfg.emojiInterval === 0) {
        text = text + " " + getRandomEmoji();
    }
    return text;
}

/**
 * 获取不重复的随机表情包
 * TODO: 替换成你的表情包列表或图片路径
 */
var emojiList = [
    "[舔屏]", "[害羞]", "[流泪]", "[调皮]", "[酷拽]", "[鼓掌]", "[赞]", "[呲牙]",
    "[捂脸]", "[看]", "[比心]", "[尬笑]", "[发呆]", "[感谢]", "[懵]", "[点火]",
    "[哭哭]", "[机智]", "[爱心手]"
];
function getRandomEmoji() {
    if (usedEmojis.length >= emojiList.length) {
        usedEmojis = []; // 全部用完后重置
    }
    var available = [];
    for (var i = 0; i < emojiList.length; i++) {
        if (usedEmojis.indexOf(i) === -1) available.push(i);
    }
    var pick = available[Math.floor(Math.random() * available.length)];
    usedEmojis.push(pick);
    return emojiList[pick];
}

/** 获取随机私信内容 */
function getRandomDM() {
    return cfg.dmContent[Math.floor(Math.random() * cfg.dmContent.length)];
}

// ========== 核心操作 ==========

/**
 * 执行关注操作
 * 找到关注按钮就点击，找不到说明已关注，直接跳过
 * @returns {boolean}
 */
function doFollow() {
    var uiObject = selector().id("com.ss.android.ugc.aweme:id/jxp").className("android.widget.ImageView").visibleToUser(true).findOne(500);
    if (uiObject) {
        click(uiObject.bounds().centerX(), uiObject.bounds().centerY());
        sleep(500)
        return true;
    }
    logMsg(" 未找到关注按钮，已关注跳过");
    return false;
}

/**
 * 执行点赞操作
 * TODO: 填充节点点击逻辑
 * @returns {boolean}
 */
function doLike() {
    // TODO: 找到点赞按钮并点击    找到关注-评论   的中间,中间就是点赞按钮
    var uiObject = selector().id("com.ss.android.ugc.aweme:id/gk5").visibleToUser(true).findOne(500);
    if (uiObject) {
        click(uiObject.bounds().centerX(), uiObject.bounds().centerY());
        sleep(500)
        log("执行点赞 *****")
        return true
    }
    return false;
}




/**
 * 执行评论操作
 * @param {string} text 评论内容
 * TODO: 填充节点点击逻辑
 * @returns {boolean}
 */
function doComment(wenb) {
    var pp = 0
    for (var i = 0; i < 60; i++) {
        sleep(500)
        if (pp == 0) {
            var 评论按钮 = selector().id("com.ss.android.ugc.aweme:id/eqe").className("android.widget.ImageView").visibleToUser(true).findOne(500);
            if (评论按钮) {
                click(评论按钮.bounds().centerX(), 评论按钮.bounds().centerY());
            }
            pp = 1
        }

        if (pp == 1) {
            try {
                var uiObject = selector().desc("插入图片").visibleToUser(true).findOne(500);
                if (uiObject) {
                    click(uiObject.bounds().centerX() - 100, uiObject.bounds().centerY());
                    sleep(1000)
                    pp = 2
                }
            } catch (error) {
                /* 处理异常的代码块 */
                console.error(error);
            }
        }
        if (pp == 2) {
            var uiObject = text("发送").visibleToUser(true).findOne(2000);
            if (uiObject) {
                log("发送")
                var 输入框 = selector().className("android.widget.EditText").visibleToUser(true).findOne(2000);
                if (输入框) {
                    输入框.setText(wenb)
                }
                click(uiObject.bounds().centerX(), uiObject.bounds().centerY());
                sleep(1000)
                var uiObject = selector().id("com.ss.android.ugc.aweme:id/back_btn").desc("关闭").className("android.widget.ImageView").visibleToUser(true).findOne(1000);
                if (uiObject) {
                    click(uiObject.bounds().centerX(), uiObject.bounds().centerY())
                }
                break
            }
        }

        var uiObject = text("作者已开启防打扰保护功能").visibleToUser(true).findOne(2000);
        if (uiObject) {
            break
        }
    }

    for (var i = 0; i < 10; i++) {
        sleep(500)
        if (shou_ye_page()) {
            break
        } else {
            var uiObject = selector().id("com.ss.android.ugc.aweme:id/back_btn").desc("关闭").className("android.widget.ImageView").visibleToUser(true).findOne(1000);
            if (uiObject) {
                click(uiObject.bounds().centerX(), uiObject.bounds().centerY())
            }
        }
    }
}
function shou_ye_page() {
    var src = images.captureScreen();
    sleep(300);
    var img = null;
    try {
        img = images.clip(src, 0, device.height - 300, device.width, 300);
        var results = ocr.detect(img);
        for (var i = 0; i < results.length; i++) {
            var t = results[i].text;
            if (t.indexOf("首页") > -1) return true;
        }
    } catch (e) {
        log("OCR检测失败: " + e);
    } finally {
        if (img != null) img.recycle();
        if (src != null) src.recycle();
    }
    return false;
}


/**
 * 检测是否有回关（进入个人主页后判断）
 * TODO: 填充节点判断逻辑
 * @returns {boolean}
 */
function hasNewMessage() {
    // id/14f 是消息角标容器，id/0_4 是数字
    try {
        var xx = desc("消息，按钮").findOne(500)
        if (xx) {
            xx = xx.parent().parent().parent().child(1).child(0).text()
            个数 = parseInt(xx) || 0;
            log("有新消息: " + 个数 + " 条");
            if (个数 > 0) {
                return true
            }
        }
    } catch (error) {
        /* 处理异常的代码块 */
        console.error(error);
    }
    return false
}
function 是否回复过() {
    var arr = selector().id("com.ss.android.ugc.aweme:id/dlr").find(200);
    if (arr) {
        for (var i = 0; i < arr.length; i++) {
            var element = arr[i];
            if (element.bounds()["right"] > device.width / 2) {
                return true
            }
        }
    }
    return false
}

/**
 * 发送私信
 * @param {string} text 私信内容
 * TODO: 填充节点点击逻辑
 */
function sendDM(text) {
    var 是否返回 = false
    var 消息按钮 = id("com.ss.android.ugc.aweme:id/0r8").desc("消息，按钮").visibleToUser(true).findOne(500);
    if (消息按钮) {
        click(消息按钮.bounds().centerX(), 消息按钮.bounds().centerY());
        sleep(100);
    }
    for (var i = 0; i < 300; i++) {
        sleep(500)
        var uiObject = selector().text("新关注我的").findOne(200);
        if (uiObject) {
            log("互动记录")
            back()
            sleep(1000)
            是否返回 = false
        }


        if (是否返回) {
            log("私信中...返回")
            var 聊天界面 = selector().id("com.ss.android.ugc.aweme:id/pjq").findOne(200);
            if (聊天界面) {
                back()
                sleep(1000)
            } else {
                是否返回 = false
            }
        } else {
            log("私信中...")
            var 聊天界面 = selector().id("com.ss.android.ugc.aweme:id/pjq").findOne(200);
            if (聊天界面) {
                if (是否回复过()) {
                    log("回复过了")
                    是否返回 = true
                    continue
                } else {
                    click(device.width / 2, 聊天界面.bounds().centerY())
                    sleep(1000)
                    var 输入框 = selector().className("android.widget.EditText").visibleToUser(true).findOne(200);
                    if (输入框) {
                        输入框.setText(text)
                        sleep(1000)
                    }
                    var 发送 = selector().descContains("发送").visibleToUser(true).findOne(200);
                    if (发送) {
                        click(发送.bounds().centerX(), 发送.bounds().centerY())
                        是否返回 = true
                        continue
                    }
                }
            } else {
                var 消息按钮 = id("com.ss.android.ugc.aweme:id/0r8").desc("消息，按钮").visibleToUser(true).findOne(500);
                if (消息按钮) {
                    click(消息按钮.bounds().centerX(), 消息按钮.bounds().centerY());
                    sleep(100);
                    click(消息按钮.bounds().centerX(), 消息按钮.bounds().centerY());
                    sleep(1000)
                    var a = id("com.ss.android.ugc.aweme:id/w64").findOne(500);
                    if (a) {
                        log("找到红点")
                        click(a.bounds().centerX(), a.bounds().centerY());
                    } else {
                        break
                    }
                }
            }
        }
    }
}

function 返回首页() {
    var pp = 0
    for (var i = 0; i < 60; i++) {
        sleep(300)
        var uiObject = text("返回").visibleToUser(true).findOne(500);
        if (uiObject) {
            click(uiObject.bounds().centerX(), uiObject.bounds().centerY());
            break
        }
        var 评论按钮 = id("com.ss.android.ugc.aweme:id/eqe").findOne(500);
        if (评论按钮) {
            log("推荐作品界面")
            break
        }

        if (pp == 0) {
            var uiObject = desc("首页，按钮").visibleToUser(true).findOne(500);
            if (uiObject) {
                click(uiObject.bounds().centerX(), uiObject.bounds().centerY());
                sleep(1000)
                pp = 1
            }
        }

        var uiObject = selector().text("新关注我的").findOne(1000);
        if (uiObject) {
            back()
        }
    }
}

/**
 * OCR 检测当前页面是否包含直播/广告/应用等需要跳过的内容
 */
function shouldSkip() {
    var src = images.captureScreen();
    sleep(300);
    var img = null;
    try {
        img = images.clip(src, 0, device.height / 2, device.width, device.height / 2);
        var results = ocr.detect(img);
        for (var i = 0; i < results.length; i++) {
            var t = results[i].text;
            if (t.indexOf("直播") > -1) return true;
            if (t.indexOf("广告") > -1) return true;
            if (t.indexOf("应用") > -1) return true;
            if (t.indexOf("咨询") > -1) return true;
        }
    } catch (e) {
        log("OCR检测失败: " + e);
    } finally {
        if (img != null) img.recycle();
        if (src != null) src.recycle();
    }
    return false;
}

/**
 * 滑动到下一个视频
 * TODO: 根据实际坐标调整
 */
function swipeToNext() {
    // TODO: 向上滑动切换视频
    swipe(device.width / 2, device.height * 0.7, device.width / 2, device.height * 0.3, 500);
}


var USED_ACCOUNTS_FILE = "/sdcard/dy_used_accounts.txt";

// 读取已用账号列表
function getUsedAccounts() {
    if (!files.exists(USED_ACCOUNTS_FILE)) return [];
    var content = files.read(USED_ACCOUNTS_FILE) || "";
    return content.split("\n").filter(function (s) { return s.trim() !== ""; });
}

// 标记账号为已用
function markAccountUsed(name) {
    if (getUsedAccounts().indexOf(name) === -1) {
        files.append(USED_ACCOUNTS_FILE, name + "\n");
    }
}

/**
 * 换号操作 - 优先选择未使用过的账号
 */
function switchAccount() {
    logMsg(" 已达到关注上限，执行换号...");
    // 导航到"我"页面
    var pp = 0
    for (var i = 0; i < 60; i++) {
        sleep(1000)
        if (pp == 2) {
            if (id("com.ss.android.ugc.aweme:id/tv_nickname").exists()) {
                if (id("com.ss.android.ugc.aweme:id/title").exists()) {
                    logMsg(" 账号界面");
                    break;
                }
            }
        }
        if (pp == 1) {
            var uiObject = text("切换账号").visibleToUser(true).findOne(500);
            if (uiObject) {
                click(uiObject.bounds().centerX(), uiObject.bounds().centerY());
                sleep(1000);
                pp = 2
            }
            var uiObject = desc("更多").visibleToUser(true).findOne(500);
            if (uiObject) {
                click(uiObject.bounds().centerX(), uiObject.bounds().centerY());
                sleep(1000);
            }
        }
        if (pp == 0) {
            var uiObject = text("我").visibleToUser(true).findOne(500);
            if (uiObject) {
                click(uiObject.bounds().centerX(), uiObject.bounds().centerY());
                sleep(1000);
                pp = 1
            }
        }
    }

    // 获取账号列表，找一个没用过的
    var 账号列表 = id("com.ss.android.ugc.aweme:id/tv_nickname").find();
    var usedAccounts = getUsedAccounts();
    var targetAccount = null;

    for (var i = 0; i < 账号列表.length; i++) {
        var name = 账号列表[i].text();
        log("账号: " + name + " 已用: " + (usedAccounts.indexOf(name) !== -1));
        if (usedAccounts.indexOf(name) === -1) {
            targetAccount = 账号列表[i];
            break;
        }
    }

    if (targetAccount) {
        logMsg(" 切换到新账号: " + targetAccount.text());
        markAccountUsed(targetAccount.text());
        click(targetAccount.bounds().centerX(), targetAccount.bounds().centerY());
        sleep(2000);
    } else {
        logMsg(" 所有账号已用完，清空记录重新开始");
        files.write(USED_ACCOUNTS_FILE, "");
        // 取第一个账号
        if (账号列表.length > 0) {
            markAccountUsed(账号列表[0].text());
            click(账号列表[0].bounds().centerX(), 账号列表[0].bounds().centerY());
            sleep(2000);
        }
    }
    for (var i = 0; i < 60; i++) {
        var uiObject = desc("首页，按钮").visibleToUser(true).findOne(500);
        if (uiObject) {
            break
        } else {
            back()
        }
        sleep(1000)
    }

    // 重置计数
    followCount = 0;
    commentCount = 0;
    usedEmojis = [];
    commentPingPongDir = 1;
    commentPingPongIdx = 0;
    storage.put("follow_count", "0");
    targetFollowCount = cfg.followMin + Math.floor(Math.random() * (cfg.followMax - cfg.followMin + 1));
    logMsg(" 换号完成，新目标关注数: " + targetFollowCount);
}

// ========== 主循环 ==========
function main() {
    logMsg(" 脚本启动，目标关注数: " + targetFollowCount);
    switchAccount()
    返回首页()
    logMsg(" 进入首页，准备执行...");
    while (isRunning) {
        if (checkStop()) break;

        // 0. OCR 检测直播/广告/应用，是则跳过
        if (shouldSkip()) {
            logMsg(" 检测到直播/广告，跳过");
            swipeToNext();
            sleep(800);
            continue;
        }

        // 1. 停留视频
        var stayTime = randInt(cfg.stayMin, cfg.stayMax);
        var staySec = Math.round(stayTime / 1000);
        logMsg(" 停留视频 " + staySec + " 秒");
        for (var s = staySec; s > 0; s--) {
            if (checkStop()) break;
            storage.put("log_msg", " 倒计时 " + s + " 秒...");
            storage.put("log_countdown", " 倒计时 " + s + " 秒...");
            sleep(1000);
        }

        if (checkStop()) break;

        // 2. 随机打乱关注/点赞/评论的执行顺序
        var ops = [];
        if (cfg.doFollow) ops.push("follow");
        if (cfg.doLike)   ops.push("like");
        if (cfg.doComment) ops.push("comment");
        // Fisher-Yates shuffle
        for (var i = ops.length - 1; i > 0; i--) {
            var j = Math.floor(Math.random() * (i + 1));
            var tmp = ops[i]; ops[i] = ops[j]; ops[j] = tmp;
        }
        logMsg(" 本次操作顺序: " + ops.join(" → "));

        for (var i = 0; i < ops.length; i++) {
            if (checkStop()) break;
            var op = ops[i];
            if (op === "follow") {
                logMsg(" 执行关注 (" + (followCount + 1) + "/" + targetFollowCount + ")");
                var ok = doFollow();
                if (ok) {
                    followCount++;
                    logMsg(" 关注成功，累计: " + followCount);
                    storage.put("follow_count", followCount.toString());
                    if (followCount >= cfg.switchCount) {
                        switchAccount();
                        返回首页();
                    }
                    if (followCount >= targetFollowCount) {
                        logMsg(" 本轮目标完成，累计: " + followCount);
                    }
                }
            } else if (op === "like") {
                logMsg(" 执行点赞");
                doLike();
            } else if (op === "comment") {
                var commentText = getNextComment();
                logMsg(" 执行评论: " + commentText);
                doComment(commentText);
            }
            randSleep(cfg.intervalMin, cfg.intervalMax);
        }

        if (hasNewMessage()) {
            var dmText = getRandomDM();
            logMsg(" 检测到回关，发送私信: " + dmText);
            sendDM(dmText);
            返回首页()
        }


        if (checkStop()) break;

        // 3. 滑动到下一个视频
        logMsg(" 切换下一个视频");
        swipeToNext();
        randSleep(800, 1500);
    }

    logMsg(" 脚本已停止，本次共关注: " + followCount + " 人");
    storage.put("star_running", "0");
}




main();

