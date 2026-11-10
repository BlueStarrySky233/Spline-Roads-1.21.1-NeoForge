package com.sora.splineroads.world;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Mutable view of an {@link ItemStack}'s custom data for 1.21, where item NBT was replaced by
 * data components.
 *
 * <p>Before 1.21 the idiom was {@code stack.getOrCreateTag()}, which returned the live tag stored
 * on the stack, so in-place mutations such as {@code tag.putInt(..)} were persisted immediately.
 * {@code minecraft:custom_data} is a component, so there is no live tag to hand out. This subclass
 * keeps the same call shape by writing the whole tag back to the component after every mutator,
 * which makes {@code ItemTag.of(stack).putInt("k", 1)} behave like the old live tag.
 *
 * <p>Reads use a snapshot: every call re-reads the component, so a value written through any other
 * {@code ItemTag} instance is visible.
 */
public final class ItemTag extends CompoundTag {

  private final ItemStack stack;

  private ItemTag(ItemStack stack, CompoundTag source) {
    this.stack = stack;
    for (String key : source.getAllKeys()) {
      Tag value = source.get(key);
      if (value != null) super.put(key, value.copy());
    }
  }

  /** Snapshot of the stack's custom data; never null. */
  public static ItemTag of(ItemStack stack) {
    if (stack == null) return new ItemTag(null, new CompoundTag());
    CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
    return new ItemTag(stack, data.copyTag());
  }

  /** Copies this tag back onto the owning stack. */
  public void persist() {
    if (stack == null) return;
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(super.copy()));
  }

  @Override
  public Tag put(String key, Tag value) {
    Tag previous = super.put(key, value);
    persist();
    return previous;
  }

  @Override
  public void putByte(String key, byte value) {
    super.putByte(key, value);
    persist();
  }

  @Override
  public void putShort(String key, short value) {
    super.putShort(key, value);
    persist();
  }

  @Override
  public void putInt(String key, int value) {
    super.putInt(key, value);
    persist();
  }

  @Override
  public void putLong(String key, long value) {
    super.putLong(key, value);
    persist();
  }

  @Override
  public void putFloat(String key, float value) {
    super.putFloat(key, value);
    persist();
  }

  @Override
  public void putDouble(String key, double value) {
    super.putDouble(key, value);
    persist();
  }

  @Override
  public void putString(String key, String value) {
    super.putString(key, value);
    persist();
  }

  @Override
  public void putByteArray(String key, byte[] value) {
    super.putByteArray(key, value);
    persist();
  }

  @Override
  public void putIntArray(String key, int[] value) {
    super.putIntArray(key, value);
    persist();
  }

  @Override
  public void putIntArray(String key, List<Integer> value) {
    super.putIntArray(key, value);
    persist();
  }

  @Override
  public void putLongArray(String key, long[] value) {
    super.putLongArray(key, value);
    persist();
  }

  @Override
  public void putLongArray(String key, List<Long> value) {
    super.putLongArray(key, value);
    persist();
  }

  @Override
  public void putBoolean(String key, boolean value) {
    super.putBoolean(key, value);
    persist();
  }

  @Override
  public void putUUID(String key, UUID value) {
    super.putUUID(key, value);
    persist();
  }

  @Override
  public void remove(String key) {
    super.remove(key);
    persist();
  }

  @Override
  public CompoundTag merge(CompoundTag other) {
    CompoundTag merged = super.merge(other);
    persist();
    return merged;
  }
}
