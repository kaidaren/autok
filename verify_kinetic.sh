#!/bin/bash
# Kinetic Design System 验证脚本
# 验证所有设计系统文件是否正确创建

echo "======================================"
echo "🎨 Kinetic Design System 文件验证"
echo "======================================"
echo ""

# 定义文件数组
REQUIRED_FILES=(
    "app/src/main/res/values/kinetic_colors.xml"
    "app/src/main/res/values/kinetic_styles.xml"
    "app/src/main/res/values/kinetic_dimens.xml"
    "app/src/main/res/values-night/kinetic_colors.xml"
    "app/src/main/res/drawable/kinetic_btn_primary.xml"
    "app/src/main/res/drawable/kinetic_btn_secondary.xml"
    "app/src/main/res/drawable/kinetic_input_background.xml"
    "app/src/main/res/drawable/kinetic_card_background.xml"
    "app/src/main/res/drawable/kinetic_btn_primary_ripple.xml"
    "KINETIC_DESIGN_GUIDE.md"
    "MIGRATION_SUMMARY.md"
)

UPDATED_FILES=(
    "app/src/main/res/values/colors.xml"
    "app/src/main/res/layout/activity_login.xml"
    "app/src/main/res/layout/activity_register.xml"
    "app/src/main/res/layout/activity_settings.xml"
)

echo "📋 检查新建文件 (9个):"
echo "========================"
failed=0
for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ $file - 缺失!"
        ((failed++))
    fi
done
echo ""

echo "📝 检查已更新文件 (4个):"
echo "========================"
for file in "${UPDATED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ $file - 缺失!"
        ((failed++))
    fi
done
echo ""

if [ $failed -eq 0 ]; then
    echo "🎉 所有文件验证通过!"
    echo ""
    echo "现在可以构建项目:"
    echo "  ./gradlew build"
    echo ""
    echo "或运行应用:"
    echo "  ./gradlew installDebug"
else
    echo "⚠️  发现 $failed 个问题"
    exit 1
fi
