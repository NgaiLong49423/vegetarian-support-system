package tech.mamxanh.expertapplication.dto;

import jakarta.validation.constraints.Size;

public record ApproveRequest(@Size(max = 1000) String note) {
    public ApproveRequest { if (note != null) note = note.trim(); }
}
