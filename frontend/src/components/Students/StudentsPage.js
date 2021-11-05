import React, { useState } from 'react';
import MovingGroupsDialog from '../../share/DialogWindows/_dialogWindows/MovingGroupsDialog';
import { StudentsTable } from './StudentsTable';
import CustomDialog from '../../containers/Dialogs/CustomDialog';
import { dialogTypes } from '../../constants/dialogs';
import AddStudentDialog from '../../share/DialogWindows/_dialogWindows/AddStudentDialog';

export const StudentsPage = (props) => {
    const {
        group,
        groups,
        deleteStudentStart,
        isOpenConfirmDialog,
        selectStudentSuccess,
        updateStudentSuccess,
        setIsOpenConfirmDialog,
        isOpenStudentListDialog,
        moveStudentsToGroupStart,
        setIsOpenStudentListDialog,
        setExistingGroupStudentStart,
        ...rest
    } = props;

    const [student, setStudent] = useState(0);
    const [checkBoxStudents, setCheckBoxStudents] = useState([]);
    const [isOpenUpdateDialog, setIsOpenUpdateDialog] = useState(false);

    const confirmDeleteStudent = (studentId) => {
        setIsOpenConfirmDialog(false);
        deleteStudentStart(studentId);
    };

    return (
        <>
            <StudentsTable
                group={group}
                groups={groups}
                setStudent={setStudent}
                checkBoxStudents={checkBoxStudents}
                setCheckBoxStudents={setCheckBoxStudents}
                updateStudentSuccess={updateStudentSuccess}
                setIsOpenUpdateDialog={setIsOpenUpdateDialog}
                setIsOpenConfirmDialog={setIsOpenConfirmDialog}
                setIsOpenStudentListDialog={setIsOpenStudentListDialog}
                {...rest}
            />
            {isOpenStudentListDialog && (
                <MovingGroupsDialog
                    group={group}
                    groups={groups}
                    open={isOpenStudentListDialog}
                    checkBoxStudents={checkBoxStudents}
                    onClose={() => setIsOpenStudentListDialog(false)}
                    moveStudentsToGroupStart={moveStudentsToGroupStart}
                    setExistingGroupStudentStart={setExistingGroupStudentStart}
                />
            )}
            {isOpenUpdateDialog && (
                <AddStudentDialog
                    student={student}
                    groupId={group.id}
                    open={isOpenUpdateDialog}
                    setOpen={() => setIsOpenUpdateDialog(false)}
                />
            )}
            <CustomDialog
                whatDelete='student'
                open={isOpenConfirmDialog}
                type={dialogTypes.DELETE_CONFIRM}
                handelConfirm={() => confirmDeleteStudent(student.id)}
            />
        </>
    );
};
