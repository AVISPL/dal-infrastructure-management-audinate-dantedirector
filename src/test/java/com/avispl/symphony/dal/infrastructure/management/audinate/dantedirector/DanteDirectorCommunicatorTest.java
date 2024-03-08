/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;

/**
 * NaViSetAdministrator2SECommunicatorTest
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 9/8/2023
 * @since 1.0.0
 */
public class DanteDirectorCommunicatorTest {
	private ExtendedStatistics extendedStatistic;
	private DanteDirectorCommunicator danteDirectorCommunicator;

	@BeforeEach
	void setUp() throws Exception {
		danteDirectorCommunicator = new DanteDirectorCommunicator();
		danteDirectorCommunicator.setHost("");
		danteDirectorCommunicator.setLogin("");
		danteDirectorCommunicator.setPassword("");
		danteDirectorCommunicator.setPort(443);
		danteDirectorCommunicator.init();
		danteDirectorCommunicator.connect();
	}

	@AfterEach
	void destroy() throws Exception {
		danteDirectorCommunicator.disconnect();
		danteDirectorCommunicator.destroy();
	}

	@Test
	void testGetAggregatorData() throws Exception {
		extendedStatistic = (ExtendedStatistics) danteDirectorCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		List<AdvancedControllableProperty> advancedControllableProperties = extendedStatistic.getControllableProperties();
		Assert.assertEquals(6, statistics.size());
		Assert.assertEquals(0, advancedControllableProperties.size());
	}
}
