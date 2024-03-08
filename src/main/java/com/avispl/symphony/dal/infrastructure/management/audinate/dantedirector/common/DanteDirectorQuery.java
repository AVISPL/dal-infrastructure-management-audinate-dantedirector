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
}
