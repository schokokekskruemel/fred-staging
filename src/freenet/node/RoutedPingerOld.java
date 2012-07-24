package freenet.node;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

public class RoutedPingerOld implements Runnable {

	int ONLINE = 1;
	int OFFLINE = 0;
	Node node;
	static RoutedPingerOld singlePinger;
	double ownLocation;

	private RoutedPingerOld(Node node) {
		this.node = node;
		ownLocation = this.node.lm.getLocation();
		System.out.println("RoutedPinger created.");
	}
	
	private RoutedPingerOld() {
		this.node = null;
	}
	
	public static RoutedPingerOld getRoutedPinger(Node node) {
		if (singlePinger == null) {
			if (node == null) {
				singlePinger = new RoutedPingerOld();
			} else {
				singlePinger = new RoutedPingerOld(node);
			}
		}
		return singlePinger;
	}

	@Override
	public void run() {
		try {
			System.out.println("Start RoutedPinger...");
			Thread.sleep(300000);
			HashMap<Double, HashMap<Long, Integer>> churn = new HashMap<Double, HashMap<Long, Integer>>();
			HashMap<Double, HashMap<Long, Integer>> distances = new HashMap<Double, HashMap<Long, Integer>>();
			TreeSet<Long> timer = new TreeSet<Long>();

			while (true) {
				
				maybePingNodes(churn, distances, timer);
	//				if ((timer.size() % 5) == 0) {
					writeChurnDataToFile(churn, distances, timer);
	//				}
				Thread.sleep(2000);
				
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void maybePingNodes(HashMap<Double, HashMap<Long, Integer>> churn,
			HashMap<Double, HashMap<Long, Integer>> distances,
			TreeSet<Long> timer) {
		
		
		
		pingNodes(churn, distances, timer);
	}

	/**
	 * Reads file with all seen locations and pings them using the FNPRoutedPing
	 * message. It writes all results in the text file "Churn.txt"
	 */
	public void pingNodes(HashMap<Double, HashMap<Long, Integer>> churn,
			HashMap<Double, HashMap<Long, Integer>> distances,
			TreeSet<Long> timer) {

		Vector<RoutedPingerThread> runningThreads = new Vector<RoutedPingerThread>();
		
		long time = System.currentTimeMillis();
		timer.add(time);

		double intervalStart = ownLocation - 0.05;
		double intervalEnd = ownLocation + 0.05;
		if (intervalStart < 0) {
			intervalStart = 1 + intervalStart;
		}
		if (intervalEnd > 1) {
			intervalEnd = intervalEnd -1;
		}
		
		HashMap<Double, byte[]> locations = readLocations("locations.txt");
		Iterator<Double> keys = locations.keySet().iterator();
		while (keys.hasNext()) {			
			boolean intervalFlag = false;
			double location = keys.next();
			if (Math.abs(location - ownLocation)<= Double.MIN_VALUE) {
				continue;
			}
//			if ((ownLocation > 0.95) || (ownLocation < 0.05)) {
//				intervalFlag = ((location > intervalStart) || (location < intervalEnd));
//			} else {
//				intervalFlag = ((location > intervalStart) && (location < intervalEnd));
//			}
//			if (!intervalFlag){
//				continue;
//			}
			
			RoutedPingerThread newThread = new RoutedPingerThread(location, locations.get(location), node);
			runningThreads.add(newThread);
			node.executor.execute(newThread);
			
			while (runningThreads.size() >= 4) {
				for (RoutedPingerThread thread : runningThreads) {
					if (thread.isDone()) {
						double currentLoc = thread.getLocation();
						int distance = thread.getResult();
						System.out.println("Ping for location " + currentLoc + ", distance " + distance);
						HashMap<Long, Integer> collectedDistances = new HashMap<Long, Integer>();
						if (distances.containsKey(currentLoc)) {
							collectedDistances = distances.get(currentLoc);
						}
						collectedDistances.put(time, distance);
						distances.put(currentLoc, collectedDistances);

						HashMap<Long, Integer> collectedChurn = new HashMap<Long, Integer>();
						if (churn.containsKey(currentLoc)) {
							collectedChurn = churn.get(currentLoc);
						}
						if (distance > 0) {
							// node with location found, is online
							collectedChurn.put(time, ONLINE);
						}
						if (distance < 0) {
							// node with location not found/timeout, is offline
							collectedChurn.put(time, OFFLINE);
						}
						if (distance == 0) {
							// own location
						}
						
						churn.put(currentLoc, collectedChurn);
						runningThreads.remove(thread);
						break;
					}
				}
			}
			
		}
		
		for (RoutedPingerThread thread : runningThreads) {
			while (!thread.isDone()) {
//				wait
			}
			double currentLoc = thread.getLocation();
			int distance = thread.getResult();
			System.out.println("Ping for location " + currentLoc + ", distance " + distance);
			HashMap<Long, Integer> collectedDistances = new HashMap<Long, Integer>();
			if (distances.containsKey(currentLoc)) {
				collectedDistances = distances.get(currentLoc);
			}
			collectedDistances.put(time, distance);
			distances.put(currentLoc, collectedDistances);

			HashMap<Long, Integer> collectedChurn = new HashMap<Long, Integer>();
			if (churn.containsKey(currentLoc)) {
				collectedChurn = churn.get(currentLoc);
			}
			if (distance > 0) {
				// node with location found, is online
				collectedChurn.put(time, ONLINE);
			}
			if (distance < 0) {
				// node with location not found/timeout, is offline
				collectedChurn.put(time, OFFLINE);
			}
			if (distance == 0) {
				// own location
			}
			
			churn.put(currentLoc, collectedChurn);
		}
		
	}
	
	/**
	 * reads Locations from the given destination
	 * 
	 * @return
	 */
	public HashMap<Double, byte[]> readLocations(String filename) {
		HashMap<Double, byte[]> locations = new HashMap<Double, byte[]>();
		try {
			FileInputStream fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = br.readLine()) != null) {
				try {
					if (!line.equals("-1.0")) {
						Double currentLocation = Double.parseDouble(line);
						byte[] identity = br.readLine().getBytes();
						locations.put(currentLocation, identity);	
					} else {
						br.readLine();
					}
				} catch (NumberFormatException e) {
					// line cannot be transformed into a double
				}
			}
			br.close();
			
			System.out.println("found locations: " + locations.keySet().toString());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return locations;
	}

	public void writeChurnDataToFile(
			HashMap<Double, HashMap<Long, Integer>> churn,
			HashMap<Double, HashMap<Long, Integer>> distances,
			TreeSet<Long> timer) {

		try {

			String[][] churnOutput = new String[timer.size()+1][churn.keySet().size()+1];
			Double[] locs = new Double[churn.keySet().size()];
			locs = churn.keySet().toArray(locs);
			churnOutput[0][0] = "0";
			Long[] times = new Long[timer.size()];
			times = timer.toArray(times);
			for (int i = 0; i < locs.length; i++) {
				churnOutput[0][i + 1] = locs[i].toString();
			}
			for (int i = 0; i < times.length; i++) {
				churnOutput[i + 1][0] = times[i].toString();
			}
			for (int i = 1; i <= churn.keySet().size(); i++) {
				for (int j = 1; j <= timer.size(); j++) {
					churnOutput[j][i] = churn
							.get(Double.parseDouble(churnOutput[0][i]))
							.get(Long.parseLong(churnOutput[j][0])).toString();
				}
			}
			
			String[][] distOutput = new String[timer.size()+1][distances.keySet().size()+1];
   			Double[] dists = new Double[distances.keySet().size()];
   			dists = distances.keySet().toArray(dists);
   			distOutput[0][0] = "0";
   			for (int i = 0; i < dists.length; i++) {
   				distOutput[0][i + 1] = dists[i].toString();
   			}
   			for (int i = 0; i < times.length; i++) {
   				distOutput[i + 1][0] = times[i].toString();
   			}
   			for (int i = 1; i <= distances.keySet().size(); i++) {
   				for (int j = 1; j <= timer.size(); j++) {
   					distOutput[j][i] = distances
   							.get(Double.parseDouble(distOutput[0][i]))
   							.get(Long.parseLong(distOutput[j][0])).toString();
   				}
   			}

			FileWriter fw1 = new FileWriter("Churn.txt");
			BufferedWriter outChurn = new BufferedWriter(fw1);
			FileWriter fw2 = new FileWriter("Distances.txt");
			BufferedWriter outDistances = new BufferedWriter(fw2);
			
			for (int i = 0; i < churnOutput.length; i++) {
				StringBuilder line = new StringBuilder();
				for (int j = 0; j< churnOutput[0].length; j++) {
					line.append(churnOutput[i][j] + " ");
				}
				outChurn.write(line.toString() + "\n");
			}
			outChurn.close();
			
			for (int i = 0; i < distOutput.length; i++) {
				StringBuilder line = new StringBuilder();
				for (int j = 0; j< distOutput[0].length; j++) {
					line.append(distOutput[i][j] + " ");
				}
				outDistances.write(line.toString() + "\n");
			}
			
			outDistances.close();

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
