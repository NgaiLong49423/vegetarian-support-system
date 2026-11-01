package tech.mamxanh.integration.ai.adk;

import org.springframework.context.ApplicationEventPublisher;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.subscription.service.AiTokenQuotaService;

/** Calls the injected transactional service proxy; never self-invokes accounting transactions. */
public final class MemberAccountingPolicy implements AdkModelGateway.Policy {
    private final CurrentUserService users;
    private final AiTokenQuotaService quota;
    private final ApplicationEventPublisher events;
    public MemberAccountingPolicy(CurrentUserService users, AiTokenQuotaService quota, ApplicationEventPublisher events) {
        this.users=users; this.quota=quota; this.events=events;
    }
    public Object beforeCall(AdkModelGateway.Call call) {
        Long member = null;
        try {
            var authUserId = users.findAuthenticatedUserId();
            if (authUserId.isPresent()) {
                member = authUserId.get();
            } else {
                var current = users.requireActiveMember();
                if (current != null) member = current.id();
            }
        } catch (Exception ignored) {
            return null;
        }
        if (member == null) return null;
        return quota.beginCall(member,null,"FR-51",call.model(),call.startedAt());
    }
    public void onUsableResponse(AdkModelGateway.Call call,Object context,AdkModelGateway.Usage usage) {
        if (context == null) return;
        if (!(context instanceof AiTokenQuotaService.CallStartSnapshot snapshot)) throw new IllegalStateException("Missing call-start snapshot");
        quota.completeCall(call.id(),snapshot,usage.metadata());
        if(events!=null) events.publishEvent(new AiUsageEvent(call.id(),snapshot.userId(),null,snapshot.feature(),snapshot.startedAt(),usage.metadata()));
    }
}
