import com.google.gson.Gson;
import config.PollerConfig;
import model.domain.DataNode;
import model.domain.Machine;
import model.domain.Poller;
import model.domain.Subscription;
import org.eclipse.milo.examples.server.ExampleServer;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.PollerService;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application {
    private final static  Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String args[]){

        String pathPollerConfig = System.getenv("CONFIG_PATH");

        try {
            ExampleServer exampleServer = new ExampleServer();
            exampleServer.startup().get();
        } catch (Exception e) {
            e.printStackTrace();
        }


        Gson gson = new Gson();
        try {
            boolean manualTest = true;
            PollerConfig config;
            if(manualTest){

                DataNode dataNode = new DataNode();
                dataNode.setNamespace(2);
                dataNode.setStringId("HelloWorld/ScalarTypes/Boolean");
                Map<String, DataNode> dataNodeMap = new HashMap<>();
                dataNodeMap.put("RandomEntry", dataNode);

                Subscription subscription = new Subscription();
                subscription.setName("Subscription Name");
                subscription.setSampleTime(1000);
                subscription.setDataPoints(dataNodeMap);

                Machine machine = new Machine();
                machine.setName("Mathi Machine");
                machine.setRequestTimeout(5000);
                machine.setOpcuaAddress("opc.tcp://127.0.0.1:12686/milo");
                machine.setSubscriptions(List.of(subscription));

                Poller poller = new Poller();
                poller.setMqttBrokerAddress("tcp://mqtt.eclipse.org:1883");
                poller.setMqttClientId("Mathi Test Client");
                poller.setMqttQoS(2);
                poller.setMqttTopic("Mathi Test Topic");
                poller.setMachineSubscriptions(List.of(machine));

                config = new PollerConfig();
                config.setConfig(poller);

            }else{
                 config = gson.fromJson(new FileReader(pathPollerConfig), PollerConfig.class);
            }



            String topic            = config.getConfig().getMqttTopic();
            String broker           = config.getConfig().getMqttBrokerAddress();
            String clientId         = config.getConfig().getMqttClientId();
            int qos                 = config.getConfig().getMqttQoS();
            MemoryPersistence persistence = new MemoryPersistence();
                MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(true);
                logger.info("Connecting to broker: "+broker);
                sampleClient.connect(connOpts);
                logger.info("Connected");

            PollerService pollerService = new PollerService();
            String finalTopic = topic;
            config.getConfig().getMachineSubscriptions().forEach(
                    machine ->
                            machine.getSubscriptions().forEach(
                    subscription -> {
                pollerService.addSubscription(machine, subscription, subscriptionUpdate -> {
                    Map<String, String> content = new HashMap<>();
                    content.put("timestamp", subscriptionUpdate.getTimestamp().toString());
                    subscriptionUpdate.getUpdateValues().forEach((key, dataValue) -> content.put(key, dataValue.getValue().getValue().toString()));
                    logger.debug("Publishing message: "+ content);
                    MqttMessage message = new MqttMessage(gson.toJson(content).getBytes());
                    message.setQos(qos);
                    try {
                        sampleClient.publish(finalTopic, message);
                    } catch (MqttException e) {
                        logger.error("Failed to publish Message: " + message.toString() + " cause of: " + e.getMessage());
                    }
                    return null;
                });
            }));
        } catch (FileNotFoundException e) {
            logger.error("No config file available to start poller");
        } catch (MqttException e) {
            logger.error("Failure of connection to MQTT broker: "+e.getReasonCode());
        }


    }
}
