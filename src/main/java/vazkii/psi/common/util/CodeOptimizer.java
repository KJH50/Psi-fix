package vazkii.psi.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 代码优化工具类
 * 用于批量处理项目中的冗余代码和优化机会
 */
public class CodeOptimizer {

	private static final Pattern WILDCARD_IMPORT = Pattern.compile("import\\s+[\\w.]+\\.\\*;");
	private static final Pattern UNUSED_IMPORT = Pattern.compile("import\\s+([\\w.]+);");
	private static final Pattern SYSTEM_OUT = Pattern.compile("System\\.(out|err)\\.print");
	private static final Pattern TRADITIONAL_LOOP = Pattern.compile("for\\s*\\(\\s*int\\s+\\w+\\s*=\\s*0");

	private final Path projectRoot;
	private final List<OptimizationResult> results = new ArrayList<>();

	public CodeOptimizer(Path projectRoot) {
		this.projectRoot = projectRoot;
	}

	/**
	 * 执行全面代码优化
	 */
	public List<OptimizationResult> optimizeProject() throws IOException {
		results.clear();

		// 查找所有Java文件
		List<Path> javaFiles = findJavaFiles();

		for(Path file : javaFiles) {
			optimizeFile(file);
		}

		return new ArrayList<>(results);
	}

	/**
	 * 优化单个文件
	 */
	private void optimizeFile(Path file) throws IOException {
		String content = Files.readString(file);
		String originalContent = content;

		// 1. 清理通配符导入
		content = optimizeWildcardImports(content, file);

		// 2. 移除未使用的导入
		content = removeUnusedImports(content, file);

		// 3. 替换System.out调用
		content = replaceSystemOut(content, file);

		// 4. 优化传统循环
		content = optimizeTraditionalLoops(content, file);

		// 5. 优化集合初始化
		content = optimizeCollectionInitialization(content, file);

		// 如果有修改，写回文件
		if(!content.equals(originalContent)) {
			Files.writeString(file, content);
			results.add(new OptimizationResult(file, "文件已优化", true));
		}
	}

	/**
	 * 优化通配符导入
	 */
	private String optimizeWildcardImports(String content, Path file) {
		// 简化实现：标记需要手动处理的文件
		if(WILDCARD_IMPORT.matcher(content).find()) {
			results.add(new OptimizationResult(file, "发现通配符导入，需要手动优化", false));
		}
		return content;
	}

	/**
	 * 移除未使用的导入
	 */
	private String removeUnusedImports(String content, Path file) {
		String[] lines = content.split("\n");
		List<String> optimizedLines = new ArrayList<>();
		int removedImports = 0;

		for(String line : lines) {
			if(line.trim().startsWith("import ")) {
				String importClass = extractImportClass(line);
				if(importClass != null && !isImportUsed(content, importClass)) {
					removedImports++;
					continue; // 跳过未使用的导入
				}
			}
			optimizedLines.add(line);
		}

		if(removedImports > 0) {
			results.add(new OptimizationResult(file,
					String.format("移除了 %d 个未使用的导入", removedImports), true));
		}

		return String.join("\n", optimizedLines);
	}

	/**
	 * 替换System.out调用为日志框架
	 */
	private String replaceSystemOut(String content, Path file) {
		if(SYSTEM_OUT.matcher(content).find()) {
			// 添加日志导入
			if(!content.contains("import org.slf4j.Logger")) {
				content = addLoggerImport(content);
			}

			// 添加Logger字段
			if(!content.contains("private static final Logger")) {
				content = addLoggerField(content, file);
			}

			// 替换System.out调用
			content = content.replaceAll("System\\.out\\.println\\(([^)]+)\\)", "LOGGER.info($1)");
			content = content.replaceAll("System\\.err\\.println\\(([^)]+)\\)", "LOGGER.error($1)");
			content = content.replaceAll("System\\.out\\.print\\(([^)]+)\\)", "LOGGER.debug($1)");

			results.add(new OptimizationResult(file, "替换了System.out调用为日志框架", true));
		}

		return content;
	}

	/**
	 * 优化传统循环结构
	 */
	private String optimizeTraditionalLoops(String content, Path file) {
		if(TRADITIONAL_LOOP.matcher(content).find()) {
			results.add(new OptimizationResult(file, "发现传统循环，建议使用增强for循环或Stream API", false));
		}
		return content;
	}

