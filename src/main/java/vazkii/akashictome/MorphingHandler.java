package vazkii.akashictome;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.*;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import vazkii.akashictome.network.MessageUnmorphTome;
import vazkii.arl.util.ItemNBTHelper;

public final class MorphingHandler {

	public static final MorphingHandler INSTANCE = new MorphingHandler();

	public static final String MINECRAFT = "minecraft";

	public static final String TAG_MORPHING = "akashictome:is_morphing";
	public static final String TAG_TOME_DATA = "akashictome:data";
	public static final String TAG_TOME_DISPLAY_NAME = "akashictome:displayName";
	public static final String TAG_ITEM_DEFINED_MOD = "akashictome:definedMod";

	@SubscribeEvent
	public void onPlayerLeftClick(PlayerInteractEvent.LeftClickEmpty event) {
		ItemStack stack = event.getItemStack();
		if(stack != null && isAkashicTome(stack) && stack.getItem() != ModItems.tome) {
			AkashicTome.sendToServer(new MessageUnmorphTome());
		}
	}

	@SubscribeEvent
	public void onItemDropped(ItemTossEvent event) {
		if(!event.getPlayer().isDiscrete())
			return;

		ItemEntity e = event.getEntityItem();
		ItemStack stack = e.getItem();
		if(!stack.isEmpty() && isAkashicTome(stack) && stack.getItem() != ModItems.tome) {
			CompoundNBT morphData = (CompoundNBT) stack.getTag().getCompound(TAG_TOME_DATA).copy();
			String currentMod = ItemNBTHelper.getString(stack, TAG_ITEM_DEFINED_MOD, getModFromStack(stack));

			ItemStack morph = makeMorphedStack(stack, MINECRAFT, morphData);
			CompoundNBT newMorphData = morph.getTag().getCompound(TAG_TOME_DATA);
			newMorphData.remove(currentMod);

			if(!e.getEntityWorld().isRemote) {
				ItemEntity newItem = new ItemEntity(e.getEntityWorld(), e.getPosX(), e.getPosY(), e.getPosZ(), morph);
				e.getEntityWorld().addEntity(newItem);
			}

			ItemStack copy = stack.copy();
			CompoundNBT copyCmp = copy.getTag();
			if(copyCmp == null) {
				copyCmp = new CompoundNBT();
				copy.setTag(copyCmp);
			}

			copyCmp.remove("display");
			ITextComponent displayName = null;
			CompoundNBT nameCmp = (CompoundNBT) copyCmp.get(TAG_TOME_DISPLAY_NAME);
			if (nameCmp != null)
				displayName = new StringTextComponent(nameCmp.getString("text"));
			if(displayName != null && !displayName.getString().isEmpty() && displayName != copy.getDisplayName())
				copy.setDisplayName(displayName);

			copyCmp.remove(TAG_MORPHING);
			copyCmp.remove(TAG_TOME_DISPLAY_NAME);
			copyCmp.remove(TAG_TOME_DATA);

			e.setItem(copy);
		}
	}

	public static String getModFromState(BlockState state) {
		return getModOrAlias(state.getBlock().getRegistryName().getNamespace());
	}

	public static String getModFromStack(ItemStack stack) {
		return getModOrAlias(stack.isEmpty() ? MINECRAFT : stack.getItem().getCreatorModId(stack));
	}

	public static String getModOrAlias(String mod) {
		Map<String, String> aliases = new HashMap<>();

		for(String s : ConfigHandler.aliasesList.get())
			if(s.matches(".+?=.+")) {
				String[] tokens = s.toLowerCase().split("=");
				aliases.put(tokens[0], tokens[1]);
			}

		return aliases.getOrDefault(mod, mod);
	}
	
	public static boolean doesStackHaveModAttached(ItemStack stack, String mod) {
		if(!stack.hasTag())
			return false;
		
		CompoundNBT morphData = stack.getTag().getCompound(TAG_TOME_DATA);
		return morphData.contains(mod);
	}

	public static ItemStack getShiftStackForMod(ItemStack stack, String mod) {
		if(!stack.hasTag())
			return stack;

		String currentMod = getModFromStack(stack);
		if(mod.equals(currentMod))
			return stack;

		CompoundNBT morphData = stack.getTag().getCompound(TAG_TOME_DATA);
		return makeMorphedStack(stack, mod, morphData);
	}

	public static ItemStack makeMorphedStack(ItemStack currentStack, String targetMod, CompoundNBT morphData) {
		String currentMod = ItemNBTHelper.getString(currentStack, TAG_ITEM_DEFINED_MOD, getModFromStack(currentStack));

		CompoundNBT currentCmp = new CompoundNBT();
		currentStack.write(currentCmp);
		currentCmp = currentCmp.copy();
		if(currentCmp.contains("tag"))
			currentCmp.getCompound("tag").remove(TAG_TOME_DATA);

		if(!currentMod.equalsIgnoreCase(MINECRAFT) && !currentMod.equalsIgnoreCase(AkashicTome.MOD_ID))
			morphData.put(currentMod, currentCmp);

		ItemStack stack;
		if(targetMod.equals(MINECRAFT))
			stack = new ItemStack(ModItems.tome);
		else {
			CompoundNBT targetCmp = morphData.getCompound(targetMod);
			morphData.remove(targetMod);

			stack = ItemStack.read(targetCmp);
			if(stack.isEmpty())
				stack = new ItemStack(ModItems.tome);
		}

		if(!stack.hasTag())
			stack.setTag(new CompoundNBT());

		CompoundNBT stackCmp = stack.getTag();
		stackCmp.put(TAG_TOME_DATA, morphData);
		stackCmp.putBoolean(TAG_MORPHING, true);

		if(stack.getItem() != ModItems.tome) {
			CompoundNBT displayName = new CompoundNBT();
			displayName.putString("text", stack.getDisplayName().getString());
			if(stackCmp.contains(TAG_TOME_DISPLAY_NAME))
				displayName = (CompoundNBT) stackCmp.get(TAG_TOME_DISPLAY_NAME);
			else stackCmp.put(TAG_TOME_DISPLAY_NAME, displayName);

			ITextComponent stackName = new StringTextComponent(displayName.getString("text")).setStyle(Style.EMPTY.createStyleFromFormattings(TextFormatting.GREEN));
			ITextComponent comp = new TranslationTextComponent("akashictome.sudo_name", stackName);
			stack.setDisplayName(comp);
		}

		stack.setCount(1);
		return stack;
	}

	private static final Map<String, String> modNames = new HashMap<String, String>();

	static {
		for(ModInfo modEntry : ModList.get().getMods())
			modNames.put(modEntry.getModId().toLowerCase(Locale.ENGLISH),  modEntry.getDisplayName());
	}

	public static String getModNameForId(String modId) {
		modId = modId.toLowerCase(Locale.ENGLISH);
		return modNames.containsKey(modId) ? modNames.get(modId) : modId;
	}

	public static boolean isAkashicTome(ItemStack stack) {
		if(stack.isEmpty())
			return false;

		if(stack.getItem() == ModItems.tome)
			return true;

		return stack.hasTag() && stack.getTag().getBoolean(TAG_MORPHING);
	}

}
