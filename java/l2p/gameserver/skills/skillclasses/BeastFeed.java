package l2p.gameserver.skills.skillclasses;

import l2p.gameserver.cache.Msg;
import l2p.gameserver.model.L2Character;
import l2p.gameserver.model.L2Player;
import l2p.gameserver.model.L2Skill;
import l2p.gameserver.model.instances.L2FeedableBeastInstance;
import l2p.gameserver.templates.StatsSet;
import l2p.util.GArray;

public class BeastFeed extends L2Skill
{
	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(!(target instanceof L2FeedableBeastInstance))
		{
			activeChar.sendPacket(Msg.INVALID_TARGET);
			return false;
		}
		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	public BeastFeed(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, GArray<L2Character> targets)
	{
		_log.fine("Beast Feed casting succeded.");

		for(L2Character target : targets)
			if(target != null)
				((L2FeedableBeastInstance) target).onSkillUse((L2Player) activeChar, _id);
	}
}
