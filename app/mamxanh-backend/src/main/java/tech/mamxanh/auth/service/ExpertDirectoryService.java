package tech.mamxanh.auth.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.mamxanh.auth.dto.response.ExpertDirectoryEntryResponse;
import tech.mamxanh.auth.repository.MemberProfileRepository;
import tech.mamxanh.auth.repository.UserFollowRepository;
import tech.mamxanh.common.response.PageResponse;

/** FR-61 public directory of active Experts. */
@Service
public class ExpertDirectoryService {
    private final MemberProfileRepository profileRepository;
    private final UserFollowRepository followRepository;
    private final CurrentUserService currentUserService;

    public ExpertDirectoryService(MemberProfileRepository profileRepository,
            UserFollowRepository followRepository, CurrentUserService currentUserService) {
        this.profileRepository = profileRepository;
        this.followRepository = followRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpertDirectoryEntryResponse> search(String query, int page, int size) {
        String normalized = query == null || query.isBlank() ? null : query.trim();
        var results = profileRepository.searchActiveExperts(normalized,
                PageRequest.of(page, size, Sort.by(Sort.Order.asc("displayName"), Sort.Order.asc("id"))));
        List<Long> expertIds = results.getContent().stream().map(profile -> profile.getId()).toList();
        Map<Long, Long> followerCounts = new HashMap<>();
        if (!expertIds.isEmpty()) {
            followRepository.findActiveFollowerCounts(expertIds)
                    .forEach(count -> followerCounts.put(count.getUserId(), count.getFollowerCount()));
        }
        Set<Long> followedIds = currentUserService.findAuthenticatedUserId()
                .filter(ignored -> !expertIds.isEmpty())
                .map(viewerId -> followRepository.findFollowedIds(viewerId, expertIds).stream().collect(Collectors.toSet()))
                .orElseGet(Set::of);

        List<ExpertDirectoryEntryResponse> items = results.getContent().stream()
                .map(profile -> new ExpertDirectoryEntryResponse(profile.getId(), profile.getDisplayName(),
                        profile.getAvatarUrl(), profile.getBio(), followerCounts.getOrDefault(profile.getId(), 0L),
                        followedIds.contains(profile.getId())))
                .toList();
        return PageResponse.of(items, results.getNumber(), results.getSize(), results.getTotalElements());
    }
}
