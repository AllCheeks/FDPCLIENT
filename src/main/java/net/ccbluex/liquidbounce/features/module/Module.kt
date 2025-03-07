/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.module.modules.client.HUD
import net.ccbluex.liquidbounce.features.module.modules.client.SoundModule
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.ccbluex.liquidbounce.ui.i18n.LanguageManager
import net.ccbluex.liquidbounce.utils.AnimationHelper
import net.ccbluex.liquidbounce.utils.ClassUtils
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.Animation
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.Translate
import net.ccbluex.liquidbounce.features.value.Value
import org.lwjgl.input.Keyboard
import java.util.ArrayList

open class Module @JvmOverloads constructor(
    var name: String,
    var description: String = "",
    var category: ModuleCategory,
    keyBind: Int = Keyboard.CHAR_NONE,
    array: Boolean = true,
    val canEnable: Boolean = true,
    var autoDisable: EnumAutoDisableType = EnumAutoDisableType.NONE,
    val moduleCommand: Boolean = true,
    var defaultOn: Boolean,
    val forceNoSound: Boolean = false,
    var triggerType: EnumTriggerType = EnumTriggerType.TOGGLE
) : MinecraftInstance(), Listenable, Annotation {

    var keyBind = keyBind
        set(new) {
            field = new

        if (!FDPClient.isStarting) {
            FDPClient.configManager.smartSave()
        }
    }
    var array = array
        set(new) {
            field = new

            if (!FDPClient.isStarting) {
                FDPClient.configManager.smartSave()
            }
        }

    val translate = Translate(0F,0F)
    val animation: AnimationHelper
        get() {
            TODO()
        }
    val tab = Translate(0f , 0f)
    var expanded: Boolean = false
    private var suffix: String? = null
    private val properties: List<Value<*>> = ArrayList<Value<*>>()
    var toggled = false
    var localizedName = ""
        get() = field.ifEmpty { name }
    var splicedName = ""
        get() {
            if (field.isEmpty()) {
                val sb = StringBuilder()
                val arr = name.toCharArray()
                for (i in arr.indices) {
                    val char = arr[i]
                    if (i != 0 && !Character.isLowerCase(char) && Character.isLowerCase(arr[i - 1])) {
                        sb.append(' ')
                    }
                    sb.append(char)
                }
                field = sb.toString()
            }
            return field
        }

    open fun onLoad() {
        localizedName = if(LanguageManager.getAndFormat("module.$name.name") == "module.$name.name") {
            name
        } else {
            LanguageManager.getAndFormat("module.$name.name")
        }
    }

    // Current state of module
    var state = false
        set(value) {
            if (field == value) return

            // Call toggle
            onToggle(value)

            // Play sound and add notification
            if (!FDPClient.isStarting) {
                if (value) {
                    SoundModule.playSound(true)
                    FDPClient.hud.addNotification(Notification(LanguageManager.getAndFormat("notify.module.title"), LanguageManager.getAndFormat("notify.module.enable", localizedName), NotifyType.SUCCESS))
                } else {
                    SoundModule.playSound(false)
                    FDPClient.hud.addNotification(Notification(LanguageManager.getAndFormat("notify.module.title"), LanguageManager.getAndFormat("notify.module.disable", localizedName), NotifyType.ERROR))
                }
            }

            // Call on enabled or disabled
            try {
                field = canEnable && value
                if (value) {
                    onEnable()
                } else {
                    onDisable()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            // Save module state
            FDPClient.configManager.smartSave()
        }

    // HUD
    val hue = Math.random().toFloat()
    var slideAnimation: Animation? = null
    var slide = 0f
        get() {
            if (slideAnimation != null) {
                field = slideAnimation!!.value.toFloat()
                if (slideAnimation!!.state == Animation.EnumAnimationState.STOPPED) {
                    slideAnimation = null
                }
            }
            return field
        }
        set(value) {
            if (slideAnimation == null || (slideAnimation != null && slideAnimation!!.to != value.toDouble())) {
                slideAnimation = Animation(EaseUtils.EnumEasingType.valueOf(HUD.arraylistXAxisAnimTypeValue.get()), EaseUtils.EnumEasingOrder.valueOf(HUD.arraylistXAxisAnimOrderValue.get()), field.toDouble(), value.toDouble(), HUD.arraylistXAxisAnimSpeedValue.get() * 30L).start()
            }
        }
    var yPosAnimation: Animation? = null
    open var yPos = 0f
        get() {
            if (yPosAnimation != null) {
                field = yPosAnimation!!.value.toFloat()
                if (yPosAnimation!!.state == Animation.EnumAnimationState.STOPPED) {
                    yPosAnimation = null
                }
            }
            return field
        }
        set(value) {
            if (yPosAnimation == null || (yPosAnimation != null && yPosAnimation!!.to != value.toDouble())) {
                yPosAnimation = Animation(EaseUtils.EnumEasingType.valueOf(HUD.arraylistYAxisAnimTypeValue.get()), EaseUtils.EnumEasingOrder.valueOf(HUD.arraylistYAxisAnimOrderValue.get()), field.toDouble(), value.toDouble(), HUD.arraylistYAxisAnimSpeedValue.get() * 30L).start()
            }
        }

    // Tag
    open val tag: String?
        get() = null

    val tagName: String
        get() = "$name${if (tag == null) "" else " §7$tag"}"

    val colorlessTagName: String
        get() = "$name${if (tag == null) "" else " " + stripColor(tag!!)}"

    var width = 10

    /**
     * Toggle module
     */
    fun toggle() {
        state = !state
    }
    open fun getSuffix(): String? {
        return suffix
    }

    open fun setSuffix(suffix: String?) {
        this.suffix = suffix
    }

    open fun getProperties(): List<Value<*>?>? {
        return properties
    }

    open fun hasMode(): Boolean {
        return suffix != null
    }
    open fun isToggled(): Boolean {
        return toggled
    }
    open fun toggleSilent() {
        this.toggled = !this.toggled
        if (this.toggled) {
            onEnable()
        } else {
            onDisable()
        }
    }

    /**
     * Print [msg] to chat as alert
     */
    protected fun alert(msg: String) = ClientUtils.displayAlert(msg)

    /**
     * Print [msg] to chat as plain text
     */
    protected fun chat(msg: String) = ClientUtils.displayChatMessage(msg)

    /**
     * Called when module toggled
     */
    open fun onToggle(state: Boolean) {}

    /**
     * Called when module enabled
     */
    open fun onEnable() {}

    /**
     * Called when module disabled
     */
    open fun onDisable() {}

    /**
     * Called when module initialized
     */
    open fun onInitialize() {}

    /**
     * Get all values of module
     */
    open val values: List<Value<*>>
        get() = ClassUtils.getValues(this.javaClass, this)

    /**
     * Get module by [valueName]
     */
    open fun getValue(valueName: String) = values.find { it.name.equals(valueName, ignoreCase = true) }

    /**
     * Events should be handled when module is enabled
     */
    override fun handleEvents() = state

    enum class EnumAutoDisableType {
        NONE,
        RESPAWN,
        FLAG,
        GAME_END
    }

    enum class EnumTriggerType {
        TOGGLE,
        PRESS
    }

}