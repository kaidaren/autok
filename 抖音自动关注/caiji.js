auto();

// 递归打印所有节点信息
function dumpNodes(node, depth) {
    if (!node) return;
    depth = depth || 0;

    var indent = "";
    for (var i = 0; i < depth; i++) indent += "  ";

    var info = indent
        + "[" + node.className() + "]"
        + " id=" + node.id()
        + " text=" + node.text()
        + " desc=" + node.desc()
        + " bounds=" + node.bounds();

    log(info);

    for (var i = 0; i < node.childCount(); i++) {
        dumpNodes(node.child(i), depth + 1);
    }
}

// 获取根节点并打印
var root = className("FrameLayout").findOne(3000);
if (root) {
    log("===== 节点树开始 =====");
    dumpNodes(root, 0);
    log("===== 节点树结束 =====");
} else {
    log("未找到根节点");
}
