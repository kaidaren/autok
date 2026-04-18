"ui";

// Runtime smoke test for project doc globals.
// It reads baseline exported by init.js: global.__docGlobalApiBaseline__

function runDocGlobalSmokeTest() {
    var baseline = global.__docGlobalApiBaseline__ || [];
    if (!baseline.length) {
        log("未读取到文档全局API基线（__docGlobalApiBaseline__ 为空）");
        return;
    }
    var missing = [];
    var exists = 0;
    baseline.forEach(function (item) {
        var key = item.key;
        if (global[key] === undefined) {
            missing.push(key);
        } else {
            exists++;
        }
    });
    log("文档全局API总数: " + baseline.length);
    log("已注入数量: " + exists);
    log("缺失数量: " + missing.length);
    if (missing.length > 0) {
        log("缺失列表: " + JSON.stringify(missing));
    } else {
        log("Smoke测试通过: 文档标记global=true的命令全部可用");
    }
}

runDocGlobalSmokeTest();
