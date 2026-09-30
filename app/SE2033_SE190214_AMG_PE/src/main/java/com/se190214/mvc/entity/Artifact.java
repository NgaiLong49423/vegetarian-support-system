package com.se190214.mvc.entity;
@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "artifacts")
public class Artifact {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "artifact_id", nullable = false)
private java.lang.Integer id;

@jakarta.validation.constraints.Size(max = 6)
@jakarta.validation.constraints.NotNull
@jakarta.persistence.Column(name = "artifact_code", nullable = false, length = 6)
private java.lang.String artifactCode;

@jakarta.validation.constraints.Size(max = 200)
@jakarta.validation.constraints.NotNull
@org.hibernate.annotations.Nationalized
@jakarta.persistence.Column(name = "artifact_name", nullable = false, length = 200)
private java.lang.String artifactName;

@jakarta.validation.constraints.NotNull
@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@jakarta.persistence.JoinColumn(name = "category_id", nullable = false)
private com.se190214.mvc.entity.Category category;

@jakarta.validation.constraints.NotNull
@jakarta.persistence.Column(name = "estimated_value", nullable = false, precision = 10)
private java.math.BigDecimal estimatedValue;

@jakarta.validation.constraints.NotNull
@jakarta.persistence.Column(name = "age_years", nullable = false)
private java.lang.Integer ageYears;

@jakarta.validation.constraints.NotNull
@jakarta.persistence.Column(name = "acquisition_date", nullable = false)
private java.time.LocalDate acquisitionDate;

@jakarta.validation.constraints.Size(max = 10)
@jakarta.validation.constraints.NotNull
@org.hibernate.annotations.Nationalized
@jakarta.persistence.Column(name = "status", nullable = false, length = 10)
private java.lang.String status;

public java.lang.Integer getId() {
  return id;
}public void setId(java.lang.Integer id) {
  this.id = id;
}

public java.lang.String getArtifactCode() {
  return artifactCode;
}public void setArtifactCode(java.lang.String artifactCode) {
  this.artifactCode = artifactCode;
}

public java.lang.String getArtifactName() {
  return artifactName;
}public void setArtifactName(java.lang.String artifactName) {
  this.artifactName = artifactName;
}

public com.se190214.mvc.entity.Category getCategory() {
  return category;
}public void setCategory(com.se190214.mvc.entity.Category category) {
  this.category = category;
}

public java.math.BigDecimal getEstimatedValue() {
  return estimatedValue;
}public void setEstimatedValue(java.math.BigDecimal estimatedValue) {
  this.estimatedValue = estimatedValue;
}

public java.lang.Integer getAgeYears() {
  return ageYears;
}public void setAgeYears(java.lang.Integer ageYears) {
  this.ageYears = ageYears;
}

public java.time.LocalDate getAcquisitionDate() {
  return acquisitionDate;
}public void setAcquisitionDate(java.time.LocalDate acquisitionDate) {
  this.acquisitionDate = acquisitionDate;
}

public java.lang.String getStatus() {
  return status;
}public void setStatus(java.lang.String status) {
  this.status = status;
}

}