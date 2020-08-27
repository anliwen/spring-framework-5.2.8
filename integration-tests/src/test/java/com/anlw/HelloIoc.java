package com.anlw;

public class HelloIoc {
	
	private String myName;
	
	public void show() {
		System.out.println("Welcome to Spring~~baby~~" + myName);
	}

	public String getMyName() {
		return myName;
	}

	public void setMyName(String myName) {
		this.myName = myName;
	}
	
	
}
