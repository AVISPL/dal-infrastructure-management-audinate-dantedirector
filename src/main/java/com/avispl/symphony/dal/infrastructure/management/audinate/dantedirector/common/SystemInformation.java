/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.common;

/**
 * Enum representing system information properties for Dante Director.
 * Each property includes a name and a corresponding GraphQL value.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 3/1/2024
 * @since 1.0.0
 */
public enum SystemInformation {
	CLOCKING("Clocking", "clocking"),
	CONNECTIVITY("Connectivity", "connectivity"),
	LATENCY("Latency", "latency"),
	SUBSCRIPTION("Subscriptions", "subscriptions"),
	;
	private final String name;
	private final String value;

	/**
	 * Constructor for SystemInfo.
	 *
	 * @param name The name representing the system information category.
	 * @param value The corresponding value associated with the category.
	 */
	SystemInformation(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves {@link #value}
	 *
	 * @return value of {@link #value}
	 */
	public String getValue() {
		return value;
	}
}
