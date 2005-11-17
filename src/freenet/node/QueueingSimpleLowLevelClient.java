package freenet.node;

import freenet.client.InsertBlock;
import freenet.keys.ClientCHKBlock;
import freenet.keys.ClientKey;
import freenet.keys.KeyBlock;

interface QueueingSimpleLowLevelClient extends SimpleLowLevelClient {

	/** Unqueued version. Only call from QueuedDataRequest ! */
	KeyBlock realGetKey(ClientKey key, boolean localOnly) throws LowLevelGetException;

	/** Ditto */
	void realPutCHK(ClientCHKBlock block) throws LowLevelPutException;

}
