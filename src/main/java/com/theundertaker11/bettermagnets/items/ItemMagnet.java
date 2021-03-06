package com.theundertaker11.bettermagnets.items;

import java.util.Iterator;
import java.util.List;

import com.theundertaker11.bettermagnets.BetterMagnetsMain;
import com.theundertaker11.bettermagnets.Reference;
import com.theundertaker11.bettermagnets.init.BlockRegistry;
import com.theundertaker11.bettermagnets.init.ConfigMain;
import com.theundertaker11.bettermagnets.util.IItemModelProvider;
import com.theundertaker11.bettermagnets.util.ModUtils;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Optional.Interface(iface="baubles.api.IBauble", modid="Baubles", striprefs=true)
public class ItemMagnet extends Item implements IItemModelProvider, IBauble{
	
	protected String name;
	
	public ItemMagnet(String name){
		super();
		setRegistryName(name);
		this.name= name;
		setUnlocalizedName(name);
		setCreativeTab(BetterMagnetsMain.BMtab);
		setMaxDamage(0);
		setMaxStackSize(1);
		setHasSubtypes(true);
	}
	
	@Override
	public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced)
    {
		if(ModUtils.isMagnetActive(stack))
		{
			tooltip.add("Magnet Enabled");
		}
		else{
			tooltip.add("Magnet Disabled");
		}
    }
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack itemStackIn, World world, EntityPlayer player, EnumHand hand)
    {
		ItemStack stack = player.getHeldItemMainhand();
		/*for(int i=0;i<player.inventory.getSizeInventory();i++)
		{
			ItemStack invStack = player.inventory.getStackInSlot(i);
			if(!ModUtils.isStackEmpty(invStack)&&stack!=invStack)
			{
				ModUtils.disableMagnet(invStack);
			}
		}*/
		if(player.isSneaking())
		{
			ModUtils.toggleMagnet(stack);
			return new ActionResult(EnumActionResult.SUCCESS, itemStackIn);
		}
		return new ActionResult(EnumActionResult.PASS, itemStackIn);
    }
	@Override
	public void registerItemModel(Item item)
	{
		for(int i=0;i<=3;i++)
		{
			BetterMagnetsMain.proxy.registerItemRenderer(this, i, name);
		}
		BetterMagnetsMain.proxy.registerItemRenderer(this, 4, "magnet4");
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean hasEffect(ItemStack stack)
	{
		return ModUtils.isMagnetActive(stack);
	}
	
	public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected)
    {
		if(world.isRemote) return;
		if(entity instanceof EntityPlayer && ModUtils.isMagnetActive(stack))
		{
			doMagnet(stack, (EntityPlayer)entity, world);
		}
		
    }
	
	@Optional.Method(modid="Baubles")
	@Override
	public void onWornTick(ItemStack stack, EntityLivingBase player)
	{
		if(player instanceof EntityPlayer && ModUtils.isMagnetActive(stack))
		{
			doMagnet(stack, (EntityPlayer)player, player.getEntityWorld());
		}
	}
	
	public static void doMagnet(ItemStack stack, EntityPlayer player, World world)
	{
		if(world.isRemote) return;
		double range = ConfigMain.getRange(stack);
		
		Iterator iterator = ModUtils.getEntitiesInRange(EntityItem.class, world, player.posX, player.posY,
				player.posZ, range).iterator();
		while (iterator.hasNext()) {
			EntityItem itemToGet = (EntityItem) iterator.next();
			
			if(!itemToGet.getTags().contains(Reference.NO_PICKUP)&&shouldPickupItem(world, itemToGet.getPosition()))
			{
				EntityItemPickupEvent pickupEvent = new EntityItemPickupEvent(player, itemToGet);
				MinecraftForge.EVENT_BUS.post(pickupEvent);
				ItemStack itemStackToGet = itemToGet.getEntityItem();
				int stackSize = itemStackToGet.stackSize;

				if (pickupEvent.getResult() == Result.ALLOW || stackSize <= 0 || player.inventory.addItemStackToInventory(itemStackToGet))
				{
					player.onItemPickup(itemToGet, stackSize);
					world.playSound(player, player.getPosition(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.AMBIENT,
						0.15F, ((world.rand.nextFloat() - world.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
				}
			}else if(!itemToGet.getTags().contains(Reference.NO_PICKUP)) itemToGet.addTag(Reference.NO_PICKUP);
		}
		
		//XP
		Iterator xpiterator = ModUtils.getEntitiesInRange(EntityXPOrb.class, world, player.posX, player.posY, player.posZ,
				6.5).iterator();
		while (xpiterator.hasNext()) {
			EntityXPOrb xpToGet = (EntityXPOrb) xpiterator.next();

			if (xpToGet.isDead || xpToGet.isInvisible()) {
				continue;
			}
			player.xpCooldown = 0;
			xpToGet.delayBeforeCanPickup=0;
			xpToGet.setPosition(player.posX,player.posY,player.posZ);
			PlayerPickupXpEvent xpEvent = new PlayerPickupXpEvent(player, xpToGet);
			MinecraftForge.EVENT_BUS.post(xpEvent);
			if(xpEvent.getResult()==Result.ALLOW){
				xpToGet.onCollideWithPlayer(player);
			}
			
		}
	}
	
	public static boolean shouldPickupItem(World world, BlockPos pos)
	{
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		for(int i=-5;i<=5;i++)
		{
			BlockPos newpos = new BlockPos(x,y+i,z);
			if(world.getBlockState(newpos).getBlock() == BlockRegistry.anti_magnet_block)
			{
				return false;
			}
		}
		return true;
	}
	
	@Optional.Method(modid="Baubles")
	@Override
	public BaubleType getBaubleType(ItemStack itemstack)
	{
		return BaubleType.CHARM;
	}
	
	@Override
	public String getUnlocalizedName()
    {
        return "item." + this.name + "0";
    }
	
	@Override
	public String getUnlocalizedName(ItemStack stack)
    {
        return "item." + this.name + stack.getItemDamage();
    }
	
	@Override
	@SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems)
    {
        subItems.add(new ItemStack(item));
        for(int i=1;i<=4;i++)
        {
        	subItems.add(new ItemStack(item, 1, i));
        }
    }
	
	
	/* For 1.11.2
	 //Items
		Iterator iterator = ModUtils.getEntitiesInRange(EntityItem.class, world, player.posX, player.posY,
				player.posZ, range).iterator();
		while (iterator.hasNext()) {
			EntityItem itemToGet = (EntityItem) iterator.next();
			
			if(!itemToGet.getTags().contains(Reference.NO_PICKUP)&&shouldPickupItem(world, itemToGet.getPosition()))
			{
				EntityItemPickupEvent pickupEvent = new EntityItemPickupEvent(player, itemToGet);
				MinecraftForge.EVENT_BUS.post(pickupEvent);
				ItemStack itemStackToGet = itemToGet.getEntityItem();
				int stackSize = itemStackToGet.stackSize;

				if (pickupEvent.getResult() == Result.ALLOW || stackSize <= 0 || !player.inventory.addItemStackToInventory(itemStackToGet))
				{
					player.onItemPickup(itemToGet, stackSize);
					world.playSound(player, player.getPosition(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.AMBIENT,
						0.15F, ((world.rand.nextFloat() - world.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
				}
			}else if(!itemToGet.getTags().contains(Reference.NO_PICKUP)) itemToGet.addTag(Reference.NO_PICKUP);
		}
		//XP
		Iterator xpiterator = ModUtils.getEntitiesInRange(EntityXPOrb.class, world, player.posX, player.posY, player.posZ,
				range).iterator();
		while (xpiterator.hasNext()) {
			EntityXPOrb xpToGet = (EntityXPOrb) xpiterator.next();

			if (xpToGet.isDead || xpToGet.isInvisible()) {
				continue;
			}
			player.xpCooldown = 0;
			xpToGet.delayBeforeCanPickup=0;
			xpToGet.setPosition(player.posX,player.posY,player.posZ);
			PlayerPickupXpEvent xpEvent = new PlayerPickupXpEvent(player, xpToGet);
			MinecraftForge.EVENT_BUS.post(xpEvent);
			if(xpEvent.getResult()==Result.ALLOW){
				xpToGet.onCollideWithPlayer(player);
			}
			
		}
		*/
	 
}
