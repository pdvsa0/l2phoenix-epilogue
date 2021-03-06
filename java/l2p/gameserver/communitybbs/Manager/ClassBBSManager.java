package l2p.gameserver.communitybbs.Manager;

import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import l2p.Config;
import l2p.gameserver.cache.Msg;
import l2p.gameserver.model.L2Player;
import l2p.gameserver.model.base.ClassId;
import l2p.gameserver.model.items.L2ItemInstance;
import l2p.gameserver.serverpackets.SystemMessage;
import l2p.gameserver.tables.ItemTable;
import l2p.gameserver.templates.L2Item;
import l2p.util.Files;
import l2p.util.Util;

public class ClassBBSManager extends BaseBBSManager
{
	private static ClassBBSManager _Instance = null;

	public static ClassBBSManager getInstance()
	{
		if(_Instance == null)
			_Instance = new ClassBBSManager();
		return _Instance;
	}

	public void parsecmd(String command, L2Player activeChar)
	{
		ClassId classId = activeChar.getClassId();
		int jobLevel = classId.getLevel();
		int level = activeChar.getLevel();
		TextBuilder html = new TextBuilder("");
		html.append("<br>");
		html.append("<table width=600>");
		html.append("<tr><td>");
		if(Config.ALLOW_CLASS_MASTERS_LIST.isEmpty() || !Config.ALLOW_CLASS_MASTERS_LIST.contains(jobLevel))
			jobLevel = 4;
		
		if((level >= 20 && jobLevel == 1 || level >= 40 && jobLevel == 2 || level >= 76 && jobLevel == 3) && Config.ALLOW_CLASS_MASTERS_LIST.contains(jobLevel))
		{
			L2Item item = ItemTable.getInstance().getTemplate(Config.CLASS_MASTERS_PRICE_ITEM);
			html.append("Вы должны заплатить: <font color=\"LEVEL\">");
			html.append(Util.formatAdena(Config.CLASS_MASTERS_PRICE_LIST[jobLevel])).append("</font> <font color=\"LEVEL\">").append(item.getName()).append("</font> для смены профессии<br>");
			html.append("<center><table width=600><tr>");
			for(ClassId cid : ClassId.values())
			{
				if(cid == ClassId.inspector)
					continue;
				if(cid.childOf(classId) && cid.level() == classId.level() + 1)
					html.append("<td><center><button value=\"").append(cid.name()).append("\" action=\"bypass -h _bbsclass;change_class;").append(cid.getId()).append(";").append(Config.CLASS_MASTERS_PRICE_LIST[jobLevel]).append("\" width=150 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center></td>");

			}
			html.append("</tr></table></center>");
			html.append("</td>");
			html.append("</tr>");
			html.append("</table>");
		}
		else
		{
			switch(jobLevel)
			{
				case 1:
					html.append("Приветствую <font color=F2C202>" + activeChar.getName() + "</font> ваша текущая профессия <font color=F2C202>" + activeChar.getClassId().name() + "</font><br>");
					html.append("Для того чтобы сменить вашу профессию вы должны достичь: <font color=F2C202>20-го уровня</font><br>");
					html.append("Для активации сабклассов вы должны достичь <font color=F2C202>76-го уровня</font><br>");
					html.append("Чтобы стать дворянином вы должны прокачать сабкласс до <font color=F2C202>76-го уровня</font><br>");
					break;
				case 2:
					html.append("Приветствую <font color=F2C202>" + activeChar.getName() + "</font> ваша текущая профессия <font color=F2C202>" + activeChar.getClassId().name() + "</font><br>");
					html.append("Для того чтобы сменить вашу профессию вы должны достичь: <font color=F2C202>40-го уровня</font><br>");
					html.append("Для активации сабклассов вы должны достичь <font color=F2C202>76-го уровня</font><br>");
					html.append("Чтобы стать дворянином вы должны прокачать сабкласс до <font color=F2C202>76-го уровня</font><br>");
					break;
				case 3:
					html.append("Приветствую <font color=F2C202>" + activeChar.getName() + "</font> ваша текущая профессия <font color=F2C202>" + activeChar.getClassId().name() + "</font><br>");
					html.append("Для того чтобы сменить вашу профессию вы должны достичь: <font color=F2C202>76-го уровня</font><br>");
					html.append("Для активации сабклассов вы должны достичь <font color=F2C202>76-го уровня</font><br>");
					html.append("Чтобы стать дворянином вы должны прокачать сабкласс до <font color=F2C202>76-го уровня</font><br>");
					break;
				case 4:
					html.append("Приветствую <font color=F2C202>" + activeChar.getName() + "</font> ваша текущая профессия <font color=F2C202>" + activeChar.getClassId().name() + "</font><br>");
					html.append("Для вас больше нет доступных профессий, либо Класс мастер в данный момент недоступен.<br>");
					if(level >= 76)
					{
						html.append("Вы достигли <font color=F2C202>76-го уровня</font> активация сабклассов теперь доступна<br>");
						if(!activeChar.isNoble())
						{
							html.append("Вы можете получить дворянство. Посетите раздел 'Магазин'.<br>");
						}
						else
						{
							html.append("Вы уже дворянин. Получение дворянства более не доступно.<br>");
						}
					}
					break;
			}
		}
		String content = Files.read("data/html/CommunityBoardAlt/100.htm");
		content = content.replace("%classmaster%", html.toString());
		separateAndSend(content, activeChar);

		if(command.startsWith("_bbsclass;change_class;"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			short val = Short.parseShort(st.nextToken());
			int price = Integer.parseInt(st.nextToken());
			L2Item item = ItemTable.getInstance().getTemplate(Config.CLASS_MASTERS_PRICE_ITEM);
			L2ItemInstance pay = activeChar.getInventory().getItemByItemId(item.getItemId());
			if(pay != null && pay.getCount() >= price)
			{
				activeChar.getInventory().destroyItem(pay, (long) price, true);
				changeClass(activeChar, val);
				parsecmd("_bbsclass;", activeChar);
			}
			else if(Config.CLASS_MASTERS_PRICE_ITEM == 57)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_ADENA));
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_ADENA));
			}
		}
	}

	private void changeClass(L2Player player, int val)
	{
		if(player.getClassId().getLevel() == 3)
			player.sendPacket(Msg.YOU_HAVE_COMPLETED_THE_QUEST_FOR_3RD_OCCUPATION_CHANGE_AND_MOVED_TO_ANOTHER_CLASS_CONGRATULATIONS); // для 3 профы
		else
			player.sendPacket(Msg.CONGRATULATIONS_YOU_HAVE_TRANSFERRED_TO_A_NEW_CLASS); // для 1 и 2 профы

		player.setClassId(val, false);
		player.broadcastUserInfo(true);
	}

	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2Player activeChar)
	{}
}