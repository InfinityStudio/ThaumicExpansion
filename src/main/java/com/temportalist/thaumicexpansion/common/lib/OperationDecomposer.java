package com.temportalist.thaumicexpansion.common.lib;

import com.temportalist.thaumicexpansion.common.TEC;
import com.temportalist.thaumicexpansion.common.tile.TEThaumicAnalyzer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import java.util.Random;
import java.util.Set;

/**
 * @author TheTemportalist
 */
public class OperationDecomposer implements IOperation {

	private int maxTicks, energyCost, currentTicks = -1;
	private AspectList outputAspects;
	private boolean hasItemKeeper;

	public OperationDecomposer(ItemStack inputStack, int decomposerTier,
			Set<EnumAugmentTA> augments) {
		this.outputAspects = this.filterAspects(
				ThaumcraftApiHelper.getObjectAspects(inputStack), decomposerTier, augments
		);
		this.updateAugments(augments);
		Aspect[] aspects = this.outputAspects.getAspects();
		int energy = 0;
		int time = 0;
		for (Aspect aspect : aspects) {
			// todo wish Azanor had a Aspect.getTier method
			int tier = TEC.aspectTiers.containsKey(aspect) ?
					TEC.aspectTiers.get(aspect) :
					3;
			int[] stats = TEC.decompositionStats.get(tier);
			energy += stats[0];
			time += stats[1];
		}
		this.maxTicks = time;
		this.energyCost = energy;
	}

	private AspectList filterAspects(AspectList aspectList, int machineTier,
			Set<EnumAugmentTA> augments) {
		AspectList resultList = new AspectList();
		Random rand = new Random();
		Aspect[] aspects = aspectList.getAspectsSorted();
		double genericChance = this.getMachineChance(machineTier, augments);
		for (Aspect aspect : aspects) {
			int amount = aspectList.getAmount(aspect);
			int complexity = TEC.aspectTiers.containsKey(aspect) ?
					TEC.aspectTiers.get(aspect) :
					3;
			double complexityChance = this.getComplexityChance(machineTier, complexity, augments);
			int resultAmount = 0;
			for (int each = 1; each <= amount; each++) {
				if (rand.nextDouble() < complexityChance && rand.nextDouble() < genericChance) {
					resultAmount++;
				}
			}
			if (resultAmount > 0)
				resultList.add(aspect, resultAmount);
		}
		return resultList;
	}

	private double getComplexityChance(int machineTier, int aspectComplexity,
			Set<EnumAugmentTA> augments) {
		double base = TEC.complexityTierChance[machineTier][aspectComplexity - 1];
		for (EnumAugmentTA augment : augments) {
			base *= augment.getOutputMultipliers()[aspectComplexity];
		}
		return base;
	}

	private double getMachineChance(int machineTier, Set<EnumAugmentTA> augments) {
		double base = TEC.tieredChance[machineTier];
		for (EnumAugmentTA augment : augments) {
			base *= augment.getOutputMultipliers()[1];
		}
		return base;
	}

	@Override
	public int ticksForOperation() {
		return this.maxTicks;
	}

	@Override
	public boolean isRunning() {
		return this.currentTicks != -1;
	}

	@Override
	public void start() {
		this.currentTicks = 0;
	}

	@Override
	public void tick() {
		this.currentTicks += 1;
	}

	@Override
	public boolean canRun(TileEntity tileEntity, IOperator operator) {
		TEThaumicAnalyzer tile = (TEThaumicAnalyzer) tileEntity;
		return tile.getEnergyStorage().getEnergyStored() >= this.energyCost
				&& this.canOperateWithIO(operator);
	}

	private boolean canOperateWithIO(IOperator operator) {
		return operator.getOutput() == null || (
				operator.getInput().getItem() == operator.getOutput().getItem() &&
						operator.getInput().getItemDamage() == operator.getOutput().stackSize &&
						ItemStack.areItemStackTagsEqual(operator.getInput(), operator.getOutput())
						&& operator.getOutput().stackSize + operator.getInput().stackSize <=
						operator.getOutput().getMaxStackSize()
		);
	}

	@Override
	public double getProgress() {
		return (double) this.currentTicks / (double) this.maxTicks;
	}

	@Override
	public boolean areTicksReady() {
		return this.currentTicks >= this.maxTicks;
	}

	@Override
	public void reset() {
		this.currentTicks = -1;
	}

	@Override
	public void updateAugments(Set<EnumAugmentTA> augments) {
		this.hasItemKeeper = augments.contains(EnumAugmentTA.ITEM_KEEPER);
	}

	@Override
	public void run(TileEntity tileEntity, IOperator operator) {
		if (!this.canRun(tileEntity, operator))
			return;
		TEThaumicAnalyzer tile = (TEThaumicAnalyzer) tileEntity;
		tile.addAspects(this.outputAspects);

		ItemStack input = operator.getInput();
		ItemStack output = null;
		if (!this.hasItemKeeper
				|| tile.getWorldObj().rand.nextDouble() >= .5d) { // 50% chance to discard
			output = input.copy();
			output.stackSize = 1;
		}
		if (--input.stackSize <= 0)
			input = null;
		operator.finishedOperation(input, output);
	}

	@Override
	public Object getCosts() {
		return new int[] { this.energyCost };
	}

	@Override
	public void writeTo(NBTTagCompound tagCom, String key) {
		NBTTagCompound selfTag = new NBTTagCompound();

		selfTag.setInteger("maxTicks", this.maxTicks);
		selfTag.setInteger("energy", this.energyCost);
		selfTag.setInteger("ticks", this.currentTicks);

		tagCom.setTag(key, selfTag);
	}

	@Override
	public void readFrom(NBTTagCompound tagCom, String key) {
		NBTTagCompound selfTag = tagCom.getCompoundTag(key);

		this.maxTicks = selfTag.getInteger("maxTicks");
		this.energyCost = selfTag.getInteger("energy");
		this.currentTicks = selfTag.getInteger("ticks");

	}

}