# 调试会话: tractor-game-bugs

**状态**: [OPEN]
**开始时间**: 2026-06-21
**问题描述**: 四人升级项目中存在未明确的BUG，需要系统排查

---

## 假设列表

| 编号 | 假设 | 可证伪 | 状态 |
|-----|------|--------|------|
| H1 | Socket.IO事件处理器参数顺序错误，导致事件无法正确响应 | ✅ | 待验证 |
| H2 | 前端Socket.IO v2与后端netty-socketio 1.7.x兼容性问题 | ✅ | 待验证 |
| H3 | 游戏状态机流转逻辑缺陷（如亮主轮询、出牌比较） | ✅ | 待验证 |
| H4 | 玩家退出时的状态重置不完整，导致后续对局异常 | ✅ | 待验证 |
| H5 | 前端手牌排序、主牌判断逻辑与后端不一致 | ✅ | 待验证 |

---

## 插桩日志

### 后端插桩点
- GameRoom: addPlayer, removePlayer, declareTrump, playCards, finishRound
- GameEventHandler: 所有OnEvent方法入口和出口

### 前端插桩点
- Socket.IO事件监听: connect, gameState, error
- 关键操作: joinRoom, declareTrump, playCards

---

## 证据分析

| 证据 | 说明 | 相关假设 |
|------|------|----------|
| (待收集) | | |

---

## 修复记录

| 修复点 | 变更描述 | 状态 |
|--------|----------|------|
| (待修复) | | |
