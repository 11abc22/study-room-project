# 自习室预约系统换位请求功能后端设计方案

> 基于 `docs/backend-design-agent-handoff-final.md` 与当前后端代码库整理  
> 日期：2026-04-06

---

## 1. 设计目标

本方案用于指导“换位请求（Seat Swap Request）”功能的后端实现，目标是：

1. **贴合现有代码结构**，不脱离当前 Spring Boot + JPA 单体架构另起一套方案
2. **尽量不破坏现有预约功能**，尤其是座位占用判断、冲突校验、我的预约、管理端预约管理
3. **优先兼容现有前端预留接口与字段依赖**
4. **优先小步修改**，把高风险点集中在可控范围内

---

## 2. 现状分析

### 2.1 当前预约模块结构

当前与预约相关的真实代码结构：

- 实体
  - `entity/Reservation.java`
  - `entity/Seat.java`
  - `entity/StudyRoom.java`
  - `entity/User.java`
- Repository
  - `repository/ReservationRepository.java`
  - `repository/SeatRepository.java`
  - `repository/StudyRoomRepository.java`
  - `repository/UserRepository.java`
- Service
  - `service/ReservationService.java`
- Controller
  - `controller/ReservationController.java`
  - `controller/AdminController.java`
- DTO / VO
  - `dto/ReservationRequest.java`
  - `vo/ReservationVO.java`

当前链路比较直接：

- Controller 接请求
- Service 做业务校验与写库
- Repository 提供冲突与列表查询

没有额外的领域层、事件层、消息层。

### 2.2 当前预约状态模型

`Reservation.status` 当前是 `Integer`：

- `1 = RESERVED`（有效预约）
- `2 = CANCELLED`（已取消）

并且当前很多查询都直接依赖 `status = 1` 表示“实际占座”：

- `findSeatConflicts(...)`
- `findUserConflicts(...)`
- `findRoomReservations(...)`
- `getRoomTimeline(...)`
- `getSeatTimeline(...)`
- `createReservation(...)`
- `cancelReservation(...)`

**结论：不能直接把 REQUESTING / PENDING 这类流程态强行塞进 `reservation.status`。**

### 2.3 当前认证方式

虽然项目中存在：

- `JwtService`
- `JwtAuthenticationFilter`
- `UserService implements UserDetailsService`

但当前业务接口的真实认证方式仍然是：

- Controller 手动读取 `X-User-Id`
- `SecurityConfig` 中 `anyRequest().permitAll()`
- `JwtAuthenticationFilter` 未接入过滤链
- `AuthController#login()` 不返回 token

**结论：本期换位接口应继续沿用 `X-User-Id`，不要顺手重构成 JWT 正式鉴权。**

### 2.4 当前返回风格

当前接口返回风格不统一：

- 有的直接返回 `List<ReservationVO>`
- 有的返回 `Map<String, Object>`
- 错误统一由 `GlobalExceptionHandler` 转成：

```json
{
  "message": "...",
  "status": 400,
  "timestamp": "..."
}
```

**结论：新增 swap request 接口可单独采用 `code/message/data` 风格，但不建议顺带全站统一响应结构。**

### 2.5 当前邮件能力

代码库中当前没有：

- `MailService`
- `NotificationService`
- `JavaMailSender`
- Resend 集成
- 邮件模板机制

`pom.xml` 里也没有邮件依赖。

**结论：当前无邮件模块。通知能力需要最小侵入新增。**

### 2.6 当前定时任务能力

当前代码库中没有：

- `@Scheduled`
- `@EnableScheduling`
- job / scheduler 模块

**结论：超时自动关闭需要新增调度能力。**

### 2.7 当前数据库设计与换位需求的冲突点

1. `reservation` 表只能表达“谁预约了哪个座位”，不能表达完整换位流程
2. 前端需要 `REQUESTING / PENDING` 语义态，但现有 `reservation.status` 是资源状态，不适合扩成流程状态
3. 当前并发保护较弱，换位流程比普通预约更容易出现竞态
4. 当前没有通知和超时任务基础设施

---

## 3. 总体方案

### 3.1 核心原则

1. **保留 `reservation.status` 继续表示资源状态**
   - `1 = RESERVED`
   - `2 = CANCELLED`
