package commands.voiced;

import l2p.extensions.scripts.ScriptFile;
import l2p.gameserver.handler.IVoicedCommandHandler;
import l2p.gameserver.handler.VoicedCommandHandler;
import l2p.gameserver.instancemanager.CastleManager;
import l2p.gameserver.model.L2Player;
import l2p.gameserver.model.entity.residence.Residence;
import l2p.gameserver.model.instances.L2DoorInstance;

public class PlayerCastleDoors implements IVoicedCommandHandler, ScriptFile
{
	private static String[] _voicedCommands = { "open", "close" };

	public boolean useVoicedCommand(String command, L2Player activeChar, String target)
	{
		if(activeChar.getTarget() == null || !activeChar.getTarget().isDoor() || activeChar.getClan() == null)
			return false;
		L2DoorInstance door = (L2DoorInstance) activeChar.getTarget();
		Residence castle = CastleManager.getInstance().getCastleByIndex(activeChar.getClan().getHasCastle());
		if(door == null || castle == null)
			return false;

		if(target.equals("doors") && activeChar.isClanLeader() && castle.checkIfInZone(door.getX(), door.getY()))
			if(command.startsWith("open"))
				door.openMe();
			else if(command.startsWith("close"))
				door.closeMe();
		return true;
	}

	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}

	public void onLoad()
	{
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}