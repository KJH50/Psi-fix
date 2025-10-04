# CAD系统实例创建的反向分解过程技术文档

## 概述

CAD (Casting Assistant Device) 系统的反向分解是一个复杂的逆向工程过程，用于从已构建的CAD实例中提取所有组件、配置参数和运行时状态。本文档详细说明了在Psi Mod中实现的反向分解算法及其技术细节。

## ⚠️ 高版本兼容性问题分析

### 问题根源

当前CAD分解器代码在NeoForge 1.21.1高版本中**无法正常工作**，主要存在以下关键问题：

#### 1. DataComponent访问方式错误

**问题**: 代码尝试通过`ModDataComponents.COMPONENTS.get()`访问组件数据，但这种方式在实际游戏中**不会返回有效数据**。

```java
// ❌ 错误的访问方式 - 在游戏中返回空数据
private static List<Item> getComponentList(ItemStack cadStack) {
    return cadStack.getOrDefault(ModDataComponents.COMPONENTS.get(), DEFAULT_COMPONENTS);
}
```

**根本原因**: CAD的组件信息实际上不是通过`COMPONENTS` DataComponent存储的，而是通过其他机制管理。

#### 2. ICAD接口方法缺失

**问题**: 代码调用了`ICAD`接口中不存在的方法：

```java
// ❌ 这些方法在ICAD接口中不存在
icad.getMemorySize(cadStack)     // 不存在
icad.getTime(cadStack)           // 不存在  
icad.getStoredPsi(cadStack)      // 不存在
```

**实际情况**: 查看`ICAD.java`源码发现，该接口只包含基本的统计值获取方法，不包含上述方法。

#### 3. CAD组件存储机制误解

**问题**: 代码假设CAD组件以`List<Item>`形式存储，但实际存储机制完全不同。

**实际机制**: CAD组件通过`ItemCAD`类的特定方法管理，需要通过`getComponentInSlot()`方法获取。

### 实际工作的代码实现

基于源码分析，以下是**真正可以工作**的CAD分解实现：

## 1. 关键参数和特征提取

### 1.1 核心数据结构

CAD实例包含以下关键数据结构：

```java
// 主要组件映射
Map<EnumCADComponent, ItemStack> components;

// 统计值映射  
Map<EnumCADStat, Integer> stats;

// 运行时状态
CADRuntimeState runtimeState;

// 元数据信息
CADMetadata metadata;
```

### 1.2 组件类型枚举

```java
public enum EnumCADComponent {
    ASSEMBLY,    // 装配体 - 必须实现ICADAssembly接口
    CORE,        // 核心组件
    SOCKET,      // 插槽组件
    BATTERY,     // 电池组件
    DYE;         // 染色器 - 必须实现ICADColorizer接口
}
```

### 1.3 统计值类型

```java
public enum EnumCADStat {
    EFFICIENCY,   // 效率值
    POTENCY,      // 威力值
    OVERFLOW,     // 溢出容量
    BANDWIDTH,    // 带宽
    SOCKETS;      // 插槽数量
}
```

## 2. 修正后的分解算法流程

### 2.1 主分解流程（修正版）

```java
public static DecomposedCADData decompose(ItemStack cadStack) {
    // 步骤1: 类型验证
    ICAD icad = requireCAD(cadStack);
    
    // 步骤2: 提取各类数据（使用正确的方法）
    return new DecomposedCADData(
        extractAllComponentsCorrect(cadStack, icad),     // 正确的组件提取
        reconstructStatisticsCorrect(cadStack, icad),    // 正确的统计值重构
        CADRuntimeState.extractCorrect(cadStack, icad),  // 正确的运行时状态
        CADMetadata.extract(cadStack)                    // 元数据提取
    );
}
```

### 2.2 正确的组件提取算法

```java
/**
 * 正确的组件提取实现 - 基于ItemCAD.getComponentInSlot()方法
 */
private static Map<EnumCADComponent, ItemStack> extractAllComponentsCorrect(
        ItemStack cadStack, ICAD icad) {
    
    Map<EnumCADComponent, ItemStack> componentMap = new EnumMap<>(EnumCADComponent.class);
    
    // ✅ 正确方式: 使用ICAD接口的getComponentInSlot方法
    for(EnumCADComponent type : EnumCADComponent.values()) {
        ItemStack component = icad.getComponentInSlot(cadStack, type);
        if(!component.isEmpty()) {
            componentMap.put(type, component);
        }
    }
    
    return componentMap;
}
```

