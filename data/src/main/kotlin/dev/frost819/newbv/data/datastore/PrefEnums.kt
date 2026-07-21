package dev.frost819.newbv.data.datastore

/**
 * 偏好设置相关枚举集合。
 *
 * 这些枚举用于 DataStore 持久化，仅保留持久化所需的 code/ordinal 与反序列化方法。
 * 显示名称（getDisplayName）由 app 层扩展，避免 data 模块依赖 Android 资源。
 *
 * 当 bili-api / player / danmaku 模块迁移完成后，部分枚举（ApiType / Resolution /
 * VideoCodec / Audio）可迁移至对应模块，data 层改为引用。
 */

// ===== 网络 & API =====

/**
 * 接口类型。
 *
 * 控制使用 Web HTTP API 还是 App gRPC API。
 * 通过 ordinal 持久化，反序列化时越界回退到 [Web]。
 */
enum class ApiType {
    /** Web HTTP API（需 WBI 签名）。 */
    Web,

    /** App gRPC API（需 App 签名）。 */
    App;

    companion object {
        /** 从序号安全解析，越界返回 [Web]。 */
        fun fromOrdinal(ordinal: Int): ApiType = entries.getOrElse(ordinal) { Web }
    }
}

// ===== 播放器 - 视频 =====

/**
 * 视频画质。
 *
 * code 对应 B 站 `qn` 参数。通过 [code] 持久化，反序列化时未知 code 回退到 [R1080P]。
 *
 * @property code B 站画质标识（qn）。
 */
enum class Resolution(val code: Int) {
    R240P(6),
    R360P(16),
    R480P(32),
    R720P(64),
    R720P60(74),
    R1080P(80),
    R1080PPlus(112),
    R1080P60(116),
    R4K(120),
    RHdr(125),
    RDolby(126),
    R8K(127);

    companion object {
        /** 从 code 安全解析，未知 code 返回 [R1080P]。 */
        fun fromCode(code: Int): Resolution = entries.find { it.code == code } ?: R1080P
    }
}

/**
 * 视频编码。
 *
 * 通过 ordinal 持久化，反序列化时越界回退到 [AVC]。
 *
 * @property prefix 编码前缀，用于从 codec string 匹配（如 `avc1`、`hev1`）。
 * @property codecId B 站编码 ID。
 */
enum class VideoCodec(val prefix: String, val codecId: Int) {
    AVC("avc1", 7),
    HEVC("hev1", 12),
    AV1("av01", 13),
    DVH1("dvh1", 0),
    HVC1("hvc", 0);

    companion object {
        /** 从 ordinal 安全解析，越界返回 [AVC]。 */
        fun fromCode(ordinal: Int): VideoCodec = entries.find { it.ordinal == ordinal } ?: AVC

        /** 从 codec string（如 `avc1.640028`）匹配编码，无匹配返回 null。 */
        fun fromCodecString(codec: String): VideoCodec? = runCatching {
            entries.forEach { if (codec.startsWith(it.prefix)) return it }
            null
        }.getOrNull()

        /** 从 B 站 codecId 匹配，无匹配返回 [AVC]。 */
        fun fromCodecId(codecId: Int): VideoCodec = entries.find { it.codecId == codecId } ?: AVC
    }
}

/**
 * 播放结束动作。
 *
 * 通过 [code] 持久化，反序列化时未知 code 回退到 [PlayNext]。
 *
 * @property code 动作标识。
 */
enum class ActionAfterPlay(val code: Int) {
    Pause(0),
    PlayNext(1),
    Exit(2),
    PlayRelated(3);

    companion object {
        /** 从 code 安全解析，未知 code 返回 [PlayNext]。 */
        fun fromCode(code: Int): ActionAfterPlay = entries.find { it.code == code } ?: PlayNext
    }
}

// ===== 播放器 - 音频 =====

/**
 * 音频编码。
 *
 * code 对应 B 站音频 ID。通过 [code] 持久化，反序列化时未知 code 回退到 [A192K]。
 *
 * @property code B 站音频标识。
 */
