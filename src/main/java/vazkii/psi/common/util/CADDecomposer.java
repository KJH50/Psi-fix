package vazkii.psi.common.util;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import vazkii.psi.api.cad.EnumCADComponent;
import vazkii.psi.api.cad.EnumCADStat;
import vazkii.psi.api.cad.ICAD;
import vazkii.psi.common.core.handler.capability.CADData;
import vazkii.psi.common.item.base.ModDataComponents;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CAD实例反向分解工具类 - 修复版本
 * 兼容NeoForge 1.21.1版本
 * 
 * 主要修复点:
 * 1. 修复错误的DataComponent访问方式
 * 2. 移除不存在的ICAD接口方法调用
 * 3. 使用正确的组件提取机制
 * 4. 确保在高版本游戏中正常工作
 * 
 * 保持的优化:
 * 1. 缓存机制减少重复计算
 * 2. 统一类型检查避免重复验证
 * 3. 预计算常量提升性能
 * 4. 线程安全的缓存实现
 */
public class CADDecomposer {

	// ========== 性能优化: 预计算常量 ==========

	/** 缓存默认组件列表，避免重复创建 */
	private static final List<net.minecraft.world.item.Item> DEFAULT_COMPONENTS =
			Collections.unmodifiableList(Collections.nCopies(EnumCADComponent.values().length, Items.AIR));

	/** 缓存枚举数组，避免重复调用values() */
	private static final EnumCADComponent[] CAD_COMPONENTS = EnumCADComponent.values();
	private static final EnumCADStat[] CAD_STATS = EnumCADStat.values();

	// ========== 性能优化: 缓存机制 ==========

	/** 组件提取结果缓存 - 线程安全 */
	private static final Map<ItemStack, Map<EnumCADComponent, ItemStack>> COMPONENT_CACHE =
			new ConcurrentHashMap<>();

	/** 统计值计算结果缓存 - 线程安全 */
	private static final Map<ItemStack, Map<EnumCADStat, Integer>> STATS_CACHE =
			new ConcurrentHashMap<>();

	// ========== 可读性优化: 常量定义 ==========

	private static final String VALIDATION_ERROR_NOT_CAD = "ItemStack is not a CAD";
	private static final String VALIDATION_ERROR_MISSING_ASSEMBLY = "Missing ASSEMBLY component";
	private static final String VALIDATION_ERROR_EMPTY_COMPONENT = "Empty component: ";
	private static final String JSON_COMPONENTS = "components";
	private static final String JSON_STATS = "stats";
	private static final String JSON_RUNTIME = "runtime";
	private static final String JSON_METADATA = "metadata";

	// ========== 核心API方法 ==========

	/**
	 * 完整分解CAD实例 - 修复版本
	 * 修复点: 统一类型检查，避免重复验证
	 */
	public static DecomposedCADData decompose(ItemStack cadStack) {
		ICAD icad = requireCAD(cadStack);

		return new DecomposedCADData(
				extractAllComponentsInternal(cadStack, icad),
				reconstructStatisticsInternal(cadStack, icad),
				CADRuntimeState.extractInternal(cadStack, icad),
				CADMetadata.extract(cadStack)
		);
	}

	/**
	 * 清除缓存 - 用于内存管理
	 */
	public static void clearCache() {
		COMPONENT_CACHE.clear();
		STATS_CACHE.clear();
	}

	/**
	 * 获取缓存统计信息
	 */
	public static CacheStats getCacheStats() {
		return new CacheStats(COMPONENT_CACHE.size(), STATS_CACHE.size());
	}

	// ========== 内部工具方法 ==========

	/**
	 * 要求ItemStack必须是CAD，否则抛出异常
	 */
	private static ICAD requireCAD(ItemStack stack) {
		if(!(stack.getItem() instanceof ICAD icad)) {
			throw new IllegalArgumentException(VALIDATION_ERROR_NOT_CAD + ": " + stack.getItem());
		}
		return icad;
	}

	/**
	 * 获取CAD（如果存在）
	 */
	private static Optional<ICAD> getCADIfPresent(ItemStack stack) {
		return stack.getItem() instanceof ICAD icad ? Optional.of(icad) : Optional.empty();
	}

	// ========== 修复后的内部实现方法 ==========

	/**
	 * 提取所有组件 - 公共API
	 */
	public static Map<EnumCADComponent, ItemStack> extractAllComponents(ItemStack cadStack) {
		Optional<ICAD> cadOpt = getCADIfPresent(cadStack);
		if(cadOpt.isEmpty()) {
			return new EnumMap<>(EnumCADComponent.class);
		}

		return extractAllComponentsInternal(cadStack, cadOpt.get());
	}