2. **新增独立 `swap_request` 表承载换位流程状态**
3. **通过 `/api/reservations/my` 的聚合输出映射前端展示态**
4. **普通预约逻辑尽量不动，只在必要处加保护**

### 3.2 为什么不直接改 Reservation.status

如果把 `REQUESTING / PENDING` 直接塞进 `reservation.status`，会直接影响：

- 冲突检测
- 座位时间线
- 房间时间线
- 管理端按 status 过滤
- 我的预约现有逻辑

所以不推荐。

---

## 4. 数据模型设计

### 4.1 reservation 表

本期建议**不强制修改 `reservation` 表结构**。

原因：

- 当前表已经足够承载最终预约结果
- 同意换位时，只需要：
  - 把请求方原预约改到目标座位
  - 把持有方原预约改为取消
- 不需要给 `reservation` 增加流程字段

### 4.2 新增 swap_request 表

建议新增表：`swap_request`

#### 字段设计

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK auto_increment | 主键 |
| requester_user_id | bigint not null | 请求方用户 A |
| requester_reservation_id | bigint not null | 请求方自己当前预约 |
| target_reservation_id | bigint not null | 目标预约（持有方 B 的预约） |
| target_user_id | bigint not null | 持有方用户 B |
| reserve_date | date not null | 冗余预约日期，便于筛选 |
| start_time | time not null | 冗余开始时间 |
| end_time | time not null | 冗余结束时间 |
| status | int not null | 换位请求状态 |
| message | varchar(500) null | 请求留言 |
| expire_at | datetime not null | 到期时间 |
| processed_at | datetime null | 最终处理时间 |
| processed_by | bigint null | 谁完成处理 |
| process_reason | varchar(64) null | APPROVED / REJECTED / WITHDRAWN / EXPIRED / STARTED |
| created_at | datetime not null | 创建时间 |
| updated_at | datetime not null | 更新时间 |

### 4.3 swap_request 状态设计

建议 `swap_request.status` 仍用整数：

- `1 = PENDING`
- `2 = APPROVED`
- `3 = REJECTED`
- `4 = WITHDRAWN`
- `5 = EXPIRED`

说明：

- `REQUESTING` / `PENDING` 是前端对 reservation 展示的语义态
- 对 swap_request 实体内部，只需要一个进行中态 `PENDING`
- 请求方看到自己预约时映射为 `REQUESTING`
- 持有方看到自己预约时映射为 `PENDING`

### 4.4 索引建议

建议至少增加：

- `idx_swap_target_status (target_reservation_id, status)`
- `idx_swap_requester_status (requester_user_id, status)`
- `idx_swap_target_user_status (target_user_id, status)`
- `idx_swap_status_expire (status, expire_at)`
- `idx_swap_requester_reservation (requester_reservation_id)`
- `idx_swap_target_reservation (target_reservation_id)`

### 4.5 业务约束

业务层必须保证：

1. 不能申请自己的预约
2. `requester_reservation_id != target_reservation_id`
3. 两条预约必须是同日期、同时间段
4. 两条预约都必须仍是有效预约（`reservation.status = 1`）
5. 同一个目标预约同一时间只允许一个进行中的换位请求
6. 同一请求方在同一时间段只允许一个进行中的换位请求
7. 对同一目标预约已被拒绝过的请求方不能重复申请

---

## 5. 状态输出兼容设计

### 5.1 Reservation 底层状态保持不变

保留：

- `1 -> RESERVED`
- `2 -> CANCELLED`

### 5.2 `/api/reservations/my` 的展示态映射

按当前用户视角做聚合：

- 当前用户是 `requester_user_id`，且存在进行中的 `swap_request`
  - 当前 reservation 展示 `REQUESTING`
- 当前用户是 `target_user_id`，且存在进行中的 `swap_request`
  - 当前 reservation 展示 `PENDING`
- 其他情况
  - `status=1 -> RESERVED`
  - `status=2 -> CANCELLED`

### 5.3 ReservationVO 修改建议

当前 `ReservationVO`：

```java
private Long id;
private Long userId;
private String username;
private Long roomId;
private String roomName;
private Long seatId;
private String seatCode;
private LocalDate reserveDate;
private LocalTime startTime;
private LocalTime endTime;
private Integer status;
```

