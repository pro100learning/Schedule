package com.softserve.service;

import com.softserve.entity.Semester;
import com.softserve.exception.EntityNotFoundException;
import com.softserve.exception.IncorrectTimeException;
import com.softserve.repository.SemesterRepository;
import com.softserve.service.impl.SemesterServiceImpl;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@Category(UnitTestCategory.class)
@RunWith(MockitoJUnitRunner.class)
public class SemesterServiceTest {

    @Mock
    private SemesterRepository semesterRepository;

    @InjectMocks
    private SemesterServiceImpl semesterService;

    @Test
    public void testGetById() {
        Semester semester = new Semester();
        semester.setId(1L);
        semester.setDescription("1 semester");
        semester.setStartDay(LocalDate.of(2020, 4, 10));
        semester.setEndDay(LocalDate.of(2020, 5, 10));

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));

        Semester result = semesterService.getById(1L);
        assertNotNull(result);
        assertEquals(semester.getId(), result.getId());
        verify(semesterRepository, times(1)).findById(anyLong());
    }

    @Test(expected = EntityNotFoundException.class)
    public void notFoundId() {
        Semester semester = new Semester();
        semester.setId(1L);
        semester.setDescription("1 semester");
        semester.setStartDay(LocalDate.of(2020, 4, 10));
        semester.setEndDay(LocalDate.of(2020, 5, 10));

        semesterService.getById(2L);
        verify(semesterRepository, times(1)).findById(2L);
    }

    @Test
    public void testSave() {
        Semester semester = new Semester();
        semester.setId(1L);
        semester.setDescription("1 semester");
        semester.setStartDay(LocalDate.of(2020, 4, 10));
        semester.setEndDay(LocalDate.of(2020, 5, 10));

        when(semesterRepository.save(any(Semester.class))).thenReturn(semester);

        Semester result = semesterService.save(semester);
        assertNotNull(result);
        assertEquals(semester.getDescription(), result.getDescription());
        verify(semesterRepository, times(1)).save(semester);
    }

    @Test(expected = IncorrectTimeException.class)
    public void saveWhenStartDayAfterEndDay() {
        Semester semester = new Semester();
        semester.setId(1L);
        semester.setDescription("1 semester");
        semester.setStartDay(LocalDate.of(2020, 4, 10));
        semester.setEndDay(LocalDate.of(2020, 3, 10));

        semesterService.save(semester);
    }

    @Test
    public void testUpdate() {
        Semester semester = new Semester();
        semester.setId(1L);
        semester.setDescription("1 semester");
        semester.setStartDay(LocalDate.of(2020, 4, 10));
        semester.setEndDay(LocalDate.of(2020, 5, 10));
        Semester updatedSemester = new Semester();
        updatedSemester.setId(1L);
        updatedSemester.setDescription("2 semester");
        updatedSemester.setStartDay(LocalDate.of(2020, 5, 11));
        updatedSemester.setEndDay(LocalDate.of(2020, 6, 22));

        when(semesterRepository.update(updatedSemester)).thenReturn(updatedSemester);

        semester = semesterService.update(updatedSemester);
        assertNotNull(semester);
        assertEquals(updatedSemester, semester);
        verify(semesterRepository, times(1)).update(semester);
    }

    @Test(expected = IncorrectTimeException.class)
    public void updateWhenStartDayAfterEndDay() {
        Semester semester = new Semester();
        semester.setId(1L);
        semester.setDescription("1 semester");
        semester.setStartDay(LocalDate.of(2020, 3, 10));
        semester.setEndDay(LocalDate.of(2020, 1, 11));

        semesterService.update(semester);
    }
}
