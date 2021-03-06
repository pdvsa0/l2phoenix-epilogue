package l2p.gameserver.model.instances;

import java.util.StringTokenizer;

import l2p.gameserver.cache.Msg;
import l2p.gameserver.instancemanager.ZoneManager;
import l2p.gameserver.model.L2Player;
import l2p.gameserver.model.L2Zone.ZoneType;
import l2p.gameserver.templates.L2NpcTemplate;
import l2p.util.Location;

public final class L2ObservationInstance extends L2NpcInstance
{
	public L2ObservationInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(!canBypassCheck(player, this))
			return;

		// first do the common stuff and handle the commands that all NPC classes know
		super.onBypassFeedback(player, command);

		if(player.getOlympiadGameId() > 0 || player.isTerritoryFlagEquipped())
			return;

		if(command.startsWith("observeSiege"))
		{
			String val = command.substring(13);
			StringTokenizer st = new StringTokenizer(val);
			st.nextToken(); // Bypass cost

			if(ZoneManager.getInstance().checkIfInZone(ZoneType.Siege, Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken())))
				doObserve(player, val);
			else
				player.sendPacket(Msg.OBSERVATION_IS_ONLY_POSSIBLE_DURING_A_SIEGE);
		}
		else if(command.startsWith("observe"))
			doObserve(player, command.substring(8));
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return "data/html/observation/" + pom + ".htm";
	}

	private void doObserve(L2Player player, String val)
	{
		StringTokenizer st = new StringTokenizer(val);
		int cost = Integer.parseInt(st.nextToken());
		if(player.getAdena() < cost)
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
		else if(player.enterObserverMode(new Location(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()))))
			player.reduceAdena(cost, true);
		player.sendActionFailed();
	}
}