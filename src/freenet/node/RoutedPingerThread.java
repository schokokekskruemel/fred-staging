package freenet.node;

public class RoutedPingerThread implements Runnable {

	double location;
	byte[] identity;
	boolean done;
	Node node;
	int result;
	
	public RoutedPingerThread(double location, byte[] identity, Node node) {
		this.location = location;
		this.identity = identity;
		this.node = node;
		done = false;
		result = -1;
	}
	
	@Override
	public void run() {
		result = node.routedPing(location, identity);
		done();
	}
	
	private synchronized void done() {
		done = true;
	}
	
	public synchronized boolean isDone() {
		return done;
	}
	
	public int getResult() {
		return result;
	}
	
	public double getLocation() {
		return location;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
