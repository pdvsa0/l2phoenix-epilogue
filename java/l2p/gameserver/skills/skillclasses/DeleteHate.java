package l2p.gameserver.skills.skillclasses;

import l2p.Config;
import l2p.extensions.multilang.CustomMessage;
import l2p.gameserver.ai.CtrlIntention;
import l2p.gameserver.model.L2Character;
import l2p.gameserver.model.L2Player;
import l2p.gameserver.model.L2Skill;
import l2p.gameserver.model.instances.L2NpcInstance;
import l2p.gameserver.templates.StatsSet;
import l2p.util.GArray;
import l2p.util.Rnd;

public class DeleteHate extends L2Skill
{
	public DeleteHate(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, GArray<L2Character> targets)
	{
		for(L2Character target : targets)
			if(target != null)
			{

				if(target.isRaid())
					continue;

				if(getActivateRate() > 0)
				{
					if(Config.SKILLS_SHOW_CHANCE && activeChar.isPlayer() && !((L2Player) activeChar).getVarB("SkillsHideChance"))
						activeChar.sendMessage(new CustomMessage("l2p.gameserver.skills.Formulas.Chance", activeChar).addString(getName()).addNumber(getActivateRate()));

					if(!Rnd.chance(getActivateRate()))
						return;
				}

				if(target.isNpc())
				{
					L2NpcInstance npc = (L2NpcInstance) target;
					npc.clearAggroList(true);
					npc.getAI().clearTasks();
					npc.getAI().setGlobalAggro(System.currentTimeMillis() + 10000);
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				}

				getEffects(activeChar, target, false, false);
			}
	}
}
