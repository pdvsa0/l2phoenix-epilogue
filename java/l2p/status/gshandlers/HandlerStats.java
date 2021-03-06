package l2p.status.gshandlers;

import java.io.PrintWriter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

import l2p.util.StrTable;

public class HandlerStats
{
	public static void Stats(String fullCmd, String[] argv, PrintWriter _print)
	{
		if(argv.length < 2 || argv[1] == null || argv[1].isEmpty())
			_print.println("USAGE: stat pf|obj|compression");
		else if(argv[1].equalsIgnoreCase("pf") || argv[1].equalsIgnoreCase("pathfind"))
			_print.print(l2p.gameserver.geodata.PathFindBuffers.getStats());
		else if(argv[1].equalsIgnoreCase("obj") || argv[1].equalsIgnoreCase("objects"))
			_print.print(l2p.gameserver.model.L2ObjectsStorage.getStats());
		else if(argv[1].equalsIgnoreCase("selector") || argv[1].equalsIgnoreCase("compression"))
			_print.print(l2p.extensions.network.SelectorThread.getStats());
		else if(argv[1].equalsIgnoreCase("gc"))
			_print.print(getGCStats());
	}

	public static StrTable getGCStats()
	{
		StrTable table = new StrTable("Garbage Collectors Stats");
		int i = 0;
		for(GarbageCollectorMXBean gc_bean : ManagementFactory.getGarbageCollectorMXBeans())
		{
			table.set(i, "Name", gc_bean.getName());
			table.set(i, "ColCount", gc_bean.getCollectionCount());
			table.set(i, "ColTime", gc_bean.getCollectionTime());
			i++;
		}
		return table;
	}
}