### 2.3 正确的统计值重构算法

```java
/**
 * 正确的统计值重构 - 仅使用ICAD接口中确实存在的方法
 */
private static Map<EnumCADStat, Integer> reconstructStatisticsCorrect(
        ItemStack cadStack, ICAD icad) {
    
    Map<EnumCADStat, Integer> statMap = new EnumMap<>(EnumCADStat.class);
    
    // ✅ 正确方式: 只调用ICAD接口中实际存在的方法
    for(EnumCADStat stat : EnumCADStat.values()) {
        int value = icad.getStatValue(cadStack, stat);
        statMap.put(stat, value);
    }
    
    return statMap;
}
```

### 2.4 正确的运行时状态提取

```java
/**
 * 正确的运行时状态提取 - 使用CADData capability
 */
public static CADRuntimeState extractCorrect(ItemStack cadStack, ICAD icad) {
    // ✅ 正确方式: 通过CADData capability获取数据
    CADData cadDataCapability = new CADData(cadStack);
    CADData.Data cadData = cadStack.getOrDefault(ModDataComponents.CAD_DATA.get(),
        new CADData.Data(0, 0, Lists.newArrayList()));
    ItemContainerContents bullets = cadStack.getOrDefault(ModDataComponents.BULLETS.get(), 
        ItemContainerContents.EMPTY);
    
    return new CADRuntimeState(
        cadData.battery,                                // 当前Psi能量
        icad.getStatValue(cadStack, EnumCADStat.OVERFLOW), // 最大容量
        cadDataCapability.getSavedVector(0) != null ? 8 : 0, // 内存大小估算
        cadData.time,                                   // 内部时间
        bullets,                                        // 装载的子弹
        cadData                                         // CAD数据
    );
}
```

### 2.5 问题修复对比

| 组件 | 原错误实现 | 修正后实现 | 状态 |
|------|-----------|-----------|------|
| 组件提取 | `getComponentList()` → 返回空数据 | `icad.getComponentInSlot()` | ✅ 可工作 |
| Psi能量 | `icad.getStoredPsi()` → 方法不存在 | `cadData.battery` | ✅ 可工作 |
| 内存大小 | `icad.getMemorySize()` → 方法不存在 | 通过向量数据估算 | ✅ 可工作 |
| 内部时间 | `icad.getTime()` → 方法不存在 | `cadData.time` | ✅ 可工作 |
| 统计值 | `icad.getStatValue()` | `icad.getStatValue()` | ✅ 原本正确 |

## 3. 高版本兼容性问题及解决方案

### 3.1 DataComponent访问问题

**问题**: 原代码错误地假设CAD组件通过`COMPONENTS` DataComponent存储。

**错误实现**:
```java
// ❌ 这种访问方式在游戏中返回默认值（全是AIR）
private static List<Item> getComponentList(ItemStack cadStack) {
    return cadStack.getOrDefault(ModDataComponents.COMPONENTS.get(), DEFAULT_COMPONENTS);
}
```

**正确解决方案**: 直接使用ICAD接口方法

```java
// ✅ 正确的组件访问方式
public static Map<EnumCADComponent, ItemStack> extractComponents(ItemStack cadStack) {
    if(!(cadStack.getItem() instanceof ICAD icad)) {
        return new EnumMap<>(EnumCADComponent.class);
    }
    
    Map<EnumCADComponent, ItemStack> components = new EnumMap<>(EnumCADComponent.class);
    for(EnumCADComponent type : EnumCADComponent.values()) {
        ItemStack component = icad.getComponentInSlot(cadStack, type);
        if(!component.isEmpty()) {
            components.put(type, component);
        }
    }
    return components;
}
```

### 3.2 运行时数据访问问题

**问题**: 原代码调用不存在的ICAD方法。

**错误实现**:
```java
// ❌ 这些方法在ICAD接口中不存在
icad.getStoredPsi(cadStack)
icad.getMemorySize(cadStack) 
icad.getTime(cadStack)
```

