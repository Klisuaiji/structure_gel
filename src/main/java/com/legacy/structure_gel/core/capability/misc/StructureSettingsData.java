package com.legacy.structure_gel.core.capability.misc;

import com.legacy.structure_gel.api.structure.base.IModifyState;

public interface StructureSettingsData
{
	void structure_gel$setNatural(boolean natural);
	
	boolean structure_gel$isNatural();
	
	void structure_gel$setStateModifier(IModifyState modifier);
	
	IModifyState structure_gel$getStateModifier();
}
