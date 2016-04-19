package com.ibm.bluemixmqtt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.bluemixmqtt.IOTSecurityUtil;

import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class AppTest {

	private MqttHandler handler;
	private String strAppId = null;
	private String strOrg = null;
	private String strAuthMethod = null;
	private String strAuthToken = null;
	private String strSSL = null;
	private TimeOutTask task = null;
	private Timer t = null;
	private String otp = null;
	boolean otpValidated = false;
	boolean otpTimeOut = false;
	boolean isOTPCapable = false;
	private int commandCount = 0;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new AppTest().doApp();
	}

	/**
	 * Run the app
	 */
	public void doApp() {
		// Read properties from the conf file
		Properties props = MqttUtil.readProperties("MyData/app.conf");

		strOrg = props.getProperty("org");
		strAppId = props.getProperty("appid");
		strAuthMethod = props.getProperty("key");
		strAuthToken = props.getProperty("token");
		// isSSL property
		strSSL = props.getProperty("isSSL");
		boolean isSSL = false;
		if (strSSL.equals("T")) {
			isSSL = true;
		}

		System.out.println("org: " + strOrg);
		System.out.println("id: " + strAppId);
		System.out.println("authmethod: " + strAuthMethod);
		System.out.println("authtoken" + strAuthToken);
		System.out.println("isSSL: " + isSSL);

		// Format: a:<orgid>:<app-id>
		String clientId = "a:" + strOrg + ":" + strAppId;
		String serverHost = strOrg + MqttUtil.SERVER_SUFFIX;

		handler = new AppMqttHandler();
		handler.connect(serverHost, clientId, strAuthMethod, strAuthToken,
				isSSL);

		handler.subscribe("iot-2/type/" + MqttUtil.DEFAULT_DEVICE_TYPE
				+ "/id/+/mon", 0);

		// Subscribe Device Events
		// iot-2/type/<type-id>/id/<device-id>/evt/<event-id>/fmt/<format-id>
		handler.subscribe("iot-2/type/" + MqttUtil.DEFAULT_DEVICE_TYPE
				+ "/id/+/evt/" + MqttUtil.DEFAULT_EVENT_ID + "/fmt/json", 0);
		handler.subscribe("iot-2/evt/", 0);

	}

	/**
	 * This class implements as the application MqttHandler
	 * 
	 */
	private class AppMqttHandler extends MqttHandler {

		// Pattern to check whether the events comes from a device for an event
		Pattern pattern = Pattern.compile("iot-2/evt/");

		Pattern pattern1 = Pattern.compile("iot-2/type/"
				+ MqttUtil.DEFAULT_DEVICE_TYPE + "/id/(.+)/seq/"
				+ MqttUtil.DEFAULT_EVENT_ID + "/fmt/json");

		/**
		 * Once a subscribed message is received
		 */
		@Override
		public void messageArrived(String topic, MqttMessage mqttMessage)
				throws Exception {

			super.messageArrived(topic, mqttMessage);
			try {
				System.out.println("topic " + topic);

				Matcher matcher = pattern.matcher(topic);
				if (matcher.matches()) {
					String deviceid = "4213";
					String rawPayload = new String(mqttMessage.getPayload());
					System.out.println(rawPayload);
					// Parse the payload in Json Format
					JSONObject jsonObject = new JSONObject(rawPayload);

					if (jsonObject.containsKey("event")) {

						try {
							String strReq = jsonObject.getString("event");
							if (strReq != null
									&& strReq.equals("server_uid_request")) {
								// Send MAC address
								String strMacId = IOTSecurityUtil.getMACAdress(
										"S", strAppId);
								System.out.println("Server mac address - "
										+ strMacId);
								sendMacAddress(strMacId, deviceid);

							} else if (strReq != null
									&& strReq.equals("server_otp_request")) {
								// Send otp
								System.out.println("Sending OTP for device -"
										+ deviceid);
								sendOTP(deviceid);
							} else if (strReq != null
									&& strReq.equals("device_otp_response")) {
								// OTP received
								String strOtp = jsonObject.getString("otp");
								System.out
										.println("OTP received from the device -"
												+ strOtp);
								validateOTPandSendResponse(strOtp, deviceid);

							} else if (strReq.equals("encryption_key")) {
								// Send encryption key
							} else if (strReq.equals("publish")) {
								// process data
								JSONObject contObj = jsonObject
										.getJSONObject("d");

								String strCount = contObj.getString("count");
								System.out.println("Receive count " + strCount
										+ " from device " + deviceid);

								// If count >=4, start a new thread to reset the
								// count
								if (Integer.valueOf(strCount) >= 4) {
									new ResetCountThread(deviceid, 0).start();
								}
							} else if (strReq.equals("data")) {
								// process data
								System.out
										.println("New incoming data from device "
												+ deviceid
												+ " - "
												+ jsonObject.write());
								new ResetCountThread(deviceid, commandCount++)
										.start();
							}
						} catch (Exception ee) {
							ee.printStackTrace();
						}

					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void sendMacAddress(String strMacID, String strDeviceId) {
			JSONObject jsonObj = new JSONObject();
			try {
				jsonObj.put("cmd", "server_uid_response");
				jsonObj.put("uid", strMacID);
				jsonObj.put("appid", strAppId);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			System.out.println("Sending mac address -  " + strMacID);
			// Publish command to one specific device
			new sendMessageToDevice(strDeviceId, "server_uid_response", jsonObj)
					.start();

		}

		public void validateOTPandSendResponse(String receivedOTP,
				String strDeviceId) {

			// Perform OTP validation
			if (receivedOTP.equals(otp)) {
				if (task.isTimedOut) {
					// User took more than 100 seconds and hence the OTP is
					// invalid
					System.out.println("Time out!");
					otpValidated = false;
					otpTimeOut = true;
				} else {
					System.out.println("OTP validated..");
					otpValidated = true;
					otpTimeOut = false;
				}
			} else {
				System.out.println("Incorrect OTP..");
				otpValidated = false;
				otpTimeOut = false;
			}

			JSONObject otpRespObj = new JSONObject();
			try {
				otpRespObj.put("cmd", "server_otp_validate");
				otpRespObj.put("isOTPValid", String.valueOf(otpValidated));
				otpRespObj.put("isTimeOut", String.valueOf(otpTimeOut));
				otpRespObj.put("appid", strAppId);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			System.out.println("Result of OTP validation -  " + otpValidated);
			// Publish command to one specific device
			new sendMessageToDevice(strDeviceId, "server_otp_validate",
					otpRespObj).start();

		}

		public void sendOTP(String strDeviceId) {
			otp = IOTSecurityUtil.generateOTP();

			JSONObject jsonObj = new JSONObject();
			try {
				jsonObj.put("cmd", "server_otp_response");
				jsonObj.put("otp", otp);
				jsonObj.put("appid", strAppId);

				// Server starts a timer of 5 mins during which the OTP is
				// valid.
				task = new TimeOutTask();
				t = new Timer();
				t.schedule(task, 30000000L);

			} catch (JSONException e) {
				e.printStackTrace();
			}
			System.out.println("Sending otp  -  " + otp);

			// Publish command to one specific device
			// iot-2/type/<type-id>/id/<device-id>/cmd/<cmd-id>/fmt/<format-id>
			new sendMessageToDevice(strDeviceId, "server_otp_response", jsonObj)
					.start();
		}
	}

	/**
	 * A thread to reset the count
	 * 
	 */
	private class ResetCountThread extends Thread {
		private String deviceid = null;
		private int count = 0;

		public ResetCountThread(String deviceid, int count) {
			this.deviceid = deviceid;
			this.count = count;
		}

		public void run() {
			JSONObject jsonObj = new JSONObject();
			String cmdTest = "Command " + count;
			String serverId = null;
			if (count % 2 == 0)
				serverId = IOTSecurityUtil.getMACAdress("S", strAppId);
			else
				serverId = "dummy";
			try {
				jsonObj.put("cmd", "command");
				jsonObj.put("text", cmdTest);
				jsonObj.put("appid", strAppId);
				jsonObj.put("uid", serverId);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			System.out.println("Reset count for device " + deviceid);

			// Publish command to one specific device
			// iot-2/type/<type-id>/id/<device-id>/cmd/<cmd-id>/fmt/<format-id>
			handler.publish("iot-2/type/" + MqttUtil.DEFAULT_DEVICE_TYPE
					+ "/id/" + deviceid + "/cmd/" + MqttUtil.DEFAULT_CMD_ID
					+ "/fmt/json", jsonObj.toString(), false, 0);

		}
	}

	/**
	 * A thread to send message to the device
	 * 
	 */
	private class sendMessageToDevice extends Thread {
		private String deviceid = null;
		private String command = null;
		private JSONObject obj = null;

		public sendMessageToDevice(String deviceId1, String command1,
				JSONObject obj1) {
			this.command = command1;
			this.obj = obj1;
			this.deviceid = deviceId1;
		}

		public void run() {
			handler.publish("iot-2/cmd/", obj.toString(), false, 0);
		}
	}

}
