package vazkii.psi.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import vazkii.psi.common.core.handler.PsiSoundHandler;

/**
 * 声音播放工具类，统一管理声音播放逻辑
 */
public final class SoundHelper {

	// 常用音量和音调常量
	public static final float DEFAULT_VOLUME = 0.5F;
	public static final float DEFAULT_PITCH = 1.0F;
	public static final float SOUND_VOLUME = 0.5F;
	public static final float SOUND_PITCH = 1.0F;

	/**
	 * 在玩家位置播放声音
	 */
	public static void playPlayerSound(Level world, Player player, SoundEvent sound, float volume, float pitch) {
		if(!world.isClientSide) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					sound, SoundSource.PLAYERS, volume, pitch);
		}
	}

	/**
	 * 在指定位置播放声音
	 */
	public static void playPositionalSound(Level world, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
		if(!world.isClientSide) {
			world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
					sound, source, volume, pitch);
		}
	}

	/**
	 * 在指定坐标播放声音
	 */
	public static void playPositionalSound(Level world, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch) {
		if(!world.isClientSide) {
			world.playSound(null, x, y, z, sound, source, volume, pitch);
		}
	}

	/**
	 * 播放CAD射击音效
	 */
	public static void playCadShootSound(Level world, Player player, float volume) {
		float randomPitch = (float) (0.5 + Math.random() * 0.5);
		playPlayerSound(world, player, PsiSoundHandler.cadShoot, volume, randomPitch);
	}

	/**
	 * 播放编译错误音效
	 */
	public static void playCompileErrorSound(Level world, Player player) {
		playPlayerSound(world, player, PsiSoundHandler.compileError, SOUND_VOLUME, SOUND_PITCH);
	}

	/**
	 * 播放编译错误音效（指定位置）
	 */
	public static void playCompileErrorSound(Level world, BlockPos pos, SoundSource source) {
		playPositionalSound(world, pos, PsiSoundHandler.compileError, source, SOUND_VOLUME, SOUND_PITCH);
	}

	/**
	 * 播放编译错误音效（指定坐标）
	 */
	public static void playCompileErrorSound(Level world, double x, double y, double z, SoundSource source) {
		playPositionalSound(world, x, y, z, PsiSoundHandler.compileError, source, SOUND_VOLUME, SOUND_PITCH);
	}

	/**
	 * 播放子弹创建音效
	 */
	public static void playBulletCreateSound(Level world, BlockPos pos, SoundSource source) {
		playPositionalSound(world, pos, PsiSoundHandler.bulletCreate, source, SOUND_VOLUME, SOUND_PITCH);
	}

	/**
	 * 播放子弹创建音效（指定坐标）
	 */
	public static void playBulletCreateSound(Level world, double x, double y, double z, SoundSource source) {
		playPositionalSound(world, x, y, z, PsiSoundHandler.bulletCreate, source, SOUND_VOLUME, SOUND_PITCH);
	}

	/**
	 * 播放CAD创建音效
	 */
	public static void playCadCreateSound(Level world, BlockPos pos, SoundSource source) {
		playPositionalSound(world, pos, PsiSoundHandler.cadCreate, source, SOUND_VOLUME, SOUND_PITCH);
	}

	/**
	 * 播放CAD创建音效（高音调版本）
	 */
	public static void playCadCreateSoundHighPitch(Level world, BlockPos pos, SoundSource source) {
		playPositionalSound(world, pos, PsiSoundHandler.cadCreate, source, SOUND_VOLUME, 1.2F);
	}

	/**
	 * 播放循环施法音效
	 */
	public static void playLoopcastSound(Level world, Player player) {
		float randomPitch = (float) (0.15 + Math.random() * 0.85);
		playPlayerSound(world, player, PsiSoundHandler.loopcast, 0.1F, randomPitch);
	}

	private SoundHelper() {
		// 工具类，禁止实例化
	}
}
