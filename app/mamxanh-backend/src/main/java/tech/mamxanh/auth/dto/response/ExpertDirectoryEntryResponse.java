package tech.mamxanh.auth.dto.response;

/** Public fields shown by the expert directory; no account or private profile data. */
public record ExpertDirectoryEntryResponse(
        long userId,
        String displayName,
        String avatarUrl,
        String bio,
        long followerCount,
        boolean following) {
}
