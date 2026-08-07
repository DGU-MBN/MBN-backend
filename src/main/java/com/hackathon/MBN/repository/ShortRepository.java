package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.Short;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortRepository extends JpaRepository<Short, Long> {

    List<Short> findByLangOrderByIdDesc(String lang);
}