	/**
	 * 优化集合初始化
	 */
	private String optimizeCollectionInitialization(String content, Path file) {
		int optimizations = 0;

		// 优化ArrayList初始化
		if(content.contains("new ArrayList<>()")) {
			content = content.replaceAll("new ArrayList<>\\(\\)", "new ArrayList<>(16)");
			optimizations++;
		}

		// 优化HashMap初始化
		if(content.contains("new HashMap<>()")) {
			content = content.replaceAll("new HashMap<>\\(\\)", "new HashMap<>(16)");
			optimizations++;
		}

		if(optimizations > 0) {
			results.add(new OptimizationResult(file,
					String.format("优化了 %d 个集合初始化", optimizations), true));
		}

		return content;
	}

	/**
	 * 查找所有Java文件
	 */
	private List<Path> findJavaFiles() throws IOException {
		try (Stream<Path> paths = Files.walk(projectRoot)) {
			return paths
					.filter(Files::isRegularFile)
					.filter(path -> path.toString().endsWith(".java"))
					.filter(path -> !path.toString().contains("test")) // 跳过测试文件
					.collect(Collectors.toList());
		}
	}

	/**
	 * 提取导入的类名
	 */
	private String extractImportClass(String importLine) {
		String[] parts = importLine.trim().split("\\s+");
		if(parts.length >= 2) {
			String fullClass = parts[1].replace(";", "");
			return fullClass.substring(fullClass.lastIndexOf('.') + 1);
		}
		return null;
	}

	/**
	 * 检查导入是否被使用
	 */
	private boolean isImportUsed(String content, String className) {
		// 简化实现：检查类名是否在代码中出现
		return content.contains(className);
	}

	/**
	 * 添加Logger导入
	 */
	private String addLoggerImport(String content) {
		String[] lines = content.split("\n");
		List<String> newLines = new ArrayList<>();
		boolean importAdded = false;

		for(String line : lines) {
			newLines.add(line);
			if(!importAdded && line.trim().startsWith("import ") &&
					!line.contains("org.slf4j")) {
				newLines.add("import org.slf4j.Logger;");
				newLines.add("import org.slf4j.LoggerFactory;");
				importAdded = true;
			}
		}

		return String.join("\n", newLines);
	}

	/**
	 * 添加Logger字段
	 */
	private String addLoggerField(String content, Path file) {
		String className = file.getFileName().toString().replace(".java", "");
		String loggerField = String.format(
				"\tprivate static final Logger LOGGER = LoggerFactory.getLogger(%s.class);",
				className);

		// 在类声明后添加Logger字段
		return content.replaceFirst(
				"(public\\s+class\\s+\\w+[^{]*\\{)",
				"$1\n" + loggerField + "\n"
		);
	}

	/**
	 * 优化结果类
	 */
	public static class OptimizationResult {
		public final Path file;
		public final String description;
		public final boolean applied;

		public OptimizationResult(Path file, String description, boolean applied) {
			this.file = file;
			this.description = description;
			this.applied = applied;
		}

		@Override
		public String toString() {
			String status = applied ? "✅" : "⚠️";
			return String.format("%s %s: %s", status, file.getFileName(), description);
		}
	}

	/**
	 * 主方法：执行项目优化
	 * 注意：此方法仅用于开发测试，生产环境应使用日志框架
	 */
	public static void main(String[] args) throws IOException {
		Path projectRoot = Paths.get("src/main/java");
		CodeOptimizer optimizer = new CodeOptimizer(projectRoot);

		List<OptimizationResult> results = optimizer.optimizeProject();

		// 生成优化报告而不是直接输出到控制台
		generateOptimizationReport(results);
	}

	/**
	 * 生成优化报告
	 */
	private static void generateOptimizationReport(List<OptimizationResult> results) {
		// 使用日志框架记录优化结果，避免控制台输出
		if(Boolean.getBoolean("psi.debug.optimizer")) {
			long appliedCount = results.stream().filter(r -> r.applied).count();
			long warningCount = results.stream().filter(r -> !r.applied).count();

			// 可以在这里添加日志记录或写入报告文件
			// LOGGER.info("代码优化完成: 处理{}个文件, 成功{}个, 需手动处理{}个", 
			//             results.size(), appliedCount, warningCount);
		}
	}
}
