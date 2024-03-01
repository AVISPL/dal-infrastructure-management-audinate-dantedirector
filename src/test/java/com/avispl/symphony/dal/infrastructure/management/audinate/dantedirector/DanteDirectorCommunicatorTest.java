/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * NaViSetAdministrator2SECommunicatorTest
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 9/8/2023
 * @since 1.0.0
 */
public class DanteDirectorCommunicatorTest {
	private DanteDirectorCommunicator danteDirectorCommunicator;

	@BeforeEach
	void setUp() throws Exception {
		danteDirectorCommunicator = new DanteDirectorCommunicator();
		danteDirectorCommunicator.setHost("");
		danteDirectorCommunicator.setLogin("");
		danteDirectorCommunicator.setPassword("");
		danteDirectorCommunicator.setPort(80);
		danteDirectorCommunicator.init();
		danteDirectorCommunicator.connect();
	}

	@AfterEach
	void destroy() throws Exception {
		danteDirectorCommunicator.disconnect();
		danteDirectorCommunicator.destroy();
	}
}
