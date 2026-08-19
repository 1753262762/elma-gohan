package com.elma.gohan.controller;

import com.elma.gohan.application.RecommendationService;
import com.elma.gohan.application.ValidationFailedException;
import com.elma.gohan.controller.api.CreateRecommendationRequest;
import com.elma.gohan.controller.api.FeedbackResponse;
import com.elma.gohan.controller.api.RecommendationResponse;
import com.elma.gohan.controller.api.SubmitFeedbackRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 三个接口严格按 contracts/openapi.yaml 的路径、请求头与响应码实现。
 *
 * 契约疑点(已向负责人标记,未擅改契约):契约把 reroll/feedback 的路径写成字面量
 * /recommendations/glm-5.3_common/...,但同一操作又声明了 in:path、format:uuid 的
 * 推荐会话 id 参数,两者矛盾(字面量路径无法寻址会话)。此处按声明的 path 参数实现为
 * {id};待契约修正为 /recommendations/{id}/reroll 后端无需再改。
 */
@RestController
@RequestMapping("/api/v1")
public class RecommendationController {

    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @PostMapping("/recommendations")
    public ResponseEntity<RecommendationResponse> create(
            @RequestHeader("X-Anonymous-User-Id") String anonymousUserIdHeader,
            @Valid @RequestBody CreateRecommendationRequest request) {
        UUID anonymousUserId = parseUserId(anonymousUserIdHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(anonymousUserId, request));
    }

    @PostMapping("/recommendations/{id}/reroll")
    public RecommendationResponse reroll(
            @RequestHeader("X-Anonymous-User-Id") String anonymousUserIdHeader,
            @PathVariable("id") String id) {
        UUID anonymousUserId = parseUserId(anonymousUserIdHeader);
        return service.reroll(anonymousUserId, parseRecommendationId(id));
    }

    @PostMapping("/recommendations/{id}/feedback")
    public ResponseEntity<FeedbackResponse> feedback(
            @RequestHeader("X-Anonymous-User-Id") String anonymousUserIdHeader,
            @PathVariable("id") String id,
            @Valid @RequestBody SubmitFeedbackRequest request) {
        UUID anonymousUserId = parseUserId(anonymousUserIdHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.submitFeedback(anonymousUserId, parseRecommendationId(id), request));
    }

    private UUID parseUserId(String header) {
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            throw new ValidationFailedException("X-Anonymous-User-Id", "必须是合法的 UUID");
        }
    }

    private UUID parseRecommendationId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ValidationFailedException("id", "必须是合法的 UUID");
        }
    }
}
