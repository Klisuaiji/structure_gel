package com.legacy.structure_gel.core.util;

import java.util.Locale;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class LoggerWrapper
{
	private final Logger logger;
	private final String prefix;
	private boolean enabled = true;

	public LoggerWrapper(String modID)
	{
		modID = modID.toLowerCase(Locale.ENGLISH);
		StringBuilder modProper = new StringBuilder();
		for (String word : modID.split("_"))
			modProper.append(word.substring(0, 1).toUpperCase(Locale.ENGLISH)).append(word.substring(1));

		this.logger = LogManager.getLogger("ModdingLegacy/" + modProper);
		this.prefix = "[" + modID + "] ";
	}

	public void enable()
	{
		this.enabled = true;
	}

	public void disable()
	{
		this.enabled = false;
	}

	private void log(Level level, Object message, Object... params)
	{
		if (this.enabled)
			this.logger.log(level, this.prefix + message, params);
	}

	public void log(Object message, Object... params)
	{
		this.info(message, params);
	}
	
	public void info(Object message, Object... params)
	{
		this.log(Level.INFO, message, params);
	}

	public void debug(Object message, Object... params)
	{
		this.log(Level.DEBUG, message, params);
	}

	public void error(Object message, Object... params)
	{
		this.log(Level.ERROR, message, params);
	}

	public void warn(Object message, Object... params)
	{
		this.log(Level.WARN, message, params);
	}

	public void fatal(Object message, Object... params)
	{
		this.log(Level.FATAL, message, params);
	}

	public Logger getLogger()
	{
		return this.logger;
	}
}