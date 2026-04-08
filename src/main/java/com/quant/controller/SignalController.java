package com.quant.controller;

import com.quant.entity.SignalAlert;
import com.quant.repository.SignalAlertRepository;
import com.quant.service.AlertService;
import com.quant.service.SignalGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 信号提醒控制器
 *
 * 提供信号提醒相关的REST API：
 * - 信号生成
 * - 信号查询
 * - 提醒规则管理
 */
@RestController
@RequestMapping("/api/signal")
public class SignalController {

    private final SignalGenerator signalGenerator;
    private final SignalAlertRepository signalAlertRepository;
    private final AlertService alertService;

    public SignalController(SignalGenerator signalGenerator,
                            SignalAlertRepository signalAlertRepository,
                            AlertService alertService) {
        this.signalGenerator = signalGenerator;
        this.signalAlertRepository = signalAlertRepository;
        this.alertService = alertService;
    }

    /**
     * 获取信号列表
     *
     * @param stockCode 股票代码（可选）
     * @param status 状态（可选）
     * @return 信号列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<SignalAlert>> getSignalList(
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) String status) {
        List<SignalAlert> signals;

        if (stockCode != null && !stockCode.isEmpty()) {
            signals = signalAlertRepository.findByStockCode(stockCode);
        } else if (status != null && !status.isEmpty()) {
            signals = signalAlertRepository.findByAlertStatus(status);
        } else {
            signals = signalAlertRepository.findAll();
        }

        return ResponseEntity.ok(signals);
    }

    /**
     * 生成指定股票的信号
     *
     * @param stockCode 股票代码
     * @param stockName 股票名称
     * @param days 分析天数（默认60）
     * @return 生成的信号列表
     */
    @PostMapping("/generate")
    public ResponseEntity<List<SignalAlert>> generateSignals(
            @RequestParam String stockCode,
            @RequestParam(required = false) String stockName,
            @RequestParam(defaultValue = "60") int days) {
        List<SignalAlert> signals = signalGenerator.generateSignals(
            stockCode,
            stockName != null ? stockName : stockCode,
            days
        );
        return ResponseEntity.ok(signals);
    }

    /**
     * 获取待发送的信号
     *
     * @return 待发送信号列表
     */
    @GetMapping("/pending")
    public ResponseEntity<List<SignalAlert>> getPendingSignals() {
        List<SignalAlert> signals = signalGenerator.getPendingSignals();
        return ResponseEntity.ok(signals);
    }

    /**
     * 发送信号提醒
     *
     * @param id 信号ID
     * @param channels 发送渠道（逗号分隔）
     * @return 发送结果
     */
    @PostMapping("/send/{id}")
    public ResponseEntity<SignalAlert> sendSignal(
            @PathVariable Long id,
            @RequestParam(required = false) String channels) {
        return signalAlertRepository.findById(id)
            .map(signal -> {
                String sendChannels = channels != null ? channels : signal.getSendChannels();
                alertService.sendAlert(signal, sendChannels);
                signal.setUpdateTime(LocalDateTime.now());
                return ResponseEntity.ok(signalAlertRepository.save(signal));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 批量发送待处理信号
     *
     * @return 发送结果
     */
    @PostMapping("/send-all")
    public ResponseEntity<String> sendAllPendingSignals() {
        List<SignalAlert> signals = signalGenerator.getPendingSignals();
        int successCount = 0;
        int failCount = 0;

        for (SignalAlert signal : signals) {
            try {
                alertService.sendAlert(signal, signal.getSendChannels());
                signalAlertRepository.save(signal);
                successCount++;
            } catch (Exception e) {
                failCount++;
            }
        }

        return ResponseEntity.ok(String.format("发送完成: 成功%d, 失败%d", successCount, failCount));
    }

    /**
     * 更新信号状态
     *
     * @param id 信号ID
     * @param status 新状态
     * @param note 备注
     * @return 更新后的信号
     */
    @PutMapping("/status/{id}")
    public ResponseEntity<SignalAlert> updateSignalStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String note) {
        return signalAlertRepository.findById(id)
            .map(signal -> {
                signal.setAlertStatus(status);
                if (note != null) {
                    signal.setUserNote(note);
                }
                signal.setUpdateTime(LocalDateTime.now());
                return ResponseEntity.ok(signalAlertRepository.save(signal));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 删除信号
     *
     * @param id 信号ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSignal(@PathVariable Long id) {
        if (signalAlertRepository.existsById(id)) {
            signalAlertRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 获取最近的信号
     *
     * @param stockCode 股票代码
     * @param limit 数量限制
     * @return 信号列表
     */
    @GetMapping("/recent/{stockCode}")
    public ResponseEntity<List<SignalAlert>> getRecentSignals(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "10") int limit) {
        List<SignalAlert> signals = signalGenerator.getRecentSignals(stockCode, limit);
        return ResponseEntity.ok(signals);
    }

    /**
     * 测试通知渠道
     *
     * @param channel 渠道类型
     * @return 测试结果
     */
    @PostMapping("/test-channel")
    public ResponseEntity<String> testNotificationChannel(@RequestParam String channel) {
        boolean success = alertService.sendTestNotification(channel);
        return ResponseEntity.ok(success ? "发送成功" : "发送失败");
    }
}