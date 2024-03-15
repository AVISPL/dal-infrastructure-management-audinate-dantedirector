/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.common;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum representing aggregated information properties for devices.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 3/2/2024
 * @since 1.0.0
 */
public enum AggregatedInformation {
	MANUFACTURER("Manufacturer", ""),
	PRODUCT_VERSION("ProductVersion", ""),
	CONNECTED_SINCE("ConnectedSince(GMT)", ""),
	ENROLMENT_STATE("EnrolmentState", ""),
	CLOCKING("Clocking", DanteDirectorConstant.STATUS_GROUP),
	CONNECTIVITY("Connectivity", DanteDirectorConstant.STATUS_GROUP),
	LATENCY("Latency", DanteDirectorConstant.STATUS_GROUP),
	SUBSCRIPTIONS("Subscriptions", DanteDirectorConstant.STATUS_GROUP),
	LOCATION("Location", ""),
	DESCRIPTION("Description", ""),
	COMMENTS("Comments", ""),
	DISCOVERY_TYPE("DiscoveryType", ""),
	DISCOVERY_DOMAIN_NAME("DiscoveryDomainName", ""),
	IP_ADDRESS("IPAddress", ""),
	MAC_ADDRESS("MACAddress", ""),
	DANTE_SOFTWARE_VERSION("DanteSoftwareVersion", ""),
	DANTE_VERSION("DanteVersion", ""),
	SITE_NAME("SiteName", ""),
	MUTE_STATUS("MuteStatus", DanteDirectorConstant.CLOCK_SYNCHRONISATION_GROUP),
	SYNC_STATUS("SyncStatus", DanteDirectorConstant.CLOCK_SYNCHRONISATION_GROUP),
	DOMAIN_CLOCKING("DomainClocking", DanteDirectorConstant.CLOCK_SYNCHRONISATION_GROUP),
	PRIMARY_MULTICAST("PrimaryMulticast", DanteDirectorConstant.CLOCK_SYNCHRONISATION_GROUP),
	UNICAST("Unicast", DanteDirectorConstant.CLOCK_SYNCHRONISATION_GROUP),
	EXTERNAL_WORD_CLOCK("SyncToExternalWordClock", DanteDirectorConstant.CLOCK_SYNCHRONISATION_GROUP),
	LEADER("PreferredLeader", DanteDirectorConstant.CLOCK_SYNCHRONISATION_GROUP),
	UNICAST_CLOCKING("UnicastClocking", DanteDirectorConstant.CLOCK_SYNCHRONISATION_GROUP),
	DELAY_REQUEST("V1DelayRequests", DanteDirectorConstant.CLOCK_SYNCHRONISATION_GROUP),
	FREQUENCY("FrequencyOffset(ppm)", DanteDirectorConstant.CLOCK_SYNCHRONISATION_GROUP),
	RECEIVE_CHANNELS("ReceiveChannels", DanteDirectorConstant.CLOCK_SYNCHRONISATION_GROUP),
	;
	private final String name;
	private final String group;

	/**
	 * Constructs an AggregatedInformation with the specified name and group.
	 *
	 * @param name The name of the information property.
	 * @param group The group associated with the information property.
	 */
	AggregatedInformation(String name, String group) {
		this.name = name;
		this.group = group;
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
	 * Retrieves {@link #group}
	 *
	 * @return value of {@link #group}
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * Retrieve a AggregatedInformation by its name.
	 *
	 * @param name The default name to search for.
	 * @return The AggregatedInformation with the specified default name, or null if not found.
	 */
	public static AggregatedInformation getByDefaultName(String name) {
		Optional<AggregatedInformation> property = Arrays.stream(AggregatedInformation.values()).filter(item -> item.getName().equalsIgnoreCase(name)).findFirst();
		return property.orElse(null);
	}
}
