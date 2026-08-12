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
| 夜视开关 | `/nv` 指令开关夜视效果，显示 0:00 |

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

## 指令

| 指令 | 说明 | 权限 |
|---|---|---|
| `/nv` | 开关夜视效果 | 所有玩家 |
| `/assist` | 查看帮助 | 所有玩家 |
| `/assist reload` | 重载配置文件 | OP |

## 配置文件

插件首次运行自动生成 `config.yml`，可开关每个功能：

```yaml
stonecutter: true        # 万能切石机
phantom-repel: true      # 火把驱幻翼
pet-health: true         # 宠物生命提升
glow-berries: true       # 发光浆果
cat-chest: true          # 猫开箱子
block-chest: true        # 方块开箱子
snowball-extinguish: true # 雪球灭火
night-vision: true       # 夜视开关
```

支持热更新：修改 `config.yml` 后自动生效，或执行 `/assist reload` 手动重载。

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
git tag v1.1.0
git push origin v1.1.0
```

## 信息

- 作者: 1zyq1
- 版本: 1.1.0
- 协议: MIT
