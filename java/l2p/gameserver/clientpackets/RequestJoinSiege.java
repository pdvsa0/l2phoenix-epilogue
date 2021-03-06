package l2p.gameserver.clientpackets;

import l2p.extensions.multilang.CustomMessage;
import l2p.gameserver.cache.Msg;
import l2p.gameserver.instancemanager.CastleManager;
import l2p.gameserver.instancemanager.ClanHallManager;
import l2p.gameserver.instancemanager.FortressManager;
import l2p.gameserver.model.L2Clan;
import l2p.gameserver.model.L2Player;
import l2p.gameserver.model.entity.residence.Castle;
import l2p.gameserver.model.entity.residence.Fortress;
import l2p.gameserver.model.entity.residence.Residence;
import l2p.gameserver.model.entity.residence.ResidenceType;

public class RequestJoinSiege extends L2GameClientPacket
{
	// format: cddd

	private int _id;
	private boolean _isAttacker;
	private boolean _isJoining;

	@Override
	public void readImpl()
	{
		_id = readD();
		_isAttacker = readD() == 1;
		_isJoining = readD() == 1;
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		// Нельзя регистрироваться куда-либо, если клан участвует в осаде.
		if(activeChar.getClan() == null || activeChar.getClan().getSiege() != null)
			return;

		if((activeChar.getClanPrivileges() & L2Clan.CP_CS_MANAGE_SIEGE) != L2Clan.CP_CS_MANAGE_SIEGE)
		{
			activeChar.sendPacket(Msg.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}

		Residence siegeUnit = CastleManager.getInstance().getCastleByIndex(_id);

		if(siegeUnit == null)
			siegeUnit = FortressManager.getInstance().getFortressByIndex(_id);

		if(siegeUnit == null)
			siegeUnit = ClanHallManager.getInstance().getClanHall(_id);

		if(siegeUnit == null || siegeUnit.getSiege() == null)
			return;

		if(_isJoining)
		{
			switch(siegeUnit.getType())
			{
				case Castle:
				case Clanhall:
					for(Castle temp : CastleManager.getInstance().getCastles().values())
						if(temp.getSiege().checkIsClanRegistered(activeChar.getClan()) && siegeUnit.getSiegeDayOfWeek() == temp.getSiegeDayOfWeek() && siegeUnit.getSiegeHourOfDay() == temp.getSiegeHourOfDay())
						{
							activeChar.sendMessage(new CustomMessage("l2p.gameserver.clientpackets.RequestJoinSiege.AlreadyRegistered", activeChar).addString(temp.getName()));
							return;
						}
					break;
				case Fortress:
					for(Fortress fort : FortressManager.getInstance().getFortresses().values())
						if(fort.getSiege().checkIsClanRegistered(activeChar.getClan()))
						{
							activeChar.sendMessage(new CustomMessage("l2p.gameserver.clientpackets.RequestJoinSiege.AlreadyRegistered", activeChar).addString(fort.getName()));
							return;
						}
					break;
			}

			if(_isAttacker)
				siegeUnit.getSiege().registerAttacker(activeChar);
			else
				siegeUnit.getSiege().registerDefender(activeChar);
		}
		else if(siegeUnit.getType() == ResidenceType.Fortress)
			activeChar.sendMessage(new CustomMessage("l2p.gameserver.clientpackets.RequestJoinSiege.CantUnregister", activeChar));
		else
			siegeUnit.getSiege().clearSiegeClan(activeChar.getClan(), false);

		siegeUnit.getSiege().listRegisterClan(activeChar);
	}
}