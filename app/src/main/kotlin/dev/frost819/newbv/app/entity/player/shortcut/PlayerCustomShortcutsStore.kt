package dev.frost819.newbv.app.entity.player.shortcut

import dev.frost819.newbv.data.datastore.Prefs

/**
 * 自定义快捷键持久化存储。
 *
 * 通过 [Prefs.playerCustomShortcuts]（DataStore JSON 字符串）进行 CRUD 操作。
 */
object PlayerCustomShortcutsStore {
    /** 获取所有快捷键绑定。 */
    fun get(): List<PlayerCustomShortcut> = PlayerCustomShortcutsCodec.parse(Prefs.playerCustomShortcuts)

    /** 获取快捷键映射（keyCode → shortcut）。 */
    fun getByKey(): Map<Int, PlayerCustomShortcut> = get().associateBy { it.keyCode }

    /** 保存快捷键列表（覆盖写入）。 */
    fun save(shortcuts: List<PlayerCustomShortcut>): List<PlayerCustomShortcut> {
        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)
        Prefs.playerCustomShortcuts = PlayerCustomShortcutsCodec.serialize(normalized)
        return normalized
    }

    /** 新增或更新单个快捷键绑定。 */
    fun upsert(
        keyCode: Int,
        action: PlayerCustomShortcutAction,
    ): List<PlayerCustomShortcut> {
        val next =
            get()
                .filterNot { it.keyCode == keyCode }
                .plus(PlayerCustomShortcut(keyCode, action))
        return save(next)
    }

    /** 删除指定键码的快捷键绑定。 */
    fun remove(keyCode: Int): List<PlayerCustomShortcut> = save(get().filterNot { it.keyCode == keyCode })

    /** 清空所有快捷键绑定。 */
    fun clear(): List<PlayerCustomShortcut> = save(emptyList())
}
