/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.avispl.symphony.dal.aggregator.parser.AggregatedDeviceProcessor;
import com.avispl.symphony.dal.aggregator.parser.PropertiesMapping;
import com.avispl.symphony.dal.aggregator.parser.PropertiesMappingParser;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.common.AggregatedControllableProperty;
import com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.common.AggregatedInformation;
import com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.common.DanteDirectorConstant;
import com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.common.DanteDirectorQuery;
import com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.common.SystemInformation;
import com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.dto.ChannelDTO;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * DanteDirectorCommunicator
 * Supported features are:
 * Monitoring Aggregator Device:
 *  <ul>
 *  <li> - Clocking</li>
 *  <li> - Connectivity</li>
 *  <li> - Latency</li>
 *  <li> - NumberOfDevices</li>
 *  <li> - SiteName</li>
 *  <li> - Subscriptions</li>
 *  <ul>
 *
 * General Info Aggregated Device:
 * <ul>
 * <li> - Comments</li>
 * <li> - ConnectedSince</li>
 * <li> - DanteSoftwareVersion</li>
 * <li> - DanteVersion</li>
 * <li> - Description</li>
 * <li> - deviceId</li>
 * <li> - deviceModel</li>
 * <li> - deviceName</li>
 * <li> - deviceOnline</li>
 * <li> - DiscoveryDomainName</li>
 * <li> - DiscoveryType</li>
 * <li> - EnrolmentState</li>
 * <li> - IPAddress</li>
 * <li> - Location</li>
 * <li> - MACAddress</li>
 * <li> - Manufacturer</li>
 * <li> - ProductVersion</li>
 * <li> - Site</li>
 * </ul>
 *
 * ClockSynchronisation Group:
 * <ul>
 * <li> - DomainClocking</li>
 * <li> - FrequencyOffset(ppm)</li>
 * <li> - MuteStatus</li>
 * <li> - PreferredLeader</li>
 * <li> - PrimaryMulticast</li>
 * <li> - SyncStatus</li>
 * <li> - Unicast</li>
 * <li> - UnicastClocking</li>
 * <li> - V1DelayRequests</li>
 * </ul>
 *
 * Status Group:
 * <ul>
 * <li> - Clocking</li>
 * <li> - Connectivity</li>
 * <li> - Latency</li>
 * <li> - Subscriptions</li>
 * </ul>
 * @author Harry / Symphony Dev Team<br>
 * Created on 03/05/2024
 * @since 1.0.0
 */
public class DanteDirectorCommunicator extends RestCommunicator implements Aggregator, Monitorable, Controller {
	/**
	 * Process that is running constantly and triggers collecting data from Dante Director SE API endpoints, based on the given timeouts and thresholds.
	 *
	 * @author Harry
	 * @since 1.0.0
	 */
	class DanteDirectorDataLoader implements Runnable {
		private volatile boolean inProgress;

		public DanteDirectorDataLoader() {
			inProgress = true;
		}

		@Override
		public void run() {
			loop:
			while (inProgress) {
				try {
					TimeUnit.MILLISECONDS.sleep(500);
				} catch (InterruptedException e) {
					logger.info(String.format("Sleep for 0.5 second was interrupted with error message: %s", e.getMessage()));
				}

				if (!inProgress) {
					break loop;
				}

				// next line will determine whether Dante Director monitoring was paused
				updateAggregatorStatus();
				if (devicePaused) {
					continue loop;
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Fetching other than aggregated device list");
				}

				while (nextDevicesCollectionIterationTimestamp > System.currentTimeMillis()) {
					try {
						TimeUnit.MILLISECONDS.sleep(1000);
					} catch (InterruptedException e) {
						logger.info(String.format("Sleep for 1 second was interrupted with error message: %s", e.getMessage()));
					}
				}

				if (!inProgress) {
					break loop;
				}

				long startCycle = System.currentTimeMillis();
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("Fetching devices list");
					}
					populateDeviceDetails();
				} catch (Exception e) {
					logger.error("Error occurred during device list retrieval: " + e.getMessage(), e);
				}

