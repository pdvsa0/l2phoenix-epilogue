package l2p.gameserver.clientpackets;

import l2p.Config;
import l2p.extensions.scripts.Functions;
import l2p.gameserver.cache.Msg;
import l2p.gameserver.model.L2Character;
import l2p.gameserver.model.L2Clan;
import l2p.gameserver.model.L2Player;
import l2p.gameserver.model.L2ShortCut;
import l2p.gameserver.model.L2Skill;
import l2p.gameserver.model.L2SkillLearn;
import l2p.gameserver.model.instances.L2NpcInstance;
import l2p.gameserver.model.instances.L2VillageMasterInstance;
import l2p.gameserver.model.items.L2ItemInstance;
import l2p.gameserver.serverpackets.AcquireSkillList;
import l2p.gameserver.serverpackets.ExStorageMaxCount;
import l2p.gameserver.serverpackets.ShortCutRegister;
import l2p.gameserver.serverpackets.SkillList;
import l2p.gameserver.serverpackets.SystemMessage;
import l2p.gameserver.tables.SkillSpellbookTable;
import l2p.gameserver.tables.SkillTable;
import l2p.gameserver.tables.SkillTreeTable;
import l2p.gameserver.tables.SkillTable.SubclassSkills;
import l2p.util.Util;

public class RequestAquireSkill extends L2GameClientPacket
{
	// format: cddd(d)
	private int _id, _level, _skillType, _unk;

	@Override
	public void readImpl()
	{
		_id = readD();
		_level = readD();
		_skillType = readD();
		if(_skillType == 3)
			_unk = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null || activeChar.getTransformation() != 0)
			return;
		L2NpcInstance trainer = activeChar.getLastNpc();
		if((trainer == null || activeChar.getDistance(trainer.getX(), trainer.getY()) > L2Character.INTERACTION_DISTANCE) && !activeChar.isGM())
			return;

		if(_skillType == 3)
			System.out.println(getType() + " :: skillType == 3 :: " + _unk + " // " + activeChar.toFullString());

		if(SubclassSkills.isSubclassSkill(_id))
		{
			Functions.callScripts("services.SubclassSkills", "learnSkill", new Object[] { activeChar, new Integer(_id) });
			return;
		}

		activeChar.setSkillLearningClassId(activeChar.getClassId());

		L2Skill skill = SkillTable.getInstance().getInfo(_id, _level);
		if(skill == null)
			return;

		if(activeChar.getSkillLevel(_id) >= _level)
			return; // already knows the skill with this level

		boolean isTransferSkill = _skillType == AcquireSkillList.TRANSFER;

		if(isTransferSkill && (activeChar.getLevel() < 76 || activeChar.getClassId().getLevel() < 4))
		{
			activeChar.sendMessage("You must have 3rd class change quest completed.");
			return;
		}

		if(!isTransferSkill && _level > 1 && activeChar.getSkillLevel(_id) != _level - 1)
		{
			Util.handleIllegalPlayerAction(activeChar, "RequestAquireSkill[58]", "tried to increase skill " + _id + " level to " + _level + " while having it's level " + activeChar.getSkillLevel(_id), 1);
			return;
		}

		// TODO обязательно добавить проверку на изучение при isTransferSkill
		if(!(skill.isCommon() || isTransferSkill || SkillTreeTable.getInstance().isSkillPossible(activeChar, _id, _level)))
		{
			Util.handleIllegalPlayerAction(activeChar, "RequestAquireSkill[64]", "tried to learn skill " + _id + " while on class " + activeChar.getActiveClass(), 1);
			return;
		}

		L2SkillLearn SkillLearn = SkillTreeTable.getSkillLearn(_id, _level, activeChar.getClassId(), _skillType == AcquireSkillList.CLAN ? activeChar.getClan() : null, isTransferSkill);

		int itemCount = SkillLearn.getItemCount();
		if(itemCount == -1)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(_skillType == AcquireSkillList.CLAN)
			learnClanSkill(skill, activeChar.getClan());
		else if(_skillType == AcquireSkillList.TRANSFER)
		{
			if(isTransferSkill)
			{
				int item_id = 0;
				switch(activeChar.getClassId())
				{
					case cardinal:
						item_id = 15307;
						break;
					case evaSaint:
						item_id = 15308;
						break;
					case shillienSaint:
						item_id = 15309;
						break;
					default:
						activeChar.sendMessage("There is no skills for your class.");
						return;
				}

				L2ItemInstance spb = activeChar.getInventory().getItemByItemId(item_id);
				if(spb == null || spb.getCount() < 1)
				{
					activeChar.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_SKILLS);
					return;
				}

				L2ItemInstance ri = activeChar.getInventory().destroyItem(spb, 1, true);
				activeChar.sendPacket(SystemMessage.removeItems(ri.getItemId(), 1));

				String var = activeChar.getVar("TransferSkills" + item_id);
				if(var == null)
					var = "";
				if(!var.isEmpty())
					var += ";";
				var += skill.getId();
				activeChar.setVar("TransferSkills" + item_id, var);
			}

