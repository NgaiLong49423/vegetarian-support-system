package tech.mamxanh.recipe.service;

import org.springframework.stereotype.Component;
import tech.mamxanh.recipe.dto.response.AiChatResponse.Source;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** Search only the versioned, source-attributed knowledge snippets approved for the assistant. */
@Component
public class ApprovedAiKnowledgeRetriever {
    private static final Pattern WORDS = Pattern.compile("[\\p{L}\\p{N}]{3,}");
    private static final List<Chunk> APPROVED = List.of(
            new Chunk("FR-51", "Phạm vi trợ lý ẩm thực chay",
                    "Trợ lý hỗ trợ ẩm thực chay, công thức, nguyên liệu, cách chế biến và thông tin dinh dưỡng phổ thông. "
                    + "Ưu tiên bài công thức công khai thực tế của Mâm Xanh; không bịa dữ kiện công thức. "
                    + "Nếu bài không ghi rõ nguyên liệu hoặc thời gian, phải nói rõ dữ liệu không có.",
                    "docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md#fr-51"),
            new Chunk("BR-09/BR-41", "Giới hạn thông tin y tế và dinh dưỡng",
                    "AI chỉ cung cấp thông tin dinh dưỡng phổ thông, không chẩn đoán, kê điều trị hoặc lập chế độ ăn điều trị bệnh. "
                    + "BMI và kết quả dinh dưỡng chỉ mang tính tham khảo, không thay thế bác sĩ hoặc chuyên gia dinh dưỡng y tế.",
                    "docs/requirements/srs/BUSINESS-RULES.md#br-09"),
            new Chunk("BR-79", "Đồng ý dữ liệu và thao tác có xác nhận",
                    "Dữ liệu cá nhân chỉ được gửi tới Google Gemini sau khi người dùng đồng ý rõ ràng đúng loại dữ liệu, mục đích và bên nhận. "
                    + "Khi đổi loại dữ liệu, mục đích hoặc bên nhận phải xin đồng ý lại. Mode Có xác nhận cần preview trước khi ghi; "
                    + "đăng công thức và xóa vĩnh viễn luôn cần xác nhận rõ.",
                    "docs/requirements/srs/BUSINESS-RULES.md#br-79"),
            new Chunk("FR-66", "Nguồn và quyền của AI Assistant",
                    "MVP chỉ dùng dữ liệu Mâm Xanh và tài liệu nội bộ được duyệt, không tìm Internet. "
                    + "Mọi thao tác thực tế phải qua quyền, ownership, validation và kết quả Backend; không có công cụ Admin. "
                    + "Nếu không có bằng chứng nguồn, AI cần nêu giới hạn hoặc từ chối khẳng định.",
                    "docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md#fr-66"));

    public List<Source> retrieve(String query) {
        if (query != null && !query.isBlank() && query.length() <= 1000) {
            try (var retriever = new tech.mamxanh.integration.ai.knowledge.HybridKnowledgeRetriever(
                    BUSINESS, null, null, java.time.Clock.systemUTC())) {
                var queryTerms = terms(query);
                var hits = retriever.search(query).hits().stream().filter(hit -> {
                    var evidenceTerms = terms(hit.title() + " " + hit.excerpt());
                    long overlap = queryTerms.stream().filter(evidenceTerms::contains).count();
                    var ordered = WORDS.matcher(fold(query)).results().map(java.util.regex.MatchResult::group).toList();
                    String evidence = String.join(" ", WORDS.matcher(fold(hit.title() + " " + hit.excerpt()))
                            .results().map(java.util.regex.MatchResult::group).toList());
                    boolean phrase = java.util.stream.IntStream.range(1, ordered.size())
                            .anyMatch(i -> evidence.contains(ordered.get(i-1) + " " + ordered.get(i)));
                    return phrase && overlap >= Math.max(2, Math.ceil(queryTerms.size() * .6));
                }).toList();
                if (!hits.isEmpty()) return hits.stream().map(hit -> new Source("APPROVED_DOCUMENT",
                        hit.knowledgeId(), hit.title(),
                        "[keyword_degraded; derived business guidance, not current account data] " + hit.excerpt(),
                        hit.sources().getFirst(), hit.version())).toList();
            }
        }
        Set<String> terms = terms(query);
        if (terms.isEmpty()) return List.of();
        List<Scored> matches = new ArrayList<>();
        for (Chunk chunk : APPROVED) {
            Set<String> docTerms = terms(chunk.title + " " + chunk.text);
            long overlap = terms.stream().filter(docTerms::contains).count();
            if (overlap >= 2) matches.add(new Scored(chunk, overlap));
        }
        return matches.stream().sorted(Comparator.comparingLong(Scored::score).reversed())
                .limit(3).map(item -> new Source("APPROVED_DOCUMENT", item.chunk.id, item.chunk.title,
                        item.chunk.text, item.chunk.uri)).toList();
    }

    private static final tech.mamxanh.integration.ai.knowledge.KnowledgeCatalog BUSINESS =
            tech.mamxanh.integration.ai.knowledge.KnowledgeCatalog.business();

    private static Set<String> terms(String value) {
        if (value == null || value.isBlank()) return Set.of();
        String normalized = fold(value);
        var matcher = WORDS.matcher(normalized);
        java.util.HashSet<String> terms = new java.util.HashSet<>();
        while (matcher.find()) terms.add(matcher.group());
        return Set.copyOf(terms);
    }

    private static String fold(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replace('đ', 'd');
    }

    private record Chunk(String id, String title, String text, String uri) { }
    private record Scored(Chunk chunk, long score) { }
}