建议新增字段：

```java
private String displayStatus;
private Long swapRequestId;
private String recordType;
private Boolean virtual;
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
```

其中：

- `status` 保留原 int，兼容旧逻辑
- `displayStatus` 提供给新前端看字符串状态
- `swapRequestId` 方便前端联动处理
- `recordType` / `virtual` 可选，默认可分别为 `RESERVATION` / `false`

---

## 6. API 设计

以下路径尽量贴合前端已预留接口。

### 6.1 创建换位请求

**POST** `/api/swap-requests`

请求：

```json
{
  "targetReservationId": 123,
  "message": "你好，希望能换一下座位"
}
```

成功响应：

```json
{
  "code": 200,
  "message": "换位请求已发送",
  "data": {
    "swapRequestId": 456,
    "status": "REQUESTING"
  }
}
```

#### 说明

为了尽量少改前端，不额外要求前端传 `requesterReservationId`。后端在创建时根据目标预约的日期与时间段，自动查找当前用户自己的对应有效预约。

---

### 6.2 撤回换位请求

**PUT** `/api/swap-requests/{id}/withdraw`

成功响应：

```json
{
  "code": 200,
  "message": "换位请求已撤回",
  "data": null
}
```

---

### 6.3 同意换位请求

**PUT** `/api/swap-requests/{id}/approve`

成功响应：

```json
{
  "code": 200,
  "message": "换位请求已同意",
  "data": {
    "swapRequestId": 456,
    "status": "RESERVED"
  }
}
```

---

### 6.4 拒绝换位请求

**PUT** `/api/swap-requests/{id}/reject`

成功响应：

```json
{
  "code": 200,
  "message": "换位请求已拒绝",
  "data": null
}
```

---

### 6.5 检查是否允许申请

**GET** `/api/swap-requests/check?reservationId=123`

响应：

```json
{
  "code": 200,
  "data": {
    "canRequest": false,
    "reason": "ALREADY_REJECTED"
  }
}
```

#### reason 枚举建议

- `CAN_REQUEST`
- `OWN_SEAT`
- `ALREADY_REJECTED`
- `HAS_ACTIVE_REQUEST`
- `SEAT_LOCKED`
- `TARGET_NOT_AVAILABLE`
- `RESERVATION_NOT_FOUND`

---

### 6.6 查询某条预约对应的待处理换位请求

**GET** `/api/swap-requests/my-pending?reservationId=123`

成功响应：

```json
{
  "code": 200,
  "data": {
    "swapRequestId": 456,
    "requesterId": 789,
    "requesterName": "用户A",
    "message": "你好，希望能换一下座位",
    "createdAt": "2026-04-06T10:30:00"
  }
}
```

无待处理请求：

```json
{
  "code": 200,
  "data": null
}
```

---

## 7. DTO / VO 设计

### 7.1 新增 DTO

#### `dto/CreateSwapRequestDTO.java`

```java
private Long targetReservationId;
private String message;
```

### 7.2 新增 VO

#### `vo/SwapRequestCheckVO.java`

```java
private Boolean canRequest;
private String reason;
```

#### `vo/SwapRequestCreateVO.java`

```java
private Long swapRequestId;
private String status;
```

#### `vo/SwapRequestPendingVO.java`

```java
private Long swapRequestId;
private Long requesterId;
private String requesterName;
private String message;
private LocalDateTime createdAt;
```

### 7.3 可选统一响应包装

可单独给 swap request 新接口增加一个简单泛型包装：

#### `vo/ApiResponseVO<T>.java`

```java
private Integer code;
private String message;
private T data;
```

注意：仅建议用于 swap request 新接口，不强推全站统一。

---

## 8. 模块设计

### 8.1 新增实体

- `entity/SwapRequest.java`

### 8.2 新增 Repository

- `repository/SwapRequestRepository.java`

建议提供的方法：

1. `findByTargetReservationIdAndStatus(...)`
2. `existsByRequesterUserIdAndTargetReservationIdAndStatus(...)`
3. `findByStatusAndExpireAtBefore(...)`
4. `findLockedById(...)`
5. `findLockedByTargetReservationId(...)`

