package com.hxl.plugin.springboot.invoke.utils;

import com.hxl.plugin.springboot.invoke.IdeaTopic;
import com.hxl.plugin.springboot.invoke.bean.RequestEnvironment;
import com.hxl.plugin.springboot.invoke.model.*;
import com.hxl.plugin.springboot.invoke.state.CoolRequestEnvironmentPersistentComponent;
import com.hxl.plugin.springboot.invoke.utils.service.CacheStorageService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageHandlers {
    private final UserProjectManager userProjectManager;
    private final Map<String, ServerMessageHandler> messageHandlerMap = new HashMap<>();
    private final List<String> canLoadCacheType = new ArrayList<>();

    public MessageHandlers(UserProjectManager userProjectManager) {
        this.userProjectManager = userProjectManager;
        putNewMessageHandler("controller", new ControllerInfoServerMessageHandler());
        putNewMessageHandler("response_info", new ResponseInfoServerMessageHandler());
        putNewMessageHandler("clear", new ClearServerMessageHandler());
        putNewMessageHandler("scheduled", new ScheduledServerMessageHandler());
        putNewMessageHandler("startup", new ProjectStartupServerMessageHandler());
        putNewMessageHandler("invoke_receive", new RequestReceiveMessageHandler());
        putNewMessageHandler("spring_gateway", new SpringGatewayMessageHandler());

    }

    private void putNewMessageHandler(String type, ServerMessageHandler serverMessageHandler) {
        if (serverMessageHandler instanceof BaseServerMessageHandler) {
            canLoadCacheType.add(type);
            ((BaseServerMessageHandler) serverMessageHandler).setMsgType(type);
        }
        messageHandlerMap.put(type, serverMessageHandler);
    }

    public void loadCache() {
//        for (String msgType : this.canLoadCacheType) {
//            String customCache = ApplicationManager.getApplication().getService(CacheStorageService.class).getCustomCache(msgType, userProjectManager.getProject());
//            if (StringUtils.isEmpty(customCache)) continue;
//            handlerMessage(customCache,false);
//        }
    }

    public void handlerMessage(String msg, boolean dynamic) {
        try {
            MessageType messageType = ObjectMappingUtils.readValue(msg, MessageType.class);
            if (!StringUtils.isEmpty(messageType)) {
                if (messageHandlerMap.containsKey(messageType.getType())) {
                    messageHandlerMap.get(messageType.getType()).handler(msg, dynamic);
                }
            }
        } catch (Exception ignored) {
        }
    }

    interface ServerMessageHandler {
        void handler(String msg, boolean dynamic);
    }

    /**
     * 负责序列化数据
     */
    abstract class BaseServerMessageHandler implements ServerMessageHandler {
        public boolean isSerialize;
        private String msgType;

        @Override
        public void handler(String msg, boolean dynamic) {
            doHandler(msg, dynamic);
            if (!isSerialize) return;
            ApplicationManager.getApplication().getService(CacheStorageService.class)
                    .storageCustomCache(msgType, msg, userProjectManager.getProject());
        }

        public BaseServerMessageHandler(boolean isSerialize) {
            this.isSerialize = isSerialize;
        }

        public boolean isSerialize() {
            return isSerialize;
        }

        public void setSerialize(boolean serialize) {
            isSerialize = serialize;
        }

        public String getMsgType() {
            return msgType;
        }

        public void setMsgType(String msgType) {
            this.msgType = msgType;
        }

        public abstract void doHandler(String msg, boolean dynamic);

        public BaseServerMessageHandler() {
            this(false);
        }
    }

    static class MessageType {
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    class SpringGatewayMessageHandler extends BaseServerMessageHandler {
        public SpringGatewayMessageHandler() {
            super(false);
        }

        @Override
        public void doHandler(String msg, boolean dynamic) {
            GatewayModel gatewayModel = ObjectMappingUtils.readValue(msg, GatewayModel.class);
            CoolRequestEnvironmentPersistentComponent.State instance = CoolRequestEnvironmentPersistentComponent.getInstance();

            for (GatewayModel.Gateway gateway : gatewayModel.getGateways()) {
                RequestEnvironment requestEnvironment = new RequestEnvironment();
                requestEnvironment.setId(gateway.getId());
                requestEnvironment.setEnvironmentName(gateway.getRouteId());
                requestEnvironment.setPrefix("http://localhost:" + gatewayModel.getPort() + StringUtils.joinUrlPath(gatewayModel.getContext(), addPrefix(gateway.getPrefix())));
                if (instance.environments.contains(requestEnvironment)) continue;
                instance.environments.add(requestEnvironment);
            }
            ApplicationManager.getApplication().getMessageBus().syncPublisher(IdeaTopic.ENVIRONMENT_ADDED).event();
        }
    }

    private String addPrefix(String prefix) {
        if (StringUtils.isEmpty(prefix)) return "";
        if (prefix.startsWith("/")) return prefix;
        return "/" + prefix;
    }

    class ProjectStartupServerMessageHandler implements ServerMessageHandler {
        @Override
        public void handler(String msg, boolean dynamic) {
            ProjectStartupModel projectStartupModel = ObjectMappingUtils.readValue(msg, ProjectStartupModel.class);
//            userProjectManager.onUserProjectStartup(projectStartupModel);
        }
    }

    class RequestReceiveMessageHandler implements ServerMessageHandler {
        @Override
        public void handler(String msg, boolean dynamic) {
            InvokeReceiveModel invokeReceiveModel = ObjectMappingUtils.readValue(msg, InvokeReceiveModel.class);
            if (invokeReceiveModel != null) {
                userProjectManager.onInvokeReceive(invokeReceiveModel);
            }
        }
    }

    class ControllerInfoServerMessageHandler extends BaseServerMessageHandler {
        public ControllerInfoServerMessageHandler() {
            super(true);
        }

        @Override
        public void doHandler(String msg, boolean dynamic) {
            RequestMappingModel requestMappingModel = ObjectMappingUtils.readValue(msg, RequestMappingModel.class);
            if (requestMappingModel == null) return;
            userProjectManager.addSpringBootApplicationInstance(requestMappingModel.getServerPort(), requestMappingModel.getPluginPort());
            ApplicationManager.getApplication().runReadAction(() -> {
                requestMappingModel.getControllers().forEach(controller -> {
                    Module classNameModule = PsiUtils.findClassNameModule(userProjectManager.getProject(), controller.getSimpleClassName());
                    controller.setModuleName(classNameModule == null ? "unknown" : classNameModule.getName());
                    controller.setId(ComponentIdUtils.getMd5(userProjectManager.getProject(), controller));
                    controller.setSpringBootStartPort(requestMappingModel.getPluginPort());
                });
                userProjectManager.addComponent(requestMappingModel.getControllers());
            });

        }
    }

    class ResponseInfoServerMessageHandler implements ServerMessageHandler {
        @Override
        public void handler(String msg, boolean dynamic) {
            InvokeResponseModel invokeResponseModel = ObjectMappingUtils.readValue(msg, InvokeResponseModel.class);
            if (invokeResponseModel == null) return;
            invokeResponseModel.setId(userProjectManager.getDynamicControllerRawId(invokeResponseModel.getId()));

            userProjectManager.getProject().getMessageBus()
                    .syncPublisher(IdeaTopic.HTTP_RESPONSE)
                    .onResponseEvent(invokeResponseModel.getId(), invokeResponseModel);
        }
    }

    class ClearServerMessageHandler implements ServerMessageHandler {
        @Override
        public void handler(String msg, boolean dynamic) {
//            userProjectManager.getProject().getMessageBus()
//                    .syncPublisher(IdeaTopic.DELETE_ALL_REQUEST)
//                    .event();
        }
    }

    class ScheduledServerMessageHandler extends BaseServerMessageHandler {
        public ScheduledServerMessageHandler() {
            super(true);
        }

        @Override
        public void doHandler(String msg, boolean dynamic) {
            ScheduledModel scheduledModel = ObjectMappingUtils.readValue(msg, ScheduledModel.class);
            if (scheduledModel == null) return;

            ApplicationManager.getApplication().runReadAction(() -> {
                scheduledModel.getScheduledInvokeBeans().forEach(dynamicSpringScheduled -> {
                    Module classNameModule = PsiUtils.findClassNameModule(userProjectManager.getProject(), dynamicSpringScheduled.getClassName());
                    dynamicSpringScheduled.setModuleName(classNameModule == null ? "" : classNameModule.getName());
                    dynamicSpringScheduled.setServerPort(scheduledModel.getPort());
                    dynamicSpringScheduled.setId(ComponentIdUtils.getMd5(userProjectManager.getProject(), dynamicSpringScheduled));
                });
                userProjectManager.addComponent(scheduledModel.getScheduledInvokeBeans());
            });
        }
    }

}
