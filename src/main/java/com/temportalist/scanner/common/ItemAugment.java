package com.temportalist.scanner.common;

import cofh.api.item.IAugmentItem;
import com.google.common.collect.Sets;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import java.util.List;
import java.util.Set;

/**
 * @author TheTemportalist
 */
public class ItemAugment extends Item implements IAugmentItem {

	private final String modid;
	private final String[] names;
	@SideOnly(Side.CLIENT)
	private IIcon[] icons;
	private final Set<String> defaultSet;

	public ItemAugment(String modid, String base, String... names) {
		this.modid = modid;
		this.names = names;
		this.defaultSet = Sets.newHashSet(this.names);
		this.setHasSubtypes(true);
		this.setUnlocalizedName(base);
		GameRegistry.registerItem(this, "augment-" + base);
	}

	public ItemAugment(String modid, String base) {
		this(modid, base, base);
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return super.getUnlocalizedName(stack) + "." + this.names[stack.getItemDamage()];
	}

	@Override
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		for (int i = 0; i < this.names.length; i++) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerIcons(IIconRegister reg) {
		this.icons = new IIcon[this.names.length];
		for (int i = 0; i < this.names.length; i++) {
			this.icons[i] = reg.registerIcon(this.modid + ":" + this.names[i]);
		}
	}

	@Override
	public IIcon getIconFromDamage(int damage) {
		return this.icons[damage];
	}

	@Override
	public int getAugmentLevel(ItemStack itemStack, String s) {
		return itemStack.getItemDamage();
	}

	@Override
	public Set<String> getAugmentTypes(ItemStack itemStack) {
		return this.defaultSet;
	}

}
