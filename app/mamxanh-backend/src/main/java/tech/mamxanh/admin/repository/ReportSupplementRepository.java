package tech.mamxanh.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.admin.entity.ReportSupplementEntity;

import java.util.List;

public interface ReportSupplementRepository extends JpaRepository<ReportSupplementEntity, Long> {
    List<ReportSupplementEntity> findAllByReportIdOrderByCreatedAtAscIdAsc(long reportId);

    long countByReportId(long reportId);
}
