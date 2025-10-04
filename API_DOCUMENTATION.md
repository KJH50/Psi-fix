# Psi Mod API 文档

## 概述
Psi Mod 提供了一个完整的法术编程系统，允许玩家创建和执行复杂的法术。本文档详细介绍了主要的API接口和使用方法。

## 核心API

### 1. 法术系统 (Spell System)

#### SpellContext
法术执行的上下文环境，包含执行所需的所有信息。

```java
public final class SpellContext {
    public Player caster;                    // 施法者
    public Entity focalPoint;               // 焦点实体
    public CompiledSpell cspell;            // 编译后的法术
    public int loopcastIndex = 0;           // 循环施法索引
    public final Map<String, Object> customData; // 自定义数据
    
    // 距离检查
    public boolean isInRadius(Vector3 vec);
    public boolean isInRadius(Entity e);
    public boolean isInRadius(double x, double y, double z);
}
```

#### SpellCompiler
法术编译器，将法术网格编译为可执行的法术。

```java
public interface ISpellCompiler {
    Either<CompiledSpell, SpellCompilationException> compile(Spell in);
}
```

#### CompiledSpell
编译后的法术，包含执行逻辑和元数据。

```java
public class CompiledSpell {
    public final Spell sourceSpell;
    public final SpellMetadata metadata;
    public final Stack<Action> actions;
    
    public boolean execute(SpellContext context) throws SpellRuntimeException;
    public void safeExecute(SpellContext context);
}
```

### 2. CAD系统 (Casting Assistant Device)

#### ItemCAD
CAD物品的核心实现，管理法术存储和执行。

```java
public class ItemCAD extends Item implements ICAD {
    // CAD组件管理
    public ItemStack getComponentInSlot(ItemStack stack, EnumCADComponent type);
    public void setComponent(ItemStack stack, ItemStack component);
    
    // 法术管理
    public void setSpell(Player player, ItemStack stack, Spell spell);
    public Spell getSpell(ItemStack stack);
    
    // PSI能量管理
    public int getStoredPsi(ItemStack stack);
    public void regenPsi(ItemStack stack, int psi);
}
```

#### CADDecomposer
CAD分解器，用于分析和重构CAD组件。

```java
public class CADDecomposer {
    public static DecomposedCADData decompose(ItemStack cadStack);
    public static List<ItemStack> extractAllComponents(ItemStack cadStack);
    public static Map<EnumCADStat, Integer> reconstructStatistics(ItemStack cadStack);
    public static void clearCache();
}
```

### 3. 法术片段系统 (Spell Pieces)

#### SpellPiece
所有法术片段的基类。

```java
public abstract class SpellPiece {
    public int x, y;  // 网格位置
    public final Map<SpellParam<?>, SpellParam.Side> paramSides;
    
    // 核心方法
    public abstract Object execute(SpellContext context) throws SpellRuntimeException;
    public abstract Class<?> getEvaluationType();
    public abstract EnumPieceType getPieceType();
    public abstract void addToMetadata(SpellMetadata meta) throws SpellCompilationException;
}
```

#### 法术片段类型
- **Trick**: 执行具体动作的法术片段
- **Operator**: 数据处理和计算
- **Selector**: 目标选择
- **Constant**: 常量值

### 4. 网络通信 (Network Communication)

#### MessageRegister
网络消息注册和分发系统。

```java
public class MessageRegister {
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload message);
    public static void sendToServer(CustomPacketPayload message);
}
```

#### 主要消息类型
- `MessageDataSync`: 数据同步
- `MessageParticleTrail`: 粒子轨迹效果
- `MessageVisualEffect`: 视觉效果
- `MessageSpellError`: 法术错误信息

### 5. 数学工具 (Math Utilities)

#### Vector3
三维向量操作。

```java
public class Vector3 {
    public double x, y, z;
    
    public Vector3 add(Vector3 other);
    public Vector3 subtract(Vector3 other);
    public Vector3 multiply(double scalar);
    public double dotProduct(Vector3 other);
    public Vector3 crossProduct(Vector3 other);
}
```

#### MathHelper
数学辅助函数。

```java
public class MathHelper {
    public static double pointDistanceSpace(double x1, double y1, double z1, 
                                          double x2, double y2, double z2);
    public static Vector3 rotateVectorAroundVector(Vector3 vec, Vector3 axis, double angle);
}
```

