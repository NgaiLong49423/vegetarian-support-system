package tech.mamxanh.admin.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.admin.entity.ReportSupplementEntity;

public interface ReportSupplementRepository extends JpaRepository<ReportSupplementEntity, Long> {
    List<ReportSupplementEntity> findAllByReportIdOrderByCreatedAtAscIdAsc(long reportId);

    long countByReportId(long reportId);
}
