import com.google.gson.Gson;
import config.ExampleConfig;
import config.PollerConfig;
import model.domain.DataNode;
import model.domain.Machine;
import model.domain.Poller;
import model.domain.Subscription;
import org.eclipse.milo.examples.server.ExampleServer;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.PollerService;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application {
    private final static Logger logger = LoggerFactory.getLogger(Application.class);
    private static final String CONFIG_PATH = "CONFIG_PATH";
    public static void main(String args[]) {

        String pathPollerConfig = System.getenv(CONFIG_PATH);

        Gson gson = new Gson();
        try {
            PollerConfig config;
            if (pathPollerConfig == null) {
                logger.info("No config file on ENV variable ["+ CONFIG_PATH +"] which will result in a " +
                        "sample config for local tests");
                config = new ExampleConfig();
            } else {
                logger.info("Try to load config from path: " + pathPollerConfig);
                config = gson.fromJson(new FileReader(pathPollerConfig), PollerConfig.class);
            }

            if (config.isLoggingOfConfig()){
                logger.info("Starting Poller with config:");
                logger.info(gson.toJson(config));
            }

            if(config.isStartOpcExampleServer()){
                try {
                    ExampleServer exampleServer = new ExampleServer();
                    exampleServer.startup().get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }





            String topic = config.getConfig().getMqttTopic();
            String broker = config.getConfig().getMqttBrokerAddress();
            String clientId = config.getConfig().getMqttClientId();
            int qos = config.getConfig().getMqttQoS();
            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient mqttClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            logger.info("Connecting to broker: " + broker);
            mqttClient.connect(connOpts);
            logger.info("Connected");

            PollerService pollerService = new PollerService();
            config.getConfig().getMachineSubscriptions().forEach(
                    machine ->
                            machine.getSubscriptions().forEach(
                                    subscription -> {
                                        pollerService.addSubscription(machine, subscription, subscriptionUpdate -> {
                                            Map<String, String> content = new HashMap<>();
                                            content.put("timestamp", subscriptionUpdate.getTimestamp().toString());
                                            subscriptionUpdate.getUpdateValues().forEach((key, dataValue) -> content.put(key, dataValue.getValue().getValue().toString()));
                                            logger.debug("Publishing message: " + content);
                                            MqttMessage message = new MqttMessage(gson.toJson(content).getBytes());
                                            message.setQos(qos);
                                            try {
                                                mqttClient.publish(topic, message);
                                            } catch (MqttException e) {
                                                logger.error("Failed to publish Message: " + message.toString() + " cause of: " + e.getMessage());
                                            }
                                            return null;
                                        });
                                    }));
        } catch (FileNotFoundException e) {
            logger.error("No config file available to start poller");
        } catch (MqttException e) {
            logger.error("Failure of connection to MQTT broker: " + e.getReasonCode());
        }


    }
}
