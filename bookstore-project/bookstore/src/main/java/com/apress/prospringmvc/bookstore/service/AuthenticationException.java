package com.apress.prospringmvc.bookstore.service;

/**
 * Thrown when username or password are incorect
 * 
 * @author Marten Deinum
 * @author Koen Serneels
 * 
 */
public class AuthenticationException extends Exception {

	private String code;

	public AuthenticationException(String message, String code) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return this.code;
	}

}