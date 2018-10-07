package sunlabs.brazil.server;

import java.io.IOException;

public class HelloHandler implements Handler {

	public boolean init(Server server, String prefix) {
		// TODO Auto-generated method stub
		return true;
	}

	public boolean respond(Request request) throws IOException {
		request.sendResponse("Hello World") ;
		return false;
	}

}