			activeChar.addSkill(skill, true);
			activeChar.updateStats();
			activeChar.sendUserInfo(true);
		}
		else
		{
			int _requiredSp = SkillTreeTable.getInstance().getSkillCost(activeChar, skill);

			if(activeChar.getSp() >= _requiredSp || SkillLearn.common || SkillLearn.transformation)
			{
				Integer spb_id = SkillSpellbookTable.getSkillSpellbooks().get(SkillSpellbookTable.hashCode(new int[] {
						skill.getId(), skill.getLevel() }));
				if(spb_id != null)
				{
					L2ItemInstance spb = activeChar.getInventory().getItemByItemId(spb_id);
					if(spb == null || spb.getCount() < itemCount)
					{
						activeChar.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_SKILLS);
						return;
					}
					L2ItemInstance ri = activeChar.getInventory().destroyItem(spb, itemCount, true);
					activeChar.sendPacket(SystemMessage.removeItems(ri.getItemId(), itemCount));
				}

				activeChar.addSkill(skill, true);
				if(!SkillLearn.common && !SkillLearn.transformation)
					activeChar.setSp(activeChar.getSp() - _requiredSp);

				activeChar.updateStats();
				activeChar.sendUserInfo(true);

				//update all the shortcuts to this skill
				if(_level > 1)
					for(L2ShortCut sc : activeChar.getAllShortCuts())
						if(sc.id == _id && sc.type == L2ShortCut.TYPE_SKILL)
						{
							L2ShortCut newsc = new L2ShortCut(sc.slot, sc.page, sc.type, sc.id, _level);
							activeChar.sendPacket(new ShortCutRegister(newsc));
							activeChar.registerShortCut(newsc);
						}
			}
			else
			{
				activeChar.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_LEARN_SKILLS);
				return;
			}
		}

		if(SkillLearn.common)
			activeChar.sendPacket(new ExStorageMaxCount(activeChar));
		activeChar.sendPacket(new SkillList(activeChar));

		if(trainer != null)
			if(_skillType == AcquireSkillList.USUAL)
				trainer.showSkillList(activeChar);
			else if(_skillType == AcquireSkillList.FISHING)
				trainer.showFishingSkillList(activeChar);
			else if(_skillType == AcquireSkillList.CLAN)
				trainer.showClanSkillList(activeChar);
			else if(_skillType == AcquireSkillList.TRANSFORMATION)
				trainer.showTransformationSkillList(activeChar);
			else if(_skillType == AcquireSkillList.TRANSFER)
				trainer.showTransferSkillList(activeChar);
	}

	private void learnClanSkill(L2Skill skill, L2Clan clan)
	{
		L2Player player = getClient().getActiveChar();
		if(player == null || skill == null || clan == null)
			return;
		L2NpcInstance trainer = player.getLastNpc();
		if(trainer == null)
			return;
		if(!(trainer instanceof L2VillageMasterInstance))
		{
			System.out.println("RequestAquireSkill.learnClanSkill, trainer isn't L2VillageMasterInstance");
			System.out.println(trainer.getName() + "[" + trainer.getNpcId() + "] Loc: " + trainer.getLoc());
			return;
		}
		if(!player.isClanLeader())
		{
			player.sendPacket(Msg.ONLY_THE_CLAN_LEADER_IS_ENABLED);
			return;
		}
		L2SkillLearn SkillLearn = SkillTreeTable.getSkillLearn(_id, _level, null, clan, false);
		int requiredRep = SkillTreeTable.getInstance().getSkillRepCost(clan, skill);
		int itemId = 0;
		if(!Config.ALT_DISABLE_SPELLBOOKS)
			itemId = SkillLearn.itemId;
		if(skill.getMinPledgeClass() <= clan.getLevel() && clan.getReputationScore() >= requiredRep)
		{
			if(itemId > 0)
			{
				L2ItemInstance spb = player.getInventory().getItemByItemId(itemId);
				if(spb == null)
				{
					// Haven't spellbook
					player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_SKILLS);
					return;
				}
				L2ItemInstance ri = player.getInventory().destroyItem(spb, SkillLearn.itemCount, true);
				player.sendPacket(SystemMessage.removeItems(ri.getItemId(), SkillLearn.itemCount));
			}
			clan.incReputation(-requiredRep, false, "AquireSkill: " + _id + ", lvl " + _level);
			clan.addNewSkill(skill, true);
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1).addSkillName(_id, _level));

			((L2VillageMasterInstance) trainer).showClanSkillWindow(player); //Maybe we shoud add a check here...
		}
		else
		{
			player.sendMessage("Your clan doesn't have enough reputation points to learn this skill");
			return;
		}

		//update all the shortcuts to this skill
		if(_level > 1)
			for(L2ShortCut sc : player.getAllShortCuts())
				if(sc.id == _id && sc.type == L2ShortCut.TYPE_SKILL)
				{
					L2ShortCut newsc = new L2ShortCut(sc.slot, sc.page, sc.type, sc.id, _level);
					player.sendPacket(new ShortCutRegister(newsc));
					player.registerShortCut(newsc);
				}

		clan.addAndShowSkillsToPlayer(player);
	}
}