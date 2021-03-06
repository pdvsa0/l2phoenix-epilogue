import l2p.Config;
import l2p.common.ThreadPoolManager;
import l2p.extensions.multilang.CustomMessage;
import l2p.extensions.scripts.Functions;
import l2p.extensions.scripts.ScriptFile;
import l2p.gameserver.cache.Msg;
import l2p.gameserver.geodata.GeoEngine;
import l2p.gameserver.instancemanager.TownManager;
import l2p.gameserver.instancemanager.ZoneManager;
import l2p.gameserver.model.L2Character;
import l2p.gameserver.model.L2Object;
import l2p.gameserver.model.L2Playable;
import l2p.gameserver.model.L2Player;
import l2p.gameserver.model.L2Skill;
import l2p.gameserver.model.L2Zone;
import l2p.gameserver.model.L2Skill.SkillType;
import l2p.gameserver.model.L2Zone.ZoneType;
import l2p.gameserver.model.entity.SevenSigns;
import l2p.gameserver.model.entity.residence.Castle;
import l2p.gameserver.model.instances.L2NpcInstance;
import l2p.gameserver.model.items.L2ItemInstance;
import l2p.gameserver.serverpackets.SystemMessage;
import l2p.gameserver.tables.ReflectionTable;
import l2p.gameserver.tables.SkillTable;
import l2p.util.Location;

public class Util extends Functions implements ScriptFile
{
	public void onLoad()
	{
		System.out.println("Utilites Loaded");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	/**
	 * Перемещает за плату в аденах
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @param price
	 */
	public void Gatekeeper(String[] param)
	{
		if(param.length < 4)
			throw new IllegalArgumentException();

		L2Player player = (L2Player) getSelf();
		if(player == null)
			return;

		long price = Long.parseLong(param[3]);

		if(!L2NpcInstance.canBypassCheck(player, player.getLastNpc()))
			return;

		if(price > 0 && player.getAdena() < price)
		{
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			return;
		}

		if(player.isTerritoryFlagEquipped() || player.isCombatFlagEquipped())
		{
			player.sendPacket(Msg.YOU_CANNOT_TELEPORT_WHILE_IN_POSSESSION_OF_A_WARD);
			return;
		}

		if(player.getMountType() == 2)
		{
			player.sendMessage("Телепортация верхом на виверне невозможна.");
			return;
		}

		/* Затычка, npc Mozella не ТПшит чаров уровень которых превышает заданный в конфиге
		* Off Like >= 56 lvl, данные по ограничению lvl'a устанавливаются в altsettings.properties.
		*/
		if(player.getLastNpc() != null)
		{
			int mozella_cruma = 30483; // NPC Mozella id 30483
			if(player.getLastNpc().getNpcId() == mozella_cruma && player.getLevel() >= Config.CRUMA_GATEKEEPER_LVL)
			{
				show("data/html/teleporter/30483-no.htm", player);
				return;
			}
		}

		int x = Integer.parseInt(param[0]);
		int y = Integer.parseInt(param[1]);
		int z = Integer.parseInt(param[2]);

		// Нельзя телепортироваться в города, где идет осада
		// Узнаем, идет ли осада в ближайшем замке к точке телепортации
		Castle castle = TownManager.getInstance().getClosestTown(x, y).getCastle();
		if(castle != null && castle.getSiege().isInProgress())
		{
			// Определяем, в город ли телепортируется чар
			boolean teleToTown = false;
			int townId = 0;
			for(L2Zone town : ZoneManager.getInstance().getZoneByType(ZoneType.Town))
				if(town.checkIfInZone(x, y))
				{
					teleToTown = true;
					townId = town.getIndex();
					break;
				}

			if(teleToTown && townId == castle.getTown())
			{
				player.sendPacket(Msg.YOU_CANNOT_TELEPORT_TO_A_VILLAGE_THAT_IS_IN_A_SIEGE);
				return;
			}
		}

		Location pos = GeoEngine.findPointToStay(x, y, z, 50, 100, player.getReflection().getGeoIndex());

		if(price > 0)
			player.reduceAdena(price, true);
		player.teleToLocation(pos);
	}

	public void SSGatekeeper(String[] param)
	{
		if(param.length < 4)
			throw new IllegalArgumentException();

		L2Player player = (L2Player) getSelf();
		if(player == null)
			return;

		int type = Integer.parseInt(param[3]);

		if(!L2NpcInstance.canBypassCheck(player, player.getLastNpc()))
			return;

		if(type > 0)
		{
			int player_cabal = SevenSigns.getInstance().getPlayerCabal(player);
			int period = SevenSigns.getInstance().getCurrentPeriod();
			if(period == SevenSigns.PERIOD_COMPETITION && player_cabal == SevenSigns.CABAL_NULL)
			{
				player.sendPacket(Msg.USED_ONLY_DURING_A_QUEST_EVENT_PERIOD);
				return;
			}

			int winner;
			if(period == SevenSigns.PERIOD_SEAL_VALIDATION && (winner = SevenSigns.getInstance().getCabalHighestScore()) != SevenSigns.CABAL_NULL)
			{
				if(winner != player_cabal)
					return;
				if(type == 1 && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE) != player_cabal)
					return;
				if(type == 2 && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS) != player_cabal)
					return;
			}
		}

