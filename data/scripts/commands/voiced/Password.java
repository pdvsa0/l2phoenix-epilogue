package commands.voiced;

import l2p.Config;
import l2p.extensions.multilang.CustomMessage;
import l2p.extensions.scripts.Functions;
import l2p.extensions.scripts.ScriptFile;
import l2p.gameserver.handler.IVoicedCommandHandler;
import l2p.gameserver.handler.VoicedCommandHandler;
import l2p.gameserver.loginservercon.LSConnection;
import l2p.gameserver.loginservercon.gspackets.ChangePassword;
import l2p.gameserver.model.L2Player;
import l2p.util.Util;

public class Password extends Functions implements IVoicedCommandHandler, ScriptFile
{
	private String[] _commandList = new String[] { "password" };

	public void onLoad()
	{
		if(Config.SERVICES_CHANGE_PASSWORD)
			VoicedCommandHandler.getInstance().registerVoicedCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public void check(String[] var)
	{
		if(var.length != 3)
		{
			show(new CustomMessage("scripts.commands.user.password.IncorrectValues", getSelf()), (L2Player) getSelf());
			return;
		}
		useVoicedCommand("password", (L2Player) getSelf(), var[0] + " " + var[1] + " " + var[2]);
	}

	public boolean useVoicedCommand(String command, L2Player activeChar, String target)
	{
		if(command.equals("password") && (target == null || target.equals("")))
		{
			show("data/scripts/commands/voiced/password.html", activeChar);
			return true;
		}

		String[] parts = target.split(" ");

		if(parts.length != 3)
		{
			show(new CustomMessage("scripts.commands.user.password.IncorrectValues", activeChar), activeChar);
			return false;
		}

		if(!parts[1].equals(parts[2]))
		{
			show(new CustomMessage("scripts.commands.user.password.IncorrectConfirmation", activeChar), activeChar);
			return false;
		}

		if(parts[1].equals(parts[0]))
		{
			show(new CustomMessage("scripts.commands.user.password.NewPassIsOldPass", activeChar), activeChar);
			return false;
		}

		if(parts[1].length() < 5 || parts[1].length() > 20)
		{
			show(new CustomMessage("scripts.commands.user.password.IncorrectSize", activeChar), activeChar);
			return false;
		}

		if(!Util.isMatchingRegexp(parts[1], Config.APASSWD_TEMPLATE))
		{
			show(new CustomMessage("scripts.commands.user.password.IncorrectInput", activeChar), activeChar);
			return false;
		}

		LSConnection.getInstance().sendPacket(new ChangePassword(activeChar.getAccountName(), parts[0], parts[1], activeChar.hasHWID() ? activeChar.getHWID().Full : "null"));
		return true;
	}

	public String[] getVoicedCommandList()
	{
		return _commandList;
	}
}