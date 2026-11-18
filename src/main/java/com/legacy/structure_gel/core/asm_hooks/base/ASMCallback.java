package com.legacy.structure_gel.core.asm_hooks.base;

import javax.annotation.Nullable;

public class ASMCallback<T> {
   private T returnVal = (T)null;

   public ASMCallback(T returnVal) {
      this.returnVal = returnVal;
   }

   public ASMCallback() {
   }

   public boolean shouldReturn() {
      return this.returnVal != null;
   }

   @Nullable
   public T getReturnVal() {
      return this.returnVal;
   }

   public void setReturnVal(T returnVal) {
      this.returnVal = returnVal;
   }
}
