package vazkii.akashictome;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import vazkii.arl.util.ItemNBTHelper;

public class AttachementRecipe extends SpecialRecipe {

	public AttachementRecipe(ResourceLocation idIn) {
		super(idIn);
	}

	@Override
	public boolean matches(CraftingInventory var1, World var2) {
		boolean foundTool = false;
		boolean foundTarget = false;

		for(int i = 0; i < var1.getSizeInventory(); i++) {
			ItemStack stack = var1.getStackInSlot(i);
			if(!stack.isEmpty()) {
				if(isTarget(stack)) {
					if(foundTarget)
						return false;
					foundTarget = true;
				} else if(stack.getItem() == ModItems.tome) {
					if(foundTool)
						return false;
					foundTool = true;
				} else return false;
			}
		}

		return foundTool && foundTarget;
	}

	@Override
	public ItemStack getCraftingResult(CraftingInventory var1) {
		ItemStack tool = ItemStack.EMPTY;
		ItemStack target = ItemStack.EMPTY;

		for(int i = 0; i < var1.getSizeInventory(); i++) {
			ItemStack stack = var1.getStackInSlot(i);
			if(!stack.isEmpty()) {
				if(stack.getItem() == ModItems.tome)
					tool = stack;
				else target = stack;
			}
		}

		ItemStack copy = tool.copy();
		CompoundNBT cmp = copy.getTag();
		if(cmp == null) {
			cmp = new CompoundNBT();
			copy.setTag(cmp);
		}

		if(!cmp.contains(MorphingHandler.TAG_TOME_DATA))
			cmp.put(MorphingHandler.TAG_TOME_DATA, new CompoundNBT());

		CompoundNBT morphData = cmp.getCompound(MorphingHandler.TAG_TOME_DATA);
		String mod = MorphingHandler.getModFromStack(target);
		String modClean = mod;
		int iter = 1;
		
		while(morphData.contains(mod)) {
			mod = modClean + iter;
			iter++;
		}

		ItemNBTHelper.setString(target, MorphingHandler.TAG_ITEM_DEFINED_MOD, mod);
		CompoundNBT modCmp = new CompoundNBT();
		target.write(modCmp);
		morphData.put(mod, modCmp);

		return copy;
	}

	@Override
	public boolean canFit(int width, int height) {
		return width * height >= 2;
	}

	public boolean isTarget(ItemStack stack) {
		if(stack.isEmpty() || MorphingHandler.isAkashicTome(stack))
			return false;

		String mod = MorphingHandler.getModFromStack(stack);
		if(mod.equals(MorphingHandler.MINECRAFT))
			return false;

		if(ConfigHandler.allItems.get())
			return true;

		if(ConfigHandler.blacklistedMods.get().contains(mod))
			return false;

		if(stack.getItem() instanceof IModdedBook)
			return true;

		ResourceLocation registryNameRL = stack.getItem().getRegistryName();
		String registryName = registryNameRL.toString();
		if(ConfigHandler.whitelistedItems.get().contains(registryName) || ConfigHandler.whitelistedItems.get().contains(registryName + ":" + stack.getDamage()))
			return true;

		String itemName = registryNameRL.getPath().toLowerCase();
		for(String s : ConfigHandler.whitelistedNames.get())
			if(itemName.contains(s.toLowerCase()))
				return true;

		return false;
	}

	@Override
	public ItemStack getRecipeOutput() {
		return ItemStack.EMPTY;
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
		return NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
	}

	@Override
	public IRecipeSerializer<?> getSerializer() {
		return RecipeSerializer.ATTACHMENT;
	}

}
