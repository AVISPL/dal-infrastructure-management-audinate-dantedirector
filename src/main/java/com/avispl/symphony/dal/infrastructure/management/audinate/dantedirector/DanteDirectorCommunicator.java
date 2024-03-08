/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import javax.security.auth.login.FailedLoginException;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.api.dal.monitor.aggregator.Aggregator;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.common.DanteDirectorConstant;
import com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.common.DanteDirectorQuery;
import com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.common.SystemInformation;
import com.avispl.symphony.dal.util.StringUtils;


public class DanteDirectorCommunicator extends RestCommunicator implements Aggregator, Monitorable, Controller {

	/**
	 * A private final ReentrantLock instance used to provide exclusive access to a shared resource
	 * that can be accessed by multiple threads concurrently. This lock allows multiple reentrant
	 * locks on the same shared resource by the same thread.
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	/**
	 * Private variable representing the local extended statistics.
	 */
	private ExtendedStatistics localExtendedStatistics;

	/**
	 * A JSON node containing the response from an aggregator.
	 */
	private List<JsonNode> domainList = Collections.synchronizedList(new ArrayList<>());

	/**
	 * current site value
	 */
	private JsonNode currentSiteValue;

	/**
	 * Constructs a new instance of DanteDirectorCommunicator.
	 *
	 * @throws IOException If an I/O error occurs while loading the properties mapping YAML file.
	 */
	public DanteDirectorCommunicator() throws IOException {
		this.setTrustAllCertificates(true);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 *
	 * Check for available devices before retrieving the value
	 * ping latency information to Symphony
	 */
	@Override
	public int ping() throws Exception {
		if (isInitialized()) {
			long pingResultTotal = 0L;

			for (int i = 0; i < this.getPingAttempts(); i++) {
				long startTime = System.currentTimeMillis();

				try (Socket puSocketConnection = new Socket(this.host, this.getPort())) {
					puSocketConnection.setSoTimeout(this.getPingTimeout());
					if (puSocketConnection.isConnected()) {
						long pingResult = System.currentTimeMillis() - startTime;
						pingResultTotal += pingResult;
						if (this.logger.isTraceEnabled()) {
							this.logger.trace(String.format("PING OK: Attempt #%s to connect to %s on port %s succeeded in %s ms", i + 1, host, this.getPort(), pingResult));
						}
					} else {
						if (this.logger.isDebugEnabled()) {
							logger.debug(String.format("PING DISCONNECTED: Connection to %s did not succeed within the timeout period of %sms", host, this.getPingTimeout()));
						}
						return this.getPingTimeout();
					}
				} catch (SocketTimeoutException | ConnectException tex) {
					throw new SocketTimeoutException("Socket connection timed out");
				} catch (UnknownHostException tex) {
					throw new SocketTimeoutException("Socket connection timed out" + tex.getMessage());
				} catch (Exception e) {
					if (this.logger.isWarnEnabled()) {
						this.logger.warn(String.format("PING TIMEOUT: Connection to %s did not succeed, UNKNOWN ERROR %s: ", host, e.getMessage()));
					}
					return this.getPingTimeout();
				}
			}
			return Math.max(1, Math.toIntExact(pingResultTotal / this.getPingAttempts()));
		} else {
			throw new IllegalStateException("Cannot use device class without calling init() first");
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		reentrantLock.lock();
		try {
			Map<String, String> statistics = new HashMap<>();
			List<AdvancedControllableProperty> advancedControllableProperties = new ArrayList<>();
			ExtendedStatistics extendedStatistics = new ExtendedStatistics();
			retrieveSystemInfo();
			populateSystemInfo(statistics, advancedControllableProperties);
			extendedStatistics.setStatistics(statistics);
			extendedStatistics.setControllableProperties(advancedControllableProperties);
			localExtendedStatistics = extendedStatistics;
		} finally {
			reentrantLock.unlock();
		}
		return Collections.singletonList(localExtendedStatistics);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {
		String property = controllableProperty.getProperty();
		String value = String.valueOf(controllableProperty.getValue());

		String[] propertyList = property.split(DanteDirectorConstant.HASH);
		String propertyName = property;
		if (property.contains(DanteDirectorConstant.HASH)) {
			propertyName = propertyList[1];
		}
		reentrantLock.lock();
		try {
			if (DanteDirectorConstant.SITE_NAME.equals(propertyName)) {
				Optional<JsonNode> matchingDomain = domainList.stream().filter(item -> item.get(DanteDirectorConstant.NAME).asText().equals(value)).findFirst();
				if (matchingDomain.isPresent()) {
					currentSiteValue = matchingDomain.get();
				} else {
					throw new IllegalArgumentException("Error while switch site name");
				}
			} else {
				System.out.println("Handle control for aggregated device");
			}
		} finally {
			reentrantLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperties(List<ControllableProperty> controllableProperties) throws Exception {
		if (CollectionUtils.isEmpty(controllableProperties)) {
			throw new IllegalArgumentException("ControllableProperties can not be null or empty");
		}
		for (ControllableProperty p : controllableProperties) {
			try {
				controlProperty(p);
			} catch (Exception e) {
				logger.error(String.format("Error when control property %s", p.getProperty()), e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics() throws Exception {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics(List<String> list) throws Exception {
		return retrieveMultipleStatistics().stream().filter(aggregatedDevice -> list.contains(aggregatedDevice.getDeviceId())).collect(Collectors.toList());
	}

	/**
	 * {@inheritDoc}
	 * set API Key into Header of Request
	 */
	@Override
	protected HttpHeaders putExtraRequestHeaders(HttpMethod httpMethod, String uri, HttpHeaders headers) {
		headers.set("Authorization", this.getPassword());
		return headers;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void authenticate() throws Exception {

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal init is called.");
		}
		super.internalInit();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalDestroy() {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal destroy is called.");
		}
		if (localExtendedStatistics != null && localExtendedStatistics.getStatistics() != null && localExtendedStatistics.getControllableProperties() != null) {
			localExtendedStatistics.getStatistics().clear();
			localExtendedStatistics.getControllableProperties().clear();
		}
		domainList = null;
		super.internalDestroy();
	}

	/**
	 * Retrieves system information by making a POST request to Dante Director and updating the domain list.
	 * Throws exceptions in case of errors during the process, such as failed login, resource not reachable, or missing data.
	 *
	 * @throws FailedLoginException If there is an error during login. Please check the credentials.
	 * @throws ResourceNotReachableException If there is an error retrieving system information or the number of sites is 0.
	 */
	private void retrieveSystemInfo() throws Exception {
		JsonNode response = this.doPost(DanteDirectorConstant.URL, DanteDirectorQuery.SYSTEM_INFO, JsonNode.class);

		if (response.has(DanteDirectorConstant.ERRORS) && checkUnauthenticated(response.get(DanteDirectorConstant.ERRORS))) {
			throw new FailedLoginException("Error while login. Please check the credentials");
		}

		if (!response.has(DanteDirectorConstant.DATA) || !response.get(DanteDirectorConstant.DATA).has(DanteDirectorConstant.DOMAINS)) {
			throw new ResourceNotReachableException("Error when retrieve system information.");
		}

		if (response.get(DanteDirectorConstant.DATA).get(DanteDirectorConstant.DOMAINS).size() == 0) {
			throw new ResourceNotReachableException("The Account is empty");
		} else {
			domainList.clear();
			for (JsonNode item : response.get(DanteDirectorConstant.DATA).get(DanteDirectorConstant.DOMAINS)) {
				domainList.add(item);
			}
		}
	}

	/**
	 * Checks if the provided JSON data contains an "UNAUTHENTICATED" code in the "extensions" field.
	 *
	 * @param data The JSON data to check for unauthenticated status.
	 * @return true if "UNAUTHENTICATED" is found; false otherwise.
	 */
	private boolean checkUnauthenticated(JsonNode data) {
		if (data.isArray()) {
			for (JsonNode item : data) {
				if ("UNAUTHENTICATED".equals(item.get("extensions").get("code").asText())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Populates system information into the provided stats map and advanced controllable properties list.
	 * System information includes properties from the {@link SystemInformation} enumeration.
	 *
	 * @param stats The map to store system information properties.
	 * @param advancedControllableProperties The list to store advanced controllable properties.
	 */
	private void populateSystemInfo(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		List<String> siteNameList = domainList.stream().map(node -> node.get(DanteDirectorConstant.NAME).asText()).collect(Collectors.toList());
		if (currentSiteValue == null) {
			currentSiteValue = domainList.get(0);
		}
		for (SystemInformation item : SystemInformation.values()) {
			String propertyName = item.getName();
			String value = getDefaultValueForNullData(currentSiteValue.get(DanteDirectorConstant.STATUS).get(item.getValue()).asText());
			stats.put(propertyName, value);
		}
		//Name
		String name = currentSiteValue.get(DanteDirectorConstant.NAME).asText();
		if (domainList.size() > 1) {
			addAdvancedControlProperties(advancedControllableProperties, stats, createDropdown(DanteDirectorConstant.SITE_NAME, siteNameList.toArray(new String[0]), name), name);
		} else {
			stats.put(DanteDirectorConstant.SITE_NAME, name);
		}
		//Number of devices
		stats.put("NumberOfDevices", String.valueOf(currentSiteValue.get(DanteDirectorConstant.DEVICES).size()));
	}

	/**
	 * check value is null or empty
	 *
	 * @param value input value
	 * @return value after checking
	 */
	private String getDefaultValueForNullData(String value) {
		return StringUtils.isNotNullOrEmpty(value) ? value : DanteDirectorConstant.NONE;
	}

	/***
	 * Create dropdown advanced controllable property
	 *
	 * @param name the name of the control
	 * @param values  The array of values for the dropdown options.
	 * @param initialValue initial value of the control
	 * @return AdvancedControllableProperty dropdown instance
	 */
	private AdvancedControllableProperty createDropdown(String name, String[] values, String initialValue) {
		AdvancedControllableProperty.DropDown dropDown = new AdvancedControllableProperty.DropDown();
		dropDown.setOptions(values);
		dropDown.setLabels(values);

		return new AdvancedControllableProperty(name, new Date(), dropDown, initialValue);
	}

	/**
	 * Add addAdvancedControlProperties if advancedControllableProperties different empty
	 *
	 * @param advancedControllableProperties advancedControllableProperties is the list that store all controllable properties
	 * @param stats store all statistics
	 * @param property the property is item advancedControllableProperties
	 * @throws IllegalStateException when exception occur
	 */
	private void addAdvancedControlProperties(List<AdvancedControllableProperty> advancedControllableProperties, Map<String, String> stats, AdvancedControllableProperty property, String value) {
		if (property != null) {
			for (AdvancedControllableProperty controllableProperty : advancedControllableProperties) {
				if (controllableProperty.getName().equals(property.getName())) {
					advancedControllableProperties.remove(controllableProperty);
					break;
				}
			}
			if (StringUtils.isNotNullOrEmpty(value)) {
				stats.put(property.getName(), value);
			} else {
				stats.put(property.getName(), DanteDirectorConstant.EMPTY);
			}
			advancedControllableProperties.add(property);
		}
	}
}
