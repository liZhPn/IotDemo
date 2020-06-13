package com.lizhpn.iot.server;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.iot.model.v20180120.*;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.lizhpn.iot.config.Config;
import com.lizhpn.utils.ResultUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.qpid.jms.JmsConnection;
import org.apache.qpid.jms.JmsConnectionListener;
import org.apache.qpid.jms.message.JmsInboundMessageDispatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.net.URI;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AmqpService {

    // 云端API调用凭据
    private static String accessKey = Config.accessKey;
    private static String accessSecret = Config.accessSecret;

    // 产品标识
    private static String productKey = Config.produceKey;
    private static String regionId = Config.regionId;

    private final static Logger logger = LoggerFactory.getLogger(AmqpController.class);

    // 与云端连接的客户机，通过此客户机服务端可以和云端进行交互
    private static DefaultAcsClient client;


    //业务处理异步线程池，线程池参数可以根据您的业务特点调整；或者您也可以用其他异步方式处理接收到的消息
    private final static ExecutorService executorService = new ThreadPoolExecutor
            (Runtime.getRuntime().availableProcessors(),
                    Runtime.getRuntime().availableProcessors() * 2,
                    60,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(50000));

    static {
        try {
            // 初始化
            initClient();
        } catch (ClientException e) {
            System.out.println("错误：服务端初始化客户机出错了.");
            e.printStackTrace();
        }
    }

    /**
     * 属性设置
     * <p>
     * 服务端发送属性更新命令到设备端，改变设备的属性，这里指温度
     * <p>
     * 这个方法是：
     * 1、服务端通过云端API发送属性设置指令到云端;
     * 2、云端向属性设置的相关 topic -- /thing/service/property/set 发布该指令；
     * 3、设备端若订阅了属性设置的 topic -- /thing/service/property/set，将收到这条指令；
     * 4、设备根据这条指令进行相应的操作，这里的操作是指更新设备温度；
     * 5、设备执行指令完毕后，向 topic -- /thing/service/property/set_reply 发布执行结果消息；
     * 6、云端收到设备发布的结果消息后，因为配置了服务端订阅，所以这条结果消息将会被服务端接收到；
     *
     * @param deviceName  设备名称
     * @param temperature 设备的期望温度
     * @return 命令执行结果
     */
    public static JSONObject sendUpdateAttributeCommandToDevice(String deviceName, Integer temperature) {
        JSONObject result = new JSONObject();
        try {
            // 服务端生成一个请求指令
            SetDevicePropertyRequest request = new SetDevicePropertyRequest();
            request.setProductKey(productKey);
            request.setDeviceName(deviceName);

            // 发送给设备的消息，这里看起来不是很规范，那是因为这里已经被云端SDK给简化了
            JSONObject json = new JSONObject();
            json.put("temperature", temperature);
            request.setItems(json.toString());

            // 不信我们打印出来看一下,这个 QueryParameters 就是请求参数
            Map<String, String> params = request.getQueryParameters();
            for (String param : params.keySet()) {
                System.out.println(param + ":" + params.get(param));
            }

            // 服务端利用 DefaultAcsClient 客户端向云端发送这个请求并接收云端的响应
            SetDevicePropertyResponse response = client.getAcsResponse(request);
            // 获取请求ID
            result.put("requestId", response.getRequestId());
            // 获取执行结果，是否成功
            result.put("success", response.getSuccess());
            return result;
        } catch (ClientException e) {
            System.out.println("错误：云端设置设备属性出错了.");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 调用设备的服务
     * <p>
     * 这里只针对控制台配置的服务功能  -- boolean SetWarningTemp(int minWarningTemp,int maxWarningTemp)
     *
     * @param serviceName 设备服务名
     * @return 服务调用结果
     */
    public static JSONObject invokingDeviceService(String deviceName, String serviceName, int min, int max) {
        JSONObject result = new JSONObject();
        try {
            // 云端应用向设备下发属性设置指令
            InvokeThingServiceRequest request = new InvokeThingServiceRequest();
            request.setProductKey(productKey);
            request.setDeviceName(deviceName);
            request.setIdentifier(serviceName);
            // 控制台定义功能时定义的两个参数，不能缺少
            request.setArgs("{'minWarningTemp':" + min + ",'maxWarningTemp':" + max + "}");

            // 服务端利用 DefaultAcsClient 客户端向云端发送这个请求并接收云端的响应
            InvokeThingServiceResponse response = client.getAcsResponse(request);
            result.put("requestId", response.getRequestId());
            result.put("success", response.getSuccess());
            if (response.getSuccess()) {
                result.put("error", "无");
            } else {
                result.put("error", response.getErrorMessage());
            }
            return result;
        } catch (ClientException e) {
            System.out.println("错误：云端调用设备服务出错了.");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 服务端获取设备的状态，即是否在线
     *
     * @param deviceName 需要获取此设备的状态
     * @return
     */
    public static JSONObject GetDeviceStatus(String deviceName) {
        JSONObject result = new JSONObject();
        try {
            // 服务端向云端请求设备状态
            GetDeviceStatusRequest request = new GetDeviceStatusRequest();
            request.setProductKey(productKey);
            request.setDeviceName(deviceName);

            // 响应
            GetDeviceStatusResponse response = client.getAcsResponse(request);
            result.put("requestId", response.getRequestId());
            result.put("success", response.getSuccess());
            if (response.getSuccess()) {     // 成功请求
                result.put("error", "无");
                // 将响应的设备状态加进结果
                result.put("status", response.getData().getStatus());
            } else {
                result.put("error", response.getErrorMessage());
            }
            return result;
        } catch (ClientException e) {
            System.out.println("错误：云端获取设备状态出错了.");
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 服务端获取设备影子的 JSON
     *
     * @param deviceName 需要获取此设备的设备影子
     * @return 设备影子JSON格式消息
     */
    public static JSONObject GetDeviceShadow(String deviceName) {
        JSONObject result = new JSONObject();
        try {
            // 云端应用向设备下发属性设置指令
            GetDeviceShadowRequest request = new GetDeviceShadowRequest();
            request.setProductKey(productKey);
            request.setDeviceName(deviceName);

            // 响应
            GetDeviceShadowResponse response = client.getAcsResponse(request);
            result.put("requestId", response.getRequestId());
            result.put("success", response.getSuccess());
            if (response.getSuccess()) {
                result.put("error", "无");
                result.put("shadow", response.getShadowMessage());
            } else {
                result.put("error", response.getErrorMessage());
            }
            return result;
        } catch (ClientException e) {
            System.out.println("错误：云端获取设备影子出错了.");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 服务端更新设备影子的 JSON
     *
     * @param deviceName 需要更新此设备的设备影子
     * @param temp       更新的温度
     * @return
     */
    public static JSONObject UpdateDeviceShadow(String deviceName, Integer temp, long shadowVersion) {
        JSONObject result = new JSONObject();
        try {
            // 云端应用向设备下发属性设置指令
            UpdateDeviceShadowRequest request = new UpdateDeviceShadowRequest();
            request.setProductKey(productKey);
            request.setDeviceName(deviceName);
            // 更新设备影子
            /**
             * 发布此 JSON 去更新云端设备影子：
             * {
             *  "method": "update",
             *  "state": {
             *      "desired": {
             *          "temperature": ${temperature}
             *      }
             *  },
             *  "version": ${shadowVersion}
             * }
             */
            JSONObject json = new JSONObject();
            json.put("method", "update");
            JSONObject state = new JSONObject();
            JSONObject desired = new JSONObject();
            desired.put("temperature", temp);
            state.put("desired", desired);
            json.put("state", state);
            json.put("version", shadowVersion);
            request.setShadowMessage(json.toString());

            // 响应
            UpdateDeviceShadowResponse response = client.getAcsResponse(request);
            result.put("requestId", response.getRequestId());
            result.put("success", response.getSuccess());
            if (response.getSuccess()) {
                result.put("error", "无");
            } else {
                result.put("error", response.getErrorMessage());
            }
            return result;
        } catch (ClientException e) {
            System.out.println("错误：云端更新设备影子出错了.");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 初始化用来和云平台交互的客户机，交互是指向云平台发送请求以及接收云平台返回的响应
     *
     * 初始化客户机需要前面说过的 accessKey, accessSecret ，
     * 这样这个客户机才能够调用云端的API，才能让云平台转发消息给指定的设备
     * 执行时机：应用向云端发送或者接收消息之前
     *
     * @throws ClientException
     */
    private static void initClient() throws ClientException {
        // 初始化 客户机
        DefaultProfile.addEndpoint(regionId, regionId, "iot", "iot." + regionId + ".aliyuncs.com");
        IClientProfile profile = DefaultProfile.getProfile(regionId, accessKey, accessSecret);
        client = new DefaultAcsClient(profile);
    }

    /**
     * AMQP客户端接入，使用qpid-jms-client接入
     *
     * 初始化并创建用于接收服务端订阅消息的 qpid-jms-client
     * 执行时机：在应用准备接收服务端订阅消息之前
     *
     */
    public static void initQpidJMSClient() {
        System.out.println("信息：服务端开始初始化并创建 qpid-jms 连接.");

        try {

            // 消费组ID
            String consumerGroupId = Config.consumerGroupId;

            // 实例ID，仅购买的实例需要传入
            String iotInstanceId = "";

            // 时间戳，用于唯一地标志某一刻
            long timeStamp = System.currentTimeMillis();

            // 阿里云账号
            String IotId = Config.IoTId;

            // 签名方法：支持hmacmd5，hmacsha1和hmacsha256
            String signMethod = "hmacsha1";

            // 控制台服务端订阅中消费组状态页客户端ID一栏将显示clientId参数。
            // 建议使用机器UUID、MAC地址、IP等唯一标识等作为clientId。便于您区分识别不同的客户端。
            String clientId = "设备1";

            // UserName组装方法，请参见上一篇文档：AMQP客户端接入说明。
            String userName = clientId + "|authMode=aksign"
                    + ",signMethod=" + signMethod
                    + ",timestamp=" + timeStamp
                    + ",authId=" + accessKey
                    + ",iotInstanceId=" + iotInstanceId //如果是购买的实例，需要传实例ID
                    + ",consumerGroupId=" + consumerGroupId
                    + "|";

            // password组装方法，请参见上一篇文档：AMQP客户端接入说明。
            String signContent = "authId=" + accessKey + "&timestamp=" + timeStamp;
            String password = doSign(signContent, accessSecret, signMethod);

            // 按照qpid-jms的规范，组装连接URL。
            String connectionUrl = "failover:(amqps://"
                    + IotId + ".iot-amqp."
                    + regionId + ".aliyuncs.com:5671?amqp.idleTimeout=80000)"
                    + "?failover.reconnectDelay=30";
            Hashtable<String, String> hashtable = new Hashtable<>();
            hashtable.put("connectionfactory.SBCF", connectionUrl);
            hashtable.put("queue.QUEUE", "default");
            hashtable.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
            Context context = new InitialContext(hashtable);
            ConnectionFactory cf = (ConnectionFactory) context.lookup("SBCF");
            Destination queue = (Destination) context.lookup("QUEUE");

            // Create Connection    创建连接
            Connection connection = cf.createConnection(userName, password);
            // 配置此连接的连接状态监听者
            ((JmsConnection) connection).addConnectionListener(myJmsConnectionListener);

            // Create Session   创建会话
            // Session.CLIENT_ACKNOWLEDGE: 收到消息后，需要手动调用message.acknowledge()
            // Session.AUTO_ACKNOWLEDGE: SDK自动ACK（推荐）
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            connection.start();

            // Create Receiver Link
            MessageConsumer consumer = session.createConsumer(queue);
            // 为这个消息消费者配置消息监听者，以便能在接收到云端消息时可以及时进行相应的处理
            consumer.setMessageListener(messageListener);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("服务端已成功创建 qpid-jms 连接，现在可以接收和处理云端的服务端订阅消息了.\n");
    }

    // 这是一个监听云端服务端订阅消息的监听类，一旦有消息到达，都会被其监听到，从而进行下一步操作
    private static MessageListener messageListener = new MessageListener() {
        @Override
        public void onMessage(Message message) {
            try {
                //1.收到消息之后一定要ACK
                // 推荐做法：创建Session选择Session.AUTO_ACKNOWLEDGE，这里会自动ACK。
                // 其他做法：创建Session选择Session.CLIENT_ACKNOWLEDGE，这里一定要调message.acknowledge()来ACK。
                // message.acknowledge();
                //2.建议异步处理收到的消息，确保onMessage函数里没有耗时逻辑。
                // 如果业务处理耗时过程过长阻塞住线程，可能会影响SDK收到消息后的正常回调。
                executorService.submit(() -> processMessage(message));
            } catch (Exception e) {
                logger.error("submit task occurs exception ", e);
            }
        }
    };

    /**
     * 在这里处理您收到消息后的具体业务逻辑。
     * <p>
     * 函数名：处理消息
     *
     * @param message 服务端订阅的消息
     */
    private static void processMessage(Message message) {
        try {
            // 获取消息体字节数组
            byte[] body = message.getBody(byte[].class);

            // 获取 Topic，表示这条消息是与这个 topic 相关的
            String topic = message.getStringProperty("topic");
            // messageId
            String messageId = message.getStringProperty("messageId");
            // 字节数组变为消息体字符串
            if (Base64.isBase64(body)) {
                body = Base64.decodeBase64(body);
            }
            String content = new String(body);

            // 应用服务端缓存云端推送的数据
            if (topic.contains("property")) {
                ResultUtil.addObject("property", topic.isBlank() ? null : topic, messageId.isBlank() ? null : messageId, content);
            } else if (topic.contains("status")) {
                ResultUtil.addObject("status", topic.isBlank() ? null : topic, messageId.isBlank() ? null : messageId, content);
            } else if (topic.contains("event")) {
                ResultUtil.addObject("event", topic.isBlank() ? null : topic, messageId.isBlank() ? null : messageId, content);
            } else if (topic.contains("downlink")) {
                ResultUtil.addObject("service", topic.isBlank() ? null : topic, messageId.isBlank() ? null : messageId, content);
            }

            logger.info("receive message" + ", topic = " + topic + ", messageId = " + messageId + ", content = " + content);

        } catch (Exception e) {
            logger.error("processMessage occurs error ", e);
        }
    }

    // 这个是监听服务端的客户机与云端的连接状态的监听类
    private static JmsConnectionListener myJmsConnectionListener = new JmsConnectionListener() {
        /** * 连接成功建立 */
        @Override
        public void onConnectionEstablished(URI remoteURI) {
            logger.info("onConnectionEstablished, remoteUri:{}", remoteURI);
        }

        /** * 尝试过最大重试次数之后，最终连接失败。 */
        @Override
        public void onConnectionFailure(Throwable error) {
            logger.error("onConnectionFailure, {}", error.getMessage());
        }

        /** * 连接中断。 */
        @Override
        public void onConnectionInterrupted(URI remoteURI) {
            logger.info("onConnectionInterrupted, remoteUri:{}", remoteURI);
        }

        /** * 连接中断后又自动重连上。 */
        @Override
        public void onConnectionRestored(URI remoteURI) {
            logger.info("onConnectionRestored, remoteUri:{}", remoteURI);
        }

        @Override
        public void onInboundMessage(JmsInboundMessageDispatch envelope) {
        }

        @Override
        public void onSessionClosed(Session session, Throwable cause) {
        }

        @Override
        public void onConsumerClosed(MessageConsumer consumer, Throwable cause) {
        }

        @Override
        public void onProducerClosed(MessageProducer producer, Throwable cause) {
        }
    };

    /**
     * password签名计算方法，请参见上一篇文档：AMQP客户端接入说明。
     */
    private static String doSign(String toSignString, String secret, String signMethod) throws Exception {
        SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), signMethod);
        Mac mac = Mac.getInstance(signMethod);
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(toSignString.getBytes());
        return Base64.encodeBase64String(rawHmac);
    }
}
