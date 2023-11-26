package net.illuc.kontraption.multiblocks.largeHydrogenThruster

import mekanism.api.AutomationType
import mekanism.api.chemical.attribute.ChemicalAttributeValidator
import mekanism.api.chemical.gas.Gas
import mekanism.common.capabilities.chemical.multiblock.MultiblockChemicalTankBuilder
import mekanism.common.capabilities.fluid.MultiblockFluidTank
import mekanism.common.lib.multiblock.IValveHandler
import mekanism.common.lib.multiblock.MultiblockData
import mekanism.common.recipe.lookup.ISingleRecipeLookupHandler.FluidRecipeLookupHandler
import mekanism.common.registries.MekanismGases
import net.illuc.kontraption.ThrusterInterface
import net.illuc.kontraption.blockEntities.TileEntityLiquidFuelThrusterCasing
import net.illuc.kontraption.config.KontraptionConfigs
import net.illuc.kontraption.particles.ThrusterParticleData
import net.illuc.kontraption.util.KontraptionVSUtils
import net.illuc.kontraption.util.toDoubles
import net.illuc.kontraption.util.toJOMLD
import net.illuc.kontraption.util.toMinecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.Ship
import java.util.function.BiPredicate
import java.util.function.Predicate


class LiquidFuelThrusterMultiblockData(tile: TileEntityLiquidFuelThrusterCasing) : MultiblockData(tile), ThrusterInterface, IValveHandler {

    // :cri:
    val te = tile
    var exhaustDirection: Direction = Direction.NORTH
    var centerExhaust: BlockEntity? = tile
    var exhaustDiameter = 0
    var offset: Vec3 = Vec3(0.0, 0.0, 0.0)
    var center: BlockPos = BlockPos(0, 0, 0)
    var innerVolume = 1

    var particleDir = exhaustDirection.normal.multiply(3+exhaustDiameter).toJOMLD()
    var pos = centerExhaust?.blockPos?.offset(exhaustDirection.normal.multiply(2))

    var ship: Ship? = null


    //----------------THRUSTER CONTROL-----------------------
    override var enabled = true
    override var thrusterLevel: Level? = centerExhaust?.level
    override var worldPosition: BlockPos? = center
    override var forceDirection: Direction = exhaustDirection.opposite
    override var powered: Boolean = true
    override var thrusterPower: Double = KontraptionConfigs.kontraption.liquidFuelThrust.get()
    override val basePower: Double = KontraptionConfigs.kontraption.liquidFuelThrust.get()

    //----------------stuff-----------------------

    init {
        fluidTanks.add(MultiblockFluidTank.create(10, tile))

    }

    override fun onCreated(world: Level?) {


        super.onCreated(world)
        //smh my balls
        ship = KontraptionVSUtils.getShipObjectManagingPos((thrusterLevel as ServerLevel), center)
                ?: KontraptionVSUtils.getShipManagingPos((thrusterLevel as ServerLevel), center)
        offset = Vector3d(1.0, 1.0, 1.0)
                .add(exhaustDirection.normal.toJOMLD().normalize().negate())
                .mul(0.25 * exhaustDiameter)
                .add(exhaustDirection.normal.toJOMLD()
                        .mul(1.5)).toMinecraft()
        pos = centerExhaust?.blockPos?.offset(exhaustDirection.normal.multiply(1))
        if (ship != null) {
            thrusterLevel = centerExhaust?.level
            worldPosition = center
            forceDirection = exhaustDirection.opposite
            thrusterPower = (24 * innerVolume).toDouble()

            enable()
        }
    }


    override fun tick(world: Level?): Boolean {
        if (powered){
            if (Dist.DEDICATED_SERVER.isDedicatedServer and (thrusterLevel != null)) {
                particleDir = if (ship == null){
                    exhaustDirection.normal.multiply(3+exhaustDiameter).toJOMLD()
                }else {
                    ship!!.transform.shipToWorld.transformDirection(exhaustDirection.normal.toJOMLD())
                }

                thrusterLevel as ServerLevel
                pos?.let { sendParticleData(thrusterLevel as ServerLevel, it.toDoubles(), particleDir) }
            }
        }
        return super.tick(world)
    }


    private fun sendParticleData(level: Level, pos: Vec3, particleDir: Vector3d) {
        if (!isRemote && level is ServerLevel) {

            for (player in level.players()) {
                level.sendParticles(player, ThrusterParticleData(particleDir.x.toDouble(), particleDir.y.toDouble(), particleDir.z.toDouble(), exhaustDiameter.toDouble()), true, pos.x+0.5, pos.y+0.5, pos.z+0.5, 2*exhaustDiameter, offset.x, offset.y, offset.z, 0.0)
            }
        }
    }

    fun getMaxFluid(): Int {
        return height() * 4 * 1
    }


}