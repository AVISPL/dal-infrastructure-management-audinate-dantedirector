/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.common;

/**
 * Class containing constant values used in Dante Director communication.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 3/1/2024
 * @since 1.0.0
 */
public class DanteDirectorConstant {
	public static final String URL = "graphql";
	public static final String HASH = "#";
	public static final String MODEL_MAPPING_AGGREGATED_DEVICE = "dante/model-mapping.yml";
	public static final String NONE = "None";
	public static final String SPACE = " ";
	public static final String EMPTY = "";
	public static final String TRUE = "true";
	public static final String FALSE = "false";
	public static final String ON = "On";
	public static final String OFF = "Off";
	public static final String NUMBER_ONE = "1";
	public static final String ZERO = "0";
	public static final String CLOCK_SYNCHRONISATION_GROUP = "ClockSynchronisation#";
	public static final String STATUS_GROUP = "Status#";
	public static final String RECEIVE_CHANNEL_GROUP = "ReceiveChannels#";
	public static final String DEFAULT_FORMAT_DATETIME = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	public static final String TARGET_FORMAT_DATETIME = "MMM d, yyyy, h:mm a";
	public static final String SITE_NAME = "SiteName";
	public static final String CAPABILITY = "Capability";
	public static final String ERRORS = "errors";
	public static final String DATA = "data";
	public static final String DOMAINS = "domains";
	public static final String NAME = "name";
	public static final String STATUS = "status";
	public static final String DEVICES = "devices";
	public static final String ID = "id";
	public static final String EXTENSIONS = "extensions";
	public static final String CODE = "code";
	public static final String MESSAGE = "message";

	// Adapter metadata
	public static final String MONITORING_CYCLE_DURATION = "LastMonitoringCycleDuration(sec)";
	public static final String ADAPTER_VERSION = "AdapterVersion";
	public static final String MONITORED_DEVICES_TOTAL = "MonitoredDevicesTotal";
	public static final String ADAPTER_BUILD_DATE = "AdapterBuildDate";
	public static final String ADAPTER_UPTIME_MIN = "AdapterUptime(min)";
	public static final String ADAPTER_UPTIME = "AdapterUptime";
	public static final String SYSTEM_MONITORING_CYCLE = "MonitoringCycleInterval(min)";
}
