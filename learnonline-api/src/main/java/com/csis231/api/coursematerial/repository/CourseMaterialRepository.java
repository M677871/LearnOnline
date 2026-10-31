package com.csis231.api.coursematerial.repository;

import com.csis231.api.coursematerial.model.CourseMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, Long> {

    List<CourseMaterial> findByCourse_Id(Long courseId);
}
