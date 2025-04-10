/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.FloatValue

object ItemPhysics : Module(name = "ItemPhysics", category = ModuleCategory.RENDER, defaultOn = false) {
    val itemWeight = FloatValue("Weight", 0.5F, 0F, 1F)
    override val tag: String?
        get() = "${itemWeight.get()}"
}