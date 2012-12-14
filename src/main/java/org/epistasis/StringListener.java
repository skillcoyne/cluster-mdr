package org.epistasis;

import java.util.EventListener;

public interface StringListener extends EventListener {
	public void stringReceived(String s);
}
