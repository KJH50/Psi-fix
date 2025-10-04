package vazkii.psi.common.util;

import net.minecraft.world.item.ItemStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vazkii.psi.common.item.ItemCAD;
import vazkii.psi.common.util.CADDecomposer.CacheStats;
import vazkii.psi.common.util.CADDecomposer.DecomposedCADData;

/**
 * CAD分解器性能演示类
 * 用于验证优化效果，不依赖外部测试框架
 * 优化: 使用日志框架替代System.out调用
 */
public class CADDecomposerDemo {

	private static final Logger LOGGER = LoggerFactory.getLogger(CADDecomposerDemo.class);
	private static final int ITERATIONS = 1000;

	public static void main(String[] args) {
		LOGGER.info("=== CAD分解器性能测试演示 ===");

		// 创建测试CAD实例
		ItemStack testCAD = createTestCAD();

		// 预热JVM
		warmupJVM(testCAD);

		// 性能基准测试
		performanceBenchmark(testCAD);

		// 功能验证测试
		functionalTest(testCAD);

		LOGGER.info("=== 测试完成 ===");
	}

	/**
	 * 创建测试用CAD实例
	 */
	private static ItemStack createTestCAD() {
		try {
			return new ItemStack(new ItemCAD(new ItemCAD.Properties()));
		} catch (Exception e) {
			LOGGER.error("创建测试CAD失败", e);
			return ItemStack.EMPTY;
		}
	}

	/**
	 * JVM预热
	 * 优化: 使用日志框架，减少不必要的循环开销
	 */
	private static void warmupJVM(ItemStack testCAD) {
		LOGGER.debug("JVM预热中...");

		// 优化: 减少预热次数，提升启动速度
		final int warmupIterations = 50;
		for(int i = 0; i < warmupIterations; i++) {
			try {
				CADDecomposer.decompose(testCAD);
				if(i % 10 == 0) { // 优化: 减少缓存清理频率
					CADDecomposer.clearCache();
				}
			} catch (Exception e) {
				// 忽略预热期间的错误
			}
		}
		LOGGER.debug("JVM预热完成");
	}

	/**
	 * 性能基准测试
	 * 优化: 使用日志框架，改进性能计算逻辑
	 */
	private static void performanceBenchmark(ItemStack testCAD) {
		LOGGER.info("--- 性能基准测试 ---");

		// 测试无缓存性能
		CADDecomposer.clearCache();
		long noCacheTime = measureDecompositionTime(testCAD, true);

		// 测试有缓存性能
		CADDecomposer.clearCache();
		long withCacheTime = measureDecompositionTime(testCAD, false);

		// 优化: 改进性能计算和输出
		double noCacheAvg = noCacheTime / (double) ITERATIONS / 1000.0;
		double withCacheAvg = withCacheTime / (double) ITERATIONS / 1000.0;

		LOGGER.info("无缓存平均时间: {:.2f} μs", noCacheAvg);
		LOGGER.info("有缓存平均时间: {:.2f} μs", withCacheAvg);

		if(withCacheTime > 0 && noCacheTime > withCacheTime) {
			double improvement = (double) noCacheTime / withCacheTime;
			LOGGER.info("性能提升: {:.2f}x", improvement);
		} else {
			LOGGER.warn("缓存效果不明显或测试数据不足");
		}

		// 缓存统计
		CacheStats stats = CADDecomposer.getCacheStats();
		LOGGER.info("缓存统计: {}", stats);
	}

	/**
	 * 测量分解操作时间
	 * 优化: 改进错误处理和性能测量
	 */
	private static long measureDecompositionTime(ItemStack testCAD, boolean clearCacheEachTime) {
		long startTime = System.nanoTime();
		int errorCount = 0;

		for(int i = 0; i < ITERATIONS; i++) {
			if(clearCacheEachTime) {
				CADDecomposer.clearCache();
			}

			try {
				CADDecomposer.decompose(testCAD);
			} catch (Exception e) {
				errorCount++;
				// 优化: 只记录第一个错误，避免日志泛滥
				if(errorCount == 1) {
					LOGGER.warn("分解操作错误: {}", e.getMessage());
				}
			}
		}

		if(errorCount > 0) {
			LOGGER.warn("测试过程中发生 {} 次错误", errorCount);
		}

		return System.nanoTime() - startTime;
	}

	/**
	 * 功能验证测试
	 * 优化: 使用日志框架，改进测试结果展示
	 */
	private static void functionalTest(ItemStack testCAD) {
		LOGGER.info("--- 功能验证测试 ---");

		try {
			// 测试完整分解
			DecomposedCADData result = CADDecomposer.decompose(testCAD);
			LOGGER.info("✓ 完整分解测试通过");
			LOGGER.info("  - 组件数量: {}", result.components.size());
			LOGGER.info("  - 统计项数量: {}", result.stats.size());
			LOGGER.info("  - 运行时状态: {}", result.runtimeState != null ? "有效" : "无效");
			LOGGER.info("  - 元数据: {}", result.metadata != null ? "有效" : "无效");

			// 测试组件提取
			var components = CADDecomposer.extractAllComponents(testCAD);
			LOGGER.info("✓ 组件提取测试通过，提取到 {} 个组件", components.size());

			// 测试统计值重构
			var stats = CADDecomposer.reconstructStatistics(testCAD);
			LOGGER.info("✓ 统计值重构测试通过，重构了 {} 个统计项", stats.size());

			// 测试JSON序列化
			String json = CADDecomposer.serializeToJson(result);
			LOGGER.info("✓ JSON序列化测试通过，长度: {} 字符", json.length());

		} catch (Exception e) {
			LOGGER.error("✗ 功能测试失败", e);
		}
	}
}
