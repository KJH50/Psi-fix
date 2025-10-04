package vazkii.psi.common.lib;

/**
 * 统一的NBT标签常量定义，避免重复定义
 */
public final class LibNBTTags {

	// 通用实体标签
	public static final String TAG_COLORIZER = "colorizer";
	public static final String TAG_BULLET = "bullet";
	public static final String TAG_TIME_ALIVE = "timeAlive";
	public static final String TAG_CASTER = "caster";

	// 运动相关标签
	public static final String TAG_LAST_MOTION_X = "lastMotionX";
	public static final String TAG_LAST_MOTION_Y = "lastMotionY";
	public static final String TAG_LAST_MOTION_Z = "lastMotionZ";

	// 法术圆环特定标签
	public static final String TAG_TIMES_CAST = "timesCast";
	public static final String TAG_LOOK_X = "savedLookX";
	public static final String TAG_LOOK_Y = "savedLookY";
	public static final String TAG_LOOK_Z = "savedLookZ";

	// 法术相关标签
	public static final String TAG_SPELL = "spell";
	public static final String TAG_PLAYER_LOCK = "playerLock";
	public static final String TAG_CONSTANT_VALUE = "constantValue";

	// 遗留标签
	public static final String TAG_TIME_LEGACY = "time";
	public static final String TAG_STORED_PSI_LEGACY = "storedPsi";
	public static final String TAG_X_LEGACY = "x";
	public static final String TAG_Y_LEGACY = "y";
	public static final String TAG_Z_LEGACY = "z";

	private LibNBTTags() {
		// 工具类，禁止实例化
	}
}
