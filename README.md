# AssistPlugin

永乐服辅助插件 - Paper 服务端插件

## 功能列表

| 功能 | 说明 |
|---|---|
| 万能切石机 | 手持原木右键切石机，可切割成木板、台阶、楼梯、栅栏等，支持所有木种，中文 GUI |
| 火把驱赶幻翼 | 16 格范围内的火把会自动驱赶幻翼 |
| 宠物生命提升 | 已驯服的狗和猫生命值提升至 20 点 |
| 发光浆果效果 | 食用发光浆果后获得 10 秒发光效果 |
| 猫在箱子上可打开 | 猫坐在箱子上不再阻挡打开箱子 |
| 箱子上有方块可打开 | 箱子上方有实体方块时仍可正常打开 |
| 雪球灭火 | 雪球可熄灭蜡烛、篝火和灵魂篝火、火焰 |

## 切石机配方

1 原木 →:

| 产物 | 数量 |
|---|---|
| 木板 | 4 |
| 台阶 | 6 |
| 楼梯 | 2 |
| 栅栏 | 3 |
| 栅栏门 | 1 |
| 压力板 | 1 |
| 按钮 | 1 |

## 环境要求

- Java 21+
- Paper 1.21.4+ / Leaves 26.1.2+

## 安装

1. 下载 `AssistPlugin.jar`
2. 放入服务端 `plugins/` 目录
3. 重启服务端

## 构建

```bash
git clone https://github.com/1zyq1/AssistPlugin.git
cd AssistPlugin
./gradlew clean jar
```

输出: `build/libs/AssistPlugin.jar`

## 自动发布

推送 tag 即可触发 GitHub Actions 自动构建并发布到 Releases:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## 信息

- 作者: 1zyq1
- 版本: 1.0.0
- 协议: MIT