		player.teleToLocation(Integer.parseInt(param[0]), Integer.parseInt(param[1]), Integer.parseInt(param[2]));
	}

	/**
	 * Перемещает за определенный предмет
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @param count
	 * @param item
	 */
	public void QuestGatekeeper(String[] param)
	{
		if(param.length < 5)
			throw new IllegalArgumentException();

		L2Player player = (L2Player) getSelf();
		if(player == null)
			return;

		if(player.isTerritoryFlagEquipped())
		{
			player.sendPacket(Msg.YOU_CANNOT_TELEPORT_WHILE_IN_POSSESSION_OF_A_WARD);
			return;
		}

		long count = Long.parseLong(param[3]);
		int item = Integer.parseInt(param[4]);

		if(!L2NpcInstance.canBypassCheck(player, player.getLastNpc()))
			return;

		if(count > 0)
		{
			L2ItemInstance ii = player.getInventory().getItemByItemId(item);
			if(ii == null || ii.getCount() < count)
			{
				player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS);
				return;
			}
			player.getInventory().destroyItem(ii, count, true);
			player.sendPacket(SystemMessage.removeItems(item, count));
		}

		int x = Integer.parseInt(param[0]);
		int y = Integer.parseInt(param[1]);
		int z = Integer.parseInt(param[2]);

		Location pos = GeoEngine.findPointToStay(x, y, z, 20, 70, player.getReflection().getGeoIndex());

		player.teleToLocation(pos);
	}

	public void ReflectionGatekeeper(String[] param)
	{
		if(param.length < 5)
			throw new IllegalArgumentException();

		L2Player player = (L2Player) getSelf();
		if(player == null)
			return;

		player.setReflection(Integer.parseInt(param[4]));

		Gatekeeper(param);
	}

	/**
	 * Используется для телепортации за Newbie Token, проверяет уровень и передает
	 * параметры в QuestGatekeeper
	 */
	public void TokenJump(String[] param)
	{
		L2Player player = (L2Player) getSelf();
		if(player == null)
			return;
		if(player.getLevel() <= 19)
			QuestGatekeeper(param);
		else
			show("Only for newbies", player);
	}

	public void NoblessTeleport()
	{
		L2Player player = (L2Player) getSelf();
		if(player == null)
			return;
		if(player.isNoble() || Config.ALLOW_NOBLE_TP_TO_ALL)
			show("data/scripts/noble.htm", player);
		else
			show("data/scripts/nobleteleporter-no.htm", player);
	}

	public void PayPage(String[] param)
	{
		if(param.length < 2)
			throw new IllegalArgumentException();

		L2Player player = (L2Player) getSelf();
		if(player == null)
			return;

		String page = param[0];
		int item = Integer.parseInt(param[1]);
		long price = Long.parseLong(param[2]);

		if(getItemCount(player, item) < price)
		{
			player.sendPacket(item == 57 ? Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA : Msg.INCORRECT_ITEM_COUNT);
			return;
		}

		removeItem(player, item, price);
		show(page, player);
	}

	public void SimpleExchange(String[] param)
	{
		if(param.length < 4)
			throw new IllegalArgumentException();

		L2Player player = (L2Player) getSelf();
		if(player == null)
			return;

		int itemToTake = Integer.parseInt(param[0]);
		long countToTake = Long.parseLong(param[1]);
		int itemToGive = Integer.parseInt(param[2]);
		long countToGive = Long.parseLong(param[3]);

		if(getItemCount(player, itemToTake) < countToTake)
		{
			player.sendPacket(itemToTake == 57 ? Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA : Msg.INCORRECT_ITEM_COUNT);
			return;
		}

		removeItem(player, itemToTake, countToTake);
		addItem(player, itemToGive, countToGive);
	}

	public void MakeEchoCrystal(String[] param)
	{
		if(param.length < 2)
			throw new IllegalArgumentException();

		L2Player player = (L2Player) getSelf();
		if(player == null)
			return;

		if(!L2NpcInstance.canBypassCheck(player, player.getLastNpc()))
			return;

		int crystal = Integer.parseInt(param[0]);
		int score = Integer.parseInt(param[1]);

		if(getItemCount(player, score) == 0)
		{
			player.getLastNpc().onBypassFeedback(player, "Chat 1");
			return;
		}

		if(getItemCount(player, 57) < 200)
		{
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			return;
		}

		removeItem(player, 57, 200);
		addItem(player, crystal, 1);
	}

	public void TakeNewbieWeaponCoupon()
	{
		L2Player player = (L2Player) getSelf();
		if(player == null)
			return;
		if(!Config.ALT_ALLOW_SHADOW_WEAPONS)
		{
			show(new CustomMessage("common.Disabled", player), player);
			return;
		}
		if(player.getLevel() > 19 || player.getClassId().getLevel() > 1)
		{
			show("Your level is too high!", player);
			return;
		}
		if(player.getLevel() < 6)
		{
			show("Your level is too low!", player);
			return;
		}
		if(player.getVarB("newbieweapon"))
		{
			show("Your already got your newbie weapon!", player);
			return;
		}
		addItem(player, 7832, 5);
		player.setVar("newbieweapon", "true");
	}

	public void TakeAdventurersArmorCoupon()
	{
		L2Player player = (L2Player) getSelf();
		if(player == null)
			return;
		if(!Config.ALT_ALLOW_SHADOW_WEAPONS)
		{
			show(new CustomMessage("common.Disabled", player), player);
			return;
		}
		if(player.getLevel() > 39 || player.getClassId().getLevel() > 2)
		{
			show("Your level is too high!", player);
			return;
		}
		if(player.getLevel() < 20 || player.getClassId().getLevel() < 2)
		{
			show("Your level is too low!", player);
			return;
		}
		if(player.getVarB("newbiearmor"))
		{
			show("Your already got your newbie weapon!", player);
			return;
		}
		addItem(player, 7833, 1);
		player.setVar("newbiearmor", "true");
	}

	public static void CheckPlayerInEpicZone(final L2Zone zone, final L2Object object, final Boolean enter)
	{
		if(enter && object.isPlayable() && ((L2Playable) object).getLevel() > 48 && object.isInZone(zone))
		{
			boolean playerIsCastingRecallSkill = object.isPlayer() && object.getPlayer().getCastingSkill() != null && object.getPlayer().getCastingSkill().getSkillType() == SkillType.RECALL;
			boolean alreadyInRaidCurce = ((L2Character) object).getEffectList().getEffectsBySkillId(L2Skill.SKILL_RAID_CURSE) != null;

			if(!playerIsCastingRecallSkill && !alreadyInRaidCurce)
				SkillTable.getInstance().getInfo(L2Skill.SKILL_RAID_CURSE, 1).getEffects(((L2Character) object), ((L2Character) object), false, false);

			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){
				@Override
				public void run()
				{
					Functions.callScripts("Util", "CheckPlayerInEpicZone", new Object[] { zone, object, enter });
				}
			}, 10000);
		}
	}

	public static void CheckPlayerInMultilayerZone(L2Zone zone, final L2Object object, final Boolean enter)
	{
		if(object.isPlayable() || object.isMonster())
			if(enter && ((L2Character) object).getLevel() < 76 && object.isInZone(zone))
				object.setReflection(ReflectionTable.MULTILAYER);
			else if(object.getReflection().getId() == ReflectionTable.MULTILAYER)
				object.setReflection(ReflectionTable.DEFAULT);
	}

	public static void CheckPlayerInTully1Zone(L2Zone zone, final L2Object object, final Boolean enter)
	{
		if(!enter || !object.isPlayer() || !object.isInZone(zone))
			return;

		L2Player p = object.getPlayer();
		Location TullyFloor2LocationPoint = new Location(-14180, 273060, -13600);
		final int MASTER_ZELOS_ID = 22377;
		boolean teleport = true;

		for(L2NpcInstance npc : p.getAroundNpc(3000, 256))
			if(npc.getNpcId() == MASTER_ZELOS_ID && !npc.isDead())
				teleport = false;

		if(teleport)
			p.teleToLocation(TullyFloor2LocationPoint);
	}

	public static void CheckPlayerInTully2Zone(L2Zone zone, final L2Object object, final Boolean enter)
	{
		if(!enter || !object.isPlayer() || !object.isInZone(zone))
			return;

		L2Player p = object.getPlayer();
		Location TullyFloor4LocationPoint = new Location(-14238, 273002, -10496);
		final int MASTER_FESTINA_ID = 22380;
		boolean teleport = true;

		for(L2NpcInstance npc : p.getAroundNpc(3000, 500))
			if(npc.getNpcId() == MASTER_FESTINA_ID && !npc.isDead())
				teleport = false;

		if(teleport)
			p.teleToLocation(TullyFloor4LocationPoint);
	}
}