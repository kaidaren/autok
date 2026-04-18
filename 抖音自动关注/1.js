
if (!requestScreenCapture(0)) {
    console.log("请求截图权限失败");
    exit();
}

var img = captureScreen();
var p = images.findMultiColors(img, "#FFC851", [[1, 11, "#FFB705"],[19, 11, "#F59F04"],[7, 0, "#FACB23"],[11, 2, "#E74A20"]], {threshold: 16});
if (p) {
    log("找到: " + p.x + ", " + p.y);
}



