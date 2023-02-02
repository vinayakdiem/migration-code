
package com.diemlife.providers;


public class LoginUsernamePasswordAuthUser extends MyUsernamePasswordAuthUser {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The session timeout in seconds
	 * Defaults to two weeks
	 */
	final static long SESSION_TIMEOUT = 24L * 14 * 36000;
	private long expiration;

	/**
	 * For logging the user in automatically
	 *
	 * @param email
	 */
	public LoginUsernamePasswordAuthUser(String email) {
		this(null, email);
	}

	public LoginUsernamePasswordAuthUser(String clearPassword, String email) {
		super(clearPassword, email);

		expiration = System.currentTimeMillis() + 1000 * SESSION_TIMEOUT;
	}

	@Override
	public long expires() {
		return expiration;
	}

	@Override
	public String getId() {
		return super.getEmail();
	}

}