package l2p.gameserver.taskmanager;

import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import l2p.Config;
import l2p.common.ThreadPoolManager;
import l2p.database.DatabaseUtils;
import l2p.database.FiltredPreparedStatement;
import l2p.database.L2DatabaseFactory;
import l2p.database.ThreadConnection;
import l2p.gameserver.model.L2ObjectsStorage;
import l2p.gameserver.model.L2Player;
import l2p.gameserver.model.items.L2ItemInstance;
import l2p.gameserver.model.items.L2ItemInstance.ItemLocation;
import l2p.gameserver.model.items.PcInventory;
import l2p.gameserver.serverpackets.SystemMessage;
import l2p.gameserver.tables.ItemTable;
import l2p.util.Log;

public class DelayedItemsManager implements Runnable
{
	protected static final Logger _log = Logger.getLogger(DelayedItemsManager.class.getName());
	private static DelayedItemsManager _instance;

	private static final Object _lock = new Object();
	private int last_payment_id = 0;

	public static DelayedItemsManager getInstance()
	{
		if(_instance == null)
			_instance = new DelayedItemsManager();
		return _instance;
	}

	public DelayedItemsManager()
	{
		ThreadConnection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			last_payment_id = get_last_payment_id(con);
		}
		catch(Exception e)
		{}
		finally
		{
			DatabaseUtils.closeConnection(con);
		}

		ThreadPoolManager.getInstance().scheduleGeneral(this, Config.DELAYED_ITEMS_UPDATE_INTERVAL);
		_log.fine("DelayedItemsManager scheduled, last payment: " + last_payment_id);
	}

	private int get_last_payment_id(ThreadConnection con)
	{
		FiltredPreparedStatement st = null;
		ResultSet rset = null;
		int result = last_payment_id;
		try
		{
			st = con.prepareStatement("SELECT MAX(payment_id) AS last FROM items_delayed");
			rset = st.executeQuery();
			if(rset.next())
				result = rset.getInt("last");
		}
		catch(Exception e)
		{}
		finally
		{
			DatabaseUtils.closeDatabaseSR(st, rset);
		}
		return result;
	}

	public void run()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement st = null;
		ResultSet rset = null;
		L2Player player = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			int last_payment_id_temp = get_last_payment_id(con);
			if(last_payment_id_temp != last_payment_id)
				synchronized (_lock)
				{
					st = con.prepareStatement("SELECT DISTINCT owner_id FROM items_delayed WHERE payment_status=0 AND payment_id > ?");
					st.setInt(1, last_payment_id);
					rset = st.executeQuery();
					while(rset.next())
						if((player = L2ObjectsStorage.getPlayer(rset.getInt("owner_id"))) != null)
							loadDelayed(player, true);
					last_payment_id = last_payment_id_temp;
				}
		}
		catch(Exception e)
		{}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, st, rset);
		}

		ThreadPoolManager.getInstance().scheduleGeneral(this, Config.DELAYED_ITEMS_UPDATE_INTERVAL);
	}

	public int loadDelayed(L2Player player, boolean notify)
	{
		if(player == null)
			return 0;
		final int player_id = player.getObjectId();
		final PcInventory inv = player.getInventory();
		if(inv == null)
			return 0;

		ThreadConnection con = null;
		FiltredPreparedStatement st = null, st_delete = null;
		ResultSet rset = null;
		int restored_counter = 0;

		synchronized (_lock)
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				st = con.prepareStatement("SELECT * FROM items_delayed WHERE owner_id=? AND payment_status=0");
				st.setInt(1, player_id);
				rset = st.executeQuery();

				L2ItemInstance item, newItem;
				st_delete = con.prepareStatement("UPDATE items_delayed SET payment_status=1 WHERE payment_id=?");

				while(rset.next())
				{
					final int ITEM_ID = rset.getShort("item_id");
					final long ITEM_COUNT = rset.getLong("count");
					final short ITEM_ENCHANT = rset.getShort("enchant_level");
					final int PAYMENT_ID = rset.getInt("payment_id");
					final int FLAGS = rset.getInt("flags");
					final byte ATTRIBUTE = rset.getByte("attribute");
					final int ATTRIBUTE_LEVEL = rset.getInt("attribute_level");
					boolean stackable = ItemTable.getInstance().getTemplate(ITEM_ID).isStackable();
					boolean success = false;

					for(int i = 0; i < (stackable ? 1 : ITEM_COUNT); i++)
					{
						item = ItemTable.getInstance().createItem(ITEM_ID);
						if(item.isStackable())
							item.setCount(ITEM_COUNT);
						else
						{
							item.setEnchantLevel(ITEM_ENCHANT);
							item.setAttributeElement(ATTRIBUTE, ATTRIBUTE_LEVEL, true);
						}
						item.setLocation(ItemLocation.INVENTORY);
						item.setCustomFlags(FLAGS, false);

						// При нулевом количестве выдача предмета не производится
						if(ITEM_COUNT > 0)
						{
							newItem = inv.addItem(item);
							if(newItem == null)
							{
								_log.warning("Unable to delayed create item " + ITEM_ID + " request " + PAYMENT_ID);
								continue;
							}
							newItem.updateDatabase(true, false);
						}

						success = true;
						restored_counter++;
						if(notify && ITEM_COUNT > 0)
							player.sendPacket(SystemMessage.obtainItems(ITEM_ID, stackable ? ITEM_COUNT : 1, ITEM_ENCHANT));
					}
					if(!success)
						continue;

					Log.add("<add owner_id=" + player_id + " item_id=" + ITEM_ID + " count=" + ITEM_COUNT + " enchant_level=" + ITEM_ENCHANT + " payment_id=" + PAYMENT_ID + "/>", "delayed_add");

					st_delete.setInt(1, PAYMENT_ID);
					st_delete.execute();
				}
			}
			catch(Exception e)
			{
				_log.log(Level.WARNING, "could not load delayed items for player " + player.getName() + ":", e);
			}
			finally
			{
				DatabaseUtils.closeStatement(st_delete);
				DatabaseUtils.closeDatabaseCSR(con, st, rset);
			}
		}
		return restored_counter;
	}
}