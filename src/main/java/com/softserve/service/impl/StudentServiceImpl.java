package com.softserve.service.impl;

import com.opencsv.bean.CsvToBeanBuilder;
import com.softserve.entity.Student;
import com.softserve.exception.EntityNotFoundException;
import com.softserve.exception.FieldAlreadyExistsException;
import com.softserve.repository.StudentRepository;
import com.softserve.service.StudentService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;

    @Autowired
    public StudentServiceImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * The method used for getting Student by id
     *
     * @param id Identity Student id
     * @return target Student
     * @throws EntityNotFoundException if Student with id doesn't exist
     */
    @Transactional(readOnly = true)
    @Override
    public Student getById(Long id) {
        log.info("Enter into getById method with id {}", id);
        return studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Student.class, "id", id.toString()));
    }

    /**
     * Method gets information about Students from Repository
     *
     * @return List of all Students
     */
    @Transactional(readOnly = true)
    @Override
    public List<Student> getAll() {
        log.info("Enter into getAll method with no input params");
        return studentRepository.getAll();
    }

    /**
     * Method creates new Student in Repository
     *
     * @param object Student entity with info to be created
     * @return created Student entity
     * @throws FieldAlreadyExistsException if Student with input email already exists
     */
    @Transactional
    @Override
    public Student save(Student object) {
        return saveToDatabase(object);
    }

    private Student saveToDatabase(Student object) {
        log.info("Enter into save method with entity:{}", object);
        checkEmailForUniqueness(object.getEmail());
        return studentRepository.save(object);
    }

    /**
     * Method updates information for an existing Student in Repository
     *
     * @param object Student entity with info to be updated
     * @return updated Student entity
     * @throws FieldAlreadyExistsException if Student with input email already exists
     */
    @SneakyThrows
    @Transactional
    @Override
    public Student update(Student object) {
        log.info("Enter into update method with entity:{}", object);
        checkEmailForUniquenessIgnoringId(object.getEmail(), object.getId());
        return studentRepository.update(object);
    }

    /**
     * Method deletes an existing Student from Repository
     *
     * @param object Student entity to be deleted
     * @return deleted Student entity
     */
    @Transactional
    @Override
    public Student delete(Student object) {
        log.info("Enter into delete method with entity:{}", object);
        return studentRepository.delete(object);
    }

    /**
     * The method used for importing students from csv file.
     * Each line of the file should consist of one or more fields, separated by commas.
     * Each field may or may not be enclosed in double-quotes.
     * First line of the file is a header.
     * All subsequent lines contain data about students, i.e.:
     * <p>
     * "surname","name","patronymic","email"
     * "Romaniuk","Hanna","Stepanivna","romaniuk@gmail.com"
     * "Boichuk","Oleksandr","Ivanovych","boichuk@ukr.net"
     * etc.
     * <p>
     * The method is not transactional in order to prevent interruptions while reading a file
     *
     * @param file file with students data
     * @return list of created students
     * @throws IOException if error happens while creating or deleting file
     */
    @Override
    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    public List<Student> saveFromFile(MultipartFile file, Long groupId) throws IOException {
        log.info("Enter into saveFromFile of StudentServiceImpl");

        String fileName = String.join("", "students_group",
                String.valueOf(groupId), "_", String.valueOf(LocalDateTime.now().getNano()), ".csv");

        File csvFile = new File(fileName);
        file.transferTo(csvFile);
        List<Student> students = new ArrayList<>();

        try (Reader reader = new FileReader(csvFile, StandardCharsets.UTF_8)) {
            students = new CsvToBeanBuilder<Student>(reader)
                    .withType(Student.class)
                    .build().parse();
        } catch (RuntimeException e) {
            log.error("Error occurred while parsing file {}", file.getOriginalFilename(), e);
        }

        List<Student> savedStudents = new ArrayList<>();

        for (Student student : students) {
            try {
                student.getGroup().setId(groupId);
                savedStudents.add(saveToDatabase(student));
            } catch (RuntimeException e) {
                log.error("Error occurred while saving student with email {}", student.getEmail(), e);
            }
        }
        Files.delete(csvFile.toPath());
        return savedStudents;
    }

    private void checkEmailForUniqueness(String email) {
        if(studentRepository.isExistsByEmail(email)) {
            throw new FieldAlreadyExistsException(Student.class, "email", email);
        }
    }

    private void checkEmailForUniquenessIgnoringId(String email, Long id) {
        if(studentRepository.isExistsByEmailIgnoringId(email, id)) {
            throw new FieldAlreadyExistsException(Student.class, "email", email);
        }
    }
}