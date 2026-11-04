# A股技术信号预警选股系统（Android）

全自动化的沪深 A 股技术公式交易信号预警选股 APP。原生 Android（Kotlin）实现，**数据来源为免费行情接口，自包含、无需服务器、零成本**。

## 功能

- **全局范围**：仅沪深 A 股 + 创业板（自动剔除科创板 688/689、北交所、新三板）
- **股价过滤**：2 < 价格 < 15 元
- **集合竞价过滤**：竞价/实时涨幅 < 6.8%
- **自动剔除**：ST、停牌、退市、一字板、多元金融
- **技术信号**（四类，命中即预警）：
  - MACD 金叉
  - 均线多头 / 均线金叉
  - KDJ 金叉
  - 放量突破
- **自动轮询**：集合竞价时段(9:15-9:25)5 秒高频刷新，盘中 30 秒，盘后 60 秒
- 暗黑主题界面、红涨绿跌、信号徽章高亮

## 数据来源（免费）

| 数据 | 接口 |
|------|------|
| 实时快照 / 集合竞价涨幅 | 新浪 `Market_Center.getHQNodeData`（sh_a + sz_a） |
| 个股日 K（技术指标） | 新浪 `CN_MarketDataService.getKLineData` |
| 行业过滤兜底 | 内置多元金融代码清单 `SectorProvider` |

> 说明：东财 push2 接口在本机网络不可达（手机网络通常可达），故主数据源采用新浪；多元金融过滤以内置清单可靠兜底。若你的网络可达东财，可在 `SinaApi` 中扩展行业字段增强。

## 环境要求

- Android Studio（Hedgehog 2023.1.1 或更新）
- JDK 11+（工程配置 target JVM 11）
- compileSdk 34 / minSdk 24

## 构建 APK

方式一：Android Studio 直接打开 `StockAlertApp` 目录，`Build → Build APK(s)`。

方式二：命令行
```bash
cd StockAlertApp
./gradlew :app:assembleDebug
```
产物：`app/build/outputs/apk/debug/app-debug.apk`，可直接安装到手机。

## 使用说明

1. 安装 APK，授权网络权限（Android 13+ 可授予通知权限用于后续提醒扩展）。
2. 打开 APP 即自动刷新一次。
3. 点「立即刷新」手动拉取；点「自动刷新」开启轮询，集合竞价时段自动高频更新。
4. 列表展示通过筛选且触发信号的个股，命中信号以黄色徽章标注。

## 技术架构

```
com.stockalert
├── data
│   ├── model        # StockQuote / KLine 数据模型
│   └── api          # SinaApi 行情接口 + SectorProvider 行业兜底
├── engine
│   ├── FilterEngine     # 全部硬性筛选规则
│   ├── SignalEngine     # 四类技术信号
│   └── indicator        # MACD / MA / KDJ 指标计算
├── ui
│   ├── MainViewModel    # 快照→过滤→K线→信号 编排
│   └── StockAdapter     # 列表适配器
└── MainActivity         # 界面 + 集合竞价轮询调度
```

## 免责声明

本项目仅提供技术指标计算与筛选工具，不构成任何投资建议。股市有风险，据此操作盈亏自负。
