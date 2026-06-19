package com.virtualbank.assistant.web;

import com.virtualbank.assistant.AssistantService;
import com.virtualbank.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * Answers one question for the authenticated caller. The identity is taken
     * from the validated JWT, never from the request, so the tools can only read
     * the caller's own data. The reply is the model's answer, not a data dump.
     */
    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        // Asserts an authenticated caller is present before doing any work.
        CurrentUser.requireId();
        return new ChatResponse(assistantService.reply(request.message()));
    }
}
