package nl.moj.server.test;

import java.security.Permission;

public class SelectiveSecurityManager extends SecurityManager {

	@Override
	public void checkPermission(Permission permission) {
		if (shouldCheck(permission)) {
			super.checkPermission(permission);
		}
	}

	@Override
	public void checkPermission(Permission permission, Object context) {
		if (shouldCheck(permission)) {
			super.checkPermission(permission, context);
		}
	}

	@Override
	public void checkExit(int arg0) {
		throw new SecurityException("exit access denied");
		//super.checkExit(arg0);
		
	}

	@Override
	public void checkRead(String arg0) {
		// TODO Auto-generated method stub
		//super.checkRead(arg0);
	}
	
	@Override
	public void checkWrite(String arg0) {
		//throw new SecurityException("write access denied");
		//super.checkWrite(arg0);
	}
	
	private boolean shouldCheck(Permission permission) {
		return false;
	}

}
