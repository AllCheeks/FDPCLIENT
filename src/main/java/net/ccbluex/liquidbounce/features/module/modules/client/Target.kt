package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue

object Target : Module(name = "Target", category = ModuleCategory.CLIENT, canEnable = false, defaultOn = false) {
    val playerValue = BoolValue("Player", true)
    val animalValue = BoolValue("Animal", false)
    val mobValue = BoolValue("Mob", true)
    val invisibleValue = BoolValue("Invisible", false)
    val deadValue = BoolValue("Dead", false)

    // always handle event
    override fun handleEvents() = true
}