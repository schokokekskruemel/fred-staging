package freenet.client.async;

import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.InserterException;
import freenet.keys.FreenetURI;

/**
 * A client process. Something that initiates requests, and can cancel
 * them. FCP, Fproxy, and the GlobalPersistentClient, implement this
 * somewhere.
 */
public interface ClientCallback {

	public void onSuccess(FetchResult result, ClientGetter state);
	
	public void onFailure(FetchException e, ClientGetter state);

	public void onSuccess(BaseClientPutter state);
	
	public void onFailure(InserterException e, BaseClientPutter state);
	
	public void onGeneratedURI(FreenetURI uri, BaseClientPutter state);
	
}