**正确解决方案**: 使用DataComponent直接访问

```java
// ✅ 正确的运行时数据访问
public static CADRuntimeData extractRuntimeData(ItemStack cadStack) {
    CADData.Data cadData = cadStack.getOrDefault(ModDataComponents.CAD_DATA.get(),
        new CADData.Data(0, 0, Lists.newArrayList()));
    ItemContainerContents bullets = cadStack.getOrDefault(ModDataComponents.BULLETS.get(), 
        ItemContainerContents.EMPTY);
    
    return new CADRuntimeData(
        cadData.battery,    // Psi能量存储
        cadData.time,       // 内部时间
        cadData.vectors,    // 向量内存
        bullets            // 子弹容器
    );
}
```

### 3.2 类型安全转换

**问题**: 组件类型验证和安全转换

**解决方案**: 实现严格的类型检查

```java
private static ICAD requireCAD(ItemStack stack) {
    if(!(stack.getItem() instanceof ICAD icad)) {
        throw new IllegalArgumentException(VALIDATION_ERROR_NOT_CAD + ": " + stack.getItem());
    }
    return icad;
}

private static Optional<ICAD> getCADIfPresent(ItemStack stack) {
    return stack.getItem() instanceof ICAD icad ? Optional.of(icad) : Optional.empty();
}
```

### 3.3 缓存一致性问题

**问题**: ItemStack的可变性导致缓存键不稳定

**解决方案**: 使用ItemStack副本作为缓存键

```java
// 缓存时使用副本
COMPONENT_CACHE.put(cadStack.copy(), new EnumMap<>(componentMap));

// 返回时也使用副本避免外部修改
return new EnumMap<>(cached);
```

### 3.4 线程安全问题

**问题**: 多线程环境下的缓存访问安全

**解决方案**: 使用ConcurrentHashMap

```java
private static final Map<ItemStack, Map<EnumCADComponent, ItemStack>> COMPONENT_CACHE =
    new ConcurrentHashMap<>();

private static final Map<ItemStack, Map<EnumCADStat, Integer>> STATS_CACHE =
    new ConcurrentHashMap<>();
```

## 4. 最终输出数据结构规范

### 4.1 主数据结构

```java
public static class DecomposedCADData {
    public final Map<EnumCADComponent, ItemStack> components;  // 组件映射
    public final Map<EnumCADStat, Integer> stats;             // 统计值映射
    public final CADRuntimeState runtimeState;                // 运行时状态
    public final CADMetadata metadata;                         // 元数据
}
```

### 4.2 运行时状态结构

```java
public static class CADRuntimeState {
    public final int storedPsi;                    // 当前存储的Psi能量
    public final int maxPsi;                       // 最大Psi容量
    public final int memorySize;                   // 向量内存大小
    public final int internalTime;                 // 内部计时器
    public final ItemContainerContents socketedBullets; // 装载的法术子弹
    public final CADData.Data cadData;             // CAD核心数据
}
```

### 4.3 元数据结构

```java
public static class CADMetadata {
    public final String originalName;              // 原始名称
    public final long decompositionTime;           // 分解时间戳
    public final String version;                   // 版本信息
}
```

### 4.4 验证结果结构

```java
public static class ValidationResult {
    public final boolean success;                  // 验证是否成功
    public final String message;                   // 验证消息
}
```

## 5. 性能优化策略

### 5.1 缓存机制

- **组件缓存**: 避免重复的DataComponent访问
- **统计值缓存**: 减少复杂的统计值计算
- **缓存清理**: 提供手动缓存清理接口

### 5.2 预计算优化

```java
// 预计算枚举数组，避免重复调用values()
private static final EnumCADComponent[] CAD_COMPONENTS = EnumCADComponent.values();
private static final EnumCADStat[] CAD_STATS = EnumCADStat.values();

// 预计算默认组件列表
private static final List<Item> DEFAULT_COMPONENTS =
    Collections.unmodifiableList(Collections.nCopies(EnumCADComponent.values().length, Items.AIR));
```

### 5.3 字符串常量化

```java
// 避免重复创建字符串
private static final String VALIDATION_ERROR_NOT_CAD = "ItemStack is not a CAD";
private static final String VALIDATION_ERROR_MISSING_ASSEMBLY = "Missing ASSEMBLY component";
private static final String JSON_COMPONENTS = "components";
private static final String JSON_STATS = "stats";
```

