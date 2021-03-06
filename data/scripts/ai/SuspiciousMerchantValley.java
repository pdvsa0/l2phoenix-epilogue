//
// Suspicious Merchant - Valley Fortress (35759).
//
package ai;

import l2p.gameserver.ai.DefaultAI;
import l2p.gameserver.model.L2Character;
import l2p.gameserver.model.instances.L2NpcInstance;
import l2p.util.Location;
import l2p.util.Rnd;

public class SuspiciousMerchantValley extends DefaultAI
{
	static final Location[] points = { new Location(123383, 121093, -2864), new Location(122670, 120890, -3088),
			new Location(124617, 119069, -3088), new Location(126177, 118273, -3080), new Location(125979, 119528, -2728),
			new Location(126177, 118273, -3080), new Location(124617, 119069, -3088), new Location(122670, 120890, -3088),
			new Location(123383, 121093, -2864) };

	private int current_point = -1;
	private long wait_timeout = 0;
	private boolean wait = false;

	public SuspiciousMerchantValley(L2Character actor)
	{
		super(actor);
	}

	@Override
	public boolean isGlobalAI()
	{
		return true;
	}

	@Override
	protected boolean thinkActive()
	{
		L2NpcInstance actor = getActor();
		if(actor == null || actor.isDead())
			return true;

		if(_def_think)
		{
			doTask();
			return true;
		}

		if(System.currentTimeMillis() > wait_timeout && (current_point > -1 || Rnd.chance(5)))
		{
			if(!wait)
				switch(current_point)
				{
					case 0:
						wait_timeout = System.currentTimeMillis() + 30000;
						wait = true;
						return true;
					case 4:
						wait_timeout = System.currentTimeMillis() + 60000;
						wait = true;
						return true;
					case 8:
						wait_timeout = System.currentTimeMillis() + 30000;
						wait = true;
						return true;
				}

			wait_timeout = 0;
			wait = false;
			current_point++;

			if(current_point >= points.length)
				current_point = 0;

			addTaskMove(points[current_point], false);
			doTask();
			return true;
		}

		if(randomAnimation())
			return true;

		return false;
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{}
}