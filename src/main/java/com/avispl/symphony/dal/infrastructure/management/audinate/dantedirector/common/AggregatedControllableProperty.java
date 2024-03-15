/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.common;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum representing controllable properties for aggregated devices.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 3/7/2024
 * @since 1.0.0
 */
public enum AggregatedControllableProperty {
	EXTERNAL_WORD_CLOCK("SyncToExternalWordClock", "DeviceClockingSyncToExternalSet", "DeviceClockingSyncToExternalSetInput"),
	LEADER("PreferredLeader", "DeviceClockingPreferredLeaderSet", "DeviceClockingPreferredLeaderSetInput"),
	UNICAST_CLOCKING("UnicastClocking", "DeviceClockingUnicastSet", "DeviceClockingUnicastSetInput"),
	DELAY_REQUEST("V1DelayRequests", "DeviceClockingPTPV1UnicastDelayRequestSet", "DeviceClockingPTPV1UnicastDelayRequestSetInput"),
	;
	private final String name;
	private final String commandName;
	private final String commandParam;

	/**
	 * Constructs an AggregatedControllableProperty with the specified name, command name, and command parameter.
	 *
	 * @param name The name of the controllable property.
	 * @param commandName The command name associated with the property.
	 * @param commandParam The command parameter associated with the property.
	 */
	AggregatedControllableProperty(String name, String commandName, String commandParam) {
		this.name = name;
		this.commandName = commandName;
		this.commandParam = commandParam;
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
	 * Retrieves {@link #commandName}
	 *
	 * @return value of {@link #commandName}
	 */
	public String getCommandName() {
		return commandName;
	}

	/**
	 * Retrieves {@link #commandParam}
	 *
	 * @return value of {@link #commandParam}
	 */
	public String getCommandParam() {
		return commandParam;
	}

	/**
	 * Retrieve a AggregatedControllableProperty by its name.
	 *
	 * @param name The default name to search for.
	 * @return The AggregatedControllableProperty with the specified default name, or null if not found.
	 */
	public static AggregatedControllableProperty getByDefaultName(String name) {
		Optional<AggregatedControllableProperty> property = Arrays.stream(AggregatedControllableProperty.values()).filter(item -> item.getName().equalsIgnoreCase(name)).findFirst();
		return property.orElse(null);
	}
}
