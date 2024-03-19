/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.common;

/**
 * Class containing GraphQL queries used in Dante Director communication.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 3/1/2024
 * @since 1.0.0
 */
public class DanteDirectorQuery {
	public static final String SYSTEM_INFO = "{\"query\":\"query Domains "
			+ "{ domains {    "
			+ "name    id  "
			+ "devices { id }   "
			+ "status { clocking  connectivity  latency  subscriptions summary }   } }\"}";

	public static final String DEVICES_INFO = "{\"query\":\"query Devices "
			+ "{ domains {  id name "
			+ "devices { "
			+ "id  name  enrolmentState  comments description  location "
			+ "domain { name } "
			+ "connection { state lastChanged }  "
			+ "discovery { type fqdn } "
			+ "identity { productModelName productVersion danteHardwareVersion productSoftwareVersion danteVersion } "
			+ "manufacturer { name } "
			+ "interfaces { address macAddress subnet netmask} "
			+ "capabilities { CAN_WRITE_UNICAST_DELAY_REQUESTS  CAN_WRITE_PREFERRED_MASTER  CAN_WRITE_EXT_WORD_CLOCK  CAN_UNICAST_CLOCKING   } "
			+ "status { clocking connectivity latency subscriptions summary }  "
			+ "rxChannels { mediaType  name  subscribedChannel  subscribedDevice  } "
			+ "clockingState { followerWithoutLeader frequencyOffset grandLeader locked multicastLeader muteStatus unicastFollower unicastLeader } "
			+ "clockPreferences { externalWordClock leader unicastClocking v1UnicastDelayRequests } } } }\"}";

	public static final String CONTROL_CLOCK_SYNC = "{\"query\":\"mutation ControlCommand($input: %s!) "
			+ "{ %s(input: $input) "
			+ "{ ok } }\","
			+ "\"variables\": {"
			+ "\"input\" : "
			+ "{\"deviceId\":\"%s\", \"enabled\":%s}}}";

	public static final String CONTROL_SITE = "{\"query\":\"mutation DevicesEnroll($input: DevicesEnrollInput!) "
			+ "{ DevicesEnroll(input: $input) "
			+ "{ ok } }\","
			+ "\"variables\": {"
			+ "\"input\" : "
			+ "{\"deviceIds\": [\"%s\"], \"domainId\":\"%s\"}}}";
}
