package l2p.gameserver.serverpackets;

import java.util.concurrent.ConcurrentLinkedQueue;

import l2p.gameserver.model.L2Player;
import l2p.gameserver.model.items.L2ItemInstance;

public class ExRefundList extends L2GameServerPacket
{
	private ConcurrentLinkedQueue<L2ItemInstance> _RefundList;
	long _adena;

	public ExRefundList(L2Player cha)
	{
		_adena = cha.getAdena();
		_RefundList = cha.getInventory().getRefundItemsList();
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xA8);

		writeQ(_adena);

		// hx[ddQhhhQhhhhhhhh]
		if(_RefundList == null)
			writeH(0);
		else
		{
			writeH(_RefundList.size());
			for(L2ItemInstance item : _RefundList)
			{
				writeD(item.getObjectId());
				writeD(item.getItemId());
				writeQ(item.getCount());
				writeH(item.getItem().getType2ForPackets());
				writeH(item.getEnchantLevel());
				writeH(0x00); // unknown				
				writeQ(item.getItem().getReferencePrice() / 2);
				writeItemElements(item);
			}
		}
	}
}