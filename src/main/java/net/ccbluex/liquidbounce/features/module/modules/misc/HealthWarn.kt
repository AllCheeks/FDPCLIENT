/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.ccbluex.liquidbounce.features.value.IntegerValue

object HealthWarn : Module(name = "HealthWarn", category = ModuleCategory.MISC, defaultOn = true) {

    private val healthValue = IntegerValue("Health", 7, 1, 20)

    private var canWarn = true

    override fun onEnable() {
        canWarn = true
    }

    override fun onDisable() {
        canWarn = true
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.health <= healthValue.get()) {
            if (canWarn) {
                FDPClient.hud.addNotification(
                    Notification("HP Warning", "YOU ARE AT LOW HP!", NotifyType.ERROR, 3000))
                canWarn = false
            }
        } else {
            canWarn = true
        }
    }
}
