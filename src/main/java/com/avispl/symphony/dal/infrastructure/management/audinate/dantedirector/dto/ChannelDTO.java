/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.audinate.dantedirector.dto;

/**
 * Represents a DTO (Data Transfer Object) for a channel
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 3/8/2024
 * @since 1.0.0
 */
public class ChannelDTO {
	private String name;
	private String mediaType;
	private String subscribedChannel;
	private String subscribedDevice;


	/**
	 * Constructs a ChannelDTO with the specified properties.
	 *
	 * @param name The name of the channel.
	 * @param mediaType The media type of the channel.
	 * @param subscribedChannel The subscribed channel.
	 * @param subscribedDevice The subscribed device.
	 */
	public ChannelDTO(String name, String mediaType, String subscribedChannel, String subscribedDevice) {
		this.name = name;
		this.mediaType = mediaType;
		this.subscribedChannel = subscribedChannel;
		this.subscribedDevice = subscribedDevice;
	}

	/**
	 * Constructs an empty ChannelDTO.
	 */
	public ChannelDTO() {
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
	 * Sets {@link #name} value
	 *
	 * @param name new value of {@link #name}
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Retrieves {@link #mediaType}
	 *
	 * @return value of {@link #mediaType}
	 */
	public String getMediaType() {
		return mediaType;
	}

	/**
	 * Sets {@link #mediaType} value
	 *
	 * @param mediaType new value of {@link #mediaType}
	 */
	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	/**
	 * Retrieves {@link #subscribedChannel}
	 *
	 * @return value of {@link #subscribedChannel}
	 */
	public String getSubscribedChannel() {
		return subscribedChannel;
	}

	/**
	 * Sets {@link #subscribedChannel} value
	 *
	 * @param subscribedChannel new value of {@link #subscribedChannel}
	 */
	public void setSubscribedChannel(String subscribedChannel) {
		this.subscribedChannel = subscribedChannel;
	}

	/**
	 * Retrieves {@link #subscribedDevice}
	 *
	 * @return value of {@link #subscribedDevice}
	 */
	public String getSubscribedDevice() {
		return subscribedDevice;
	}

	/**
	 * Sets {@link #subscribedDevice} value
	 *
	 * @param subscribedDevice new value of {@link #subscribedDevice}
	 */
	public void setSubscribedDevice(String subscribedDevice) {
		this.subscribedDevice = subscribedDevice;
	}
}
