package org.wikipedia.vlsergey.secretary.jwpf.model;

import java.net.InetAddress;

public interface User {

	Long getEditcount();

	InetAddress getInetAddress();

	String getName();

	Long getUserId();
}
