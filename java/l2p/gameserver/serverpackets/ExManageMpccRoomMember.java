package l2p.gameserver.serverpackets;

public class ExManageMpccRoomMember extends L2GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x9E);
		// TODO ...
	}
}