### 8.3 ReservationRepository 小改

建议新增：

#### 加锁读取 reservation

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select r from Reservation r where r.id = ?1")
Optional<Reservation> findByIdForUpdate(Long id);
```

#### 查询用户在指定精确时段的有效预约

```java
@Query("""
    select r from Reservation r
    where r.userId = ?1
      and r.reserveDate = ?2
      and r.status = 1
      and r.startTime = ?3
      and r.endTime = ?4
""")
List<Reservation> findActiveByUserAndExactTime(Long userId, LocalDate reserveDate, LocalTime startTime, LocalTime endTime);
```

### 8.4 新增 Service

- `service/SwapRequestService.java`

建议核心方法：

- `createSwapRequest(Long currentUserId, CreateSwapRequestDTO req)`
- `checkCanRequest(Long currentUserId, Long targetReservationId)`
- `withdraw(Long currentUserId, Long swapRequestId)`
- `approve(Long currentUserId, Long swapRequestId)`
- `reject(Long currentUserId, Long swapRequestId)`
- `getMyPending(Long currentUserId, Long reservationId)`
- `expirePendingRequests()`

### 8.5 ReservationService 小改

重点修改：`getMyReservations(Long userId)`

处理方式：

1. 查询当前用户所有 reservation
2. 查询该用户相关的进行中 swap request
3. 构建：
   - `requesterReservationId -> swapRequest`
   - `targetReservationId -> swapRequest`
4. 组装 `ReservationVO` 时填充：
   - `displayStatus`
   - `swapRequestId`
   - `createdAt`
   - `updatedAt`

### 8.6 新增 Controller

- `controller/SwapRequestController.java`

路径：

- `POST /api/swap-requests`
- `PUT /api/swap-requests/{id}/withdraw`
- `PUT /api/swap-requests/{id}/approve`
- `PUT /api/swap-requests/{id}/reject`
- `GET /api/swap-requests/check`
- `GET /api/swap-requests/my-pending`

认证方式继续沿用 `X-User-Id`。

---

## 9. 关键业务流程

### 9.1 用户 A 发起换位

#### 输入

- 当前用户 A
- `targetReservationId`
- `message`

#### 关键校验

1. 目标预约存在
2. 目标预约状态必须为有效预约（`reservation.status = 1`）
3. 当前用户不能申请自己的预约
4. 当前用户必须在同日期、同时间段存在自己的有效预约
5. 当前用户在同一时间段不能已有进行中的换位请求
6. 同一目标预约不能已有进行中的换位请求
7. 当前用户若曾对同一目标预约被拒绝过，不可重复申请

#### 表变化

- 新增一条 `swap_request(status=PENDING)`
- `reservation` 表不改

#### 展示结果

- A 的对应预约显示 `REQUESTING`
- B 的目标预约显示 `PENDING`

#### 通知

- 若已接入通知模块，则给 B 发通知邮件

---

### 9.2 用户 B 拒绝

#### 校验

1. swap_request 存在
2. 当前状态是 `PENDING`
3. 当前用户必须是目标预约持有方

#### 表变化

- `swap_request.status = REJECTED`
- 补 `processed_at / processed_by / process_reason`
- `reservation` 不改

#### 结果

- A/B 双方预约恢复展示为正常 `RESERVED`

#### 通知

- 给 A 发“被拒绝”通知

---

### 9.3 用户 B 同意

#### 校验

1. swap_request 当前必须为 `PENDING`
2. 当前用户必须是持有方
3. requesterReservation 仍存在且有效
4. targetReservation 仍存在且有效
5. 两条 reservation 时间段仍一致

#### 同一事务内操作

1. 锁住 `swap_request`
2. 锁住 `requesterReservation`
3. 锁住 `targetReservation`
4. 将 `requesterReservation.roomId / seatId` 改为目标预约的房间与座位
5. 将 `targetReservation.status = 2`
6. 将 `swap_request.status = APPROVED`
7. 记录处理信息

#### 结果

- A 获得目标座位，显示 `RESERVED`
- B 原预约变为 `CANCELLED`

#### 通知

- 给 A 发“换位成功”通知

---

### 9.4 用户 A 撤回

#### 校验

1. swap_request 存在
2. 当前状态是 `PENDING`
3. 当前用户必须是请求方

#### 表变化

- `swap_request.status = WITHDRAWN`
- 写入处理信息
- `reservation` 不改

#### 结果

- A/B 双方恢复正常 `RESERVED`

#### 通知

- 不发邮件，静默回退

---

### 9.5 超时自动关闭

#### 触发条件

取先到者：

1. 发起后 24 小时未处理
2. 到达预约开始时间仍未处理

#### 表变化

- `swap_request.status = EXPIRED`
- 写入 `processed_at`
- `process_reason = EXPIRED` 或 `STARTED`

#### 结果

- 展示效果等同于拒绝
- reservation 不改

#### 通知

- 建议通知 A 请求已超时

---

## 10. 并发与事务策略

### 10.1 必须加事务的方法

以下方法必须 `@Transactional`：

- `createSwapRequest`
- `withdraw`
- `approve`
- `reject`
- `expirePendingRequests`

### 10.2 必须加锁的点

推荐使用 JPA `PESSIMISTIC_WRITE`。

#### 发起换位时

至少锁：

- `targetReservation`
- 必要时锁 `requesterReservation`

避免同一目标被多人同时申请。

#### 同意 / 拒绝 / 撤回时

必须锁：

- `swap_request`
- 同意时还要锁两条 reservation

#### 超时任务处理时

- 先锁 `swap_request`
- 只有 `status=PENDING` 才允许处理

### 10.3 幂等控制

第一版可采用：

- `findByIdForUpdate()`
- 事务内检查 `status == PENDING`
- 成功后更新为终态

这样就能解决：

- 撤回与同意并发
- 定时超时与人工处理并发
- 同一请求被重复点击处理

### 10.4 防止同一目标被多人同时申请

建议流程：

1. 事务开始
2. 锁 `targetReservation`
3. 查询该目标预约是否已有 `status=PENDING` 的 swap_request
4. 若存在则返回 `SEAT_LOCKED`
5. 若不存在则插入新请求

---

## 11. 超时自动关闭设计

### 11.1 调度能力

需要启用：

```java
@EnableScheduling
```

### 11.2 新增任务类

建议新增：

- `service/SwapRequestExpireScheduler.java`
  或
- `job/SwapRequestExpireJob.java`

### 11.3 到期时间计算

创建 swap request 时直接写入：

```text
expireAt = min(createdAt + 24h, reservationStartDateTime)
```

这样定时任务只需扫描：

- `status = PENDING`
- `expire_at <= now`

### 11.4 扫描策略

建议每分钟或每 5 分钟扫描一次。
每条请求单独事务处理，避免整批失败回滚。

---

## 12. 通知设计

### 12.1 当前现状

当前仓库无邮件能力，因此建议分两步做。

### 12.2 第一步：加抽象，不强依赖真邮件

新增：

- `service/NotificationService.java`
- `service/NoopNotificationService.java`

定义方法：

- `notifySwapRequested(...)`
- `notifySwapRejected(...)`
- `notifySwapApproved(...)`
- `notifySwapExpired(...)`

默认实现仅打日志，不影响主功能上线。

### 12.3 第二步：真实邮件实现

后续若接入邮箱，可增加：

- `spring-boot-starter-mail`
- `service/EmailNotificationService.java`

配置建议追加到 `application.example.properties`：

```properties
spring.mail.host=
spring.mail.port=
spring.mail.username=
spring.mail.password=
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.notification.mail.enabled=false
app.notification.mail.from=
```

### 12.4 失败策略

邮件发送失败**不能影响主事务提交**。
建议在通知实现内部 catch 异常并记录日志。

---

## 13. 对现有接口的兼容性修改建议

### 13.1 `/api/reservations/my`

保持返回 `List<ReservationVO>`，但扩充字段：

- `status` 保留 int
- 新增 `displayStatus`
- 新增 `swapRequestId`
- 可选新增 `recordType` / `virtual` / `createdAt` / `updatedAt`

这是前端联调最关键的兼容点。

### 13.2 `DELETE /api/reservations/{id}`

建议增加保护：

- 若该 reservation 关联进行中的 swap_request，则禁止直接取消

原因：否则会把 swap_request 留成悬挂状态。

### 13.3 `PUT /api/reservations/{id}`

同样建议增加保护：

- 若该 reservation 关联进行中的 swap_request，则禁止修改

否则会破坏 swap_request 对应的时段与座位上下文。

### 13.4 AdminController

管理端建议保持现有结构，但补保护：

- `DELETE /api/admin/reservations/{id}`
- `PUT /api/admin/reservations/{id}/status`

若 reservation 存在进行中的 swap_request，则禁止直接取消或改状态。

同时，`/api/admin/reservations` 返回中可补充 `displayStatus`，便于管理员识别换位中状态。

---

## 14. 风险分析

### 14.1 最高风险：approve 同意换位

这是全方案最关键的方法，因为它涉及：

- 锁三条记录
- 修改两条 reservation
- 更新一条 swap_request
- 要求原子成功

必须重点测试。

### 14.2 高风险：误把流程态塞进 reservation.status

这会直接破坏现有座位占用逻辑，应避免。

### 14.3 中风险：管理端绕过流程直接改状态

必须在 AdminController 层增加保护，否则容易产生悬挂请求。

### 14.4 中风险：当前鉴权较弱

`X-User-Id` 可伪造，这是系统既有问题，但不建议本期扩大范围重构认证。

---

## 15. 实施顺序建议

### 第一步：数据模型与查询能力

1. 新增 `SwapRequest` 实体
2. 新增 `SwapRequestRepository`
3. ReservationRepository 增加锁查询与精确时段查询

### 第二步：用户侧主流程

1. 新增 `SwapRequestService`
2. 新增 `SwapRequestController`
3. 实现：
   - 创建
   - 检查可申请性
   - 撤回
   - 查询待处理
4. 修改 `/api/reservations/my` 输出展示态

### 第三步：闭环与保护

1. 实现同意 / 拒绝
2. 为 reservation update/cancel 增加保护
3. 为 admin reservation status/delete 增加保护

### 第四步：超时与通知

1. 启用 scheduling
2. 实现自动过期
3. 加通知抽象
4. 需要时再接真实邮件

---

## 16. 建议新增/修改的类清单

### 新增

- `entity/SwapRequest.java`
- `repository/SwapRequestRepository.java`
- `dto/CreateSwapRequestDTO.java`
- `vo/SwapRequestCheckVO.java`
- `vo/SwapRequestCreateVO.java`
- `vo/SwapRequestPendingVO.java`
- `service/SwapRequestService.java`
- `controller/SwapRequestController.java`
- `service/NotificationService.java`
- `service/NoopNotificationService.java`
- `service/SwapRequestExpireScheduler.java`（或 `job/...`）

### 修改

- `vo/ReservationVO.java`
- `service/ReservationService.java`
- `repository/ReservationRepository.java`
- `controller/AdminController.java`
- `controller/ReservationController.java`（如果只改 `/my` 返回结构则可不动签名）
- `StudyRoomBackendApplication.java`（若需加 `@EnableScheduling`）
- `application.example.properties`（后续需要邮件配置时）

---

## 17. 结论

### 17.1 可直接复用的部分

- 现有 `reservation` 主表
- 现有普通预约逻辑
- 现有 Spring Data JPA 风格
- 现有 `X-User-Id` 认证方式
- 现有 AdminController 基本结构

### 17.2 必须新增的部分

- `swap_request` 表与实体
- `SwapRequestRepository`
- `SwapRequestService`
- `SwapRequestController`
- 超时任务
- 通知抽象

### 17.3 只需小改的部分

- `ReservationVO` 扩字段
- `ReservationService#getMyReservations()` 聚合展示态
- `ReservationRepository` 补锁查询和精确查询
- Admin / Reservation 的取消、修改保护逻辑

### 17.4 风险最高的部分

1. `approve()` 同意换位事务
2. 并发状态竞争
3. 错误地改造 `reservation.status`

---

## 18. 推荐后续文档

如果需要继续细化实现，建议补：

- `docs/swap-request-api-contract.md`
- `docs/db-migration-swap-request.md`

前者适合前后端联调，后者适合建表与回滚说明。