	/**
	 * 提取所有组件 - 修复版本（带缓存）
	 * 修复点:
	 * 1. 使用正确的ICAD.getComponentInSlot()方法
	 * 2. 移除错误的DataComponent访问
	 * 3. 保持缓存机制
	 */
	private static Map<EnumCADComponent, ItemStack> extractAllComponentsInternal(ItemStack cadStack, ICAD icad) {
		// 检查缓存
		Map<EnumCADComponent, ItemStack> cached = COMPONENT_CACHE.get(cadStack);
		if(cached != null) {
			return new EnumMap<>(cached); // 返回副本避免外部修改
		}

		Map<EnumCADComponent, ItemStack> componentMap = new EnumMap<>(EnumCADComponent.class);

		// ✅ 修复: 使用ICAD接口的getComponentInSlot方法，这是唯一正确的方式
		for(EnumCADComponent type : CAD_COMPONENTS) {
			ItemStack component = icad.getComponentInSlot(cadStack, type);
			if(!component.isEmpty()) {
				componentMap.put(type, component);
			}
		}

		// 缓存结果
		COMPONENT_CACHE.put(cadStack.copy(), new EnumMap<>(componentMap));

		return componentMap;
	}

	/**
	 * 提取特定组件
	 */
	public static ItemStack extractComponent(ItemStack cadStack, EnumCADComponent type) {
		if(cadStack.getItem() instanceof ICAD icad) {
			return icad.getComponentInSlot(cadStack, type);
		}
		return ItemStack.EMPTY;
	}

	/**
	 * 重构统计值映射 - 公共API
	 */
	public static Map<EnumCADStat, Integer> reconstructStatistics(ItemStack cadStack) {
		Optional<ICAD> cadOpt = getCADIfPresent(cadStack);
		if(cadOpt.isEmpty()) {
			return new EnumMap<>(EnumCADStat.class);
		}

		return reconstructStatisticsInternal(cadStack, cadOpt.get());
	}

	/**
	 * 重构统计值映射 - 内部实现（带缓存）
	 * 优化点:
	 * 1. 缓存机制避免重复计算
	 * 2. 预计算的枚举数组
	 */
	private static Map<EnumCADStat, Integer> reconstructStatisticsInternal(ItemStack cadStack, ICAD icad) {
		// 检查缓存
		Map<EnumCADStat, Integer> cached = STATS_CACHE.get(cadStack);
		if(cached != null) {
			return new EnumMap<>(cached); // 返回副本避免外部修改
		}

		Map<EnumCADStat, Integer> statMap = new EnumMap<>(EnumCADStat.class);

		// 使用预计算的枚举数组
		for(EnumCADStat stat : CAD_STATS) {
			int value = icad.getStatValue(cadStack, stat);
			statMap.put(stat, value);
		}

		// 缓存结果
		STATS_CACHE.put(cadStack.copy(), new EnumMap<>(statMap));

		return statMap;
	}

	/**
	 * 验证分解结果完整性 - 优化版本
	 * 优化点: 使用预定义常量，提前返回机制
	 */
	public static ValidationResult validate(DecomposedCADData data) {
		// 检查必需组件
		if(!data.components.containsKey(EnumCADComponent.ASSEMBLY)) {
			return ValidationResult.failure(VALIDATION_ERROR_MISSING_ASSEMBLY);
		}

		// 验证DataComponent完整性 - 使用预计算的枚举数组
		for(EnumCADComponent component : CAD_COMPONENTS) {
			if(data.components.containsKey(component)) {
				ItemStack componentStack = data.components.get(component);
				if(componentStack.isEmpty()) {
					return ValidationResult.failure(VALIDATION_ERROR_EMPTY_COMPONENT + component);
				}
			}
		}

		return ValidationResult.success();
	}

	/**
	 * 序列化分解结果为JSON - 优化版本
	 * 优化点: 使用预定义常量，减少字符串创建
	 */
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

		// 运行时状态序列化
		root.add(JSON_RUNTIME, serializeRuntimeState(data.runtimeState));

		// 元数据序列化
		root.add(JSON_METADATA, serializeMetadata(data.metadata));

