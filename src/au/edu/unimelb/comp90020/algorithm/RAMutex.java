package au.edu.unimelb.comp90020.algorithm;

import au.edu.unimelb.comp90020.net.Linker;
import au.edu.unimelb.comp90020.net.Msg;
import au.edu.unimelb.comp90020.util.IntLinkedList;

/*
 * The processes p1 ,p2 ... pN bear distinct numeric identifiers. 
 * They are assumed to possess communication channels to one another, 
 * and each process pi keeps a Lamport clock. Execution of the CS requests 
 * in this algorithm is always in the order of their timestamps.
 */
public class RAMutex extends Process implements Lock {

	//Timestamps are used to decide the priority of requests in case of conflict
	LamportClock myClock = new LamportClock();
	IntLinkedList pendingQ = new IntLinkedList();
	int numReplies = 0;

	/*The value Symbols.INFINITY signifies that Pi does not have any 
	 * record of outstanding request by process Pj.
	 */
	public RAMutex(Linker initComm) {
		super(initComm);
		setState(ProcessState.RELEASED); //on initialization
		System.out.println("Hello, I'm process: " + getMyId() + ", my state is: " + getMyState());
	}

	/*
	 * A process broadcasts a timestamped REQUEST message to all other processes to 
	 * request their permission to enter the critical section
	 */
	public synchronized void requestCS() {
		setState(ProcessState.WANTED);
		System.out.println("Hello, I'm process: " + getMyId() + ", my state is: " + getMyState());
		myClock.tick();
		//myts = myClock.getValue();
		broadcastMsg("REQUEST", myClock.getValue());
		numReplies = 0;
		while (numReplies < getNumProcesses() - 1) //Wait until all have replied
			myWait();
		setState(ProcessState.HELD);
		System.out.println("Hello, I'm process: " + getMyId() + ", my state is: " + getMyState());
	}

	/*
	 * When process Pi exits the CS, replies to any queued requests in pendingQ, 
	 * in this case it empties pendingQ at the same time as well.
	 * 
	 */
	public synchronized void releaseCS() {
		//myts = Symbols.INFINITY; //--Is the same as state = RELEASED
		setState(ProcessState.RELEASED);
		System.out.println("Hello, I'm process: " + getMyId() + ", my state is: " + getMyState());
		System.out.println("Hello, I'm process: " + getMyId() + ", my state is: " + getMyState());
		while (!pendingQ.isEmpty()) {
			int pid = pendingQ.removeHead();
			sendMsg(pid, "REPLY", myClock.getValue());
		}
	}

	/*
	 * A process sends a REPLY message to a process to give its permission to that process.
	 */
	public synchronized void handleMsg(Msg m, int src, String tag) {
		//When a site receives a message, it updates its clock using the timestamp in the message
		int timeStamp = m.getMessageInt();
		
		//When a site takes up a request for the CS for processing, it updates its local clock and assigns a timestamp to the request.
		myClock.receiveAction(src, timeStamp);
		
		/*
		 * When process Pj receives a REQUEST message from process Pi, it sends a REPLY message to process Pi 
		 * if process Pj is neither requesting nor executing the CS, or if the process Pj is requesting 
		 * and Pi’s request’s timestamp is smaller than process Pj’s own request’s timestamp. 
		 * Otherwise, the reply is deferred and Pj updates its pendingQ.
		 */
		if (tag.equals("REQUEST")) {
			//We do tie break by using ID's as well
			if ((getMyState() == ProcessState.RELEASED) || (timeStamp < myClock.getValue()) || ((timeStamp == myClock.getValue()) && (src < getMyId())))
				sendMsg(src, "REPLY", myClock.getValue()); //Reply immediately
			else
				pendingQ.add(src); //Queue request without replying
		} else if (tag.equals("REPLY")) { //I got permission from process Pj
			numReplies++;
			if (numReplies == getNumProcesses() - 1) //N-1
				notify(); // Execute CS only when all processes have answered with a REPLY
		}
	}
}
