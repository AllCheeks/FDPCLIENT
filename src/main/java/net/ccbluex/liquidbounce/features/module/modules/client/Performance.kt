/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue

object Performance : Module("Performance", category = ModuleCategory.CLIENT, defaultOn = true) {
    @JvmField
    var staticParticleColorValue = BoolValue("StaticParticleColor", false)
    @JvmField
    var fastEntityLightningValue = BoolValue("FastEntityLightning", false)
    @JvmField
    var fastBlockLightningValue = BoolValue("FastBlockLightning", false)
}

