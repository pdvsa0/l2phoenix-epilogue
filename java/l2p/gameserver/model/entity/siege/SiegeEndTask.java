package l2p.gameserver.model.entity.siege;

import java.util.Calendar;

import l2p.common.ThreadPoolManager;
import l2p.gameserver.serverpackets.SystemMessage;

public class SiegeEndTask implements Runnable
{
	private final Siege _siege;

	public SiegeEndTask(Siege siege)
	{
		_siege = siege;
	}

	public void run()
	{
		if(!_siege.isInProgress())
			return;

		try
		{
			long timeRemaining = _siege.getSiegeEndDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
			if(timeRemaining > 3600000)
				ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(_siege), timeRemaining - 3600000); // Prepare task for 1 hr left.
			else if(timeRemaining <= 3600000 && timeRemaining > 600000)
			{
				_siege.announceToPlayer(new SystemMessage(SystemMessage.S1_MINUTE_S_UNTIL_CASTLE_SIEGE_CONCLUSION).addNumber(Math.round(timeRemaining / 60000) + 1), false, false);
				ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(_siege), timeRemaining - 600000); // Prepare task for 10 minute left.
			}
			else if(timeRemaining <= 600000 && timeRemaining > 300000)
			{
				_siege.announceToPlayer(new SystemMessage(SystemMessage.S1_MINUTE_S_UNTIL_CASTLE_SIEGE_CONCLUSION).addNumber(Math.round(timeRemaining / 60000) + 1), false, false);
				ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(_siege), timeRemaining - 300000); // Prepare task for 5 minute left.
			}
			else if(timeRemaining <= 300000 && timeRemaining > 10000)
			{
				_siege.announceToPlayer(new SystemMessage(SystemMessage.S1_MINUTE_S_UNTIL_CASTLE_SIEGE_CONCLUSION).addNumber(Math.round(timeRemaining / 60000) + 1), false, false);
				ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(_siege), timeRemaining - 10000); // Prepare task for 10 seconds count down
			}
			else if(timeRemaining <= 10000 && timeRemaining > 0)
			{
				_siege.announceToPlayer(new SystemMessage(SystemMessage.CASTLE_SIEGE_S1_SECOND_S_LEFT).addNumber(Math.round(timeRemaining / 1000) + 1), false, false);
				ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(_siege), timeRemaining); // Prepare task for second count down
			}
			else
				_siege.endSiege();
		}
		catch(Throwable t)
		{}
	}
}