enum class Audio(val code: Int) {
    A64K(30216),
    A132K(30232),
    A192K(30280),
    ADolbyAtoms(30250),
    AHiRes(30251);

    companion object {
        /** 从 code 安全解析，未知 code 返回 [A192K]。 */
        fun fromCode(code: Int): Audio = entries.find { it.code == code } ?: A192K
    }
}

// ===== 播放器 - 弹幕 =====

/**
 * 弹幕类型。
 *
 * 通过 ordinal 持久化，用于多选过滤。
 */
enum class DanmakuType {
    /** 全部（元类型，选中时表示显示所有）。 */
    All,

    /** 顶部弹幕。 */
    Top,

    /** 滚动弹幕。 */
    Rolling,

    /** 底部弹幕。 */
    Bottom;
}

// ===== 播放器 - 界面 =====

/**
 * 播放速度。
 *
 * 通过 [code] 持久化，反序列化时未知 code 回退到 [X1]。
 *
 * @property code 速度标识。
 * @property speed 实际倍速值。
 */
enum class PlaySpeed(val code: Int, val speed: Float) {
    X0_5(0, 0.5f),
    X1(1, 1f),
    X1_25(2, 1.25f),
    X1_5(3, 1.5f),
    X2(4, 2f);

    companion object {
        /** 从 code 安全解析，未知 code 返回 [X1]。 */
        fun fromCode(code: Int): PlaySpeed = entries.find { it.code == code } ?: X1

        /** 从倍速值匹配，无精确匹配返回 [X1]。 */
        fun fromSpeed(speed: Float): PlaySpeed = entries.find { it.speed == speed } ?: X1
    }
}

// ===== 应用界面 =====

/**
 * 左侧导航项（启动页）。
 *
 * 通过 ordinal 持久化，反序列化时越界回退到 [Home]。
 * 相对原版新增 [Live] 项（PRD 7.2）。
 */
enum class LeftNaviItem {
    Search,
    Personal,
    Home,
    UGC,
    PGC,
    Live;

    companion object {
        /** 从序号安全解析，越界返回 [Home]。 */
        fun fromOrdinal(ordinal: Int): LeftNaviItem = entries.getOrElse(ordinal) { Home }
    }
}

/**
 * 首页顶部 Tab。
 *
 * 通过 [code] 持久化，反序列化时未知 code 回退到 [Dynamics]。
 *
 * @property code Tab 标识。
 */
enum class HomeTopNavItem(val code: Int) {
    Dynamics(0),
    Recommend(1),
    Popular(2);

    companion object {
        /** 从 code 安全解析，未知 code 返回 [Dynamics]。 */
        fun fromCode(code: Int): HomeTopNavItem = entries.find { it.code == code } ?: Dynamics
    }
}

/**
 * 个人页顶部 Tab。
 *
 * 通过 ordinal 持久化，反序列化时越界回退到 [ToView]。
 */
enum class PersonalTopNavItem {
    ToView,
    History,
    Favorite,
    FollowingSeason;

    companion object {
        /** 从序号安全解析，越界返回 [ToView]。 */
        fun fromOrdinal(ordinal: Int): PersonalTopNavItem = entries.getOrElse(ordinal) { ToView }
    }
}

/**
 * 主题模式。
 *
 * 通过 ordinal 持久化，反序列化时越界回退到 [FollowSystem]。
 *
 * 注意：core 模块已有同名 `ThemeMode` 枚举，此处在 data 模块独立定义以避免循环依赖。
 * app 层负责两者之间的映射。
 */
enum class ThemeMode {
    /** 跟随系统暗色模式。 */
    FollowSystem,

    /** 强制深色。 */
    Dark,

    /** 强制浅色。 */
    Light;

    companion object {
        /** 从序号安全解析，越界返回 [FollowSystem]。 */
        fun fromOrdinal(ordinal: Int): ThemeMode = entries.getOrElse(ordinal) { FollowSystem }
    }
}
