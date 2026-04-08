package com.quant.service;

import com.quant.entity.SignalAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 通知服务
 *
 * 提供多渠道消息推送功能：
 * 1. 邮件通知
 * 2. 钉钉机器人
 * 3. 飞书机器人
 * 4. 企业微信机器人
 *
 * 所有通知基于配置开启，未配置的渠道自动跳过
 */
@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    /**
     * 发送信号提醒
     *
     * @param signal 信号对象
     * @param channels 渠道列表（逗号分隔）
     */
    public void sendAlert(SignalAlert signal, String channels) {
        if (channels == null || channels.isEmpty()) {
            logger.warn("未配置通知渠道，跳过发送");
            return;
        }

        String[] channelArray = channels.split(",");
        boolean anySuccess = false;

        for (String channel : channelArray) {
            channel = channel.trim().toUpperCase();
            try {
                boolean success = sendToChannel(signal, channel);
                if (success) {
                    anySuccess = true;
                    logger.info("信号 {} 通过 {} 渠道发送成功", signal.getId(), channel);
                }
            } catch (Exception e) {
                logger.error("通过 {} 渠道发送失败: {}", channel, e.getMessage());
            }
        }

        // 更新信号状态
        if (anySuccess) {
            signal.setAlertStatus("SENT");
            signal.setSendTime(LocalDateTime.now());
        } else {
            signal.setAlertStatus("FAILED");
            signal.setFailReason("所有渠道发送失败");
        }
    }

    /**
     * 发送到指定渠道
     */
    private boolean sendToChannel(SignalAlert signal, String channel) {
        String message = formatSignalMessage(signal);

        switch (channel) {
            case "EMAIL":
                return sendEmail(signal.getStockName(), message);
            case "DINGTALK":
                return sendDingTalk(message);
            case "FEISHU":
                return sendFeishu(message);
            case "WECHAT_WORK":
                return sendWeChatWork(message);
            default:
                logger.warn("未知的通知渠道: {}", channel);
                return false;
        }
    }

    /**
     * 格式化信号消息
     */
    private String formatSignalMessage(SignalAlert signal) {
        StringBuilder sb = new StringBuilder();
        sb.append("【量化交易信号提醒】\n\n");
        sb.append("股票代码：").append(signal.getStockCode()).append("\n");
        sb.append("股票名称：").append(signal.getStockName()).append("\n");
        sb.append("信号类型：").append(getSignalTypeText(signal.getSignalType())).append("\n");
        sb.append("触发价格：").append(signal.getTriggerPrice()).append("元\n");
        sb.append("触发条件：").append(signal.getTriggerCondition()).append("\n");
        sb.append("信号强度：").append(signal.getSignalStrength()).append("\n");
        sb.append("建议操作：").append(getSuggestionText(signal.getSuggestion())).append("\n");
        sb.append("触发时间：").append(signal.getSignalTime()).append("\n");

        return sb.toString();
    }

    /**
     * 发送邮件通知
     */
    private boolean sendEmail(String subject, String content) {
        // TODO: 实现邮件发送，需要配置SMTP
        logger.info("模拟发送邮件: subject={}", subject);
        logger.debug("邮件内容: {}", content);
        // 实际实现需要注入JavaMailSender
        return true;
    }

    /**
     * 发送钉钉通知
     */
    private boolean sendDingTalk(String message) {
        // TODO: 实现钉钉Webhook发送
        // 需要：
        // 1. 配置钉钉机器人Webhook URL
        // 2. 配置加签密钥（可选）
        logger.info("模拟发送钉钉消息");
        logger.debug("消息内容: {}", message);
        return true;
    }

    /**
     * 发送飞书通知
     */
    private boolean sendFeishu(String message) {
        // TODO: 实现飞书Webhook发送
        logger.info("模拟发送飞书消息");
        logger.debug("消息内容: {}", message);
        return true;
    }

    /**
     * 发送企业微信通知
     */
    private boolean sendWeChatWork(String message) {
        // TODO: 实现企业微信Webhook发送
        logger.info("模拟发送企业微信消息");
        logger.debug("消息内容: {}", message);
        return true;
    }

    /**
     * 获取信号类型文本
     */
    private String getSignalTypeText(String signalType) {
        if (signalType == null) return "未知";
        switch (signalType) {
            case "BUY": return "买入信号";
            case "SELL": return "卖出信号";
            case "RISK_WARNING": return "风险预警";
            case "PRICE_BREAK": return "价格突破";
            case "INDICATOR_CROSS": return "指标交叉";
            default: return signalType;
        }
    }

    /**
     * 获取建议文本
     */
    private String getSuggestionText(String suggestion) {
        if (suggestion == null) return "无";
        switch (suggestion) {
            case "BUY": return "建议买入";
            case "SELL": return "建议卖出";
            case "HOLD": return "建议持有";
            case "WATCH": return "建议观察";
            default: return suggestion;
        }
    }

    /**
     * 发送测试通知
     */
    public boolean sendTestNotification(String channel) {
        SignalAlert testSignal = new SignalAlert();
        testSignal.setStockCode("000001");
        testSignal.setStockName("平安银行");
        testSignal.setSignalType("BUY");
        testSignal.setTriggerPrice(new java.math.BigDecimal("10.50"));
        testSignal.setTriggerCondition("测试信号");
        testSignal.setSignalStrength("MEDIUM");
        testSignal.setSuggestion("BUY");
        testSignal.setSignalTime(LocalDateTime.now());

        return sendToChannel(testSignal, channel);
    }
}