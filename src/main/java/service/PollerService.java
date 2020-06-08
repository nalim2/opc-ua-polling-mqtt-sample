package service;

import model.domain.DataNode;
import model.domain.Machine;
import model.domain.Subscription;
import model.domain.SubscriptionUpdate;
import model.domain.auth.UserAuth;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.api.UaSession;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class PollerService {
    private Map<Machine, OpcUaClient> activeConnections = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PollerService() {

    }

    public void addSubscription(Machine machine, Subscription sub, Function<SubscriptionUpdate,Void> callback){
        try {
            OpcUaClient uaClient = getConnection(machine);
            uaClient.addSessionActivityListener(new SessionActivityListener() {
                @Override
                public void onSessionInactive(UaSession session) {
                    logger.error("Lost connection to Session: " + session.getSessionName());
                    boolean reconnect = true;
                    while (reconnect){
                        try {
                            Thread.sleep(1000);
                            logger.info("Try to reconnect: " + session.getSessionName());
                            uaClient.connect().get();
                            reconnect = false;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            List<String> subOrder = new ArrayList(sub.getDataPoints().keySet());
            List<NodeId> nodeIds = new ArrayList<>();
            Map<String, DataValue> updateSet = new HashMap<>();
            for (String nodeKey:
                    subOrder) {
                DataNode nodeInfo = sub.getDataPoints().get(nodeKey);
                NodeId nodeId;
                if(nodeInfo.getStringId() == null || nodeInfo.getStringId().isEmpty()){
                    nodeId = new NodeId(nodeInfo.getNamespace(), nodeInfo.getNumericId());
                }else {
                    nodeId =new NodeId(nodeInfo.getNamespace(), nodeInfo.getStringId());
                }
                nodeIds.add(nodeId);

            }
            Timer t = new Timer();
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        List<DataValue> response = uaClient.readValues(0, TimestampsToReturn.Both, nodeIds).get();
                        for(int counter = 0; counter < response.size(); counter++){
                            updateSet.put(subOrder.get(counter), response.get(counter));
                        }
                        if(response.stream().allMatch(dataValue -> dataValue.getStatusCode().equals(StatusCode.GOOD))){
                            Timestamp timestamp = new Timestamp(Calendar.getInstance().getTimeInMillis());
                            callback.apply(new SubscriptionUpdate(updateSet, timestamp, machine));
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            },0, machine.requestTimeout);
        } catch (ConnectException  e) {
            e.printStackTrace();
        }
    }

    private OpcUaClient getConnection(Machine machine) throws ConnectException {
        String discoveryUrl = machine.getOpcuaAddress();
        List<EndpointDescription> endpoints = null;
        EndpointDescription endpoint = null;

        try {
            endpoints = DiscoveryClient.getEndpoints(discoveryUrl).get();
        } catch (Exception ex) {
            if (!discoveryUrl.endsWith("/")) {
                discoveryUrl += "/";
            }
            discoveryUrl += "discovery";

            try {
                endpoints = DiscoveryClient.getEndpoints(discoveryUrl).get();
            } catch (InterruptedException |ExecutionException e) {
                throw new ConnectException("Error while discovering opc ua endpoints");
            }
        }
        endpoint = endpoints.stream()
                .filter(e -> e.getSecurityPolicyUri().equals(getSecurityPolicy(machine).getUri()))
                .findFirst()
                .orElseThrow(() -> new ConnectException("Error while discovering opc ua endpoints"));

        //ApplicationDescription applicationDescription = new ApplicationDescription();
        //endpoint = new EndpointDescription(address.getHostAddress(),applicationDescription , null, MessageSecurityMode.None, SecurityPolicy.None.getUri(), null , TransportProfile.TCP_UASC_UABINARY.getUri(), UByte.valueOf(0));// TODO hier machen wenn fertig
        ApplicationDescription currentAD = endpoint.getServer();
        ApplicationDescription withoutDiscoveryAD = new ApplicationDescription(
                currentAD.getApplicationUri(),
                currentAD.getProductUri(),
                currentAD.getApplicationName(),
                currentAD.getApplicationType(),
                currentAD.getGatewayServerUri(),
                currentAD.getDiscoveryProfileUri(),
                new String[0]);

        endpoint = new EndpointDescription(
                machine.getOpcuaAddress(),
                withoutDiscoveryAD,
                endpoint.getServerCertificate(),
                endpoint.getSecurityMode(),
                endpoint.getSecurityPolicyUri(),
                endpoint.getUserIdentityTokens(),
                endpoint.getTransportProfileUri(),
                endpoint.getSecurityLevel());



        OpcUaClientConfig config = OpcUaClientConfig.builder()
                .setApplicationName(LocalizedText.english("eclipse milo opc-ua client of the apache PLC4X:PLC4J project"))
                .setApplicationUri("urn:eclipse:milo:plc4x:client")
                .setEndpoint(endpoint)
                .setIdentityProvider(getIdentityProvider(machine))
                .setRequestTimeout(UInteger.valueOf(machine.getRequestTimeout()))
                .build();

        try {
            OpcUaClient client = OpcUaClient.create(config);
            client.connect().get();
            return client;
        } catch (UaException e) {
            String message = (config == null) ? "NULL" : config.toString();
            throw  new ConnectException("The given input values are a not valid OPC UA connection configuration [CONFIG]: " + message);
        } catch (InterruptedException | ExecutionException e) {
            throw new ConnectException("Error while creation of the connection because of : " + e.getMessage());
        }
    }

        SecurityPolicy getSecurityPolicy(Machine machine) {
            return SecurityPolicy.None;
        }

        IdentityProvider getIdentityProvider(Machine machine) {
            if(machine.getAuth() == null){
                return new AnonymousProvider();
            }else {
                if(machine.getAuth().getUserAuth() != null){
                    UserAuth userAuth = machine.getAuth().getUserAuth();
                    return new UsernameProvider(userAuth.getUser(), userAuth.getPassword());
                }else {
                    return new AnonymousProvider();
                }
            }

        }
}
