package com.diemlife.providers;

import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser;
import com.feth.play.module.pa.user.NameIdentity;
import forms.AdminEditForm;
import forms.RegistrationForm;
import com.diemlife.providers.MyUsernamePasswordAuthProvider.MySignup;
import com.diemlife.security.PasswordHasher;

public class MyUsernamePasswordAuthUser extends UsernamePasswordAuthUser implements NameIdentity {

	/**
	 *
	 */
	private static Integer userId;
	private static final long serialVersionUID = 1L;
	private final transient String password;
	private final String email;
	private final String name;
	private final String firstName;
	private final String lastName;
	private final String goals;
	private final String zip;
	private final String receiveEmail;
	private PasswordHasher hasher;

	public MyUsernamePasswordAuthUser(RegistrationForm signup) {
		super(signup.getPassword1(), signup.getEmail());
		this.password = signup.getPassword1();
		this.email = signup.getEmail();
		this.name = signup.getFirstName() + " " + signup.getLastName();
		this.firstName = signup.getFirstName();
		this.lastName = signup.getLastName();
		this.goals = signup.getGoals();
		this.zip = signup.getZip();
		this.receiveEmail = signup.getReceiveEmail();
		this.hasher = new PasswordHasher(this.password);
	}

	public MyUsernamePasswordAuthUser(AdminEditForm signup) {
		super(signup.password1, signup.email);
		this.password = signup.password1;
		this.email = signup.email;
		this.name = signup.firstName + " " + signup.lastName;
		this.firstName = signup.firstName;
		this.lastName = signup.lastName;
		this.goals = signup.goals;
		this.zip = signup.zip;
		this.receiveEmail = signup.receiveEmail;
		this.hasher = new PasswordHasher(this.password);
	}

	public MyUsernamePasswordAuthUser(MySignup signup) {
		super(signup.password, signup.email);
		this.password = signup.password;
		this.email = signup.email;
		this.name = signup.name;
		this.firstName = signup.firstName;
		this.lastName = signup.lastName;
		this.goals = signup.goals;
		this.zip = signup.zip;
		this.receiveEmail = signup.receiveEmail;
		this.hasher = new PasswordHasher(this.password);
	}

	/**
	 * Used for password reset only - do not use this to signup a user!
	 * @param password
	 */
	public MyUsernamePasswordAuthUser(String password) {
		super(password, null);
		this.password = password;
		this.email = null;
		this.name = null;
		this.hasher = new PasswordHasher(this.password);
		this.firstName = null;
		this.lastName = null;
		this.goals = null;
		this.zip = null;
		this.receiveEmail = null;
	}

	/**
	 * Used for password reset only - do not use this to signup a user!
	 * @param password
	 */
	public MyUsernamePasswordAuthUser(String password, String email) {
		super(password, email);
		this.password = password;
		this.email = email;
		this.name = null;
		this.hasher = new PasswordHasher(this.password);
		this.firstName = null;
		this.lastName = null;
		this.goals = null;
		this.zip = null;
		this.receiveEmail = null;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return this.getHashedPassword();
	}

	@Override
	protected String createPassword(String clearString) {
		// TODO Auto-generated method stub
		this.hasher = new PasswordHasher(clearString);
		return hasher.getSha256hex() + "#" + hasher.getSalt();
	}

	@Override
	public String getEmail() {
		// TODO Auto-generated method stub
		return this.email;
	}

	@Override
	public String getPassword() {
		// TODO Auto-generated method stub
		return this.password;
	}

	/*@Override
	public String getProvider() {
		// TODO Auto-generated method stub
		return super.getProvider();
	}*/

	@Override
	public String getHashedPassword() {
		// TODO Auto-generated method stub
		return hasher.getSha256hex() + "#" + hasher.getSalt();
	}

	@Override
	public boolean checkPassword(String hashed, String candidate) {
		if(hashed == null || candidate == null) {
			return false;
		}

		String[] hashSalt = hashed.split("#");
		String storedSha256hex = hashSalt[0];
		String storedSalt = hashSalt[1];

		return hasher.checkPassword(storedSha256hex, storedSalt, candidate);
	}

	public String getFirstName() {
		return this.firstName;
	}

	public  String getLastName() {
		return this.lastName;
	}

	public String getGoals() {
		return this.goals;
	}

	public String getZip() {
		return this.zip;
	}

	public String getReceiveEmail() {
		return this.receiveEmail;
	}
	
	public Integer getUserId() {
		return this.userId;
	}
	
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
}