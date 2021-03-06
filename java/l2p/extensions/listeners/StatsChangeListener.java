package l2p.extensions.listeners;

import l2p.gameserver.skills.Calculator;
import l2p.gameserver.skills.Env;
import l2p.gameserver.skills.Stats;

public abstract class StatsChangeListener
{
	public final Stats _stat;
	protected Calculator _calculator;

	public StatsChangeListener(Stats stat)
	{
		_stat = stat;
	}

	public void setCalculator(Calculator calculator)
	{
		_calculator = calculator;
	}

	public abstract void statChanged(Double oldValue, double newValue, double baseValue, Env env);
}