		return root.toString();
	}

	// ========== 辅助方法 - 提升可读性 ==========

	/**
	 * 序列化单个组件
	 */
	private static JsonObject serializeComponent(ItemStack stack) {
		JsonObject componentObj = new JsonObject();
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		componentObj.addProperty("item", itemId.toString());
		componentObj.addProperty("count", stack.getCount());
		return componentObj;
	}

	/**
	 * 序列化运行时状态
	 */
	private static JsonObject serializeRuntimeState(CADRuntimeState runtimeState) {
		JsonObject runtime = new JsonObject();
		runtime.addProperty("storedPsi", runtimeState.storedPsi);
		runtime.addProperty("maxPsi", runtimeState.maxPsi);
		runtime.addProperty("memorySize", runtimeState.memorySize);
		runtime.addProperty("internalTime", runtimeState.internalTime);
		return runtime;
	}

	/**
	 * 序列化元数据
	 */
	private static JsonObject serializeMetadata(CADMetadata metadata) {
		JsonObject metadataObj = new JsonObject();
		metadataObj.addProperty("originalName", metadata.originalName);
		metadataObj.addProperty("decompositionTime", metadata.decompositionTime);
		metadataObj.addProperty("version", metadata.version);
		return metadataObj;
	}

	/**
	 * 获取CAD数据 - 统一DataComponent访问
	 */
	private static CADData.Data getCADData(ItemStack cadStack) {
		return cadStack.getOrDefault(ModDataComponents.CAD_DATA.get(),
				new CADData.Data(0, 0, Lists.newArrayList()));
	}

	/**
	 * 获取子弹容器 - 统一DataComponent访问
	 */
	private static ItemContainerContents getBullets(ItemStack cadStack) {
		return cadStack.getOrDefault(ModDataComponents.BULLETS.get(), ItemContainerContents.EMPTY);
	}

	/**
	 * 分解结果数据结构
	 */
	public static class DecomposedCADData {
		public final Map<EnumCADComponent, ItemStack> components;
		public final Map<EnumCADStat, Integer> stats;
		public final CADRuntimeState runtimeState;
		public final CADMetadata metadata;

		public DecomposedCADData(Map<EnumCADComponent, ItemStack> components,
				Map<EnumCADStat, Integer> stats,
				CADRuntimeState runtimeState,
				CADMetadata metadata) {
			this.components = components;
			this.stats = stats;
			this.runtimeState = runtimeState;
			this.metadata = metadata;
		}
	}

	/**
	 * CAD运行时状态
	 */
	public static class CADRuntimeState {
		public final int storedPsi;
		public final int maxPsi;
		public final int memorySize;
		public final int internalTime;
		public final ItemContainerContents socketedBullets;
		public final CADData.Data cadData;

		public CADRuntimeState(int storedPsi, int maxPsi, int memorySize, int internalTime,
				ItemContainerContents socketedBullets, CADData.Data cadData) {
			this.storedPsi = storedPsi;
			this.maxPsi = maxPsi;
			this.memorySize = memorySize;
			this.internalTime = internalTime;
			this.socketedBullets = socketedBullets;
			this.cadData = cadData;
		}

		public static CADRuntimeState extract(ItemStack cadStack) {
			Optional<ICAD> cadOpt = CADDecomposer.getCADIfPresent(cadStack);
			if(cadOpt.isEmpty()) {
				return new CADRuntimeState(0, 0, 0, 0,
						ItemContainerContents.EMPTY, new CADData.Data(0, 0, Lists.newArrayList()));
			}

			return extractInternal(cadStack, cadOpt.get());
		}

		/**
		 * 内部提取方法 - 修复版本
		 * 修复点: 移除不存在的ICAD方法调用，使用正确的数据访问方式
		 */
		public static CADRuntimeState extractInternal(ItemStack cadStack, ICAD icad) {
			// ✅ 修复: 直接从DataComponent获取数据，不依赖不存在的ICAD方法
			CADData.Data cadData = CADDecomposer.getCADData(cadStack);
			ItemContainerContents bullets = CADDecomposer.getBullets(cadStack);

			return new CADRuntimeState(
					cadData.battery, // ✅ 修复: 使用cadData.battery替代不存在的getStoredPsi
					icad.getStatValue(cadStack, EnumCADStat.OVERFLOW), // ✅ 保持: 这个方法确实存在
					cadData.vectors.size(), // ✅ 修复: 通过向量数量估算内存大小
					cadData.time, // ✅ 修复: 使用cadData.time替代不存在的getTime
					bullets,
					cadData
			);
		}
	}

	/**
	 * CAD元数据
	 */
	public static class CADMetadata {
		public final String originalName;
		public final long decompositionTime;
		public final String version;

		public CADMetadata(String originalName, long decompositionTime, String version) {
			this.originalName = originalName;
			this.decompositionTime = decompositionTime;
			this.version = version;
		}

		public static CADMetadata extract(ItemStack cadStack) {
			String name = cadStack.getHoverName().getString();
			long time = System.currentTimeMillis();
			String version = "1.21.1";

			return new CADMetadata(name, time, version);
		}
	}

	/**
	 * 验证结果
	 */
	public static class ValidationResult {
		public final boolean success;
		public final String message;

		private ValidationResult(boolean success, String message) {
			this.success = success;
			this.message = message;
		}

		public static ValidationResult success() {
			return new ValidationResult(true, "Validation successful");
		}

		public static ValidationResult failure(String message) {
			return new ValidationResult(false, message);
		}
	}

	// ========== 新增: 缓存统计信息 ==========

	/**
	 * 缓存统计信息
	 */
	public static class CacheStats {
		public final int componentCacheSize;
		public final int statsCacheSize;

		public CacheStats(int componentCacheSize, int statsCacheSize) {
			this.componentCacheSize = componentCacheSize;
			this.statsCacheSize = statsCacheSize;
		}

		@Override
		public String toString() {
			return String.format("CacheStats{components=%d, stats=%d}",
					componentCacheSize, statsCacheSize);
		}
	}
}
