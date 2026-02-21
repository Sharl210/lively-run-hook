# 🚀 LivelyRunHook

> 解除Realme机型上的独有特性后台运行（Lively Run）限制

![platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)
![xposed](https://img.shields.io/badge/Xposed-LSPosed-blue)
![license](https://img.shields.io/badge/License-MIT-green)

## ✨ 功能说明

- ✅ 解除 Lively Run 的后台运行数量限制（`2` 限制，含相关判定路径）
- ✅ 解除静态后台数量限制（`12` 限制，含相关判定路径）
- ✅ 解除包名 / 场景不支持限制
- ✅ 解除最长运行时长限制（1 小时限制）
- ✅ 保留核心 Hook 逻辑，默认直接生效，无额外配置页

## 📱 开发者设备状态

- 机型：**真我 GT8 Pro**
- 设备代号：**RMX5200**
- 系统版本：**16.0.3.502 (CNO1)**

## 🧪 兼容性说明

- 理论支持：**所有支持 Lively Run 特性的 realme 机型的所有版本(Maybe)**
- 说明：该功能在系统侧通常不会频繁大改，且相关类名未做刻意混淆，因此跨版本具备一定稳定性

## 🛠️ 使用方式

1. 安装并启用 LSPosed
2. 安装本模块 APK
3. 在 LSPosed 中勾选作用域：`android`
4. 重启系统进程或设备后生效

## 📦 发布产物

- 推荐发布文件名：`LivelyRunHook_1.5_sign.apk`

## ⚠️ 免责声明

- 本项目仅供学习与研究用途，请勿用于违反设备使用协议或当地法律法规的场景

## 📄 开源协议

本项目基于 **MIT License** 开源。
