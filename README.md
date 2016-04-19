#Certificate based authentication using HiveMQ 
In this demo, HiveMQ is used to demonstrate the certificate based authentication. You should have installed HiveMQ already in order to proceed.
HiveMQ is an MQTT broker that can be used to enable enterprises with M2M and IoT capabilities. HiveMQ focuses solely on standard MQTT as a protocol for device communication. In this article, HiveMQ has been used to demonstrate certificate based two way SSL authentication. The included code samples to demonstrate payload encryption/decryption have been tested both in IBM IoTF and HiveMQ. HiveMQ can be easily downloaded, installed and started by following the steps mentioned in http://www.hivemq.com. Optional plugins can be used to retrieve the retained messages from HiveMQ. MQTT client works with HiveMQ in the same way as with IoTF.
Generating the certificate
Following steps to be followed for generating the certificate for authentication. This uses the keytool bundled with a Java Runtime Environment.
1. Generate device key and keystore
keytool -genkey -alias iotdevice1 -keyalg RSA -keypass devicepass -storepass devicepass -keystore iot_device_keystore.jks -storetype jks

2. Export device certificate from keystore
keytool -export -alias iotdevice1 -storepass devicepass -file iotdevice1.cer -keystore iot_device_keystore.jks

3. Add device certificate into broker truststore
keytool -import -v -trustcacerts -alias iotdevice1 -file iotdevice1.cer -keystore iot_broker_truststore.jks -keypass devicepass -storepass brokerpass -storetype jks

4. Generate broker key and keystore
keytool -genkey -alias broker -keyalg RSA -keypass brokerpass -storepass brokerpass -keystore iot_broker_keystore.jks -storetype jks

5. Export broker certificate
keytool -export -alias broker -storepass brokerpass -file broker.cer -keystore iot_broker_keystore.jks

6. Add the certificate into device truststore
keytool -import -v -trustcacerts -alias broker -file broker.cer -keystore iot_device_truststore.jks -keypass brokerpass -storepass brokerpass -storetype jks

The same approach can be extended for multiple devices. All certificates are needed to be added in broker's truststore and broker's certificate is needed in trust stores for all the devices.

#Steps to run the demo
1. Download the codebase and import ineclipse as java project.
2. Install and start HiveMQ broker
3. Configure broker/device certificates, keystore and truststore as mentioned above.
4. Update HiveMQ config xml with the certificate details.
An example - 
 <tls-tcp-listener>
            <port>8883</port>
            <bind-address>0.0.0.0</bind-address>
            <tls>
                <keystore>
                    <path><Your path>\iot_broker_keystore.jks</path>
                    <password>brokerpass</password>
                    <private-key-password>brokerpass</private-key-password>
                </keystore>
                <truststore>                  <path>C:\certificates\iot_broker_truststore.jks</path>
                    <password>brokerpass</password>
                </truststore>
                <client-authentication-mode>REQUIRED</client-authentication-mode>
            </tls>
        </tls-tcp-listener>
5. Configure other security options in app.conf and device.conf files
6. Add jars from lib folder into application classpath.
7. Run AppTest.java and DeviceTest.java