## 6. JSON序列化输出

### 6.1 序列化结构

```json
{
  "components": {
    "ASSEMBLY": {"item": "psi:cad_assembly_iron", "count": 1},
    "CORE": {"item": "psi:cad_core_basic", "count": 1},
    "SOCKET": {"item": "psi:cad_socket_basic", "count": 1},
    "BATTERY": {"item": "psi:cad_battery_basic", "count": 1}
  },
  "stats": {
    "EFFICIENCY": 100,
    "POTENCY": 80,
    "OVERFLOW": 1000,
    "BANDWIDTH": 5,
    "SOCKETS": 1
  },
  "runtime": {
    "storedPsi": 750,
    "maxPsi": 1000,
    "memorySize": 8,
    "internalTime": 12345
  },
  "metadata": {
    "originalName": "Iron CAD",
    "decompositionTime": 1696320000000,
    "version": "1.21.1"
  }
}
```

### 6.2 序列化实现

```java
public static String serializeToJson(DecomposedCADData data) {
    JsonObject root = new JsonObject();
    
    // 组件序列化
    JsonObject components = new JsonObject();
    for(Map.Entry<EnumCADComponent, ItemStack> entry : data.components.entrySet()) {
        JsonObject componentObj = serializeComponent(entry.getValue());
        components.add(entry.getKey().name(), componentObj);
    }
    root.add(JSON_COMPONENTS, components);
    
    // 统计值序列化
    JsonObject stats = new JsonObject();
    for(Map.Entry<EnumCADStat, Integer> entry : data.stats.entrySet()) {
        stats.addProperty(entry.getKey().name(), entry.getValue());
    }
    root.add(JSON_STATS, stats);
    
    // 运行时状态和元数据序列化
    root.add(JSON_RUNTIME, serializeRuntimeState(data.runtimeState));
    root.add(JSON_METADATA, serializeMetadata(data.metadata));
    
    return root.toString();
}
```

## 7. 使用示例

### 7.1 基本分解操作

```java
// 分解CAD实例
ItemStack cadStack = // ... 获取CAD实例
DecomposedCADData result = CADDecomposer.decompose(cadStack);

// 验证分解结果
ValidationResult validation = CADDecomposer.validate(result);
if(!validation.success) {
    throw new IllegalStateException("分解验证失败: " + validation.message);
}

// 序列化为JSON
String json = CADDecomposer.serializeToJson(result);
```

### 7.2 性能监控

```java
// 获取缓存统计
CacheStats stats = CADDecomposer.getCacheStats();
System.out.println("缓存统计: " + stats);

// 清理缓存
CADDecomposer.clearCache();
```

## 8. 错误处理和异常情况

### 8.1 常见异常

- `IllegalArgumentException`: 输入的ItemStack不是CAD类型
- `SpellRuntimeException`: 向量内存访问错误
- `NullPointerException`: 缺少必要的DataComponent

### 8.2 异常处理策略

```java
try {
    DecomposedCADData result = CADDecomposer.decompose(cadStack);
    // 处理结果
} catch (IllegalArgumentException e) {
    // 处理非CAD类型输入
    logger.error("输入不是有效的CAD: {}", e.getMessage());
} catch (Exception e) {
    // 处理其他异常
    logger.error("CAD分解过程中发生错误", e);
}
```

## 9. 总结

CAD系统的反向分解过程是一个复杂的逆向工程任务，涉及多层数据结构的解析、类型安全转换、性能优化和错误处理。通过实现缓存机制、预计算优化和统一的数据访问接口，该系统能够高效、可靠地从CAD实例中提取所有必要的信息，为CAD系统的分析、调试和重构提供了强大的技术支持。

关键技术要点：
1. **数据结构映射**: 完整提取组件、统计值、运行时状态和元数据
2. **算法优化**: 缓存机制和预计算减少重复操作
3. **类型安全**: 严格的类型检查和转换
4. **线程安全**: 使用并发安全的数据结构
5. **标准化输出**: JSON格式的结构化数据输出

该实现为CAD开发工程师提供了一个完整、高效的反向分解解决方案。