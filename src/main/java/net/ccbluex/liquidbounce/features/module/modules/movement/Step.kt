/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.stats.StatList
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Step : Module(name = "Step", category = ModuleCategory.MOVEMENT, defaultOn = false) {

    /**
     * OPTIONS
     */

    val modeValue = ListValue("Mode", arrayOf("Vanilla", "Jump", "Matrix6.7.0",
                                                      "NCP", "NCPNew", "OldNCP",
                                                      "OldAAC", "AAC4.4.0",
                                                      "Spartan", "Rewinside", "Vulcan", "Verus", "BlocksMC"), "NCP")
    private val heightValue = FloatValue("Height", 1F, 0.6F, 10F)
    private val jumpHeightValue = FloatValue("JumpMotion", 0.42F, 0.37F, 0.42F).displayable { modeValue.equals("Jump") || modeValue.equals("TimerJump") }
    private val delayValue = IntegerValue("Delay", 0, 0, 500)
    private val timerValue = FloatValue("Timer", 1F, 0.05F, 1F).displayable { !modeValue.equals("Matrix6.7.0") && !modeValue.equals("Verus") }
    private val timerDynValue = BoolValue("UseDynamicTimer", false).displayable { !modeValue.equals("Matrix6.7.0") && !modeValue.equals("Verus")}

    /**
     * VALUES
     */

    private var isStep = false
    private var stepX = 0.0
    private var stepY = 0.0
    private var stepZ = 0.0

    private var ncpNextStep = 0
    private var spartanSwitch = false
    private var isAACStep = false
    private var wasTimer = false
    private var lastOnGround = false
    private var canStep = false

    private val timer = MSTimer()

    override fun onDisable() {
        mc.thePlayer ?: return

        // Change step height back to default (0.5 is default)
        mc.thePlayer.stepHeight = 0.6F
        mc.timer.timerSpeed = 1.0F
        wasTimer = false
        lastOnGround = mc.thePlayer.onGround
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (wasTimer) {
            mc.timer.timerSpeed = 1.0F
            wasTimer = false
        }
        if (mc.thePlayer.isCollidedHorizontally && mc.thePlayer.onGround && lastOnGround) {
            canStep = true
            if(modeValue.equals("AAC4.4.0") || modeValue.equals("NCPNew") || modeValue.equals("Matrix6.7.0")) {
                mc.thePlayer.stepHeight = heightValue.get()
            }
        }else {
            canStep = false
            mc.thePlayer.stepHeight = 0.6F
        }
        
        lastOnGround = mc.thePlayer.onGround
        
        val mode = modeValue.get()
        

        // Motion steps
        when {
            mode.equals("jump", true) && mc.thePlayer.isCollidedHorizontally && mc.thePlayer.onGround
                    && !mc.gameSettings.keyBindJump.isKeyDown -> {
                fakeJump()
                mc.thePlayer.motionY = jumpHeightValue.get().toDouble()
            }

        }
    }

    

    @EventTarget
    fun onStep(event: StepEvent) {
        mc.thePlayer ?: return
        val mode = modeValue.get()

        if (event.eventState == EventState.PRE) {
            if (mode.equals("AAC4.4.0", ignoreCase = true) || mode.equals("NCPNew", ignoreCase = true) || modeValue.equals("Matrix6.7.0")) {
                if (event.stepHeight > 0.6F && !canStep) return
                if (event.stepHeight <= 0.6F) return
            }

            // Set step to default in some cases
            if (!mc.thePlayer.onGround || !timer.hasTimePassed(delayValue.get().toLong()) ||
                mode.equals("Jump", ignoreCase = true) || mode.equals("MotionNCP", ignoreCase = true) ||
                mode.equals("LAAC", ignoreCase = true) || mode.equals("AAC3.3.4", ignoreCase = true) ||
                mode.equals("TimerJump", ignoreCase = true)) {
                mc.thePlayer.stepHeight = 0.6F
                event.stepHeight = 0.6F
                return
            }

            // Set step height
            val height = heightValue.get()

            // Detect possible step
            
            mc.thePlayer.stepHeight = height
            event.stepHeight = height
            
            if (event.stepHeight > 0.6F) {
                isStep = true
                stepX = mc.thePlayer.posX
                stepY = mc.thePlayer.posY
                stepZ = mc.thePlayer.posZ
            }
            
        } else {
            if (!isStep) { // Check if step
                return
            }

            if (mc.thePlayer.entityBoundingBox.minY - stepY > 0.6) { // Check if full block step
                if (timerValue.get()<1.0) {
                    wasTimer = true
                    mc.timer.timerSpeed = timerValue.get()
                    if (timerDynValue.get()) {
                        mc.timer.timerSpeed = (mc.timer.timerSpeed / sqrt(mc.thePlayer.entityBoundingBox.minY - stepY)).toFloat()
                    }
                }
                when {
                    mode.equals("NCP", ignoreCase = true) || mode.equals("OldAAC", ignoreCase = true) -> {
                        fakeJump()

                        // Half legit step (1 packet missing) [COULD TRIGGER TOO MANY PACKETS]
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                            stepY + 0.41999998688698, stepZ, false))
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                            stepY + 0.7531999805212, stepZ, false))
                        timer.reset()
                    }

                    mode.equals("AAC4.4.0", ignoreCase = true) -> {
                        val rstepHeight = mc.thePlayer.entityBoundingBox.minY - stepY
                        fakeJump()
                        timer.reset()
                        when {
                            rstepHeight >= 1.0 - 0.015625 && rstepHeight < 1.5 - 0.015625 -> {
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 0.4, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 0.7, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 0.9, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.0, stepZ, true))
                            }
                            rstepHeight >= 1.5 - 0.015625 && rstepHeight < 2.0 - 0.015625 -> {
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 0.42, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 0.7718, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.0556, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.2714, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.412, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.50, stepZ, true))
                            }
                            rstepHeight >= 2.0 - 0.015625 -> {
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 0.45, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 0.84375, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.18125, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.4625, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.6875, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.85625, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.96875, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX + mc.thePlayer.motionX * 0.5,
                                    stepY + 2.0000, stepZ + mc.thePlayer.motionZ * 0.5, true))
                            }
                        }
                        /*
                            AAC4 Step Code By Co丶Dynamic
                            NO SKIDDER PLEASE
                        */

                        /*
                            BRUH it's useful
                            don't remove it
                         */
                    }
                    
                    mode.equals("NCPNew", ignoreCase = true) -> {
                        val rstepHeight = mc.thePlayer.entityBoundingBox.minY - stepY
                        fakeJump()
                        when {
                            rstepHeight > 2.019 -> {
                                val stpPacket = arrayOf(0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.919)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                                mc.thePlayer.motionX = 0.0
                                mc.thePlayer.motionZ = 0.0
                            }
                            
                            rstepHeight <= 2.019 && rstepHeight > 1.869 -> {
                                val stpPacket = arrayOf(0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                                mc.thePlayer.motionX = 0.0
                                mc.thePlayer.motionZ = 0.0
                            }
                            
                            rstepHeight <= 1.869 && rstepHeight > 1.5 -> {
                                val stpPacket = arrayOf(0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                                mc.thePlayer.motionX = 0.0
                                mc.thePlayer.motionZ = 0.0
                            }
                            
                            rstepHeight <= 1.5 && rstepHeight > 1.015 -> {
                                val stpPacket = arrayOf(0.42, 0.7532, 1.01, 1.093, 1.015)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                            }
                            
                            rstepHeight <= 1.015 && rstepHeight > 0.875 -> {
                                val stpPacket = arrayOf(0.41999998688698, 0.7531999805212)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                            }
                            
                            rstepHeight <= 0.875 && rstepHeight > 0.6 -> {
                                val stpPacket = arrayOf(0.39, 0.6938)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                            }
                        }
                        timer.reset()
                    }
                    
                    mode.equals("Verus", ignoreCase = true) -> {
                        val rstepHeight = mc.thePlayer.entityBoundingBox.minY - stepY
                        mc.timer.timerSpeed = 1f / ceil(rstepHeight * 2.0).toFloat()
                        var stpHight = 0.0
                        fakeJump()
                        repeat ((ceil(rstepHeight * 2.0) - 1.0).toInt()) {
                            stpHight += 0.5
                            mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX, stepY + stpHight, stepZ, true))
                        } 
                        wasTimer = true
                    }
                    
                    mode.equals("Vulcan", ignoreCase = true) -> {
                        val rstepHeight = mc.thePlayer.entityBoundingBox.minY - stepY
                        fakeJump()
                        when {
                            rstepHeight > 2.0 -> {
                                val stpPacket = arrayOf(0.5, 1.0, 1.5, 2.0)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, true))
                                }
                            }
                            
                            rstepHeight <= 2.0 && rstepHeight > 1.5 -> {
                                val stpPacket = arrayOf(0.5, 1.0, 1.5)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, true))
                                }
                            }
                            
                            rstepHeight <= 1.5 && rstepHeight > 1.0 -> {
                                val stpPacket = arrayOf(0.5, 1.0)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, true))
                                }
                            }
                            
                            rstepHeight <= 1.0 && rstepHeight > 0.6 -> {
                                val stpPacket = arrayOf(0.5)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, true))
                                }
                            }
                        }
                        timer.reset()
                    }
                    
                    mode.equals("Matrix6.7.0", ignoreCase = true) -> {
                        val rstepHeight = mc.thePlayer.entityBoundingBox.minY - stepY
                        fakeJump()
                        when {
                            rstepHeight <= 3.0042 && rstepHeight > 2.95 -> {
                                val stpPacket = arrayOf(0.41951, 0.75223, 0.99990, 1.42989, 1.77289, 2.04032, 2.23371, 2.35453, 2.40423)
                                stpPacket.forEach {
                                    if(it in 0.9..1.01) {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, true))
                                    }else {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, false))
                                    }
                                }
                                mc.timer.timerSpeed = 0.11f
                                wasTimer = true
                            }
                            
                            rstepHeight <= 2.95 && rstepHeight > 2.83 -> {
                                val stpPacket = arrayOf(0.41951, 0.75223, 0.99990, 1.42989, 1.77289, 2.04032, 2.23371, 2.35453)
                                stpPacket.forEach {
                                    if(it in 0.9..1.01) {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, true))
                                    }else {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, false))
                                    }
                                }
                                mc.timer.timerSpeed = 0.12f
                                wasTimer = true
                            }
                            
                            rstepHeight <= 2.83 && rstepHeight > 2.64 -> {
                                val stpPacket = arrayOf(0.41951, 0.75223, 0.99990, 1.42989, 1.77289, 2.04032, 2.23371)
                                stpPacket.forEach {
                                    if(it in 0.9..1.01) {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, true))
                                    }else {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, false))
                                    }
                                }
                                mc.timer.timerSpeed = 0.13f
                                wasTimer = true
                            }
                            
                            rstepHeight <= 2.64 && rstepHeight > 2.37 -> {
                                val stpPacket = arrayOf(0.41951, 0.75223, 0.99990, 1.42989, 1.77289, 2.04032)
                                stpPacket.forEach {
                                    if(it in 0.9..1.01) {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, true))
                                    }else {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, false))
                                    }
                                }
                                mc.timer.timerSpeed = 0.14f
                                wasTimer = true
                            }
                            
                            rstepHeight <= 2.37 && rstepHeight > 2.02 -> {
                                val stpPacket = arrayOf(0.41951, 0.75223, 0.99990, 1.42989, 1.77289)
                                stpPacket.forEach {
                                    if(it in 0.9..1.01) {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, true))
                                    }else {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, false))
                                    }
                                }
                                mc.timer.timerSpeed = 0.16f
                                wasTimer = true
                            }
                            
                            rstepHeight <= 2.02 && rstepHeight > 1.77 -> {
                                val stpPacket = arrayOf(0.41951, 0.75223, 0.99990, 1.42989)
                                stpPacket.forEach {
                                    if(it in (0.9..1.01)) {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, true))
                                    }else {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, false))
                                    }
                                }
                                mc.timer.timerSpeed = 0.21f
                                wasTimer = true
                            }
                            
                            rstepHeight <= 1.77 && rstepHeight > 1.6 -> {
                                val stpPacket = arrayOf(0.41999998688698, 0.7531999805212, 1.17319996740818)
                                stpPacket.forEach {
                                    if(it in (0.753..0.754)) {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, true))
                                    }else {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, false))
                                    }
                                }
                                mc.timer.timerSpeed = 0.28f
                                wasTimer = true
                            }
                            
                            rstepHeight <= 1.6 && rstepHeight > 1.3525 -> {
                                val stpPacket = arrayOf(0.41999998688698, 0.7531999805212, 1.001335979112147)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                                mc.timer.timerSpeed = 0.28f
                                wasTimer = true
                            }
                            
                            rstepHeight <= 1.3525 && rstepHeight > 1.02 -> {
                                val stpPacket = arrayOf(0.41999998688698, 0.7531999805212)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                                mc.timer.timerSpeed = 0.34f
                                wasTimer = true
                            }
                            
                            rstepHeight <= 1.02 && rstepHeight > 0.6 -> {
                                val stpPacket = arrayOf(0.41999998688698)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                                mc.timer.timerSpeed = 0.5f
                                wasTimer = true
                            }
                        }
                        timer.reset()
                    }
                    
                    mode.equals("Spartan", ignoreCase = true) -> {
                        fakeJump()

                        if (spartanSwitch) {
                            // Vanilla step (3 packets) [COULD TRIGGER TOO MANY PACKETS]
                            mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                stepY + 0.41999998688698, stepZ, false))
                            mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                stepY + 0.7531999805212, stepZ, false))
                            mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                stepY + 1.001335979112147, stepZ, false))
                        } else { // Force step
                            mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                stepY + 0.6, stepZ, false))
                        }

                        // Spartan allows one unlegit step so just swap between legit and unlegit
                        spartanSwitch = !spartanSwitch

                        // Reset timer
                        timer.reset()
                    }

                    

                    mode.equals("Rewinside", ignoreCase = true) -> {
                        fakeJump()

                        // Vanilla step (3 packets) [COULD TRIGGER TOO MANY PACKETS]
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                            stepY + 0.41999998688698, stepZ, false))
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                            stepY + 0.7531999805212, stepZ, false))
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                            stepY + 1.001335979112147, stepZ, false))

                        // Reset timer
                        timer.reset()
                    }
                    mode.equals("BlocksMC", ignoreCase = true) -> {
                        fakeJump()
                        
                        val pos = mc.thePlayer.position.add(0.0, -1.5, 0.0)
                        
                        mc.netHandler.addToSendQueue(
                            C08PacketPlayerBlockPlacement(pos, 1,
                            ItemStack(Blocks.stone.getItem(mc.theWorld, pos)), 0.0F, 0.5F + Math.random().toFloat() * 0.44.toFloat(), 0.0F)
                            )

                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                            stepY + 0.41999998688698, stepZ, false))
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                            stepY + 0.7531999805212, stepZ, false))
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                            stepY + 1, stepZ, true))

                        // Reset timer
                        timer.reset()
                    }
                }
            }

            isStep = false
            stepX = 0.0
            stepY = 0.0
            stepZ = 0.0
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer && isStep && modeValue.equals("OldNCP")) {
            packet.y += 0.07
            isStep = false
        }
    }

    // There could be some anti cheats which tries to detect step by checking for achievements and stuff
    private fun fakeJump() {
        mc.thePlayer.isAirBorne = true
        mc.thePlayer.triggerAchievement(StatList.jumpStat)
    }

    private fun couldStep(): Boolean {
        val yaw = MovementUtils.direction
        val x = -sin(yaw) * 0.32
        val z = cos(yaw) * 0.32

        return mc.theWorld.getCollisionBoxes(mc.thePlayer.entityBoundingBox.offset(x, 1.001335979112147, z))
            .isEmpty()
    }

    override val tag: String
        get() = modeValue.get()
}
