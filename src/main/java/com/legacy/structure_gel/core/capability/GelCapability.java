package com.legacy.structure_gel.core.capability;

import com.legacy.structure_gel.core.capability.entity.GelEntity;
import com.legacy.structure_gel.core.registry.SGRegistry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;

public class GelCapability {
   @Nullable
   public static <E extends Entity> GelEntity get(E entity) {
      return entity != null ? (GelEntity)entity.getData(SGRegistry.AttachmentTypes.GEL_ENTITY) : null;
   }

   public static <E extends Entity> void ifPresent(E entity, Consumer<GelEntity> action) {
      GelEntity cap = get(entity);
      if (cap != null) {
         action.accept(cap);
      }
   }

   public static <E extends Entity> void ifPresent(E entity, Consumer<GelEntity> action, Runnable elseAction) {
      GelEntity cap = get(entity);
      if (cap != null) {
         action.accept(cap);
      } else {
         elseAction.run();
      }
   }

   @Nullable
   public static <E extends Entity, R> R getIfPresent(E entity, Function<GelEntity, R> action, Supplier<R> elseAction) {
      GelEntity cap = get(entity);
      return cap != null ? action.apply(cap) : elseAction.get();
   }
}
