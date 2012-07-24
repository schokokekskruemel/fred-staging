package freenet.node;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import freenet.support.Logger;

public class RoutedPinger implements Runnable {

	int ONLINE = 1;
	int OFFLINE = 0;
	Node node;
	final String doneName = "donePinger";
	final String doneClient = "doneClient";
	private double ownLocation;
	private static RoutedPinger singlePinger;
	private final String INPUT_FILENAME = "locations.txt";

	private RoutedPinger(Node node) {
		this.node = node;
		ownLocation = node.lm.getLocation();
		System.out.println("RoutedPinger created.");
	}

	private RoutedPinger() {
		this.node = null;
	}

	public static RoutedPinger getRoutedPinger(Node node) {
		if (singlePinger == null) {
			if (node == null) {
				singlePinger = new RoutedPinger();
			} else {
				singlePinger = new RoutedPinger(node);
			}
		}
		return singlePinger;
	}

	@Override
	public void run() {
		try {
			System.out.println("Start RoutedPinger...");
			// Sleep in the beginning to give the node time to startup and look
			// for neighbors.
			// Without the waiting, the pinging can interfere with other
			// processes and the initial results will be always negative.
			Thread.sleep(300000);
			HashMap<Double, Integer> churn = new HashMap<Double, Integer>();
			HashMap<Double, Integer> distances = new HashMap<Double, Integer>();

			while (true) {
				maybePingNodes(churn, distances);
				Thread.sleep(2000);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void maybePingNodes(HashMap<Double, Integer> churn,
			HashMap<Double, Integer> distances) {

		boolean newTask = checkTask();
		if (newTask) {
			pingNodes(churn, distances);
			writeChurnDataToFile(churn, distances);
			churn.clear();
			distances.clear();
		}
	}

	public boolean checkTask() {
		File inputFile = new File(INPUT_FILENAME);
		if ((System.currentTimeMillis() - inputFile.lastModified()) < 10000) {
			File doneClientFile = new File(doneClient);
			while (!doneClientFile.exists()) {
				
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Reads file with all seen locations and pings them using the FNPRoutedPing
	 * message. It writes all results in the text file "Churn.txt"
	 */
	public void pingNodes(HashMap<Double, Integer> churn,
			HashMap<Double, Integer> distances) {
		System.out.println("Start pinging nodes...");

		Vector<RoutedPingerThread> runningThreads = new Vector<RoutedPingerThread>();

		HashMap<Double, byte[]> locations = readLocations(INPUT_FILENAME);
		Iterator<Double> keys = locations.keySet().iterator();
		while (keys.hasNext()) {
			double location = keys.next();
			// If own location: can tell without ping that is online. Distance
			// is 0, since it doesn't have to be pinged
			if (Math.abs(location - ownLocation) <= Double.MIN_VALUE) {
				distances.put(location, 0);
				churn.put(location, ONLINE);
			}

			RoutedPingerThread newThread = new RoutedPingerThread(location,
					locations.get(location), node);
			runningThreads.add(newThread);
			node.executor.execute(newThread);
			
			while (runningThreads.size() >= 5) {
				for (RoutedPingerThread thread : runningThreads) {
					if (thread.isDone()) {
						double currentLoc = thread.getLocation();
						int distance = thread.getResult();
						System.out.println("Ping for location " + currentLoc
								+ ", distance " + distance);
						distances.put(currentLoc, distance);
						int churnValue = OFFLINE;
						if (distance >= 0) {
							// node with location found, is online
							churnValue = ONLINE;
						}

						churn.put(currentLoc, churnValue);
						runningThreads.remove(thread);
						break;
					}
				}
			}

		}

		for (RoutedPingerThread thread : runningThreads) {
			while (!thread.isDone()) {
				// wait
			}
			double currentLoc = thread.getLocation();
			int distance = thread.getResult();
			System.out.println("Ping for location " + currentLoc
					+ ", distance " + distance);
			distances.put(currentLoc, distance);

			int churnValue = OFFLINE;
			if (distance >= 0) {
				// node with location found, is online
				churnValue = ONLINE;
			}
			churn.put(currentLoc, churnValue);
		}
		runningThreads.clear();
		System.out.println("Pinging done.");
	}

	/**
	 * reads Locations from the given destination
	 * 
	 * @return
	 */
	public HashMap<Double, byte[]> readLocations(String filename) {
		HashMap<Double, byte[]> locations = new HashMap<Double, byte[]>();
		try {
			FileReader reader = new FileReader(filename);
			BufferedReader br = new BufferedReader(reader);
			String line = br.readLine();
			while (line != null) {
				try {
					if (line.contains("0.")) {
						Double currentLocation = Double.parseDouble(line);
						String id = br.readLine();
						if (id != null) {
							byte[] identity = id.getBytes();
							locations.put(currentLocation, identity);	
						}
					}
				} catch (NumberFormatException e) {
					// line cannot be transformed into a double
					e.printStackTrace();
				}
				line = br.readLine();
			}
			br.close();

			System.out.println("found locations: "
					+ locations.keySet().toString());

		} catch (IOException e) {
			e.printStackTrace();
		}
		return locations;
	}

	public void writeChurnDataToFile(HashMap<Double, Integer> churn,
			HashMap<Double, Integer> distances) {

		try {

			FileWriter fw1 = new FileWriter("output.txt");
			BufferedWriter out = new BufferedWriter(fw1);

			TreeSet<Double> sortedLocations = new TreeSet<Double>(
					churn.keySet());
			for (double loc : sortedLocations) {
				out.write(loc + " " + churn.get(loc) + " " + distances.get(loc)
						+ "\n");
			}
			out.close();
			
			FileWriter writer = new FileWriter(doneName);
			BufferedWriter outDone = new BufferedWriter(writer);
			outDone.write("done");
			outDone.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
