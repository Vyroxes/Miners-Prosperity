package net.vyroxes.minersprosperity.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.vyroxes.minersprosperity.init.ModEnchantments;

public class MessageEnchantmentState implements IMessage
{
    private boolean value;
    private String state;
    private int enchantmentID;

    public MessageEnchantmentState()
    {
    }

    public MessageEnchantmentState(boolean value, String state, int enchantmentID)
    {
        this.value = value;
        this.state = state;
        this.enchantmentID = enchantmentID;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.value = buf.readBoolean();
        this.state = ByteBufUtils.readUTF8String(buf);
        this.enchantmentID = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeBoolean(this.value);
        ByteBufUtils.writeUTF8String(buf, this.state);
        buf.writeInt(this.enchantmentID);
    }

    public static class Handler implements IMessageHandler<MessageEnchantmentState, IMessage>
    {
        @Override
        public IMessage onMessage(MessageEnchantmentState message, MessageContext ctx)
        {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Enchantment enchantment = Enchantment.getEnchantmentByID(message.enchantmentID);
                if (enchantment == null || !(enchantment == ModEnchantments.VACUUM && "Vacuum".equals(message.state)
                        || enchantment == ModEnchantments.GLOW && "Glow".equals(message.state))) return;
                ItemStack held = player.getHeldItemMainhand();
                ItemStack stack = null;

                if (enchantment == ModEnchantments.VACUUM)
                {
                    stack = player.getItemStackFromSlot(EntityEquipmentSlot.FEET);
                }
                else if (enchantment == ModEnchantments.GLOW)
                {
                    stack = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
                }

                if (!held.isEmpty() && EnchantmentHelper.getEnchantmentLevel(enchantment, held) > 0)
                {
                    NBTTagCompound compound = held.getTagCompound();
                    compound.setBoolean(message.state, message.value);
                    held.setTagCompound(compound);
                }
                else if (!stack.isEmpty() && EnchantmentHelper.getEnchantmentLevel(enchantment, stack) > 0)
                {
                    NBTTagCompound compound = stack.getTagCompound();
                    compound.setBoolean(message.state, message.value);
                    stack.setTagCompound(compound);
                }

            });
            return null;
        }
    }
}