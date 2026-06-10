# Audinate Dante Director Integration - Capabilities & Configuration
This document covers Audinate Dante Director Aggregator Capabilities and Configuration.

Symphony integrates with Audinate Dante Director — a cloud-based SaaS platform — to provide monitoring and control of Dante audio network devices organized into logical groups and sites. The adapter communicates with the Dante Director cloud API and exposes device identity, clock synchronisation status, network health, audio channel subscriptions, and enrolment state.

Main features are: Dante device inventory and online status monitoring, clock synchronisation monitoring and control, audio subscription and latency health tracking, and aggregator-level site management.

## Audinate Dante Director - Main use cases
- **Monitor** Dante device online status, enrolment state, firmware versions, IP/MAC address, clock synchronisation, and audio channel subscriptions
- **Control** device site assignment, preferred clock leader setting, unicast clocking, and V1 delay requests
- **Track** aggregator-level health indicators for Clocking, Connectivity, Latency, and Subscriptions
- **Inventory** Dante devices discovered and managed through the Dante Director cloud platform

## Audinate Dante Director - Prerequisites
The Dante Director aggregator authenticates using an API key from your Dante Director cloud account.

- Obtain the API key from the Dante Director cloud portal at https://director.dante.cloud/
- Use the key as the **Password** in the Symphony device configuration (Username is not required)

## Audinate Dante Director - Device Configuration and Provisioning

### Audinate Dante Director - Connection Setup

| Field | Value |
|---|---|
| Device Type | Infrastructure |
| Category | Management |
| Manufacturer | Audinate |
| Model | Dante Director |
| Monitoring Service | Advanced Monitoring |
| Monitoring Source | Direct |
| Management Address | `director.dante.cloud` (default — may differ in proxy or custom DNS environments) |
| Protocol | HTTPS |
| Username | Leave blank |
| Password | Dante Director API key |
| Port Number | 443 (default — may differ depending on server or proxy configuration) |

### Audinate Dante Director - Device Provisioning

After the aggregator retrieves device metadata, Dante devices appear in the aggregator's room. Unprovisioned devices must be imported before monitoring data is shown.

To import a Dante device:
1. Open Aggregated Devices
2. Click the (+) icon on the unprovisioned device
3. Fill in: Type (AV Devices), Category (Audio Network Interface), Manufacturer (Audinate), Model (Unknown)
4. Click Import, then OK to confirm

For detailed information on the aggregator and its configuration, please refer to our knowledgebase -> https://symphony.knowledgeowl.com/help/audinate-dante-director-aggregator

## Audinate Dante Director - Available Monitored Data

### Aggregator Properties
AdapterBuildDate, AdapterVersion, AdapterUptime, AdapterUptime(min), LastMonitoringCycleDuration(s), MonitoredDevicesTotal, MonitoringCycleInterval(min), Clocking, Connectivity, Latency, NumberOfDevices, SiteName, Subscriptions

### Aggregated Device Properties

**General:**

| Property | Description |
|---|---|
| deviceName, deviceModel, deviceId | Device identity |
| deviceOnline, EnrolmentState | Online status and enrolment in Dante Director |
| IPAddress, MACAddress | Network identity |
| DanteSoftwareVersion, DanteVersion | Dante firmware versions |
| ConnectedSince(GMT) | When the device connected to Dante Director |
| Manufacturer, ProductVersion | Hardware identity |
| DiscoveryDomainName, DiscoveryType | Network discovery details |
| Site | Dante Director site assignment — also controllable |
| Comments, Description, Location | Editable free-text fields |

**ClockSynchronisation group:**

| Property | Description |
|---|---|
| DomainClocking | Clock mode summary for the device |
| FrequencyOffset(ppm) | Clock frequency offset in parts per million |
| MuteStatus | Audio mute state: NOT_MUTED, MUTED_EXTERNAL_CLOCK, MUTED_INTERNAL_CLOCK, MUTED_USER, UNKNOWN |
| SyncStatus | Clock sync state: LOCKED, NOT_LOCKED, UNKNOWN |
| PrimaryMulticast | Multicast role and network address |
| Unicast | Unicast clock role |
| PreferredLeader | Toggle — preferred clock leader (also controllable) |
| UnicastClocking | Toggle — unicast clocking enabled (also controllable) |
| V1DelayRequests | Toggle — PTP v1 unicast delay requests (also controllable) |

**ReceiveChannels group:** Left, Right — shows subscribed audio channel sources. This group does not appear if the device has no connected channels.

**Status group:** Clocking, Connectivity, Latency, Subscriptions — each reports UNKNOWN, OK, WARNING, or ERROR.

## Audinate Dante Director - Control Capabilities

| Property | Type | Description |
|---|---|---|
| SiteName | Dropdown | Assign the aggregator to a Dante Director site |
| Site | Dropdown | Assign the aggregated device to a Dante Director site |
| PreferredLeader | Toggle | Set the device as preferred clock leader |
| UnicastClocking | Toggle | Enable or disable unicast clocking |
| V1DelayRequests | Toggle | Enable or disable PTP v1 unicast delay requests |

## Audinate Dante Director - Troubleshooting

**Login / Connection Error**
- Verify the Password field contains a valid Dante Director API key
- Confirm the Management Address is the correct hostname for the Dante Director cloud
- Ensure the API key has not expired or been revoked in the Dante Director portal

**API Error**
- Check the API error description in the aggregator extended properties
- Verify the API key is still active in the Dante Director account settings

**Link Error / Ping Timeout**
- Confirm the Cloud Connector can reach `director.dante.cloud` on port 443
- Check the Ping Protocol in the Symphony device configuration

**Devices Not Appearing**
- Confirm the device has been provisioned (imported) in Symphony
- Verify the Dante device is enrolled in Dante Director (EnrolmentState = ENROLLED)

**ReceiveChannels Group Missing**
- This group only appears when the device has active audio channel subscriptions — it hides automatically when no channels are connected

If none of the recommended steps help, please enter an SOS ticket at {https://avi-spl.atlassian.net/servicedesk/customer/portals}

## Audinate Dante Director - What AI Assistant can do with it:
- Find Audinate Dante Director Aggregated Devices (Dante Director as Monitoring Proxy) in Symphony
- Verify Audinate Dante Director Aggregator configuration and adapter property settings
- Report on device online status, enrolment state, clock synchronisation, and audio subscription health

## Audinate Dante Director - What AI Assistant cannot do with it:
- Provision devices
- Generate or manage API keys in the Dante Director portal
- Configure Dante domain settings or audio routing directly on devices