## 性能优化特性

### 1. 缓存系统
- **几何缓存**: GUI渲染中的几何数据缓存
- **HUD缓存**: HUD元素的帧级缓存
- **数学函数缓存**: Gamma函数等数学计算结果缓存
- **网络消息缓存**: 频繁使用的网络消息缓存

### 2. 批处理优化
- **渲染批处理**: 减少OpenGL状态切换
- **粒子批处理**: 批量处理粒子效果
- **网络批处理**: 批量发送网络消息

### 3. 内存优化
- **对象池化**: 重用频繁创建的对象
- **集合预分配**: 根据预期大小初始化集合
- **延迟计算**: 按需计算边界和统计信息

## 扩展开发

### 创建自定义法术片段

```java
@SpellPiece(name = "my_custom_trick")
public class MyCustomTrick extends SpellPiece {
    SpellParam<Vector3> position = new SpellParam<>(SpellParam.GENERIC_NAME_POSITION, 
                                                   SpellParam.BLUE, false);
    
    @Override
    public Object execute(SpellContext context) throws SpellRuntimeException {
        Vector3 pos = this.getParamValue(context, position);
        // 执行自定义逻辑
        return null;
    }
    
    @Override
    public Class<?> getEvaluationType() {
        return Void.class;
    }
    
    @Override
    public EnumPieceType getPieceType() {
        return EnumPieceType.TRICK;
    }
}
```

### 创建自定义CAD组件

```java
public class MyCADComponent extends Item implements ICADComponent {
    @Override
    public EnumCADComponent getComponentType(ItemStack stack) {
        return EnumCADComponent.CORE;
    }
    
    @Override
    public int getCADStatValue(ItemStack stack, EnumCADStat stat) {
        return switch(stat) {
            case EFFICIENCY -> 100;
            case POTENCY -> 80;
            default -> 0;
        };
    }
}
```

## 事件系统

### 法术事件
- `SpellCastEvent`: 法术施放事件
- `SpellCompileEvent`: 法术编译事件
- `PsiRegenEvent`: PSI能量恢复事件

### CAD事件
- `CADEquipEvent`: CAD装备事件
- `CADComponentChangeEvent`: CAD组件变更事件

## 配置选项

### 性能配置
```java
// 法术缓存大小
public static final ForgeConfigSpec.IntValue spellCacheSize;

// 内存优化选项
public static final ForgeConfigSpec.BooleanValue enableMemoryOptimization;

// 渲染优化选项
public static final ForgeConfigSpec.BooleanValue enableRenderOptimization;
```

### 游戏平衡配置
```java
// PSI能量相关
public static final ForgeConfigSpec.IntValue maxPsiCapacity;
public static final ForgeConfigSpec.IntValue psiRegenRate;

// 法术限制
public static final ForgeConfigSpec.IntValue maxSpellComplexity;
public static final ForgeConfigSpec.DoubleValue spellRange;
```

## 最佳实践

### 1. 性能优化
- 使用缓存避免重复计算
- 批量处理相似操作
- 合理使用对象池
- 避免在热路径中创建临时对象

### 2. 法术设计
- 保持法术网格简洁
- 合理使用错误处理
- 避免无限循环
- 考虑性能影响

### 3. 扩展开发
- 遵循现有的命名约定
- 实现适当的错误处理
- 提供清晰的文档
- 进行充分的测试

## 故障排除

### 常见错误
1. **SpellRuntimeException**: 法术运行时错误
2. **SpellCompilationException**: 法术编译错误
3. **CADComponentException**: CAD组件错误

### 调试工具
- 法术调试器：可视化法术执行流程
- 性能分析器：分析法术性能瓶颈
- 网络监视器：监控网络消息传输

## 版本兼容性

当前版本：1.21.1-105
- 支持NeoForge 1.21.1
- 向后兼容1.20.x版本的法术
- API稳定性保证

## 更多资源

- [官方网站](https://psi.vazkii.net/)
- [GitHub仓库](https://github.com/VazkiiMods/Psi)
- [Wiki文档](https://psi.vazkii.net/wiki/)
- [Discord社区](https://discord.gg/vazkii)