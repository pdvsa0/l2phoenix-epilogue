package services.villagemasters;

import l2p.extensions.scripts.Functions;
import l2p.extensions.scripts.ScriptFile;
import l2p.gameserver.model.L2Player;
import l2p.gameserver.model.instances.L2VillageMasterInstance;

public class Clan extends Functions implements ScriptFile
{
	public void onLoad()
	{
		System.out.println("Loaded Service: Villagemasters [Clan Operations]");
	}

	public void CheckCreateClan()
	{
		if(getNpc() == null || getSelf() == null)
			return;
		L2Player pl = (L2Player) getSelf();
		String htmltext = "clan-02.htm";
		// Player less 10 levels, and can not create clan
		if(pl.getLevel() <= 9)
			htmltext = "clan-06.htm";
		// Player already is a clan by leader and can not newly create clan
		else if(pl.isClanLeader())
			htmltext = "clan-07.htm";
		// Player already consists in clan and can not create clan
		else if(pl.getClan() != null)
			htmltext = "clan-09.htm";
		((L2VillageMasterInstance) getNpc()).showChatWindow(pl, "data/html/villagemaster/" + htmltext);
	}

	public void CheckDissolveClan()
	{
		if(getNpc() == null || getSelf() == null)
			return;
		L2Player pl = (L2Player) getSelf();
		String htmltext = "clan-01.htm";
		if(pl.isClanLeader())
			htmltext = "clan-04.htm";
		else
		// Player already consists in clan and can not create clan
		if(pl.getClan() != null)
			htmltext = "9000-08.htm";
		// Player not in clan and can not dismiss clan
		else
			htmltext = "9000-11.htm";
		((L2VillageMasterInstance) getNpc()).showChatWindow(pl, "data/html/villagemaster/" + htmltext);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}