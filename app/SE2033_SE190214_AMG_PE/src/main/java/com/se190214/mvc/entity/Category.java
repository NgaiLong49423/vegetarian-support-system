package com.se190214.mvc.entity;
@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "categories")
public class Category {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "category_id", nullable = false)
private java.lang.Integer id;

@jakarta.validation.constraints.Size(max = 100)
@jakarta.validation.constraints.NotNull
@org.hibernate.annotations.Nationalized
@jakarta.persistence.Column(name = "category_name", nullable = false, length = 100)
private java.lang.String categoryName;

public java.lang.Integer getId() {
  return id;
}public void setId(java.lang.Integer id) {
  this.id = id;
}

public java.lang.String getCategoryName() {
  return categoryName;
}public void setCategoryName(java.lang.String categoryName) {
  this.categoryName = categoryName;
}

}