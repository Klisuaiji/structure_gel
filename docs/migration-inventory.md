# Structure Gel — 1.21.1 迁移待办清单（逐处）

由 1.20.4 源码扫描生成，格式为 `文件:行`。改完一处可自行勾掉。

## 类别：NBT/数据组件
com/legacy/structure_gel/api/data_handler/parsing/DataParser.java:155:         defaultClone.getOrCreateTag().putByte("Count", (byte)defaultClone.getCount());
com/legacy/structure_gel/api/item/StructureFinderItem.java:100:      CompoundTag tag = stack.getOrCreateTag();
com/legacy/structure_gel/api/item/StructureFinderItem.java:51:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/api/item/StructureFinderItem.java:63:      CompoundTag tag = stack.getOrCreateTag();
com/legacy/structure_gel/api/item/StructureFinderItem.java:73:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/api/item/StructureFinderItem.java:78:      CompoundTag tag = stack.getOrCreateTag();
com/legacy/structure_gel/api/item/StructureFinderItem.java:88:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/api/structure/StructureAccessHelper.java:89:         chunkGenStructureState, registryAccess.registryOrThrow(Registries.STRUCTURE).getOr
com/legacy/structure_gel/core/block/DataHandlerBlock.java:174:         CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/block/DynamicSpawnerBlock.java:95:         CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/block_entity/SGBlockEntity.java:35:      this.load(packet.getTag());
com/legacy/structure_gel/core/events/SGClientEvents.java:157:                  CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/events/SGCommonEvents.java:117:               CompoundTag tag = stack.getOrCreateTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:621:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:636:      CompoundTag tag = stack.getOrCreateTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:642:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:665:         CompoundTag tag = stack.getOrCreateTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:674:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:684:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:724:      stack.getOrCreateTag().put("pallete", paletteList);
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:825:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:832:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:845:      CompoundTag tag = stack.getOrCreateTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:852:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:858:      stack.getOrCreateTag().putInt("reach_distance", reachDistanceModifier);
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:863:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:868:      stack.getOrCreateTag().putBoolean("cause_block_updates", causeBlockUpdates);
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:873:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:886:      stack.getOrCreateTag().putInt("selected_corner", cornerType == null ? -1 : corn
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:890:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:896:      stack.getOrCreateTag().putFloat("selected_corner_distance", cornerDistance);
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:901:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:906:      stack.getOrCreateTag().putBoolean("move_bounds", moveBounds);
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:911:      CompoundTag tag = stack.getTag();
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:934:         CompoundTag tag = stack.getOrCreateTag();
com/legacy/structure_gel/core/network/c_to_s/UpdateDataHandlerPacket.java:181:                                 CompoundTag tag = cloneInvStack.getTag();

## 类别：Capability/LazyOptional
com/legacy/structure_gel/api/block/GelPortalBlock.java:141:      GelCapability.ifPresent(entity, gelEntity -> {
com/legacy/structure_gel/api/block/GelPortalBlock.java:5:import com.legacy.structure_gel.core.capability.GelCapability;
com/legacy/structure_gel/api/registry/registrar/Registrar.java:175:      protected final LazyOptional<T> valueLazy;
com/legacy/structure_gel/api/registry/registrar/Registrar.java:179:         this.valueLazy = LazyOptional.of(value);
com/legacy/structure_gel/api/registry/registrar/Registrar.java:3:import com.legacy.structure_gel.api.util.LazyOptional;
com/legacy/structure_gel/api/registry/StructureGelRegistries.java:19:   public static final Registry<JigsawCapabilityType<?>> JIGSAW_TYPE = new RegistryBuilder(
com/legacy/structure_gel/api/registry/StructureGelRegistries.java:27:      public static final ResourceKey<Registry<JigsawCapabilityType<?>>> JIGSAW_TYPE = crea
com/legacy/structure_gel/api/registry/StructureGelRegistries.java:5:import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapabilityType;
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:101:   public <T extends JigsawCapability> Optional<T> getCapability(Class<T> expectedClass)
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:102:      return this.getCapability().map(cap -> expectedClass.isInstance(cap) ? expectedCla
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:115:      Optional<JigsawCapability> capability = Optional.empty();
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:178:      public ExtendedJigsawStructure.Builder capability(JigsawCapability capability) {
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:186:               "JigsawCapabilityType not registered for " + this.capability.get() + ". B
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:212:      Optional<JigsawCapability> capability
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:41:               JigsawCapability.CODEC.optionalFieldOf("capability").forGetter(jigsaw -> j
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:47:   protected final Optional<JigsawCapability> capability;
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:5:import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapability;
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:72:      Optional<JigsawCapability> capability
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:97:   public Optional<JigsawCapability> getCapability() {
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:100:      return this.jigsawCapability;
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:103:   public <C extends JigsawCapability> Optional<C> getCapability(Class<C> ca
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:104:      return this.getCapability().map(c -> capabilityClass.isInstance(c) ? c
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:107:   protected <C extends JigsawCapability, T> Lazy<T> getCapabilityValue(Clas
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:108:      return Lazy.of(() -> this.getCapability(capabilityClass).map(getter).o
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:39:   private Optional<JigsawCapability> jigsawCapability = Optional.empty();
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:44:      this.jigsawCapability = context.jigsawCapability();
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:50:      this.loadCapability(context, nbt);
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:55:      this.saveCapability(context, nbt);
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:58:   private void loadCapability(StructurePieceSerializationContext context, Co
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:59:      if (nbt.contains(this.getCapabilityKey(), 10)) {
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:60:         Optional<Pair<JigsawCapability, Tag>> result = JigsawCapability.CODE
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:64:            JigsawCapability cap = (JigsawCapability)result.get().getFirst();
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:65:            this.attachCapability(Optional.ofNullable(cap));
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:70:   private void saveCapability(StructurePieceSerializationContext context, Co
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:71:      if (this.jigsawCapability.isPresent()) {
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:72:         Optional<Tag> result = JigsawCapability.CODEC
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:73:            .encodeStart(RegistryOps.create(NbtOps.INSTANCE, context.registry
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:77:            nbt.put(this.getCapabilityKey(), tag);
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:82:   protected String getCapabilityKey() {
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:94:   private final void attachCapability(@Nonnull Optional<JigsawCapability> ji
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:95:      Objects.requireNonNull(jigsawCapability);
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:96:      this.jigsawCapability = jigsawCapability;
com/legacy/structure_gel/api/structure/jigsaw/ExtendedJigsawStructurePiece.java:99:   public Optional<JigsawCapability> getCapability() {
com/legacy/structure_gel/api/structure/jigsaw/IPieceFactory.java:24:      Optional<JigsawCapability> jigsawCapability
com/legacy/structure_gel/api/structure/jigsaw/IPieceFactory.java:26:      public <T extends JigsawCapability> Optional<T> jigsawCapability(Class<T> expectedClas
com/legacy/structure_gel/api/structure/jigsaw/IPieceFactory.java:27:         return this.jigsawCapability().map(cap -> expectedClass.isInstance(cap) ? expectedC
com/legacy/structure_gel/api/structure/jigsaw/JigsawCapability.java:10:public interface JigsawCapability {
com/legacy/structure_gel/api/structure/jigsaw/JigsawCapability.java:11:   Codec<JigsawCapability> CODEC = StructureGelRegistries.JIGSAW_TYPE.byNameCodec().dispa
com/legacy/structure_gel/api/structure/jigsaw/JigsawCapability.java:13:   JigsawCapabilityType<?> getType();
com/legacy/structure_gel/api/structure/jigsaw/JigsawCapability.java:26:   public static final class Vanilla implements JigsawCapability {
com/legacy/structure_gel/api/structure/jigsaw/JigsawCapability.java:27:      public static final JigsawCapability.Vanilla INSTANCE = new JigsawCapability.Vanill
com/legacy/structure_gel/api/structure/jigsaw/JigsawCapability.java:28:      public static final Codec<JigsawCapability.Vanilla> CODEC = Codec.unit(INSTANCE);
com/legacy/structure_gel/api/structure/jigsaw/JigsawCapability.java:31:      public JigsawCapabilityType<?> getType() {
com/legacy/structure_gel/api/structure/jigsaw/JigsawCapability.java:37:         return "Vanilla Capability";
com/legacy/structure_gel/api/structure/jigsaw/JigsawCapabilityType.java:5:public interface JigsawCapabilityType<T extends JigsawCapability> {
com/legacy/structure_gel/api/util/LazyOptional.java:11:   public static <T> LazyOptional<T> of(Supplier<T> supplier) {
com/legacy/structure_gel/api/util/LazyOptional.java:12:      return new LazyOptional<>(supplier);
com/legacy/structure_gel/api/util/LazyOptional.java:15:   public static <T> LazyOptional<T> concurrent(Supplier<T> supplier) {
com/legacy/structure_gel/api/util/LazyOptional.java:16:      return new LazyOptional.Concurrent<>(supplier);
com/legacy/structure_gel/api/util/LazyOptional.java:19:   protected LazyOptional(Supplier<T> supplier) {
com/legacy/structure_gel/api/util/LazyOptional.java:48:   public static class Concurrent<T> extends LazyOptional<T> {
com/legacy/structure_gel/api/util/LazyOptional.java:7:public class LazyOptional<T> implements Lazy<T> {
com/legacy/structure_gel/core/asm_hooks/EntityHooks.java:14:      GelCapability.ifPresent(entity, gelEntity -> {
com/legacy/structure_gel/core/asm_hooks/EntityHooks.java:6:import com.legacy.structure_gel.core.capability.GelCapability;
com/legacy/structure_gel/core/asm_hooks/GuiHooks.java:16:         GelCapability.ifPresent(mc.player, gelEntity -> {
com/legacy/structure_gel/core/asm_hooks/GuiHooks.java:5:import com.legacy.structure_gel.core.capability.GelCapability;
com/legacy/structure_gel/core/capability/GelCapability.java:11:public class GelCapability {
com/legacy/structure_gel/core/capability/misc/GenerationContextData.java:15:   default Optional<JigsawCapability> structure_gel$getJigsawCapability() {
com/legacy/structure_gel/core/capability/misc/GenerationContextData.java:16:      return this.structure_gel$getStructure() instanceof ExtendedJigsawStructure ex
com/legacy/structure_gel/core/capability/misc/GenerationContextData.java:4:import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapability;
com/legacy/structure_gel/core/events/SGCommonEvents.java:67:            GelCapability.ifPresent(serverPlayer, gelPlayer -> gelPlayer.sendToClient(serverPlayer))
com/legacy/structure_gel/core/events/SGCommonEvents.java:6:import com.legacy.structure_gel.core.capability.GelCapability;
com/legacy/structure_gel/core/mixin/common/JigsawMixins.java:115:            Optional<JigsawCapability> capability = extendedPiece.getCapability();
com/legacy/structure_gel/core/mixin/common/JigsawMixins.java:41:      private static void addPieces_cancelOnCapabilityCheck(
com/legacy/structure_gel/core/mixin/common/JigsawMixins.java:53:         Optional<JigsawCapability> capability = ((GenerationContextData)context).structure_gel$
com/legacy/structure_gel/core/mixin/common/JigsawMixins.java:6:import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapability;
com/legacy/structure_gel/core/mixin/common/JigsawMixins.java:84:         Optional<JigsawCapability> capability = ((GenerationContextData)context).structure_gel$
com/legacy/structure_gel/core/network/s_to_c/UpdateGelPlayerPacket.java:4:import com.legacy.structure_gel.core.capability.GelCapability;
com/legacy/structure_gel/core/network/s_to_c/UpdateGelPlayerPacket.java:52:         GelCapability.ifPresent(mc.player, e -> e.copyFrom(packet.gelEntity));
com/legacy/structure_gel/core/registry/SGRegistry.java:16:import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapability;
com/legacy/structure_gel/core/registry/SGRegistry.java:17:import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapabilityType;
com/legacy/structure_gel/core/registry/SGRegistry.java:198:      public static final RegistrarHandler<JigsawCapabilityType<?>> HANDLER = RegistrarHandler.getOrC
com/legacy/structure_gel/core/registry/SGRegistry.java:201:      public static final Registrar.Static<JigsawCapabilityType<JigsawCapability.Vanilla>> VANILLA = 
com/legacy/structure_gel/core/registry/SGRegistry.java:202:         "vanilla", () -> () -> JigsawCapability.Vanilla.CODEC

## 类别：MobEffectInstance
com/legacy/structure_gel/core/commands/NightVisionCommand.java:13:import net.minecraft.world.effect.MobEffectInstance;
com/legacy/structure_gel/core/commands/NightVisionCommand.java:38:               if (player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, fals

## 类别：CreativeModeTab
com/legacy/structure_gel/core/events/SGCommonEvents.java:111:      protected static void onTabLoad(BuildCreativeModeTabContentsEvent event) {
com/legacy/structure_gel/core/events/SGCommonEvents.java:112:         if (event.getTabKey().equals(CreativeModeTabs.OP_BLOCKS) && event.hasPermissions()) {
com/legacy/structure_gel/core/events/SGCommonEvents.java:33:import net.minecraft.world.item.CreativeModeTabs;
com/legacy/structure_gel/core/events/SGCommonEvents.java:36:import net.minecraft.world.item.CreativeModeTab.TabVisibility;
com/legacy/structure_gel/core/events/SGCommonEvents.java:50:import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

## 类别：Level.random
com/legacy/structure_gel/api/block_entity/SpawnerAccessHelper.java:14:import net.minecraft.util.random.SimpleWeightedRandomList;
com/legacy/structure_gel/api/block_entity/SpawnerAccessHelper.java:15:import net.minecraft.util.random.SimpleWeightedRandomList.Builder;
com/legacy/structure_gel/api/block_entity/SpawnerAccessHelper.java:16:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/api/block_entity/SpawnerAccessHelper.java:72:                        NbtOps.INSTANCE, (SpawnData)((Wrapper)spawnDatas.getRandom(level !
com/legacy/structure_gel/api/data_handler/handlers/DataHandler.java:223:         this.random = rand;
com/legacy/structure_gel/api/data_handler/handlers/DataHandler.java:252:         return this.random;
com/legacy/structure_gel/api/registry/registrar/StructureRegistrar.java:33:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/block_entity/DataHandlerBlockEntity.java:23:import net.minecraft.util.random.Weight;
com/legacy/structure_gel/core/block_entity/DataHandlerBlockEntity.java:24:import net.minecraft.util.random.WeightedEntry;
com/legacy/structure_gel/core/block_entity/DataHandlerBlockEntity.java:25:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/client/gui/ClientPaletteTooltip.java:11:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/core/client/renderers/BuildingToolRenderers.java:162:                        RandomSource rand = mc.level.random;
com/legacy/structure_gel/core/client/screen/building_tool/BuildingToolOverlay.java:15:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/client/screen/building_tool/BuildingToolOverlay.java:16:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/core/client/screen/building_tool/BuildingToolScreen.java:52:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/client/screen/building_tool/BuildingToolScreen.java:53:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/core/client/screen/building_tool/EditPaletteScreen.java:41:import net.minecraft.util.random.SimpleWeightedRandomList;
com/legacy/structure_gel/core/client/screen/building_tool/EditPaletteScreen.java:42:import net.minecraft.util.random.SimpleWeightedRandomList.Builder;
com/legacy/structure_gel/core/client/screen/building_tool/EditPaletteScreen.java:43:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/core/client/screen/DataHandlerScreen.java:31:import net.minecraft.util.random.Weight;
com/legacy/structure_gel/core/commands/GetSpawnsCommand.java:19:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/item/BlockPaletteItem.java:10:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/core/item/BlockPaletteItem.java:9:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:51:import net.minecraft.util.random.SimpleWeightedRandomList;
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:52:import net.minecraft.util.random.WeightedRandom;
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:53:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:54:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:687:         net.minecraft.util.random.SimpleWeightedRandomList.Builder<BlockState> build
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:781:      net.minecraft.util.random.SimpleWeightedRandomList.Builder<BlockState> builder 
com/legacy/structure_gel/core/item/building_tool/BuildingToolMode.java:31:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/item/building_tool/BuildingToolMode.java:32:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/core/item/building_tool/modes/FillTool.java:19:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/item/building_tool/modes/FillTool.java:20:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/core/item/building_tool/modes/FillTool.java:45:            RandomSource rand = level.random;
com/legacy/structure_gel/core/item/building_tool/modes/FillTool.java:61:                     Optional<Wrapper<BlockState>> opState = palette.getRandom(level.ran
com/legacy/structure_gel/core/item/building_tool/modes/FloodTool.java:20:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/item/building_tool/modes/FloodTool.java:21:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/core/item/building_tool/modes/FloodTool.java:57:                  Optional<Wrapper<BlockState>> opState = palette.getRandom(level.rando
com/legacy/structure_gel/core/item/building_tool/modes/LineTool.java:21:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/item/building_tool/modes/LineTool.java:22:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/core/item/building_tool/modes/ReplaceTool.java:21:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/item/building_tool/modes/ReplaceTool.java:22:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/core/item/building_tool/modes/ReplaceTool.java:44:            RandomSource rand = level.random;
com/legacy/structure_gel/core/item/building_tool/modes/ShapeTool.java:18:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/item/building_tool/modes/ShapeTool.java:19:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/core/item/util/PaletteTooltip.java:3:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/item/util/PaletteTooltip.java:4:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/core/network/bi_directional/UpdateBuildingToolPacket.java:204:            net.minecraft.util.random.SimpleWeightedRandomList.Builder<Bl
com/legacy/structure_gel/core/network/bi_directional/UpdateBuildingToolPacket.java:22:import net.minecraft.util.random.SimpleWeightedRandomList;
com/legacy/structure_gel/core/network/bi_directional/UpdateBuildingToolPacket.java:23:import net.minecraft.util.random.WeightedRandomList;
com/legacy/structure_gel/core/network/bi_directional/UpdateBuildingToolPacket.java:24:import net.minecraft.util.random.WeightedEntry.Wrapper;
com/legacy/structure_gel/core/network/c_to_s/UpdateDataHandlerPacket.java:206:                              state, dataHandler.saveWithFullMetadata(), pos, leve
com/legacy/structure_gel/core/network/c_to_s/UpdateDataHandlerPacket.java:23:import net.minecraft.util.random.Weight;
com/legacy/structure_gel/core/network/c_to_s/UpdateDataHandlerPacket.java:24:import net.minecraft.util.random.WeightedRandomList;

## 类别：StructureType
com/legacy/structure_gel/api/registry/registrar/StructureRegistrar.java:112:   public static <S extends Structure> StructureRegistrar.Builder<S> builder(Resourc
com/legacy/structure_gel/api/registry/registrar/StructureRegistrar.java:117:      ResourceLocation registryName, Supplier<StructureType<S>> structureType
com/legacy/structure_gel/api/registry/registrar/StructureRegistrar.java:123:      return existingTypeBuilder(registryName, (Supplier<StructureType<S>>)SGRegistr
com/legacy/structure_gel/api/registry/registrar/StructureRegistrar.java:126:   public StructureType<S> getType() {
com/legacy/structure_gel/api/registry/registrar/StructureRegistrar.java:204:      private final Supplier<StructureType<S>> type;
com/legacy/structure_gel/api/registry/registrar/StructureRegistrar.java:210:      private Builder(ResourceLocation registryName, @Nullable Supplier<StructureTyp
com/legacy/structure_gel/api/registry/registrar/StructureRegistrar.java:42:import net.minecraft.world.level.levelgen.structure.StructureType;
com/legacy/structure_gel/api/registry/registrar/StructureRegistrar.java:52:   private final Supplier<StructureType<S>> structureType;
com/legacy/structure_gel/api/registry/registrar/StructureRegistrar.java:60:      RegistrarHandler<StructureType<?>> typeHandler = RegistrarHandler.getOrCreate(R
com/legacy/structure_gel/api/registry/registrar/StructureRegistrar.java:64:      this.structureType = (Supplier<StructureType<S>>)(!builder.existingType ? typeH
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:20:import net.minecraft.world.level.levelgen.structure.StructureType;
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:86:   public StructureType<?> type() {
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:87:      return SGRegistry.StructureTypes.EXTENDED_JIGSAW.get();
com/legacy/structure_gel/api/structure/StructureAccessHelper.java:28:import net.minecraft.world.level.levelgen.structure.StructureType;
com/legacy/structure_gel/api/structure/StructureAccessHelper.java:58:   public static boolean isInStructurePiece(ServerLevelAccessor levelAccessor, StructureTyp
com/legacy/structure_gel/api/structure/StructureAccessHelper.java:77:   public static boolean isInStructure(ServerLevelAccessor levelAccessor, StructureType<?> 
com/legacy/structure_gel/core/registry/SGRegistry.java:264:   public static class StructureTypes {
com/legacy/structure_gel/core/registry/SGRegistry.java:265:      public static final RegistrarHandler<StructureType<?>> HANDLER = RegistrarHandler.getOrCreate(R
com/legacy/structure_gel/core/registry/SGRegistry.java:266:      public static final Registrar.Static<StructureType<ExtendedJigsawStructure>> EXTENDED_JIGSAW = 
com/legacy/structure_gel/core/registry/SGRegistry.java:50:import net.minecraft.world.level.levelgen.structure.StructureType;

## 类别：BiomeModifier

## 类别：Registry 取用
com/legacy/structure_gel/api/block_entity/BlockEntityAccessHelper.java:25:      level.setBlock(pos, ((DynamicSpawnerBlock)SGRegistry.Blocks.DYNAMIC_SPAWNER.get(
com/legacy/structure_gel/api/block_entity/BlockEntityAccessHelper.java:36:      if (level.getBlockState(pos).is((Block)SGRegistry.Blocks.DYNAMIC_SPAWNER.get())
com/legacy/structure_gel/api/block_entity/SpawnerAccessHelper.java:89:      ResourceLocation name = BuiltInRegistries.ENTITY_TYPE.getKey(entity);
com/legacy/structure_gel/api/block_entity/SpawnerAccessHelper.java:8:import net.minecraft.core.registries.BuiltInRegistries;
com/legacy/structure_gel/api/block_entity/SpawnerAccessHelper.java:90:      nbt.putString("id", name != null ? BuiltInRegistries.ENTITY_TYPE.getKey(entity).toSt
com/legacy/structure_gel/api/block/StructureGelBlock.java:16:import net.minecraft.core.registries.BuiltInRegistries;
com/legacy/structure_gel/api/block/StructureGelBlock.java:58:      BuiltInRegistries.ITEM.stream().filter(item -> item instanceof StructureGelItem).forEach(set:
com/legacy/structure_gel/api/data_handler/parsing/DataParser.java:148:         ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(defaultClone.getIte
com/legacy/structure_gel/api/data_handler/parsing/DataParser.java:186:      Registry registry = (Registry)BuiltInRegistries.REGISTRY.get(registryKey);
com/legacy/structure_gel/api/data_handler/parsing/DataParser.java:264:         BlockState state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLooku
com/legacy/structure_gel/api/data_handler/parsing/DataParser.java:274:         ItemResult result = ItemParser.parseForItem(BuiltInRegistries.ITEM.asLookup(), ne
com/legacy/structure_gel/api/data_handler/parsing/DataParser.java:30:import net.minecraft.core.registries.BuiltInRegistries;
com/legacy/structure_gel/api/data/tags/FilterHolderSet.java:100:      return SGRegistry.HolderSetTypes.FILTER.get();
com/legacy/structure_gel/api/dynamic_spawner/DynamicSpawnerType.java:31:         BlockState state = level != null ? level.getBlockState(pos) : ((DynamicSpawnerB
com/legacy/structure_gel/api/dynamic_spawner/DynamicSpawnerType.java:32:         if (!state.is((Block)SGRegistry.Blocks.DYNAMIC_SPAWNER.get())) {
com/legacy/structure_gel/api/dynamic_spawner/DynamicSpawnerType.java:33:            state = ((DynamicSpawnerBlock)SGRegistry.Blocks.DYNAMIC_SPAWNER.get()).defau
com/legacy/structure_gel/api/registry/registrar/RegistrarHandler.java:17:import net.minecraft.core.registries.BuiltInRegistries;
com/legacy/structure_gel/api/registry/registrar/RegistrarHandler.java:221:      return BuiltInRegistries.REGISTRY.containsKey(this.registry.location());
com/legacy/structure_gel/api/registry/registrar/StructureRegistrar.java:123:      return existingTypeBuilder(registryName, (Supplier<StructureType<S>>)SGRegistr
com/legacy/structure_gel/api/registry/StructureGelRegistries.java:17:      .defaultKey(SGRegistry.DynamicSpawnerTypes.DEFAULT.getName())
com/legacy/structure_gel/api/registry/StructureGelRegistries.java:20:      .defaultKey(SGRegistry.JigsawTypes.VANILLA.getName())
com/legacy/structure_gel/api/structure/ExtendedJigsawStructure.java:87:      return SGRegistry.StructureTypes.EXTENDED_JIGSAW.get();
com/legacy/structure_gel/api/structure/GelTemplateStructurePiece.java:126:      DataHandlerBlock dataHandler = SGRegistry.Blocks.DATA_HANDLER.get();
com/legacy/structure_gel/api/structure/GridStructurePlacement.java:169:      return SGRegistry.StructurePlacementTypes.GRID_PLACEMENT.get();
com/legacy/structure_gel/api/structure/processor/RandomBlockSwapProcessor.java:20:            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("condition").forGett
com/legacy/structure_gel/api/structure/processor/RandomBlockSwapProcessor.java:58:      return SGRegistry.Processors.REPLACE_BLOCK.get();
com/legacy/structure_gel/api/structure/processor/RandomBlockSwapProcessor.java:8:import net.minecraft.core.registries.BuiltInRegistries;
com/legacy/structure_gel/api/structure/processor/RandomStateSwapProcessor.java:57:      return SGRegistry.Processors.REPLACE_BLOCK.get();
com/legacy/structure_gel/api/structure/processor/RandomTagSwapProcessor.java:59:      return SGRegistry.Processors.REPLACE_TAG.get();
com/legacy/structure_gel/api/structure/processor/RemoveGelStructureProcessor.java:34:      return SGRegistry.Processors.REMOVE_FILLER.get();
com/legacy/structure_gel/core/block/DynamicSpawnerBlock.java:64:         SGRegistry.BlockEntities.DYNAMIC_SPAWNER.get(),
com/legacy/structure_gel/core/block/DynamicSpawnerBlock.java:86:      return new ItemStack((ItemLike)SGRegistry.Blocks.DYNAMIC_SPAWNER.get());
com/legacy/structure_gel/core/block_entity/DataHandlerBlockEntity.java:43:      super(SGRegistry.BlockEntities.DATA_HANDLER.get(), pos, state);
com/legacy/structure_gel/core/block_entity/DynamicSpawnerBlockEntity.java:19:   private ResourceLocation spawnerID = SGRegistry.DynamicSpawnerTypes.DEFAULT.getN
com/legacy/structure_gel/core/block_entity/DynamicSpawnerBlockEntity.java:20:   private DynamicSpawner spawner = SGRegistry.DynamicSpawnerTypes.DEFAULT.get().cr
com/legacy/structure_gel/core/block_entity/DynamicSpawnerBlockEntity.java:24:      super(SGRegistry.BlockEntities.DYNAMIC_SPAWNER.get(), pos, state);
com/legacy/structure_gel/core/capability/entity/GelEntity.java:10:import net.minecraft.core.registries.BuiltInRegistries;
com/legacy/structure_gel/core/capability/entity/GelEntity.java:77:            name = BuiltInRegistries.BLOCK.getKey(gelEntity.portal);
com/legacy/structure_gel/core/capability/entity/GelEntity.java:88:               && BuiltInRegistries.BLOCK.containsKey(portal)
com/legacy/structure_gel/core/capability/entity/GelEntity.java:89:               && BuiltInRegistries.BLOCK.get(portal) instanceof GelPortalBlock gelPortalBlock
com/legacy/structure_gel/core/capability/GelCapability.java:14:      return entity != null ? (GelEntity)entity.getData(SGRegistry.AttachmentTypes.GEL_ENTITY) : 
com/legacy/structure_gel/core/client/ClientProxy.java:38:         SGRegistry.Items.BUILDING_TOOL.get().getDescriptionId()
com/legacy/structure_gel/core/client/ClientProxy.java:47:         SGRegistry.Items.BUILDING_TOOL.get().getDescriptionId()
com/legacy/structure_gel/core/client/ClientProxy.java:56:         SGRegistry.Items.BUILDING_TOOL.get().getDescriptionId()
com/legacy/structure_gel/core/client/ClientProxy.java:65:         SGRegistry.Items.BUILDING_TOOL.get().getDescriptionId()
com/legacy/structure_gel/core/client/ClientProxy.java:74:         SGRegistry.Items.BUILDING_TOOL.get().getDescriptionId()
com/legacy/structure_gel/core/client/ClientProxy.java:83:         SGRegistry.Items.BUILDING_TOOL.get().getDescriptionId()
com/legacy/structure_gel/core/client/renderers/BuildingToolRenderer.java:76:      BuildingToolMode newMode = stack != null && stack.is(SGRegistry.Items.BUILDING
com/legacy/structure_gel/core/client/screen/building_tool/BuildingToolScreen.java:212:                        SGRegistry.Items.BUILDING_TOOL.get().getDefaultIns
com/legacy/structure_gel/core/client/screen/building_tool/BuildingToolScreen.java:240:                        SGRegistry.Items.BUILDING_TOOL.get().getDefaultIns
com/legacy/structure_gel/core/client/screen/building_tool/BuildingToolScreen.java:46:import net.minecraft.core.registries.BuiltInRegistries;
com/legacy/structure_gel/core/client/screen/building_tool/BuildingToolScreen.java:573:               BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLo
com/legacy/structure_gel/core/client/screen/building_tool/BuildingToolScreen.java:665:      if (stack.is(SGRegistry.Items.BUILDING_TOOL.get())) {
com/legacy/structure_gel/core/client/screen/building_tool/EditPaletteScreen.java:129:            BlockResult result = BlockStateParser.parseForBlock(BuiltInRegi
com/legacy/structure_gel/core/client/screen/building_tool/EditPaletteScreen.java:37:import net.minecraft.core.registries.BuiltInRegistries;
com/legacy/structure_gel/core/client/screen/building_tool/EditPaletteScreen.java:485:            BlockState state = BlockStateParser.parseForBlock(BuiltInRegist
com/legacy/structure_gel/core/client/screen/building_tool/EditPaletteScreen.java:89:            ItemStack paletteItem = SGRegistry.Items.BLOCK_PALETTE.getDefaul
com/legacy/structure_gel/core/client/screen/building_tool/EditSelectionScreen.java:565:      if (tool.is(SGRegistry.Items.BUILDING_TOOL.get())) {
com/legacy/structure_gel/core/client/widget/DataParamsList.java:33:import net.minecraft.core.registries.BuiltInRegistries;
com/legacy/structure_gel/core/client/widget/DataParamsList.java:49:         map.put(BlockState.class, (mc, parser) -> DataParamsList.WidgetFactory.registryEditB
com/legacy/structure_gel/core/client/widget/DataParamsList.java:50:         map.put(ItemStack.class, (mc, parser) -> DataParamsList.WidgetFactory.registryEditBo
com/legacy/structure_gel/core/commands/GetSpawnsCommand.java:101:         ResourceLocation n1 = BuiltInRegistries.ENTITY_TYPE.getKey(o1.type);
com/legacy/structure_gel/core/commands/GetSpawnsCommand.java:102:         ResourceLocation n2 = BuiltInRegistries.ENTITY_TYPE.getKey(o2.type);
com/legacy/structure_gel/core/commands/GetSpawnsCommand.java:14:import net.minecraft.core.registries.BuiltInRegistries;
com/legacy/structure_gel/core/commands/GetSpawnsCommand.java:81:                           BuiltInRegistries.ENTITY_TYPE.getKey(spawn.type),
com/legacy/structure_gel/core/data/generators/SGTagProv.java:24:         this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add((Block)SGRegistry.Blocks.DYNAMIC_SPAWNER.
com/legacy/structure_gel/core/data/generators/SGTagProv.java:25:         this.tag(BlockTags.FEATURES_CANNOT_REPLACE).add((Block)SGRegistry.Blocks.DYNAMIC_SPAWNE
com/legacy/structure_gel/core/data/generators/SGTagProv.java:26:         this.tag(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE).add((Block)SGRegistry.Blocks.DYNAMIC
com/legacy/structure_gel/core/data/generators/SGTagProv.java:27:         this.tag(BlockTags.DRAGON_IMMUNE).add(SGRegistry.Blocks.DATA_HANDLER.get());
com/legacy/structure_gel/core/data/generators/SGTagProv.java:28:         this.tag(BlockTags.WITHER_IMMUNE).add(SGRegistry.Blocks.DATA_HANDLER.get());
com/legacy/structure_gel/core/data_handler/handlers/ArchaeologyHandler.java:19:      SGRegistry.DataHandlerTypes.CHEST,
com/legacy/structure_gel/core/data_handler/handlers/BlockStateHandler.java:19:   private static final DataParser PARSER = DataParser.of(SGRegistry.DataHandlerTy
com/legacy/structure_gel/core/data_handler/handlers/ChestHandler.java:24:   private static final DataParser PARSER = DataParser.of(SGRegistry.DataHandlerTypes.C
com/legacy/structure_gel/core/data_handler/handlers/ConfiguredFeatureHandler.java:17:   private static final DataParser PARSER = DataParser.of(SGRegistry.DataHa
com/legacy/structure_gel/core/data_handler/handlers/DynamicSpawnerHandler.java:19:   private static final DataParser PARSER = DataParser.of(SGRegistry.DataHandl
com/legacy/structure_gel/core/data_handler/handlers/DynamicSpawnerHandler.java:20:      parser.add("dynamic_spawner_type", StructureGelRegistries.Keys.DYNAMIC_S
com/legacy/structure_gel/core/data_handler/handlers/DynamicSpawnerHandler.java:42:      level.setBlock(pos, ((DynamicSpawnerBlock)SGRegistry.Blocks.DYNAMIC_SPAW
com/legacy/structure_gel/core/data_handler/handlers/EmptyDataHandler.java:9:   private static final DataParser PARSER = DataParser.of(SGRegistry.DataHandlerType
com/legacy/structure_gel/core/data_handler/handlers/EntityHandler.java:37:   private static final DataParser PARSER = DataParser.of(SGRegistry.DataHandlerTypes.
com/legacy/structure_gel/core/data_handler/handlers/VanillaSpawnerHandler.java:22:   private static final DataParser PARSER = DataParser.of(SGRegistry.DataHandl
com/legacy/structure_gel/core/events/SGClientEvents.java:121:         if (stack.is(SGRegistry.Items.BUILDING_TOOL.get())) {
com/legacy/structure_gel/core/events/SGClientEvents.java:168:                  SGRegistry.Items.BUILDING_TOOL.get(),
com/legacy/structure_gel/core/events/SGClientEvents.java:186:         event.registerBlockEntityRenderer(SGRegistry.BlockEntities.DATA_HANDLER.get(), DataHandler
com/legacy/structure_gel/core/events/SGClientEvents.java:187:         event.registerBlockEntityRenderer(SGRegistry.BlockEntities.DYNAMIC_SPAWNER.get(), DynamicS
com/legacy/structure_gel/core/events/SGCommonEvents.java:125:            event.accept(SGRegistry.Blocks.RED_GEL);
com/legacy/structure_gel/core/events/SGCommonEvents.java:126:            event.accept(SGRegistry.Blocks.BLUE_GEL);
com/legacy/structure_gel/core/events/SGCommonEvents.java:127:            event.accept(SGRegistry.Blocks.GREEN_GEL);
com/legacy/structure_gel/core/events/SGCommonEvents.java:128:            event.accept(SGRegistry.Blocks.CYAN_GEL);
com/legacy/structure_gel/core/events/SGCommonEvents.java:129:            event.accept(SGRegistry.Blocks.ORANGE_GEL);
com/legacy/structure_gel/core/events/SGCommonEvents.java:130:            event.accept(SGRegistry.Blocks.YELLOW_GEL);
com/legacy/structure_gel/core/events/SGCommonEvents.java:132:            event.accept(SGRegistry.Blocks.DATA_HANDLER);
com/legacy/structure_gel/core/events/SGCommonEvents.java:133:            event.accept(SGRegistry.Blocks.DYNAMIC_SPAWNER);
com/legacy/structure_gel/core/events/SGCommonEvents.java:134:            event.accept(SGRegistry.Items.BUILDING_TOOL);
com/legacy/structure_gel/core/events/SGCommonEvents.java:138:                  ItemStack stack = SGRegistry.Items.BUILDING_TOOL.getDefaultInstance();
com/legacy/structure_gel/core/events/SGCommonEvents.java:78:         if (event.getItemStack().is(SGRegistry.Items.BUILDING_TOOL.get())) {
com/legacy/structure_gel/core/item/building_tool/ActionHistory.java:482:         ItemStack buildingTool = mainItem.is(SGRegistry.Items.BUILDING_TOOL.get())
com/legacy/structure_gel/core/item/building_tool/ActionHistory.java:484:            : (offItem.is(SGRegistry.Items.BUILDING_TOOL.get()) ? offItem : null);
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:163:      if (stack.is(SGRegistry.Items.BUILDING_TOOL.get())) {
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:167:         return stack.is(SGRegistry.Items.BUILDING_TOOL.get()) ? Pair.of(InteractionH
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:41:import net.minecraft.core.registries.BuiltInRegistries;
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:512:      return SGRegistry.Items.BUILDING_TOOL.get().hasPermission(stack, player);
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:693:                  BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:748:      if (readFrom.is(SGRegistry.Items.BLOCK_PALETTE.get())) {
com/legacy/structure_gel/core/mixin/client/ItemMixin.java:33:            if (item == Items.STRUCTURE_BLOCK || item == Items.JIGSAW || item == SGRegistry.Blocks.
com/legacy/structure_gel/core/mixin/client/ItemMixin.java:53:                  } else if (item == SGRegistry.Blocks.DATA_HANDLER.get().asItem() && blockEntityTa
com/legacy/structure_gel/core/mixin/common/BlockMixin.java:20:      if (state.is(SGRegistry.Blocks.DATA_HANDLER.get()) && !(Boolean)state.getValue(DataHandlerBl
com/legacy/structure_gel/core/network/bi_directional/UpdateBuildingToolPacket.java:18:import net.minecraft.core.registries.BuiltInRegistries;
com/legacy/structure_gel/core/network/bi_directional/UpdateBuildingToolPacket.java:208:               BlockState state = NbtUtils.readBlockState(BuiltInRegistri
com/legacy/structure_gel/core/network/bi_directional/UpdateBuildingToolPacket.java:242:         if (stack.is(SGRegistry.Items.BUILDING_TOOL.get())) {
com/legacy/structure_gel/core/network/c_to_s/ExportPalettePacket.java:42:               if (BuildingToolItem.hasToolPermission(SGRegistry.Items.BUILDING_TOOL.ge
com/legacy/structure_gel/core/network/c_to_s/LeftClickBuildingToolPacket.java:54:                  if (stack.is(SGRegistry.Items.BUILDING_TOOL.get())) {
com/legacy/structure_gel/core/network/c_to_s/LeftClickBuildingToolPacket.java:55:                     SGRegistry.Items.BUILDING_TOOL.get().onLeftClick(stack, se
com/legacy/structure_gel/core/network/c_to_s/MiddleClickBuildingToolPacket.java:47:               if (stack.is(SGRegistry.Items.BUILDING_TOOL.get())) {
com/legacy/structure_gel/core/network/c_to_s/MiddleClickBuildingToolPacket.java:48:                  SGRegistry.Items.BUILDING_TOOL.get().onMiddleClick(stack, s
com/legacy/structure_gel/core/network/c_to_s/RequestRegistryKeysPacket.java:44:               if (opRegistry.isPresent()) {
com/legacy/structure_gel/core/network/c_to_s/RequestRegistryKeysPacket.java:45:                  Registry<?> registry = opRegistry.get();
com/legacy/structure_gel/core/network/c_to_s/RightClickBuildingToolPacket.java:54:                  if (stack.is(SGRegistry.Items.BUILDING_TOOL.get())) {
com/legacy/structure_gel/core/network/c_to_s/RightClickBuildingToolPacket.java:55:                     SGRegistry.Items.BUILDING_TOOL.get().onRightClick(stack, 
com/legacy/structure_gel/core/network/c_to_s/UpdateDataHandlerPacket.java:159:                     if (state.is(SGRegistry.Blocks.DATA_HANDLER.get()) && blockEn
com/legacy/structure_gel/core/network/c_to_s/UpdateDataHandlerPacket.java:174:                           ItemStack stack = SGRegistry.Blocks.DATA_HANDLER.get().
com/legacy/structure_gel/core/network/c_to_s/UpdateDataHandlerPacket.java:179:                              if (!invStack.isEmpty() && invStack.is(SGRegistry.Bl
com/legacy/structure_gel/core/network/c_to_s/UpdateDynamicSpawnerPacket.java:74:               if (state.getBlock() == SGRegistry.Blocks.DYNAMIC_SPAWNER.get() &
com/legacy/structure_gel/core/registry/SGRegistry.java:74:         "data_handler", () -> Builder.of(DataHandlerBlockEntity::new, new Block[]{SGRegistry.Blocks.D
com/legacy/structure_gel/core/registry/SGRegistry.java:77:         "dynamic_spawner", () -> Builder.of(DynamicSpawnerBlockEntity::new, new Block[]{(Block)SGRegi
com/legacy/structure_gel/core/structure/jigsaw/GelSinglePoolElement.java:172:      DataHandlerBlock dataHandler = SGRegistry.Blocks.DATA_HANDLER.get();
com/legacy/structure_gel/core/structure/jigsaw/GelSinglePoolElement.java:200:      return SGRegistry.JigsawDeserializers.GEL_SINGLE_POOL_ELEMENT.get();
com/legacy/structure_gel/core/structure/jigsaw/VanillaJigsawStructurePiece.java:27:      return SGRegistry.StructurePieceTypes.VANILLA_JIGSAW.get();

## 类别：ItemStack.is
com/legacy/structure_gel/core/item/building_tool/BuildingToolItem.java:802:      if (stack.is(Items.AIR)) {
com/legacy/structure_gel/core/network/c_to_s/UpdateDataHandlerPacket.java:186:                                 if (ItemStack.isSameItemSameTags(cloneInvStack, s