				try{
					nextDevicesCollectionIterationTimestamp = System.currentTimeMillis() + (getMonitoringRate() * 60000L);
				} catch (NoSuchMethodError error){
					nextDevicesCollectionIterationTimestamp = System.currentTimeMillis() + 60000L;
					logger.warn("Unsupported feature: getMonitoringRate isn't available on current Cloud Connector version.", error);
				}

				lastMonitoringCycleDuration = Math.max((System.currentTimeMillis() - startCycle) / 1000, 1L);

				if (logger.isDebugEnabled()) {
					logger.debug("Finished collecting devices statistics cycle at " + new Date() + ", total duration: " + lastMonitoringCycleDuration);
				}
			}
			// Finished collecting
		}

		/**
		 * Triggers main loop to stop
		 */
		public void stop() {
			inProgress = false;
		}
	}

	/**
	 * Indicates whether a device is considered as paused.
	 * True by default so if the system is rebooted and the actual value is lost -> the device won't start stats
	 * collection unless the {@link DanteDirectorCommunicator#retrieveMultipleStatistics()} method is called which will change it
	 * to a correct value
	 */
	private volatile boolean devicePaused = true;

	/**
	 * We don't want the statistics to be collected constantly, because if there's not a big list of devices -
	 * new devices' statistics loop will be launched before the next monitoring iteration. To avoid that -
	 * this variable stores a timestamp which validates it, so when the devices' statistics is done collecting, variable
	 * is set to currentTime + 30s, at the same time, calling {@link #retrieveMultipleStatistics()} and updating the
	 */
	private long nextDevicesCollectionIterationTimestamp;

	/**
	 * This parameter holds timestamp of when we need to stop performing API calls
	 * It used when device stop retrieving statistic. Updated each time of called #retrieveMultipleStatistics
	 */
	private volatile long validRetrieveStatisticsTimestamp;

	/**
	 * Aggregator inactivity timeout. If the {@link DanteDirectorCommunicator#retrieveMultipleStatistics()}  method is not
	 * called during this period of time - device is considered to be paused, thus the Cloud API
	 * is not supposed to be called
	 */
	private static final long retrieveStatisticsTimeOut = 3 * 60 * 1000;

	/**
	 * Update the status of the device.
	 * The device is considered as paused if did not receive any retrieveMultipleStatistics()
	 * calls during {@link DanteDirectorCommunicator}
	 */
	private synchronized void updateAggregatorStatus() {
		devicePaused = validRetrieveStatisticsTimestamp < System.currentTimeMillis();
	}

	/**
	 * Uptime time stamp to valid one
	 */
	private synchronized void updateValidRetrieveStatisticsTimestamp() {
		validRetrieveStatisticsTimestamp = System.currentTimeMillis() + retrieveStatisticsTimeOut;
		updateAggregatorStatus();
	}

	/**
	 * A mapper for reading and writing JSON using Jackson library.
	 * ObjectMapper provides functionality for converting between Java objects and JSON.
	 * It can be used to serialize objects to JSON format, and deserialize JSON data to objects.
	 */
	ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Executor that runs all the async operations, that is posting and
	 */
	private ExecutorService executorService;

	/**
	 * A private field that represents an instance of the DanteDirectorLoader class, which is responsible for loading device data for Dante Director
	 */
	private DanteDirectorDataLoader deviceDataLoader;

	/**
	 * A private final ReentrantLock instance used to provide exclusive access to a shared resource
	 * that can be accessed by multiple threads concurrently. This lock allows multiple reentrant
	 * locks on the same shared resource by the same thread.
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	/**
	 * Adapter metadata properties - adapter version and build date
	 */
	private Properties adapterProperties;

	/**
	 * How much time last monitoring cycle took to finish
	 */
	private long lastMonitoringCycleDuration;

	/**
	 * Device adapter instantiation timestamp.
	 */
	private long adapterInitializationTimestamp;

	/**
	 * Private variable representing the local extended statistics.
	 */
	private ExtendedStatistics localExtendedStatistics;

	/**
	 * An instance of the AggregatedDeviceProcessor class used to process and aggregate device-related data.
	 */
	private AggregatedDeviceProcessor aggregatedDeviceProcessor;

	/**
	 * A JSON node containing the response from an aggregator.
	 */
	private List<JsonNode> domainList = Collections.synchronizedList(new ArrayList<>());

	/**
	 * List of aggregated device
	 */
	private List<AggregatedDevice> aggregatedDeviceList = Collections.synchronizedList(new ArrayList<>());

	/**
	 * cache data for aggregated
	 */
	private List<AggregatedDevice> cachedData = Collections.synchronizedList(new ArrayList<>());

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
		adapterProperties = new Properties();
		Map<String, PropertiesMapping> mapping = new PropertiesMappingParser().loadYML(DanteDirectorConstant.MODEL_MAPPING_AGGREGATED_DEVICE, getClass());
		aggregatedDeviceProcessor = new AggregatedDeviceProcessor(mapping);
		adapterProperties.load(getClass().getResourceAsStream("/version.properties"));
		this.setTrustAllCertificates(true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		reentrantLock.lock();
		try {
			Map<String, String> statistics = new HashMap<>();
			Map<String, String> dynamicStatistics = new HashMap<>();
			List<AdvancedControllableProperty> advancedControllableProperties = new ArrayList<>();
			ExtendedStatistics extendedStatistics = new ExtendedStatistics();
			retrieveMetadata(statistics, dynamicStatistics);
			retrieveSystemInfo();
			populateSystemInfo(statistics, advancedControllableProperties);
			extendedStatistics.setStatistics(statistics);
			extendedStatistics.setDynamicStatistics(dynamicStatistics);
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
		reentrantLock.lock();
		try {
			String property = controllableProperty.getProperty();
			String deviceId = controllableProperty.getDeviceId();
			String value = String.valueOf(controllableProperty.getValue());

			String[] propertyList = property.split(DanteDirectorConstant.HASH);
			String propertyName = property;
			if (property.contains(DanteDirectorConstant.HASH)) {
				propertyName = propertyList[1];
			}
			if (DanteDirectorConstant.SITE_NAME.equals(propertyName)) {
				Optional<JsonNode> matchingDomain = domainList.stream().filter(item -> item.get(DanteDirectorConstant.NAME).asText().equals(value)).findFirst();
				if (matchingDomain.isPresent()) {
					currentSiteValue = matchingDomain.get();
				} else {
					throw new IllegalArgumentException("Error when control SiteName");
				}
			} else {
				Optional<AggregatedDevice> aggregatedDevice = aggregatedDeviceList.stream().filter(item -> item.getDeviceId().equals(deviceId)).findFirst();
				if (aggregatedDevice.isPresent()) {
					AggregatedInformation item = AggregatedInformation.getByDefaultName(propertyName);
					switch (item) {
						case LEADER:
						case DELAY_REQUEST:
						case EXTERNAL_WORD_CLOCK:
						case UNICAST_CLOCKING:
							AggregatedControllableProperty aggregatedProperty = AggregatedControllableProperty.getByDefaultName(propertyName);
							String requestValue = DanteDirectorConstant.NUMBER_ONE.equals(value) ? DanteDirectorConstant.TRUE : DanteDirectorConstant.FALSE;
							sendCommandToControlDevice(deviceId, requestValue, aggregatedProperty);
							updateCacheValue(deviceId, propertyName, requestValue);
							break;
						case SITE_NAME:
							Optional<JsonNode> node = domainList.stream().filter(itemValue -> itemValue.get(DanteDirectorConstant.NAME).asText().equals(value)).findFirst();
							if (!node.isPresent()) {
								throw new IllegalArgumentException("Error when control SiteName with value is " + value);
							}
							sendCommandToControlTheSiteName(deviceId, node.get().get(DanteDirectorConstant.ID).asText(), value);
							updateCacheValue(deviceId, propertyName, value);
							for (AggregatedControllableProperty control : AggregatedControllableProperty.values()) {
								updateCacheValue(deviceId, control.getName(), "false");
							}
							break;
						default:
							if (logger.isWarnEnabled()) {
								logger.warn(String.format("Unable to execute %s command on device %s: Not Supported", property, deviceId));
							}
							break;
					}
				} else {
					throw new IllegalArgumentException(String.format("Unable to control property: %s as the device does not exist.", property));
				}
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
		if (executorService == null) {
			executorService = Executors.newFixedThreadPool(1);
			executorService.submit(deviceDataLoader = new DanteDirectorDataLoader());
		}
		nextDevicesCollectionIterationTimestamp = System.currentTimeMillis();
		updateValidRetrieveStatisticsTimestamp();
		if (cachedData.isEmpty()) {
			return Collections.emptyList();
		}
		return cloneAndPopulateAggregatedDeviceList();
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
		adapterInitializationTimestamp = System.currentTimeMillis();
		executorService = Executors.newFixedThreadPool(1);
		executorService.submit(deviceDataLoader = new DanteDirectorDataLoader());
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
		if (deviceDataLoader != null) {
			deviceDataLoader.stop();
			deviceDataLoader = null;
		}
		if (executorService != null) {
			executorService.shutdownNow();
			executorService = null;
		}
		if (localExtendedStatistics != null && localExtendedStatistics.getStatistics() != null && localExtendedStatistics.getControllableProperties() != null) {
			localExtendedStatistics.getStatistics().clear();
			localExtendedStatistics.getControllableProperties().clear();
		}
		domainList = null;
		currentSiteValue = null;
		nextDevicesCollectionIterationTimestamp = 0;
		aggregatedDeviceList.clear();
		cachedData.clear();
		super.internalDestroy();
	}

	/**
	 * Sends a control command to the specified device for a controllable property.
	 *
	 * @param deviceId The ID of the device to control.
	 * @param value The value to set for the controllable property.
	 * @param property The controllable property to control.
	 */
	private void sendCommandToControlDevice(String deviceId, String value, AggregatedControllableProperty property) {
		try {
			String command = String.format(DanteDirectorQuery.CONTROL_CLOCK_SYNC, property.getCommandParam(), property.getCommandName(), deviceId, value);
			JsonNode response = this.doPost(command);
			if (response.has(DanteDirectorConstant.ERRORS)) {
				throw new IllegalArgumentException("The command response is error");
			}

		} catch (Exception e) {
			throw new IllegalArgumentException(
					String.format("Can't control %s with value is %s. %s", property.getName(), DanteDirectorConstant.TRUE.equals(value) ? DanteDirectorConstant.ON : DanteDirectorConstant.OFF, e.getMessage()));
		}
	}

	/**
	 * Sends a command to control the site name associated with the specified device ID and domain ID.
	 *
	 * @param deviceId The ID of the device.
	 * @param domainId The ID of the domain.
	 * @param siteName The new site name to set.
	 * @throws IllegalArgumentException If the command response indicates an error, or if an error occurs during the execution of the command.
	 */
	private void sendCommandToControlTheSiteName(String deviceId, String domainId, String siteName) {
		try {
			String command = String.format(DanteDirectorQuery.CONTROL_SITE, deviceId, domainId);
			JsonNode response = this.doPost(command);
			if (response.has(DanteDirectorConstant.ERRORS)) {
				throw new IllegalArgumentException("The command response is error");
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Can't control SiteName with value is %s. %s", siteName, e.getMessage()));
		}
	}

	/**
	 * Retrieves metadata information and updates the provided statistics and dynamic map.
	 *
	 * @param stats the map where statistics will be stored
	 * @param dynamicStatistics the map where dynamic statistics will be stored
	 */
	private void retrieveMetadata(Map<String, String> stats, Map<String, String> dynamicStatistics) {
		try {
			dynamicStatistics.put(DanteDirectorConstant.MONITORING_CYCLE_DURATION, String.valueOf(lastMonitoringCycleDuration));
			stats.put(DanteDirectorConstant.ADAPTER_VERSION,
					getDefaultValueForNullData(adapterProperties.getProperty("aggregator.version")));
			stats.put(DanteDirectorConstant.ADAPTER_BUILD_DATE,
					getDefaultValueForNullData(adapterProperties.getProperty("aggregator.build.date")));
			long adapterUptime = System.currentTimeMillis() - adapterInitializationTimestamp;

			stats.put(DanteDirectorConstant.ADAPTER_UPTIME_MIN, String.valueOf(adapterUptime / (1000 * 60)));
			stats.put(DanteDirectorConstant.ADAPTER_UPTIME, normalizeUptime(adapterUptime / 1000));
			try{
				stats.put(DanteDirectorConstant.SYSTEM_MONITORING_CYCLE, String.valueOf(getMonitoringRate()));
			}catch (NoSuchMethodError error){
				logger.warn("Unsupported feature: getMonitoringRate isn't available on current Cloud Connector version.", error);
			}
			dynamicStatistics.put(DanteDirectorConstant.MONITORED_DEVICES_TOTAL, String.valueOf(this.cachedData.size()));
		} catch (Exception e) {
			logger.error("Failed to populate metadata information", e);
		}
	}

	/**
	 * Retrieves system information by making a POST request to Dante Director and updating the domain list.
	 * Throws exceptions in case of errors during the process, such as failed login, resource not reachable, or missing data.
	 *
	 * @throws FailedLoginException If there is an error during login. Please check the credentials.
	 * @throws ResourceNotReachableException If there is an error retrieving system information or the number of sites is 0.
	 */
	private void retrieveSystemInfo() throws Exception {
		JsonNode response = this.doPost(DanteDirectorQuery.SYSTEM_INFO);

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
				if ("UNAUTHENTICATED".equals(item.get(DanteDirectorConstant.EXTENSIONS).get(DanteDirectorConstant.CODE).asText())) {
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
		} else {
			Optional<JsonNode> matchingDomain = domainList.stream().filter(item -> item.get(DanteDirectorConstant.NAME).asText().equals(currentSiteValue.get(DanteDirectorConstant.NAME).asText())).findFirst();
			currentSiteValue = matchingDomain.orElse(currentSiteValue);
		}
		for (SystemInformation item : SystemInformation.values()) {
			String propertyName = item.getName();
			String value = getDefaultValueForNullData(currentSiteValue.get(DanteDirectorConstant.STATUS).get(item.getValue()).asText());
			JsonNode message = currentSiteValue.get(DanteDirectorConstant.STATUS).get("domainAlertMessage").get(item.getValue());
			if (message != null && message.has(DanteDirectorConstant.MESSAGE)) {
				stats.put(propertyName + "Message", message.get(DanteDirectorConstant.MESSAGE).asText());
			}
			stats.put(propertyName, value);
		}
		//Name
		String name = currentSiteValue.get(DanteDirectorConstant.NAME).asText();
		if (domainList.size() > 1) {
			addAdvancedControlProperties(advancedControllableProperties, stats, createDropdown(DanteDirectorConstant.SITE_NAME, siteNameList.toArray(new String[0]), name), name);
		} else {
			advancedControllableProperties.removeIf(item -> item.getName().equalsIgnoreCase(DanteDirectorConstant.SITE_NAME));
			stats.put(DanteDirectorConstant.SITE_NAME + DanteDirectorConstant.SPACE, name);
		}
	}

	/**
	 * Populates device details by making a POST request to retrieve information from Dante Director.
	 * The method clears the existing aggregated device list, processes the response, and updates the list accordingly.
	 * Any error during the process is logged.
	 */
	private void populateDeviceDetails() {
		try {
			JsonNode response = this.doPost(DanteDirectorQuery.DEVICES_INFO);
			if (response.has(DanteDirectorConstant.DATA) && response.get(DanteDirectorConstant.DATA).has(DanteDirectorConstant.DOMAINS)) {
				cachedData.clear();
				for (JsonNode domainNode : response.get(DanteDirectorConstant.DATA).get(DanteDirectorConstant.DOMAINS)) {
					String domainId = domainNode.get(DanteDirectorConstant.ID).asText();
					if (checkExistDomainId(domainId)) {
						JsonNode jsonArray = domainNode.get(DanteDirectorConstant.DEVICES);
						for (JsonNode jsonNode : jsonArray) {
							JsonNode node = objectMapper.createArrayNode().add(jsonNode);

							String id = jsonNode.get(DanteDirectorConstant.ID).asText();
							cachedData.removeIf(item -> item.getDeviceId().equals(id));
							cachedData.addAll(aggregatedDeviceProcessor.extractDevices(node));
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error while populate aggregated device", e);
		}
	}

	/**
	 * Clones and populates a new list of aggregated devices with mapped monitoring properties.
	 *
	 * @return A new list of {@link AggregatedDevice} objects with mapped monitoring properties.
	 */
	private List<AggregatedDevice> cloneAndPopulateAggregatedDeviceList() {
		aggregatedDeviceList.clear();
		synchronized (cachedData) {
			for (AggregatedDevice item : cachedData) {
				AggregatedDevice aggregatedDevice = new AggregatedDevice();
				Map<String, String> cachedValue = item.getProperties();
				aggregatedDevice.setDeviceId(item.getDeviceId());
				aggregatedDevice.setDeviceModel(item.getDeviceModel());
				aggregatedDevice.setDeviceName(item.getDeviceName());
				aggregatedDevice.setDeviceOnline(item.getDeviceOnline());

				List<AdvancedControllableProperty> controllableProperties = new ArrayList<>();
				Map<String, String> stats = new HashMap<>();
				Map<String, String> controlStats = new HashMap<>();
				mapMonitoringProperty(cachedValue, stats, controlStats, controllableProperties);
				if (Boolean.TRUE.equals(aggregatedDevice.getDeviceOnline())) {
					stats.putAll(controlStats);
					aggregatedDevice.setControllableProperties(controllableProperties);
				}
				aggregatedDevice.setProperties(stats);
				aggregatedDeviceList.add(aggregatedDevice);
			}
		}
		return aggregatedDeviceList;
	}

	/**
	 * Checks if a given domain ID exists in the list of domains.
	 *
	 * @param id The domain ID to check for existence.
	 * @return true if the domain ID exists in the list; false otherwise.
	 */
	private boolean checkExistDomainId(String id) {
		for (JsonNode item : domainList) {
			if (item.has(DanteDirectorConstant.ID) && item.get(DanteDirectorConstant.ID).asText().equals(id)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Maps monitoring properties from cached values to statistics and advanced control properties.
	 *
	 * @param cachedValue The cached values map containing raw monitoring data.
	 * @param stats The statistics map to store mapped monitoring properties.
	 * @param statsControl The advanced control map to store properties requiring control.
	 * @param advancedControllableProperties The list of advanced controllable properties to be populated.
	 */
	private void mapMonitoringProperty(Map<String, String> cachedValue, Map<String, String> stats, Map<String, String> statsControl, List<AdvancedControllableProperty> advancedControllableProperties) {
		for (AggregatedInformation property : AggregatedInformation.values()) {
			String name = property.getName();
			String propertyName = property.getGroup() + name;
			String value = getDefaultValueForNullData(cachedValue.get(name));
			switch (property) {
				case CLOCKING:
				case LATENCY:
				case CONNECTIVITY:
				case SUBSCRIPTIONS:
					if (StringUtils.isNotNullOrEmpty(cachedValue.get(name + "Message"))) {
						value += " (" + cachedValue.get(name + "Message") + ")";
					}
					stats.put(propertyName, value);
					break;
				case CONNECTED_SINCE:
					stats.put(propertyName, convertDateTimeFormat(value));
					break;
				case DOMAIN_CLOCKING:
					stats.put(propertyName, DanteDirectorConstant.TRUE.equals(value) ? "Grand Leader" : DanteDirectorConstant.NONE);
					break;
				case UNICAST:
					String unicastFollower = getDefaultValueForNullData(cachedValue.get("UnicastFollower"));
					String unicastLeader = getDefaultValueForNullData(cachedValue.get("UnicastLeader"));
					if (DanteDirectorConstant.TRUE.equals(unicastLeader) && DanteDirectorConstant.TRUE.equals(unicastFollower)) {
						value = "Unicast Leader, Unicast Follower";
					} else if (DanteDirectorConstant.TRUE.equals(unicastLeader)) {
						value = "Unicast Leader";
					} else if (DanteDirectorConstant.TRUE.equals(unicastFollower)) {
						value = "Unicast Follower";
					} else {
						value = DanteDirectorConstant.NONE;
					}
					stats.put(propertyName, value);
					break;
				case PRIMARY_MULTICAST:
					String subNet = getDefaultValueForNullData(cachedValue.get("Subnet"));
					String netMask = getDefaultValueForNullData(cachedValue.get("Netmask"));
					String newValue = DanteDirectorConstant.TRUE.equals(value) ? "Multicast Leader" : "Multicast Follower";
					if (!DanteDirectorConstant.NONE.equals(subNet) && !DanteDirectorConstant.NONE.equals(netMask)) {
						newValue += " (" + subNet + "/" + netMask + ")";
					}
					stats.put(propertyName, newValue);
					break;
				case EXTERNAL_WORD_CLOCK:
				case LEADER:
				case UNICAST_CLOCKING:
					if (DanteDirectorConstant.TRUE.equals(getDefaultValueForNullData(cachedValue.get(name + DanteDirectorConstant.CAPABILITY)))) {
						addAdvancedControlProperties(advancedControllableProperties, statsControl,
								createSwitch(propertyName, DanteDirectorConstant.TRUE.equals(value) ? 1 : 0, DanteDirectorConstant.OFF, DanteDirectorConstant.ON),
								DanteDirectorConstant.TRUE.equals(value) ? DanteDirectorConstant.NUMBER_ONE : DanteDirectorConstant.ZERO);
					}
					break;
				case DELAY_REQUEST:
					if (DanteDirectorConstant.TRUE.equals(getDefaultValueForNullData(cachedValue.get(name + DanteDirectorConstant.CAPABILITY)))) {
						addAdvancedControlProperties(advancedControllableProperties, statsControl,
								createSwitch(propertyName, DanteDirectorConstant.TRUE.equals(value) ? 1 : 0, "Multicast", "Unicast"),
								DanteDirectorConstant.TRUE.equals(value) ? DanteDirectorConstant.NUMBER_ONE : DanteDirectorConstant.ZERO);
					}
					break;
				case SITE_NAME:
					List<String> siteNameList = domainList.stream().map(node -> node.get(DanteDirectorConstant.NAME).asText()).collect(Collectors.toList());
					addAdvancedControlProperties(advancedControllableProperties, stats, createDropdown(propertyName, siteNameList.toArray(new String[0]), value), value);
					break;
				case RECEIVE_CHANNELS:
					try {
						List<ChannelDTO> channelList = objectMapper.readValue(value, new TypeReference<List<ChannelDTO>>() {
						});
						if (!channelList.isEmpty()) {
							for (ChannelDTO item : channelList) {
								String channelName = item.getName();
								if (StringUtils.isNotNullOrEmpty(item.getSubscribedChannel()) && StringUtils.isNotNullOrEmpty(item.getSubscribedDevice())) {
									stats.put(DanteDirectorConstant.RECEIVE_CHANNEL_GROUP + channelName, item.getSubscribedChannel() + "@" + item.getSubscribedDevice());
								}
							}
						}
					} catch (Exception e) {
						logger.error("Error while retrieve Receive Channels", e);
					}
					break;
				default:
					stats.put(propertyName, value);
			}
		}
	}

	/**
	 * Updates the cache value for a specified property in the aggregated device list.
	 *
	 * @param deviceId The ID of the device whose cache value needs to be updated.
	 * @param name The name of the property to be updated.
	 * @param value The new value to set for the property.
	 */
	private void updateCacheValue(String deviceId, String name, String value) {
		cachedData.stream().filter(item -> deviceId.equals(item.getDeviceId()))
				.findFirst().ifPresent(item -> item.getProperties().put(name, value));
	}

	/**
	 * Converts a date-time string from the default format to the target format with GMT timezone.
	 *
	 * @param inputDateTime The input date-time string in the default format.
	 * @return The date-time string after conversion to the target format with GMT timezone.
	 * Returns {@link DanteDirectorConstant#NONE} if there is an error during conversion.
	 * @throws Exception If there is an error parsing the input date-time string.
	 */
	private String convertDateTimeFormat(String inputDateTime) {
		if (DanteDirectorConstant.NONE.equals(inputDateTime)) {
			return inputDateTime;
		}
		try {
			SimpleDateFormat inputFormat = new SimpleDateFormat(DanteDirectorConstant.DEFAULT_FORMAT_DATETIME);
			inputFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

			SimpleDateFormat outputFormat = new SimpleDateFormat(DanteDirectorConstant.TARGET_FORMAT_DATETIME);
			outputFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

			Date date = inputFormat.parse(inputDateTime);
			return outputFormat.format(date);
		} catch (Exception e) {
			logger.warn("Can't convert the date time value");
			return DanteDirectorConstant.NONE;
		}
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

	/**
	 * Uptime is received in seconds, need to normalize it and make it human-readable, like
	 * 1 day 5 hour 12 minute 55 minute
	 * Incoming parameter is may have a decimal point, so in order to safely process this - it's rounded first.
	 * We don't need to add a segment of time if it's 0.
	 *
	 * @param uptimeSeconds value in seconds
	 * @return string value of format 'x d x hr x min x sec'
	 */
	public static String normalizeUptime(long uptimeSeconds) {
		StringBuilder normalizedUptime = new StringBuilder();

		long seconds = uptimeSeconds % 60;
		long minutes = uptimeSeconds % 3600 / 60;
		long hours = uptimeSeconds % 86400 / 3600;
		long days = uptimeSeconds / 86400;

		if (days > 0) {
			normalizedUptime.append(days).append(" d ");
		}
		if (hours > 0) {
			normalizedUptime.append(hours).append(" hr ");
		}
		if (minutes > 0) {
			normalizedUptime.append(minutes).append(" min ");
		}
		if (seconds > 0 || normalizedUptime.length() == 0) {
			normalizedUptime.append(seconds).append(" sec");
		}
		return normalizedUptime.toString().trim();
	}

	/**
	 * Create switch is control property for metric
	 *
	 * @param name the name of property
	 * @param status initial status (0|1)
	 * @return AdvancedControllableProperty switch instance
	 */
	private AdvancedControllableProperty createSwitch(String name, int status, String labelOff, String labelOn) {
		AdvancedControllableProperty.Switch toggle = new AdvancedControllableProperty.Switch();
		toggle.setLabelOff(labelOff);
		toggle.setLabelOn(labelOn);

		AdvancedControllableProperty advancedControllableProperty = new AdvancedControllableProperty();
		advancedControllableProperty.setName(name);
		advancedControllableProperty.setValue(status);
		advancedControllableProperty.setType(toggle);
		advancedControllableProperty.setTimestamp(new Date());

		return advancedControllableProperty;
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

	/**
	 * Sends a POST request to the Dante Director API with retry mechanism.
	 *
	 * <p>This method attempts to call the API up to a maximum number of retries
	 * when a {@link ResourceAccessException} occurs (e.g., connection timeout).
	 * Between retries, it waits for 500 milliseconds and attempts to reconnect
	 * by calling {@code disconnect()}.</p>
	 * <p>If all retry attempts fail, a {@link RuntimeException} is thrown.</p>
	 *
	 * @param data the request payload to be sent in the POST body
	 * @return the response body as a {@link JsonNode}
	 * @throws RuntimeException if the request fails after all retry attempts
	 */
	private JsonNode doPost(String data) throws Exception {
		int maxRetry = 3;
		for (int i = 1; i <= maxRetry; i++) {
			try {
				return super.doPost(DanteDirectorConstant.URL, data, JsonNode.class);
			} catch (ResourceNotReachableException e) {
				this.logger.error(String.format("API call failed at attempt %s - data %s", i, data), e);
				if (i == maxRetry) {
					throw new RuntimeException(e);
				}
				this.disconnect();
				this.logger.debug("Retrying send API after 0.5s...");
				try {
					Thread.sleep(500);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(ex);
				}
			}
		}
		throw new RuntimeException("Failed to request to " + DanteDirectorConstant.URL);
	}
}
